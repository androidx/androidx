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

package androidx.emoji2.integration.macrobenchmark.enabled

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.measureStartup
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmojiStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun emojiCompatInitEnabledStartup() {
        // only run this test if the device can configure emoji2
        assumeTrue(hasDiscoverableFontProviderOnDevice())
        benchmarkRule.measureStartup(
            compilationMode = CompilationMode.None,
            startupMode = StartupMode.COLD,
            packageName = "androidx.emoji2.integration.macrobenchmark.enabled.target"
        ) {
            action = "androidx.emoji2.integration.macrobenchmark.enabled.target.MAIN"
        }
    }

    private fun hasDiscoverableFontProviderOnDevice(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return DefaultEmojiCompatConfig.create(context) != null
    }
}