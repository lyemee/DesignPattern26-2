package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.plant.factory.AppleTreeFactory;
import com.holub.life.plant.species.AppleTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

//GameSession의 Undo와 수확량 집계 테스트
class GameSessionUndoTest
{
    private static final int BLOCK = 4;                 //4x4 블록 안에 4x4 셀 = 16x16 보드
    private static final int SIZE = BLOCK * BLOCK;
    private static final int CELL_PX = 10;
    private static final Rectangle BOUNDS = new Rectangle(0, 0, SIZE * CELL_PX, SIZE * CELL_PX);
    private static final long SEED = 42;

    private GameSession session;

    @BeforeEach
    void setUp()
    {
        session = newSession(new GenerationManager());
    }

    //--- 세대 진행 ---

    @Test
    @DisplayName("세대 진행을 Undo하면 직전 세대로 돌아감")
    void undoGeneration() throws IOException
    {
        click(4, 3); click(4, 4); click(4, 5);                    //블록 경계에 걸친 가로 blinker
        Set<String> horizontal = alive();

        session.nextGeneration();
        assertNotEquals(horizontal, alive());

        assertTrue(session.undo());
        assertEquals(horizontal, alive());
    }

    @Test
    @DisplayName("Undo 뒤 다시 진행하면 Undo 없이 진행한 것과 같은 결과 (블록 경계를 넘는 glider)")
    void continueAfterUndo() throws IOException
    {
        //Undo 없이 8세대
        placeGlider(session);
        for (int i = 0; i < 8; i++)
            session.nextGeneration();
        Set<String> expected = alive();

        //8세대 -> 3번 Undo -> 다시 3세대
        GameSession other = newSession(new GenerationManager());
        placeGlider(other);
        for (int i = 0; i < 8; i++)
            other.nextGeneration();
        for (int i = 0; i < 3; i++)
            assertTrue(other.undo());
        for (int i = 0; i < 3; i++)
            other.nextGeneration();

        assertEquals(expected, alive(other));
    }

    @Test
    @DisplayName("변화 없는 Tick은 이력에 남지 않음 (안정 상태에서 Go)")
    void stableTickNotRecorded() throws IOException
    {
        click(1, 1); click(1, 2); click(2, 1); click(2, 2);       //2x2 블록: 바뀌지 않는 모양
        Set<String> block = alive();
        for (int i = 0; i < 5; i++)
            session.nextGeneration();
        assertEquals(block, alive());

        //Undo 한 번이면 마지막 클릭이 되돌아감 (변화 없는 Tick 5번은 이력에 없음)
        session.undo();
        assertEquals(Set.of(cell(1, 1), cell(1, 2), cell(2, 1)), alive());
    }

    //--- 클릭 ---

    @Test
    @DisplayName("일반 모드 클릭을 하나씩 Undo")
    void undoConwayClicks() throws IOException
    {
        click(1, 1);
        click(2, 2);

        session.undo();
        assertEquals(Set.of(cell(1, 1)), alive());
        session.undo();
        assertTrue(alive().isEmpty());
        assertFalse(session.canUndo());
        assertFalse(session.undo());                              //이력이 없으면 아무 일도 없음
    }

    @Test
    @DisplayName("식물 모드: 심기·제거를 Undo하면 식물이 되돌아오고, 아무것도 안 바뀐 클릭은 이력에 없음")
    void undoPlantClicks() throws IOException
    {
        session.changeMode(SimulationMode.PLANT);
        click(3, 3);                                              //식물을 고르지 않아 아무 일도 없음
        assertFalse(session.canUndo());

        session.selectPlant(new AppleTreeFactory());
        click(3, 3);                                              //심기
        click(3, 3);                                              //제거
        assertTrue(alive().isEmpty());

        session.undo();                                           //제거 취소
        assertEquals(Set.of(cell(3, 3)), alive());
        session.undo();                                           //심기 취소
        assertTrue(alive().isEmpty());
    }

    //--- Clear·Load ---

    @Test
    @DisplayName("Clear 뒤 Undo하면 지운 칸이 돌아오고, 빈 보드의 Clear는 이력에 없음")
    void undoClear() throws IOException
    {
        session.clear();
        assertFalse(session.canUndo());

        click(5, 5); click(6, 7);
        Set<String> before = alive();
        session.clear();
        assertTrue(alive().isEmpty());

        session.undo();
        assertEquals(before, alive());
    }

    @Test
    @DisplayName("Load 뒤 Undo하면 불러오기 전 보드로 돌아감")
    void undoLoad() throws IOException
    {
        GameSession source = newSession(new GenerationManager());
        source.userClicked(centerOf(9, 9), BOUNDS);
        byte[] saved = store(source);

        click(1, 1);
        session.load(new ByteArrayInputStream(saved));
        assertEquals(Set.of(cell(9, 9)), alive());

        session.undo();
        assertEquals(Set.of(cell(1, 1)), alive());
    }

