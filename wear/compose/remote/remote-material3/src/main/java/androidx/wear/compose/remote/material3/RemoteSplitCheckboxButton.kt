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
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
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
 * The Wear Material SplitCheckboxButton offers two slots and shows the current toggle state via a
 * checkbox.
 *
 * The [RemoteSplitCheckboxButton] is essentially a [RemoteRow] with two split areas. The first area
 * containing [label] and optional [secondaryLabel] is clickable and triggers [onContainerClick].
 * The second area contains a checkbox toggle control and triggers [onCheckedChange].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSplitCheckboxButtonSample
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
 * @param colors [RemoteSplitCheckboxButtonColors] that will be used to resolve the container and
 *   content colors for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param secondaryLabel A slot for a secondary label, displayed below the [label].
 * @param label A slot for the main label content.
 */
@RemoteComposable
@Composable
public fun RemoteSplitCheckboxButton(
    checked: RemoteBoolean,
    onCheckedChange: Action,
    toggleContentDescription: RemoteString?,
    onContainerClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteSplitCheckboxButtonDefaults.shape,
    colors: RemoteSplitCheckboxButtonColors =
        RemoteSplitCheckboxButtonDefaults.splitCheckboxButtonColors(),
    contentPadding: RemotePaddingValues = RemoteSplitCheckboxButtonDefaults.ContentPadding,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = checked.select(1f.rf, 0f.rf)
    val containerShape = RemoteSplitCheckboxButtonDefaults.splitSectionsShape
    val containerColor =
        colors.containerColor(enabled = enabled, checked = checked, progress = progress)
    val contentColor =
        colors.contentColor(enabled = enabled, checked = checked, progress = progress)
    val secondaryContentColor =
        colors.secondaryContentColor(enabled = enabled, checked = checked, progress = progress)
    val splitContainerColor =
        colors.splitContainerColor(enabled = enabled, checked = checked, progress = progress)
    val boxColor = colors.boxColor(enabled = enabled, checked = checked, progress = progress)
    val checkmarkColor = colors.checkmarkColor(enabled = enabled)

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        modifier = modifier.heightIn(min = RemoteSplitCheckboxButtonDefaults.Height).clip(shape),
    ) {
        // Container clickable slot (label + optional secondary label)
        RemoteRow(
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Start,
            modifier =
                RemoteModifier.weight(1f.rf)
                    .heightIn(min = RemoteSplitCheckboxButtonDefaults.Height)
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
                                    content = secondaryLabel,
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
                    .heightIn(min = RemoteSplitCheckboxButtonDefaults.Height)
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
                        role = Role.Checkbox
                        if (toggleContentDescription != null) {
                            contentDescription = toggleContentDescription
                        }
                    },
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(24.rdp)) {
                drawCheckboxControl(checked, boxColor, checkmarkColor, progress)
            }
        }
    }
}

