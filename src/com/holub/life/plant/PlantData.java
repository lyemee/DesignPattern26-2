package com.holub.life.plant;

public class PlantData {
    private final Plant plant;  //식물 종류(변경 X)
    private PlantState state;   // 현재 성장 단계
    private int growthProgress; // 이번 단계에서 성장한 step 수
    private int deathProgress;  // 연속으로 생존 조건을 채우지 못한 step 수

    //이번 step 신호: State가 onStep()에서 표시하고 Resident가 읽음
    // copy()로 만든 next는 모두 false에서 시작 -> 지난 step의 신호가 남지 않음
    private boolean reproduce;  // 번식 (꽃)
    private boolean harvest;    // 자동 수확 (열매나무)
    private boolean remove;     // 소멸 (번식한 꽃)

    //새로 심을 때 -> 식물 종류에 따른 시작 단계에서 출발
    public PlantData(Plant plant){
        this.plant = plant;
        this.state = plant.initialState();
    }

    //copy() 메서드용 -> initialState()를 다시 호출하지 않고 값을 그대로 전달
    private PlantData(Plant plant, PlantState state, int growthProgress, int deathProgress){
        this.plant = plant;
        this.state = state;
        this.growthProgress = growthProgress;
        this.deathProgress = deathProgress;
    }

    //다음 세대 계산용 복사(Resident에서 복사해 next를 만들고 next만 수정)
    //현재 상태와 다음 세대 상태를 분리해야 현재 세대만 읽고 계산 가능
    //Plant·PlantState 구현체는 개체별 값을 필드로 가지지 않고 PlantData에 정보 보관
    public PlantData copy()
    {
        return new PlantData(plant, state, growthProgress, deathProgress);
    }

    //getter (Resident 그리기, GenerationManager 번식 조정 등) -> public
    public Plant getPlant()          { return plant; }
    public PlantState getState()     { return state; }
    public int getGrowthProgress()   { return growthProgress; }
    public int getDeathProgress()    { return deathProgress; }

    // 읽기 (Resident)
    public boolean reproduces()      { return reproduce; }
    public boolean harvested()       { return harvest; }
    public boolean removed()         { return remove; }

    //변경
    void markReproduce()                   { this.reproduce = true; }
    void markHarvest()                     { this.harvest = true; }
    void markRemove()                      { this.remove = true; }
    void setState(PlantState state)        { this.state = state; }
    void setGrowthProgress(int value)      { this.growthProgress = value; }
    void setDeathProgress(int value)       { this.deathProgress = value; }

}
