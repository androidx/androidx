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
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.CASUAL
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.QUICK
import androidx.wear.compose.material.RAPID
import androidx.wear.compose.material.STANDARD_IN
import androidx.wear.compose.material.STANDARD_OUT
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

/**
 * [Dialog] displays a full-screen dialog, layered over any other content. It takes a single slot,
 * which is expected to be an opinionated Wear dialog content, such as [Alert] or [Confirmation].
 *
 * The dialog supports swipe-to-dismiss and reveals the parent content in the background during the
 * swipe gesture.
 *
 * Example of content using [Dialog] to trigger an alert dialog using [Alert]:
 *
 * @sample androidx.wear.compose.material.samples.AlertDialogSample
 *
 * Example of content using [Dialog] to trigger a confirmation dialog using [Confirmation]:
 *
 * @sample androidx.wear.compose.material.samples.ConfirmationDialogSample
 * @param showDialog Controls whether to display the [Dialog]. Set to true initially to trigger an
 *   'intro' animation and display the [Dialog]. Subsequently, setting to false triggers an 'outro'
 *   animation, then [Dialog] calls [onDismissRequest] and hides itself.
 * @param onDismissRequest Executes when the user dismisses the dialog. Must remove the dialog from
 *   the composition.
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
    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        positionIndicator = { if (scrollState != null) PositionIndicator(scrollState) },
        content = content
    )
}

/**
 * [Dialog] displays a full-screen dialog, layered over any other content. It takes a single slot,
 * which is expected to be an opinionated Wear dialog content, such as [Alert] or [Confirmation].
 *
 * The dialog supports swipe-to-dismiss and reveals the parent content in the background during the
 * swipe gesture.
 *
 * Example of content using [Dialog] to trigger an alert dialog using [Alert]:
 *
 * @sample androidx.wear.compose.material.samples.AlertDialogSample
 *
 * Example of content using [Dialog] to trigger a confirmation dialog using [Confirmation]:
 *
 * @sample androidx.wear.compose.material.samples.ConfirmationDialogSample
 * @param showDialog Controls whether to display the [Dialog]. Set to true initially to trigger an
 *   'intro' animation and display the [Dialog]. Subsequently, setting to false triggers an 'outro'
 *   animation, then [Dialog] calls [onDismissRequest] and hides itself.
 * @param onDismissRequest Executes when the user dismisses the dialog. Must remove the dialog from
 *   the composition.
 * @param modifier Modifier to be applied to the dialog.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed.
 * @param properties Typically platform specific properties to further configure the dialog.
 * @param content Slot for dialog content such as [Alert] or [Confirmation].
 */
@Suppress("DEPRECATION")
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
        "A newer overload is available which uses ScalingLazyListState from " +
        "wear.compose.foundation.lazy package",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun Dialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: androidx.wear.compose.material.ScalingLazyListState? =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        positionIndicator = { if (scrollState != null) PositionIndicator(scrollState) },
        content = content
    )
}

/**
 * A Dialog composable which was created for sharing code between 2 versions of public [Dialog]s -
 * with ScalingLazyListState from material and another from foundation.lazy
 */
@Composable
private fun Dialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    positionIndicator: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    // Transitions for dialog animation.
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(DialogVisibility.Hide))
    }
    val transition = rememberTransition(transitionState)

    var pendingOnDismissCall by remember { mutableStateOf(false) }

    if (showDialog || transition.currentState == DialogVisibility.Display) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val backgroundScrimAlpha by animateBackgroundScrimAlpha(transition)
            val contentAlpha by animateContentAlpha(transition)
            val scale by animateDialogScale(transition)
            Scaffold(
                vignette = {
                    AnimatedVisibility(
                        visible = transition.targetState == DialogVisibility.Display,
                        enter =
                            fadeIn(
                                animationSpec =
                                    TweenSpec(durationMillis = CASUAL, easing = STANDARD_IN)
                            ),
                        exit =
                            fadeOut(
                                animationSpec =
                                    TweenSpec(durationMillis = CASUAL, easing = STANDARD_OUT)
                            ),
                    ) {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    }
                },
                modifier = modifier,
            ) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(),
                    modifier =
                        Modifier.graphicsLayer(
                            alpha = backgroundScrimAlpha,
                            scaleX = scale,
                            scaleY = scale,
                        ),
                    onDismissed = {
                        onDismissRequest()
                        // Reset state for the next time this dialog is shown.
                        transitionState = MutableTransitionState(DialogVisibility.Hide)
                    }
                ) { isBackground ->
                    if (!isBackground) {
                        Box(
                            modifier =
                                Modifier.matchParentSize()
                                    .graphicsLayer(alpha = contentAlpha)
                                    .background(MaterialTheme.colors.background)
                        ) {
                            content()
                            positionIndicator()
                        }
                    }
                }
            }
            LaunchedEffect(showDialog) {
                if (showDialog) {
                    // a) Fade out previous screen contents b) Scale down dialog contents from 125%
                    transitionState.targetState = DialogVisibility.Display
                    pendingOnDismissCall = true
                } else {
                    // a) Fade out dialog contents b) Scale up dialog contents.
                    transitionState.targetState = DialogVisibility.Hide
                }
            }

            LaunchedEffect(transitionState.currentState) {
                if (
                    pendingOnDismissCall &&
                        transitionState.currentState == DialogVisibility.Hide &&
                        transitionState.isIdle
                ) {
                    // After the outro animation, leave the dialog & reset alpha/scale transitions.
                    onDismissRequest()
                    pendingOnDismissCall = false
                }
            }
        }
    }
}

@Composable
private fun animateBackgroundScrimAlpha(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display ->
                    tween(durationMillis = (RAPID / 0.9f).toInt(), easing = STANDARD_OUT)
                DialogVisibility.Hide ->
                    keyframes {
                        // Outro
                        durationMillis = QUICK + RAPID
                        1f at 0
                        0.9f at RAPID using STANDARD_IN
                        0.0f at RAPID + QUICK
                    }
            }
        },
        label = "background-scrim-alpha"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 0f
            DialogVisibility.Display -> 1f
        }
    }

@Composable
private fun animateContentAlpha(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display ->
                    keyframes {
                        // Intro
                        durationMillis = QUICK + RAPID
                        0.0f at 0
                        0.1f at RAPID using STANDARD_IN
                        1f at RAPID + QUICK
                    }
                DialogVisibility.Hide ->
                    tween(durationMillis = (RAPID / 0.9f).toInt(), easing = STANDARD_OUT)
            }
        },
        label = "content-alpha"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 0f
            DialogVisibility.Display -> 1f
        }
    }

@Composable
private fun animateDialogScale(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display -> tween(durationMillis = CASUAL, easing = STANDARD_IN)
                DialogVisibility.Hide -> tween(durationMillis = CASUAL, easing = STANDARD_OUT)
            }
        },
        label = "scale"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 1.25f
            DialogVisibility.Display -> 1.0f
        }
    }

private enum class DialogVisibility {
    Hide,
    Display
}
