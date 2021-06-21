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

package androidx.camera.camera2.pipe.config

import android.content.Context
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraPipeComponentTest {
    private val fakeCameraId = RobolectricCameras.create()

    @Test
    fun createCameraPipeComponent() {
        val context = ApplicationProvider.getApplicationContext() as Context
        assertThat(context).isNotNull()

        val builder = DaggerCameraPipeComponent.builder()
        val config = CameraPipe.Config(context)
        val module = CameraPipeConfigModule(config)
        builder.cameraPipeConfigModule(module)
        builder.threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
        val component = builder.build()
        assertThat(component).isNotNull()
    }

    @Test
    fun createCameraGraphComponent() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val component = DaggerCameraPipeComponent.builder()
            .cameraPipeConfigModule(CameraPipeConfigModule(CameraPipe.Config(context)))
            .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
            .build()

        val cameraId = fakeCameraId
        val config = CameraGraph.Config(
            camera = cameraId,
            streams = listOf(),
        )
        val module = CameraGraphConfigModule(config)
        val builder = component.cameraGraphComponentBuilder()
        builder.cameraGraphConfigModule(module)
        val graphComponent = builder.build()
        assertThat(graphComponent).isNotNull()
    }

    @Test
    fun createCameraGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val component = DaggerCameraPipeComponent.builder()
            .cameraPipeConfigModule(CameraPipeConfigModule(CameraPipe.Config(context)))
            .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
            .build()

        val graphComponent = component.cameraGraphComponentBuilder()
            .cameraGraphConfigModule(
                CameraGraphConfigModule(
                    CameraGraph.Config(
                        camera = fakeCameraId,
                        streams = listOf(),
                    )
                )
            )
            .build()

        val graph = graphComponent.cameraGraph()
        assertThat(graph).isNotNull()
    }
}