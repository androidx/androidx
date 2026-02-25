/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState

private const val PressedStateMask = 1 shl 0
private const val HoveredStateMask = 1 shl 1
private const val FocusedStateMask = 1 shl 2
private const val SelectedStateMask = 1 shl 3
private const val EnabledStateMask = 1 shl 4

private const val ToggleStateShift = 5
private const val ToggleStateOff = 0 shl ToggleStateShift
private const val ToggleStateOn = 1 shl ToggleStateShift
private const val ToggleStateIndeterminate = 2 shl ToggleStateShift
private const val ToggleStateMask = 3 shl ToggleStateShift

/**
 * The key to a [StyleState] value. A [StyleState] is a map from [StyleStateKey] to the current
 * value of the style state and the current value can be retrieved from a [StyleState] using array
 * index syntax or calling [StyleState.get] directly.
 *
 * A [StyleStateKey] can be used to create a custom value that can be used in a [Style] to provide
 * properties based on the state of the value.
 *
 * There are a set of predefined states that are expected to be commonly provided by elements that
 * are styleable, [StyleStateKey.Pressed], [StyleStateKey.Hovered], [StyleStateKey.Toggle],
 * [StyleStateKey.Enabled], and [StyleStateKey.Selected]. The [StyleState] provides helper
 * properties for these states, [StyleState.isPressed], [StyleState.isHovered], etc.
 *
 * It is recommended that any new [StyleStateKey] be a constant with at least the same accessibility
 * as the component that uses it. It is also recommended to have a corresponding extension functions
 * for [StyleState] to allow a style to more easily access the state. For example, if a
 * [StyleStateKey] is added for a "playing" state, such as `var PlayingStateKey =
 * styleStateKey(false)` then it is recommended for a `val StyleState.isPlaying =
 * this.get(PlayingStateKey)` to be declared.
 *
 * It is also recommended that an extension function for [StyleScope] also be created that allows
 * the style`fun StyleScope.playing(block: StyleScope.() -> Unit)` be declared that will only call
 * `block` when the `StyleState.isPlaying` is `true`.
 *
 * @sample androidx.compose.foundation.samples.StyleStateKeySample
 */
@ExperimentalFoundationStyleApi
open class StyleStateKey<T>(internal val defaultValue: T) {
    /**
     * Called when an interaction is received on [MutableStyleState.interactionSource] when this key
     * is included in the style state.
     *
     * If a composable function uses an
     * [androidx.compose.foundation.interaction.MutableInteractionSource] to send notifications of
     * interactions, this method can be overridden to update the [styleState] to reflect the state
     * implied by the interactions.
     */
    protected open suspend fun processInteraction(
        interaction: Interaction,
        styleState: MutableStyleState,
    ) {}

    // Internal access. Allows calling [processInteraction] from inside this module.
    internal suspend fun processInteractionAccess(
        interaction: Interaction,
        styleState: MutableStyleState,
    ) {
        processInteraction(interaction, styleState)
    }

    // Overridden by [PredefinedKey] derived types to read the predefined state.
    internal open fun getValueFrom(state: MutableStyleState): T {
        return state.getCustomValue(this)
    }

    // Overridden by [PredefinedKey] derived types to update the predefined state.
    internal open fun setValueTo(value: T, state: MutableStyleState) {
        state.setCustomValue(this, value)
    }

    companion object {
        /**
         * The style state key for the pressed state of a state.
         *
         * This state is `true` when the style component is pressed
         *
         * @see MutableStyleState
         * @see StyleState
         * @see clickable
         */
        val Pressed: StyleStateKey<Boolean> = BooleanPredefinedKey(PressedStateMask)

        /**
         * The style state key for the hovered state of a style.
         *
         * @see MutableStyleState
         * @see StyleState
         * @see androidx.compose.ui.Modifier.hoverable
         */
        val Hovered: StyleStateKey<Boolean> = BooleanPredefinedKey(HoveredStateMask)

        /**
         * The style state key for the focused state of a style.
         *
         * @see MutableStyleState
         * @see StyleState
         * @see androidx.compose.ui.Modifier.focusable
         */
        val Focused: StyleStateKey<Boolean> = BooleanPredefinedKey(FocusedStateMask)

        /**
         * The style state key for the selected state of a style.
         *
         * @see MutableStyleState
         * @see StyleState
         */
        val Selected: StyleStateKey<Boolean> = BooleanPredefinedKey(SelectedStateMask)

        /**
         * The style state key for the enabled state of a style.
         *
         * @see StyleState
         */
        val Enabled: StyleStateKey<Boolean> =
            BooleanPredefinedKey(mask = EnabledStateMask, defaultValue = true)

        /**
         * The style state key for the hovered state of a style.
         *
         * @see StyleState
         */
        val Toggle: StyleStateKey<ToggleableState>
            get() = PredefinedToggleStateKey
    }
}

