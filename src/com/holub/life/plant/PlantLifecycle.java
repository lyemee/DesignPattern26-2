package com.holub.life.plant;

public class PlantLifecycle {

    //생존 판정 - deathProgress 변경
    //ruleSatisfied: Resident의 생존 규칙으로 판정한 결과
    //충족 - > deathProgress = 0, 불충족 -> +1, 유예 step 초과 -> 사망
    //반환: 다음 세대 생존 여부(사망이 아니면 true)
    public boolean applySurvival(PlantData next, boolean ruleSatisfied)
    {
        /* 구현 필요 */
        return false;
    }

    //성장 가능 여부
    //growthBlocked : 근처에 잡초로 인해 성장이 막히는지
    public boolean canGrow(PlantData next, boolean ruleSatisfied, boolean growthBlocked){
        /* 구현 필요 */
        return false;
    }

}
