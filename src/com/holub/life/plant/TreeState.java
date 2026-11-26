package com.holub.life.plant;

//나무: 성장 가능한 step이면 열매나무로, 생존 조건을 못 채우면 시듦
final class TreeState extends GrowthState {

    @Override
    public boolean canWither() { return true; }

    @Override public String toString() { return "TREE"; }
}
