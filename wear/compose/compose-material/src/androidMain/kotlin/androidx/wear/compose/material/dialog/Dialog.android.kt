/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

/**
 * [Dialog] displays a full-screen dialog, layered over any other content. It takes a single slot,
 * which is expected to be an opinionated Wear dialog content, such as [Alert]
 * or [Confirmation].
 *
 * The dialog supports swipe-to-dismiss and reveals the parent content in the background
 * during the swipe gesture.
 *
 * Example of content using [Dialog] to trigger an alert dialog using [Alert]:
 * @sample androidx.wear.compose.material.samples.AlertDialogSample
 *
 * Example of content using [Dialog] to trigger a confirmation dialog using
 * [Confirmation]:
 * @sample androidx.wear.compose.material.samples.ConfirmationDialogSample

 * @param showDialog Controls whether to display the [Dialog]. Set to true initially to trigger
 * an 'intro' animation and display the [Dialog]. Subsequently, setting to false triggers
 * an 'outro' animation, then [Dialog] calls [onDismissRequest] and hides itself.
 * @param onDismissRequest Executes when the user dismisses the dialog.
 * Must remove the dialog from the composition.
 * @param modifier Modifier to be applied to the dialog.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed.
 * @param properties Typically platform specific properties to further configure the dialog.
 * @param content Slot for dialog content such as [Alert] or [Confirmation].
 */
@Composable
public fun Dialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScalingLazyListState? = rememberScalingLazyListState(),
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    // Transitions for background and 'dialog content' alpha.
    var alphaTransitionState by remember {
        mutableStateOf(MutableTransitionState(AlphaStage.IntroFadeOut))
    }
    val alphaTransition = updateTransition(alphaTransitionState)

    // Transitions for dialog content scaling.
    var scaleTransitionState by remember {
        mutableStateOf(MutableTransitionState(ScaleStage.Intro))
    }
    val scaleTransition = updateTransition(scaleTransitionState)

    if (showDialog ||
        alphaTransitionState.targetState != AlphaStage.IntroFadeOut ||
        scaleTransitionState.targetState != ScaleStage.Intro) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val backgroundAlpha by animateBackgroundAlpha(alphaTransition, alphaTransitionState)
            val alpha by animateDialogAlpha(alphaTransition, alphaTransitionState)
            val scale by animateDialogScale(scaleTransition, scaleTransitionState)

            Scaffold(
                vignette = {
                    AnimatedVisibility(
                        visible = scaleTransitionState.targetState == ScaleStage.Display,
                        enter = fadeIn(animationSpec =
                            TweenSpec(durationMillis = CASUAL, easing = STANDARD_IN)
                        ),
                        exit = fadeOut(animationSpec =
                            TweenSpec(durationMillis = CASUAL, easing = STANDARD_OUT)
                        ),
                    ) {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    }
                },
                positionIndicator = { if (scrollState != null) PositionIndicator(scrollState) },
                modifier = modifier,
            ) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(),
                    modifier = Modifier.graphicsLayer(
                        alpha = alpha,
                        scaleX = scale,
                        scaleY = scale,
                    ),
                    onDismissed = {
                        onDismissRequest()
                        // Reset state for the next time this dialog is shown.
                        alphaTransitionState = MutableTransitionState(AlphaStage.IntroFadeOut)
                        scaleTransitionState = MutableTransitionState(ScaleStage.Intro)
                    }
                ) { isBackground ->
                    Box(
                        modifier = Modifier.matchParentSize().background(
                            MaterialTheme.colors.background.copy(alpha = backgroundAlpha))
                    )
                    if (!isBackground) content()
                }
            }

            SideEffect {
                // Trigger initial Intro animation
                if (alphaTransitionState.currentState == AlphaStage.IntroFadeOut) {
                    // a) Fade out previous screen contents b) Scale down dialog contents.
                    alphaTransitionState.targetState = AlphaStage.IntroFadeIn
                    scaleTransitionState.targetState = ScaleStage.Display
                } else if (alphaTransitionState.currentState == AlphaStage.IntroFadeIn) {
                    // Now conclude the Intro animation by fading in the dialog contents.
                    alphaTransitionState.targetState = AlphaStage.Display
                }
            }

            // Trigger Outro animation when the caller updates showDialog to false.
            LaunchedEffect(showDialog) {
                if (!showDialog) {
                    // a) Fade out dialog contents b) Scale up dialog contents.
                    alphaTransitionState.targetState = AlphaStage.OutroFadeOut
                    scaleTransitionState.targetState = ScaleStage.Outro
                }
            }

            LaunchedEffect(alphaTransitionState.currentState) {
                if (alphaTransitionState.currentState == AlphaStage.OutroFadeOut) {
                    // Conclude the Outro animation by fading in the background contents.
                    alphaTransitionState.targetState = AlphaStage.OutroFadeIn
                } else if (alphaTransitionState.currentState == AlphaStage.OutroFadeIn) {
                    // After the outro animation, leave the dialog & reset alpha/scale transitions.
                    onDismissRequest()
                    alphaTransitionState = MutableTransitionState(AlphaStage.IntroFadeOut)
                    scaleTransitionState = MutableTransitionState(ScaleStage.Intro)
                }
            }
        }
    }
}

