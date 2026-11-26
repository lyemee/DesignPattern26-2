package com.holub.life.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class UniformSelectionPolicyTest
{
    private final CandidateSelectionPolicy<Candidate> policy = new UniformSelectionPolicy<>();

    @Test
    void emptyListReturnsEmptyWithoutConsumingRandom()
    {
        ControlledRandom random = new ControlledRandom(0);

        assertTrue(policy.select(List.of(), random).isEmpty());
        assertEquals(0, random.calls);
    }

    @Test
    void singleCandidateReturnsSameObjectWithoutConsumingRandom()
    {
        Candidate candidate = new Candidate("A", 1);
        ControlledRandom random = new ControlledRandom(0);

        assertSame(candidate, policy.select(List.of(candidate), random).orElseThrow());
        assertEquals(0, random.calls);
    }

    @Test
    void everyIndexIsSelectableWithExactlyOneBoundedRandomCall()
    {
        List<Candidate> candidates = List.of(
                new Candidate("A", 1), new Candidate("B", 2),
                new Candidate("C", 3), new Candidate("D", 4));

        for (int index = 0; index < candidates.size(); index++)
        {
            ControlledRandom random = new ControlledRandom(index);

            assertSame(candidates.get(index), policy.select(candidates, random).orElseThrow());
            assertEquals(candidates.size(), random.bound);
            assertEquals(1, random.calls);
        }
    }

    @Test
    void sameKindCandidatesRemainSeparateEntries()
    {
        Candidate a1 = new Candidate("A", 1);
        Candidate a2 = new Candidate("A", 2);
        Candidate b1 = new Candidate("B", 1);
        List<Candidate> candidates = List.of(a1, a2, b1);

        assertNotSame(a1, a2);
        assertEquals(a1.kind, a2.kind);
        for (int index = 0; index < candidates.size(); index++)
        {
            ControlledRandom random = new ControlledRandom(index);

            assertSame(candidates.get(index), policy.select(candidates, random).orElseThrow());
            assertEquals(3, random.bound);
            assertEquals(1, random.calls);
        }
    }

    @Test
    void repeatedReferenceIsNotDeduplicated()
    {
        Candidate a = new Candidate("A", 1);
        Candidate b = new Candidate("B", 1);
        List<Candidate> candidates = List.of(a, a, b);

        for (int index = 0; index < candidates.size(); index++)
        {
            ControlledRandom random = new ControlledRandom(index);

            assertSame(candidates.get(index), policy.select(candidates, random).orElseThrow());
            assertEquals(3, random.bound);
            assertEquals(1, random.calls);
        }
    }

    @Test
    void selectionDoesNotChangeListOrCandidateState()
    {
        List<Candidate> candidates = new ArrayList<>(List.of(
                new Candidate("A", 10), new Candidate("B", 20), new Candidate("C", 30)));
        List<Candidate> original = new ArrayList<>(candidates);

        for (int index = 0; index < original.size(); index++)
            assertSame(original.get(index),
                    policy.select(candidates, new ControlledRandom(index)).orElseThrow());

        assertEquals(original.size(), candidates.size());
        for (int index = 0; index < original.size(); index++)
        {
            assertSame(original.get(index), candidates.get(index));
            assertEquals(List.of("A", "B", "C").get(index), candidates.get(index).kind);
            assertEquals((index + 1) * 10, candidates.get(index).state);
        }
    }

    @Test
    void unmodifiableListIsAccepted()
    {
        Candidate a = new Candidate("A", 1);
        Candidate b = new Candidate("B", 2);
        List<Candidate> candidates = List.of(a, b);
        ControlledRandom random = new ControlledRandom(1);

        assertSame(b, policy.select(candidates, random).orElseThrow());
        assertEquals(List.of(a, b), candidates);
        assertEquals(2, random.bound);
        assertEquals(1, random.calls);
    }

    @Test
    void nullListIsRejectedWithoutConsumingRandom()
    {
        ControlledRandom random = new ControlledRandom(0);

        assertThrows(NullPointerException.class, () -> policy.select(null, random));
        assertEquals(0, random.calls);
    }

    @Test
    void nullRandomIsRejectedEvenForEmptyOrSingleCandidateList()
    {
        Candidate a = new Candidate("A", 1);
        Candidate b = new Candidate("B", 2);

        assertThrows(NullPointerException.class, () -> policy.select(List.of(), null));
        assertThrows(NullPointerException.class, () -> policy.select(List.of(a), null));
        assertThrows(NullPointerException.class, () -> policy.select(List.of(a, b), null));
        assertThrows(NullPointerException.class, () -> policy.select(null, null));
    }

    @Test
    void nullSingleCandidateIsRejectedWithoutConsumingRandom()
    {
        ControlledRandom random = new ControlledRandom(0);
        List<Candidate> candidates = Arrays.asList((Candidate) null);

        assertThrows(NullPointerException.class, () -> policy.select(candidates, random));
        assertEquals(0, random.calls);
    }

    @Test
    void nullAtEveryPositionIsRejectedBeforeAnyRandomCall()
    {
        Candidate a = new Candidate("A", 1);
        Candidate b = new Candidate("B", 2);
        List<List<Candidate>> invalidLists = List.of(
                Arrays.asList(null, a, b),
                Arrays.asList(a, null, b),
                Arrays.asList(a, b, null));

        for (List<Candidate> candidates : invalidLists)
        {
            ControlledRandom random = new ControlledRandom(0);

            assertThrows(NullPointerException.class, () -> policy.select(candidates, random));
            assertEquals(0, random.calls);
        }
    }

    @Test
    void separateRandomsWithSameSeedProduceSameSelectionSequence()
    {
        Random first = new Random(42);
        Random second = new Random(42);
        Candidate a1 = new Candidate("A", 1);
        Candidate a2 = new Candidate("A", 2);
        Candidate b1 = new Candidate("B", 1);
        List<List<Candidate>> inputs = List.of(
                List.of(), List.of(a1), List.of(a1, a2), List.of(a1, a2, b1), List.of(b1, a1, a1));

        for (int round = 0; round < 20; round++)
            for (List<Candidate> candidates : inputs)
                assertEquals(policy.select(candidates, first), policy.select(candidates, second));
    }

    // 객체의 종류와 상태를 변경하지 않는지 검증하기 위한 가변 테스트 후보.
    private static final class Candidate
    {
        private String kind;
        private int state;

        private Candidate(String kind, int state)
        {
            this.kind = kind;
            this.state = state;
        }
    }

    private static final class ControlledRandom extends Random
    {
        private final int index;
        private int calls;
        private int bound;

        private ControlledRandom(int index)
        {
            this.index = index;
        }

        @Override
        public int nextInt(int bound)
        {
            calls++;
            this.bound = bound;
            assertTrue(index >= 0 && index < bound);
            return index;
        }

        @Override
        protected int next(int bits)
        {
            throw new AssertionError("nextInt(bound) 이외의 난수 소비는 허용하지 않음");
        }
    }
}
