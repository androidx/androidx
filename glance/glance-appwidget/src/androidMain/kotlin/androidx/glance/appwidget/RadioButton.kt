/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.unit.CheckableColorProvider
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider.Companion.createCheckableColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider

/** Set of colors to apply to a RadioButton depending on the checked state. */
class RadioButtonColors internal constructor(internal val radio: CheckableColorProvider)

/**
 * RadioButtonColors to tint the drawable of the [RadioButton] according to the checked state.
 *
 * None of the [ColorProvider] parameters to this function can be created from resource ids. To use
 * resources to tint the switch color, use `RadioButtonColors(Int, Int)` instead.
 *
 * @param checkedColor the tint to apply to the radio button when it is checked, or null
 * to use the default tint.
 * @param uncheckedColor the tint to apply to the radio button when it is not checked,
 * or null to use the default tint.
 */
fun RadioButtonColors(
    checkedColor: ColorProvider?,
    uncheckedColor: ColorProvider?,
): RadioButtonColors {
    return RadioButtonColors(
        radio = createCheckableColorProvider(
            source = "RadioButtonColors",
            checked = checkedColor,
            unchecked = uncheckedColor,
            fallback = R.color.glance_default_radio_button
        )
    )
}

/**
 * [RadioButtonColors] set to color resources.
 *
 * These may be fixed colors or a color selector that selects color depending on
 * [android.R.attr.state_checked].
 *
 * @param color the resource to use to tint the radio button. If an invalid resource id is provided,
 * the default colors will be used.
 */
fun RadioButtonColors(
    @ColorRes color: Int = R.color.glance_default_radio_button
): RadioButtonColors =
    RadioButtonColors(
        radio = ResourceCheckableColorProvider(
            resId = color,
            fallback = R.color.glance_default_radio_button
        )
    )

/**
 * [RadioButtonColors] that uses [checkedColor] or [uncheckedColor] depending on the checked state
 * of the RadioButton.
 *
 * @param checkedColor the [Color] to use when the RadioButton is checked
 * @param uncheckedColor the [Color] to use when the RadioButton is not checked
 */
fun RadioButtonColors(checkedColor: Color, uncheckedColor: Color): RadioButtonColors =
    RadioButtonColors(FixedColorProvider(checkedColor), FixedColorProvider(uncheckedColor))

internal class EmittableRadioButton : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var enabled: Boolean = true
    var text: String = ""
    var style: TextStyle? = null
    var colors: RadioButtonColors = RadioButtonColors()
    var maxLines: Int = Int.MAX_VALUE
}

/**
 * Adds a radio button to the glance view.
 *
 * When showing a [Row] or [Column] that has [RadioButton] children, use
 * [GlanceModifier.selectableGroup] to enable the radio group effect (unselecting the previously
 * selected radio button when another is selected).
 *
 * @param checked whether the radio button is checked
 * @param onClick the action to be run when the radio button is clicked
 * @param modifier the modifier to apply to the radio button
 * @param enabled if false, the radio button will not be clickable
 * @param text the text to display to the end of the radio button
 * @param style the style to apply to [text]
 * @param colors the color tint to apply to the radio button
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun RadioButton(
    checked: Boolean,
    onClick: Action?,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    text: String = "",
    style: TextStyle? = null,
    colors: RadioButtonColors = RadioButtonColors(),
    maxLines: Int = Int.MAX_VALUE,
) {
    val finalModifier = if (enabled && onClick != null) modifier.clickable(onClick) else modifier
    GlanceNode(
        factory = ::EmittableRadioButton,
        update = {
            this.set(checked) { this.checked = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(enabled) { this.enabled = it }
            this.set(text) { this.text = it }
            this.set(style) { this.style = it }
            this.set(colors) { this.colors = it }
            this.set(maxLines) { this.maxLines = it }
        }
    )
}

/**
 * Use this modifier to group a list of RadioButtons together for accessibility purposes.
 *
 * This modifier can only be used on a [Row] or [Column]. This modifier additonally enables
 * the radio group effect, which automatically unselects the currently selected RadioButton when
 * another is selected. When this modifier is used, an error will be thrown if more than one
 * RadioButton has their "checked" value set to true.
 */
fun GlanceModifier.selectableGroup(): GlanceModifier =
    this.then(SelectableGroupModifier)

internal object SelectableGroupModifier : GlanceModifier.Element

internal val GlanceModifier.isSelectableGroup: Boolean
    get() = any { it is SelectableGroupModifier }
