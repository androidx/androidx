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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
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
) = composed(
    factory = {
        val pressInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
        val currentKeyPressInteractions = remember { mutableMapOf<Key, PressInteraction.Press>() }
        val centreOffset = remember { mutableStateOf(Offset.Zero) }

        val interactionModifier = if (enabled) {
            ClickableInteractionElement(
                interactionSource,
                pressInteraction,
                currentKeyPressInteractions
            )
        } else Modifier

        val pointerInputModifier = ClickablePointerInputElement(
            enabled,
            interactionSource,
            onClick,
            centreOffset,
            pressInteraction
        )

        Modifier
            .genericClickableWithoutGesture(
                interactionSource = interactionSource,
                indication = indication,
                indicationScope = rememberCoroutineScope(),
                currentKeyPressInteractions = currentKeyPressInteractions,
                keyClickOffset = centreOffset,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClickLabel = null,
                onLongClick = null,
                onClick = onClick
            )
            .then(pointerInputModifier)
            .then(interactionModifier)
    },
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
)

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
) = composed(
    factory = {
        val hasLongClick = onLongClick != null
        val pressInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
        val currentKeyPressInteractions = remember { mutableMapOf<Key, PressInteraction.Press>() }
        if (enabled) {
            // Handles the case where a long click causes a null onLongClick lambda to be passed,
            // so we can cancel the existing press.
            DisposableEffect(hasLongClick) {
                onDispose {
                    pressInteraction.value?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        pressInteraction.value = null
                    }
                }
            }
        }
        val centreOffset = remember { mutableStateOf(Offset.Zero) }
        val interactionModifier = if (enabled) {
            ClickableInteractionElement(
                interactionSource,
                pressInteraction,
                currentKeyPressInteractions
            )
        } else Modifier

        val pointerInputModifier = CombinedClickablePointerInputElement(
            enabled,
            interactionSource,
            onClick,
            centreOffset,
            pressInteraction,
            onLongClick,
            onDoubleClick
        )

        Modifier
            .genericClickableWithoutGesture(
                interactionSource = interactionSource,
                indication = indication,
                indicationScope = rememberCoroutineScope(),
                currentKeyPressInteractions = currentKeyPressInteractions,
                keyClickOffset = centreOffset,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .then(pointerInputModifier)
            .then(interactionModifier)
    },
    inspectorInfo = debugInspectorInfo {
        name = "combinedClickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onLongClickLabel"] = onLongClickLabel
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
)

