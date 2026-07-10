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

package androidx.xr.arcore.playservices

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import com.google.ar.core.AugmentedImage as ARCore1xAugmentedImage
import com.google.ar.core.Pose as ARCore1xPose
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreAugmentedImageTest {

    private lateinit var mockAugmentedImage: ARCore1xAugmentedImage
    lateinit private var underTest: ArCoreAugmentedImage

    @Before
    fun setUp() {
        mockAugmentedImage = mock<ARCore1xAugmentedImage>()
        underTest = ArCoreAugmentedImage(mockAugmentedImage)
    }

    @Test
    fun centerPose_callsArCore1xAugmentedImageCenterPose() {
        val pose =
            ARCore1xPose(floatArrayOf(1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))
        whenever(mockAugmentedImage.centerPose).thenReturn(pose)

        assertThat(underTest.centerPose).isEqualTo(pose.toRuntimePose())

        verify(mockAugmentedImage).centerPose
    }

    @Test
    fun extents_callsArCore1xAugmentedImageExtent() {
        whenever(mockAugmentedImage.extentX).thenReturn(1.0f)
        whenever(mockAugmentedImage.extentZ).thenReturn(2.0f)

        assertThat(underTest.extents).isEqualTo(FloatSize2d(1.0f, 2.0f))

        verify(mockAugmentedImage).extentX
        verify(mockAugmentedImage).extentZ
    }

    @Test
    fun trackingState_callsArCore1AugmentedImageTrackingState() {
        whenever(mockAugmentedImage.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)

        verify(mockAugmentedImage).trackingState
    }

    @Test
    fun index_callsArCore1AugmentedImageIndex() {
        whenever(mockAugmentedImage.index).thenReturn(0)

        assertThat(underTest.index).isEqualTo(0)

        verify(mockAugmentedImage).index
    }
}
