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

package androidx.benchmark.macro

import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.benchmark.macro.MacrobenchmarkScope.KillMode
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertFalse
import kotlin.test.Ignore
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@LargeTest
class KillSystemProcessTest {

    @Test
    @Ignore("Still need to account for cases where process ids are recycled.")
    @SdkSuppress(minSdkVersion = 30)
    fun killSystemUiTest() {
        // Don't run these tests on an emulator
        assumeTrue(DeviceInfo.isRooted && !DeviceInfo.isEmulator)
        val scope = MacrobenchmarkScope(packageName = SYSTEM_UI, launchWithClearTask = true)
        assertTrue { Shell.isPackageAlive(scope.packageName) }
        // Look at the last kill exit record and keep track of that.
        val processIds = Shell.getPidsForProcess(SYSTEM_UI)
        scope.withKillMode(current = KillMode.None, override = KillMode(isKillSoftly = true)) {
            scope.killProcess()
            // Wait for some time for the book-keeping to be complete
            @Suppress("BanThreadSleep") Thread.sleep(DELAY)
            val newProcessIds = Shell.getPidsForProcess(SYSTEM_UI)
            // There should be at least one new process id.
            assertFalse(newProcessIds.any { it in processIds })
        }
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val DELAY = 1_000L
    }
}
