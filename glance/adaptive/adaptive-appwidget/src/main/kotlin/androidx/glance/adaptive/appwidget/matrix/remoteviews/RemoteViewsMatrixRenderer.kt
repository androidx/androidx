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

package androidx.glance.adaptive.appwidget.matrix.remoteviews

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.RemoteViewsActions
import androidx.glance.adaptive.appwidget.components.badge.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.graph.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.icon.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.ring.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.setCornerRadius
import androidx.glance.adaptive.appwidget.components.setOptionalText
import androidx.glance.adaptive.appwidget.components.setSlot
import androidx.glance.adaptive.appwidget.components.setSpreadSlot
import androidx.glance.adaptive.appwidget.components.setText
import androidx.glance.adaptive.appwidget.components.text.buildSupportingRemoteViews
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.appwidget.components.visibility
import androidx.glance.adaptive.appwidget.matrix.ActionComponent
import androidx.glance.adaptive.appwidget.matrix.Badge
import androidx.glance.adaptive.appwidget.matrix.BulbComponent
import androidx.glance.adaptive.appwidget.matrix.ContainedRing
import androidx.glance.adaptive.appwidget.matrix.Graph
import androidx.glance.adaptive.appwidget.matrix.HeroComponent
import androidx.glance.adaptive.appwidget.matrix.Icon
import androidx.glance.adaptive.appwidget.matrix.IconButton
import androidx.glance.adaptive.appwidget.matrix.IconGrid
import androidx.glance.adaptive.appwidget.matrix.LeadingComponent
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.MatrixDefaults
import androidx.glance.adaptive.appwidget.matrix.PrimaryComponent
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixBulb
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixCard
import androidx.glance.adaptive.appwidget.matrix.Ring
import androidx.glance.adaptive.appwidget.matrix.SecondaryComponent
import androidx.glance.adaptive.appwidget.matrix.SlotComponent
import androidx.glance.adaptive.appwidget.matrix.TemplateText
import androidx.glance.adaptive.appwidget.matrix.TrailingComponent
import androidx.glance.adaptive.appwidget.matrix.resolve
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsTemplateRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.renderer.ArchetypeRenderer
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate

/**
 * Direct [RemoteViews] renderer for the Information Matrix, shared by every template.
 *
 * A template's selector maps its data onto a [MatrixContent]; this renderer lays it out for the
 * container (see [resolve]) and draws it, so a new template needs no [RemoteViews] code at all. The
 * template payload itself is not read: everything to draw is in the matrix.
 *
 * Taps run the [GlanceWidgetAction]s the matrix carries. A [GlanceWidgetAction.Callback] hands its
 * callback the name and instance ID of the widget it was tapped on, which a renderer is not given,
 * so this renderer draws a callback as inert; register the renderer of [forWidget] to deliver them
 * with the widget's name (`widgetId` remains `null` until the renderer is given the target
 * instance).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteViewsMatrixRenderer :
    RemoteViewsTemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, MatrixContent> {

    override fun renderRemoteViews(
        context: Context,
        template: GlanceAdaptiveTemplate,
        archetype: MatrixContent,
        surface: AppWidgetSurface,
    ): RemoteViews =
        renderMatrix(archetype, surface, RemoteViewsActions(context, widgetName = null))

    /**
     * A renderer that draws as this one does, and delivers [GlanceWidgetAction.Callback] taps to
     * [widgetName].
     *
     * @param widgetName Developer widget identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName], which the
     *   renderer is registered for.
     * @return The renderer to register for [widgetName].
     */
    // TODO(b/580007927): Drop once a renderer is given the widget name and widgetId it renders
    //  for, so the default registration can deliver callbacks with their instance ID too.
    public fun forWidget(
        widgetName: String
    ): ArchetypeRenderer<
        GlanceAdaptiveTemplate,
        AppWidgetSurface,
        MatrixContent,
        AppWidgetRenderOutput,
    > = RemoteViewsTemplateRenderer { context, _, archetype, surface ->
        renderMatrix(archetype, surface, RemoteViewsActions(context, widgetName))
    }
}

/** Lays [content] out for [surface] and draws it, running taps through [taps]. */
private fun renderMatrix(
    content: MatrixContent,
    surface: AppWidgetSurface,
    taps: RemoteViewsActions,
): RemoteViews =
    when (content) {
        is MatrixCardContent -> matrixCard(taps, resolve(content, surface))
        is MatrixBulbContent -> matrixBulb(taps.context, resolve(content, surface))
    }

/**
 * The main card: header, hero and action slots, top to bottom.
 *
 * The slots are distributed vertically by what is present:
 * - **Hero present**: it takes the layout weight and absorbs all the slack, so it grows with the
 *   container while the header and the action row keep their intrinsic heights.
 * - **Hero absent, actions present**: a weighted spacer takes the slack instead, pinning the action
 *   row to the bottom edge and the header to the top.
 * - **Header only**: the card centers it.
 *
 * A header with every slot empty is hidden, so a hero below it takes its room, as a card that is a
 * single button needs.
 *
 * The card itself is never clickable: taps belong to its interactive components, which run them
 * through [taps].
 */
