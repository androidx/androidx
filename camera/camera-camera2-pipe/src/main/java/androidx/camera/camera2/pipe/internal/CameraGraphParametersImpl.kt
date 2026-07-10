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
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.ParameterUpdateListener
import androidx.camera.camera2.pipe.Parameters
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.graph.GraphProcessor
import java.util.concurrent.atomic.AtomicBoolean
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
    @GuardedBy("lock") private val listeners = mutableListOf<ParameterUpdateRequestListener>()
    @GuardedBy("lock")
    private val listenerMap = mutableMapOf<CaptureRequest.Key<*>, Request.Listener>()
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
                    val isModified = modify(parameters, key, value)
                    if (isModified) {
                        modified = true
                    }
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

    public override fun <T : Any> apply(
        key: CaptureRequest.Key<T>,
        value: T?,
        listener: ParameterUpdateListener,
    ) {
        var skipListener = false
        val invokeUpdate =
            synchronized(lock) {
                if (parameters.containsKey(key) && parameters[key] == value) {
                    skipListener = true
                    false
                } else {
                    val wrappedListener = ParameterUpdateRequestListener(key, listener, this)
                    modify(parameters, key, value)
                    listeners.add(wrappedListener)
                    listenerMap[key] = wrappedListener
                    shouldApplyUpdate(modified = true)
                }
            }
        if (skipListener) {
            listener.onUpdateSkipped(null)
        } else if (invokeUpdate) {
            applyUpdate()
        }
    }

    // A listener is purged (meaning it receives an onUpdateSkipped callback and is removed) only
    // when it is superseded by a newer apply() call for the same parameter key before it ever gets
    // a chance to be sent to the camera framework.
    //
    // This avoids premature updates to the listener collection and prevents accidental listener
    // removal when apply() and other lifecycle methods are called in rapid succession.
    internal fun removeAndPurgePriors(
        key: CaptureRequest.Key<*>,
        listener: ParameterUpdateRequestListener,
    ) {
        val listenersToPurge = mutableSetOf<ParameterUpdateRequestListener>()
        synchronized(lock) {
            val iterator = listeners.listIterator()
            while (iterator.hasNext()) {
                val currentListener = iterator.next()
                // Once found remove the given listener from listener list as well as the
                // listenerMap.
                if (currentListener == listener) {
                    iterator.remove()
                    if (listenerMap[key] == listener) {
                        listenerMap.remove(key)
                    }
                    break
                }
                // Only remove the listener with matching Key.
                if (currentListener.key == key) {
                    listenersToPurge.add(currentListener)
                    iterator.remove()
                }
            }
        }
        for (priorListener in listenersToPurge) {
            priorListener.trySkip()
        }
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
                    checkNotNull(key) { "Parameter key should not be null!" }
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
        var snapshotListeners: List<Request.Listener> = emptyList()
        val snapshot =
            synchronized(lock) {
                if (!dirty) {
                    return
                }
                dirty = false
                snapshotListeners = listenerMap.values.toList()
                HashMap(parameters)
            }
        graphProcessor.updateGraphParameters(snapshot, snapshotListeners)
    }
}

internal class ParameterUpdateRequestListener(
    val key: CaptureRequest.Key<*>,
    val clientListener: ParameterUpdateListener,
    private val cameraGraphParameters: CameraGraphParametersImpl,
) : Request.Listener by NoOpRequestListener {
    private val started = AtomicBoolean(false)
    private val completed = AtomicBoolean(false)

    internal fun trySkip() {
        if (!started.get() && completed.compareAndSet(false, true)) {
            clientListener.onUpdateSkipped(null)
        }
    }

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ) {
        if (started.compareAndSet(false, true)) {
            clientListener.onUpdateStarted(requestMetadata, frameNumber, timestamp)
        }
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo,
    ) {
        if (completed.compareAndSet(false, true)) {
            clientListener.onUpdateCompleted(requestMetadata, frameNumber, result)
            cameraGraphParameters.removeAndPurgePriors(key, this)
        }
    }

    override fun onRequestSequenceCreated(requestMetadata: RequestMetadata) {
        if (!started.get()) {
            clientListener.onUpdateRequestCreated(requestMetadata)
        }
    }

    override fun onRequestSequenceSubmitted(requestMetadata: RequestMetadata) {
        if (!started.get()) {
            clientListener.onUpdateRequestSubmitted(requestMetadata)
        }
    }

    override fun onAborted(request: Request) {
        if (!started.get() && completed.compareAndSet(false, true)) {
            clientListener.onUpdateSkipped(null)
        }
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure,
    ) {
        if (!started.get() && completed.compareAndSet(false, true)) {
            clientListener.onUpdateSkipped(requestFailure)
        }
    }
}

private object NoOpRequestListener : Request.Listener
