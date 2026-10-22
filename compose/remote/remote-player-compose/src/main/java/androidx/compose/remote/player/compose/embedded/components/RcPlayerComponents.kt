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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection", "ModifierParameter")

package androidx.compose.remote.player.compose.embedded.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.player.compose.embedded.LocalComponentTouchExpressionsMap
import androidx.compose.remote.player.compose.embedded.LocalComponentValueMap
import androidx.compose.remote.player.compose.embedded.LocalComponentValueStateMap
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalHasTouchExpressions
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.LocalRootLayoutCoordinates
import androidx.compose.remote.player.compose.embedded.draw.executeOperations
import androidx.compose.remote.player.compose.embedded.inspectable.rcComponentContentInspector
import androidx.compose.remote.player.compose.embedded.inspectable.rcComponentOuterInspector
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerBox
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerColumn
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerFitBoxLayout
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerFlowRow
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerImageLayout
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerRow
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerStateLayout
import androidx.compose.remote.player.compose.embedded.modifier.ClickActionHandlers
import androidx.compose.remote.player.compose.embedded.modifier.toModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * Renders a document that has no root layout component — a raw draw-list document whose draw
 * operations live at the top level of `mOperations` rather than inside a component tree (e.g.
 * shader / canvas demos built without the layout DSL). The View player renders these via
 * `CoreDocument.paint`; here the document's operations are executed directly in a Canvas. Without
 * this fallback the layout-tree renderer dereferenced a null `rootLayoutComponent` and crashed
 * (NPE) for such documents.
 */
@Composable
internal fun RcPlayerRawDocument() {
    val document = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val textMeasurer = rememberTextMeasurer()
    val scale = remember(document) { FloatArray(2) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // The document size is published from the player's onPlaced, which runs before draw.
        if (document.contentSizing == RootContentBehavior.SIZING_SCALE) {
            // RootContentBehavior: scale and align the document's own size into the player, as
            // CoreDocument.paint does.
            document.computeScale(size.width, size.height, scale)
            val translate = document.contentTranslation(size.width, size.height, scale)
            withTransform({
                translate(translate.x, translate.y)
                scale(scale[0], scale[1], pivot = Offset.Zero)
            }) {
                executeOperations(
                    document.operations,
                    remoteContext,
                    graph = graph,
                    textMeasurer = textMeasurer,
                )
            }
        } else {
            executeOperations(
                document.operations,
                remoteContext,
                graph = graph,
                textMeasurer = textMeasurer,
            )
        }
    }
}

/**
 * Where content scaled by [scale] is placed in a [width] x [height] player, following the
 * document's RootContentBehavior alignment. Mirrors CoreDocument.computeTranslate.
 */
internal fun CoreDocument.contentTranslation(
    width: Float,
    height: Float,
    scale: FloatArray,
): Offset {
    val alignment = contentAlignment
    val contentWidth = this.width * scale[0]
    val contentHeight = this.height * scale[1]
    // RootContentBehavior packs horizontal alignment in the upper nibble (0xF0) and vertical
    // alignment in the lower nibble (0xF); see CoreDocument.computeTranslate.
    val x =
        when (alignment and 0xF0) {
            RootContentBehavior.ALIGNMENT_HORIZONTAL_CENTER -> (width - contentWidth) / 2f
            RootContentBehavior.ALIGNMENT_END -> width - contentWidth
            else -> 0f
        }
    val y =
        when (alignment and 0xF) {
            RootContentBehavior.ALIGNMENT_VERTICAL_CENTER -> (height - contentHeight) / 2f
            RootContentBehavior.ALIGNMENT_BOTTOM -> height - contentHeight
            else -> 0f
        }
    return Offset(x, y)
}

@Composable
internal fun RcPlayerRootLayoutComponent(root: RootLayoutComponent) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current

    val drawOps = remember(root) { root.list.fastFilter { it !is Component } }
    if (drawOps.isNotEmpty()) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxSize()) {
            executeOperations(
                drawOps,
                remoteContext,
                graph = graph,
                textMeasurer = textMeasurer,
            )
        }
    }

    RcPlayerChildren(root)
}

