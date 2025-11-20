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

package androidx.camera.camera2.pipe.compat

import android.annotation.SuppressLint
import android.hardware.camera2.params.InputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.HandlerExecutor
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
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper>
}

@Module
internal object Camera2CaptureSessionsModule {
    @SuppressLint("ObsoleteSdkInt")
    @Camera2ControllerScope
    @Provides
    fun provideSessionFactory(
        androidMProvider: Provider<AndroidMSessionFactory>,
        androidMHighSpeedProvider: Provider<AndroidMHighSpeedSessionFactory>,
        androidNProvider: Provider<AndroidNSessionFactory>,
        androidPProvider: Provider<AndroidPSessionFactory>,
        androidExtensionProvider: Provider<AndroidExtensionSessionFactory>,
        graphConfig: CameraGraph.Config,
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
            return androidMHighSpeedProvider.get()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return androidNProvider.get()
        }

        check(graphConfig.input == null) { "Reprocessing is not supported on Android M" }
        return androidMProvider.get()
    }
}

internal class AndroidMSessionFactory
@Inject
constructor(private val threads: Threads, private val graphConfig: CameraGraph.Config) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (graphConfig.input != null) {
            val outputConfig = graphConfig.input.single().stream.outputs.single()
            if (
                !cameraDevice.createReprocessableCaptureSession(
                    InputConfiguration(
                        outputConfig.size.width,
                        outputConfig.size.height,
                        outputConfig.format.value,
                    ),
                    surfaces.map { it.value },
                    captureSessionState,
                )
            ) {
                Log.warn {
                    "Failed to create reprocessable captures session from $cameraDevice for" +
                        " $captureSessionState!"
                }
                captureSessionState.shutdown()
            }
        } else {
            if (
                !cameraDevice.createCaptureSession(surfaces.map { it.value }, captureSessionState)
            ) {
                Log.warn {
                    "Failed to create captures session from $cameraDevice for $captureSessionState!"
                }
                captureSessionState.onSessionFinalized()
            }
        }
        return emptyMap()
    }
}

internal class AndroidMHighSpeedSessionFactory @Inject constructor(private val threads: Threads) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (
            !cameraDevice.createConstrainedHighSpeedCaptureSession(
                surfaces.map { it.value },
                captureSessionState,
            )
        ) {
            Log.warn {
                "Failed to create ConstrainedHighSpeedCaptureSession " +
                    "from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.shutdown()
        }
        return emptyMap()
    }
}

@RequiresApi(24)
internal class AndroidNSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val streamGraph: StreamGraphImpl,
    private val graphConfig: CameraGraph.Config,
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {
        val outputs = buildOutputConfigurations(graphConfig, streamGraph, surfaces)
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            captureSessionState.onSessionFinalized()
            return emptyMap()
        }

        val result =
            if (graphConfig.input == null) {
                cameraDevice.createCaptureSessionByOutputConfigurations(
                    outputs.all,
                    captureSessionState,
                )
            } else {
                val outputConfig = graphConfig.input.single().stream.outputs.single()
                cameraDevice.createReprocessableCaptureSessionByConfigurations(
                    InputConfigData(
                        outputConfig.size.width,
                        outputConfig.size.height,
                        outputConfig.format.value,
                    ),
                    outputs.all,
                    captureSessionState,
                )
            }
        if (!result) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.onSessionFinalized()
        }
        return emptyMap()
    }
}

@RequiresApi(28)
internal class AndroidPSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {

        val operatingMode =
            when (graphConfig.sessionMode) {
                CameraGraph.OperatingMode.NORMAL -> Camera2SessionTypes.SESSION_TYPE_REGULAR
                CameraGraph.OperatingMode.HIGH_SPEED -> Camera2SessionTypes.SESSION_TYPE_HIGH_SPEED
                CameraGraph.OperatingMode.EXTENSION ->
                    throw IllegalArgumentException(
                        "Unsupported session mode: ${graphConfig.sessionMode}"
                    )
                else -> graphConfig.sessionMode.mode
            }

        val outputs = buildOutputConfigurations(graphConfig, streamGraph, surfaces)
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            captureSessionState.onSessionFinalized()
            return emptyMap()
        }

        val inputs =
            graphConfig.input?.map { inputConfig ->
                val outputConfig = inputConfig.stream.outputs.single()
                InputConfigData(
                    outputConfig.size.width,
                    outputConfig.size.height,
                    outputConfig.format.value,
                )
            }

        inputs?.let {
            check(it.all { input -> input.format == inputs[0].format }) {
                "All InputStream.Config objects must have the same format for multi resolution"
            }
        }

        val sessionConfig =
            SessionConfigData(
                operatingMode,
                inputs,
                outputs.all,
                threads.camera2Executor,
                captureSessionState,
                graphConfig.sessionTemplate.value,
                graphConfig.sessionParameters,
                graphConfig.sessionColorSpace,
            )

        if (!cameraDevice.createCaptureSession(sessionConfig)) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $captureSessionState!"
            }
            captureSessionState.onSessionFinalized()
        }
        return outputs.deferred
    }
}

