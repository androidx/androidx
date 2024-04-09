/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.internal.BasicTooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class TooltipTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun plainTooltip_noContent_size() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState()
            scope = rememberCoroutineScope()
            PlainTooltipTest(tooltipState = state)
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(TooltipMinHeight)
            .assertWidthIsEqualTo(TooltipMinWidth)
    }

    @Test
    fun richTooltip_noContent_size() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState(isPersistent = true)
            scope = rememberCoroutineScope()
            RichTooltipTest(tooltipState = state)
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(TooltipMinHeight)
            .assertWidthIsEqualTo(TooltipMinWidth)
    }

    @Test
    fun plainTooltip_customSize_size() {
        val customWidth = 100.dp
        val customHeight = 100.dp
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState()
            scope = rememberCoroutineScope()
            PlainTooltipTest(
                modifier = Modifier.size(customWidth, customHeight),
                tooltipState = state
            )
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(customHeight)
            .assertWidthIsEqualTo(customWidth)
    }

    @Test
    fun richTooltip_customSize_size() {
        val customWidth = 100.dp
        val customHeight = 100.dp
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState(isPersistent = true)
            scope = rememberCoroutineScope()
            RichTooltipTest(
                modifier = Modifier.size(customWidth, customHeight),
                tooltipState = state
            )
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(customHeight)
            .assertWidthIsEqualTo(customWidth)
    }

    @Test
    fun plainTooltip_content_padding() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState()
            scope = rememberCoroutineScope()
            PlainTooltipTest(
                tooltipContent = {
                    Text(
                        text = "Test",
                        modifier = Modifier.testTag(TextTestTag)
                    )
                },
                tooltipState = state
            )
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        rule.onNodeWithTag(TextTestTag)
            .assertLeftPositionInRootIsEqualTo(8.dp)
            .assertTopPositionInRootIsEqualTo(4.dp)
    }

    @Test
    fun richTooltip_content_padding() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState(isPersistent = true)
            scope = rememberCoroutineScope()
            RichTooltipTest(
                title = { Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag)) },
                text = { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) },
                action = { Text(text = "Action", modifier = Modifier.testTag(ActionTestTag)) },
                tooltipState = state
            )
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        val subhead = rule.onNodeWithTag(SubheadTestTag)
        val text = rule.onNodeWithTag(TextTestTag)

        val subheadBaseline = subhead.getFirstBaselinePosition()
        val textBaseLine = text.getFirstBaselinePosition()

        val subheadBound = subhead.getUnclippedBoundsInRoot()
        val textBound = text.getUnclippedBoundsInRoot()

        rule.onNodeWithTag(SubheadTestTag)
            .assertLeftPositionInRootIsEqualTo(RichTooltipHorizontalPadding)
            .assertTopPositionInRootIsEqualTo(28.dp - subheadBaseline)

        rule.onNodeWithTag(TextTestTag)
            .assertLeftPositionInRootIsEqualTo(RichTooltipHorizontalPadding)
            .assertTopPositionInRootIsEqualTo(subheadBound.bottom + 24.dp - textBaseLine)

        rule.onNodeWithTag(ActionTestTag)
            .assertLeftPositionInRootIsEqualTo(RichTooltipHorizontalPadding)
            .assertTopPositionInRootIsEqualTo(textBound.bottom + 16.dp)
    }

    @Test
    fun plainTooltip_behavior() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState()
            scope = rememberCoroutineScope()
            PlainTooltipTest(
                tooltipContent = { Text(text = "Test", modifier = Modifier.testTag(TextTestTag)) },
                tooltipState = state
            )
        }

        // Test will manually advance the time to check the timeout
        rule.mainClock.autoAdvance = false

        // Tooltip should initially be not visible
        assertThat(state.isVisible).isFalse()

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        // Check that the tooltip is now showing
        rule.waitForIdle()
        assertThat(state.isVisible).isTrue()

        // Tooltip should dismiss itself after 1.5s
        rule.mainClock.advanceTimeBy(milliseconds = BasicTooltipDefaults.TooltipDuration)
        rule.waitForIdle()
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun richTooltip_behavior_noAction() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState(isPersistent = false)
            scope = rememberCoroutineScope()
            RichTooltipTest(
                title = { Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag)) },
                text = { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) },
                tooltipState = state
            )
        }

        // Test will manually advance the time to check the timeout
        rule.mainClock.autoAdvance = false

        // Tooltip should initially be not visible
        assertThat(state.isVisible).isFalse()

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        // Check that the tooltip is now showing
        rule.waitForIdle()
        assertThat(state.isVisible).isTrue()

        // Tooltip should dismiss itself after 1.5s
        rule.mainClock.advanceTimeBy(milliseconds = BasicTooltipDefaults.TooltipDuration)
        rule.waitForIdle()
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun richTooltip_behavior_persistent() {
        lateinit var state: TooltipState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberTooltipState(isPersistent = true)
            scope = rememberCoroutineScope()
            RichTooltipTest(
                title = { Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag)) },
                text = { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) },
                action = {
                    TextButton(
                        onClick = { scope.launch { state.dismiss() } },
                        modifier = Modifier.testTag(ActionTestTag)
                    ) { Text(text = "Action") }
                },
                tooltipState = state
            )
        }

        // Test will manually advance the time to check the timeout
        rule.mainClock.autoAdvance = false

        // Tooltip should initially be not visible
        assertThat(state.isVisible).isFalse()

        // Trigger tooltip
        scope.launch {
            state.show()
        }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        // Check that the tooltip is now showing
        rule.waitForIdle()
        assertThat(state.isVisible).isTrue()

        // Tooltip should still be visible after the normal TooltipDuration, since we have an action.
        rule.mainClock.advanceTimeBy(milliseconds = BasicTooltipDefaults.TooltipDuration)
        rule.waitForIdle()
        assertThat(state.isVisible).isTrue()

        // Click the action and check that it closed the tooltip
        rule.onNodeWithTag(ActionTestTag)
            .performTouchInput { click() }

        // Advance by the fade out duration
        // plus some additional time to make sure that the tooltip is full faded out.
        rule.mainClock.advanceTimeBy(TooltipFadeOutDuration.toLong() + 100L)
        rule.waitForIdle()
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun plainTooltipPositioning_tooltipCollideWithTopOfScreen_flipToBelowAnchor() {
        // Test Anchor Bounds
        val anchorPosition = IntOffset(0, 0)
        val anchorSize = IntSize(10, 20)
        val anchorBounds = IntRect(anchorPosition, anchorSize)

        // Test window size
        val screenWidth = 500
        val screenHeight = 1000
        val windowSize = IntSize(screenWidth, screenHeight)

        // Tooltip that will be large enough to collide with the top of the screen.
        val popupSize = IntSize(500, 500)

        // Plain tooltip positioning
        lateinit var positionProvider: PopupPositionProvider
        rule.setContent {
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
        }

        val tooltipPosition = positionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupSize
        )

        val tooltipBounds = IntRect(tooltipPosition, popupSize)

        // The tooltip should be placed below the anchor, since it's colliding with top of screen.
        assertThat(tooltipBounds.top > anchorBounds.bottom).isTrue()
    }

    @Test
    fun richTooltipPositioning_tooltipCollideWithTopOfScreen_flipToBelowAnchor() {
        // Test Anchor Bounds
        val anchorPosition = IntOffset(0, 0)
        val anchorSize = IntSize(10, 20)
        val anchorBounds = IntRect(anchorPosition, anchorSize)

        // Test window size
        val screenWidth = 500
        val screenHeight = 1000
        val windowSize = IntSize(screenWidth, screenHeight)

        // Tooltip that will be large enough to collide with the top of the screen.
        val popupSize = IntSize(500, 500)

        // Rich tooltip positioning
        lateinit var positionProvider: PopupPositionProvider
        rule.setContent {
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider()
        }

        val tooltipPosition = positionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupSize
        )

        val tooltipBounds = IntRect(tooltipPosition, popupSize)

        // The tooltip should be placed below the anchor, since it's colliding with top of screen.
        assertThat(tooltipBounds.top > anchorBounds.bottom).isTrue()
    }

    @Test
    fun plainTooltip_caretAnchorPositioning() {
        var anchorBounds = Rect.Zero
        rule.setContent {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = rememberTooltipState(initialIsVisible = true, isPersistent = true),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.drawCaret {
                            it?.let { anchorBounds = it.boundsInWindow() }
                            onDrawBehind {}
                        }
                    ) {}
                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    modifier = Modifier.testTag(AnchorTestTag),
                    contentDescription = null
                )
            }
        }

        rule.waitForIdle()
        val expectedAnchorBoundsDp =
            rule.onNodeWithTag(AnchorTestTag, true).getUnclippedBoundsInRoot()
        val expectedAnchorBounds = with(rule.density) {
            Rect(
                expectedAnchorBoundsDp.left.roundToPx().toFloat(),
                expectedAnchorBoundsDp.top.roundToPx().toFloat(),
                expectedAnchorBoundsDp.right.roundToPx().toFloat(),
                expectedAnchorBoundsDp.bottom.roundToPx().toFloat()
            )
        }

        assertThat(anchorBounds.left).isWithin(0.001f).of(expectedAnchorBounds.left)
        assertThat(anchorBounds.top).isWithin(0.001f).of(expectedAnchorBounds.top)
        assertThat(anchorBounds.right).isWithin(0.001f).of(expectedAnchorBounds.right)
        assertThat(anchorBounds.bottom).isWithin(0.001f).of(expectedAnchorBounds.bottom)
    }

    @Test
    fun richTooltip_caretAnchorPositioning() {
        var anchorBounds = Rect.Zero
        rule.setContent {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                state = rememberTooltipState(initialIsVisible = true, isPersistent = true),
                tooltip = {
                    RichTooltip(
                        modifier = Modifier.drawCaret {
                            it?.let { anchorBounds = it.boundsInWindow() }
                            onDrawBehind {}
                        }
                    ) {}
                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    modifier = Modifier.testTag(AnchorTestTag),
                    contentDescription = null
                )
            }
        }

        rule.waitForIdle()
        val expectedAnchorBoundsDp =
            rule.onNodeWithTag(AnchorTestTag, true).getUnclippedBoundsInRoot()
        val expectedAnchorBounds = with(rule.density) {
            Rect(
                expectedAnchorBoundsDp.left.roundToPx().toFloat(),
                expectedAnchorBoundsDp.top.roundToPx().toFloat(),
                expectedAnchorBoundsDp.right.roundToPx().toFloat(),
                expectedAnchorBoundsDp.bottom.roundToPx().toFloat()
            )
        }

        assertThat(anchorBounds.left).isWithin(0.001f).of(expectedAnchorBounds.left)
        assertThat(anchorBounds.top).isWithin(0.001f).of(expectedAnchorBounds.top)
        assertThat(anchorBounds.right).isWithin(0.001f).of(expectedAnchorBounds.right)
        assertThat(anchorBounds.bottom).isWithin(0.001f).of(expectedAnchorBounds.bottom)
    }

    @Test
    fun tooltipSync_global_onlyOneVisible() {
        val topTooltipTag = "Top Tooltip"
        val bottomTooltipTag = " Bottom Tooltip"
        lateinit var topState: TooltipState
        lateinit var bottomState: TooltipState
        rule.setMaterialContent(lightColorScheme()) {
            val scope = rememberCoroutineScope()
            topState = rememberTooltipState(isPersistent = true)
            bottomState = rememberTooltipState(isPersistent = true)
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = {
                            Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag))
                        },
                        action = {
                            TextButton(
                                modifier = Modifier.testTag(ActionTestTag),
                                onClick = {}
                            ) {
                                Text(text = "Action")
                            }
                        }

                    ) { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) }
                },
                state = topState,
                modifier = Modifier.testTag(topTooltipTag)
            ) {}
            scope.launch { topState.show() }

            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = {
                            Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag))
                        },
                        action = {
                            TextButton(
                                modifier = Modifier.testTag(ActionTestTag),
                                onClick = {}
                            ) {
                                Text(text = "Action")
                            }
                        }

                    ) { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) }
                },
                state = bottomState,
                modifier = Modifier.testTag(bottomTooltipTag)
            ) {}
            scope.launch { bottomState.show() }
        }

        // Test will manually advance the time to check the timeout
        rule.mainClock.autoAdvance = false

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        // Check that only the tooltip associated with bottomState is visible
        rule.waitForIdle()
        assertThat(topState.isVisible).isFalse()
        assertThat(bottomState.isVisible).isTrue()
    }

    @Test
    fun tooltipSync_local_bothVisible() {
        val topTooltipTag = "Top Tooltip"
        val bottomTooltipTag = " Bottom Tooltip"
        lateinit var topState: TooltipState
        lateinit var bottomState: TooltipState
        rule.setMaterialContent(lightColorScheme()) {
            val scope = rememberCoroutineScope()
            topState = rememberTooltipState(
                isPersistent = true,
                mutatorMutex = MutatorMutex()
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = {
                            Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag))
                        },
                        action = {
                            TextButton(
                                modifier = Modifier.testTag(ActionTestTag),
                                onClick = {}
                            ) {
                                Text(text = "Action")
                            }
                        }

                    ) { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) }
                },
                state = topState,
                modifier = Modifier.testTag(topTooltipTag)
            ) {}
            scope.launch { topState.show() }

            bottomState = rememberTooltipState(
                isPersistent = true,
                mutatorMutex = MutatorMutex()
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = {
                            Text(text = "Subhead", modifier = Modifier.testTag(SubheadTestTag))
                        },
                        action = {
                            TextButton(
                                modifier = Modifier.testTag(ActionTestTag),
                                onClick = {}
                            ) {
                                Text(text = "Action")
                            }
                        }

                    ) { Text(text = "Text", modifier = Modifier.testTag(TextTestTag)) }
                },
                state = bottomState,
                modifier = Modifier.testTag(bottomTooltipTag)
            ) {}
            scope.launch { bottomState.show() }
        }

        // Test will manually advance the time to check the timeout
        rule.mainClock.autoAdvance = false

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        // Check that both tooltips are now showing
        rule.waitForIdle()
        assertThat(topState.isVisible).isTrue()
        assertThat(bottomState.isVisible).isTrue()
    }

    @Composable
    private fun PlainTooltipTest(
        modifier: Modifier = Modifier,
        tooltipContent: @Composable () -> Unit = {},
        tooltipState: TooltipState = rememberTooltipState(),
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(
                    modifier = modifier.testTag(ContainerTestTag),
                    content = tooltipContent
                )
            },
            state = tooltipState
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null
            )
        }
    }

    @Composable
    private fun RichTooltipTest(
        modifier: Modifier = Modifier,
        text: @Composable () -> Unit = {},
        title: (@Composable () -> Unit)? = null,
        action: (@Composable () -> Unit)? = null,
        tooltipState: TooltipState = rememberTooltipState(action != null),
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = title,
                    action = action,
                    modifier = modifier.testTag(ContainerTestTag),
                    text = text
                )
            },
            state = tooltipState,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null
            )
        }
    }
}

private const val ActionTestTag = "Action"
private const val AnchorTestTag = "Anchor"
private const val ContainerTestTag = "Container"
private const val SubheadTestTag = "Subhead"
private const val TextTestTag = "Text"
