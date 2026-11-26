package com.holub.life.plant;

import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//구체 State 단위 테스트: 각 단계가 onStep()에서 하는 일 (TemporaryStageTest를 대체)
class PlantStatesTest
{
    private static final PlantState[] ALL = {
        PlantStages.SEED, PlantStages.SEEDLING, PlantStages.TREE, PlantStages.FRUIT,
        PlantStages.WITHERED, PlantStages.FLOWER, PlantStages.WEED };

    @Test
    @DisplayName("나무·열매나무만 시들 수 있고, 시든 나무만 isWithered")
    void witherFlags()
    {
        for (PlantState s : ALL)
        {
            assertEquals(s == PlantStages.TREE || s == PlantStages.FRUIT, s.canWither(), s.toString());
            assertEquals(s == PlantStages.WITHERED, s.isWithered(), s.toString());
        }
    }

    @Test
    @DisplayName("씨앗·새싹·나무: 다음 단계는 식물 종류가 정함 (과수 새싹 -> 나무, 꽃 새싹 -> 꽃)")
    void growthFollowsSpecies()
    {
        assertEquals(PlantStages.SEEDLING, stepFrom(new AppleTree(), PlantStages.SEED).getState());
        assertEquals(PlantStages.TREE,     stepFrom(new AppleTree(), PlantStages.SEEDLING).getState());
        assertEquals(PlantStages.FRUIT,    stepFrom(new AppleTree(), PlantStages.TREE).getState());
        assertEquals(PlantStages.FLOWER,   stepFrom(new Sunflower(), PlantStages.SEEDLING).getState());
    }

    @Test
    @DisplayName("열매나무: 수확 신호를 남기고 나무로 돌아감")
    void fruitHarvests()
    {
        PlantData next = stepFrom(new AppleTree(), PlantStages.FRUIT);
        assertEquals(PlantStages.TREE, next.getState());
        assertTrue(next.harvested());
        assertFalse(next.reproduces());
        assertFalse(next.removed());
    }

    @Test
    @DisplayName("꽃: 씨 뿌림·소멸 신호만 남기고 단계는 그대로")
    void flowerReproducesAndRemoves()
    {
        PlantData next = stepFrom(new Sunflower(), PlantStages.FLOWER);
        assertEquals(PlantStages.FLOWER, next.getState());
        assertTrue(next.reproduces());
        assertTrue(next.removed());
        assertFalse(next.harvested());
    }

    @Test
    @DisplayName("잡초·시든 나무: onStep을 불러도 아무 변화 없음")
    void weedAndWitheredDoNothing()
    {
        PlantData weed = stepFrom(new Weed(), PlantStages.WEED);
        assertEquals(PlantStages.WEED, weed.getState());
        assertFalse(weed.reproduces() || weed.harvested() || weed.removed());

        PlantData withered = stepFrom(new AppleTree(), PlantStages.WITHERED);
        assertEquals(PlantStages.WITHERED, withered.getState());
        assertFalse(withered.reproduces() || withered.harvested() || withered.removed());
    }

    @Test
    @DisplayName("copy()로 만든 다음 세대에는 지난 step의 신호가 남지 않음")
    void copyClearsSignals()
    {
        PlantData next = stepFrom(new AppleTree(), PlantStages.FRUIT);
        assertTrue(next.harvested());
        assertFalse(next.copy().harvested());
    }

    //state 단계의 식물에서 onStep 한 번 실행한 결과
    private static PlantData stepFrom(Plant plant, PlantState state)
    {
        PlantData next = new PlantData(plant);
        next.setState(state);
        state.onStep(next);
        return next;
    }
}
