/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.annotation.SuppressLint
import android.hardware.camera2.params.InputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Provider

/** Creates a Camera2 CaptureSession from a CameraDevice */
internal interface CaptureSessionFactory {
    /**
     * Create a Camera2 CaptureSession using the given device, surfaces, and listener and return a
     * map of outputs that are not yet available.
     */
    fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper>
}

@Module
internal object Camera2CaptureSessionsModule {
    @SuppressLint("ObsoleteSdkInt")
    @Camera2ControllerScope
    @Provides
    fun provideSessionFactory(
        androidLProvider: Provider<AndroidLSessionFactory>,
        androidMProvider: Provider<AndroidMSessionFactory>,
        androidMHighSpeedProvider: Provider<AndroidMHighSpeedSessionFactory>,
        androidNProvider: Provider<AndroidNSessionFactory>,
        androidPProvider: Provider<AndroidPSessionFactory>,
        androidExtensionProvider: Provider<AndroidExtensionSessionFactory>,
        graphConfig: CameraGraph.Config
    ): CaptureSessionFactory {
        if (graphConfig.sessionMode == CameraGraph.OperatingMode.EXTENSION) {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "Cannot use Extension sessions below Android S"
            }
            return androidExtensionProvider.get()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return androidPProvider.get()
        }

        if (graphConfig.sessionMode == CameraGraph.OperatingMode.HIGH_SPEED) {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                "Cannot use HighSpeed sessions below Android M"
            }
            return androidMHighSpeedProvider.get()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return androidNProvider.get()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return androidMProvider.get()
        }

        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            "CameraPipe is not supported below Android L"
        }
        check(graphConfig.input == null) { "Reprocessing is not supported on Android L" }

        return androidLProvider.get()
    }
}

