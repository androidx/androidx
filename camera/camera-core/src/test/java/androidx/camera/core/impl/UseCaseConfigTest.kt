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

import android.os.Build
import android.util.Range
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(
    RobolectricTestRunner::class
)
@DoNotInstrument
class UseCaseConfigTest {
    @Test
    fun canGetTargetFrameRate() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        val range = Range(10, 20)
        useCaseBuilder.mutableConfig.insertOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, range)
        Truth.assertThat(useCaseBuilder.useCaseConfig.targetFrameRate).isEqualTo(range)
    }

    @Test
    fun canGetIsZslDisabled() {
        val useCaseBuilder = FakeUseCaseConfig.Builder()
        useCaseBuilder.mutableConfig.insertOption(UseCaseConfig.OPTION_ZSL_DISABLED, true)
        Truth.assertThat(useCaseBuilder.useCaseConfig.isZslDisabled(false)).isTrue()
    }
}
