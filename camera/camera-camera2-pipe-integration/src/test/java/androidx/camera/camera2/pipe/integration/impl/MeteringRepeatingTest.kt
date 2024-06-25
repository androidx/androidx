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
import androidx.camera.core.impl.StreamSpec
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MeteringRepeatingTest {
    companion object {
        val dummyZeroSizeStreamSpec = StreamSpec.builder(Size(0, 0)).build()

        val dummySizeListWithout640x480 =
            listOf(
                Size(4160, 3120),
                Size(1920, 1080),
                Size(1280, 720),
                Size(320, 240),
                Size(240, 144),
            )

        val dummySizeListWith640x480 =
            listOf(
                Size(4160, 3120),
                Size(1920, 1080),
                Size(1280, 720),
                Size(640, 480),
                Size(320, 240),
            )

        val dummySizeListWithoutSmaller =
            listOf(Size(4160, 3120), Size(1920, 1080), Size(1280, 720))

        val dummySizeListSmallerThan640x480 =
            listOf(
                Size(320, 480),
                Size(320, 240),
                Size(240, 144),
            )

        val dummySizeListNotWithin320x240And640x480 =
            listOf(
                Size(4160, 3120),
                Size(1920, 1080),
                Size(1280, 720),
                Size(240, 144),
            )

        val dummySizeListSmallerThan320x240 =
            listOf(
                Size(240, 144),
                Size(192, 144),
                Size(160, 120),
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

        if (Build.VERSION.SDK_INT in 26..28) {
            /**
             * There seems to be a Robolectric bug where removeDisplay(0) call without adding other
             * displays and then calling [DisplayManager.getDisplays] first will make subsequent
             * [DisplayManager.getDisplays] calls fail.
             */
            ((ApplicationProvider.getApplicationContext() as Context).getSystemService(
                    Context.DISPLAY_SERVICE
                ) as DisplayManager?)
                ?.displays
        }

        removeDisplay(0)

        return MeteringRepeating.Builder(
                FakeCameraProperties(getFakeMetadata(outputSizeList)),
                DisplayInfoManager(ApplicationProvider.getApplicationContext())
            )
            .build()
    }

    @After
    fun tearDown() {
        val displayManager =
            (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager?

        displayManager?.let {
            for (display in it.displays) {
                removeDisplay(display.displayId)
            }
        }
    }

    @Test
    fun surfaceResolutionIsLargestLessThan640x480_when640x480NotPresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithout640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(320, 240), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIs640x480_when640x480PresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWith640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(640, 480), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionFallsBackToMinimum_whenAllOutputSizesLargerThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithoutSmaller)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(1280, 720), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIsLargestWithinPreviewSize_whenAllOutputSizesLessThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListSmallerThan640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(320, 480), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIsLargestLessThan640x480_whenSizesOutside320x240And640x480() {
        meteringRepeating =
            getMeteringRepeatingAndInitDisplay(dummySizeListNotWithin320x240And640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(240, 144))
    }

    @Test
    fun surfaceResolutionIsLargerThan320x240_whenHuaweiMate9AndSizesOutside320x240And640x480() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Huawei")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "mha-l29")

        meteringRepeating =
            getMeteringRepeatingAndInitDisplay(dummySizeListNotWithin320x240And640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun surfaceResolutionFallsBackToLargest_whenHuaweiMate9AndAllOutputSizesLessThan320x240() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Huawei")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "mha-l29")

        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListSmallerThan320x240)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(240, 144))
    }
}
