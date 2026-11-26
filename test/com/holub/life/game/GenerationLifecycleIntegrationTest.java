package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.history.BoardSnapshot;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantStages;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.factory.WeedFactory;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

//발생 신호나 매니저 결과를 준비하지 않고 실제 State·Lifecycle·세대 처리·Undo를 검증한다.
class GenerationLifecycleIntegrationTest
{
    @Test
    void consecutiveWeedTicksRunWithoutResettingTheBoardAndCanAllBeUndone()
    {
        Random random = firstPositionRandom();
        Cell board = newBoard(random);
        GameSession session = new GameSession(board, random);
        session.changeMode(SimulationMode.PLANT);
        WeedFactory factory = new WeedFactory();

        for (int tick = 0; tick < 8; tick++)
        {
            assertTrue(session.nextGeneration(factory));
            BoardSnapshot snapshot = BoardSnapshot.of(board);
            assertEquals(1, alive(snapshot).size());
            Point position = alive(snapshot).iterator().next();
            assertInstanceOf(Weed.class, snapshot.plantAt(position).getPlant());
            assertSame(PlantStages.WEED, snapshot.plantAt(position).getState());
        }

        for (int tick = 0; tick < 8; tick++)
            assertTrue(session.undo());
        assertTrue(BoardSnapshot.of(board).isEmpty());
        assertTrue(session.harvestCounts().isEmpty());
        assertFalse(session.canUndo());
    }

    @Test
    void matureFlowerThatFailsSurvivalDiesWithoutRequestingTargets()
    {
        Random random = firstPositionRandom();
        Cell board = newBoard(random);
        TrackingSunflower sunflower = new TrackingSunflower();
        board.apply(8, 8, cell -> cell.plant(matureFlower(sunflower)));
        //외로운 꽃은 죽지 않으므로 과밀(이웃 4개)로 생존 조건을 못 채우게 함
        board.apply(7, 7, cell -> cell.plant(new PlantData(new AppleTree())));
        board.apply(7, 8, cell -> cell.plant(new PlantData(new AppleTree())));
        board.apply(7, 9, cell -> cell.plant(new PlantData(new AppleTree())));
        board.apply(8, 7, cell -> cell.plant(new PlantData(new AppleTree())));

        assertTrue(new GenerationManager().nextGeneration(board, random).changed());

        assertEquals(0, sunflower.targetRequests);
        assertNull(BoardSnapshot.of(board).plantAt(new Point(8, 8)));
    }

    @Test
    void survivingFlowerBlockedByWeedKeepsItsStageWithoutReproducing()
    {
        Random random = firstPositionRandom();
        Cell board = newBoard(random);
        TrackingSunflower sunflower = new TrackingSunflower();
        board.apply(5, 5, cell -> cell.plant(matureFlower(sunflower)));
        board.apply(5, 6, cell -> cell.plant(new PlantData(new AppleTree())));
        board.apply(6, 5, cell -> cell.plant(new PlantData(new Weed())));

        new GenerationManager().nextGeneration(board, random);

        assertEquals(0, sunflower.targetRequests);
        PlantData remaining = BoardSnapshot.of(board).plantAt(new Point(5, 5));
        assertSame(sunflower, remaining.getPlant());
        assertSame(PlantStages.FLOWER, remaining.getState());
    }

    @Test
    void survivingMatureFlowersSeedOnlyCurrentlyEmptyCellsAndDisappear()
    {
        Random random = firstPositionRandom();
        Cell board = newBoard(random);
        TrackingSunflower sunflower = new TrackingSunflower();
        for (int row = 5; row < 7; row++)
            for (int column = 5; column < 7; column++)
                board.apply(row, column, cell -> cell.plant(matureFlower(sunflower)));

        new GenerationManager().nextGeneration(board, random);

        Set<Point> expected = Set.of(new Point(5, 4), new Point(6, 4),
                new Point(4, 5), new Point(7, 5), new Point(4, 6), new Point(7, 6),
                new Point(5, 7), new Point(6, 7));
        BoardSnapshot snapshot = BoardSnapshot.of(board);
        assertEquals(4, sunflower.targetRequests);
        assertEquals(expected, alive(snapshot));
        for (Point target : expected)
        {
            assertSame(sunflower, snapshot.plantAt(target).getPlant());
            assertSame(PlantStages.SEED, snapshot.plantAt(target).getState());
        }
    }

    @Test
    void realHarvestAndWeedTickUndoRestoresFruitStageCountsAndWeedPosition()
    {
        Random random = firstPositionRandom();
        Cell board = newBoard(random);
        GameSession session = new GameSession(board, random);
        session.changeMode(SimulationMode.PLANT);
        for (int row = 5; row < 7; row++)
            for (int column = 5; column < 7; column++)
                board.apply(row, column, cell -> cell.plant(new PlantData(new AppleTree())));
        WeedFactory factory = new WeedFactory();
        for (int tick = 0; tick < 3; tick++)
            session.nextGeneration(factory);
        BoardSnapshot beforeHarvest = BoardSnapshot.of(board);
        assertSame(PlantStages.FRUIT, beforeHarvest.plantAt(new Point(5, 5)).getState());
        assertTrue(session.harvestCounts().isEmpty());

        session.nextGeneration(factory);

        assertEquals(Map.of(AppleTree.class, 4), session.harvestCounts());
        assertSame(PlantStages.TREE, BoardSnapshot.of(board).plantAt(new Point(5, 5)).getState());
        assertTrue(session.undo());
        assertTrue(session.harvestCounts().isEmpty());
        BoardSnapshot restored = BoardSnapshot.of(board);
        assertEquals(alive(beforeHarvest), alive(restored));
        for (Point point : alive(beforeHarvest))
        {
            assertSame(beforeHarvest.plantAt(point).getPlant(), restored.plantAt(point).getPlant());
            assertSame(beforeHarvest.plantAt(point).getState(), restored.plantAt(point).getState());
        }
    }

    private static Cell newBoard(Random random)
    {
        return new Neighborhood(4, new Neighborhood(4, new Resident(random)));
    }

    private static Random firstPositionRandom()
    {
        return new Random(42)
        {
            @Override public int nextInt(int bound) { return 0; }
        };
    }

    private static PlantData matureFlower(Plant plant)
    {
        PlantData data = new PlantData(plant);
        data.getState().onStep(data);
        data.getState().onStep(data);
        assertSame(PlantStages.FLOWER, data.getState());
        return data;
    }

    private static Set<Point> alive(BoardSnapshot snapshot)
    {
        Set<Point> positions = new HashSet<>();
        for (int row = 0; row < 16; row++)
            for (int column = 0; column < 16; column++)
                if (snapshot.isAlive(new Point(column, row)))
                    positions.add(new Point(column, row));
        return positions;
    }

    private static final class TrackingSunflower extends Sunflower
    {
        private int targetRequests;

        @Override
        public List<Point> seedTargets(Point position, SeedField field, Random random)
        {
            targetRequests++;
            return super.seedTargets(position, field, random);
        }
    }
}
