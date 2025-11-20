/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.autofill.inline.v1

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.autofill.R
import kotlin.math.max

/**
 * A custom ViewGroup that arranges and manages the layout of a primary title and a secondary
 * subtitle.
 * - This layout horizontally positions a title and a subtitle, intelligently handling truncation
 *   based on the available space and a set of predefined rules to ensure optimal readability.
 * - Layout and Truncation Logic: The primary objective is to display both the title and subtitle in
 *   their entirety whenever possible. When the combined width of the title and subtitle exceeds the
 *   available space, the following truncation rules are applied in order:
 *     1. Sufficient Space for Full Title: If the title can be fully displayed, and the remaining
 *        space is at least 37% of the parent's total width, the subtitle will be truncated to fit
 *        the remaining space.
 *     2. Sufficient Space for Full Subtitle: If the subtitle's required width does not exceed 37%
 *        of the parent's total width, the subtitle will be displayed in full, and the title will be
 *        truncated to fit the remaining space.
 *     3. Default Truncation: In all other cases, the subtitle is truncated and constrained to a
 *        maximum width of 37% of the parent. The title will then occupy the rest of the available
 *        space and be truncated if necessary.
 */
internal class BalancingTitleSubtitleLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs) {

    private lateinit var startIcon: View
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var endIcon: View

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Assuming the children in the XML layout is:
        // 1. Start Icon
        // 2. Title TextView
        // 3. Subtitle TextView
        // 4. End Icon
        if (childCount > 4) {
            throw IllegalStateException("TitleSubtitleLayout can host up to 4 children.")
        }

        startIcon = findViewById(R.id.autofill_inline_suggestion_start_icon)
        titleView = findViewById(R.id.autofill_inline_suggestion_title)
        subtitleView = findViewById(R.id.autofill_inline_suggestion_subtitle)
        endIcon = findViewById(R.id.autofill_inline_suggestion_end_icon)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableWidth = parentWidth - paddingStart - paddingEnd

        var totalWidthUsed = 0
        var totalHeight = 0

        // 1. Measure start and end icons
        measureChildWithMargins(startIcon, widthMeasureSpec, 0, heightMeasureSpec, 0)
        totalWidthUsed += startIcon.totalWidth
        totalHeight = max(totalHeight, startIcon.totalHeight)

        measureChildWithMargins(endIcon, widthMeasureSpec, 0, heightMeasureSpec, 0)
        totalWidthUsed += endIcon.totalWidth
        totalHeight = max(totalHeight, endIcon.totalHeight)

        // 2. Measure title and subtitle without constraints to get their desired widths
        measureChildWithMargins(titleView, widthMeasureSpec, 0, heightMeasureSpec, 0)
        measureChildWithMargins(subtitleView, widthMeasureSpec, 0, heightMeasureSpec, 0)

        val titleDesiredWidth = titleView.totalWidth
        val subtitleDesiredWidth = subtitleView.totalWidth
        val textAvailableWidth = availableWidth - totalWidthUsed

        val subtitleMaxWidth = (parentWidth * SUBTITLE_MAX_WIDTH_PERCENTAGE).toInt()

        var titleWidth = titleDesiredWidth
        var subtitleWidth = subtitleDesiredWidth

        if (titleDesiredWidth + subtitleDesiredWidth > textAvailableWidth) {
            // Truncation logic
            if (textAvailableWidth - titleDesiredWidth >= subtitleMaxWidth) {
                // Truncate subtitle only
                subtitleWidth = textAvailableWidth - titleDesiredWidth
            } else if (subtitleDesiredWidth <= subtitleMaxWidth) {
                // Truncate title only
                titleWidth = textAvailableWidth - subtitleDesiredWidth
            } else {
                // Truncate both, subtitle gets 37%
                subtitleWidth = subtitleMaxWidth
                titleWidth = textAvailableWidth - subtitleWidth
            }
        }

        // Remeasure title and subtitle with the final calculated widths
        val titleWidthMeasureSpec =
            MeasureSpec.makeMeasureSpec(
                titleWidth - titleView.marginHorizontal,
                MeasureSpec.EXACTLY,
            )
        val subtitleWidthMeasureSpec =
            MeasureSpec.makeMeasureSpec(
                subtitleWidth - subtitleView.marginHorizontal,
                MeasureSpec.EXACTLY,
            )
        val heightMeasure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        titleView.measure(titleWidthMeasureSpec, heightMeasure)
        subtitleView.measure(subtitleWidthMeasureSpec, heightMeasure)

        totalWidthUsed += titleView.totalWidth
        totalWidthUsed += subtitleView.totalWidth
        totalHeight = max(totalHeight, titleView.totalHeight)
        totalHeight = max(totalHeight, subtitleView.totalHeight)

        setMeasuredDimension(
            resolveSize(totalWidthUsed + paddingStart + paddingEnd, widthMeasureSpec),
            resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availableHeight = b - t - paddingTop - paddingBottom
        val parentPaddingTop = paddingTop
        val parentWidth = r - l
        val isLtr = layoutDirection == LAYOUT_DIRECTION_LTR

        var currentStart = paddingStart

        fun View.layout() {
            if (visibility == GONE) return
            // center aligned in parent's available height
            val top = parentPaddingTop + (availableHeight - measuredHeight) / 2
            currentStart += marginStart
            val left = if (isLtr) currentStart else parentWidth - currentStart - measuredWidth
            layout(
                /* l = */ left,
                /* t = */ top,
                /* r = */ left + measuredWidth,
                /* b = */ top + measuredHeight,
            )
            currentStart += measuredWidth + marginEnd
        }

        startIcon.layout()
        titleView.layout()
        subtitleView.layout()
        endIcon.layout()
    }

    override fun generateDefaultLayoutParams() =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?) = MarginLayoutParams(context, attrs)

    override fun checkLayoutParams(p: LayoutParams?) = p is MarginLayoutParams

    override fun generateLayoutParams(p: LayoutParams?) = MarginLayoutParams(p)

    companion object {
        private const val SUBTITLE_MAX_WIDTH_PERCENTAGE = 0.37f

        private val View.totalWidth: Int
            get() = measuredWidth + marginHorizontal

        private val View.marginHorizontal: Int
            get() = marginStart + marginEnd

        private val View.totalHeight: Int
            get() = measuredHeight + marginTop + marginBottom

        private val View.marginStart
            get() = (layoutParams as? MarginLayoutParams)?.marginStart ?: 0

        private val View.marginEnd
            get() = (layoutParams as? MarginLayoutParams)?.marginEnd ?: 0

        private val View.marginTop
            get() = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0

        private val View.marginBottom
            get() = (layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0
    }
}
