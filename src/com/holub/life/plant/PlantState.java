package com.holub.life.plant;


//식물의 State -> 성장 단계별 행동
//context = PlantData
//구현체는 개체별 값을 필드로 가지지 않음 -> 여러 식물이 같은 State 객체를 공유 가능
public interface PlantState {

    //성장 가능한 Step에 호출
    //이번 step에 성장한 결과를 next에 기록
    void onStep(PlantData next);

    //생존 조건을 못 채웠을 때 바로 죽지 않고 시드는 단계인지
    // 나무·열매나무 State만 true로 overriding -> PlantLifecycle이 종류·단계 검사 없이 판단
    default boolean canWither() { return false; }
}
