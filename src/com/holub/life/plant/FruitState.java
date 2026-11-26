package com.holub.life.plant;

//열매나무: 성장 가능한 step이면 자동 수확하고 나무로 돌아감, 생존 조건을 못 채우면 시듦(수확 없음)
final class FruitState extends GrowthState {

    @Override
    public void onStep(PlantData next) {
        next.markHarvest();         //수확 신호 -> Resident가 collect로 알림
        super.onStep(next);         //다음 단계(나무)는 식물 종류가 정함
    }

    @Override
    public boolean canWither() { return true; }

    @Override public String toString() { return "FRUIT"; }
}
