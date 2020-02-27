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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
    // TODO(soboleva): add layout direction here?
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
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.LayoutResult {
            if (constraints.minWidth != 0.ipx) {
                minimumWidth = constraints.minWidth.value
            }
            if (constraints.minHeight != 0.ipx) {
                minimumHeight = constraints.minHeight.value
            }
            // TODO (soboleva): native view should get LD value from Compose?
            measure(
                obtainMeasureSpec(constraints.minWidth, constraints.maxWidth, layoutParams.width),
                obtainMeasureSpec(constraints.minHeight, constraints.maxHeight, layoutParams.height)
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

/**
 * Intersects [Constraints] and [View] LayoutParams to obtain the suitable [View.MeasureSpec]
 * for measuring the [View].
 */
private fun obtainMeasureSpec(
    min: IntPx,
    max: IntPx,
    preferred: Int
): Int = when {
    preferred >= 0 || min == max -> {
        // Fixed size due to fixed size layout param or fixed constraints.
        View.MeasureSpec.makeMeasureSpec(
            preferred.coerceIn(min.value, max.value),
            View.MeasureSpec.EXACTLY
        )
    }
    preferred == WRAP_CONTENT && max.isFinite() -> {
        // Wrap content layout param with finite max constraint. If max constraint is infinite,
        // we will measure the child with UNSPECIFIED.
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.AT_MOST)
    }
    preferred == MATCH_PARENT && max.isFinite() -> {
        // Match parent layout param, so we force the child to fill the available space.
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.EXACTLY)
    }
    else -> {
        // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    }
}
