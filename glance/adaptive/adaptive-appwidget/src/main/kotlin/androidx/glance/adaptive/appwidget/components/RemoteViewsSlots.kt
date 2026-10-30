/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.components

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import kotlin.math.roundToInt

/*
 * Building blocks shared by the RemoteViews slot components and the Information Matrix layouts that
 * host them.
 */

/** Display density [Context] resolves DP against. */
internal val Context.density: Float
    get() = resources.displayMetrics.density

/**
 * Converts DP to pixels, rounding to the nearest pixel while keeping any non-zero value at least
 * one pixel.
 */
internal fun Float.dpToPx(density: Float): Int {
    val px = (this * density).roundToInt()
    return if (this > 0f) px.coerceAtLeast(1) else 0
}

/** Converts this [Dp] to pixels against [density]. */
internal fun Dp.toPx(density: Float): Int = value.dpToPx(density)

/** [View.VISIBLE] if [visible], otherwise [View.GONE]. */
internal fun visibility(visible: Boolean): Int = if (visible) View.VISIBLE else View.GONE

/** This ARGB color with its alpha replaced by [alpha], in `[0.0, 1.0]`. */
internal fun Int.withAlpha(alpha: Float): Int =
    Color.argb((alpha * 255f).roundToInt(), Color.red(this), Color.green(this), Color.blue(this))

/**
 * Sets a text size in SP.
 *
 * Set at runtime rather than in the layouts because a layout's text size is rounded to whole
 * pixels, while the Glance renderer's is not.
 */
internal fun RemoteViews.setTextSizeSp(@IdRes viewId: Int, sizeSp: Float) {
    setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

/** Shows [text] at [sizeSp] in [viewId]. */
internal fun RemoteViews.setText(@IdRes viewId: Int, text: CharSequence, sizeSp: Float) {
    setTextViewText(viewId, text)
    setTextSizeSp(viewId, sizeSp)
}

/** Shows [text] at [sizeSp] in [viewId], or hides the view when [text] is `null`. */
internal fun RemoteViews.setOptionalText(@IdRes viewId: Int, text: CharSequence?, sizeSp: Float) {
    setViewVisibility(viewId, visibility(text != null))
    if (text != null) setText(viewId, text, sizeSp)
}

/**
 * Caps both edges of [viewId] at [px].
 *
 * Views cannot be given a size before API 31. A view that wraps content larger than the cap, with
 * `adjustViewBounds` for images, is sized by the cap instead.
 */
internal fun RemoteViews.setMaxSize(@IdRes viewId: Int, px: Int) {
    setInt(viewId, "setMaxWidth", px)
    setInt(viewId, "setMaxHeight", px)
}

/**
 * Adds [child] to [containerId] under [stableId], a key unique among the container's children.
 *
 * The host reapplies an update onto the previous hierarchy when the layout is unchanged. From API
 * 31 a child added with a stable ID survives a preceding `removeAllViews`, and the host reapplies
 * [child] onto it when its layout is unchanged instead of inflating it again, which is what keeps a
 * resize from flickering. Earlier, the child is inflated again on every update.
 */
internal fun RemoteViews.addChild(@IdRes containerId: Int, child: RemoteViews, stableId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViewsSlotsApi31Impl.addStableView(this, containerId, child, stableId)
    } else {
        addView(containerId, child)
    }
}

/**
 * Fills the slot container [slotId] with [component], or hides the slot when it is `null`.
 *
 * The slot is emptied first and its visibility is set on every call, because the host reapplies an
 * update onto the previous hierarchy. See [addChild] for how the component is still recycled, and
 * [tappableStableId] for what [tappable] changes.
 */
internal fun RemoteViews.setSlot(
    @IdRes slotId: Int,
    component: RemoteViews?,
    tappable: Boolean = false,
) {
    removeAllViews(slotId)
    setViewVisibility(slotId, visibility(component != null))
    if (component != null) addChild(slotId, component, tappableStableId(0, tappable))
}

