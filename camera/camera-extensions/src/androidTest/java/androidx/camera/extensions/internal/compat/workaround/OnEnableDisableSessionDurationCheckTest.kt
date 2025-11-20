/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.workaround

import androidx.camera.extensions.internal.compat.workaround.OnEnableDisableSessionDurationCheck.MIN_DURATION_FOR_ENABLE_DISABLE_SESSION
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnEnableDisableSessionDurationCheckTest {
    companion object {
        const val TOLERANCE = 60L
    }

    @Test
    fun enabled_ensureMinimalDuration() = runBlocking {
        // Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */ true)

        val duration = 80L
        // Act
        check.onEnableSessionInvoked()
        val elapsedTime: Long
        val totalTime = measureTimeMillis {
            delay(duration)
            elapsedTime = measureTimeMillis { check.onDisableSessionInvoked() }
        }
        // |----------|--|---|
        // ^-delay           ^--totalTime
        //               ^--check.onDisableSessionInvoked()
        // Assert
        val min =
            (MIN_DURATION_FOR_ENABLE_DISABLE_SESSION - (totalTime - elapsedTime) - 2 /*tolerance*/)
                .coerceAtLeast(0)
        assertThat(elapsedTime).isAtLeast(min)
    }

    @Test
    fun enabled_doNotWaitExtraIfDurationExceeds() = runBlocking {
        // 1. Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */ true)

        // make the duration of onEnable to onDisable to be the minimal duration.
        val duration = MIN_DURATION_FOR_ENABLE_DISABLE_SESSION

        // 2. Act
        check.onEnableSessionInvoked()
        // make the duration of onEnable to onDisable to be the minimal duration.
        delay(duration)
        val elapsedTime = measureTimeMillis { check.onDisableSessionInvoked() }

        // 3. Assert: no extra time waited.
        assertThat(elapsedTime).isLessThan(TOLERANCE)
    }

    @Test
    fun disabled_doNotWait() {
        // 1. Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */ false)

        // 2. Act
        val elapsedTime = measureTimeMillis {
            check.onEnableSessionInvoked()
            check.onDisableSessionInvoked()
        }

        // 3. Assert
        assertThat(elapsedTime).isLessThan(TOLERANCE)
    }
}
