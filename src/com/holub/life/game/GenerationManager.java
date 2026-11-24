package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.plant.PlantFactory;

import java.util.List;
import java.util.Random;

// 한 세대의 처리 순서와 보드 규칙(번식 배치, 잡초 생성) 관리
// 사용자 명령과 세션 상태(모드, 선택한 식물, 수확 개수)는 GameSession 담당
public class GenerationManager
{
    //기존 코드: public boolean nextGeneration(Cell board) { ① 계산 → 변화 없으면 false → ③ 반영 }
    // -> 반환값을 GenerationResult로(수확 목록 추가), 번식 배치용 Random을 받도록 변경

    //한 세대 = ① 계산 → ② 수집과 번식 배치 → ③ 반영
    public GenerationResult nextGeneration(Cell board, Random random)
    {
        // ① 현재 세대를 기준으로 셀마다 다음 상태 계산
        //    식물 셀은 State가 번식·수확·소멸 신호를 표시
        boolean needsTransition = board.figureNextState(
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY
        );

        // 변화 없음 → ②③ 없이 끝
        //  식물 셀은 항상 "변화 있음"을 반환하므로, 여기서 끝나면 보드에 식물이 없다는 뜻 → 번식·수확도 없음
        if (!needsTransition)
            return GenerationResult.UNCHANGED;

        // ② 수집과 번식 배치 (구현 필요: Plant.seedTargets(Point, SeedField, Random) 사용)
        //  1) board.collect(0, 0, report) 한 번으로 모음
        //     - cell(): 칸별 hasSurvivingPlant, bornPlant 표
        //     - seeding(): 번식 요청 (행, 열, 꽃 종류), 순회 순서대로
        //     - harvested(): 수확 목록 → 결과로 반환
        //  2) 번식 요청마다 plant.seedTargets(new Point(열, 행), field, random)
        //     - field(SeedField): 1)의 표로 구현. 씨앗을 놓을 수 있는 칸 = 보드 안이고 살아남는 식물이 없는 칸
        //       (다음 step에 사라질 칸도 포함, 요구사항 3-1-v)
        //     - 돌려받은 칸마다 후보 목록에 그 꽃 종류 추가
        //  3) 칸마다: 탄생 예정(bornPlant)이면 탄생 종류도 후보에 추가 → random으로 같은 확률로 하나 선택
        //     씨앗이 뽑히면 board.apply(행, 열, cell -> cell.sow(new PlantData(종류)))
        //     (apply가 블록을 활성화하므로 ③ 뒤 다음 ①에서 씨앗 칸이 계산됨)

        // ③ 계산이 끝난 다음 상태를 반영
        boolean changed = board.transition();
        return new GenerationResult(changed, List.of());   //수확 목록은 ② 구현 후
    }

    //빈 칸 중 무작위 한 칸에 잡초 생성 (언제 생성할지는 GameSession이 정함)
    // ③ 반영 뒤에 호출: GameSession.nextGeneration()에서 nextGeneration()과 따로 호출
    //  (여기서 nextGeneration() 안에 두면 ①의 조기 반환에 함께 건너뛰어져 빈 보드에 잡초가 생기지 않음)
    //  1) board.collect(0, 0, report)로 빈 칸 목록 (cell()의 hasSurvivingPlant == false인 칸)
    //     ③ 뒤라 current와 next가 같음 → 살아남는 식물이 없는 칸 = 현재 빈 칸
    //  2) 빈 칸이 있으면 random으로 한 칸 → board.apply(행, 열, cell -> cell.plant(new PlantData(weedFactory.createPlant())))
    //반환: 잡초를 생성했는지 (GameSession이 화면 갱신 여부에 반영)
    public boolean spawnWeeds(Cell board, Random random, PlantFactory weedFactory)
    {
        throw new UnsupportedOperationException("잡초 생성 구현 필요");
    }
}
