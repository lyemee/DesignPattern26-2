package com.holub.life.plant;

import java.awt.Point;

//해바라기·민들레를 포함한 모든 씨 뿌리기에 공통으로 사용하는 현재 빈 칸 조회 계약.
//한 세대의 모든 꽃은 같은 현재 보드 스냅샷을 보며, 다음 상태로 빈 칸을 재판정하지 않는다.
public interface SeedField
{
    //최종 요구사항 「번식 구현 / 3. 씨 뿌리기 / 공통 규칙」의 현재 죽어 있는 칸 기준.
    //보드 안이면서 현재 비어 있는 칸인지. 이번 계산에서 죽을 식물이 있는 칸도 false.
    boolean isEmpty(Point position);

    //보드 한 변의 셀 개수 (민들레의 무작위 위치 계산용)
    int widthInCells();
}
