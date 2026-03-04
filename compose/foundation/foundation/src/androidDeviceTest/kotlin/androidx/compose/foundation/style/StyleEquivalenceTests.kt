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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class StyleEquivalenceTests {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun background() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(onClick = {}, style = { background(Color.Blue) }) {
                    Box(modifier = Modifier.size(10.dp))
                }
            },
            modifierVersion = {
                BaseModifierButton(onClick = {}, background = SolidColor(Color.Blue)) {
                    Box(modifier = Modifier.size(10.dp))
                }
            },
        )
    }

    @Test
    fun border() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(onClick = {}, style = { border(2.dp, Color.Red) }) {
                    Box(modifier = Modifier.size(10.dp))
                }
            },
            modifierVersion = {
                BaseModifierButton(onClick = {}, border = BorderStroke(2.dp, Color.Red)) {
                    Box(modifier = Modifier.size(10.dp))
                }
            },
        )
    }

    @Test
    fun contentPadding() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(onClick = {}, style = { contentPadding(5.dp) }) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
            modifierVersion = {
                BaseModifierButton(onClick = {}, contentPadding = PaddingValues(5.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
        )
    }

    @Test
    fun border_fillArea() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        border(20.dp, Color.Red)
                        shape(RoundedCornerShape(5.dp))
                    },
                ) {}
            },
            modifierVersion = {
                BaseModifierButton(
                    onClick = {},
                    border = BorderStroke(20.dp, Color.Red),
                    shape = RoundedCornerShape(5.dp),
                ) {}
            },
        )
    }

    @Test
    fun border_shape_background() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        border(2.dp, Color.Red)
                        shape(RoundedCornerShape(2.dp))
                        background(Color.Blue)
                    },
                ) {
                    Box(modifier = Modifier.size(20.dp))
                }
            },
            modifierVersion = {
                BaseModifierButton(
                    onClick = {},
                    border = BorderStroke(2.dp, Color.Red),
                    shape = RoundedCornerShape(2.dp),
                    background = SolidColor(Color.Blue),
                ) {
                    Box(modifier = Modifier.size(20.dp))
                }
            },
        )
    }

    @Test
    fun border_shape_notSimple() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        border(2.dp, Color.Red)
                        shape(RoundedCornerShape(topStart = 5.dp, bottomEnd = 10.dp))
                    },
                ) {
                    Box(modifier = Modifier.size(20.dp))
                }
            },
            modifierVersion = {
                BaseModifierButton(
                    onClick = {},
                    border = BorderStroke(2.dp, Color.Red),
                    shape = RoundedCornerShape(topStart = 5.dp, bottomEnd = 10.dp),
                ) {
                    Box(modifier = Modifier.size(20.dp))
                }
            },
        )
    }

    @Test
    fun externalPadding() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(onClick = {}, style = { externalPadding(10.dp) }) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
            modifierVersion = {
                BaseModifierButton(onClick = {}, externalPadding = PaddingValues(10.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
        )
    }

    @Test
    fun shape() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        background(Color.Blue)
                        shape(RoundedCornerShape(5.dp))
                        contentPadding(10.dp)
                    },
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red))
                }
            },
            modifierVersion = {
                BaseModifierButton(
                    onClick = {},
                    background = SolidColor(Color.Blue),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red))
                }
            },
        )
    }

    @Test
    fun alpha() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(onClick = {}, style = { alpha(0.5f) }) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
            modifierVersion = {
                BaseModifierButton(onClick = {}, layerSpec = { alpha = 0.5f }) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
        )
    }

    @Test
    fun size() {
        checkEquivalence(
            styleVersion = {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        size(50.dp)
                        background(Color.Green)
                    },
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
            modifierVersion = {
                BaseModifierButton(
                    onClick = {},
                    size = DpSize(50.dp, 50.dp),
                    background = SolidColor(Color.Green),
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                }
            },
        )
    }

    /** Validate the style and the modifier version produce the same drawing. */
    @SdkSuppress(minSdkVersion = 26)
    private fun checkEquivalence(
        styleVersion: @Composable () -> Unit,
        modifierVersion: @Composable () -> Unit,
        debug: Boolean = false,
    ) {
        if (debug) {
            // When debugging it will show renderings in a column and wait for
            // the button to be clicked.
            var done = false
            rule.setContent {
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    BasicText("Style version")
                    Box(modifier = Modifier.border(1.dp, Color.Black).padding(20.dp)) {
                        styleVersion()
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BasicText("No style version")
                    Box(modifier = Modifier.border(1.dp, Color.Black).padding(20.dp)) {
                        modifierVersion()
                    }
                    if (!done) {
                        Box(
                            modifier =
                                Modifier.border(
                                        10.dp,
                                        color = Color.LightGray,
                                        RoundedCornerShape(15.dp),
                                    )
                                    .background(Color.Cyan, RoundedCornerShape(15.dp))
                                    .padding(20.dp)
                                    .clickable { done = true }
                        ) {
                            BasicText("Done")
                        }
                    }
                }
            }
            rule.waitUntil(1000 * 60 * 2) { done }
        } else {
            var withStyle by mutableStateOf(true)
            rule.setContent {
                if (withStyle) {
                    styleVersion()
                } else {
                    modifierVersion()
                }
            }
            val styleBitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            withStyle = false
            rule.waitForIdle()
            val modifierBitmap = rule.onRoot().captureToImage().asAndroidBitmap()

            assertEquals(modifierBitmap.width, styleBitmap.width, "Width mismatch")
            assertEquals(modifierBitmap.height, styleBitmap.height, "Height mismatch")
            if (
                modifierBitmap.width == styleBitmap.width &&
                    modifierBitmap.height == styleBitmap.height
            ) {
                val matcher = MSSIMMatcher(threshold = 0.995)
                val result =
                    matcher.compareBitmaps(
                        styleBitmap.toIntArray(),
                        modifierBitmap.toIntArray(),
                        modifierBitmap.width,
                        modifierBitmap.height,
                    )
                if (!result.matches) {
                    val message = buildString {
                        appendLine("Style and modifier versions are different")
                        appendLine()
                        appendLine("Styles")
                        append(styleBitmap.renderedToString())
                        appendLine()
                        appendLine("Modifiers")
                        append(modifierBitmap.renderedToString())
                        appendLine()
                        appendLine("Difference")
                        append(styleBitmap.differenceToString(modifierBitmap))
                    }
                    error(message)
                }
            }
        }
    }
}

