package com.holub.life.plant;

//성장 단계 State 객체 모음
// State는 개체별 값을 갖지 않으므로 모든 식물이 같은 객체를 공유 (Flyweight)
// 식물 종류는 initialState()/nextState()에서 이 상수를 돌려줌
public final class PlantStages {

    public static final PlantState SEED     = new SeedState();      // 씨앗
    public static final PlantState SEEDLING = new SeedlingState();  // 새싹
    public static final PlantState TREE     = new TreeState();      // 나무
    public static final PlantState FRUIT    = new FruitState();     // 열매나무
    public static final PlantState WITHERED = new WitheredState();  // 시든 나무
    public static final PlantState FLOWER   = new FlowerState();    // 꽃
    public static final PlantState WEED     = new WeedState();      // 잡초

    private PlantStages() { }
}
