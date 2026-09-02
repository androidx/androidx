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
 * The Wear Material [RemoteRadioButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and a radio selection control. The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots and a Radio control at the end.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteRadioButtonSample
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelect Callback to be invoked when this button is clicked.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. Note that only constant values are currently supported for [enabled] for click
 *   handling.
 * @param shape Defines the button's shape.
 * @param colors [RemoteRadioButtonColors] that will be used to resolve the colors used for this
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
public fun RemoteRadioButton(
    selected: RemoteBoolean,
    onSelect: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteRadioButtonDefaults.radioButtonShape,
    colors: RemoteRadioButtonColors = RemoteRadioButtonDefaults.radioButtonColors(),
    contentPadding: RemotePaddingValues = RemoteRadioButtonDefaults.ContentPadding,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = selected.select(1f.rf, 0f.rf)

    RemoteSelectionButtonImpl(
        onClick = onSelect,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor =
            colors.containerColor(enabled = enabled, selected = selected, progress = progress),
        contentColor =
            colors.contentColor(enabled = enabled, selected = selected, progress = progress),
        secondaryContentColor =
            colors.secondaryContentColor(
                enabled = enabled,
                selected = selected,
                progress = progress,
            ),
        contentPadding = contentPadding,
        border = border,
        borderColor = borderColor,
        role = Role.RadioButton,
        icon = icon,
        secondaryLabel = secondaryLabel,
        label = label,
        selectionControl = {
            RemoteRadioControl(
                selected = selected,
                controlColor =
                    colors.controlColor(
                        enabled = enabled,
                        selected = selected,
                        progress = progress,
                    ),
                progress = progress,
            )
        },
    )
}

/** Contains the default values used by [RemoteRadioButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteRadioButtonDefaults {
    /** The default height of [RemoteRadioButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default spacing between an icon and label in [RemoteRadioButton]. */
    public val IconSpacing: RemoteDp = 6.rdp

    /** The recommended default size for icons when used inside [RemoteRadioButton]. */
    public val IconSize: RemoteDp = 24.rdp

    /** The default spacing between label and secondary label in [RemoteRadioButton]. */
    public val LabelSpacerSize: RemoteDp = 1.rdp

    /** The default content padding used by [RemoteRadioButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    /** The default shape for [RemoteRadioButton]. */
    public val radioButtonShape: RemoteRoundedCornerShape
        @Composable get() = RemoteShapeDefaults.Large

    /** The outer radius of the radio button ring. */
    public val CircleRadius: RemoteDp = 9.rdp

    /** The stroke width of the radio button ring. */
    public val CircleStroke: RemoteDp = 2.rdp

    /** The radius of the inner dot when selected. */
    public val DotRadius: RemoteDp = 5.rdp

    /** The outer size of the radio button control. */
    public val ControlSize: RemoteDp = 24.rdp

    /** Creates a [RemoteRadioButtonColors] with default values for [RemoteRadioButton]. */
    @Composable
    public fun radioButtonColors(): RemoteRadioButtonColors =
        RemoteMaterialTheme.colorScheme.defaultRadioButtonColors

    /**
     * Creates a [RemoteRadioButtonColors] with customized colors for [RemoteRadioButton].
     *
     * @param selectedContainerColor Container color when selected and enabled.
     * @param selectedContentColor Content color when selected and enabled.
     * @param selectedSecondaryContentColor Secondary content color when selected and enabled.
     * @param selectedIconColor Icon color when selected and enabled.
     * @param selectedControlColor Control color when selected and enabled.
     * @param unselectedContainerColor Container color when unselected and enabled.
     * @param unselectedContentColor Content color when unselected and enabled.
     * @param unselectedSecondaryContentColor Secondary content color when unselected and enabled.
     * @param unselectedIconColor Icon color when unselected and enabled.
     * @param unselectedControlColor Control color when unselected and enabled.
     * @param disabledSelectedContainerColor Container color when selected and disabled.
     * @param disabledSelectedContentColor Content color when selected and disabled.
     * @param disabledSelectedSecondaryContentColor Secondary content color when selected and
     *   disabled.
     * @param disabledSelectedIconColor Icon color when selected and disabled.
     * @param disabledSelectedControlColor Control color when selected and disabled.
     * @param disabledUnselectedContainerColor Container color when unselected and disabled.
     * @param disabledUnselectedContentColor Content color when unselected and disabled.
     * @param disabledUnselectedSecondaryContentColor Secondary content color when unselected and
     *   disabled.
     * @param disabledUnselectedIconColor Icon color when unselected and disabled.
     * @param disabledUnselectedControlColor Control color when unselected and disabled.
     */
    @Composable
    public fun radioButtonColors(
        selectedContainerColor: RemoteColor? = null,
        selectedContentColor: RemoteColor? = null,
        selectedSecondaryContentColor: RemoteColor? = null,
        selectedIconColor: RemoteColor? = null,
        selectedControlColor: RemoteColor? = null,
        unselectedContainerColor: RemoteColor? = null,
        unselectedContentColor: RemoteColor? = null,
        unselectedSecondaryContentColor: RemoteColor? = null,
        unselectedIconColor: RemoteColor? = null,
        unselectedControlColor: RemoteColor? = null,
        disabledSelectedContainerColor: RemoteColor? = null,
        disabledSelectedContentColor: RemoteColor? = null,
        disabledSelectedSecondaryContentColor: RemoteColor? = null,
        disabledSelectedIconColor: RemoteColor? = null,
        disabledSelectedControlColor: RemoteColor? = null,
        disabledUnselectedContainerColor: RemoteColor? = null,
        disabledUnselectedContentColor: RemoteColor? = null,
        disabledUnselectedSecondaryContentColor: RemoteColor? = null,
        disabledUnselectedIconColor: RemoteColor? = null,
        disabledUnselectedControlColor: RemoteColor? = null,
    ): RemoteRadioButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultRadioButtonColors
        return default.copy(
            selectedContainerColor = selectedContainerColor ?: default.selectedContainerColor,
            selectedContentColor = selectedContentColor ?: default.selectedContentColor,
            selectedSecondaryContentColor =
                selectedSecondaryContentColor ?: default.selectedSecondaryContentColor,
            selectedIconColor = selectedIconColor ?: default.selectedIconColor,
            selectedControlColor = selectedControlColor ?: default.selectedControlColor,
            unselectedContainerColor = unselectedContainerColor ?: default.unselectedContainerColor,
            unselectedContentColor = unselectedContentColor ?: default.unselectedContentColor,
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor ?: default.unselectedSecondaryContentColor,
            unselectedIconColor = unselectedIconColor ?: default.unselectedIconColor,
            unselectedControlColor = unselectedControlColor ?: default.unselectedControlColor,
            disabledSelectedContainerColor =
                disabledSelectedContainerColor ?: default.disabledSelectedContainerColor,
            disabledSelectedContentColor =
                disabledSelectedContentColor ?: default.disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor
                    ?: default.disabledSelectedSecondaryContentColor,
            disabledSelectedIconColor =
                disabledSelectedIconColor ?: default.disabledSelectedIconColor,
            disabledSelectedControlColor =
                disabledSelectedControlColor ?: default.disabledSelectedControlColor,
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor ?: default.disabledUnselectedContainerColor,
            disabledUnselectedContentColor =
                disabledUnselectedContentColor ?: default.disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor
                    ?: default.disabledUnselectedSecondaryContentColor,
            disabledUnselectedIconColor =
                disabledUnselectedIconColor ?: default.disabledUnselectedIconColor,
            disabledUnselectedControlColor =
                disabledUnselectedControlColor ?: default.disabledUnselectedControlColor,
        )
    }

    private val RemoteColorScheme.defaultRadioButtonColors: RemoteRadioButtonColors
        @Composable
        get() {
            return RemoteRadioButtonColors(
                selectedContainerColor = primaryContainer,
                selectedContentColor = onPrimaryContainer,
                selectedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                selectedIconColor = primary,
                selectedControlColor = primary,
                unselectedContainerColor = surfaceContainer,
                unselectedContentColor = onSurface,
                unselectedSecondaryContentColor = onSurfaceVariant,
                unselectedIconColor = primary,
                unselectedControlColor = outline,
                disabledSelectedContainerColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledSelectedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSelectedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSelectedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSelectedControlColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUnselectedContainerColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUnselectedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUnselectedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUnselectedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUnselectedControlColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

/**
 * Represents the container, content, and control colors used in [RemoteRadioButton] in various
 * states.
 *
 * @param selectedContainerColor Container color when selected and enabled.
 * @param selectedContentColor Content color when selected and enabled.
 * @param selectedSecondaryContentColor Secondary content color when selected and enabled.
 * @param selectedIconColor Icon color when selected and enabled.
 * @param selectedControlColor Control color when selected and enabled.
 * @param unselectedContainerColor Container color when unselected and enabled.
 * @param unselectedContentColor Content color when unselected and enabled.
 * @param unselectedSecondaryContentColor Secondary content color when unselected and enabled.
 * @param unselectedIconColor Icon color when unselected and enabled.
 * @param unselectedControlColor Control color when unselected and enabled.
 * @param disabledSelectedContainerColor Container color when selected and disabled.
 * @param disabledSelectedContentColor Content color when selected and disabled.
 * @param disabledSelectedSecondaryContentColor Secondary content color when selected and disabled.
 * @param disabledSelectedIconColor Icon color when selected and disabled.
 * @param disabledSelectedControlColor Control color when selected and disabled.
 * @param disabledUnselectedContainerColor Container color when unselected and disabled.
 * @param disabledUnselectedContentColor Content color when unselected and disabled.
 * @param disabledUnselectedSecondaryContentColor Secondary content color when unselected and
 *   disabled.
 * @param disabledUnselectedIconColor Icon color when unselected and disabled.
 * @param disabledUnselectedControlColor Control color when unselected and disabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteRadioButtonColors(
    public val selectedContainerColor: RemoteColor,
    public val selectedContentColor: RemoteColor,
    public val selectedSecondaryContentColor: RemoteColor,
    public val selectedIconColor: RemoteColor,
    public val selectedControlColor: RemoteColor,
    public val unselectedContainerColor: RemoteColor,
    public val unselectedContentColor: RemoteColor,
    public val unselectedSecondaryContentColor: RemoteColor,
    public val unselectedIconColor: RemoteColor,
    public val unselectedControlColor: RemoteColor,
    public val disabledSelectedContainerColor: RemoteColor,
    public val disabledSelectedContentColor: RemoteColor,
    public val disabledSelectedSecondaryContentColor: RemoteColor,
    public val disabledSelectedIconColor: RemoteColor,
    public val disabledSelectedControlColor: RemoteColor,
    public val disabledUnselectedContainerColor: RemoteColor,
    public val disabledUnselectedContentColor: RemoteColor,
    public val disabledUnselectedSecondaryContentColor: RemoteColor,
    public val disabledUnselectedIconColor: RemoteColor,
    public val disabledUnselectedControlColor: RemoteColor,
) {
    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean,
        selected: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(unselectedContainerColor, selectedContainerColor, it) }
                    ?: selected.select(selectedContainerColor, unselectedContainerColor),
            ifFalse =
                selected.select(disabledSelectedContainerColor, disabledUnselectedContainerColor),
        )

    @Stable
    internal fun contentColor(
        enabled: RemoteBoolean,
        selected: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(unselectedContentColor, selectedContentColor, it) }
                    ?: selected.select(selectedContentColor, unselectedContentColor),
            ifFalse = selected.select(disabledSelectedContentColor, disabledUnselectedContentColor),
        )

    @Stable
    internal fun secondaryContentColor(
        enabled: RemoteBoolean,
        selected: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let {
                    tween(unselectedSecondaryContentColor, selectedSecondaryContentColor, it)
                }
                    ?: selected.select(
                        selectedSecondaryContentColor,
                        unselectedSecondaryContentColor,
                    ),
            ifFalse =
                selected.select(
                    disabledSelectedSecondaryContentColor,
                    disabledUnselectedSecondaryContentColor,
                ),
        )

    @Stable
    internal fun iconColor(
        enabled: RemoteBoolean,
        selected: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(unselectedIconColor, selectedIconColor, it) }
                    ?: selected.select(selectedIconColor, unselectedIconColor),
            ifFalse = selected.select(disabledSelectedIconColor, disabledUnselectedIconColor),
        )

    @Stable
    internal fun controlColor(
        enabled: RemoteBoolean,
        selected: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(unselectedControlColor, selectedControlColor, it) }
                    ?: selected.select(selectedControlColor, unselectedControlColor),
            ifFalse = selected.select(disabledSelectedControlColor, disabledUnselectedControlColor),
        )

    /** Returns a copy of this [RemoteRadioButtonColors] optionally overriding some values. */
    public fun copy(
        selectedContainerColor: RemoteColor? = this.selectedContainerColor,
        selectedContentColor: RemoteColor? = this.selectedContentColor,
        selectedSecondaryContentColor: RemoteColor? = this.selectedSecondaryContentColor,
        selectedIconColor: RemoteColor? = this.selectedIconColor,
        selectedControlColor: RemoteColor? = this.selectedControlColor,
        unselectedContainerColor: RemoteColor? = this.unselectedContainerColor,
        unselectedContentColor: RemoteColor? = this.unselectedContentColor,
        unselectedSecondaryContentColor: RemoteColor? = this.unselectedSecondaryContentColor,
        unselectedIconColor: RemoteColor? = this.unselectedIconColor,
        unselectedControlColor: RemoteColor? = this.unselectedControlColor,
        disabledSelectedContainerColor: RemoteColor? = this.disabledSelectedContainerColor,
        disabledSelectedContentColor: RemoteColor? = this.disabledSelectedContentColor,
        disabledSelectedSecondaryContentColor: RemoteColor? =
            this.disabledSelectedSecondaryContentColor,
        disabledSelectedIconColor: RemoteColor? = this.disabledSelectedIconColor,
        disabledSelectedControlColor: RemoteColor? = this.disabledSelectedControlColor,
        disabledUnselectedContainerColor: RemoteColor? = this.disabledUnselectedContainerColor,
        disabledUnselectedContentColor: RemoteColor? = this.disabledUnselectedContentColor,
        disabledUnselectedSecondaryContentColor: RemoteColor? =
            this.disabledUnselectedSecondaryContentColor,
        disabledUnselectedIconColor: RemoteColor? = this.disabledUnselectedIconColor,
        disabledUnselectedControlColor: RemoteColor? = this.disabledUnselectedControlColor,
    ): RemoteRadioButtonColors =
        RemoteRadioButtonColors(
            selectedContainerColor = selectedContainerColor ?: this.selectedContainerColor,
            selectedContentColor = selectedContentColor ?: this.selectedContentColor,
            selectedSecondaryContentColor =
                selectedSecondaryContentColor ?: this.selectedSecondaryContentColor,
            selectedIconColor = selectedIconColor ?: this.selectedIconColor,
            selectedControlColor = selectedControlColor ?: this.selectedControlColor,
            unselectedContainerColor = unselectedContainerColor ?: this.unselectedContainerColor,
            unselectedContentColor = unselectedContentColor ?: this.unselectedContentColor,
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor ?: this.unselectedSecondaryContentColor,
            unselectedIconColor = unselectedIconColor ?: this.unselectedIconColor,
            unselectedControlColor = unselectedControlColor ?: this.unselectedControlColor,
            disabledSelectedContainerColor =
                disabledSelectedContainerColor ?: this.disabledSelectedContainerColor,
            disabledSelectedContentColor =
                disabledSelectedContentColor ?: this.disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor ?: this.disabledSelectedSecondaryContentColor,
            disabledSelectedIconColor = disabledSelectedIconColor ?: this.disabledSelectedIconColor,
            disabledSelectedControlColor =
                disabledSelectedControlColor ?: this.disabledSelectedControlColor,
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor ?: this.disabledUnselectedContainerColor,
            disabledUnselectedContentColor =
                disabledUnselectedContentColor ?: this.disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor
                    ?: this.disabledUnselectedSecondaryContentColor,
            disabledUnselectedIconColor =
                disabledUnselectedIconColor ?: this.disabledUnselectedIconColor,
            disabledUnselectedControlColor =
                disabledUnselectedControlColor ?: this.disabledUnselectedControlColor,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteRadioButtonColors) return false

        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedSecondaryContentColor != other.selectedSecondaryContentColor) return false
        if (selectedIconColor != other.selectedIconColor) return false
        if (selectedControlColor != other.selectedControlColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (unselectedControlColor != other.unselectedControlColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor != other.disabledSelectedSecondaryContentColor) {
            return false
        }
        if (disabledSelectedIconColor != other.disabledSelectedIconColor) return false
        if (disabledSelectedControlColor != other.disabledSelectedControlColor) return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (
            disabledUnselectedSecondaryContentColor != other.disabledUnselectedSecondaryContentColor
        ) {
            return false
        }
        if (disabledUnselectedIconColor != other.disabledUnselectedIconColor) return false
        if (disabledUnselectedControlColor != other.disabledUnselectedControlColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedIconColor.hashCode()
        result = 31 * result + selectedControlColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + unselectedControlColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedIconColor.hashCode()
        result = 31 * result + disabledSelectedControlColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedIconColor.hashCode()
        result = 31 * result + disabledUnselectedControlColor.hashCode()
        return result
    }
}
