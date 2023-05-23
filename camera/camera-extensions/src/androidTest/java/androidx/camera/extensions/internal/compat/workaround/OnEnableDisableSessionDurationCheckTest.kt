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

import android.os.SystemClock
import androidx.camera.extensions.internal.compat.workaround.OnEnableDisableSessionDurationCheck.MIN_DURATION_FOR_ENABLE_DISABLE_SESSION
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class OnEnableDisableSessionDurationCheckTest {
    companion object {
        const val TOLERANCE = 60L
    }

    @Ignore("b/270962873")
    @Test
    fun enabled_ensureMinimalDuration() {
        // Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */true)

        val duration = 80L
        // Act
        val startTime = SystemClock.elapsedRealtime()
        check.onEnableSessionInvoked()
        Thread.sleep(duration)
        check.onDisableSessionInvoked()
        val endTime = SystemClock.elapsedRealtime()

        // Assert
        assertThat((endTime - startTime))
            .isIn(
                Range.closed(
                    MIN_DURATION_FOR_ENABLE_DISABLE_SESSION,
                    MIN_DURATION_FOR_ENABLE_DISABLE_SESSION + TOLERANCE
                ))
    }

    @Ignore("b/283351331")
    @Test
    fun enabled_doNotWaitExtraIfDurationExceeds() {
        // 1. Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */true)

        // make the duration of onEnable to onDisable to be the minimal duration.
        val duration = MIN_DURATION_FOR_ENABLE_DISABLE_SESSION

        // 2. Act
        val startTime = SystemClock.elapsedRealtime()
        check.onEnableSessionInvoked()
        // make the duration of onEnable to onDisable to be the minimal duration.
        Thread.sleep(duration)
        check.onDisableSessionInvoked()
        val endTime = SystemClock.elapsedRealtime()

        // 3. Assert: no extra time waited.
        assertThat((endTime - startTime))
            .isLessThan(
                duration + TOLERANCE
            )
    }

    @Test
    fun disabled_doNotWait() {
        // 1. Arrange
        val check = OnEnableDisableSessionDurationCheck(/* enabledMinimumDuration */ false)

        // 2. Act
        val startTime = SystemClock.elapsedRealtime()
        check.onEnableSessionInvoked()
        check.onDisableSessionInvoked()
        val endTime = SystemClock.elapsedRealtime()

        // 3. Assert
        assertThat((endTime - startTime))
            .isLessThan(TOLERANCE)
    }
}