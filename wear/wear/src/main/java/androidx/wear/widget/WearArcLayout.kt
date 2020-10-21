/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import androidx.wear.R
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Container which will lay its elements out on an arc. Elements will be relative to a given
 * anchor angle (where 0 degrees = 12 o clock), where the layout relative to the anchor angle is
 * controlled using `anchorPositionDegrees`. The thickness of the arc is calculated based on the
 * child element with the greatest height (in the case of Android widgets), or greatest thickness
 * (for curved widgets). By default, the container lays its children one by one in clockwise
 * direction. The attribute 'clockwise' can be set to false to make the layout direction as
 * anti-clockwise. These two types of widgets will be drawn as follows.
 *
 *
 * Standard Android Widgets:
 *
 *
 * These widgets will be drawn as usual, but placed at the correct position on the arc, with the
 * correct amount of rotation applied. As an example, for an Android Text widget, the text
 * baseline would be drawn at a tangent to the arc. The arc length of a widget is obtained by
 * measuring the width of the widget, and transforming that to the length of an arc on a circle.
 *
 *
 * A standard Android widget will be measured as usual, but the maximum height constraint will be
 * capped at the minimum radius of the arc (i.e. width / 2).
 *
 *
 * "Curved" widgets:
 *
 *
 * Widgets which implement ArcLayoutWidget are expected to draw themselves within an arc
 * automatically. These widgets will be measured with the full dimensions of the arc container.
 * They are also expected to provide their thickness (used when calculating the thickness of the
 * arc) and the current sweep angle (used for laying out when drawing). Note that the
 * WearArcLayout will apply a rotation transform to the canvas before drawing this child; the
 * inner child need not perform any rotations itself.
 *
 *
 * An example of a widget which implements this interface is WearCurvedTextView, which will lay
 * itself out along the arc.
 */
