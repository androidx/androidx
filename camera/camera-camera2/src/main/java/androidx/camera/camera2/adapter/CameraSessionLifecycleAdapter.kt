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

package androidx.camera.camera2.adapter

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.core.impl.CameraSessionLifecycleCallback
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * An adapter that monitors logical [CameraGraph] state transitions and translates them into
 * hardware-agnostic [CameraSessionLifecycleCallback] events.
 *
 * Why we use CameraGraph state events instead of physical android CameraCaptureSession events:
 * 1. Bypassing Session Finalization Cooldown: camera-pipe intentionally keeps the camera device
 *    open for 2 seconds when closed to allow quick switches. During this cooldown, the physical
 *    CameraCaptureSession remains unclosed. By listening to logical CameraGraph stops instead, we
 *    report stops instantly to the viewfinder.
 * 2. Overcoming Flaky Hardware: Buggy device HALs occasionally fail to report capture session
 *    onClosed. CameraGraph's stopped transition is fully deterministic within our library.
 *
 * Note on Implementation Limits and Maintenance: Because this adapter translates high-level
 * [CameraGraph] lifecycle states, only the essential [CameraSessionLifecycleCallback] contract
 * events needed for session-level lifecycle monitoring are actively supported for now:
 * - [CameraSessionLifecycleCallback.onSessionStarted] is dispatched on [GraphStateStarted].
 * - [CameraSessionLifecycleCallback.onSessionStopped] is dispatched on [GraphStateStopped].
 * - [CameraSessionLifecycleCallback.onSessionError] is dispatched on [GraphStateError].
 *
 * Other transient or intermediate capture session states (such as active state changes or queue
 * draining events) are omitted here to keep the core callback API clean and focused. This contract
 * can be extended with additional granular tracking methods in the future if new requirements
 * arise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@CameraScope
public class CameraSessionLifecycleAdapter @Inject constructor() {
    private val lock = Any()

    @GuardedBy("lock")
    private val sessionLifecycleCallbacks = mutableMapOf<CameraSessionLifecycleCallback, Executor>()

    /**
     * Registers a [CameraSessionLifecycleCallback] to monitor logical camera session events. Uses
     * synchronized lock-based insertions to prevent registration race conditions.
     */
    internal fun addSessionLifecycleCallback(
        executor: Executor,
        callback: CameraSessionLifecycleCallback,
    ) {
        synchronized(lock) { sessionLifecycleCallbacks.put(callback, executor) }
    }

    /**
     * Unregisters a previously added [CameraSessionLifecycleCallback]. Uses synchronized lock-based
     * removals to prevent unregistration race conditions.
     */
    internal fun removeSessionLifecycleCallback(callback: CameraSessionLifecycleCallback) {
        synchronized(lock) { sessionLifecycleCallbacks.remove(callback) }
    }

    /**
     * Translates CameraGraph state transitions to hardware-agnostic
     * [CameraSessionLifecycleCallback] events.
     */
    internal fun dispatchSessionLifecycle(graphState: GraphState) {
        val callbacks =
            synchronized(lock) {
                if (sessionLifecycleCallbacks.isEmpty()) null else sessionLifecycleCallbacks.toMap()
            }
        if (callbacks == null) return

        when (graphState) {
            GraphStateStarted -> {
                dispatchToAll(callbacks) { onSessionStarted() }
            }
            GraphStateStopped -> {
                dispatchToAll(callbacks) { onSessionStopped() }
            }
            is GraphStateError -> {
                dispatchToAll(callbacks) { onSessionError() }
            }
            else -> {}
        }
    }

    /**
     * Dispatches the specified callback block to all registered listeners asynchronously. Operates
     * on a safe snapshot copy of the callback map to prevent deadlocks and
     * ConcurrentModificationExceptions during dispatch.
     */
    private fun dispatchToAll(
        callbacks: Map<CameraSessionLifecycleCallback, Executor>,
        action: CameraSessionLifecycleCallback.() -> Unit,
    ) {
        for ((callback, executor) in callbacks) {
            executor.execute { callback.action() }
        }
    }
}
