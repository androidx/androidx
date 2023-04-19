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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.ModifierLocalScrollableContainer
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default indication from
 * [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use another
 * overload.
 *
 * If you need to support double click or long click alongside the single click, consider
 * using [combinedClickable].
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will
 * appear disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() }
    )
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show an indication
 * as specified in [indication] parameter.
 *
 * If you need to support double click or long click alongside the single click, consider
 * using [combinedClickable].
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 *
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and dispatched with [MutableInteractionSource].
 * @param indication indication to be shown when modified element is pressed. By default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will
 * appear disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    Modifier
        .indication(interactionSource, indication)
        .hoverable(enabled = enabled, interactionSource = interactionSource)
        .focusableInNonTouchMode(enabled = enabled, interactionSource = interactionSource)
        .then(ClickableElement(interactionSource, enabled, onClickLabel, role, onClick))
}
/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable]
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication],
 * use another overload.
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@ExperimentalFoundationApi
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "combinedClickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onLongClickLabel"] = onLongClickLabel
    }
) {
    Modifier.combinedClickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick,
        onDoubleClick = onDoubleClick,
        onClick = onClick,
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() }
    )
}

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable].
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and emitted with [MutableInteractionSource].
 * @param indication indication to be shown when modified element is pressed. By default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@ExperimentalFoundationApi
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "combinedClickable"
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onLongClickLabel"] = onLongClickLabel
    }
) {
    Modifier
        .indication(interactionSource, indication)
        .hoverable(enabled = enabled, interactionSource = interactionSource)
        .focusableInNonTouchMode(enabled = enabled, interactionSource = interactionSource)
        .then(CombinedClickableElement(
            interactionSource,
            enabled,
            onClickLabel,
            role,
            onClick,
            onLongClickLabel,
            onLongClick,
            onDoubleClick
        ))
}

private suspend fun PressGestureScope.handlePressInteraction(
    pressPoint: Offset,
    interactionSource: MutableInteractionSource,
    interactionData: AbstractClickableNode.InteractionData,
    delayPressInteraction: () -> Boolean
) {
    coroutineScope {
        val delayJob = launch {
            if (delayPressInteraction()) {
                delay(TapIndicationDelay)
            }
            val press = PressInteraction.Press(pressPoint)
            interactionSource.emit(press)
            interactionData.pressInteraction = press
        }
        val success = tryAwaitRelease()
        if (delayJob.isActive) {
            delayJob.cancelAndJoin()
            // The press released successfully, before the timeout duration - emit the press
            // interaction instantly. No else branch - if the press was cancelled before the
            // timeout, we don't want to emit a press interaction.
            if (success) {
                val press = PressInteraction.Press(pressPoint)
                val release = PressInteraction.Release(press)
                interactionSource.emit(press)
                interactionSource.emit(release)
            }
        } else {
            interactionData.pressInteraction?.let { pressInteraction ->
                val endInteraction = if (success) {
                    PressInteraction.Release(pressInteraction)
                } else {
                    PressInteraction.Cancel(pressInteraction)
                }
                interactionSource.emit(endInteraction)
            }
        }
        interactionData.pressInteraction = null
    }
}

/**
 * How long to wait before appearing 'pressed' (emitting [PressInteraction.Press]) - if a touch
 * down will quickly become a drag / scroll, this timeout means that we don't show a press effect.
 */
internal expect val TapIndicationDelay: Long

/**
 * Returns whether the root Compose layout node is hosted in a scrollable container outside of
 * Compose. On Android this will be whether the root View is in a scrollable ViewGroup, as even if
 * nothing in the Compose part of the hierarchy is scrollable, if the View itself is in a scrollable
 * container, we still want to delay presses in case presses in Compose convert to a scroll outside
 * of Compose.
 *
 * Combine this with [ModifierLocalScrollableContainer], which returns whether a [Modifier] is
 * within a scrollable Compose layout, to calculate whether this modifier is within some form of
 * scrollable container, and hence should delay presses.
 */
internal expect fun CompositionLocalConsumerModifierNode
    .isComposeRootInScrollableContainer(): Boolean

/**
 * Whether the specified [KeyEvent] should trigger a press for a clickable component.
 */
internal expect val KeyEvent.isPress: Boolean

/**
 * Whether the specified [KeyEvent] should trigger a click for a clickable component.
 */
internal expect val KeyEvent.isClick: Boolean

