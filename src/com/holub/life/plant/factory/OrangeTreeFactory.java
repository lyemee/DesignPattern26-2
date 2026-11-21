package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.OrangeTree;

//오렌지나무 생성 팩토리
public class OrangeTreeFactory extends PlantFactory {

    @Override
    public Plant createPlant() {
        return new OrangeTree();
    }
}
