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

package androidx.camera.camera2.pipe.compat

import android.content.Context
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.CameraPipeModule
import androidx.camera.camera2.pipe.config.DefaultCameraBackend
import androidx.camera.camera2.pipe.config.SharedCameraGraphModules
import androidx.camera.camera2.pipe.config.ThreadConfigModule
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCaptureSequence
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class CaptureSessionFactoryTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)
    private val cameraErrorListener: CameraErrorListener = mock()
    private val fakeSurfaces = FakeSurfaces()

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
        fakeSurfaces.close()
    }

    @Test
    fun canCreateSessionFactoryTestComponent() = runTest {
        val component: Camera2CaptureSessionTestComponent =
            DaggerCamera2CaptureSessionTestComponent.builder()
                .fakeCameraPipeModule(FakeCameraPipeModule(context, testCamera))
                .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
                .build()

        val sessionFactory = component.sessionFactory()
        assertThat(sessionFactory).isNotNull()
    }

    @Test
    // Robolectric doesn't stub out older create capture session methods pre-P.
    @Config(minSdk = Build.VERSION_CODES.P)
    fun createCameraCaptureSession() = runTest {
        val component: Camera2CaptureSessionTestComponent =
            DaggerCamera2CaptureSessionTestComponent.builder()
                .fakeCameraPipeModule(FakeCameraPipeModule(context, testCamera))
                .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
                .build()

        val sessionFactory = component.sessionFactory()
        val streamMap = component.streamMap()
        val cameraStreamConfig = component.graphConfig().streams.first()
        val stream1 = streamMap[cameraStreamConfig]!!
        val stream1Output = stream1.outputs.first()

        val surface = fakeSurfaces.createFakeSurface(stream1Output.size)
        val threads = FakeThreads.fromTestScope(this)

        val result =
            sessionFactory.create(
                AndroidCameraDevice(
                    testCamera.metadata,
                    testCamera.cameraDevice,
                    testCamera.cameraId,
                    cameraErrorListener,
                    threads = threads,
                ),
                mapOf(stream1.id to surface),
                captureSessionState =
                    CaptureSessionState(
                        FakeGraphProcessor(),
                        sessionFactory,
                        object : Camera2CaptureSequenceProcessorFactory {
                            override fun create(
                                session: CameraCaptureSessionWrapper,
                                streamToSurfaceMap: Map<StreamId, Surface>,
                                outputToSurfaceMap: Map<OutputId, Surface>,
                            ): CaptureSequenceProcessor<Request, FakeCaptureSequence> =
                                FakeCaptureSequenceProcessor()
                        },
                        CameraSurfaceManager(),
                        SystemTimeSource(),
                        CameraGraph.Flags(
                            finalizeSessionOnCloseBehavior = FinalizeSessionOnCloseBehavior.OFF,
                            closeCaptureSessionOnDisconnect = false,
                        ),
                        concurrentSessionSequencer = null,
                        streamMap,
                        StrictMode(true),
                        threads,
                        this,
                    ),
            )

        assertThat(result).isInstanceOf(CaptureSessionFactory.Result.Success::class.java)
        val pendingOutputs = (result as CaptureSessionFactory.Result.Success).deferred
        assertThat(pendingOutputs).isNotNull()
        assertThat(pendingOutputs).isEmpty()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    fun buildOutputConfigurationsWithPartiallyAvailableSharedDeferrableStream() {
        val sharedOutputConfig =
            OutputStream.Config.create(
                size = Size(640, 480),
                format = StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE_VIEW,
            )
        val streamConfig1 = CameraStream.Config.create(sharedOutputConfig)
        val streamConfig2 = CameraStream.Config.create(sharedOutputConfig)
        val graphConfig =
            CameraGraph.Config(
                camera = testCamera.cameraId,
                streams = listOf(streamConfig1, streamConfig2),
            )
        val streamGraph =
            StreamGraphImpl(
                testCamera.metadata,
                graphConfig,
                mock(),
                mock(),
                MemoryEstimator.create(),
            )
        val stream1 = checkNotNull(streamGraph[streamConfig1])
        val stream2 = checkNotNull(streamGraph[streamConfig2])

        val surface1 = fakeSurfaces.createFakeSurface(Size(640, 480))

        val outputs =
            buildOutputConfigurations(graphConfig, streamGraph, mapOf(stream1.id to surface1))

        assertThat(outputs.all).hasSize(1)
        assertThat(outputs.all.single().surface).isEqualTo(surface1)
        assertThat(outputs.all.single().surfaces).containsExactly(surface1)
        assertThat(outputs.deferred.keys).containsExactly(stream2.id)
        assertThat(outputs.outputSurfaceMap).containsExactly(stream1.outputs.single().id, surface1)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    fun buildOutputConfigurationsWithFullyMissingSharedDeferrableStream() {
        val sharedOutputConfig =
            OutputStream.Config.create(
                size = Size(640, 480),
                format = StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE_VIEW,
            )
        val streamConfig1 = CameraStream.Config.create(sharedOutputConfig)
        val streamConfig2 = CameraStream.Config.create(sharedOutputConfig)
        val graphConfig =
            CameraGraph.Config(
                camera = testCamera.cameraId,
                streams = listOf(streamConfig1, streamConfig2),
            )
        val streamGraph =
            StreamGraphImpl(
                testCamera.metadata,
                graphConfig,
                mock(),
                mock(),
                MemoryEstimator.create(),
            )
        val stream1 = checkNotNull(streamGraph[streamConfig1])
        val stream2 = checkNotNull(streamGraph[streamConfig2])

        val outputs = buildOutputConfigurations(graphConfig, streamGraph, emptyMap())

        assertThat(outputs.all).hasSize(1)
        assertThat(outputs.all.single().surface).isNull()
        assertThat(outputs.all.single().surfaces).isEmpty()
        assertThat(outputs.deferred.keys).containsExactly(stream1.id, stream2.id)
        assertThat(outputs.outputSurfaceMap).isEmpty()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun androidOutputConfiguration_create_withUseReadoutTimestamp_enablesReadoutTimestampOnApi34() {
        val surface = fakeSurfaces.createFakeSurface(Size(640, 480))

        val enabledWrapper = AndroidOutputConfiguration.create(surface, useReadoutTimestamp = true)
        assertThat(enabledWrapper).isNotNull()
        val enabledConfig = enabledWrapper!!.unwrapAs(OutputConfiguration::class.java)
        assertThat(enabledConfig).isNotNull()
        assertThat(Api34Compat.isReadoutTimestampEnabled(enabledConfig!!)).isTrue()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun androidOutputConfiguration_create_withUseReadoutTimestamp_preApi34_throwsIfNotNull() {
        val surface = fakeSurfaces.createFakeSurface(Size(640, 480))

        assertThat(AndroidOutputConfiguration.create(surface, useReadoutTimestamp = null))
            .isNotNull()
        assertThrows<IllegalStateException> {
            AndroidOutputConfiguration.create(surface, useReadoutTimestamp = false)
        }
        assertThrows<IllegalStateException> {
            AndroidOutputConfiguration.create(surface, useReadoutTimestamp = true)
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun buildOutputConfigurations_wiresUseReadoutTimestamp() = runTest {
        val surface1 = fakeSurfaces.createFakeSurface(Size(640, 480))
        val surface2 = fakeSurfaces.createFakeSurface(Size(640, 480))

        val streamWithReadout =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                useReadoutTimestamp = true,
            )
        val streamWithoutReadout =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                useReadoutTimestamp = false,
            )
        val deferredStreamWithReadout =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.PRIVATE,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE,
                useReadoutTimestamp = true,
            )

        val customGraphConfig =
            CameraGraph.Config(
                camera = testCamera.cameraId,
                streams =
                    listOf(streamWithReadout, streamWithoutReadout, deferredStreamWithReadout),
            )

        val component: Camera2CaptureSessionTestComponent =
            DaggerCamera2CaptureSessionTestComponent.builder()
                .fakeCameraPipeModule(FakeCameraPipeModule(context, testCamera))
                .fakeCameraGraphModule(FakeCameraGraphModule(customGraphConfig))
                .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
                .build()

        val streamMap = component.streamMap()
        val s1 = streamMap[streamWithReadout]!!
        val s2 = streamMap[streamWithoutReadout]!!
        val s3 = streamMap[deferredStreamWithReadout]!!

        val outputConfigurations =
            buildOutputConfigurations(
                customGraphConfig,
                streamMap,
                mapOf(s1.id to surface1, s2.id to surface2),
            )

        assertThat(outputConfigurations.all).hasSize(3)
        assertThat(outputConfigurations.deferred).containsKey(s3.id)

        val deferredConfig =
            outputConfigurations.deferred[s3.id]!!.unwrapAs(OutputConfiguration::class.java)!!
        val surfaceToConfig =
            outputConfigurations.all.associateBy {
                it.unwrapAs(OutputConfiguration::class.java)!!.surface
            }

        assertThat(
                Api34Compat.isReadoutTimestampEnabled(
                    surfaceToConfig[surface1]!!.unwrapAs(OutputConfiguration::class.java)!!
                )
            )
            .isTrue()
        assertThat(
                Api34Compat.isReadoutTimestampEnabled(
                    surfaceToConfig[surface2]!!.unwrapAs(OutputConfiguration::class.java)!!
                )
            )
            .isFalse()
        assertThat(Api34Compat.isReadoutTimestampEnabled(deferredConfig)).isTrue()
    }
}

@Singleton
@CameraGraphScope
@Camera2ControllerScope
@Component(
    modules =
        [
            FakeCameraGraphModule::class,
            FakeCameraPipeModule::class,
            Camera2CaptureSessionsModule::class,
            FakeCamera2Module::class,
        ]
)
internal interface Camera2CaptureSessionTestComponent {
    fun graphConfig(): CameraGraph.Config

    fun sessionFactory(): CaptureSessionFactory

    fun streamMap(): StreamGraphImpl
}

/** Utility module for testing the Dagger generated graph with a a reasonable default config. */
@Module(includes = [ThreadConfigModule::class, CameraPipeModule::class])
class FakeCameraPipeModule(
    private val context: Context,
    private val fakeCamera: RobolectricCameras.FakeCamera,
) {
    @Provides fun provideFakeCamera() = fakeCamera

    @Provides @Singleton fun provideFakeCameraPipeConfig() = CameraPipe.Config(context)

    @Provides @Singleton fun provideFakeCameraPipeFlags(config: CameraPipe.Config) = config.flags

    @Provides
    @Singleton
    @DefaultCameraBackend
    fun provideCameraPipeCameraBackend(): CameraBackend =
        FakeCameraBackend(mapOf(fakeCamera.metadata.camera to fakeCamera.metadata))
}

@Module(includes = [SharedCameraGraphModules::class])
class FakeCameraGraphModule(private val customGraphConfig: CameraGraph.Config? = null) {
    @Provides
    @CameraGraphScope
    fun provideFakeCameraMetadata(fakeCamera: RobolectricCameras.FakeCamera) = fakeCamera.metadata

    @Provides
    @CameraGraphScope
    fun provideFakeGraphConfig(fakeCamera: RobolectricCameras.FakeCamera): CameraGraph.Config {
        if (customGraphConfig != null) {
            return customGraphConfig
        }
        val stream = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
        return CameraGraph.Config(camera = fakeCamera.cameraId, streams = listOf(stream))
    }

    @Provides
    @CameraGraphScope
    fun provideFakeCameraController(
        cameraContext: CameraContext,
        cameraGraphConfig: CameraGraph.Config,
    ): CameraController {
        val graphId = CameraGraphId.nextId()
        return CameraControllerSimulator(cameraContext, graphId, cameraGraphConfig, mock())
    }
}

@Module
class FakeCamera2Module {
    @Provides
    @Singleton
    @JvmName("provideFakeCamera2MetadataProvider")
    internal fun provideFakeCamera2MetadataProvider(
        fakeCamera: RobolectricCameras.FakeCamera
    ): Camera2MetadataProvider =
        object : Camera2MetadataProvider {
            override suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata {
                return fakeCamera.metadata
            }

            override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata {
                return fakeCamera.metadata
            }

            override suspend fun getCameraExtensionMetadata(
                cameraId: CameraId,
                extension: Int,
            ): CameraExtensionMetadata {
                throw UnsupportedOperationException("Unused for internal tests")
            }

            override fun awaitCameraExtensionMetadata(
                cameraId: CameraId,
                extension: Int,
            ): CameraExtensionMetadata {
                throw UnsupportedOperationException("Unused for internal tests")
            }

            override fun getSupportedCameraExtensions(cameraId: CameraId): Set<Int> {
                throw UnsupportedOperationException("Unused for internal tests")
            }
        }
}
