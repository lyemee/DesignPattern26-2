package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.TemporaryStage;

import java.awt.*;
import java.util.List;
import java.util.Random;

//꽃 공통 경로: 씨앗 -> 새싹 -> 꽃
//꽃이 된 다음 step에 씨를 뿌리고 소멸
//씨 뿌림 시점 판단과 소멸 처리는 GenerationManager·PlantLifecycle 담당 -> 여기서는 경로와 대상 좌표만 정의
//새 꽃은 이 클래스를 상속하고 꽃 그림과 씨 뿌릴 위치만 정하면 됨
public abstract class FlowerPlant extends Plant {

    @Override
    public final PlantState initialState() {
        return TemporaryStage.SEED;
    }

    @Override
    public final PlantState nextState(PlantState current) {
        if (current == TemporaryStage.SEED)     return TemporaryStage.SEEDLING;
        if (current == TemporaryStage.SEEDLING) return TemporaryStage.FLOWER;
        return current;     // 꽃은 꽃 유지 (소멸은 씨 뿌린 뒤 외부에서 처리), 모르는 단계도 유지
    }

    //단계별 그림 파일 (resources/plant/) - 꽃 단계만 종류별로 다름
    @Override
    public final String getImage(PlantState state) {
        if (state == TemporaryStage.SEED)     return PlantImages.SEED;
        if (state == TemporaryStage.SEEDLING) return PlantImages.SPROUT;
        if (state == TemporaryStage.FLOWER)   return flowerImage();
        return null;                                                // 모르는 단계는 그리지 않음
    }

    //꽃 단계 그림 (종류별)
    protected abstract String flowerImage();

    //꽃은 반드시 씨 뿌릴 위치를 정해야 함 (종류별)
    @Override
    public abstract List<Point> seedTargets(Point position, SeedField field, Random random);
}
