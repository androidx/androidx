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

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.collection.mutableObjectIntMapOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
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
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.modifier.background
import androidx.compose.remote.player.compose.embedded.modifier.border
import androidx.compose.remote.player.compose.embedded.modifier.clipRect
import androidx.compose.remote.player.compose.embedded.modifier.combinedClick
import androidx.compose.remote.player.compose.embedded.modifier.dimensionConstraints
import androidx.compose.remote.player.compose.embedded.modifier.graphicsLayer
import androidx.compose.remote.player.compose.embedded.modifier.height
import androidx.compose.remote.player.compose.embedded.modifier.heightIn
import androidx.compose.remote.player.compose.embedded.modifier.marquee
import androidx.compose.remote.player.compose.embedded.modifier.offset
import androidx.compose.remote.player.compose.embedded.modifier.padding
import androidx.compose.remote.player.compose.embedded.modifier.ripple
import androidx.compose.remote.player.compose.embedded.modifier.roundedClipRect
import androidx.compose.remote.player.compose.embedded.modifier.scroll
import androidx.compose.remote.player.compose.embedded.modifier.touchActions
import androidx.compose.remote.player.compose.embedded.modifier.width
import androidx.compose.remote.player.compose.embedded.modifier.widthIn
import androidx.compose.remote.player.compose.embedded.modifier.zIndex
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.ceil
import kotlin.math.floor

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

@Composable
internal fun rememberComponentVisibility(op: ComponentVisibilityOperation): Int {
    val rawInt by rememberRemoteIntAsState(op.getVisibilityId())
    return when {
        Component.Visibility.isVisible(rawInt) -> Component.Visibility.VISIBLE
        Component.Visibility.isGone(rawInt) -> Component.Visibility.GONE
        else -> Component.Visibility.INVISIBLE
    }
}

/** A tween, or a snap when there is neither a duration nor a delay. */
private fun <T> visibilityTween(
    durationMillis: Int,
    delayMillis: Int,
    easing: Easing,
): FiniteAnimationSpec<T> =
    if (durationMillis <= 0 && delayMillis <= 0) {
        snap()
    } else {
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
    }

private fun buildEnterTransition(
    anim: AnimationSpec.ANIMATION,
    visDur: Int,
    visDelay: Int,
    visEasing: Easing,
    motDur: Int,
    motDelay: Int,
    motEasing: Easing,
): EnterTransition {
    val visualPart =
        when (anim) {
            AnimationSpec.ANIMATION.FADE_IN,
            AnimationSpec.ANIMATION.FADE_OUT -> fadeIn(visibilityTween(visDur, visDelay, visEasing))
            AnimationSpec.ANIMATION.SLIDE_LEFT ->
                slideInHorizontally(visibilityTween(visDur, visDelay, visEasing)) { -it }
            AnimationSpec.ANIMATION.SLIDE_RIGHT ->
                slideInHorizontally(visibilityTween(visDur, visDelay, visEasing)) { it }
            AnimationSpec.ANIMATION.SLIDE_TOP ->
                slideInVertically(visibilityTween(visDur, visDelay, visEasing)) { -it }
            AnimationSpec.ANIMATION.SLIDE_BOTTOM ->
                slideInVertically(visibilityTween(visDur, visDelay, visEasing)) { it }
            // Drawn by visibilityAnimationDraw, driven by the AnimatedVisibility scope's
            // transition.
            AnimationSpec.ANIMATION.ROTATE,
            AnimationSpec.ANIMATION.PARTICLE,
            AnimationSpec.ANIMATION.CUSTOM -> EnterTransition.None
        }
    val motionPart =
        if (motDur > 0 || motDelay > 0) {
            expandIn(
                animationSpec = visibilityTween(motDur, motDelay, motEasing),
                expandFrom = Alignment.TopStart,
                clip = false,
            )
        } else {
            EnterTransition.None
        }
    return visualPart + motionPart
}

