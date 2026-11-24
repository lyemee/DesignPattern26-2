package com.holub.life.plant.species;

import com.holub.life.plant.SeedField;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

//민들레: 꽃 경로는 FlowerPlant가 정의
//꽃이 된 다음 step에 보드의 무작위 위치 3곳에 씨앗(가로 3칸 blinker)을 뿌리고 소멸
public class Dandelion extends FlowerPlant {

    static final int SEED_COUNT = 3;        // 씨앗 개수
    static final int SEED_LENGTH = 3;       // 씨앗 1개 = 가로로 연속된 3칸 (blinker)

    //씨앗마다 무작위 위치를 골라, 가로 3칸 중 빈 칸만 씨앗 자리로 반환
    // - 위치: 거리 조건 없이 보드 전체에서 무작위 (3칸이 보드 안에 들어오는 위치만)
    // - 3칸 중 빈 칸이 하나도 없으면 빈 칸이 나올 때까지 다시 고름
    // - 일부 칸만 비어 있으면 빈 칸만 채움 (부분 생성)
    // - 씨앗끼리 겹쳐도 됨, 겹친 칸은 한 번만 반환
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        Set<Point> targets = new LinkedHashSet<>();
        for (int seed = 0; seed < SEED_COUNT; seed++)
            targets.addAll(pickSeed(field, random));
        return new ArrayList<>(targets);
    }

    //빈 칸이 하나 이상 있는 위치가 나올 때까지 다시 골라 그 위치의 빈 칸 반환
    //보드에 빈 칸이 거의 없으면 끝나지 않을 수 있으므로, 보드 칸 수만큼 고른 뒤에는 이 씨앗을 포기 (안전장치)
    private List<Point> pickSeed(SeedField field, Random random) {
        int width = field.widthInCells();
        if (width < SEED_LENGTH)
            return List.of();

        int maxTries = width * width;
        for (int tries = 0; tries < maxTries; tries++) {
            int x = random.nextInt(width - SEED_LENGTH + 1);   // 3칸이 보드 안에 들어오는 시작 위치
            int y = random.nextInt(width);

            List<Point> empty = new ArrayList<>();
            for (int i = 0; i < SEED_LENGTH; i++) {
                Point cell = new Point(x + i, y);
                if (field.isEmpty(cell))
                    empty.add(cell);
            }
            if (!empty.isEmpty())
                return empty;
        }
        return List.of();
    }

    @Override
    protected String flowerImage() {
        return "🌼";
    }
}
