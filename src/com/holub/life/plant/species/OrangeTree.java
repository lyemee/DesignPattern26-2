package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.TemporaryStage;

//오렌지나무: 씨앗 -> 새싹 -> 나무 -> 열매나무 -> 나무 -> 열매나무 ... (반복)
//열매나무 -> 나무 전환 시점이 자동 수확 시점 (수확 카운트는 GameSession.harvest() 담당)
//씨를 뿌리지 않음 (seedTargets 기본 구현 사용)
public class OrangeTree extends Plant {

    @Override
    public PlantState initialState() {
        return TemporaryStage.SEED;
    }

    @Override
    public PlantState nextState(PlantState current) {
        if (current == TemporaryStage.SEED)     return TemporaryStage.SEEDLING;
        if (current == TemporaryStage.SEEDLING) return TemporaryStage.TREE;
        if (current == TemporaryStage.TREE)     return TemporaryStage.FRUIT;
        if (current == TemporaryStage.FRUIT)    return TemporaryStage.TREE;  // 수확 후 다시 나무
        return current;                                                        // 모르는 단계는 유지
    }

    //임시 이모지 (팀에서 확정 시 교체)
    @Override
    public String getImage(PlantState state) {
        if (state == TemporaryStage.SEED)     return "🟤";
        if (state == TemporaryStage.SEEDLING) return "🌱";
        if (state == TemporaryStage.TREE)     return "🌳";
        if (state == TemporaryStage.FRUIT)    return "🍊";
        return "?";
    }
}
