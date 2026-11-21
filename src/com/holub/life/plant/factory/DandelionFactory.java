package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.Dandelion;

//민들레 생성 팩토리
public class DandelionFactory extends PlantFactory {

    @Override
    public Plant createPlant() {
        return new Dandelion();
    }
}
