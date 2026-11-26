package com.holub.life.game;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerationSnapshotTest
{
    @Test
    void positionListPreservesReportedEmptyCellOrderInsteadOfRowOrder()
    {
        GenerationManager.GenerationSnapshot snapshot = new GenerationManager.GenerationSnapshot(8);
        snapshot.currentCell(4, 0, false);
        snapshot.currentCell(0, 4, false);
        snapshot.currentCell(1, 1, true);
        snapshot.currentCell(7, 7, false);

        assertTrue(snapshot.isEmpty(new Point(0, 4)));
        assertTrue(snapshot.isEmpty(new Point(4, 0)));
        assertFalse(snapshot.isEmpty(new Point(1, 1)));
        assertEquals(List.of(new Point(0, 4), new Point(4, 0), new Point(7, 7)),
                snapshot.emptyPositions());
    }

    @Test
    void repeatedRequestsReuseOnePositionListWithoutAllowingReordering()
    {
        GenerationManager.GenerationSnapshot snapshot = new GenerationManager.GenerationSnapshot(4);
        snapshot.currentCell(2, 3, false);
        snapshot.currentCell(0, 0, false);

        List<Point> positions = snapshot.emptyPositions();

        assertSame(positions, snapshot.emptyPositions());
        assertThrows(UnsupportedOperationException.class, () -> positions.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> positions.add(new Point(1, 1)));
        assertEquals(List.of(new Point(3, 2), new Point(0, 0)), snapshot.emptyPositions());
    }

    @Test
    void newSnapshotDoesNotReuseThePreviousGenerationsPositions()
    {
        GenerationManager.GenerationSnapshot first = new GenerationManager.GenerationSnapshot(2);
        first.currentCell(0, 0, false);
        first.currentCell(1, 1, true);
        List<Point> original = first.emptyPositions();

        GenerationManager.GenerationSnapshot next = new GenerationManager.GenerationSnapshot(2);
        next.currentCell(0, 0, true);
        next.currentCell(1, 1, false);

        assertEquals(List.of(new Point(1, 1)), next.emptyPositions());
        assertEquals(List.of(new Point(0, 0)), original);
        assertSame(original, first.emptyPositions());
        assertNotSame(original, next.emptyPositions());
    }

    @Test
    void snapshotWithNoEmptyCellsCachesAnEmptyPositionList()
    {
        GenerationManager.GenerationSnapshot snapshot = new GenerationManager.GenerationSnapshot(2);
        for (int row = 0; row < 2; row++)
            for (int column = 0; column < 2; column++)
                snapshot.currentCell(row, column, true);

        List<Point> positions = snapshot.emptyPositions();

        assertTrue(positions.isEmpty());
        assertSame(positions, snapshot.emptyPositions());
    }
}
