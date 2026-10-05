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

package androidx.text.vertical.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.os.LocaleList as AndroidLocaleList
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests Kex-derived branches in [VerticalText], [VerticalTextLayoutCache], and `setStyleToPaint`.
 *
 * Covers [VerticalTextLayoutCache.getLayout] cache hit/miss conditions, `setStyleToPaint` style
 * branches, and [VerticalText] parameter validation, overflow modifiers, font resolution fallbacks,
 * and column-width measurement branches.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class VerticalTextKexVerifiedTest {

    // =========================================================================
    // Target 1: VerticalTextLayoutCache.getLayout
    // =========================================================================

    // Branch: cached != null && all six cache keys match -> returns the cached VerticalTextLayout.
    @Test
    fun kex_getLayout_whenAllInputsMatchCachedState_returnsSameLayoutInstance() {
        val cache = VerticalTextLayoutCache()
        val style = VerticalTextStyle(fontSize = 16.sp)
        val density = Density(1f)

        val first =
            cache.getLayout("縦書き", 200, TextOrientation.Mixed, style, Typeface.DEFAULT, density)
        val second =
            cache.getLayout("縦書き", 200, TextOrientation.Mixed, style, Typeface.DEFAULT, density)

        assertSame(first, second)
    }

    // Branch: cached != null, but each of the six keys (text, height, orientation, style, typeface,
    // density) mismatches in turn -> reuses cachedPaint and computes a new VerticalTextLayout.
    @Test
    fun kex_getLayout_whenAnyOfSixCacheKeysChanges_reusesCachedPaintAndReturnsNewLayout() {
        val cache = VerticalTextLayoutCache()
        val baseStyle = VerticalTextStyle(fontSize = 16.sp)
        val altStyle = VerticalTextStyle(fontSize = 20.sp)
        val baseDensity = Density(1f)
        val altDensity = Density(2f)

        val l0 =
            cache.getLayout(
                "A",
                100,
                TextOrientation.Mixed,
                baseStyle,
                Typeface.DEFAULT,
                baseDensity,
            )
        val p0 = l0.captureDrawPaint()
        val l1 =
            cache.getLayout(
                "B",
                100,
                TextOrientation.Mixed,
                baseStyle,
                Typeface.DEFAULT,
                baseDensity,
            )
        val p1 = l1.captureDrawPaint()
        val l2 =
            cache.getLayout(
                "B",
                200,
                TextOrientation.Mixed,
                baseStyle,
                Typeface.DEFAULT,
                baseDensity,
            )
        val p2 = l2.captureDrawPaint()
        val l3 =
            cache.getLayout(
                "B",
                200,
                TextOrientation.Upright,
                baseStyle,
                Typeface.DEFAULT,
                baseDensity,
            )
        val p3 = l3.captureDrawPaint()
        val l4 =
            cache.getLayout(
                "B",
                200,
                TextOrientation.Upright,
                altStyle,
                Typeface.DEFAULT,
                baseDensity,
            )
        val p4 = l4.captureDrawPaint()
        val l5 =
            cache.getLayout(
                "B",
                200,
                TextOrientation.Upright,
                altStyle,
                Typeface.SERIF,
                baseDensity,
            )
        val p5 = l5.captureDrawPaint()
        val l6 =
            cache.getLayout("B", 200, TextOrientation.Upright, altStyle, Typeface.SERIF, altDensity)
        val p6 = l6.captureDrawPaint()

        assertNotSame(l0, l1)
        assertNotSame(l1, l2)
        assertNotSame(l2, l3)
        assertNotSame(l3, l4)
        assertNotSame(l4, l5)
        assertNotSame(l5, l6)
        assertSame(p0, p1)
        assertSame(p0, p2)
        assertSame(p0, p3)
        assertSame(p0, p4)
        assertSame(p0, p5)
        assertSame(p0, p6)
    }

    // =========================================================================
    // Target 2: setStyleToPaint (exercised via VerticalTextLayoutCache.getLayout)
    // =========================================================================

    // Branch: style.fontSize.isSpecified == false (TextUnit.Unspecified -> DefaultFontSize = 16.sp)
    // vs style.fontSize.isSpecified == true (16.sp and 32.sp).
    @Test
    fun kex_setStyleToPaint_fontSizeSpecifiedVsUnspecified_scalesLayoutWidth() {
        val cache = VerticalTextLayoutCache()
        val density = Density(2f)

        val defaultSizeLayout =
            cache.getLayout(
                "あ",
                300,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = TextUnit.Unspecified),
                Typeface.DEFAULT,
                density,
            )
        val explicit16SpLayout =
            cache.getLayout(
                "あ",
                300,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 16.sp),
                Typeface.DEFAULT,
                density,
            )
        val largeSizeLayout =
            cache.getLayout(
                "あ",
                300,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 32.sp),
                Typeface.DEFAULT,
                density,
            )

        assertThat(defaultSizeLayout.width).isEqualTo(explicit16SpLayout.width)
        assertThat(largeSizeLayout.width).isGreaterThan(defaultSizeLayout.width)
    }

    // Branch: style.color.isSpecified == true (sets out.color) vs false (Color.Unspecified leaves
    // Paint.reset() default black).
    @Test
    fun kex_setStyleToPaint_colorSpecifiedVsUnspecified_rendersExpectedGlyphColor() {
        val cache = VerticalTextLayoutCache()
        val density = Density(1f)
        val height = 200

        val redLayout =
            cache.getLayout(
                "黒",
                height,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 28.sp, color = Color.Red),
                Typeface.DEFAULT,
                density,
            )
        // Assert redLayout before the next getLayout call because VerticalTextLayoutCache reuses a
        // single cachedPaint instance and mutates it in place on each cache miss.
        assertThat(redLayout.countColorPixels(Color.Red.toArgb(), height)).isGreaterThan(0)

        val unspecifiedColorLayout =
            cache.getLayout(
                "黒",
                height,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 28.sp, color = Color.Unspecified),
                Typeface.DEFAULT,
                density,
            )
        assertThat(unspecifiedColorLayout.countColorPixels(AndroidColor.BLACK, height))
            .isGreaterThan(0)
        assertThat(unspecifiedColorLayout.countColorPixels(Color.Red.toArgb(), height)).isEqualTo(0)
    }

    // Branch: style.background.isSpecified == true (sets out.bgColor) vs false (sets
    // AndroidColor.TRANSPARENT).
    @Test
    fun kex_setStyleToPaint_backgroundSpecifiedVsUnspecified_setsOrClearsBackgroundColor() {
        val cache = VerticalTextLayoutCache()
        val density = Density(1f)
        val height = 200

        val yellowBgLayout =
            cache.getLayout(
                "背景",
                height,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 24.sp, background = Color.Yellow),
                Typeface.DEFAULT,
                density,
            )
        // Assert yellowBgLayout before the next getLayout call because VerticalTextLayoutCache
        // reuses a single cachedPaint instance and mutates it in place on each cache miss.
        assertThat(yellowBgLayout.countColorPixels(Color.Yellow.toArgb(), height)).isGreaterThan(0)

        val transparentBgLayout =
            cache.getLayout(
                "背景",
                height,
                TextOrientation.Mixed,
                VerticalTextStyle(fontSize = 24.sp, background = Color.Unspecified),
                Typeface.DEFAULT,
                density,
            )
        assertThat(transparentBgLayout.countColorPixels(Color.Yellow.toArgb(), height)).isEqualTo(0)
    }

    // Branch: style.fontFeatureSettings non-null vs null (ungated across all API levels).
    @Test
    fun kex_setStyleToPaint_fontFeatureSettingsSpecifiedVsNull_appliesToPaint() {
        val cache = VerticalTextLayoutCache()
        val density = Density(1f)
        var capturedFeatureSettings: String? = "uninitialized"
        val probeText =
            SpannableString("日本語").apply {
                setSpan(
                    object : CharacterStyle() {
                        override fun updateDrawState(tp: TextPaint) {
                            capturedFeatureSettings = tp.fontFeatureSettings
                        }
                    },
                    0,
                    length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }

        cache.getLayout(
            probeText,
            200,
            TextOrientation.Mixed,
            VerticalTextStyle(fontSize = 20.sp, fontFeatureSettings = "vpal"),
            Typeface.DEFAULT,
            density,
        )
        assertThat(capturedFeatureSettings).isEqualTo("vpal")

        cache.getLayout(
            probeText,
            200,
            TextOrientation.Mixed,
            VerticalTextStyle(fontSize = 20.sp, fontFeatureSettings = null),
            Typeface.DEFAULT,
            density,
        )
        assertThat(capturedFeatureSettings).isNull()
    }

    // Branch: style.localeList non-null vs null on API 25+.
    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun kex_setStyleToPaint_localeListSpecifiedVsNull_appliesTextLocalesOnApi25Plus() {
        val cache = VerticalTextLayoutCache()
        val density = Density(1f)
        var capturedLocales: AndroidLocaleList? = null
        val probeText =
            SpannableString("日本語").apply {
                setSpan(
                    object : CharacterStyle() {
                        override fun updateDrawState(tp: TextPaint) {
                            capturedLocales = tp.textLocales
                        }
                    },
                    0,
                    length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }

        cache.getLayout(
            probeText,
            200,
            TextOrientation.Mixed,
            VerticalTextStyle(fontSize = 20.sp, localeList = LocaleList("ja-JP,zh-CN")),
            Typeface.DEFAULT,
            density,
        )
        assertThat(capturedLocales).isEqualTo(AndroidLocaleList.forLanguageTags("ja-JP,zh-CN"))

        cache.getLayout(
            probeText,
            200,
            TextOrientation.Mixed,
            VerticalTextStyle(fontSize = 20.sp, localeList = null),
            Typeface.DEFAULT,
            density,
        )
        assertThat(capturedLocales).isEqualTo(TextPaint().textLocales)
    }

    // =========================================================================
    // Target 3: VerticalText and VerticalTextImpl
    // =========================================================================

    // Branch: minColumns > maxColumns -> check(minColumns <= maxColumns) throws
    // IllegalStateException.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenMinColumnsExceedsMaxColumns_throwsIllegalStateException() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                runComposeUiTest {
                    setContent { VerticalText(text = "Hello", maxColumns = 1, minColumns = 2) }
                }
            }

        assertThat(exception).hasMessageThat().contains("maxColumn must be bigger than minColumns!")
    }

    // Branch: overflow == TextOverflow.Visible (takes the else Modifier branch instead of
    // Modifier.clipToBounds()) and non-null fontWeight, fontStyle, and fontSynthesis in style.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenOverflowIsVisibleAndFontPropertiesSpecified_composesAndDisplays() =
        runComposeUiTest {
            var measuredSize = IntSize.Zero
            var capturedTypeface: Typeface? = null
            val probeText =
                SpannableString("VisibleVertical").apply {
                    setSpan(
                        object : CharacterStyle() {
                            override fun updateDrawState(tp: TextPaint) {
                                capturedTypeface = tp.typeface
                            }
                        },
                        0,
                        length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                    )
                }
            val style =
                VerticalTextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.None,
                    fontFamily = FontFamily.Serif,
                )

            setContent {
                VerticalText(
                    text = probeText,
                    modifier = Modifier.fixedHeight(300).onSizeChanged { measuredSize = it },
                    style = style,
                    overflow = TextOverflow.Visible,
                    maxColumns = 3,
                    minColumns = 1,
                    orientation = TextOrientation.Upright,
                )
            }

            onNodeWithText("VisibleVertical").assertExists().assertIsDisplayed()
            assertThat(measuredSize.width).isGreaterThan(0)
            assertThat(capturedTypeface?.isBold).isTrue()
            assertThat(capturedTypeface?.isItalic).isTrue()
        }

    // Branch: maxColumns == 0 && minColumns == 0 -> effectiveColumns == 0, so desiredWidth == 0f,
    // minWidth == 0f, and layoutWidth == 0.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenMinAndMaxColumnsAreZero_measuresZeroLayoutWidth() = runComposeUiTest {
        var measuredSize = IntSize(-1, -1)

        setContent {
            VerticalText(
                text = "あいうえお",
                modifier = Modifier.fixedHeight(300).onSizeChanged { measuredSize = it },
                style = VerticalTextStyle(fontSize = 20.sp),
                maxColumns = 0,
                minColumns = 0,
            )
        }

        waitForIdle()
        assertThat(measuredSize.width).isEqualTo(0)
    }

    // Branch: minColumns > vtl.lineCount -> minWidth = columnWidth * minColumns exceeds
    // desiredWidth, so maxOf(desiredWidth, minWidth) expands layoutWidth to minColumns columns.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenMinColumnsExceedsLineCount_expandsWidthToMinColumns() =
        runComposeUiTest {
            var oneColumnSize = IntSize.Zero
            var fourColumnMinSize = IntSize.Zero

            setContent {
                VerticalText(
                    text = "あ",
                    modifier = Modifier.fixedHeight(300).onSizeChanged { oneColumnSize = it },
                    style = VerticalTextStyle(fontSize = 20.sp),
                    minColumns = 1,
                )
                VerticalText(
                    text = "あ",
                    modifier = Modifier.fixedHeight(300).onSizeChanged { fourColumnMinSize = it },
                    style = VerticalTextStyle(fontSize = 20.sp),
                    minColumns = 4,
                )
            }

            waitForIdle()
            assertThat(oneColumnSize.width).isGreaterThan(0)
            assertThat(fourColumnMinSize.width).isGreaterThan(oneColumnSize.width * 3)
        }

    // Branch: maxColumns < vtl.lineCount (with minColumns <= maxColumns) -> effectiveColumns =
    // min(vtl.lineCount, maxColumns) caps desiredWidth at maxColumns columns.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenMaxColumnsIsLessThanLineCount_capsWidthAtMaxColumns() =
        runComposeUiTest {
            val longText = "あいうえおかきくけこさしすせそたちつてと"
            var uncappedSize = IntSize.Zero
            var cappedSize = IntSize.Zero

            setContent {
                VerticalText(
                    text = longText,
                    modifier = Modifier.fixedHeight(80).onSizeChanged { uncappedSize = it },
                    style = VerticalTextStyle(fontSize = 24.sp),
                    maxColumns = Int.MAX_VALUE,
                    minColumns = 1,
                )
                VerticalText(
                    text = longText,
                    modifier = Modifier.fixedHeight(80).onSizeChanged { cappedSize = it },
                    style = VerticalTextStyle(fontSize = 24.sp),
                    maxColumns = 1,
                    minColumns = 1,
                )
            }

            waitForIdle()
            assertThat(cappedSize.width).isGreaterThan(0)
            assertThat(uncappedSize.width).isGreaterThan(cappedSize.width)
        }

    // Branch: coerceIn(constraints.minWidth, constraints.maxWidth) clamps layoutWidth to both
    // minWidth (when desired width < minWidth) and maxWidth (when desired width > maxWidth).
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_verticalText_whenFixedWidthConstraintProvided_coercesWidthToConstraints() =
        runComposeUiTest {
            var minClampedSize = IntSize.Zero
            var maxClampedSize = IntSize.Zero

            setContent {
                VerticalText(
                    text = "あ",
                    modifier =
                        Modifier.layout { measurable, _ ->
                                val placeable = measurable.measure(Constraints.fixed(300, 200))
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                            .onSizeChanged { minClampedSize = it },
                    style = VerticalTextStyle(fontSize = 16.sp),
                )
                VerticalText(
                    text = "あいうえおかきくけこ",
                    modifier =
                        Modifier.layout { measurable, _ ->
                                val placeable =
                                    measurable.measure(
                                        Constraints(
                                            minWidth = 0,
                                            maxWidth = 10,
                                            minHeight = 80,
                                            maxHeight = 80,
                                        )
                                    )
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                            .onSizeChanged { maxClampedSize = it },
                    style = VerticalTextStyle(fontSize = 24.sp),
                )
            }

            waitForIdle()
            assertThat(minClampedSize.width).isEqualTo(300)
            assertThat(maxClampedSize.width).isEqualTo(10)
        }
}

private fun Modifier.fixedHeight(heightPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = heightPx, maxHeight = heightPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun VerticalTextLayout.captureDrawPaint(): Paint {
    var capturedPaint: Paint? = null
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    try {
        val recordingCanvas =
            object : Canvas(bitmap) {
                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    capturedPaint = paint
                }
            }
        draw(recordingCanvas, 1f, 0f)
        return checkNotNull(capturedPaint) { "Expected VerticalTextLayout.draw to invoke drawText" }
    } finally {
        bitmap.recycle()
    }
}

private fun VerticalTextLayout.countColorPixels(targetColor: Int, height: Int): Int {
    val bitmapWidth = ceil(width).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    try {
        draw(Canvas(bitmap), bitmapWidth.toFloat(), 0f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it == targetColor }
    } finally {
        bitmap.recycle()
    }
}
