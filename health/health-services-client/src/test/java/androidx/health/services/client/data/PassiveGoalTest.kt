/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.health.services.client.data

import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN
import androidx.health.services.client.data.DataType.Companion.STEPS
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.proto.DataProto
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PassiveGoalTest {
    @Test
    fun protoRoundTrip() {
        val proto = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN)).proto

        val goal = PassiveGoal(proto)

        assertThat(goal.dataTypeCondition.dataType).isEqualTo(STEPS_DAILY)
        assertThat(goal.dataTypeCondition.threshold).isEqualTo(400)
        assertThat(goal.dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(goal.triggerFrequency).isEqualTo(PassiveGoal.TriggerFrequency.REPEATED)
    }

    @Test
    fun shouldEqual() {
        val goal1 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN))
        val goal2 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN))

        assertThat(goal1).isEqualTo(goal2)
    }

    @Test
    fun shouldNotEqual_differentTriggerFrequency() {
        // This case isn't expected to happen for clients, but it _could_ happen from the service
        // side for old clients. Using proto constructor because triggerFrequency constructor is
        // private.
        val goal1 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN))
        val goal2 = PassiveGoal(
            goal1.proto.toBuilder()
                .setTriggerFrequency(DataProto.PassiveGoal.TriggerFrequency.TRIGGER_FREQUENCY_ONCE)
                .build()
        )

        assertThat(goal1).isNotEqualTo(goal2)
    }

    @Test
    fun shouldNotEqual_differentThreshold() {
        val goal1 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN))
        val goal2 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 800, GREATER_THAN))

        assertThat(goal1).isNotEqualTo(goal2)
    }

    @Test
    fun shouldNotEqual_differentDataType() {
        val goal1 = PassiveGoal(DataTypeCondition(STEPS_DAILY, 400, GREATER_THAN))
        val goal2 = PassiveGoal(DataTypeCondition(STEPS, 400, GREATER_THAN))

        assertThat(goal1).isNotEqualTo(goal2)
    }
}
