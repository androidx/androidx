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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
public class CompilationModeTest {
    private val vmRunningInterpretedOnly: Boolean

    init {
        val device = InstrumentationRegistry.getInstrumentation().device()
        val getProp = device.executeShellCommand("getprop dalvik.vm.extra-opts")
        vmRunningInterpretedOnly = getProp.contains("-Xusejit:false")
    }

    @Test
    public fun names() {
        // We test these names, as they're likely built into parameterized
        // test strings, so stability/brevity are important
        assertEquals("None", CompilationMode.None.toString())
        assertEquals("SpeedProfile(iterations=123)", CompilationMode.SpeedProfile(123).toString())
        assertEquals("Speed", CompilationMode.Speed.toString())
        assertEquals("Interpreted", CompilationMode.Interpreted.toString())
    }

    @Test
    public fun isSupportedWithVmSettings_jitEnabled() {
        assumeFalse(vmRunningInterpretedOnly)

        assertTrue(CompilationMode.None.isSupportedWithVmSettings())
        assertTrue(CompilationMode.SpeedProfile().isSupportedWithVmSettings())
        assertTrue(CompilationMode.Speed.isSupportedWithVmSettings())
        assertFalse(CompilationMode.Interpreted.isSupportedWithVmSettings())
    }

    @Test
    public fun isSupportedWithVmSettings_jitDisabled() {
        assumeTrue(vmRunningInterpretedOnly)

        assertFalse(CompilationMode.None.isSupportedWithVmSettings())
        assertFalse(CompilationMode.SpeedProfile().isSupportedWithVmSettings())
        assertFalse(CompilationMode.Speed.isSupportedWithVmSettings())
        assertTrue(CompilationMode.Interpreted.isSupportedWithVmSettings())
    }
}