/**
 * Fills the horizontal slot container [slotId] with one entry per position in [components],
 * separated by [weightedSpacer]s so that they spread to the container's edges.
 *
 * A `null` entry leaves its position empty but keeps the spacer before it, so the components after
 * it keep their place. The slot is hidden when [components] is empty, and emptied first for the
 * same reason as [setSlot]. Stable IDs follow the position, so an update recycles each position's
 * component, unless it became tappable or stopped being so (see [tappableStableId]).
 *
 * @param tappable Whether the component at each position runs an action when tapped; positions past
 *   its end are not.
 */
internal fun RemoteViews.setSpreadSlot(
    context: Context,
    @IdRes slotId: Int,
    components: List<RemoteViews?>,
    tappable: List<Boolean> = emptyList(),
) {
    removeAllViews(slotId)
    setViewVisibility(slotId, visibility(components.isNotEmpty()))
    for (index in components.indices) {
        if (index > 0) addChild(slotId, weightedSpacer(context), stableId = 2 * index - 1)
        components[index]?.let {
            addChild(slotId, it, tappableStableId(2 * index, tappable.getOrElse(index) { false }))
        }
    }
}

/**
 * The stable ID to add a child under at [stableId], kept apart for a [tappable] child.
 *
 * A view that ignores taps is never given a click, since any click makes it clickable. So a
 * tappable child and an inert one at the same position must not be recycled into each other: the
 * inert one would keep the earlier tap. [addChild] recycles a child only under the same stable ID.
 */
internal fun tappableStableId(stableId: Int, tappable: Boolean): Int =
    if (tappable) stableId or TAPPABLE_STABLE_ID_BIT else stableId

/** Bit [tappableStableId] sets, above any position a slot holds. */
private const val TAPPABLE_STABLE_ID_BIT = 1 shl 30

/** An empty view that takes an equal share of a horizontal container's slack. */
internal fun weightedSpacer(context: Context): RemoteViews =
    RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_weighted_spacer)

/**
 * Rounds the outline of [viewId] to [radius], from API 31. Below API 31 the radius cannot be set at
 * runtime, and the card keeps the radius of its layout.
 */
internal fun RemoteViews.setCornerRadius(@IdRes viewId: Int, radius: Dp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViewsSlotsApi31Impl.setOutlineRadius(this, viewId, radius.value)
    }
}

/**
 * Spaces a weighted horizontal item [viewId] by [gap] from the next item while keeping every item's
 * inner content width equal.
 *
 * From API 31, sets `marginEnd` (`0dp` on [isLast]), which `LinearLayout` subtracts before dividing
 * the remaining width equally across the items, leaving the outer items flush with the edges. Below
 * API 31 margins are not remotable, so each item takes symmetric `gap / 2` horizontal padding
 * instead, keeping all items equal in width.
 */
internal fun RemoteViews.setHorizontalItemSpacing(
    @IdRes viewId: Int,
    gap: Dp,
    isLast: Boolean,
    density: Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViewsSlotsApi31Impl.setMarginEnd(this, viewId, if (isLast) 0f else gap.value)
    } else {
        val half = (gap.value / 2f).dpToPx(density)
        setViewPadding(viewId, half, 0, half, 0)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object RemoteViewsSlotsApi31Impl {
    fun addStableView(rv: RemoteViews, containerId: Int, child: RemoteViews, stableId: Int) {
        rv.addStableView(containerId, child, stableId)
    }

    fun setOutlineRadius(rv: RemoteViews, viewId: Int, radiusDp: Float) {
        rv.setViewOutlinePreferredRadius(viewId, radiusDp, TypedValue.COMPLEX_UNIT_DIP)
    }

    fun setMarginEnd(rv: RemoteViews, viewId: Int, marginDp: Float) {
        rv.setViewLayoutMargin(
            viewId,
            RemoteViews.MARGIN_END,
            marginDp,
            TypedValue.COMPLEX_UNIT_DIP,
        )
    }
}
