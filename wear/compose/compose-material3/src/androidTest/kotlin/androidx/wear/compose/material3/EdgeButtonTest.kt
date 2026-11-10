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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EdgeButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            EdgeButton(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            EdgeButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    fun has_role_button() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun correct_height_for_button_sizes() {
        val sizes =
            listOf(
                EdgeButtonSize.ExtraSmall,
                EdgeButtonSize.Small,
                EdgeButtonSize.Medium,
                EdgeButtonSize.Large,
            )
        rule.setContentWithTheme {
            for (size in sizes) {
                EdgeButton(
                    onClick = {},
                    buttonSize = size,
                    modifier = Modifier.testTag("EdgeButton_${size.maximumHeight.value}"),
                ) {
                    Text("Test")
                }
            }
        }
        for (size in sizes) {
            rule
                .onNodeWithTag("EdgeButton_${size.maximumHeight.value}")
                .assertHeightIsEqualTo(size.maximumHeight)
        }
    }

    @Test
    fun clicks_inside_and_outside_around_edges_small() {
        verifyStandaloneClicksAroundEdges(EdgeButtonSize.Small)
    }

    @Test
    fun clicks_inside_and_outside_around_edges_medium() {
        verifyStandaloneClicksAroundEdges(EdgeButtonSize.Medium)
    }

    @Test
    fun clicks_inside_and_outside_around_edges_large() {
        verifyStandaloneClicksAroundEdges(EdgeButtonSize.Large)
    }

    @Test
    fun clicks_around_edges_when_scaled_in_tlc() {
        var clicks = 0
        val scrollTag = "TLC_SCROLL"

        rule.setContentWithTheme {
            Box(Modifier.size(200.dp)) {
                val state = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = state,
                    edgeButton = {
                        EdgeButton(
                            onClick = { clicks++ },
                            buttonSize = EdgeButtonSize.Large,
                            modifier = Modifier.testTag(TEST_TAG),
                        ) {
                            Text("Action")
                        }
                    },
                    edgeButtonSpacing = 0.dp,
                ) { contentPadding ->
                    TransformingLazyColumn(
                        state = state,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize().testTag(scrollTag),
                    ) {
                        items(5) {
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                label = { Text("Item $it") },
                            )
                        }
                    }
                }
            }
        }

        // Scroll to the bottom so the EdgeButton is fully expanded, then verify edge clicks.
        rule.onNodeWithTag(scrollTag).performTouchInput { repeat(3) { swipeUp() } }
        rule.waitForIdle()
        verifyClicksAroundEdgeButton(getClicks = { clicks })

        // Partially scroll back down so the EdgeButton is scaled/constrained to a smaller height,
        // then verify clicking around the scaled edge still triggers inside and ignores outside.
        rule.onNodeWithTag(scrollTag).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 60f)
        }
        rule.waitForIdle()
        verifyClicksAroundEdgeButton(getClicks = { clicks })
    }

    @Test
    fun clicks_around_edges_when_scaled_in_slc() {
        var clicks = 0
        val scrollTag = "SLC_SCROLL"

        rule.setContentWithTheme {
            Box(Modifier.size(200.dp)) {
                val state = rememberScalingLazyListState()
                ScreenScaffold(
                    scrollState = state,
                    edgeButton = {
                        EdgeButton(
                            onClick = { clicks++ },
                            buttonSize = EdgeButtonSize.Large,
                            modifier = Modifier.testTag(TEST_TAG),
                        ) {
                            Text("Action")
                        }
                    },
                    edgeButtonSpacing = 0.dp,
                ) { contentPadding ->
                    ScalingLazyColumn(
                        state = state,
                        autoCentering = null,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize().testTag(scrollTag),
                    ) {
                        items(5) {
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                label = { Text("Item $it") },
                            )
                        }
                    }
                }
            }
        }

        // Scroll to the bottom so the EdgeButton is fully expanded, then verify edge clicks.
        rule.onNodeWithTag(scrollTag).performTouchInput { repeat(3) { swipeUp() } }
        rule.waitForIdle()
        verifyClicksAroundEdgeButton(getClicks = { clicks })

        // Partially scroll back down so the EdgeButton is scaled/constrained to a smaller height,
        // then verify clicking around the scaled edge still triggers inside and ignores outside.
        rule.onNodeWithTag(scrollTag).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 60f)
        }
        rule.waitForIdle()
        verifyClicksAroundEdgeButton(getClicks = { clicks })
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun draws_focus_ring_when_focused() {
        rule.verifyFocusRingDrawnAfterFocusInteraction { modifier, interactionSource ->
            EdgeButton(onClick = {}, modifier = modifier, interactionSource = interactionSource) {
                Text("Test")
            }
        }
    }

    private fun verifyStandaloneClicksAroundEdges(buttonSize: EdgeButtonSize) {
        var clicks = 0
        rule.setContentWithTheme {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.BottomCenter) {
                EdgeButton(
                    onClick = { clicks++ },
                    buttonSize = buttonSize,
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Text("Test")
                }
            }
        }
        rule.waitForIdle()
        verifyClicksAroundEdgeButton(getClicks = { clicks })
    }

    /**
     * Verifies that clicking just inside the [EdgeButton]'s curved perimeter triggers `onClick`,
     * and clicking just outside the border does not trigger `onClick`, at 8 positions around the
     * edge (top, bottom, left, right, top-left corner curve, top-right corner curve, bottom-left
     * curved edge, and bottom-right curved edge).
     */
    private fun verifyClicksAroundEdgeButton(getClicks: () -> Int) {
        val node = rule.onNodeWithTag(TEST_TAG)
        val nodeBounds = node.fetchSemanticsNode().boundsInRoot
        val rowWidth = nodeBounds.width
        val rowHeight = nodeBounds.height

        val helper = ShapeHelper(rule.density)
        val outline =
            EdgeButtonShape(helper)
                .createOutline(Size(rowWidth, rowHeight), LayoutDirection.Ltr, rule.density)
                as Outline.Generic
        val shapePath = outline.path
        val pathBounds = shapePath.getBounds()

        fun isInsideShape(point: Offset): Boolean {
            val probe =
                Path().apply {
                    addRect(
                        Rect(point.x - 0.01f, point.y - 0.01f, point.x + 0.01f, point.y + 0.01f)
                    )
                }
            return !Path.combine(PathOperation.Intersect, shapePath, probe).isEmpty
        }

        fun findBoundaryPoint(inside: Offset, outside: Offset): Offset {
            var lo = inside
            var hi = outside
            repeat(20) {
                val mid = Offset((lo.x + hi.x) / 2f, (lo.y + hi.y) / 2f)
                if (isInsideShape(mid)) {
                    lo = mid
                } else {
                    hi = mid
                }
            }
            return Offset((lo.x + hi.x) / 2f, (lo.y + hi.y) / 2f)
        }

        val center = Offset(rowWidth / 2f, rowHeight / 2f)
        val verticalPaddingPx = with(rule.density) { EdgeButtonVerticalPadding.toPx() }
        // Target points outside the shape along 8 rays around the perimeter:
        // top, bottom, left, right, top-left corner, top-right corner, bottom-left curve,
        // and bottom-right curve.
        val exteriorTargets =
            listOf(
                Offset(center.x, -verticalPaddingPx),
                Offset(center.x, rowHeight + verticalPaddingPx),
                Offset(0f, center.y),
                Offset(rowWidth, center.y),
                Offset(pathBounds.left, 0f),
                Offset(pathBounds.right, 0f),
                Offset(pathBounds.left, rowHeight),
                Offset(pathBounds.right, rowHeight),
            )

        val deltaPx = with(rule.density) { 2.dp.toPx() }
        var expectedClicks = getClicks()

        for (target in exteriorTargets) {
            val boundary = findBoundaryPoint(center, target)
            val dx = target.x - center.x
            val dy = target.y - center.y
            val len = sqrt(dx * dx + dy * dy)
            val unitOutward = Offset(dx / len, dy / len)

            val insidePoint =
                Offset(
                    x = boundary.x - unitOutward.x * deltaPx,
                    y = boundary.y - unitOutward.y * deltaPx,
                )
            val outsidePoint =
                Offset(
                    x = boundary.x + unitOutward.x * deltaPx,
                    y = boundary.y + unitOutward.y * deltaPx,
                )

            // Clicking just outside the border should NOT trigger onClick.
            node.performTouchInput { click(outsidePoint) }
            rule.runOnIdle { assertEquals(expectedClicks, getClicks()) }

            // Clicking just inside the border SHOULD trigger onClick.
            node.performTouchInput { click(insidePoint) }
            expectedClicks++
            rule.runOnIdle { assertEquals(expectedClicks, getClicks()) }
        }
    }
}
