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

package androidx.ui.node

import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.ui.core.AndroidOwner
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutNode
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.drawBehind
import androidx.ui.graphics.drawscope.drawCanvas
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.util.fastFirstOrNull
import androidx.ui.util.fastForEach
import androidx.ui.viewinterop.AndroidViewHolder

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ViewAdapter {
    val id: Int
    fun willInsert(view: View, parent: ViewGroup)
    fun didInsert(view: View, parent: ViewGroup)
    fun didUpdate(view: View, parent: ViewGroup)
}

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T : ViewAdapter> View.getOrAddAdapter(id: Int, factory: () -> T): T {
    return getViewAdapter().get(id, factory)
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
    preferred == ViewGroup.LayoutParams.WRAP_CONTENT && max.isFinite() -> {
        // Wrap content layout param with finite max constraint. If max constraint is infinite,
        // we will measure the child with UNSPECIFIED.
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.AT_MOST)
    }
    preferred == ViewGroup.LayoutParams.MATCH_PARENT && max.isFinite() -> {
        // Match parent layout param, so we force the child to fill the available space.
        View.MeasureSpec.makeMeasureSpec(max.value, View.MeasureSpec.EXACTLY)
    }
    else -> {
        // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    }
}

/**
 * Builds a [LayoutNode] tree representation for an Android [View].
 * The component nodes will proxy the Compose core calls to the [View].
 */
internal fun AndroidViewHolder.toLayoutNode(): LayoutNode {
    // TODO(soboleva): add layout direction here?
    // TODO(popam): forward pointer input, accessibility, focus
    // Prepare layout node that proxies measure and layout passes to the View.
    val layoutNode = LayoutNode()
    layoutNode.modifier = Modifier
        .pointerInteropModifier(this)
        .drawBehind {
            drawCanvas { canvas, _ -> draw(canvas.nativeCanvas) }
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
        ): MeasureScope.MeasureResult {
            if (constraints.minWidth != 0.ipx) {
                getChildAt(0).minimumWidth = constraints.minWidth.value
            }
            if (constraints.minHeight != 0.ipx) {
                getChildAt(0).minimumHeight = constraints.minHeight.value
            }
            // TODO (soboleva): native view should get LD value from Compose?

            // TODO(shepshapard): !! necessary?
            measure(
                obtainMeasureSpec(
                    constraints.minWidth,
                    constraints.maxWidth,
                    layoutParams!!.width
                ),
                obtainMeasureSpec(
                    constraints.minHeight,
                    constraints.maxHeight,
                    layoutParams!!.height
                )
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

internal class MergedViewAdapter : ViewAdapter {
    override val id = 0
    val adapters = mutableListOf<ViewAdapter>()

    inline fun <T : ViewAdapter> get(id: Int, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val existing = adapters.fastFirstOrNull { it.id == id } as? T
        if (existing != null) return existing
        val next = factory()
        adapters.add(next)
        return next
    }

    override fun willInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.willInsert(view, parent) }
    }

    override fun didInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didInsert(view, parent) }
    }

    override fun didUpdate(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didUpdate(view, parent) }
    }
}

/**
 * This function will take in a string and pass back a valid resource identifier for
 * View.setTag(...). We should eventually move this to a resource id that's actually generated via
 * AAPT but doing that in this project is proving to be complicated, so for now I'm just doing this
 * as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

private val viewAdaptersKey = tagKey("ViewAdapter")

internal fun View.getViewAdapterIfExists(): MergedViewAdapter? {
    return getTag(viewAdaptersKey) as? MergedViewAdapter
}

internal fun View.getViewAdapter(): MergedViewAdapter {
    var adapter = getTag(viewAdaptersKey) as? MergedViewAdapter
    if (adapter == null) {
        adapter = MergedViewAdapter()
        setTag(viewAdaptersKey, adapter)
    }
    return adapter
}