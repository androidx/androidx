/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.telecom.extensions.CallExtensionsScope
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * This class defines the Jetpack InCallService with the additional ability of defining a
 * [LifecycleOwner]
 */
@RequiresApi(Build.VERSION_CODES.O)
internal open class InCallServiceCompat : InCallService(), LifecycleOwner {
    // Since we define this service as a LifecycleOwner, we need to implement this dispatcher as
    // well. See [LifecycleService] for the example used to implement [LifecycleOwner].
    private val dispatcher = ServiceLifecycleDispatcher(this)

    companion object {
        private val TAG = InCallServiceCompat::class.simpleName
    }

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    @CallSuper
    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @CallSuper
    override fun onBind(intent: Intent): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    // We do not use onStart, but if the client does for some reason, we still want to override to
    // ensure the lifecycle events are consistent.
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    // We do not use onStartCommand, but if the client does for some reason, we still want to ensure
    // that the super is called (this command internally calls onStart)
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        // Todo: invoke CapabilityExchangeListener#onRemoveExtensions to inform the VOIP app
        super.onDestroy()
    }

    /**
     * Connects extensions to the provided [Call], allowing the call to support additional optional
     * behaviors beyond the traditional call state management.
     *
     * For example, an extension may allow the participants of a meeting to be surfaced to this
     * application so that the user can view and manage the participants in the meeting on different
     * surfaces:
     * ```
     * class InCallServiceImpl : InCallServiceCompat() {
     * ...
     *   override fun onCallAdded(call: Call) {
     *     lifecycleScope.launch {
     *       connectExtensions(context, call) {
     *         // Initialize extensions
     *         onConnected { call ->
     *           // change call states & listen/update extensions
     *         }
     *       }
     *       // Once the call is destroyed, control flow will resume again
     *     }
     *   }
     *  ...
     * }
     * ```
     *
     * @param call The Call to connect extensions on.
     * @param init The scope used to initialize and manage extensions in the scope of the Call.
     */
    // TODO: Refactor to Public API
    @ExperimentalAppActions
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun connectExtensions(call: Call, init: CallExtensionsScope.() -> Unit) {
        // Attach this to the scope of the InCallService so it does not outlive its lifecycle
        lifecycleScope
            .launch {
                val scope = CallExtensionsScope(applicationContext, this, call)
                Log.v(TAG, "connectExtensions: calling init")
                scope.init()
                Log.v(TAG, "connectExtensions: connecting extensions")
                scope.connectExtensionSession()
            }
            .join()
        Log.d(TAG, "connectExtensions: complete")
    }
}
