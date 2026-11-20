package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.species.Weed;

//잡초 생성 팩토리
public class WeedFactory extends PlantFactory {

    @Override
    public Plant createPlant() {
        return new Weed();
    }
}
