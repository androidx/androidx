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

/** @sample androidx.wear.compose.remote.material3.samples.RemoteCheckboxButtonSample */
@Composable
@RemoteComposable
public fun RemoteCheckboxButton(
    checked: RemoteBoolean,
    onCheckedChange: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCheckboxButtonDefaults.checkboxButtonShape,
    colors: RemoteCheckboxButtonColors = RemoteCheckboxButtonDefaults.checkboxButtonColors(),
    contentPadding: RemotePaddingValues = RemoteCheckboxButtonDefaults.ContentPadding,
    border: RemoteBorderStroke? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = animateSelectionControlProgress(checked)
    val colorProgress = animateSelectionColorProgress(checked)

    RemoteSelectionButtonImpl(
        onClick = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor =
            colors.containerColor(enabled = enabled, checked = checked, progress = colorProgress),
        contentColor =
            colors.contentColor(enabled = enabled, checked = checked, progress = colorProgress),
        secondaryContentColor =
            colors.secondaryContentColor(
                enabled = enabled,
                checked = checked,
                progress = colorProgress,
            ),
        iconColor =
            colors.iconColor(enabled = enabled, checked = checked, progress = colorProgress),
        contentPadding = contentPadding,
        border = border,
        role = RemoteRole.Checkbox,
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteCheckboxButtonDefaults {
    public val Height: RemoteDp = 52.rdp

    public val IconSpacing: RemoteDp = 6.rdp

    public val IconSize: RemoteDp = 24.rdp

    public val LabelSpacerSize: RemoteDp = 1.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    public val checkboxButtonShape: RemoteRoundedCornerShape = RemoteShapeDefaults.Large

    public val BoxSize: RemoteDp = 18.rdp

    public val BoxStroke: RemoteDp = 2.rdp

    public val BoxRadius: RemoteDp = 2.rdp

    public val BoxOuterSize: RemoteDp = 24.rdp

    @Composable
    @RemoteComposable
    public fun checkboxButtonColors(): RemoteCheckboxButtonColors =
        RemoteMaterialTheme.colorScheme.defaultCheckboxButtonColors

    @Composable
    @RemoteComposable
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
        @RemoteComposable
        get() {
            return RemoteCheckboxButtonColors(
                checkedContainerColor = primaryContainer,
                checkedContentColor = onPrimaryContainer,
                checkedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                checkedIconColor = primary,
                checkedBoxColor = primary,
                checkedCheckmarkColor = primaryContainer,
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
                disabledCheckedCheckmarkColor =
                    background.toDisabledColor(disabledAlpha = 0.38f.rf),
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
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
                progress?.let { lerp(uncheckedContainerColor, checkedContainerColor, it) }
                    ?: checked.select(checkedContainerColor, uncheckedContainerColor),
            ifFalse =
                checked.select(disabledCheckedContainerColor, disabledUncheckedContainerColor),
        )

    @Stable
    internal fun contentColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { lerp(uncheckedContentColor, checkedContentColor, it) }
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
                    lerp(uncheckedSecondaryContentColor, checkedSecondaryContentColor, it)
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
                progress?.let { lerp(uncheckedIconColor, checkedIconColor, it) }
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
                progress?.let { lerp(uncheckedBoxColor, checkedBoxColor, it) }
                    ?: checked.select(checkedBoxColor, uncheckedBoxColor),
            ifFalse = checked.select(disabledCheckedBoxColor, disabledUncheckedBoxColor),
        )

    @Stable
    internal fun checkmarkColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(checkedCheckmarkColor, disabledCheckedCheckmarkColor)

    public fun copy(
        checkedContainerColor: RemoteColor = this.checkedContainerColor,
        checkedContentColor: RemoteColor = this.checkedContentColor,
        checkedSecondaryContentColor: RemoteColor = this.checkedSecondaryContentColor,
        checkedIconColor: RemoteColor = this.checkedIconColor,
        checkedBoxColor: RemoteColor = this.checkedBoxColor,
        checkedCheckmarkColor: RemoteColor = this.checkedCheckmarkColor,
        uncheckedContainerColor: RemoteColor = this.uncheckedContainerColor,
        uncheckedContentColor: RemoteColor = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: RemoteColor = this.uncheckedSecondaryContentColor,
        uncheckedIconColor: RemoteColor = this.uncheckedIconColor,
        uncheckedBoxColor: RemoteColor = this.uncheckedBoxColor,
        disabledCheckedContainerColor: RemoteColor = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: RemoteColor = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: RemoteColor =
            this.disabledCheckedSecondaryContentColor,
        disabledCheckedIconColor: RemoteColor = this.disabledCheckedIconColor,
        disabledCheckedBoxColor: RemoteColor = this.disabledCheckedBoxColor,
        disabledCheckedCheckmarkColor: RemoteColor = this.disabledCheckedCheckmarkColor,
        disabledUncheckedContainerColor: RemoteColor = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: RemoteColor = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: RemoteColor =
            this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedIconColor: RemoteColor = this.disabledUncheckedIconColor,
        disabledUncheckedBoxColor: RemoteColor = this.disabledUncheckedBoxColor,
    ): RemoteCheckboxButtonColors =
        RemoteCheckboxButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            checkedBoxColor = checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            uncheckedBoxColor = uncheckedBoxColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor,
            disabledCheckedCheckmarkColor = disabledCheckedCheckmarkColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor,
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
