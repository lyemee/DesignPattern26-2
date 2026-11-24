package com.holub.life.plant;

import java.awt.*;

//씨 뿌리기 대상 칸 조회용 (구현은 세대 처리 ② 단계 담당)
//구체 식물은 보드 구조를 모른 채 이 인터페이스로만 빈 칸을 확인한다
public interface SeedField {

    //보드 안이면서 현재 죽어 있는(씨를 뿌릴 수 있는) 칸인지
    boolean isEmpty(Point position);

    //보드 한 변의 셀 개수 (민들레의 무작위 위치 계산용)
    int widthInCells();
}
