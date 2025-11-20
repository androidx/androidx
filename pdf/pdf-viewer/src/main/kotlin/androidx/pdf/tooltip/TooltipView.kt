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

package androidx.pdf.tooltip

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatTextView
import androidx.pdf.R
import com.google.android.material.color.MaterialColors

/**
 * A custom [View] that displays a tooltip with a beak, pointing to a specific anchor position.
 *
 * The tooltip can be positioned above or below the anchor and the beak can be aligned to the left,
 * center, or right of the tooltip.
 */
internal class TooltipView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
        resolveAndSetTextColor()
        setBackgroundResource(R.drawable.tooltip_bottom_center)
        gravity = Gravity.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER
        text = context.getString(R.string.form_filling_tooltip)
        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    fun show(anchorPositionCoordinates: PointF, anchorViewDimensions: Point, yOffset: Int) {
        val positioner = TooltipPositioner(context)
        val (alignment, bounds) =
            positioner.computePosition(
                this,
                anchorPositionCoordinates,
                anchorViewDimensions,
                yOffset,
            )
        setBackground(getTooltipDrawable(alignment.x, alignment.y))
        setTextPadding(alignment.y)
        this.layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    fun setBackground(@DrawableRes backgroundDrawable: Int) {
        setBackgroundResource(backgroundDrawable)
    }

    private fun setTextPadding(verticalAlignment: Int) {
        val padding = resources.getDimensionPixelSize(R.dimen.tooltip_padding)
        val extraPadding = resources.getDimensionPixelSize(R.dimen.tooltip_extra_padding)
        // Add extra padding at the top/bottom to adjust the offset created due to the tooltip head.
        // This ensures that the text looks centered,i.e. it is at the visual center and not at
        // the real center.
        if (verticalAlignment == BEAK_ALIGNMENT_TOP) {
            setPadding(padding, padding + extraPadding, padding, padding)
        } else {
            setPadding(padding, padding, padding, padding + extraPadding)
        }
    }

    private fun getTooltipDrawable(horizontalAlignment: Int, verticalAlignment: Int): Int {
        return when (horizontalAlignment) {
            BEAK_ALIGNMENT_LEFT -> {
                if (verticalAlignment == BEAK_ALIGNMENT_TOP) {
                    R.drawable.tooltip_top_left
                } else {
                    R.drawable.tooltip_bottom_left
                }
            }
            BEAK_ALIGNMENT_RIGHT -> {
                if (verticalAlignment == BEAK_ALIGNMENT_TOP) {
                    R.drawable.tooltip_top_right
                } else {
                    R.drawable.tooltip_bottom_right
                }
            }
            else ->
                if (verticalAlignment == BEAK_ALIGNMENT_TOP) {
                    R.drawable.tooltip_top_center
                } else {
                    R.drawable.tooltip_bottom_center
                }
        }
    }

    private fun resolveAndSetTextColor() {
        val textColor =
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnTertiaryFixed)
        setTextColor(textColor)
    }

    @IntDef(BEAK_ALIGNMENT_TOP, BEAK_ALIGNMENT_BOTTOM)
    @Retention(AnnotationRetention.SOURCE)
    annotation class BeakVerticalAlignment

    @IntDef(BEAK_ALIGNMENT_LEFT, BEAK_ALIGNMENT_CENTER, BEAK_ALIGNMENT_RIGHT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class BeakHorizontalAlignment

    companion object {
        const val BEAK_ALIGNMENT_TOP = 0
        const val BEAK_ALIGNMENT_BOTTOM = 1

        const val BEAK_ALIGNMENT_LEFT = 0
        const val BEAK_ALIGNMENT_CENTER = 1
        const val BEAK_ALIGNMENT_RIGHT = 2
    }
}