@UiThread
public class WearArcLayout @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Interface for a widget which knows it is being rendered inside an arc, and will draw
     * itself accordingly. Any widget implementing this interface will receive the full-sized
     * canvas pre-rotated, in its draw call.
     */
    public interface ArcLayoutWidget {
        /** Returns the sweep angle that this widget is drawn with.  */
        public fun getSweepAngleDegrees(): Float

        /** Returns the thickness of this widget inside the arc.  */
        public fun getThicknessPx(): Int

        /** Check whether the widget contains invalid attributes as a child of WearArcLayout  */
        public fun checkInvalidAttributeAsChild(clockwise: Boolean)

        /** Return whether the widget will handle the layout rotation requested by the container
         *  If return true, make sure that the layout rotation is done inside the widget since the
         *  container will skip this process.
         */
        public fun handleLayoutRotate(angle: Float): Boolean
    }

    /**
     * Layout parameters for a widget added to an arc. This allows each element to specify
     * whether or not it should be rotated (around the center of the child) when drawn inside the
     * arc. For example, when the child is put at the center-bottom of the arc, whether the
     * parent layout is responsible to rotate it 180 degree to draw it upside down.
     *
     * Note that the `rotate` parameter is ignored when drawing "Fullscreen" elements.
     */
    public class LayoutParams : ViewGroup.LayoutParams {

        internal companion object {
            /** Align to the outer edge of the parent ArcContainer.  */
            const val VALIGN_OUTER = 0

            /** Align to the center of the parent ArcContainer.  */
            const val VALIGN_CENTER = 1

            /** Align to the inner edge of the parent ArcContainer.  */
            const val VALIGN_INNER = 2

            /** Vertical alignment of elements within the arc.  */
            @IntDef(VALIGN_OUTER, VALIGN_CENTER, VALIGN_INNER)
            internal annotation class VerticalAlignment
        }

        @VerticalAlignment
        public var verticalAlignment: Int = VALIGN_CENTER
        public var rotate: Boolean = true

        /**
         * Creates a new set of layout parameters. The values are extracted from the supplied
         * attributes set and context.
         *
         * @param c the application environment
         * @param attrs the set of attributes from which to extract the layout parameters' values
         */
        public constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val a = c.obtainStyledAttributes(attrs, R.styleable.WearArcLayout)
            rotate = a.getBoolean(R.styleable.WearArcLayout_Layout_layout_rotate, true)
            verticalAlignment = a.getInt(
                R.styleable.WearArcLayout_Layout_layout_valign,
                VALIGN_CENTER
            )
            a.recycle()
        }

        /**
         * Creates a new set of layout parameters with specified width and height
         *
         * @param width the width, either WRAP_CONTENT, MATCH_PARENT or a fixed size in pixels
         * @param height, the height, either WRAP_CONTENT, MATCH_PARENT or a fixed size in pixels
         */
        public constructor(width: Int, height: Int) : super(width, height) {}

        /** Copy constructor */
        public constructor(source: ViewGroup.LayoutParams?) : super(source) {}
    }

    private var thicknessPx = 0
    public var clockwise: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    @AnchorType
    public var anchorType: Int = DEFAULT_ANCHOR_TYPE
        set(value) {
            require(value in ANCHOR_START..ANCHOR_END) { "Unknown anchor type" }
            field = value
            invalidate()
        }

    public var anchorAngleDegrees: Float = DEFAULT_START_ANGLE_DEGREES
        set(value) {
            field = value
            invalidate()
        }

    private var currentCumulativeAngle = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Need to derive the thickness of the curve from the children. We're a curve, so the
        // children can only be sized up to (width or height)/2 units. This currently only
        // supports fitting to a circle.
        //
        // No matter what, fit to the given size, be it a maximum or a fixed size. It doesn't make
        // sense for this container to wrap its children.
        var actualWidthPx = MeasureSpec.getSize(widthMeasureSpec)
        var actualHeightPx = MeasureSpec.getSize(heightMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED &&
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
        ) {
            // We can't actually resolve this.
            // Let's fit to the screen dimensions, for need of anything better...
            val displayMetrics = context.resources.displayMetrics
            actualWidthPx = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            actualHeightPx = (displayMetrics.heightPixels / displayMetrics.density).toInt()
        }
        val maxChildDimension = min(actualHeightPx, actualWidthPx) / 2

        // Measure all children in the new measurespec, and cache the largest.
        val childMeasureSpec = MeasureSpec.makeMeasureSpec(maxChildDimension, MeasureSpec.AT_MOST)

        // We need to do two measure passes. First, we need to measure all "normal" children, and
        // get the thickness of all "WearArcLayout" children. Once we have that, we know the
        // maximum thickness, and we can lay out the "WearArcLayout" children, taking into
        // account their vertical alignment.
        var maxChildHeightPx = 0
        var childState = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            // ArcLayoutWidget is a special case. Because of how it draws, fit it to the size of
            // the whole widget.
            when (child) {
                is ArcLayoutWidget -> {
                    maxChildHeightPx =
                        max(maxChildHeightPx, (child as ArcLayoutWidget).getThicknessPx())
                }
                else -> {
                    // Measure the child.
                    measureChild(child, childMeasureSpec, childMeasureSpec)
                    maxChildHeightPx = max(maxChildHeightPx, child.measuredHeight)
                    childState = combineMeasuredStates(childState, child.measuredState)
                }
            }
        }
        thicknessPx = maxChildHeightPx

        // And now do the pass for the ArcLayoutWidgets
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            if (child is ArcLayoutWidget) {
                val arcLayoutChild: ArcLayoutWidget = child
                val childLayoutParams = child.layoutParams as LayoutParams

                val childThicknessPx: Int = arcLayoutChild.getThicknessPx()
                val thicknessDiffPx = thicknessPx - childThicknessPx

                var insetPx = when (childLayoutParams.verticalAlignment) {
                    LayoutParams.VALIGN_CENTER -> thicknessDiffPx / 2f
                    LayoutParams.VALIGN_INNER -> thicknessDiffPx.toFloat()
                    else -> 0f
                }

                val innerChildMeasureSpec = MeasureSpec.makeMeasureSpec(
                    maxChildDimension * 2 - (insetPx * 2).roundToInt(),
                    MeasureSpec.EXACTLY
                )
                measureChild(
                    child,
                    getChildMeasureSpec(
                        innerChildMeasureSpec, /* padding = */ 0, childLayoutParams.width
                    ),
                    getChildMeasureSpec(
                        innerChildMeasureSpec, /* padding = */ 0, childLayoutParams.height
                    )
                )

                childState = combineMeasuredStates(childState, child.measuredState)
            }
        }

        setMeasuredDimension(
            resolveSizeAndState(actualWidthPx, widthMeasureSpec, childState),
            resolveSizeAndState(actualHeightPx, heightMeasureSpec, childState)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            // Curved container widgets have been measured so that the "arc" inside their widget
            // will touch the outside of the box they have been measured in, taking into account
            // the vertical alignment. Just grow them from the center.
            when (child) {
                is ArcLayoutWidget -> {
                    val leftPx: Int =
                        (this.measuredWidth / 2f - child.measuredWidth / 2f).roundToInt()
                    val topPx: Int =
                        (this.measuredHeight / 2f - child.measuredHeight / 2f).roundToInt()
                    child.layout(
                        leftPx, topPx, leftPx + child.measuredWidth, topPx + child.measuredHeight
                    )
                }
                else -> {
                    // Normal widgets need to be placed on their canvas, taking into account
                    // their vertical position.
                    val leftPx: Int =
                        (this.measuredWidth / 2f - child.measuredWidth / 2f).roundToInt()
                    val topPx: Int = getChildTopInset(child)
                    child.layout(
                        leftPx,
                        topPx,
                        leftPx + child.measuredWidth,
                        topPx + child.measuredHeight
                    )
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        currentCumulativeAngle = calculateInitialRotation()
        super.dispatchDraw(canvas)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        // Rotate the canvas to make the children render in the right place.
        canvas.save()
        val arcAngle = calculateArcAngle(child)
        val preRotation = arcAngle / 2f
        val multiplier = if (clockwise) 1f else -1f
        if (child is ArcLayoutWidget) {
            (child as ArcLayoutWidget).checkInvalidAttributeAsChild(clockwise)

            // Special case for ArcLayoutWidget. This doesn't need pre-rotating to get the center
            // of canvas lines up, as it should already know how to draw itself correctly from
            // the "current" rotation. The layout rotation is always passed to the child widget,
            // if the child has not handled this rotation by itself, the parent will have to
            // rotate the canvas to apply this layout.
            if (!(child as ArcLayoutWidget).handleLayoutRotate(
                    multiplier * currentCumulativeAngle
                )
            ) {
                canvas.rotate(
                    multiplier * currentCumulativeAngle, measuredWidth / 2f, measuredHeight / 2f
                )
            }
        } else {
            canvas.rotate(
                multiplier * (currentCumulativeAngle + preRotation),
                measuredWidth / 2f,
                measuredHeight / 2f
            )
            val delta = measuredHeight - measuredWidth
            if (delta > 0) {
                canvas.translate(0f, delta / 2.toFloat())
            } else {
                canvas.translate(delta / 2.toFloat(), 0f)
            }

            // Do we need to do some counter rotation?
            val layoutParams = child.layoutParams as LayoutParams

            // For anti-clockwise layout, especially when mixing standard Android widget with
            // ArcLayoutWidget as children, we might need to rotate the standard widget to make
            // them with the same upwards direction. Note that the strange rotation center is
            // because the child view is not x-centered but at the top of this container.
            canvas.rotate(
                if (clockwise || !layoutParams.rotate) 0f else 180f,
                measuredWidth / 2f,
                child.measuredHeight / 2f
            )
            if (!layoutParams.rotate) {
                // Re-rotate about the top of the canvas, around the center of the actual child.
                val childInset: Int = getChildTopInset(child)
                canvas.rotate(
                    -multiplier * (currentCumulativeAngle + preRotation),
                    measuredWidth / 2f,
                    (child.measuredHeight / 2f) + childInset
                )
            }
        }
        currentCumulativeAngle += arcAngle
        return super.drawChild(canvas, child, drawingTime).also { canvas.restore() }
    }

    private fun calculateInitialRotation(): Float {
        val multiplier = if (clockwise) 1f else -1f
        if (anchorType == ANCHOR_START) {
            return multiplier * anchorAngleDegrees
        }
        var totalArcAngle = (0 until childCount).map { calculateArcAngle(getChildAt(it)) }.sum()
        return when (anchorType) {
            ANCHOR_CENTER -> multiplier * anchorAngleDegrees - totalArcAngle / 2f
            ANCHOR_END -> multiplier * anchorAngleDegrees - totalArcAngle
            else -> 0f
        }
    }

    private fun calculateArcAngle(view: View): Float {
        if (view.visibility == GONE) {
            return 0f
        }
        return (view as? ArcLayoutWidget) ?.getSweepAngleDegrees() ?: let {
            val radiusPx = measuredWidth / 2f - thicknessPx
            Math.toDegrees(2 * asin(view.measuredWidth / radiusPx / 2.0)).toFloat()
        }
    }

    private fun getChildTopInset(child: View): Int {
        val childLayoutParams: LayoutParams = child.getLayoutParams() as LayoutParams

        val thicknessDiffPx: Int = thicknessPx - child.getMeasuredHeight()

        return when (childLayoutParams.verticalAlignment) {
            LayoutParams.VALIGN_OUTER -> 0
            LayoutParams.VALIGN_CENTER -> (thicknessDiffPx / 2f).roundToInt()
            LayoutParams.VALIGN_INNER -> thicknessDiffPx
            else -> 0 // Normally unreachable
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean = p is LayoutParams
    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams = LayoutParams(p)
    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams = LayoutParams(
        context, attrs
    )
    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
    )

    public companion object {
        /**
         * Anchor at the start of the set of elements drawn within this container. This causes
         * the first child to be drawn from `anchorAngle` degrees, to the right.
         *
         * As an example, if this container contains two arcs, one having 10 degrees of sweep and
         * the other having 20 degrees of sweep, the first will be drawn between 0-10 degrees,
         * and the second between 10-30 degrees.
         */
        public const val ANCHOR_START: Int = 0

        /**
         * Anchor at the center of the set of elements drawn within this container.
         *
         *
         * As an example, if this container contains two arcs, one having 10 degrees of sweep and
         * the other having 20 degrees of sweep, the first will be drawn between -15 and -5
         * degrees, and the second between -5 and 15 degrees.
         */
        public const val ANCHOR_CENTER: Int = 1

        /**
         * Anchor at the end of the set of elements drawn within this container. This causes the
         * last element to end at `anchorAngle` degrees, with the other elements swept to the left.
         *
         *
         * As an example, if this container contains two arcs, one having 10 degrees of sweep and
         * the other having 20 degrees of sweep, the first will be drawn between -30 and -20
         * degrees, and the second between -20 and 0 degrees.
         */
        public const val ANCHOR_END: Int = 2

        /** Annotation for anchor types.  */
        @IntDef(ANCHOR_START, ANCHOR_CENTER, ANCHOR_END)
        internal annotation class AnchorType

        private const val DEFAULT_LAYOUT_DIRECTION_IS_CLOCKWISE = true // clockwise
        private const val DEFAULT_START_ANGLE_DEGREES = 0f

        @AnchorType
        private val DEFAULT_ANCHOR_TYPE = ANCHOR_START
    }

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.WearArcLayout, defStyleAttr, defStyleRes
        )
        anchorType = a.getInt(R.styleable.WearArcLayout_anchorPosition, DEFAULT_ANCHOR_TYPE)
        anchorAngleDegrees =
            a.getFloat(R.styleable.WearArcLayout_anchorAngleDegrees, DEFAULT_START_ANGLE_DEGREES)
        clockwise = a.getBoolean(
            R.styleable.WearArcLayout_clockwise, DEFAULT_LAYOUT_DIRECTION_IS_CLOCKWISE
        )
        a.recycle()
    }
}