private fun matrixCard(taps: RemoteViewsActions, card: ResolvedMatrixCard): RemoteViews {
    val context = taps.context
    val density = context.density
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_matrix_card)

    val padding = card.padding.toPx(density)
    views.setViewPadding(android.R.id.background, padding, padding, padding, padding)
    card.cornerRadius?.let { views.setCornerRadius(android.R.id.background, it) }

    val hasHeader = card.leading != null || card.primary != null || card.secondary != null
    views.setSlot(
        R.id.glance_adaptive_appwidget_remoteviews_slot_header,
        if (hasHeader) header(context, card) else null,
    )

    val hero = card.hero
    val actions = card.actions
    val hasActions = actions.isNotEmpty()
    views.setSlot(
        R.id.glance_adaptive_appwidget_remoteviews_slot_hero,
        hero?.toRemoteViews(context, card.tiers),
    )
    if (hero != null) {
        // The gaps either side of the hero are its padding, so it still absorbs all the slack.
        val spacing = card.slotSpacing.toPx(density)
        views.setViewPadding(
            R.id.glance_adaptive_appwidget_remoteviews_slot_hero,
            0,
            if (hasHeader) spacing else 0,
            0,
            if (hasActions) spacing else 0,
        )
    }

    views.setViewVisibility(
        R.id.glance_adaptive_appwidget_remoteviews_slot_supporting_spacer,
        visibility(hasActions && hero == null),
    )
    // A single position centers; more spread to the edges, which is how the design distributes
    // them.
    val horizontalGravity = if (actions.size == 1) Gravity.CENTER_HORIZONTAL else Gravity.START
    views.setInt(
        R.id.glance_adaptive_appwidget_remoteviews_slot_supporting,
        "setGravity",
        horizontalGravity or Gravity.CENTER_VERTICAL,
    )
    views.setSpreadSlot(
        context,
        R.id.glance_adaptive_appwidget_remoteviews_slot_supporting,
        List(actions.size) { i -> actions[i]?.toRemoteViews(context) },
    )
    return views
}

/** The header slots, in a row or stacked. */
private fun header(context: Context, card: ResolvedMatrixCard): RemoteViews {
    val layout =
        when {
            card.stacked -> R.layout.glance_adaptive_appwidget_remoteviews_matrix_header_stacked
            card.compactHeader ->
                R.layout.glance_adaptive_appwidget_remoteviews_matrix_header_row_compact
            else -> R.layout.glance_adaptive_appwidget_remoteviews_matrix_header_row
        }
    val views = RemoteViews(context.packageName, layout)

    views.setSlot(
        R.id.glance_adaptive_appwidget_remoteviews_slot_leading,
        card.leading?.toRemoteViews(context, card.leadingSize),
    )
    val primary = card.primary
    // An empty primary slot keeps its place, so the secondary text does not move.
    views.setViewVisibility(
        R.id.glance_adaptive_appwidget_remoteviews_primary,
        if (primary != null) View.VISIBLE else View.INVISIBLE,
    )
    primary?.bind(views, card.primaryTextSize.value)
    views.setOptionalText(
        R.id.glance_adaptive_appwidget_remoteviews_secondary,
        card.secondary?.text(),
        MatrixDefaults.SecondaryTextSize.value,
    )
    card.trailing?.toRemoteViews()
    // In a row, the secondary text is measured before the weighted primary text, so it is capped
    // and ellipsized rather than left to squeeze the primary text out.
    card.secondaryMaxWidth?.let {
        views.setInt(
            R.id.glance_adaptive_appwidget_remoteviews_secondary,
            "setMaxWidth",
            it.toPx(context.density),
        )
    }
    return views
}

/** The bulb: its component, centered, with no card. */
private fun matrixBulb(context: Context, bulb: ResolvedMatrixBulb): RemoteViews {
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_matrix_bulb)
    views.setSlot(
        android.R.id.background,
        bulb.component.toRemoteViews(context, bulb.containerSize),
    )
    return views
}

/*
 * One dispatch per slot contract. The contracts are sealed, so adding a component to one fails to
 * compile here until the component can be drawn as RemoteViews.
 */

private fun LeadingComponent.toRemoteViews(context: Context, size: Dp): RemoteViews =
    when (this) {
        is Ring -> buildRemoteViews(context, size)
        is Icon -> buildRemoteViews(context, size)
    }

/** Shows this component in the header's primary text view, at [textSizeSp]. */
private fun PrimaryComponent.bind(views: RemoteViews, textSizeSp: Float) {
    when (this) {
        is TemplateText ->
            views.setText(R.id.glance_adaptive_appwidget_remoteviews_primary, value, textSizeSp)
    }
}

/** Text this component shows in the header's secondary text view. */
private fun SecondaryComponent.text(): CharSequence =
    when (this) {
        is TemplateText -> value
    }

private fun TrailingComponent.toRemoteViews(): RemoteViews =
    when (this) {
        is IconButton -> notDrawnYet()
    }

private fun HeroComponent.toRemoteViews(context: Context, tiers: SizeTiers): RemoteViews =
    when (this) {
        is Graph -> buildRemoteViews(context, Graph.barSpacing(tiers))
        is IconGrid -> notDrawnYet()
    }

private fun ActionComponent.toRemoteViews(context: Context): RemoteViews =
    when (this) {
        is TemplateText -> buildSupportingRemoteViews(context)
        is Badge -> buildRemoteViews(context)
        is IconButton -> notDrawnYet()
    }

private fun BulbComponent.toRemoteViews(context: Context, containerSize: Dp): RemoteViews =
    when (this) {
        is ContainedRing -> buildRemoteViews(context, containerSize)
        is IconButton -> notDrawnYet()
    }

// TODO(b/580007927): Draw the button and icon grid components. Until then no selector places
//  them.
private fun SlotComponent.notDrawnYet(): Nothing =
    throw UnsupportedOperationException(
        "${javaClass.simpleName} is not drawn with RemoteViews yet."
    )
