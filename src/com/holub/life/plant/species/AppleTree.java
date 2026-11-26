package com.holub.life.plant.species;

//사과나무: 과수 경로는 FruitTree가 정의, 열매 그림만 다름
public class AppleTree extends FruitTree {

    @Override
    protected String fruitImage() {
        return "apple.png";
    }
}
