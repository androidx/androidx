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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
import kotlinx.coroutines.delay

/**
 * A full-screen dialog that displays an animated icon with a curved text at the bottom.
 *
 * The dialog will be showing a message to the user for [durationMillis]. After a specified timeout,
 * the [onDismissRequest] callback will be invoked, where it's up to the caller to handle the
 * dismissal. To hide the dialog, [show] parameter should be set to false.
 *
 * This dialog is typically used to indicate that an action has been initiated and will continue on
 * the user's phone. Once this dialog is displayed, it's developer responsibility to establish the
 * connection between the watch and the phone.
 *
 * Example of an [OpenOnPhoneDialog] usage:
 *
 * @sample androidx.wear.compose.material3.samples.OpenOnPhoneDialogSample
 * @param show A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. Defaults to a localized open on phone message.
 * @param colors [OpenOnPhoneDialogColors] that will be used to resolve the colors used for this
 *   [OpenOnPhoneDialog].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. Defaults to
 *   [OpenOnPhoneDialogDefaults.DurationMillis].
 * @param content A slot for displaying an icon inside the open on phone dialog, which can be
 *   animated. Defaults to [OpenOnPhoneDialogDefaults.Icon].
 */
@Composable
fun OpenOnPhoneDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    curvedText: (CurvedScope.() -> Unit)? = OpenOnPhoneDialogDefaults.curvedText(),
    colors: OpenOnPhoneDialogColors = OpenOnPhoneDialogDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = OpenOnPhoneDialogDefaults.DurationMillis,
    content: @Composable BoxScope.() -> Unit = OpenOnPhoneDialogDefaults.Icon,
) {
    var progress by remember(show) { mutableFloatStateOf(0f) }
    val animatable = remember { Animatable(0f) }

    val a11yDurationMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = durationMillis,
            containsIcons = true,
            containsText = curvedText != null,
            containsControls = false,
        ) ?: durationMillis

    LaunchedEffect(show, a11yDurationMillis) {
        if (show) {
            animatable.snapTo(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = a11yDurationMillis.toInt(), easing = LinearEasing),
            ) {
                progress = value
            }
            onDismissRequest()
        }
    }

    Dialog(
        showDialog = show,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val topPadding = screenHeightDp() * HeightPaddingFraction
            val size = screenWidthDp() * SizeFraction
            Box(
                Modifier.padding(top = topPadding.dp).size(size.dp).align(Alignment.TopCenter),
            ) {
                iconContainer(
                    iconContainerColor = colors.iconContainerColor,
                    progressIndicatorColors =
                        ProgressIndicatorDefaults.colors(
                            SolidColor(colors.progressIndicatorColor),
                            SolidColor(colors.progressTrackColor)
                        ),
                    progress = { progress }
                )()
                CompositionLocalProvider(LocalContentColor provides colors.iconColor) { content() }
            }
            CompositionLocalProvider(LocalContentColor provides colors.textColor) {
                curvedText?.let { CurvedLayout(anchor = 90f, contentBuilder = curvedText) }
            }
        }
    }
}

/** Contains the default values used by [OpenOnPhoneDialog]. */
object OpenOnPhoneDialogDefaults {

    /**
     * A default composable used in [OpenOnPhoneDialog] that displays an open on phone icon with an
     * animation.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    val Icon: @Composable BoxScope.() -> Unit = {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_open_on_phone_animation)
        var atEnd by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(IconDelay)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = null,
            modifier = Modifier.size(IconSize).align(Alignment.Center),
        )
    }

    /**
     * A default composable that displays text along a curved path, used in [OpenOnPhoneDialog].
     *
     * @param text The text to display. Defaults to an open on phone message.
     * @param style The style to apply to the text. Defaults to
     *   CurvedTextStyle(MaterialTheme.typography.titleLarge).
     */
    @Composable
    fun curvedText(
        text: String = LocalContext.current.resources.getString(R.string.wear_m3c_open_on_phone),
        style: CurvedTextStyle = CurvedTextStyle(MaterialTheme.typography.titleLarge)
    ): CurvedScope.() -> Unit = {
        curvedText(
            text = text,
            style = style,
            maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
            modifier = CurvedModifier.padding(PaddingDefaults.edgePadding),
            angularDirection = CurvedDirection.Angular.Reversed
        )
    }

    /**
     * Creates a [OpenOnPhoneDialogColors] that represents the default colors used in
     * [OpenOnPhoneDialog].
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultOpenOnPhoneDialogColors

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
    fun colors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        progressIndicatorColor: Color = Color.Unspecified,
        progressTrackColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified
    ) =
        MaterialTheme.colorScheme.defaultOpenOnPhoneDialogColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            progressIndicatorColor = progressIndicatorColor,
            progressTrackColor = progressTrackColor,
            textColor = textColor
        )

    /** Default timeout for the [OpenOnPhoneDialog] dialog, in milliseconds. */
    const val DurationMillis = 4000L

    private val ColorScheme.defaultOpenOnPhoneDialogColors: OpenOnPhoneDialogColors
        get() {
            return mDefaultOpenOnPhoneDialogColorsCached
                ?: OpenOnPhoneDialogColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.Primary),
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
class OpenOnPhoneDialogColors(
    val iconColor: Color,
    val iconContainerColor: Color,
    val progressIndicatorColor: Color,
    val progressTrackColor: Color,
    val textColor: Color
) {
    internal fun copy(
        iconColor: Color? = null,
        iconContainerColor: Color? = null,
        progressIndicatorColor: Color? = null,
        progressTrackColor: Color? = null,
        textColor: Color? = null
    ) =
        OpenOnPhoneDialogColors(
            iconColor = iconColor ?: this.iconColor,
            iconContainerColor = iconContainerColor ?: this.iconContainerColor,
            progressIndicatorColor = progressIndicatorColor ?: this.progressIndicatorColor,
            progressTrackColor = progressTrackColor ?: this.progressTrackColor,
            textColor = textColor ?: this.textColor
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

private fun iconContainer(
    iconContainerColor: Color,
    progressIndicatorColors: ProgressIndicatorColors,
    progress: () -> Float
): @Composable BoxScope.() -> Unit = {
    Box(
        Modifier.fillMaxSize()
            .padding(progressIndicatorStrokeWidth + progressIndicatorPadding)
            .graphicsLayer {
                shape = CircleShape
                clip = true
            }
            .background(iconContainerColor)
    )

    CircularProgressIndicator(
        progress = progress,
        strokeWidth = progressIndicatorStrokeWidth,
        colors = progressIndicatorColors
    )
}

private const val WidthPaddingFraction = 0.176f
private const val HeightPaddingFraction = 0.157f
private const val SizeFraction = 1 - WidthPaddingFraction * 2
private val progressIndicatorStrokeWidth = 5.dp
private val progressIndicatorPadding = 5.dp
