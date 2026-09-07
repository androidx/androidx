/*
 * Copyright 2026 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * [RemoteStepper] allows users to make a selection from a range of values. It's a full-screen
 * control with increase and decrease buttons, and a slot in the middle for value or other content.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteStepperSample
 * @param value Current value of the Stepper.
 * @param steps The number of steps between the min and max value of the [valueRange].
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button.
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button.
 * @param modifier Modifiers to be applied to the Stepper.
 * @param decreaseAction Action to perform when the decrease button is clicked.
 * @param increaseAction Action to perform when the increase button is clicked.
 * @param enabled Controls the enabled state of the Stepper. Note that only constant values are
 *   currently supported for [enabled] for click handling.
 * @param valueRange The range of values that this stepper can take.
 * @param colors [RemoteStepperColors] that will be used to resolve the colors used for this
 *   [RemoteStepper].
 * @param content Slot for [RemoteStepper] content, typically a text or button displaying the
 *   current value.
 */
@Composable
@RemoteComposable
public fun RemoteStepper(
    value: RemoteFloat,
    steps: Int = 0,
    decreaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteStepperDefaults.DecreaseIcon()
    },
    increaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteStepperDefaults.IncreaseIcon()
    },
    modifier: RemoteModifier = RemoteModifier,
    decreaseAction: Action = Action.Empty,
    increaseAction: Action = Action.Empty,
    enabled: RemoteBoolean = true.rb,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    colors: RemoteStepperColors = RemoteStepperDefaults.stepperColors(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    require(steps >= 0) { "steps should be greater than or equal to 0" }

    RemoteColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.weight(RemoteStepperDefaults.ButtonWeight.rf)
                    .padding(top = verticalContentPadding()),
            contentAlignment = RemoteAlignment.TopCenter,
        ) {
            // TODO(b/557131896): Use repeatableClickable once supported
            RemoteRoundButton(
                onClick = increaseAction,
                modifier =
                    RemoteModifier.size(
                        width = RemoteStepperDefaults.ButtonWidth,
                        height = RemoteStepperDefaults.ButtonHeight,
                    ),
                backgroundColor = colors.buttonContainerColor(enabled),
                enabled = enabled,
                border = null,
                borderColor = null,
                shape = RemoteStepperDefaults.buttonShape,
                content =
                    provideScopeContent(
                        colors.buttonIconColor(enabled),
                        RemoteMaterialTheme.typography.labelMedium,
                        increaseIcon,
                    ),
            )
        }

        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth().weight(RemoteStepperDefaults.ContentWeight.rf),
            contentAlignment = RemoteAlignment.Center,
            content =
                provideScopeContent(
                    colors.contentColor(enabled),
                    RemoteMaterialTheme.typography.bodyLarge,
                    content,
                ),
        )

        RemoteBox(
            modifier =
                RemoteModifier.weight(RemoteStepperDefaults.ButtonWeight.rf)
                    .padding(bottom = verticalContentPadding()),
            contentAlignment = RemoteAlignment.BottomCenter,
        ) {
            // TODO(b/557131896): Use repeatableClickable once supported
            RemoteRoundButton(
                onClick = decreaseAction,
                modifier =
                    RemoteModifier.size(
                        width = RemoteStepperDefaults.ButtonWidth,
                        height = RemoteStepperDefaults.ButtonHeight,
                    ),
                backgroundColor = colors.buttonContainerColor(enabled),
                enabled = enabled,
                border = null,
                borderColor = null,
                shape = RemoteStepperDefaults.buttonShape,
                content =
                    provideScopeContent(
                        colors.buttonIconColor(enabled),
                        RemoteMaterialTheme.typography.labelMedium,
                        decreaseIcon,
                    ),
            )
        }
    }
}

/**
 * [RemoteStepper] allows users to make a selection from a range of values. It's a full-screen
 * control with increase and decrease buttons, and a slot in the middle for value or other content.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteStepperIntegerSample
 * @param value Current value of the Stepper.
 * @param steps The number of steps between the min and max value of the [valueRange].
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button.
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button.
 * @param modifier Modifiers to be applied to the Stepper.
 * @param decreaseAction Action to perform when the decrease button is clicked.
 * @param increaseAction Action to perform when the increase button is clicked.
 * @param enabled Controls the enabled state of the Stepper. Note that only constant values are
 *   currently supported for [enabled] for click handling.
 * @param valueRange The range of values that this stepper can take.
 * @param colors [RemoteStepperColors] that will be used to resolve the colors used for this
 *   [RemoteStepper].
 * @param content Slot for [RemoteStepper] content, typically a text or button displaying the
 *   current value.
 */
