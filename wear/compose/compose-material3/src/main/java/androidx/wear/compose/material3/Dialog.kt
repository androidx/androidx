/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.MotionScheme.Companion.standard
import androidx.wear.compose.materialcore.isRoundDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A base dialog component used by [AlertDialog] and [ConfirmationDialog] variations. This dialog
 * provides a full-screen experience with custom entry/exit animations. The parent window will also
 * be scaled, if the dialog is launched from within an AppScaffold scope.
 *
 * Below API level 36, content of the dialog is displayed within a [SwipeToDismissBox] to detect
 * swipe gestures.
 *
 * API level 36 onwards, [Dialog] listens to platform predictive back events for dismissal, and
 * [SwipeToDismissBox] is not used for swipe gesture detection.
 *
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task.
 *
 * The caller should consider whether timeText or scrollIndicator are needed on this dialog, in this
 * case they should provide that in their content by using ScreenScaffold (with suitable scrollState
 * if that's required).
 *
 * @param visible A boolean value that determines whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right. Implementation of this lambda must remove the dialog from the composition hierarchy e.g.
 *   by setting [visible] to false.
 * @param modifier Modifier to be applied to the dialog content.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 *   When the system status bar is enabled on the device, [DialogProperties.usePlatformDefaultWidth]
 *   and [DialogProperties.decorFitsSystemWindows] are overridden to false to ensure edge-to-edge
 *   full width display.
 * @param content A composable function that defines the content of the dialog.
 */
@Composable
public fun Dialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        PredictiveBackDialog(
            visible = visible,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            content = content,
        )
    } else {
        SwipeToDismissDialog(
            visible = visible,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            content = content,
        )
    }
}

/**
 * [Dialog] implementation used from API 36, which dismisses the dialog in response to platform
 * predictive back events, so that the system can recognize the edge swipe gesture and provide its
 * own threshold haptics.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Composable
internal fun PredictiveBackDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val currentVisible by rememberUpdatedState(visible)
    val isReduceMotionEnabled = LocalReduceMotion.current
    val predictiveBackProgress = remember { Animatable(0f) }
    var inPredictiveBack by remember { mutableStateOf(false) }
    var isCommitting by remember { mutableStateOf(false) }

    // Reset the predictive back progress each time the dialog is shown, because a committed
    // gesture leaves it at 1f once the dialog has been dismissed.
    LaunchedEffect(visible) {
        if (visible) {
            isCommitting = false
            predictiveBackProgress.snapTo(0f)
        }
    }

    BaseDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
        gestureProgress = { predictiveBackProgress.value },
        isGestureInProgress = {
            inPredictiveBack || isCommitting || predictiveBackProgress.value > 0f
        },
    ) {
        val scope = rememberCoroutineScope()
        PredictiveBackHandler(visible && properties.dismissOnBackPress) { backEvent ->
            if (isCommitting) {
                backEvent.collect {}
                return@PredictiveBackHandler
            }
            // Phase 1 (gesture tracking): runs in PredictiveBackHandler's job and uses try/catch
            // to reset progress to 0f if the gesture is cancelled before commit.
            inPredictiveBack = true
            try {
                predictiveBackProgress.snapTo(0f)
                backEvent.collect { predictiveBackProgress.snapTo(it.progress) }
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    if (isReduceMotionEnabled) {
                        predictiveBackProgress.snapTo(0f)
                    } else {
                        predictiveBackProgress.animateTo(0f, TRANSITION_ANIMATION_SPEC)
                    }
                }
                throw e
            } finally {
                inPredictiveBack = false
            }
            if (!currentVisible) return@PredictiveBackHandler
            // Phase 2 (committal): runs in a separate scope (try/finally only) so handler
            // cancellation cannot abort the settle animation or onDismissed().
            isCommitting = true
            scope.launch {
                try {
                    if (isReduceMotionEnabled) {
                        predictiveBackProgress.snapTo(1f)
                    } else {
                        predictiveBackProgress.animateTo(1f, TRANSITION_ANIMATION_SPEC)
                    }
                    if (currentVisible) {
                        onDismissed()
                    }
                } finally {
                    isCommitting = false
                }
            }
        }

        val isRoundDevice = isRoundDevice()
        Box(
            modifier =
                coordinatorModifier
                    .graphicsLayer {
                        // At rest, progress is 0f, so the dialog is not translated or scaled.
                        val progress = predictiveBackProgress.value
                        val backScale = lerp(1f, PredictiveBackMinScale, progress)
                        translationX = progress * size.width
                        scaleX = scale * backScale
                        scaleY = scale * backScale
                        alpha = contentAlpha
                    }
                    .then(modifier)
                    .fillMaxSize()
        ) {
            DialogContentContainer(
                modifier = Modifier.clip(if (isRoundDevice) CircleShape else RectangleShape),
                content = content,
            )
        }
    }
}

/**
 * [Dialog] implementation used below API 36, which displays the dialog content within a
 * [SwipeToDismissBox] to detect swipe gestures.
 */
