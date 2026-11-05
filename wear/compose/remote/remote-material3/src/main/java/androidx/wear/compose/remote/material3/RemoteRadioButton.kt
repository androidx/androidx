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
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** @sample androidx.wear.compose.remote.material3.samples.RemoteRadioButtonSample */
@Composable
@RemoteComposable
public fun RemoteRadioButton(
    selected: RemoteBoolean,
    onSelect: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteRadioButtonDefaults.radioButtonShape,
    colors: RemoteRadioButtonColors = RemoteRadioButtonDefaults.radioButtonColors(),
    contentPadding: RemotePaddingValues = RemoteRadioButtonDefaults.ContentPadding,
    border: RemoteBorderStroke? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = animateSelectionControlProgress(selected)
    val colorProgress = animateSelectionColorProgress(selected)

    RemoteSelectionButtonImpl(
        onClick = onSelect,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor =
            colors.containerColor(enabled = enabled, selected = selected, progress = colorProgress),
        contentColor =
            colors.contentColor(enabled = enabled, selected = selected, progress = colorProgress),
        secondaryContentColor =
            colors.secondaryContentColor(
                enabled = enabled,
                selected = selected,
                progress = colorProgress,
            ),
        iconColor =
            colors.iconColor(enabled = enabled, selected = selected, progress = colorProgress),
        contentPadding = contentPadding,
        border = border,
        role = RemoteRole.RadioButton,
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteRadioButtonDefaults {
    public val Height: RemoteDp = 52.rdp

    public val IconSpacing: RemoteDp = 6.rdp

    public val IconSize: RemoteDp = 24.rdp

    public val LabelSpacerSize: RemoteDp = 1.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    public val radioButtonShape: RemoteRoundedCornerShape = RemoteShapeDefaults.Large

    public val CircleRadius: RemoteDp = 9.rdp

    public val CircleStroke: RemoteDp = 2.rdp

    public val DotRadius: RemoteDp = 5.rdp

    public val ControlSize: RemoteDp = 24.rdp

    @Composable
    @RemoteComposable
    public fun radioButtonColors(): RemoteRadioButtonColors =
        RemoteMaterialTheme.colorScheme.defaultRadioButtonColors

    @Composable
    @RemoteComposable
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
        @RemoteComposable
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
                disabledUnselectedControlColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
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
                progress?.let { lerp(unselectedContainerColor, selectedContainerColor, it) }
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
                progress?.let { lerp(unselectedContentColor, selectedContentColor, it) }
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
                    lerp(unselectedSecondaryContentColor, selectedSecondaryContentColor, it)
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
                progress?.let { lerp(unselectedIconColor, selectedIconColor, it) }
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
                progress?.let { lerp(unselectedControlColor, selectedControlColor, it) }
                    ?: selected.select(selectedControlColor, unselectedControlColor),
            ifFalse = selected.select(disabledSelectedControlColor, disabledUnselectedControlColor),
        )

    public fun copy(
        selectedContainerColor: RemoteColor = this.selectedContainerColor,
        selectedContentColor: RemoteColor = this.selectedContentColor,
        selectedSecondaryContentColor: RemoteColor = this.selectedSecondaryContentColor,
        selectedIconColor: RemoteColor = this.selectedIconColor,
        selectedControlColor: RemoteColor = this.selectedControlColor,
        unselectedContainerColor: RemoteColor = this.unselectedContainerColor,
        unselectedContentColor: RemoteColor = this.unselectedContentColor,
        unselectedSecondaryContentColor: RemoteColor = this.unselectedSecondaryContentColor,
        unselectedIconColor: RemoteColor = this.unselectedIconColor,
        unselectedControlColor: RemoteColor = this.unselectedControlColor,
        disabledSelectedContainerColor: RemoteColor = this.disabledSelectedContainerColor,
        disabledSelectedContentColor: RemoteColor = this.disabledSelectedContentColor,
        disabledSelectedSecondaryContentColor: RemoteColor =
            this.disabledSelectedSecondaryContentColor,
        disabledSelectedIconColor: RemoteColor = this.disabledSelectedIconColor,
        disabledSelectedControlColor: RemoteColor = this.disabledSelectedControlColor,
        disabledUnselectedContainerColor: RemoteColor = this.disabledUnselectedContainerColor,
        disabledUnselectedContentColor: RemoteColor = this.disabledUnselectedContentColor,
        disabledUnselectedSecondaryContentColor: RemoteColor =
            this.disabledUnselectedSecondaryContentColor,
        disabledUnselectedIconColor: RemoteColor = this.disabledUnselectedIconColor,
        disabledUnselectedControlColor: RemoteColor = this.disabledUnselectedControlColor,
    ): RemoteRadioButtonColors =
        RemoteRadioButtonColors(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedIconColor = selectedIconColor,
            selectedControlColor = selectedControlColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedIconColor = unselectedIconColor,
            unselectedControlColor = unselectedControlColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor = disabledSelectedSecondaryContentColor,
            disabledSelectedIconColor = disabledSelectedIconColor,
            disabledSelectedControlColor = disabledSelectedControlColor,
            disabledUnselectedContainerColor = disabledUnselectedContainerColor,
            disabledUnselectedContentColor = disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor = disabledUnselectedSecondaryContentColor,
            disabledUnselectedIconColor = disabledUnselectedIconColor,
            disabledUnselectedControlColor = disabledUnselectedControlColor,
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
