package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.TemporaryStage;

//잡초: 항상 잡초 단계
//주변 8칸 식물의 성장을 막음 (판정은 Resident·PlantLifecycle에서 blocksGrowth()로 확인)
//씨를 뿌리지 않음 (seedTargets 기본 구현 사용)
public class Weed extends Plant {

    @Override
    public PlantState initialState() {
        return TemporaryStage.WEED;
    }

    @Override
    public PlantState nextState(PlantState current) {
        return TemporaryStage.WEED;
    }

    @Override
    public boolean blocksGrowth() {
        return true;
    }

    //임시 이모지 (팀에서 확정 시 교체)
    @Override
    public String getImage(PlantState state) {
        return "🌿";
    }
}
