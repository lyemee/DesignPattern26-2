package com.holub.life.plant;

//잡초: 단계 변화 없음 (주변 성장을 막는 것은 Plant.blocksGrowth(), 생존은 일반 규칙)
final class WeedState implements PlantState {

    @Override
    public void onStep(PlantData next) { }

    @Override public String toString() { return "WEED"; }
}
