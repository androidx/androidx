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

package androidx.camera.camera2.pipe

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.Handler
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraPipe.Config
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.config.CameraGraphConfigModule
import androidx.camera.camera2.pipe.config.CameraPipeComponent
import androidx.camera.camera2.pipe.config.CameraPipeConfigModule
import androidx.camera.camera2.pipe.config.DaggerCameraPipeComponent
import androidx.camera.camera2.pipe.config.FrameGraphConfigModule
import androidx.camera.camera2.pipe.config.ThreadConfigModule
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.media.ImageSources
import java.util.concurrent.Executor
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.synchronized
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
public interface CameraPipe {

    @Deprecated(
        "Use createCameraGraph instead.",
        replaceWith = ReplaceWith("createCameraGraph(config)"),
    )
    public fun create(config: CameraGraph.Config): CameraGraph

    /**
     * This creates a new [CameraGraph] that can be used to interact with a single Camera on the
     * device. Multiple [CameraGraph]s can be created, but only one should be active at a time.
     */
    public fun createCameraGraph(config: CameraGraph.Config): CameraGraph

    /**
     * This creates a list of [CameraGraph]s that can be used to interact with multiple cameras on
     * the device concurrently. Device-specific constraints may apply, such as the set of cameras
     * that can be operated concurrently, or the combination of sizes we're allowed to configure.
     */
    public fun createCameraGraphs(config: CameraGraph.ConcurrentConfig): List<CameraGraph>

    /**
     * [FrameGraph] extends [CameraGraph] and provides tools to more easily interact with [Frame]'s,
     * images, and metadata from the camera, while maintaining the capabilities of [CameraGraph].
     *
     * This creates a new [FrameGraph] that can be used to interact with a single Camera on the
     * device. Multiple [FrameGraph]s can be created, but only one should be active at a time.
     */
    public fun createFrameGraph(frameGraphConfig: FrameGraph.Config): FrameGraph

    /**
     * This creates a list of [FrameGraph]s that can be used to interact with multiple cameras on
     * the device concurrently. Device-specific constraints may apply, such as the set of cameras
     * that can be operated concurrently, or the combination of sizes we're allowed to configure.
     */
    public fun createFrameGraphs(frameGraphConfigs: FrameGraph.ConcurrentConfig): List<FrameGraph>

    /** This provides access to information about the available cameras on the device. */
    public fun cameras(): CameraDevices

    /** This returns [CameraSurfaceManager] which tracks the lifetime of Surfaces in CameraPipe. */
    public fun cameraSurfaceManager(): CameraSurfaceManager

    /**
     * Checks if a [CameraGraph.Config] is supported by the device before opening it.
     *
     * This returns a [ConfigQueryResult] based on the underlying
     * [CameraDeviceSetupCompat#isSessionConfigurationSupported](https://developer.android.com/reference/androidx/camera/featurecombinationquery/CameraDeviceSetupCompat#isSessionConfigurationSupported(android.hardware.camera2.params.SessionConfiguration)
     * method. Only configurations which can be queried through this API should be passed, otherwise
     * might lead to unexpected result (i.e. [ConfigQueryResult.UNKNOWN]). Check the
     * [CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION] API documentation to verify
     * which configurations are queryable.
     *
     * @param graphConfig The configuration to check for support.
     */
    public fun isConfigSupported(graphConfig: CameraGraph.Config): ConfigQueryResult

    /**
     * This gets and sets the global [AudioRestrictionMode] tracked by [AudioRestrictionController].
     */
    public var globalAudioRestrictionMode: AudioRestrictionMode

    /**
     * This shuts down the CameraPipe instance completely, releasing all its resources, notably the
     * threads it created. After shutdown, a new CameraPipe instance should be recreated for any
     * additional capture sessions the users would like to create.
     */
    public fun shutdown()

