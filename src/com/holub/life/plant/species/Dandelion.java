package com.holub.life.plant.species;

import com.holub.life.plant.SeedField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//민들레: 꽃 경로는 FlowerPlant가 정의
//꽃이 된 다음 step에 보드 전체의 빈 칸 중 무작위 1칸에 씨앗 1개를 뿌리고 소멸
public class Dandelion extends FlowerPlant {

    //보드의 빈 칸 중 하나를 균등하게 골라 반환
    // - 위치: 거리 조건 없이 보드 전체에서 무작위
    // - 빈 칸이 없으면 빈 결과
    @Override
    public List<Point> seedTargets(Point position, SeedField field, Random random) {
        int width = field.widthInCells();
        List<Point> empty = new ArrayList<>();
        for (int y = 0; y < width; y++)
            for (int x = 0; x < width; x++) {
                Point cell = new Point(x, y);
                if (field.isEmpty(cell))
                    empty.add(cell);
            }
        if (empty.isEmpty())
            return List.of();
        return List.of(empty.get(random.nextInt(empty.size())));
    }

    @Override
    protected String flowerImage() {
        return "dandelion.png";
    }
}
