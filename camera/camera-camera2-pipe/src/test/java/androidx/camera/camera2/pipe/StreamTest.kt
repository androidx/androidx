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
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamTest {

    private val streamConfig1 = StreamConfig(
        size = Size(640, 480),
        format = StreamFormat.YUV_420_888,
        camera = CameraId("test"),
        type = StreamType.SURFACE
    )

    private val streamConfig2 = StreamConfig(
        size = Size(640, 480),
        format = StreamFormat.YUV_420_888,
        camera = CameraId("test"),
        type = StreamType.SURFACE
    )

    private val streamConfig3 = StreamConfig(
        size = Size(640, 480),
        format = StreamFormat.JPEG,
        camera = CameraId("test"),
        type = StreamType.SURFACE
    )

    @Test
    fun equivalentStreamConfigsAreEqual() {
        assertThat(streamConfig1).isEqualTo(streamConfig2)
        assertThat(streamConfig1).isNotSameInstanceAs(streamConfig2)
    }

    @Test
    fun differentStreamConfigsAreNotEqual() {
        assertThat(streamConfig1).isNotEqualTo(streamConfig3)
        assertThat(streamConfig2).isNotEqualTo(streamConfig3)
    }

    @Test
    fun streamsFromSameConfigAreDifferent() {
        val stream1 = Stream(streamConfig1, StreamId(1))
        val stream2 = Stream(streamConfig1, StreamId(2))

        assertThat(stream1).isNotEqualTo(stream2)
        assertThat(stream1).isNotEqualTo(streamConfig1)
        assertThat(stream2).isNotEqualTo(streamConfig1)

        assertThat(stream1.config).isEqualTo(stream2.config)
    }
}