@Composable
internal fun SwipeToDismissDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val screenWidthPx = screenWidthPx()

    BaseDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
        gestureProgress = { swipeToDismissBoxState.offset / screenWidthPx },
        isGestureInProgress = { isSwipeInProgress(swipeToDismissBoxState.offset) },
    ) {
        SwipeToDismissBox(
            state = swipeToDismissBoxState,
            modifier =
                coordinatorModifier.then(modifier).graphicsLayer {
                    alpha = contentAlpha
                    scaleX = scale
                    scaleY = scale
                },
            onDismissed = onDismissed,
        ) { isBackground ->
            if (!isBackground) {
                DialogContentContainer(content = content)
            }
        }
    }
}

/**
 * Shared implementation of [Dialog], which is responsible for showing the platform dialog window,
 * running the show/hide transition, scaling the parent [AppScaffold] and coordinating the status
 * bar. The gesture-specific dismiss handling is provided by [content].
 *
 * @param visible Whether the dialog should be displayed.
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param properties [DialogProperties] for configuring the dialog's behavior.
 * @param gestureProgress Returns the progress of the dismiss gesture, from 0f (at rest) to 1f
 *   (fully dismissed), or [Float.NaN] if the progress is not yet known. Used to scale the parent
 *   [AppScaffold] while the dialog is being dismissed.
 * @param isGestureInProgress Returns whether the dismiss gesture is in progress, during which the
 *   content is rendered offscreen.
 * @param content The dialog window content, which should apply
 *   [DialogContentScope.coordinatorModifier], [DialogContentScope.contentAlpha] and
 *   [DialogContentScope.scale], and display the content within
 *   [DialogContentScope.DialogContentContainer].
 */
