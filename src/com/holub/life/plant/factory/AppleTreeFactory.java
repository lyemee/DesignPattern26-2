package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.AppleTree;

//사과나무 생성 팩토리
public class AppleTreeFactory extends PlantFactory {

    @Override
    public Plant createPlant() {
        return new AppleTree();
    }
}
