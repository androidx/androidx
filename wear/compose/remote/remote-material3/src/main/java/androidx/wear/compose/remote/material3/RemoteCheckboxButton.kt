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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.semantics.Role

/**
 * The Wear Material [RemoteCheckboxButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and a checkbox toggle control. The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots and a Checkbox at the end.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCheckboxButtonSample
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this button is clicked.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. Note that only constant values are currently supported for [enabled] for click
 *   handling.
 * @param shape Defines the button's shape.
 * @param colors [RemoteCheckboxButtonColors] that will be used to resolve the colors used for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param border Optional [RemoteDp] border stroke width.
 * @param borderColor Optional [RemoteColor] border color.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button.
 * @param secondaryLabel A slot for providing the button's secondary label.
 * @param label A slot for providing the button's main label.
 */
@Composable
@RemoteComposable
public fun RemoteCheckboxButton(
    checked: RemoteBoolean,
    onCheckedChange: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCheckboxButtonDefaults.checkboxButtonShape,
    colors: RemoteCheckboxButtonColors = RemoteCheckboxButtonDefaults.checkboxButtonColors(),
    contentPadding: RemotePaddingValues = RemoteCheckboxButtonDefaults.ContentPadding,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = checked.select(1f.rf, 0f.rf)

    RemoteSelectionButtonImpl(
        onClick = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor =
            colors.containerColor(enabled = enabled, checked = checked, progress = progress),
        contentColor =
            colors.contentColor(enabled = enabled, checked = checked, progress = progress),
        secondaryContentColor =
            colors.secondaryContentColor(enabled = enabled, checked = checked, progress = progress),
        contentPadding = contentPadding,
        border = border,
        borderColor = borderColor,
        role = Role.Checkbox,
        icon = icon,
        secondaryLabel = secondaryLabel,
        label = label,
        selectionControl = {
            RemoteCheckboxControl(
                checked = checked,
                boxColor =
                    colors.boxColor(enabled = enabled, checked = checked, progress = progress),
                checkmarkColor = colors.checkmarkColor(enabled = enabled),
                progress = progress,
            )
        },
    )
}

