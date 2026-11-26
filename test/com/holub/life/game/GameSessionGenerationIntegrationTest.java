package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.history.BoardSnapshot;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.TemporaryStage;
import com.holub.life.plant.factory.AppleTreeFactory;
import com.holub.life.plant.factory.WeedFactory;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionGenerationIntegrationTest
{
    @Test
    void weedOnlyTickIsStoredAsOneUndoEntry()
    {
        Fixture fixture = new Fixture(new GenerationManager());

        assertTrue(fixture.session.nextGeneration(new WeedFactory()));

        BoardSnapshot grown = BoardSnapshot.of(fixture.board);
        assertInstanceOf(Weed.class, grown.plantAt(new Point(0, 0)).getPlant());
        assertEquals(TemporaryStage.WEED, grown.plantAt(new Point(0, 0)).getState());
        assertTrue(fixture.session.canUndo());
        assertTrue(fixture.session.undo());
        assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
        assertEquals(SimulationMode.PLANT, fixture.session.getMode());
        assertTrue(fixture.session.harvestCounts().isEmpty());
        assertFalse(fixture.session.canUndo());
        assertFalse(fixture.session.undo());

        //Undo 뒤 빈 보드의 일반 tick은 잡초를 다시 요청하거나 이력을 만들지 않는다.
        assertFalse(fixture.session.nextGeneration());
        assertFalse(fixture.session.canUndo());
    }

    @Test
    void weedAndHarvestFromSameTickAreRestoredTogetherUsingInjectedManager()
    {
        HarvestingManager manager = new HarvestingManager();
        Fixture fixture = new Fixture(manager);

        assertTrue(fixture.session.nextGeneration(new WeedFactory()));

        assertEquals(1, manager.weedCalls);
        assertEquals(0, manager.ordinaryCalls);
        assertFalse(BoardSnapshot.of(fixture.board).isEmpty());
        assertEquals(Map.of(AppleTree.class, 1), fixture.session.harvestCounts());

        assertTrue(fixture.session.undo());

        assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
        assertTrue(fixture.session.harvestCounts().isEmpty());
        assertFalse(fixture.session.canUndo());
    }

    @Test
    void unchangedWeedRequestDoesNotAddAnEntryBeforePreviousClick()
    {
        GenerationManager unchangedManager = new GenerationManager()
        {
            @Override
            public GenerationResult nextGeneration(Cell board, Random random, PlantFactory factory)
            {
                return GenerationResult.UNCHANGED;
            }
        };
        Fixture fixture = new Fixture(unchangedManager);
        fixture.session.selectPlant(new AppleTreeFactory());
        fixture.session.userClicked(new Point(15, 15), new Rectangle(0, 0, 160, 160));
        assertTrue(fixture.session.canUndo());
        PlantFactory unusedFactory = new PlantFactory()
        {
            public Plant createPlant()
            {
                throw new AssertionError("반영 후 잡초를 별도로 생성하지 않음");
            }
        };

        assertFalse(fixture.session.nextGeneration(unusedFactory));
        assertTrue(fixture.session.undo());

        assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
        assertFalse(fixture.session.canUndo());
    }

    @Test
    void harvestOnlyResultsAreUndoableThroughBothGenerationEntryPoints()
    {
        //보드 변화 여부와 독립적으로 수확 결과를 수신·집계·복원하는 계약을 확인한다.
        for (boolean requestWeed : List.of(false, true))
        {
            GenerationManager manager = new GenerationManager()
            {
                private GenerationResult harvestOnly()
                {
                    return new GenerationResult(false, List.of(new AppleTree()));
                }

                public GenerationResult nextGeneration(Cell board, Random random)
                {
                    return harvestOnly();
                }

                public GenerationResult nextGeneration(Cell board, Random random, PlantFactory factory)
                {
                    return harvestOnly();
                }
            };
            Fixture fixture = new Fixture(manager);

            boolean changed = requestWeed
                    ? fixture.session.nextGeneration(new WeedFactory())
                    : fixture.session.nextGeneration();

            assertFalse(changed);
            assertEquals(Map.of(AppleTree.class, 1), fixture.session.harvestCounts());
            assertTrue(fixture.session.canUndo());
            assertTrue(fixture.session.undo());
            assertTrue(fixture.session.harvestCounts().isEmpty());
            assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
            assertFalse(fixture.session.canUndo());
        }
    }

    //실제 잡초 생성·배치는 실행하고, 아직 미구현인 수확 신호만 결과에 추가한다.
    private static final class HarvestingManager extends GenerationManager
    {
        private int ordinaryCalls;
        private int weedCalls;

        @Override
        public GenerationResult nextGeneration(Cell board, Random random)
        {
            ordinaryCalls++;
            GenerationResult result = super.nextGeneration(board, random);
            return new GenerationResult(result.changed(), List.of(new AppleTree()));
        }

        @Override
        public GenerationResult nextGeneration(Cell board, Random random, PlantFactory factory)
        {
            weedCalls++;
            GenerationResult result = super.nextGeneration(board, random, factory);
            return new GenerationResult(result.changed(), List.of(new AppleTree()));
        }
    }

    private static final class Fixture
    {
        private final Random random = new Random(42)
        {
            @Override
            public int nextInt(int bound) { return 0; }
        };
        private final Cell board = new Neighborhood(4, new Neighborhood(4, new Resident(random)));
        private final GameSession session;

        private Fixture(GenerationManager manager)
        {
            session = new GameSession(board, random, manager);
            session.changeMode(SimulationMode.PLANT);
        }
    }
}