@RequiresApi(24)
internal fun buildOutputConfigurations(
    graphConfig: CameraGraph.Config,
    streamGraph: StreamGraphImpl,
    surfaces: Map<StreamId, Surface>,
): OutputConfigurations {
    val allOutputs = arrayListOf<OutputConfigurationWrapper>()
    val deferredOutputs = mutableMapOf<StreamId, OutputConfigurationWrapper>()
    var postviewOutput: OutputConfigurationWrapper? = null

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
            val output =
                AndroidOutputConfiguration.create(
                    null,
                    size = outputConfig.size,
                    outputType = outputConfig.deferredOutputType!!,
                    mirrorMode = outputConfig.mirrorMode,
                    timestampBase = outputConfig.timestampBase,
                    dynamicRangeProfile = outputConfig.dynamicRangeProfile,
                    streamUseCase = outputConfig.streamUseCase,
                    sensorPixelModes = outputConfig.sensorPixelModes,
                    surfaceSharing = outputConfig.surfaceSharing,
                    surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
                    physicalCameraId =
                        if (outputConfig.camera != graphConfig.camera) {
                            outputConfig.camera
                        } else {
                            null
                        },
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
        val output =
            AndroidOutputConfiguration.create(
                outputSurfaces.first(),
                mirrorMode = outputConfig.mirrorMode,
                timestampBase = outputConfig.timestampBase,
                dynamicRangeProfile = outputConfig.dynamicRangeProfile,
                streamUseCase = outputConfig.streamUseCase,
                sensorPixelModes = outputConfig.sensorPixelModes,
                size = outputConfig.size,
                surfaceSharing = outputConfig.surfaceSharing,
                surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
                physicalCameraId =
                    if (outputConfig.camera != graphConfig.camera) {
                        outputConfig.camera
                    } else {
                        null
                    },
            )
        if (output == null) {
            Log.warn { "Failed to create AndroidOutputConfiguration for $outputConfig" }
            continue
        }
        for (surface in outputSurfaces.drop(1)) {
            output.addSurface(surface)
        }
        if (graphConfig.postviewStream != null) {
            val postviewStream = streamGraph[graphConfig.postviewStream]
            checkNotNull(postviewStream) {
                "Postview Stream in StreamGraph cannot be null for reprocessing request"
            }
            if (postviewOutput == null && outputConfig.streams.contains(postviewStream)) {
                postviewOutput = output
            } else {
                allOutputs.add(output)
            }
        } else {
            allOutputs.add(output)
        }
    }

    return OutputConfigurations(allOutputs, deferredOutputs, postviewOutput)
}

@RequiresApi(31)
internal class AndroidExtensionSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
    private val camera2MetadataProvider: Camera2MetadataProvider,
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): Map<StreamId, OutputConfigurationWrapper> {
        val operatingMode =
            when (graphConfig.sessionMode) {
                CameraGraph.OperatingMode.EXTENSION -> Camera2SessionTypes.SESSION_TYPE_EXTENSION
                else ->
                    throw IllegalArgumentException(
                        "Unsupported session mode: ${graphConfig.sessionMode} for Extension CameraGraph"
                    )
            }
        val extensionMode =
            checkNotNull(
                graphConfig.sessionParameters[CameraPipeKeys.camera2ExtensionMode] as? Int
            ) {
                "The CameraPipeKeys.camera2ExtensionMode must be set in the sessionParameters of the " +
                    "CameraGraph.Config when creating an Extension CameraGraph."
            }

        val cameraMetadata = camera2MetadataProvider.awaitCameraMetadata(cameraDevice.cameraId)

        val supportedExtensions = cameraMetadata.supportedExtensions

        check(extensionMode in supportedExtensions) {
            "$cameraDevice does not support extension mode $extensionMode. Supported " +
                "extensions are ${supportedExtensions.stream()}"
        }

        if (graphConfig.postviewStream != null) {
            val cameraExtensionMetadata = cameraMetadata.awaitExtensionMetadata(extensionMode)
            check(cameraExtensionMetadata.isPostviewSupported) {
                "$cameraDevice does not support Postview streams"
            }
            check(graphConfig.postviewStream.outputs.size == 1) {
                "Postview streams can only have one OutputStream.config object"
            }
        }

        val outputs = buildOutputConfigurations(graphConfig, streamGraph, surfaces)

        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            captureSessionState.onSessionFinalized()
            return emptyMap()
        }

        check(graphConfig.input == null) { "Reprocessing is not supported for Extensions" }

        val extensionSessionState = ExtensionSessionState(captureSessionState)

        val sessionConfig =
            ExtensionSessionConfigData(
                operatingMode,
                outputs.all,
                // This is a workaround to ensure extensions callbacks are handled in order.
                // camera2Handler is a HandlerThread and is single-threaded. This ensures callbacks
                // are executed one at a time on extension sessions. See b/425453656 for details.
                HandlerExecutor(threads.camera2Handler),
                captureSessionState,
                graphConfig.sessionTemplate.value,
                graphConfig.sessionParameters,
                extensionMode,
                extensionSessionState,
                outputs.postviewOutput,
            )

        if (!cameraDevice.createExtensionSession(sessionConfig)) {
            Log.warn {
                "Failed to create ExtensionCaptureSession from $cameraDevice " +
                    "for $captureSessionState!"
            }
            captureSessionState.shutdown()
        }

        return emptyMap()
    }
}

internal data class OutputConfigurations(
    val all: List<OutputConfigurationWrapper>,
    val deferred: Map<StreamId, OutputConfigurationWrapper>,
    val postviewOutput: OutputConfigurationWrapper?,
)
