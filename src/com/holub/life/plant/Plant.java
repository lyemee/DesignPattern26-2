package com.holub.life.plant;


import java.awt.*;
import java.util.Collections;
import java.util.List;

//식물 종류의 공통 상위 클래스
//구체 식물마다 달라지는 것: 시작 단계, 다음 단계, 행동
//구현체는 개체별 값을 필드로 가지지 않는다 -> 개체별 상태는 PlantData에 보관 (PlantData.copy() 참고)
public abstract class Plant {

    //심을 때의 첫 단계 결정
    public abstract PlantState initialState();

    //현재 단계 후 다음 담계 계산
    public abstract PlantState nextState(PlantState current);

    // 번식 후보 좌표: 기본은 없음, Sunflower·Dandelion만 재정의
    // 후보만 돌려주고, 실제 배치와 충돌 조정은 GenerationManager에서 작업
    public List<Point> seedTargets(Point position)
    {
        return Collections.emptyList();
    }

    // 이웃 성장 억제: Weed만 true
    public boolean blocksGrowth()
    {
        return false;
    }
}
