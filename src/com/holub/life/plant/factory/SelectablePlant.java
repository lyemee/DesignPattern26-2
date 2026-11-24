package com.holub.life.plant.factory;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.TemporaryStage;
import com.holub.life.plant.species.FruitTree;

//사용자가 식물 선택 메뉴에서 고를 수 있는 식물 목록
// - 잡초는 시스템만 생성하므로 목록에 없음
// - 항목마다 선택 가능 여부를 둬서, 아직 공개하지 않을 식물은 버튼만 보이고 비활성으로 둘 수 있음
//화면(Universe)은 이 목록만 보고 메뉴를 만들고, 고른 항목의 팩토리를 GameSession에 넘김
// -> 식물이 추가되어도 화면 코드는 그대로 (항목만 추가)
public enum SelectablePlant {
    APPLE_TREE ("Apple Tree",  new AppleTreeFactory(),  true),
    ORANGE_TREE("Orange Tree", new OrangeTreeFactory(), true),
    SUNFLOWER  ("Sunflower",   new SunflowerFactory(),  true),
    DANDELION  ("Dandelion",   new DandelionFactory(),  true);

    private final String label;             // 메뉴에 보이는 이름
    private final PlantFactory factory;     // 고르면 GameSession에 넘길 팩토리
    private final boolean enabled;          // 지금 선택 가능한지

    SelectablePlant(String label, PlantFactory factory, boolean enabled) {
        this.label = label;
        this.factory = factory;
        this.enabled = enabled;
    }

    public String label()           { return label; }
    public PlantFactory factory()   { return factory; }
    public boolean isEnabled()      { return enabled; }

    //수확 대상(과수)인지 -> 수확량 표시 대상
    public boolean isHarvestable()  { return factory.createPlant() instanceof FruitTree; }

    //수확량 표시에 쓸 열매 그림 (과수가 아니면 null)
    public String fruitImage()
    {
        Plant plant = factory.createPlant();
        return (plant instanceof FruitTree) ? plant.getImage(TemporaryStage.FRUIT) : null;
    }

    //수확 개수를 셀 때 쓰는 식물 종류 (GenerationResult.harvested()의 Plant 클래스와 같음)
    public Class<? extends Plant> plantType() { return factory.createPlant().getClass(); }
}
