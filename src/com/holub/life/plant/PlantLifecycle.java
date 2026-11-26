package com.holub.life.plant;

//식물 공통 생존 규칙 (모든 단계 공통). 단계별 행동은 PlantState가 담당
public class PlantLifecycle {

    //생존 판정: 이번 step 뒤에도 살아 있는지
    //ruleSatisfied: Resident의 생존 규칙(Life 규칙)으로 판정한 결과
    // 충족 -> 살아 있음
    // 불충족
    //   시들 수 있는 단계(next.getState().canWither(), 나무·열매나무) -> 시든 단계로 바꾸고 살아 있음
    //   그 외(씨앗·새싹·꽃·잡초·시든 나무) -> 사망
    //   시든 나무: 충족하면 이 메서드에서 바로 일반 나무로 회복(이웃 잡초와 무관), 불충족하면 사망 -> 시든 상태는 1 step
    //반환: 다음 세대 생존 여부
    //(기존 주석: 사망 유예 카운터 방식 -> 시든 상태 1 step으로 확정되어 바꿈)
    public boolean applySurvival(PlantData next, boolean ruleSatisfied)
    {
        PlantState state = next.getState();

        //시든 나무: 충족 -> 회복(나무 단계는 식물 종류가 정함), 불충족 -> 사망
        if (state.isWithered()) {
            if (!ruleSatisfied)
                return false;
            next.setState(next.getPlant().nextState(state));
            return true;
        }

        if (ruleSatisfied)
            return true;

        //나무·열매나무: 외로움·과밀 모두 바로 죽지 않고 시듦
        if (state.canWither()) {
            next.setState(PlantStages.WITHERED);
            return true;
        }

        return false;   //씨앗·새싹·꽃·잡초
    }

    //이번 step에 State의 onStep()을 실행할 수 있는지 (성장하면 매 step 바로 다음 단계)
    //growthBlocked : 이웃에 잡초가 있어 성장이 막히는지
    // 생존 규칙 충족, 이웃에 잡초 없음일 때만 true
    // applySurvival 전에 호출 -> 생존 판정 전 단계 기준 (시든 나무면 false -> 회복한 step에 바로 열매나무로 자라지 않음)
    public boolean canGrow(PlantData next, boolean ruleSatisfied, boolean growthBlocked){
        return ruleSatisfied && !growthBlocked && !next.getState().isWithered();
    }

}
