package com.holub.life.history;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantStages;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Sunflower;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

//보드 스냅샷(Undo용 Memento) 저장·복원 테스트
class BoardSnapshotTest
{
    private static final int BLOCK = 4;
    private static final int SIZE = BLOCK * BLOCK;      //16x16 보드

    private static Cell newBoard()
    {
        return new Neighborhood(BLOCK, new Neighborhood(BLOCK, new Resident(new Random(42))));
    }

    @Test
    @DisplayName("일반 셀: 저장 후 보드를 바꿔도 복원하면 저장 시점의 살아 있는 칸으로 돌아감")
    void restoresLiveCells()
    {
        Cell board = newBoard();
        flip(board, 3, 4); flip(board, 4, 4); flip(board, 15, 0);      //블록 경계·모서리 포함
        BoardSnapshot saved = BoardSnapshot.of(board);

        board.clear();
        flip(board, 8, 8);
        saved.restoreTo(board);

        assertEquals(Set.of("3,4", "4,4", "15,0"), alive(BoardSnapshot.of(board)));
    }

    @Test
    @DisplayName("식물: 종류와 성장 단계까지 복원")
    void restoresPlants()
    {
        Cell board = newBoard();
        PlantData apple = new PlantData(new AppleTree());
        apple.getState().onStep(apple);                                  //씨앗 -> 새싹
        board.apply(2, 3, c -> c.plant(apple));
        board.apply(9, 12, c -> c.plant(new PlantData(new Sunflower())));
        BoardSnapshot saved = BoardSnapshot.of(board);

        board.apply(2, 3, c -> c.remove());                              //저장 뒤 식물 제거
        saved.restoreTo(board);

        BoardSnapshot restored = BoardSnapshot.of(board);
        assertEquals(Set.of("2,3", "9,12"), alive(restored));

        PlantData restoredApple = restored.plantAt(new Point(3, 2));     //Point(x=열, y=행)
        assertTrue(restoredApple.getPlant() instanceof AppleTree);
        assertEquals(PlantStages.SEEDLING, restoredApple.getState());
        assertTrue(restored.plantAt(new Point(12, 9)).getPlant() instanceof Sunflower);
        assertNull(restored.plantAt(new Point(0, 0)));                   //식물 없던 칸
    }

    @Test
    @DisplayName("스냅샷은 식물 데이터를 복사해서 보관하고 꺼낼 때도 복사본을 줌")
    void keepsCopies()
    {
        Cell board = newBoard();
        PlantData apple = new PlantData(new AppleTree());
        board.apply(0, 0, c -> c.plant(apple));
        BoardSnapshot saved = BoardSnapshot.of(board);

        apple.getState().onStep(apple);                                  //저장 뒤 원본이 자라도
        assertEquals(PlantStages.SEED, saved.plantAt(new Point(0, 0)).getState());   //스냅샷은 그대로
        assertNotSame(saved.plantAt(new Point(0, 0)), saved.plantAt(new Point(0, 0)));
    }

    @Test
    @DisplayName("살아 있는 칸이 없으면 빈 스냅샷")
    void emptyBoard()
    {
        Cell board = newBoard();
        assertTrue(BoardSnapshot.of(board).isEmpty());
        flip(board, 1, 1);
        assertFalse(BoardSnapshot.of(board).isEmpty());
    }

    private static void flip(Cell board, int row, int column)
    {
        board.apply(row, column, c -> c.flipAlive());
    }

    //살아 있는 칸을 "행,열" 문자열 집합으로
    private static Set<String> alive(BoardSnapshot snapshot)
    {
        Set<String> result = new TreeSet<>();
        for (int row = 0; row < SIZE; row++)
            for (int column = 0; column < SIZE; column++)
                if (snapshot.isAlive(new Point(column, row)))
                    result.add(row + "," + column);
        return result;
    }
}
