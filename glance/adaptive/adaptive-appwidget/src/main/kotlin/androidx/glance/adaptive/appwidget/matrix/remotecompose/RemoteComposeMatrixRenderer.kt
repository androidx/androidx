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

package androidx.glance.adaptive.appwidget.matrix.remotecompose

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.components.RemoteComposeActions
import androidx.glance.adaptive.appwidget.components.RemoteComposeFonts
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.SlotText
import androidx.glance.adaptive.appwidget.components.badge.Content
import androidx.glance.adaptive.appwidget.components.graph.Content
import androidx.glance.adaptive.appwidget.components.graph.GraphDefaults
import androidx.glance.adaptive.appwidget.components.icon.Content
import androidx.glance.adaptive.appwidget.components.iconbutton.Content
import androidx.glance.adaptive.appwidget.components.iconbutton.IconButtonDefaults
import androidx.glance.adaptive.appwidget.components.ring.Content
import androidx.glance.adaptive.appwidget.components.text.SupportingContent
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.MatrixDefaults
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixBulb
import androidx.glance.adaptive.appwidget.matrix.ResolvedMatrixCard
import androidx.glance.adaptive.appwidget.matrix.resolve
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteComposeTemplateRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
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

/** Minimum height of the bottom action row, free to grow with the font scale. */
private const val ACTION_ROW_MIN_HEIGHT_DP = 24f

/**
 * Fallback widget corner radius in DP when `android.R.dimen.system_app_widget_background_radius`
 * cannot be resolved.
 */
private const val DEFAULT_WIDGET_CORNER_RADIUS_DP = 28f

/**
 * Direct RemoteCompose renderer for the Information Matrix, shared by every template.
 *
 * A template's selector maps its data onto a [MatrixContent]; this renderer lays it out for the
 * container (see [resolve]) and emits `@RemoteComposable` content using the public
 * `remote-creation-compose` API for Android 17+ (`API 37`), so a new template needs no
 * RemoteCompose layout code of its own. The template payload itself is not read: everything to draw
 * is in the matrix.
 *
 * Taps run the [GlanceAdaptiveAction]s the matrix carries. A [GlanceAdaptiveAction.CallbackAction]
 * hands its callback the name and instance ID of the widget it was tapped on, which a renderer is
 * not given, so this renderer draws a callback as inert; register the renderer of [forWidget] to
 * deliver them with the widget's name (`widgetId` remains `null` until the renderer is given the
 * target instance).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteComposeMatrixRenderer :
    RemoteComposeTemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, MatrixContent> {

    override fun renderRemoteCompose(
        template: GlanceAdaptiveTemplate,
        archetype: MatrixContent,
        surface: AppWidgetSurface,
    ): @RemoteComposable @Composable () -> Unit =
        renderMatrix(archetype, surface, RemoteComposeActions(widgetName = null))

    /**
     * A renderer that draws as this one does, and delivers [GlanceAdaptiveAction.CallbackAction]
     * taps to [widgetName].
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
    > = RemoteComposeTemplateRenderer { _, archetype, surface ->
        renderMatrix(archetype, surface, RemoteComposeActions(widgetName))
    }
}

/** Lays [content] out for [surface] and draws it, running taps through [taps]. */
private fun renderMatrix(
    content: MatrixContent,
    surface: AppWidgetSurface,
    taps: RemoteComposeActions,
): @RemoteComposable @Composable () -> Unit {
    when (content) {
        is MatrixCardContent -> {
            return {
                val fontScale = LocalResources.current.configuration.fontScale
                MatrixCard(taps, resolve(content, surface, fontScale))
            }
        }
        is MatrixBulbContent -> {
            val bulb = resolve(content, surface)
            return { MatrixBulb(taps, bulb) }
        }
    }
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
 */
@RemoteComposable
@Composable
private fun MatrixCard(taps: RemoteComposeActions, card: ResolvedMatrixCard) {
    val cornerRadius = resolveCardCornerRadius(card.cornerRadius)
    val hasHeader =
        card.leading != null ||
            card.primary != null ||
            card.secondary != null ||
            card.trailing != null

    RemoteColumn(
        modifier =
            RemoteModifier.fillMaxSize()
                .clip(RemoteRoundedCornerShape(cornerRadius.asRdp()))
                .background(RemoteComposePalette.widgetBackground)
                .padding(card.padding.asRdp()),
        verticalArrangement = RemoteArrangement.Center,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        if (hasHeader) {
            Header(taps, card)
        }

        val hero = card.hero
        val hasActions = card.actions.isNotEmpty()
        if (hero != null) {
            // The gaps either side of the hero are its padding, so it still absorbs all the slack.
            RemoteBox(
                modifier =
                    RemoteModifier.weight(1f.rf)
                        .fillMaxWidth()
                        .padding(
                            top = if (hasHeader) card.slotSpacing.asRdp() else 0.rdp,
                            bottom = if (hasActions) card.slotSpacing.asRdp() else 0.rdp,
                        ),
                contentAlignment = RemoteAlignment.Center,
            ) {
                hero.Content(card.tiers)
            }
        } else if (hasActions) {
            RemoteBox(modifier = RemoteModifier.weight(1f.rf).fillMaxWidth())
        }

        if (hasActions) {
            ActionRow(taps, card.actions)
        }
    }
}

/**
 * The header slots: stacked vertically at W1, laid out as a compact label when [card] sizes its own
 * leading component, or arranged in a headline row.
 */
@RemoteComposable
@Composable
private fun Header(taps: RemoteComposeActions, card: ResolvedMatrixCard) {
    when {
        card.stacked -> StackedHeader(card)
        card.compactHeader -> CompactRowHeader(taps, card)
        else -> RowHeader(taps, card)
    }
}

@RemoteComposable
@Composable
private fun StackedHeader(card: ResolvedMatrixCard) {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxWidth(),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        card.leading?.let { leading ->
            leading.Content(card.leadingSize)
            if (card.primary != null || card.secondary != null) {
                RemoteBox(modifier = RemoteModifier.height(MatrixDefaults.HeaderSpacing.asRdp()))
            }
        }
        val primary = card.primary
        if (primary != null) {
            primary.PrimaryContent(
                fontSize = card.primaryTextSize,
                fontWeight = RemoteComposeFonts.Bold,
                color = RemoteComposePalette.primary,
            )
        } else if (card.secondary != null) {
            // Keep the primary slot's vertical place when absent so the secondary does not jump
            // into the headline position.
            RemoteBox(modifier = RemoteModifier.height(card.primaryTextSize.value.rdp))
        }
        card.secondary?.let { secondary ->
            RemoteBox(modifier = RemoteModifier.height(MatrixDefaults.HeaderSpacing.asRdp()))
            secondary.SecondaryContent(
                maxWidth = null,
                color = RemoteComposePalette.primary,
            )
        }
    }
}

