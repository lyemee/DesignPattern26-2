package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.CellAction;
import com.holub.life.CellReport;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.Storable;
import com.holub.life.history.BoardSnapshot;
import com.holub.life.plant.Plant;
import com.holub.life.plant.factory.WeedFactory;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.OrangeTree;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.Test;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionHarvestDeliveryTest
{
    @Test
    void actualManagerDeliversReportedHarvestsToCounterAndUndo()
    {
        Fixture fixture = new Fixture();
        Plant apple = new AppleTree();
        fixture.board.harvestEvents.addAll(List.of(apple, new OrangeTree(), apple));

        fixture.session.nextGeneration();

        assertEquals(Map.of(AppleTree.class, 2, OrangeTree.class, 1), fixture.session.harvestCounts());
        assertTrue(fixture.board.harvestEvents.isEmpty());
        assertTrue(fixture.session.canUndo());
        assertEquals(0, fixture.random.calls);

        assertTrue(fixture.session.undo());
        assertTrue(fixture.session.harvestCounts().isEmpty());
        assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
        assertFalse(fixture.session.canUndo());
    }

    @Test
    void sameTickHarvestDeliveryAndRealWeedPlacementAreUndoneTogether()
    {
        Fixture fixture = new Fixture();
        fixture.board.harvestEvents.addAll(List.of(new AppleTree(), new OrangeTree()));

        assertTrue(fixture.session.nextGeneration(new WeedFactory()));

        BoardSnapshot grown = BoardSnapshot.of(fixture.board);
        assertInstanceOf(Weed.class, grown.plantAt(new Point(0, 0)).getPlant());
        assertSame(new Weed().initialState(), grown.plantAt(new Point(0, 0)).getState());
        assertEquals(Map.of(AppleTree.class, 1, OrangeTree.class, 1), fixture.session.harvestCounts());
        assertTrue(fixture.board.harvestEvents.isEmpty());
        assertEquals(1, fixture.random.calls); //위치 선택만 소비하고 수확 전달은 소비하지 않음

        assertTrue(fixture.session.undo());

        assertTrue(BoardSnapshot.of(fixture.board).isEmpty());
        assertTrue(fixture.session.harvestCounts().isEmpty());
        assertFalse(fixture.session.canUndo());
    }

    @Test
    void laterEmptyTickDoesNotRepeatHarvestsOrInsertAnUndoEntry()
    {
        Fixture fixture = new Fixture();
        Plant apple = new AppleTree();
        fixture.board.harvestEvents.addAll(List.of(apple, apple));
        fixture.session.nextGeneration();
        assertEquals(Map.of(AppleTree.class, 2), fixture.session.harvestCounts());

        fixture.board.harvestEvents.add(new OrangeTree());
        fixture.session.nextGeneration();
        assertEquals(Map.of(AppleTree.class, 2, OrangeTree.class, 1), fixture.session.harvestCounts());

        assertFalse(fixture.session.nextGeneration()); //보고가 없는 안정 상태 tick
        assertEquals(Map.of(AppleTree.class, 2, OrangeTree.class, 1), fixture.session.harvestCounts());

        assertTrue(fixture.session.undo());
        assertEquals(Map.of(AppleTree.class, 2), fixture.session.harvestCounts());
        assertTrue(fixture.session.undo());
        assertTrue(fixture.session.harvestCounts().isEmpty());
        assertFalse(fixture.session.canUndo());
        assertEquals(0, fixture.random.calls);
    }

    private static final class Fixture
    {
        private final CountingRandom random = new CountingRandom();
        private final HarvestReportingBoard board = new HarvestReportingBoard(random);
        //매니저를 대체하거나 결과를 주입하지 않고 실제 수집·전달 코드를 사용한다.
        private final GameSession session = new GameSession(board, random);

        private Fixture()
        {
            session.changeMode(SimulationMode.PLANT);
        }
    }

    //State의 수확 발생 판단만 대신한다. 조회·배치·반영·스냅샷은 실제 보드에 위임한다.
    private static final class HarvestReportingBoard implements Cell
    {
        private final Cell delegate;
        private final List<Plant> harvestEvents = new ArrayList<>();

        private HarvestReportingBoard(Random random)
        {
            delegate = new Neighborhood(4, new Neighborhood(4, new Resident(random)));
        }

        @Override
        public boolean figureNextState(Cell n, Cell s, Cell e, Cell w,
                                       Cell ne, Cell nw, Cell se, Cell sw)
        {
            boolean changed = delegate.figureNextState(n, s, e, w, ne, nw, se, sw);
            return changed || !harvestEvents.isEmpty();
        }

        @Override
        public void collect(int row, int column, CellReport report)
        {
            delegate.collect(row, column, report);
            for (Plant plant : harvestEvents)
                report.harvested(plant);
        }

        @Override
        public boolean transition()
        {
            harvestEvents.clear(); //실제 Resident와 같이 반영 시 신호를 초기화한다.
            return delegate.transition();
        }

        public void apply(int row, int column, CellAction action) { delegate.apply(row, column, action); }
        public Cell edge(int row, int column) { return delegate.edge(row, column); }
        public int widthInCells() { return delegate.widthInCells(); }
        public boolean isAlive() { return delegate.isAlive(); }
        public Direction isDisruptiveTo() { return delegate.isDisruptiveTo(); }
        public void clear() { delegate.clear(); harvestEvents.clear(); }
        public Storable createMemento() { return delegate.createMemento(); }
        public Cell create() { throw new AssertionError("테스트 보드는 복제하지 않음"); }

        public boolean transfer(Storable memento, Point point, boolean load)
        {
            return delegate.transfer(memento, point, load);
        }

        public void redraw(Graphics graphics, Rectangle bounds, boolean all)
        {
            delegate.redraw(graphics, bounds, all);
        }
    }

    private static final class CountingRandom extends Random
    {
        private int calls;

        public int nextInt(int bound)
        {
            calls++;
            return 0;
        }

        protected int next(int bits)
        {
            throw new AssertionError("수확 수집은 난수를 사용하지 않음");
        }
    }
}
