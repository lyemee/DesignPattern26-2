package com.holub.life;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//.life 파일 미리 확인 테스트
class LifeFileInspectorTest
{
    //Neighborhood가 저장하는 형식과 같게: 살아 있는 칸 좌표 목록을 직렬화
    private static ByteArrayInputStream lifeFile(List<Point> liveCells) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
        {
            out.writeObject(new LinkedList<>(liveCells));
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    @Test
    @DisplayName("보드 안의 칸만 있으면 0")
    void allInside() throws IOException
    {
        assertEquals(0, LifeFileInspector.countOutside(lifeFile(List.of(new Point(0, 0), new Point(39, 39))), 40));
    }

    @Test
    @DisplayName("예전 64x64 파일에서 40x40 보드 밖에 있는 칸 수를 셈")
    void countsOutside() throws IOException
    {
        List<Point> cells = List.of(new Point(5, 5), new Point(40, 3), new Point(3, 50), new Point(63, 63));
        assertEquals(3, LifeFileInspector.countOutside(lifeFile(cells), 40));
    }

    @Test
    @DisplayName("Life 파일 형식이 아니면 IOException")
    void notLifeFile()
    {
        assertThrows(IOException.class,
                () -> LifeFileInspector.countOutside(new ByteArrayInputStream("hello".getBytes()), 40));
    }
}
