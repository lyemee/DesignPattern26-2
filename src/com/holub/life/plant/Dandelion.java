package com.holub.life.plant;

import java.awt.*;
import java.util.ArrayList;
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
    static final int MAX_ATTEMPTS = 100;    // 빈 위치 탐색 시도 상한 (무한 반복 방지)

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

    //거리 MIN_DISTANCE 이상, 3칸이 모두 빈 위치를 무작위로 최대 SEED_COUNT곳 선택
    //자리가 부족하면 찾은 만큼만 반환 (부분 번식, 임시 정책)
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        List<Point> targets = new ArrayList<>();
        int width = field.widthInCells();
        int found = 0;

        for (int attempt = 0; attempt < MAX_ATTEMPTS && found < SEED_COUNT; attempt++) {
            int x = random.nextInt(width);
            int y = random.nextInt(width);

            if (Math.abs(x - position.x) + Math.abs(y - position.y) < MIN_DISTANCE)
                continue;

            List<Point> blinker = new ArrayList<>();
            for (int i = 0; i < SEED_LENGTH; i++)
                blinker.add(new Point(x + i, y));

            if (canSow(blinker, field, targets)) {
                targets.addAll(blinker);
                found++;
            }
        }
        return targets;
    }

    //씨앗 3칸이 모두 비어 있고, 이미 고른 칸과 겹치지 않는지
    private boolean canSow(List<Point> blinker, SeedField field, List<Point> chosen) {
        for (Point p : blinker)
            if (!field.isEmpty(p) || chosen.contains(p))
                return false;
        return true;
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