/**
 * A utility function used to update boolean values of the predefined state of a [StyleState].
 *
 * @param predefinedState the value of [StyleStateImpl.predefinedState] to update
 * @param mask the value mask of the state to update.
 * @param include whether to include the state or exclude it.
 * @see FocusedStateMask
 * @see HoveredStateMask
 * @see PressedStateMask
 * @see SelectedStateMask
 * @see ToggleStateMask
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun updateFromMask(predefinedState: Int, mask: Int, include: Boolean): Int =
    (predefinedState and mask.inv()) or if (include) mask else 0

internal interface PredefinedKey

/** [StyleStateKey] for boolean values that are stored in [MutableStyleState.predefinedState] */
@ExperimentalFoundationStyleApi
internal class BooleanPredefinedKey(val mask: Int, defaultValue: Boolean = false) :
    StyleStateKey<Boolean>(defaultValue), PredefinedKey {
    override fun getValueFrom(state: MutableStyleState): Boolean =
        state.predefinedState and mask != 0

    override fun setValueTo(value: Boolean, state: MutableStyleState) {
        state.predefinedState = updateFromMask(mask, state.predefinedState, value)
    }
}

/**
 * The [StyleStateKey] for [ToggleableState] which is stored in the
 * [MutableStyleState.predefinedState].
 */
@ExperimentalFoundationStyleApi
internal object PredefinedToggleStateKey :
    StyleStateKey<ToggleableState>(ToggleableState.Off), PredefinedKey {
    override fun getValueFrom(state: MutableStyleState): ToggleableState =
        when (state.predefinedState and ToggleStateMask) {
            ToggleStateOn -> ToggleableState.On
            ToggleStateOff -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }

    override fun setValueTo(value: ToggleableState, state: MutableStyleState) {
        state.predefinedState =
            (state.predefinedState and ToggleStateMask.inv()) or
                when (value) {
                    ToggleableState.On -> ToggleStateOn
                    ToggleableState.Off -> ToggleStateOff
                    else -> ToggleStateIndeterminate
                }
    }
}

/**
 * The state a style can use to select property values depending on the state of the component is a
 * style for.
 *
 * @sample androidx.compose.foundation.samples.StyleStateSample
 * @see StyleScope.pressed
 * @see StyleScope.focused
 * @see StyleScope.hovered
 */
@ExperimentalFoundationStyleApi
sealed class StyleState {
    /**
     * [isEnabled] is `true` when the stylable component is enabled.
     *
     * Elements that can be enabled and disabled will set value when they are called.
     *
     * The [StyleState.isEnabled] function will only execute its `block` parameter when this
     * [isEnabled] is true.
     *
     * [androidx.compose.foundation.text.BasicTextField], for example, sets this value to the value
     * of the `enabled` parameter.
     */
    abstract val isEnabled: Boolean

    /**
     * [isFocused] is `true` when the stylable component is focused.
     *
     * Elements that are focusable will set this state to `true` when are focused.
     *
     * The [StyleScope.focused] function reads this state and will only set the properties set in
     * its `block` parameter when [isPressed] is true.
     *
     * For example, [focusable] will send [FocusInteraction.Focus] and [FocusInteraction.Unfocus]
     * interactions when the component receives focus or loses focus. When the style state is
     * watching an [InteractionSource] this state will be updated when the focus interactions are
     * received in the [InteractionSource].
     */
    abstract val isFocused: Boolean

    /**
     * [isHovered] is `true` when the stylable component is hovered.
     *
     * Elements that are hoverable will set this state to `true` when they are hovered.
     *
     * The [StyleScope.hovered] function reads this state and will only set the properties set in
     * its `block` parameter when [isPressed] is true.
     *
     * For example, [hoverable] will send [HoverInteraction.Enter] and [HoverInteraction.Exit]
     * interactions when a mouse enters or exits the component. When the style state is watching an
     * [InteractionSource] this state will be updated when the focus interactions are received in
     * the [InteractionSource].
     */
    abstract val isHovered: Boolean

