package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.CellAction;
import com.holub.life.CellReport;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.Storable;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import org.junit.jupiter.api.Test;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GenerationManagerTest
{
    private final GenerationManager manager = new GenerationManager();

    @Test
    void unchangedGenerationSkipsCollectionPlacementAndTransition()
    {
        ScriptedBoard board = new ScriptedBoard();
        board.needsTransition = false;
        ControlledRandom random = new ControlledRandom();

        assertSame(GenerationResult.UNCHANGED, manager.nextGeneration(board, random));
        assertEquals(List.of("calculate"), board.events);
        assertTrue(random.bounds.isEmpty());
    }

    @Test
    void noReproductionLeavesCalculatedBoardUntouched()
    {
        ScriptedBoard board = new ScriptedBoard();
        ControlledRandom random = new ControlledRandom();

        manager.nextGeneration(board, random);

        assertEquals(List.of("calculate", "collect", "transition"), board.events);
        assertTrue(board.placements.isEmpty());
        assertTrue(random.bounds.isEmpty());
    }

    @Test
    void singleCandidateUsesBoardCoordinatesAndSowsBeforeTransitionWithoutRandom()
    {
        ScriptedBoard board = new ScriptedBoard();
        Point target = new Point(9, 4);
        TestFlower flower = new TestFlower(List.of(target));
        board.reproduce(2, 7, flower);
        ControlledRandom random = new ControlledRandom();

        GenerationResult result = manager.nextGeneration(board, random);

        assertSame(flower, board.placements.get(target));
        assertEquals(new Point(7, 2), flower.source);
        assertSame(random, flower.random);
        assertEquals(16, flower.field.widthInCells());
        assertTrue(board.isCurrentlyAlive(target));
        assertTrue(result.changed());
        assertTrue(random.bounds.isEmpty());
        assertEquals(List.of("calculate", "collect", "sow:4,9", "transition"), board.events);
    }

    @Test
    void currentOccupantsAreExcludedEvenWhenDyingAndOutOfBoundsTargetsAreIgnored()
    {
        ScriptedBoard board = new ScriptedBoard();
        Point dying = new Point(4, 4);
        Point planted = new Point(8, 8);
        Point empty = new Point(7, 9);
        board.delegate.apply(dying.y, dying.x, Resident::flipAlive); //다음 생존 값은 false로 유지
        TestFlower protectedPlant = new TestFlower(List.of());
        board.plant(planted, protectedPlant);
        TestFlower flower = new TestFlower(List.of(dying, planted, empty,
                new Point(-1, 0), new Point(0, -1), new Point(16, 0), new Point(0, 16)));
        flower.inspect = field -> {
            assertFalse(field.isEmpty(dying));
            assertFalse(field.isEmpty(planted));
            assertTrue(field.isEmpty(empty));
            assertFalse(field.isEmpty(new Point(16, 0)));
        };
        board.reproduce(1, 1, flower);

        manager.nextGeneration(board, new ControlledRandom());

        assertEquals(Map.of(empty, flower), board.placements);
        assertFalse(board.isCurrentlyAlive(dying));
        assertTrue(board.isCurrentlyAlive(planted));
        assertEquals(1, protectedPlant.initialCalls); //보호된 식물 데이터를 다시 만들지 않음
    }

    @Test
    void everyFlowerInCollisionCanWinWithOneBoundedRandomCall()
    {
        for (int index = 0; index < 3; index++)
        {
            ScriptedBoard board = new ScriptedBoard();
            Point target = new Point(10, 11);
            //같은 종류라도 서로 다른 번식 요청은 개별 항목으로 유지한다.
            List<TestFlower> flowers = List.of(new TestFlower(List.of(target)),
                    new TestFlower(List.of(target)), new TestFlower(List.of(target)));
            for (int source = 0; source < flowers.size(); source++)
                board.reproduce(1, source + 1, flowers.get(source));
            ControlledRandom random = new ControlledRandom(index);

            manager.nextGeneration(board, random);

            assertSame(flowers.get(index), board.placements.get(target));
            assertEquals(List.of(3), random.bounds);
            assertEquals(1, board.placements.size());
        }
    }

    @Test
    void repeatedTargetEntriesKeepTheirIndividualSelectionProbability()
    {
        for (int index = 0; index < 3; index++)
        {
            ScriptedBoard board = new ScriptedBoard();
            Point target = new Point(10, 11);
            TestFlower a = new TestFlower(List.of(target, target));
            TestFlower b = new TestFlower(List.of(target));
            board.reproduce(1, 1, a);
            board.reproduce(1, 2, b);
            ControlledRandom random = new ControlledRandom(index);

            manager.nextGeneration(board, random);

            assertSame(index < 2 ? a : b, board.placements.get(target));
            assertEquals(List.of(3), random.bounds);
        }
    }

    @Test
    void ordinaryBirthAndFlowerCompeteWithoutPriorityAndBirthWinnerIsPreserved()
    {
        for (int index = 0; index < 2; index++)
        {
            ScriptedBoard board = new ScriptedBoard();
            Point target = new Point(10, 11);
            TestFlower birth = new TestFlower(List.of());
            TestFlower flower = new TestFlower(List.of(target));
            board.births.put(target, birth);
            board.reproduce(1, 1, flower);
            ControlledRandom random = new ControlledRandom(index);

            manager.nextGeneration(board, random);

            assertEquals(List.of(2), random.bounds);
            assertEquals(1, birth.initialCalls); //기존 탄생 계산을 다시 실행하지 않음
            assertTrue(board.isCurrentlyAlive(target));
            if (index == 0)
                assertTrue(board.placements.isEmpty());
            else
                assertSame(flower, board.placements.get(target));
        }
    }

    @Test
    void allFlowersSeeSameCompletedSnapshotBeforeAnyPlacement()
    {
        ScriptedBoard board = new ScriptedBoard();
        Point target = new Point(10, 11);
        TestFlower first = new TestFlower(List.of(target));
        TestFlower second = new TestFlower(List.of(target));
        Consumer<SeedField> inspect = field -> {
            assertTrue(field.isEmpty(target));
            assertTrue(board.placements.isEmpty());
        };
        first.inspect = inspect;
        second.inspect = inspect;
        board.reproduce(1, 1, first);
        board.reproduce(1, 2, second);

        manager.nextGeneration(board, new ControlledRandom(1));

        assertSame(first.field, second.field);
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
        assertSame(second, board.placements.get(target));
    }

    @Test
    void unsuccessfulReproductionDoesNotRetryOrConsumeSelectionRandom()
    {
        ScriptedBoard board = new ScriptedBoard();
        TestFlower flower = new TestFlower(List.of());
        board.reproduce(1, 1, flower);
        ControlledRandom random = new ControlledRandom();

        manager.nextGeneration(board, random);

        assertEquals(1, flower.calls);
        assertTrue(board.placements.isEmpty());
        assertTrue(random.bounds.isEmpty());
    }

    private static class TestFlower extends Plant
    {
        private final List<Point> targets;
        private Consumer<SeedField> inspect = field -> {};
        private Point source;
        private SeedField field;
        private Random random;
        private int calls;
        private int initialCalls;

        private TestFlower(List<Point> targets)
        {
            this.targets = targets;
        }

        @Override
        public PlantState initialState()
        {
            initialCalls++;
            return next -> {};
        }

        public PlantState nextState(PlantState current) { return current; }
        public String getImage(PlantState state) { return ""; }

        @Override
        public List<Point> seedTargets(Point position, SeedField field, Random random)
        {
            calls++;
            this.source = position;
            this.field = field;
            this.random = random;
            inspect.accept(field);
            return targets;
        }
    }

    private record Request(int row, int column, Plant plant) {}

    //미구현인 Lifecycle·State 대신 ①의 결과와 번식 신호를 준비한다.
    //현재 상태 알림, sow와 transition은 실제 Neighborhood·Resident를 사용한다.
    private static final class ScriptedBoard implements Cell
    {
        private final Cell delegate = new Neighborhood(4,
                new Neighborhood(4, new Resident(new Random(42))));
        private final List<Request> requests = new ArrayList<>();
        private final Map<Point, Plant> births = new LinkedHashMap<>();
        private final Map<Point, Plant> placements = new LinkedHashMap<>();
        private final List<String> events = new ArrayList<>();
        private boolean needsTransition = true;
        private boolean collected;

        private void reproduce(int row, int column, Plant plant)
        {
            plant(new Point(column, row), plant);
            requests.add(new Request(row, column, plant));
        }

        private void plant(Point point, Plant plant)
        {
            delegate.apply(point.y, point.x, cell -> cell.plant(new PlantData(plant)));
        }

        private boolean isCurrentlyAlive(Point point)
        {
            boolean[] alive = { false };
            delegate.collect(0, 0, new CellReport()
            {
                @Override
                public void currentCell(int row, int column, boolean occupied)
                {
                    if (row == point.y && column == point.x)
                        alive[0] = occupied;
                }
            });
            return alive[0];
        }

        @Override
        public boolean figureNextState(Cell n, Cell s, Cell e, Cell w,
                                       Cell ne, Cell nw, Cell se, Cell sw)
        {
            events.add("calculate");
            births.forEach((point, plant) -> delegate.apply(point.y, point.x,
                    cell -> cell.sow(new PlantData(plant))));
            return needsTransition;
        }

        @Override
        public void collect(int row, int column, CellReport report)
        {
            events.add("collect");
            //현재 상태보다 먼저 신호가 와도 모두 수집한 후에 번식 좌표를 조회해야 한다.
            for (Request request : requests)
                report.seeding(request.row(), request.column(), request.plant());
            delegate.collect(row, column, report);
            collected = true;
        }

        @Override
        public void apply(int row, int column, CellAction action)
        {
            assertTrue(collected, "후보 수집 완료 후에만 배치");
            events.add("sow:" + row + "," + column);
            delegate.apply(row, column, cell -> {
                assertFalse(cell.isAlive(), "현재 점유된 칸에는 배치하지 않음");
                action.execute(cell);
                assertFalse(cell.isAlive(), "씨앗은 현재 상태를 변경하지 않음");
                cell.collect(row, column, new CellReport()
                {
                    @Override
                    public void cell(int r, int c, boolean surviving, Plant born)
                    {
                        assertNotNull(born);
                        assertNull(placements.put(new Point(c, r), born), "칸당 한 번만 배치");
                    }
                });
            });
        }

        @Override
        public boolean transition()
        {
            events.add("transition");
            return delegate.transition();
        }

        public int widthInCells() { return delegate.widthInCells(); }
        public boolean isAlive() { return delegate.isAlive(); }
        public Direction isDisruptiveTo() { return delegate.isDisruptiveTo(); }
        public Cell edge(int row, int column) { return delegate.edge(row, column); }
        public Cell create() { throw new AssertionError("create는 호출하지 않음"); }
        public void clear() { throw new AssertionError("clear는 호출하지 않음"); }
        public Storable createMemento() { throw new AssertionError("스냅샷은 collect로 수집"); }
        public boolean transfer(Storable memento, Point point, boolean load)
        {
            throw new AssertionError("읽기 중 경계 상태를 바꾸는 transfer는 사용하지 않음");
        }
        public void redraw(Graphics graphics, Rectangle bounds, boolean all)
        {
            throw new AssertionError("UI는 사용하지 않음");
        }
    }

    private static final class ControlledRandom extends Random
    {
        private final int[] indices;
        private final List<Integer> bounds = new ArrayList<>();

        private ControlledRandom(int... indices)
        {
            this.indices = indices;
        }

        @Override
        public int nextInt(int bound)
        {
            int call = bounds.size();
            assertTrue(call < indices.length, "예상하지 않은 난수 호출");
            bounds.add(bound);
            assertTrue(indices[call] >= 0 && indices[call] < bound);
            return indices[call];
        }

        @Override
        protected int next(int bits)
        {
            throw new AssertionError("nextInt(bound)만 사용");
        }
    }
}
