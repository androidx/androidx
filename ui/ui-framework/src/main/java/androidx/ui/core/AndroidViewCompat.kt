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

package androidx.ui.core

import android.view.View
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite

/**
 * Adapter from [View] to [ComponentNode].
 */
internal val AndroidViewAdapter: (Any, Any) -> Any? = { parent, child ->
    if (parent is ComponentNode) {
        (child as? View)?.toComponentNode()
    } else {
        null
    }
}

/**
 * Builds a [ComponentNode] tree representation for an Android [View].
 * The component nodes will proxy the Compose core calls to the [View].
 */
private fun View.toComponentNode(): ComponentNode {
    // TODO(popam): forward pointer input, accessibility, focus
    // Prepare layout node that proxies measure and layout passes to the View.
    val layoutNode = LayoutNode()
    layoutNode.modifier = draw { canvas, _ ->
        draw(canvas.nativeCanvas)
    }
    layoutNode.onAttach = { owner ->
        (owner as? AndroidOwner)?.addAndroidView(this, layoutNode)
    }
    layoutNode.onDetach = { owner ->
        (owner as? AndroidOwner)?.removeAndroidView(this)
    }
    layoutNode.measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks(
        "Intrinsics not supported for Android views"
    ) {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureScope.LayoutResult {
            if (constraints.minWidth != 0.ipx) {
                minimumHeight = constraints.minWidth.value
            }
            if (constraints.minHeight != 0.ipx) {
                minimumHeight = constraints.minHeight.value
            }
            // TODO(popam): look at the layout params of the child

            measure(
                obtainMeasureSpec(constraints.minWidth, constraints.maxWidth),
                obtainMeasureSpec(constraints.minHeight, constraints.maxHeight)
            )
            return measureScope.layout(measuredWidth.ipx, measuredHeight.ipx) {
                layout(
                    0,
                    0,
                    measuredWidth,
                    measuredHeight
                )
            }
        }
    }
    return layoutNode
}

private fun obtainMeasureSpec(
    min: IntPx,
    max: IntPx
): Int {
    return if (min == max) {
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.EXACTLY)
    } else if (max.isFinite()) {
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.AT_MOST)
    } else {
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    }
}