@Composable
@RemoteComposable
public fun RemoteStepper(
    value: RemoteInt,
    steps: Int = 0,
    decreaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteStepperDefaults.DecreaseIcon()
    },
    increaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteStepperDefaults.IncreaseIcon()
    },
    modifier: RemoteModifier = RemoteModifier,
    decreaseAction: Action = Action.Empty,
    increaseAction: Action = Action.Empty,
    enabled: RemoteBoolean = true.rb,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    colors: RemoteStepperColors = RemoteStepperDefaults.stepperColors(),
    content: @Composable @RemoteComposable () -> Unit,
): Unit =
    RemoteStepper(
        value = value.toRemoteFloat(),
        steps = steps,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        modifier = modifier,
        decreaseAction = decreaseAction,
        increaseAction = increaseAction,
        enabled = enabled,
        valueRange = valueRange,
        colors = colors,
        content = content,
    )

/** Contains the default values used by [RemoteStepper]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteStepperDefaults {
    /** Default size for increase and decrease icons. */
    public val IconSize: RemoteDp = 24.rdp

    /** Default decrease icon for [RemoteStepper]. */
    @Composable
    @RemoteComposable
    public fun DecreaseIcon(
        modifier: RemoteModifier = RemoteModifier,
        contentDescription: RemoteString? = null,
    ): Unit = RemoteSliderDefaults.DecreaseIcon(modifier, contentDescription)

    /** Default increase icon for [RemoteStepper]. */
    @Composable
    @RemoteComposable
    public fun IncreaseIcon(
        modifier: RemoteModifier = RemoteModifier,
        contentDescription: RemoteString? = null,
    ): Unit = RemoteSliderDefaults.IncreaseIcon(modifier, contentDescription)

    /** Default width for increase and decrease buttons. */
    public val ButtonWidth: RemoteDp = 60.rdp

    /** Default height for increase and decrease buttons. */
    public val ButtonHeight: RemoteDp = 48.rdp

    /** Default vertical spacing between components. */
    public val VerticalSpacing: RemoteDp = 8.rdp

    /** The weight of the button areas in the vertical layout. */
    public const val ButtonWeight: Float = 0.35f

    /** The weight of the central content area in the vertical layout. */
    public const val ContentWeight: Float = 0.3f

    /** Default shape for the increase and decrease buttons. */
    public val buttonShape: RemoteShape = RemoteCircleShape

    /** Default shape for the increase and decrease buttons. */
    public val StepperButtonShape: RemoteShape
        get() = buttonShape

    /**
     * Creates a [RemoteStepperColors] that represents the default colors used in a [RemoteStepper].
     */
    @Composable
    public fun stepperColors(): RemoteStepperColors =
        RemoteMaterialTheme.colorScheme.defaultStepperColors

    /**
     * Creates a [RemoteStepperColors] that represents the default colors used in a [RemoteStepper].
     *
     * @param contentColor The content color for this [RemoteStepper].
     * @param buttonContainerColor The button container color for this [RemoteStepper].
     * @param buttonIconColor The button icon color for this [RemoteStepper].
     * @param disabledContentColor The disabled content color for this [RemoteStepper].
     * @param disabledButtonContainerColor The disabled button container color for this
     *   [RemoteStepper].
     * @param disabledButtonIconColor The disabled button icon color for this [RemoteStepper].
     */
    @Composable
    public fun stepperColors(
        contentColor: RemoteColor? = null,
        buttonContainerColor: RemoteColor? = null,
        buttonIconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledButtonContainerColor: RemoteColor? = null,
        disabledButtonIconColor: RemoteColor? = null,
    ): RemoteStepperColors =
        RemoteMaterialTheme.colorScheme.defaultStepperColors.copy(
            contentColor = contentColor,
            buttonContainerColor = buttonContainerColor,
            buttonIconColor = buttonIconColor,
            disabledContentColor = disabledContentColor,
            disabledButtonContainerColor = disabledButtonContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
        )

    /**
     * Creates a [RemoteStepperColors] that represents the default colors used in a [RemoteStepper].
     */
    @Composable public fun colors(): RemoteStepperColors = stepperColors()

    /**
     * Creates a [RemoteStepperColors] that represents the default colors used in a [RemoteStepper].
     *
     * @param contentColor The content color for this [RemoteStepper].
     * @param buttonContainerColor The button container color for this [RemoteStepper].
     * @param buttonIconColor The button icon color for this [RemoteStepper].
     * @param disabledContentColor The disabled content color for this [RemoteStepper].
     * @param disabledButtonContainerColor The disabled button container color for this
     *   [RemoteStepper].
     * @param disabledButtonIconColor The disabled button icon color for this [RemoteStepper].
     */
    @Composable
    public fun colors(
        contentColor: RemoteColor? = null,
        buttonContainerColor: RemoteColor? = null,
        buttonIconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledButtonContainerColor: RemoteColor? = null,
        disabledButtonIconColor: RemoteColor? = null,
    ): RemoteStepperColors =
        stepperColors(
            contentColor = contentColor,
            buttonContainerColor = buttonContainerColor,
            buttonIconColor = buttonIconColor,
            disabledContentColor = disabledContentColor,
            disabledButtonContainerColor = disabledButtonContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
        )

    private val RemoteColorScheme.defaultStepperColors: RemoteStepperColors
        get() =
            RemoteStepperColors(
                contentColor = onSurface,
                buttonContainerColor = primaryContainer,
                buttonIconColor = primary,
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledButtonContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledButtonIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

/**
 * Represents the container and content colors used in a [RemoteStepper] in different states.
 *
 * @param contentColor The color of the central content when enabled.
 * @param buttonContainerColor The container color of the increase/decrease buttons when enabled.
 * @param buttonIconColor The icon color of the increase/decrease buttons when enabled.
 * @param disabledContentColor The color of the central content when disabled.
 * @param disabledButtonContainerColor The container color of the buttons when disabled.
 * @param disabledButtonIconColor The icon color of the buttons when disabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteStepperColors(
    public val contentColor: RemoteColor,
    public val buttonContainerColor: RemoteColor,
    public val buttonIconColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
    public val disabledButtonContainerColor: RemoteColor,
    public val disabledButtonIconColor: RemoteColor,
) {
    /** Returns a copy of this [RemoteStepperColors], optionally overriding some values. */
    public fun copy(
        contentColor: RemoteColor? = this.contentColor,
        buttonContainerColor: RemoteColor? = this.buttonContainerColor,
        buttonIconColor: RemoteColor? = this.buttonIconColor,
        disabledContentColor: RemoteColor? = this.disabledContentColor,
        disabledButtonContainerColor: RemoteColor? = this.disabledButtonContainerColor,
        disabledButtonIconColor: RemoteColor? = this.disabledButtonIconColor,
    ): RemoteStepperColors =
        RemoteStepperColors(
            contentColor = contentColor ?: this.contentColor,
            buttonContainerColor = buttonContainerColor ?: this.buttonContainerColor,
            buttonIconColor = buttonIconColor ?: this.buttonIconColor,
            disabledContentColor = disabledContentColor ?: this.disabledContentColor,
            disabledButtonContainerColor =
                disabledButtonContainerColor ?: this.disabledButtonContainerColor,
            disabledButtonIconColor = disabledButtonIconColor ?: this.disabledButtonIconColor,
        )

    internal fun contentColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(contentColor, disabledContentColor)

    internal fun buttonContainerColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(buttonContainerColor, disabledButtonContainerColor)

    internal fun buttonIconColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(buttonIconColor, disabledButtonIconColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteStepperColors) return false

        if (contentColor != other.contentColor) return false
        if (buttonContainerColor != other.buttonContainerColor) return false
        if (buttonIconColor != other.buttonIconColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledButtonContainerColor != other.disabledButtonContainerColor) return false
        if (disabledButtonIconColor != other.disabledButtonIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentColor.hashCode()
        result = 31 * result + buttonContainerColor.hashCode()
        result = 31 * result + buttonIconColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledButtonContainerColor.hashCode()
        result = 31 * result + disabledButtonIconColor.hashCode()
        return result
    }
}

@Composable
internal fun verticalContentPadding(): RemoteDp {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    return if (screenHeightDp in 1f..300f) {
        (screenHeightDp * 0.052f).rdp
    } else {
        10.rdp
    }
}
