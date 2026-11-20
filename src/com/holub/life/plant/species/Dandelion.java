package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.TemporaryStage;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

//민들레: 씨앗 -> 새싹 -> 꽃
//꽃이 된 다음 step에 멀리 떨어진 빈 위치 3곳에 씨앗(가로 3칸 blinker)을 뿌리고 소멸
//씨 뿌림 시점 판단과 소멸 처리는 GenerationManager·PlantLifecycle 담당 -> 여기서는 경로와 대상 좌표만 정의
public class Dandelion extends Plant {

    //아래 값들은 요구사항 미정 -> 임시값 (확정 시 수정)
    static final int MIN_DISTANCE = 15;     // 최소 거리 (맨해튼 거리 기준)
    static final int SEED_COUNT = 3;        // 씨앗 개수
    static final int SEED_LENGTH = 3;       // 씨앗 1개 = 가로로 연속된 3칸 (blinker)

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

    //세 칸 모두 거리 MIN_DISTANCE 이상이고 비어 있는 자리를 먼저 전부 모은 뒤, 무작위 순서로 최대 SEED_COUNT곳 선택
    //후보를 먼저 모으므로 실제로 자리가 부족할 때만 찾은 만큼 반환 (부분 번식, 임시 정책)
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        List<List<Point>> candidates = new ArrayList<>();
        int width = field.widthInCells();
        for (int y = 0; y < width; y++)
            for (int x = 0; x < width; x++) {
                List<Point> blinker = blinkerAt(x, y);
                if (canSow(blinker, position, field))
                    candidates.add(blinker);
            }

        Collections.shuffle(candidates, random);

        List<Point> targets = new ArrayList<>();
        int found = 0;
        for (List<Point> blinker : candidates) {
            if (found == SEED_COUNT)
                break;
            if (Collections.disjoint(blinker, targets)) {   // 이미 고른 씨앗과 겹치지 않을 때만
                targets.addAll(blinker);
                found++;
            }
        }
        return targets;
    }

    //(x, y)에서 시작하는 가로 3칸
    private List<Point> blinkerAt(int x, int y) {
        List<Point> blinker = new ArrayList<>();
        for (int i = 0; i < SEED_LENGTH; i++)
            blinker.add(new Point(x + i, y));
        return blinker;
    }

    //씨앗의 모든 칸이 비어 있고, 모든 칸이 부모와 최소 거리 이상인지
    private boolean canSow(List<Point> blinker, Point position, SeedField field) {
        for (Point p : blinker)
            if (!field.isEmpty(p) || distance(p, position) < MIN_DISTANCE)
                return false;
        return true;
    }

    //맨해튼 거리 (거리 기준 미정 -> 임시)
    static int distance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    //임시 이모지 (팀에서 확정 시 교체)
    @Override
    public String getImage(PlantState state) {
        if (state == TemporaryStage.SEED)     return "🟤";
        if (state == TemporaryStage.SEEDLING) return "🌱";
        if (state == TemporaryStage.FLOWER)   return "🌼";
        return "?";
    }
}
