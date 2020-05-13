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
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.camera.camera2.pipe.CameraId
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraGraphImplTest {
    @Test
    fun createCameraGraphImpl() {
        val config = CameraGraph.Config(CameraId("0"), listOf())
        val context = ApplicationProvider.getApplicationContext() as Context

        val impl = CameraGraphImpl(context, config)
        assertThat(impl).isNotNull()
    }
}