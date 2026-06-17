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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.collection.MutableIntObjectMap
import androidx.collection.intListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.core.content.ContextCompat
import androidx.wear.compose.material3.R
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.Sdk
import com.google.wear.input.ForegroundGestureSubscriptionParams
import com.google.wear.input.GestureEvent
import com.google.wear.input.GestureInputManager
import java.util.function.Consumer
import kotlin.collections.mutableMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [androidx.compose.runtime.CompositionLocal] used to provide the [GestureManager] instance
 * throughout the Composable hierarchy.
 */
internal val LocalGestureManager: ProvidableCompositionLocal<GestureManager> =
    compositionLocalWithComputedDefaultOf {
        if (cachedGestureManager == null) {
            cachedGestureManager = GestureManagerImpl()
        }
        cachedGestureManager!!
    }

internal interface GestureManager {
    /**
     * Registers a one-handed gesture.
     *
     * @param view The [View] containing the gesturable content.
     * @param haptic: The haptic to trigger events
     * @param gesture The gesture to register
     * @param isActive Whether UI component that triggers the gesture, is active
     * @param size The size of the UI component that triggers the gesture.
     */
    fun registerGesture(
        view: View,
        haptic: HapticFeedback,
        gesture: GestureConfig,
        isActive: () -> Boolean,
        size: () -> IntSize,
    )

    /**
     * Unregisters a previously registered one-handed gesture.
     *
     * This stops the application from listening for the specified [gesture]
     *
     * @param view The [View] containing the gesturable content.
     * @param gesture The gesture to unregister.
     */
    fun unregisterGesture(view: View, gesture: GestureConfig)

    /**
     * Updates previously registered gesture
     *
     * @param view The [View] containing the gesturable content.
     * @param oldGesture The currently registered gesture
     * @param newGesture The updated gesture to replace the current one
     */
    fun updateGesture(view: View, oldGesture: GestureConfig, newGesture: GestureConfig)

    /**
     * Notifies the system that gestures should be re-evaluated, typically due to changes in the UI
     * hierarchy.
     *
     * Call this when the layout changes, for example, when a new Composable that supports
     * one-handed gestures becomes visible, to ensure the correct indicator is displayed based on
     * the updated priorities.
     *
     * @param view The [View] containing the gesturable content.
     */
    fun invalidateGestures(view: View)

    /**
     * Determines whether the specified gesture indicator should be displayed to the user.
     *
     * @param action The specific [GestureAction] to evaluate.
     * @param key The unique identifier associated with this gesture instance.
     * @param isOverlay True if the indicator draws outside the boundary of its associated UI
     *   element (e.g., scroll indicator hints rendered adjacent to the track). False if the
     *   indicator is completely contained within the element's layout bounds (e.g., button hints).
     * @return True if the conditions are met to display the gesture indicator, false otherwise.
     */
    fun shouldShowGestureIndicator(action: GestureAction, key: String, isOverlay: Boolean): Boolean

    /**
     * Notifies the manager that a gesture indicator has been successfully displayed to the user.
     *
     * This acts as an acknowledgment callback and allows the manager to track presentation state
     * and manage indicator display limits.
     *
     * @param action The specific [GestureAction] whose indicator was presented.
     * @param key The unique identifier associated with the displayed gesture instance.
     */
    fun notifyIndicatorShown(action: GestureAction, key: String)
}

