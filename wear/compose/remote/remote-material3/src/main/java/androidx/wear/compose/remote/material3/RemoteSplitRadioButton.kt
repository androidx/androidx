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
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
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
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.TextConfiguration

/** @sample androidx.wear.compose.remote.material3.samples.RemoteSplitRadioButtonSample */
@RemoteComposable
@Composable
public fun RemoteSplitRadioButton(
    selected: RemoteBoolean,
    onSelectionClick: RemoteAction,
    selectionContentDescription: RemoteString?,
    onContainerClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteSplitRadioButtonDefaults.shape,
    colors: RemoteSplitRadioButtonColors = RemoteSplitRadioButtonDefaults.splitRadioButtonColors(),
    contentPadding: RemotePaddingValues = RemoteSplitRadioButtonDefaults.ContentPadding,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val progress = animateSelectionControlProgress(selected)
    val colorProgress = animateSelectionColorProgress(selected)
    val (startSectionShape, endSectionShape) =
        splitSectionShapes(shape, RemoteSplitRadioButtonDefaults.splitSectionsShape)
    val containerColor =
        colors.containerColor(enabled = enabled, selected = selected, progress = colorProgress)
    val contentColor =
        colors.contentColor(enabled = enabled, selected = selected, progress = colorProgress)
    val secondaryContentColor =
        colors.secondaryContentColor(
            enabled = enabled,
            selected = selected,
            progress = colorProgress,
        )
    val splitContainerColor =
        colors.splitContainerColor(enabled = enabled, selected = selected, progress = colorProgress)
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
                    .clip(startSectionShape)
                    .clickable(
                        onClick = onContainerClick,
                        enabled =
                            (enabled.constantValueOrNull ?: false) &&
                                onContainerClick != RemoteAction.None,
                    )
                    .drawWithContent {
                        drawSolidColorShape(startSectionShape, containerColor)
                        drawContent()
                    }
                    .padding(contentPadding)
                    .semantics(mergeDescendants = true) { role = RemoteRole.Button },
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
                    .clip(endSectionShape)
                    .clickable(
                        onClick = onSelectionClick,
                        enabled =
                            (enabled.constantValueOrNull ?: false) &&
                                onSelectionClick != RemoteAction.None,
                    )
                    .drawWithContent {
                        drawSolidColorShape(
                            endSectionShape,
                            enabled.select(containerColor, Color.Black.rc),
                        )
                        drawSolidColorShape(endSectionShape, splitContainerColor)
                        drawContent()
                    }
                    .padding(contentPadding)
                    .semantics(mergeDescendants = true) {
                        role = RemoteRole.RadioButton
                        if (selectionContentDescription != null) {
                            contentDescription = selectionContentDescription
                        }
                    },
        ) {
            RemoteRadioControl(
                selected = selected,
                controlColor = controlColor,
                progress = progress,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSplitRadioButtonDefaults {
    public val shape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(26.rdp)

    public val splitSectionsShape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(4.rdp)

    public val Height: RemoteDp = 52.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = 14.rdp, vertical = 8.rdp)

    @Composable
    @RemoteComposable
    public fun splitRadioButtonColors(): RemoteSplitRadioButtonColors =
        RemoteMaterialTheme.colorScheme.defaultSplitRadioButtonColors

    @Composable
    @RemoteComposable
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
        @RemoteComposable
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
                disabledUnselectedControlColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
            )
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
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
        selectedContainerColor: RemoteColor = this.selectedContainerColor,
        selectedContentColor: RemoteColor = this.selectedContentColor,
        selectedSecondaryContentColor: RemoteColor = this.selectedSecondaryContentColor,
        selectedSplitContainerColor: RemoteColor = this.selectedSplitContainerColor,
        selectedControlColor: RemoteColor = this.selectedControlColor,
        unselectedContainerColor: RemoteColor = this.unselectedContainerColor,
        unselectedContentColor: RemoteColor = this.unselectedContentColor,
        unselectedSecondaryContentColor: RemoteColor = this.unselectedSecondaryContentColor,
        unselectedSplitContainerColor: RemoteColor = this.unselectedSplitContainerColor,
        unselectedControlColor: RemoteColor = this.unselectedControlColor,
        disabledSelectedContainerColor: RemoteColor = this.disabledSelectedContainerColor,
        disabledSelectedContentColor: RemoteColor = this.disabledSelectedContentColor,
        disabledSelectedSecondaryContentColor: RemoteColor =
            this.disabledSelectedSecondaryContentColor,
        disabledSelectedSplitContainerColor: RemoteColor = this.disabledSelectedSplitContainerColor,
        disabledSelectedControlColor: RemoteColor = this.disabledSelectedControlColor,
        disabledUnselectedContainerColor: RemoteColor = this.disabledUnselectedContainerColor,
        disabledUnselectedContentColor: RemoteColor = this.disabledUnselectedContentColor,
        disabledUnselectedSecondaryContentColor: RemoteColor =
            this.disabledUnselectedSecondaryContentColor,
        disabledUnselectedSplitContainerColor: RemoteColor =
            this.disabledUnselectedSplitContainerColor,
        disabledUnselectedControlColor: RemoteColor = this.disabledUnselectedControlColor,
    ): RemoteSplitRadioButtonColors =
        RemoteSplitRadioButtonColors(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedSplitContainerColor = selectedSplitContainerColor,
            selectedControlColor = selectedControlColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedSplitContainerColor = unselectedSplitContainerColor,
            unselectedControlColor = unselectedControlColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor = disabledSelectedSecondaryContentColor,
            disabledSelectedSplitContainerColor = disabledSelectedSplitContainerColor,
            disabledSelectedControlColor = disabledSelectedControlColor,
            disabledUnselectedContainerColor = disabledUnselectedContainerColor,
            disabledUnselectedContentColor = disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor = disabledUnselectedSecondaryContentColor,
            disabledUnselectedSplitContainerColor = disabledUnselectedSplitContainerColor,
            disabledUnselectedControlColor = disabledUnselectedControlColor,
        )

    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
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
    internal fun splitContainerColor(
        enabled: RemoteBoolean = true.rb,
        selected: RemoteBoolean = true.rb,
        progress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                progress?.let {
                    lerp(unselectedSplitContainerColor, selectedSplitContainerColor, it)
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
                progress?.let { lerp(unselectedControlColor, selectedControlColor, it) }
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
