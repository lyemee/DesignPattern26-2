package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.TemporaryStage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//해바라기: 씨앗 -> 새싹 -> 꽃
//꽃이 된 다음 step에 상하좌우 빈 칸에 씨를 뿌리고 소멸
//씨 뿌림 시점 판단과 소멸 처리는 GenerationManager·PlantLifecycle 담당 -> 여기서는 경로와 대상 좌표만 정의
public class Sunflower extends Plant {

    //상하좌우 방향 (x, y)
    private static final int[][] DIRECTIONS = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };

    @Override
    public PlantState initialState() {
        return TemporaryStage.SEED;
    }

    @Override
    public PlantState nextState(PlantState current) {
        if (current == TemporaryStage.SEED)     return TemporaryStage.SEEDLING;
        if (current == TemporaryStage.SEEDLING) return TemporaryStage.FLOWER;
        return current;     // 꽃은 꽃 유지 (소멸은 씨 뿌린 뒤 외부에서 처리), 모르는 단계도 유지
    }

    //상하좌우 4칸 중 빈 칸만 후보로 반환 (씨앗 1개 = 1칸)
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        List<Point> targets = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            Point target = new Point(position.x + d[0], position.y + d[1]);
            if (field.isEmpty(target))
                targets.add(target);
        }
        return targets;
    }

    //임시 이모지 (팀에서 확정 시 교체)
    @Override
    public String getImage(PlantState state) {
        if (state == TemporaryStage.SEED)     return "🟤";
        if (state == TemporaryStage.SEEDLING) return "🌱";
        if (state == TemporaryStage.FLOWER)   return "🌻";
        return "?";
    }
}
