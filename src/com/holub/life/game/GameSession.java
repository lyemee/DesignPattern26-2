package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Random;

//보드를 보유해 게임 조작의 진입점을 제공 (기존 Universe로 부터 책임 분리)
public class GameSession
{
    private final Cell board;
    private final GenerationManager generationManager = new GenerationManager();
    //기본 모드는 Life game 원본
    private SimulationMode mode = SimulationMode.CONWAY;
    //보드의 Resident 프로토타입과 같은 Random (Universe가 만들어 보드와 함께 전달) -> 테스트 용이성
    // 세대 처리(GenerationManager)에 넘겨 번식 배치·잡초 생성의 무작위 선택에 사용
    private final Random random;
    //식물 선택 메뉴에서 고른 종류. 고르기 전에는 null -> 식물 모드에서 빈 칸을 눌러도 심지 않음
    private PlantFactory selectedPlant = null;

    //기존 코드
    //public GameSession(Cell board){
    //    this.board = board;
    //}
    public GameSession(Cell board, Random random){
        this.board = board;
        this.random = random;
    }

    //(수확 개수) 종류별 수확 개수 Map 자리. 키는 식물 종류 id

    //다음 상태 계산 도중 사용자의 클릭 처리와 같은 cell 변화가 영향을 미치지 않도록 synchronized로 락
    //반환: 화면을 다시 그려야 하는지 (Universe는 true일 때만 refreshNow)
    public synchronized boolean nextGeneration(){
        return advance(null);
    }

    //호출 측에서 잡초 후보를 만들 tick에만 Factory 전달. 일반 모드에서는 요청을 적용하지 않는다.
    //이 요청은 현재 tick에만 유효하며 사용자 드롭박스에서 선택한 식물과 무관하다.
    public synchronized boolean nextGeneration(PlantFactory weedFactory){
        return advance(Objects.requireNonNull(weedFactory, "weedFactory"));
    }

    //두 synchronized 진입점에서만 호출해 계산·후보 경쟁·반영 사이에 클릭이 끼어들지 않도록 한다.
    private boolean advance(PlantFactory weedFactory){

        //(Undo) 여기서 Snapshot 저장: 보드를 바꾸기 전 상태

        // 1) 한 세대 처리 (① 계산 → ② 꽃·일반 탄생·요청된 잡초 후보의 균등 경쟁 → ③ 반영)
        GenerationResult result = mode == SimulationMode.PLANT && weedFactory != null
                ? generationManager.nextGeneration(board, random, weedFactory)
                : generationManager.nextGeneration(board, random);

        // 2) (수확 개수) result.harvested()를 종류별 개수에 더함

        // 3) 잡초가 선택된 경우도 1)의 결과에 포함. 반영 뒤 별도로 생성하지 않는다.
        return result.changed();
    }

    public synchronized SimulationMode getMode(){
        return mode;
    }

    //모드 전환: 보드를 비운 뒤 모드 전환
    //일반 셀과 식물 셀이 섞이지 않도록
    //synchronized -> Tick 계산 도중 보드가 비워지지 않도록 락 사용
    public synchronized void changeMode(SimulationMode mode){
        board.clear();
        this.mode = mode;
    }

    //화면 진입점: Universe가 클릭한 픽셀 좌표와 화면 크기를 넘김
    // 클릭 처리가 다음 세대 계산과 반영 사이에 끼어들지 않도록 synchronized
    public synchronized void userClicked(Point point, Rectangle bounds){
        //기존 코드
        //board.userClicked(point, bounds);

        // 1) 픽셀 → 셀 좌표
        //    Universe가 화면을 보드 너비의 배수인 정사각형으로 맞추므로
        //    원본 Neighborhood의 단계별 나눗셈과 같은 칸을 가리킴
        int width    = board.widthInCells();
        int cellSize = bounds.width / width;
        if(cellSize == 0 || point.x < 0 || point.y < 0)   //칸 크기를 정할 수 없거나 왼쪽·위쪽 밖
            return;                                     // (나누기 전에 검사: -1 / 8도 0이 되어 첫 줄로 처리됨)
        int row    = point.y / cellSize;
        int column = point.x / cellSize;
        if(row >= width || column >= width)             //오른쪽·아래쪽 밖
            return;

        // 2) 모드에 맞는 동작(CellAction)을 만들어 apply 경로로 전달
        //    apply가 목적지 셀까지 내려가며 지나간 블록을 활성화
        if(mode == SimulationMode.CONWAY)
            board.apply(row, column, cell -> cell.flipAlive());   //원본 동작
        else
            board.apply(row, column, cell -> {
                if(cell.isAlive())                                          //식물 모드에서 살아 있는 칸 = 식물이 있는 칸
                    cell.remove();                                          // -> 종류·단계와 상관없이 제거
                else if(selectedPlant != null)
                    cell.plant(new PlantData(selectedPlant.createPlant())); //빈 칸 -> 선택한 식물 심기
                //식물을 고르지 않았으면 아무것도 하지 않음 (Conway 셀이 섞이지 않음)
            });
    }

    //식물 선택 메뉴에서 호출 -> 이후 식물 모드 클릭은 이 종류를 심음
    public synchronized void selectPlant(PlantFactory factory){
        selectedPlant = factory;
    }

    public synchronized void clear(){
        board.clear();
    }

    public int widthInCells(){
        //보드의 셀 개수 반환
        return board.widthInCells();
    }

    public synchronized void redraw(
            Graphics graphics,
            Rectangle bounds,
            boolean drawAll)
    {
        // 세대 반영이 끝난 보드 그리기
        board.redraw(graphics, bounds, drawAll);
    }

    public synchronized void load(InputStream in) throws IOException
    {
        //PLANT 모드의 load 아직 미구현 -> CONWAY를 기본으로
        mode = SimulationMode.CONWAY;
        // 기존 universe의 불러오기 방식
        board.clear();

        Storable memento = board.createMemento();
        memento.load(in);
        board.transfer(memento, new Point(0, 0), Cell.LOAD);
    }

    public synchronized void store(OutputStream out) throws IOException
    {
        // 기존 universe의 저장 방식
        Storable memento = board.createMemento();
        board.transfer(memento, new Point(0, 0), Cell.STORE);
        memento.flush(out);
    }
}