private fun buildExitTransition(
    anim: AnimationSpec.ANIMATION,
    visDur: Int,
    visDelay: Int,
    visEasing: Easing,
    motDur: Int,
    motDelay: Int,
    motEasing: Easing,
): ExitTransition {
    val visualPart =
        when (anim) {
            AnimationSpec.ANIMATION.FADE_IN,
            AnimationSpec.ANIMATION.FADE_OUT ->
                fadeOut(visibilityTween(visDur, visDelay, visEasing))
            AnimationSpec.ANIMATION.SLIDE_LEFT ->
                slideOutHorizontally(visibilityTween(visDur, visDelay, visEasing)) { -it }
            AnimationSpec.ANIMATION.SLIDE_RIGHT ->
                slideOutHorizontally(visibilityTween(visDur, visDelay, visEasing)) { it }
            AnimationSpec.ANIMATION.SLIDE_TOP ->
                slideOutVertically(visibilityTween(visDur, visDelay, visEasing)) { -it }
            AnimationSpec.ANIMATION.SLIDE_BOTTOM ->
                slideOutVertically(visibilityTween(visDur, visDelay, visEasing)) { it }
            // See buildEnterTransition.
            AnimationSpec.ANIMATION.ROTATE,
            AnimationSpec.ANIMATION.PARTICLE,
            AnimationSpec.ANIMATION.CUSTOM -> ExitTransition.None
        }
    val motionPart =
        if (motDur > 0 || motDelay > 0) {
            shrinkOut(
                animationSpec = visibilityTween(motDur, motDelay, motEasing),
                shrinkTowards = Alignment.TopStart,
                clip = false,
            )
        } else {
            ExitTransition.None
        }
    return visualPart + motionPart
}

