package com.holub.life.plant;

public class PlantLifecycle {

    //생존 판정 (이번 step뒤에 살아있는지)
    // -> PlantData의 deathProgress(식물 쇠약 step 데이터) 변경
    //ruleSatisfied: Resident의 생존 규칙으로 판정한 결과
    //충족 - > deathProgress = 0, 불충족 -> +1, 유예 step 초과 -> 사망
    //반환: 다음 세대 생존 여부(사망이 아니면 true)
    public boolean applySurvival(PlantData next, boolean ruleSatisfied)
    {
        // 구현 필요(일단 예외처리)
        throw new UnsupportedOperationException("PlantLifecycle 구현 필요");
    }

    //성장 step을 증가시킬 수 있는 상황인지 판단
    //growthBlocked : 근처에 잡초로 인해 성장이 막히는지
    public boolean canGrow(PlantData next, boolean ruleSatisfied, boolean growthBlocked){
        // 구현 필요(일단 예외 처리)
        throw new UnsupportedOperationException("PlantLifecycle 구현 필요");
    }

}