@Composable
private fun BaseDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    gestureProgress: () -> Float,
    isGestureInProgress: () -> Boolean,
    content: @Composable DialogContentScope.() -> Unit,
) {
    val showState by rememberUpdatedState(visible)

    // Transitions for dialog animation.
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(DialogVisibility.Hide))
    }
    val shouldShow = showState || transitionState.currentState == DialogVisibility.Display
    val transition = rememberTransition(transitionState)

    val scaffoldState = LocalScaffoldState.current as? AppScaffoldState
    val backgroundAnimatable = remember { Animatable(1f) }

    val backgroundAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>().faster(50f)

    val isReduceMotionEnabled = LocalReduceMotion.current

    if (!isReduceMotionEnabled) {
        LaunchedEffect(Unit) {
            launch {
                snapshotFlow(gestureProgress)
                    .filter { !it.isNaN() }
                    .collectLatest {
                        val scale = lerp(BackgroundMinScale, BackgroundMaxScale, it)
                        if (transitionState.currentState == DialogVisibility.Display) {
                            scaffoldState?.parentScale?.floatValue = scale
                            backgroundAnimatable.snapTo(scale)
                        }
                    }
            }

            snapshotFlow { showState }
                .collectLatest {
                    backgroundAnimatable.animateTo(
                        if (it) BackgroundMinScale else BackgroundMaxScale,
                        backgroundAnimationSpec,
                    ) {
                        scaffoldState?.parentScale?.floatValue = value
                    }
                }
        }
    }

    if (shouldShow) {
        val isStatusBarEnabled = LocalStatusBarEnabled.current
        val dialogProperties =
            if (isStatusBarEnabled) {
                // When GSB is enabled, force edge-to-edge and full width
                // while preserving all other properties passed by the developer.
                DialogProperties(
                    dismissOnBackPress = properties.dismissOnBackPress,
                    dismissOnClickOutside = properties.dismissOnClickOutside,
                    securePolicy = properties.securePolicy,
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    windowTitle = properties.windowTitle,
                )
            } else {
                properties
            }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
            properties = dialogProperties,
        ) {
            // Disable System dialog animations
            val view = LocalView.current
            val dialogWindowProvider = view.parent as DialogWindowProvider
            dialogWindowProvider.window.setWindowAnimations(android.R.style.Animation)
            dialogWindowProvider.window.setDimAmount(0f)

            val statusBarState =
                rememberScaffoldStatusBarState(unsuppressedStatusBarMode = StatusBarMode.Disabled)

            // Dialog runs in a separate Android window (`DialogWindowProvider`), so its Compose
            // hierarchy is separate and cannot cross window boundaries.
            // statusBarState.coordinatorModifier acts as a terminal coordinator within the dialog,
            // allowing descendant Modifier.suppressStatusBar() nodes to coordinate locally rather
            // than falling back to creating redundant standalone StatusBarOrchestrator instances.
            // When hosted within an AppScaffold, Dialog registers its window layer with
            // AppScaffoldState.screenContent; otherwise it falls back to window orchestration.
            if (scaffoldState is AppScaffoldState) {
                StatusBarRegistration(
                    showStatusBar = statusBarState.showStatusBar,
                    scaffoldState = scaffoldState,
                )
            } else {
                StandaloneStatusBarSuppression(isSuppressed = true)
            }

            val contentAlpha = animateContentAlpha(transition)
            val scale = animateDialogScale(transition)
            // Only changes when the strategy flips, so gesture frames don't re-run the layer block.
            val contentCompositingStrategy =
                remember(transition) {
                    derivedStateOf {
                        dialogContentCompositingStrategy(
                            isTransitionRunning = transition.isRunning,
                            isGestureInProgress = isGestureInProgress(),
                        )
                    }
                }

            val dialogContentScope =
                DialogContentScope(
                    coordinatorModifier = statusBarState.coordinatorModifier,
                    contentAlphaState = contentAlpha,
                    scaleState = scale,
                    contentCompositingStrategyState = contentCompositingStrategy,
                    onDismissed = {
                        onDismissRequest()
                        // Reset state for the next time this dialog is shown.
                        transitionState = MutableTransitionState(DialogVisibility.Hide)
                    },
                )

            CompositionLocalProvider(LocalScaffoldStatusBarState provides statusBarState) {
                dialogContentScope.content()
            }
        }
        LaunchedEffect(visible) {
            if (visible) {
                // a) Fade out previous screen contents b) Scale down dialog contents from 125%
                transitionState.targetState = DialogVisibility.Display
            } else {
                // a) Fade out dialog contents b) Scale up dialog contents.
                transitionState.targetState = DialogVisibility.Hide
            }
        }
    }

    // We want to be sure that background is scaled back to 1f after dialog is disposed.
    DisposableEffect(Unit) { onDispose { scaffoldState?.parentScale?.floatValue = 1f } }
}

/**
 * Scope for the gesture-specific window content of [BaseDialog].
 *
 * [contentAlpha] and [scale] are backed by the dialog's show/hide transition, so they should be
 * read in the draw phase, e.g. in a [graphicsLayer] block.
 */
