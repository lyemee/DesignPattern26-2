package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.TemporaryStage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

//식물 종류별 성장 경로와 씨 뿌리기 대상 좌표 테스트
class PlantTypesTest
{
    //테스트용 보드: size x size, occupied에 든 좌표만 살아 있음
    private static SeedField field(int size, Set<Point> occupied)
    {
        return new SeedField() {
            public boolean isEmpty(Point p) {
                boolean inside = p.x >= 0 && p.y >= 0 && p.x < size && p.y < size;
                return inside && !occupied.contains(p);
            }
            public int widthInCells() { return size; }
        };
    }

    @Test
    @DisplayName("사과나무·오렌지나무: 씨앗 -> 새싹 -> 나무 -> 열매나무 -> 나무 반복")
    void fruitTreePath()
    {
        for (Plant tree : new Plant[]{ new AppleTree(), new OrangeTree() })
        {
            PlantState s = tree.initialState();
            assertEquals(TemporaryStage.SEED, s);
            assertEquals(TemporaryStage.SEEDLING, s = tree.nextState(s));
            assertEquals(TemporaryStage.TREE, s = tree.nextState(s));
            assertEquals(TemporaryStage.FRUIT, s = tree.nextState(s));
            assertEquals(TemporaryStage.TREE, s = tree.nextState(s));
            assertEquals(TemporaryStage.FRUIT, tree.nextState(s));
            assertFalse(tree.blocksGrowth());
        }
        assertEquals("🍎", new AppleTree().getImage(TemporaryStage.FRUIT));
        assertEquals("🍊", new OrangeTree().getImage(TemporaryStage.FRUIT));
    }

    @Test
    @DisplayName("해바라기·민들레: 씨앗 -> 새싹 -> 꽃, 꽃은 유지")
    void flowerPath()
    {
        for (Plant flower : new Plant[]{ new Sunflower(), new Dandelion() })
        {
            PlantState s = flower.initialState();
            assertEquals(TemporaryStage.SEED, s);
            assertEquals(TemporaryStage.SEEDLING, s = flower.nextState(s));
            assertEquals(TemporaryStage.FLOWER, s = flower.nextState(s));
            assertEquals(TemporaryStage.FLOWER, flower.nextState(s));
        }
    }

    @Test
    @DisplayName("잡초: 항상 잡초, 성장 방해")
    void weed()
    {
        Weed weed = new Weed();
        assertEquals(TemporaryStage.WEED, weed.initialState());
        assertEquals(TemporaryStage.WEED, weed.nextState(TemporaryStage.WEED));
        assertTrue(weed.blocksGrowth());
    }

    @Test
    @DisplayName("해바라기: 상하좌우 중 빈 칸만, 보드 밖 제외")
    void sunflowerTargets()
    {
        Sunflower sunflower = new Sunflower();

        List<Point> all = sunflower.seedTargets(new Point(5, 5), field(10, Set.of()), new Random(0));
        assertEquals(Set.of(new Point(5, 4), new Point(5, 6), new Point(4, 5), new Point(6, 5)), new HashSet<>(all));

        List<Point> blocked = sunflower.seedTargets(new Point(5, 5), field(10, Set.of(new Point(5, 4))), new Random(0));
        assertEquals(3, blocked.size());
        assertFalse(blocked.contains(new Point(5, 4)));

        List<Point> corner = sunflower.seedTargets(new Point(0, 0), field(10, Set.of()), new Random(0));
        assertEquals(Set.of(new Point(0, 1), new Point(1, 0)), new HashSet<>(corner));
    }

    @Test
    @DisplayName("민들레: 거리 15 이상 빈 칸에 가로 3칸 씨앗 3개(9칸)")
    void dandelionTargets()
    {
        Point center = new Point(2, 2);
        List<Point> targets = new Dandelion().seedTargets(center, field(64, Set.of()), new Random(42));

        assertEquals(9, targets.size());
        assertEquals(9, new HashSet<>(targets).size());     //겹치는 칸 없음
        for (int i = 0; i < targets.size(); i += 3)
        {
            Point first = targets.get(i);
            assertEquals(new Point(first.x + 1, first.y), targets.get(i + 1));
            assertEquals(new Point(first.x + 2, first.y), targets.get(i + 2));
        }
        for (Point p : targets)
            assertTrue(Dandelion.distance(p, center) >= Dandelion.MIN_DISTANCE);
    }

    @Test
    @DisplayName("민들레: 부모 왼쪽 후보도 세 칸 모두 최소 거리 이상")
    void dandelionDistanceForEveryCell()
    {
        Point center = new Point(32, 32);
        for (int seed = 0; seed < 50; seed++)
            for (Point p : new Dandelion().seedTargets(center, field(64, Set.of()), new Random(seed)))
                assertTrue(Dandelion.distance(p, center) >= Dandelion.MIN_DISTANCE, p + " 거리 부족");
    }

    @Test
    @DisplayName("민들레: 빈 자리가 드물어도 있으면 모두 찾아 뿌림")
    void dandelionFindsSparseRoom()
    {
        Set<Point> empty = new HashSet<>();
        for (int[] start : new int[][]{ {40, 40}, {45, 45}, {50, 50} })
            for (int i = 0; i < 3; i++)
                empty.add(new Point(start[0] + i, start[1]));

        Set<Point> occupied = new HashSet<>();
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
                if (!empty.contains(new Point(x, y)))
                    occupied.add(new Point(x, y));

        List<Point> targets = new Dandelion().seedTargets(new Point(2, 2), field(64, occupied), new Random(42));
        assertEquals(empty, new HashSet<>(targets));
    }

    @Test
    @DisplayName("민들레: 조건을 만족하는 자리가 없으면 뿌리지 않음")
    void dandelionNoRoom()
    {
        //보드가 작아 거리 15 이상인 칸이 없음
        assertTrue(new Dandelion().seedTargets(new Point(2, 2), field(8, Set.of()), new Random(0)).isEmpty());
    }
}
