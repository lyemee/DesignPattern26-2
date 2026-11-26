package com.holub.life.plant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//임시 성장 단계 테스트
class TemporaryStageTest
{
    @Test
    @DisplayName("나무·열매나무만 시들 수 있는 단계")
    void onlyTreesCanWither()
    {
        for (TemporaryStage stage : TemporaryStage.values())
        {
            boolean expected = stage == TemporaryStage.TREE || stage == TemporaryStage.FRUIT;
            assertEquals(expected, stage.canWither(), stage.name());
        }
    }
}
