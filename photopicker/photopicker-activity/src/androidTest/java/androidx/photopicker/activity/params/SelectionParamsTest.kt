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

package androidx.photopicker.activity.params

import android.os.Build
import android.os.ext.SdkExtensions.getExtensionVersion
import androidx.annotation.RequiresExtension
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SelectionParamsTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testSelectionParams_defaultValues() {
        val params = SelectionParams.Builder().build()

        assertThat(params.minMediaItemResolutionInPixels).isEqualTo(-1L)
        assertThat(params.maxMediaItemResolutionInPixels).isEqualTo(-1L)
        assertThat(params.minVideoDuration).isNull()
        assertThat(params.maxVideoDuration).isNull()
        assertThat(params.maxMediaItemSizeInBytes).isEqualTo(-1L)
        assertThat(params.maxSelectionBatchSizeInBytes).isEqualTo(-1L)
        assertThat(params.mimeTypes).isEmpty()
    }

    @Test
    fun testSelectionParams_builderRetention() {
        val minResolution = 1000L
        val maxResolution = 50000L
        val minDuration = Duration.ofSeconds(5)
        val maxDuration = Duration.ofMinutes(2)
        val maxSize = 10 * 1024 * 1024L
        val maxBatchSize = 50 * 1024 * 1024L
        val mimeTypes = listOf("image/png", "video/mp4")

        val params =
            SelectionParams.Builder()
                .setMinMediaItemResolutionInPixels(minResolution)
                .setMaxMediaItemResolutionInPixels(maxResolution)
                .setMinVideoDuration(minDuration)
                .setMaxVideoDuration(maxDuration)
                .setMaxMediaItemSizeInBytes(maxSize)
                .setMaxSelectionBatchSizeInBytes(maxBatchSize)
                .setMimeTypes(mimeTypes)
                .build()

        assertThat(params.minMediaItemResolutionInPixels).isEqualTo(minResolution)
        assertThat(params.maxMediaItemResolutionInPixels).isEqualTo(maxResolution)
        assertThat(params.minVideoDuration).isEqualTo(minDuration)
        assertThat(params.maxVideoDuration).isEqualTo(maxDuration)
        assertThat(params.maxMediaItemSizeInBytes).isEqualTo(maxSize)
        assertThat(params.maxSelectionBatchSizeInBytes).isEqualTo(maxBatchSize)
        assertThat(params.mimeTypes).isEqualTo(mimeTypes)
    }

    @Test
    fun testSelectionParams_builderClear() {
        val minResolution = 1000L
        val maxResolution = 50000L
        val minDuration = Duration.ofSeconds(5)
        val maxDuration = Duration.ofMinutes(2)
        val maxSize = 10 * 1024 * 1024L
        val maxBatchSize = 50 * 1024 * 1024L
        val mimeTypes = listOf("image/png", "video/mp4")

        val params =
            SelectionParams.Builder()
                .setMinMediaItemResolutionInPixels(minResolution)
                .setMaxMediaItemResolutionInPixels(maxResolution)
                .setMinVideoDuration(minDuration)
                .setMaxVideoDuration(maxDuration)
                .setMaxMediaItemSizeInBytes(maxSize)
                .setMaxSelectionBatchSizeInBytes(maxBatchSize)
                .setMimeTypes(mimeTypes)
                .clearMinMediaItemResolution()
                .clearMaxMediaItemResolution()
                .clearMinVideoDuration()
                .clearMaxVideoDuration()
                .clearMaxMediaItemSize()
                .clearMaxSelectionBatchSize()
                .clearMimeTypes()
                .build()

        assertThat(params.minMediaItemResolutionInPixels).isEqualTo(-1L)
        assertThat(params.maxMediaItemResolutionInPixels).isEqualTo(-1L)
        assertThat(params.minVideoDuration).isNull()
        assertThat(params.maxVideoDuration).isNull()
        assertThat(params.maxMediaItemSizeInBytes).isEqualTo(-1L)
        assertThat(params.maxSelectionBatchSizeInBytes).isEqualTo(-1L)
        assertThat(params.mimeTypes).isEmpty()
    }

    @Test
    fun testSelectionParams_zeroResolution_allowed() {
        val params = SelectionParams.Builder().setMinMediaItemResolutionInPixels(0L).build()
        assertThat(params.minMediaItemResolutionInPixels).isEqualTo(0L)
    }

    @Test
    fun testSelectionParams_negativeResolution_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMinMediaItemResolutionInPixels(-1L)
        }

        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxMediaItemResolutionInPixels(-1L)
        }
    }

    @Test
    fun testSelectionParams_zeroOrNegativeDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMinVideoDuration(Duration.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMinVideoDuration(Duration.ofSeconds(-1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxVideoDuration(Duration.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxVideoDuration(Duration.ofSeconds(-1))
        }
    }

    @Test
    fun testSelectionParams_zeroOrNegativeSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxMediaItemSizeInBytes(0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxMediaItemSizeInBytes(-1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxSelectionBatchSizeInBytes(0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMaxSelectionBatchSizeInBytes(-1L)
        }
    }

    @Test
    fun testSelectionParams_mimeTypes_validation() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMimeTypes(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMimeTypes(listOf("audio/mp3"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder().setMimeTypes(listOf("application/pdf"))
        }
    }

    @Test
    fun testSelectionParams_invalidRange_duration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder()
                .setMinVideoDuration(Duration.ofMinutes(5))
                .setMaxVideoDuration(Duration.ofSeconds(10))
                .build()
        }
    }

    @Test
    fun testSelectionParams_invalidRange_resolution_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectionParams.Builder()
                .setMinMediaItemResolutionInPixels(5000L)
                .setMaxMediaItemResolutionInPixels(100L)
                .build()
        }
    }

    @Test
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testToPhotoPickerSelectionParams() {
        if (getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < 22) {
            return
        }

        val minResolution = 1000L
        val maxResolution = 50000L
        val minDuration = Duration.ofSeconds(5)
        val maxDuration = Duration.ofMinutes(2)
        val maxSize = 10 * 1024 * 1024L
        val maxBatchSize = 50 * 1024 * 1024L
        val mimeTypes = listOf("image/png", "video/mp4")

        val params =
            SelectionParams.Builder()
                .setMinMediaItemResolutionInPixels(minResolution)
                .setMaxMediaItemResolutionInPixels(maxResolution)
                .setMinVideoDuration(minDuration)
                .setMaxVideoDuration(maxDuration)
                .setMaxMediaItemSizeInBytes(maxSize)
                .setMaxSelectionBatchSizeInBytes(maxBatchSize)
                .setMimeTypes(mimeTypes)
                .build()

        val platformParams = params.toPhotoPickerSelectionParams()

        assertThat(platformParams.minMediaItemResolutionInPixels).isEqualTo(minResolution)
        assertThat(platformParams.maxMediaItemResolutionInPixels).isEqualTo(maxResolution)
        assertThat(platformParams.minVideoDuration).isEqualTo(minDuration)
        assertThat(platformParams.maxVideoDuration).isEqualTo(maxDuration)
        assertThat(platformParams.maxMediaItemSizeInBytes).isEqualTo(maxSize)
        assertThat(platformParams.maxSelectionBatchSizeInBytes).isEqualTo(maxBatchSize)
        assertThat(platformParams.mimeTypes).isEqualTo(mimeTypes)
    }
}
