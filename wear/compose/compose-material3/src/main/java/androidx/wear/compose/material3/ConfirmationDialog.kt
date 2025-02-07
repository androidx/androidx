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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.MotionTokens.DurationShort2
import androidx.wear.compose.material3.tokens.MotionTokens.DurationShort3
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shows a transient [ConfirmationDialog] with an icon and optional very short [curvedText]. The
 * length of the curved text should be very short and should not exceed 1-2 words. If a longer text
 * is required, then the alternative [ConfirmationDialog] overload with slot for linear text should
 * be used instead.
 *
 * The [ConfirmationDialog] shows a message to the user for [durationMillis]. After a specified
 * timeout, the [onDismissRequest] callback will be invoked, where it's up to the caller to handle
 * the dismissal. To hide the confirmation dialog, the [visible] parameter should be set to false.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [ConfirmationDialog].
 *
 * Example of a [ConfirmationDialog] with an icon and a curved text content:
 *
 * @sample androidx.wear.compose.material3.samples.ConfirmationDialogSample
 * @param visible A boolean indicating whether the confirmation dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [ConfirmationDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. This value
 *   will be adjusted by the accessibility manager according to the content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog. It's recommended to
 *   set its size to [ConfirmationDialogDefaults.IconSize]
 */
@Composable
public fun ConfirmationDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
    content: @Composable () -> Unit
): Unit {
    AnimateConfirmationDialog(
        visible = visible,
        performHapticFeedback = null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        containsText = curvedText != null,
        durationMillis = durationMillis,
        content = {
            ConfirmationDialogContent(curvedText = curvedText, colors = colors, content = content)
        }
    )
}

/**
 * This overload of [ConfirmationDialogContent] provides the content for a [ConfirmationDialog] with
 * an icon and optional very short [curvedText]. The length of the curved text should be very short
 * and should not exceed 1-2 words. If a longer text is required, then the alternative
 * [ConfirmationDialog] overload with slot for linear text should be used instead.
 *
 * Prefer using [ConfirmationDialog] directly, which provides built-in animations when
 * showing/hiding the dialog. This composable may be used to provide the content for a confirmation
 * dialog if custom show/hide animations are required.
 *
 * Example of a [ConfirmationDialog] with an icon and a curved text content:
 *
 * @sample androidx.wear.compose.material3.samples.ConfirmationDialogSample
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [ConfirmationDialog].
 * @param content A slot for displaying an icon inside the confirmation dialog. It's recommended to
 *   set its size to [ConfirmationDialogDefaults.IconSize]
 */
@Composable
public fun ConfirmationDialogContent(
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.colors(),
    content: @Composable () -> Unit
): Unit {
    ConfirmationDialogContentWrapper(
        curvedText = curvedText,
        modifier = modifier,
        colors = colors,
        content = {
            IconContainer(
                iconColor = colors.iconColor,
                iconBackground = iconContainer(true, colors.iconContainerColor),
                content = content
            )
        },
    )
}

/**
 * Shows a transient [ConfirmationDialog] with an icon and optional short [text]. The length of the
 * text should not exceed 3 lines. If the text is very short and fits into 1-2 words, consider using
 * the alternative [ConfirmationDialog] overload with the curvedText parameter instead.
 *
 * The confirmation dialog will show a message to the user for [durationMillis]. After a specified
 * timeout, the [onDismissRequest] callback will be invoked, where it's up to the caller to handle
 * the dismissal. To hide the confirmation dialog, [visible] parameter should be set to false.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [ConfirmationDialog].
 *
 * Example of a [ConfirmationDialog] with an icon and a text which fits into 3 lines:
 *
 * @sample androidx.wear.compose.material3.samples.LongTextConfirmationDialogSample
 * @param visible A boolean indicating whether the confirmation dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param text A slot for displaying text below the icon. It should not exceed 3 lines.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [ConfirmationDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. This value
 *   will be adjusted by the accessibility manager according to the content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. It's recommended to set its size to [ConfirmationDialogDefaults.SmallIconSize]
 */
@Composable
public fun ConfirmationDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    text: @Composable (ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
    content: @Composable () -> Unit
) {
    AnimateConfirmationDialog(
        visible = visible,
        performHapticFeedback = null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        containsText = text != null,
        durationMillis = durationMillis,
        content = { ConfirmationDialogContent(text = text, colors = colors, content = content) }
    )
}