/** Contains the default values used by [RemoteCheckboxButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteCheckboxButtonDefaults {
    /** The default height of [RemoteCheckboxButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default spacing between an icon and label in [RemoteCheckboxButton]. */
    public val IconSpacing: RemoteDp = 6.rdp

    /** The recommended default size for icons when used inside [RemoteCheckboxButton]. */
    public val IconSize: RemoteDp = 24.rdp

    /** The default spacing between label and secondary label in [RemoteCheckboxButton]. */
    public val LabelSpacerSize: RemoteDp = 1.rdp

    /** The default content padding used by [RemoteCheckboxButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    /** The default shape for [RemoteCheckboxButton]. */
    public val checkboxButtonShape: RemoteRoundedCornerShape
        @Composable get() = RemoteShapeDefaults.Large

    /** The size of the inner box of the checkbox. */
    public val BoxSize: RemoteDp = 18.rdp

    /** The stroke width of the checkbox border. */
    public val BoxStroke: RemoteDp = 2.rdp

    /** The corner radius of the checkbox box. */
    public val BoxRadius: RemoteDp = 2.rdp

    /** The outer size of the checkbox touch/draw target. */
    public val BoxOuterSize: RemoteDp = 24.rdp

    /** Creates a [RemoteCheckboxButtonColors] with default values for [RemoteCheckboxButton]. */
    @Composable
    public fun checkboxButtonColors(): RemoteCheckboxButtonColors =
        RemoteMaterialTheme.colorScheme.defaultCheckboxButtonColors

    /**
     * Creates a [RemoteCheckboxButtonColors] with customized colors for [RemoteCheckboxButton].
     *
     * @param checkedContainerColor Container color when checked and enabled.
     * @param checkedContentColor Content color when checked and enabled.
     * @param checkedSecondaryContentColor Secondary content color when checked and enabled.
     * @param checkedIconColor Icon color when checked and enabled.
     * @param checkedBoxColor Box color when checked and enabled.
     * @param checkedCheckmarkColor Checkmark color when checked and enabled.
     * @param uncheckedContainerColor Container color when unchecked and enabled.
     * @param uncheckedContentColor Content color when unchecked and enabled.
     * @param uncheckedSecondaryContentColor Secondary content color when unchecked and enabled.
     * @param uncheckedIconColor Icon color when unchecked and enabled.
     * @param uncheckedBoxColor Box color when unchecked and enabled.
     * @param disabledCheckedContainerColor Container color when checked and disabled.
     * @param disabledCheckedContentColor Content color when checked and disabled.
     * @param disabledCheckedSecondaryContentColor Secondary content color when checked and
     *   disabled.
     * @param disabledCheckedIconColor Icon color when checked and disabled.
     * @param disabledCheckedBoxColor Box color when checked and disabled.
     * @param disabledCheckedCheckmarkColor Checkmark color when checked and disabled.
     * @param disabledUncheckedContainerColor Container color when unchecked and disabled.
     * @param disabledUncheckedContentColor Content color when unchecked and disabled.
     * @param disabledUncheckedSecondaryContentColor Secondary content color when unchecked and
     *   disabled.
     * @param disabledUncheckedIconColor Icon color when unchecked and disabled.
     * @param disabledUncheckedBoxColor Box color when unchecked and disabled.
     */
    @Composable
    public fun checkboxButtonColors(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedIconColor: RemoteColor? = null,
        checkedBoxColor: RemoteColor? = null,
        checkedCheckmarkColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedIconColor: RemoteColor? = null,
        uncheckedBoxColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedIconColor: RemoteColor? = null,
        disabledCheckedBoxColor: RemoteColor? = null,
        disabledCheckedCheckmarkColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedIconColor: RemoteColor? = null,
        disabledUncheckedBoxColor: RemoteColor? = null,
    ): RemoteCheckboxButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultCheckboxButtonColors
        return default.copy(
            checkedContainerColor = checkedContainerColor ?: default.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: default.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: default.checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor ?: default.checkedIconColor,
            checkedBoxColor = checkedBoxColor ?: default.checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor ?: default.checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor ?: default.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: default.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: default.uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor ?: default.uncheckedIconColor,
            uncheckedBoxColor = uncheckedBoxColor ?: default.uncheckedBoxColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: default.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: default.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor
                    ?: default.disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor ?: default.disabledCheckedIconColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor ?: default.disabledCheckedBoxColor,
            disabledCheckedCheckmarkColor =
                disabledCheckedCheckmarkColor ?: default.disabledCheckedCheckmarkColor,
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor ?: default.disabledUncheckedContainerColor,
            disabledUncheckedContentColor =
                disabledUncheckedContentColor ?: default.disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor
                    ?: default.disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor =
                disabledUncheckedIconColor ?: default.disabledUncheckedIconColor,
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor ?: default.disabledUncheckedBoxColor,
        )
    }

    private val RemoteColorScheme.defaultCheckboxButtonColors: RemoteCheckboxButtonColors
        @Composable
        get() {
            return RemoteCheckboxButtonColors(
                checkedContainerColor = primaryContainer,
                checkedContentColor = onPrimaryContainer,
                checkedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                checkedIconColor = primary,
                checkedBoxColor = primary,
                checkedCheckmarkColor = onPrimary,
                uncheckedContainerColor = surfaceContainer,
                uncheckedContentColor = onSurface,
                uncheckedSecondaryContentColor = onSurfaceVariant,
                uncheckedIconColor = primary,
                uncheckedBoxColor = outline,
                disabledCheckedContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedBoxColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedCheckmarkColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedContainerColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedBoxColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

/**
 * Represents the container, content, and control colors used in [RemoteCheckboxButton] in various
 * states.
 *
 * @param checkedContainerColor Container color when checked and enabled.
 * @param checkedContentColor Content color when checked and enabled.
 * @param checkedSecondaryContentColor Secondary content color when checked and enabled.
 * @param checkedIconColor Icon color when checked and enabled.
 * @param checkedBoxColor Box color when checked and enabled.
 * @param checkedCheckmarkColor Checkmark color when checked and enabled.
 * @param uncheckedContainerColor Container color when unchecked and enabled.
 * @param uncheckedContentColor Content color when unchecked and enabled.
 * @param uncheckedSecondaryContentColor Secondary content color when unchecked and enabled.
 * @param uncheckedIconColor Icon color when unchecked and enabled.
 * @param uncheckedBoxColor Box color when unchecked and enabled.
 * @param disabledCheckedContainerColor Container color when checked and disabled.
 * @param disabledCheckedContentColor Content color when checked and disabled.
 * @param disabledCheckedSecondaryContentColor Secondary content color when checked and disabled.
 * @param disabledCheckedIconColor Icon color when checked and disabled.
 * @param disabledCheckedBoxColor Box color when checked and disabled.
 * @param disabledCheckedCheckmarkColor Checkmark color when checked and disabled.
 * @param disabledUncheckedContainerColor Container color when unchecked and disabled.
 * @param disabledUncheckedContentColor Content color when unchecked and disabled.
 * @param disabledUncheckedSecondaryContentColor Secondary content color when unchecked and
 *   disabled.
 * @param disabledUncheckedIconColor Icon color when unchecked and disabled.
 * @param disabledUncheckedBoxColor Box color when unchecked and disabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCheckboxButtonColors(
    public val checkedContainerColor: RemoteColor,
    public val checkedContentColor: RemoteColor,
    public val checkedSecondaryContentColor: RemoteColor,
    public val checkedIconColor: RemoteColor,
    public val checkedBoxColor: RemoteColor,
    public val checkedCheckmarkColor: RemoteColor,
    public val uncheckedContainerColor: RemoteColor,
    public val uncheckedContentColor: RemoteColor,
    public val uncheckedSecondaryContentColor: RemoteColor,
    public val uncheckedIconColor: RemoteColor,
    public val uncheckedBoxColor: RemoteColor,
    public val disabledCheckedContainerColor: RemoteColor,
    public val disabledCheckedContentColor: RemoteColor,
    public val disabledCheckedSecondaryContentColor: RemoteColor,
    public val disabledCheckedIconColor: RemoteColor,
    public val disabledCheckedBoxColor: RemoteColor,
    public val disabledCheckedCheckmarkColor: RemoteColor,
    public val disabledUncheckedContainerColor: RemoteColor,
    public val disabledUncheckedContentColor: RemoteColor,
    public val disabledUncheckedSecondaryContentColor: RemoteColor,
    public val disabledUncheckedIconColor: RemoteColor,
    public val disabledUncheckedBoxColor: RemoteColor,
) {
    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedContainerColor, checkedContainerColor, it) }
                    ?: checked.select(checkedContainerColor, uncheckedContainerColor),
            ifFalse = checked.select(disabledCheckedContainerColor, disabledUncheckedContainerColor),
        )

    @Stable
    internal fun contentColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedContentColor, checkedContentColor, it) }
                    ?: checked.select(checkedContentColor, uncheckedContentColor),
            ifFalse = checked.select(disabledCheckedContentColor, disabledUncheckedContentColor),
        )

    @Stable
    internal fun secondaryContentColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let {
                    tween(uncheckedSecondaryContentColor, checkedSecondaryContentColor, it)
                } ?: checked.select(checkedSecondaryContentColor, uncheckedSecondaryContentColor),
            ifFalse =
                checked.select(
                    disabledCheckedSecondaryContentColor,
                    disabledUncheckedSecondaryContentColor,
                ),
        )

    @Stable
    internal fun iconColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedIconColor, checkedIconColor, it) }
                    ?: checked.select(checkedIconColor, uncheckedIconColor),
            ifFalse = checked.select(disabledCheckedIconColor, disabledUncheckedIconColor),
        )

    @Stable
    internal fun boxColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedBoxColor, checkedBoxColor, it) }
                    ?: checked.select(checkedBoxColor, uncheckedBoxColor),
            ifFalse = checked.select(disabledCheckedBoxColor, disabledUncheckedBoxColor),
        )

    @Stable
    internal fun checkmarkColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(checkedCheckmarkColor, disabledCheckedCheckmarkColor)

    /** Returns a copy of this [RemoteCheckboxButtonColors] optionally overriding some values. */
    public fun copy(
        checkedContainerColor: RemoteColor? = this.checkedContainerColor,
        checkedContentColor: RemoteColor? = this.checkedContentColor,
        checkedSecondaryContentColor: RemoteColor? = this.checkedSecondaryContentColor,
        checkedIconColor: RemoteColor? = this.checkedIconColor,
        checkedBoxColor: RemoteColor? = this.checkedBoxColor,
        checkedCheckmarkColor: RemoteColor? = this.checkedCheckmarkColor,
        uncheckedContainerColor: RemoteColor? = this.uncheckedContainerColor,
        uncheckedContentColor: RemoteColor? = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: RemoteColor? = this.uncheckedSecondaryContentColor,
        uncheckedIconColor: RemoteColor? = this.uncheckedIconColor,
        uncheckedBoxColor: RemoteColor? = this.uncheckedBoxColor,
        disabledCheckedContainerColor: RemoteColor? = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: RemoteColor? = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: RemoteColor? =
            this.disabledCheckedSecondaryContentColor,
        disabledCheckedIconColor: RemoteColor? = this.disabledCheckedIconColor,
        disabledCheckedBoxColor: RemoteColor? = this.disabledCheckedBoxColor,
        disabledCheckedCheckmarkColor: RemoteColor? = this.disabledCheckedCheckmarkColor,
        disabledUncheckedContainerColor: RemoteColor? = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: RemoteColor? = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: RemoteColor? =
            this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedIconColor: RemoteColor? = this.disabledUncheckedIconColor,
        disabledUncheckedBoxColor: RemoteColor? = this.disabledUncheckedBoxColor,
    ): RemoteCheckboxButtonColors =
        RemoteCheckboxButtonColors(
            checkedContainerColor = checkedContainerColor ?: this.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: this.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: this.checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor ?: this.checkedIconColor,
            checkedBoxColor = checkedBoxColor ?: this.checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor ?: this.checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor ?: this.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: this.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: this.uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor ?: this.uncheckedIconColor,
            uncheckedBoxColor = uncheckedBoxColor ?: this.uncheckedBoxColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: this.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: this.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor ?: this.disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor ?: this.disabledCheckedIconColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor ?: this.disabledCheckedBoxColor,
            disabledCheckedCheckmarkColor =
                disabledCheckedCheckmarkColor ?: this.disabledCheckedCheckmarkColor,
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor ?: this.disabledUncheckedContainerColor,
            disabledUncheckedContentColor =
                disabledUncheckedContentColor ?: this.disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor
                    ?: this.disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor =
                disabledUncheckedIconColor ?: this.disabledUncheckedIconColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor ?: this.disabledUncheckedBoxColor,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteCheckboxButtonColors) return false

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor) {
            return false
        }
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledCheckedCheckmarkColor != other.disabledCheckedCheckmarkColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (
            disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor
        ) {
            return false
        }
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        return result
    }
}
