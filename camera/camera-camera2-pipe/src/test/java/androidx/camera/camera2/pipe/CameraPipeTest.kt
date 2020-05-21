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
import android.os.Build
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraPipeTest {

    @Test
    fun createCameraPipe() {
        val context = ApplicationProvider.getApplicationContext() as Context
        assertThat(context).isNotNull()

        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        assertThat(cameraPipe).isNotNull()
    }

    @Test
    fun createCameraGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        val cameraGraph = cameraPipe.create(
            CameraGraph.Config(CameraId("0"), listOf())
        )
        assertThat(cameraGraph).isNotNull()
    }
}