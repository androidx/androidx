/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common.internal

import android.graphics.ImageFormat
import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class DeviceLimitsTest {

    @Test
    fun build_withAllFields_succeeds() {
        val deviceLimits =
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes16by9 = mapOf(ImageFormat.YUV_420_888 to Size(3840, 2160))
                maxUltraOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(8000, 6000))
            }

        assertThat(deviceLimits.maxPreviewSize).isEqualTo(Size(1920, 1080))
        assertThat(deviceLimits.maxRecordSize).isEqualTo(Size(3840, 2160))
        assertThat(deviceLimits.maxOutputSizes)
            .containsExactly(ImageFormat.YUV_420_888, Size(4000, 3000))
        assertThat(deviceLimits.maxUltraOutputSizes)
            .containsExactly(ImageFormat.YUV_420_888, Size(8000, 6000))
    }

    @Test
    fun build_missingMaxPreviewSize_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = emptyMap()
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }

    @Test
    fun build_missingMaxRecordSize_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = emptyMap()
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }

    @Test
    fun build_missingMaxOutputSizes_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes4by3 = emptyMap()
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }

    @Test
    fun build_missingMaxOutputSizes4by3_defaultsToEmpty() {
        val deviceLimits =
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes16by9 = emptyMap()
            }
        assertThat(deviceLimits.maxOutputSizes4by3).isEmpty()
    }

    @Test
    fun build_missingMaxOutputSizes16by9_defaultsToEmpty() {
        val deviceLimits =
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = emptyMap()
            }
        assertThat(deviceLimits.maxOutputSizes16by9).isEmpty()
    }

    @Test
    fun build_zeroSizeInUltraOutputSizes_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = emptyMap()
                maxOutputSizes16by9 = emptyMap()
                maxUltraOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(0, 6000))
            }
        }
    }

    @Test
    fun build_exceedingGlobalMax_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = mapOf(ImageFormat.YUV_420_888 to Size(5000, 4000))
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }

    @Test
    fun build_zeroSizeInOutputSizes_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(0, 3000))
                maxOutputSizes4by3 = emptyMap()
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }

    @Test
    fun build_formatMissingFromGlobalMax_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceLimits.build {
                maxPreviewSize = Size(1920, 1080)
                maxRecordSize = Size(3840, 2160)
                maxOutputSizes = mapOf(ImageFormat.YUV_420_888 to Size(4000, 3000))
                maxOutputSizes4by3 = mapOf(ImageFormat.PRIVATE to Size(1920, 1080))
                maxOutputSizes16by9 = emptyMap()
            }
        }
    }
}
