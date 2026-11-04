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

package androidx.camera.core.processing

import android.hardware.DataSpace
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [DefaultSurfaceProcessor]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class DefaultSurfaceProcessorTest {

    @Test
    fun setFactorySupplier_factoryProvidesInstance() {
        // Arrange: create a no-op processor and set it on the factory.
        val noOpProcessor =
            object : SurfaceProcessorInternal {
                override fun onInputSurface(request: SurfaceRequest) {}

                override fun onOutputSurface(surfaceOutput: SurfaceOutput) {}

                override fun release() {}
            }
        DefaultSurfaceProcessor.Factory.setSupplier { noOpProcessor }
        // Assert: new instance returns the no-op processor.
        assertThat(DefaultSurfaceProcessor.Factory.newInstance(DynamicRange.SDR))
            .isSameInstanceAs(noOpProcessor)
    }

    @Config(minSdk = 33)
    @Test
    fun isFullRangeDataSpace_fullRangeDataSpace_returnsTrue() {
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(DataSpace.DATASPACE_BT2020_HLG))
            .isTrue()
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(DataSpace.DATASPACE_BT2020_PQ))
            .isTrue()
    }

    @Config(minSdk = 33)
    @Test
    fun isFullRangeDataSpace_limitedRangeDataSpace_returnsFalse() {
        val bt2020ItuHlg =
            DataSpace.pack(
                DataSpace.STANDARD_BT2020,
                DataSpace.TRANSFER_HLG,
                DataSpace.RANGE_LIMITED,
            )
        val bt2020ItuPq =
            DataSpace.pack(
                DataSpace.STANDARD_BT2020,
                DataSpace.TRANSFER_ST2084,
                DataSpace.RANGE_LIMITED,
            )
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(bt2020ItuHlg)).isFalse()
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(bt2020ItuPq)).isFalse()
    }

    @Config(minSdk = 33)
    @Test
    fun isFullRangeDataSpace_unknownDataSpace_returnsFalse() {
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(DataSpace.DATASPACE_UNKNOWN))
            .isFalse()
    }

    @Config(minSdk = 33)
    @Test
    fun isFullRangeDataSpace_unspecifiedRange_returnsFalse() {
        val bt2020HlgWithoutRange =
            DataSpace.pack(
                DataSpace.STANDARD_BT2020,
                DataSpace.TRANSFER_HLG,
                DataSpace.RANGE_UNSPECIFIED,
            )
        assertThat(DefaultSurfaceProcessor.isFullRangeDataSpace(bt2020HlgWithoutRange)).isFalse()
    }
}
