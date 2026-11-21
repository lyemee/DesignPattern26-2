package com.holub.life;

import com.holub.life.plant.Plant;

//Cell.collect()가 셀마다 이번 step의 계산 결과를 알려 주는 용도 (GenerationManager가 구현)
// 셀은 자기 사실만 알려 주고, 씨앗을 놓을 수 있는지 같은 판단은 받는 쪽이 함
// 필요한 알림만 받도록 모두 빈 기본 구현 (예: 잡초 생성은 cell()만 사용)
public interface CellReport
{
    //칸에서 필요한 정보
    // hasSurvivingPlant: 다음 세대에도 살아 있는 식물이 있음
    // bornPlant: 이번에 새로 태어나는 식물 종류, 없으면 null
    default void cell(int row, int column, boolean hasSurvivingPlant, Plant bornPlant) {}

    //이번 step에 번식하는 꽃 (번식한 꽃은 다음 세대에 사라지므로 따로 알림)
    default void seeding(int row, int column, Plant plant) {}

    //이번 step에 자동 수확된 과수
    default void harvested(Plant plant) {}
}
