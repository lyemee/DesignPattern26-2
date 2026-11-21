package com.holub.life.plant;

//구체 식물을 만드는 factory
public abstract class PlantFactory {

    //어떤 구체 plant를 만들지는 하위 클래스가 결정
    public abstract Plant createPlant();
}
