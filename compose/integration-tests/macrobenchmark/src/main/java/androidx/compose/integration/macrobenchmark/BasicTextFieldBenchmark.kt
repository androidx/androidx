/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.testutils.findObjectOrThrow
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test

@LargeTest
class BasicTextFieldBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun openKeyboardWithBasicTextFieldStartupBenchmark() {
        benchmarkRule.measureStartup(
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD,
            packageName = PACKAGE_NAME,
            setupIntent = { action = ACTION },
            waitForContent = {
                device.waitForIdle()
                device.findObjectOrThrow(By.desc("IME_ANIMATION_DONE"), timeoutMillis = 10_000L)
            },
        )
    }

    @Test
    fun openKeyboardWithBasicTextFieldStartupBenchmark_ContentDataTypeNone() {
        benchmarkRule.measureStartup(
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD,
            packageName = PACKAGE_NAME,
            setupIntent = {
                action = ACTION
                putExtra("CONTENT_TYPE", "NONE")
            },
            waitForContent = {
                device.waitForIdle()
                device.findObjectOrThrow(By.desc("IME_ANIMATION_DONE"), timeoutMillis = 10_000L)
            },
        )
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.macrobenchmark.target"
        private const val ACTION = "androidx.compose.integration.macrobenchmark.target.BTF_ACTIVITY"
    }
}
