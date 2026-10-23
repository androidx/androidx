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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.CustomComposablePlugin
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.components.visible
import androidx.compose.remote.player.compose.embedded.draw.executeOperations
import androidx.compose.remote.player.compose.embedded.inspectable.rcModifierInspector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastForEach

@Composable
@Suppress("ModifierFactoryExtensionFunction")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ComponentModifiers.toModifier(
    drawOpsList: List<Operation>? = null,
    ignoreVisibility: Boolean = false,
): Modifier = toModifier(drawOpsList, ignoreVisibility, ignoreClicks = false)

/**
 * Variant of [toModifier] that can skip click operations ([ignoreClicks]), used when a custom
 * component plugin dispatches the clicks itself (see [CustomComposablePlugin.handlesClick]).
 */
@Composable
@Suppress("ModifierFactoryExtensionFunction")
internal fun ComponentModifiers.toModifier(
    drawOpsList: List<Operation>?,
    ignoreVisibility: Boolean,
    ignoreClicks: Boolean,
): Modifier {
    // Only components with draw operations can DRAW_CONTENT through a layer.
    val contentLayer = if (drawOpsList != null) rememberGraphicsLayer() else null
    var modifier: Modifier = Modifier
    // Track whether a DrawContentOperation has already been attached to the modifier chain, so
    // that subsequent clip operations are hoisted before drawWithContent without bypassing
    // preceding padding.
    var drawContentProcessed = false
    var clickProcessed = ignoreClicks
    var touchProcessed = false
    // Collect the gesture operations in one pass so they can be coalesced into a single click
    // modifier and a single touch action modifier.
    val clickOps = ArrayList<ClickModifierOperation>()
    val multiClickOps = ArrayList<MultiClickModifier>()
    val touchDownOps = ArrayList<TouchDownModifierOperation>()
    val touchUpOps = ArrayList<TouchUpModifierOperation>()
    val touchCancelOps = ArrayList<TouchCancelModifierOperation>()
    list.fastForEach {
        when (it) {
            is ClickModifierOperation -> clickOps.add(it)
            is MultiClickModifier -> multiClickOps.add(it)
            is TouchDownModifierOperation -> touchDownOps.add(it)
            is TouchUpModifierOperation -> touchUpOps.add(it)
            is TouchCancelModifierOperation -> touchCancelOps.add(it)
        }
    }
    val hasClickModifier = !ignoreClicks && (clickOps.isNotEmpty() || multiClickOps.isNotEmpty())
    // A custom plugin that handles clicks itself (ignoreClicks) consumes the release in its own
    // content, which must not turn the component's TouchUp into TouchCancel.
    val hasClickHandler = hasClickModifier || ignoreClicks
    list.fastForEach { op ->
        modifier = modifier.rcModifierInspector(op)
        modifier =
            when (op) {
                is PaddingModifierOperation -> modifier.padding(op)
                is WidthModifierOperation -> modifier.width(op)
                is HeightModifierOperation -> modifier.height(op)
                is BorderModifierOperation -> modifier.border(op)
                is BackgroundModifierOperation -> modifier.background(op)
                is OffsetModifierOperation -> modifier.offset(op)
                is ClipRectModifierOperation -> modifier.clipRect(op)
                is RoundedClipRectModifierOperation ->
                    modifier.roundedClipRect(op, drawContentProcessed)
                is ZIndexModifierOperation -> modifier.zIndex(op)
                is GraphicsLayerModifierOperation -> modifier.graphicsLayer(op)
                is RippleModifierOperation -> modifier.ripple(op, hasClickModifier)
                is ScrollModifierOperation -> modifier.scroll(op)
                is WidthInModifierOperation -> modifier.widthIn(op)
                is HeightInModifierOperation -> modifier.heightIn(op)
                is DimensionConstraintsModifierOperation -> modifier.dimensionConstraints(op)
                is ClickModifierOperation,
                is MultiClickModifier -> {
                    // RemoteCompose emits a ClickModifierOperation for CLICK_TYPE_SINGLE and
                    // separate MultiClickModifier operations for CLICK_TYPE_DOUBLE and
                    // CLICK_TYPE_LONG on the same component (e.g. in combinedClickable).
                    // Coalesce them into a single gesture modifier, at the first click operation,
                    // so an outer clickable doesn't swallow double/long clicks before
                    // combinedClickable sees them.
                    if (!clickProcessed) {
                        clickProcessed = true
                        modifier.combinedClick(clickOps, multiClickOps)
                    } else {
                        modifier
                    }
                }
                is TouchDownModifierOperation,
                is TouchUpModifierOperation,
                is TouchCancelModifierOperation -> {
                    // Coalesce all touch action operations into a single modifier. When a click
                    // operation precedes it, combinedClick is outer to touchActions. The Main
                    // pass delivers events from the innermost modifier outwards, so touchActions
                    // sees the release before combinedClick consumes it, and leaves it unconsumed
                    // for combinedClick to handle. In the other order, combinedClick consumes the
                    // release first, which hasClickHandler makes touchActions treat as TouchUp.
                    if (!touchProcessed) {
                        touchProcessed = true
                        modifier.touchActions(
                            touchDownOps = touchDownOps,
                            touchUpOps = touchUpOps,
                            touchCancelOps = touchCancelOps,
                            hasClickHandler = hasClickHandler,
                        )
                    } else {
                        modifier
                    }
                }
                is ComponentVisibilityOperation ->
                    if (ignoreVisibility) modifier else modifier.visible(op)
                is MarqueeModifierOperation -> modifier.marquee(op)
                is CoreSemantics -> modifier.semantics(op)
                is DrawContentOperation -> {
                    // RemoteCompose merges all drawWithContent blocks on a component into a single
                    // CanvasOperations stream, so attach Modifier.drawWithContent once at the
                    // outermost DrawContentOperation and ignore subsequent markers.
                    if (!drawContentProcessed) {
                        drawContentProcessed = true
                        if (drawOpsList != null) {
                            val textMeasurer = rememberTextMeasurer()
                            val remoteContext = LocalRemoteContext.current
                            val graph = LocalGraphContext.current
                            modifier.drawWithContent {
                                executeOperations(
                                    operations = drawOpsList,
                                    remoteContext = remoteContext,
                                    textMeasurer = textMeasurer,
                                    onDrawContent = { drawContent() },
                                    graph = graph,
                                    contentLayer = contentLayer,
                                )
                            }
                        } else {
                            modifier
                        }
                    } else {
                        modifier
                    }
                }
                // AlignBy is applied per-child inside Row/Column scope (RcPlayerRow), where the
                // alignment lines are available; it is a no-op in the generic modifier chain.
                is AlignByModifierOperation -> modifier
                // CollapsiblePriority is a hint for collapsible layouts (which currently render as
                // plain Row/Column), and LayoutCompute is custom measure/position logic the
                // embedded
                // player doesn't support — consume both rather than warn.
                is CollapsiblePriorityModifierOperation -> modifier
                is LayoutComputeOperation -> modifier
                is AnimationSpec -> modifier
                else -> {
                    println("Warning: Unsupported modifier $op")
                    modifier
                }
            }
    }
    if (drawOpsList != null && !drawContentProcessed) {
        val textMeasurer = rememberTextMeasurer()
        val remoteContext = LocalRemoteContext.current
        val graph = LocalGraphContext.current
        modifier = modifier.drawWithContent {
            executeOperations(
                operations = drawOpsList,
                remoteContext = remoteContext,
                textMeasurer = textMeasurer,
                onDrawContent = { drawContent() },
                graph = graph,
                contentLayer = contentLayer,
            )
        }
    }
    return modifier
}