private class DialogContentScope(
    /** The status bar coordinator modifier, which should be applied to the outermost layout. */
    val coordinatorModifier: Modifier,
    private val contentAlphaState: State<Float>,
    private val scaleState: State<Float>,
    private val contentCompositingStrategyState: State<CompositingStrategy>,
    /** Called when the dismiss gesture completes. */
    val onDismissed: () -> Unit,
) {
    /** The alpha of the dialog content, from the show/hide transition. */
    val contentAlpha: Float
        get() = contentAlphaState.value

    /** The scale of the dialog content, from the show/hide transition. */
    val scale: Float
        get() = scaleState.value

    /**
     * Displays [content] on the dialog background, filling the parent [Box].
     *
     * @param modifier Modifier applied before the background, e.g. to clip it.
     * @param content The dialog content.
     */
    @Composable
    fun BoxScope.DialogContentContainer(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(
            modifier =
                Modifier.matchParentSize()
                    .then(modifier)
                    .background(MaterialTheme.colorScheme.background)
                    .graphicsLayer {
                        compositingStrategy = contentCompositingStrategyState.value
                    }
        ) {
            content()
        }
    }
}

@Composable
private fun screenWidthPx(): Float =
    with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

@Composable
private fun animateContentAlpha(transition: Transition<DialogVisibility>): State<Float> {
    val dialogAlphaAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>().faster(50f)
    return transition.animateFloat(
        transitionSpec = {
            if (LocalReduceMotion.current) SnapSpec()
            else
                when (transition.targetState) {
                    DialogVisibility.Display -> dialogAlphaAnimationSpec
                    DialogVisibility.Hide -> standard().fastEffectsSpec()
                }
        },
        label = "background-scrim-alpha",
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 0f
            DialogVisibility.Display -> 1f
        }
    }
}

@Composable
private fun animateDialogScale(transition: Transition<DialogVisibility>): State<Float> {
    val dialogAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>().faster(50f)
    return transition.animateFloat(
        transitionSpec = {
            if (LocalReduceMotion.current) SnapSpec()
            else
                when (transition.targetState) {
                    DialogVisibility.Display -> dialogAnimationSpec
                    DialogVisibility.Hide -> dialogAnimationSpec
                }
        },
        label = "scale",
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 1.25f
            DialogVisibility.Display -> 1.0f
        }
    }
}

/**
 * Returns the [CompositingStrategy] for the full-screen layer around the [Dialog] content.
 *
 * Rendering the content into an offscreen buffer keeps text from jittering while the content is
 * scaled by the dismiss gesture or by the show/hide animation (b/380901500). It is not needed while
 * the dialog is at rest, where it would hold a full-screen GPU layer for as long as the dialog is
 * shown.
 *
 * @param isTransitionRunning Whether the dialog show/hide transition is running.
 * @param isGestureInProgress Whether the content is being transformed by the dismiss gesture,
 *   either swipe-to-dismiss or predictive back.
 */
internal fun dialogContentCompositingStrategy(
    isTransitionRunning: Boolean,
    isGestureInProgress: Boolean,
): CompositingStrategy =
    if (isTransitionRunning || isGestureInProgress) {
        CompositingStrategy.Offscreen
    } else {
        CompositingStrategy.Auto
    }

/**
 * Returns whether the [SwipeToDismissBox] content is being swiped, or is animating after a swipe.
 *
 * @param swipeOffset The current swipe-to-dismiss offset, or [Float.NaN] if it has not been
 *   initialized yet.
 */
internal fun isSwipeInProgress(swipeOffset: Float): Boolean =
    !swipeOffset.isNaN() && swipeOffset != 0f

private enum class DialogVisibility {
    Hide,
    Display,
}

private const val BackgroundMinScale = 0.85f
private const val BackgroundMaxScale = 1f

// Scale of the dialog content when the predictive back gesture is fully committed, matching the
// Navigation 3 pop exit transition.
private const val PredictiveBackMinScale = 0.8f

private val TRANSITION_ANIMATION_SPEC =
    spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
