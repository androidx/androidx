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

package androidx.benchmark

import android.os.Build
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeviceInfoTest {
    @Before
    fun setup() {
        // clear any process wide warnings, since tests below will capture them
        InstrumentationResults.clearIdeWarningPrefix()
    }

    @SdkSuppress(minSdkVersion = PerfettoHelper.MIN_SDK_VERSION)
    @Test
    fun misconfiguredForTracing() {
        // NOTE: tests device capability, not implementation of DeviceInfo
        assertFalse(
            DeviceInfo.misconfiguredForTracing,
            "${DeviceInfo.typeLabel} is incorrectly configured for tracing," +
                " and is not CTS compatible. All Perfetto/Atrace capture will fail."
        )
    }

    @Test
    fun artMainlineVersionWembley() { // specific regression test for b/319541718
        assumeTrue(Build.DEVICE.startsWith("wembley"))

        // Double checks some special properties of go devices
        assertTrue(DeviceInfo.isLowRamDevice)

        // Wembley available versions don't hit any of the method tracing issues, no art mainline
        assertFalse(DeviceInfo.methodTracingAffectsMeasurements)
        assertEquals(DeviceInfo.ART_MAINLINE_VERSION_UNDETECTED, DeviceInfo.artMainlineVersion)
    }

    @Test
    fun artMainlineVersion() =
        validateArtMainlineVersion(artMainlineVersion = DeviceInfo.artMainlineVersion)
}

/**
 * Shared logic for art mainline version validation, also used by
 * [androidx.benchmark.json.BenchmarkDataTest.artMainlineVersion]
 */
fun validateArtMainlineVersion(artMainlineVersion: Long?) {
    // bypass main test if appear to be on go device without art mainline module
    if (Build.VERSION.SDK_INT in 31..33 && DeviceInfo.isLowRamDevice) {
        if (artMainlineVersion == DeviceInfo.ART_MAINLINE_VERSION_UNDETECTED) {
            return // bypass rest of test, appear to be on go device
        }
    }

    if (Build.VERSION.SDK_INT >= 30) {
        // validate we have a reasonable looking number
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(
                artMainlineVersion!! > 300000000,
                "observed $artMainlineVersion, expected over 300000000"
            )
        } else {
            assertEquals(1, artMainlineVersion)
        }
        // validate parsing by checking against shell command,
        // which we don't use at runtime due to cost of shell commands
        val shellVersion =
            Shell.executeCommandCaptureStdoutOnly(
                    "cmd package list packages --show-versioncode --apex-only art"
                )
                .trim()

        // "google" and "go" may or may not be present in package
        val expectedRegExStr =
            "package:com(\\.google)?\\.android(\\.go)?\\.art" + " versionCode:$artMainlineVersion"
        assertTrue(
            expectedRegExStr.toRegex().matches(shellVersion),
            "Expected shell version ($shellVersion) to match $expectedRegExStr"
        )
    } else {
        assertEquals(DeviceInfo.ART_MAINLINE_VERSION_UNDETECTED, artMainlineVersion)
    }
}