    /**
     * [isPressed] is `true` when the stylable component is pressed.
     *
     * Elements that are pressable will set this state to `true` when they are pressed.
     *
     * The [StyleScope.pressed] function reads this state and will only set the properties set in
     * its `block` parameter when [isPressed] is `true`.
     *
     * For example, [clickable] will send [PressInteraction.Press] and [PressInteraction.Release]
     * interactions when the component is pressed and released. When the style state is watching an
     * [InteractionSource] it will update this state.
     */
    abstract val isPressed: Boolean

    /**
     * [isSelected] is `true` when the stylable component is selected.
     *
     * Elements that are selectable will set this property to `true` when they are selected.
     *
     * The [StyleScope.selected] function reads this state and will only set the properties in its
     * `block` parameter when [isSelected] is `true`.
     */
    abstract val isSelected: Boolean

    /**
     * [isChecked] is `true` when the stylable component is checked.
     *
     * Elements that are toggleable will set this property to `true` when they are checked.
     *
     * For example, components that use [toggleable] set [isChecked] to the value of the `checked`
     * parameter.
     *
     * The [StyleScope.checked] function reads this state and will only set the properties in its
     * `block` parameter when [isChecked] is `true`.
     */
    abstract val isChecked: Boolean

    /**
     * [triStateToggle] is the state of a tri-state toggleable. A tri-state togglable is a component
     * that can represent that the checked state is unspecified.
     *
     * Elements that are selectable will set this property to `true` when is checked.
     *
     * For example, components that use [triStateToggleable] will set [triStateToggle] parameter to
     * the value of the `checked` parameter.
     *
     * The [StyleScope.checked] function reads this state and will only set the properties in its
     * `block` parameter when [triStateToggle] is [ToggleableState.On].
     *
     * The [StyleScope.triStateToggleIndeterminate] function reads this state and will only set the
     * properties in its `block` parameter when the [triStateToggle] is
     * [ToggleableState.Indeterminate].
     */
    abstract val triStateToggle: ToggleableState

    /**
     * Read the value of a style state [key]. This overloads the index operator which allows reading
     * the key using the array index syntax.
     *
     * This allows components to define custom state that can be used in a style to control the look
     * of a composable. For example, a video playing application can introduce a custom
     * `PlayingStyleState<Boolean>` and set the value of this state. This state can then be in a
     * [Style] to customize the look of the component when it moves in an out of playing a value.
     */
    abstract operator fun <T> get(key: StyleStateKey<T>): T

    internal abstract suspend fun processInteractions(interactions: InteractionSource)

    internal abstract val interactionSource: InteractionSource?
}

/**
 * Defines a [Style] to be applied when the component is [StyleState.isChecked] is `true`. The
 * properties within the provided [value] Style will override or merge with the base style of the
 * component when a toggle interaction is detected.
 *
 * @see StyleState.isChecked
 * @see focused
 * @see hovered
 * @see pressed
 * @see selected
 * @see androidx.compose.ui.Modifier.toggleable
 */
@ExperimentalFoundationStyleApi
fun StyleScope.checked(value: Style) {
    state(StyleStateKey.Toggle, value) { _, state -> state.isChecked }
}

/**
 * Defines a [Style] to be applied when the component is disabled. The properties within the
 * provided `value` Style will override or merge with the base style of the component when the style
 * state for the component is disabled.
 *
 * @param value The [Style] to apply on hover.
 * @see StyleState.isEnabled
 */
@ExperimentalFoundationStyleApi
fun StyleScope.disabled(value: Style) {
    state(StyleStateKey.Enabled, value) { _, state -> !state.isEnabled }
}

/**
 * Defines a [Style] to be applied when the component is focused. The properties within the provided
 * `value` Style will override or merge with the base style of the component when a focus
 * interaction is detected.
 *
 * @param value The [Style] to apply on focus.
 * @see StyleState.isFocused
 * @see checked
 * @see hovered
 * @see pressed
 * @see selected
 */
@ExperimentalFoundationStyleApi
fun StyleScope.focused(value: Style) {
    state(StyleStateKey.Focused, value) { _, state -> state.isFocused }
}

