package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.PlantStages;

//잡초: 항상 잡초 단계
//주변 8칸 식물의 성장을 막음 (판정은 Resident·PlantLifecycle에서 blocksGrowth()로 확인)
//씨를 뿌리지 않음 (seedTargets 기본 구현 사용)
public class Weed extends Plant {

    @Override
    public PlantState initialState() {
        return PlantStages.WEED;
    }

    @Override
    public PlantState nextState(PlantState current) {
        return PlantStages.WEED;
    }

    @Override
    public boolean blocksGrowth() {
        return true;
    }

    @Override
    public String getImage(PlantState state) {
        return "weed.png";
    }
}