internal class GestureManagerImpl(
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main),
    val gestureInputManager: SdkGestureInputManager = SdkGestureInputManagerImpl(),
) : GestureManager {

    /** Map of registered gestures per View */
    private val gestureRegistries = mutableMapOf<View, GestureRegistry>()

    override fun registerGesture(
        view: View,
        haptic: HapticFeedback,
        gesture: GestureConfig,
        isActive: () -> Boolean,
        size: () -> IntSize,
    ) {
        val gestureRegistry =
            gestureRegistries.getOrPut(
                key = view,
                defaultValue = { GestureRegistry(view, haptic, scope, gestureInputManager) },
            )
        gestureRegistry.register(gesture, isActive, size)
    }

    override fun unregisterGesture(view: View, gesture: GestureConfig) {
        gestureRegistries[view]?.let { gestureRegistry ->
            gestureRegistry.unregister(gesture)

            if (gestureRegistry.numberOfRegisteredGestures == 0) {
                gestureRegistry.dispose()
                gestureRegistries.remove(view)
            }
        }
    }

    override fun updateGesture(view: View, oldGesture: GestureConfig, newGesture: GestureConfig) {
        gestureRegistries[view]?.update(oldGesture, newGesture)
    }

    override fun invalidateGestures(view: View) {
        gestureRegistries[view]?.invalidate()
    }

    override fun shouldShowGestureIndicator(
        action: GestureAction,
        key: String,
        isOverlay: Boolean,
    ): Boolean {
        return gestureInputManager.shouldShowIndicator(key, toSdkGestureAction(action), isOverlay)
    }

    override fun notifyIndicatorShown(action: GestureAction, key: String) {
        gestureInputManager.notifyIndicatorShown(key, toSdkGestureAction(action))
    }
}