internal fun Modifier.genericClickableWithoutGesture(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    indicationScope: CoroutineScope,
    currentKeyPressInteractions: MutableMap<Key, PressInteraction.Press>,
    keyClickOffset: State<Offset>,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    fun Modifier.detectPressAndClickFromKey() = this.onKeyEvent { keyEvent ->
        when {
            enabled && keyEvent.isPress -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                if (!currentKeyPressInteractions.containsKey(keyEvent.key)) {
                    val press = PressInteraction.Press(keyClickOffset.value)
                    currentKeyPressInteractions[keyEvent.key] = press
                    indicationScope.launch { interactionSource.emit(press) }
                    true
                } else {
                    false
                }
            }
            enabled && keyEvent.isClick -> {
                currentKeyPressInteractions.remove(keyEvent.key)?.let {
                    indicationScope.launch {
                        interactionSource.emit(PressInteraction.Release(it))
                    }
                }
                onClick()
                true
            }
            else -> false
        }
    }
    return this then
        ClickableSemanticsElement(
            enabled = enabled,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
            .detectPressAndClickFromKey()
            .indication(interactionSource, indication)
            .hoverable(enabled = enabled, interactionSource = interactionSource)
            .focusableInNonTouchMode(enabled = enabled, interactionSource = interactionSource)
}

private class ClickableElement(
    private val interactionSource: MutableInteractionSource,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role? = null,
    private val onClick: () -> Unit
) : ModifierNodeElement<ClickableNode>() {
    override fun create() = ClickableNode(
        interactionSource,
        enabled,
        onClickLabel,
        role,
        onClick
    )

    override fun update(node: ClickableNode) = node.also {
        it.update(interactionSource, enabled, onClickLabel, role, onClick)
    }

    // Defined in the factory functions with inspectable
    override fun InspectorInfo.inspectableProperties() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClickableElement

        if (interactionSource != other.interactionSource) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interactionSource.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class CombinedClickableElement(
    private val interactionSource: MutableInteractionSource,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role? = null,
    private val onClick: () -> Unit,
    private val onLongClickLabel: String?,
    private val onLongClick: (() -> Unit)?,
    private val onDoubleClick: (() -> Unit)?
) : ModifierNodeElement<CombinedClickableNode>() {
    override fun create() = CombinedClickableNode(
        interactionSource,
        enabled,
        onClickLabel,
        role,
        onClick,
        onLongClickLabel,
        onLongClick,
        onDoubleClick
    )

    override fun update(node: CombinedClickableNode) = node.also {
        it.update(
            interactionSource,
            enabled,
            onClickLabel,
            role,
            onClick,
            onLongClickLabel,
            onLongClick,
            onDoubleClick
        )
    }

    // Defined in the factory functions with inspectable
    override fun InspectorInfo.inspectableProperties() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CombinedClickableElement

        if (interactionSource != other.interactionSource) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick != other.onClick) return false
        if (onLongClickLabel != other.onLongClickLabel) return false
        if (onLongClick != other.onLongClick) return false
        if (onDoubleClick != other.onDoubleClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interactionSource.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        result = 31 * result + (onLongClickLabel?.hashCode() ?: 0)
        result = 31 * result + (onLongClick?.hashCode() ?: 0)
        result = 31 * result + (onDoubleClick?.hashCode() ?: 0)
        return result
    }
}