@RemoteComposable
@Composable
private fun CompactRowHeader(taps: RemoteComposeActions, card: ResolvedMatrixCard) {
    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth(),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        card.leading?.let { leading ->
            leading.Content(card.leadingSize)
            RemoteBox(modifier = RemoteModifier.width(MatrixDefaults.HeaderSpacing.asRdp()))
        }
        RemoteColumn(
            modifier = RemoteModifier.weight(1f.rf),
            horizontalAlignment = RemoteAlignment.Start,
        ) {
            val primary = card.primary
            if (primary != null) {
                primary.PrimaryContent(
                    fontSize = card.primaryTextSize,
                    fontWeight = RemoteComposeFonts.SemiBold,
                    color = RemoteComposePalette.onPrimaryContainer,
                )
            } else if (card.secondary != null) {
                RemoteBox(modifier = RemoteModifier.height(card.primaryTextSize.value.rdp))
            }
            card.secondary?.let { secondary ->
                secondary.SecondaryContent(
                    maxWidth = card.secondaryMaxWidth,
                    color = RemoteComposePalette.onSecondaryContainer,
                )
            }
        }
        card.trailing?.let { trailing ->
            RemoteBox(modifier = RemoteModifier.width(MatrixDefaults.HeaderSpacing.asRdp()))
            trailing.Content(taps, card.trailingSize)
        }
    }
}

@RemoteComposable
@Composable
private fun RowHeader(taps: RemoteComposeActions, card: ResolvedMatrixCard) {
    RemoteRow(
        modifier =
            RemoteModifier.fillMaxWidth().heightIn(min = MatrixDefaults.HeaderRowMinHeight.asRdp()),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        card.leading?.let { leading ->
            leading.Content(card.leadingSize)
            RemoteBox(modifier = RemoteModifier.width(MatrixDefaults.HeaderSpacing.asRdp()))
        }
        RemoteBox(
            modifier = RemoteModifier.weight(1f.rf),
            contentAlignment = RemoteAlignment.CenterStart,
        ) {
            card.primary?.PrimaryContent(
                fontSize = card.primaryTextSize,
                fontWeight = RemoteComposeFonts.Bold,
                color = RemoteComposePalette.primary,
            )
        }
        card.secondary?.let { secondary ->
            RemoteBox(modifier = RemoteModifier.width(MatrixDefaults.HeaderSpacing.asRdp()))
            secondary.SecondaryContent(
                maxWidth = card.secondaryMaxWidth,
                color = RemoteComposePalette.primary,
            )
        }
        card.trailing?.let { trailing ->
            RemoteBox(modifier = RemoteModifier.width(MatrixDefaults.HeaderSpacing.asRdp()))
            trailing.Content(taps, card.trailingSize)
        }
    }
}

