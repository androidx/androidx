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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.TextConfiguration

/**
 * The Wear Material SplitRadioButton offers two slots and shows the current selection state via a
 * radio control.
 *
 * The [RemoteSplitRadioButton] is essentially a [RemoteRow] with two split areas. The first area
 * containing [label] and optional [secondaryLabel] is clickable and triggers [onContainerClick].
 * The second area contains a radio selection control and triggers [onSelectionClick].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSplitRadioButtonSample
 * @param selected [RemoteBoolean] flag indicating whether this button is currently selected.
 * @param onSelectionClick Action to be invoked when the selection control is clicked.
 * @param selectionContentDescription Text used by accessibility services to describe what this
 *   selection control represents.
 * @param onContainerClick Action to be invoked when the container area is clicked.
 * @param modifier [RemoteModifier] to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will appear
 *   visually disabled and will not be clickable. Note that only constant values are currently
 *   supported for [enabled] for click handling.
 * @param shape Defines the button's shape. It is recommended to use the default shape.
 * @param colors [RemoteSplitRadioButtonColors] that will be used to resolve the container and
 *   content colors for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param secondaryLabel A slot for a secondary label, displayed below the [label].
 * @param label A slot for the main label content.
 */
@RemoteComposable
@Composable
public fun RemoteSplitRadioButton(
    selected: RemoteBoolean,
    onSelectionClick: Action,
    selectionContentDescription: RemoteString?,
    onContainerClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteSplitRadioButtonDefaults.shape,
    colors: RemoteSplitRadioButtonColors = RemoteSplitRadioButtonDefaults.splitRadioButtonColors(),
    contentPadding: RemotePaddingValues = RemoteSplitRadioButtonDefaults.ContentPadding,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = selected.select(1f.rf, 0f.rf)
    val containerShape = RemoteSplitRadioButtonDefaults.splitSectionsShape
    val containerColor =
        colors.containerColor(enabled = enabled, selected = selected, progress = progress)
    val contentColor =
        colors.contentColor(enabled = enabled, selected = selected, progress = progress)
    val secondaryContentColor =
        colors.secondaryContentColor(enabled = enabled, selected = selected, progress = progress)
    val splitContainerColor =
        colors.splitContainerColor(enabled = enabled, selected = selected, progress = progress)
    val controlColor =
        colors.controlColor(enabled = enabled, selected = selected, progress = progress)

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        modifier = modifier.heightIn(min = RemoteSplitRadioButtonDefaults.Height).clip(shape),
    ) {
        // Container clickable slot (label + optional secondary label)
        RemoteRow(
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Start,
            modifier =
                RemoteModifier.weight(1f.rf)
                    .heightIn(min = RemoteSplitRadioButtonDefaults.Height)
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
                    .heightIn(min = RemoteSplitRadioButtonDefaults.Height)
                    .clip(containerShape)
                    .clickable(
                        action = onSelectionClick,
                        enabled =
                            (enabled.constantValueOrNull ?: false) &&
                                onSelectionClick != Action.Empty,
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
                        role = Role.RadioButton
                        if (selectionContentDescription != null) {
                            contentDescription = selectionContentDescription
                        }
                    },
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(24.rdp)) {
                drawRadioControl(selected, controlColor, progress)
            }
        }
    }
}

