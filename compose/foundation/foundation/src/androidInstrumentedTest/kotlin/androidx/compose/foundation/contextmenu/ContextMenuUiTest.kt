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

package androidx.compose.foundation.contextmenu

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun IntOffset.dx(dx: Int): IntOffset = copy(x = x + dx)

private fun IntOffset.dy(dy: Int): IntOffset = copy(y = y + dy)

private val TestColors =
    ContextMenuColors(
        backgroundColor = Color.White,
        textColor = Color.Black,
        iconColor = Color.Blue,
        disabledTextColor = Color.Red,
        disabledIconColor = Color.Green,
    )

@RunWith(AndroidJUnit4::class)
@MediumTest
class ContextMenuUiTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "testTag"
    private val longText = "M ".repeat(200).trimEnd()

    @Composable
    private fun TestColumn(
        colors: ContextMenuColors = TestColors,
        contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
    ) {
        ContextMenuColumn(colors, Modifier.testTag(tag)) {
            val scope = remember { ContextMenuScope() }
            with(scope) {
                clear()
                contextMenuBuilderBlock()
                Content(colors)
            }
        }
    }

    // region ContextMenuItem Tests
    @Composable
    private fun TestItem(
        label: String = "Item",
        enabled: Boolean = true,
        colors: ContextMenuColors = TestColors,
        modifier: Modifier = Modifier.testTag(tag),
        leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
        onClick: () -> Unit = {},
    ) {
        ContextMenuItem(
            label = label,
            enabled = enabled,
            colors = colors,
            modifier = modifier,
            leadingIcon = leadingIcon,
            onClick = onClick
        )
    }

    @Test
    fun whenContextMenuItem_enabled_isClicked_onClickTriggers() {
        var onClickCount = 0
        rule.setContent { TestItem(label = "Test") { onClickCount++ } }

        rule.onNodeWithTag(tag).performClick()
        assertThat(onClickCount).isEqualTo(1)
    }

    @Test
    fun whenContextMenuItem_disabled_isClicked_noActions() {
        var onClickCount = 0
        rule.setContent { TestItem(label = "Test", enabled = false) { onClickCount++ } }

        rule.onNodeWithTag(tag).performClick()
        assertThat(onClickCount).isEqualTo(0)
    }

    @Test
    fun whenContextMenuItem_withMinSize_sizeIsAsExpected() {
        rule.setContent {
            // emulate the context menu column asking for max intrinsic width
            Box(Modifier.width(IntrinsicSize.Max)) { TestItem(label = "M") }
        }

        rule.onNodeWithTag(tag).run {
            assertHeightIsEqualTo(ContextMenuSpec.ListItemHeight)
            assertWidthIsEqualTo(ContextMenuSpec.ContainerWidthMin)
        }
    }

    @Test
    fun whenContextMenuItem_withMaxSize_sizeIsAsExpected() {
        rule.setContent {
            // emulate the context menu column asking for max intrinsic width
            Box(Modifier.width(IntrinsicSize.Max)) {
                TestItem(
                    label = "M".repeat(200),
                    leadingIcon = { color ->
                        Box(modifier = Modifier.background(color).fillMaxSize())
                    },
                )
            }
        }

        rule.onNodeWithTag(tag).run {
            assertHeightIsEqualTo(ContextMenuSpec.ListItemHeight)
            assertWidthIsEqualTo(ContextMenuSpec.ContainerWidthMax)
        }
    }

    @Test
    fun whenContextMenuItem_withLeadingIconMaxSpace_iconSizeIsAsExpected() {
        rule.setContent {
            // emulate the context menu column asking for max intrinsic width
            Box(Modifier.width(IntrinsicSize.Max)) {
                TestItem(
                    modifier = Modifier,
                    leadingIcon = { color ->
                        Box(modifier = Modifier.testTag(tag).background(color).fillMaxSize())
                    },
                )
            }
        }

        val interaction = rule.onNodeWithTag(tag, useUnmergedTree = true)
        interaction.assertWidthIsEqualTo(ContextMenuSpec.IconSize)
        interaction.assertHeightIsEqualTo(ContextMenuSpec.IconSize)

        // Assumption: The immediate parent of the tagged composable is the box with requiredSizeIn.
        val parentSize =
            interaction.fetchSemanticsNode().layoutInfo.parentInfo?.run {
                with(density) { DpSize(width.toDp(), height.toDp()) }
            } ?: fail("Parent layout of empty box not found.")

        parentSize.width.assertIsEqualTo(ContextMenuSpec.IconSize, subject = "parent.width")
        parentSize.height.assertIsEqualTo(ContextMenuSpec.IconSize, subject = "parent.height")
    }

    @Test
    fun whenContextMenuItem_withLeadingIconMinSize_iconSizeIsAsExpected() {
        rule.setContent {
            // emulate the context menu column asking for max intrinsic width
            Box(Modifier.width(IntrinsicSize.Max)) {
                TestItem(modifier = Modifier, leadingIcon = { Spacer(Modifier.testTag(tag)) })
            }
        }

        val interaction = rule.onNodeWithTag(tag, useUnmergedTree = true)
        interaction.assertWidthIsEqualTo(0.dp)
        interaction.assertHeightIsEqualTo(0.dp)

        // Assumption: The immediate parent of the tagged composable is the box with requiredSizeIn.
        val parentSize =
            interaction.fetchSemanticsNode().layoutInfo.parentInfo?.run {
                with(density) { DpSize(width.toDp(), height.toDp()) }
            } ?: fail("Parent layout of empty box not found.")

        parentSize.width.assertIsEqualTo(ContextMenuSpec.IconSize, subject = "parent.width")
        parentSize.height.assertIsEqualTo(0.dp, subject = "parent.height")
    }

    @Test
    fun whenContextMenuItem_enabled_semanticsIsCorrect() {
        var onClickCount = 0
        val label = "Test"
        rule.setContent { TestItem(label = label) { onClickCount++ } }

        rule.onNodeWithTag(tag).apply {
            assertHasClickAction()
            val node = fetchSemanticsNode("No Semantics Node found for ContextMenuItem")

            val accessibilityAction = node.config[SemanticsActions.OnClick]
            assertThat(accessibilityAction.label).isEqualTo(label)

            val action = accessibilityAction.action
            assertThat(action).isNotNull()
            assertThat(onClickCount).isEqualTo(0)
            action!!.invoke()
            assertThat(onClickCount).isEqualTo(1)
        }
    }

    @Test
    fun whenContextMenuItem_disabled_semanticsIsCorrect() {
        var onClickCount = 0
        val label = "Test"
        rule.setContent { TestItem(label = label, enabled = false) { onClickCount++ } }

        rule.onNodeWithTag(tag).apply {
            assertHasClickAction()
            val node = fetchSemanticsNode("No Semantics Node found for ContextMenuItem")

            val accessibilityAction = node.config[SemanticsActions.OnClick]
            assertThat(accessibilityAction.label).isEqualTo(label)

            val action = accessibilityAction.action
            assertThat(action).isNotNull()
            assertThat(onClickCount).isEqualTo(0)
            action!!.invoke()
            assertThat(onClickCount).isEqualTo(0)
        }
    }

    @Test
    fun whenContextMenuItem_withLongLabel_doesNotWrap() {
        rule.setContent { TestItem(label = longText) }

        val textLayoutResult =
            rule.onNode(hasText(longText), useUnmergedTree = true).fetchTextLayoutResult()

        assertThat(textLayoutResult.lineCount).isEqualTo(1)
    }

    @Test
    fun whenContextMenuItem_enabled_correctTextStyling() {
        rule.setContent { TestItem(label = longText) }

        val textNode = rule.onNode(hasText(longText), useUnmergedTree = true)
        textNode.assertExists("Text does not exist.")

        val textStyle = textNode.fetchTextLayoutResult().layoutInput.style
        assertThat(textStyle.color).isEqualTo(TestColors.textColor)
        assertThat(textStyle.textAlign).isEqualTo(ContextMenuSpec.LabelHorizontalTextAlignment)
        assertThat(textStyle.fontSize).isEqualTo(ContextMenuSpec.FontSize)
        assertThat(textStyle.fontWeight).isEqualTo(ContextMenuSpec.FontWeight)
        assertThat(textStyle.lineHeight).isEqualTo(ContextMenuSpec.LineHeight)
        assertThat(textStyle.letterSpacing).isEqualTo(ContextMenuSpec.LetterSpacing)
    }

    @Test
    fun whenContextMenuItem_disabled_correctTextStyling() {
        rule.setContent { TestItem(label = longText, enabled = false) }

        val textNode = rule.onNode(hasText(longText), useUnmergedTree = true)
        textNode.assertExists("Text does not exist.")

        val textStyle = textNode.fetchTextLayoutResult().layoutInput.style
        assertThat(textStyle.color).isEqualTo(TestColors.disabledTextColor)
        assertThat(textStyle.textAlign).isEqualTo(ContextMenuSpec.LabelHorizontalTextAlignment)
        assertThat(textStyle.fontSize).isEqualTo(ContextMenuSpec.FontSize)
        assertThat(textStyle.fontWeight).isEqualTo(ContextMenuSpec.FontWeight)
        assertThat(textStyle.lineHeight).isEqualTo(ContextMenuSpec.LineHeight)
        assertThat(textStyle.letterSpacing).isEqualTo(ContextMenuSpec.LetterSpacing)
    }

    @Test
    fun whenContextMenuItem_enabled_correctIconColor() {
        var iconColor: Color? = null
        rule.setContent { TestItem(label = longText, leadingIcon = { iconColor = it }) }
        assertThat(iconColor).isEqualTo(TestColors.iconColor)
    }

    @Test
    fun whenContextMenuItem_disabled_correctIconColor() {
        var iconColor: Color? = null
        rule.setContent {
            TestItem(label = longText, enabled = false, leadingIcon = { iconColor = it })
        }
        assertThat(iconColor).isEqualTo(TestColors.disabledIconColor)
    }

    // endregion ContextMenuItem Tests

    // region ContextMenuColumn Tests
    @Test
    fun whenContextMenuColumn_usedNormally_allInputItemsAreRendered() {
        val labelFunction: (Int) -> String = { "Item $it" }
        rule.setContent { TestColumn { repeat(5) { testItem(label = labelFunction(it)) } } }

        rule.onNodeWithTag(tag).assertIsDisplayed()
        repeat(5) { rule.onNode(hasText(labelFunction(it))).assertExists() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColumn_usedNormally_hasExpectedShadow() {
        val containerTag = "containerTag"
        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(containerTag).padding(ContextMenuSpec.MenuContainerElevation)
            ) {
                TestColumn { testItem() }
            }
        }

        val outerNode = rule.onNodeWithTag(containerTag)
        val pixelMap = outerNode.captureToImage().toPixelMap()
        val outerRect = outerNode.fetchSemanticsNode().boundsInRoot

        val innerRect = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

        val columnBoundsInParent = innerRect.translate(-outerRect.topLeft).roundToIntRect()

        // Verify that the center of each side in the shadow is not the background color.
        // Check one pixel outwards from the column's bounds.
        fun assertShadowAt(offset: IntOffset) {
            val (x, y) = offset
            val pixelColor = pixelMap[x, y]
            val message = "Expected pixel at [$x, $y] to not be ${TestColors.backgroundColor}."
            assertWithMessage(message).that(pixelColor).isNotEqualTo(TestColors.backgroundColor)
        }

        assertShadowAt(columnBoundsInParent.centerLeft.dx(-1))
        assertShadowAt(columnBoundsInParent.topCenter.dy(-1))
        assertShadowAt(columnBoundsInParent.centerRight.dx(1))
        assertShadowAt(columnBoundsInParent.bottomCenter.dy(1))
    }

    @Test
    fun whenContextMenuColumn_allInputItemsAreRendered() {
        val labelFunction: (Int) -> String = { "Item $it" }
        rule.setContent { TestColumn { repeat(5) { testItem(label = labelFunction(it)) } } }

        rule.onNodeWithTag(tag).assertIsDisplayed()
        repeat(5) { rule.onNode(hasText(labelFunction(it))).assertExists() }
    }

    @Test
    fun whenContextMenuColumn_5ItemsMaxWidth_sizeIsAsExpected() {
        rule.setContent {
            TestColumn { repeat(5) { testItem(label = "Item ${it.toString().repeat(100)}") } }
        }

        rule.onNodeWithTag(tag).run {
            // 5 items and the vertical padding.
            assertHeightIsEqualTo(
                ContextMenuSpec.ListItemHeight * 5 + ContextMenuSpec.VerticalPadding * 2
            )
            assertWidthIsEqualTo(ContextMenuSpec.ContainerWidthMax)
        }
    }

    @Test
    fun whenContextMenuColumn_1ItemMinWidth_sizeIsAsExpected() {
        rule.setContent { TestColumn { testItem() } }

        rule.onNodeWithTag(tag).run {
            // 1 item and the vertical padding.
            assertHeightIsEqualTo(
                ContextMenuSpec.ListItemHeight + ContextMenuSpec.VerticalPadding * 2
            )
            assertWidthIsEqualTo(ContextMenuSpec.ContainerWidthMin)
        }
    }

    @Test
    fun whenContextMenuColumn_with100Items_scrolls() {
        rule.setContent { TestColumn { repeat(100) { testItem(label = "Item ${it + 1}") } } }

        val firstItemMatcher = hasText("Item 1")
        val lastItemMatcher = hasText("Item 100")

        rule.onNode(firstItemMatcher).assertIsDisplayed()
        rule.onNode(lastItemMatcher).assertIsNotDisplayed()

        rule.onNodeWithTag(tag).performScrollToNode(lastItemMatcher)

        rule.onNode(firstItemMatcher).assertIsNotDisplayed()
        rule.onNode(lastItemMatcher).assertIsDisplayed()
    }

    // Items may have varying text sizes. We want to ensure the clickable portion of
    // the Row extends all the way to the end of the Column horizontally.
    @Test
    fun whenContextMenuColumn_withItemsOfDifferentWidths_clickBoxCoversEndOfItems() {
        val size = 5
        val clickCounts = IntArray(size)
        rule.setContent {
            TestColumn {
                repeat(5) {
                    val suffix = "Item ".repeat(it).trimEnd()
                    testItem(label = "Item $suffix") { clickCounts[it]++ }
                }
            }
        }

        val listItemCenterBaselineOffset = ContextMenuSpec.ListItemHeight * 0.5f
        rule.onNodeWithTag(tag).performTouchInput {
            val x = right - 1f
            with(rule.density) {
                repeat(size) {
                    val y = ContextMenuSpec.ListItemHeight * it + listItemCenterBaselineOffset
                    click(Offset(x, y.toPx()))
                }
            }
        }

        assertWithMessage("Each item should have been clicked once.")
            .that(clickCounts)
            .asList()
            .containsExactly(1, 1, 1, 1, 1)
    }

    // endregion ContextMenuColumn Tests

    // region ContextMenuColor Tests
    private val testColor = Color.Red

    /** Without a baseline background color, the background color is non-deterministic. */
    private val testBackgroundColor = Color.White

    private fun transparentColors(
        backgroundColor: Color = testBackgroundColor,
        textColor: Color = Color.Transparent,
        iconColor: Color = Color.Transparent,
        disabledTextColor: Color = Color.Transparent,
        disabledIconColor: Color = Color.Transparent,
    ): ContextMenuColors =
        ContextMenuColors(
            backgroundColor = backgroundColor,
            textColor = textColor,
            iconColor = iconColor,
            disabledTextColor = disabledTextColor,
            disabledIconColor = disabledIconColor,
        )

    /**
     * Threshold % of pixels that must match a certain color. This filters out outlier colors
     * resulting from anti-aliasing.
     */
    private val filterThresholdPercentage = 0.02f

    /**
     * Gets all the colors in the tagged node. Removes any padding and outlier colors from
     * anti-aliasing from the result.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getColors(): Set<Color> {
        val node = rule.onNodeWithTag(tag)
        val bitmap = node.captureToImage()
        val density = node.fetchSemanticsNode().layoutInfo.density

        // Ignore some padding around the edges where the shadow/rounded corners are.
        val padding = with(density) { ContextMenuSpec.CornerRadius.toPx().times(4f).ceilToIntPx() }

        val pixelMap =
            bitmap.toPixelMap(
                startX = padding,
                startY = padding,
                width = bitmap.width - padding,
                height = bitmap.height - padding,
            )

        val pixelColorHistogram =
            buildMap<Color, Int> {
                for (x in 0 until pixelMap.width) {
                    for (y in 0 until pixelMap.height) {
                        val pixelColor = pixelMap[x, y]
                        merge(pixelColor, 1) { old, new -> old + new }
                    }
                }
            }

        val threshold = pixelMap.width * pixelMap.height * filterThresholdPercentage
        return pixelColorHistogram.filterValues { it >= threshold }.keys
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNonBackgroundColors(): Set<Color> = getColors() - testBackgroundColor

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_transparentExceptBackground_backgroundColorIsAsExpected() {
        val colors = transparentColors(backgroundColor = testColor)
        rule.setContent { TestColumn(colors) { testItem() } }
        assertThatColors(getColors()).containsExactly(testColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_transparentExceptText_textColorIsAsExpected() {
        val colors = transparentColors(textColor = testColor)
        rule.setContent { TestColumn(colors) { testItem(label = "M".repeat(10)) } }
        assertThatColors(getNonBackgroundColors()).containsExactly(testColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_transparentExceptDisabledText_textColorIsAsExpected() {
        val colors = transparentColors(disabledTextColor = testColor)
        rule.setContent { TestColumn(colors) { testItem(label = "M".repeat(10), enabled = false) } }
        assertThatColors(getNonBackgroundColors()).containsExactly(testColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_transparentExceptIcon_iconColorIsAsExpected() {
        val colors = transparentColors(iconColor = testColor)
        rule.setContent {
            TestColumn(colors) {
                testItem(
                    leadingIcon = { iconColor ->
                        Box(Modifier.background(iconColor).fillMaxSize())
                    },
                )
            }
        }
        assertThatColors(getNonBackgroundColors()).containsExactly(testColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_transparentExceptDisabledIcon_iconColorIsAsExpected() {
        val colors = transparentColors(disabledIconColor = testColor)
        rule.setContent {
            TestColumn(colors) {
                testItem(
                    enabled = false,
                    leadingIcon = { iconColor ->
                        Box(Modifier.background(iconColor).fillMaxSize())
                    },
                )
            }
        }
        assertThatColors(getNonBackgroundColors()).containsExactly(testColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuColors_enabledToggled_colorsChangeAsExpected() {
        val enabledColor = Color.Red
        val disabledColor = Color.Blue
        val colors =
            transparentColors(
                textColor = enabledColor,
                iconColor = enabledColor,
                disabledTextColor = disabledColor,
                disabledIconColor = disabledColor,
            )

        var enabled by mutableStateOf(true)
        rule.setContent {
            TestColumn(colors) {
                testItem(
                    label = "M".repeat(5),
                    enabled = enabled,
                    leadingIcon = { iconColor ->
                        Box(Modifier.background(iconColor).fillMaxSize())
                    },
                )
            }
        }

        repeat(2) {
            enabled = true
            rule.waitForIdle()
            assertThatColors(getNonBackgroundColors()).containsExactly(enabledColor)

            enabled = false
            rule.waitForIdle()
            assertThatColors(getNonBackgroundColors()).containsExactly(disabledColor)
        }
    }

    // endregion ContextMenuColor Tests

    // region ContextMenuPopup Tests
    private val centeringPopupPositionProvider =
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = windowSize.center - popupContentSize.center
        }

    @Test
    fun whenContextMenuPopup_removedAndReAdded_appearsAsExpected() {
        var show by mutableStateOf(false)
        val itemTag = "itemTag"
        rule.setContent {
            if (show) {
                ContextMenuPopup(
                    modifier = Modifier.testTag(tag),
                    popupPositionProvider = centeringPopupPositionProvider,
                    onDismiss = {},
                ) {
                    testItem(label = "Item", modifier = Modifier.testTag(itemTag))
                }
            }
        }

        repeat(2) {
            show = false
            rule.waitForIdle()

            rule.onNodeWithTag(tag).assertDoesNotExist()
            rule.onNodeWithTag(itemTag).assertDoesNotExist()

            show = true
            rule.waitForIdle()

            rule.onNodeWithTag(tag).assertExists()
            rule.onNodeWithTag(itemTag).assertExists()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun whenContextMenuPopup_enabledDisabled_colorsAreAsExpected() {
        var enabled by mutableStateOf(true)
        rule.setContent {
            ContextMenuPopup(
                modifier = Modifier.testTag(tag),
                popupPositionProvider = centeringPopupPositionProvider,
                colors = TestColors,
                onDismiss = {},
            ) {
                testItem(
                    label = "M".repeat(10),
                    enabled = enabled,
                    leadingIcon = { iconColor ->
                        Box(Modifier.background(iconColor).fillMaxSize())
                    },
                )
            }
        }

        repeat(2) {
            enabled = true
            rule.waitForIdle()
            assertThatColors(getColors())
                .containsExactly(
                    TestColors.backgroundColor,
                    TestColors.textColor,
                    TestColors.iconColor
                )

            enabled = false
            rule.waitForIdle()
            assertThatColors(getColors())
                .containsExactly(
                    TestColors.backgroundColor,
                    TestColors.disabledTextColor,
                    TestColors.disabledIconColor
                )
        }
    }

    // endregion ContextMenuPopup Tests

    // region computeContextMenuColors Tests
    private val testStyleId = androidx.compose.foundation.test.R.style.TestContextMenuStyle
    private val emptyStyleId = androidx.compose.foundation.test.R.style.TestContextMenuEmptyStyle

    @Test
    fun computeContextMenuColors_resolvesStylesAsExpected() {
        lateinit var actualColors: ContextMenuColors
        rule.setContent {
            actualColors =
                computeContextMenuColors(
                    backgroundStyleId = testStyleId,
                    foregroundStyleId = testStyleId,
                )
        }

        assertThatColor(actualColors.backgroundColor).isFuzzyEqualTo(Color.Red)
        assertThatColor(actualColors.textColor).isFuzzyEqualTo(Color.Blue)
        assertThatColor(actualColors.iconColor).isFuzzyEqualTo(Color.Blue)
        assertThatColor(actualColors.disabledTextColor).isFuzzyEqualTo(Color.Green)
        assertThatColor(actualColors.disabledIconColor).isFuzzyEqualTo(Color.Green)
    }

    @Test
    fun computeContextMenuColors_defaultsAsExpected() {
        lateinit var actualColors: ContextMenuColors
        rule.setContent {
            actualColors =
                computeContextMenuColors(
                    backgroundStyleId = emptyStyleId,
                    foregroundStyleId = emptyStyleId,
                )
        }

        assertThatColor(actualColors.backgroundColor)
            .isFuzzyEqualTo(DefaultContextMenuColors.backgroundColor)
        assertThatColor(actualColors.textColor).isFuzzyEqualTo(DefaultContextMenuColors.textColor)
        assertThatColor(actualColors.iconColor).isFuzzyEqualTo(DefaultContextMenuColors.textColor)
        assertThatColor(actualColors.disabledTextColor)
            .isFuzzyEqualTo(DefaultContextMenuColors.disabledTextColor)
        assertThatColor(actualColors.disabledIconColor)
            .isFuzzyEqualTo(DefaultContextMenuColors.disabledTextColor)
    }
    // endregion computeContextMenuColors Tests
}