private fun Bitmap.toIntArray(): IntArray {
    val bitmapArray = IntArray(width * height)
    getPixels(bitmapArray, 0, width, 0, 0, width, height)
    return bitmapArray
}

private fun Bitmap.renderedToString(): String {
    val bitmapArray = toIntArray()
    // '_' is used instead of ' ' as exception messages are often trimmed (e.g. by logcat for
    // example) and using '_' prevents the string from being trimmed.
    val chars = arrayOf('_', '░', '▒', '▓', '█')
    return buildString {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmapArray[y * width + x]
                val blue = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val red = pixel and 0xFF
                val scale = 256f
                val brightness =
                    (blue.toFloat() / scale + green.toFloat() / scale + red.toFloat() / scale) / 3
                val index = (brightness * chars.size).toInt()
                append(chars[index])
            }
            append('\n')
        }
    }
}

private fun Bitmap.differenceToString(other: Bitmap): String {
    val width = width
    val height = height
    if (width != other.width || height != other.height) {
        return "Different dimensions: ${width}x${height} - ${other.width}x${other.height}"
    }
    return buildString {
        val thesePixels = toIntArray()
        val otherPixels = other.toIntArray()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val thisPixel = thesePixels[y * width + x]
                val otherPixel = otherPixels[y * width + x]
                // '_' is used instead of ' ' as exception messages are often trimmed (e.g. by
                // logcat for example) and using '_' prevents the string from being trimmed.
                append(if (thisPixel != otherPixel) 'X' else '_')
            }
            append('\n')
        }
    }
}

/** A typical button shaped composable using style for customization. */
@ExperimentalFoundationStyleApi
@Composable
internal fun BaseStyleableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = enabled }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier =
            modifier
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                )
                .styleable(styleState, style),
        content = content,
    )
}

/** A typical button shaped composable using parameters and modifiers for customization. */
@Composable
internal fun BaseModifierButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    externalPadding: PaddingValues? = null,
    border: BorderStroke? = null,
    background: Brush? = null,
    backgroundAlpha: Float = 1f,
    contentPadding: PaddingValues? = null,
    size: DpSize = DpSize.Unspecified,
    minSize: DpSize = DpSize.Unspecified,
    maxSize: DpSize = DpSize.Unspecified,
    layerSpec: (GraphicsLayerScope.() -> Unit)? = null,
    fill: Fill? = null,
    clip: Boolean = false,
    shape: Shape = RectangleShape,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier =
            modifier
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    interactionSource = interactionSource,
                )
                .ifTrue(clip) { clip(shape) }
                .ifNonNull(layerSpec) { graphicsLayer(it) }
                .ifNonNull(externalPadding) { padding(it) }
                .ifSpecified(size) { size(it) }
                .ifNonNull(fill) {
                    when {
                        it.width.isNaN() && it.height.isNaN() -> this
                        it.width.isNaN() -> fillMaxWidth(it.height)
                        else -> fillMaxWidth(it.width)
                    }
                }
                .ifEitherSpecified(minSize, maxSize) { min, max ->
                    sizeIn(
                        minWidth = min.width,
                        minHeight = min.height,
                        maxWidth = max.width,
                        maxHeight = max.height,
                    )
                }
                .ifNonNull(border) {
                    // Compute the padding necessary to match the border. Padding uses
                    // value.roundToPx(). Border uses ceil(value.toPx()). The styleable modifier
                    // uses width computed for the border width to compute the padding. This
                    // uses the border computation to compute the Dp value that will produce the
                    // same number of pixels used by the border.
                    val padding =
                        with(density) {
                            (if (it.width == Dp.Hairline) 1f else ceil(it.width.toPx())).toDp()
                        }
                    border(it, shape)
                        .ifNonNull(background) { background(it, shape, backgroundAlpha) }
                        .padding(padding)
                }
                .ifTrue(background != null && border == null) {
                    background(background!!, shape, backgroundAlpha)
                }
                .ifNonNull(contentPadding) { padding(it) },
        content = content,
    )
}

inline fun <T : Any> Modifier.ifNonNull(
    value: T?,
    block: Modifier.(value: T) -> Modifier,
): Modifier = if (value != null) block(value) else this

inline fun Modifier.ifSpecified(
    value: DpSize,
    block: Modifier.(value: DpSize) -> Modifier,
): Modifier = if (value.isSpecified) block(value) else this

inline fun Modifier.ifEitherSpecified(
    a: DpSize,
    b: DpSize,
    block: Modifier.(a: DpSize, b: DpSize) -> Modifier,
): Modifier = if (a.isSpecified || b.isSpecified) block(a, b) else this

inline fun Modifier.ifTrue(value: Boolean, block: Modifier.() -> Modifier): Modifier =
    if (value) block() else this

internal class Fill(val width: Float, val height: Float) {
    companion object {
        fun width(width: Float) = Fill(width, Float.NaN)

        fun height(height: Float) = Fill(Float.NaN, height)
    }
}
