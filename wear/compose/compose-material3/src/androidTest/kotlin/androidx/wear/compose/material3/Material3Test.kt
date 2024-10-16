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

package androidx.wear.compose.material3

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlin.math.abs
import org.junit.Assert
import org.junit.rules.TestName

/** Constant to emulate very big but finite constraints */
val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

/** Screen size constants for screenshot tests */
val SCREEN_SIZE_SMALL = 192
val SCREEN_SIZE_LARGE = 228

enum class ScreenSize(val size: Int) {
    SMALL(SCREEN_SIZE_SMALL),
    LARGE(SCREEN_SIZE_LARGE)
}

enum class ScreenShape(val isRound: Boolean) {
    ROUND_DEVICE(true),
    SQUARE_DEVICE(false)
}

/**
 * Provides a composable function that allows you to place your content in different screen
 * configurations within your UI tests. This is useful for testing how your composables behave on
 * different screen sizes and form factors (e.g. round or square screens).
 *
 * @param screenSizeDp The desired screen size in dp. The composable will be placed into a square
 *   box with this side length.
 * @param isRound An optional boolean value to specify if the simulated screen should be round. If
 *   `true`, the screen is considered round. If `false`, it is considered rectangular. If `null`,
 *   the original device's roundness setting is used.
 * @param content The composable content to be tested within the modified screen configuration.
 */
@Composable
fun ScreenConfiguration(
    screenSizeDp: Int,
    isRound: Boolean? = null,
    content: @Composable () -> Unit
) {
    val originalConfiguration = LocalConfiguration.current
    val originalContext = LocalContext.current

    val fixedScreenSizeConfiguration =
        remember(originalConfiguration) {
            Configuration(originalConfiguration).apply {
                screenWidthDp = screenSizeDp
                screenHeightDp = screenSizeDp
                if (isRound != null) {
                    screenLayout =
                        if (isRound) Configuration.SCREENLAYOUT_ROUND_YES
                        else Configuration.SCREENLAYOUT_ROUND_NO
                }
            }
        }
    originalContext.resources.configuration.updateFrom(fixedScreenSizeConfiguration)

    CompositionLocalProvider(
        LocalContext provides originalContext,
        LocalConfiguration provides fixedScreenSizeConfiguration
    ) {
        Box(
            modifier =
                Modifier.size(screenSizeDp.dp).background(MaterialTheme.colorScheme.background),
        ) {
            content()
        }
    }
}

/**
 * Valid characters for golden identifiers are [A-Za-z0-9_-] TestParameterInjector adds '[' +
 * parameter_values + ']' + ',' to the test name.
 */
fun TestName.goldenIdentifier(): String =
    methodName.replace("[", "_").replace("]", "").replace(",", "_")

internal const val TEST_TAG = "test-item"

@Composable
fun TestImage(iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Image(
        testImage,
        iconLabel,
        modifier = Modifier.fillMaxSize().testTag(iconLabel),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
fun TestIcon(modifier: Modifier = Modifier, iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Icon(
        imageVector = testImage,
        contentDescription = iconLabel,
        modifier = modifier.testTag(iconLabel)
    )
}

@Composable
fun CenteredText(text: String) {
    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
        Text(text)
    }
}

fun ComposeContentTestRule.setContentWithThemeForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    useUnmergedTree: Boolean = false,
    content: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        MaterialTheme {
            Box(
                Modifier.sizeIn(maxWidth = parentMaxWidth, maxHeight = parentMaxHeight)
                    .testTag("containerForSizeAssertion")
            ) {
                content()
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion", useUnmergedTree)
}

fun ComposeContentTestRule.textStyleOf(text: String): TextStyle {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(
        SemanticsActions.GetTextLayoutResult
    ) {
        it(textLayoutResults)
    }
    return textLayoutResults[0].layoutInput.style
}

fun ComposeContentTestRule.setContentWithTheme(
    modifier: Modifier = Modifier,
    composable: @Composable BoxScope.() -> Unit
) {
    setContent { MaterialTheme { Box(modifier = modifier, content = composable) } }
}

internal fun ComposeContentTestRule.verifyTapSize(
    expectedSize: Dp,
    content: @Composable (modifier: Modifier) -> Unit
) {
    setContentWithTheme { content(Modifier.testTag(TEST_TAG)) }
    waitForIdle()

    onNodeWithTag(TEST_TAG)
        .assertTouchHeightIsEqualTo(expectedSize)
        .assertTouchWidthIsEqualTo(expectedSize)
}

internal fun ComposeContentTestRule.verifyActualSize(
    expectedSize: Dp,
    content: @Composable (modifier: Modifier) -> Unit
) {
    setContentWithTheme { content(Modifier.testTag(TEST_TAG)) }
    waitForIdle()

    onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(expectedSize).assertWidthIsEqualTo(expectedSize)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyColors(
    status: Status,
    expectedContainerColor: @Composable () -> Color,
    expectedContentColor: @Composable () -> Color,
    applyAlphaForDisabled: Boolean = true,
    content: @Composable () -> Color
) {
    val testBackgroundColor = Color.White
    var finalExpectedContainerColor = Color.Transparent
    var finalExpectedContent = Color.Transparent
    var actualContentColor = Color.Transparent
    setContentWithTheme {
        finalExpectedContainerColor =
            if (status.enabled() || !applyAlphaForDisabled) {
                    expectedContainerColor()
                } else {
                    expectedContainerColor().copy(DisabledContentAlpha)
                }
                .compositeOver(testBackgroundColor)
        finalExpectedContent =
            if (status.enabled() || !applyAlphaForDisabled) {
                expectedContentColor()
            } else {
                expectedContentColor().copy(DisabledContentAlpha)
            }
        Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
            actualContentColor = content()
        }
    }
    Assert.assertEquals(finalExpectedContent, actualContentColor)
    onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(finalExpectedContainerColor)
}

/** Checks that [expectedColor] is in the percentage [range] of an [ImageBitmap] color histogram */
fun ImageBitmap.assertColorInPercentageRange(
    expectedColor: Color,
    range: ClosedFloatingPointRange<Float> = 50.0f..100.0f
) {
    val histogram = histogram()
    if (!histogram.containsKey(expectedColor)) {
        throw AssertionError("Expected color $expectedColor was not found in the bitmap.")
    }

    ((histogram[expectedColor]!! * 100f) / (width * height)).let { actualPercent ->
        if (actualPercent !in range) {
            throw AssertionError(
                "Expected color $expectedColor found " +
                    "$actualPercent%, not in the percentage range $range"
            )
        }
    }
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
internal fun SemanticsNodeInteraction.assertHeightIsEqualTo(
    expectedHeight: Dp,
    tolerance: Dp = Dp(0.5f)
): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.height.assertIsEqualTo(expectedHeight, "height", tolerance)
    }
}

private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
    assertion: (DpRect) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val bounds =
        with(node.root!!.density) {
            node.unclippedBoundsInRoot.let {
                DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
            }
        }
    assertion.invoke(bounds)
    return this
}

