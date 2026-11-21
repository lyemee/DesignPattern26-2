package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.Sunflower;

//해바라기 생성 팩토리
public class SunflowerFactory extends PlantFactory {

    @Override
    public Plant createPlant() {
        return new Sunflower();
    }
}
