package com.holub.life.game;

import com.holub.life.Cell;
import com.holub.life.CellReport;
import com.holub.life.plant.Plant;
import com.holub.life.plant.PlantData;
import com.holub.life.plant.PlantFactory;
import com.holub.life.plant.SeedField;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

// 한 세대의 처리 순서와 보드 규칙(번식 배치, 잡초 생성) 관리
// 사용자 명령과 세션 상태(모드, 선택한 식물, 수확 개수)는 GameSession 담당
public class GenerationManager
{
    private final CandidateSelectionPolicy<Candidate> selectionPolicy = new UniformSelectionPolicy<>();

    //기존 코드: public boolean nextGeneration(Cell board) { ① 계산 → 변화 없으면 false → ③ 반영 }
    // -> 반환값을 GenerationResult로(수확 목록 추가), 번식 배치용 Random을 받도록 변경

    //한 세대 = ① 계산 → ② 수집과 번식 배치 → ③ 반영
    public GenerationResult nextGeneration(Cell board, Random random)
    {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(random, "random");

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

        // ② 모든 셀의 현재 점유 여부와 계산 결과를 먼저 수집. 현재 값은 ③ 전까지 유지된다.
        GenerationSnapshot snapshot = new GenerationSnapshot(board.widthInCells());
        board.collect(0, 0, snapshot);

        //모든 꽃은 같은 현재 빈 칸 스냅샷을 조회. 후보 수집 중에는 보드를 변경하지 않는다.
        for (SeedingRequest request : snapshot.requests)
            for (Point target : request.plant().seedTargets(
                    new Point(request.column(), request.row()), snapshot, random))
                if (snapshot.isEmpty(target))
                    snapshot.addCandidate(target, new Candidate(request.plant(), false));

        boolean seeded = false;
        for (Map.Entry<Point, List<Candidate>> entry : snapshot.candidates.entrySet())
        {
            Point target = entry.getKey();
            //이번에 죽을 칸도 현재 점유 중이면 제외. 클릭으로 심은 식물도 추첨 대상이 아니다.
            if (!snapshot.isEmpty(target))
                continue;

            Candidate selected = selectionPolicy.select(entry.getValue(), random).orElseThrow();
            //일반 탄생이 선택되면 셀이 계산해 둔 다음 상태를 그대로 보존한다.
            if (!selected.born())
            {
                board.apply(target.y, target.x, cell -> cell.sow(new PlantData(selected.plant())));
                seeded = true;
            }
        }

        // ③ 계산이 끝난 다음 상태를 반영
        boolean changed = board.transition();
        return new GenerationResult(changed || seeded, List.of());   //수확 목록 수집은 별도 작업
    }

    //born은 출처별 우선순위가 아니라, 기존 탄생 계산 결과를 보존하기 위한 구분이다.
    private record Candidate(Plant plant, boolean born) {}
    private record SeedingRequest(int row, int column, Plant plant) {}

    //한 세대만 사용하는 읽기 전용 현재 빈 칸 조회. 후보와 요청은 collect 완료 후 처리한다.
    private static final class GenerationSnapshot implements CellReport, SeedField
    {
        private final boolean[][] empty;
        private final List<SeedingRequest> requests = new ArrayList<>();
        //삽입 순서를 유지해 같은 보고 순서·seed에서 난수 소비 순서도 재현한다.
        private final Map<Point, List<Candidate>> candidates = new LinkedHashMap<>();

        private GenerationSnapshot(int width)
        {
            empty = new boolean[width][width];
        }

        @Override
        public void currentCell(int row, int column, boolean alive)
        {
            empty[row][column] = !alive;
        }

        @Override
        public void cell(int row, int column, boolean hasSurvivingPlant, Plant bornPlant)
        {
            if (bornPlant != null)
                addCandidate(new Point(column, row), new Candidate(bornPlant, true));
        }

        @Override
        public void seeding(int row, int column, Plant plant)
        {
            requests.add(new SeedingRequest(row, column, plant));
        }

        private void addCandidate(Point target, Candidate candidate)
        {
            //꽃이 반환한 Point가 나중에 바뀌어도 Map의 키가 변하지 않도록 좌표만 복사한다.
            candidates.computeIfAbsent(new Point(target), ignored -> new ArrayList<>()).add(candidate);
        }

        @Override
        public boolean isEmpty(Point position)
        {
            return position.x >= 0 && position.y >= 0
                    && position.x < empty.length && position.y < empty.length
                    && empty[position.y][position.x];
        }

        @Override
        public int widthInCells()
        {
            return empty.length;
        }
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