internal fun SemanticsNodeInteraction.assertOnLongClickLabelMatches(
    expectedValue: String
): SemanticsNodeInteraction {
    return assert(
        SemanticsMatcher("onLongClickLabel = '$expectedValue'") {
            it.config.getOrElseNullable(SemanticsActions.OnLongClick) { null }?.label ==
                expectedValue
        }
    )
}

private val SemanticsNode.unclippedBoundsInRoot: Rect
    get() {
        return if (layoutInfo.isPlaced) {
            Rect(positionInRoot, size.toSize())
        } else {
            Dp.Unspecified.value.let { Rect(it, it, it, it) }
        }
    }

/**
 * Returns if this value is equal to the [reference], within a given [tolerance]. If the reference
 * value is [Float.NaN], [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY], this only returns
 * true if this value is exactly the same (tolerance is disregarded).
 */
private fun Dp.isWithinTolerance(reference: Dp, tolerance: Dp): Boolean {
    return when {
        reference.isUnspecified -> this.isUnspecified
        reference.value.isInfinite() -> this.value == reference.value
        else -> abs(this.value - reference.value) <= tolerance.value
    }
}

/**
 * Asserts that this value is equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 * @throws AssertionError if comparison fails.
 */
internal fun Dp.assertIsEqualTo(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!isWithinTolerance(expected, tolerance)) {
        // Comparison failed, report the error in DPs
        throw AssertionError("Actual $subject is $this, expected $expected (tolerance: $tolerance)")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyScreenshot(
    methodName: String,
    screenshotRule: AndroidXScreenshotTestRule,
    testTag: String = TEST_TAG,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    setContentWithTheme {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }

    onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, methodName)
}

@RequiresApi(Build.VERSION_CODES.O)
fun ComposeContentTestRule.verifyRoundedButtonTapAnimationEnd(
    baseShape: RoundedCornerShape,
    pressedShape: RoundedCornerShape,
    targetProgress: Float,
    color: @Composable () -> Color,
    content: @Composable (Modifier) -> Unit
) {
    val expectedAnimationEnd =
        AnimatedRoundedCornerShape(baseShape, pressedShape) { targetProgress }
    var fillColor = Color.Transparent

    setContent {
        fillColor = color()
        content(Modifier.testTag(TEST_TAG))
    }

    mainClock.autoAdvance = false
    onNodeWithTag(TEST_TAG).performClick()

    /**
     * We are manually advancing by a fixed amount of frames since
     * 1) the RoundButton.animateButtonShape is internal and therefore we cannot modify the
     *    animation spec being used. Otherwise, we could set a custom animation time isolated and
     *    known to this test we could wait for.
     * 2) rule.mainClock.waitUntil expects a condition. However, the shape validations for
     *    ImageBitMap only includes of assets
     */
    repeat(8) { mainClock.advanceTimeByFrame() }

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertShape(
            density = density,
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp,
            shapeColor = fillColor,
            backgroundColor = Color.Transparent,
            antiAliasingGap = 2.0f,
            shape = expectedAnimationEnd,
        )
}

private fun ImageBitmap.histogram(): MutableMap<Color, Long> {
    val pixels = this.toPixelMap()
    val histogram = mutableMapOf<Color, Long>()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = pixels[x, y]
            histogram[color] = histogram.getOrDefault(color, 0) + 1
        }
    }
    return histogram
}

internal enum class Status {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}

class StableRef<T>(var value: T)
