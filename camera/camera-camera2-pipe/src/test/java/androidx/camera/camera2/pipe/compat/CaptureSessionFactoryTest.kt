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

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.Camera2CameraGraphModules
import androidx.camera.camera2.pipe.config.Camera2CameraPipeModules
import androidx.camera.camera2.pipe.config.CameraGraphModules
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ThreadConfigModule
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import javax.inject.Singleton

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
internal class CaptureSessionFactoryTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }

    @Test
    fun canCreateSessionFactoryTestComponent() = runBlockingTest {
        val component: CameraSessionTestComponent = DaggerCameraSessionTestComponent.builder()
            .fakeCameraPipeModule(FakeCameraPipeModule(context, testCamera))
            .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
            .build()

        val sessionFactory = component.sessionFactory()
        assertThat(sessionFactory).isNotNull()
    }

    @Test
    fun createCameraCaptureSession() = runBlockingTest {
        val component: CameraSessionTestComponent = DaggerCameraSessionTestComponent.builder()
            .fakeCameraPipeModule(FakeCameraPipeModule(context, testCamera))
            .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
            .build()

        val sessionFactory = component.sessionFactory()
        val streamMap = component.streamMap()
        val cameraStreamConfig = component.graphConfig().streams.first()
        val stream1 = streamMap[cameraStreamConfig]!!
        val stream1Output = stream1.outputs.first()

        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(
            stream1Output.size.width,
            stream1Output.size.height
        )
        val surface = Surface(surfaceTexture)

        val pendingOutputs = sessionFactory.create(
            AndroidCameraDevice(
                testCamera.metadata,
                testCamera.cameraDevice,
                testCamera.cameraId
            ),
            mapOf(stream1.id to surface),
            virtualSessionState = VirtualSessionState(
                FakeGraphProcessor(),
                sessionFactory,
                object : Camera2RequestProcessorFactory {
                    override fun create(
                        session: CameraCaptureSessionWrapper,
                        surfaceMap: Map<StreamId, Surface>
                    ): RequestProcessor = FakeRequestProcessor()
                },
                this
            )
        )

        assertThat(pendingOutputs).isNotNull()
        assertThat(pendingOutputs).isEmpty()
    }
}

@Singleton
@CameraGraphScope
@Component(
    modules = [
        FakeCameraGraphModule::class,
        FakeCameraPipeModule::class
    ]
)
internal interface CameraSessionTestComponent {
    fun graphConfig(): CameraGraph.Config
    fun sessionFactory(): CaptureSessionFactory
    fun streamMap(): Camera2StreamGraph
}

/**
 * Utility module for testing the Dagger generated graph with a a reasonable default config.
 */
@Module(includes = [ThreadConfigModule::class, Camera2CameraPipeModules::class])
class FakeCameraPipeModule(
    private val context: Context,
    private val fakeCamera: RobolectricCameras.FakeCamera
) {
    @Provides
    fun provideFakeCamera() = fakeCamera

    @Provides
    @Singleton
    fun provideFakeCameraPipeConfig() = CameraPipe.Config(context)
}

@Module(includes = [CameraGraphModules::class, Camera2CameraGraphModules::class])
class FakeCameraGraphModule {
    @Provides
    @CameraGraphScope
    fun provideFakeGraphConfig(fakeCamera: RobolectricCameras.FakeCamera): CameraGraph.Config {
        val stream = CameraStream.Config.create(
            Size(640, 480),
            StreamFormat.YUV_420_888
        )
        return CameraGraph.Config(
            camera = fakeCamera.cameraId,
            streams = listOf(stream),
        )
    }
}