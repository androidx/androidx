/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.integration.hero.sysui.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.sysui.macrobenchmark.SysUiSceneTransitionLayoutHeroBenchmark.Companion.ITERATIONS
import androidx.testutils.createStartupCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SysUiSceneTransitionLayoutStartupHeroBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode,
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun shadeStartup() {
        benchmarkRule.measureRepeated(
            packageName = StlDemoConstants.PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = ITERATIONS,
            startupMode = startupMode,
            compilationMode = compilationMode,
        ) {
            // Start the demo in the shade. This is our more busy screen because it has a bunch of
            // elements for the quick settings but also nested SceneTransitionLayouts for each
            // notification.
            sysuiHeroBenchmarkScope()
                .startDemoActivity(StlDemoConstants.SHADE_SCENE, notificationsInShade = 10)
        }
    }

    companion object {
        @Parameterized.Parameters(name = "startup={0},compilation={1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = createStartupCompilationParams()
    }
}
