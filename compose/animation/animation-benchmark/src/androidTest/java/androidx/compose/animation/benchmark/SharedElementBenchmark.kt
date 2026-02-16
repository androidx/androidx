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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedElementBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    /**
     * Measures the instantiation cost of a screen with Shared Elements with caller managed
     * visibility.
     */
    @Test
    fun instantiateSharedElementCallerManagedVisibility() {
        rule.benchmarkFirstCompose(::SharedElementCallerManagedVisibilityTestCase)
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with caller managed
     * visibility.
     */
    @Test
    fun recomposeSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkRecompose(
            ::SharedElementCallerManagedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the Layout cost during a Shared Element transition with caller managed visibility.
     */
    @Test
    fun layoutSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkLayout(
            ::SharedElementCallerManagedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with caller managed visibility. */
    @Test
    fun drawSharedElementCallerManagedVisibility() {
        rule.toggleStateBenchmarkDraw(
            ::SharedElementCallerManagedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the instantiation cost of a screen with Shared Elements using AnimatedVisibility.
     */
    @Test
    fun instantiateSharedElementAnimatedVisibility() {
        rule.benchmarkFirstCompose(::SharedElementAnimatedVisibilityTestCase)
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with
     * AnimatedVisibility.
     */
    @Test
    fun recomposeSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkRecompose(
            ::SharedElementAnimatedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the Layout cost during a Shared Element transition with AnimatedVisibility. */
    @Test
    fun layoutSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkLayout(
            ::SharedElementAnimatedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with AnimatedVisibility. */
    @Test
    fun drawSharedElementAnimatedVisibility() {
        rule.toggleStateBenchmarkDraw(
            ::SharedElementAnimatedVisibilityTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the instantiation cost of a screen with Shared Elements using AnimatedContent. */
    @Test
    fun instantiateSharedElementAnimatedContent() {
        rule.benchmarkFirstCompose(::SharedElementAnimatedContentTestCase)
    }

    /**
     * Measures the cost of Recomposition during a Shared Element transition with AnimatedContent.
     */
    @Test
    fun recomposeSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkRecompose(
            ::SharedElementAnimatedContentTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the Layout cost during a Shared Element transition with AnimatedContent. */
    @Test
    fun layoutSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkLayout(
            ::SharedElementAnimatedContentTestCase,
            assertOneRecomposition = false,
        )
    }

    /** Measures the Draw cost during a Shared Element transition with AnimatedContent. */
    @Test
    fun drawSharedElementAnimatedContent() {
        rule.toggleStateBenchmarkDraw(
            ::SharedElementAnimatedContentTestCase,
            assertOneRecomposition = false,
        )
    }
}

private class SharedElementAnimatedVisibilityTestCase :
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
                        .sharedElement(
                            rememberSharedContentState(key = "key"),
                            animatedVisibilityScope = this,
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
                        .sharedElement(
                            rememberSharedContentState(key = "key"),
                            animatedVisibilityScope = this,
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

private class SharedElementAnimatedContentTestCase : LayeredComposeTestCase(), ToggleableTestCase {
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
                            .sharedElement(
                                rememberSharedContentState(key = "key"),
                                animatedVisibilityScope = this,
                            )
                            .background(Color.Red)
                    )
                } else {
                    Box(
                        Modifier.offset(0.dp, 0.dp)
                            .size(50.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "key"),
                                animatedVisibilityScope = this,
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

private class SharedElementCallerManagedVisibilityTestCase :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var isExpanded by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        SharedTransitionLayout {
            Box(
                Modifier.offset(0.dp, 0.dp)
                    .size(50.dp)
                    .sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(key = "key"),
                        visible = !isExpanded,
                    )
                    .background(Color.Red)
            )

            Box(
                Modifier.offset(100.dp, 100.dp)
                    .size(200.dp)
                    .sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(key = "key"),
                        visible = isExpanded,
                    )
                    .background(Color.Red)
            )
        }
    }

    override fun toggleState() {
        isExpanded = !isExpanded
    }
}
