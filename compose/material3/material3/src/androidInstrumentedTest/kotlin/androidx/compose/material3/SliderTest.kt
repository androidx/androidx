/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.internal.HorizontalSemanticsBoundsPadding
import androidx.compose.material3.tokens.SliderTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SliderTest {
    private val tag = "slider"
    private val SliderTolerance = 0.003f

    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderPosition_valueCoercion() {
        val state = SliderState(0f)
        rule.setContent { Slider(state = state, modifier = Modifier.testTag(tag)) }
        rule.runOnIdle { state.value = 2f }
        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f, 0))
        rule.runOnIdle { state.value = -123145f }
        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test(expected = IllegalArgumentException::class)
    fun sliderPosition_stepsThrowWhenLessThanZero() {
        rule.setContent { Slider(SliderState(value = 0f, steps = -1)) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_semantics_continuous() {
        val state = SliderState(0f)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule
            .onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread { state.value = 0.5f }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f, 0))

        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(0.7f) }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.7f, 0f..1f, 0))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_semantics_stepped() {
        val state = SliderState(0f, steps = 4)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule
            .onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 4))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread { state.value = 0.6f }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, 0f..1f, 4))

        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.8f, 0f..1f, 4))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_semantics_focusable() {
        rule.setMaterialContent(lightColorScheme()) {
            Slider(SliderState(0f), modifier = Modifier.testTag(tag))
        }

        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_semantics_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = SliderState(0f), modifier = Modifier.testTag(tag), enabled = false)
        }

        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_drag() {
        val state = SliderState(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + 100 - slop)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_drag_out_of_bounds() {
        val state = SliderState(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(width.toFloat() + 100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + 100 - slop)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_tap() {
        val state = SliderState(0f)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX + 50, centerY))
            up()
            expected = calculateFraction(left, right, centerX + 50)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun vertical_slider_tap() {
        val state = SliderState(0f)

        rule.setMaterialContent(lightColorScheme()) {
            VerticalSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX, centerY + 50))
            up()
            expected = calculateFraction(top, bottom, centerY + 50)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    /** Guarantee slider doesn't move as we scroll, tapping still works */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_scrollableContainer() {
        val state = SliderState(0f)
        val offset = mutableStateOf(0f)

        rule.setContent {
            Column(
                modifier =
                    Modifier.height(2000.dp)
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state =
                                rememberScrollableState { delta ->
                                    offset.value += delta
                                    delta
                                },
                        )
            ) {
                Slider(state = state, modifier = Modifier.testTag(tag))
            }
        }

        rule.runOnIdle { Truth.assertThat(offset.value).isEqualTo(0f) }

        // Just scroll
        rule.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput {
            down(Offset(centerX, centerY))
            moveBy(Offset(0f, 500f))
            up()
        }

        rule.runOnIdle {
            Truth.assertThat(offset.value).isGreaterThan(0f)
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        // Tap
        var expected = 0f
        rule.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput {
            click(Offset(centerX, centerY))
            expected = calculateFraction(left, right, centerX)
        }

        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_tap_rangeChange() {
        val rangeEnd = mutableStateOf(0.25f)
        lateinit var state: SliderState

        rule.setMaterialContent(lightColorScheme()) {
            state = remember(rangeEnd.value) { SliderState(0f, valueRange = 0f..rangeEnd.value) }
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        // change to 1 since [calculateFraction] coerces between 0..1
        rule.runOnUiThread { rangeEnd.value = 1f }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            click(Offset(centerX + 50, centerY))
            expected = calculateFraction(left, right, centerX + 50)
        }

        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_drag_rtl() {
        val state = SliderState(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
                Slider(state = state, modifier = Modifier.testTag(tag))
            }
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
            // subtract here as we're in rtl and going in the opposite direction
            expected = calculateFraction(left, right, centerX - 100 + slop)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_tap_rtl() {
        val state = SliderState(0f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Slider(state = state, modifier = Modifier.testTag(tag))
            }
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX + 50, centerY))
            up()
            expected = calculateFraction(left, right, centerX - 50)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    private fun calculateFraction(left: Float, right: Float, pos: Float) =
        with(rule.density) {
            val offset = (ThumbWidth / 2).toPx()
            val start = left + offset
            val end = right - offset
            ((pos - start) / (end - start)).coerceIn(0f, 1f)
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_sizes() {
        val state = SliderState(0f)
        rule
            .setMaterialContentForSizeAssertions(
                parentMaxWidth = 100.dp,
                parentMaxHeight = 100.dp,
            ) {
                Slider(state)
            }
            .assertHeightIsEqualTo(48.dp)
            .assertWidthIsEqualTo(100.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_sizes_within_row() {
        val rowWidth = 100.dp
        val spacerWidth = 10.dp

        rule.setMaterialContent(lightColorScheme()) {
            Row(modifier = Modifier.requiredWidth(rowWidth)) {
                Spacer(Modifier.width(spacerWidth))
                Slider(state = SliderState(0f), modifier = Modifier.testTag(tag).weight(1f))
                Spacer(Modifier.width(spacerWidth))
            }
        }

        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(rowWidth - spacerWidth * 2 + HorizontalSemanticsBoundsPadding * 2)
            .assertHeightIsEqualTo(SliderTokens.HandleHeight)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_min_size() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(0.dp)) {
                Slider(state = SliderState(0f), modifier = Modifier.testTag(tag))
            }
        }

        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(SliderTokens.HandleWidth + HorizontalSemanticsBoundsPadding * 2)
            .assertHeightIsEqualTo(SliderTokens.InactiveTrackHeight)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_noUnwantedCallbackCalls() {
        val callCount = mutableStateOf(0f)
        val state = SliderState(0f)
        state.onValueChange = { callCount.value += 1 }

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnIdle { Truth.assertThat(callCount.value).isEqualTo(0f) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_valueChangeFinished_calledOnce() {
        val callCount = mutableStateOf(0f)
        val state = SliderState(0f, onValueChangeFinished = { callCount.value += 1 })

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnIdle { Truth.assertThat(callCount.value).isEqualTo(0) }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(50f, 50f))
            up()
        }

        rule.runOnIdle { Truth.assertThat(callCount.value).isEqualTo(1) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_setProgress_callsOnValueChangeFinished() {
        val callCount = mutableStateOf(0)
        val state = SliderState(0f, onValueChangeFinished = { callCount.value += 1 })

        rule.setMaterialContent(lightColorScheme()) {
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnIdle { Truth.assertThat(callCount.value).isEqualTo(0) }

        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(0.8f) }

        rule.runOnIdle { Truth.assertThat(callCount.value).isEqualTo(1) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitSlider by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitSlider) {
                    Slider(
                        state = SliderState(0.5f),
                        modifier = Modifier.testTag(tag),
                        interactionSource = interactionSource,
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { Truth.assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose
        rule.runOnIdle { emitSlider = false }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            Truth.assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_onValueChangedFinish_afterTap() {
        var changedFlag = false
        rule.setContent {
            Slider(
                state = SliderState(0f, onValueChangeFinished = { changedFlag = true }),
                modifier = Modifier.testTag(tag),
            )
        }

        rule.onNodeWithTag(tag).performTouchInput { click(center) }

        rule.runOnIdle { Truth.assertThat(changedFlag).isTrue() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_zero_width() {
        rule
            .setMaterialContentForSizeAssertions(parentMaxHeight = 0.dp, parentMaxWidth = 0.dp) {
                Slider(SliderState(1f))
            }
            .assertHeightIsEqualTo(0.dp)
            .assertWidthIsEqualTo(0.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_thumb_recomposition() {
        val state = SliderState(0f)
        val recompositionCounter = SliderRecompositionCounter()

        rule.setContent {
            Slider(
                state = state,
                modifier = Modifier.testTag(tag),
                thumb = { sliderState -> recompositionCounter.OuterContent(sliderState) },
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            moveBy(Offset(-100f, 0f))
            moveBy(Offset(100f, 0f))
        }
        rule.runOnIdle {
            Truth.assertThat(recompositionCounter.outerRecomposition).isEqualTo(1)
            Truth.assertThat(recompositionCounter.innerRecomposition).isEqualTo(4)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_track_recomposition() {
        val state = SliderState(0f)
        val recompositionCounter = SliderRecompositionCounter()

        rule.setContent {
            Slider(
                state = state,
                modifier = Modifier.testTag(tag),
                track = { sliderState -> recompositionCounter.OuterContent(sliderState) },
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            moveBy(Offset(-100f, 0f))
            moveBy(Offset(100f, 0f))
        }
        rule.runOnIdle {
            Truth.assertThat(recompositionCounter.outerRecomposition).isEqualTo(1)
            Truth.assertThat(recompositionCounter.innerRecomposition).isEqualTo(4)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_parentWithInfiniteWidth_minWidth() {
        val state = SliderState(0f)
        rule
            .setMaterialContentForSizeAssertions {
                Box(modifier = Modifier.requiredWidth(Int.MAX_VALUE.dp)) { Slider(state) }
            }
            .assertWidthIsEqualTo(48.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_rowWithInfiniteWidth() {
        rule.setContent {
            Row(modifier = Modifier.requiredWidth(Int.MAX_VALUE.dp)) {
                Slider(state = SliderState(0f), modifier = Modifier.weight(1f))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_onValueChangeFinishedWithSnackbar() {
        lateinit var state: SliderState
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = { _ ->
                    state = remember {
                        SliderState(
                            value = 0f,
                            onValueChangeFinished = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Snackbar Description")
                                }
                            },
                        )
                    }
                    slop = LocalViewConfiguration.current.touchSlop
                    Slider(state = state, modifier = Modifier.testTag(tag))
                },
            )
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(0f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + 100 - slop)
        }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_dragThumb() {
        val state = RangeSliderState(0f, 1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(100f, 0f))
            expected = calculateFraction(left, right, centerX + slop + 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_out_of_bounds() {
        val state = RangeSliderState(0f, 1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(width.toFloat() + 100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + slop + 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_overlap_thumbs() {
        val state = RangeSliderState(0.5f, 1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                state = state,
                modifier = Modifier.testTag(tag),
                startThumb = { SliderDefaults.Thumb(MutableInteractionSource()) },
                endThumb = { SliderDefaults.Thumb(MutableInteractionSource()) },
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(centerRight)
            moveBy(Offset(-slop, 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            up()
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0.5f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(0.5f)
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(-slop, 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            up()
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(0.5f)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_tap() {
        val state = RangeSliderState(0f, 1f)

        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX + 50, centerY))
            up()
            expected = calculateFraction(left, right, centerX + 50)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_tap_rangeChange() {
        val rangeEnd = mutableStateOf(0.25f)
        lateinit var state: RangeSliderState

        rule.setMaterialContent(lightColorScheme()) {
            state =
                remember(rangeEnd.value) {
                    RangeSliderState(0f, 25f, valueRange = 0f..rangeEnd.value)
                }
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }
        // change to 1 since [calculateFraction] coerces between 0..1
        rule.runOnUiThread { rangeEnd.value = 1f }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX + 50, centerY))
            up()
            expected = calculateFraction(left, right, centerX + 50)
        }

        rule.runOnIdle {
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_rtl() {
        val state = RangeSliderState(0f, 1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
                RangeSlider(state = state, modifier = Modifier.testTag(tag))
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(100f, 0f))
            up()
            // subtract here as we're in rtl and going in the opposite direction
            expected = calculateFraction(left, right, centerX - slop - 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_out_of_bounds_rtl() {
        val state = RangeSliderState(0f, 1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
                RangeSlider(state = state, modifier = Modifier.testTag(tag))
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(-width.toFloat(), 0f))
            moveBy(Offset(width.toFloat() + 100f, 0f))
            up()
            // subtract here as we're in rtl and going in the opposite direction
            expected = calculateFraction(left, right, centerX - slop - 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_closeThumbs_dragRight() {
        val state = RangeSliderState(0.5f, 0.5f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0.5f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(0.5f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(100f, 0f))
            up()
            // subtract here as we're in rtl and going in the opposite direction
            expected = calculateFraction(left, right, centerX + slop + 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0.5f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_closeThumbs_dragLeft() {
        val state = RangeSliderState(0.5f, 0.5f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0.5f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(0.5f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(center.x - 1f, center.y))
            moveBy(Offset(-slop, 0f))
            moveBy(Offset(-100f, 0f))
            up()
            // subtract here as we're in rtl and going in the opposite direction
            expected = calculateFraction(left, right, centerX - slop - 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isWithin(SliderTolerance).of(expected)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(0.5f)
        }
    }

    /**
     * Regression test for bug: 210289161 where RangeSlider was ignoring some modifiers like weight.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_weightModifier() {
        var sliderBounds = Rect(0f, 0f, 0f, 0f)
        val state = RangeSliderState(0f, 0.5f)
        rule.setMaterialContent(lightColorScheme()) {
            with(LocalDensity.current) {
                Row(Modifier.width(500.toDp())) {
                    Spacer(Modifier.requiredSize(100.toDp()))
                    RangeSlider(
                        state = state,
                        modifier =
                            Modifier.testTag(tag).weight(1f).onGloballyPositioned {
                                sliderBounds = it.boundsInParent()
                            },
                    )
                    Spacer(Modifier.requiredSize(100.toDp()))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sliderBounds.left).isEqualTo(100)
            Truth.assertThat(sliderBounds.right).isEqualTo(400)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_semantics_continuous() {
        val state = RangeSliderState(0f, 1f)

        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule
            .onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule
            .onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.activeRangeStart = 0.5f
            state.activeRangeEnd = 0.75f
        }

        rule
            .onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..0.75f, 0))

        rule
            .onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.75f, 0.5f..1f, 0))

        rule.onAllNodes(isFocusable(), true)[0].performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(0.6f)
        }

        rule.onAllNodes(isFocusable(), true)[1].performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(0.8f)
        }

        rule
            .onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, 0f..0.8f, 0))

        rule
            .onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.8f, 0.6f..1f, 0))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_semantics_stepped() {
        val state = RangeSliderState(0f, 20f, steps = 3, valueRange = 0f..20f)
        // Slider with [0,5,10,15,20] possible values
        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread {
            state.activeRangeStart = 5f
            state.activeRangeEnd = 10f
        }

        rule
            .onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(5f, 0f..10f, 1))

        rule
            .onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 5f..20f, 2))

        rule.onAllNodes(isFocusable(), true)[0].performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(10f)
        }

        rule.onAllNodes(isFocusable(), true)[1].performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(15f)
        }

        rule
            .onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..15f, 2))

        rule
            .onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(15f, 10f..20f, 1))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_thumbs_semanticsNodeBounds() {
        val startThumbTag = "startThumb"
        val endThumbTag = "endThumb"
        val padding = 10.dp

        val expectedWidth =
            with(rule.density) { SliderTokens.HandleWidth.roundToPx() + padding.roundToPx() }
        val expectedHeight = with(rule.density) { SliderTokens.HandleHeight.roundToPx() }

        val state = RangeSliderState(0f, 1f)
        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                state = state,
                startThumb = {
                    SliderDefaults.Thumb(
                        interactionSource = MutableInteractionSource(),
                        modifier = Modifier.testTag(startThumbTag),
                    )
                },
                endThumb = {
                    SliderDefaults.Thumb(
                        interactionSource = MutableInteractionSource(),
                        modifier = Modifier.testTag(endThumbTag),
                    )
                },
            )
        }

        listOf(startThumbTag, endThumbTag).forEach {
            val thumbNode =
                rule
                    .onNodeWithTag(it, true)
                    .onParent()
                    .fetchSemanticsNode("couldn't find node with tag $it")
            val thumbNodeBounds = thumbNode.boundsInRoot

            // Check that the SemanticsNode bounds include the padding. This means that the
            // SemanticsNode bounds are big enough to trigger TalkBack's green focus indicator.
            Truth.assertThat(thumbNodeBounds.width).isEqualTo(expectedWidth)
            Truth.assertThat(thumbNodeBounds.height).isEqualTo(expectedHeight)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_thumbs_visualBounds() {
        val startThumbTag = "startThumb"
        val endThumbTag = "endThumb"

        val state = RangeSliderState(0f, 1f)
        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                state = state,
                startThumb = {
                    SliderDefaults.Thumb(
                        interactionSource = MutableInteractionSource(),
                        modifier = Modifier.testTag(startThumbTag),
                    )
                },
                endThumb = {
                    SliderDefaults.Thumb(
                        interactionSource = MutableInteractionSource(),
                        modifier = Modifier.testTag(endThumbTag),
                    )
                },
            )
        }

        listOf(startThumbTag, endThumbTag).forEach {
            val thumbNodeBounds = rule.onNodeWithTag(it, true).getBoundsInRoot()

            // Check that the visual bounds are the expected visual size.
            thumbNodeBounds.width.assertIsEqualTo(SliderTokens.HandleWidth, it)
            thumbNodeBounds.height.assertIsEqualTo(SliderTokens.HandleHeight, it)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun slider_dragOutsideTouchArea_doesntJump() {
        val state = SliderState(.5f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            Slider(state = state, modifier = Modifier.testTag(tag))
        }

        rule.runOnUiThread { Truth.assertThat(state.value).isEqualTo(.5f) }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            // move down outside the slider area
            moveBy(Offset(0f, 500f))
            moveBy(Offset(100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + 100 - slop)
        }

        rule.runOnIdle { Truth.assertThat(state.value).isWithin(SliderTolerance).of(expected) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_thumb_recomposition() {
        val state = RangeSliderState(0f, 100f, valueRange = 0f..100f)
        val startRecompositionCounter = RangeSliderRecompositionCounter()
        val endRecompositionCounter = RangeSliderRecompositionCounter()

        rule.setContent {
            RangeSlider(
                state = state,
                modifier = Modifier.testTag(tag),
                startThumb = { rangeSliderState ->
                    startRecompositionCounter.OuterContent(rangeSliderState)
                },
                endThumb = { rangeSliderState ->
                    endRecompositionCounter.OuterContent(rangeSliderState)
                },
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            moveBy(Offset(-100f, 0f))
            moveBy(Offset(100f, 0f))
        }

        rule.runOnIdle {
            Truth.assertThat(startRecompositionCounter.outerRecomposition).isEqualTo(1)
            Truth.assertThat(startRecompositionCounter.innerRecomposition).isEqualTo(2)
            Truth.assertThat(endRecompositionCounter.outerRecomposition).isEqualTo(1)
            Truth.assertThat(endRecompositionCounter.innerRecomposition).isEqualTo(2)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_track_recomposition() {
        val state = RangeSliderState(0f, 100f, valueRange = 0f..100f)
        val recompositionCounter = RangeSliderRecompositionCounter()

        rule.setContent {
            RangeSlider(
                state = state,
                modifier = Modifier.testTag(tag),
                track = { rangeSliderState -> recompositionCounter.OuterContent(rangeSliderState) },
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            moveBy(Offset(-100f, 0f))
            moveBy(Offset(100f, 0f))
        }

        rule.runOnIdle {
            Truth.assertThat(recompositionCounter.outerRecomposition).isEqualTo(1)
            Truth.assertThat(recompositionCounter.innerRecomposition).isEqualTo(3)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_parentWithInfiniteWidth_minWidth() {
        val state = RangeSliderState(0f, 1f)
        rule
            .setMaterialContentForSizeAssertions {
                Box(modifier = Modifier.requiredWidth(Int.MAX_VALUE.dp)) { RangeSlider(state) }
            }
            .assertWidthIsEqualTo(48.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_rowWithInfiniteWidth() {
        val state = RangeSliderState(0f, 1f)
        rule.setContent {
            Row(modifier = Modifier.requiredWidth(Int.MAX_VALUE.dp)) {
                RangeSlider(state = state, modifier = Modifier.weight(1f))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_onValueChangeFinishedWithSnackbar() {
        lateinit var state: RangeSliderState
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = { _ ->
                    state = remember {
                        RangeSliderState(
                            activeRangeStart = 0f,
                            activeRangeEnd = 1f,
                            onValueChangeFinished = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Snackbar Description")
                                }
                            },
                        )
                    }
                    slop = LocalViewConfiguration.current.touchSlop
                    RangeSlider(state = state, modifier = Modifier.testTag(tag))
                },
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isEqualTo(1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(slop, 0f))
            moveBy(Offset(100f, 0f))
            up()
            expected = calculateFraction(left, right, centerX + slop + 100)
        }
        rule.runOnIdle {
            Truth.assertThat(state.activeRangeStart).isEqualTo(0f)
            Truth.assertThat(state.activeRangeEnd).isWithin(SliderTolerance).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_valueUpdatedByLaunchEffectAndInteraction() {
        lateinit var sliderPosition: MutableState<ClosedFloatingPointRange<Float>>

        rule.setMaterialContent(lightColorScheme()) {
            sliderPosition = remember { mutableStateOf(0f..100f) }
            RangeSlider(
                modifier = Modifier.testTag(tag),
                value = sliderPosition.value,
                steps = 0,
                onValueChange = { range -> sliderPosition.value = range },
                valueRange = 0f..100f,
            )
            LaunchedEffect(Unit) { sliderPosition.value = 0f..50f }
        }

        rule.waitForIdle()

        rule.runOnIdle {
            Truth.assertThat(sliderPosition.value.start).isEqualTo(0f)
            Truth.assertThat(sliderPosition.value.endInclusive).isEqualTo(50f)
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(centerX - 50, centerY))
            up()
        }

        rule.waitForIdle()

        rule.runOnIdle {
            Truth.assertThat(sliderPosition.value.endInclusive).isNotEqualTo(50f)
            Truth.assertThat(sliderPosition.value.start).isEqualTo(0f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class SliderRecompositionCounter {
    var innerRecomposition = 0
    var outerRecomposition = 0

    @Composable
    fun OuterContent(state: SliderState) {
        SideEffect { ++outerRecomposition }
        Column {
            Text("OuterContent")
            InnerContent(state)
        }
    }

    @Composable
    private fun InnerContent(state: SliderState) {
        SideEffect { ++innerRecomposition }
        Text("InnerContent: ${state.value}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class RangeSliderRecompositionCounter {
    var innerRecomposition = 0
    var outerRecomposition = 0

    @Composable
    fun OuterContent(state: RangeSliderState) {
        SideEffect { ++outerRecomposition }
        Column {
            Text("OuterContent")
            InnerContent(state)
        }
    }

    @Composable
    private fun InnerContent(state: RangeSliderState) {
        SideEffect { ++innerRecomposition }
        Text("InnerContent: ${state.activeRangeStart..state.activeRangeEnd}")
    }
}