/** Contains the default values used by [RemoteSplitRadioButton]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSplitRadioButtonDefaults {
    /**
     * Recommended [RemoteRoundedCornerShape] for the outer container of [RemoteSplitRadioButton].
     */
    public val shape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(26.rdp)

    /** Recommended [RemoteRoundedCornerShape] for the split sections. */
    public val splitSectionsShape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(4.rdp)

    /** The default minimum height applied for the [RemoteSplitRadioButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default content padding used by [RemoteSplitRadioButton]. */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    /**
     * Creates a [RemoteSplitRadioButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitRadioButton].
     */
    @Composable
    public fun splitRadioButtonColors(): RemoteSplitRadioButtonColors =
        RemoteMaterialTheme.colorScheme.defaultSplitRadioButtonColors

    /**
     * Creates a [RemoteSplitRadioButtonColors] that represents the default container and content
     * colors used in a [RemoteSplitRadioButton].
     */
    @Composable
    public fun splitRadioButtonColors(
        selectedContainerColor: RemoteColor? = null,
        selectedContentColor: RemoteColor? = null,
        selectedSecondaryContentColor: RemoteColor? = null,
        selectedSplitContainerColor: RemoteColor? = null,
        selectedControlColor: RemoteColor? = null,
        unselectedContainerColor: RemoteColor? = null,
        unselectedContentColor: RemoteColor? = null,
        unselectedSecondaryContentColor: RemoteColor? = null,
        unselectedSplitContainerColor: RemoteColor? = null,
        unselectedControlColor: RemoteColor? = null,
        disabledSelectedContainerColor: RemoteColor? = null,
        disabledSelectedContentColor: RemoteColor? = null,
        disabledSelectedSecondaryContentColor: RemoteColor? = null,
        disabledSelectedSplitContainerColor: RemoteColor? = null,
        disabledSelectedControlColor: RemoteColor? = null,
        disabledUnselectedContainerColor: RemoteColor? = null,
        disabledUnselectedContentColor: RemoteColor? = null,
        disabledUnselectedSecondaryContentColor: RemoteColor? = null,
        disabledUnselectedSplitContainerColor: RemoteColor? = null,
        disabledUnselectedControlColor: RemoteColor? = null,
    ): RemoteSplitRadioButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultSplitRadioButtonColors
        return default.copy(
            selectedContainerColor = selectedContainerColor ?: default.selectedContainerColor,
            selectedContentColor = selectedContentColor ?: default.selectedContentColor,
            selectedSecondaryContentColor =
                selectedSecondaryContentColor ?: default.selectedSecondaryContentColor,
            selectedSplitContainerColor =
                selectedSplitContainerColor ?: default.selectedSplitContainerColor,
            selectedControlColor = selectedControlColor ?: default.selectedControlColor,
            unselectedContainerColor = unselectedContainerColor ?: default.unselectedContainerColor,
            unselectedContentColor = unselectedContentColor ?: default.unselectedContentColor,
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor ?: default.unselectedSecondaryContentColor,
            unselectedSplitContainerColor =
                unselectedSplitContainerColor ?: default.unselectedSplitContainerColor,
            unselectedControlColor = unselectedControlColor ?: default.unselectedControlColor,
            disabledSelectedContainerColor =
                disabledSelectedContainerColor ?: default.disabledSelectedContainerColor,
            disabledSelectedContentColor =
                disabledSelectedContentColor ?: default.disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor
                    ?: default.disabledSelectedSecondaryContentColor,
            disabledSelectedSplitContainerColor =
                disabledSelectedSplitContainerColor ?: default.disabledSelectedSplitContainerColor,
            disabledSelectedControlColor =
                disabledSelectedControlColor ?: default.disabledSelectedControlColor,
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor ?: default.disabledUnselectedContainerColor,
            disabledUnselectedContentColor =
                disabledUnselectedContentColor ?: default.disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor
                    ?: default.disabledUnselectedSecondaryContentColor,
            disabledUnselectedSplitContainerColor =
                disabledUnselectedSplitContainerColor
                    ?: default.disabledUnselectedSplitContainerColor,
            disabledUnselectedControlColor =
                disabledUnselectedControlColor ?: default.disabledUnselectedControlColor,
        )
    }

    private val RemoteColorScheme.defaultSplitRadioButtonColors: RemoteSplitRadioButtonColors
        @Composable
        get() {
            return RemoteSplitRadioButtonColors(
                selectedContainerColor = primaryContainer,
                selectedContentColor = onPrimaryContainer,
                selectedSecondaryContentColor = onPrimaryContainer.copy(alpha = 0.9f.rf),
                selectedSplitContainerColor = onPrimaryContainer.copy(alpha = 0.12f.rf),
                selectedControlColor = primary,
                unselectedContainerColor = surfaceContainer,
                unselectedContentColor = onSurface,
                unselectedSecondaryContentColor = onSurfaceVariant,
                unselectedSplitContainerColor = surfaceContainerHigh,
                unselectedControlColor = outline,
                disabledSelectedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledSelectedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledSelectedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledSelectedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledSelectedControlColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUnselectedContainerColor = onSurface.copy(alpha = 0.12f.rf),
                disabledUnselectedContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUnselectedSecondaryContentColor =
                    onSurface.toDisabledColor(disabledAlpha = DisabledContentAlpha.rf),
                disabledUnselectedSplitContainerColor = onSurface.copy(alpha = 0.16f.rf),
                disabledUnselectedControlColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

/**
 * Represents the container, content, and selection control colors used in [RemoteSplitRadioButton]
 * in different states.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSplitRadioButtonColors(
    public val selectedContainerColor: RemoteColor,
    public val selectedContentColor: RemoteColor,
    public val selectedSecondaryContentColor: RemoteColor,
    public val selectedSplitContainerColor: RemoteColor,
    public val selectedControlColor: RemoteColor,
    public val unselectedContainerColor: RemoteColor,
    public val unselectedContentColor: RemoteColor,
    public val unselectedSecondaryContentColor: RemoteColor,
    public val unselectedSplitContainerColor: RemoteColor,
    public val unselectedControlColor: RemoteColor,
    public val disabledSelectedContainerColor: RemoteColor,
    public val disabledSelectedContentColor: RemoteColor,
    public val disabledSelectedSecondaryContentColor: RemoteColor,
    public val disabledSelectedSplitContainerColor: RemoteColor,
    public val disabledSelectedControlColor: RemoteColor,
    public val disabledUnselectedContainerColor: RemoteColor,
    public val disabledUnselectedContentColor: RemoteColor,
    public val disabledUnselectedSecondaryContentColor: RemoteColor,
    public val disabledUnselectedSplitContainerColor: RemoteColor,
    public val disabledUnselectedControlColor: RemoteColor,
) {
    public fun copy(
        selectedContainerColor: RemoteColor? = null,
        selectedContentColor: RemoteColor? = null,
        selectedSecondaryContentColor: RemoteColor? = null,
        selectedSplitContainerColor: RemoteColor? = null,
        selectedControlColor: RemoteColor? = null,
        unselectedContainerColor: RemoteColor? = null,
        unselectedContentColor: RemoteColor? = null,
        unselectedSecondaryContentColor: RemoteColor? = null,
        unselectedSplitContainerColor: RemoteColor? = null,
        unselectedControlColor: RemoteColor? = null,
        disabledSelectedContainerColor: RemoteColor? = null,
        disabledSelectedContentColor: RemoteColor? = null,
        disabledSelectedSecondaryContentColor: RemoteColor? = null,
        disabledSelectedSplitContainerColor: RemoteColor? = null,
        disabledSelectedControlColor: RemoteColor? = null,
        disabledUnselectedContainerColor: RemoteColor? = null,
        disabledUnselectedContentColor: RemoteColor? = null,
        disabledUnselectedSecondaryContentColor: RemoteColor? = null,
        disabledUnselectedSplitContainerColor: RemoteColor? = null,
        disabledUnselectedControlColor: RemoteColor? = null,
    ): RemoteSplitRadioButtonColors =
        RemoteSplitRadioButtonColors(
            selectedContainerColor = selectedContainerColor ?: this.selectedContainerColor,
            selectedContentColor = selectedContentColor ?: this.selectedContentColor,
            selectedSecondaryContentColor =
                selectedSecondaryContentColor ?: this.selectedSecondaryContentColor,
            selectedSplitContainerColor =
                selectedSplitContainerColor ?: this.selectedSplitContainerColor,
            selectedControlColor = selectedControlColor ?: this.selectedControlColor,
            unselectedContainerColor = unselectedContainerColor ?: this.unselectedContainerColor,
            unselectedContentColor = unselectedContentColor ?: this.unselectedContentColor,
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor ?: this.unselectedSecondaryContentColor,
            unselectedSplitContainerColor =
                unselectedSplitContainerColor ?: this.unselectedSplitContainerColor,
            unselectedControlColor = unselectedControlColor ?: this.unselectedControlColor,
            disabledSelectedContainerColor =
                disabledSelectedContainerColor ?: this.disabledSelectedContainerColor,
            disabledSelectedContentColor =
                disabledSelectedContentColor ?: this.disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor ?: this.disabledSelectedSecondaryContentColor,
            disabledSelectedSplitContainerColor =
                disabledSelectedSplitContainerColor ?: this.disabledSelectedSplitContainerColor,
            disabledSelectedControlColor =
                disabledSelectedControlColor ?: this.disabledSelectedControlColor,
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor ?: this.disabledUncheckedContainerColor,
            disabledUnselectedContentColor =
                disabledUnselectedContentColor ?: this.disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor
                    ?: this.disabledUnselectedSecondaryContentColor,
            disabledUnselectedSplitContainerColor =
                disabledUnselectedSplitContainerColor ?: this.disabledUnselectedSplitContainerColor,
            disabledUnselectedControlColor =
                disabledUnselectedControlColor ?: this.disabledUnselectedControlColor,
        )

    private val disabledUncheckedContainerColor: RemoteColor
        get() = disabledUnselectedContainerColor

    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
    internal fun splitContainerColor(
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let {
                    tween(unselectedSplitContainerColor, selectedSplitContainerColor, it)
                } ?: selected.select(selectedSplitContainerColor, unselectedSplitContainerColor),
            ifFalse =
                selected.select(
                    disabledSelectedSplitContainerColor,
                    disabledUnselectedSplitContainerColor,
                ),
        )

    @Stable
    internal fun controlColor(
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let { tween(unselectedControlColor, selectedControlColor, it) }
                    ?: selected.select(selectedControlColor, unselectedControlColor),
            ifFalse = selected.select(disabledSelectedControlColor, disabledUnselectedControlColor),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteSplitRadioButtonColors) return false

        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedSecondaryContentColor != other.selectedSecondaryContentColor) return false
        if (selectedSplitContainerColor != other.selectedSplitContainerColor) return false
        if (selectedControlColor != other.selectedControlColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedSplitContainerColor != other.unselectedSplitContainerColor) return false
        if (unselectedControlColor != other.unselectedControlColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor != other.disabledSelectedSecondaryContentColor)
            return false
        if (disabledSelectedSplitContainerColor != other.disabledSelectedSplitContainerColor)
            return false
        if (disabledSelectedControlColor != other.disabledSelectedControlColor) return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (
            disabledUnselectedSecondaryContentColor != other.disabledUnselectedSecondaryContentColor
        )
            return false
        if (disabledUnselectedSplitContainerColor != other.disabledUnselectedSplitContainerColor)
            return false
        if (disabledUnselectedControlColor != other.disabledUnselectedControlColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedSplitContainerColor.hashCode()
        result = 31 * result + selectedControlColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedSplitContainerColor.hashCode()
        result = 31 * result + unselectedControlColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedSplitContainerColor.hashCode()
        result = 31 * result + disabledSelectedControlColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedSplitContainerColor.hashCode()
        result = 31 * result + disabledUnselectedControlColor.hashCode()
        return result
    }
}

private fun RemoteDrawScope.drawRadioControl(
    selected: RemoteBoolean,
    controlColor: RemoteColor,
    progress: RemoteFloat = selected.select(1f.rf, 0f.rf),
) {
    val strokeWidth = 2.rdp.toPx()
    val outerRadius = 9.rdp.toPx()
    val centerOffset = RemoteOffset(12.rdp.toPx(), 12.rdp.toPx())
    val innerRadiusPx = lerp(0f.rf, 5.rdp.toPx(), progress)

    // Always draw the outer ring
    drawCircle(
        paint =
            RemotePaint {
                style = PaintingStyle.Stroke
                color = controlColor
                this.strokeWidth = strokeWidth
            },
        radius = outerRadius,
        center = centerOffset,
    )

    drawCircle(
        paint =
            RemotePaint {
                style = PaintingStyle.Fill
                color = tween(Color.Transparent.rc, controlColor, progress)
            },
        radius = innerRadiusPx,
        center = centerOffset,
    )
}
