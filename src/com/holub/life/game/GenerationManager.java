package com.holub.life.game;

import com.holub.life.Cell;

// 다음 세대 계산과 계산 결과 반영 순서 관리
public class GenerationManager
{
    public boolean nextGeneration(Cell board)
    {
        // 1. 현재 세대를 기준으로 다음 상태를 계산
        boolean needsTransition = board.figureNextState(
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY
        );

        if (!needsTransition)
        {
            return false;
        }

        // 계산이 끝난 다음 상태를 반영 -> 화면 갱신에 전달
        return board.transition();
    }
}