/**
 * Defines a [Style] to be applied when the component is hovered. The properties within the provided
 * `value` Style will override or merge with the base style of the component when a hover
 * interaction is detected.
 *
 * @param value The [Style] to apply on hover.
 * @see StyleState.isHovered
 * @see checked
 * @see focused
 * @see pressed
 * @see selected
 */
@ExperimentalFoundationStyleApi
fun StyleScope.hovered(value: Style) {
    state(StyleStateKey.Hovered, value) { _, state -> state.isHovered }
}

/**
 * Defines a [Style] to be applied when the component is pressed. The properties within the provided
 * `value` Style will override or merge with the base style of the component when a press
 * interaction is detected.
 *
 * @param value The [Style] to apply on press.
 * @see StyleState.isPressed
 * @see checked
 * @see focused
 * @see hovered
 * @see selected
 */
@ExperimentalFoundationStyleApi
fun StyleScope.pressed(value: Style) {
    state(StyleStateKey.Pressed, value) { _, state -> state.isPressed }
}

/**
 * Defines a [Style] to be applied when the component is [StyleState.isSelected] is `true`. The
 * properties within the provided [value] Style will override or merge with the base style of the
 * component when a toggle interaction is detected.
 *
 * @see StyleState.isSelected
 */
@ExperimentalFoundationStyleApi
fun StyleScope.selected(value: Style) {
    state(StyleStateKey.Selected, value) { _, state -> state.isSelected }
}

/**
 * Defines a [Style] to be applied when the component is [StyleState.triStateToggle] is
 * [ToggleableState.On]. The properties within the provided [value] Style will override or merge
 * with the base style of the component when a toggle interaction is detected.
 *
 * @see StyleState.triStateToggle
 * @see androidx.compose.ui.Modifier.triStateToggleable
 */
@ExperimentalFoundationStyleApi
fun StyleScope.triStateToggleOn(value: Style) {
    state(StyleStateKey.Toggle, value) { _, state -> state.triStateToggle == ToggleableState.On }
}

/**
 * Defines a [Style] to be applied when the component is [StyleState.triStateToggle] is
 * [ToggleableState.Off]. The properties within the provided [value] Style will override or merge
 * with the base style of the component when a toggle interaction is detected.
 *
 * @see StyleState.triStateToggle
 * @see androidx.compose.ui.Modifier.triStateToggleable
 */
@ExperimentalFoundationStyleApi
fun StyleScope.triStateToggleOff(value: Style) {
    state(StyleStateKey.Toggle, value) { _, state -> state.triStateToggle == ToggleableState.Off }
}

/**
 * Defines a [Style] to be applied when the component is [StyleState.triStateToggle] is
 * [ToggleableState.Indeterminate]. The properties within the provided [value] Style will override
 * or merge with the base style of the component when a toggle interaction is detected.
 *
 * @see StyleState.triStateToggle
 * @see androidx.compose.ui.Modifier.triStateToggleable
 */
@ExperimentalFoundationStyleApi
fun StyleScope.triStateToggleIndeterminate(value: Style) {
    state(StyleStateKey.Toggle, value) { _, state ->
        state.triStateToggle == ToggleableState.Indeterminate
    }
}

/**
 * The state a style that can be updated to reflect the current state of a component. This value
 * should be created in a component and updated to select the style parameter to be set for the
 * component.
 *
 * A component that uses an interaction source can create a [MutableStyleState] that observes the
 * interactions emitted to the interaction source.
 *
 * @sample androidx.compose.foundation.samples.StyleStateSample
 * @see StyleScope.pressed
 * @see StyleScope.focused
 * @see StyleScope.hovered
 */
