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
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.google.android.wearable.compat.WearableActivityController
import java.util.concurrent.Executor

/**
 * Lifecycle Observer which can be used to add ambient support to an activity on Wearable devices.
 *
 * Applications which wish to show layouts in ambient mode should attach this observer to their
 * activities or fragments, passing in a set of callback to be notified about ambient state. In
 * addition, the app needs to declare that it uses the [android.Manifest.permission.WAKE_LOCK]
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
 * @param callbackExecutor The executor to run the provided callbacks on.
 * @param callbacks An instance of [AmbientLifecycleObserverInterface.AmbientLifecycleCallback], used to
 *                  notify the observer about changes to the ambient state.
 */
@Suppress("CallbackName")
class AmbientLifecycleObserver(
    activity: Activity,
    callbackExecutor: Executor,
    callbacks: AmbientLifecycleObserverInterface.AmbientLifecycleCallback,
) : AmbientLifecycleObserverInterface {
    private val delegate: AmbientDelegate
    private val callbackTranslator = object : AmbientDelegate.AmbientCallback {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            val burnInProtection = ambientDetails?.getBoolean(
                WearableActivityController.EXTRA_BURN_IN_PROTECTION) ?: false
            val lowBitAmbient = ambientDetails?.getBoolean(
                WearableActivityController.EXTRA_LOWBIT_AMBIENT) ?: false
            callbackExecutor.run {
                callbacks.onEnterAmbient(AmbientLifecycleObserverInterface.AmbientDetails(
                    burnInProtectionRequired = burnInProtection,
                    deviceHasLowBitAmbient = lowBitAmbient
                ))
            }
        }

        override fun onUpdateAmbient() {
            callbackExecutor.run { callbacks.onUpdateAmbient() }
        }

        override fun onExitAmbient() {
            callbackExecutor.run { callbacks.onExitAmbient() }
        }

        override fun onAmbientOffloadInvalidated() {
        }
    }

    /**
     * Construct a [AmbientLifecycleObserver], using the UI thread to dispatch ambient
     * callbacks.
     *
     * @param activity The activity that this observer is being attached to.
     * @param callbacks An instance of [AmbientLifecycleObserverInterface.AmbientLifecycleCallback], used to
     *                  notify the observer about changes to the ambient state.
     */
    constructor(
        activity: Activity,
        callbacks: AmbientLifecycleObserverInterface.AmbientLifecycleCallback
    ) : this(activity, { r -> r.run() }, callbacks)

    init {
        delegate = AmbientDelegate(activity, WearableControllerProvider(), callbackTranslator)
    }

    override fun isAmbient(): Boolean = delegate.isAmbient

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        delegate.onCreate()
        delegate.setAmbientEnabled()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        delegate.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        delegate.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        delegate.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        delegate.onDestroy()
    }
}