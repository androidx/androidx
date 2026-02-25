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
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.HandlerExecutor
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.graph.StreamGraphImpl.OutputConfig
import androidx.camera.camera2.pipe.media.AndroidMultiResolutionImageReader
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
    ): Result

    sealed interface Result {
        data class Success(
            val deferred: Map<StreamId, OutputConfigurationWrapper>,
            val outputSurfaceMap: Map<OutputId, Surface>,
        ) : Result

        object Failed : Result
    }
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
constructor(
    private val threads: Threads,
    private val streamGraph: StreamGraphImpl,
    private val graphConfig: CameraGraph.Config,
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): CaptureSessionFactory.Result {
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
                captureSessionState.onSessionFinalized()
                return CaptureSessionFactory.Result.Failed
            }
        } else {
            if (
                !cameraDevice.createCaptureSession(surfaces.map { it.value }, captureSessionState)
            ) {
                Log.warn {
                    "Failed to create captures session from $cameraDevice for $captureSessionState!"
                }
                captureSessionState.onSessionFinalized()
                return CaptureSessionFactory.Result.Failed
            }
        }
        val outputSurfaceMap = buildSimpleOutputSurfaceMap(surfaces, streamGraph)
        return CaptureSessionFactory.Result.Success(emptyMap(), outputSurfaceMap)
    }
}

internal class AndroidMHighSpeedSessionFactory
@Inject
constructor(private val streamGraph: StreamGraphImpl, private val threads: Threads) :
    CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): CaptureSessionFactory.Result {
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
            captureSessionState.onSessionFinalized()
            return CaptureSessionFactory.Result.Failed
        }
        val outputSurfaceMap = buildSimpleOutputSurfaceMap(surfaces, streamGraph)
        return CaptureSessionFactory.Result.Success(emptyMap(), outputSurfaceMap)
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
    ): CaptureSessionFactory.Result {
        val outputs = buildOutputConfigurations(graphConfig, streamGraph, surfaces)
        if (outputs.all.isEmpty()) {
            Log.warn { "Failed to create OutputConfigurations for $graphConfig" }
            captureSessionState.onSessionFinalized()
            return CaptureSessionFactory.Result.Failed
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
            return CaptureSessionFactory.Result.Failed
        }
        return CaptureSessionFactory.Result.Success(emptyMap(), outputs.outputSurfaceMap)
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
    ): CaptureSessionFactory.Result {

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
            return CaptureSessionFactory.Result.Failed
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
            return CaptureSessionFactory.Result.Failed
        }
        return CaptureSessionFactory.Result.Success(outputs.deferred, outputs.outputSurfaceMap)
    }
}

