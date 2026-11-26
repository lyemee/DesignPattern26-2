package com.holub.life.plant;

//시든 나무: 성장하지 않음
// 회복(-> 나무)과 사망은 PlantLifecycle.applySurvival()이 생존 조건으로 판정 (회복은 성장이 아니라 생존)
// canGrow()가 시든 나무에는 false -> 이 onStep()은 불리지 않음
final class WitheredState implements PlantState {

    @Override
    public void onStep(PlantData next) { }

    @Override
    public boolean isWithered() { return true; }

    @Override public String toString() { return "WITHERED"; }
}
