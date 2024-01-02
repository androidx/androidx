/*
 * Copyright 2020 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.config.CameraGraphConfigModule
import androidx.camera.camera2.pipe.config.CameraPipeComponent
import androidx.camera.camera2.pipe.config.CameraPipeConfigModule
import androidx.camera.camera2.pipe.config.DaggerCameraPipeComponent
import androidx.camera.camera2.pipe.config.DaggerExternalCameraPipeComponent
import androidx.camera.camera2.pipe.config.ExternalCameraGraphComponent
import androidx.camera.camera2.pipe.config.ExternalCameraGraphConfigModule
import androidx.camera.camera2.pipe.config.ExternalCameraPipeComponent
import androidx.camera.camera2.pipe.config.ThreadConfigModule
import androidx.camera.camera2.pipe.core.DurationNs
import java.util.concurrent.Executor
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

internal val cameraPipeIds = atomic(0)

/**
 * [CameraPipe] is the top level scope for all interactions with a Camera2 camera.
 *
 * Under most circumstances an application should only ever have a single instance of a [CameraPipe]
 * object as each instance will cache expensive calls and operations with the Android Camera
 * framework. In addition to the caching behaviors it will optimize the access and configuration of
 * [android.hardware.camera2.CameraDevice] and [android.hardware.camera2.CameraCaptureSession] via
 * the [CameraGraph] interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CameraPipe(config: Config) {
    private val debugId = cameraPipeIds.incrementAndGet()
    private val component: CameraPipeComponent =
        DaggerCameraPipeComponent.builder()
            .cameraPipeConfigModule(CameraPipeConfigModule(config))
            .threadConfigModule(ThreadConfigModule(config.threadConfig))
            .build()

    /**
     * This creates a new [CameraGraph] that can be used to interact with a single Camera on the
     * device. Multiple [CameraGraph]s can be created, but only one should be active at a time.
     */
    fun create(config: CameraGraph.Config): CameraGraph {
        return component
            .cameraGraphComponentBuilder()
            .cameraGraphConfigModule(CameraGraphConfigModule(config))
            .build()
            .cameraGraph()
    }

    /**
     * This creates a list of [CameraGraph]s that can be used to interact with multiple cameras on
     * the device concurrently. Device-specific constraints may apply, such as the set of cameras
     * that can be operated concurrently, or the combination of sizes we're allowed to configure.
     */
    fun createCameraGraphs(concurrentConfigs: List<CameraGraph.Config>): List<CameraGraph> {
        check(concurrentConfigs.isNotEmpty())
        if (concurrentConfigs.size == 1) {
            return listOf(create(concurrentConfigs.first()))
        }
        check(concurrentConfigs.all {
            it.cameraBackendId == concurrentConfigs.first().cameraBackendId
        }) {
            "All concurrent CameraGraph configs should have the same camera backend ID!"
        }
        val allCameraIds = concurrentConfigs.map { it.camera }
        check(allCameraIds.size == allCameraIds.toSet().size) {
            "All camera IDs specified should be distinct!"
        }
        val configs = concurrentConfigs.map { config ->
            config.apply {
                sharedCameraIds = allCameraIds.filter { it != config.camera }
            }
        }
        return configs.map {
            component
                .cameraGraphComponentBuilder()
                .cameraGraphConfigModule(CameraGraphConfigModule(it))
                .build()
                .cameraGraph()
        }
    }

    /** This provides access to information about the available cameras on the device. */
    fun cameras(): CameraDevices {
        return component.cameras()
    }

    /** This returns [CameraSurfaceManager] which tracks the lifetime of Surfaces in CameraPipe. */
    fun cameraSurfaceManager(): CameraSurfaceManager {
        return component.cameraSurfaceManager()
    }

    /**
     * Application level configuration for [CameraPipe]. Nullable values are optional and reasonable
     * defaults will be provided if values are not specified.
     */
    data class Config(
        val appContext: Context,
        val threadConfig: ThreadConfig = ThreadConfig(),
        val cameraMetadataConfig: CameraMetadataConfig = CameraMetadataConfig(),
        val cameraBackendConfig: CameraBackendConfig = CameraBackendConfig(),
        val cameraInteropConfig: CameraInteropConfig = CameraInteropConfig()
    )

    /**
     * Application level configuration for Camera2Interop callbacks. If set, these callbacks will be
     * triggered at the appropriate places in Camera-Pipe.
     */
    data class CameraInteropConfig(
        val cameraDeviceStateCallback: CameraDevice.StateCallback? = null,
        val cameraSessionStateCallback: CameraCaptureSession.StateCallback? = null,
        val cameraOpenRetryMaxTimeoutNs: DurationNs? = null
    )

    /**
     * Application level configuration for default thread and executors. If set, these executors
     * will be used to run asynchronous background work across [CameraPipe].
     * - [defaultLightweightExecutor] is used to run fast, non-blocking, lightweight tasks.
     * - [defaultBackgroundExecutor] is used to run blocking and/or io bound tasks.
     * - [defaultCameraExecutor] is used on newer API versions to interact with CameraAPIs. This is
     *   split into a separate field since many camera operations are extremely latency sensitive.
     * - [defaultCameraHandler] is used on older API versions to interact with CameraAPIs. This is
     *   split into a separate field since many camera operations are extremely latency sensitive.
     * - [testOnlyDispatcher] is used for testing to overwrite all internal dispatchers to the
     *   testOnly version. If specified, default executors and handlers are ignored.
     * - [testOnlyScope] is used for testing to overwrite the internal global scope with the test
     *   method scope.
     */
    data class ThreadConfig(
        val defaultLightweightExecutor: Executor? = null,
        val defaultBackgroundExecutor: Executor? = null,
        val defaultBlockingExecutor: Executor? = null,
        val defaultCameraExecutor: Executor? = null,
        val defaultCameraHandler: HandlerThread? = null,
        val testOnlyDispatcher: CoroutineDispatcher? = null,
        val testOnlyScope: CoroutineScope? = null
    )

    /**
     * Application level configuration options for [CameraMetadata] provider(s).
     *
     * @param cacheBlocklist is used to prevent the metadata backend from caching the results of
     *   specific keys.
     * @param cameraCacheBlocklist is used to prevent the metadata backend from caching the results
     *   of specific keys for specific cameraIds.
     */
    class CameraMetadataConfig(
        val cacheBlocklist: Set<CameraCharacteristics.Key<*>> = emptySet(),
        val cameraCacheBlocklist: Map<CameraId, Set<CameraCharacteristics.Key<*>>> =
            emptyMap()
    )

    /**
     * Configure the default and available [CameraBackend] instances that are available.
     *
     * @param internalBackend will override the default camera backend defined by [CameraPipe]. This
     *   may be used to mock and replace all interactions with camera2.
     * @param defaultBackend defines which camera backend instance should be used by default. If
     *   this value is specified, it must appear in the list of [cameraBackends]. If no value is
     *   specified, the [internalBackend] instance will be used. If [internalBackend] is null, the
     *   default backend will use the pre-defined [CameraPipe] internal backend.
     * @param cameraBackends defines a map of unique [CameraBackendFactory] that may be used to
     *   create, query, and operate cameras via [CameraPipe].
     */
    class CameraBackendConfig(
        val internalBackend: CameraBackend? = null,
        val defaultBackend: CameraBackendId? = null,
        val cameraBackends: Map<CameraBackendId, CameraBackendFactory> = emptyMap()
    ) {
        init {
            check(defaultBackend == null || cameraBackends.containsKey(defaultBackend)) {
                "$defaultBackend does not exist in cameraBackends! Available backends are:" +
                    " ${cameraBackends.keys}"
            }
        }
    }

    override fun toString(): String = "CameraPipe-$debugId"

    /**
     * External may be used if the underlying implementation needs to delegate to another library or
     * system.
     */
    @Deprecated(
        "CameraPipe.External is deprecated, use customCameraBackend on " + "GraphConfig instead."
    )
    class External(threadConfig: ThreadConfig = ThreadConfig()) {
        private val component: ExternalCameraPipeComponent =
            DaggerExternalCameraPipeComponent.builder()
                .threadConfigModule(ThreadConfigModule(threadConfig))
                .build()

        /**
         * This creates a new [CameraGraph] instance that is configured to use an externally defined
         * [RequestProcessor].
         */
        @Suppress("DEPRECATION")
        @Deprecated(
            "CameraPipe.External is deprecated, use customCameraBackend on " +
                "GraphConfig instead."
        )
        fun create(
            config: CameraGraph.Config,
            cameraMetadata: CameraMetadata,
            requestProcessor: RequestProcessor
        ): CameraGraph {
            check(config.camera == cameraMetadata.camera) {
                "Invalid camera config: ${config.camera} does not match ${cameraMetadata.camera}"
            }
            val componentBuilder = component.cameraGraphBuilder()
            val component: ExternalCameraGraphComponent =
                componentBuilder
                    .externalCameraGraphConfigModule(
                        ExternalCameraGraphConfigModule(config, cameraMetadata, requestProcessor)
                    )
                    .build()
            return component.cameraGraph()
        }
    }
}
