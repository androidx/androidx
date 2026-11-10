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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class ButtonGroupTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            ButtonGroup(modifier = Modifier.testTag(TEST_TAG)) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun two_items_equally_sized_by_default() =
        verifyWidths(
            2,
            expectedWidths = { availableSpace -> arrayOf(availableSpace / 2, availableSpace / 2) },
        )

    @Test
    fun two_items_one_double_size() =
        verifyWidths(
            2,
            expectedWidths = { availableSpace ->
                arrayOf(availableSpace / 3, availableSpace / 3 * 2)
            },
            minWidthAndWeights = arrayOf(25.dp to 1f, 25.dp to 2f),
        )

    @Test
    fun respects_min_width() =
        verifyWidths(
            2,
            expectedWidths = { availableSpace -> arrayOf(15.dp, availableSpace - 15.dp) },
            size = 100.dp,
            minWidthAndWeights = arrayOf(15.dp to 1f, 15.dp to 10f),
        )

    @Test
    fun three_equal_buttons() =
        verifyWidths(3, expectedWidths = { availableSpace -> Array(3) { availableSpace / 3 } })

    @Test
    fun three_equal_buttons_density_override() =
        verifyWidths(
            3,
            expectedWidths = { availableSpace -> Array(3) { availableSpace / 3 } },
            densityOverride = 1.5f,
        )

    @Test
    fun three_buttons_one_two_one() =
        verifyWidths(
            3,
            expectedWidths = { availableSpace ->
                arrayOf(availableSpace / 4, availableSpace / 2, availableSpace / 4)
            },
            minWidthAndWeights = arrayOf(25.dp to 1f, 25.dp to 2f, 25.dp to 1f),
        )

    @Test
    fun modifier_order_ignored() {
        val size = 150.dp
        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(size),
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
            ) {
                Box(Modifier.weight(1f).minWidth(30.dp).testTag("${TEST_TAG}0"))
                Box(Modifier.minWidth(30.dp).weight(1f).testTag("${TEST_TAG}1"))
                Box(Modifier.weight(2f).minWidth(30.dp).testTag("${TEST_TAG}2"))
                Box(Modifier.minWidth(30.dp).weight(2f).testTag("${TEST_TAG}3"))
            }
        }

        // Items 0 & 1 should be 60.dp, 2 & 3 should be 90.dp
        listOf(30.dp, 30.dp, 45.dp, 45.dp).forEachIndexed { index, dp ->
            rule.onNodeWithTag(TEST_TAG + index.toString()).assertWidthIsEqualTo(dp)
        }
    }

    @Test
    fun rtl_inverts_order() {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ButtonGroup(
                    modifier = Modifier.size(150.dp),
                    contentPadding = PaddingValues(0.dp),
                    spacing = 0.dp,
                ) {
                    Box(Modifier.testTag("${TEST_TAG}0"))
                    Box(Modifier.testTag("${TEST_TAG}1"))
                }
            }
        }

        rule.waitForIdle()

        val (n0Left, _) = rule.onNodeWithTag("${TEST_TAG}0").getXRange()
        val (_, n1Right) = rule.onNodeWithTag("${TEST_TAG}1").getXRange()

        assertEquals(n0Left, n1Right)
    }

    @Test
    fun modifiers_equal_when_properties_match() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        lateinit var weight1: Modifier
        lateinit var weight1Duplicate: Modifier
        lateinit var weight2: Modifier
        lateinit var minWidth30: Modifier
        lateinit var minWidth30Duplicate: Modifier
        lateinit var minWidth40: Modifier
        lateinit var chained1: Modifier
        lateinit var chained1Duplicate: Modifier
        lateinit var animateWidth1: Modifier
        lateinit var animateWidth1Duplicate: Modifier
        lateinit var animateWidth2: Modifier

        rule.setContentWithTheme {
            ButtonGroup {
                weight1 = Modifier.weight(1f)
                weight1Duplicate = Modifier.weight(1f)
                weight2 = Modifier.weight(2f)
                // Modifier.minWidth() leaves weight at its default Float.NaN.
                minWidth30 = Modifier.minWidth(30.dp)
                minWidth30Duplicate = Modifier.minWidth(30.dp)
                minWidth40 = Modifier.minWidth(40.dp)
                chained1 = Modifier.weight(1f).minWidth(30.dp)
                chained1Duplicate = Modifier.weight(1f).minWidth(30.dp)
                animateWidth1 = Modifier.animateWidth(interactionSource1)
                animateWidth1Duplicate = Modifier.animateWidth(interactionSource1)
                animateWidth2 = Modifier.animateWidth(interactionSource2)
                Box(Modifier.size(10.dp))
            }
        }

        rule.runOnIdle {
            assertEquals(weight1, weight1Duplicate)
            assertEquals(weight1.hashCode(), weight1Duplicate.hashCode())
            assertNotEquals(weight1, weight2)
            assertEquals(minWidth30, minWidth30Duplicate)
            assertEquals(minWidth30.hashCode(), minWidth30Duplicate.hashCode())
            assertNotEquals(minWidth30, minWidth40)
            assertNotEquals(weight1, minWidth30)
            assertEquals(chained1, chained1Duplicate)
            assertEquals(chained1.hashCode(), chained1Duplicate.hashCode())
            assertEquals(animateWidth1, animateWidth1Duplicate)
            assertEquals(animateWidth1.hashCode(), animateWidth1Duplicate.hashCode())
            assertNotEquals(animateWidth1, animateWidth2)
            assertNotEquals(weight1, animateWidth1)
        }
    }

    @Test
    fun weight_and_min_width_update_on_recomposition() {
        var firstWeight by mutableFloatStateOf(1f)
        var firstMinWidth by mutableStateOf(24.dp)
        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(150.dp),
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
            ) {
                Box(Modifier.weight(firstWeight).minWidth(firstMinWidth).testTag("${TEST_TAG}0"))
                Box(Modifier.weight(2f).minWidth(24.dp).testTag("${TEST_TAG}1"))
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(50.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(100.dp)

        firstWeight = 2f

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(75.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(75.dp)

        firstMinWidth = 100.dp

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(100.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(50.dp)
    }

    @Test
    fun animate_width_interaction_source_updates_on_recomposition() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        var currentInteractionSource by mutableStateOf(interactionSource1)

        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(150.dp),
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
                expansionWidth = 20.dp,
            ) {
                Box(Modifier.animateWidth(currentInteractionSource).testTag("${TEST_TAG}0"))
                Box(Modifier.testTag("${TEST_TAG}1"))
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(75.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(75.dp)

        currentInteractionSource = interactionSource2
        rule.waitForIdle()

        // Pressing the old InteractionSource should have no effect.
        interactionSource1.tryEmit(PressInteraction.Press(Offset.Zero))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(75.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(75.dp)

        // Pressing the new InteractionSource should expand the first item by expansionWidth (20.dp)
        // and shrink its neighbor by the same amount.
        val press = PressInteraction.Press(Offset.Zero)
        interactionSource2.tryEmit(press)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(95.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(55.dp)

        interactionSource2.tryEmit(PressInteraction.Release(press))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(75.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(75.dp)
    }

    @Test
    fun content_padding_exceeding_width_clamps_item_widths_to_zero() {
        val interactionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(100.dp),
                contentPadding = PaddingValues(horizontal = 100.dp),
                spacing = 0.dp,
            ) {
                Box(Modifier.animateWidth(interactionSource).testTag("${TEST_TAG}0"))
                Box(Modifier.testTag("${TEST_TAG}1"))
                Box(Modifier.testTag("${TEST_TAG}2"))
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(0.dp)

        val press = PressInteraction.Press(Offset.Zero)
        interactionSource.tryEmit(press)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(0.dp)
    }

    @Test
    fun spacing_exceeding_width_clamps_item_widths_to_zero() {
        val interactionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(10.dp),
                contentPadding = PaddingValues(0.dp),
                spacing = 10.dp,
            ) {
                // Extreme weights leave a float residual > 1e-4f in computeWidths'
                // remainingWeight, which trips its require check without the clamp.
                Box(Modifier.weight(10000f).animateWidth(interactionSource).testTag("${TEST_TAG}0"))
                Box(Modifier.weight(0.1f).testTag("${TEST_TAG}1"))
                Box(Modifier.weight(0.1f).testTag("${TEST_TAG}2"))
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(0.dp)

        val press = PressInteraction.Press(Offset.Zero)
        interactionSource.tryEmit(press)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(0.dp)
    }

    @Test
    fun expansion_width_larger_than_neighbor_clamps_to_zero() {
        val interactionSources = List(3) { MutableInteractionSource() }
        val weights = listOf(1f, 1f, 2f)

        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(160.dp),
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
                expansionWidth = 100.dp,
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier.minWidth(24.dp)
                            .weight(weights[index])
                            .animateWidth(interactionSources[index])
                            .testTag("$TEST_TAG$index")
                    )
                }
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)

        // Pressing the first item clamps its expansion to neighbor 1's width (40.dp).
        val press0 = PressInteraction.Press(Offset.Zero)
        interactionSources[0].tryEmit(press0)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(80.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)

        interactionSources[0].tryEmit(PressInteraction.Release(press0))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)

        // Pressing the middle item clamps its expansion on each side to min(40.dp, 80.dp) = 40.dp.
        val press1 = PressInteraction.Press(Offset.Zero)
        interactionSources[1].tryEmit(press1)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(120.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(40.dp)

        interactionSources[1].tryEmit(PressInteraction.Release(press1))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)

        // Pressing the last item clamps its expansion to neighbor 1's width (40.dp).
        val press2 = PressInteraction.Press(Offset.Zero)
        interactionSources[2].tryEmit(press2)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(0.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(120.dp)

        interactionSources[2].tryEmit(PressInteraction.Release(press2))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)
    }

    @Test
    fun pressing_two_adjacent_items_clamps_widths_and_preserves_total_width() {
        val interactionSources = List(3) { MutableInteractionSource() }
        val weights = listOf(1f, 1f, 2f)

        rule.setContentWithTheme {
            ButtonGroup(
                modifier = Modifier.size(160.dp),
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
                expansionWidth = 100.dp,
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier.minWidth(24.dp)
                            .weight(weights[index])
                            .animateWidth(interactionSources[index])
                            .testTag("$TEST_TAG$index")
                    )
                }
            }
        }

        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)

        // Pressing two adjacent items (0 and 1) before releasing either keeps all widths >= 0 and
        // preserves the total width (160.dp): item 0 first grows to 80.dp (shrinking item 1 to
        // 0.dp), then item 1 runs later in the loop and grows by 100.dp, taking 50.dp back from
        // pressed item 0 (80.dp -> 30.dp, below its 40.dp rest width, which is expected for this
        // extreme expansionWidth) and 50.dp from item 2 (80.dp -> 30.dp).
        val simultaneousPress0 = PressInteraction.Press(Offset.Zero)
        val simultaneousPress1 = PressInteraction.Press(Offset.Zero)
        interactionSources[0].tryEmit(simultaneousPress0)
        interactionSources[1].tryEmit(simultaneousPress1)
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(30.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(100.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(30.dp)

        interactionSources[0].tryEmit(PressInteraction.Release(simultaneousPress0))
        interactionSources[1].tryEmit(PressInteraction.Release(simultaneousPress1))
        rule.onNodeWithTag("${TEST_TAG}0").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}1").assertWidthIsEqualTo(40.dp)
        rule.onNodeWithTag("${TEST_TAG}2").assertWidthIsEqualTo(80.dp)
    }

    private fun SemanticsNodeInteraction.getXRange() =
        fetchSemanticsNode("Failed to retrieve bounds of the node.").let { node ->
            node.positionInRoot.x to node.positionInRoot.x + node.size.width
        }

    private fun verifyWidths(
        numItems: Int,
        expectedWidths: (Dp) -> Array<Dp>,
        size: Dp = 150.dp,
        spacing: Dp = 5.dp,
        minWidthAndWeights: Array<Pair<Dp, Float>> = Array(numItems) { 24.dp to 1f },
        densityOverride: Float? = null,
    ) {
        val horizontalPadding = 5.dp
        val actualExpectedWidths =
            expectedWidths(size - horizontalPadding * 2 - spacing * (numItems - 1))

        require(numItems == actualExpectedWidths.size)
        require(numItems == minWidthAndWeights.size)

        rule.setContentWithTheme {
            val currentDensity = LocalDensity.current
            val density =
                densityOverride?.let {
                    object : Density {
                        override val density: Float
                            get() = it

                        override val fontScale: Float
                            get() = 1f
                    }
                } ?: currentDensity

            CompositionLocalProvider(LocalDensity provides density) {
                ButtonGroup(
                    modifier = Modifier.size(size),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    spacing = spacing,
                ) {
                    repeat(numItems) { ix ->
                        Box(
                            modifier =
                                Modifier.testTag(TEST_TAG + (ix + 1).toString())
                                    .fillMaxSize()
                                    .weight(minWidthAndWeights[ix].second)
                                    .minWidth(minWidthAndWeights[ix].first)
                        )
                    }
                }
            }
        }

        repeat(numItems) {
            rule
                .onNodeWithTag(TEST_TAG + (it + 1).toString())
                .assertWidthIsEqualTo(actualExpectedWidths[it], 1.dp)
        }
    }
}
