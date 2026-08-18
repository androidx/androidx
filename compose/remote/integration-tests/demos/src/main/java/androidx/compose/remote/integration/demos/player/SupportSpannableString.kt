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

package androidx.compose.remote.integration.demos.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.widget.Toast
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.CustomContext

/**
 * Custom component delegate for [SpannableString] supporting [RemoteAnnotatedString] styling,
 * links, bullet lists, and paragraph formatting, rendered directly via [StaticLayout] on [Canvas]
 * without allocating an Android View.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SupportSpannableString : AndroidCustomContextImpl.CustomComponentDelegate {

    public companion object {
        public const val PROP_TEXT: Short = 1
        public const val PROP_TEXT_COLOR: Short = 2
        public const val PROP_TEXT_SIZE: Short = 3
        public const val PROP_LINK_COLOR: Short = 4

        // Links
        public const val PROP_LINK_COUNT: Short = 10
        public const val PROP_LINK_URL_BASE: Short = 1000
        public const val PROP_LINK_START_BASE: Short = 2000
        public const val PROP_LINK_END_BASE: Short = 3000

        // SpanStyles
        public const val PROP_SPAN_COUNT: Short = 20
        public const val PROP_SPAN_START_BASE: Short = 4000
        public const val PROP_SPAN_END_BASE: Short = 5000
        public const val PROP_SPAN_COLOR_BASE: Short = 6000
        public const val PROP_SPAN_BG_COLOR_BASE: Short = 7000
        public const val PROP_SPAN_FONT_SIZE_BASE: Short = 8000
        public const val PROP_SPAN_FLAGS_BASE: Short = 9000

        // Bullets
        public const val PROP_BULLET_COUNT: Short = 30
        public const val PROP_BULLET_START_BASE: Short = 10000
        public const val PROP_BULLET_END_BASE: Short = 11000

        // Paragraphs
        public const val PROP_PARAGRAPH_COUNT: Short = 40
        public const val PROP_PARAGRAPH_START_BASE: Short = 12000
        public const val PROP_PARAGRAPH_END_BASE: Short = 13000
        public const val PROP_PARAGRAPH_ALIGN_BASE: Short = 14000
    }

    private class SpannableData(val context: Context) {
        var text: String = ""

        var linkCount: Int = 0
        val urls: MutableIntObjectMap<String> = mutableIntObjectMapOf()
        val linkStarts: MutableIntIntMap = mutableIntIntMapOf()
        val linkEnds: MutableIntIntMap = mutableIntIntMapOf()

        var spanCount: Int = 0
        val spanStarts: MutableIntIntMap = mutableIntIntMapOf()
        val spanEnds: MutableIntIntMap = mutableIntIntMapOf()
        val spanColors: MutableIntIntMap = mutableIntIntMapOf()
        val spanBgColors: MutableIntIntMap = mutableIntIntMapOf()
        val spanFontSizes: MutableIntIntMap = mutableIntIntMapOf()
        val spanFlags: MutableIntIntMap = mutableIntIntMapOf()

        var bulletCount: Int = 0
        val bulletStarts: MutableIntIntMap = mutableIntIntMapOf()
        val bulletEnds: MutableIntIntMap = mutableIntIntMapOf()

        var paragraphCount: Int = 0
        val paragraphStarts: MutableIntIntMap = mutableIntIntMapOf()
        val paragraphEnds: MutableIntIntMap = mutableIntIntMapOf()
        val paragraphAligns: MutableIntIntMap = mutableIntIntMapOf()

        private val displayMetrics = context.resources.displayMetrics

        val textPaint: TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                linkColor = 0xFF1A73E8.toInt() // Standard Link Blue
                density = displayMetrics.density
                textSize =
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, displayMetrics)
            }

        var spannable: SpannableString? = null
        var staticLayout: StaticLayout? = null

        fun invalidate() {
            spannable = null
            staticLayout = null
        }

        fun buildSpannable(): SpannableString {
            val existing = spannable
            if (existing != null) return existing

            val s = SpannableString(text)
            val textLength = text.length

            // 1. SpanStyle formatting
            for (i in 0 until spanCount) {
                if (!spanStarts.contains(i) || !spanEnds.contains(i)) continue
                val start = spanStarts[i].coerceIn(0, textLength)
                val end = spanEnds[i].coerceIn(0, textLength)
                if (start >= end) continue

                if (spanColors.contains(i)) {
                    s.setSpan(
                        ForegroundColorSpan(spanColors[i]),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                if (spanBgColors.contains(i)) {
                    s.setSpan(
                        BackgroundColorSpan(spanBgColors[i]),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                val fontSize = spanFontSizes.getOrDefault(i, 0)
                if (fontSize > 0) {
                    val pxSize =
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                fontSize.toFloat(),
                                displayMetrics,
                            )
                            .toInt()
                    s.setSpan(
                        AbsoluteSizeSpan(pxSize, /* dip= */ false),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                val flags = spanFlags.getOrDefault(i, 0)
                val isBold = (flags and 1) != 0
                val isItalic = (flags and 2) != 0
                if (isBold && isItalic) {
                    s.setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                } else if (isBold) {
                    s.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                } else if (isItalic) {
                    s.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                if ((flags and 4) != 0) {
                    s.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if ((flags and 8) != 0) {
                    s.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            // 2. Link annotations
            for (i in 0 until linkCount) {
                val url = urls[i]
                if (url != null && linkStarts.contains(i) && linkEnds.contains(i)) {
                    val start = linkStarts[i].coerceIn(0, textLength)
                    val end = linkEnds[i].coerceIn(0, textLength)
                    if (start < end) {
                        s.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            // 3. Bullets
            for (i in 0 until bulletCount) {
                if (!bulletStarts.contains(i) || !bulletEnds.contains(i)) continue
                val start = bulletStarts[i].coerceIn(0, textLength)
                val end = bulletEnds[i].coerceIn(0, textLength)
                if (start < end) {
                    val bulletGap = (12 * displayMetrics.density).toInt()
                    val bulletRadius = (3 * displayMetrics.density).toInt()
                    s.setSpan(
                        BulletSpan(bulletGap, textPaint.color, bulletRadius),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            // 4. Paragraph alignment
            for (i in 0 until paragraphCount) {
                if (!paragraphStarts.contains(i) || !paragraphEnds.contains(i)) continue
                val start = paragraphStarts[i].coerceIn(0, textLength)
                val end = paragraphEnds[i].coerceIn(0, textLength)
                if (start < end) {
                    val align = paragraphAligns.getOrDefault(i, 0)
                    val alignment =
                        when (align) {
                            1 -> Layout.Alignment.ALIGN_CENTER
                            2 -> Layout.Alignment.ALIGN_OPPOSITE
                            else -> Layout.Alignment.ALIGN_NORMAL
                        }
                    s.setSpan(
                        AlignmentSpan.Standard(alignment),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            spannable = s
            return s
        }
    }

    private val components: MutableIntObjectMap<SpannableData> = mutableIntObjectMapOf()

    override fun create(id: Int, context: Context) {
        components[id] = SpannableData(context)
    }

    private fun getOrCreateData(id: Int, context: Context): SpannableData {
        var data = components[id]
        if (data == null) {
            data = SpannableData(context)
            components[id] = data
        }
        return data
    }

    override fun configure(id: Int, type: Int, value: String) {
        val data = components[id] ?: return
        if (type == PROP_TEXT.toInt()) {
            data.text = value
            data.invalidate()
        } else if (type in PROP_LINK_URL_BASE..<PROP_LINK_START_BASE) {
            val index = type - PROP_LINK_URL_BASE
            data.urls[index] = value
            data.invalidate()
        }
    }

    override fun configure(id: Int, type: Int, value: Int) {
        val data = components[id] ?: return
        when {
            type == PROP_TEXT_COLOR.toInt() -> {
                data.textPaint.color = value
            }
            type == PROP_LINK_COLOR.toInt() -> {
                data.textPaint.linkColor = value
            }
            type == PROP_TEXT_SIZE.toInt() -> {
                data.textPaint.textSize =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        value.toFloat(),
                        data.context.resources.displayMetrics,
                    )
                data.invalidate()
            }
            type == PROP_LINK_COUNT.toInt() -> {
                data.linkCount = value
                data.invalidate()
            }
            type in PROP_LINK_START_BASE..<PROP_LINK_END_BASE -> {
                val index = type - PROP_LINK_START_BASE
                data.linkStarts[index] = value
                data.invalidate()
            }
            type in PROP_LINK_END_BASE..<PROP_LINK_END_BASE + 1000 -> {
                val index = type - PROP_LINK_END_BASE
                data.linkEnds[index] = value
                data.invalidate()
            }
            type == PROP_SPAN_COUNT.toInt() -> {
                data.spanCount = value
                data.invalidate()
            }
            type in PROP_SPAN_START_BASE..<PROP_SPAN_END_BASE -> {
                data.spanStarts[type - PROP_SPAN_START_BASE] = value
                data.invalidate()
            }
            type in PROP_SPAN_END_BASE..<PROP_SPAN_COLOR_BASE -> {
                data.spanEnds[type - PROP_SPAN_END_BASE] = value
                data.invalidate()
            }
            type in PROP_SPAN_COLOR_BASE..<PROP_SPAN_BG_COLOR_BASE -> {
                data.spanColors[type - PROP_SPAN_COLOR_BASE] = value
                data.invalidate()
            }
            type in PROP_SPAN_BG_COLOR_BASE..<PROP_SPAN_FONT_SIZE_BASE -> {
                data.spanBgColors[type - PROP_SPAN_BG_COLOR_BASE] = value
                data.invalidate()
            }
            type in PROP_SPAN_FONT_SIZE_BASE..<PROP_SPAN_FLAGS_BASE -> {
                data.spanFontSizes[type - PROP_SPAN_FONT_SIZE_BASE] = value
                data.invalidate()
            }
            type in PROP_SPAN_FLAGS_BASE..<PROP_SPAN_FLAGS_BASE + 1000 -> {
                data.spanFlags[type - PROP_SPAN_FLAGS_BASE] = value
                data.invalidate()
            }
            type == PROP_BULLET_COUNT.toInt() -> {
                data.bulletCount = value
                data.invalidate()
            }
            type in PROP_BULLET_START_BASE..<PROP_BULLET_END_BASE -> {
                data.bulletStarts[type - PROP_BULLET_START_BASE] = value
                data.invalidate()
            }
            type in PROP_BULLET_END_BASE..<PROP_BULLET_END_BASE + 1000 -> {
                data.bulletEnds[type - PROP_BULLET_END_BASE] = value
                data.invalidate()
            }
            type == PROP_PARAGRAPH_COUNT.toInt() -> {
                data.paragraphCount = value
                data.invalidate()
            }
            type in PROP_PARAGRAPH_START_BASE..<PROP_PARAGRAPH_END_BASE -> {
                data.paragraphStarts[type - PROP_PARAGRAPH_START_BASE] = value
                data.invalidate()
            }
            type in PROP_PARAGRAPH_END_BASE..<PROP_PARAGRAPH_ALIGN_BASE -> {
                data.paragraphEnds[type - PROP_PARAGRAPH_END_BASE] = value
                data.invalidate()
            }
            type in PROP_PARAGRAPH_ALIGN_BASE..<PROP_PARAGRAPH_ALIGN_BASE + 1000 -> {
                data.paragraphAligns[type - PROP_PARAGRAPH_ALIGN_BASE] = value
                data.invalidate()
            }
        }
    }

    override fun configure(id: Int, type: Int, value: Float) {
        val data = components[id] ?: return
        if (type == PROP_TEXT_SIZE.toInt()) {
            data.textPaint.textSize =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    value,
                    data.context.resources.displayMetrics,
                )
            data.invalidate()
        }
    }

    override fun measure(id: Int, context: Context, bounds: FloatArray) {
        val data = getOrCreateData(id, context)
        if (data.text.isEmpty()) {
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = 0f
            bounds[3] = 0f
            return
        }

        val s = data.buildSpannable()
        val minWidth = bounds[0]
        val maxWidth = bounds[1]
        val width =
            if (maxWidth > 0 && maxWidth != Float.MAX_VALUE) {
                maxWidth.toInt()
            } else {
                Layout.getDesiredWidth(s, data.textPaint).toInt().coerceAtLeast(1)
            }

        val layout =
            StaticLayout.Builder.obtain(s, 0, s.length, data.textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()

        data.staticLayout = layout
        bounds[0] = 0f
        bounds[1] = 0f
        bounds[2] = maxOf(minWidth, layout.width.toFloat())
        bounds[3] = layout.height.toFloat()
    }

    override fun layout(id: Int, bounds: FloatArray) {
        // Layout dimensions are handled during measure and draw
    }

    override fun draw(id: Int, canvas: Canvas) {
        val data = components[id] ?: return
        data.staticLayout?.draw(canvas)
    }

    override fun onTouch(id: Int, type: Int, x: Float, y: Float): Boolean {
        if (type != CustomContext.TOUCH_UP) {
            return true
        }
        val data = components[id] ?: return false
        val layout = data.staticLayout ?: return false
        val s = data.spannable ?: return false

        val yInt = y.toInt()
        if (yInt >= 0 && yInt < layout.height) {
            val line = layout.getLineForVertical(yInt)
            if (line >= 0 && line < layout.lineCount) {
                val offset = layout.getOffsetForHorizontal(line, x)
                val urlSpans = s.getSpans(offset, offset, URLSpan::class.java)
                if (urlSpans.isNotEmpty()) {
                    val url = urlSpans[0].url
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        try {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            data.context.startActivity(intent)
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(data.context, "Clicked tag: $url", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
        }
        return false
    }
}
