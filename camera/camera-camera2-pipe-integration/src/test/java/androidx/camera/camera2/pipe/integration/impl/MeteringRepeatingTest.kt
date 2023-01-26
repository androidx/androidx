/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowDisplayManager.removeDisplay
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MeteringRepeatingTest {
    companion object {
        val dummyZeroSize = Size(0, 0)

        val dummySizeListWithout640x480 = listOf(
            Size(4160, 3120),
            Size(1920, 1080),
            Size(1280, 720),
            Size(320, 240),
            Size(240, 144),
        )

        val dummySizeListWith640x480 = listOf(
            Size(4160, 3120),
            Size(1920, 1080),
            Size(1280, 720),
            Size(640, 480),
            Size(320, 240),
        )

        val dummySizeListWithoutSmaller = listOf(
            Size(4160, 3120),
            Size(1920, 1080),
            Size(1280, 720)
        )

        val dummySizeListSmallerThan640x480 = listOf(
            Size(320, 480),
            Size(320, 240),
            Size(240, 144),
        )

        fun getFakeMetadata(sizeList: List<Size>): FakeCameraMetadata {
            val shuffledList = sizeList.shuffled()

            val builder = StreamConfigurationMapBuilder.newBuilder()
            for (size in shuffledList) {
                builder.addOutputSize(size)
            }

            return FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to builder.build(),
                )
            )
        }

        @JvmStatic
        @BeforeClass
        fun classSetUp() {
            DisplayInfoManager.invalidateLazyFields()
        }
    }

    private lateinit var meteringRepeating: MeteringRepeating

    private fun addDisplay(width: Int, height: Int) {
        ShadowDisplayManager.addDisplay(String.format("w%ddp-h%ddp", width, height))
    }

    private fun getMeteringRepeatingAndInitDisplay(outputSizeList: List<Size>): MeteringRepeating {
        for (size in outputSizeList) {
            addDisplay(size.width, size.height)
        }

        return MeteringRepeating.Builder(
            FakeCameraProperties(
                getFakeMetadata(
                    outputSizeList
                )
            ),
            DisplayInfoManager(ApplicationProvider.getApplicationContext())
        ).build()
    }

    @After
    fun tearDown() {
        val displayManager = (ApplicationProvider.getApplicationContext() as Context)
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?

        displayManager?.let {
            for (display in it.displays) {
                removeDisplay(display.displayId)
            }
        }
    }

    @Test
    fun attachedSurfaceResolutionIsLargestLessThan640x480_when640x480NotPresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithout640x480)

        meteringRepeating.updateSuggestedResolution(dummyZeroSize)

        assertEquals(Size(320, 240), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun attachedSurfaceResolutionIs640x480_when640x480PresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWith640x480)

        meteringRepeating.updateSuggestedResolution(dummyZeroSize)

        assertEquals(Size(640, 480), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun attachedSurfaceResolutionFallsBackToMinimum_whenAllOutputSizesLargerThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithoutSmaller)

        meteringRepeating.updateSuggestedResolution(dummyZeroSize)

        assertEquals(Size(1280, 720), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun attachedSurfaceResolutionIsLargestWithinPreviewSize_whenAllOutputSizesLessThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListSmallerThan640x480)

        meteringRepeating.updateSuggestedResolution(dummyZeroSize)

        assertEquals(Size(320, 480), meteringRepeating.attachedSurfaceResolution)
    }
}
