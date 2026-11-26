package com.holub.life.plant;

//꽃: 성장 가능한 step(생존 조건 충족, 이웃에 잡초 없음)이면 씨를 뿌리고 소멸
// 씨 뿌릴 자리는 GenerationManager ②가 Plant.seedTargets()로 정함
// 잡초가 옆에 있으면 onStep이 불리지 않음 -> 꽃 유지 / 생존 조건을 못 채우면 씨 없이 사망
final class FlowerState implements PlantState {

    @Override
    public void onStep(PlantData next) {
        next.markReproduce();
        next.markRemove();
    }

    @Override public String toString() { return "FLOWER"; }
}
