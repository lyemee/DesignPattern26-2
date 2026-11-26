package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.TemporaryStage;

//과수 공통 경로: 씨앗 -> 새싹 -> 나무 -> 열매나무 -> 나무 -> 열매나무 ... (반복)
//시든 나무 -> 나무 (시들지·회복할지·사망할지 판정은 PlantLifecycle 담당, 여기서는 회복 후 단계만 정의)
//열매나무 -> 나무 전환 시점이 자동 수확 시점 (수확 카운트는 수확 담당)
//씨를 뿌리지 않음 (seedTargets 기본 구현 사용)
//새 과수는 이 클래스를 상속하고 열매 이모지만 정하면 됨
public abstract class FruitTree extends Plant {

    @Override
    public final PlantState initialState() {
        return TemporaryStage.SEED;
    }

    @Override
    public final PlantState nextState(PlantState current) {
        if (current == TemporaryStage.SEED)     return TemporaryStage.SEEDLING;
        if (current == TemporaryStage.SEEDLING) return TemporaryStage.TREE;
        if (current == TemporaryStage.TREE)     return TemporaryStage.FRUIT;
        if (current == TemporaryStage.FRUIT)    return TemporaryStage.TREE;  // 수확 후 다시 나무
        if (current == TemporaryStage.WITHERED) return TemporaryStage.TREE;  // 회복 시 일반 나무 (열매나무였어도)
        return current;                                                        // 모르는 단계는 유지
    }

    //임시 이모지 (팀에서 확정 시 교체) - 열매 단계만 종류별로 다름
    @Override
    public final String getImage(PlantState state) {
        if (state == TemporaryStage.SEED)     return "🟤";
        if (state == TemporaryStage.SEEDLING) return "🌱";
        if (state == TemporaryStage.TREE)     return "🌳";
        if (state == TemporaryStage.FRUIT)    return fruitImage();
        if (state == TemporaryStage.WITHERED) return "🍂";
        return "?";
    }

    //열매나무 단계 이모지 (종류별)
    protected abstract String fruitImage();
}
