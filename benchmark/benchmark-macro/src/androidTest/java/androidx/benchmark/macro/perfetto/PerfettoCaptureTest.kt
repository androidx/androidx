/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.macro.Packages
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoHelper.Companion.MIN_BUNDLED_SDK_VERSION
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertFailsWith
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for PerfettoCapture */
@RunWith(AndroidJUnit4::class)
class PerfettoCaptureTest {
    @SdkSuppress(maxSdkVersion = MIN_BUNDLED_SDK_VERSION - 1)
    @SmallTest
    @Test
    fun bundledNotSupported() {
        assumeTrue(isAbiSupported())

        assertFailsWith<IllegalArgumentException> { PerfettoCapture(false) }
    }

    @MediumTest
    @Test
    fun launchWouldBeCold() {
        assertFalse( // process alive, will not be cold launch
            PerfettoCapture.PerfettoSdkConfig(
                    Packages.TEST,
                    PerfettoCapture.PerfettoSdkConfig.InitialProcessState.Alive,
                )
                .launchWouldBeCold()
        )
        assertTrue( // process not alive, will be cold launch
            PerfettoCapture.PerfettoSdkConfig(
                    Packages.TEST,
                    PerfettoCapture.PerfettoSdkConfig.InitialProcessState.NotAlive,
                )
                .launchWouldBeCold()
        )
        assertFalse( // this process is alive, will not be cold launch
            PerfettoCapture.PerfettoSdkConfig(
                    Packages.TEST,
                    PerfettoCapture.PerfettoSdkConfig.InitialProcessState.Unknown,
                )
                .launchWouldBeCold()
        )
        assertTrue( // this process doesn't exist, will be cold launch
            PerfettoCapture.PerfettoSdkConfig(
                    Packages.MISSING,
                    PerfettoCapture.PerfettoSdkConfig.InitialProcessState.Unknown,
                )
                .launchWouldBeCold()
        )
    }
}