internal class GestureRegistry(
    private val view: View,
    private val haptic: HapticFeedback,
    private val scope: CoroutineScope,
    private val gestureInputManager: SdkGestureInputManager,
) {
    val numberOfRegisteredGestures: Int
        get() = registeredGestures.size

    private val gestureAccessibilityAnnouncer: GestureAccessibilityAnnouncer =
        GestureAccessibilityAnnouncer(view)

    fun register(config: GestureConfig, isActive: () -> Boolean, size: () -> IntSize) {
        gestureAccessibilityAnnouncer.attach(config.action)
        registeredGestures.add(RegisteredGesture(config, isActive, size))
        registeredGestures.sortWith { gesture1, gesture2 ->
            gesture2.config.priority - gesture1.config.priority
        }

        invalidate()
    }

    fun unregister(config: GestureConfig) {
        if (registeredGestures.removeIf { (g, _) -> g == config }) {
            gestureAccessibilityAnnouncer.detach(config.action)
            invalidate()
        }
    }

    fun update(oldGesture: GestureConfig, newGesture: GestureConfig) {
        val index = registeredGestures.indexOfFirst { (g, _) -> g == oldGesture }
        val isActive = registeredGestures[index].isActive
        val size = registeredGestures[index].size

        registeredGestures.removeAt(index)
        register(newGesture, isActive, size)
    }

    @SuppressLint("ListIterator")
    fun invalidate() {
        showIndicatorJob?.cancel()

        if (!gestureInputManager.isAvailable(view.context)) {
            // Nothing to be done if device doesn't support gestures
            return
        }

        supportedSdkGestureActions.forEach { sdkGestureAction ->
            resubscribeToSdkGestureActionIfNeeded(sdkGestureAction)
        }

        showIndicatorJob =
            scope.launch {
                // A slight delay of 1s to avoid jumping indicators while the user is mid-flick.
                delay(1000)

                // Make a copy of registeredGestures, because emitting Indicate() might modify the
                // list
                val snapshot = registeredGestures.toList()
                // Since gestures are sorted by priority, the first visible gesture corresponds
                // to the highest priority.
                supportedSdkGestureActions.forEach { sdkAction ->
                    val gestureAction = fromSdkGestureAction(sdkAction)
                    val priority =
                        snapshot
                            .fastFirstOrNull { gesture ->
                                gesture.isActive() && gesture.config.action == gestureAction
                            }
                            ?.config
                            ?.priority

                    snapshot.fastForEach { gesture ->
                        if (
                            gesture.config.priority == priority &&
                                gesture.config.action == gestureAction &&
                                gesture.isActive()
                        ) {
                            gesture.config.interactionSource?.emit(
                                OneHandedGestureInteraction.Indicate(
                                    gesture.config.action,
                                    gesture.config.key,
                                )
                            )

                            gestureAccessibilityAnnouncer.announce(gesture.config)
                        }
                    }
                }
            }
    }

    fun dispose() {
        showIndicatorJob?.cancel()
    }

    /**
     * Checks if enabledInAmbient for currently prioritized gesture action has changed and
     * resubscribes if it does
     */
    private fun resubscribeToSdkGestureActionIfNeeded(sdkGestureAction: Int) {
        val gestureAction = fromSdkGestureAction(sdkGestureAction)
        val enabledInAmbient = isEnabledInAmbient(gestureAction)

        if (!shouldListenToGesture(gestureAction)) {
            unsubscribeFromSdkGestureAction(sdkGestureAction)
        } else if (enabledInAmbient != gestureActionIsAmbientEnabled[gestureAction.value]) {
            unsubscribeFromSdkGestureAction(sdkGestureAction)
            subscribeToSdkGestureAction(sdkGestureAction)
        }
    }

    /**
     * If there are multiple gestures with the same priority, check if any of them are enabled in
     * ambient
     */
    private fun isEnabledInAmbient(action: GestureAction): Boolean {
        val priority =
            registeredGestures
                .fastFirstOrNull { gesture ->
                    gesture.config.action == action && gesture.isActive()
                }
                ?.config
                ?.priority
        return priority?.let { prio ->
            registeredGestures.fastAny { gesture ->
                gesture.config.priority == prio &&
                    gesture.isActive() &&
                    gesture.config.enabledInAmbient
            }
        } ?: false
    }

    private fun subscribeToSdkGestureAction(sdkGestureAction: Int) {
        val gestureAction = fromSdkGestureAction(sdkGestureAction)
        if (!shouldListenToGesture(gestureAction)) {
            return
        }

        val enabledInAmbient = isEnabledInAmbient(gestureAction)
        gestureActionIsAmbientEnabled[gestureAction.value] = enabledInAmbient
        gestureInputManager.subscribeToSdkGestureAction(
            view,
            sdkGestureAction,
            enabledInAmbient,
            onGesture = { action -> handleAction(action) },
        )
    }

    private fun unsubscribeFromSdkGestureAction(sdkGestureAction: Int) {
        val gestureAction = fromSdkGestureAction(sdkGestureAction)
        gestureInputManager.unsubscribeFromSdkGestureAction(view, sdkGestureAction)
        gestureActionIsAmbientEnabled.remove(gestureAction.value)
    }

    @SuppressLint("ListIterator")
    private fun handleAction(sdkGestureAction: Int) {
        scope.launch {
            val gestureAction = fromSdkGestureAction(sdkGestureAction)

            // Make a copy of registeredGestures, because invoking onGesture() might modify the list
            val snapshot = registeredGestures.toList()

            // Since registeredGestures are sorted by priority, the first element that is
            // both visible and matches the requested action will have the highest priority
            val priority =
                snapshot
                    .fastFirstOrNull { gesture ->
                        gesture.isActive() && gesture.config.action == gestureAction
                    }
                    ?.config
                    ?.priority

            // Trigger all the visible gestures for the highest priority
            var hapticDone = false
            snapshot.fastForEach { gesture ->
                if (
                    gesture.config.priority == priority &&
                        gesture.config.action == gestureAction &&
                        gesture.isActive()
                ) {
                    if (!hapticDone) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        hapticDone = true
                    }

                    gesture.config.interactionSource?.let { source ->
                        val press = PressInteraction.Press(gesture.size().center.toOffset())
                        source.emit(press)
                        source.emit(PressInteraction.Release(press))
                    }

                    gesture.config.onGesture()
                    gestureInputManager.notifyGestureConsumed(
                        gesture.config.key,
                        toSdkGestureAction(gesture.config.action),
                    )
                }
            }
        }
    }

    /** Returns true if there are visible [action] gestures */
    private fun shouldListenToGesture(action: GestureAction): Boolean {
        return registeredGestures.fastFirstOrNull { gesture ->
            gesture.config.action == action && gesture.isActive()
        } != null
    }

    /** The list of [GestureEvent] actions that this system is capable of handling. */
    private val supportedSdkGestureActions =
        intListOf(GestureEvent.ACTION_PRIMARY, GestureEvent.ACTION_DISMISS)

    /** List of registered gestures, sorted by priority (descending). */
    private val registeredGestures: MutableList<RegisteredGesture> = mutableListOf()

    /**
     * Coroutine job that periodically evaluates the [registeredGestures] to identify the
     * highest-priority visible gesture and triggers its visual indicator.
     */
    var showIndicatorJob: Job? = null

    /** Tracks the 'ambientEnabled' state for each registered GestureAction */
    private val gestureActionIsAmbientEnabled: MutableIntObjectMap<Boolean> =
        mutableIntObjectMapOf()

    private data class RegisteredGesture(
        val config: GestureConfig,
        val isActive: () -> Boolean,
        val size: () -> IntSize,
    )
}

