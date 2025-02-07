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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material3.internal.Strings.Companion.OpenOnPhoneContentDescriptionIcon
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.MotionTokens.DurationLong2
import androidx.wear.compose.material3.tokens.MotionTokens.DurationShort3
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A full-screen dialog that displays an animated icon with a curved text at the bottom.
 *
 * The dialog will be showing a message to the user for [durationMillis]. After a specified timeout,
 * the [onDismissRequest] callback will be invoked, where it's up to the caller to handle the
 * dismissal. To hide the dialog, [visible] parameter should be set to false.
 *
 * This dialog is typically used to indicate that an action has been initiated and will continue on
 * the user's phone. Once this dialog is displayed, it's developer responsibility to establish the
 * connection between the watch and the phone.
 *
 * Example of an [OpenOnPhoneDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.OpenOnPhoneDialogSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [openOnPhoneDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding, and [OpenOnPhoneDialogDefaults.curvedTextStyle]
 *   as the style.
 * @param modifier Modifier to be applied to the dialog content.
 * @param colors [OpenOnPhoneDialogColors] that will be used to resolve the colors used for this
 *   [OpenOnPhoneDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. This value
 *   will be adjusted by the accessibility manager according to the content displayed.
 * @param content A slot for displaying an icon inside the open on phone dialog, which can be
 *   animated. Defaults to [OpenOnPhoneDialogDefaults.Icon].
 */
@Composable
public fun OpenOnPhoneDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: OpenOnPhoneDialogColors = OpenOnPhoneDialogDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = OpenOnPhoneDialogDefaults.DurationMillis,
    content: @Composable () -> Unit = { OpenOnPhoneDialogDefaults.Icon() },
) {
    val a11yFullDurationMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = durationMillis,
            containsIcons = true,
            containsText = curvedText != null,
            containsControls = false,
        ) ?: durationMillis

    LaunchedEffect(visible, a11yFullDurationMillis) {
        if (visible) {
            delay(a11yFullDurationMillis)
            onDismissRequest()
        }
    }
    Dialog(
        visible = visible,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        OpenOnPhoneDialogContent(
            curvedText = curvedText,
            durationMillis = a11yFullDurationMillis,
            colors = colors,
            content = content
        )
    }
}

/**
 * This composable provides the content for an [OpenOnPhoneDialog] that displays an animated icon
 * with curved text at the bottom.
 *
 * Prefer using [OpenOnPhoneDialog] directly, which provides built-in animations when showing/hiding
 * the dialog. This composable may be used to provide the content for an openOnPhone dialog if
 * custom show/hide animations are required.
 *
 * Example of an [OpenOnPhoneDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.OpenOnPhoneDialogSample
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. We recommend using [openOnPhoneDialogCurvedText] for this parameter, which
 *   will give the default sweep angle and padding, and [OpenOnPhoneDialogDefaults.curvedTextStyle]
 *   as the style.
 * @param durationMillis The duration in milliseconds for which the progress indicator inside of
 *   this content is animated. This value should be previously adjusted by the accessibility manager
 *   according to the content displayed. See [OpenOnPhoneDialog] implementation for more details.
 * @param modifier Modifier to be applied to the openOnPhone content.
 * @param colors [OpenOnPhoneDialogColors] that will be used to resolve the colors used for this
 *   [OpenOnPhoneDialog].
 * @param content A slot for displaying an icon inside the open on phone dialog, which can be
 *   animated. Defaults to [OpenOnPhoneDialogDefaults.Icon].
 */
@Composable
public fun OpenOnPhoneDialogContent(
    curvedText: (CurvedScope.() -> Unit)?,
    durationMillis: Long,
    modifier: Modifier = Modifier,
    colors: OpenOnPhoneDialogColors = OpenOnPhoneDialogDefaults.colors(),
    content: @Composable () -> Unit
): Unit {
    var progress by remember { mutableFloatStateOf(0f) }
    val progressAnimatable = remember { Animatable(0f) }
    val alphaAnimatable = remember { Animatable(0f) }

    var finalAnimation by remember { mutableStateOf(false) }

    val finalAnimationDuration = DurationLong2
    val progressDuration = durationMillis - finalAnimationDuration

    val alphaAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(durationMillis) {
        launch {
            animatedDelay(DurationShort3.toLong(), reduceMotionEnabled)
            alphaAnimatable.animateTo(1f, alphaAnimationSpec)
        }
        launch {
            if (!reduceMotionEnabled) {
                progressAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(durationMillis = progressDuration.toInt(), easing = LinearEasing),
                ) {
                    progress = value
                }
                finalAnimation = true
            }
        }
    }

    val colorReversalAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val sizeAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val progressAlphaAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val sizeAnimationFraction =
        animateFloatAsState(if (finalAnimation) 0f else 1f, sizeAnimationSpec)
    val progressAlphaAnimationFraction =
        animateFloatAsState(if (finalAnimation) 0f else 1f, progressAlphaAnimationSpec)
    val iconColor =
        animateColorAsState(
            if (finalAnimation) colors.iconContainerColor else colors.iconColor,
            colorReversalAnimationSpec
        )
    val iconContainerColor =
        animateColorAsState(
            if (finalAnimation) colors.iconColor else colors.iconContainerColor,
            colorReversalAnimationSpec
        )

    Box(modifier = modifier.fillMaxSize()) {
        val topPadding = screenHeightDp() * HeightPaddingFraction
        val size = screenWidthDp() * SizeFraction
        Box(
            modifier =
                Modifier.padding(top = topPadding.dp).size(size.dp).align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            iconAndProgressContainer(
                iconContainerColor = iconContainerColor.value,
                progressIndicatorColors =
                    ProgressIndicatorDefaults.colors(
                        SolidColor(colors.progressIndicatorColor),
                        SolidColor(colors.progressTrackColor)
                    ),
                sizeAnimationFraction = sizeAnimationFraction,
                progressAlphaAnimationFraction = progressAlphaAnimationFraction,
                progress = { progress }
            )()
            CompositionLocalProvider(LocalContentColor provides iconColor.value, content)
        }
        CompositionLocalProvider(LocalContentColor provides colors.textColor) {
            curvedText?.let {
                CurvedLayout(
                    modifier = Modifier.graphicsLayer { alpha = alphaAnimatable.value },
                    anchor = 90f,
                    angularDirection = CurvedDirection.Angular.Reversed,
                    contentBuilder = curvedText
                )
            }
        }
    }
}

