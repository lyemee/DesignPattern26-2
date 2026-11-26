package com.holub.life.plant;

//성장 단계 공통: onStep()이 불리면 식물 종류가 정한 다음 단계로 넘어감
// 다음 단계가 무엇인지(새싹 다음이 나무인지 꽃인지)는 식물 종류(Plant.nextState)가 결정
// 씨앗·새싹·나무가 상속, 열매나무는 수확 표시를 더함
abstract class GrowthState implements PlantState {

    @Override
    public void onStep(PlantData next) {
        next.setState(next.getPlant().nextState(this));
    }
}