/**
 * This overload of [ConfirmationDialogContent] provides the content for a [ConfirmationDialog] with
 * with an icon and optional short [text]. The length of the text should not exceed 3 lines. If the
 * text is very short and fits into 1-2 words, consider using the alternative [ConfirmationDialog]
 * overload with the curvedText parameter instead.
 *
 * Prefer using [ConfirmationDialog] directly, which provides built-in animations when
 * showing/hiding the dialog. This composable may be used to provide the content for a confirmation
 * dialog if custom show/hide animations are required.
 *
 * Example of a [ConfirmationDialog] with an icon and a text which fits into 3 lines:
 *
 * @sample androidx.wear.compose.material3.samples.LongTextConfirmationDialogSample
 * @param text A slot for displaying text below the icon. It should not exceed 3 lines.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [ConfirmationDialog].
 * @param content A slot for displaying an icon inside the confirmation dialog. It's recommended to
 *   set its size to [ConfirmationDialogDefaults.IconSize]
 */
@Composable
public fun ConfirmationDialogContent(
    text: @Composable (ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.colors(),
    content: @Composable () -> Unit
) {
    val reduceMotionEnabled = LocalReduceMotion.current

    val alphaAnimatable = remember { Animatable(0f) }
    val textOpacityAnimationSpec = TextOpacityAnimationSpec
    LaunchedEffect(Unit) {
        animatedDelay(DurationShort2.toLong(), reduceMotionEnabled)
        alphaAnimatable.animateTo(1f, textOpacityAnimationSpec)
    }

    Box(modifier.fillMaxSize()) {
        val horizontalPadding = screenWidthDp().dp * HorizontalLinearContentPaddingFraction
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                iconContainer(false, colors.iconContainerColor)()
                CompositionLocalProvider(LocalContentColor provides colors.iconColor, content)
            }
            CompositionLocalProvider(
                LocalContentColor provides colors.textColor,
                LocalTextStyle provides MaterialTheme.typography.titleMedium,
                LocalTextConfiguration provides
                    TextConfiguration(
                        textAlign = TextAlign.Center,
                        maxLines = LinearContentMaxLines,
                        overflow = TextOverflow.Ellipsis
                    ),
            ) {
                if (text != null) {
                    Spacer(Modifier.height(LinearContentSpacing))
                    Column(
                        modifier =
                            Modifier.fillMaxWidth().graphicsLayer { alpha = alphaAnimatable.value },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = text
                    )
                    Spacer(Modifier.height(LinearContentSpacing))
                }
            }
        }
    }
}

/**
 * Shows a [SuccessConfirmationDialog] dialog with a success icon and optional short curved text.
 * This variation of confirmation dialog indicates a successful operation or action.
 *
 * The confirmation dialog will show a message to the user for [durationMillis]. After a specified
 * timeout, the [onDismissRequest] callback will be invoked, where it's up to the caller to handle
 * the dismissal. To hide the confirmation, [visible] parameter should be set to false.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [SuccessConfirmationDialog].
 *
 * Example of a [SuccessConfirmationDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.SuccessConfirmationDialogSample
 * @param visible A boolean indicating whether the confirmation dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding, and [ConfirmationDialogDefaults.curvedTextStyle]
 *   as the style.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [SuccessConfirmationDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. This value
 *   will be adjusted by the accessibility manager according to the content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to an animated [ConfirmationDialogDefaults.SuccessIcon].
 */
@Composable
public fun SuccessConfirmationDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.successColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
    content: @Composable () -> Unit = { ConfirmationDialogDefaults.SuccessIcon() },
) {
    val hapticFeedback = LocalHapticFeedback.current
    AnimateConfirmationDialog(
        visible = visible,
        performHapticFeedback = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        durationMillis = durationMillis,
        containsText = curvedText != null,
    ) {
        SuccessConfirmationDialogContent(
            curvedText = curvedText,
            colors = colors,
            content = content
        )
    }
}