@Composable
internal fun RcPlayerComponent(component: Component, modifier: Modifier = Modifier) {
    if (component is LayoutComponent) {
        val document = LocalCoreDocument.current
        val componentValueMap = LocalComponentValueMap.current
        val componentValueStateMap = LocalComponentValueStateMap.current
        val componentTouchExpressionsMap = LocalComponentTouchExpressionsMap.current
        val hasTouchExpressions = LocalHasTouchExpressions.current
        val rootCoordsHolder = LocalRootLayoutCoordinates.current
        val remoteContext = LocalRemoteContext.current
        val graphContext = LocalGraphContext.current

        // FitBoxLayout and StateLayout unconditionally override the visibility of their direct
        // candidate/page children (matching FitBoxLayout.addVisibilityOverride and
        // StateLayout.measure in remote-core).
        val visibilityDelegatedToParent =
            component.parent is FitBoxLayout || component.parent is StateLayout
        val visibilityOp =
            if (visibilityDelegatedToParent) null
            else
                component.componentModifiers.list.fastFirstOrNull {
                    it is ComponentVisibilityOperation
                } as? ComponentVisibilityOperation
        val animSpec =
            component.componentModifiers.list.fastFirstOrNull { it is AnimationSpec }
                as? AnimationSpec ?: component.animationSpec

        // Custom components whose plugin handles clicks itself (e.g. a native button) receive the
        // click actions via RcCustomComponent instead of a wrapping clickable.
        val customClickHandlers =
            if (component is Custom) rememberCustomClickHandlers(component) else null

        val touchExpressions = componentTouchExpressionsMap[component.componentId].orEmpty()
        val placement = remember(component) { ComponentPlacementHolder() }
        var outerModifier =
            Modifier.sharedElementTransition(component)
                .then(
                    // Allow StateLayout to adopt its active child's measured dimensions rather than
                    // expanding to incoming minimum parent constraints.
                    if (component is StateLayout) {
                        Modifier.wrapContentSize(Alignment.TopStart)
                    } else {
                        Modifier
                    }
                )
                // The caller's modifier carries parent data (e.g. Row/Column weight and
                // alignment), so it must be on the outermost layout node, which is
                // AnimatedVisibility's when the component animates its visibility.
                .then(modifier)

        // Visibility is applied by RcPlayerAnimatedVisibility, inside the outer (placement)
        // modifiers and outside the component's own modifiers.
        val contentModifier =
            Modifier.rcComponentOuterInspector(component)
                .then(
                    component.componentModifiers.toModifier(
                        component.drawContentOperations?.list,
                        ignoreVisibility = true,
                        ignoreClicks = customClickHandlers != null,
                    )
                )
                .rcComponentContentInspector(component)
                .then(
                    if (touchExpressions.isNotEmpty()) {
                        Modifier.componentTouchExpressions(
                            component = component,
                            touchExpressions = touchExpressions,
                            placement = placement,
                            remoteContext = remoteContext,
                            graphContext = graphContext,
                        )
                    } else {
                        Modifier
                    }
                )

        // Publish the component's measured dimensions and positions (read by ComponentValue
        // expressions and TouchExpression) from an onPlaced callback rather than a custom
        // Modifier.layout that wrote snapshot state during the measure pass (a relayout hazard) and
        // sat ahead of the real modifiers (which disturbed constraint propagation, e.g. FILL
        // children collapsing to wrap size). As the outermost modifier, onPlaced reports the full
        // component size and placement and fires after layout.
        val sizeFeedbackOps = componentValueMap[component.getId()]
        if (
            !sizeFeedbackOps.isNullOrEmpty() || touchExpressions.isNotEmpty() || hasTouchExpressions
        ) {
            outerModifier =
                Modifier.onPlaced { coords ->
                        placement.outerCoords = coords
                        val posInParent = coords.positionInParent()
                        val w = coords.size.width.toFloat()
                        val h = coords.size.height.toFloat()
                        val rootCoords = rootCoordsHolder.coordinates
                        val rootOffset =
                            if (rootCoords != null && rootCoords.isAttached) {
                                rootCoords.localPositionOf(coords, Offset.Zero)
                            } else {
                                val posOnScreen = coords.positionOnScreen()
                                Offset(
                                    posOnScreen.x - document.originX,
                                    posOnScreen.y - document.originY,
                                )
                            }
                        val sizeChanged =
                            !placement.boundsInitialized ||
                                placement.width != w ||
                                placement.height != h
                        val parentPosChanged =
                            !placement.boundsInitialized || placement.posInParent != posInParent
                        val rootPosChanged =
                            !placement.boundsInitialized || placement.rootOffset != rootOffset
                        placement.boundsInitialized = true
                        placement.width = w
                        placement.height = h
                        placement.posInParent = posInParent
                        placement.rootOffset = rootOffset

                        if (parentPosChanged) {
                            component.setLayoutPosition(posInParent.x, posInParent.y)
                        }
                        if (sizeChanged) {
                            component.setWidth(w)
                            component.setHeight(h)
                        }
                        if (
                            !sizeFeedbackOps.isNullOrEmpty() &&
                                (sizeChanged || parentPosChanged || rootPosChanged)
                        ) {
                            val contentW =
                                (w - component.paddingLeft - component.paddingRight).coerceAtLeast(
                                    0f
                                )
                            val contentH =
                                (h - component.paddingTop - component.paddingBottom).coerceAtLeast(
                                    0f
                                )
                            sizeFeedbackOps.fastForEach { op ->
                                val newVal =
                                    when (op.type) {
                                        ComponentValue.WIDTH -> w
                                        ComponentValue.CONTENT_WIDTH -> contentW
                                        ComponentValue.HEIGHT -> h
                                        ComponentValue.CONTENT_HEIGHT -> contentH
                                        ComponentValue.POS_X -> posInParent.x
                                        ComponentValue.POS_Y -> posInParent.y
                                        ComponentValue.POS_ROOT_X -> rootOffset.x
                                        ComponentValue.POS_ROOT_Y -> rootOffset.y
                                        else -> return@fastForEach
                                    }
                                remoteContext.loadFloat(op.valueId, newVal)
                                val state = componentValueStateMap[op.valueId] ?: return@fastForEach
                                if (abs(newVal - state.value) > 2.0f) {
                                    state.value = newVal
                                }
                            }
                        }
                        // Position changes don't re-apply touch expressions: their bounds are
                        // refreshed at touch down.
                        if (touchExpressions.isNotEmpty() && sizeChanged) {
                            touchExpressions.fastForEach { te ->
                                te.updateVariables(graphContext ?: remoteContext)
                                te.apply(remoteContext)
                            }
                        }
                    }
                    .then(outerModifier)
        } else {
            // Custom visibility animations pass the component's position to the animation
            // function (see executeCustomVisibilityAnimation).
            outerModifier =
                Modifier.onPlaced { coords ->
                        val pos = coords.positionInParent()
                        component.setX(pos.x)
                        component.setY(pos.y)
                    }
                    .then(outerModifier)
        }

        RcPlayerAnimatedVisibility(
            component = component,
            visibilityOp = visibilityOp,
            animSpec = animSpec,
            modifier = outerModifier,
        ) { visibilityModifier ->
            RcPlayerLayoutComponent(
                component,
                visibilityModifier.then(contentModifier),
                customClickHandlers,
            )
        }
    } else {
        // Non-LayoutComponent component reached dispatch — skip gracefully instead of crashing.
        println(
            "Warning: unsupported component ${component?.let { it::class.java.simpleName }}; rendering nothing"
        )
    }
}

