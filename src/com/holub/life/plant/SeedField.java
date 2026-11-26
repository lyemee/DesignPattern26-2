package com.holub.life.plant;

import java.awt.Point;

//꽃의 번식 위치 조회용. 한 세대의 모든 꽃은 같은 현재 보드 스냅샷을 본다.
public interface SeedField
{
    //최종 요구사항 「번식 구현 / 3. 씨 뿌리기 / 공통 규칙」의 현재 죽어 있는 칸 기준.
    //보드 안이면서 현재 비어 있는 칸인지. 이번 계산에서 죽을 식물이 있는 칸도 false.
    boolean isEmpty(Point position);

    int widthInCells();
}
