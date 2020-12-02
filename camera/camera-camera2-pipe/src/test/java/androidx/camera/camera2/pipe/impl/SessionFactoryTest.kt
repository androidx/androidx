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

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.FakeCameras
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import javax.inject.Singleton

@Singleton
@CameraGraphScope
@Component(
    modules = [
        FakeCameras.FakeCameraGraphModule::class,
        FakeCameras.FakeCameraPipeModule::class
    ]
)
interface CameraSessionTestComponent {
    fun graphConfig(): CameraGraph.Config
    fun sessionFactory(): SessionFactory
    fun streamMap(): StreamMap
}

@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
class SessionFactoryTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    private val cameraId = FakeCameras.create()
    private val testCamera = FakeCameras.open(cameraId)

    @After
    fun teardown() {
        mainLooper.idle()
        FakeCameras.removeAll()
    }

    @Test
    fun canCreateSessionFactoryTestComponent() = runBlockingTest {
        val component: CameraSessionTestComponent = DaggerCameraSessionTestComponent.builder()
            .fakeCameraPipeModule(
                FakeCameras.FakeCameraPipeModule(context, testCamera)
            )
            .build()

        val sessionFactory = component.sessionFactory()
        assertThat(sessionFactory).isNotNull()
    }

    @Test
    fun createCameraCaptureSession() = runBlockingTest {
        val component: CameraSessionTestComponent = DaggerCameraSessionTestComponent.builder()
            .fakeCameraPipeModule(
                FakeCameras.FakeCameraPipeModule(context, testCamera)
            )
            .build()

        val sessionFactory = component.sessionFactory()
        val streamMap = component.streamMap()
        val streamConfig = component.graphConfig().streams.first()
        val stream1 = streamMap.streamConfigMap[streamConfig]!!

        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(
            stream1.size.width,
            stream1.size.height
        )
        val surface = Surface(surfaceTexture)

        val pendingOutputs = sessionFactory.create(
            testCamera.cameraDeviceWrapper,
            mapOf(stream1.id to surface),
            virtualSessionState = VirtualSessionState(
                FakeGraphProcessor(),
                sessionFactory,
                FakeRequestProcessor(),
                this
            )
        )

        assertThat(pendingOutputs).isNotNull()
        assertThat(pendingOutputs).isEmpty()
    }
}