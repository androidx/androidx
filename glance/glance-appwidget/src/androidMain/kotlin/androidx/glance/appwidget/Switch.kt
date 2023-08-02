/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.appwidget.action.CompoundButtonAction
import androidx.glance.appwidget.unit.CheckableColorProvider
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider.Companion.createCheckableColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Set of colors to apply to a Switch depending on the checked state. */
sealed class SwitchColors {
    internal abstract val thumb: CheckableColorProvider
    internal abstract val track: CheckableColorProvider
}

internal data class SwitchColorsImpl(
    override val thumb: CheckableColorProvider,
    override val track: CheckableColorProvider
) : SwitchColors()

/**
 * SwitchColors to tint the thumb and track of the [Switch] according to the checked state.
 *
 * None of the [ColorProvider] parameters to this function can be created from resource ids. To use
 * resources to tint the switch color, use `SwitchColors(Int, Int)` instead.
 *
 * @param checkedThumbColor the tint to apply to the thumb of the switch when it is checked, or null
 * to use the default tint
 * @param uncheckedThumbColor the tint to apply to the thumb of the switch when it is not checked,
 * or null to use the default tint
 * @param checkedTrackColor the tint to apply to the track of the switch when it is checked, or null
 * to use the default tints
 * @param uncheckedTrackColor the tint to apply to the track of the switch when it is not checked,
 * or null to use the default tint
 */
fun SwitchColors(
    checkedThumbColor: ColorProvider? = null,
    uncheckedThumbColor: ColorProvider? = null,
    checkedTrackColor: ColorProvider? = null,
    uncheckedTrackColor: ColorProvider? = null,
): SwitchColors {
    return SwitchColorsImpl(
        thumb = createCheckableColorProvider(
            source = "SwitchColors",
            checked = checkedThumbColor,
            unchecked = uncheckedThumbColor,
            fallback = R.color.glance_default_switch_thumb
        ),
        track = createCheckableColorProvider(
            source = "SwitchColors",
            checked = checkedTrackColor,
            unchecked = uncheckedTrackColor,
            fallback = R.color.glance_default_switch_track
        )
    )
}

/**
 * [SwitchColors] set to color resources.
 *
 * These may be fixed colors or a color selector that selects color depending on
 * [android.R.attr.state_checked].
 *
 * @param thumbColor the resource to use to tint the thumb. If an invalid resource id is provided,
 * the default switch colors will be used.
 * @param trackColor the resource to use to tint the track. If an invalid resource id is provided,
 * the default switch colors will be used.
 */
fun SwitchColors(
    @ColorRes thumbColor: Int,
    @ColorRes trackColor: Int = R.color.glance_default_switch_track
): SwitchColors =
    SwitchColorsImpl(
        thumb = ResourceCheckableColorProvider(
            resId = thumbColor,
            fallback = R.color.glance_default_switch_thumb
        ),
        track = ResourceCheckableColorProvider(
            resId = trackColor,
            fallback = R.color.glance_default_switch_track
        )
    )

/** Defaults for the [Switch]. */
object SwitchDefaults {
    /** Default [SwitchColors] to apply. */
    val colors: SwitchColors = SwitchColors()
}

/**
 * Adds a switch view to the glance view.
 *
 * @param checked whether the switch is checked
 * @param onCheckedChange the action to be run when the switch is clicked. The current value of
 * checked is provided to this action in its ActionParameters, and can be retrieved using the
 * [ToggleableStateKey]. If this action launches an activity, the current value of checked will be
 * passed as an intent extra with the name [RemoteViews.EXTRA_CHECKED].
 * @param modifier the modifier to apply to the switch
 * @param text the text to display to the end of the switch
 * @param style the style to apply to [text]
 * @param colors the tint colors for the thumb and track of the switch
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: SwitchColors = SwitchDefaults.colors,
    maxLines: Int = Int.MAX_VALUE,
) {
    val finalModifier = if (onCheckedChange != null) {
        modifier.then(ActionModifier(CompoundButtonAction(onCheckedChange, checked)))
    } else {
        modifier
    }
    GlanceNode(
        factory = ::EmittableSwitch,
        update = {
            this.set(checked) { this.checked = it }
            this.set(text) { this.text = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(colors) { this.colors = it }
            this.set(maxLines) { this.maxLines = it }
        }
    )
}

internal class EmittableSwitch : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var text: String = ""
    var style: TextStyle? = null
    var colors: SwitchColors = SwitchDefaults.colors
    var maxLines: Int = Int.MAX_VALUE

    override fun toString(): String = "EmittableSwitch(" +
        "$text, " +
        "modifier=$modifier, " +
        "checked=$checked, " +
        "style=$style, " +
        "colors=$colors, " +
        "maxLines=$maxLines" +
        ")"
}