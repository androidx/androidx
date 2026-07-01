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
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.wear.compose.foundation.LocalScreenIsActive

/**
 * Registers a gesture handler.
 *
 * Note: Gesture recognition can be explicitly disabled across a component hierarchy by providing
 * false` to [LocalOneHandedGestureEnabled].
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
 * @param gestureConfiguration The [OneHandedGestureConfiguration] containing the configuration for
 *   this gesture.
 * @param enabledInAmbient Whether the gesture should remain active in ambient mode.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [androidx.compose.foundation.interaction.Interaction]s for this gesture. This can be used to
 *   visualize the gesture state (e.g., showing a ripple, custom pressed state) when the one-handed
 *   gesture is being interacted with.
 * @param gestureLabel Semantic label used by accessibility services to describe the purpose of this
 *   gesture. This is highly recommended for ensuring that users with screen readers understand what
 *   action will be performed.
 * @param onGestureAvailable A callback invoked by the system to signal that this gesture is
 *   currently available as a high-priority action. Developers should use this callback to set
 *   [OneHandedGestureIndicatorState.isIndicatorActive] to `true` to trigger the associated visual
 *   feedback.
 * @param onGesture The callback invoked when the gesture is triggered.
 */
public fun Modifier.oneHandedGesture(
    gestureConfiguration: OneHandedGestureConfiguration,
    enabledInAmbient: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    gestureLabel: String? = null,
    onGestureAvailable: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier {
    return then(
        GestureElement(
            gestureConfiguration = gestureConfiguration,
            enabledInAmbient = enabledInAmbient,
            gestureLabel = gestureLabel,
            onGestureAvailable = onGestureAvailable,
            onGesture = { centerOffset ->
                interactionSource?.let { source ->
                    val press = PressInteraction.Press(centerOffset)
                    source.emit(press)
                    source.emit(PressInteraction.Release(press))
                }
                onGesture()
            },
        )
    )
}

private class GestureElement(
    val gestureConfiguration: OneHandedGestureConfiguration,
    val enabledInAmbient: Boolean,
    val gestureLabel: String?,
    val onGestureAvailable: () -> Unit,
    val onGesture: suspend (centerOffset: Offset) -> Unit,
) : ModifierNodeElement<GestureNode>() {

    override fun create() =
        GestureNode(
            gestureConfiguration,
            enabledInAmbient,
            gestureLabel,
            onGestureAvailable,
            onGesture,
        )

    override fun update(node: GestureNode) {
        node.updateGesture(
            gestureConfiguration,
            enabledInAmbient,
            gestureLabel,
            onGestureAvailable,
            onGesture,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GestureElement"
        properties["action"] = gestureConfiguration.action
        properties["priority"] = gestureConfiguration.priority
        properties["key"] = gestureConfiguration.key
        properties["enabledInAmbient"] = enabledInAmbient
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GestureElement) return false
        return gestureConfiguration == other.gestureConfiguration &&
            gestureLabel == other.gestureLabel &&
            onGesture === other.onGesture &&
            onGestureAvailable === other.onGestureAvailable
    }

    override fun hashCode(): Int {
        var result = gestureConfiguration.hashCode()
        result = 31 * result + enabledInAmbient.hashCode()
        result = 31 * result + gestureLabel.hashCode()
        result = 31 * result + onGesture.hashCode()
        result = 31 * result + onGestureAvailable.hashCode()
        return result
    }
}

private class GestureNode(
    private var gestureConfiguration: OneHandedGestureConfiguration,
    private var enabledInAmbient: Boolean,
    private var gestureLabel: String?,
    private var onGestureAvailable: () -> Unit,
    private var onGesture: suspend (centerOffset: Offset) -> Unit,
) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    LayoutAwareModifierNode {

    private var gestureManager: GestureManager? = null
    private var localScreenIsActive = false
    private var currentView: View? = null
    private var hapticFeedback: HapticFeedback? = null
    private var size: IntSize = IntSize.Zero
    private var isEnabled = true

    override fun onAttach() {
        updateCompositionLocals(true)
    }

    override fun onObservedReadsChanged() = updateCompositionLocals(true)

    override fun onDetach() {
        unregisterGesture(gestureManager, currentView!!, gestureConfiguration)
        gestureManager = null
        localScreenIsActive = false
        currentView = null
        hapticFeedback = null
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        size = coordinates.size
    }

    fun updateGesture(
        newConfig: OneHandedGestureConfiguration,
        newEnabledInAmbient: Boolean,
        newGestureLabel: String?,
        newOnGestureAvailable: () -> Unit,
        newOnGesture: suspend (centerOffset: Offset) -> Unit,
    ) {
        val oldConfig = gestureConfiguration
        val oldGestureManager = gestureManager
        val wasEnabled = isEnabled
        /* Update local compositions here to handle node reparenting. onAttach is not sufficient as
         * it may trigger before the node is fully settled in its new composition context. Manually
         * syncing ensures we capture the correct providers after the tree has stabilized. */
        updateCompositionLocals(false)

        val managerChanged = oldGestureManager != gestureManager
        if (!isEnabled || managerChanged) {
            unregisterGesture(oldGestureManager, currentView!!, oldConfig)
        }

        if (isEnabled && isAttached) {
            if (managerChanged || !wasEnabled) {
                registerGesture(
                    gestureManager,
                    currentView!!,
                    hapticFeedback!!,
                    gestureConfiguration,
                    newEnabledInAmbient,
                    gestureLabel,
                    onGestureAvailable,
                    onGesture,
                )
            } else {
                gestureManager?.updateGesture(
                    currentView!!,
                    oldConfig,
                    newConfig,
                    newEnabledInAmbient,
                    newGestureLabel,
                    newOnGestureAvailable,
                    newOnGesture,
                )
            }
        }

        gestureConfiguration = newConfig
        gestureLabel = newGestureLabel
        onGesture = newOnGesture
    }

    private fun updateCompositionLocals(reregister: Boolean) = observeReads {
        localScreenIsActive = currentValueOf(LocalScreenIsActive)
        currentView = currentValueOf(LocalView)
        hapticFeedback = currentValueOf(LocalHapticFeedback)
        isEnabled = currentValueOf(LocalOneHandedGestureEnabled)
        val newGestureManager = currentValueOf(LocalGestureManager)
        if (reregister) {
            unregisterGesture(gestureManager, currentView!!, gestureConfiguration)
            registerGesture(
                newGestureManager,
                currentView!!,
                hapticFeedback!!,
                gestureConfiguration,
                enabledInAmbient,
                gestureLabel,
                onGestureAvailable,
                onGesture,
            )
        }
        gestureManager = newGestureManager
    }

    private fun registerGesture(
        manager: GestureManager?,
        view: View,
        haptic: HapticFeedback,
        gestureConfiguration: OneHandedGestureConfiguration,
        enabledInAmbient: Boolean,
        gestureLabel: String?,
        onGestureAvailable: () -> Unit,
        onGesture: suspend (centerOffset: Offset) -> Unit,
    ) {
        if (isEnabled) {
            manager?.registerGesture(
                view = view,
                haptic = haptic,
                gestureConfiguration = gestureConfiguration,
                enabledInAmbient = enabledInAmbient,
                gestureLabel = gestureLabel,
                onGestureAvailable = onGestureAvailable,
                onGesture = onGesture,
                isActive = { localScreenIsActive },
                size = { size },
            )
        }
    }

    private fun unregisterGesture(
        manager: GestureManager?,
        view: View,
        gestureConfiguration: OneHandedGestureConfiguration,
    ) {
        manager?.unregisterGesture(view, gestureConfiguration)
    }
}
