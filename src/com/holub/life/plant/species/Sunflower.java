package com.holub.life.plant.species;

import com.holub.life.plant.SeedField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//해바라기: 꽃 경로는 FlowerPlant가 정의
//꽃이 된 다음 step에 상하좌우 빈 칸에 씨를 뿌리고 소멸
public class Sunflower extends FlowerPlant {

    //상하좌우 방향 (x, y)
    private static final int[][] DIRECTIONS = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };

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

    @Override
    protected String flowerImage() {
        return "🌻";
    }
}
