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

package androidx.wear.compose.foundation

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.util.fastForEach
import com.google.wear.Sdk
import com.google.wear.services.ambient.AmbientComponentState
import com.google.wear.services.ambient.AmbientManager
import com.google.wear.services.ambient.AmbientManager.AmbientComponentListener
import com.google.wear.services.ambient.AmbientManager.ConfigurationDetails
import com.google.wear.services.ambient.AmbientOptions
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A [androidx.compose.runtime.CompositionLocal] that provides the current [AmbientModeManager]
 * interface.
 *
 * This local is the primary way for any composable in the hierarchy to access the manager
 * responsible for tracking the ambient (low-power) mode state.
 *
 * Composables read the manager using [LocalAmbientModeManager].current and subscribe to changes via
 * the [AmbientModeManager.currentAmbientMode] property.
 *
 * If no [AmbientModeManager] is explicitly provided higher up the tree (e.g., via
 * [rememberAmbientModeManager]), this local returns `null`.
 *
 * @sample androidx.wear.compose.foundation.samples.AmbientModeBasicSample
 */
public val LocalAmbientModeManager: ProvidableCompositionLocal<AmbientModeManager?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Creates, remembers, and manages the lifecycle of the default [AmbientModeManager] implementation.
 *
 * **This function requires a [LocalActivity] be present and automatically enables Always-On mode
 * for that activity, ensuring it remains visible in the low-power ambient state.**
 *
 * The resulting [AmbientModeManager] is retained across recompositions via [remember] and its
 * system listeners are automatically registered and unregistered using [DisposableEffect], tying
 * the ambient tracking to the composition lifecycle.
 *
 * See the Android documentation for more details on enabling Always-On mode:
 * [https://developer.android.com/training/wearables/views/always-on]
 *
 * Example of a simple screen switching between Interactive and Ambient modes:
 *
 * @sample androidx.wear.compose.foundation.samples.AmbientModeBasicSample
 * @return A new or remembered [AmbientModeManager] instance.
 */
@Composable
public fun rememberAmbientModeManager(): AmbientModeManager {
    val activity = LocalActivity.current
    requireNotNull(activity) {
        "rememberAmbientModeManager requires non-null LocalActivity.current, because it turns on" +
            "always-on mode for that activity"
    }
    val ambientManager = remember(activity) { AmbientModeManagerImpl(activity) }
    DisposableEffect(ambientManager) {
        ambientManager.startListening()
        onDispose { ambientManager.stopListening() }
    }
    return ambientManager
}

public interface AmbientModeManager {

    /**
     * The current state of the display, which is either [AmbientMode.Interactive] or
     * [AmbientMode.Ambient].
     *
     * This property should be backed by Compose's Snapshot State system. Reading this value inside
     * a composable function should automatically subscribe the composable to state changes and
     * trigger a recomposition when the ambient mode changes.
     */
    public val currentAmbientMode: AmbientMode

    /**
     * Suspends the calling coroutine and waits for the next delivered "ambient tick".
     *
     * When the device is in [AmbientMode.Ambient], the system only provides updates at infrequent
     * intervals (typically once per minute) to preserve battery life. This function allows a
     * coroutine to pause and resume execution specifically when that tick is received.
     *
     * The provided [block] will be executed synchronously when the tick occurs, and any state
     * updates performed within it will be reflected on the ambient screen.
     *
     * This function should typically be used within a looping [LaunchedEffect] that runs only when
     * [currentAmbientMode] is [AmbientMode.Ambient].
     *
     * @param block The state update logic to execute immediately upon receiving the ambient tick.
     * @sample androidx.wear.compose.foundation.samples.AmbientModeWithAmbientTickSample
     */
    public suspend fun withAmbientTick(block: () -> Unit)
}

/**
 * A convenience extension that performs recurrent, battery-efficient UI updates when the device is
 * in [AmbientMode.Ambient].
 *
 * This extension handles the boilerplate for ambient tick synchronization: it automatically
 * launches and manages a [LaunchedEffect] that repeatedly suspends using
 * [AmbientModeManager.withAmbientTick] to align state updates with the system's infrequent ambient
 * tick schedule.
 *
 * The internal loop automatically terminates when the device returns to [AmbientMode.Interactive].
 *
 * **Efficiency Note:** The [block] lambda should only update the minimal
 * [androidx.compose.runtime.State] required to prevent excessive recomposition and maximize battery
 * life.
 *
 * Example of using AmbientTickEffect:
 *
 * @sample androidx.wear.compose.foundation.samples.AmbientModeWithAmbientTickSample
 * @param block The state update logic to execute once per ambient tick.
 */
@Composable
public fun AmbientModeManager.AmbientTickEffect(block: () -> Unit) {
    if (currentAmbientMode is AmbientMode.Ambient) {
        LaunchedEffect(this, block) {
            while (isActive) {
                withAmbientTick(block)
            }
        }
    }
}

/**
 * Default implementation of [AmbientModeManager] which is created during a call to
 * [rememberAmbientModeManager]
 */
internal class AmbientModeManagerImpl(val activity: Activity) : AmbientModeManager {
    override val currentAmbientMode: AmbientMode
        get() = ambientMode

    override suspend fun withAmbientTick(block: () -> Unit) {
        return suspendCancellableCoroutine { co ->
            val callback: () -> Unit = { co.resumeWith(runCatching(block)) }
            oneShotTickCallbacks.add(callback)
            co.invokeOnCancellation { oneShotTickCallbacks.remove(callback) }
        }
    }

    fun startListening() {
        if (!activity.isDestroyed) {
            controller = createAmbientController(activity)
            activity.application.registerActivityLifecycleCallbacks(lifecycleCallback)
            registry.onResume()
        }
    }

    fun stopListening() {
        controller.let {
            activity.application.unregisterActivityLifecycleCallbacks(lifecycleCallback)
            it?.destroy()
        }
        controller = null
    }

    /**
     * A state registry used to signal AmbientManager when to start or stop sending ambient events.
     */
    private val registry = AmbientComponentState.makeActivityStateRegistry()

    /** AmbientManager controller which is used to subscribe to ambient events */
    private var controller: AmbientManager.Controller? = null

    /**
     * A list of one-shot ambient tick callbacks. Callbacks are invoked on the next ambient update
     * and then disposed of.
     */
    private val oneShotTickCallbacks = mutableListOf<() -> Unit>()

    /** current ambient mode, backed up by state */
    private var ambientMode: AmbientMode by mutableStateOf(AmbientMode.Interactive)

    private val lifecycleCallback: Application.ActivityLifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity == this@AmbientModeManagerImpl.activity) {
                    registry.onResume()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity == this@AmbientModeManagerImpl.activity) {
                    registry.onPause()
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity == this@AmbientModeManagerImpl.activity) {
                    stopListening()
                }
            }
        }

    private fun createAmbientController(activity: Activity): AmbientManager.Controller {
        val activityManager =
            Sdk.getWearManager(activity.applicationContext, AmbientManager::class.java)
        val options =
            AmbientOptions.makeActivityOptions(
                activity,
                registry,
                object : AmbientComponentListener {
                    override fun onEnterAmbient(
                        details: ConfigurationDetails?,
                        transitionComplete: Boolean,
                    ) {
                        ambientMode =
                            AmbientMode.Ambient(
                                details?.isBurnInProtectionEnabled == true,
                                details?.isLowBitDepthEnabled == true,
                            )
                    }

                    override fun onExitAmbient() {
                        ambientMode = AmbientMode.Interactive
                    }

                    override fun onUpdateAmbient() {
                        // Fire one-shot callbacks and clear the list
                        oneShotTickCallbacks.fastForEach { it() }
                        oneShotTickCallbacks.clear()
                    }
                },
                null,
            )
        return activityManager.createController(activity, options)
    }
}
