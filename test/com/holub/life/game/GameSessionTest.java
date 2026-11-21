package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

//GameSession을 추가한 Life Game의 기존 기능 및 게임 모드 테스트
class GameSessionTest
{
    private static final int BLOCK = 4;                 //블록 한 변의 셀 수 → 4x4 블록 안에 4x4 셀 = 16x16 보드
    private static final int SIZE = BLOCK * BLOCK;
    private static final int CELL_PX = 10;              //클릭 좌표 계산용 셀 한 칸의 픽셀 크기
    private static final Rectangle BOUNDS = new Rectangle(0, 0, SIZE * CELL_PX, SIZE * CELL_PX);
    private static final long SEED = 42;                //무작위 규칙(탄생 종류, 번식, 잡초)의 결과를 고정

    private GameSession session;

    @BeforeEach
    void setUp()
    {
        session = newSession();
    }

    //Universe에서 GameSession·GenerationManager로 옮긴 실행 흐름이 원본 Life Game과 같은지 확인하는 테스트
    // 클릭이 Session을 거쳐 셀에 전달되는지
    // 계산(figureNextState) → 반영(transition) 순서가 잘 지켜지는지 (순서가 바뀌면 첫 세대에 셀이 사라져 실패)
    // 블록 경계 이웃 계산: 반복되는 셀 ㄴ패턴(blinker)를 경계에 걸쳐 배치 (Composite 구조 확인)
    @Test
    @DisplayName("블록 경계에 걸친 셀 반복 패턴이 2세대 주기로 가로·세로를 반복하는 테스트")
    void blinkerAcrossBlockBoundary() throws IOException
    {
        //blinker cell 패턴 배치
        click(4, 3); click(4, 4); click(4, 5);
        //가로 3칸: 3열|4열 경계에 걸침
        // 첫 세대 전에 alive()(저장 경로)를 부르면 클릭한 셀의 블록 경계가 활성으로 표시되어
        // 클릭 경로의 경계 표시 누락을 가릴 수 있음 → 배치한 좌표를 직접 적음
        Set<String> horizontal = Set.of(cell(4, 3), cell(4, 4), cell(4, 5));

        session.nextGeneration();
        //blinker cell 패턴의 다음 세대 계산이 정상적으로 잘 되는지
        assertEquals(Set.of(cell(3, 4), cell(4, 4), cell(5, 4)), alive());

        session.nextGeneration();
        //blinker 패턴은 두 세대마다 반복
        assertEquals(horizontal, alive());
    }

    //Universe에 있던 저장·불러오기 코드를 GameSession으로 옮긴 뒤에도 기존 .life 형식으로 왕복되는지 확인하는 테스트
    @Test
    @DisplayName("저장한 보드를 새 세션에서 불러오는 테스트")
    void storeAndLoadRoundTrip() throws IOException
    {
        click(1, 2); click(2, 3); click(3, 1); click(3, 2); click(3, 3);   //glider 모양 5칸
        //저장
        byte[] saved = store(session);

        //새 게임세션
        GameSession other = newSession();
        //불러오기
        other.load(new ByteArrayInputStream(saved));

        //배치한 좌표가 그대로 불러와지는지
        assertEquals(
                Set.of(cell(1, 2), cell(2, 3),
                       cell(3, 1), cell(3, 2), cell(3, 3)),
                alive(other));
    }

    //두 모드의 셀이 한 보드에 섞이지 않도록 모드 전환 시 보드를 비우는지 확인하는 테스트
    @Test
    @DisplayName("모드를 바꿀 때 보드를 비우는지 테스트")
    void changeModeClearsBoard() throws IOException
    {
        click(1, 1); click(8, 8);                                 //일반 모드에서 Conway 셀 생성

        //모드 바꾸기
        session.changeMode(SimulationMode.PLANT);

        //모드가 바뀌고 보드가 비어있는지 확인
        assertEquals(SimulationMode.PLANT, session.getMode());
        assertTrue(alive().isEmpty());
        //Conway 모드의 일반 셀이 식물 모드로 넘어가지 않음
    }

    //생존 좌표만 담긴 기존 .life 파일을 식물 모드에서 불러와도 Conway 셀이 식물 모드에 섞이지 않는지 확인
    // 전체 구조 기반 마련 브랜치 -> plant 모드의 load를 아직 구현 X, 일반모드로만 열리도록 설정 (추후 수정할 테스트)
    @Test
    @DisplayName("식물 모드에서 불러오면 일반 모드로 바뀌고 불러온 일반 셀들이 보이는지 테스트")
    void loadSwitchesToConwayMode() throws IOException
    {
        GameSession source = newSession();                       //불러올 파일 내용 준비
        source.userClicked(centerOf(6, 6), BOUNDS);
        byte[] saved = store(source);

        session.changeMode(SimulationMode.PLANT);
        session.load(new ByteArrayInputStream(saved));

        assertEquals(SimulationMode.CONWAY, session.getMode());
        assertEquals(Set.of(cell(6, 6)), alive());
    }

    //테스트용 보조 메서드들 ---

    //Universe와 같은 방식으로 보드와 세션 생성 (보드와 세션이 같은 Random을 공유)
    private static GameSession newSession()
    {
        Random random = new Random(SEED);
        return new GameSession(newBoard(random), random);
    }

    private static Cell newBoard(Random random)
    {
        return new Neighborhood(BLOCK, new Neighborhood(BLOCK, new Resident(random)));
    }

    //(행, 열) 셀을 클릭(GUI의 클릭 테스트용) -> 셀 중앙의 픽셀 좌표로 변환해 전달
    private void click(int row, int column)
    {
        session.userClicked(centerOf(row, column), BOUNDS);
    }

    private static Point centerOf(int row, int column)
    {
        return new Point(column * CELL_PX + CELL_PX / 2, row * CELL_PX + CELL_PX / 2);
    }

    //기존 Store와 같은 경로로 보드를 바이트 배열에 저장
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

    //살아 있는 셀을 "행,열" 문자열 집합으로 -> 비교 및 출력 용이
    //  store()로 저장 → 빈 Memento 객체에 불러오기 → Cell.Memento.isAlive(Point)로 칸마다 생존 여부 확인
    private static Set<String> alive(GameSession target) throws IOException
    {
        Cell.Memento memento = (Cell.Memento) newBoard(new Random(SEED)).createMemento();   //저장 형식을 읽는 데만 쓰는 빈 보드
        memento.load(new ByteArrayInputStream(store(target)));

        Set<String> result = new TreeSet<>();
        for (int row = 0; row < SIZE; row++)
            for (int column = 0; column < SIZE; column++)
                if (memento.isAlive(new Point(column, row)))
                    result.add(row + "," + column);
        return result;
    }

    //(행, 열) 셀 하나를 alive()와 같은 행,열 문자열 형식으로 표현
    private static String cell(int row, int column)
    {
        return row + "," + column;
    }
}