/**
 * A customized variation of [androidx.wear.compose.material3.curvedText] that displays text along a
 * curved path. This variation adopts suitable sweep angle and padding for use in
 * [OpenOnPhoneDialog].
 *
 * @param text The text to display.
 * @param style The style to apply to the text. It is recommended to use
 *   [OpenOnPhoneDialogDefaults.curvedTextStyle] for curved text in [OpenOnPhoneDialog].
 */
public fun CurvedScope.openOnPhoneDialogCurvedText(
    text: String,
    style: CurvedTextStyle,
): Unit =
    curvedText(
        text = text,
        style = style,
        maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
        modifier = CurvedModifier.padding(PaddingDefaults.edgePadding)
    )

/** Contains the default values used by [OpenOnPhoneDialog]. */
public object OpenOnPhoneDialogDefaults {

    /** The default style for curved text content. */
    public val curvedTextStyle: CurvedTextStyle
        @Composable get() = CurvedTextStyle(MaterialTheme.typography.titleLarge)

    /** The default message for an [OpenOnPhoneDialog]. */
    public val text: String
        @Composable get() = LocalContext.current.getString(R.string.wear_m3c_open_on_phone)

    /**
     * A default composable used in [OpenOnPhoneDialog] that displays an open on phone icon with an
     * animation.
     *
     * @param modifier Modifier to be applied to the icon.
     * @param contentDescription The content description for the icon.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    public fun Icon(
        modifier: Modifier = Modifier,
        contentDescription: String = iconContentDescription
    ) {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_open_on_phone_animation)
        var atEnd by remember { mutableStateOf(false) }
        val reduceMotionEnabled = LocalReduceMotion.current

        LaunchedEffect(Unit) {
            animatedDelay(IconDelay, reduceMotionEnabled)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = contentDescription,
            modifier = modifier.size(IconSize)
        )
    }

    /** The default content description for the open on phone icon */
    public val iconContentDescription: String
        @Composable get() = getString(OpenOnPhoneContentDescriptionIcon)

    /**
     * Creates a [OpenOnPhoneDialogColors] that represents the default colors used in
     * [OpenOnPhoneDialog].
     */
    @Composable
    public fun colors(): OpenOnPhoneDialogColors =
        MaterialTheme.colorScheme.defaultOpenOnPhoneDialogColors

    /**
     * Creates a [OpenOnPhoneDialogColors] with modified colors used in [OpenOnPhoneDialog].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param progressIndicatorColor The progress indicator color.
     * @param progressTrackColor The progress track color.
     * @param textColor The text color.
     */
    @Composable
    public fun colors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        progressIndicatorColor: Color = Color.Unspecified,
        progressTrackColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified
    ): OpenOnPhoneDialogColors =
        MaterialTheme.colorScheme.defaultOpenOnPhoneDialogColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            progressIndicatorColor = progressIndicatorColor,
            progressTrackColor = progressTrackColor,
            textColor = textColor
        )

    /** Default timeout for the [OpenOnPhoneDialog] dialog, in milliseconds. */
    public const val DurationMillis: Long = 4000L

    private val ColorScheme.defaultOpenOnPhoneDialogColors: OpenOnPhoneDialogColors
        get() {
            return mDefaultOpenOnPhoneDialogColorsCached
                ?: OpenOnPhoneDialogColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.OnPrimaryContainer),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.PrimaryContainer),
                        progressIndicatorColor = fromToken(ColorSchemeKeyTokens.Primary),
                        progressTrackColor = fromToken(ColorSchemeKeyTokens.OnPrimary),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { mDefaultOpenOnPhoneDialogColorsCached = it }
        }

    private const val IconDelay = 67L
    private val IconSize = 52.dp
}

