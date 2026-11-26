package com.holub.life.plant.species;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.SeedField;
import com.holub.life.plant.PlantStages;

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
            assertEquals(PlantStages.SEED, s);
            assertEquals(PlantStages.SEEDLING, s = tree.nextState(s));
            assertEquals(PlantStages.TREE, s = tree.nextState(s));
            assertEquals(PlantStages.FRUIT, s = tree.nextState(s));
            assertEquals(PlantStages.TREE, s = tree.nextState(s));
            assertEquals(PlantStages.FRUIT, tree.nextState(s));
            assertFalse(tree.blocksGrowth());
        }
        assertEquals("apple.png", new AppleTree().getImage(PlantStages.FRUIT));
        assertEquals("orange.png", new OrangeTree().getImage(PlantStages.FRUIT));
    }

    @Test
    @DisplayName("사과나무·오렌지나무: 시든 나무는 회복 시 일반 나무, 시든 나무 그림 표시")
    void witheredTree()
    {
        for (Plant tree : new Plant[]{ new AppleTree(), new OrangeTree() })
        {
            assertEquals(PlantStages.TREE, tree.nextState(PlantStages.WITHERED));
            assertEquals("withered.png", tree.getImage(PlantStages.WITHERED));
        }
    }

    @Test
    @DisplayName("식물 종류·단계별 그림 파일이 모두 resources/plant/에 있음")
    void imageFilesExist()
    {
        Object[][] cases = {
            { new AppleTree(),  new PlantState[]{ PlantStages.SEED, PlantStages.SEEDLING, PlantStages.TREE, PlantStages.FRUIT, PlantStages.WITHERED } },
            { new OrangeTree(), new PlantState[]{ PlantStages.FRUIT } },
            { new Sunflower(),  new PlantState[]{ PlantStages.SEED, PlantStages.SEEDLING, PlantStages.FLOWER } },
            { new Dandelion(),  new PlantState[]{ PlantStages.FLOWER } },
            { new Weed(),       new PlantState[]{ PlantStages.WEED } } };
        for (Object[] c : cases)
            for (PlantState stage : (PlantState[]) c[1])
            {
                String file = ((Plant) c[0]).getImage(stage);
                assertNotNull(getClass().getResource("/plant/" + file), c[0].getClass().getSimpleName() + " " + stage + " -> " + file);
            }
    }

    @Test
    @DisplayName("해바라기·민들레: 씨앗 -> 새싹 -> 꽃, 꽃은 유지")
    void flowerPath()
    {
        for (Plant flower : new Plant[]{ new Sunflower(), new Dandelion() })
        {
            PlantState s = flower.initialState();
            assertEquals(PlantStages.SEED, s);
            assertEquals(PlantStages.SEEDLING, s = flower.nextState(s));
            assertEquals(PlantStages.FLOWER, s = flower.nextState(s));
            assertEquals(PlantStages.FLOWER, flower.nextState(s));
        }
    }

    @Test
    @DisplayName("잡초: 항상 잡초, 성장 방해")
    void weed()
    {
        Weed weed = new Weed();
        assertEquals(PlantStages.WEED, weed.initialState());
        assertEquals(PlantStages.WEED, weed.nextState(PlantStages.WEED));
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
    @DisplayName("해바라기: 상하좌우가 모두 막혀 있으면 뿌리지 않음")
    void sunflowerBlocked()
    {
        Set<Point> occupied = Set.of(new Point(5, 4), new Point(5, 6), new Point(4, 5), new Point(6, 5));
        assertTrue(new Sunflower().seedTargets(new Point(5, 5), field(10, occupied), new Random(0)).isEmpty());
    }

    @Test
    @DisplayName("민들레: 빈 보드에서 한 곳에 가로로 연속된 3칸(씨앗 3개), 모두 보드 안")
    void dandelionTargets()
    {
        for (int seed = 0; seed < 50; seed++)
        {
            List<Point> targets = new Dandelion().seedTargets(new Point(2, 2), field(16, Set.of()), new Random(seed));
            assertEquals(3, targets.size());
            Point first = targets.get(0);
            assertEquals(new Point(first.x + 1, first.y), targets.get(1));     //한 곳에 가로로 연속
            assertEquals(new Point(first.x + 2, first.y), targets.get(2));
            for (Point p : targets)
                assertTrue(p.x >= 0 && p.x < 16 && p.y >= 0 && p.y < 16, p + " 보드 밖");
        }
    }

    @Test
    @DisplayName("민들레: 이미 식물이 있는 칸은 고르지 않음")
    void dandelionOnlyEmptyCells()
    {
        Set<Point> occupied = new HashSet<>();
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                if ((x + y) % 2 == 0)
                    occupied.add(new Point(x, y));      //체스판 모양으로 절반을 채움

        for (int seed = 0; seed < 50; seed++)
            for (Point p : new Dandelion().seedTargets(new Point(0, 0), field(8, occupied), new Random(seed)))
                assertFalse(occupied.contains(p), p + " 이미 식물이 있는 칸");
    }

    @Test
    @DisplayName("민들레: 3칸 중 일부만 비어 있으면 빈 칸만 채움")
    void dandelionPartialSeed()
    {
        //3x3 보드에서 가운데 (1,1)만 빈 칸 -> 어느 위치를 골라도 결과는 그 한 칸뿐
        Set<Point> occupied = new HashSet<>();
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                if (!(x == 1 && y == 1))
                    occupied.add(new Point(x, y));

        List<Point> targets = new Dandelion().seedTargets(new Point(0, 0), field(3, occupied), new Random(7));
        assertEquals(List.of(new Point(1, 1)), targets);
    }

    @Test
    @DisplayName("민들레: 보드가 꽉 차 있으면 멈추지 않고 빈 결과")
    void dandelionFullBoard()
    {
        Set<Point> occupied = new HashSet<>();
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 6; x++)
                occupied.add(new Point(x, y));

        assertTrue(new Dandelion().seedTargets(new Point(0, 0), field(6, occupied), new Random(0)).isEmpty());
    }
}