/**
 * [SuccessConfirmationDialogContent] provides the content for a success confirmation dialog with a
 * success icon and optional short curved text. This variation of confirmation dialog indicates a
 * successful operation or action.
 *
 * Prefer using [SuccessConfirmationDialog] directly, which provides built-in animations when
 * showing/hiding the dialog. This composable may be used to provide the content for a confirmation
 * dialog if custom show/hide animations are required.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [SuccessConfirmationDialog].
 *
 * Example of a [SuccessConfirmationDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.SuccessConfirmationDialogSample
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding, and [ConfirmationDialogDefaults.curvedTextStyle]
 *   as the style.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [SuccessConfirmationDialog]. will be adjusted by the accessibility manager according to the
 *   content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to an animated [ConfirmationDialogDefaults.SuccessIcon].
 */
@Composable
public fun SuccessConfirmationDialogContent(
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.successColors(),
    content: @Composable () -> Unit = { ConfirmationDialogDefaults.SuccessIcon() },
) {
    ConfirmationDialogContentWrapper(
        curvedText = curvedText,
        modifier = modifier,
        colors = colors,
        content = {
            IconContainer(
                iconColor = colors.iconColor,
                iconBackground = successIconContainer(colors.iconContainerColor),
                content = content
            )
        },
    )
}

/**
 * Shows a [FailureConfirmationDialog] with a failure icon and an optional short curved text. This
 * variation of confirmation dialog indicates an unsuccessful operation or action.
 *
 * The confirmation dialog will show a message to the user for [durationMillis]. After a specified
 * timeout, the [onDismissRequest] callback will be invoked, where it's up to the caller to handle
 * the dismissal. To hide the confirmation, [visible] parameter should be set to false.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [FailureConfirmationDialog].
 *
 * Example of [FailureConfirmationDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.FailureConfirmationDialogSample
 * @param visible A boolean indicating whether the confirmation dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [FailureConfirmationDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. This value
 *   will be adjusted by the accessibility manager according to the content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to [ConfirmationDialogDefaults.FailureIcon].
 */
