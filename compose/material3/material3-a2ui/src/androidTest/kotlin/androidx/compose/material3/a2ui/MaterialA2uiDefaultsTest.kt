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

package androidx.compose.material3.a2ui

import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiDefaultsTest {

    @Test
    fun loadingIndicator_standalone_rendersWithExpectedSize() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MaterialA2uiDefaults.LoadingIndicator(
                    modifier = Modifier.size(width = 120.dp, height = 40.dp)
                )
            }
        }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(120.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun loadingIndicator_lightAndDarkTheme_rendersAndAnimates() = runComposeUiTest {
        mainClock.autoAdvance = false
        var isDark by mutableStateOf(false)
        var capturedGroupState: ShimmerIndicatorGroupState? = null

        setContent {
            MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
                MaterialA2uiDefaults.LoadingIndicatorGroup { rootModifier ->
                    capturedGroupState = LocalShimmerIndicatorGroupState.current
                    Column(modifier = rootModifier) {
                        MaterialA2uiDefaults.LoadingIndicator(
                            modifier = Modifier.size(width = 100.dp, height = 36.dp),
                            index = 1,
                        )
                    }
                }
            }
        }

        mainClock.advanceTimeBy(400L)
        val node = onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG)
        node.assertIsDisplayed()

        val lightElement =
            node
                .fetchSemanticsNode()
                .layoutInfo
                .getModifierInfo()
                .map { it.modifier }
                .filterIsInstance<ShimmerIndicatorElement>()
                .single()
        assertThat(lightElement.isLight).isTrue()

        var lightSweepProgress = 0f
        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            lightSweepProgress = groupState.sweepProgress
            assertThat(lightSweepProgress).isGreaterThan(0f)
            assertThat(groupState.highlightAlpha).isGreaterThan(0f)
            assertThat(groupState.breathingPhase).isGreaterThan(0f)
        }

        isDark = true
        mainClock.advanceTimeBy(200L)
        node.assertIsDisplayed()

        val darkElement =
            node
                .fetchSemanticsNode()
                .layoutInfo
                .getModifierInfo()
                .map { it.modifier }
                .filterIsInstance<ShimmerIndicatorElement>()
                .single()
        assertThat(darkElement.isLight).isFalse()

        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.sweepProgress).isGreaterThan(lightSweepProgress)
            assertThat(groupState.highlightAlpha).isGreaterThan(0f)
            assertThat(groupState.breathingPhase).isGreaterThan(0f)
        }
    }

    @Test
    fun loadingIndicatorGroup_sharesGroupStateAndCoordinatesAcrossIndicators() = runComposeUiTest {
        mainClock.autoAdvance = false
        var capturedGroupState: ShimmerIndicatorGroupState? = null

        setContent {
            MaterialTheme {
                MaterialA2uiDefaults.LoadingIndicatorGroup { rootModifier ->
                    capturedGroupState = LocalShimmerIndicatorGroupState.current
                    Column(modifier = rootModifier) {
                        MaterialA2uiDefaults.LoadingIndicator(
                            modifier = Modifier.size(width = 100.dp, height = 40.dp),
                            index = 0,
                        )
                        MaterialA2uiDefaults.LoadingIndicator(
                            modifier = Modifier.size(width = 100.dp, height = 40.dp),
                            index = 1,
                        )
                    }
                }
            }
        }

        val nodes = onAllNodesWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG)
        nodes[0].assertIsDisplayed()
        nodes[1].assertIsDisplayed()

        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.activeIndicatorCount).isEqualTo(2)
            assertThat(groupState.rootCoordinates).isNotNull()
            assertThat(groupState.rootCoordinates!!.isAttached).isTrue()
        }

        mainClock.advanceTimeBy(500L)

        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.sweepProgress).isGreaterThan(0f)
            assertThat(groupState.highlightAlpha).isGreaterThan(0f)
            assertThat(groupState.breathingPhase).isGreaterThan(0f)
        }
    }

    @Test
    fun loadingIndicatorGroup_stopsAndResetsWhenAllIndicatorsDetach() = runComposeUiTest {
        mainClock.autoAdvance = false
        var showIndicators by mutableStateOf(true)
        var capturedGroupState: ShimmerIndicatorGroupState? = null

        setContent {
            MaterialTheme {
                MaterialA2uiDefaults.LoadingIndicatorGroup { rootModifier ->
                    capturedGroupState = LocalShimmerIndicatorGroupState.current
                    Column(modifier = rootModifier) {
                        if (showIndicators) {
                            MaterialA2uiDefaults.LoadingIndicator(
                                modifier = Modifier.size(width = 100.dp, height = 40.dp),
                                index = 0,
                            )
                        }
                    }
                }
            }
        }

        mainClock.advanceTimeBy(500L)
        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.activeIndicatorCount).isEqualTo(1)
            assertThat(groupState.sweepProgress).isGreaterThan(0f)
        }

        showIndicators = false
        mainClock.advanceTimeByFrame()

        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.activeIndicatorCount).isEqualTo(0)
            assertThat(groupState.sweepProgress).isEqualTo(0f)
            assertThat(groupState.highlightAlpha).isEqualTo(0f)
            assertThat(groupState.breathingPhase).isEqualTo(0f)
        }
    }

    @Test
    fun loadingIndicatorGroup_recomposition_doesNotResetAnimationState() = runComposeUiTest {
        mainClock.autoAdvance = false
        var indicatorIndex by mutableIntStateOf(0)
        var capturedGroupState: ShimmerIndicatorGroupState? = null

        setContent {
            MaterialTheme {
                MaterialA2uiDefaults.LoadingIndicatorGroup { rootModifier ->
                    capturedGroupState = LocalShimmerIndicatorGroupState.current
                    Column(modifier = rootModifier) {
                        MaterialA2uiDefaults.LoadingIndicator(
                            modifier = Modifier.size(width = 100.dp, height = 40.dp),
                            index = indicatorIndex,
                        )
                    }
                }
            }
        }

        mainClock.advanceTimeBy(500L)
        var progressBeforeRecompose = 0f
        runOnIdle {
            progressBeforeRecompose = checkNotNull(capturedGroupState).sweepProgress
            assertThat(progressBeforeRecompose).isGreaterThan(0f)
        }

        indicatorIndex = 1
        mainClock.advanceTimeByFrame()

        runOnIdle {
            val groupState = checkNotNull(capturedGroupState)
            assertThat(groupState.sweepProgress).isAtLeast(progressBeforeRecompose)
            assertThat(groupState.sweepProgress).isGreaterThan(0f)
        }
    }

    @Test
    fun errorFallback_rendersErrorSurface() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MaterialA2uiDefaults.ErrorFallback(
                    exception = A2uiRuntimeException("Test error"),
                    modifier = Modifier.testTag("error_fallback"),
                )
            }
        }

        onNodeWithTag("error_fallback").assertIsDisplayed()
    }
}
