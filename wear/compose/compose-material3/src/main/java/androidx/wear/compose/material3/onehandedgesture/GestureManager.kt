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

import android.content.Context
import android.view.View
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
}

internal class GestureRegistry(
    private val view: View,
    private val haptic: HapticFeedback,
    private val scope: CoroutineScope,
    private val gestureInputManager: SdkGestureInputManager,
) {
    val numberOfRegisteredGestures: Int
        get() = registeredGestures.size

    fun register(config: GestureConfig, isActive: () -> Boolean, size: () -> IntSize) {
        registeredGestures.add(Triple(config, isActive, size))
        registeredGestures.sortWith { (gesture1), (gesture2) ->
            gesture2.priority - gesture1.priority
        }

        invalidate()
    }

    fun unregister(gesture: GestureConfig) {
        if (registeredGestures.removeIf { (g, _) -> g == gesture }) {
            invalidate()
        }
    }

    fun update(oldGesture: GestureConfig, newGesture: GestureConfig) {
        val index = registeredGestures.indexOfFirst { (g, _) -> g == oldGesture }
        val isActive = registeredGestures[index].second
        val size = registeredGestures[index].third

        registeredGestures.removeAt(index)
        register(newGesture, isActive, size)
    }

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

                val sdkPrimaryAction = toSdkGestureAction(GestureAction.Primary)
                // Since gestures are sorted by priority, the first visible gesture corresponds
                // to the highest priority.
                val priority =
                    registeredGestures
                        .fastFirstOrNull { (gesture, isActive) ->
                            isActive() && gesture.action == GestureAction.Primary
                        }
                        ?.first
                        ?.priority

                registeredGestures.fastForEach { (gesture, isActive) ->
                    if (
                        gesture.priority == priority &&
                            gesture.action == GestureAction.Primary &&
                            isActive() &&
                            gestureInputManager.shouldShowIndicator(gesture.key, sdkPrimaryAction)
                    ) {
                        gesture.onShowIndicator()
                        gestureInputManager.notifyIndicatorShown(gesture.key, sdkPrimaryAction)
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
                .fastFirstOrNull { (gesture, isActive) -> gesture.action == action && isActive() }
                ?.first
                ?.priority
        return priority?.let { prio ->
            registeredGestures.fastAny { (gesture, isActive) ->
                gesture.priority == prio && isActive() && gesture.enabledInAmbient
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

    private fun handleAction(sdkGestureAction: Int) {
        scope.launch {
            val gestureAction = fromSdkGestureAction(sdkGestureAction)

            // Since registeredGestures are sorted by priority, the first element that is
            // both visible and matches the requested action will have the highest priority
            val priority =
                registeredGestures
                    .fastFirstOrNull { (gesture, isActive) ->
                        isActive() && gesture.action == gestureAction
                    }
                    ?.first
                    ?.priority

            // Trigger all the visible gestures for the highest priority
            var hapticDone = false
            registeredGestures.fastForEach { (gesture, isActive, size) ->
                if (gesture.priority == priority && gesture.action == gestureAction && isActive()) {
                    if (!hapticDone) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        hapticDone = true
                    }

                    gesture.interactionSource?.let { source ->
                        val press = PressInteraction.Press(size().center.toOffset())
                        source.emit(press)
                        source.emit(PressInteraction.Release(press))
                    }

                    gesture.onGesture()
                    gestureInputManager.notifyGestureConsumed(
                        gesture.key,
                        toSdkGestureAction(gesture.action),
                    )
                }
            }
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

    /** Returns true if there are visible [action] gestures */
    private fun shouldListenToGesture(action: GestureAction): Boolean {
        return registeredGestures.fastFirstOrNull { (gesture, isActive) ->
            gesture.action == action && isActive()
        } != null
    }

    /** The list of [GestureEvent] actions that this system is capable of handling. */
    private val supportedSdkGestureActions =
        intListOf(GestureEvent.ACTION_PRIMARY, GestureEvent.ACTION_DISMISS)

    /** List of registered gestures, sorted by priority (descending). */
    private val registeredGestures:
        MutableList<Triple<GestureConfig, () -> Boolean, () -> IntSize>> =
        mutableListOf()

    /**
     * Coroutine job that periodically evaluates the [registeredGestures] to identify the
     * highest-priority visible gesture and triggers its visual indicator.
     */
    var showIndicatorJob: Job? = null

    /** Tracks the 'ambientEnabled' state for each registered GestureAction */
    private val gestureActionIsAmbientEnabled: MutableIntObjectMap<Boolean> =
        mutableIntObjectMapOf()
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

    fun shouldShowIndicator(key: String, sdkGestureAction: Int): Boolean

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

    override fun shouldShowIndicator(key: String, sdkGestureAction: Int): Boolean =
        gestureInputManager?.isActionSupported(sdkGestureAction) == true &&
            gestureInputManager?.isActionEnabled(sdkGestureAction) == true &&
            gestureInputManager?.shouldShowHint(key, sdkGestureAction) == true

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

internal data class GestureConfig(
    val action: GestureAction,
    val key: String,
    val priority: Int,
    val enabledInAmbient: Boolean,
    val interactionSource: MutableInteractionSource?,
    val onShowIndicator: () -> Unit,
    val onGesture: suspend () -> Unit,
)

private var cachedGestureManager: GestureManager? = null
