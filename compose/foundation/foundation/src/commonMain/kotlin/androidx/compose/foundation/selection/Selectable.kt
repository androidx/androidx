/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time.
 * A typical example of mutually exclusive set is a RadioGroup or a row of Tabs. To ensure
 * correct accessibility behavior, make sure to pass [Modifier.selectableGroup] modifier into the
 * RadioGroup or the row.
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this selectable modifier inside composition, consider using the other
 * overload and explicitly passing `LocalIndication.current` for improved performance. For more
 * information see the documentation on the other overload.
 *
 * @sample androidx.compose.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick callback to invoke when this item is clicked
 */
fun Modifier.selectable(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "selectable"
        properties["selected"] = selected
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
    Modifier.selectable(
        selected = selected,
        interactionSource = interactionSource,
        indication = localIndication,
        enabled = enabled,
        role = role,
        onClick = onClick
    )
}

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time.
 * A typical example of mutually exclusive set is a RadioGroup or a row of Tabs. To ensure
 * correct accessibility behavior, make sure to pass [Modifier.selectableGroup] modifier into the
 * RadioGroup or the row.
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an
 * internal [MutableInteractionSource] will be lazily created along with the [indication] only when
 * needed. This reduces the performance cost of selectable during composition, as creating the
 * [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of selectable, it is recommended to
 * instead provide `null` to enable lazy creation. If you need [indication] to be created eagerly,
 * provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside selectable.
 *
 * @sample androidx.compose.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * PressInteraction.Press when this selectable is pressed. If `null`, an internal
 * [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when the modified element is pressed. By default,
 * the indication from [LocalIndication] will be used. Set to `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick callback to invoke when this item is clicked
 */
fun Modifier.selectable(
    selected: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = clickableWithIndicationIfNeeded(
    enabled = enabled,
    interactionSource = interactionSource,
    indication = indication
) { intSource, indicationNodeFactory ->
    SelectableElement(
        selected = selected,
        interactionSource = intSource,
        indicationNodeFactory = indicationNodeFactory,
        enabled = enabled,
        role = role,
        onClick = onClick
    )
}

private class SelectableElement(
    private val selected: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val enabled: Boolean,
    private val role: Role?,
    private val onClick: () -> Unit
) : ModifierNodeElement<SelectableNode>() {
    override fun create() = SelectableNode(
        selected = selected,
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        enabled = enabled,
        role = role,
        onClick = onClick
    )

    override fun update(node: SelectableNode) {
        node.update(
            selected = selected,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "selectable"
        properties["selected"] = selected
        properties["interactionSource"] = interactionSource
        properties["indicationNodeFactory"] = indicationNodeFactory
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onClick"] = onClick
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as SelectableElement

        if (selected != other.selected) return false
        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (enabled != other.enabled) return false
        if (role != other.role) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selected.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class SelectableNode(
    private var selected: Boolean,
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
        selected: Boolean,
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        enabled: Boolean,
        role: Role?,
        onClick: () -> Unit
    ) {
        if (this.selected != selected) {
            this.selected = selected
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
        selected = this@SelectableNode.selected
    }
}
