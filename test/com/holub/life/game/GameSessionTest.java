package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.CellReport;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;
import com.holub.life.history.BoardSnapshot;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.PlantStages;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.factory.AppleTreeFactory;
import com.holub.life.plant.factory.SunflowerFactory;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Sunflower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    //경로 확인용 식물: 구체 식물 종류와 상관없이 심기·수집 경로만 확인
    private static final Plant TEST_PLANT = new Plant()
    {
        public PlantState initialState()                { return next -> {}; }
        public PlantState nextState(PlantState current) { return current; }
        public String getImage(PlantState state)        { return ""; }   //Plant에 getImage가 추상 메서드로 있어도 컴파일되도록
    };
    //식물 선택 메뉴 대신 사용하는 Factory
    private static final PlantFactory TEST_FACTORY = new PlantFactory()
    {
        public Plant createPlant() { return TEST_PLANT; }
    };

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

    //식물 모드 클릭 규칙: 식물이 있으면 종류와 상관없이 제거, 빈 칸이면 선택한 식물 심기
    // 식물을 고르지 않고 누르면 아무것도 심지 않아야 함 (Conway 셀이 식물 모드에 섞이지 않음)
    // figureNextState는 사용하지 않는 테스트
    @Test
    @DisplayName("식물 모드에서 클릭으로 선택한 식물을 심고, 다시 클릭하면 제거하는지 테스트")
    void plantModeClickPlantsAndRemoves() throws IOException
    {
        session.changeMode(SimulationMode.PLANT);

        click(2, 2);                                              //아직 식물을 고르지 않음
        assertTrue(alive().isEmpty());

        session.selectPlant(TEST_FACTORY);
        click(2, 2);                                              //빈 칸 → 심기
        assertEquals(Set.of(cell(2, 2)), alive());

        click(2, 2);                                              //식물 칸 → 제거
        assertTrue(alive().isEmpty());
    }

    //셀에 쓰는 경로(apply)와 셀을 읽는 경로(collect)가 같은 (행, 열)을 쓰는지 확인
    // 블록 경계 칸(3, 4)과 보드 모서리 칸을 심어 하위 블록 좌표 계산 확인
    // 행·열을 뒤집은 좌표가 기대 집합에 없도록 비대칭으로 배치 -> 행·열을 통째로 바꾸는 오류도 잡음
    // collect가 모든 칸을 한 번씩 방문하는지도 확인 (번식·잡초 생성이 빈 칸을 빠짐없이 봐야 함)
    @Test
    @DisplayName("apply로 심은 칸을 collect가 정상적으로 알리고 모든 칸을 한 번씩 방문하는지 테스트")
    void applyAndCollectUseSameCoordinates()
    {
        Cell board = newBoard(new Random(SEED));
        int[][] targets = { {3, 4}, {4, 12}, {0, SIZE - 1}, {13, 2} };
        for (int[] target : targets)
            board.apply(target[0], target[1], c -> c.plant(new PlantData(TEST_PLANT)));

        List<String> visited = new ArrayList<>();
        Set<String> withPlant = new TreeSet<>();
        board.collect(0, 0, new CellReport()
        {
            public void cell(int row, int column, boolean hasSurvivingPlant, Plant bornPlant)
            {
                visited.add(GameSessionTest.cell(row, column));
                if (hasSurvivingPlant)
                    withPlant.add(GameSessionTest.cell(row, column));
            }
        });

        assertEquals(SIZE * SIZE, visited.size());                  //모든 칸을 한 번씩
        assertEquals(SIZE * SIZE, new HashSet<>(visited).size());   //서로 다른 좌표로
        assertEquals(Set.of(cell(3, 4), cell(4, 12), cell(0, SIZE - 1), cell(13, 2)), withPlant);
    }

    //직접 심은 칸은 다음 세대 계산에서 이미 있는 식물로 취급되어 자동 생성이 덮어쓰지 않는지 확인
    // 사과나무 3그루를 한 줄로 심으면 바로 위(2,5)·아래(4,5) 칸이 이웃 3개로 탄생 조건을 만족
    // 아래 칸에는 해바라기를 직접 심고, 위 칸은 비워 두어 이 세대에 탄생이 실제로 일어나는지 함께 확인
    // 사과나무 줄(3행)과 해바라기(4행)는 서로 다른 블록 -> 블록 경계를 넘는 이웃 계산도 함께 확인
    @Test
    @DisplayName("탄생 조건을 만족하는 칸에 직접 심은 식물이 다음 세대에도 유지되는지 테스트")
    void plantedCellIsNotOverwrittenByBirth()
    {
        //1) 식물 모드 보드 준비 (보드를 함께 받아 칸의 식물 데이터를 확인)
        Cell board = plantModeBoard();

        //2) 사과나무 3그루를 3행 4~6열에 클릭으로 심기
        //   클릭 -> userClicked가 픽셀 좌표를 칸 좌표로 변환 -> apply가 해당 칸까지 내려가 plant 실행
        //   plant는 현재·다음 상태를 함께 바꿈 -> 다음 세대 계산에서 처음부터 있던 식물로 취급
        session.selectPlant(new AppleTreeFactory());
        click(3, 4); click(3, 5); click(3, 6);

        //3) 탄생 조건을 만족하는 아래 칸(4,5)에 해바라기를 직접 심기, 위 칸(2,5)은 비워 둠
        session.selectPlant(new SunflowerFactory());
        click(4, 5);

        //4) 한 세대 진행 (잡초 요청 없음 -> 무작위 잡초 위치가 결과에 끼어들지 않음)
        //   ① 계산: (2,5)는 빈 칸 + 이웃 사과나무 3개 -> 사과나무 탄생 예정
        //           (4,5)는 식물이 있는 칸 -> 탄생 계산 없이 해바라기 생존·성장 (씨앗 -> 새싹)
        //   ② 후보 수집: (2,5)는 탄생 후보, (4,5)는 차지된 칸이라 어떤 후보도 놓이지 않음
        //   ③ 반영: 계산해 둔 다음 상태를 실제 상태로
        session.nextGeneration();

        //5) 결과 확인
        //   비워 둔 칸: 사과나무 탄생 -> 이 세대에 탄생이 실제로 일어남
        assertInstanceOf(AppleTree.class, plantAt(board, 2, 5).getPlant());
        //   심은 칸: 해바라기가 그대로 있고 한 단계 자람 -> 새로 생긴 식물이 아니라 심은 식물
        PlantData planted = plantAt(board, 4, 5);
        assertInstanceOf(Sunflower.class, planted.getPlant());
        assertSame(PlantStages.SEEDLING, planted.getState());
    }

    //클릭으로 제거한 칸은 다음 세대 계산에서 빈 칸으로 취급되어 다시 식물이 생길 수 있는지 확인
    // 탄생 조건을 만족하는 칸(4,5)에 사과나무를 심었다가 바로 제거
    // 이웃이 모두 사과나무라 탄생 종류는 시드와 상관없이 사과나무
    // 씨앗(SEED)이면 새로 탄생한 식물, 제거가 안 됐다면 심은 씨앗이 자라 새싹(SEEDLING)이 되므로 구분됨
    @Test
    @DisplayName("클릭으로 제거한 칸에 다음 세대에 식물이 다시 탄생하는지 테스트")
    void removedCellCanBeRebornNextGeneration()
    {
        //1) 식물 모드 보드 준비
        Cell board = plantModeBoard();

        //2) 사과나무 3그루를 3행 4~6열에 클릭으로 심기
        session.selectPlant(new AppleTreeFactory());
        click(3, 4); click(3, 5); click(3, 6);

        //3) 탄생 조건을 만족하는 칸(4,5)에 사과나무를 심었다가 다시 클릭해 제거
        //   식물이 있는 칸을 클릭하면 remove 실행 -> 현재·다음 상태를 함께 지움
        //   -> 다음 세대 계산에서 처음부터 빈 칸으로 취급
        click(4, 5);                                                        //심기
        click(4, 5);                                                        //제거

        //4) 한 세대 진행 (잡초 요청 없음)
        //   ① 계산: (4,5)는 빈 칸 + 이웃 사과나무 3개 -> 사과나무 탄생 예정 (씨앗 단계부터)
        //   ② 후보 수집: (4,5)는 빈 칸이라 탄생 후보, 후보가 하나라 그대로 선택
        //   ③ 반영: (4,5)에 사과나무 씨앗이 생김
        session.nextGeneration();

        //5) 결과 확인
        //   사과나무이고 씨앗 단계 -> 제거된 뒤 새로 탄생한 식물 (제거가 안 됐다면 새싹)
        PlantData reborn = plantAt(board, 4, 5);
        assertInstanceOf(AppleTree.class, reborn.getPlant());
        assertSame(PlantStages.SEED, reborn.getState());
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

    //식물 모드 세션을 만들고 보드를 함께 돌려줌 -> 칸의 식물 종류·단계를 직접 확인하기 위함
    // Universe와 같이 보드와 세션이 같은 Random을 공유
    private Cell plantModeBoard()
    {
        Random random = new Random(SEED);
        Cell board = newBoard(random);
        session = new GameSession(board, random);
        session.changeMode(SimulationMode.PLANT);
        return board;
    }

    //(행, 열) 칸의 식물 데이터, 빈 칸이면 null
    // Undo용 스냅샷이 쓰는 저장 경로(transfer)로 읽음 -> 보드는 바꾸지 않음
    private static PlantData plantAt(Cell board, int row, int column)
    {
        return BoardSnapshot.of(board).plantAt(new Point(column, row));
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