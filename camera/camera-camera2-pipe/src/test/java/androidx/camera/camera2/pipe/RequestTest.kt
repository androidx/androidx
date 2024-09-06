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

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.testing.FakeMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class RequestTest {
    private val request =
        Request(
            listOf(StreamId(1)),
            parameters = mapOf(CaptureRequest.EDGE_MODE to CaptureRequest.EDGE_MODE_HIGH_QUALITY),
            extras = mapOf(FakeMetadata.TEST_KEY to 42)
        )

    @Test
    fun requestHasDefaults() {
        val request = Request(listOf(StreamId(1)))

        assertThat(request.parameters).isEmpty()
        assertThat(request.extras).isEmpty()
        assertThat(request.template).isNull()
        assertThat(request.listeners).isEmpty()

        assertThat(request.streams).contains(StreamId(1))
    }

    @Test
    fun requestsWithIdenticalParametersAreNotEqual() {
        val request1 = Request(listOf(StreamId(1)))
        val request2 = Request(listOf(StreamId(1)))

        assertThat(request1).isNotSameInstanceAs(request2)
        assertThat(request1).isNotEqualTo(request2)
    }

    @Test
    fun requestHasNiceLoggingString() {
        val request1 = Request(listOf(StreamId(1)))

        assertThat("$request1").contains("1")
        assertThat("$request1").contains("Request")

        val requestString = request.toStringVerbose()
        assertThat(requestString).contains("42")
        assertThat(requestString).contains("parameters")
        assertThat(requestString).contains("extras")
    }

    @Test
    fun requestHasNiceLoggingString_notEqual() {
        val request1 = Request(listOf(StreamId(1)))
        val request2 = Request(listOf(StreamId(1)))

        assertThat(request1).isNotEqualTo(request2)

        // The Request string should be different if the Requests themselves are different.
        assertThat("$request1").isNotEqualTo("$request2")
    }

    @Test
    fun canReadCaptureParameters() {
        // Check with a valid test key
        assertThat(request[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(request.getOrDefault(FakeMetadata.TEST_KEY, default = 24)).isEqualTo(42)

        // Check with an invalid test key
        assertThat(request[FakeMetadata.TEST_KEY_ABSENT]).isNull()
        assertThat(request.getOrDefault(FakeMetadata.TEST_KEY_ABSENT, default = 24)).isEqualTo(24)

        // Check with a valid test key
        assertThat(request.get(CaptureRequest.EDGE_MODE))
            .isEqualTo(CaptureRequest.EDGE_MODE_HIGH_QUALITY)
        assertThat(request.getOrDefault(CaptureRequest.EDGE_MODE, default = 24))
            .isEqualTo(CaptureRequest.EDGE_MODE_HIGH_QUALITY)

        // Check with an invalid test key
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE)).isNull()
        assertThat(
                request.getOrDefault(
                    CaptureRequest.CONTROL_AE_MODE,
                    default = CaptureRequest.CONTROL_AE_MODE_ON
                )
            )
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }
}