@Composable
internal fun RcPlayerAnimatedVisibility(
    component: LayoutComponent,
    visibilityOp: ComponentVisibilityOperation?,
    animSpec: AnimationSpec?,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val sizeTrackerModifier =
        remember(component) {
            Modifier.onPlaced { coords ->
                component.setWidth(coords.size.width.toFloat())
                component.setHeight(coords.size.height.toFloat())
            }
        }
    if (visibilityOp == null) {
        content(modifier.then(sizeTrackerModifier))
        return
    }
    val visible = rememberComponentVisibility(visibilityOp)
    val canAnimate = animSpec != null && animSpec.isAnimationEnabled
    if (!canAnimate) {
        SideEffect { component.mVisibility = visible }
        if (Component.Visibility.isGone(visible)) {
            return
        }
        content(
            modifier.then(sizeTrackerModifier).graphicsLayer {
                alpha = if (Component.Visibility.isVisible(visible)) 1f else 0f
            }
        )
        return
    }

    val visibilityTransition = updateTransition(targetState = visible, label = "RcVisibility")
    val visDur = animSpec.visibilityDuration.toInt().coerceAtLeast(0)
    val motDur = animSpec.motionDuration.toInt().coerceAtLeast(0)
    val visEasing = mapEasing(animSpec.visibilityEasingType)
    val motEasing = mapEasing(animSpec.motionEasingType)
    val enterVisDelay = if (animSpec.enterSequence == AnimationSpec.SEQUENCE.AFTER) motDur else 0
    val enterMotDelay = if (animSpec.enterSequence == AnimationSpec.SEQUENCE.BEFORE) visDur else 0
    val exitVisDelay = if (animSpec.exitSequence == AnimationSpec.SEQUENCE.AFTER) motDur else 0
    val exitMotDelay = if (animSpec.exitSequence == AnimationSpec.SEQUENCE.BEFORE) visDur else 0

    // Held as State and only read in the draw phase (and in derivedStateOf below), so the
    // animation invalidates drawing each frame rather than recomposing the component's subtree.
    val visProgressState =
        visibilityTransition.animateFloat(
            transitionSpec = {
                val isEntering = Component.Visibility.isVisible(targetState)
                val delay = if (isEntering) enterVisDelay else exitVisDelay
                visibilityTween(visDur, delay, visEasing)
            },
            label = "visProgress",
        ) { state ->
            if (Component.Visibility.isVisible(state)) 1f else 0f
        }

    val isActive = visibilityTransition.currentState != visibilityTransition.targetState
    // Report the component to core as VISIBLE until its exit animation has faded it out. Read in
    // composition (not inside the SideEffect) so a change of the derived value recomposes.
    val coreVisibility =
        remember(visibilityTransition, visProgressState) {
                derivedStateOf {
                    val active =
                        visibilityTransition.currentState != visibilityTransition.targetState
                    if (active && visProgressState.value > 0.0001f) {
                        Component.Visibility.VISIBLE
                    } else {
                        visibilityTransition.targetState
                    }
                }
            }
            .value
    SideEffect { component.mVisibility = coreVisibility }

    val enterTransition =
        remember(animSpec, visDur, motDur, enterVisDelay, enterMotDelay, visEasing, motEasing) {
            buildEnterTransition(
                anim = animSpec.enterAnimation,
                visDur = visDur,
                visDelay = enterVisDelay,
                visEasing = visEasing,
                motDur = motDur,
                motDelay = enterMotDelay,
                motEasing = motEasing,
            )
        }
    val exitTransition =
        remember(animSpec, visDur, motDur, exitVisDelay, exitMotDelay, visEasing, motEasing) {
            buildExitTransition(
                anim = animSpec.exitAnimation,
                visDur = visDur,
                visDelay = exitVisDelay,
                visEasing = visEasing,
                motDur = motDur,
                motDelay = exitMotDelay,
                motEasing = motEasing,
            )
        }

    visibilityTransition.AnimatedVisibility(
        visible = { !Component.Visibility.isGone(it) },
        modifier = modifier,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        DisposableEffect(component) {
            onDispose {
                if (Component.Visibility.isGone(visibilityTransition.targetState)) {
                    component.mVisibility = Component.Visibility.GONE
                }
            }
        }
        val isEntering = Component.Visibility.isVisible(visibilityTransition.targetState)
        // ROTATE, CUSTOM and PARTICLE are drawn by visibilityAnimationDraw and have no
        // Enter/ExitTransition. Animating on this scope's transition keeps the content composed
        // until the visibility animation finishes, instead of disposing it as soon as the exit
        // starts.
        val drawProgressState =
            transition.animateFloat(
                transitionSpec = {
                    val delay =
                        if (targetState == EnterExitState.Visible) enterVisDelay else exitVisDelay
                    visibilityTween(visDur, delay, visEasing)
                },
                label = "drawProgress",
            ) { state ->
                if (state == EnterExitState.Visible) 1f else 0f
            }
        val innerModifier =
            sizeTrackerModifier.visibilityAnimationDraw(
                component = component,
                animSpec = animSpec,
                isActive = isActive,
                isEntering = isEntering,
                visProgressProvider = { drawProgressState.value },
            )
        content(innerModifier)
    }
}

@Composable
private fun Modifier.visible(op: ComponentVisibilityOperation): Modifier {
    val visible = rememberComponentVisibility(op)

    return this.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (visible != Component.Visibility.GONE) {
                    placeable.place(0, 0)
                }
            }
        }
        .graphicsLayer { alpha = if (visible == Component.Visibility.VISIBLE) 1f else 0f }
}

