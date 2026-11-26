package com.holub.life.game;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * 후보의 출처나 종류와 무관하게 목록의 각 항목을 같은 확률로 선택한다.
 * 중복 항목도 유지하므로 [A1, A2, B1]에서 각 항목은 1/3,
 * 종류 A는 합계 2/3의 확률을 가진다.
 * 후보 0~1개는 난수를 소비하지 않고, 2개 이상은 nextInt(후보 수)를 한 번 호출한다.
 */
public final class UniformSelectionPolicy<T> implements CandidateSelectionPolicy<T>
{
    @Override
    public Optional<T> select(List<T> candidates, Random random)
    {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(random, "random");
        for (T candidate : candidates)
            Objects.requireNonNull(candidate, "candidate");

        int size = candidates.size();
        if (size == 0)
            return Optional.empty();
        if (size == 1)
            return Optional.of(candidates.get(0));

        return Optional.of(candidates.get(random.nextInt(size)));
    }
}
