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

package androidx.glance.adaptive.appwidget.components.graph

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.addChild
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.setHorizontalItemSpacing
import androidx.glance.adaptive.appwidget.components.setMaxSize
import androidx.glance.adaptive.appwidget.components.setOptionalText
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.appwidget.components.visibility
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.Graph

/**
 * Draws the graph for the hero slot — the [RemoteViews] counterpart of the Glance `BarChartBlock`.
 *
 * Every column stretches across the full height of the bars row. A bar is sized as a weighted
 * fraction of the vertical space above its label, so the graph always fills the actual on-screen
 * height of the hero slot; a dotted graph draws one dot per value at the bottom of its column
 * instead. Unlike Glance, [RemoteViews] has no cap on the number of children a container holds, so
 * the columns are added to a single row, one per value of the graph, however many there are.
 *
 * @param context Context the views are built for.
 * @param barSpacing Horizontal gap between adjacent columns.
 * @return The graph's views.
 */
internal fun Graph.buildRemoteViews(context: Context, barSpacing: Dp): RemoteViews {
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_bar_chart)

    views.removeAllViews(R.id.glance_adaptive_appwidget_remoteviews_bars)
    for (index in values.indices) {
        views.addChild(
            R.id.glance_adaptive_appwidget_remoteviews_bars,
            stableId = index,
            child = column(context, index, isLast = index == values.lastIndex, barSpacing),
        )
    }

    views.setViewVisibility(
        R.id.glance_adaptive_appwidget_remoteviews_chart_description,
        visibility(hasDescription),
    )
    if (hasDescription) {
        val iconPx = GraphDefaults.CaptionIconSize.toPx(context.density)
        views.bindCaption(
            textId = R.id.glance_adaptive_appwidget_remoteviews_chart_caption_start,
            iconId = R.id.glance_adaptive_appwidget_remoteviews_chart_caption_start_icon,
            label = startLabel,
            iconSizePx = iconPx,
        )
        views.bindCaption(
            textId = R.id.glance_adaptive_appwidget_remoteviews_chart_caption_end,
            iconId = R.id.glance_adaptive_appwidget_remoteviews_chart_caption_end_icon,
            label = endLabel,
            iconSizePx = iconPx,
        )
    }
    return views
}

private fun RemoteViews.bindCaption(
    @IdRes textId: Int,
    @IdRes iconId: Int,
    label: ChartLabel?,
    iconSizePx: Int,
) {
    setOptionalText(textId, label?.text, GraphDefaults.CaptionTextSize.value)
    val iconRes = label?.icon
    if (iconRes == null) {
        setViewVisibility(iconId, View.GONE)
    } else {
        setViewVisibility(iconId, View.VISIBLE)
        setImageViewResource(iconId, iconRes)
        setMaxSize(iconId, iconSizePx)
    }
}

/**
 * One graph column: the bar, or the dot of a dotted graph, and its axis label.
 *
 * A bar has `layout_height="0dp"` and `layout_weight="1"` inside a bottom-aligned vertical
 * `LinearLayout` whose `weightSum` is set to `1 / fillFraction`, so the bar takes `fillFraction` of
 * the height above the label and the tallest bar always reaches the top of the hero slot. A dot
 * keeps its own size.
 */
private fun Graph.column(
    context: Context,
    index: Int,
    isLast: Boolean,
    barSpacing: Dp,
): RemoteViews {
    val layout =
        when {
            dotted && isDotFilledAt(index) ->
                R.layout.glance_adaptive_appwidget_remoteviews_dot_column_emphasized
            dotted -> R.layout.glance_adaptive_appwidget_remoteviews_dot_column_muted
            isEmphasizedAt(index) ->
                R.layout.glance_adaptive_appwidget_remoteviews_bar_column_emphasized
            else -> R.layout.glance_adaptive_appwidget_remoteviews_bar_column_muted
        }
    val views = RemoteViews(context.packageName, layout)

    views.setHorizontalItemSpacing(
        R.id.glance_adaptive_appwidget_remoteviews_bar_column,
        gap = barSpacing,
        isLast = isLast,
        density = context.density,
    )
    if (!dotted) {
        val fillFraction = fractionAt(index).coerceIn(GraphDefaults.MinBarFraction, 1f)
        views.setFloat(
            R.id.glance_adaptive_appwidget_remoteviews_bar_column,
            "setWeightSum",
            1f / fillFraction,
        )
    }
    views.setOptionalText(
        R.id.glance_adaptive_appwidget_remoteviews_bar_label,
        labelAt(index),
        GraphDefaults.LabelTextSize.value,
    )
    return views
}
