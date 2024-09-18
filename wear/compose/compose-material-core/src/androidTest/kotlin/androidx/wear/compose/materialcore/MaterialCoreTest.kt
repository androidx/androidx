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

package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import kotlin.math.round

internal val LocalContentTestData: ProvidableCompositionLocal<Int> = compositionLocalOf { 1 }

internal val DEFAULT_SHAPE_COLOR = Color.Magenta
internal const val EXPECTED_LOCAL_TEST_DATA = 42

internal const val TEST_TAG = "test-item"

val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

internal enum class Status {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.isShape(
    shape: Shape,
    layoutDirection: LayoutDirection,
    padding: Dp = 0.dp,
    backgroundColor: Color = Color.Red,
    shapeColor: Color = DEFAULT_SHAPE_COLOR,
    content: @Composable () -> Unit
) {
    setContent {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(Modifier.padding(padding).background(backgroundColor)) { content() }
        }
    }

    this.waitForIdle()
    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertShape(
            density = density,
            shape = shape,
            horizontalPadding = padding,
            verticalPadding = padding,
            backgroundColor = backgroundColor,
            antiAliasingGap = 2.0f,
            shapeColor = shapeColor
        )
}

internal fun ComposeContentTestRule.verifyHeight(expected: Dp, content: @Composable () -> Unit) {
    setContentForSizeAssertions { content() }.assertHeightIsEqualTo(expected)
}

/**
 * assertContainsColor - uses a threshold on an ImageBitmap's color distribution to check that a UI
 * element is predominantly the expected color.
 */
internal fun ImageBitmap.assertContainsColor(expectedColor: Color, minPercent: Float = 50.0f) {
    val histogram = histogram()

    val actualPercent = round(((histogram[expectedColor] ?: 0) * 100f) / (width * height))
    if (actualPercent < minPercent) {
        throw AssertionError(
            "Expected color $expectedColor found $actualPercent%, below threshold $minPercent%"
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

@Composable
internal fun TestImage(modifier: Modifier = Modifier, iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Image(
        testImage,
        iconLabel,
        modifier = modifier.fillMaxSize().testTag(iconLabel),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
internal fun TestText(text: String, modifier: Modifier = Modifier) {
    TextWithDefaults(text = AnnotatedString(text), modifier = modifier, style = TextStyle.Default)
}

internal fun ComposeContentTestRule.setContentForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    useUnmergedTree: Boolean = false,
    content: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        Box {
            Box(
                Modifier.sizeIn(maxWidth = parentMaxWidth, maxHeight = parentMaxHeight)
                    .testTag("containerForSizeAssertion")
            ) {
                content()
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion", useUnmergedTree = useUnmergedTree)
}