@RequiresApi(31)
internal class AndroidExtensionSessionFactory
@Inject
constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: StreamGraphImpl,
    private val camera2MetadataProvider: Camera2MetadataProvider,
    private val strictMode: StrictMode,
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState,
    ): CaptureSessionFactory.Result {
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

        check(graphConfig.input == null) { "Reprocessing is not supported for Extensions" }

        // On certain platforms, supported extensions may return an empty list, but said
        // extension mode may actually be supported. See b/477805428 for an example.
        val cameraMetadata = camera2MetadataProvider.awaitCameraMetadata(cameraDevice.cameraId)
        val supportedExtensions = cameraMetadata.supportedExtensions
        strictMode.check(extensionMode in supportedExtensions) {
            "$cameraDevice does not support extension mode $extensionMode. Supported " +
                "extensions are $supportedExtensions"
        }

        if (graphConfig.postviewStream != null) {
            val cameraExtensionMetadata = cameraMetadata.awaitExtensionMetadata(extensionMode)
            strictMode.check(cameraExtensionMetadata.isPostviewSupported) {
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
            return CaptureSessionFactory.Result.Failed
        }

        check(outputs.deferred.isEmpty()) { "Deferred output is not supported for Extensions" }

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
            captureSessionState.onSessionFinalized()
            return CaptureSessionFactory.Result.Failed
        }

        return CaptureSessionFactory.Result.Success(outputs.deferred, outputs.outputSurfaceMap)
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
    val outputSurfaceMap = mutableMapOf<OutputId, Surface>()

    val outputConfigurationMap = mutableMapOf<OutputConfig, OutputConfiguration>()
    for ((streamId, imageSource) in streamGraph.imageSourceMap) {
        val stream = checkNotNull(streamGraph[streamId])
        val outputs = stream.outputs
        if (outputs.size == 1) {
            continue
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Create OutputConfigurations for multi-output streams.
            //
            // The camera framework stipulates that each OutputConfiguration should be configured
            // with its "internal" Surfaces. We can create these OutputConfigurations with a
            // dedicated API - createInstancesForMultiResolutionOutput(). This API returns a list of
            // OutputConfigurations in the same order as the order of the MultiResolutionStreamInfos
            // used to create the MultiResolutionImageReader. As such, we can line up our
            // OutputStreams with the returned OutputConfigurations one-by-one.
            val multiResImageReader =
                checkNotNull(imageSource.unwrapAs(AndroidMultiResolutionImageReader::class))
            val outputConfigurations = multiResImageReader.outputConfigurations
            check(outputConfigurations.size == outputs.size)

            for (outputIdx in outputs.indices) {
                val outputStream = outputs[outputIdx]
                val outputConfiguration = outputConfigurations[outputIdx]
                // TODO: b/470146651 - Validate the paired OutputConfiguration on newer API levels.

                val outputConfig = checkNotNull(streamGraph.outputConfigMap[outputStream])
                check(outputConfig.externalOutputConfig == null) {
                    "External OutputConfiguration shouldn't be set in " +
                        "multi-output streams configured with ImageSource.Config"
                }

                outputConfigurationMap[outputConfig] = outputConfiguration
            }
        } else {
            throw IllegalArgumentException("Cannot configure multiple outputs pre-S!")
        }
    }

    // Create a map of OutputId to Surface for all streams. This is needed as multi-output streams
    // would have "internal" Surfaces. Here we either use the configured Surface on the stream
    // (single-output scenarios), or use the Surface on each of the OutputConfigurations
    // (multi-output scenarios).
    for (stream in streamGraph.streams) {
        val outputStreams = stream.outputs
        if (outputStreams.size == 1) {
            val surface = surfaces[stream.id]
            if (surface != null) {
                outputSurfaceMap[outputStreams.single().id] = surface
            }
        } else {
            for (outputStream in outputStreams) {
                val outputConfig = checkNotNull(streamGraph.outputConfigMap[outputStream])
                val androidOutputConfig =
                    outputConfig.externalOutputConfig ?: outputConfigurationMap[outputConfig]
                val surface =
                    if (androidOutputConfig != null) {
                        androidOutputConfig.surface
                    } else {
                        surfaces[stream.id]
                    }
                if (surface != null) {
                    outputSurfaceMap[outputStream.id] = surface
                }
            }
        }
    }

    for (outputConfig in streamGraph.outputConfigs) {
        val outputSurfaces = outputConfig.streams.mapNotNull { surfaces[it.id] }

        val androidOutputConfiguration =
            outputConfig.externalOutputConfig ?: outputConfigurationMap[outputConfig]
        if (androidOutputConfiguration != null) {
            check(outputSurfaces.size == outputConfig.streams.size) {
                val missingStreams = outputConfig.streams.filter { !surfaces.contains(it.id) }
                "Surfaces are not yet available for $outputConfig!" +
                    " Missing surfaces for $missingStreams!"
            }
            allOutputs.add(
                AndroidOutputConfiguration(
                    androidOutputConfiguration,
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

    return OutputConfigurations(allOutputs, deferredOutputs, postviewOutput, outputSurfaceMap)
}

private fun buildSimpleOutputSurfaceMap(
    surfaces: Map<StreamId, Surface>,
    streamGraph: StreamGraphImpl,
) = buildMap {
    for (cameraStream in streamGraph.streams) {
        val surface = surfaces[cameraStream.id] ?: continue
        for (outputStream in cameraStream.outputs) {
            put(outputStream.id, surface)
        }
    }
}

internal data class OutputConfigurations(
    val all: List<OutputConfigurationWrapper>,
    val deferred: Map<StreamId, OutputConfigurationWrapper>,
    val postviewOutput: OutputConfigurationWrapper?,
    val outputSurfaceMap: Map<OutputId, Surface>,
)
