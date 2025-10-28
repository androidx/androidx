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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.animation.easeOutTimingFunction
import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.isDialogAnimationEnabled
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.exclude
import androidx.compose.ui.platform.findDefaultNavigationEventDispatcherOwner
import androidx.compose.ui.platform.union
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.Content
import androidx.compose.ui.scene.rememberComposeSceneLayer
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import kotlin.coroutines.CoroutineContext
import kotlin.getValue
import kotlin.setValue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
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
 */
@Immutable
actual class DialogProperties(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false
        if (useSoftwareKeyboardInset != other.useSoftwareKeyboardInset) return false
        if (scrimColor != other.scrimColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
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
    val onBackHandler = remember {
        OnBackClickEventHandler { currentOnDismissRequest() }
    }
    LaunchedEffect(onBackHandler, properties.dismissOnBackPress) {
        onBackHandler.backClickIsEnabled = properties.dismissOnBackPress
    }
    val navigationEventDispatcher =
        requireNotNull(findDefaultNavigationEventDispatcherOwner()) {
            error("NavigationEventDispatcherOwner not found")
        }.navigationEventDispatcher
    DisposableEffect(navigationEventDispatcher) {
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
    val currentContent by rememberUpdatedState(content)
    val compositionContext = rememberCompositionContext()
    val layer = rememberComposeSceneLayer(focusable = true)
    layer.setOutsidePointerEventListener(onOutsidePointerEvent)

    val animator = remember {
        DialogAppearanceController(layer = layer, coroutineContext = compositionContext.effectCoroutineContext)
    }
    animator.scrimColor = properties.scrimColor

    layer.Content {
        LaunchedEffect(Unit) {
            animator.onDialogShown()
        }

        val platformInsets = properties.platformInsets
        val containerSize = LocalWindowInfo.current.containerSize
        val measurePolicy = rememberDialogMeasurePolicy(
            layer = layer,
            properties = properties,
            containerSize = containerSize,
            platformInsets = platformInsets
        )

        LocalPlatformWindowInsets.current.exclude(
            safeInsets = properties.usePlatformInsets,
            ime = properties.useSoftwareKeyboardInset
        ) {
            Layout(
                content = currentContent,
                modifier = animator.modifier.then(modifier),
                measurePolicy = measurePolicy
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            animator.hideDialog()
        }
    }
}

private interface DialogAppearanceController {
    var scrimColor: Color?
    val modifier: Modifier
    fun onDialogShown()
    fun hideDialog()
}

private fun DialogAppearanceController(
    layer: ComposeSceneLayer,
    coroutineContext: CoroutineContext
): DialogAppearanceController =
    if (ComposeUiFlags.isDialogAnimationEnabled) {
        AnimatedDialogAppearanceController(layer, coroutineContext)
    } else {
        NonAnimatedDialogAppearanceController(layer)
    }

private class AnimatedDialogAppearanceController(
    private val layer: ComposeSceneLayer,
    private val coroutineContext: CoroutineContext
) : DialogAppearanceController {
    private val appearanceProgress = mutableStateOf(0f)
    private var appearAnimationJob: Job? = null

    override var modifier by mutableStateOf(
        Modifier.animationLayerTransform(appearanceProgress)
    )
        private set

    override var scrimColor: Color? = Color.Transparent
        set(value) {
            field = value
            updateScrimLayerColor()
        }

    override fun onDialogShown() {
        appearAnimationJob =
            CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
                withAnimationProgress(
                    duration = (durationScale() * AnimatedLayerAppearanceDuration).seconds,
                    timingFunction = ::easeOutTimingFunction
                ) { progress ->
                    appearanceProgress.value = progress
                    updateScrimLayerColor()
                }

                modifier = Modifier
                layer.scrimColor = scrimColor
            }
    }

    override fun hideDialog() {
        appearAnimationJob?.cancel()
        CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
            val initialProgress = appearanceProgress.value
            val duration =
                durationScale() * initialProgress * AnimatedLayerDisappearanceDuration
            modifier = Modifier.animationLayerTransform(appearanceProgress)

            withAnimationProgress(
                duration = duration.seconds,
                timingFunction = ::easeOutTimingFunction
            ) { progress ->
                appearanceProgress.value = (1f - progress) * initialProgress
                updateScrimLayerColor()
            }

            layer.close()
        }
    }

    private fun updateScrimLayerColor() {
        layer.scrimColor = scrimColor?.let {
            it.copy(it.alpha * contentAlpha(appearanceProgress.value))
        }
    }

    private fun contentAlpha(progress: Float): Float =
        AnimatedLayerInitialAlpha + (1f - AnimatedLayerInitialAlpha) * progress

    private fun durationScale(): Float =
        coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f

    private fun Modifier.animationLayerTransform(progress: State<Float>): Modifier =
        graphicsLayer {
            this.alpha = contentAlpha(progress.value)
            val reversedProgress = 1f - progress.value
            val scale = 1f - reversedProgress * AnimatedLayerScale
            this.scaleX = scale
            this.scaleY = scale
            this.translationY = AnimatedLayerOffsetDp * reversedProgress * density
        }
}

private class NonAnimatedDialogAppearanceController(
    private val layer: ComposeSceneLayer
) : DialogAppearanceController {
    override var scrimColor: Color? by layer::scrimColor
    override fun onDialogShown() {}
    override fun hideDialog() = layer.close()
    override val modifier = Modifier
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
    containerSize: IntSize,
    platformInsets: PlatformInsets
) = remember(layer, properties, containerSize, platformInsets) {
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

internal fun getDialogScrimBlendMode(isWindowTransparent: Boolean) =
    if (isWindowTransparent) {
        // Use background alpha channel to respect transparent window shape.
        BlendMode.SrcAtop
    } else {
        BlendMode.SrcOver
    }
