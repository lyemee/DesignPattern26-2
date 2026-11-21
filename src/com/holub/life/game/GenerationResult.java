package com.holub.life.game;

import com.holub.life.plant.Plant;

import java.util.List;

//한 세대 처리 결과 (GenerationManager -> GameSession)
// changed: 바뀐 칸이 있는지 (Universe의 화면 갱신 여부)
// harvested: 이번 step에 자동 수확된 과수 목록 (GameSession이 종류별 수확 개수에 더함)
public record GenerationResult(boolean changed, List<Plant> harvested)
{
    //① 계산에서 변화가 없어 ②③을 건너뛴 경우
    static final GenerationResult UNCHANGED = new GenerationResult(false, List.of());
}