private class ClickableNode(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit
) : AbstractClickableNode(interactionSource, enabled, onClickLabel, role, onClick) {
    override val clickableSemanticsNode = delegated {
        ClickableSemanticsNode(
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
            onLongClick = null,
            onLongClickLabel = null
        )
    }

    override val clickablePointerInputNode = delegated {
        ClickablePointerInputNode(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            interactionData = interactionData
        )
    }

    fun update(
        interactionSource: MutableInteractionSource,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit
    ) {
        updateCommon(interactionSource, enabled, onClickLabel, role, onClick)
        clickableSemanticsNode.update(
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
            onLongClickLabel = null,
            onLongClick = null
        )
        clickablePointerInputNode.update(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }
}

private class CombinedClickableNode(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit,
    onLongClickLabel: String?,
    private var onLongClick: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?
) : AbstractClickableNode(interactionSource, enabled, onClickLabel, role, onClick) {
    override val clickableSemanticsNode = delegated {
        ClickableSemanticsNode(
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick
        )
    }

    override val clickablePointerInputNode = delegated {
        CombinedClickablePointerInputNode(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            interactionData = interactionData,
            onLongClick,
            onDoubleClick
        )
    }

    fun update(
        interactionSource: MutableInteractionSource,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
        onLongClickLabel: String?,
        onLongClick: (() -> Unit)?,
        onDoubleClick: (() -> Unit)?
    ) {
        // If we have gone from no long click to having a long click or vice versa,
        // cancel any existing press interactions.
        if ((this.onLongClick == null) != (onLongClick == null)) {
            disposeInteractionSource()
        }
        this.onLongClick = onLongClick
        updateCommon(interactionSource, enabled, onClickLabel, role, onClick)
        clickableSemanticsNode.update(
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick
        )
        clickablePointerInputNode.update(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick
        )
    }
}

private sealed class AbstractClickableNode(
    private var interactionSource: MutableInteractionSource,
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var onClick: () -> Unit
) : DelegatingNode(), SemanticsModifierNode, PointerInputModifierNode, KeyInputModifierNode {
    abstract val clickablePointerInputNode: AbstractClickablePointerInputNode
    abstract val clickableSemanticsNode: ClickableSemanticsNode

    class InteractionData {
        val currentKeyPressInteractions = mutableMapOf<Key, PressInteraction.Press>()
        var pressInteraction: PressInteraction.Press? = null
        var centreOffset: Offset = Offset.Zero
    }

    protected val interactionData = InteractionData()

    protected fun updateCommon(
        interactionSource: MutableInteractionSource,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role? = null,
        onClick: () -> Unit
    ) {
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }
        if (this.enabled != enabled) {
            if (!enabled) {
                disposeInteractionSource()
            }
            this.enabled = enabled
        }
        this.onClickLabel = onClickLabel
        this.role = role
        this.onClick = onClick
    }

    override fun onDetach() {
        disposeInteractionSource()
    }

    protected fun disposeInteractionSource() {
        interactionData.pressInteraction?.let { oldValue ->
            val interaction = PressInteraction.Cancel(oldValue)
            interactionSource.tryEmit(interaction)
        }
        interactionData.currentKeyPressInteractions.values.forEach {
            interactionSource.tryEmit(PressInteraction.Cancel(it))
        }
        interactionData.pressInteraction = null
        interactionData.currentKeyPressInteractions.clear()
    }

    override val semanticsConfiguration: SemanticsConfiguration
        get() = clickableSemanticsNode.semanticsConfiguration

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        clickablePointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        clickablePointerInputNode.onCancelPointerInput()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return when {
            enabled && event.isPress -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                if (!interactionData.currentKeyPressInteractions.containsKey(event.key)) {
                    val press = PressInteraction.Press(interactionData.centreOffset)
                    interactionData.currentKeyPressInteractions[event.key] = press
                    coroutineScope.launch { interactionSource.emit(press) }
                    true
                } else {
                    false
                }
            }
            enabled && event.isClick -> {
                interactionData.currentKeyPressInteractions.remove(event.key)?.let {
                    coroutineScope.launch {
                        interactionSource.emit(PressInteraction.Release(it))
                    }
                }
                onClick()
                true
            }
            else -> false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false
}

private class ClickableSemanticsElement(
    private val enabled: Boolean,
    private val role: Role?,
    private val onLongClickLabel: String?,
    private val onLongClick: (() -> Unit)?,
    private val onClickLabel: String?,
    private val onClick: () -> Unit
) : ModifierNodeElement<ClickableSemanticsNode>() {
    override fun create() = ClickableSemanticsNode(
        enabled = enabled,
        role = role,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick,
        onClickLabel = onClickLabel,
        onClick = onClick
    )

    override fun update(node: ClickableSemanticsNode) = node.also {
        it.update(enabled, onClickLabel, role, onClick, onLongClickLabel, onLongClick)
    }

    override fun InspectorInfo.inspectableProperties() = Unit

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + onLongClickLabel.hashCode()
        result = 31 * result + onLongClick.hashCode()
        result = 31 * result + onClickLabel.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClickableSemanticsElement) return false

        if (enabled != other.enabled) return false
        if (role != other.role) return false
        if (onLongClickLabel != other.onLongClickLabel) return false
        if (onLongClick != other.onLongClick) return false
        if (onClickLabel != other.onClickLabel) return false
        if (onClick != other.onClick) return false

        return true
    }
}

