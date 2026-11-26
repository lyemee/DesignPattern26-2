package com.holub.life.plant;

//새싹: 성장 가능한 step이면 나무(과수) 또는 꽃으로
final class SeedlingState extends GrowthState {
    @Override public String toString() { return "SEEDLING"; }
}
