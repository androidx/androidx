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
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.URLSpan
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.CustomContext

/**
 * Custom component delegate for [SpannableString] with [androidx.compose.ui.text.LinkAnnotation]
 * support, rendered directly via [StaticLayout] on [Canvas] without allocating an Android View.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SupportSpannableString : AndroidCustomContextImpl.CustomComponentDelegate {

    public companion object {
        public const val PROP_TEXT: Short = 1
        public const val PROP_TEXT_COLOR: Short = 2
        public const val PROP_TEXT_SIZE: Short = 3
        public const val PROP_LINK_COLOR: Short = 4
        public const val PROP_LINK_COUNT: Short = 10
        public const val PROP_LINK_URL_BASE: Short = 1000
        public const val PROP_LINK_START_BASE: Short = 2000
        public const val PROP_LINK_END_BASE: Short = 3000
    }

    private class SpannableData(val context: Context) {
        var text: String = ""
        var linkCount: Int = 0
        val urls: MutableIntObjectMap<String> = mutableIntObjectMapOf()
        val starts: MutableIntIntMap = mutableIntIntMapOf()
        val ends: MutableIntIntMap = mutableIntIntMapOf()

        val textPaint: TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                linkColor = 0xFF1A73E8.toInt() // Standard Link Blue
                textSize = 48f
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
            for (i in 0 until linkCount) {
                val url = urls[i]
                if (url != null && starts.contains(i) && ends.contains(i)) {
                    val start = starts[i].coerceIn(0, textLength)
                    val end = ends[i].coerceIn(0, textLength)
                    if (start < end) {
                        s.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
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
        } else if (type >= PROP_LINK_URL_BASE && type < PROP_LINK_START_BASE) {
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
                data.textPaint.textSize = value.toFloat()
                data.invalidate()
            }
            type == PROP_LINK_COUNT.toInt() -> {
                data.linkCount = value
                data.invalidate()
            }
            type >= PROP_LINK_START_BASE && type < PROP_LINK_END_BASE -> {
                val index = type - PROP_LINK_START_BASE
                data.starts[index] = value
                data.invalidate()
            }
            type >= PROP_LINK_END_BASE && type < PROP_LINK_END_BASE + 1000 -> {
                val index = type - PROP_LINK_END_BASE
                data.ends[index] = value
                data.invalidate()
            }
        }
    }

    override fun configure(id: Int, type: Int, value: Float) {
        val data = components[id] ?: return
        if (type == PROP_TEXT_SIZE.toInt()) {
            data.textPaint.textSize = value
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
                    }
                    return true
                }
            }
        }
        return false
    }
}
