package com.holub.life.plant.species;

import com.holub.life.plant.SeedField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//민들레: 꽃 경로는 FlowerPlant가 정의
//꽃이 된 다음 step에 보드의 무작위 위치 1곳에 씨앗 3개를 ㄱ자 모양으로 뿌리고 소멸
public class Dandelion extends FlowerPlant {

    //씨앗 3개의 ㄱ자 모양: 고른 위치(왼쪽 위) 기준 {열, 행} 오프셋
    //  ■ ■
    //    ■
    static final int[][] SEED_SHAPE = { {0, 0}, {1, 0}, {1, 1} };
    static final int SEED_SIZE = 2;         // 모양이 차지하는 가로·세로 칸 수

    //무작위 위치 1곳을 골라, ㄱ자 3칸 중 빈 칸만 씨앗 자리로 반환
    // - 위치: 거리 조건 없이 보드 전체에서 무작위 (3칸이 보드 안에 들어오는 위치만)
    // - 3칸 중 빈 칸이 하나도 없으면 빈 칸이 나올 때까지 다시 고름
    // - 일부 칸만 비어 있으면 빈 칸만 채움 (부분 생성)
    //보드에 빈 칸이 거의 없으면 끝나지 않을 수 있으므로, 보드 칸 수만큼 고른 뒤에는 포기 (안전장치)
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        int width = field.widthInCells();
        if (width < SEED_SIZE)
            return List.of();

        int maxTries = width * width;
        for (int tries = 0; tries < maxTries; tries++) {
            int x = random.nextInt(width - SEED_SIZE + 1);     // 3칸이 보드 안에 들어오는 시작 위치
            int y = random.nextInt(width - SEED_SIZE + 1);

            List<Point> empty = new ArrayList<>();
            for (int[] offset : SEED_SHAPE) {
                Point cell = new Point(x + offset[0], y + offset[1]);
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
        return "dandelion.png";
    }
}