internal interface SdkGestureInputManager {
    fun isAvailable(context: Context): Boolean

    fun subscribeToSdkGestureAction(
        view: View,
        sdkGestureAction: Int,
        enabledInAmbient: Boolean,
        onGesture: (Int) -> Unit,
    )

    fun unsubscribeFromSdkGestureAction(view: View, sdkGestureAction: Int)

    fun notifyGestureConsumed(key: String, sdkGestureAction: Int)

    fun shouldShowIndicator(key: String, sdkGestureAction: Int, isOverlay: Boolean): Boolean

    fun notifyIndicatorShown(key: String, sdkGestureAction: Int)
}

internal class SdkGestureInputManagerImpl : SdkGestureInputManager {
    private var gestureInputManager: GestureInputManager? = null
    private var gestureInputManagerAttemptedToBeCreated = false
    private val gestureConsumers = mutableMapOf<View, MutableIntObjectMap<Consumer<GestureEvent>>>()

    override fun isAvailable(context: Context): Boolean {
        createSdkWearManagerIfNeeded(context)
        return gestureInputManager != null
    }

    override fun notifyGestureConsumed(key: String, sdkGestureAction: Int) {
        gestureInputManager?.notifyGestureConsumed(key, sdkGestureAction)
    }

    override fun subscribeToSdkGestureAction(
        view: View,
        sdkGestureAction: Int,
        enabledInAmbient: Boolean,
        onGesture: (Int) -> Unit,
    ) {
        createSdkWearManagerIfNeeded(view.context)
        if (gestureInputManager?.isActionSupported(sdkGestureAction) != true) {
            return
        }

        val consumers = gestureConsumers.getOrPut(view) { mutableIntObjectMapOf() }
        consumers[sdkGestureAction] = Consumer<GestureEvent> { onGesture(sdkGestureAction) }
        if (WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_CINNAMON_BUN_0)) {
            gestureInputManager?.addGestureEventListener(
                ForegroundGestureSubscriptionParams.Builder(intArrayOf(sdkGestureAction), view)
                    .setAmbientSupported(enabledInAmbient)
                    .build(),
                ContextCompat.getMainExecutor(view.context.applicationContext),
                consumers[sdkGestureAction],
            )
        } else {
            gestureInputManager?.addGestureEventListener(
                intArrayOf(sdkGestureAction),
                view,
                ContextCompat.getMainExecutor(view.context.applicationContext),
                consumers[sdkGestureAction],
            )
        }
    }

    override fun unsubscribeFromSdkGestureAction(view: View, sdkGestureAction: Int) {
        if (gestureInputManager?.isActionSupported(sdkGestureAction) == true) {
            val consumer = gestureConsumers[view]?.get(sdkGestureAction)
            consumer?.run {
                gestureInputManager?.removeGestureEventListener(consumer)
                gestureConsumers[view]?.remove(sdkGestureAction)
                if (gestureConsumers[view]?.size == 0) {
                    gestureConsumers.remove(view)
                }
            }
        }
    }

    override fun shouldShowIndicator(
        key: String,
        sdkGestureAction: Int,
        isOverlay: Boolean,
    ): Boolean {
        val isEnabled =
            gestureInputManager?.isActionSupported(sdkGestureAction) == true &&
                gestureInputManager?.isActionEnabled(sdkGestureAction) == true
        if (WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_CINNAMON_BUN_0)) {
            val flags = if (isOverlay) GestureInputManager.FLAG_HINT_STYLE_OVERLAY else 0
            return isEnabled &&
                gestureInputManager?.shouldShowHint(key, sdkGestureAction, flags) == true
        } else {
            return isEnabled && gestureInputManager?.shouldShowHint(key, sdkGestureAction) == true
        }
    }

    override fun notifyIndicatorShown(key: String, sdkGestureAction: Int) {
        if (gestureInputManager?.isActionSupported(sdkGestureAction) == true) {
            gestureInputManager?.notifyHintShown(key, sdkGestureAction)
        }
    }

    private fun createSdkWearManagerIfNeeded(context: Context) {
        if (gestureInputManagerAttemptedToBeCreated) return

        // Do not crash if either Wear SDK or GestureInputManager are missing
        try {
            if (Sdk.hasApiFeature(Sdk.FEATURE_WEAR_GESTURE_DETECTION)) {
                gestureInputManager =
                    Sdk.getWearManager(context.applicationContext, GestureInputManager::class.java)
                        as GestureInputManager
            }
        } catch (t: Throwable) {}
        gestureInputManagerAttemptedToBeCreated = true
    }
}

