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

package androidx.camera.core.streamsharing

import android.os.Build
import android.util.Size
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.streamsharing.ResolutionMerger.getCroppedSize
import androidx.camera.core.streamsharing.ResolutionMerger.isDoubleCropping
import androidx.camera.core.streamsharing.ResolutionMerger.scoreParentAgainstChild
import androidx.camera.testing.fakes.FakeSupportedOutputSizesSorter
import androidx.camera.testing.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ResolutionMerger].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ResolutionMergerTest {

    companion object {
        val SIZE_1600_900 = Size(1600, 900)
        val SIZE_1800_900 = Size(1800, 900)
        val SIZE_400_300 = Size(400, 300)
        val SIZE_500_400 = Size(500, 400)
        val SIZES = listOf(SIZE_1800_900, SIZE_1600_900, SIZE_500_400, SIZE_400_300)
    }

    @Test(expected = IllegalStateException::class)
    fun mergeChildrenWithSamePriority_throwsException() {
        // Arrange: 2 children with same priority
        val useCaseConfig1 = createUseCaseConfig(1)
        val useCaseConfig2 = createUseCaseConfig(1)
        val fakeSupportedOutputSizesSorter = FakeSupportedOutputSizesSorter(
            mapOf(useCaseConfig1 to SIZES, useCaseConfig2 to SIZES)
        )
        val merger = ResolutionMerger(Size(1600, 900), fakeSupportedOutputSizesSorter, SIZES)
        // Act.
        merger.getMergedResolutions(setOf(useCaseConfig1, useCaseConfig2))
    }

    @Test
    fun getMergedResolutions_prioritizesHighPriorityChild() {
        // Arrange: 2 children with different priority.
        val sensorSize = Size(1600, 900)
        val useCaseConfig1 = createUseCaseConfig(1)
        val useCaseSizes1 = listOf(SIZE_1800_900, SIZE_1600_900, SIZE_500_400, SIZE_400_300)
        val useCaseConfig2 = createUseCaseConfig(2)
        val useCaseSizes2 = listOf(SIZE_400_300, SIZE_500_400, SIZE_1800_900, SIZE_1600_900)
        val fakeSupportedOutputSizesSorter = FakeSupportedOutputSizesSorter(
            mapOf(useCaseConfig1 to useCaseSizes1, useCaseConfig2 to useCaseSizes2)
        )
        val merger = ResolutionMerger(sensorSize, fakeSupportedOutputSizesSorter, SIZES)

        // Act.
        val result = merger.getMergedResolutions(setOf(useCaseConfig1, useCaseConfig2))

        // Assert:
        assertThat(result).containsExactly(
            SIZE_1600_900, // works for both children so it's the first.
            SIZE_400_300, // 1st for the child2 but require upscaling for child1.
            SIZE_500_400, // 2st for the child2 but require upscaling for child1
            SIZE_1800_900 // 3rd for the child2 but require double cropping for child1
        ).inOrder()
    }

    @Test
    fun getCroppedSize_returnsTheLargestSize() {
        // Arrange.
        val parent = Size(16, 9)
        val sensor = Size(1600, 1200)
        // Act.
        val croppedSize = getCroppedSize(sensor, parent)
        // Assert.
        assertThat(croppedSize).isEqualTo(SIZE_1600_900)
    }

    @Test
    fun largeParentSize_matchesTheLargestChild() {
        // Arrange.
        val parent = Size(4000, 3000)
        val sensor = Size(8000, 6000)
        // Act.
        val score = scoreParentAgainstChild(parent, sensor, SIZES)
        // Assert: Score is the index of SIZE_1800_900.
        assertThat(SIZES[score]).isEqualTo(SIZE_1800_900)
    }

    @Test
    fun smallParentSize_matchesTheChildWithNoUpscaling() {
        // Arrange.
        val parent = Size(400, 300)
        val sensor = Size(4000, 3000)
        // Act.
        val score = scoreParentAgainstChild(parent, sensor, SIZES)
        // Assert: Score is the index of SIZE_400_300 because others requires upscaling.
        assertThat(SIZES[score]).isEqualTo(SIZE_400_300)
    }

    @Test
    fun squareParentSize_scoreIsMaxValue() {
        // Arrange.
        val parent = Size(1000, 1000)
        val sensor = Size(4000, 3000)
        // Act.
        val score = scoreParentAgainstChild(parent, sensor, SIZES)
        // Assert: Score is Integer.MAX_VALUE because all sizes require double cropping.
        assertThat(score).isEqualTo(Integer.MAX_VALUE)
    }

    @Test
    fun narrowerParentSize_skipsDoubleCropping() {
        // Arrange.
        val parent = Size(3800, 3000)
        val sensor = Size(4000, 3000)
        // Act.
        val score = scoreParentAgainstChild(parent, sensor, SIZES)
        // Assert: Score is SIZE_500_400 because all other require double cropping.
        assertThat(SIZES[score]).isEqualTo(SIZE_500_400)
    }

    @Test
    fun cropWiderThanOriginal() {
        assertThat(getCroppedSize(SIZE_400_300, SIZE_1600_900)).isEqualTo(Size(400, 225))
    }

    @Test
    fun cropNarrowerThanOriginal() {
        assertThat(getCroppedSize(SIZE_1600_900, SIZE_400_300)).isEqualTo(Size(1200, 900))
    }

    @Test
    fun parentEqualsSensor_noDoubleCropping() {
        assertThat(isDoubleCropping(SIZE_400_300, SIZE_1600_900, SIZE_1600_900)).isFalse()
    }

    @Test
    fun parentWiderThanSensor_childNarrowerThanSensor_isDoubleCropping() {
        assertThat(isDoubleCropping(SIZE_1600_900, SIZE_500_400, SIZE_400_300)).isTrue()
    }

    @Test
    fun childWiderThanSensor_parentNarrowerThanSensor_isDoubleCropping() {
        assertThat(isDoubleCropping(SIZE_500_400, SIZE_1600_900, SIZE_400_300)).isTrue()
    }

    @Test
    fun parentWiderThanSensorButNarrowerThanChild_noDoubleCropping() {
        assertThat(isDoubleCropping(SIZE_1800_900, SIZE_1600_900, SIZE_400_300)).isFalse()
    }

    private fun createUseCaseConfig(priority: Int): UseCaseConfig<*> {
        val config = FakeUseCaseConfig.Builder().mutableConfig
        config.insertOption(UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY, priority)
        return FakeUseCaseConfig.Builder(config).useCaseConfig
    }
}