@Composable
public fun FailureConfirmationDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.failureColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
    content: @Composable () -> Unit = { ConfirmationDialogDefaults.FailureIcon() },
) {
    val hapticFeedback = LocalHapticFeedback.current
    AnimateConfirmationDialog(
        visible = visible,
        performHapticFeedback = { hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        durationMillis = durationMillis,
        containsText = curvedText != null,
    ) {
        FailureConfirmationDialogContent(
            curvedText = curvedText,
            colors = colors,
            content = content
        )
    }
}

/**
 * [FailureConfirmationDialogContent] provides the content for a failure confirmation dialog with
 * icon and an optional short curved text. This variation of confirmation dialog indicates an
 * unsuccessful operation or action.
 *
 * Prefer using [FailureConfirmationDialog] directly, which provides built-in animations when
 * showing/hiding the dialog. This composable may be used to provide the content for a confirmation
 * dialog if custom show/hide animations are required.
 *
 * Where user input is required, such as choosing to ok or cancel an action, use [AlertDialog]
 * instead of [FailureConfirmationDialog].
 *
 * Example of [FailureConfirmationDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.FailureConfirmationDialogSample
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [confirmationDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationDialogColors] object for customizing the colors used in this
 *   [FailureConfirmationDialog]. will be adjusted by the accessibility manager according to the
 *   content displayed.
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to [ConfirmationDialogDefaults.FailureIcon].
 */
@Composable
public fun FailureConfirmationDialogContent(
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationDialogColors = ConfirmationDialogDefaults.failureColors(),
    content: @Composable () -> Unit = { ConfirmationDialogDefaults.FailureIcon() },
) {
    ConfirmationDialogContentWrapper(
        curvedText = curvedText,
        modifier = modifier,
        colors = colors,
        content = {
            val translationXAnimatable = remember { Animatable(FailureContentTransition[0]) }
            val reduceMotionEnabled = LocalReduceMotion.current
            LaunchedEffect(Unit) {
                animatedDelay(DurationShort3.toLong(), reduceMotionEnabled)
                translationXAnimatable.animateTo(
                    FailureContentTransition[1],
                    FailureContentAnimationSpecs[0]
                )
                translationXAnimatable.animateTo(
                    FailureContentTransition[2],
                    FailureContentAnimationSpecs[1]
                )
            }

            IconContainer(
                modifier = Modifier.graphicsLayer { translationX = translationXAnimatable.value },
                iconColor = colors.iconColor,
                iconBackground = failureIconContainer(colors.iconContainerColor),
                content = content
            )
        }
    )
}

/**
 * A customized variation of [androidx.wear.compose.material3.curvedText] that displays text along a
 * curved path. This variation adopts suitable sweep angle and padding for use in
 * [ConfirmationDialog]s.
 *
 * @param text The text to display.
 * @param style It is recommended to use [ConfirmationDialogDefaults.curvedTextStyle] for curved
 *   text in Confirmation Dialogs.
 */
public fun CurvedScope.confirmationDialogCurvedText(
    text: String,
    style: CurvedTextStyle,
): Unit =
    curvedText(
        text = text,
        style = style,
        maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
        modifier = CurvedModifier.padding(PaddingDefaults.edgePadding),
    )

/** Contains default values used by [ConfirmationDialog] composable. */
public object ConfirmationDialogDefaults {

    /** The default style for curved text content. */
    public val curvedTextStyle: CurvedTextStyle
        @Composable get() = CurvedTextStyle(MaterialTheme.typography.titleLarge)

    /**
     * A default composable used in [SuccessConfirmationDialog] that displays a success icon with an
     * animation.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    public fun SuccessIcon() {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_check_animation)
        var atEnd by remember { mutableStateOf(false) }
        val reduceMotionEnabled = LocalReduceMotion.current

        LaunchedEffect(Unit) {
            animatedDelay(IconDelay, reduceMotionEnabled)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }

    /**
     * A default composable used in [FailureConfirmationDialog] that displays a failure icon with an
     * animation.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    public fun FailureIcon() {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_failure_animation)
        var atEnd by remember { mutableStateOf(false) }
        val reduceMotionEnabled = LocalReduceMotion.current

        LaunchedEffect(Unit) {
            animatedDelay(IconDelay, reduceMotionEnabled)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }

    /**
     * Creates a [ConfirmationDialogColors] that represents the default colors used in a
     * [ConfirmationDialog].
     */
    @Composable
    public fun colors(): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultConfirmationDialogColors

    /**
     * Creates a [ConfirmationDialogColors] with modified colors used in [ConfirmationDialog].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    public fun colors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultConfirmationDialogColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /**
     * Creates a [ConfirmationDialogColors] that represents the default colors used in a
     * [SuccessConfirmationDialog].
     */
    @Composable
    public fun successColors(): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultSuccessConfirmationDialogColors

    /**
     * Creates a [ConfirmationDialogColors] with modified colors used in
     * [SuccessConfirmationDialog].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    public fun successColors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultSuccessConfirmationDialogColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /**
     * Creates a [ConfirmationDialogColors] that represents the default colors used in a
     * [FailureConfirmationDialog].
     */
    @Composable
    public fun failureColors(): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultFailureConfirmationDialogColors

    /**
     * Creates a [ConfirmationDialogColors] with modified colors used in
     * [FailureConfirmationDialog].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    public fun failureColors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ): ConfirmationDialogColors =
        MaterialTheme.colorScheme.defaultFailureConfirmationDialogColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /**
     * Default timeout for the [ConfirmationDialog] dialog, in milliseconds. The actual timeout used
     * will be adjusted for accessibility, taking into account the contents displayed.
     */
    public const val DurationMillis: Long = 4000L

    /** Default icon size for the [ConfirmationDialog] with curved content */
    public val IconSize: Dp = 52.dp

    /** Default icon size for the [ConfirmationDialog] with linear content */
    public val SmallIconSize: Dp = 36.dp

    private val ColorScheme.defaultConfirmationDialogColors: ConfirmationDialogColors
        get() {
            return defaultConfirmationColorsCached
                ?: ConfirmationDialogColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.Primary),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.OnPrimary),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultConfirmationColorsCached = it }
        }

    private val ColorScheme.defaultSuccessConfirmationDialogColors: ConfirmationDialogColors
        get() {
            return defaultSuccessConfirmationColorsCached
                ?: ConfirmationDialogColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.Primary),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.OnPrimary),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultSuccessConfirmationColorsCached = it }
        }

    private val ColorScheme.defaultFailureConfirmationDialogColors: ConfirmationDialogColors
        get() {
            return defaultFailureConfirmationColorsCached
                ?: ConfirmationDialogColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.ErrorDim),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.OnError).copy(.8f),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultFailureConfirmationColorsCached = it }
        }

    private const val IconDelay = DurationShort2.toLong()
}