@Composable
private fun Modifier.visibilityAnimationDraw(
    component: LayoutComponent,
    animSpec: AnimationSpec,
    isActive: Boolean,
    isEntering: Boolean,
    visProgressProvider: () -> Float,
): Modifier {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val textMeasurer = rememberTextMeasurer()
    val contentLayer = rememberGraphicsLayer()

    return this.drawWithContent {
        val visProgress = visProgressProvider()
        if (visProgress <= 0.0001f) {
            // Hold invisible during AFTER-enter Phase 1, BEFORE-exit Phase 2, or when INVISIBLE
            return@drawWithContent
        }
        if (isActive) {
            val anim = if (isEntering) animSpec.enterAnimation else animSpec.exitAnimation
            val fnId = if (isEntering) animSpec.enterFunctionId else animSpec.exitFunctionId
            val progress = (if (isEntering) visProgress else 1f - visProgress).coerceIn(0f, 1f)
            when (anim) {
                AnimationSpec.ANIMATION.CUSTOM -> {
                    executeCustomVisibilityAnimation(
                        component = component,
                        functionId = fnId,
                        progress = progress,
                        remoteContext = remoteContext,
                        graph = graph,
                        textMeasurer = textMeasurer,
                        contentLayer = contentLayer,
                    )
                }
                AnimationSpec.ANIMATION.ROTATE -> {
                    // A full turn, fading in on enter and out on exit.
                    contentLayer.alpha = if (isEntering) progress else 1f - progress
                    contentLayer.rotationZ =
                        if (isEntering) (1f - progress) * 360f else -progress * 360f
                    contentLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(contentLayer)
                }
                else -> {
                    drawContent()
                }
            }
        } else {
            drawContent()
        }
    }
}

private val activeFunctionDepth = mutableObjectIntMapOf<FloatFunctionDefine>()

private fun ContentDrawScope.executeCustomVisibilityAnimation(
    component: LayoutComponent,
    functionId: Int,
    progress: Float,
    remoteContext: RemoteContext,
    graph: GraphContext?,
    textMeasurer: TextMeasurer,
    contentLayer: GraphicsLayer,
) {
    if (functionId == -1) {
        drawContent()
        return
    }
    val fn = remoteContext.getObject(functionId) as? FloatFunctionDefine
    if (fn == null) {
        drawContent()
        return
    }
    val args = fn.args
    val currentDepth =
        synchronized(activeFunctionDepth) {
            activeFunctionDepth.getOrDefault(fn, 0) + fn.executionDepth
        }
    var prevArgs: FloatArray? = null
    var prevIdInt = 0
    if (currentDepth > 0) {
        prevArgs = FloatArray(args.size) { i -> remoteContext.getFloat(args[i]) }
        prevIdInt = if (args.size >= 6) remoteContext.getInteger(args[5]) else 0
    }

    val rawX = component.x
    val rawY = component.y
    val compW = if (component.width > 0f) component.width else size.width
    val compH = if (component.height > 0f) component.height else size.height
    component.width = compW
    component.height = compH
    val x = floor(rawX)
    val y = floor(rawY)
    val w = ceil((rawX - x) + compW)
    val h = ceil((rawY - y) + compH)

    if (args.size >= 1) remoteContext.loadFloat(args[0], progress)
    if (args.size >= 2) remoteContext.loadFloat(args[1], w)
    if (args.size >= 3) remoteContext.loadFloat(args[2], h)
    if (args.size >= 4) remoteContext.loadFloat(args[3], x)
    if (args.size >= 5) remoteContext.loadFloat(args[4], y)
    if (args.size >= 6) {
        remoteContext.loadInteger(args[5], component.componentId)
        remoteContext.loadFloat(args[5], component.componentId.toFloat())
    }

    val prevComponent = remoteContext.mLastComponent
    remoteContext.mLastComponent = component
    BitmapData.pushOffscreenScope(remoteContext)
    synchronized(activeFunctionDepth) {
        activeFunctionDepth[fn] = activeFunctionDepth.getOrDefault(fn, 0) + 1
    }
    val offscreenState = OffscreenDrawState()
    drawContext.canvas.save()
    drawContext.canvas.translate(-rawX, -rawY)
    try {
        executeOperations(
            operations = fn.list,
            remoteContext = remoteContext,
            textMeasurer = textMeasurer,
            paintState = ComposeLocalPaint(),
            onDrawContent = {
                if (offscreenState.isDrawingToOffscreen) {
                    drawContext.canvas.save()
                    drawContext.canvas.translate(rawX - x, rawY - y)
                    try {
                        this@executeCustomVisibilityAnimation.drawContent()
                    } finally {
                        drawContext.canvas.restore()
                    }
                } else {
                    this@executeCustomVisibilityAnimation.drawContent()
                }
            },
            graph = graph,
            offscreenState = offscreenState,
            contentLayer = contentLayer,
            contentOrigin = Offset(rawX, rawY),
        )
    } finally {
        drawContext.canvas.restore()
        synchronized(activeFunctionDepth) {
            val next = activeFunctionDepth.getOrDefault(fn, 1) - 1
            if (next <= 0) activeFunctionDepth.remove(fn) else activeFunctionDepth[fn] = next
        }
        BitmapData.popOffscreenScope(remoteContext)
        remoteContext.mLastComponent = prevComponent
        if (prevArgs != null) {
            for (i in args.indices) {
                remoteContext.loadFloat(args[i], prevArgs[i])
            }
            if (args.size >= 6) {
                remoteContext.loadInteger(args[5], prevIdInt)
            }
        }
    }
}

