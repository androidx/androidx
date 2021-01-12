/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShellTest {
    @Test
    fun optionalCommand_ls() {
        // command isn't important, it's just something that's not `echo`, and guaranteed to print
        val output = Shell.optionalCommand("ls /sys/devices/system/cpu")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertNotNull(output)
        } else {
            assertNull(output)
        }
    }

    @Test
    fun optionalCommand_echo() {
        val output = Shell.optionalCommand("echo foo")

        val expected = when {
            Build.VERSION.SDK_INT >= 23 -> "foo\n"
            // known bug in the shell on L (21,22). `echo` doesn't work with shell
            // programmatically, only works in interactive shell :|
            Build.VERSION.SDK_INT in 21..22 -> ""
            else -> null
        }

        assertEquals(expected, output)
    }

    private fun CpuInfo.CoreDir.scalingMinFreqPath() = "$path/cpufreq/scaling_min_freq"

    @Test
    fun catProcFileLong() {
        val onlineCores = CpuInfo.coreDirs.filter { it.online }

        // skip test on devices that can't read scaling_min_freq, like emulators
        assumeTrue(
            "cpufreq dirs don't have scaling_min_freq, bypassing test",
            onlineCores
                .all { File(it.scalingMinFreqPath()).exists() }
        )

        onlineCores.forEach {
            // While CpuInfo actually uses this function to read scaling_setspeed, reading
            // that value isn't enabled on all devices, so we use scaling_min_freq, which
            // is more likely to be readable
            val output = Shell.catProcFileLong(it.scalingMinFreqPath())

            // if the path exists, it should be readable by shell for every online core
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                assertNotNull(output)
            } else {
                assertNull(output)
            }
        }
    }
}
