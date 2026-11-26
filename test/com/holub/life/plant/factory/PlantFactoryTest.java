package com.holub.life.plant.factory;

import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Dandelion;
import com.holub.life.plant.species.OrangeTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//종류별 팩토리가 맞는 식물을 만드는지 테스트
class PlantFactoryTest
{
    @Test
    @DisplayName("팩토리마다 해당 종류의 식물을 생성")
    void createsMatchingPlant()
    {
        assertTrue(new AppleTreeFactory().createPlant() instanceof AppleTree);
        assertTrue(new OrangeTreeFactory().createPlant() instanceof OrangeTree);
        assertTrue(new SunflowerFactory().createPlant() instanceof Sunflower);
        assertTrue(new DandelionFactory().createPlant() instanceof Dandelion);
        assertTrue(new WeedFactory().createPlant() instanceof Weed);
    }

    @Test
    @DisplayName("PlantFactory 타입으로 다뤄도 종류별 식물이 생성됨")
    void usableAsPlantFactory()
    {
        PlantFactory factory = new SunflowerFactory();
        assertTrue(factory.createPlant() instanceof Sunflower);

        factory = new WeedFactory();
        assertTrue(factory.createPlant().blocksGrowth());
    }
}
