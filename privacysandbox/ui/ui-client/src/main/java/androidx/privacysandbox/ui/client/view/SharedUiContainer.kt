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

package androidx.privacysandbox.ui.client.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import kotlin.math.max

/**
 * [SharedUiContainer] is a [ViewGroup] that's designed to host 'shared UI', meaning it can handle
 * both client-owned and provider-owned UI (via [SandboxedSdkView]s).
 *
 * __Children__: the container should be used to host a single direct child view. However, it allows
 * adding more than one child view to it. All child views added to [SharedUiContainer] are placed in
 * the top left corner of the container plus padding, with newly added views overlaying older views.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
class SharedUiContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ViewGroup(context, attrs) {

    /**
     * Lays out the container's children in the top left corner with their measured sizes. Takes
     * into account the container's padding settings.
     *
     * Child views that are [View.GONE] are ignored and don't take any space.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Ignore View.GONE views as these are not expected to take any space for layout
            // purposes.
            if (child.visibility == GONE) {
                continue
            }
            child.layout(
                paddingLeft,
                paddingTop,
                child.measuredWidth + paddingLeft,
                child.measuredHeight + paddingTop
            )
        }
    }

    /**
     * Measures the container and its children. The size of the container is determined by the size
     * of its largest child, container's padding and suggested dimensions, but only if they do not
     * exceed size restrictions imposed by the container's parent view. Child views are measured
     * with respect to the container's padding settings.
     *
     * [View.GONE] child views are not used for sizing and are not measured.
     */
    // TODO(b/373866405): extract out the code common across SharedUiContainer and SandboxedSdkView.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        var maxHeight = 0
        var childState = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Don't use View.GONE views for sizing and measurement as these are not expected to
            // take any space.
            if (child.visibility == GONE) {
                continue
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            maxWidth = max(maxWidth, child.measuredWidth)
            maxHeight = max(maxHeight, child.measuredHeight)
            childState = combineMeasuredStates(childState, child.measuredState)
        }

        maxWidth = max(maxWidth + paddingLeft + paddingRight, suggestedMinimumWidth)
        maxHeight = max(maxHeight + paddingTop + paddingBottom, suggestedMinimumHeight)

        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                maxHeight,
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }
}