    @Test
    @DisplayName("식물 모드에서 Load 뒤 Undo하면 식물 모드와 심어 둔 식물이 돌아옴")
    void undoLoadInPlantMode() throws IOException
    {
        byte[] saved = store(newSession(new GenerationManager()));    //빈 보드 파일

        session.changeMode(SimulationMode.PLANT);
        session.selectPlant(new AppleTreeFactory());
        click(2, 2);
        session.load(new ByteArrayInputStream(saved));
        assertEquals(SimulationMode.CONWAY, session.getMode());

        session.undo();
        assertEquals(SimulationMode.PLANT, session.getMode());
        assertEquals(Set.of(cell(2, 2)), alive());
    }

    //--- 수확량 ---

    @Test
    @DisplayName("수확 이벤트마다 종류별 개수가 늘고, Undo하면 개수도 되돌아감")
    void harvestCountedAndUndone()
    {
        //매 세대 사과 1개를 수확했다고 알려 주는 GenerationManager (수확 판정 구현 전이라 결과만 정해 둠)
        session = newSession(new HarvestingGenerationManager());
        click(4, 3); click(4, 4); click(4, 5);                    //세대가 바뀌도록 blinker 배치

        for (int i = 0; i < 3; i++)
            session.nextGeneration();
        assertEquals(Map.of(AppleTree.class, 3), session.harvestCounts());

        session.undo();
        assertEquals(Map.of(AppleTree.class, 2), session.harvestCounts());
        session.undo();
        session.undo();
        assertTrue(session.harvestCounts().isEmpty());
    }

    @Test
    @DisplayName("모드를 바꾸면 수확량과 Undo 이력이 초기화됨")
    void changeModeResetsHarvestAndHistory()
    {
        session = newSession(new HarvestingGenerationManager());
        click(4, 3); click(4, 4); click(4, 5);
        session.nextGeneration();
        assertFalse(session.harvestCounts().isEmpty());

        session.changeMode(SimulationMode.PLANT);
        assertTrue(session.harvestCounts().isEmpty());
        assertFalse(session.canUndo());
    }

    @Test
    @DisplayName("이력은 최대 개수까지만 보관")
    void historyLimit() throws IOException
    {
        for (int i = 0; i < GameSession.MAX_UNDO + 5; i++)
            click(0, 0);                                          //같은 칸을 계속 토글

        int undone = 0;
        while (session.undo())
            undone++;
        assertEquals(GameSession.MAX_UNDO, undone);
        assertEquals(Set.of(cell(0, 0)), alive());                //가장 오래된 5번은 되돌릴 수 없음
    }

    //--- 테스트용 보조 ---

    //매 세대 사과나무 1그루를 수확한 것으로 결과를 돌려줌 (보드 계산은 원래대로)
    private static class HarvestingGenerationManager extends GenerationManager
    {
        @Override
        public GenerationResult nextGeneration(Cell board, Random random)
        {
            GenerationResult result = super.nextGeneration(board, random);
            List<com.holub.life.plant.Plant> harvested = new ArrayList<>();
            harvested.add(new AppleTree());
            return new GenerationResult(result.changed(), harvested);
        }
    }

    private static GameSession newSession(GenerationManager manager)
    {
        Random random = new Random(SEED);
        return new GameSession(newBoard(random), random, manager);
    }

    private static Cell newBoard(Random random)
    {
        return new Neighborhood(BLOCK, new Neighborhood(BLOCK, new Resident(random)));
    }

    //오른쪽 아래로 움직이는 glider: 몇 세대 뒤 (3|4) 블록 경계를 넘음
    private static void placeGlider(GameSession target)
    {
        int[][] cells = { {0, 2}, {1, 3}, {2, 1}, {2, 2}, {2, 3} };
        for (int[] c : cells)
            target.userClicked(centerOf(c[0], c[1]), BOUNDS);
    }

    private void click(int row, int column)
    {
        session.userClicked(centerOf(row, column), BOUNDS);
    }

    private static Point centerOf(int row, int column)
    {
        return new Point(column * CELL_PX + CELL_PX / 2, row * CELL_PX + CELL_PX / 2);
    }

    private static byte[] store(GameSession target) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        target.store(out);
        return out.toByteArray();
    }

    private Set<String> alive() throws IOException
    {
        return alive(session);
    }

    //살아 있는 칸을 "행,열" 문자열 집합으로 (기존 .life 저장 경로로 읽음)
    private static Set<String> alive(GameSession target) throws IOException
    {
        Cell.Memento memento = (Cell.Memento) newBoard(new Random(SEED)).createMemento();
        memento.load(new ByteArrayInputStream(store(target)));

        Set<String> result = new TreeSet<>();
        for (int row = 0; row < SIZE; row++)
            for (int column = 0; column < SIZE; column++)
                if (memento.isAlive(new Point(column, row)))
                    result.add(cell(row, column));
        return result;
    }

    private static String cell(int row, int column)
    {
        return row + "," + column;
    }
}