/**
 * Represents the colors used in [ConfirmationDialog], [SuccessConfirmationDialog] and
 * [FailureConfirmationDialog].
 *
 * @param iconColor Color used to tint the icon.
 * @param iconContainerColor The color of the container behind the icon.
 * @param textColor Color used to tint the text.
 */
public class ConfirmationDialogColors(
    public val iconColor: Color,
    public val iconContainerColor: Color,
    public val textColor: Color,
) {
    /**
     * Returns a copy of this ConfirmationColors, optionally overriding some of the values.
     *
     * @param iconColor Color used to tint the icon.
     * @param iconContainerColor The color of the container behind the icon.
     * @param textColor Color used to tint the text.
     */
    public fun copy(
        iconColor: Color = this.iconColor,
        iconContainerColor: Color = this.iconContainerColor,
        textColor: Color = this.textColor
    ): ConfirmationDialogColors =
        ConfirmationDialogColors(
            iconColor = iconColor.takeOrElse { this.iconColor },
            iconContainerColor = iconContainerColor.takeOrElse { this.iconContainerColor },
            textColor = textColor.takeOrElse { this.textColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ConfirmationDialogColors) return false

        if (iconColor != other.iconColor) return false
        if (iconContainerColor != other.iconContainerColor) return false
        if (textColor != other.textColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iconColor.hashCode()
        result = 31 * result + iconContainerColor.hashCode()
        result = 31 * result + textColor.hashCode()
        return result
    }
}

@Composable
private fun AnimateConfirmationDialog(
    visible: Boolean,
    performHapticFeedback: (() -> Unit)?,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    containsText: Boolean,
    durationMillis: Long,
    content: @Composable () -> Unit,
) {
    val a11yDurationMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = durationMillis,
            containsIcons = true,
            containsText = containsText,
            containsControls = false,
        ) ?: durationMillis

    LaunchedEffect(visible, a11yDurationMillis) {
        if (visible) {
            performHapticFeedback?.invoke()
            delay(a11yDurationMillis)
            onDismissRequest()
        }
    }

    Dialog(
        visible = visible,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        properties = properties,
        content = content
    )
}

