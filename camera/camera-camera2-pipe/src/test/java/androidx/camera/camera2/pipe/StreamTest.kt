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

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class StreamTest {
    private val streamConfig1 = CameraStream.Config.create(
        size = Size(640, 480),
        format = StreamFormat.YUV_420_888
    )

    private val streamConfig2 = CameraStream.Config.create(
        size = Size(640, 480),
        format = StreamFormat.YUV_420_888
    )

    private val streamConfig3 = CameraStream.Config.create(
        size = Size(640, 480),
        format = StreamFormat.JPEG
    )

    @Test
    fun differentStreamConfigsAreNotEqual() {
        assertThat(streamConfig1).isNotEqualTo(streamConfig3)
        assertThat(streamConfig2).isNotEqualTo(streamConfig3)
    }

    @Test
    fun equivalentStreamConfigsAreNotEqual() {
        assertThat(streamConfig1).isNotEqualTo(streamConfig2)
        assertThat(streamConfig1).isNotSameInstanceAs(streamConfig2)
    }

    @Test
    fun equivalentOutputsAreNotEqual() {
        assertThat(streamConfig1.outputs.single()).isNotEqualTo(streamConfig2.outputs.single())
        assertThat(streamConfig1.outputs.single())
            .isNotSameInstanceAs(streamConfig2.outputs.single())
    }

    @Test
    fun sharedOutputsAreShared() {
        val outputConfig = OutputStream.Config.create(
            size = Size(640, 480),
            format = StreamFormat.YUV_420_888
        )
        val sharedConfig1 = CameraStream.Config.create(outputConfig)
        val sharedConfig2 = CameraStream.Config.create(outputConfig)
        assertThat(sharedConfig1).isNotEqualTo(sharedConfig2)
        assertThat(sharedConfig1.outputs.single()).isEqualTo(sharedConfig2.outputs.single())
        assertThat(sharedConfig1.outputs.single()).isSameInstanceAs(sharedConfig2.outputs.single())
    }
}
