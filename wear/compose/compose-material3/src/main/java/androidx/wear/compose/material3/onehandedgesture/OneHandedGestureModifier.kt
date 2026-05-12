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

package androidx.wear.compose.material3.onehandedgesture

import android.view.View
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.wear.compose.foundation.LocalScreenIsActive
import kotlin.String

/**
 * Registers a gesture handler.
 *
 * **Visibility Management:** This gesture handler is active as long as the Modifier is part of the
 * composition. On its own, it does not track whether the composable is visible or clipped (e.g., in
 * a Lazy layout).
 *
 * To prevent accidental triggers from off-screen items, developers should apply this modifier
 * conditionally. For many cases, [androidx.compose.ui.layout.onVisibilityChanged] Modifier can be
 * used to determine the visibility of a composable.
 *
 * Example usage in a list:
 * ```kotlin
 * var isVisible by remember { mutableStateOf(false) }
 * val gestureModifier = remember(isVisible) {
 *   if (isVisible) Modifier.oneHandedGesture() else Modifier
 * }
 *
 * Box(
 *   modifier = Modifier
 *     .onVisibilityChanged { isVisible = it }
 *     .then(gestureModifier)
 * ) {
 *   ...
 * }
 * ```
 *
 * **Haptics:** When a gesture is successfully triggered, the system automatically performs haptic
 * feedback to acknowledge the interaction; developers do not need to trigger haptics manually
 * within [onGesture].
 *
 * Example of adding one-handed gesture handler to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 * @param action The gesture action to handle.
 * @param priority The priority value; higher values take precedence if multiple handlers are
 *   registered for the same [action]. It is not recommended to register multiple gestures for the
 *   same action and priority (but if that is the case, all of them will be actioned).
 * @param enabledInAmbient Whether the gesture should remain active in ambient mode.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [androidx.compose.foundation.interaction.Interaction]s for this gesture. This can be used to
 *   visualize the gesture state (e.g., showing a ripple or custom pressed state) when the
 *   one-handed gesture is being interacted with.
 * @param onShowIndicator Callback invoked when the system determines a gesture indicator should be
 *   displayed for this component. This occurs when the component holds the highest priority for the
 *   current gesture. Only [GestureAction.Primary] gesture indicator callbacks will be called.
 * @param onGesture The callback invoked when the gesture is triggered.
 */
