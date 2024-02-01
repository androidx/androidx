/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Wear Material [ToggleButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [ToggleButton] is circular in shape and defaults to size
 * [ToggleButtonDefaults.DefaultToggleButtonSize] or [ToggleButtonDefaults.SmallToggleButtonSize].
 * Icon content should be of size [ToggleButtonDefaults.DefaultIconSize] or
 * [ToggleButtonDefaults.SmallIconSize] respectively.
 *
 * The recommended set of checked and unchecked [ToggleButtonColors] can be obtained
 * from [ToggleButtonDefaults.toggleButtonColors], which defaults to
 * checked colors being
 * a solid background of [Colors.primary] with content color of [Colors.onPrimary]
 * and unchecked colors being
 * a solid background of [Colors.surface] with content color of [Colors.onSurface].
 *
 * [ToggleButton]s can be enabled or disabled. A disabled toggle button will not respond to click
 * events.
 *
 * Example of a [ToggleButton] with an icon:
 * @sample androidx.wear.compose.material.samples.ToggleButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons#toggle-button)
 * guide.
 *
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`,
 * this toggle button will not be clickable.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this toggle button. See [ToggleButtonDefaults.toggleButtonColors].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this toggle button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this ToggleButton in different [Interaction]s.
 * @param content The icon, image or text to be drawn inside the toggle button.
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional shape parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = ToggleButton(
    checked,
    onCheckedChange,
    modifier,
    enabled,
    colors,
    interactionSource,
    CircleShape,
    ToggleButtonDefaults.DefaultRole,
    content)

/**
 * Wear Material [ToggleButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [ToggleButton] defaults to size [ToggleButtonDefaults.DefaultToggleButtonSize] or [ToggleButtonDefaults.SmallToggleButtonSize].
 * Icon content should be of size [ToggleButtonDefaults.DefaultIconSize] or
 * [ToggleButtonDefaults.SmallIconSize] respectively.
 *
 * The recommended set of checked and unchecked [ToggleButtonColors] can be obtained
 * from [ToggleButtonDefaults.toggleButtonColors], which defaults to
 * checked colors being
 * a solid background of [Colors.primary] with content color of [Colors.onPrimary]
 * and unchecked colors being
 * a solid background of [Colors.surface] with content color of [Colors.onSurface].
 *
 * [ToggleButton]s can be enabled or disabled. A disabled toggle button will not respond to click
 * events.
 *
 * Example of a [ToggleButton] with an icon:
 * @sample androidx.wear.compose.material.samples.ToggleButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons#toggle-button)
 * guide.
 *
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`,
 * this toggle button will not be clickable.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this toggle button. See [ToggleButtonDefaults.toggleButtonColors].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this toggle button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this ToggleButton in different [Interaction]s.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme.
 * @param content The icon, image or text to be drawn inside the toggle button.
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
    "A newer overload is available with an additional semantic role parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit,
) = ToggleButton(
    checked,
    onCheckedChange,
    modifier,
    enabled,
    colors,
    interactionSource,
    shape,
    ToggleButtonDefaults.DefaultRole,
    content)

/**
 * Wear Material [ToggleButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [ToggleButton] defaults to size [ToggleButtonDefaults.DefaultToggleButtonSize] or [ToggleButtonDefaults.SmallToggleButtonSize].
 * Icon content should be of size [ToggleButtonDefaults.DefaultIconSize] or
 * [ToggleButtonDefaults.SmallIconSize] respectively.
 *
 * The recommended set of checked and unchecked [ToggleButtonColors] can be obtained
 * from [ToggleButtonDefaults.toggleButtonColors], which defaults to
 * checked colors being
 * a solid background of [Colors.primary] with content color of [Colors.onPrimary]
 * and unchecked colors being
 * a solid background of [Colors.surface] with content color of [Colors.onSurface].
 *
 * [ToggleButton]s can be enabled or disabled. A disabled toggle button will not respond to click
 * events.
 *
 * Example of a [ToggleButton] with an icon:
 * @sample androidx.wear.compose.material.samples.ToggleButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons#toggle-button)
 * guide.
 *
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`,
 * this toggle button will not be clickable.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this toggle button. See [ToggleButtonDefaults.toggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 * appearance or preview the toggle button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 * @param content The icon, image or text to be drawn inside the toggle button.
 */
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = CircleShape,
    role: Role = ToggleButtonDefaults.DefaultRole,
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { this.role = role },
        enabled = enabled,
        backgroundColor = { isEnabled, isChecked ->
            colors.backgroundColor(enabled = isEnabled, checked = isChecked)
        },
        border = { _, _ -> null },
        toggleButtonSize = ToggleButtonDefaults.DefaultToggleButtonSize,
        interactionSource = interactionSource,
        shape = shape,
        ripple = rippleOrFallbackImplementation(),
        content = provideScopeContent(
            colors.contentColor(enabled = enabled, checked = checked),
            MaterialTheme.typography.button,
            content
        )
    )
}
/**
 * Represents the background and content colors used in a toggle button in different states.
 *
 * See [ToggleButtonDefaults.toggleButtonColors] for the default colors used, which are
 * primary-styled for a checked toggle button and surface-styled for unchecked.
 */
