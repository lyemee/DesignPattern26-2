package com.holub.life.plant;

//씨앗: 성장 가능한 step이면 새싹으로, 외로워도 죽지 않음
final class SeedState extends GrowthState {
    @Override public boolean survivesLoneliness() { return true; }
    @Override public String toString() { return "SEED"; }
}
