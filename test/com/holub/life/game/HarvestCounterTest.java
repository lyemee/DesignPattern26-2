package com.holub.life.game;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//식물 종류별 수확량 집계 테스트
class HarvestCounterTest
{
    //종류 구분만 필요하므로 구체 식물 대신 테스트용 식물 두 종류 사용
    static class TestApple extends Plant
    {
        public PlantState initialState()                { return next -> {}; }
        public PlantState nextState(PlantState current) { return current; }
        public String getImage(PlantState state)        { return null; }
    }
    static class TestOrange extends TestApple {}

    @Test
    @DisplayName("수확 이벤트마다 해당 종류의 개수가 1씩 늘어남")
    void countsByType()
    {
        HarvestCounter counter = new HarvestCounter();
        counter.add(List.of(new TestApple(), new TestOrange(), new TestApple()));
        counter.add(List.of(new TestApple()));

        assertEquals(3, counter.count(TestApple.class));
        assertEquals(1, counter.count(TestOrange.class));
        assertEquals(Map.of(TestApple.class, 3, TestOrange.class, 1), counter.counts());
    }

    @Test
    @DisplayName("수확한 적 없는 종류는 0, 빈 목록은 변화 없음")
    void noHarvest()
    {
        HarvestCounter counter = new HarvestCounter();
        counter.add(List.of());
        assertEquals(0, counter.count(TestApple.class));
        assertTrue(counter.counts().isEmpty());
    }

    @Test
    @DisplayName("초기화하면 모든 종류가 0")
    void reset()
    {
        HarvestCounter counter = new HarvestCounter();
        counter.add(List.of(new TestApple()));
        counter.reset();
        assertTrue(counter.counts().isEmpty());
    }

    @Test
    @DisplayName("저장해 둔 개수로 되돌리고, 저장본은 이후 집계에 영향받지 않음")
    void restore()
    {
        HarvestCounter counter = new HarvestCounter();
        counter.add(List.of(new TestApple()));
        Map<Class<? extends Plant>, Integer> saved = counter.counts();

        counter.add(List.of(new TestApple(), new TestOrange()));
        assertEquals(Map.of(TestApple.class, 1), saved);         //복사본이라 그대로

        counter.restore(saved);
        assertEquals(Map.of(TestApple.class, 1), counter.counts());
    }
}
