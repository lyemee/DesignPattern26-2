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
    private final CandidateSelectionPolicy<Point> positionPolicy = new UniformSelectionPolicy<>();

    //기존 코드: public boolean nextGeneration(Cell board) { ① 계산 → 변화 없으면 false → ③ 반영 }
    // -> 반환값을 GenerationResult로(수확 목록 추가), 번식 배치용 Random을 받도록 변경

    //한 세대 = ① 계산 → ② 수집과 번식 배치 → ③ 반영
    public GenerationResult nextGeneration(Cell board, Random random)
    {
        return advance(board, random, null);
    }

    //호출 측이 잡초를 생성할 tick에만 사용. Factory가 만든 잡초도 자동 생성 후보로 경쟁한다.
    public GenerationResult nextGeneration(Cell board, Random random, PlantFactory weedFactory)
    {
        Objects.requireNonNull(weedFactory, "weedFactory");
        return advance(board, random, weedFactory);
    }

    private GenerationResult advance(Cell board, Random random, PlantFactory weedFactory)
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

        //기존 셀에 변화가 없어도 잡초 요청이 있으면 빈 칸을 수집한다. 빈 보드도 생성 가능.
        if (!needsTransition && weedFactory == null)
            return GenerationResult.UNCHANGED;

        // ② 모든 셀의 현재 점유 여부와 계산 결과를 먼저 수집. 현재 값은 ③ 전까지 유지된다.
        GenerationSnapshot snapshot = new GenerationSnapshot(board.widthInCells());
        board.collect(0, 0, snapshot);

        //최종 요구사항 「번식 구현 / 3. 씨 뿌리기 / 공통 규칙」: 현재 죽어 있는 칸만 대상.
        //모든 꽃은 같은 현재 빈 칸 스냅샷을 조회. 후보 수집 중에는 보드를 변경하지 않는다.
        //해바라기·민들레 예외 없이 적용하며, 다음 생존 상태를 이용한 빈 칸 재계산은 하지 않는다.
        for (SeedingRequest request : snapshot.requests)
            for (Point target : request.plant().seedTargets(
                    new Point(request.column(), request.row()), snapshot, random))
                if (snapshot.isEmpty(target))
                    snapshot.addCandidate(target, new Candidate(request.plant(), false));

        //현재 빈 칸 하나를 균등 선택한 뒤 후보에만 추가. 기존 후보를 지우거나 즉시 심지 않는다.
        if (weedFactory != null)
            positionPolicy.select(snapshot.emptyPositions, random).ifPresent(target -> {
                Plant weed = Objects.requireNonNull(weedFactory.createPlant(), "weed");
                snapshot.addCandidate(target, new Candidate(weed, false));
            });

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

        //안정 상태이고 씨앗·수확 이벤트도 없으면 반영하거나 전달할 결과가 없다.
        if (!needsTransition && !seeded && snapshot.harvested.isEmpty())
            return GenerationResult.UNCHANGED;

        // ③ 계산이 끝난 다음 상태를 반영
        boolean changed = board.transition();
        //transition이 셀의 신호를 지워도 수집한 이벤트는 유지한다. 원본 Plant와 중복은 보존한다.
        return new GenerationResult(changed || seeded, List.copyOf(snapshot.harvested));
    }

    //born은 출처별 우선순위가 아니라, 기존 탄생 계산 결과를 보존하기 위한 구분이다.
    private record Candidate(Plant plant, boolean born) {}
    private record SeedingRequest(int row, int column, Plant plant) {}

    //한 세대만 사용하는 읽기 전용 현재 빈 칸 조회. 후보와 요청은 collect 완료 후 처리한다.
    private static final class GenerationSnapshot implements CellReport, SeedField
    {
        private final boolean[][] empty;
        private final List<Point> emptyPositions = new ArrayList<>();
        private final List<SeedingRequest> requests = new ArrayList<>();
        private final List<Plant> harvested = new ArrayList<>();
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
            if (!alive)
                emptyPositions.add(new Point(column, row));
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

        @Override
        public void harvested(Plant plant)
        {
            harvested.add(Objects.requireNonNull(plant, "harvested plant"));
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

}
