package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.Storable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    //기존 코드
    //public GameSession(Cell board){
    //    this.board = board;
    //}
    public GameSession(Cell board, Random random){
        this.board = board;
        this.random = random;
    }

    //다음 상태 계산 도중 사용자의 클릭 처리와 같은 cell 변화가 영향을 미치지 않도록 synchronized로 락
    public synchronized boolean nextGeneration(){
        // generation manager 객체에게 다음 세대 처리 위임
        return generationManager.nextGeneration(board);
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

        //PLANT 동작 구현 필요
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
