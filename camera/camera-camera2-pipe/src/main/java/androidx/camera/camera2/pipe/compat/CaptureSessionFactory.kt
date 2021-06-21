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
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import androidx.camera.camera2.pipe.core.Threads
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Provider

/**
 * Creates a Camera2 CaptureSession from a CameraDevice
 */
internal interface CaptureSessionFactory {
    /**
     * Create a Camera2 CaptureSession using the given device, surfaces, and listener and return
     * a map of outputs that are not yet available.
     */
    fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper>
}

@Module
internal object SessionFactoryModule {
    @SuppressLint("ObsoleteSdkInt")
    @CameraGraphScope
    @Provides
    fun provideSessionFactory(
        androidLProvider: Provider<AndroidLSessionFactory>,
        androidMProvider: Provider<AndroidMSessionFactory>,
        androidMHighSpeedProvider: Provider<AndroidMHighSpeedSessionFactory>,
        androidNProvider: Provider<AndroidNSessionFactory>,
        androidPProvider: Provider<AndroidPSessionFactory>,
        graphConfig: CameraGraph.Config
    ): CaptureSessionFactory {
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
        check(graphConfig.input == null) {
            "Reprocessing is not supported on Android L"
        }

        return androidLProvider.get()
    }
}

internal class AndroidLSessionFactory @Inject constructor(
    private val threads: Threads
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        try {
            cameraDevice.createCaptureSession(
                surfaces.map { it.value },
                virtualSessionState,
                threads.camera2Handler
            )
        } catch (e: Throwable) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $virtualSessionState!"
            }
            virtualSessionState.disconnect()
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidMSessionFactory @Inject constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (graphConfig.input != null) {
            try {
                val outputConfig = graphConfig.input.stream.outputs.single()
                cameraDevice.createReprocessableCaptureSession(
                    InputConfiguration(
                        outputConfig.size.width,
                        outputConfig.size.height,
                        outputConfig.format.value
                    ),
                    surfaces.map { it.value },
                    virtualSessionState,
                    threads.camera2Handler
                )
            } catch (e: Throwable) {
                Log.warn {
                    "Failed to create reprocessable captures session from $cameraDevice for" +
                        " $virtualSessionState!"
                }
                virtualSessionState.disconnect()
            }
        } else {
            try {
                cameraDevice.createCaptureSession(
                    surfaces.map { it.value },
                    virtualSessionState,
                    threads.camera2Handler
                )
            } catch (e: Throwable) {
                Log.warn {
                    "Failed to create captures session from $cameraDevice for $virtualSessionState!"
                }
                virtualSessionState.disconnect()
            }
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidMHighSpeedSessionFactory @Inject constructor(
    private val threads: Threads
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        TODO("Implement this")
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal class AndroidNSessionFactory @Inject constructor(
    private val threads: Threads,
    private val streamGraph: Camera2StreamGraph,
    private val graphConfig: CameraGraph.Config
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        val outputs = buildOutputConfigurations(
            graphConfig,
            streamGraph,
            surfaces
        )

        try {
            if (graphConfig.input == null) {
                cameraDevice.createCaptureSessionByOutputConfigurations(
                    outputs.all,
                    virtualSessionState,
                    threads.camera2Handler
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
                    virtualSessionState,
                    threads.camera2Handler
                )
            }
        } catch (e: Throwable) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $virtualSessionState!"
            }
            virtualSessionState.disconnect()
        }
        return emptyMap()
    }
}

@RequiresApi(Build.VERSION_CODES.P)
internal class AndroidPSessionFactory @Inject constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamGraph: Camera2StreamGraph
) : CaptureSessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {

        val operatingMode =
            when (graphConfig.sessionMode) {
                CameraGraph.OperatingMode.NORMAL -> SessionConfigData.SESSION_TYPE_REGULAR
                CameraGraph.OperatingMode.HIGH_SPEED -> SessionConfigData.SESSION_TYPE_HIGH_SPEED
            }

        val outputs = buildOutputConfigurations(
            graphConfig,
            streamGraph,
            surfaces
        )

        val input = graphConfig.input?.let {
            val outputConfig = it.stream.outputs.single()
            InputConfigData(
                outputConfig.size.width,
                outputConfig.size.height,
                outputConfig.format.value
            )
        }

        val sessionConfig = SessionConfigData(
            operatingMode,
            input,
            outputs.all,
            threads.camera2Executor,
            virtualSessionState,
            graphConfig.sessionTemplate.value,
            graphConfig.sessionParameters
        )

        try {
            cameraDevice.createCaptureSession(sessionConfig)
        } catch (e: Throwable) {
            Log.warn {
                "Failed to create capture session from $cameraDevice for $virtualSessionState!"
            }
            virtualSessionState.disconnect()
        }
        return outputs.deferred
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal fun buildOutputConfigurations(
    graphConfig: CameraGraph.Config,
    streamGraph: Camera2StreamGraph,
    surfaces: Map<StreamId, Surface>
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
                surfaceSharing = outputConfig.surfaceSharing,
                surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
                physicalCameraId = if (outputConfig.camera != graphConfig.camera) {
                    outputConfig.camera
                } else {
                    null
                }
            )
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
            size = outputConfig.size,
            surfaceSharing = outputConfig.surfaceSharing,
            surfaceGroupId = outputConfig.groupNumber ?: SURFACE_GROUP_ID_NONE,
            physicalCameraId = if (outputConfig.camera != graphConfig.camera) {
                outputConfig.camera
            } else {
                null
            }
        )
        for (surface in outputSurfaces.drop(1)) {
            output.addSurface(surface)
        }
        allOutputs.add(output)
    }

    return OutputConfigurations(allOutputs, deferredOutputs)
}

internal data class OutputConfigurations(
    val all: List<OutputConfigurationWrapper>,
    val deferred: Map<StreamId, OutputConfigurationWrapper>
)
