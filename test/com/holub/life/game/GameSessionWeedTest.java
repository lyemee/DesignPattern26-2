package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.CellReport;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.PlantState;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionWeedTest
{
    @Test
    void ordinaryTicksDoNotCreateWeedCandidates()
    {
        Fixture fixture = new Fixture();
        fixture.session.changeMode(SimulationMode.PLANT);

        assertFalse(fixture.session.nextGeneration());
        assertFalse(fixture.session.nextGeneration());

        assertTrue(fixture.occupied().isEmpty());
        assertEquals(0, fixture.random.calls);
    }

    @Test
    void requestedTickCreatesWeedInPlantModeAndRequestsRedraw()
    {
        Fixture fixture = new Fixture();
        fixture.session.changeMode(SimulationMode.PLANT);
        CountingWeedFactory factory = new CountingWeedFactory();

        assertTrue(fixture.session.nextGeneration(factory));

        assertEquals(List.of(new Point(0, 0)), fixture.occupied());
        assertEquals(1, factory.calls);
        assertEquals(1, fixture.random.calls);
        assertEquals(16, fixture.random.bound);
    }

    @Test
    void weedRequestIsIgnoredInConwayModeWithoutConsumingRandom()
    {
        Fixture fixture = new Fixture();
        CountingWeedFactory factory = new CountingWeedFactory();

        assertFalse(fixture.session.nextGeneration(factory));

        assertEquals(SimulationMode.CONWAY, fixture.session.getMode());
        assertTrue(fixture.occupied().isEmpty());
        assertEquals(0, factory.calls);
        assertEquals(0, fixture.random.calls);
    }

    @Test
    void weedRequestDoesNotCarryOverToLaterTicks()
    {
        Fixture fixture = new Fixture();
        fixture.session.changeMode(SimulationMode.PLANT);
        CountingWeedFactory factory = new CountingWeedFactory();
        fixture.session.nextGeneration(factory);
        fixture.session.clear();

        assertFalse(fixture.session.nextGeneration());

        assertTrue(fixture.occupied().isEmpty());
        assertEquals(1, factory.calls);
        assertEquals(1, fixture.random.calls);
    }

    @Test
    void weedFactoryIsIndependentOfUserSelectedPlantFactory()
    {
        Fixture fixture = new Fixture();
        fixture.session.changeMode(SimulationMode.PLANT);
        fixture.session.selectPlant(new PlantFactory()
        {
            public Plant createPlant()
            {
                throw new AssertionError("잡초를 만들 때 사용자 선택 Factory는 사용하지 않음");
            }
        });
        CountingWeedFactory factory = new CountingWeedFactory();

        assertTrue(fixture.session.nextGeneration(factory));

        assertEquals(1, factory.calls);
        assertEquals(1, fixture.occupied().size());
    }

    @Test
    void nullWeedFactoryIsRejectedBeforeAdvancing()
    {
        Fixture fixture = new Fixture();

        assertThrows(NullPointerException.class, () -> fixture.session.nextGeneration(null));

        assertTrue(fixture.occupied().isEmpty());
        assertEquals(0, fixture.random.calls);
    }

    private static final class Fixture
    {
        private final FirstPositionRandom random = new FirstPositionRandom();
        private final Cell board = new Neighborhood(2, new Neighborhood(2, new Resident(random)));
        private final GameSession session = new GameSession(board, random);

        private List<Point> occupied()
        {
            List<Point> result = new ArrayList<>();
            board.collect(0, 0, new CellReport()
            {
                public void currentCell(int row, int column, boolean alive)
                {
                    if (alive)
                        result.add(new Point(column, row));
                }
            });
            return result;
        }
    }

    //PR #3의 Weed처럼 초기 상태부터 잡초이며 성장을 방해하는 테스트 후보.
    private static final class CountingWeedFactory extends PlantFactory
    {
        private int calls;

        public Plant createPlant()
        {
            calls++;
            return new Plant()
            {
                private final PlantState weed = next -> {};
                public PlantState initialState() { return weed; }
                public PlantState nextState(PlantState current) { return weed; }
                public boolean blocksGrowth() { return true; }
                public String getImage(PlantState state) { return ""; }
            };
        }
    }

    private static final class FirstPositionRandom extends Random
    {
        private int calls;
        private int bound;

        public int nextInt(int bound)
        {
            this.bound = bound;
            calls++;
            return 0;
        }

        protected int next(int bits)
        {
            throw new AssertionError("위치 선택 이외의 난수 호출 없음");
        }
    }
}
