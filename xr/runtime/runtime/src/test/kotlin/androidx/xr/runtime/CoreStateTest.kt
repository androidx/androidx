/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CoreStateTest {

    private val timeSource = TestTimeSource()

    @Test
    fun equals_sameObject_returnsTrue() {
        val coreState = CoreState(timeMark = timeSource.markNow())

        assertThat(coreState).isEqualTo(coreState)
    }

    @Test
    fun equals_sameTimeMark_returnsTrue() {
        val timeMark = timeSource.markNow()
        val coreState1 = CoreState(timeMark = timeMark)
        val coreState2 = CoreState(timeMark = timeMark)

        assertThat(coreState1).isEqualTo(coreState2)
    }

    @Test
    fun equals_differentTimeMark_returnsFalse() {
        val coreState1 = CoreState(timeMark = timeSource.markNow())
        timeSource += 1.seconds
        val coreState2 = CoreState(timeMark = timeSource.markNow())

        assertThat(coreState1).isNotEqualTo(coreState2)
    }

    @Test
    fun equals_differentObjectType_returnsFalse() {
        val coreState = CoreState(timeMark = timeSource.markNow())
        val other = Object()

        assertThat(coreState).isNotEqualTo(other)
    }

    @Test
    fun hashCode_sameTimeMark_returnsSameHashCode() {
        val timeMark = timeSource.markNow()
        val coreState1 = CoreState(timeMark = timeMark)
        val coreState2 = CoreState(timeMark = timeMark)

        assertThat(coreState1.hashCode()).isEqualTo(coreState2.hashCode())
    }

    @Test
    fun hashCode_differentTimeMark_returnsDifferentHashCode() {
        val coreState1 = CoreState(timeMark = timeSource.markNow())
        timeSource += 1.seconds
        val coreState2 = CoreState(timeMark = timeSource.markNow())

        assertThat(coreState1.hashCode()).isNotEqualTo(coreState2.hashCode())
    }

    @Test
    fun toString_returnsCorrectString() {
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark = timeMark)

        assertThat(coreState.toString()).isEqualTo("CoreState(timeMark=$timeMark)")
    }
}