    /**
     * Application level configuration for [CameraPipe]. Nullable values are optional and reasonable
     * defaults will be provided if values are not specified.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Config(
        val appContext: Context,
        val threadConfig: ThreadConfig = ThreadConfig(),
        val cameraMetadataConfig: CameraMetadataConfig = CameraMetadataConfig(),
        val cameraBackendConfig: CameraBackendConfig = CameraBackendConfig(),
        val cameraInteropConfig: CameraInteropConfig = CameraInteropConfig(),
        val imageSources: ImageSources? = null,
    )

    /**
     * Application level configuration for Camera2Interop callbacks. If set, these callbacks will be
     * triggered at the appropriate places in CameraPipe.
     */
    public data class CameraInteropConfig(
        val cameraDeviceStateCallback: CameraDevice.StateCallback? = null,
        val cameraCaptureSessionListener: CameraInterop.CaptureSessionListener? = null,
        val cameraOpenRetryMaxTimeoutNs: DurationNs? = null,
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
    public data class ThreadConfig(
        val defaultLightweightExecutor: Executor? = null,
        val defaultBackgroundExecutor: Executor? = null,
        val defaultBlockingExecutor: Executor? = null,
        val defaultCameraExecutor: Executor? = null,
        val defaultCameraHandler: Handler? = null,
        val testOnlyDispatcher: CoroutineDispatcher? = null,
        val testOnlyScope: CoroutineScope? = null,
    )

    /**
     * Application level configuration options for [CameraMetadata] provider(s).
     *
     * @param cacheBlocklist is used to prevent the metadata backend from caching the results of
     *   specific keys.
     * @param cameraCacheBlocklist is used to prevent the metadata backend from caching the results
     *   of specific keys for specific cameraIds.
     */
    public class CameraMetadataConfig(
        public val cacheBlocklist: Set<CameraCharacteristics.Key<*>> = emptySet(),
        public val cameraCacheBlocklist: Map<CameraId, Set<CameraCharacteristics.Key<*>>> =
            emptyMap(),
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
    public class CameraBackendConfig(
        public val internalBackend: CameraBackend? = null,
        public val defaultBackend: CameraBackendId? = null,
        public val cameraBackends: Map<CameraBackendId, CameraBackendFactory> = emptyMap(),
    ) {
        init {
            check(defaultBackend == null || cameraBackends.containsKey(defaultBackend)) {
                "$defaultBackend does not exist in cameraBackends! Available backends are:" +
                    " ${cameraBackends.keys}"
            }
        }
    }

    public companion object {
        public fun create(config: Config): CameraPipe {
            val cameraPipeComponent =
                Debug.trace("CameraPipe") {
                    DaggerCameraPipeComponent.builder()
                        .cameraPipeConfigModule(CameraPipeConfigModule(config))
                        .threadConfigModule(ThreadConfigModule(config.threadConfig))
                        .build()
                }

            return CameraPipeImpl(cameraPipeComponent)
        }
    }
}

/** Utility constructor for existing classes that construct CameraPipe directly */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CameraPipe(config: Config): CameraPipe = CameraPipe.create(config)

internal class CameraPipeImpl(private val component: CameraPipeComponent) : CameraPipe {
    private val debugId = cameraPipeIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock") private var shutdown = false

    @Deprecated(
        "Use createCameraGraph instead.",
        replaceWith = ReplaceWith("createCameraGraph(config)"),
    )
    override fun create(config: CameraGraph.Config): CameraGraph = createCameraGraph(config)

    override fun createCameraGraph(config: CameraGraph.Config): CameraGraph =
        synchronized(lock) {
            check(!shutdown)
            createCameraGraphLocked(config)
        }

    override fun createCameraGraphs(config: CameraGraph.ConcurrentConfig): List<CameraGraph> =
        synchronized(lock) {
            check(!shutdown)
            config.graphConfigs.map { createCameraGraphLocked(it) }
        }

    @GuardedBy("lock")
    private fun createCameraGraphLocked(config: CameraGraph.Config) =
        Debug.trace("CXCP#CameraGraph-${config.camera}") {
            component
                .cameraGraphComponentBuilder()
                .cameraGraphConfigModule(CameraGraphConfigModule(config))
                .build()
                .cameraGraph()
        }

    override fun createFrameGraph(frameGraphConfig: FrameGraph.Config): FrameGraph =
        synchronized(lock) {
            check(!shutdown)
            createFrameGraphLocked(frameGraphConfig)
        }

    override fun createFrameGraphs(
        frameGraphConfigs: FrameGraph.ConcurrentConfig
    ): List<FrameGraph> =
        synchronized(lock) {
            check(!shutdown)
            frameGraphConfigs.frameGraphConfigs.map { createFrameGraphLocked(it) }
        }

    @GuardedBy("lock")
    private fun createFrameGraphLocked(frameGraphConfig: FrameGraph.Config) =
        Debug.trace("CXCP#CreateFrameGraph-${frameGraphConfig.cameraGraphConfig.camera}") {
            val cameraGraphComponent =
                component
                    .cameraGraphComponentBuilder()
                    .cameraGraphConfigModule(
                        CameraGraphConfigModule(frameGraphConfig.cameraGraphConfig)
                    )
                    .build()
            component
                .frameGraphComponentBuilder()
                .frameGraphConfigModule(
                    FrameGraphConfigModule(cameraGraphComponent, frameGraphConfig)
                )
                .build()
                .frameGraph()
        }

    /** This provides access to information about the available cameras on the device. */
    override fun cameras(): CameraDevices =
        synchronized(lock) {
            check(!shutdown)
            component.cameras()
        }

    /** This returns [CameraSurfaceManager] which tracks the lifetime of Surfaces in CameraPipe. */
    override fun cameraSurfaceManager(): CameraSurfaceManager =
        synchronized(lock) {
            check(!shutdown)
            component.cameraSurfaceManager()
        }

    /**
     * This checks if the given [CameraGraph.Config] is supported by the device.
     *
     * @param graphConfig The configuration to check for support.
     */
    // TODO: b/425425744 - Return default unknown until complete implementation
    override fun isConfigSupported(graphConfig: CameraGraph.Config): ConfigQueryResult =
        ConfigQueryResult.UNKNOWN

    /**
     * This gets and sets the global [AudioRestrictionMode] tracked by [AudioRestrictionController].
     */
    override var globalAudioRestrictionMode: AudioRestrictionMode
        get(): AudioRestrictionMode =
            synchronized(lock) {
                if (shutdown) {
                    Log.warn { "Trying to get audio restriction after shutdown! Returning NONE" }
                    return AudioRestrictionMode.AUDIO_RESTRICTION_NONE
                }
                component.cameraAudioRestrictionController().globalAudioRestrictionMode
            }
        set(value) =
            synchronized(lock) {
                if (shutdown) {
                    Log.warn { "Trying to set audio restriction after shutdown!" }
                    return
                }
                component.cameraAudioRestrictionController().globalAudioRestrictionMode = value
            }

    override fun shutdown() =
        synchronized(lock) {
            check(!shutdown)
            component.cameraPipeLifetime().shutdown()
            shutdown = true
        }

    override fun toString(): String = "CameraPipe-$debugId"
}
