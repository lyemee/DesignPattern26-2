package com.holub.life;

//셀 하나에서 할 일 (Command)
// GameSession(클릭), GenerationManager(씨 뿌리기, 잡초 생성)가 상황에 맞게 생성
// Cell.apply()의 재귀에서 사용 -> 어떤 작업인지에 관계없이 목적지 Resident까지 전달
// Resident -> flipAlive, plant, remove, sow로 실제 상태 변경
public interface CellAction
{
    void execute(Resident cell);
}
