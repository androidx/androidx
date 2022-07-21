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

import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.Deferred

@JvmInline
value class CameraBackendId(public val value: String)

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
interface CameraBackend {
    val id: CameraBackendId

    /**
     * Read out a list of openable [CameraId]s for this backend. This call may block the calling
     * thread and should not cache the list of [CameraId]s if it's possible for them to change at
     * runtime.
     */
    fun readCameraIdList(): List<CameraId>

    /** Retrieve [CameraMetadata] for this backend. This call may block the calling thread and
     * should not internally cache the [CameraMetadata] instance if it's possible for it to change
     * at runtime.
     *
     * This call should should succeed if the [CameraId] is in the list of ids returned by
     * [readCameraIdList]. For some backends, it may be possible to retrieve metadata for cameras
     * that cannot be opened directly.
     */
    fun readCameraMetadata(cameraId: CameraId): CameraMetadata

    /**
     * Stops all active [CameraController]s, which may disconnect any cached camera connection(s).
     * This may be called on the main thread, and any long running background operations should be
     * executed in the background. Once all connections are fully closed, the returned [Deferred]
     * should be completed.
     *
     * Subsequent [CameraController]s may still be created after invoking [disconnectAllAsync], and
     * existing [CameraController]s may attempt to restart.
     */
    fun disconnectAllAsync(): Deferred<Unit>

    /**
     * Shutdown this backend, closing active [CameraController]s, and clearing any cached resources.
     *
     * This method should be used carefully, as it can cause expensive reloading and re-querying of
     * camera lists, metadata, and state. Once a backend instance has been shut down it should not
     * be reused, and a new instance must be recreated.
     */
    fun shutdownAsync(): Deferred<Unit>

    /**
     * Creates a new [CameraController] instance that can be used to initialize and interact with a
     * specific camera device defined by this CameraBackend. Creating a [CameraController] should
     * _not_ begin opening or interacting with the camera device until [CameraController.start] is
     * called.
     */
    fun createCameraController(
        cameraContext: CameraContext,
        graphConfig: CameraGraph.Config,
        graphListener: GraphListener,
        streamGraph: StreamGraph
    ): CameraController
}

/**
 * Factory for creating a new [CameraBackend].
 *
 * [CameraBackend] instances should not be cached by the factory instance, as the lifecycle of
 * returned instances is managed by [CameraPipe] unless the application asks [CameraPipe] to close
 * and release previously created [CameraBackend]s.
 */
interface CameraBackendFactory {
    /**
     * Create a new [CameraBackend] instance based on the provided [CameraContext].
     */
    fun create(cameraContext: CameraContext): CameraBackend
}

/**
 * Api for requesting and interacting with [CameraBackend] that are available in the current
 * [CameraPipe] instance.
 */
interface CameraBackends {
    /**
     * This provides access to the default [CameraBackend]. Accessing this property will create the
     * backend if it is not already created.
     */
    val default: CameraBackend

    /**
     * This provides a list of all available [CameraBackend] instances, including the default one.
     * Accessing this set will not create or initialize [CameraBackend] instances.
     */
    val allIds: Set<CameraBackendId>

    /**
     * This provides a list of [CameraBackend] instances that have been loaded, including the
     * default camera backend. Accessing this set will not create or initialize [CameraBackend]
     * instances.
     */
    val activeIds: Set<CameraBackendId>

    /**
     * Get a previously created [CameraBackend] instance, or create a new one. If the backend
     * fails to load or is not available, this method will return null.
     */
    operator fun get(backendId: CameraBackendId): CameraBackend?
}