@ExperimentalFoundationStyleApi
class MutableStyleState
@RememberInComposition
constructor(override val interactionSource: InteractionSource?) : StyleState() {
    internal var customStates = mutableStateMapOf<StyleStateKey<*>, Any>()
    internal var predefinedState: Int by mutableIntStateOf(EnabledStateMask)

    override var isEnabled: Boolean
        get() = predefinedState and EnabledStateMask != 0
        set(value) {
            predefinedState = updateFromMask(predefinedState, EnabledStateMask, value)
        }

    override var isFocused: Boolean
        get() = predefinedState and FocusedStateMask != 0
        set(value) {
            predefinedState = updateFromMask(predefinedState, FocusedStateMask, value)
        }

    override var isHovered: Boolean
        get() = predefinedState and HoveredStateMask != 0
        set(value) {
            predefinedState = updateFromMask(predefinedState, HoveredStateMask, value)
        }

    override var isPressed: Boolean
        get() = predefinedState and PressedStateMask != 0
        set(value) {
            predefinedState = updateFromMask(predefinedState, PressedStateMask, value)
        }

    override var isSelected: Boolean
        get() = predefinedState and SelectedStateMask != 0
        set(value) {
            predefinedState = updateFromMask(predefinedState, SelectedStateMask, value)
        }

    override var triStateToggle: ToggleableState
        get() = PredefinedToggleStateKey.getValueFrom(this)
        set(value) {
            PredefinedToggleStateKey.setValueTo(value, this)
        }

    override var isChecked: Boolean
        get() = PredefinedToggleStateKey.getValueFrom(this) == ToggleableState.On
        set(value) {
            PredefinedToggleStateKey.setValueTo(
                if (value) ToggleableState.On else ToggleableState.Off,
                this,
            )
        }

    override operator fun <T> get(key: StyleStateKey<T>): T = key.getValueFrom(this)

    /** Set the [value] of the [key] in the [StyleState]. */
    operator fun <T> set(key: StyleStateKey<T>, value: T) {
        key.setValueTo(value, this)
    }

    /**
     * Removes the [StyleStateKey] from the [StyleState].
     *
     * A [Style] will read the [StyleStateKey.defaultValue] for the [StyleStateKey] when the key is
     * not present in the [StyleState].
     *
     * Removing a key that is updated via an [InteractionSource] will no longer observe the
     * interaction source.
     *
     * Predefined style keys, such as [StyleStateKey.Pressed] and [StyleStateKey.Hovered], cannot be
     * removed from the set of keys and this will throw if removed.
     */
    fun <T> remove(key: StyleStateKey<T>) {
        check(key !is PredefinedKey) { "Cannot remove an internal StyleStateKey" }
        customStates.remove(key)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> getCustomValue(key: StyleStateKey<T>): T =
        customStates.getOrElse(key) { key.defaultValue } as T

    internal fun <T> setCustomValue(key: StyleStateKey<T>, value: T) {
        customStates[key] = value as Any
    }

    override suspend fun processInteractions(interactions: InteractionSource) {
        val pressedInteractions = InteractionSet<PressInteraction.Press>()
        val hoveredInteractions = InteractionSet<HoverInteraction.Enter>()
        val focusedInteractions = InteractionSet<FocusInteraction.Focus>()

        isPressed = false
        isHovered = false
        isFocused = false

        interactions.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressedInteractions.add(interaction)
                    isPressed = true
                }

                is PressInteraction.Release -> {
                    pressedInteractions.remove(interaction.press)
                    isPressed = pressedInteractions.isNotEmpty()
                }

                is PressInteraction.Cancel -> {
                    pressedInteractions.remove(interaction.press)
                    isPressed = pressedInteractions.isNotEmpty()
                }

                is HoverInteraction.Enter -> {
                    hoveredInteractions.add(interaction)
                    isHovered = true
                }

                is HoverInteraction.Exit -> {
                    hoveredInteractions.remove(interaction.enter)
                    isHovered = hoveredInteractions.isNotEmpty()
                }

                is FocusInteraction.Focus -> {
                    focusedInteractions.add(interaction)
                    isFocused = true
                }

                is FocusInteraction.Unfocus -> {
                    focusedInteractions.remove(interaction.focus)
                    isFocused = focusedInteractions.isNotEmpty()
                }
                else ->
                    customStates.forEach { styleStateEntry ->
                        styleStateEntry.key.processInteractionAccess(interaction, this)
                    }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private class InteractionSet<T : Interaction> {
    private var setOrValue: Any? = null

    fun isNotEmpty() = setOrValue != null

    fun add(interaction: T) {
        when (val value = setOrValue) {
            null -> setOrValue = interaction
            is MutableScatterSet<*> -> (value as MutableScatterSet<T>).add(interaction)
            interaction -> {
                // Nothing to do as the value is already recorded
            }
            else -> {
                setOrValue = mutableScatterSetOf(value as T, interaction)
            }
        }
    }

    fun remove(interaction: T) {
        when (val value = setOrValue) {
            interaction -> setOrValue = null
            is MutableScatterSet<*> -> {
                val set = (value as MutableScatterSet<T>)
                set.remove(interaction)
                when (set.size) {
                    0 -> setOrValue = null
                    1 -> setOrValue = set.first()
                }
            }
        }
    }
}