@Composable
public fun Modifier.oneHandedGesture(
    action: GestureAction,
    priority: GesturePriority = GesturePriority.Unspecified,
    enabledInAmbient: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    onShowIndicator: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier {
    val key = currentCompositeKeyHashCode.toString(MaxSupportedRadix)
    return then(
        Modifier.oneHandedGesture(
            action = action,
            key = key,
            priority = priority,
            enabledInAmbient = enabledInAmbient,
            interactionSource = interactionSource,
            onShowIndicator = onShowIndicator,
            onGesture = onGesture,
        )
    )
}

/**
 * Registers a gesture handler.
 *
 * **Visibility Management:** This gesture handler is active as long as the Modifier is part of the
 * composition. On its own, it does not track whether the composable is visible or clipped (e.g., in
 * a Lazy layout).
 *
 * To prevent accidental triggers from off-screen items, developers should apply this modifier
 * conditionally. For many cases, [androidx.compose.ui.layout.onVisibilityChanged] Modifier can be
 * used to determine the visibility of a composable.
 *
 * Example usage in a list:
 * ```kotlin
 * var isVisible by remember { mutableStateOf(false) }
 * val gestureModifier = remember(isVisible) {
 *   if (isVisible) Modifier.oneHandedGesture() else Modifier
 * }
 *
 * Box(
 *   modifier = Modifier
 *     .onVisibilityChanged { isVisible = it }
 *     .then(gestureModifier)
 * ) {
 *   ...
 * }
 * ```
 *
 * **Haptics:** When a gesture is successfully triggered, the system automatically performs haptic
 * feedback to acknowledge the interaction; developers do not need to trigger haptics manually
 * within [onGesture].
 *
 * Example of adding one-handed gesture handler to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 * @param action The gesture action to handle.
 * @param key A unique identifier for this gesture instance. This ID allows the system to track user
 *   interactions - for example, to mute gesture indicators that have been frequently shown or
 *   successfully performed, in accordance with user preferences. If the same key is reused across
 *   multiple gestures, they will share a common interaction history (such as frequency-based
 *   gesture indicator display logic). Note that this only affects the presentation of the UI; the
 *   underlying logic and handling remain independent for each instance.
 * @param priority The priority value; higher values take precedence if multiple handlers are
 *   registered for the same [action]. It is not recommended to register multiple gestures for the
 *   same action and priority (but if that is the case, all of them will be actioned).
 * @param enabledInAmbient Whether the gesture should remain active in ambient mode.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [androidx.compose.foundation.interaction.Interaction]s for this gesture. This can be used to
 *   visualize the gesture state (e.g., showing a ripple or custom pressed state) when the
 *   one-handed gesture is being interacted with.
 * @param onShowIndicator Callback invoked when the system determines a gesture indicator should be
 *   displayed for this component. This occurs when the component holds the highest priority for the
 *   current gesture. Only [GestureAction.Primary] gesture indicator callbacks will be called.
 * @param onGesture The callback invoked when the gesture is triggered.
 */
public fun Modifier.oneHandedGesture(
    action: GestureAction,
    key: String,
    priority: GesturePriority = GesturePriority.Unspecified,
    enabledInAmbient: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    onShowIndicator: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier {
    return then(
        GestureElement(
            GestureConfig(
                action = action,
                key = key,
                priority = priority.value,
                enabledInAmbient = enabledInAmbient,
                interactionSource = interactionSource,
                onShowIndicator = onShowIndicator,
                onGesture = onGesture,
            )
        )
    )
}

private class GestureElement(val config: GestureConfig) : ModifierNodeElement<GestureNode>() {

    override fun create() = GestureNode(config)

    override fun update(node: GestureNode) {
        node.updateGesture(config)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GestureElement"
        properties["type"] = config.action
        properties["priority"] = config.priority
        properties["key"] = config.key
        properties["enabledInAmbient"] = config.enabledInAmbient
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GestureElement) return false
        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }
}

private class GestureNode(var config: GestureConfig) :
    Modifier.Node(), CompositionLocalConsumerModifierNode, ObserverModifierNode {

    private var gestureManager: GestureManager? = null
    private var localScreenIsActive = false
    private var currentView: View? = null
    private var hapticFeedback: HapticFeedback? = null
    private var size: IntSize = IntSize.Zero

    override fun onAttach() {
        updateCompositionLocals(false)
        registerGesture(gestureManager, currentView!!, hapticFeedback!!, config)
    }

    override fun onObservedReadsChanged() = updateCompositionLocals(true)

    override fun onDetach() {
        unregisterGesture(gestureManager, currentView!!, config)
        gestureManager = null
        localScreenIsActive = false
        currentView = null
        hapticFeedback = null
    }

    fun updateGesture(newConfig: GestureConfig) {
        val oldConfig = config
        val oldGestureManager = gestureManager
        /* Update local compositions here to handle node reparenting. onAttach is not sufficient as
         * it may trigger before the node is fully settled in its new composition context. Manually
         * syncing ensures we capture the correct providers after the tree has stabilized. */
        updateCompositionLocals(false)

        if (oldGestureManager == gestureManager) {
            if (isAttached) {
                gestureManager?.updateGesture(currentView!!, oldConfig, newConfig)
            }
        } else {
            unregisterGesture(oldGestureManager, currentView!!, oldConfig)
            if (isAttached) {
                registerGesture(gestureManager, currentView!!, hapticFeedback!!, newConfig)
            }
        }
        config = newConfig
    }

    private fun updateCompositionLocals(reregister: Boolean) = observeReads {
        localScreenIsActive = currentValueOf(LocalScreenIsActive)
        currentView = currentValueOf(LocalView)
        hapticFeedback = currentValueOf(LocalHapticFeedback)
        val newGestureManager = currentValueOf(LocalGestureManager)
        if (reregister) {
            unregisterGesture(gestureManager, currentView!!, config)
            registerGesture(newGestureManager, currentView!!, hapticFeedback!!, config)
        }
        gestureManager = newGestureManager
    }

    private fun registerGesture(
        manager: GestureManager?,
        view: View,
        haptic: HapticFeedback,
        gesture: GestureConfig,
    ) {
        manager?.registerGesture(
            view = view,
            haptic = haptic,
            gesture = gesture,
            isActive = { localScreenIsActive },
            size = { size },
        )
    }

    private fun unregisterGesture(manager: GestureManager?, view: View, gesture: GestureConfig) {
        manager?.unregisterGesture(view, gesture)
    }
}

/** The maximum radix available for conversion to and from strings. */
private const val MaxSupportedRadix = 36
