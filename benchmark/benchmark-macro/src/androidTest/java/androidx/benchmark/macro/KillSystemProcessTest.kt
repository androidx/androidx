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

import android.annotation.SuppressLint
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.text.SimpleDateFormat
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@MediumTest
class KillSystemProcessTest {

    @Test
    @SdkSuppress(minSdkVersion = 30) // getHistoricalExitReasons was introduced in API 30.
    fun killSystemUiTest() {
        // Don't run these tests on an emulator
        assumeTrue(DeviceInfo.isRooted && !DeviceInfo.isEmulator)
        val scope = MacrobenchmarkScope(
            packageName = SYSTEM_UI,
            launchWithClearTask = true
        )
        assertTrue { Shell.isPackageAlive(scope.packageName) }
        // Look at the last kill exit record and keep track of that.
        val start = applicationExitTimestamps(packageName = scope.packageName).maxOrNull() ?: 0L
        scope.killProcess()
        // Wait for some time for the book-keeping to be complete
        @Suppress("BanThreadSleep")
        Thread.sleep(DELAY)
        assertTrue(
            // Here we want to make sure that there is at least one new timestamp
            // more recent than the last ones we looked at.
            applicationExitTimestamps(packageName = scope.packageName)
                .any { it >= start }
        )
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"

        private const val DELAY = 1_000L

        private const val TIMESTAMP_START_MARKER = "timestamp="

        private const val TIMESTAMP_END_MARKER = "pid="

        @SuppressLint("SimpleDateFormat")
        private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

        fun applicationExitTimestamps(packageName: String): List<Long> {

            // Last Timestamp of Persistence Into Persistent Storage: 2024-01-22 22:23:41.974
            //  package: com.android.systemui
            //    Historical Process Exit for uid=10085
            //        ApplicationExitInfo #0:
            //          timestamp=2024-01-23 00:54:34.671 pid=1967 realUid=10085 packageUid=10085

            val output = Shell.executeScriptCaptureStdoutStderr(
                "dumpsys activity exit-info $packageName"
            )
            require(output.stderr.isBlank())
            return output.stdout.lines().mapNotNull { line ->
                val start = line.indexOf(TIMESTAMP_START_MARKER)
                val end = line.indexOf(TIMESTAMP_END_MARKER, startIndex = start)
                if (start >= 0 && end >= 0) {
                    val timestamp = line.substringAfter(TIMESTAMP_START_MARKER)
                        .substringBefore(TIMESTAMP_END_MARKER)
                        .trim()

                    TIMESTAMP_FORMAT.parse(timestamp)?.time
                } else {
                    null
                }
            }
        }
    }
}