internal class AndroidLSessionFactory @Inject constructor(private val threads: Threads) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (!cameraDevice.createCaptureSession(
                surfaces.map { it.value }, captureSessionState, threads.camera2Handler
            )
        ) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.disconnect()
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidMSessionFactory
@Inject
constructor(private val threads: Threads, private val graphConfig: CameraGraph.Config) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (graphConfig.input != null) {
            val outputConfig = graphConfig.input.stream.outputs.single()
            if (!cameraDevice.createReprocessableCaptureSession(
                    InputConfiguration(
                        outputConfig.size.width,
                        outputConfig.size.height,
                        outputConfig.format.value
                    ),
                    surfaces.map { it.value },
                    captureSessionState,
                    threads.camera2Handler
                )
            ) {
                Log.warn {
                    "Failed to create reprocessable captures session from $cameraDevice for" +
                        " $captureSessionState!"
                }
                captureSessionState.disconnect()
            }
        } else {
            if (!cameraDevice.createCaptureSession(
                    surfaces.map { it.value }, captureSessionState, threads.camera2Handler
                )
            ) {
                Log.warn {
                    "Failed to create captures session from $cameraDevice for $captureSessionState!"
                }
                captureSessionState.disconnect()
            }
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidMHighSpeedSessionFactory @Inject constructor(private val threads: Threads) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (!cameraDevice.createConstrainedHighSpeedCaptureSession(
                surfaces.map { it.value }, captureSessionState, threads.camera2Handler
            )
        ) {
            Log.warn {
                "Failed to create ConstrainedHighSpeedCaptureSession " +
                    "from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.disconnect()
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal class AndroidNSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val streamGraph: StreamGraphImpl,
    private val graphConfig: CameraGraph.Config,
    private val camera2MetadataProvider: Camera2MetadataProvider
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        val outputs = buildOutputConfigurations(
            graphConfig,
            streamGraph,
            surfaces,
            camera2MetadataProvider,
            cameraDevice.cameraId
        )
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            return emptyMap()
        }

        val result = if (graphConfig.input == null) {
            cameraDevice.createCaptureSessionByOutputConfigurations(
                outputs.all, captureSessionState, threads.camera2Handler
            )
        } else {
            val outputConfig = graphConfig.input.stream.outputs.single()
            cameraDevice.createReprocessableCaptureSessionByConfigurations(
                InputConfigData(
                    outputConfig.size.width,
                    outputConfig.size.height,
                    outputConfig.format.value
                ),
                outputs.all,
                captureSessionState,
                threads.camera2Handler
            )
        }
        if (!result) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.disconnect()
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.P)
internal class AndroidPSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
    private val camera2MetadataProvider: Camera2MetadataProvider
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {

        val operatingMode =
            when (graphConfig.sessionMode) {
                CameraGraph.OperatingMode.NORMAL -> SessionConfigData.SESSION_TYPE_REGULAR
                CameraGraph.OperatingMode.HIGH_SPEED -> SessionConfigData.SESSION_TYPE_HIGH_SPEED
                else -> throw IllegalArgumentException(
                    "Unsupported session mode: ${graphConfig.sessionMode}")
            }

        val outputs = buildOutputConfigurations(
            graphConfig,
            streamGraph,
            surfaces,
            camera2MetadataProvider,
            cameraDevice.cameraId
        )
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            return emptyMap()
        }

        val input =
            graphConfig.input?.let {
                val outputConfig = it.stream.outputs.single()
                InputConfigData(
                    outputConfig.size.width, outputConfig.size.height, outputConfig.format.value
                )
            }

        val sessionConfig =
            SessionConfigData(
                operatingMode,
                input,
                outputs.all,
                threads.camera2Executor,
                captureSessionState,
                graphConfig.sessionTemplate.value,
                graphConfig.sessionParameters
            )

        if (!cameraDevice.createCaptureSession(sessionConfig)) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.disconnect()
        }
        return outputs.deferred
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal fun buildOutputConfigurations(
    graphConfig: CameraGraph.Config,
    streamGraph: StreamGraphImpl,
    surfaces: Map<StreamId, Surface>,
    camera2MetadataProvider: Camera2MetadataProvider,
    cameraId: CameraId
): OutputConfigurations {
    val allOutputs = arrayListOf<OutputConfigurationWrapper>()
    val deferredOutputs = mutableMapOf<StreamId, OutputConfigurationWrapper>()

    for (outputConfig in streamGraph.outputConfigs) {
        val outputSurfaces = outputConfig.streams.mapNotNull { surfaces[it.id] }

        val externalConfig = outputConfig.externalOutputConfig
        if (externalConfig != null) {
            check(outputSurfaces.size == outputConfig.streams.size) {
                val missingStreams = outputConfig.streams.filter { !surfaces.contains(it.id) }
                "Surfaces are not yet available for $outputConfig!" +
                    " Missing surfaces for $missingStreams!"
            }
            allOutputs.add(
                AndroidOutputConfiguration(
                    externalConfig,
                    surfaceSharing = false, // No way to read this value.
                    maxSharedSurfaceCount = 1, // Hardcoded
                    physicalCameraId = null, // No way to read this value.
                )
            )
            continue
        }

        if (outputConfig.deferrable && outputSurfaces.size != outputConfig.streams.size) {
            val output = AndroidOutputConfiguration.create(
                null,
                size = outputConfig.size,
                outputType = outputConfig.deferredOutputType!!,
                mirrorMode = outputConfig.mirrorMode,
                timestampBase = outputConfig.timestampBase,
                dynamicRangeProfile = outputConfig.dynamicRangeProfile,
                streamUseCase = outputConfig.streamUseCase,
                surfaceSharing = outputConfig.surfaceSharing,
                surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
                physicalCameraId = if (outputConfig.camera != graphConfig.camera) {
                    outputConfig.camera
                } else {
                    null
                },
                cameraId = cameraId,
                camera2MetadataProvider = camera2MetadataProvider
            )
            if (output == null) {
                Log.warn { "Failed to create AndroidOutputConfiguration for $outputConfig" }
                continue
            }
            allOutputs.add(output)
            for (outputSurface in outputConfig.streamBuilder) {
                deferredOutputs[outputSurface.id] = output
            }
            continue
        }

        // Default case: We have the surface(s)
        check(outputSurfaces.size == outputConfig.streams.size) {
            val missingStreams = outputConfig.streams.filter { !surfaces.contains(it.id) }
            "Surfaces are not yet available for $outputConfig!" +
                " Missing surfaces for $missingStreams!"
        }
        val output = AndroidOutputConfiguration.create(
            outputSurfaces.first(),
            mirrorMode = outputConfig.mirrorMode,
            timestampBase = outputConfig.timestampBase,
            dynamicRangeProfile = outputConfig.dynamicRangeProfile,
            streamUseCase = outputConfig.streamUseCase,
            size = outputConfig.size,
            surfaceSharing = outputConfig.surfaceSharing,
            surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
            physicalCameraId = if (outputConfig.camera != graphConfig.camera) {
                outputConfig.camera
            } else {
                null
            },
            cameraId = cameraId,
            camera2MetadataProvider = camera2MetadataProvider
        )
        if (output == null) {
            Log.warn { "Failed to create AndroidOutputConfiguration for $outputConfig" }
            continue
        }
        for (surface in outputSurfaces.drop(1)) {
            output.addSurface(surface)
        }
        allOutputs.add(output)
    }

    return OutputConfigurations(allOutputs, deferredOutputs)
}

@RequiresApi(Build.VERSION_CODES.S)
internal class AndroidExtensionSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
    private val camera2MetadataProvider: Camera2MetadataProvider
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {
        val operatingMode =
            when (graphConfig.sessionMode) {
                CameraGraph.OperatingMode.EXTENSION -> SessionConfigData.SESSION_TYPE_EXTENSION
                else -> throw IllegalArgumentException(
                    "Unsupported session mode: ${graphConfig.sessionMode}")
            }

        val outputs = buildOutputConfigurations(
            graphConfig,
            streamGraph,
            surfaces,
            camera2MetadataProvider,
            cameraDevice.cameraId
        )
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            return emptyMap()
        }

        check(graphConfig.input == null) { "Reprocessing is not supported for Extensions" }

        // TODO(b/276971147): get extensionMode from metadata and create extensionCaptureCallback
        val sessionConfig =
            SessionConfigData(
                operatingMode,
                graphConfig.input,
                outputs.all,
                threads.camera2Executor,
                captureSessionState,
                graphConfig.sessionTemplate.value,
                graphConfig.sessionParameters
            )

        if (!cameraDevice.createExtensionSession(sessionConfig)) {
            Log.warn {
                "Failed to create ExtensionCaptureSession from $cameraDevice " +
                    "for $captureSessionState!"
            }
            captureSessionState.disconnect()
        }

        return emptyMap()
    }
}

internal data class OutputConfigurations(
    val all: List<OutputConfigurationWrapper>,
    val deferred: Map<StreamId, OutputConfigurationWrapper>
)
