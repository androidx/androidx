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

package androidx.camera.camera2.pipe.impl

import android.annotation.SuppressLint
import android.hardware.camera2.params.InputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.wrapper.AndroidOutputConfiguration
import androidx.camera.camera2.pipe.wrapper.CameraDeviceWrapper
import androidx.camera.camera2.pipe.wrapper.OutputConfigurationWrapper
import androidx.camera.camera2.pipe.wrapper.SessionConfigData
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Provider

/**
 * Creates a Camera2 CaptureSession from a CameraDevice
 */
interface SessionFactory {
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
object SessionFactoryModule {
    @CameraGraphScope
    @Provides
    fun provideSessionFactory(
        androidLProvider: Provider<AndroidLSessionFactory>,
        androidMProvider: Provider<AndroidMSessionFactory>,
        androidMHighSpeedProvider: Provider<AndroidMHighSpeedSessionFactory>,
        androidNProvider: Provider<AndroidNSessionFactory>,
        androidPProvider: Provider<AndroidPSessionFactory>,
        graphConfig: CameraGraph.Config
    ): SessionFactory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return androidPProvider.get()
        }

        if (graphConfig.operatingMode == CameraGraph.OperatingMode.HIGH_SPEED) {
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
        check(graphConfig.inputStream == null) {
            "Reprocessing is not supported on Android L"
        }

        return androidLProvider.get()
    }
}

class AndroidLSessionFactory @Inject constructor(
    private val threads: Threads
) : SessionFactory {
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
class AndroidMSessionFactory @Inject constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config
) : SessionFactory {
    @SuppressLint("NewApi")
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        if (graphConfig.inputStream != null) {
            try {
                cameraDevice.createReprocessableCaptureSession(
                    InputConfiguration(
                        graphConfig.inputStream.width,
                        graphConfig.inputStream.height,
                        graphConfig.inputStream.format
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
class AndroidMHighSpeedSessionFactory @Inject constructor(
    private val threads: Threads
) : SessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        TODO("Implement this")
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class AndroidNSessionFactory @Inject constructor(
    private val threads: Threads,
    private val streamMap: StreamMap,
    private val graphConfig: CameraGraph.Config
) : SessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        val outputs = buildOutputConfigurations(
            graphConfig,
            streamMap,
            surfaces
        )

        try {
            if (graphConfig.inputStream == null) {
                cameraDevice.createCaptureSessionByOutputConfigurations(
                    outputs.all,
                    virtualSessionState,
                    threads.camera2Handler
                )
            } else {
                cameraDevice.createReprocessableCaptureSessionByConfigurations(
                    graphConfig.inputStream,
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
class AndroidPSessionFactory @Inject constructor(
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val streamMap: StreamMap
) : SessionFactory {
    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        virtualSessionState: VirtualSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {

        val operatingMode =
            when (graphConfig.operatingMode) {
                CameraGraph.OperatingMode.NORMAL -> 0
                CameraGraph.OperatingMode.HIGH_SPEED -> 1
            }

        val outputs = buildOutputConfigurations(
            graphConfig,
            streamMap,
            surfaces
        )

        val sessionConfig = SessionConfigData(
            operatingMode,
            graphConfig.inputStream,
            outputs.all,
            threads.camera2Executor,
            virtualSessionState,
            graphConfig.template.value,
            graphConfig.defaultParameters
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
    streamMap: StreamMap,
    surfaces: Map<StreamId, Surface>
): OutputConfigurations {
    // TODO: Add support for:
    //   surfaceGroupId
    //   surfaceSharing
    //   multipleSurfaces?

    val outputs = arrayListOf<OutputConfigurationWrapper>()
    val deferredOutputs = mutableMapOf<StreamId, OutputConfigurationWrapper>()

    for (streamConfig in graphConfig.streams) {
        val streamId = streamMap.streamConfigMap[streamConfig]!!.id
        val physicalCameraId = if (streamConfig.camera != graphConfig.camera) {
            streamConfig.camera
        } else {
            null
        }

        val surface = surfaces[streamId]

        val config = AndroidOutputConfiguration.create(
            surface,
            streamType = streamConfig.type,
            size = streamConfig.size,
            physicalCameraId = physicalCameraId
        )

        outputs.add(config)

        if (surface == null) {
            deferredOutputs[streamId] = config
        }
    }

    // TODO: Sort outputs by type to try and put the viewfinder output first in the list
    //   This is important as some devices assume that the first surface is the viewfinder and
    //   will treat it differently.

    return OutputConfigurations(outputs, deferredOutputs)
}

internal data class OutputConfigurations(
    val all: List<OutputConfigurationWrapper>,
    val deferred: Map<StreamId, OutputConfigurationWrapper>
)
