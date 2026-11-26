package com.holub.life.history;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Undo 이력(Caretaker) 테스트
class HistoryTest
{
    @Test
    @DisplayName("가장 최근에 저장한 것부터 꺼냄, 비면 null")
    void lastInFirstOut()
    {
        History<String> history = new History<>(10);
        assertFalse(history.canUndo());
        assertNull(history.pop());

        history.push("a");
        history.push("b");
        assertTrue(history.canUndo());
        assertEquals("b", history.pop());
        assertEquals("a", history.pop());
        assertFalse(history.canUndo());
    }

    @Test
    @DisplayName("최대 개수를 넘으면 가장 오래된 것부터 버림")
    void dropsOldest()
    {
        History<Integer> history = new History<>(3);
        for (int i = 1; i <= 5; i++)
            history.push(i);

        assertEquals(3, history.size());
        assertEquals(5, history.pop());
        assertEquals(4, history.pop());
        assertEquals(3, history.pop());
        assertNull(history.pop());
    }

    @Test
    @DisplayName("초기화하면 이력이 비워짐")
    void clear()
    {
        History<String> history = new History<>(10);
        history.push("a");
        history.clear();
        assertFalse(history.canUndo());
        assertEquals(0, history.size());
    }

    @Test
    @DisplayName("최대 개수는 1 이상이어야 함")
    void invalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new History<String>(0));
    }
}