/**
 * Represents the colors used in [OpenOnPhoneDialog].
 *
 * @param iconColor Color used to tint the icon.
 * @param iconContainerColor The color of the container behind the icon.
 * @param progressIndicatorColor Color used to draw the indicator arc of progress indicator.
 * @param progressTrackColor Color used to draw the track of progress indicator.
 * @param textColor Color used to draw the text.
 */
public class OpenOnPhoneDialogColors(
    public val iconColor: Color,
    public val iconContainerColor: Color,
    public val progressIndicatorColor: Color,
    public val progressTrackColor: Color,
    public val textColor: Color
) {
    /**
     * Returns a copy of this OpenOnPhoneDialogColors optionally overriding some of the values.
     *
     * @param iconColor Color used to tint the icon.
     * @param iconContainerColor The color of the container behind the icon.
     * @param progressIndicatorColor Color used to draw the indicator arc of progress indicator.
     * @param progressTrackColor Color used to draw the track of progress indicator.
     * @param textColor Color used to draw the text.
     */
    public fun copy(
        iconColor: Color = this.iconColor,
        iconContainerColor: Color = this.iconContainerColor,
        progressIndicatorColor: Color = this.progressIndicatorColor,
        progressTrackColor: Color = this.progressTrackColor,
        textColor: Color = this.textColor
    ): OpenOnPhoneDialogColors =
        OpenOnPhoneDialogColors(
            iconColor = iconColor.takeOrElse { this.iconColor },
            iconContainerColor = iconContainerColor.takeOrElse { this.iconContainerColor },
            progressIndicatorColor =
                progressIndicatorColor.takeOrElse { this.progressIndicatorColor },
            progressTrackColor = progressTrackColor.takeOrElse { this.progressTrackColor },
            textColor = textColor.takeOrElse { this.textColor }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is OpenOnPhoneDialogColors) return false

        if (iconColor != other.iconColor) return false
        if (iconContainerColor != other.iconContainerColor) return false
        if (progressIndicatorColor != other.progressIndicatorColor) return false
        if (progressTrackColor != other.progressTrackColor) return false
        if (textColor != other.textColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iconColor.hashCode()
        result = 31 * result + iconContainerColor.hashCode()
        result = 31 * result + progressIndicatorColor.hashCode()
        result = 31 * result + progressTrackColor.hashCode()
        result = 31 * result + textColor.hashCode()
        return result
    }
}

private fun iconAndProgressContainer(
    iconContainerColor: Color,
    progressIndicatorColors: ProgressIndicatorColors,
    sizeAnimationFraction: State<Float>,
    progressAlphaAnimationFraction: State<Float>,
    progress: () -> Float
): @Composable BoxScope.() -> Unit = {
    // Some animations might overshoot outside 0..1 range, that's why we need to coerce values above
    // 0 to eliminate negative padding and strokeWidth.
    val padding =
        ((progressIndicatorStrokeWidth + progressIndicatorPadding) * sizeAnimationFraction.value)
            .coerceAtLeast(0.dp)
    val strokeWidth =
        (progressIndicatorStrokeWidth * sizeAnimationFraction.value).coerceAtLeast(0.dp)
    Box(
        Modifier.fillMaxSize()
            .padding(padding)
            .graphicsLayer {
                shape = CircleShape
                clip = true
            }
            .background(iconContainerColor)
            .align(Alignment.Center)
    )

    IconContainerProgressIndicator(
        progress = progress,
        progressAlpha = progressAlphaAnimationFraction.value,
        strokeWidth = strokeWidth,
        colors = progressIndicatorColors
    )
}

@Composable
private fun IconContainerProgressIndicator(
    progress: () -> Float,
    progressAlpha: Float,
    colors: ProgressIndicatorColors,
    strokeWidth: Dp,
) {
    Spacer(
        Modifier.fillMaxSize()
            .focusable()
            .graphicsLayer { alpha = progressAlpha }
            .drawBehind {
                drawCircularProgressIndicator(
                    progress = progress(),
                    strokeWidth = strokeWidth,
                    colors = colors,
                )
            }
    )
}

private const val WidthPaddingFraction = 0.176f
private const val HeightPaddingFraction = 0.157f
private const val SizeFraction = 1 - WidthPaddingFraction * 2
private val progressIndicatorStrokeWidth = 5.dp
private val progressIndicatorPadding = 5.dp