@Composable
private fun Modifier.semantics(op: CoreSemantics): Modifier {
    val contentDescription =
        op.mContentDescriptionId.takeIf { it != 0 }?.let { rememberRemoteStringAsState(it).value }
    val text = op.mTextId.takeIf { it != 0 }?.let { rememberRemoteStringAsState(it).value }

    if (contentDescription == null && text == null && op.mRole == null) return this

    val properties: SemanticsPropertyReceiver.() -> Unit = {
        if (contentDescription != null) {
            this.contentDescription = contentDescription
        }

        if (text != null) {
            this.text = AnnotatedString(text)
        }

        op.mRole?.let { this.role = it.toComposeRole() }
    }
    return when (op.mMode) {
        AccessibleComponent.Mode.SET -> this.semantics(properties = properties)
        AccessibleComponent.Mode.CLEAR_AND_SET -> this.clearAndSetSemantics(properties = properties)
        AccessibleComponent.Mode.MERGE ->
            this.semantics(mergeDescendants = true, properties = properties)
    }
}

private fun AccessibleComponent.Role.toComposeRole(): Role {
    return when (this) {
        AccessibleComponent.Role.BUTTON -> Role.Button
        AccessibleComponent.Role.CHECKBOX -> Role.Checkbox
        AccessibleComponent.Role.SWITCH -> Role.Switch
        AccessibleComponent.Role.RADIO_BUTTON -> Role.RadioButton
        AccessibleComponent.Role.TAB -> Role.Tab
        AccessibleComponent.Role.IMAGE -> Role.Image
        AccessibleComponent.Role.DROPDOWN_LIST -> Role.DropdownList
        // No direct Compose Role equivalents; Button is the closest interactive fallback.
        AccessibleComponent.Role.PICKER -> Role.Button
        AccessibleComponent.Role.CAROUSEL -> Role.Button
        else -> Role.Button
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.sharedElementTransition(component: Component): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this

    val animationId = component.animationId
    if (animationId == -1 || animationId == 0) return this

    val layout = component as? LayoutComponent
    val spec =
        layout?.componentModifiers?.list?.fastFirstOrNull { it is AnimationSpec } as? AnimationSpec
            ?: component.animationSpec
            ?: AnimationSpec.DEFAULT

    val motionDuration = spec.motionDuration.toInt()
    val motionEasing = mapEasing(spec.motionEasingType)

    val boundsTransform =
        remember(motionDuration, motionEasing) {
            BoundsTransform { _, _ ->
                if (motionDuration <= 0) {
                    snap()
                } else {
                    tween(durationMillis = motionDuration, easing = motionEasing)
                }
            }
        }

    val fadeSpec =
        remember(motionDuration, motionEasing) {
            if (motionDuration <= 0) {
                snap()
            } else {
                tween<Float>(durationMillis = motionDuration, easing = motionEasing)
            }
        }

    with(sharedTransitionScope) {
        return this@sharedElementTransition.sharedBounds(
            sharedContentState = rememberSharedContentState(key = animationId),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
            boundsTransform = boundsTransform,
            resizeMode = ResizeMode.RemeasureToBounds,
        )
    }
}
