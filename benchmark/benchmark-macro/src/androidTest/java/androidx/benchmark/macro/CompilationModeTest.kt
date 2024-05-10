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

package androidx.benchmark.macro

import android.os.Build
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
@SmallTest
class CompilationModeTest {
    private val vmRunningInterpretedOnly: Boolean

    init {
        vmRunningInterpretedOnly = Shell.getprop("dalvik.vm.extra-opts").contains("-Xusejit:false")
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun partial() {
        assertFailsWith<IllegalArgumentException> { // can't ignore with 0 iters
            CompilationMode.Partial(BaselineProfileMode.Disable, warmupIterations = 0)
        }
        assertFailsWith<java.lang.IllegalArgumentException> { // can't set negative iters
            CompilationMode.Partial(BaselineProfileMode.Require, warmupIterations = -1)
        }
    }

    @Test
    fun names() {
        // We test these names, as they're likely built into parameterized
        // test strings, so stability/brevity are important
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertEquals("None", CompilationMode.None().toString())
            assertEquals("BaselineProfile", CompilationMode.Partial().toString())
            assertEquals(
                "WarmupProfile(iterations=3)",
                CompilationMode.Partial(BaselineProfileMode.Disable, warmupIterations = 3)
                    .toString()
            )
            assertEquals(
                "Partial(baselineProfile=Require,iterations=3)",
                CompilationMode.Partial(warmupIterations = 3).toString()
            )
            assertEquals("Full", CompilationMode.Full().toString())
        }
        assertEquals("Interpreted", CompilationMode.Interpreted.toString())
    }

    @Test
    fun isSupportedWithVmSettings_jitEnabled() {
        assumeFalse(vmRunningInterpretedOnly)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertTrue(CompilationMode.None().isSupportedWithVmSettings())
            assertTrue(CompilationMode.Partial().isSupportedWithVmSettings())
            assertTrue(CompilationMode.Full().isSupportedWithVmSettings())
        }
        assertFalse(CompilationMode.Interpreted.isSupportedWithVmSettings())
    }

    @Test
    fun isSupportedWithVmSettings_jitDisabled() {
        assumeTrue(vmRunningInterpretedOnly)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertFalse(CompilationMode.None().isSupportedWithVmSettings())
            assertFalse(CompilationMode.Partial().isSupportedWithVmSettings())
            assertFalse(CompilationMode.Full().isSupportedWithVmSettings())
        }
        assertTrue(CompilationMode.Interpreted.isSupportedWithVmSettings())
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 30, maxSdkVersion = 33)
    fun reinstallTargetPackageTest() {
        assumeFalse(DeviceInfo.isEmulator) // Don't run these tests on an emulator.
        val mode = CompilationMode.DEFAULT
        val copiedApkPaths = mode.copiedApkPaths(Packages.TARGET)
        try {
            kotlin.test.assertTrue { copiedApkPaths.isNotEmpty() }
            mode.uninstallPackage(Packages.TARGET)
            var path = Shell.pmPath(Packages.TARGET)
            kotlin.test.assertTrue { path.isEmpty() }
            mode.installPackageFromPaths(
                packageName = Packages.TARGET,
                copiedApkPaths = copiedApkPaths
            )
            path = Shell.pmPath(Packages.TARGET)
            kotlin.test.assertTrue { path.isNotEmpty() }
        } finally {
            Shell.rm(copiedApkPaths)
        }
    }
}
