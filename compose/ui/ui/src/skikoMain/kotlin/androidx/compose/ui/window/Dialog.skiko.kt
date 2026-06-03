/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.animation.easeOutTimingFunction
import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.isDialogAnimationEnabled
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.exclude
import androidx.compose.ui.platform.excludeWindowInsets
import androidx.compose.ui.platform.union
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.Content
import androidx.compose.ui.scene.rememberComposeSceneLayer
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * The default scrim opacity.
 */
private const val DefaultScrimOpacity = 0.6f
private val DefaultScrimColor = Color.Black.copy(alpha = DefaultScrimOpacity)
private const val AnimatedLayerOffsetDp = 10f
private const val AnimatedLayerInitialAlpha = 0.2f
private const val AnimatedLayerScale = 0.05f
private const val AnimatedLayerAppearanceDuration = 0.2
private const val AnimatedLayerDisappearanceDuration = 0.1

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the dialog can be dismissed by pressing the back button
 *  * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 * dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 * the platform default, which is smaller than the screen width.
 * @property usePlatformInsets Whether the size of the dialog's content should be limited by
 * platform insets.
 * @property useSoftwareKeyboardInset Whether the size of the dialog's content should be limited by
 * software keyboard inset.
 * @property scrimColor Color of background fill.
 * @property animateTransition Whether to animate the appearance and disappearance of the dialog.
 */
@Immutable
actual class DialogProperties @ExperimentalComposeUiApi constructor(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
    @property:ExperimentalComposeUiApi
    val animateTransition: Boolean = ComposeUiFlags.isDialogAnimationEnabled,
) {
    actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
        usePlatformDefaultWidth: Boolean,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        usePlatformInsets = true,
        useSoftwareKeyboardInset = true,
        scrimColor = DefaultScrimColor,
    )

    constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        usePlatformDefaultWidth: Boolean = true,
        usePlatformInsets: Boolean = true,
        useSoftwareKeyboardInset: Boolean = true,
        scrimColor: Color = DefaultScrimColor,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        usePlatformInsets = usePlatformInsets,
        useSoftwareKeyboardInset = useSoftwareKeyboardInset,
        scrimColor = scrimColor,
        animateTransition = ComposeUiFlags.isDialogAnimationEnabled,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false
        if (useSoftwareKeyboardInset != other.useSoftwareKeyboardInset) return false
        if (scrimColor != other.scrimColor) return false
        if (animateTransition != other.animateTransition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
        result = 31 * result + animateTransition.hashCode()
        return result
    }
}

@OptIn(InternalComposeApi::class)
@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val compositeKey = currentCompositeKeyHashCode
    val onBackHandler = remember(compositeKey) {
        OnBackClickEventHandler(compositeKey) { currentOnDismissRequest() }
    }
    LaunchedEffect(onBackHandler, properties.dismissOnBackPress) {
        onBackHandler.backClickIsEnabled = properties.dismissOnBackPress
    }
    val navigationEventDispatcher =
        requireNotNull(LocalNavigationEventDispatcherOwner.current) {
            error("NavigationEventDispatcherOwner not found")
        }.navigationEventDispatcher
    DisposableEffect(navigationEventDispatcher, onBackHandler) {
        navigationEventDispatcher.addHandler(onBackHandler)
        onDispose { onBackHandler.remove() }
    }
    val onOutsidePointerEvent = if (properties.dismissOnClickOutside) {
        { eventType: PointerEventType, button: PointerButton? ->
            // Clicking outside dialog is clicking on scrim.
            // So this behavior should match regular clicks or [detectTapGestures] that accepts
            // only primary mouse button clicks.
            if (eventType == PointerEventType.Release &&
                (button == null || button == PointerButton.Primary)
            ) {
                currentOnDismissRequest()
            }
        }
    } else {
        null
    }
    DialogLayout(
        modifier = Modifier.semantics { dialog() },
        onOutsidePointerEvent = onOutsidePointerEvent,
        properties = properties,
        content = content
    )
}

