/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.graph.SessionLock
import javax.inject.Inject

@CameraGraphScope
public class CameraGraphParametersImpl
@Inject
internal constructor(private val sessionLock: SessionLock) : CameraGraph.Parameters {
    private val lock = Any()

    @GuardedBy("lock") private val parameters = mutableMapOf<Any, Any?>()
    @GuardedBy("lock") private var listener: (() -> Unit)? = null

    /**
     * Set to true when [parameters] is modified. Set to false when the modified [parameters] is
     * fetched.
     */
    @GuardedBy("lock") private var dirty = false

    @Suppress("UNCHECKED_CAST")
    public override operator fun <T> get(key: CaptureRequest.Key<T>): T? =
        synchronized(lock) { parameters[key] as T }

    @Suppress("UNCHECKED_CAST")
    public override operator fun <T> get(key: Metadata.Key<T>): T? =
        synchronized(lock) { parameters[key] as T }

    public override operator fun <T : Any> set(key: CaptureRequest.Key<T>, value: T?) {
        setAll(mapOf(key to value))
    }

    public override operator fun <T : Any> set(key: Metadata.Key<T>, value: T?) {
        setAll(mapOf(key to value))
    }

    public override fun setAll(newParameters: Map<Any, Any?>) {
        var invokeListener = false
        synchronized(lock) {
            var modified = false
            for ((key, value) in newParameters.entries) {
                modified = modified || modify(parameters, key, value)
            }
            if (modified && !dirty) {
                dirty = true
                invokeListener = true
            }
        }
        if (invokeListener) listener?.invoke()
    }

    private fun modify(map: MutableMap<Any, Any?>, key: Any, value: Any?): Boolean {
        if (key !is CaptureRequest.Key<*> && key !is Metadata.Key<*>) {
            warn {
                "Skipping set parameter (key=$key, value=$value). $key is not a valid parameter type."
            }
            return false
        }
        var modified = false
        if (!map.containsKey(key)) {
            modified = true
        }
        if (map[key] != value) {
            modified = true
        }
        map[key] = value
        return modified
    }

    public override fun clear() {
        var invokeListener = false
        synchronized(lock) {
            var modified = false
            if (parameters.isNotEmpty()) {
                parameters.clear()
                modified = true
            }
            if (modified && !dirty) {
                dirty = true
                invokeListener = true
            }
        }
        if (invokeListener) listener?.invoke()
    }

    public override fun <T> remove(key: CaptureRequest.Key<T>): Boolean {
        return removeAll(setOf(key))
    }

    public override fun <T> remove(key: Metadata.Key<T>): Boolean {
        return removeAll(setOf(key))
    }

    public override fun removeAll(keys: Set<*>): Boolean {
        var invokeListener = false
        var modified = false
        synchronized(lock) {
            for (key in keys) {
                if (parameters.containsKey(key)) {
                    parameters.remove(key)
                    modified = true
                }
                if (key !is CaptureRequest.Key<*> && key !is Metadata.Key<*>) {
                    warn {
                        "Skipping removing parameter with key $key. $key is not a valid parameter type."
                    }
                }
            }
            if (modified && !dirty) {
                dirty = true
                invokeListener = true
            }
        }
        if (invokeListener) listener?.invoke()
        return modified
    }

    /**
     * Return the latest parameters if the class stored parameters has changed since the last time
     * this method is called. If there is no parameter changes, return null.
     */
    public fun fetchUpdatedParameters(): Map<Any, Any?>? {
        synchronized(lock) {
            if (!dirty) return null

            dirty = false
            return parameters
        }
    }

    public fun setListener(listener: () -> Unit) {
        synchronized(lock) {
            if (this.listener != null) {
                warn { "Listener already set in $this." }
            } else {
                this.listener = listener
            }
        }
    }
}