private class ClickableSemanticsNode(
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var onClick: () -> Unit,
    private var onLongClickLabel: String?,
    private var onLongClick: (() -> Unit)?,
) : SemanticsModifierNode, Modifier.Node() {
    fun update(
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
        onLongClickLabel: String?,
        onLongClick: (() -> Unit)?,
    ) {
        this.enabled = enabled
        this.onClickLabel = onClickLabel
        this.role = role
        this.onClick = onClick
        this.onLongClickLabel = onLongClickLabel
        this.onLongClick = onLongClick
    }

    override val semanticsConfiguration
        get() = SemanticsConfiguration().apply {
            isMergingSemanticsOfDescendants = true
            if (this@ClickableSemanticsNode.role != null) {
                role = this@ClickableSemanticsNode.role!!
            }
            onClick(
                action = { onClick(); true },
                label = onClickLabel
            )
            if (onLongClick != null) {
                onLongClick(
                    action = { onLongClick?.invoke(); true },
                    label = onLongClickLabel
                )
            }
            if (!enabled) {
                disabled()
            }
        }
}

private sealed class AbstractClickablePointerInputNode(
    protected var enabled: Boolean,
    protected var interactionSource: MutableInteractionSource?,
    protected var onClick: () -> Unit,
    protected val interactionData: AbstractClickableNode.InteractionData
) : DelegatingNode(), ModifierLocalNode, CompositionLocalConsumerModifierNode,
    PointerInputModifierNode {

    private val delayPressInteraction = {
        ModifierLocalScrollableContainer.current || isComposeRootInScrollableContainer()
    }

    private val pointerInputNode = SuspendingPointerInputModifierNode { pointerInput() }
        // TODO: remove `.node` after aosp/2462416 lands and merge everything into one delegated
        //  block
        .also { delegated { it.node } }

    protected abstract suspend fun PointerInputScope.pointerInput()

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }

    protected suspend fun PressGestureScope.handlePressInteraction(offset: Offset) {
        interactionSource?.let { interactionSource ->
            handlePressInteraction(
                offset,
                interactionSource,
                interactionData,
                delayPressInteraction
            )
        }
    }

    protected fun resetPointerInputHandler() = pointerInputNode.resetPointerInputHandler()
}

private class ClickablePointerInputNode(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    interactionData: AbstractClickableNode.InteractionData
) : AbstractClickablePointerInputNode(
    enabled,
    interactionSource,
    onClick,
    interactionData
) {
    override suspend fun PointerInputScope.pointerInput() {
        interactionData.centreOffset = size.center.toOffset()
        detectTapAndPress(
            onPress = { offset ->
                if (enabled) {
                    handlePressInteraction(offset)
                }
            },
            onTap = { if (enabled) onClick() }
        )
    }

    fun update(
        enabled: Boolean,
        interactionSource: MutableInteractionSource,
        onClick: () -> Unit,
    ) {
        // These are captured inside callbacks, not as an input to detectTapGestures,
        // so no need need to reset pointer input handling
        this.enabled = enabled
        this.onClick = onClick
        this.interactionSource = interactionSource
    }
}

private class CombinedClickablePointerInputNode(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    interactionData: AbstractClickableNode.InteractionData,
    private var onLongClick: (() -> Unit)?,
    private var onDoubleClick: (() -> Unit)?
) : AbstractClickablePointerInputNode(
    enabled,
    interactionSource,
    onClick,
    interactionData
) {
    override suspend fun PointerInputScope.pointerInput() {
        interactionData.centreOffset = size.center.toOffset()
        detectTapGestures(
            onDoubleTap = if (enabled && onDoubleClick != null) {
                { onDoubleClick?.invoke() }
            } else null,
            onLongPress = if (enabled && onLongClick != null) {
                { onLongClick?.invoke() }
            } else null,
            onPress = { offset ->
                if (enabled) {
                    handlePressInteraction(offset)
                }
            },
            onTap = { if (enabled) onClick() }
        )
    }

    fun update(
        enabled: Boolean,
        interactionSource: MutableInteractionSource,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?,
        onDoubleClick: (() -> Unit)?
    ) {
        // These are captured inside callbacks, not as an input to detectTapGestures,
        // so no need need to reset pointer input handling
        this.onClick = onClick
        this.interactionSource = interactionSource

        var changed = false

        // This is captured as a parameter to detectTapGestures, so we need to restart detecting
        // gestures if it changes.
        if (this.enabled != enabled) {
            this.enabled = enabled
            changed = true
        }

        // We capture these inside the callback, so if the lambda changes value we don't want to
        // reset input handling - only reset if they go from not-defined to defined, and vice-versa,
        // as that is what is captured in the parameter to detectTapGestures.
        if ((this.onLongClick == null) != (onLongClick == null)) {
            changed = true
        }
        this.onLongClick = onLongClick
        if ((this.onDoubleClick == null) != (onDoubleClick == null)) {
            changed = true
        }
        this.onDoubleClick = onDoubleClick
        if (changed) resetPointerInputHandler()
    }
}
