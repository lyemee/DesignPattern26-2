package com.holub.life;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CurrentCellReportTest
{
    @Test
    void nestedBoardReportsEveryCurrentCellOnceWithBoardCoordinates()
    {
        Cell board = new Neighborhood(4, new Neighborhood(4, new Resident(new Random(42))));
        board.apply(3, 4, Resident::flipAlive);
        board.apply(15, 0, Resident::flipAlive);
        Set<Point> visited = new HashSet<>();
        Set<Point> occupied = new HashSet<>();

        board.collect(0, 0, new CellReport()
        {
            @Override
            public void currentCell(int row, int column, boolean alive)
            {
                Point point = new Point(column, row);
                assertTrue(visited.add(point), "각 셀은 한 번만 알림");
                if (alive)
                    occupied.add(point);
            }
        });

        assertEquals(256, visited.size());
        assertEquals(Set.of(new Point(4, 3), new Point(0, 15)), occupied);
    }

    @Test
    void dyingCellStaysOccupiedUntilTransition()
    {
        Resident cell = new Resident(new Random(42));
        cell.flipAlive();
        cell.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);

        assertTrue(currentAlive(cell));
        cell.transition();
        assertFalse(currentAlive(cell));
    }

    @Test
    void upcomingBirthDoesNotOccupyCurrentEmptyCell()
    {
        Resident cell = new Resident(new Random(42));
        Resident neighbor = new Resident(new Random(42));
        neighbor.flipAlive();
        cell.figureNextState(neighbor, neighbor, neighbor, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);

        assertFalse(currentAlive(cell));
        cell.transition();
        assertTrue(currentAlive(cell));
    }

    @Test
    void currentStateCollectionDoesNotChangeBoardOrActiveEdges()
    {
        Cell board = new Neighborhood(4, new Neighborhood(4, new Resident(new Random(42))));
        board.apply(5, 5, Resident::flipAlive);
        Direction before = new Direction(board.isDisruptiveTo());

        assertTrue(before.equals(Direction.NONE));
        assertTrue(currentAlive(board));
        assertTrue(currentAlive(board));
        assertTrue(before.equals(board.isDisruptiveTo()));
    }

    private static boolean currentAlive(Cell cell)
    {
        boolean[] occupied = { false };
        cell.collect(0, 0, new CellReport()
        {
            @Override
            public void currentCell(int row, int column, boolean alive)
            {
                occupied[0] |= alive;
            }
        });
        return occupied[0];
    }
}
