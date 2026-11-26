package com.holub.life.plant;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlantSeedTargetsTest
{
    private final SeedField field = new SeedField()
    {
        public boolean isEmpty(Point position) { return true; }
        public int widthInCells() { return 16; }
    };

    @Test
    void nonReproducingPlantHasNoTargetsAndDoesNotConsumeRandom()
    {
        Random random = new Random()
        {
            @Override
            protected int next(int bits)
            {
                throw new AssertionError("번식하지 않는 식물은 난수를 소비하지 않음");
            }
        };

        assertTrue(new TestPlant().seedTargets(new Point(1, 2), field, random).isEmpty());
    }

    @Test
    void existingSingleArgumentOverrideRemainsUsableThroughNewContract()
    {
        List<Point> targets = List.of(new Point(3, 4));
        Point source = new Point(1, 2);
        Plant plant = new TestPlant()
        {
            @Override
            public List<Point> seedTargets(Point position)
            {
                assertSame(source, position);
                return targets;
            }
        };

        assertSame(targets, plant.seedTargets(source, field, new Random(42)));
    }

    private static class TestPlant extends Plant
    {
        public PlantState initialState() { return next -> {}; }
        public PlantState nextState(PlantState current) { return current; }
        //PR #3의 이미지 계약이 합쳐져도 이 테스트 후보를 그대로 사용할 수 있다.
        public String getImage(PlantState state) { return ""; }
    }
}
