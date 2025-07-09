/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe.core

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DebugTest {
    @Test
    fun formatParameterMap_empty() {
        val parameters: Map<CaptureRequest.Key<*>, Any> = emptyMap()
        val formatted = Debug.formatParameterMap(parameters)
        val expected = "{}"

        Truth.assertThat(formatted).isEqualTo(expected)
    }

    @Test
    fun formatParameterMap_single_MeteringRectangle() {
        val parameters: Map<CaptureRequest.Key<*>, Any> =
            mapOf(
                CONTROL_AF_REGIONS to
                    MeteringRectangle(5, 20, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
            )
        val formatted = Debug.formatParameterMap(parameters)
        val expected = "{android.control.afRegions=(x:5, y:20, w:100, h:100, wt:1000)}"

        Truth.assertThat(formatted).isEqualTo(expected)
    }

    @Test
    fun formatParameterMap_array_of_MeteringRectangles() {
        val parameters: Map<CaptureRequest.Key<*>, Any> =
            mapOf(
                CONTROL_AF_REGIONS to
                    arrayOf(
                        MeteringRectangle(1, 2, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX),
                        MeteringRectangle(3, 4, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX),
                    )
            )
        val formatted = Debug.formatParameterMap(parameters)
        val expected =
            "{android.control.afRegions=[(x:1, y:2, w:100, h:100, wt:1000), (x:3, y:4, w:100, h:100, wt:1000)]}"

        Truth.assertThat(formatted).isEqualTo(expected)
    }

    @Test
    fun formatParameterMap_empty_array() {
        val parameters: Map<CaptureRequest.Key<*>, Any> =
            mapOf(CONTROL_AF_REGIONS to arrayOf<Any>())
        val formatted = Debug.formatParameterMap(parameters)
        val expected = "{android.control.afRegions=[]}"

        Truth.assertThat(formatted).isEqualTo(expected)
    }
}
