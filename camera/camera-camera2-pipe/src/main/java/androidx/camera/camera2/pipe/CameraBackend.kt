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
package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

/** This is used to uniquely identify a specific backend implementation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class CameraBackendId(public val value: String)

/**
 * A CameraStatusMonitors monitors the status of the cameras, and emits updates when the status of
 * cameras changes, for instance when the camera access priorities have changed or when a particular
 * camera has become available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraStatusMonitor {
    public val cameraStatus: Flow<CameraStatus>

    public abstract class CameraStatus internal constructor() {
        public object CameraPrioritiesChanged : CameraStatus() {
            override fun toString(): String = "CameraPrioritiesChanged"
        }

        public class CameraAvailable(public val cameraId: CameraId) : CameraStatus() {
            override fun toString(): String = "CameraAvailable(camera=$cameraId"
        }
    }
}

/**
 * A CameraBackend is used by [CameraPipe] to abstract out the lifecycle, state, and interactions
 * with a set of camera devices in a standard way.
 *
 * Each [CameraBackend] is responsible for interacting with all of the individual cameras that are
 * available through this backend. Since cameras often have complicated lifecycles and expensive
 * interactions, this object serves as a low level facade that is used to manage access _across_ all
 * cameras exposed by this backend.
 *
 * The lifecycle of an individual camera is managed by [CameraController]s, which may be created via
 * [CameraBackend.createCameraController].
 */
@JvmDefaultWithCompatibility
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraBackend {
    public val id: CameraBackendId

    /**
     * A flow of camera statuses that provide camera status updates such as when the camera access
     * priorities have changed, or a certain camera has become available.
     */
    public val cameraStatus: Flow<CameraStatusMonitor.CameraStatus>

    /**
     * Read out a list of _openable_ [CameraId]s for this backend. The backend may be able to report
     * Metadata for non-openable cameras. However, these cameras should not appear the list of
     * cameras returned by [getCameraIds].
     */
    public suspend fun getCameraIds(): List<CameraId>? = awaitCameraIds()

    /** Thread-blocking version of [getCameraIds] for compatibility. */
    public fun awaitCameraIds(): List<CameraId>?

    /**
     * Read out a set of [CameraId] sets that can be operated concurrently. When multiple cameras
     * are open, the number of configurable streams, as well as their sizes, might be considerably
     * limited.
     */
    public suspend fun getConcurrentCameraIds(): Set<Set<CameraId>>? = awaitConcurrentCameraIds()

    /** Thread-blocking version of [getConcurrentCameraIds] for compatibility. */
    public fun awaitConcurrentCameraIds(): Set<Set<CameraId>>?

    /**
     * Retrieve [CameraMetadata] for this backend. Backends may cache the results of these calls.
     *
     * This call should should always succeed if the [CameraId] is in the list of ids returned by
     * [getCameraIds]. For some backends, it may be possible to retrieve metadata for cameras that
     * cannot be opened directly.
     */
    public suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata? =
        awaitCameraMetadata(cameraId)

    /** Thread-blocking version of [getCameraMetadata] for compatibility. */
    public fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata?

    /**
     * Stops all active [CameraController]s, which may disconnect any cached camera connection(s).
     * This may be called on the main thread, and any long running background operations should be
     * executed in the background. Once all connections are fully closed, the returned [Deferred]
     * should be completed.
     *
     * Subsequent [CameraController]s may still be created after invoking [disconnectAllAsync], and
     * existing [CameraController]s may attempt to restart.
     */
    public fun disconnectAllAsync(): Deferred<Unit>

    /**
     * Shutdown this backend, closing active [CameraController]s, and clearing any cached resources.
     *
     * This method should be used carefully, as it can cause expensive reloading and re-querying of
     * camera lists, metadata, and state. Once a backend instance has been shut down it should not
     * be reused, and a new instance must be recreated.
     */
    public fun shutdownAsync(): Deferred<Unit>

    /**
     * Creates a new [CameraController] instance that can be used to initialize and interact with a
     * specific Camera that is available from this CameraBackend. Creating a [CameraController]
     * should _not_ begin opening or interacting with the Camera until [CameraController.start] is
     * called.
     */
    public fun createCameraController(
        cameraContext: CameraContext,
        graphId: CameraGraphId,
        graphConfig: CameraGraph.Config,
        graphListener: GraphListener,
        streamGraph: StreamGraph
    ): CameraController

    /** Connects and starts the underlying camera */
    public fun prewarm(cameraId: CameraId)

    /** Disconnects the underlying camera. */
    public fun disconnect(cameraId: CameraId)

    /**
     * Disconnects the underlying camera. Once the connection is closed, the returned [Deferred]
     * should be completed.
     */
    public fun disconnectAsync(cameraId: CameraId): Deferred<Unit>

    /** Disconnects all active Cameras. */
    public fun disconnectAll()
}

/**
 * Factory for creating a new [CameraBackend].
 *
 * [CameraBackend] instances should not be cached by the factory instance, as the lifecycle of
 * returned instances is managed by [CameraPipe] unless the application asks [CameraPipe] to close
 * and release previously created [CameraBackend]s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface CameraBackendFactory {
    /** Create a new [CameraBackend] instance based on the provided [CameraContext]. */
    public fun create(cameraContext: CameraContext): CameraBackend
}

/**
 * Api for requesting and interacting with [CameraBackend] that are available in the current
 * [CameraPipe] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraBackends {
    /**
     * This provides access to the default [CameraBackend]. Accessing this property will create the
     * backend if it is not already created.
     */
    public val default: CameraBackend

    /**
     * This provides a list of all available [CameraBackend] instances, including the default one.
     * Accessing this set will not create or initialize [CameraBackend] instances.
     */
    public val allIds: Set<CameraBackendId>

    /**
     * This provides a list of [CameraBackend] instances that have been loaded, including the
     * default camera backend. Accessing this set will not create or initialize [CameraBackend]
     * instances.
     */
    public val activeIds: Set<CameraBackendId>

    /**
     * Get a previously created [CameraBackend] instance, or create a new one. If the backend fails
     * to load or is not available, this method will return null.
     */
    public operator fun get(backendId: CameraBackendId): CameraBackend?
}
