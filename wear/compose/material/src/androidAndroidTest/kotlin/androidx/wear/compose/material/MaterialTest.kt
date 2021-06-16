/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.round

/**
 * Constant to emulate very big but finite constraints
 */
val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

internal const val TEST_TAG = "test-item"

fun ComposeContentTestRule.setContentWithTheme(
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        MaterialTheme {
            Surface(modifier = modifier, content = composable)
        }
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
            Surface {
                Box {
                    Box(
                        Modifier.sizeIn(
                            maxWidth = parentMaxWidth,
                            maxHeight = parentMaxHeight
                        ).testTag("containerForSizeAssertion")
                    ) {
                        content()
                    }
                }
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion", useUnmergedTree = useUnmergedTree)
}

fun ComposeContentTestRule.textStyleOf(text: String): TextStyle {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    onNodeWithText(text, useUnmergedTree = true)
        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(textLayoutResults)
        }
    return textLayoutResults[0].layoutInput.style
}

fun assertTextStyleEquals(expectedStyle: TextStyle, actualStyle: TextStyle) {
    Assert.assertEquals(expectedStyle.fontSize, actualStyle.fontSize)
    Assert.assertEquals(expectedStyle.fontFamily, actualStyle.fontFamily)
    Assert.assertEquals(expectedStyle.letterSpacing, actualStyle.letterSpacing)
    Assert.assertEquals(expectedStyle.fontWeight, actualStyle.fontWeight)
    Assert.assertEquals(expectedStyle.lineHeight, actualStyle.lineHeight)
}

@Composable
fun CreateImage(iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Image(
        testImage, iconLabel,
        modifier = Modifier.fillMaxSize().testTag(iconLabel),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
fun TestIcon(modifier: Modifier = Modifier, iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Icon(
        imageVector = testImage, contentDescription = iconLabel,
        modifier = modifier
            .testTag(iconLabel)
    )
}

/**
 * assertContainsColor - uses a threshold on an ImageBitmap's color distribution
 * to check that a UI element is predominantly the expected color.
 */
fun ImageBitmap.assertContainsColor(expectedColor: Color, minPercent: Float = 50.0f) {
    val histogram = histogram()
    if (!histogram.containsKey(expectedColor)) {
        throw AssertionError("Expected color $expectedColor was not found in the bitmap.")
    }

    val actualPercent = round((histogram[expectedColor]!! * 100f) / (width * height))
    if (actualPercent < minPercent) {
        throw AssertionError(
            "Expected color $expectedColor found $actualPercent%, below threshold $minPercent%"
        )
    }
}

/**
 * printHistogramToLog - utility for writing an ImageBitmap's color distribution to debug log.
 * The histogram can be extracted using adb logcat, for example:
 *   adb logcat | grep Histogram
 * This can be useful when debugging a captured image from a compose UI element.
 */
@kotlin.ExperimentalUnsignedTypes
fun ImageBitmap.printHistogramToLog(expectedColor: Color): ImageBitmap {
    val n = width * height
    Log.d("Histogram", "---------------------------------------------------------------")
    val expectedColorInHex = expectedColor.toHexString()
    Log.d("Histogram", "Expecting color $expectedColorInHex")
    for ((key, value) in histogram()) {
        val percent = 100 * value / n
        val colorInHex = key.toHexString()
        Log.d("Histogram", "$colorInHex : $value ($percent)")
    }

    return this
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
internal fun SemanticsNodeInteraction.assertHeightIsEqualTo(
    expectedHeight: Dp,
    tolerance: Dp = Dp(0.5f)
):
    SemanticsNodeInteraction {
        return withUnclippedBoundsInRoot {
            it.height.assertIsEqualTo(expectedHeight, "height", tolerance)
        }
    }

private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
    assertion: (DpRect) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val bounds = with(node.root!!.density) {
        node.unclippedBoundsInRoot.let {
            DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
        }
    }
    assertion.invoke(bounds)
    return this
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
 * Returns if this value is equal to the [reference], within a given [tolerance]. If the
 * reference value is [Float.NaN], [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY], this
 * only returns true if this value is exactly the same (tolerance is disregarded).
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
 *
 * @throws AssertionError if comparison fails.
 */
private fun Dp.assertIsEqualTo(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!isWithinTolerance(expected, tolerance)) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected $expected (tolerance: $tolerance)"
        )
    }
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

@kotlin.ExperimentalUnsignedTypes
private fun Color.toHexString() =
    this.value.toString(16)

/**
 * writeToDevice - utility for writing an image bitmap to storage on the emulated device.
 * The image can be extract using adb pull, for example:
 * adb pull
 *   /storage/emulated/0/Android/data/androidx.wear.compose.test/cache/screenshots/mytest.png
 *   /usr/local/username/Desktop/mytest.png
 */
fun ImageBitmap.writeToDevice(testName: String) {
    this.asAndroidBitmap().writeToDevice(testName)
}

private val deviceOutputDirectory
    get() = File(
        InstrumentationRegistry.getInstrumentation().context.externalCacheDir,
        "screenshots"
    )

private fun Bitmap.writeToDevice(testName: String): File {
    return writeToDevice(testName) {
        compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
    }
}

private fun writeToDevice(
    testName: String,
    writeAction: (FileOutputStream) -> Unit
): File {
    if (!deviceOutputDirectory.exists() && !deviceOutputDirectory.mkdir()) {
        throw IOException("Could not create folder $deviceOutputDirectory")
    }

    val file = File(deviceOutputDirectory, "$testName.png")
    Log.d("Screenshot", "File path is ${file.absolutePath}")
    try {
        FileOutputStream(file).use {
            writeAction(it)
        }
    } catch (e: Exception) {
        throw IOException(
            "Could not write file to storage (path: ${file.absolutePath}). " +
                " Stacktrace: " + e.stackTrace
        )
    }
    return file
}