@Composable
private fun animateBackgroundAlpha(
    alphaTransition: Transition<AlphaStage>,
    alphaTransitionState: MutableTransitionState<AlphaStage>
) = alphaTransition.animateFloat(
    transitionSpec = {
        if (alphaTransitionState.currentState == AlphaStage.IntroFadeOut)
            tween(durationMillis = RAPID, easing = STANDARD_OUT)
        else if (alphaTransitionState.targetState == AlphaStage.OutroFadeIn)
            tween(durationMillis = QUICK, easing = STANDARD_IN)
        else
            tween(durationMillis = 0)
    },
    label = "background-alpha"
) { stage ->
    when (stage) {
        AlphaStage.IntroFadeOut -> 0.0f
        AlphaStage.IntroFadeIn -> 0.9f
        AlphaStage.Display -> 1.0f
        AlphaStage.OutroFadeOut -> 0.9f
        AlphaStage.OutroFadeIn -> 0.0f
    }
}

@Composable
private fun animateDialogAlpha(
    alphaTransition: Transition<AlphaStage>,
    alphaTransitionState: MutableTransitionState<AlphaStage>
) = alphaTransition.animateFloat(
    transitionSpec = {
        if (alphaTransitionState.currentState == AlphaStage.IntroFadeIn)
            tween(durationMillis = QUICK, easing = STANDARD_IN)
        else if (alphaTransitionState.targetState == AlphaStage.OutroFadeOut)
            tween(durationMillis = RAPID, easing = STANDARD_OUT)
        else
            tween(durationMillis = 0)
    },
    label = "alpha"
) { stage ->
    when (stage) {
        AlphaStage.IntroFadeOut -> 0.0f
        AlphaStage.IntroFadeIn -> 0.1f
        AlphaStage.Display -> 1.0f
        AlphaStage.OutroFadeOut -> 0.1f
        AlphaStage.OutroFadeIn -> 0.0f
    }
}

@Composable
private fun animateDialogScale(
    scaleTransition: Transition<ScaleStage>,
    scaleTransitionState: MutableTransitionState<ScaleStage>
) = scaleTransition.animateFloat(
    transitionSpec = {
        if (scaleTransitionState.currentState == ScaleStage.Intro)
            tween(durationMillis = CASUAL, easing = STANDARD_IN)
        else
            tween(durationMillis = CASUAL, easing = STANDARD_OUT)
    },
    label = "scale"
) { stage ->
    when (stage) {
        ScaleStage.Intro -> 1.25f
        ScaleStage.Display -> 1.0f
        ScaleStage.Outro -> 1.25f
    }
}

// Alpha transition stages - Intro and Outro are split into FadeIn/FadeOut stages.
private enum class AlphaStage {
    IntroFadeOut, IntroFadeIn, Display, OutroFadeOut, OutroFadeIn;
}

// Scale transition stages - scaling is applied as single Intro/Outro animations.
private enum class ScaleStage {
    Intro, Display, Outro;
}

private const val RAPID = 150
private const val QUICK = 250
private const val CASUAL = 400
private val STANDARD_IN = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val STANDARD_OUT = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