private fun toSdkGestureAction(gestureAction: GestureAction): Int {
    return when (gestureAction) {
        GestureAction.Dismiss -> GestureEvent.ACTION_DISMISS
        else -> GestureEvent.ACTION_PRIMARY
    }
}

private fun fromSdkGestureAction(sdkGestureAction: Int): GestureAction {
    return when (sdkGestureAction) {
        GestureEvent.ACTION_DISMISS -> GestureAction.Dismiss
        else -> GestureAction.Primary
    }
}

/**
 * Responsible for managing accessibility announcements for gestures. This class is designed as a
 * singleton per [GestureManager]. It manages the lifecycle of multiple accessibility "anchor" views
 * (one per [GestureAction]), ensuring that views are added to the hierarchy only when needed and
 * removed when no longer in use.
 *
 * @property container The host view where accessibility announcer views are added; typically an
 *   AndroidComposeView
 */
private class GestureAccessibilityAnnouncer(val container: View) {
    private val gestureAnnouncers = mutableIntObjectMapOf<ViewRefCount>()

    /**
     * Registers a [GestureAction] and creates a hidden accessibility view if one does not already
     * exist.
     * * If an announcer view for this action is already registered, its reference count is
     *   incremented.
     *
     * @param action The [GestureAction] to attach
     */
    fun attach(action: GestureAction) {
        val host = container as? ViewGroup ?: return

        if (gestureAnnouncers.contains(action.value)) {
            gestureAnnouncers[action.value]!!.refCount++
        } else {
            val hiddenAnnouncer =
                View(host.context).apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1)
                    visibility = View.VISIBLE
                    alpha = 0f
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                }
            host.addView(hiddenAnnouncer)
            gestureAnnouncers[action.value] = ViewRefCount(hiddenAnnouncer)
        }
    }

    /**
     * Unregisters a [GestureAction].
     * * The associated accessibility view is removed from the [container] only when the reference
     *   count drops to zero
     *
     * @param action The [GestureAction] to release
     */
    fun detach(action: GestureAction) {
        val viewRefCount = gestureAnnouncers[action.value] ?: return

        viewRefCount.refCount--
        if (viewRefCount.refCount <= 0) {
            (container as? ViewGroup)?.removeView(viewRefCount.view)
            gestureAnnouncers.remove(action.value)
        }
    }

    /**
     * Updates the content description of the announcer view associated with the given [config] to
     * trigger an accessibility announcement.
     * * This should be called by the `GestureManager` when a prioritized gesture is detected.
     *
     * @param config The [GestureConfig] defining the gesture and label to announce.
     */
    fun announce(config: GestureConfig) {
        val stringId = getGestureLabelStringId(config.action)
        val resources = gestureAnnouncers[config.action.value]?.view?.resources
        if (stringId != null && config.gestureLabel != null && resources != null) {
            gestureAnnouncers[config.action.value]?.view?.contentDescription =
                resources.getString(stringId, config.gestureLabel)
        }
    }

    private fun getGestureLabelStringId(action: GestureAction): Int? {
        return when (action) {
            GestureAction.Primary -> R.string.one_handed_gesture_primary_action_accessibility_text
            GestureAction.Dismiss -> R.string.one_handed_gesture_dismiss_action_accessibility_text
            else -> null
        }
    }

    private data class ViewRefCount(val view: View, var refCount: Int = 1)
}

internal data class GestureConfig(
    val action: GestureAction,
    val gestureLabel: String?,
    val key: String,
    val priority: Int,
    val enabledInAmbient: Boolean,
    val interactionSource: MutableInteractionSource?,
    val onGesture: suspend () -> Unit,
)

private var cachedGestureManager: GestureManager? = null
