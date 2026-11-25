package com.holub.life.history;

import java.util.ArrayDeque;
import java.util.Deque;

//Undo 이력 보관 (Memento 패턴의 Caretaker)
// 스냅샷(T)의 내용은 모르고 쌓고 꺼내기만 함 -> 무엇을 저장할지는 Originator(GameSession)가 정함
// 최대 개수를 넘으면 가장 오래된 스냅샷부터 버림 (Go로 오래 돌려도 메모리가 계속 늘지 않도록)
public class History<T>
{
    private final int capacity;
    private final Deque<T> snapshots = new ArrayDeque<>();

    public History(int capacity)
    {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        this.capacity = capacity;
    }

    //동작 직전 상태를 저장
    public void push(T snapshot)
    {
        snapshots.push(snapshot);
        if (snapshots.size() > capacity)
            snapshots.removeLast();
    }

    //가장 최근 스냅샷을 꺼냄, 이력이 없으면 null
    public T pop()
    {
        return snapshots.poll();
    }

    public boolean canUndo()
    {
        return !snapshots.isEmpty();
    }

    public int size()
    {
        return snapshots.size();
    }

    //모드 전환 시 이력 초기화
    public void clear()
    {
        snapshots.clear();
    }
}
