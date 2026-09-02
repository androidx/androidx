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
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.TextConfiguration

/**
 * The Wear Material SplitSwitchButton offers two slots and shows the current toggle state via a
 * switch.
 *
 * The [RemoteSplitSwitchButton] is essentially a [RemoteRow] with two split areas. The first area
 * containing [label] and optional [secondaryLabel] is clickable and triggers [onContainerClick].
 * The second area contains a switch toggle control and triggers [onCheckedChange].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSplitSwitchButtonSample
 * @param checked [RemoteBoolean] flag indicating whether this button is currently checked.
 * @param onCheckedChange Action to be invoked when the toggle control is clicked.
 * @param toggleContentDescription Text used by accessibility services to describe what this toggle
 *   control represents.
 * @param onContainerClick Action to be invoked when the container area is clicked.
 * @param modifier [RemoteModifier] to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will appear
 *   visually disabled and will not be clickable. Note that only constant values are currently
 *   supported for [enabled] for click handling.
 * @param shape Defines the button's shape. It is recommended to use the default shape.
 * @param colors [RemoteSplitSwitchButtonColors] that will be used to resolve the container and
 *   content colors for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param secondaryLabel A slot for a secondary label, displayed below the [label].
 * @param label A slot for the main label content.
 */
@RemoteComposable
@Composable
public fun RemoteSplitSwitchButton(
    checked: RemoteBoolean,
    onCheckedChange: Action,
    toggleContentDescription: RemoteString?,
    onContainerClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteSplitSwitchButtonDefaults.shape,
    colors: RemoteSplitSwitchButtonColors =
        RemoteSplitSwitchButtonDefaults.splitSwitchButtonColors(),
    contentPadding: RemotePaddingValues = RemoteSplitSwitchButtonDefaults.ContentPadding,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = checked.select(1f.rf, 0f.rf)
    val containerShape = RemoteSplitSwitchButtonDefaults.splitSectionsShape
    val containerColor =
        colors.containerColor(enabled = enabled, checked = checked, progress = progress)
    val contentColor =
        colors.contentColor(enabled = enabled, checked = checked, progress = progress)
    val secondaryContentColor =
        colors.secondaryContentColor(enabled = enabled, checked = checked, progress = progress)
    val splitContainerColor =
        colors.splitContainerColor(enabled = enabled, checked = checked, progress = progress)
    val thumbColor = colors.thumbColor(enabled = enabled, checked = checked, progress = progress)
    val thumbIconColor =
        colors.thumbIconColor(enabled = enabled, checked = checked, progress = progress)
    val trackColor = colors.trackColor(enabled = enabled, checked = checked, progress = progress)
    val trackBorderColor =
        colors.trackBorderColor(enabled = enabled, checked = checked, progress = progress)

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        modifier = modifier.heightIn(min = RemoteSplitSwitchButtonDefaults.Height).clip(shape),
    ) {
        // Container clickable slot (label + optional secondary label)
        RemoteRow(
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Start,
            modifier =
                RemoteModifier.weight(1f.rf)
                    .heightIn(min = RemoteSplitSwitchButtonDefaults.Height)
                    .clip(containerShape)
                    .clickable(
                        action = onContainerClick,
                        enabled =
                            (enabled.constantValueOrNull ?: false) &&
                                onContainerClick != Action.Empty,
                    )
                    .drawWithContent {
                        drawSolidColorShape(containerShape, containerColor)
                        drawContent()
                    }
                    .padding(contentPadding)
                    .semantics(mergeDescendants = true) { role = Role.Button },
            content = {
                RemoteColumn {
                    RemoteRow(
                        content =
                            provideScopeContent(
                                contentColor = contentColor,
                                textStyle = RemoteMaterialTheme.typography.labelMedium,
                                textConfiguration =
                                    TextConfiguration(
                                        textAlign = TextAlign.Start,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                    ),
                                content = label,
                            )
                    )
                    if (secondaryLabel != null) {
                        RemoteBox(RemoteModifier.size(1.rdp))
                        RemoteRow(
                            content =
                                provideScopeContent(
                                    contentColor = secondaryContentColor,
                                    textStyle = RemoteMaterialTheme.typography.labelSmall,
                                    textConfiguration =
                                        TextConfiguration(
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            textAlign = TextAlign.Start,
                                        ),
                                    secondaryLabel,
                                )
                        )
                    }
                }
            },
        )

        RemoteBox(RemoteModifier.size(2.rdp))

        // Selection control clickable slot
        RemoteBox(
            contentAlignment = RemoteAlignment.Center,
            modifier =
                RemoteModifier.widthIn(min = 48.rdp)
                    .heightIn(min = RemoteSplitSwitchButtonDefaults.Height)
                    .clip(containerShape)
                    .clickable(
                        action = onCheckedChange,
                        enabled =
                            (enabled.constantValueOrNull ?: false) &&
                                onCheckedChange != Action.Empty,
                    )
                    .drawWithContent {
                        drawSolidColorShape(
                            containerShape,
                            enabled.select(containerColor, Color.Black.rc),
                        )
                        drawSolidColorShape(containerShape, splitContainerColor)
                        drawContent()
                    }
                    .padding(contentPadding)
                    .semantics(mergeDescendants = true) {
                        role = Role.Switch
                        if (toggleContentDescription != null) {
                            contentDescription = toggleContentDescription
                        }
                    },
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(width = 32.rdp, height = 24.rdp)) {
                drawSwitchControl(
                    checked = checked,
                    thumbColor = thumbColor,
                    thumbIconColor = thumbIconColor,
                    trackColor = trackColor,
                    trackBorderColor = trackBorderColor,
                    progress = progress,
                )
            }
        }
    }
}

