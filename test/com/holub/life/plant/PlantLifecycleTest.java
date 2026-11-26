package com.holub.life.plant;

import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.OrangeTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//생존 판정(applySurvival)과 성장 가능 여부(canGrow) 단위 테스트
class PlantLifecycleTest
{
    private final PlantLifecycle lifecycle = new PlantLifecycle();

    @Test
    @DisplayName("생존 조건 충족: 단계 그대로 살아 있음")
    void survivesWhenRuleSatisfied()
    {
        for (PlantData p : new PlantData[]{ at(new AppleTree(), PlantStages.SEED), at(new AppleTree(), PlantStages.TREE),
                                            at(new AppleTree(), PlantStages.FRUIT), at(new Sunflower(), PlantStages.FLOWER),
                                            at(new Weed(), PlantStages.WEED) })
        {
            PlantState before = p.getState();
            assertTrue(lifecycle.applySurvival(p, true), before.toString());
            assertSame(before, p.getState());
        }
    }

    @Test
    @DisplayName("생존 조건 불충족: 씨앗·새싹·꽃·잡초는 사망")
    void othersDie()
    {
        assertFalse(lifecycle.applySurvival(at(new AppleTree(), PlantStages.SEED), false));
        assertFalse(lifecycle.applySurvival(at(new AppleTree(), PlantStages.SEEDLING), false));
        assertFalse(lifecycle.applySurvival(at(new Sunflower(), PlantStages.FLOWER), false));
        assertFalse(lifecycle.applySurvival(at(new Weed(), PlantStages.WEED), false));
    }

    @Test
    @DisplayName("생존 조건 불충족: 나무·열매나무는 사망 대신 시듦 (외로움·과밀 모두), 수확 없음")
    void treesWither()
    {
        for (PlantState s : new PlantState[]{ PlantStages.TREE, PlantStages.FRUIT })
        {
            PlantData p = at(new AppleTree(), s);
            assertTrue(lifecycle.applySurvival(p, false), s.toString());
            assertEquals(PlantStages.WITHERED, p.getState());
            assertFalse(p.harvested());
        }
    }

    @Test
    @DisplayName("시든 나무: 충족하면 일반 나무로 회복, 아니면 사망 (시든 상태는 1 step)")
    void witheredRecoversOrDies()
    {
        PlantData recovered = at(new OrangeTree(), PlantStages.WITHERED);
        assertTrue(lifecycle.applySurvival(recovered, true));
        assertEquals(PlantStages.TREE, recovered.getState());

        assertFalse(lifecycle.applySurvival(at(new AppleTree(), PlantStages.WITHERED), false));
    }

    @Test
    @DisplayName("성장 가능: 생존 조건 충족 + 이웃에 잡초 없음 + 시든 나무 아님")
    void canGrow()
    {
        assertTrue (lifecycle.canGrow(at(new AppleTree(), PlantStages.TREE), true,  false));
        assertFalse(lifecycle.canGrow(at(new AppleTree(), PlantStages.TREE), false, false));
        assertFalse(lifecycle.canGrow(at(new AppleTree(), PlantStages.TREE), true,  true));
        assertFalse(lifecycle.canGrow(at(new AppleTree(), PlantStages.WITHERED), true, false));  //회복한 step엔 성장 X
    }

    private static PlantData at(Plant plant, PlantState state)
    {
        PlantData data = new PlantData(plant);
        data.setState(state);
        return data;
    }
}