@Composable
private fun RcPlayerLayoutComponent(
    component: LayoutComponent,
    modifier: Modifier,
    customClickHandlers: ClickActionHandlers?,
) {
    when (component) {
        is CanvasLayout -> RcPlayerCanvas(component, modifier)
        is ColumnLayout -> RcPlayerColumn(component, modifier)
        is FlowLayout -> RcPlayerFlowRow(component, modifier)
        is RowLayout -> RcPlayerRow(component, modifier)
        is CoreText -> RcPlayerText(component, modifier)
        is TextLayout -> RcPlayerText(component, modifier)
        is FitBoxLayout -> RcPlayerFitBoxLayout(component, modifier)
        is StateLayout -> RcPlayerStateLayout(component, modifier)
        is ImageLayout -> RcPlayerImageLayout(component, modifier)
        is Custom -> RcPlayerCustom(component, modifier, customClickHandlers)
        // Last as others are often BoxLayout subclasses
        is BoxLayout -> RcPlayerBox(component, modifier)
        else -> {
            // Unsupported layout type. Render nothing rather than crash; see
            // operation_coverage.md. The modifier (incl. any drawContent) was still applied
            // above.
            println(
                "Warning: unsupported layout component ${component::class.java.simpleName}; rendering nothing"
            )
        }
    }
}

@Composable
internal fun RcPlayerChildren(
    layout: Component,
    modifierProvider: @Composable (Component) -> Modifier = { Modifier },
) {
    if (layout is LayoutComponent) {
        layout.childrenComponents.fastForEach { child ->
            val scopeModifier = modifierProvider(child)
            RcPlayerComponent(child, scopeModifier)
        }
    } else {
        val children =
            remember(layout) { ArrayList<Component>().apply { layout.getComponents(this) } }
        children.fastForEach { op -> RcPlayerComponent(op) }
    }
}
