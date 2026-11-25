package com.holub.life.history;

import com.holub.life.Cell;
import com.holub.life.plant.PlantData;

import java.awt.Point;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//보드 한 장의 스냅샷 (Undo용 Memento)
// 기존 NeighborhoodState와 같은 방식(transfer로 모든 칸을 순회)으로 만들고 복원하며,
// 살아 있는 칸 좌표에 더해 칸별 식물 데이터(종류·State·진행도)까지 저장
// 메모리에만 두는 Undo 이력용이라 파일 저장(load/flush)은 지원하지 않음 -> .life 저장은 기존 NeighborhoodState 사용
public final class BoardSnapshot implements Cell.PlantMemento
{
    private final Set<Point> liveCells = new HashSet<>();
    private final Map<Point, PlantData> plants = new HashMap<>();

    private BoardSnapshot() {}

    //보드의 현재 상태를 저장한 스냅샷 생성 (보드는 바꾸지 않음)
    public static BoardSnapshot of(Cell board)
    {
        BoardSnapshot snapshot = new BoardSnapshot();
        board.transfer(snapshot, new Point(0, 0), Cell.STORE);
        return snapshot;
    }

    //보드를 이 스냅샷 상태로 되돌림 (기존 Load와 같은 순서: 비우기 -> LOAD)
    // 같은 스냅샷으로 여러 번 복원해도 보드끼리 식물 데이터를 공유하지 않도록 plantAt()이 복사본을 줌
    public void restoreTo(Cell board)
    {
        board.clear();
        board.transfer(this, new Point(0, 0), Cell.LOAD);
    }

    //살아 있는 칸이 하나도 없는 보드였는지 (비어 있는 보드의 Clear는 이력에 남기지 않기 위해)
    public boolean isEmpty()
    {
        return liveCells.isEmpty();
    }

    //transfer가 넘기는 좌표 객체는 순회 중 계속 바뀌므로 복사해서 보관
    @Override
    public void markAsAlive(Point location)
    {
        liveCells.add(new Point(location));
    }

    @Override
    public boolean isAlive(Point location)
    {
        return liveCells.contains(location);
    }

    //식물 데이터는 복사해서 보관 -> 저장 뒤 보드의 식물이 자라도 스냅샷은 그대로
    @Override
    public void markPlant(Point location, PlantData data)
    {
        plants.put(new Point(location), data.copy());
    }

    @Override
    public PlantData plantAt(Point location)
    {
        PlantData data = plants.get(location);
        return data == null ? null : data.copy();
    }

    @Override
    public void load(InputStream in)
    {
        throw new UnsupportedOperationException("Undo 스냅샷은 파일로 불러오지 않음");
    }

    @Override
    public void flush(OutputStream out)
    {
        throw new UnsupportedOperationException("Undo 스냅샷은 파일로 저장하지 않음");
    }
}
