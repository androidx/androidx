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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/** @sample androidx.wear.compose.remote.material3.samples.RemoteSwitchButtonSample */
@Composable
@RemoteComposable
public fun RemoteSwitchButton(
    checked: RemoteBoolean,
    onCheckedChange: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteSwitchButtonDefaults.switchButtonShape,
    colors: RemoteSwitchButtonColors = RemoteSwitchButtonDefaults.switchButtonColors(),
    contentPadding: RemotePaddingValues = RemoteSwitchButtonDefaults.ContentPadding,
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
        role = RemoteRole.Switch,
        icon = icon,
        secondaryLabel = secondaryLabel,
        label = label,
        selectionControl = {
            RemoteSwitchControl(
                checked = checked,
                trackColor =
                    colors.trackColor(enabled = enabled, checked = checked, progress = progress),
                trackBorderColor =
                    colors.trackBorderColor(
                        enabled = enabled,
                        checked = checked,
                        progress = progress,
                    ),
                thumbColor =
                    colors.thumbColor(enabled = enabled, checked = checked, progress = progress),
                thumbIconColor =
                    colors.thumbIconColor(
                        enabled = enabled,
                        checked = checked,
                        progress = progress,
                    ),
                progress = progress,
            )
        },
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSwitchButtonDefaults {
    public val Height: RemoteDp = 52.rdp

    public val IconSpacing: RemoteDp = 6.rdp

    public val IconSize: RemoteDp = 24.rdp

    public val LabelSpacerSize: RemoteDp = 1.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    public val switchButtonShape: RemoteRoundedCornerShape = RemoteShapeDefaults.Large

    public val SwitchWidth: RemoteDp = 32.rdp

    public val SwitchHeight: RemoteDp = 24.rdp

    public val SwitchInnerHeight: RemoteDp = 22.rdp

    public val SwitchTrackWidth: RemoteDp = 2.rdp

    public val ThumbRadiusUnchecked: RemoteDp = 6.rdp

    public val ThumbRadiusChecked: RemoteDp = 9.rdp

    @Composable
    @RemoteComposable
    public fun switchButtonColors(): RemoteSwitchButtonColors =
        RemoteMaterialTheme.colorScheme.defaultSwitchButtonColors

    @Composable
    @RemoteComposable
    public fun switchButtonColors(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedIconColor: RemoteColor? = null,
        checkedThumbColor: RemoteColor? = null,
        checkedThumbIconColor: RemoteColor? = null,
        checkedTrackColor: RemoteColor? = null,
        checkedTrackBorderColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedIconColor: RemoteColor? = null,
        uncheckedThumbColor: RemoteColor? = null,
        uncheckedTrackColor: RemoteColor? = null,
        uncheckedTrackBorderColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedIconColor: RemoteColor? = null,
        disabledCheckedThumbColor: RemoteColor? = null,
        disabledCheckedThumbIconColor: RemoteColor? = null,
        disabledCheckedTrackColor: RemoteColor? = null,
        disabledCheckedTrackBorderColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedIconColor: RemoteColor? = null,
        disabledUncheckedThumbColor: RemoteColor? = null,
        disabledUncheckedTrackBorderColor: RemoteColor? = null,
    ): RemoteSwitchButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultSwitchButtonColors
        return default.copy(
            checkedContainerColor = checkedContainerColor ?: default.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: default.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: default.checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor ?: default.checkedIconColor,
            checkedThumbColor = checkedThumbColor ?: default.checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor ?: default.checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor ?: default.checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor ?: default.checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor ?: default.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: default.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: default.uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor ?: default.uncheckedIconColor,
            uncheckedThumbColor = uncheckedThumbColor ?: default.uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor ?: default.uncheckedTrackColor,
            uncheckedTrackBorderColor =
                uncheckedTrackBorderColor ?: default.uncheckedTrackBorderColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: default.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: default.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor
                    ?: default.disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor ?: default.disabledCheckedIconColor,
            disabledCheckedThumbColor =
                disabledCheckedThumbColor ?: default.disabledCheckedThumbColor,
            disabledCheckedThumbIconColor =
                disabledCheckedThumbIconColor ?: default.disabledCheckedThumbIconColor,
            disabledCheckedTrackColor =
                disabledCheckedTrackColor ?: default.disabledCheckedTrackColor,
            disabledCheckedTrackBorderColor =
                disabledCheckedTrackBorderColor ?: default.disabledCheckedTrackBorderColor,
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor ?: default.disabledUncheckedContainerColor,
            disabledUncheckedContentColor =
                disabledUncheckedContentColor ?: default.disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor
                    ?: default.disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor =
                disabledUncheckedIconColor ?: default.disabledUncheckedIconColor,
            disabledUncheckedThumbColor =
                disabledUncheckedThumbColor ?: default.disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor =
                disabledUncheckedTrackBorderColor ?: default.disabledUncheckedTrackBorderColor,
        )
    }

    private val RemoteColorScheme.defaultSwitchButtonColors: RemoteSwitchButtonColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteSwitchButtonColors(
                checkedContainerColor = primaryContainer,
                checkedContentColor = onPrimaryContainer,
                checkedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                checkedIconColor = primary,
                checkedThumbColor = primaryContainer,
                checkedThumbIconColor = primary,
                checkedTrackColor = primary,
                checkedTrackBorderColor = primary,
                uncheckedContainerColor = surfaceContainer,
                uncheckedContentColor = onSurface,
                uncheckedSecondaryContentColor = onSurfaceVariant,
                uncheckedIconColor = primary,
                uncheckedThumbColor = outline,
                uncheckedTrackColor = surfaceContainer,
                uncheckedTrackBorderColor = outline,
                disabledCheckedContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledCheckedThumbColor = background.copy(alpha = 0.38f.rf),
                disabledCheckedThumbIconColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedTrackColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedTrackBorderColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedContainerColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedThumbColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedTrackBorderColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteSwitchButtonColors(
    public val checkedContainerColor: RemoteColor,
    public val checkedContentColor: RemoteColor,
    public val checkedSecondaryContentColor: RemoteColor,
    public val checkedIconColor: RemoteColor,
    public val checkedThumbColor: RemoteColor,
    public val checkedThumbIconColor: RemoteColor,
    public val checkedTrackColor: RemoteColor,
    public val checkedTrackBorderColor: RemoteColor,
    public val uncheckedContainerColor: RemoteColor,
    public val uncheckedContentColor: RemoteColor,
    public val uncheckedSecondaryContentColor: RemoteColor,
    public val uncheckedIconColor: RemoteColor,
    public val uncheckedThumbColor: RemoteColor,
    public val uncheckedTrackColor: RemoteColor,
    public val uncheckedTrackBorderColor: RemoteColor,
    public val disabledCheckedContainerColor: RemoteColor,
    public val disabledCheckedContentColor: RemoteColor,
    public val disabledCheckedSecondaryContentColor: RemoteColor,
    public val disabledCheckedIconColor: RemoteColor,
    public val disabledCheckedThumbColor: RemoteColor,
    public val disabledCheckedThumbIconColor: RemoteColor,
    public val disabledCheckedTrackColor: RemoteColor,
    public val disabledCheckedTrackBorderColor: RemoteColor,
    public val disabledUncheckedContainerColor: RemoteColor,
    public val disabledUncheckedContentColor: RemoteColor,
    public val disabledUncheckedSecondaryContentColor: RemoteColor,
    public val disabledUncheckedIconColor: RemoteColor,
    public val disabledUncheckedThumbColor: RemoteColor,
    public val disabledUncheckedTrackBorderColor: RemoteColor,
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
    internal fun thumbColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { lerp(uncheckedThumbColor, checkedThumbColor, it) }
                    ?: checked.select(checkedThumbColor, uncheckedThumbColor),
            ifFalse = checked.select(disabledCheckedThumbColor, disabledUncheckedThumbColor),
        )

    @Stable
    internal fun thumbIconColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { lerp(Color.Transparent.rc, checkedThumbIconColor, it) }
                    ?: checked.select(checkedThumbIconColor, Color.Transparent.rc),
            ifFalse =
                progress?.let { lerp(Color.Transparent.rc, disabledCheckedThumbIconColor, it) }
                    ?: checked.select(disabledCheckedThumbIconColor, Color.Transparent.rc),
        )

    @Stable
    internal fun trackColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { lerp(uncheckedTrackColor, checkedTrackColor, it) }
                    ?: checked.select(checkedTrackColor, uncheckedTrackColor),
            ifFalse =
                progress?.let { lerp(Color.Transparent.rc, disabledCheckedTrackColor, it) }
                    ?: checked.select(disabledCheckedTrackColor, Color.Transparent.rc),
        )

    @Stable
    internal fun trackBorderColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { lerp(uncheckedTrackBorderColor, checkedTrackBorderColor, it) }
                    ?: checked.select(checkedTrackBorderColor, uncheckedTrackBorderColor),
            ifFalse =
                checked.select(disabledCheckedTrackBorderColor, disabledUncheckedTrackBorderColor),
        )

    public fun copy(
        checkedContainerColor: RemoteColor = this.checkedContainerColor,
        checkedContentColor: RemoteColor = this.checkedContentColor,
        checkedSecondaryContentColor: RemoteColor = this.checkedSecondaryContentColor,
        checkedIconColor: RemoteColor = this.checkedIconColor,
        checkedThumbColor: RemoteColor = this.checkedThumbColor,
        checkedThumbIconColor: RemoteColor = this.checkedThumbIconColor,
        checkedTrackColor: RemoteColor = this.checkedTrackColor,
        checkedTrackBorderColor: RemoteColor = this.checkedTrackBorderColor,
        uncheckedContainerColor: RemoteColor = this.uncheckedContainerColor,
        uncheckedContentColor: RemoteColor = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: RemoteColor = this.uncheckedSecondaryContentColor,
        uncheckedIconColor: RemoteColor = this.uncheckedIconColor,
        uncheckedThumbColor: RemoteColor = this.uncheckedThumbColor,
        uncheckedTrackColor: RemoteColor = this.uncheckedTrackColor,
        uncheckedTrackBorderColor: RemoteColor = this.uncheckedTrackBorderColor,
        disabledCheckedContainerColor: RemoteColor = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: RemoteColor = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: RemoteColor =
            this.disabledCheckedSecondaryContentColor,
        disabledCheckedIconColor: RemoteColor = this.disabledCheckedIconColor,
        disabledCheckedThumbColor: RemoteColor = this.disabledCheckedThumbColor,
        disabledCheckedThumbIconColor: RemoteColor = this.disabledCheckedThumbIconColor,
        disabledCheckedTrackColor: RemoteColor = this.disabledCheckedTrackColor,
        disabledCheckedTrackBorderColor: RemoteColor = this.disabledCheckedTrackBorderColor,
        disabledUncheckedContainerColor: RemoteColor = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: RemoteColor = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: RemoteColor =
            this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedIconColor: RemoteColor = this.disabledUncheckedIconColor,
        disabledUncheckedThumbColor: RemoteColor = this.disabledUncheckedThumbColor,
        disabledUncheckedTrackBorderColor: RemoteColor = this.disabledUncheckedTrackBorderColor,
    ): RemoteSwitchButtonColors =
        RemoteSwitchButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            checkedThumbColor = checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedTrackBorderColor = uncheckedTrackBorderColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledCheckedThumbIconColor = disabledCheckedThumbIconColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledCheckedTrackBorderColor = disabledCheckedTrackBorderColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor = disabledUncheckedTrackBorderColor,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteSwitchButtonColors) return false

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedThumbIconColor != other.checkedThumbIconColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedTrackBorderColor != other.checkedTrackBorderColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedTrackBorderColor != other.uncheckedTrackBorderColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor) {
            return false
        }
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedThumbIconColor != other.disabledCheckedThumbIconColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (
            disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor
        ) {
            return false
        }
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackBorderColor != other.disabledUncheckedTrackBorderColor) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + checkedThumbColor.hashCode()
        result = 31 * result + checkedThumbIconColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedTrackBorderColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedThumbIconColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackBorderColor.hashCode()
        return result
    }
}
