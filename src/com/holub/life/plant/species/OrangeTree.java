package com.holub.life.plant.species;

//오렌지나무: 과수 경로는 FruitTree가 정의, 열매 그림만 다름
public class OrangeTree extends FruitTree {

    @Override
    protected String fruitImage() {
        return "orange.png";
    }
}
