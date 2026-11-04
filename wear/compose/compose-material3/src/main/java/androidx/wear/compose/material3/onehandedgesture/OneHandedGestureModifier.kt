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
import androidx.annotation.IntRange
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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureButtonSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of adding one-handed gesture handler that is enabled in ambient mode to a
 * [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonInAmbientSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureTransformingLazyColumnSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of using [androidx.compose.ui.layout.onVisibilityChanged] to trigger adding a one-handed
 * gesture handler to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVisibilitySample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureHorizontalPagerSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureVerticalPagerSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * @param gestureConfiguration The [OneHandedGestureConfiguration] containing the configuration for
 *   this gesture.
 * @param onGestureLabel Semantic label used by accessibility services to describe the purpose of
 *   this gesture. This is highly recommended for ensuring that users with screen readers understand
 *   what action will be performed.
 * @param showIndicatorDelayMillis The delay, in milliseconds, before the gesture indicator is
 *   shown - i.e. between the gesture becoming the highest-priority gesture for its action and
 *   [onGestureAvailable] being invoked. Must be non-negative.
 * @param enabledInAmbient Whether the gesture should remain active in ambient mode.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [androidx.compose.foundation.interaction.PressInteraction.Press] and an immediate
 *   [androidx.compose.foundation.interaction.PressInteraction.Release] when this gesture is
 *   triggered. This allows UI components to visually react (such as triggering a touch ripple or
 *   pressed state animation) upon gesture activation.
 * @param onGestureAvailable A callback invoked by the system to signal that this gesture is
 *   currently available as a high-priority action. Developers should use this callback to call
 *   showIndicator on the indicator state to trigger the associated visual feedback.
 * @param onGesture The callback invoked when the gesture is triggered.
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
public fun Modifier.oneHandedGesture(
    gestureConfiguration: OneHandedGestureConfiguration,
    onGestureLabel: String?,
    @IntRange(from = 0L) showIndicatorDelayMillis: Long,
    enabledInAmbient: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    onGestureAvailable: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier {
    return then(
        GestureElement(
            gestureConfiguration = gestureConfiguration,
            enabledInAmbient = enabledInAmbient,
            interactionSource = interactionSource,
            onGestureLabel = onGestureLabel,
            onGestureAvailable = onGestureAvailable,
            onGesture = onGesture,
            showIndicatorDelayMillis = showIndicatorDelayMillis,
        )
    )
}

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
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureButtonSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of adding one-handed gesture handler that is enabled in ambient mode to a
 * [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonInAmbientSample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureTransformingLazyColumnSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of using [androidx.compose.ui.layout.onVisibilityChanged] to trigger adding a one-handed
 * gesture handler to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVisibilitySample
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureHorizontalPagerSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * Example of adding one-handed gesture handler to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureVerticalPagerSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * @param gestureConfiguration The [OneHandedGestureConfiguration] containing the configuration for
 *   this gesture.
 * @param enabledInAmbient Whether the gesture should remain active in ambient mode.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [androidx.compose.foundation.interaction.PressInteraction.Press] and an immediate
 *   [androidx.compose.foundation.interaction.PressInteraction.Release] when this gesture is
 *   triggered. This allows UI components to visually react (such as triggering a touch ripple or
 *   pressed state animation) upon gesture activation.
 * @param onGestureLabel Semantic label used by accessibility services to describe the purpose of
 *   this gesture. This is highly recommended for ensuring that users with screen readers understand
 *   what action will be performed.
 * @param onGestureAvailable A callback invoked by the system to signal that this gesture is
 *   currently available as a high-priority action. Developers should use this callback to call
 *   showIndicator on the indicator state to trigger the associated visual feedback. It is invoked
 *   after a delay of [OneHandedGestureDefaults.ShowIndicatorDelayMillis], use the overload with
 *   `showIndicatorDelayMillis` parameter to customize this delay.
 * @param onGesture The callback invoked when the gesture is triggered.
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
public fun Modifier.oneHandedGesture(
    gestureConfiguration: OneHandedGestureConfiguration,
    onGestureLabel: String?,
    enabledInAmbient: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    onGestureAvailable: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier =
    oneHandedGesture(
        gestureConfiguration = gestureConfiguration,
        onGestureLabel = onGestureLabel,
        enabledInAmbient = enabledInAmbient,
        interactionSource = interactionSource,
        onGestureAvailable = onGestureAvailable,
        showIndicatorDelayMillis = OneHandedGestureDefaults.ShowIndicatorDelayMillis,
        onGesture = onGesture,
    )

private class GestureElement(
    val gestureConfiguration: OneHandedGestureConfiguration,
    val enabledInAmbient: Boolean,
    val interactionSource: MutableInteractionSource?,
    val onGestureLabel: String?,
    val onGestureAvailable: () -> Unit,
    val onGesture: suspend () -> Unit,
    val showIndicatorDelayMillis: Long,
) : ModifierNodeElement<GestureNode>() {

    override fun create() =
        GestureNode(
            gestureConfiguration,
            enabledInAmbient,
            interactionSource,
            onGestureLabel,
            onGestureAvailable,
            onGesture,
            showIndicatorDelayMillis,
        )

    override fun update(node: GestureNode) {
        node.updateGesture(
            gestureConfiguration,
            enabledInAmbient,
            interactionSource,
            onGestureLabel,
            onGestureAvailable,
            onGesture,
            showIndicatorDelayMillis,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GestureElement"
        properties["action"] = gestureConfiguration.action
        properties["priority"] = gestureConfiguration.priority
        properties["gestureId"] = gestureConfiguration.gestureId
        properties["enabledInAmbient"] = enabledInAmbient
        properties["interactionSource"] = interactionSource
        properties["onGestureLabel"] = onGestureLabel
        properties["showIndicatorDelayMillis"] = showIndicatorDelayMillis
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GestureElement) return false
        return gestureConfiguration == other.gestureConfiguration &&
            enabledInAmbient == other.enabledInAmbient &&
            interactionSource == other.interactionSource &&
            onGestureLabel == other.onGestureLabel &&
            onGesture === other.onGesture &&
            onGestureAvailable === other.onGestureAvailable &&
            showIndicatorDelayMillis == other.showIndicatorDelayMillis
    }

    override fun hashCode(): Int {
        var result = gestureConfiguration.hashCode()
        result = 31 * result + enabledInAmbient.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (onGestureLabel?.hashCode() ?: 0)
        result = 31 * result + onGesture.hashCode()
        result = 31 * result + onGestureAvailable.hashCode()
        result = 31 * result + showIndicatorDelayMillis.hashCode()
        return result
    }
}

private class GestureNode(
    private var gestureConfiguration: OneHandedGestureConfiguration,
    private var enabledInAmbient: Boolean,
    private var interactionSource: MutableInteractionSource?,
    private var onGestureLabel: String?,
    private var onGestureAvailable: () -> Unit,
    private var onGesture: suspend () -> Unit,
    private var showIndicatorDelayMillis: Long,
) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    LayoutAwareModifierNode {

    private var gestureManager: OneHandedGestureManager? = null
    private var localScreenIsActive = false
    private var currentView: View? = null
    private var hapticFeedback: HapticFeedback? = null
    private var size: IntSize = IntSize.Zero
    private var isEnabled = true

    /*
     * The callbacks (onGesture, onGestureAvailable) are registered with the gesture manager.
     * The callbacks are hoisted above the Modifier.oneHandedGesture, and the node's current values
     * are read when invoking them. Updating the callbacks or interaction source does not trigger
     * recomposition or re-register the gesture, so the gesture indicator is not re-shown.
     */
    private val handleGestureAvailable: () -> Unit = { onGestureAvailable() }

    private val handleGesture: suspend (centerOffset: Offset) -> Unit = { centerOffset ->
        interactionSource?.let { source ->
            val press = PressInteraction.Press(centerOffset)
            source.emit(press)
            try {
                source.emit(PressInteraction.Release(press))
            } catch (e: CancellationException) {
                withContext(NonCancellable) { source.emit(PressInteraction.Cancel(press)) }
                throw e
            }
        }
        onGesture()
    }

    override fun onAttach() {
        updateCompositionLocals(true)
    }

    override fun onObservedReadsChanged() = updateCompositionLocals(true)

    override fun onDetach() {
        unregisterGesture(gestureManager, currentView, gestureConfiguration)
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
        newInteractionSource: MutableInteractionSource?,
        newOnGestureLabel: String?,
        newOnGestureAvailable: () -> Unit,
        newOnGesture: suspend () -> Unit,
        newShowIndicatorDelayMillis: Long,
    ) {
        val oldConfig = gestureConfiguration
        val oldGestureManager = gestureManager
        val oldView = currentView
        val oldIsEnabled = isEnabled
        // Check if the registration changed before overwriting with the new values
        val registrationChanged =
            newConfig != oldConfig ||
                newEnabledInAmbient != enabledInAmbient ||
                newOnGestureLabel != onGestureLabel ||
                newShowIndicatorDelayMillis != showIndicatorDelayMillis

        // Store the new values first so that the callbacks and re-registration use the new values.
        gestureConfiguration = newConfig
        enabledInAmbient = newEnabledInAmbient
        interactionSource = newInteractionSource
        onGestureLabel = newOnGestureLabel
        onGestureAvailable = newOnGestureAvailable
        onGesture = newOnGesture
        showIndicatorDelayMillis = newShowIndicatorDelayMillis

        /* Update local compositions here to handle node reparenting. onAttach is not sufficient as
         * it may trigger before the node is fully settled in its new composition context. Manually
         * syncing ensures we capture the correct providers after the tree has stabilized. */
        updateCompositionLocals(false)

        val managerChanged = oldGestureManager != gestureManager
        val viewChanged = oldView != currentView
        if (!isEnabled || managerChanged || viewChanged) {
            unregisterGesture(oldGestureManager, oldView, oldConfig)
        }

        if (isEnabled && isAttached) {
            if (managerChanged || viewChanged || !oldIsEnabled) {
                registerGesture(gestureManager, currentView, hapticFeedback)
            } else if (registrationChanged) {
                // The registration details changed, so the registered gesture must be updated.
                currentView?.let { view ->
                    gestureManager?.updateGesture(
                        view,
                        oldConfig,
                        newConfig,
                        newEnabledInAmbient,
                        newOnGestureLabel,
                        handleGestureAvailable,
                        handleGesture,
                        newShowIndicatorDelayMillis,
                    )
                }
            }
            // Otherwise only the callbacks or the interaction source changed, which
            // handleGestureAvailable and handleGesture already pick up.
        }
    }

    private fun updateCompositionLocals(reregister: Boolean) {
        if (!isAttached) return

        observeReads {
            val oldView = currentView
            localScreenIsActive = currentValueOf(LocalScreenIsActive)
            currentView = currentValueOf(LocalView)
            hapticFeedback = currentValueOf(LocalHapticFeedback)
            isEnabled = currentValueOf(LocalOneHandedGestureEnabled)
            val newGestureManager = currentValueOf(LocalOneHandedGestureManager)
            if (reregister) {
                unregisterGesture(gestureManager, oldView, gestureConfiguration)
                registerGesture(newGestureManager, currentView, hapticFeedback)
            }
            gestureManager = newGestureManager
        }
    }

    private fun registerGesture(
        manager: OneHandedGestureManager?,
        view: View?,
        haptic: HapticFeedback?,
    ) {
        if (!isEnabled || view == null || haptic == null) return

        manager?.registerGesture(
            view = view,
            haptic = haptic,
            gestureConfiguration = gestureConfiguration,
            enabledInAmbient = enabledInAmbient,
            onGestureLabel = onGestureLabel,
            onGestureAvailable = handleGestureAvailable,
            onGesture = handleGesture,
            isActive = { localScreenIsActive },
            size = { size },
            showIndicatorDelayMillis = showIndicatorDelayMillis,
        )
    }

    private fun unregisterGesture(
        manager: OneHandedGestureManager?,
        view: View?,
        gestureConfiguration: OneHandedGestureConfiguration,
    ) {
        if (view == null) return

        manager?.unregisterGesture(view, gestureConfiguration)
    }
}
