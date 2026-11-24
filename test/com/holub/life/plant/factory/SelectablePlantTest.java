package com.holub.life.plant.factory;

import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Dandelion;
import com.holub.life.plant.species.OrangeTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//식물 선택 목록 테스트
class SelectablePlantTest
{
    @Test
    @DisplayName("선택 목록에 잡초는 없음")
    void weedNotSelectable()
    {
        for (SelectablePlant plant : SelectablePlant.values())
            assertFalse(plant.factory().createPlant() instanceof Weed, plant.name());
    }

    @Test
    @DisplayName("사과나무·오렌지나무·해바라기·민들레 모두 선택 가능")
    void enabledPlants()
    {
        assertTrue(SelectablePlant.APPLE_TREE.isEnabled());
        assertTrue(SelectablePlant.SUNFLOWER.isEnabled());
        assertTrue(SelectablePlant.DANDELION.isEnabled());
        assertTrue(SelectablePlant.ORANGE_TREE.isEnabled());
    }

    @Test
    @DisplayName("항목마다 해당 종류의 팩토리를 가짐")
    void matchingFactory()
    {
        assertTrue(SelectablePlant.APPLE_TREE.factory().createPlant() instanceof AppleTree);
        assertTrue(SelectablePlant.ORANGE_TREE.factory().createPlant() instanceof OrangeTree);
        assertTrue(SelectablePlant.SUNFLOWER.factory().createPlant() instanceof Sunflower);
        assertTrue(SelectablePlant.DANDELION.factory().createPlant() instanceof Dandelion);
    }
}
