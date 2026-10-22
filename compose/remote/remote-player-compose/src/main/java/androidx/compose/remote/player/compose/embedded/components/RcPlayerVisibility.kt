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

package androidx.compose.remote.player.compose.embedded.components

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
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.player.compose.embedded.LocalAnimatedVisibilityScope
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.LocalSharedTransitionScope
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.mapEasing
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.OffscreenDrawState
import androidx.compose.remote.player.compose.embedded.draw.executeOperations
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.ceil
import kotlin.math.floor

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
internal fun Modifier.visible(op: ComponentVisibilityOperation): Modifier {
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