@Composable
private fun DialogLayout(
    properties: DialogProperties,
    modifier: Modifier = Modifier,
    onOutsidePointerEvent: ((eventType: PointerEventType, button: PointerButton?) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val layer = rememberComposeSceneLayer(focusable = true)
    layer.setOutsidePointerEventListener(onOutsidePointerEvent)
    val currentContent by rememberUpdatedState(content)
    val parentCompositionContext = rememberCompositionContext()
    val graphicsContext = LocalGraphicsContext.current

    val animator = remember {
        DialogAppearanceController(
            layer = layer,
            parentCompositionContext = parentCompositionContext,
            graphicsContext = graphicsContext,
            properties = properties,
        )
    }

    animator.properties = properties
    var layerScope: CoroutineScope? = null

    layer.Content(parentCompositionContext) {
        layerScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            animator.onDialogShown()
        }

        val containerSize = LocalWindowInfo.current.containerSize
        val measurePolicy = rememberDialogMeasurePolicy(
            layer = layer,
            properties = properties,
            containerSize = containerSize
        )

        // TODO: remove exclude in favor of excludeWindowInsets https://youtrack.jetbrains.com/issue/CMP-9379
        LocalPlatformWindowInsets.current.exclude(
            safeInsets = properties.usePlatformInsets,
            ime = properties.useSoftwareKeyboardInset
        ) {
            Layout(
                content = currentContent,
                modifier = animator.modifier
                    .then(modifier)
                    .excludeWindowInsets(properties.usePlatformInsets, properties.useSoftwareKeyboardInset),
                measurePolicy = measurePolicy
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (layerScope?.isActive == true) {
                animator.hideDialog()
            }
        }
    }
}

private class DialogAppearanceController(
    private val layer: ComposeSceneLayer,
    private val parentCompositionContext: CompositionContext,
    private val graphicsContext: GraphicsContext,
    properties: DialogProperties,
) {
    private var appearanceProgress by mutableFloatStateOf(0f)
    private val graphicsLayer = graphicsContext.createGraphicsLayer()
    var properties: DialogProperties = properties
        set(value) {
            field = value
            updateScrimLayerColor(Snapshot.withoutReadObservation { appearanceProgress })
        }

    val modifier = Modifier.drawWithGraphicsLayer { appearanceProgress }

    suspend fun onDialogShown() {
        if (properties.animateTransition) {
            val durationScale = currentCoroutineContext().durationScale()
            withAnimationProgress(
                duration = (durationScale * AnimatedLayerAppearanceDuration).seconds,
                timingFunction = ::easeOutTimingFunction
            ) { progress ->
                appearanceProgress = progress
                updateScrimLayerColor(progress)
            }
        }
        appearanceProgress = 1f
        layer.scrimColor = properties.scrimColor
    }

    fun hideDialog() {
        if (properties.animateTransition) {
            hideDialogWithAnimation()
        } else {
            layer.close()
        }
    }

    fun hideDialogWithAnimation() {
        layer.setContent(parentCompositionContext) {
            val containerSize = LocalWindowInfo.current.containerSize
            val measurePolicy = rememberDialogMeasurePolicy(
                layer = layer,
                properties = properties,
                containerSize = containerSize
            )

            Layout(
                modifier = Modifier.drawBehind {
                    graphicsLayer.applyAnimationProgress(appearanceProgress, density)
                    drawLayer(graphicsLayer)
                },
                measurePolicy = measurePolicy
            )
            LaunchedEffect(Unit) {
                val durationScale = currentCoroutineContext().durationScale()
                val initialProgress = appearanceProgress
                val duration = durationScale * initialProgress * AnimatedLayerDisappearanceDuration
                withAnimationProgress(
                    duration = duration.seconds,
                    timingFunction = ::easeOutTimingFunction
                ) { progress ->
                    val reversedProgress = (1f - progress) * initialProgress
                    appearanceProgress = reversedProgress
                    updateScrimLayerColor(reversedProgress)
                }
                graphicsContext.releaseGraphicsLayer(graphicsLayer)
                layer.close()
            }
        }
    }

    private fun updateScrimLayerColor(progress: Float) {
        layer.scrimColor =
            properties.scrimColor.copy(properties.scrimColor.alpha * contentAlpha(progress))
    }

    private fun contentAlpha(progress: Float): Float =
        AnimatedLayerInitialAlpha + (1f - AnimatedLayerInitialAlpha) * progress

    private fun GraphicsLayer.applyAnimationProgress(progress: Float, density: Float) {
        alpha = contentAlpha(progress)
        val reversedProgress = 1f - progress
        val scale = 1f - reversedProgress * AnimatedLayerScale
        scaleX = scale
        scaleY = scale
        translationY = AnimatedLayerOffsetDp * reversedProgress * density
    }

    private fun Modifier.drawWithGraphicsLayer(getProgress: () -> Float): Modifier =
        drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            graphicsLayer.applyAnimationProgress(getProgress(), density)
            drawLayer(graphicsLayer)
        }
}

private val DialogProperties.platformInsets: PlatformInsets
    @Composable get() {
        val safeInsets = if (usePlatformInsets) {
            LocalPlatformWindowInsets.current.systemBars
        } else {
            PlatformInsets.Zero
        }

        val ime = if (useSoftwareKeyboardInset) {
            LocalPlatformWindowInsets.current.ime
        } else {
            PlatformInsets.Zero
        }

        return safeInsets.union(ime)
    }

@Composable
private fun rememberDialogMeasurePolicy(
    layer: ComposeSceneLayer,
    properties: DialogProperties,
    containerSize: IntSize
): MeasurePolicy {
    val platformInsets = properties.platformInsets
    return remember(layer, properties, containerSize, platformInsets) {
        RootMeasurePolicy(
            platformInsets = platformInsets,
            usePlatformDefaultWidth = properties.usePlatformDefaultWidth
        ) { contentSize ->
            val positionWithInsets =
                positionWithInsets(platformInsets, containerSize) { sizeWithoutInsets ->
                    sizeWithoutInsets.center - contentSize.center
                }
            layer.boundsInWindow = IntRect(positionWithInsets, contentSize)
            layer.calculateLocalPosition(positionWithInsets)
        }
    }
}

internal fun getDialogScrimBlendMode(isWindowTransparent: Boolean) =
    if (isWindowTransparent) {
        // Use background alpha channel to respect transparent window shape.
        BlendMode.SrcAtop
    } else {
        BlendMode.SrcOver
    }

private fun CoroutineContext.durationScale(): Float {
    return this[MotionDurationScale]?.scaleFactor ?: 1f
}