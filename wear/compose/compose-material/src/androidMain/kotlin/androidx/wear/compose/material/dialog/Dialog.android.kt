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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(DialogStage.Intro))
    }
    val transition = updateTransition(transitionState)
    if (showDialog || transitionState.targetState != DialogStage.Intro) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val backgroundAlpha by transition.animateFloat(
                transitionSpec = {
                    if (transitionState.targetState != DialogStage.Outro)
                        tween(durationMillis = RAPID, easing = STANDARD_OUT)
                    else
                        tween(durationMillis = QUICK, delayMillis = RAPID, easing = STANDARD_IN)
                },
                label = "background-alpha"
            ) { stage ->
                when (stage) {
                    DialogStage.Intro -> 1.0f
                    DialogStage.Display -> 0.1f
                    DialogStage.Outro -> 1.0f
                }
            }

            val alpha by transition.animateFloat(
                transitionSpec = {
                    if (transitionState.targetState != DialogStage.Outro)
                        tween(durationMillis = QUICK, delayMillis = RAPID, easing = STANDARD_IN)
                    else
                        tween(durationMillis = RAPID, easing = STANDARD_OUT)
                },
                label = "alpha"
            ) { stage ->
                when (stage) {
                    DialogStage.Intro -> 0.1f
                    DialogStage.Display -> 1.0f
                    DialogStage.Outro -> 0.1f
                }
            }

            val scale by transition.animateFloat(
                transitionSpec = {
                    if (transitionState.targetState != DialogStage.Outro)
                        tween(durationMillis = CASUAL, easing = STANDARD_IN)
                    else
                        tween(durationMillis = CASUAL, easing = STANDARD_OUT)
                },
                label = "scale"
            ) { stage ->
                when (stage) {
                    DialogStage.Intro -> 1.25f
                    DialogStage.Display -> 1.0f
                    DialogStage.Outro -> 1.25f
                }
            }

            Scaffold(
                vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
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
                        transitionState = MutableTransitionState(DialogStage.Intro)
                    }
                ) { isBackground ->
                    Box(
                        modifier = Modifier.matchParentSize().background(
                            MaterialTheme.colors.background.copy(alpha = backgroundAlpha))
                    )
                    if (!isBackground) content()
                }
            }

            // Trigger initial intro animation when dialog is displayed.
            SideEffect {
                if (transitionState.currentState == DialogStage.Intro) {
                    transitionState.targetState = DialogStage.Display
                }
            }

            // Trigger leaving the Dialog when the caller updates showDialog to false.
            LaunchedEffect(showDialog) {
                if (!showDialog) {
                    transitionState.targetState = DialogStage.Outro
                }
            }

            // After the outro animation, request to leave the dialog and reset stage to Intro.
            LaunchedEffect(transitionState.currentState) {
                if (transitionState.currentState == DialogStage.Outro) {
                    onDismissRequest()
                    transitionState = MutableTransitionState(DialogStage.Intro)
                }
            }
        }
    }
}

private enum class DialogStage {
    Intro, Display, Outro;
}

private const val RAPID = 150
private const val QUICK = 250
private const val CASUAL = 400
private val STANDARD_IN = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val STANDARD_OUT = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
