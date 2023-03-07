/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal

import android.graphics.ImageFormat
import android.os.Build
import android.util.Pair
import android.util.Size
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

private val DEFAULT_SUPPORTED_SIZES = listOf(
    Size(4032, 3024), // 4:3
    Size(3840, 2160), // 16:9
    Size(1920, 1440), // 4:3
    Size(1920, 1080), // 16:9
    Size(1280, 960), // 4:3
    Size(1280, 720), // 16:9
    Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
    Size(800, 450), // 16:9
    Size(640, 480), // 4:3
    Size(320, 240), // 4:3
    Size(320, 180), // 16:9
    Size(256, 144) // 16:9
)

/**
 * Unit tests for [SupportedOutputSizesSorter].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedOutputSizesSorterTest {

    @Test
    fun canSelectCustomOrderedResolutions() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val cameraInfoInternal = FakeCameraInfoInternal().apply {
            setSupportedResolutions(imageFormat, DEFAULT_SUPPORTED_SIZES)
        }
        val supportedOutputSizesSorter = SupportedOutputSizesSorter(cameraInfoInternal)
        // Sets up the custom ordered resolutions
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.IMAGE_CAPTURE, imageFormat).apply {
                setCustomOrderedResolutions(
                    listOf(
                        Size(1920, 1080),
                        Size(720, 480),
                        Size(640, 480)
                    )
                )
            }.useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert
        assertThat(sortedResult).containsExactlyElementsIn(
            listOf(
                Size(1920, 1080),
                Size(720, 480),
                Size(640, 480)
            )
        ).inOrder()
    }

    @Test
    fun canSelectCustomSupportedResolutions() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val cameraInfoInternal = FakeCameraInfoInternal().apply {
            setSupportedResolutions(imageFormat, DEFAULT_SUPPORTED_SIZES)
        }
        val supportedOutputSizesSorter = SupportedOutputSizesSorter(cameraInfoInternal)
        // Sets up the custom supported resolutions
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.IMAGE_CAPTURE, imageFormat).apply {
                setSupportedResolutions(
                    listOf(
                        Pair.create(
                            imageFormat, arrayOf(
                                Size(1920, 1080),
                                Size(720, 480),
                                Size(640, 480)
                            )
                        )
                    )
                )
            }.useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert
        assertThat(sortedResult).containsExactlyElementsIn(
            listOf(
                Size(1920, 1080),
                Size(720, 480),
                Size(640, 480)
            )
        ).inOrder()
    }
}