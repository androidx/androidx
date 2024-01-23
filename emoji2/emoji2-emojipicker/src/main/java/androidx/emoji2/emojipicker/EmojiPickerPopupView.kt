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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
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
    private var layoutTemplate: LayoutTemplate
    private var emojiFacingLeft = true

    init {
        popupView = inflate(context, R.layout.variant_popup, /* root= */ null)
            .findViewById<LinearLayout>(R.id.variant_popup)
        layoutTemplate = getLayoutTemplate(variants)
        if (layoutTemplate.layout == Layout.BIDIRECTIONAL) {
            addBidirectionalLayoutHeader(popupView)
        }
        addRowsToPopupView()
        addView(popupView)
    }

    private fun addRowsToPopupView() {
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
    }

    fun getPopupViewWidth(): Int {
        return layoutTemplate.numberOfColumns * targetEmojiView.width +
            popupView.paddingStart + popupView.paddingEnd
    }

    fun getPopupViewHeight(): Int {
        val numberOfRows = if (layoutTemplate.layout == Layout.BIDIRECTIONAL)
            layoutTemplate.numberOfRows + 1 else layoutTemplate.numberOfRows
        return numberOfRows * targetEmojiView.height +
            popupView.paddingTop + popupView.paddingBottom
    }

    private fun getLayoutTemplate(variants: List<String>): LayoutTemplate {
        val layout =
            if (variants.size == SQUARE_LAYOUT_VARIANT_COUNT)
                if (SQUARE_LAYOUT_EMOJI_NO_SKIN_TONE.contains(variants[0]))
                    Layout.SQUARE
                else Layout.SQUARE_WITH_SKIN_TONE_CIRCLE
            else if (variants.size == BIDIRECTIONAL_VARIANTS_COUNT)
                Layout.BIDIRECTIONAL
            else
                Layout.FLAT
        var template = when (layout) {
            Layout.SQUARE -> SQUARE_LAYOUT_TEMPLATE
            Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> SQUARE_LAYOUT_WITH_SKIN_TONES_TEMPLATE
            Layout.FLAT -> arrayOf(variants.indices.map { it + 1 }.toIntArray())
            Layout.BIDIRECTIONAL ->
                if (emojiFacingLeft)
                    arrayOf((variants.indices.filter { it % 12 < 6 }.map { it + 1 }).toIntArray())
                else
                    arrayOf((variants.indices.filter { it % 12 >= 6 }.map { it + 1 }).toIntArray())
        }
        val column = when (layout) {
            Layout.SQUARE, Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> template[0].size
            Layout.FLAT, Layout.BIDIRECTIONAL -> minOf(6, template[0].size)
        }
        val row = when (layout) {
            Layout.SQUARE, Layout.SQUARE_WITH_SKIN_TONE_CIRCLE -> template.size
            Layout.FLAT -> variants.size / column + if (variants.size % column == 0) 0 else 1
            Layout.BIDIRECTIONAL -> variants.size / 2 / column
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
        return LayoutTemplate(layout, template, row, column)
    }

    private data class LayoutTemplate(
        var layout: Layout,
        val template: Array<IntArray>,
        val numberOfRows: Int,
        val numberOfColumns: Int
    )

    private fun addBidirectionalLayoutHeader(popupView: LinearLayout) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        inflate(context, R.layout.emoji_picker_popup_bidirectional, row)
            .findViewById<AppCompatImageView>(R.id.emoji_picker_popup_bidirectional_icon)
            .apply {
                layoutParams = LinearLayout.LayoutParams(
                    targetEmojiView.width, targetEmojiView.height)
            }
        popupView.addView(row)
        val imageView =
            row.findViewById<AppCompatImageView>(R.id.emoji_picker_popup_bidirectional_icon)
        imageView.setOnClickListener {
            emojiFacingLeft = !emojiFacingLeft
            layoutTemplate = getLayoutTemplate(variants)
            popupView.removeViews( /* start= */1, layoutTemplate.numberOfRows)
            addRowsToPopupView()
        }
    }

    companion object {
        private enum class Layout { FLAT, SQUARE, SQUARE_WITH_SKIN_TONE_CIRCLE, BIDIRECTIONAL }

        /**
         * The number of variants expected when using a square layout strategy. Square layouts are
         * comprised of a 5x5 grid + the base variant.
         */
        private const val SQUARE_LAYOUT_VARIANT_COUNT = 26

        /**
         * The number of variants expected when using a bidirectional layout strategy. Bidirectional
         * layouts are comprised of bidirectional icon and a 3x6 grid with left direction emojis as
         * default. After clicking the bidirectional icon, it switches to a bidirectional icon and a 3x6
         * grid with right direction emojis.
         */
        private const val BIDIRECTIONAL_VARIANTS_COUNT = 36

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