/** Contains the default values used by [RemoteSplitSwitchButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSplitSwitchButtonDefaults {
    /**
     * Recommended [RemoteRoundedCornerShape] for the outer container of [RemoteSplitSwitchButton].
     */
    public val shape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(26.rdp)

    /** Recommended [RemoteRoundedCornerShape] for the split sections. */
    public val splitSectionsShape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(4.rdp)

    /** The default minimum height applied for the [RemoteSplitSwitchButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default content padding used by [RemoteSplitSwitchButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    /**
     * Creates a [RemoteSplitSwitchButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitSwitchButton].
     */
    @Composable
    public fun splitSwitchButtonColors(): RemoteSplitSwitchButtonColors =
        RemoteMaterialTheme.colorScheme.defaultSplitSwitchButtonColors

    /**
     * Creates a [RemoteSplitSwitchButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitSwitchButton].
     */
    @Composable
    public fun splitSwitchButtonColors(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedSplitContainerColor: RemoteColor? = null,
        checkedThumbColor: RemoteColor? = null,
        checkedThumbIconColor: RemoteColor? = null,
        checkedTrackColor: RemoteColor? = null,
        checkedTrackBorderColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedSplitContainerColor: RemoteColor? = null,
        uncheckedThumbColor: RemoteColor? = null,
        uncheckedTrackColor: RemoteColor? = null,
        uncheckedTrackBorderColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedSplitContainerColor: RemoteColor? = null,
        disabledCheckedThumbColor: RemoteColor? = null,
        disabledCheckedThumbIconColor: RemoteColor? = null,
        disabledCheckedTrackColor: RemoteColor? = null,
        disabledCheckedTrackBorderColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedSplitContainerColor: RemoteColor? = null,
        disabledUncheckedThumbColor: RemoteColor? = null,
        disabledUncheckedTrackBorderColor: RemoteColor? = null,
    ): RemoteSplitSwitchButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultSplitSwitchButtonColors
        return default.copy(
            checkedContainerColor = checkedContainerColor ?: default.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: default.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: default.checkedSecondaryContentColor,
            checkedSplitContainerColor =
                checkedSplitContainerColor ?: default.checkedSplitContainerColor,
            checkedThumbColor = checkedThumbColor ?: default.checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor ?: default.checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor ?: default.checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor ?: default.checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor ?: default.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: default.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: default.uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor ?: default.uncheckedSplitContainerColor,
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
            disabledCheckedSplitContainerColor =
                disabledCheckedSplitContainerColor ?: default.disabledCheckedSplitContainerColor,
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
            disabledUncheckedSplitContainerColor =
                disabledUncheckedSplitContainerColor
                    ?: default.disabledUncheckedSplitContainerColor,
            disabledUncheckedThumbColor =
                disabledUncheckedThumbColor ?: default.disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor =
                disabledUncheckedTrackBorderColor ?: default.disabledUncheckedTrackBorderColor,
        )
    }

    private val RemoteColorScheme.defaultSplitSwitchButtonColors: RemoteSplitSwitchButtonColors
        @Composable
        get() {
            return RemoteSplitSwitchButtonColors(
                checkedContainerColor = primaryContainer,
                checkedContentColor = onPrimaryContainer,
                checkedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                checkedSplitContainerColor = onPrimaryContainer.copy(alpha = 0.12f.rf),
                checkedThumbColor = primaryContainer,
                checkedThumbIconColor = primary,
                checkedTrackColor = primary,
                checkedTrackBorderColor = primary,
                uncheckedContainerColor = surfaceContainer,
                uncheckedContentColor = onSurface,
                uncheckedSecondaryContentColor = onSurfaceVariant,
                uncheckedSplitContainerColor = surfaceContainerHigh,
                uncheckedThumbColor = outline,
                uncheckedTrackColor = surfaceContainerHigh,
                uncheckedTrackBorderColor = outline,
                disabledCheckedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledCheckedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledCheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledCheckedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledCheckedThumbColor =
                    background.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledCheckedThumbIconColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedTrackColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedTrackBorderColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledUncheckedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUncheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUncheckedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledUncheckedThumbColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedTrackBorderColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

/**
 * Represents the container, content, and switch control colors used in [RemoteSplitSwitchButton] in
 * different states.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSplitSwitchButtonColors(
    public val checkedContainerColor: RemoteColor,
    public val checkedContentColor: RemoteColor,
    public val checkedSecondaryContentColor: RemoteColor,
    public val checkedSplitContainerColor: RemoteColor,
    public val checkedThumbColor: RemoteColor,
    public val checkedThumbIconColor: RemoteColor,
    public val checkedTrackColor: RemoteColor,
    public val checkedTrackBorderColor: RemoteColor,
    public val uncheckedContainerColor: RemoteColor,
    public val uncheckedContentColor: RemoteColor,
    public val uncheckedSecondaryContentColor: RemoteColor,
    public val uncheckedSplitContainerColor: RemoteColor,
    public val uncheckedThumbColor: RemoteColor,
    public val uncheckedTrackColor: RemoteColor,
    public val uncheckedTrackBorderColor: RemoteColor,
    public val disabledCheckedContainerColor: RemoteColor,
    public val disabledCheckedContentColor: RemoteColor,
    public val disabledCheckedSecondaryContentColor: RemoteColor,
    public val disabledCheckedSplitContainerColor: RemoteColor,
    public val disabledCheckedThumbColor: RemoteColor,
    public val disabledCheckedThumbIconColor: RemoteColor,
    public val disabledCheckedTrackColor: RemoteColor,
    public val disabledCheckedTrackBorderColor: RemoteColor,
    public val disabledUncheckedContainerColor: RemoteColor,
    public val disabledUncheckedContentColor: RemoteColor,
    public val disabledUncheckedSecondaryContentColor: RemoteColor,
    public val disabledUncheckedSplitContainerColor: RemoteColor,
    public val disabledUncheckedThumbColor: RemoteColor,
    public val disabledUncheckedTrackBorderColor: RemoteColor,
) {
    public fun copy(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedSplitContainerColor: RemoteColor? = null,
        checkedThumbColor: RemoteColor? = null,
        checkedThumbIconColor: RemoteColor? = null,
        checkedTrackColor: RemoteColor? = null,
        checkedTrackBorderColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedSplitContainerColor: RemoteColor? = null,
        uncheckedThumbColor: RemoteColor? = null,
        uncheckedTrackColor: RemoteColor? = null,
        uncheckedTrackBorderColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedSplitContainerColor: RemoteColor? = null,
        disabledCheckedThumbColor: RemoteColor? = null,
        disabledCheckedThumbIconColor: RemoteColor? = null,
        disabledCheckedTrackColor: RemoteColor? = null,
        disabledCheckedTrackBorderColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedSplitContainerColor: RemoteColor? = null,
        disabledUncheckedThumbColor: RemoteColor? = null,
        disabledUncheckedTrackBorderColor: RemoteColor? = null,
    ): RemoteSplitSwitchButtonColors =
        RemoteSplitSwitchButtonColors(
            checkedContainerColor = checkedContainerColor ?: this.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: this.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: this.checkedSecondaryContentColor,
            checkedSplitContainerColor =
                checkedSplitContainerColor ?: this.checkedSplitContainerColor,
            checkedThumbColor = checkedThumbColor ?: this.checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor ?: this.checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor ?: this.checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor ?: this.checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor ?: this.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: this.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: this.uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor ?: this.uncheckedSplitContainerColor,
            uncheckedThumbColor = uncheckedThumbColor ?: this.uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor ?: this.uncheckedTrackColor,
            uncheckedTrackBorderColor = uncheckedTrackBorderColor ?: this.uncheckedTrackBorderColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: this.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: this.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor ?: this.disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor =
                disabledCheckedSplitContainerColor ?: this.disabledCheckedSplitContainerColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor ?: this.disabledCheckedThumbColor,
            disabledCheckedThumbIconColor =
                disabledCheckedThumbIconColor ?: this.disabledCheckedThumbIconColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor ?: this.disabledCheckedTrackColor,
            disabledCheckedTrackBorderColor =
                disabledCheckedTrackBorderColor ?: this.disabledCheckedTrackBorderColor,
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor ?: this.disabledUncheckedContainerColor,
            disabledUncheckedContentColor =
                disabledUncheckedContentColor ?: this.disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor
                    ?: this.disabledUncheckedSecondaryContentColor,
            disabledUncheckedSplitContainerColor =
                disabledUncheckedSplitContainerColor ?: this.disabledUncheckedSplitContainerColor,
            disabledUncheckedThumbColor =
                disabledUncheckedThumbColor ?: this.disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor =
                disabledUncheckedTrackBorderColor ?: this.disabledUncheckedTrackBorderColor,
        )

    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
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
    internal fun splitContainerColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let {
                    tween(uncheckedSplitContainerColor, checkedSplitContainerColor, it)
                } ?: checked.select(checkedSplitContainerColor, uncheckedSplitContainerColor),
            ifFalse =
                checked.select(
                    disabledCheckedSplitContainerColor,
                    disabledUncheckedSplitContainerColor,
                ),
        )

    @Stable
    internal fun thumbColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedThumbColor, checkedThumbColor, it) }
                    ?: checked.select(checkedThumbColor, uncheckedThumbColor),
            ifFalse = checked.select(disabledCheckedThumbColor, disabledUncheckedThumbColor),
        )

    @Stable
    internal fun thumbIconColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(Color.Transparent.rc, checkedThumbIconColor, it) }
                    ?: checked.select(checkedThumbIconColor, Color.Transparent.rc),
            ifFalse =
                progress?.let { tween(Color.Transparent.rc, disabledCheckedThumbIconColor, it) }
                    ?: checked.select(disabledCheckedThumbIconColor, Color.Transparent.rc),
        )

    @Stable
    internal fun trackColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedTrackColor, checkedTrackColor, it) }
                    ?: checked.select(checkedTrackColor, uncheckedTrackColor),
            ifFalse =
                progress?.let { tween(Color.Transparent.rc, disabledCheckedTrackColor, it) }
                    ?: checked.select(disabledCheckedTrackColor, Color.Transparent.rc),
        )

    @Stable
    internal fun trackBorderColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedTrackBorderColor, checkedTrackBorderColor, it) }
                    ?: checked.select(checkedTrackBorderColor, uncheckedTrackBorderColor),
            ifFalse =
                checked.select(disabledCheckedTrackBorderColor, disabledUncheckedTrackBorderColor),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteSplitSwitchButtonColors) return false

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedThumbIconColor != other.checkedThumbIconColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedTrackBorderColor != other.checkedTrackBorderColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedTrackBorderColor != other.uncheckedTrackBorderColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedThumbIconColor != other.disabledCheckedThumbIconColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackBorderColor != other.disabledUncheckedTrackBorderColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + checkedThumbColor.hashCode()
        result = 31 * result + checkedThumbIconColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedTrackBorderColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedThumbIconColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackBorderColor.hashCode()
        return result
    }
}

private fun RemoteDrawScope.drawSwitchControl(
    checked: RemoteBoolean,
    thumbColor: RemoteColor,
    thumbIconColor: RemoteColor,
    trackColor: RemoteColor,
    trackBorderColor: RemoteColor,
    progress: RemoteFloat = checked.select(1f.rf, 0f.rf),
) {
    val trackWidth = 32.rdp.toPx()
    val trackHeight = 22.rdp.toPx()
    val trackTop = 1.rdp.toPx()
    val cornerRadius = 11.rdp.toPx()
    val strokeWidth = 2.rdp.toPx()

    // Draw track fill
    drawRoundRect(
        paint =
            RemotePaint {
                style = PaintingStyle.Fill
                color = trackColor
            },
        topLeft = RemoteOffset(0f.rf, trackTop),
        size = RemoteSize(trackWidth, trackHeight),
        cornerRadius = RemoteOffset(cornerRadius, cornerRadius),
    )

    // Draw track border
    val halfStroke = strokeWidth / 2f.rf
    drawRoundRect(
        paint =
            RemotePaint {
                style = PaintingStyle.Stroke
                color = trackBorderColor
                this.strokeWidth = strokeWidth
            },
        topLeft = RemoteOffset(halfStroke, trackTop + halfStroke),
        size = RemoteSize(trackWidth - strokeWidth, trackHeight - strokeWidth),
        cornerRadius = RemoteOffset(cornerRadius - halfStroke, cornerRadius - halfStroke),
    )

    // Draw thumb
    val thumbRadiusPx = lerp(6.rdp.toPx(), 9.rdp.toPx(), progress)
    val thumbCenterXPx = lerp(11.rdp.toPx(), 21.rdp.toPx(), progress)
    val thumbCenterYPx = 12.rdp.toPx()
    drawCircle(
        paint =
            RemotePaint {
                style = PaintingStyle.Fill
                color = thumbColor
            },
        radius = thumbRadiusPx,
        center = RemoteOffset(thumbCenterXPx, thumbCenterYPx),
    )

    // Draw tick icon on thumb
    val tickPaint = RemotePaint {
        style = PaintingStyle.Stroke
        color = thumbIconColor
        this.strokeWidth = strokeWidth
        strokeCap = StrokeCap.Round
    }
    drawLine(
        paint = tickPaint,
        start = RemoteOffset(thumbCenterXPx - 4.6f.rdp.toPx(), thumbCenterYPx + 1.0f.rdp.toPx()),
        end = RemoteOffset(thumbCenterXPx - 2.1f.rdp.toPx(), thumbCenterYPx + 3.5f.rdp.toPx()),
    )
    drawLine(
        paint = tickPaint,
        start = RemoteOffset(thumbCenterXPx - 1.5f.rdp.toPx(), thumbCenterYPx + 3.1f.rdp.toPx()),
        end = RemoteOffset(thumbCenterXPx + 4.5f.rdp.toPx(), thumbCenterYPx - 2.9f.rdp.toPx()),
    )
}
