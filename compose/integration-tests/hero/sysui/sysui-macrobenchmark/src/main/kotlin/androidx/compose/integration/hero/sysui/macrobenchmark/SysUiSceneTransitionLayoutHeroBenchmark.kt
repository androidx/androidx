/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SysUiSceneTransitionLayoutHeroBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun lockscreenToShade() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.SHADE_SCENE,
            direction = Direction.DOWN,
        )
    }

    @Test
    fun shadeToQuickSettings() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.SHADE_SCENE,
            toContent = StlDemoConstants.QUICK_SETTINGS_SCENE,
            direction = Direction.DOWN,
        )
    }

    @Test
    fun lockscreenToNotificationsOverlay() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.NOTIFICATIONS_OVERLAY,
            direction = Direction.DOWN,
            toContentIsOverlay = true,
            swipeOn = StlDemoConstants.STL_START_HALF_SELECTOR,
        )
    }

    @Test
    fun lockscreenToQuickSettingsOverlay() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.QUICK_SETTINGS_OVERLAY,
            direction = Direction.DOWN,
            toContentIsOverlay = true,
            swipeOn = StlDemoConstants.STL_END_HALF_SELECTOR,
        )
    }

    private fun benchmarkSwipeFromScene(
        fromScene: String,
        toContent: String,
        direction: Direction,
        toContentIsOverlay: Boolean = false,
        swipeOn: BySelector? = null,
    ) {
        benchmarkRule.measureRepeated(
            packageName = StlDemoConstants.PACKAGE,
            metrics = defaultComposeScrollingMetrics() + AndroidBlockingCallsMetric(),
            iterations = ITERATIONS,
            compilationMode = compilationMode,
            setupBlock = {
                sysuiHeroBenchmarkScope()
                    .setupSwipeFromScene(fromScene, toContent, toContentIsOverlay)
            },
        ) {
            swipeFromScene(fromScene, toContent, direction, toContentIsOverlay, swipeOn)
        }
    }

    companion object {
        const val ITERATIONS = 5

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = createCompilationParams()
    }
}
