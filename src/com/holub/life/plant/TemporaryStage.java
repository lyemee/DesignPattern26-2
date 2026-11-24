package com.holub.life.plant;

//임시 성장 단계 (D1 작업용)
//State 담당자가 다이어그램의 구체 State(SeedState, SeedlingState, TreeState, FruitState, FlowerState, WeedState)를
//구현하기 전까지 구체 식물이 initialState()/nextState()에서 반환할 이름표 역할만 한다.
//구체 State가 생기면 이 enum을 그 클래스들로 교체하고 삭제한다.
public enum TemporaryStage implements PlantState {
    SEED,       // 씨앗
    SEEDLING,   // 새싹
    TREE,       // 나무
    FRUIT,      // 열매나무
    WITHERED,   // 시든 나무 (나무·열매나무만, 1턴 후 나무로 복귀하거나 사망)
    FLOWER,     // 꽃
    WEED;       // 잡초

    //임시 동작: 다음 단계를 식물 종류에게 물어 기록만 한다
    @Override
    public void onStep(PlantData next) {
        next.setState(next.getPlant().nextState(this));
    }
}
