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

package androidx.compose.animation.benchmark

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
class SharedElementBenchmark(private val count: Int) {
    companion object {
        @JvmStatic @Parameters(name = "count={0}") fun data() = listOf(1, 10)
    }

    @get:Rule val rule = ComposeBenchmarkRule()

    /**
     * Measures the instantiation cost of a screen with Shared Elements with caller managed
     * visibility.
     */
    @Test
    fun instantiateSharedElementCallerManagedVisibility() {
        rule.benchmarkFirstCompose { SharedElementCallerManagedVisibilityTestCase(count) }
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with caller managed
     * visibility.
     */
    @Test
    fun recomposeSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkRecompose(
            { SharedElementCallerManagedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the Layout cost during a Shared Element transition with caller managed visibility.
     */
    @Test
    fun layoutSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkLayout(
            { SharedElementCallerManagedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with caller managed visibility. */
    @Test
    fun drawSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkDraw(
            { SharedElementCallerManagedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the instantiation cost of a screen with Shared Elements using AnimatedVisibility.
     */
    @Test
    fun instantiateSharedElementAnimatedVisibility() {
        rule.benchmarkFirstCompose { SharedElementAnimatedVisibilityTestCase(count) }
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with
     * AnimatedVisibility.
     */
    @Test
    fun recomposeSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkRecompose(
            { SharedElementAnimatedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the Layout cost during a Shared Element transition with AnimatedVisibility. */
    @Test
    fun layoutSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkLayout(
            { SharedElementAnimatedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with AnimatedVisibility. */
    @Test
    fun drawSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkDraw(
            { SharedElementAnimatedVisibilityTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the instantiation cost of a screen with Shared Elements using AnimatedContent. */
    @Test
    fun instantiateSharedElementAnimatedContent() {
        rule.benchmarkFirstCompose { SharedElementAnimatedContentTestCase(count) }
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with AnimatedContent.
     */
    @Test
    fun recomposeSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkRecompose(
            { SharedElementAnimatedContentTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the Layout cost during a Shared Element transition with AnimatedContent. */
    @Test
    fun layoutSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkLayout(
            { SharedElementAnimatedContentTestCase(count) },
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with AnimatedContent. */
    @Test
    fun drawSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkDraw(
            { SharedElementAnimatedContentTestCase(count) },
            assertOneRecomposition = false,
        )
    }
}

@Composable
private fun Modifier.sharedElements(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    count: Int,
): Modifier =
    with(sharedTransitionScope) {
        var result = this@sharedElements
        repeat(count) {
            result =
                result.sharedElement(
                    rememberSharedContentState(key = "key_$it"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
        }
        result
    }

@Composable
private fun Modifier.sharedElements(
    sharedTransitionScope: SharedTransitionScope,
    isExpanded: Boolean,
    count: Int,
): Modifier =
    with(sharedTransitionScope) {
        var result = this@sharedElements
        repeat(count) {
            result =
                result.sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(key = "key_$it"),
                    isExpanded,
                )
        }
        result
    }

private class SharedElementAnimatedVisibilityTestCase(val modifierCount: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var isExpanded by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        SharedTransitionLayout {
            AnimatedVisibility(
                visible = isExpanded,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                Box(
                    Modifier.offset(100.dp, 100.dp)
                        .size(200.dp)
                        .sharedElements(
                            this@SharedTransitionLayout,
                            this@AnimatedVisibility,
                            modifierCount,
                        )
                        .background(Color.Red)
                )
            }

            AnimatedVisibility(
                visible = !isExpanded,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                Box(
                    Modifier.offset(0.dp, 0.dp)
                        .size(50.dp)
                        .sharedElements(
                            this@SharedTransitionLayout,
                            this@AnimatedVisibility,
                            modifierCount,
                        )
                        .background(Color.Red)
                )
            }
        }
    }

    override fun toggleState() {
        isExpanded = !isExpanded
    }
}

private class SharedElementAnimatedContentTestCase(val modifierCount: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var isExpanded by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        SharedTransitionLayout {
            AnimatedContent(targetState = isExpanded, label = "SharedElementAnim") { targetExpanded
                ->
                if (targetExpanded) {
                    Box(
                        Modifier.offset(100.dp, 100.dp)
                            .size(200.dp)
                            .sharedElements(
                                this@SharedTransitionLayout,
                                this@AnimatedContent,
                                modifierCount,
                            )
                            .background(Color.Red)
                    )
                } else {
                    Box(
                        Modifier.offset(0.dp, 0.dp)
                            .size(50.dp)
                            .sharedElements(
                                this@SharedTransitionLayout,
                                this@AnimatedContent,
                                modifierCount,
                            )
                            .background(Color.Red)
                    )
                }
            }
        }
    }

    override fun toggleState() {
        isExpanded = !isExpanded
    }
}

private class SharedElementCallerManagedVisibilityTestCase(val modifierCount: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var isExpanded by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        SharedTransitionLayout {
            Box(
                Modifier.offset(0.dp, 0.dp)
                    .size(50.dp)
                    .sharedElements(this@SharedTransitionLayout, isExpanded, modifierCount)
                    .background(Color.Red)
            )

            Box(
                Modifier.offset(100.dp, 100.dp)
                    .size(200.dp)
                    .sharedElements(this@SharedTransitionLayout, !isExpanded, modifierCount)
                    .background(Color.Red)
            )
        }
    }

    override fun toggleState() {
        isExpanded = !isExpanded
    }
}
