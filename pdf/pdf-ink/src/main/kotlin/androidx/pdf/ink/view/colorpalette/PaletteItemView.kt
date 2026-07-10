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

package androidx.pdf.ink.view.colorpalette

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.pdf.ink.R
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.colorpalette.model.Emoji
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import kotlin.math.min

/**
 * A custom [View] that displays a single item from a color palette, which can be either a solid
 * color or an emoji.
 *
 * This view is responsible for its own drawing and animation logic. It handles two main visual
 * states: selected and deselected.
 * - In the **deselected** state, it draws a circular shape representing the color.
 * - In the **selected** state, it animates into a squircle (a square with rounded corners), briefly
 *   expands its width for a "pop" effect, and fades in a check mark to indicate selection.
 */
internal class PaletteItemView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimension(R.dimen.color_palette_item_stroke_width)
        }

    private var emojiDrawable: Drawable? = null
    private val tickDrawable: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.ic_check)?.mutate()

    private val paletteItemContentSize =
        resources.getDimensionPixelSize(R.dimen.color_palette_content_size)
    private val paletteItemExpandedWidth =
        resources.getDimensionPixelSize(R.dimen.color_palette_expanded_content_width)
    private val squareCornerRadius = resources.getDimension(R.dimen.corner_radius_8dp)
    // Setting corner radius to half will result in drawing a circle
    private val circleCornerRadius = paletteItemContentSize.toFloat() / 2
    // Below 2 values will be frequently updated when animation is running.
    private var currentCornerRadius = circleCornerRadius
    private var currentWidth = paletteItemContentSize

    // Bounds in which either a color or emoji will be drawn
    private val paletteDrawingBounds = RectF()

    private var paletteItem: PaletteItem? = null
    private var tickAlpha = INVISIBLE_ALPHA // initially, none of the view is selected
    private var animatorSet: AnimatorSet? = null

    init {
        clipToOutline = true
        val defaultPadding = resources.getDimensionPixelSize(R.dimen.padding_8dp)
        setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
    }

    /** Sets the PaletteItem to be displayed and triggers a redraw. */
    fun setPaletteItem(item: PaletteItem) {
        this.paletteItem = item
        this.contentDescription = item.contentDescription

        when (item) {
            is Color -> {
                colorPaint.color = item.color
                outlinePaint.color = item.outlineColor
                tickDrawable?.setTint(item.tickColor)
                emojiDrawable = null
            }
            is Emoji -> {
                emojiDrawable = ContextCompat.getDrawable(context, item.emoji)
                colorPaint.color = 0
                outlinePaint.color = 0
            }
        }
        invalidate()
    }

    override fun setSelected(selected: Boolean) {
        setSelected(selected, animate = true)
    }

    fun setSelected(selected: Boolean, animate: Boolean = true) {
        val paletteItem = paletteItem ?: return

        if (isSelected == selected) return

        super.setSelected(selected)

        // TODO(b/452228992): Check animation for emoji from UX
        if (paletteItem !is Color) return

        animatorSet?.cancel() // Cancel any ongoing animation

        if (animate) {
            if (selected) playSelectionAnimation() else playDeselectionAnimation()
        } else {
            // No animation? Instantly set the final state.
            if (selected) {
                currentWidth = paletteItemContentSize
                currentCornerRadius = squareCornerRadius
                tickAlpha = VISIBLE_ALPHA
            } else {
                currentWidth = paletteItemContentSize
                currentCornerRadius = (paletteItemContentSize / 2).toFloat()
                tickAlpha = INVISIBLE_ALPHA
            }
            invalidate()
        }
    }

    /** Resets the view to its default, non-selected state without animation. */
    fun reset() {
        animatorSet?.cancel()
        isSelected = false
        currentCornerRadius = (paletteItemContentSize / 2).toFloat()
        currentWidth = paletteItemContentSize
        tickAlpha = INVISIBLE_ALPHA
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Raw item size with paddings
        val colorPaletteItemSize = resources.getDimensionPixelSize(R.dimen.color_palette_item_size)

        val desiredWidth = resolveSize(colorPaletteItemSize, widthMeasureSpec)
        val desiredHeight = resolveSize(colorPaletteItemSize, heightMeasureSpec)
        //  largest possible square that can fit in the space allocated by the parent
        val finalSize = min(desiredWidth, desiredHeight)

        setMeasuredDimension(finalSize, finalSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Do not draw anything if no item is set
        val item = paletteItem ?: return

        // To achieve the "overshoot" width expansion animation while keeping the View's own
        // bounds fixed, we calculate a temporary, wider drawing area. `currentWidth` is animated
        // from a normal size (e.g., 32dp) to an expanded size (e.g., 40dp) and back.
        val halfWidthDelta = (currentWidth - paletteItemContentSize) / 2

        paletteDrawingBounds.set(
            paddingLeft.toFloat() - halfWidthDelta,
            paddingTop.toFloat(),
            (width - paddingRight).toFloat() + halfWidthDelta,
            (height - paddingBottom).toFloat(),
        )

        when (item) {
            is Color -> {
                drawColorItem(canvas)
                // Selection tick is only supported for color item
                if (tickAlpha > 0) drawSelectionTick(canvas)
            }
            is Emoji -> drawEmojiItem(canvas)
        }
    }

    private fun drawColorItem(canvas: Canvas) {
        canvas.drawRoundRect(
            paletteDrawingBounds,
            currentCornerRadius,
            currentCornerRadius,
            colorPaint,
        )
        // Draw the stroke on top of the fill
        canvas.drawRoundRect(
            paletteDrawingBounds,
            currentCornerRadius,
            currentCornerRadius,
            outlinePaint,
        )
    }

    private fun drawEmojiItem(canvas: Canvas) {
        emojiDrawable?.let {
            it.bounds.set(
                paletteDrawingBounds.left.toInt(),
                paletteDrawingBounds.top.toInt(),
                paletteDrawingBounds.right.toInt(),
                paletteDrawingBounds.bottom.toInt(),
            )
            it.draw(canvas)
        }
    }

    private fun drawSelectionTick(canvas: Canvas) {
        tickDrawable?.alpha = tickAlpha
        // Scale tick to 60% of palette drawing bounds
        val tickSize =
            (min(paletteDrawingBounds.width(), paletteDrawingBounds.height()) * 0.6f).toInt()
        // Center tick
        val tickMarginX = ((width - tickSize) / 2)
        val tickMarginY = ((height - tickSize) / 2)
        tickDrawable?.setBounds(tickMarginX, tickMarginY, width - tickMarginX, height - tickMarginY)
        tickDrawable?.draw(canvas)
    }

    /**
     * Plays a coordinated animation to transition the view to its "selected" state.
     *
     * This animation consists of three parts that run simultaneously:
     * 1. **Corner Animation**: The corners of the shape animate from a circle
     *    (`circleCornerRadius`) to a square (`squareCornerRadius`).
     * 2. **Width Animation**: The width of the shape expands slightly and then returns to its
     *    original size, creating an "overshoot" or "pop" effect.
     * 3. **Tick Animation**: After a short delay, the selection tick mark fades into view.
     *
     * The animations update the `currentCornerRadius`, `currentWidth`, and `tickAlpha` properties
     * and call `invalidate()` to redraw the view on each frame.
     */
    private fun playSelectionAnimation() {
        val cornerAnimator =
            ValueAnimator.ofFloat(currentCornerRadius, squareCornerRadius).apply {
                duration = CORNER_RADIUS_ANIMATION_DURATION
                addUpdateListener { currentCornerRadius = it.animatedValue as Float }
            }

        // Animate width from 32dp -> 40dp -> 32dp
        val widthAnimator =
            ValueAnimator.ofInt(
                    paletteItemContentSize,
                    paletteItemExpandedWidth,
                    paletteItemContentSize,
                )
                .apply {
                    duration = OVERSHOOT_ANIMATION_DURATION
                    interpolator = OvershootInterpolator(1f)
                    addUpdateListener {
                        currentWidth = it.animatedValue as Int
                        invalidate()
                    }
                }

        val tickAnimator =
            ValueAnimator.ofInt(tickAlpha, VISIBLE_ALPHA).apply {
                duration = TICK_ALPHA_ANIMATION_DURATION
                startDelay = TICK_ANIMATION_INITIAL_DELAY // Start after the shape has mostly formed
                addUpdateListener { tickAlpha = it.animatedValue as Int }
            }

        animatorSet =
            AnimatorSet().apply {
                playTogether(cornerAnimator, widthAnimator, tickAnimator)
                start()
            }
    }

    /**
     * Plays a coordinated animation to transition the view to its "deselected" state.
     *
     * This animation consists of two parts that run simultaneously:
     * 1. **Corner Animation**: The corners of the shape animate from a square
     *    (`squareCornerRadius`) back to a perfect circle (`circleCornerRadius`).
     * 2. **Tick Animation**: The selection tick mark quickly fades out of view.
     */
    private fun playDeselectionAnimation() {
        val cornerAnimator =
            ValueAnimator.ofFloat(currentCornerRadius, circleCornerRadius).apply {
                duration = CORNER_RADIUS_ANIMATION_DURATION
                addUpdateListener {
                    currentCornerRadius = it.animatedValue as Float
                    invalidate()
                }
            }

        val tickAnimator =
            ValueAnimator.ofInt(tickAlpha, INVISIBLE_ALPHA).apply {
                duration = TICK_ALPHA_ANIMATION_DURATION
                addUpdateListener { tickAlpha = it.animatedValue as Int }
            }

        animatorSet =
            AnimatorSet().apply {
                playTogether(cornerAnimator, tickAnimator)
                start()
            }
    }

    companion object {
        private const val INVISIBLE_ALPHA = 0
        private const val VISIBLE_ALPHA = 255
        private const val OVERSHOOT_ANIMATION_DURATION = 500L
        private const val CORNER_RADIUS_ANIMATION_DURATION = 300L
        private const val TICK_ALPHA_ANIMATION_DURATION = 200L
        private const val TICK_ANIMATION_INITIAL_DELAY = 300L
    }
}
