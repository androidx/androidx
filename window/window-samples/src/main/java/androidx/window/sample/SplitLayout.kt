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

package androidx.window.sample

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.widget.FrameLayout
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

/**
 * An example of split-layout for two views, separated by a display feature that goes across the
 * window. When both start and end views are added, it checks if there are display features that
 * separate the area in two (e.g. fold or hinge) and places them side-by-side or top-bottom.
 */
class SplitLayout : FrameLayout {
    private var windowLayoutInfo: WindowLayoutInfo? = null
    private var startViewId = 0
    private var endViewId = 0

    private var lastWidthMeasureSpec: Int = 0
    private var lastHeightMeasureSpec: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setAttributes(attrs)
    }

    fun updateWindowLayout(windowLayoutInfo: WindowLayoutInfo) {
        this.windowLayoutInfo = windowLayoutInfo
        requestLayout()
    }

    private fun setAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.SplitLayout, 0, 0).apply {
            try {
                startViewId = getResourceId(R.styleable.SplitLayout_startViewId, 0)
                endViewId = getResourceId(R.styleable.SplitLayout_endViewId, 0)
            } finally {
                recycle()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val startView = findStartView()
        val endView = findEndView()
        val splitPositions = splitViewPositions(startView, endView)

        if (startView != null && endView != null && splitPositions != null) {
            val startPosition = splitPositions[0]
            val startWidthSpec = MeasureSpec.makeMeasureSpec(startPosition.width(), EXACTLY)
            val startHeightSpec = MeasureSpec.makeMeasureSpec(startPosition.height(), EXACTLY)
            startView.measure(startWidthSpec, startHeightSpec)
            startView.layout(
                startPosition.left, startPosition.top, startPosition.right,
                startPosition.bottom
            )

            val endPosition = splitPositions[1]
            val endWidthSpec = MeasureSpec.makeMeasureSpec(endPosition.width(), EXACTLY)
            val endHeightSpec = MeasureSpec.makeMeasureSpec(endPosition.height(), EXACTLY)
            endView.measure(endWidthSpec, endHeightSpec)
            endView.layout(
                endPosition.left, endPosition.top, endPosition.right,
                endPosition.bottom
            )
        } else {
            super.onLayout(changed, left, top, right, bottom)
        }
    }

    private fun findStartView(): View? {
        var startView = findViewById<View>(startViewId)
        if (startView == null && childCount > 0) {
            startView = getChildAt(0)
        }
        return startView
    }

    private fun findEndView(): View? {
        var endView = findViewById<View>(endViewId)
        if (endView == null && childCount > 1) {
            endView = getChildAt(1)
        }
        return endView
    }

    /**
     * Gets the position of the split for this view.
     * @return A rect that defines of split, or {@code null} if there is no split.
     */
    private fun splitViewPositions(startView: View?, endView: View?): Array<Rect>? {
        if (windowLayoutInfo == null || startView == null || endView == null) {
            return null
        }

        // Calculate the area for view's content with padding
        val paddedWidth = width - paddingLeft - paddingRight
        val paddedHeight = height - paddingTop - paddingBottom

        windowLayoutInfo?.displayFeatures
            ?.firstOrNull { feature -> isValidFoldFeature(feature) }
            ?.let { feature ->
                val featureBounds = adjustFeaturePositionOffset(feature, this)
                if (feature.bounds.left == 0) { // Horizontal layout
                    val topRect = Rect(
                        paddingLeft, paddingTop,
                        paddingLeft + paddedWidth, featureBounds.top
                    )
                    val bottomRect = Rect(
                        paddingLeft, featureBounds.bottom,
                        paddingLeft + paddedWidth, paddingTop + paddedHeight
                    )

                    if (measureAndCheckMinSize(topRect, startView) &&
                        measureAndCheckMinSize(bottomRect, endView)
                    ) {
                        return arrayOf(topRect, bottomRect)
                    }
                } else if (feature.bounds.top == 0) { // Vertical layout
                    val leftRect = Rect(
                        paddingLeft, paddingTop,
                        featureBounds.left, paddingTop + paddedHeight
                    )
                    val rightRect = Rect(
                        featureBounds.right, paddingTop,
                        paddingLeft + paddedWidth, paddingTop + paddedHeight
                    )

                    if (measureAndCheckMinSize(leftRect, startView) &&
                        measureAndCheckMinSize(rightRect, endView)
                    ) {
                        return arrayOf(leftRect, rightRect)
                    }
                }
            }

        // We have tried to fit the children and measured them previously. Since they didn't fit,
        // we need to measure again to update the stored values.
        measure(lastWidthMeasureSpec, lastHeightMeasureSpec)
        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        lastWidthMeasureSpec = widthMeasureSpec
        lastHeightMeasureSpec = heightMeasureSpec
    }

    /**
     * Measures a child view and sees if it will fit in the provided rect.
     * <p>Note: This method calls [View.measure] on the child view, which updates
     * its stored values for measured with and height. If the view will end up with different
     * values, it should be measured again.
     */
    private fun measureAndCheckMinSize(rect: Rect, childView: View): Boolean {
        val widthSpec = MeasureSpec.makeMeasureSpec(rect.width(), AT_MOST)
        val heightSpec = MeasureSpec.makeMeasureSpec(rect.height(), AT_MOST)
        childView.measure(widthSpec, heightSpec)
        return childView.measuredWidthAndState and MEASURED_STATE_TOO_SMALL == 0 &&
            childView.measuredHeightAndState and MEASURED_STATE_TOO_SMALL == 0
    }

    private fun isValidFoldFeature(displayFeature: DisplayFeature): Boolean {
        return displayFeature is FoldingFeature
    }
}