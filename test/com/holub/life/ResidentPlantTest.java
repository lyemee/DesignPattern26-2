package com.holub.life;

import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantStages;
import com.holub.life.plant.PlantState;
import com.holub.life.plant.species.AppleTree;
import com.holub.life.plant.species.Dandelion;
import com.holub.life.plant.species.OrangeTree;
import com.holub.life.plant.species.Sunflower;
import com.holub.life.plant.species.Weed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

//보드 위에서 Resident의 식물 계산 흐름 테스트 (① 계산 -> collect -> ③ 반영)
// 실제 보드(Neighborhood 안의 Neighborhood)를 써서 블록 경계·활성화까지 함께 확인
// ② 씨 뿌리기(GenerationManager)는 담당 4 영역이라 여기서는 collect로 받은 신호만 확인
class ResidentPlantTest
{
    private static final int[][] BLOCK_CELLS = { {0, 0}, {0, 1}, {1, 0}, {1, 1} };

    private Cell board;
    private final List<Plant> harvested = new ArrayList<>();
    private final List<Point> seeding   = new ArrayList<>();

    @BeforeEach
    void setUp()
    {
        board = newBoard(7);
    }

    //--- A. 성장과 수확 ---

    @Test
    @DisplayName("사과 2x2: 씨앗 -> 새싹 -> 나무 -> 열매나무 -> 수확하며 나무 (4그루 수확 신호)")
    void appleBlockGrowsAndHarvests()
    {
        block(new AppleTree(), 5, 5);
        step(); assertBlock(PlantStages.SEEDLING, 5, 5);
        step(); assertBlock(PlantStages.TREE,     5, 5);
        step(); assertBlock(PlantStages.FRUIT,    5, 5);
        assertTrue(harvested.isEmpty(), "열매가 열린 step에는 아직 수확 없음");
        step(); assertBlock(PlantStages.TREE,     5, 5);
        assertEquals(4, harvested.size());
        for (Plant p : harvested)
            assertTrue(p instanceof AppleTree);
    }

    @Test
    @DisplayName("오렌지 수확 신호는 오렌지나무 종류로 전달 (수확 개수를 종류별로 세는 기준)")
    void orangeHarvestKeepsSpecies()
    {
        block(new OrangeTree(), 5, 5);
        step(); step(); step(); step();
        assertEquals(4, harvested.size());
        for (Plant p : harvested)
            assertTrue(p instanceof OrangeTree);
    }

    @Test
    @DisplayName("혼자 심은 씨앗은 외로워도 죽지 않고 새싹으로 자람")
    void loneSeedSurvivesAndGrows()
    {
        plant(new AppleTree(), 5, 5);
        step();
        assertSame(PlantStages.SEEDLING, plantAt(5, 5).getState());
        assertTrue(aliveAt(5, 5));
    }

    //--- B. 시듦·회복 ---

    @Test
    @DisplayName("외로운 나무: 시든 나무로 1 step, 다음 step에도 조건을 못 채우면 사망")
    void lonelyTreeWithersThenDies()
    {
        block(new AppleTree(), 5, 5);
        step(); step();                                     //나무
        remove(5, 6); remove(6, 5); remove(6, 6);           //(5,5)만 남김
        step();
        assertEquals(PlantStages.WITHERED, plantAt(5, 5).getState());
        assertTrue(aliveAt(5, 5), "시든 나무는 살아 있는 칸 (이웃 수에 포함)");
        step();
        assertNull(plantAt(5, 5));
    }

    @Test
    @DisplayName("과밀한 나무도 바로 죽지 않고 시듦 (나무는 사망 전에 반드시 시듦)")
    void overcrowdedTreeWithers()
    {
        block(new AppleTree(), 5, 5);
        step(); step();                                     //나무 4그루
        plant(new AppleTree(), 4, 4); plant(new AppleTree(), 4, 5); plant(new AppleTree(), 5, 4);
        step();                                             //(5,5) 이웃 6개
        assertEquals(PlantStages.WITHERED, plantAt(5, 5).getState());
    }

    @Test
    @DisplayName("열매나무가 외로워지면 수확 없이 시듦")
    void lonelyFruitWithersWithoutHarvest()
    {
        block(new AppleTree(), 5, 5);
        step(); step(); step();                             //열매나무
        remove(5, 6); remove(6, 5); remove(6, 6);
        step();
        assertEquals(PlantStages.WITHERED, plantAt(5, 5).getState());
        assertTrue(harvested.isEmpty());
    }

