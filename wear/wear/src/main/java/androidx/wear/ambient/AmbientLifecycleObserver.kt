/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.ambient

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import java.util.concurrent.Executor

/**
 * Create a new [AmbientLifecycleObserver] for use on a real device.
 *
 * Applications which wish to show layouts in ambient mode should attach the returned observer to
 * their activities or fragments, passing in a set of callback to be notified about ambient state.
 * In addition, the app needs to declare that it uses the [android.Manifest.permission.WAKE_LOCK]
 * permission in its manifest.
 *
 * The created [AmbientLifecycleObserver] can also be used to query whether the device is in
 * ambient mode.
 *
 * As an example of how to use this class, see the following example:
 *
 * ```
 * class MyActivity : ComponentActivity() {
 *     private val callbacks = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
 *         // ...
 *     }
 *
 *     private val ambientObserver = AmbientLifecycleObserver(this, callbacks)
 *
 *     override fun onCreate(savedInstanceState: Bundle) {
 *         lifecycle.addObserver(ambientObserver)
 *     }
 * }
 * ```
 *
 * @param activity The activity that this observer is being attached to.
 * @param callbackExecutor The executor to run the provided callbacks on.
 * @param callbacks An instance of [AmbientLifecycleObserver.AmbientLifecycleCallback], used to
 *                  notify the observer about changes to the ambient state.
 */
fun AmbientLifecycleObserver(
    activity: Activity,
    callbackExecutor: Executor,
    callbacks: AmbientLifecycleObserver.AmbientLifecycleCallback
): AmbientLifecycleObserver = AmbientLifecycleObserverImpl(activity, callbackExecutor, callbacks)

/**
 * Create a new [AmbientLifecycleObserver] for use on a real device.
 *
 * Applications which wish to show layouts in ambient mode should attach the returned observer to
 * their activities or fragments, passing in a set of callback to be notified about ambient state.
 * In addition, the app needs to declare that it uses the [android.Manifest.permission.WAKE_LOCK]
 * permission in its manifest.
 *
 * The created [AmbientLifecycleObserver] can also be used to query whether the device is in
 * ambient mode.
 *
 * As an example of how to use this class, see the following example:
 *
 * ```
 * class MyActivity : ComponentActivity() {
 *     private val callbacks = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
 *         // ...
 *     }
 *
 *     private val ambientObserver = DefaultAmbientLifecycleObserver(this, callbacks)
 *
 *     override fun onCreate(savedInstanceState: Bundle) {
 *         lifecycle.addObserver(ambientObserver)
 *     }
 * }
 * ```
 *
 * @param activity The activity that this observer is being attached to.
 * @param callbacks An instance of [AmbientLifecycleObserver.AmbientLifecycleCallback], used to
 *                  notify the observer about changes to the ambient state.
 */
fun AmbientLifecycleObserver(
    activity: Activity,
    callbacks: AmbientLifecycleObserver.AmbientLifecycleCallback
): AmbientLifecycleObserver = AmbientLifecycleObserverImpl(activity, callbacks)

/**
 * Interface for LifecycleObservers which are used to add ambient mode support to activities on
 * Wearable devices.
 *
 * This interface can be implemented, or faked out, to allow for testing activities which use
 * ambient support.
 */
@Suppress("CallbackName")
interface AmbientLifecycleObserver : DefaultLifecycleObserver {
    /**
     * Details about ambient mode support on the current device, passed to
     * [AmbientLifecycleCallback.onEnterAmbient].
     *
     * @param burnInProtectionRequired whether the ambient layout must implement burn-in protection.
     *     When this property is set to true, views must be shifted around periodically in ambient
     *     mode. To ensure that content isn't shifted off the screen, avoid placing content within
     *     10 pixels of the edge of the screen. Activities should also avoid solid white areas to
     *     prevent pixel burn-in. Both of these requirements  only apply in ambient mode, and only
     *     when this property is set to true.
     * @param deviceHasLowBitAmbient whether this device has low-bit ambient mode. When this
     *     property is set to true, the screen supports fewer bits for each color in ambient mode.
     *     In this case, activities should disable anti-aliasing in ambient mode.
     */
    class AmbientDetails(
        val burnInProtectionRequired: Boolean,
        val deviceHasLowBitAmbient: Boolean
    ) {
        override fun toString(): String =
            "AmbientDetails - burnInProtectionRequired: $burnInProtectionRequired, " +
                "deviceHasLowBitAmbient: $deviceHasLowBitAmbient"
    }

    /** Callback to receive ambient mode state changes. */
    interface AmbientLifecycleCallback {
        /**
         * Called when an activity is entering ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause). All drawing should complete by the conclusion
         * of this method. Note that {@code invalidate()} calls will be executed before resuming
         * lower-power mode.
         *
         * @param ambientDetails instance of [AmbientDetails] containing information about the
         *     display being used.
         */
        fun onEnterAmbient(ambientDetails: AmbientDetails) {}

        /**
         * Called when the system is updating the display for ambient mode. Activities may use this
         * opportunity to update or invalidate views.
         */
        fun onUpdateAmbient() {}

        /**
         * Called when an activity should exit ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause).
         */
        fun onExitAmbient() {}
    }

    /**
     * @return {@code true} if the activity is currently in ambient.
     */
    val isAmbient: Boolean
}
