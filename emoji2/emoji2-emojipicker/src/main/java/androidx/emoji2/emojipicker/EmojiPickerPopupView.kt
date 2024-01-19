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

package androidx.emoji2.emojipicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat

/** Popup view for emoji picker to show emoji variants. */
internal class EmojiPickerPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0,
    private val targetEmojiView: View,
    private val variants: List<String>,
    private val emojiViewOnClickListener: OnClickListener
) :
    FrameLayout(context, attrs, defStyleAttr) {
    private val popupView: LinearLayout
    private val layoutTemplate: LayoutTemplate

    init {
        popupView = inflate(context, R.layout.variant_popup, /* root= */ null)
            .findViewById<LinearLayout>(R.id.variant_popup)

        layoutTemplate = getLayoutTemplate(variants)
        for (row in layoutTemplate.template) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                )
            }
            for (item in row) {
                val cell = when (item) {
                    in 1..variants.size ->
                        EmojiView(context).apply {
                            willDrawVariantIndicator = false
                            emoji = variants[item - 1]
                            setOnClickListener(emojiViewOnClickListener)
                            if (item == 1) {
                                // Hover on the first emoji in the popup
                                popupView.post {
                                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                                }
                            }
                        }

                    0 -> EmojiView(context)

                    else -> SkinToneCircleView(context).apply {
                        paint = Paint().apply {
                            color = ContextCompat.getColor(
                                context, SKIN_TONE_COLOR_RES_IDS[item + 5])
                            style = Paint.Style.FILL
                        }
                    }
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        targetEmojiView.width, targetEmojiView.height)
                }
                rowLayout.addView(cell)
            }
            popupView.addView(rowLayout)
        }
        addView(popupView)
    }

    fun getPopupViewWidth(): Int {
        return layoutTemplate.numberOfColumns * targetEmojiView.width +
            popupView.paddingStart + popupView.paddingEnd
    }

    fun getPopupViewHeight(): Int {
        return layoutTemplate.numberOfRows * targetEmojiView.height +
            popupView.paddingTop + popupView.paddingBottom
    }

    private fun getLayoutTemplate(variants: List<String>): LayoutTemplate {
        val layout =
            if (variants.size == SQUARE_LAYOUT_VARIANT_COUNT)
                if (SQUARE_LAYOUT_EMOJI_NO_SKIN_TONE.contains(variants[0]))
                    Layout.SQUARE
                else Layout.SQUARE_WITH_SKIN_TONE_CIRCLE
            else Layout.FLAT
        var template = when (layout) {
            Layout.SQUARE -> SQUARE_LAYOUT_TEMPLATE
            Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> SQUARE_LAYOUT_WITH_SKIN_TONES_TEMPLATE
            Layout.FLAT -> arrayOf(variants.indices.map { it + 1 }.toIntArray())
        }
        val column = when (layout) {
            Layout.SQUARE, Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> template[0].size
            Layout.FLAT -> minOf(6, template[0].size)
        }
        val row = when (layout) {
            Layout.SQUARE, Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> template.size
            Layout.FLAT -> variants.size / column + if (variants.size % column == 0) 0 else 1
        }

        // Rewrite template when the number of row mismatch
        if (row != template.size) {
            val overrideTemplate = Array(row) { IntArray(column) }
            var index = 0
            for (i in 0 until row) {
                for (j in 0 until column) {
                    if (index < template[0].size) {
                        overrideTemplate[i][j] = template[0][index]
                        index++
                    }
                }
            }
            template = overrideTemplate
        }
        return LayoutTemplate(template, row, column)
    }

    private data class LayoutTemplate(
        val template: Array<IntArray>,
        val numberOfRows: Int,
        val numberOfColumns: Int
    )

    companion object {
        private enum class Layout { FLAT, SQUARE, SQUARE_WITH_SKIN_TONE_CIRCLE }

        /**
         * The number of variants expected when using a square layout strategy. Square layouts are
         * comprised of a 5x5 grid + the base variant.
         */
        private const val SQUARE_LAYOUT_VARIANT_COUNT = 26

        // Set of emojis that use the square layout without skin tone swatches.
        private val SQUARE_LAYOUT_EMOJI_NO_SKIN_TONE = setOf("👪")

        private val SKIN_TONE_COLOR_RES_IDS = listOf(
            R.color.light_skin_tone,
            R.color.medium_light_skin_tone,
            R.color.medium_skin_tone,
            R.color.medium_dark_skin_tone,
            R.color.dark_skin_tone
        )

        /**
         * Square variant layout template with skin tone.
         * 0 : a place holder
         * -5: light skin tone circle
         * -4: medium-light skin tone circle
         * -3: medium skin tone circle
         * -2: medium-dark skin tone circle
         * -1: dark skin tone circle
         * Positive number is the index + 1 in the variant array
         */
        private val SQUARE_LAYOUT_WITH_SKIN_TONES_TEMPLATE = arrayOf(
            intArrayOf(0, 0, -5, -4, -3, -2, -1),
            intArrayOf(0, -5, 2, 3, 4, 5, 6),
            intArrayOf(0, -4, 7, 8, 9, 10, 11),
            intArrayOf(0, -3, 12, 13, 14, 15, 16),
            intArrayOf(0, -2, 17, 18, 19, 20, 21),
            intArrayOf(1, -1, 22, 23, 24, 25, 26)
        )

        /**
         * Square variant layout template without skin tone.
         * 0 : a place holder
         * Positive number is the index + 1 in the variant array
         */
        private val SQUARE_LAYOUT_TEMPLATE = arrayOf(
            intArrayOf(0, 2, 3, 4, 5, 6),
            intArrayOf(0, 7, 8, 9, 10, 11),
            intArrayOf(0, 12, 13, 14, 15, 16),
            intArrayOf(0, 17, 18, 19, 20, 21),
            intArrayOf(1, 22, 23, 24, 25, 26)
        )
    }
}

internal class SkinToneCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val radius = resources.getDimension(R.dimen.emoji_picker_skin_tone_circle_radius)
    var paint: Paint? = null

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.apply {
            paint?.let { drawCircle(width / 2f, height / 2f, radius, it) }
        }
    }
}