/** Contains the default values used by [RemoteSplitCheckboxButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSplitCheckboxButtonDefaults {
    /**
     * Recommended [RemoteRoundedCornerShape] for the outer container of
     * [RemoteSplitCheckboxButton].
     */
    public val shape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(26.rdp)

    /** Recommended [RemoteRoundedCornerShape] for the split sections. */
    public val splitSectionsShape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(4.rdp)

    /** The default minimum height applied for the [RemoteSplitCheckboxButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default content padding used by [RemoteSplitCheckboxButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    /**
     * Creates a [RemoteSplitCheckboxButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitCheckboxButton].
     */
    @Composable
    public fun splitCheckboxButtonColors(): RemoteSplitCheckboxButtonColors =
        RemoteMaterialTheme.colorScheme.defaultSplitCheckboxButtonColors

    /**
     * Creates a [RemoteSplitCheckboxButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitCheckboxButton].
     */
    @Composable
    public fun splitCheckboxButtonColors(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedSplitContainerColor: RemoteColor? = null,
        checkedBoxColor: RemoteColor? = null,
        checkedCheckmarkColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedSplitContainerColor: RemoteColor? = null,
        uncheckedBoxColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedSplitContainerColor: RemoteColor? = null,
        disabledCheckedBoxColor: RemoteColor? = null,
        disabledCheckedCheckmarkColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedSplitContainerColor: RemoteColor? = null,
        disabledUncheckedBoxColor: RemoteColor? = null,
    ): RemoteSplitCheckboxButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultSplitCheckboxButtonColors
        return default.copy(
            checkedContainerColor = checkedContainerColor ?: default.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: default.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: default.checkedSecondaryContentColor,
            checkedSplitContainerColor =
                checkedSplitContainerColor ?: default.checkedSplitContainerColor,
            checkedBoxColor = checkedBoxColor ?: default.checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor ?: default.checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor ?: default.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: default.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: default.uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor ?: default.uncheckedSplitContainerColor,
            uncheckedBoxColor = uncheckedBoxColor ?: default.uncheckedBoxColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: default.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: default.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor
                    ?: default.disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor =
                disabledCheckedSplitContainerColor ?: default.disabledCheckedSplitContainerColor,
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
            disabledUncheckedSplitContainerColor =
                disabledUncheckedSplitContainerColor
                    ?: default.disabledUncheckedSplitContainerColor,
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor ?: default.disabledUncheckedBoxColor,
        )
    }

    private val RemoteColorScheme.defaultSplitCheckboxButtonColors: RemoteSplitCheckboxButtonColors
        @Composable
        get() {
            return RemoteSplitCheckboxButtonColors(
                checkedContainerColor = primaryContainer,
                checkedContentColor = onPrimaryContainer,
                checkedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                checkedSplitContainerColor = onPrimaryContainer.copy(alpha = 0.12f.rf),
                checkedBoxColor = primary,
                checkedCheckmarkColor = primaryContainer,
                uncheckedContainerColor = surfaceContainer,
                uncheckedContentColor = onSurface,
                uncheckedSecondaryContentColor = onSurfaceVariant,
                uncheckedSplitContainerColor = surfaceContainerHigh,
                uncheckedBoxColor = outline,
                disabledCheckedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledCheckedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledCheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledCheckedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledCheckedBoxColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedCheckmarkColor =
                    background.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUncheckedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledUncheckedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUncheckedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUncheckedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledUncheckedBoxColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

/**
 * Represents the container, content, and toggle control colors used in [RemoteSplitCheckboxButton]
 * in different states.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSplitCheckboxButtonColors(
    public val checkedContainerColor: RemoteColor,
    public val checkedContentColor: RemoteColor,
    public val checkedSecondaryContentColor: RemoteColor,
    public val checkedSplitContainerColor: RemoteColor,
    public val checkedBoxColor: RemoteColor,
    public val checkedCheckmarkColor: RemoteColor,
    public val uncheckedContainerColor: RemoteColor,
    public val uncheckedContentColor: RemoteColor,
    public val uncheckedSecondaryContentColor: RemoteColor,
    public val uncheckedSplitContainerColor: RemoteColor,
    public val uncheckedBoxColor: RemoteColor,
    public val disabledCheckedContainerColor: RemoteColor,
    public val disabledCheckedContentColor: RemoteColor,
    public val disabledCheckedSecondaryContentColor: RemoteColor,
    public val disabledCheckedSplitContainerColor: RemoteColor,
    public val disabledCheckedBoxColor: RemoteColor,
    public val disabledCheckedCheckmarkColor: RemoteColor,
    public val disabledUncheckedContainerColor: RemoteColor,
    public val disabledUncheckedContentColor: RemoteColor,
    public val disabledUncheckedSecondaryContentColor: RemoteColor,
    public val disabledUncheckedSplitContainerColor: RemoteColor,
    public val disabledUncheckedBoxColor: RemoteColor,
) {
    public fun copy(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        checkedSecondaryContentColor: RemoteColor? = null,
        checkedSplitContainerColor: RemoteColor? = null,
        checkedBoxColor: RemoteColor? = null,
        checkedCheckmarkColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        uncheckedSecondaryContentColor: RemoteColor? = null,
        uncheckedSplitContainerColor: RemoteColor? = null,
        uncheckedBoxColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledCheckedSecondaryContentColor: RemoteColor? = null,
        disabledCheckedSplitContainerColor: RemoteColor? = null,
        disabledCheckedBoxColor: RemoteColor? = null,
        disabledCheckedCheckmarkColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
        disabledUncheckedSecondaryContentColor: RemoteColor? = null,
        disabledUncheckedSplitContainerColor: RemoteColor? = null,
        disabledUncheckedBoxColor: RemoteColor? = null,
    ): RemoteSplitCheckboxButtonColors =
        RemoteSplitCheckboxButtonColors(
            checkedContainerColor = checkedContainerColor ?: this.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: this.checkedContentColor,
            checkedSecondaryContentColor =
                checkedSecondaryContentColor ?: this.checkedSecondaryContentColor,
            checkedSplitContainerColor =
                checkedSplitContainerColor ?: this.checkedSplitContainerColor,
            checkedBoxColor = checkedBoxColor ?: this.checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor ?: this.checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor ?: this.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: this.uncheckedContentColor,
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor ?: this.uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor ?: this.uncheckedSplitContainerColor,
            uncheckedBoxColor = uncheckedBoxColor ?: this.uncheckedBoxColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: this.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: this.disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor ?: this.disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor =
                disabledCheckedSplitContainerColor ?: this.disabledCheckedSplitContainerColor,
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
            disabledUncheckedSplitContainerColor =
                disabledUncheckedSplitContainerColor ?: this.disabledUncheckedSplitContainerColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor ?: this.disabledUncheckedBoxColor,
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
            ifFalse =
                checked.select(disabledCheckedContainerColor, disabledUncheckedContainerColor),
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
    internal fun boxColor(
        enabled: RemoteBoolean = true.rb,
        checked: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(uncheckedBoxColor, checkedBoxColor, it) }
                    ?: checked.select(checkedBoxColor, uncheckedBoxColor),
            ifFalse = checked.select(disabledCheckedBoxColor, disabledUncheckedBoxColor),
        )

    @Stable
    internal fun checkmarkColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(
            ifTrue = checkedCheckmarkColor,
            ifFalse = disabledCheckedCheckmarkColor,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteSplitCheckboxButtonColors) return false

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledCheckedCheckmarkColor != other.disabledCheckedCheckmarkColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        return result
    }
}

private fun RemoteDrawScope.drawCheckboxControl(
    checked: RemoteBoolean,
    boxColor: RemoteColor,
    checkmarkColor: RemoteColor,
    progress: RemoteFloat = checked.select(1f.rf, 0f.rf),
) {
    val boxSize = 18.rdp.toPx()
    val strokeWidth = 2.rdp.toPx()
    val cornerRadius = 2.rdp.toPx()
    val topLeft = (24.rdp.toPx() - boxSize) / 2f.rf
    val halfStroke = strokeWidth / 2f.rf

    val strokePaint = RemotePaint {
        style = PaintingStyle.Stroke
        color = tween(boxColor, Color.Transparent.rc, progress)
        this.strokeWidth = strokeWidth
    }
    drawRoundRect(
        paint = strokePaint,
        topLeft = RemoteOffset(topLeft + halfStroke, topLeft + halfStroke),
        size = RemoteSize(boxSize - strokeWidth, boxSize - strokeWidth),
        cornerRadius = RemoteOffset(cornerRadius - halfStroke, cornerRadius - halfStroke),
    )

    val fillPaint = RemotePaint {
        style = PaintingStyle.Fill
        color = tween(Color.Transparent.rc, boxColor, progress)
    }
    drawRoundRect(
        paint = fillPaint,
        topLeft = RemoteOffset(topLeft, topLeft),
        size = RemoteSize(boxSize, boxSize),
        cornerRadius = RemoteOffset(cornerRadius, cornerRadius),
    )

    val tickPaint = RemotePaint {
        style = PaintingStyle.Stroke
        color = tween(Color.Transparent.rc, checkmarkColor, progress)
        this.strokeWidth = strokeWidth
        strokeCap = StrokeCap.Round
    }
    drawLine(
        paint = tickPaint,
        start = RemoteOffset(7.4f.rdp.toPx(), 13.0f.rdp.toPx()),
        end = RemoteOffset(9.9f.rdp.toPx(), 15.5f.rdp.toPx()),
    )
    drawLine(
        paint = tickPaint,
        start = RemoteOffset(10.5f.rdp.toPx(), 15.1f.rdp.toPx()),
        end = RemoteOffset(16.5f.rdp.toPx(), 9.1f.rdp.toPx()),
    )
}

/** Draws a solid color fill for [shape] using [color]. */
internal fun RemoteDrawScope.drawSolidColorShape(shape: RemoteShape, color: RemoteColor) =
    drawOutline(
        shape.createOutline(RemoteSize(width, height), remoteDensity, layoutDirection),
        RemotePaint {
            style = PaintingStyle.Fill
            this.color = color
        },
    )
