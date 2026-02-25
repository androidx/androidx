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
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Parameters
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.graph.GraphProcessor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Implementation of [Parameters]. It propagates the parameter changes to the camera device via
 * [graphProcessor]. This is designed in such a way that helps us with reducing the actual number of
 * invocations to [graphProcessor]. We try to batch the changes and apply them together if possible.
 */
@CameraGraphScope
public class CameraGraphParametersImpl
@Inject
internal constructor(
    private val sessionLock: GraphSessionLock,
    private val graphProcessor: GraphProcessor,
    @ForCameraGraph private val graphScope: CoroutineScope,
) : Parameters {
    private val lock = Any()

    @GuardedBy("lock") private val parameters = mutableMapOf<Any, Any?>()

    /**
     * It tracks if the [parameters] map contains changes that have not been applied. It is set to
     * true when we detect changes to [parameters] and is set to false when a snapshot is taken to
     * be sent to the graphProcessor.
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
        val invokeUpdate =
            synchronized(lock) {
                var modified = false
                for ((key, value) in newParameters.entries) {
                    modified = modify(parameters, key, value) || modified
                }
                shouldApplyUpdate(modified)
            }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    @GuardedBy("lock")
    private fun modify(map: MutableMap<Any, Any?>, key: Any, value: Any?): Boolean {
        if (key !is CaptureRequest.Key<*> && key !is Metadata.Key<*>) {
            warn {
                "Skipping set parameter (key=$key, value=$value). $key is not a valid parameter type."
            }
            return false
        }
        if (map.containsKey(key) && map[key] == value) {
            return false
        }
        map[key] = value
        return true
    }

    public override fun clear() {
        val invokeUpdate =
            synchronized(lock) {
                if (parameters.isNotEmpty()) {
                    parameters.clear()
                    shouldApplyUpdate(modified = true)
                } else {
                    false
                }
            }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    public override fun <T> remove(key: CaptureRequest.Key<T>): Boolean {
        return removeAll(setOf(key))
    }

    public override fun <T> remove(key: Metadata.Key<T>): Boolean {
        return removeAll(setOf(key))
    }

    public override fun removeAll(keys: Set<*>): Boolean {
        var modified = false
        val invokeUpdate =
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
                shouldApplyUpdate(modified)
            }

        if (invokeUpdate) {
            applyUpdate()
        }
        return modified
    }

    // We should apply the update only if we the parameters are modified, and we are the one setting
    // dirty to true. If the dirty was already true then someone else should "flush" the parameters
    // as part of their update call.
    @GuardedBy("lock")
    private fun shouldApplyUpdate(modified: Boolean): Boolean {
        if (!modified) {
            return false
        }
        if (!dirty) {
            dirty = true
            return true
        }
        return false
    }

    private fun applyUpdate() {
        sessionLock.withTokenIn(graphScope) { flush() }
    }

    // Note: this must be called only when caller has an active sessionLock token.
    public fun flush() {
        val snapshot =
            synchronized(lock) {
                if (!dirty) {
                    return
                }
                dirty = false
                HashMap(parameters)
            }
        graphProcessor.updateGraphParameters(snapshot)
    }
}
