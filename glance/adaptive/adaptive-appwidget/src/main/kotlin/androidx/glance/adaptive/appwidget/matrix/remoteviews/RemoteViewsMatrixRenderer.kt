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
import androidx.glance.adaptive.appwidget.components.graph.GraphDefaults
import androidx.glance.adaptive.appwidget.components.graph.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.icon.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.iconbutton.IconButtonDefaults
import androidx.glance.adaptive.appwidget.components.iconbutton.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.icongrid.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.ring.buildRemoteViews
import androidx.glance.adaptive.appwidget.components.setCornerRadius
import androidx.glance.adaptive.appwidget.components.setOptionalText
import androidx.glance.adaptive.appwidget.components.setSlot
import androidx.glance.adaptive.appwidget.components.setSpreadSlot
import androidx.glance.adaptive.appwidget.components.setText
import androidx.glance.adaptive.appwidget.components.text.buildSupportingRemoteViews
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.appwidget.components.visibility
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.MatrixDefaults
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixBulb
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixCard
import androidx.glance.adaptive.appwidget.matrix.resolve
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsTemplateRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.renderer.ArchetypeRenderer
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.slots.ActionComponent
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.BulbComponent
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.HeroComponent
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.slots.TrailingComponent
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
        is MatrixCardContent ->
            matrixCard(
                taps,
                resolve(content, surface, taps.context.resources.configuration.fontScale),
            )
        is MatrixBulbContent -> matrixBulb(taps, resolve(content, surface))
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

    val hasHeader =
        card.leading != null ||
            card.primary != null ||
            card.secondary != null ||
            card.trailing != null
    views.setSlot(
        R.id.glance_adaptive_appwidget_remoteviews_slot_header,
        if (hasHeader) header(taps, card) else null,
    )

    val hero = card.hero
    val actions = card.actions
    val hasActions = actions.isNotEmpty()
    views.setSlot(
        R.id.glance_adaptive_appwidget_remoteviews_slot_hero,
        hero?.toRemoteViews(taps, card.tiers, card.heroHeight),
        tappable = hero?.isTappable() == true,
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
        List(actions.size) { i -> actions[i]?.toRemoteViews(taps) },
        tappable = List(actions.size) { i -> actions[i]?.isTappable(taps) == true },
    )
    return views
}

/** The header slots, in a row or stacked. */
private fun header(taps: RemoteViewsActions, card: ResolvedMatrixCard): RemoteViews {
    val context = taps.context
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
    // In a row, the secondary text is measured before the weighted primary text, so it is capped
    // and ellipsized rather than left to squeeze the primary text out.
    // Only a row has a trailing slot; resolve() drops the trailing component of a stacked header.
    if (!card.stacked) {
        val trailing = card.trailing
        views.setSlot(
            R.id.glance_adaptive_appwidget_remoteviews_slot_trailing,
            trailing?.toRemoteViews(taps, card.trailingSize),
            tappable = trailing?.isTappable(taps) == true,
        )
    }
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
private fun matrixBulb(taps: RemoteViewsActions, bulb: ResolvedMatrixBulb): RemoteViews {
    val views =
        RemoteViews(
            taps.context.packageName,
            R.layout.glance_adaptive_appwidget_remoteviews_matrix_bulb,
        )
    views.setSlot(
        android.R.id.background,
        bulb.component.toRemoteViews(taps, bulb.containerSize),
        tappable = bulb.component.isTappable(taps),
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

private fun TrailingComponent.toRemoteViews(taps: RemoteViewsActions, size: Dp): RemoteViews =
    when (this) {
        is IconButton ->
            buildRemoteViews(
                taps,
                diameter = size,
                glyphSize = size * IconButtonDefaults.TrailingGlyphRatio,
                width = size + MatrixDefaults.TrailingExtraWidth,
            )
    }

private fun HeroComponent.toRemoteViews(
    taps: RemoteViewsActions,
    tiers: SizeTiers,
    heroHeight: Dp?,
): RemoteViews =
    when (this) {
        is Graph -> buildRemoteViews(taps.context, GraphDefaults.barSpacing(tiers))
        is IconGrid -> buildRemoteViews(taps, heroHeight)
    }

private fun ActionComponent.toRemoteViews(taps: RemoteViewsActions): RemoteViews =
    when (this) {
        is TemplateText -> buildSupportingRemoteViews(taps.context)
        is Badge -> buildRemoteViews(taps.context)
        is IconButton ->
            buildRemoteViews(
                taps,
                diameter = IconButtonDefaults.ActionSize,
                glyphSize = IconButtonDefaults.ActionGlyphSize,
            )
    }

private fun BulbComponent.toRemoteViews(taps: RemoteViewsActions, containerSize: Dp): RemoteViews =
    when (this) {
        is ContainedRing -> buildRemoteViews(taps.context, containerSize)
        is IconButton ->
            buildRemoteViews(
                taps,
                diameter = containerSize,
                glyphSize = IconButtonDefaults.bulbGlyphSize(containerSize),
            )
    }

/*
 * Whether a component runs an action when tapped, which decides the stable ID it is added under
 * (see [androidx.glance.adaptive.appwidget.components.tappableStableId]). The root [SlotComponent]
 * is open to other surface modules, so tappability is dispatched per sealed slot contract to stay
 * exhaustive.
 */

private fun TrailingComponent.isTappable(taps: RemoteViewsActions): Boolean =
    when (this) {
        is IconButton -> taps.isTappable(action)
    }

/** An icon grid is not tappable itself: it keys each of its tiles the same way. */
private fun HeroComponent.isTappable(): Boolean =
    when (this) {
        is Graph,
        is IconGrid -> false
    }

private fun ActionComponent.isTappable(taps: RemoteViewsActions): Boolean =
    when (this) {
        is IconButton -> taps.isTappable(action)
        is TemplateText,
        is Badge -> false
    }

private fun BulbComponent.isTappable(taps: RemoteViewsActions): Boolean =
    when (this) {
        is IconButton -> taps.isTappable(action)
        is ContainedRing -> false
    }
