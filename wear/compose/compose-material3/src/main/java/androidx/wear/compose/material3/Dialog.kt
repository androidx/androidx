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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.tokens.MotionTokens

/**
 * This is a base dialog component used by [AlertDialog] and [Confirmation] variations. Dialogs
 * provide important prompts in a user flow. They can require an action, communicate information, or
 * help users accomplish a task.
 *
 * @param show A boolean value that determines whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right.
 * @param modifier Modifier to be applied to the dialog content.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A composable function that defines the content of the dialog.
 */
@Composable
internal fun Dialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    // Transitions for dialog animation.
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(DialogVisibility.Hide))
    }
    val transition = rememberTransition(transitionState)

    if (show || transition.currentState == DialogVisibility.Display) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val backgroundScrimAlpha by animateBackgroundScrimAlpha(transition)
            val contentAlpha by animateContentAlpha(transition)
            val scale by animateDialogScale(transition)
            ScreenScaffold(
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
                                    .background(MaterialTheme.colorScheme.background)
                        ) {
                            content()
                        }
                    }
                }
            }
            LaunchedEffect(show) {
                if (show) {
                    // a) Fade out previous screen contents b) Scale down dialog contents from 125%
                    transitionState.targetState = DialogVisibility.Display
                } else {
                    // a) Fade out dialog contents b) Scale up dialog contents.
                    transitionState.targetState = DialogVisibility.Hide
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
                    tween(
                        durationMillis = (MotionTokens.DurationShort3 / 0.9f).toInt(),
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
                DialogVisibility.Hide ->
                    keyframes {
                        // Outro
                        durationMillis = MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                        1f at 0
                        0.9f at
                            MotionTokens.DurationShort3 using
                            MotionTokens.EasingLegacyDecelerate
                        0.0f at MotionTokens.DurationShort3 + MotionTokens.DurationMedium1
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
                        durationMillis = MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                        0.0f at 0
                        0.1f at
                            MotionTokens.DurationShort3 using
                            MotionTokens.EasingLegacyDecelerate
                        1f at MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                    }
                DialogVisibility.Hide ->
                    tween(
                        durationMillis = (MotionTokens.DurationShort3 / 0.9f).toInt(),
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
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
                DialogVisibility.Display ->
                    tween(
                        durationMillis = MotionTokens.DurationMedium4,
                        easing = MotionTokens.EasingLegacyDecelerate
                    )
                DialogVisibility.Hide ->
                    tween(
                        durationMillis = MotionTokens.DurationMedium4,
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
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