/**
 * Supporting positions along the bottom of the card: a single position centers, while two or more
 * spread to the edges separated by weighted spacers so absent positions keep their place.
 */
@RemoteComposable
@Composable
private fun ActionRow(taps: RemoteComposeActions, actions: List<ActionComponent?>) {
    if (actions.size == 1) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().heightIn(min = ACTION_ROW_MIN_HEIGHT_DP.rdp),
            horizontalArrangement = RemoteArrangement.Center,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            actions[0]?.Content(taps)
        }
        return
    }

    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth().heightIn(min = ACTION_ROW_MIN_HEIGHT_DP.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        for (index in actions.indices) {
            if (index > 0) {
                RemoteBox(modifier = RemoteModifier.weight(1f.rf))
            }
            actions[index]?.Content(taps)
        }
    }
}

/** The bulb: its component, centered, with no card. */
@RemoteComposable
@Composable
private fun MatrixBulb(taps: RemoteComposeActions, bulb: ResolvedMatrixBulb) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center,
    ) {
        bulb.component.Content(taps, bulb.containerSize)
    }
}

/**
 * Resolves the card's corner radius: explicit [overrideRadius] when specified, otherwise the
 * platform's `@android:dimen/system_app_widget_background_radius` (or `28.dp` fallback).
 */
@SuppressLint("InlinedApi")
@Composable
private fun resolveCardCornerRadius(overrideRadius: Dp?): Dp {
    if (overrideRadius != null) return overrideRadius
    val resources = LocalResources.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val px = resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        if (px > 0f) (px / resources.displayMetrics.density).dp
        else DEFAULT_WIDGET_CORNER_RADIUS_DP.dp
    } else {
        DEFAULT_WIDGET_CORNER_RADIUS_DP.dp
    }
}

/*
 * One dispatch per slot contract. The contracts are sealed, so adding a component to one fails to
 * compile here until the component can be drawn with RemoteCompose.
 */

@RemoteComposable
@Composable
private fun LeadingComponent.Content(size: Dp) {
    when (this) {
        is Ring -> Content(size)
        is Icon -> Content(size)
    }
}

@RemoteComposable
@Composable
private fun PrimaryComponent.PrimaryContent(
    fontSize: TextUnit,
    fontWeight: RemoteFontWeight,
    color: RemoteColor,
) {
    when (this) {
        is TemplateText ->
            SlotText(
                text = value,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
            )
    }
}

@RemoteComposable
@Composable
private fun SecondaryComponent.SecondaryContent(
    maxWidth: Dp?,
    color: RemoteColor,
) {
    when (this) {
        is TemplateText -> {
            val modifier =
                if (maxWidth != null) {
                    RemoteModifier.widthIn(max = maxWidth.asRdp())
                } else {
                    RemoteModifier
                }
            SlotText(
                text = value,
                fontSize = MatrixDefaults.SecondaryTextSize,
                fontWeight = RemoteComposeFonts.Medium,
                color = color,
                modifier = modifier,
            )
        }
    }
}

@RemoteComposable
@Composable
private fun TrailingComponent.Content(taps: RemoteComposeActions, size: Dp) {
    when (this) {
        is IconButton ->
            Content(
                taps = taps,
                diameter = size,
                glyphSize = size * IconButtonDefaults.TrailingGlyphRatio,
                width = size + MatrixDefaults.TrailingExtraWidth,
            )
    }
}

@RemoteComposable
@Composable
private fun HeroComponent.Content(tiers: SizeTiers) {
    when (this) {
        is Graph -> Content(GraphDefaults.barSpacing(tiers))
        is IconGrid -> {
            // Drawn in DashboardTemplate follow-up (b/580007927).
        }
    }
}

@RemoteComposable
@Composable
private fun ActionComponent.Content(taps: RemoteComposeActions) {
    when (this) {
        is TemplateText -> SupportingContent()
        is Badge -> Content()
        is IconButton ->
            Content(
                taps = taps,
                diameter = IconButtonDefaults.ActionSize,
                glyphSize = IconButtonDefaults.ActionGlyphSize,
            )
    }
}

@RemoteComposable
@Composable
private fun BulbComponent.Content(taps: RemoteComposeActions, containerSize: Dp) {
    when (this) {
        is ContainedRing -> Content(containerSize)
        is IconButton ->
            Content(
                taps = taps,
                diameter = containerSize,
                glyphSize = IconButtonDefaults.bulbGlyphSize(containerSize),
            )
    }
}
