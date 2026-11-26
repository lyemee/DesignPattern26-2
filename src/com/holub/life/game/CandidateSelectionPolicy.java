package com.holub.life.game;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 자동 생성 후보 중 하나를 선택하는 교체 가능한 정책.
 * 클릭 보호, 빈 칸 판단, 일반 탄생·꽃 번식 후보 수집은 호출 측 책임이며,
 * 사용자 클릭은 후보 목록에 포함하지 않는다.
 *
 * @param <T> 식물 구현에 의존하지 않는 후보 타입
 */
public interface CandidateSelectionPolicy<T>
{
    /**
     * 빈 목록이면 빈 결과를, 그 외에는 목록에 있는 원본 후보 하나를 반환한다.
     * 목록의 순서·내용과 후보 객체를 변경하지 않는다.
     * 모든 입력 검증은 난수 소비 전에 완료한다.
     *
     * @param candidates 호출 측에서 수집한 자동 생성 후보 목록
     * @param random 호출 측에서 제공하는 난수
     * @return 선택된 원본 후보 또는 빈 결과
     * @throws NullPointerException 목록, 난수 또는 목록 안의 항목이 null인 경우
     */
    Optional<T> select(List<T> candidates, Random random);
}