/** Display confirmation dialog content, with common animation of curved text opacity */
@Composable
private fun ConfirmationDialogContentWrapper(
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier,
    colors: ConfirmationDialogColors,
    content: @Composable BoxScope.() -> Unit,
) {
    val alphaAnimatable = remember { Animatable(0f) }
    val textOpacityAnimationSpec = TextOpacityAnimationSpec
    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(Unit) {
        animatedDelay(DurationShort2.toLong(), reduceMotionEnabled)
        alphaAnimatable.animateTo(1f, textOpacityAnimationSpec)
    }
    Box(modifier = modifier.fillMaxSize()) {
        content()
        CompositionLocalProvider(LocalContentColor provides colors.textColor) {
            curvedText?.let {
                CurvedLayout(
                    modifier = Modifier.graphicsLayer { alpha = alphaAnimatable.value },
                    anchor = 90f,
                    contentBuilder = curvedText,
                    angularDirection = CurvedDirection.Angular.Reversed,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.IconContainer(
    modifier: Modifier = Modifier,
    iconColor: Color,
    iconBackground: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
        iconBackground()
        CompositionLocalProvider(LocalContentColor provides iconColor, content)
    }
}

private fun iconContainer(curvedContent: Boolean, color: Color): @Composable BoxScope.() -> Unit = {
    val width =
        if (curvedContent) {
            (screenWidthDp() * ConfirmationSizeFraction).dp
        } else ConfirmationLinearIconContainerSize

    val startShape = ShapeTokens.CornerFull
    val targetShape =
        (if (curvedContent) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large)
            as RoundedCornerShape

    val rotateAnimatable = remember { Animatable(ConfirmationIconInitialAngle) }
    val shapeAnimatable = remember { Animatable(0f) }
    val shape =
        remember(shapeAnimatable) {
            AnimatedRoundedCornerShape(startShape, targetShape) { shapeAnimatable.value }
        }
    val heroShapeMorphAnimationSpec: AnimationSpec<Float> =
        MaterialTheme.motionScheme.defaultSpatialSpec()
    val heroShapeRotationAnimationSpec: AnimationSpec<Float> =
        MaterialTheme.motionScheme.slowEffectsSpec()
    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(Unit) {
        animatedDelay(DurationShort2.toLong(), reduceMotionEnabled)
        launch { shapeAnimatable.animateTo(1f, heroShapeMorphAnimationSpec) }
        rotateAnimatable.animateTo(0f, heroShapeRotationAnimationSpec)
    }

    Box(
        Modifier.size(width)
            .graphicsLayer {
                this.shape = shape
                rotationZ = rotateAnimatable.value
                clip = true
            }
            .background(color)
            .align(Alignment.Center)
    )
}

private fun successIconContainer(color: Color): @Composable BoxScope.() -> Unit = {
    val width = screenWidthDp() * SuccessWidthFraction

    val targetHeight = screenHeightDp() * SuccessHeightFraction.toFloat()
    val heightAnimatable = remember { Animatable(width) }
    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(Unit) {
        animatedDelay(DurationShort2.toLong(), reduceMotionEnabled)
        heightAnimatable.animateTo(targetHeight, SuccessContainerAnimationSpec)
    }
    Box(
        Modifier.size(width.dp, heightAnimatable.value.dp)
            .graphicsLayer {
                rotationZ = 45f
                shape = CircleShape
                clip = true
            }
            .background(color)
    )
}

private fun failureIconContainer(color: Color): @Composable BoxScope.() -> Unit = {
    val size = screenWidthDp() * FailureSizeFraction

    val startShape = ShapeTokens.CornerFull
    val targetShape = MaterialTheme.shapes.extraLarge as RoundedCornerShape
    val shapeAnimatable = remember { Animatable(0f) }
    val shape =
        remember(shapeAnimatable) {
            AnimatedRoundedCornerShape(startShape, targetShape) { shapeAnimatable.value }
        }
    val failureContainerAnimationSpec: AnimationSpec<Float> =
        MaterialTheme.motionScheme.fastEffectsSpec()
    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(Unit) {
        animatedDelay(DurationShort2.toLong(), reduceMotionEnabled)
        shapeAnimatable.animateTo(1f, failureContainerAnimationSpec)
    }

    Box(
        Modifier.size(size.dp)
            .graphicsLayer {
                this.shape = shape
                clip = true
            }
            .background(color)
    )
}

internal val ConfirmationLinearIconContainerSize = 80.dp
internal val LinearContentSpacing = 8.dp

private const val SuccessWidthPaddingFraction = 0.2315f
private const val SuccessHeightPaddingFraction = 0.176
private const val SuccessWidthFraction = 1 - SuccessWidthPaddingFraction * 2
private const val SuccessHeightFraction = 1 - SuccessHeightPaddingFraction * 2

private const val FailureSizePaddingFraction = 0.213f
private const val FailureSizeFraction = 1 - FailureSizePaddingFraction * 2

private const val ConfirmationSizePaddingFraction = 0.213f
private const val ConfirmationSizeFraction = 1 - ConfirmationSizePaddingFraction * 2

private const val LinearContentMaxLines = 3
private const val HorizontalLinearContentPaddingFraction = 0.12f

private const val ConfirmationIconInitialAngle = -45f

private val FailureContentTransition = arrayOf(-8f, -15f, 0f)
private val FailureContentAnimationSpecs =
    arrayOf(
        spring(
            dampingRatio = ExpressiveDefaultDamping,
            stiffness = ExpressiveDefaultStiffness,
            visibilityThreshold = 0f
        ),
        spring(
            dampingRatio = 0.5f,
            stiffness = ExpressiveDefaultStiffness,
        )
    )
private val TextOpacityAnimationSpec: AnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
private val SuccessContainerAnimationSpec: AnimationSpec<Float> =
    spring(dampingRatio = 0.55f, stiffness = 800f)
