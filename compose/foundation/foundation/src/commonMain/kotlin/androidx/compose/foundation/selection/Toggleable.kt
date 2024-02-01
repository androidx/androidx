/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.foundation.selection

import androidx.compose.foundation.ClickableNode
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickableWithIndicationIfNeeded
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

/**
 * Configure component to make it toggleable via input and accessibility events
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this toggleable modifier inside composition, consider using the other
 * overload and explicitly passing `LocalIndication.current` for improved performance. For more
 * information see the documentation on the other overload.
 *
 * @sample androidx.compose.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 */
fun Modifier.toggleable(
    value: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    onValueChange: (Boolean) -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "toggleable"
        properties["value"] = value
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onValueChange"] = onValueChange
    }
) {
    val localIndication = LocalIndication.current
    val interactionSource = if (localIndication is IndicationNodeFactory) {
        // We can fast path here as it will be created inside clickable lazily
        null
    } else {
        // We need an interaction source to pass between the indication modifier and clickable, so
        // by creating here we avoid another composed down the line
        remember { MutableInteractionSource() }
    }
    Modifier.toggleable(
        value = value,
        interactionSource = interactionSource,
        indication = localIndication,
        enabled = enabled,
        role = role,
        onValueChange = onValueChange
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an
 * internal [MutableInteractionSource] will be lazily created along with the [indication] only when
 * needed. This reduces the performance cost of toggleable during composition, as creating the
 * [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of toggleable, it is recommended to
 * instead provide `null` to enable lazy creation. If you need [indication] to be created eagerly,
 * provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside toggleable.
 *
 * @sample androidx.compose.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this toggleable is pressed. If `null`, an internal
 * [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 */
fun Modifier.toggleable(
    value: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onValueChange: (Boolean) -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "toggleable"
        properties["value"] = value
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onValueChange"] = onValueChange
    }
) {
    clickableWithIndicationIfNeeded(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = indication
    ) { interactionSource, indicationNodeFactory ->
        ToggleableElement(
            value = value,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            role = role,
            onValueChange = onValueChange
        )
    }
}

private class ToggleableElement(
    private val value: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val enabled: Boolean,
    private val role: Role?,
    private val onValueChange: (Boolean) -> Unit
) : ModifierNodeElement<ToggleableNode>() {
    override fun create() = ToggleableNode(
        value = value,
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        enabled = enabled,
        role = role,
        onValueChange = onValueChange
    )

    override fun update(node: ToggleableNode) {
        node.update(
            value = value,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            role = role,
            onValueChange = onValueChange
        )
    }

    // Defined in the factory functions with inspectable
    override fun InspectorInfo.inspectableProperties() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as ToggleableElement

        if (value != other.value) return false
        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (enabled != other.enabled) return false
        if (role != other.role) return false
        if (onValueChange != other.onValueChange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onValueChange.hashCode()
        return result
    }
}

private class ToggleableNode(
    private var value: Boolean,
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    enabled: Boolean,
    role: Role?,
    private var onValueChange: (Boolean) -> Unit
) : ClickableNode(
    interactionSource = interactionSource,
    indicationNodeFactory = indicationNodeFactory,
    enabled = enabled,
    onClickLabel = null,
    role = role,
    onClick = { onValueChange(!value) }
) {
    // the onClick passed in the constructor captures onValueChanged and value as passed to the
    // constructor, so we need to define a new lambda that references the properties. When these
    // change, update will be called, which will set this as the new onClick, so it doesn't matter
    // that we are pointing to the wrong lambda before the first toggle. (Additionally changing
    // onClick does not cause any invalidations / side effects, so there is no cost from setting
    // it up this way).
    val _onClick = { onValueChange(!value) }

    fun update(
        value: Boolean,
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        enabled: Boolean,
        role: Role?,
        onValueChange: (Boolean) -> Unit
    ) {
        if (this.value != value) {
            this.value = value
            invalidateSemantics()
        }
        this.onValueChange = onValueChange
        super.update(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            onClickLabel = null,
            role = role,
            onClick = _onClick
        )
    }

    override fun SemanticsPropertyReceiver.applyAdditionalSemantics() {
        toggleableState = ToggleableState(value)
    }
}

/**
 * Configure component to make it toggleable via input and accessibility events with three
 * states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are dependent Toggleables associated to this
 * component and those can have different values.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this triStateToggleable modifier inside composition, consider using the
 * other overload and explicitly passing `LocalIndication.current` for improved performance. For
 * more information see the documentation on the other overload.
 *
 * @sample androidx.compose.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks the toggleable.
 */
fun Modifier.triStateToggleable(
    state: ToggleableState,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "triStateToggleable"
        properties["state"] = state
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    val localIndication = LocalIndication.current
    val interactionSource = if (localIndication is IndicationNodeFactory) {
        // We can fast path here as it will be created inside clickable lazily
        null
    } else {
        // We need an interaction source to pass between the indication modifier and clickable, so
        // by creating here we avoid another composed down the line
        remember { MutableInteractionSource() }
    }
    Modifier.triStateToggleable(
        state = state,
        interactionSource = interactionSource,
        indication = localIndication,
        enabled = enabled,
        role = role,
        onClick = onClick
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events with three
 * states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are dependent Toggleables associated to this
 * component and those can have different values.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an
 * internal [MutableInteractionSource] will be lazily created along with the [indication] only when
 * needed. This reduces the performance cost of triStateToggleable during composition, as creating
 * the [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of triStateToggleable, it is
 * recommended to instead provide `null` to enable lazy creation. If you need [indication] to be
 * created eagerly, provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside triStateToggleable.
 *
 * @sample androidx.compose.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this triStateToggleable is pressed. If `null`, an internal
 * [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks the toggleable.
 */
fun Modifier.triStateToggleable(
    state: ToggleableState,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "triStateToggleable"
        properties["state"] = state
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    clickableWithIndicationIfNeeded(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = indication
    ) { interactionSource, indicationNodeFactory ->
        TriStateToggleableElement(
            state = state,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
    }
}

private class TriStateToggleableElement(
    private val state: ToggleableState,
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val enabled: Boolean,
    private val role: Role?,
    private val onClick: () -> Unit
) : ModifierNodeElement<TriStateToggleableNode>() {
    override fun create() = TriStateToggleableNode(
        state = state,
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        enabled = enabled,
        role = role,
        onClick = onClick
    )

    override fun update(node: TriStateToggleableNode) {
        node.update(
            state = state,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
    }

    // Defined in the factory functions with inspectable
    override fun InspectorInfo.inspectableProperties() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as TriStateToggleableElement

        if (state != other.state) return false
        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (enabled != other.enabled) return false
        if (role != other.role) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class TriStateToggleableNode(
    private var state: ToggleableState,
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    enabled: Boolean,
    role: Role?,
    onClick: () -> Unit
) : ClickableNode(
    interactionSource = interactionSource,
    indicationNodeFactory = indicationNodeFactory,
    enabled = enabled,
    onClickLabel = null,
    role = role,
    onClick = onClick
) {
    fun update(
        state: ToggleableState,
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        enabled: Boolean,
        role: Role?,
        onClick: () -> Unit
    ) {
        if (this.state != state) {
            this.state = state
            invalidateSemantics()
        }
        super.update(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            onClickLabel = null,
            role = role,
            onClick = onClick
        )
    }

    override fun SemanticsPropertyReceiver.applyAdditionalSemantics() {
        toggleableState = state
    }
}
