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

package androidx.work

import androidx.work.WorkInfo.State.RUNNING
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WorkInfoTest {

    @Test
    fun testEqualityWithGeneration() {
        val id = UUID.randomUUID()
        val info1 = WorkInfo(id = id, state = RUNNING, tags = setOf("a"), generation = 1)
        val info2 = WorkInfo(id = id, state = RUNNING, tags = setOf("a"), generation = 4)
        assertThat(info1 == info2).isFalse()
    }

    @Test
    fun testEqualityWithConstraints() {
        val id = UUID.randomUUID()
        val info1 = WorkInfo(
            id = id,
            state = RUNNING,
            tags = setOf("a"),
            constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
        )
        val info2 = WorkInfo(
            id = id,
            state = RUNNING,
            tags = setOf("a"),
            constraints = Constraints(requiredNetworkType = NetworkType.NOT_ROAMING)
        )
        assertThat(info1 == info2).isFalse()
    }

    @Test
    fun testEqualityWithStopReason() {
        val id = UUID.randomUUID()
        val info1 = WorkInfo(id = id, state = RUNNING, tags = setOf("a"), generation = 1)
        val info2 = WorkInfo(
            id = id,
            state = RUNNING,
            tags = setOf("a"),
            stopReason = WorkInfo.STOP_REASON_UNKNOWN
        )
        assertThat(info1 == info2).isFalse()
    }
}
