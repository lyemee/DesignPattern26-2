package com.holub.life.game;

import com.holub.life.plant.Plant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//식물 종류별 수확량 집계
// 세대 처리 결과(GenerationResult.harvested())로 받은 수확 이벤트마다 그 식물 종류의 개수를 1 늘림
// 키는 식물 클래스 (AppleTree.class 등) -> 과수가 추가되어도 이 클래스는 그대로
public class HarvestCounter
{
    private final Map<Class<? extends Plant>, Integer> counts = new LinkedHashMap<>();

    //이번 step에 수확된 식물들을 종류별로 더함
    public void add(List<? extends Plant> harvested)
    {
        for (Plant plant : harvested)
            counts.merge(plant.getClass(), 1, Integer::sum);
    }

    //한 종류의 수확 개수 (수확한 적 없으면 0)
    public int count(Class<? extends Plant> type)
    {
        return counts.getOrDefault(type, 0);
    }

    //종류별 수확 개수 (읽기 전용 복사본 -> 화면 표시, Undo 스냅샷에 사용)
    public Map<Class<? extends Plant>, Integer> counts()
    {
        return Map.copyOf(counts);
    }

    //모드 전환 시 0으로 초기화
    public void reset()
    {
        counts.clear();
    }

    //Undo: 스냅샷에 저장해 둔 개수로 되돌림
    public void restore(Map<Class<? extends Plant>, Integer> saved)
    {
        counts.clear();
        counts.putAll(saved);
    }
}
