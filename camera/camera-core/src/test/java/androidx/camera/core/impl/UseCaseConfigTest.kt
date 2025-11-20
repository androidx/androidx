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
package androidx.camera.core.impl

import android.util.Range
import android.util.Size
import androidx.camera.core.impl.FrameRates.FRAME_RATE_UNLIMITED
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@Config(sdk = [Config.ALL_SDKS])
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class UseCaseConfigTest {
    @Test
    fun canGetSessionType() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        val sessionType = SESSION_TYPE_HIGH_SPEED
        useCaseBuilder.mutableConfig.insertOption(UseCaseConfig.OPTION_SESSION_TYPE, sessionType)
        Truth.assertThat(useCaseBuilder.useCaseConfig.sessionType).isEqualTo(sessionType)
    }

    @Test
    fun canGetTargetFrameRate() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        val range = Range(10, 20)
        useCaseBuilder.mutableConfig.insertOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, range)
        Truth.assertThat(useCaseBuilder.useCaseConfig.targetFrameRate).isEqualTo(range)
    }

    @Test
    fun canGetResolutionToMaxFrameRates() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        val sizeToMaxFpsMap: Map<Size, Int> = mapOf(RESOLUTION_1080P to 30, RESOLUTION_720P to 60)
        useCaseBuilder.mutableConfig.insertOption(
            UseCaseConfig.OPTION_RESOLUTION_TO_MAX_FRAME_RATES,
            sizeToMaxFpsMap,
        )
        Truth.assertThat(useCaseBuilder.useCaseConfig.getCustomMaxFrameRate(RESOLUTION_1080P))
            .isEqualTo(30)
        Truth.assertThat(useCaseBuilder.useCaseConfig.getCustomMaxFrameRate(RESOLUTION_720P))
            .isEqualTo(60)
        Truth.assertThat(useCaseBuilder.useCaseConfig.getCustomMaxFrameRate(Size(100, 100)))
            .isEqualTo(FRAME_RATE_UNLIMITED)
    }

    @Test
    fun canGetIsZslDisabled() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        useCaseBuilder.mutableConfig.insertOption(UseCaseConfig.OPTION_ZSL_DISABLED, true)
        Truth.assertThat(useCaseBuilder.useCaseConfig.isZslDisabled(false)).isTrue()
    }
}