internal suspend fun PressGestureScope.handlePressInteraction(
    pressPoint: Offset,
    interactionSource: MutableInteractionSource,
    pressInteraction: MutableState<PressInteraction.Press?>,
    delayPressInteraction: () -> Boolean
) {
    coroutineScope {
        val delayJob = launch {
            if (delayPressInteraction()) {
                delay(TapIndicationDelay)
            }
            val press = PressInteraction.Press(pressPoint)
            interactionSource.emit(press)
            pressInteraction.value = press
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
            pressInteraction.value?.let { pressInteraction ->
                val endInteraction = if (success) {
                    PressInteraction.Release(pressInteraction)
                } else {
                    PressInteraction.Cancel(pressInteraction)
                }
                interactionSource.emit(endInteraction)
            }
        }
        pressInteraction.value = null
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
        it.enabled = enabled
        it.role = role
        it.onLongClickLabel = onLongClickLabel
        it.onLongClick = onLongClick
        it.onClickLabel = onClickLabel
        it.onClick = onClick
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
    var enabled: Boolean,
    var role: Role?,
    var onLongClickLabel: String?,
    var onLongClick: (() -> Unit)?,
    var onClickLabel: String?,
    var onClick: () -> Unit,
) : SemanticsModifierNode, Modifier.Node() {
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

// Only interactionSource should ever change - the rest must be remembered with no keys.
private class ClickableInteractionElement(
    private val interactionSource: MutableInteractionSource,
    private val pressInteraction: MutableState<PressInteraction.Press?>,
    private val currentKeyPressInteractions: MutableMap<Key, PressInteraction.Press>
) : ModifierNodeElement<ClickableInteractionNode>() {
    override fun create(): ClickableInteractionNode = ClickableInteractionNode(
        interactionSource,
        pressInteraction,
        currentKeyPressInteractions
    )

    override fun update(node: ClickableInteractionNode) = node.also {
        it.updateInteractionSource(interactionSource)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClickableInteractionElement) return false

        if (interactionSource != other.interactionSource) return false

        return true
    }

    override fun hashCode(): Int {
        return interactionSource.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() = Unit
}

private class ClickableInteractionNode(
    private var interactionSource: MutableInteractionSource,
    private val pressInteraction: MutableState<PressInteraction.Press?>,
    private val currentKeyPressInteractions: MutableMap<Key, PressInteraction.Press>
) : Modifier.Node() {
    fun updateInteractionSource(interactionSource: MutableInteractionSource) {
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }
    }

    override fun onDetach() {
        disposeInteractionSource()
    }

    private fun disposeInteractionSource() {
        pressInteraction.value?.let { oldValue ->
            val interaction = PressInteraction.Cancel(oldValue)
            interactionSource.tryEmit(interaction)
        }
        currentKeyPressInteractions.values.forEach {
            interactionSource.tryEmit(PressInteraction.Cancel(it))
        }
        pressInteraction.value = null
        currentKeyPressInteractions.clear()
    }
}

private class ClickablePointerInputElement(
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource,
    private val onClick: () -> Unit,
    private val centreOffset: MutableState<Offset>,
    private val pressInteraction: MutableState<PressInteraction.Press?>
) : ModifierNodeElement<ClickablePointerInputNode>() {
    override fun create(): ClickablePointerInputNode = ClickablePointerInputNode(
        enabled,
        interactionSource,
        onClick,
        centreOffset,
        pressInteraction
    )

    override fun update(node: ClickablePointerInputNode) = node.also {
        it.updateParameters(enabled, interactionSource, onClick)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClickablePointerInputElement) return false

        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() = Unit

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class CombinedClickablePointerInputElement(
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource,
    private val onClick: () -> Unit,
    private val centreOffset: MutableState<Offset>,
    private val pressInteraction: MutableState<PressInteraction.Press?>,
    private val onLongClick: (() -> Unit)?,
    private val onDoubleClick: (() -> Unit)?
) : ModifierNodeElement<CombinedClickablePointerInputNode>() {
    override fun create(): CombinedClickablePointerInputNode = CombinedClickablePointerInputNode(
        enabled,
        interactionSource,
        onClick,
        centreOffset,
        pressInteraction,
        onLongClick,
        onDoubleClick
    )

    override fun update(node: CombinedClickablePointerInputNode) = node.also {
        it.updateParameters(enabled, interactionSource, onClick, onLongClick, onDoubleClick)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CombinedClickablePointerInputElement) return false

        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (onClick != other.onClick) return false
        if (onLongClick != other.onLongClick) return false
        if (onDoubleClick != other.onDoubleClick) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() = Unit

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + onClick.hashCode()
        result = 31 * result + (onLongClick?.hashCode() ?: 0)
        result = 31 * result + (onDoubleClick?.hashCode() ?: 0)
        return result
    }
}

private sealed class AbstractClickablePointerInputNode(
    protected var enabled: Boolean,
    protected var interactionSource: MutableInteractionSource?,
    protected var onClick: () -> Unit,
    protected val centreOffset: MutableState<Offset>,
    private val pressInteraction: MutableState<PressInteraction.Press?>,
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
                pressInteraction,
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
    centreOffset: MutableState<Offset>,
    pressInteraction: MutableState<PressInteraction.Press?>
) : AbstractClickablePointerInputNode(
    enabled,
    interactionSource,
    onClick,
    centreOffset,
    pressInteraction
) {
    override suspend fun PointerInputScope.pointerInput() {
        centreOffset.value = size.center.toOffset()
        detectTapAndPress(
            onPress = { offset ->
                if (enabled) {
                    handlePressInteraction(offset)
                }
            },
            onTap = { if (enabled) onClick() }
        )
    }

    fun updateParameters(
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
    centreOffset: MutableState<Offset>,
    pressInteraction: MutableState<PressInteraction.Press?>,
    private var onLongClick: (() -> Unit)?,
    private var onDoubleClick: (() -> Unit)?
) : AbstractClickablePointerInputNode(
    enabled,
    interactionSource,
    onClick,
    centreOffset,
    pressInteraction
) {
    override suspend fun PointerInputScope.pointerInput() {
        centreOffset.value = size.center.toOffset()
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

    fun updateParameters(
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