    @Test
    @DisplayName("시든 나무가 조건을 다시 채우면 나무로 회복, 그 step에는 열매로 가지 않음")
    void witheredTreeRecovers()
    {
        block(new AppleTree(), 5, 5);
        step(); step();
        remove(5, 6); remove(6, 5); remove(6, 6);
        step();                                             //(5,5) 시듦
        plant(new AppleTree(), 5, 6); plant(new AppleTree(), 6, 5);   //이웃 2개
        step();
        assertEquals(PlantStages.TREE, plantAt(5, 5).getState(), "회복한 step은 나무까지만");
        step();
        assertEquals(PlantStages.FRUIT, plantAt(5, 5).getState(), "다음 step부터 다시 성장");
    }

    //--- C. 꽃 ---

    @Test
    @DisplayName("해바라기 2x2: 꽃이 된 다음 step에 4송이 모두 씨 뿌림 신호를 남기고 소멸")
    void sunflowersSeedAndDisappear()
    {
        block(new Sunflower(), 5, 5);
        step(); step();
        assertBlock(PlantStages.FLOWER, 5, 5);
        step();
        assertEquals(4, seeding.size());
        for (int[] c : BLOCK_CELLS)
            assertNull(plantAt(5 + c[0], 5 + c[1]), "②가 씨를 뿌리기 전이라 빈 칸");
    }

    @Test
    @DisplayName("민들레도 같은 꽃 단계 행동: 씨 뿌림 신호 4개, 소멸")
    void dandelionsSeedAndDisappear()
    {
        block(new Dandelion(), 5, 5);
        step(); step(); step();
        assertEquals(4, seeding.size());
        assertNull(plantAt(5, 5));
    }

    @Test
    @DisplayName("생존 조건 때문에 죽는 꽃은 씨를 뿌리지 않음 (단계로 사라질 때만 씨 뿌림)")
    void flowerDyingByRuleDoesNotSeed()
    {
        block(new Sunflower(), 5, 5);
        step(); step();                                     //꽃 4송이
        plant(new AppleTree(), 4, 4); plant(new AppleTree(), 4, 5); plant(new AppleTree(), 4, 6);
        step();                                             //위쪽 두 송이는 이웃 5~6개 -> 과밀
        assertEquals(2, seeding.size());
        assertTrue(seeding.contains(new Point(5, 6)) && seeding.contains(new Point(6, 6)),
                   "아래쪽 두 송이만 씨 뿌림 " + seeding);
        assertNull(plantAt(5, 5));
        assertNull(plantAt(5, 6));
    }

    //--- D. 잡초 ---

    @Test
    @DisplayName("잡초 옆 꽃: 생존 조건을 채우면 꽃으로 머물고 씨를 안 뿌림")
    void weedKeepsFlower()
    {
        block(new Sunflower(), 5, 5);
        step(); step();                                     //꽃 4송이
        remove(6, 5); remove(6, 6);
        plant(new Weed(), 6, 5);                            //(5,5)·(5,6) 이웃 = 꽃 1 + 잡초 1
        step();
        assertEquals(PlantStages.FLOWER, plantAt(5, 5).getState());
        assertEquals(PlantStages.FLOWER, plantAt(5, 6).getState());
        assertTrue(seeding.isEmpty());
    }

    @Test
    @DisplayName("잡초 옆 꽃이 사망 조건이면 씨 없이 사망, 잡초와 안 닿은 꽃은 정상 번식")
    void weedNextToDyingFlower()
    {
        block(new Sunflower(), 5, 5);
        step(); step();
        plant(new Weed(), 4, 4);                            //(5,5)만 잡초와 닿음, 이웃 4개
        step();
        assertNull(plantAt(5, 5));
        assertEquals(3, seeding.size());
        assertFalse(seeding.contains(new Point(5, 5)));
    }

    @Test
    @DisplayName("잡초 옆 나무는 열매로 못 감 (잡초도 이웃 수에 포함, 생존 판정은 정상)")
    void weedBlocksTreeGrowth()
    {
        block(new AppleTree(), 5, 5);
        step(); step();                                     //나무
        remove(6, 6);
        plant(new Weed(), 6, 6);                            //이웃 3개 그대로
        step();
        assertEquals(PlantStages.TREE, plantAt(5, 5).getState());
        assertEquals(PlantStages.TREE, plantAt(5, 6).getState());
        assertEquals(PlantStages.WEED, plantAt(6, 6).getState());
    }

    @Test
    @DisplayName("잡초 생존은 일반 규칙: 2x2는 유지, 혼자면 사망")
    void weedFollowsRule()
    {
        block(new Weed(), 5, 5);
        plant(new Weed(), 10, 10);
        step(); step(); step();
        assertBlock(PlantStages.WEED, 5, 5);
        assertNull(plantAt(10, 10));
    }

    //--- E. 규칙 탄생 ---

