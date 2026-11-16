package com.holub.life.game;


//게임 모드
//Conway : 기존 life game (원본 동작 유지)
//plant : 식물 성장 확장 모드
//현재 계산은 모드를 직접 확인하지 않고 셀마다 식물 데이터의 유무로 결정하는 구조(추후 필요시 모드 직접 참조로 수정)
//모드는 GameSession에 클릭 시 셀을 추가할 때 식물을 추가할 지, 일반 셀을 추가할 지 관리하는데 사용
public enum SimulationMode {
    CONWAY,
    PLANT
}