@Stable
public interface ToggleButtonColors {
    /**
     * Represents the background color for this toggle button, depending on [enabled] and [checked].
     *
     * @param enabled whether the toggle button is enabled
     * @param checked whether the toggle button is checked
     */
    @Composable
    public fun backgroundColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the content color for this toggle button, depending on [enabled] and [checked].
     *
     * @param enabled whether the toggle button is enabled
     * @param checked whether the toggle button is checked
     */
    @Composable
    public fun contentColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Contains the default values used by [ToggleButton].
 */
public object ToggleButtonDefaults {
    /**
     * The recommended size for a small [ToggleButton].
     * You can apply this value for the size by overriding Modifier.size directly on [ToggleButton].
     */
    public val SmallToggleButtonSize = 48.dp

    /**
     * The default size applied for the [ToggleButton].
     * Note that you can override it by applying Modifier.size directly on [ToggleButton].
     */
    public val DefaultToggleButtonSize = 52.dp

    /**
     * The size of an icon when used inside a small-sized [ToggleButton].
     */
    public val SmallIconSize = 24.dp

    /**
     * The default size of an icon when used inside a default-sized [ToggleButton].
     */
    public val DefaultIconSize = 26.dp

    /**
     * Role semantics that accessibility services can use to provide more context to users.
     */
    public val DefaultRole = Role.Checkbox

    /**
     * Creates a [ToggleButtonColors] that represents the background and content colors
     * used in a [ToggleButton]. Defaults to primary-styled checked colors
     * and surface-styled unchecked colors.
     *
     * @param checkedBackgroundColor the background color of this [ToggleButton] when enabled and
     * checked
     * @param checkedContentColor the content color of this [ToggleButton] when enabled and checked
     * @param disabledCheckedBackgroundColor the background color of this [ToggleButton] when
     * checked and not enabled
     * @param disabledCheckedContentColor the content color of this [ToggleButton] when checked
     * and not enabled
     * @param uncheckedBackgroundColor the background color of this [ToggleButton] when enabled and
     * unchecked
     * @param uncheckedContentColor the content color of this [ToggleButton] when enabled and
     * unchecked
     * @param disabledUncheckedBackgroundColor the background color of this [ToggleButton] when
     * unchecked and not enabled
     * @param disabledUncheckedContentColor the content color of this [ToggleButton] when unchecked
     * and not enabled
     */
    @Composable
    public fun toggleButtonColors(
        checkedBackgroundColor: Color = MaterialTheme.colors.primary,
        checkedContentColor: Color = contentColorFor(checkedBackgroundColor),
        disabledCheckedBackgroundColor: Color =
            checkedBackgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledCheckedContentColor: Color = MaterialTheme.colors.background,
        uncheckedBackgroundColor: Color = MaterialTheme.colors.surface,
        uncheckedContentColor: Color = contentColorFor(uncheckedBackgroundColor),
        disabledUncheckedBackgroundColor: Color =
            uncheckedBackgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledUncheckedContentColor: Color =
            uncheckedContentColor.copy(alpha = ContentAlpha.disabled),
    ): ToggleButtonColors = DefaultToggleButtonColors(
        checkedBackgroundColor = checkedBackgroundColor,
        checkedContentColor = checkedContentColor,
        disabledCheckedBackgroundColor = disabledCheckedBackgroundColor,
        disabledCheckedContentColor = disabledCheckedContentColor,
        uncheckedBackgroundColor = uncheckedBackgroundColor,
        uncheckedContentColor = uncheckedContentColor,
        disabledUncheckedBackgroundColor = disabledUncheckedBackgroundColor,
        disabledUncheckedContentColor = disabledUncheckedContentColor,
    )
}

/**
 * Default [ToggleButtonColors] implementation.
 */
@Immutable
private class DefaultToggleButtonColors(
    private val checkedBackgroundColor: Color,
    private val checkedContentColor: Color,
    private val disabledCheckedBackgroundColor: Color,
    private val disabledCheckedContentColor: Color,
    private val uncheckedBackgroundColor: Color,
    private val uncheckedContentColor: Color,
    private val disabledUncheckedBackgroundColor: Color,
    private val disabledUncheckedContentColor: Color,
) : ToggleButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedBackgroundColor else uncheckedBackgroundColor
            } else {
                if (checked) disabledCheckedBackgroundColor else disabledUncheckedBackgroundColor
            }
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedContentColor else uncheckedContentColor
            } else {
                if (checked) disabledCheckedContentColor else disabledUncheckedContentColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultToggleButtonColors

        if (checkedBackgroundColor != other.checkedBackgroundColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (disabledCheckedBackgroundColor != other.disabledCheckedBackgroundColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (uncheckedBackgroundColor != other.uncheckedBackgroundColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (disabledUncheckedBackgroundColor != other.disabledUncheckedBackgroundColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedBackgroundColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + disabledCheckedBackgroundColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + uncheckedBackgroundColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedBackgroundColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        return result
    }
}