    @Test
    @DisplayName("규칙 탄생: 이웃 식물이 3개인 빈 칸에 그중 한 종류의 씨앗이 태어남")
    void ruleBirthTakesNeighborSpecies()
    {
        plant(new AppleTree(), 5, 5); plant(new AppleTree(), 5, 6); plant(new OrangeTree(), 6, 5);
        step();
        PlantData born = plantAt(6, 6);
        assertNotNull(born);
        assertEquals(PlantStages.SEED, born.getState());
        assertTrue(born.getPlant() instanceof AppleTree || born.getPlant() instanceof OrangeTree);
    }

    @Test
    @DisplayName("규칙 탄생 부모에는 잡초도 포함: 잡초 3개 옆 빈 칸에는 잡초가 태어남")
    void ruleBirthFromWeeds()
    {
        plant(new Weed(), 5, 5); plant(new Weed(), 5, 6); plant(new Weed(), 6, 5);
        step();
        assertTrue(plantAt(6, 6).getPlant() instanceof Weed);
        assertEquals(PlantStages.WEED, plantAt(6, 6).getState());
    }

    @Test
    @DisplayName("이웃이 2개뿐인 빈 칸에는 아무것도 태어나지 않음")
    void noBirthWithTwoNeighbors()
    {
        plant(new AppleTree(), 5, 5); plant(new AppleTree(), 5, 7);
        step();
        assertNull(plantAt(5, 6));
        assertFalse(aliveAt(5, 6));
    }

    @Test
    @DisplayName("식물이 없는 칸은 원본 Conway 규칙 그대로 (blinker 진동)")
    void conwayCellsUnchanged()
    {
        for (int column = 4; column <= 6; column++)
            board.apply(5, column, Resident::flipAlive);    //가로 blinker
        step();
        assertTrue(aliveAt(4, 5) && aliveAt(5, 5) && aliveAt(6, 5), "세로로 바뀜");
        assertFalse(aliveAt(5, 4) || aliveAt(5, 6));
        assertNull(plantAt(4, 5), "식물이 없는 칸에서는 식물이 생기지 않음");
        step();
        assertTrue(aliveAt(5, 4) && aliveAt(5, 5) && aliveAt(5, 6), "다시 가로");
    }

    //--- F. 재현성 ---

    @Test
    @DisplayName("같은 시드면 같은 결과 (공유 Random으로 규칙 탄생 종류가 결정됨)")
    void sameSeedSameResult()
    {
        Cell other = newBoard(7);
        for (Cell b : new Cell[]{ board, other })
        {
            Cell saved = board;
            board = b;
            plant(new AppleTree(), 3, 3); plant(new OrangeTree(), 3, 4); plant(new Sunflower(), 4, 3);
            plant(new AppleTree(), 4, 4); plant(new Dandelion(), 5, 4); plant(new OrangeTree(), 5, 5);
            for (int i = 0; i < 12; i++)
                step();
            board = saved;
        }
        for (int row = 0; row < 16; row++)
            for (int column = 0; column < 16; column++)
            {
                PlantData a = plantAt(board, row, column), b = plantAt(other, row, column);
                assertEquals(a == null, b == null, row + "," + column);
                if (a != null)
                {
                    assertSame(a.getPlant().getClass(), b.getPlant().getClass());
                    assertSame(a.getState(), b.getState());
                }
            }
    }

    //--- 보조 ---

    private static Cell newBoard(long seed)
    {
        return new Neighborhood(4, new Neighborhood(4, new Resident(new Random(seed))));    //16x16
    }

    private void block(Plant plant, int row, int column)
    {
        for (int[] c : BLOCK_CELLS)
            plant(plant, row + c[0], column + c[1]);
    }

    private void plant(Plant plant, int row, int column)
    {
        board.apply(row, column, cell -> cell.plant(new PlantData(plant)));
    }

    private void remove(int row, int column)
    {
        board.apply(row, column, Resident::remove);
    }

    //GenerationManager의 ①·③과 같은 순서, ②는 신호 수집만
    private void step()
    {
        harvested.clear();
        seeding.clear();
        board.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
                              Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
        board.collect(0, 0, new CellReport() {
            @Override public void seeding(int row, int column, Plant plant) { seeding.add(new Point(column, row)); }
            @Override public void harvested(Plant plant) { harvested.add(plant); }
        });
        board.transition();
    }

    private PlantData plantAt(int row, int column)
    {
        return plantAt(board, row, column);
    }

    private static PlantData plantAt(Cell target, int row, int column)
    {
        PlantData[] found = new PlantData[1];
        target.apply(row, column, cell -> found[0] = cell.plantData());
        return found[0];
    }

    private boolean aliveAt(int row, int column)
    {
        boolean[] found = new boolean[1];
        board.apply(row, column, cell -> found[0] = cell.isAlive());
        return found[0];
    }

    private void assertBlock(PlantState expected, int row, int column)
    {
        for (int[] c : BLOCK_CELLS)
            assertEquals(expected, plantAt(row + c[0], column + c[1]).getState(),
                         (row + c[0]) + "," + (column + c[1]));
    }
}
