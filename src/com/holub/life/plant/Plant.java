package com.holub.life.plant;


import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;

//식물 종류의 공통 상위 클래스
//구체 식물마다 달라지는 것: 시작 단계, 다음 단계, 화면 표시, 행동
//구현체는 개체별 값을 필드로 가지지 않는다 -> 개체별 상태는 PlantData에 보관 (PlantData.copy() 참고)
public abstract class Plant {

    //심을 때의 첫 단계 결정
    public abstract PlantState initialState();

    //현재 단계 후 다음 단계 계산
    public abstract PlantState nextState(PlantState current);

    //단계별 화면 표시: resources/plant/ 아래 이모지 그림 파일 이름 (예: "apple.png")
    public abstract String getImage(PlantState state);

    // 번식 후보 좌표: 기본은 없음, Sunflower·Dandelion만 재정의
    // 후보만 돌려주고, 실제 배치와 충돌 조정은 GenerationManager에서 작업
    //기존 단일 인자 계약을 유지하고 아래의 새 조회 계약에서 기본 동작으로 사용한다.
    public List<Point> seedTargets(Point position)
    {
        return Collections.emptyList();
    }

    //PR #3의 꽃 구현과 같은 계약. 기존 단일 인자 구현도 계속 사용할 수 있도록 위임한다.
    //field는 현재 빈 칸 스냅샷이며, 반환된 좌표의 실제 배치·충돌 처리는 GenerationManager 책임.
    //해바라기·민들레 모두 현재 상태만 조회. 이번 step에 죽을 칸을 빈 칸으로 추가하지 않는다.
    public List<Point> seedTargets(Point position, SeedField field, Random random)
    {
        return seedTargets(position);
    }

    // 이웃 성장 억제: Weed만 true
    public boolean blocksGrowth()
    {
        return false;
    }
}
