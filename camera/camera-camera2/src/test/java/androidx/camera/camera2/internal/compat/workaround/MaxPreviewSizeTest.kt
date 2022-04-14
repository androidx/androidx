/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround

import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk
import androidx.camera.core.impl.SurfaceConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val SELECT_RESOLUTION_PRIV = Size(1001, 1000)
private val SELECT_RESOLUTION_YUV = Size(1002, 1000)
private val SELECT_RESOLUTION_JPEG = Size(1003, 1000)

/**
 * Unit tests for [MaxPreviewSize].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MaxPreviewSizeTest {

    private val mMaxPreviewSize = MaxPreviewSize(object : ExtraCroppingQuirk() {
        override fun getVerifiedResolution(configType: SurfaceConfig.ConfigType): Size? {
            return when (configType) {
                SurfaceConfig.ConfigType.YUV -> SELECT_RESOLUTION_YUV
                SurfaceConfig.ConfigType.PRIV -> SELECT_RESOLUTION_PRIV
                SurfaceConfig.ConfigType.JPEG -> SELECT_RESOLUTION_JPEG
                else -> null
            }
        }
    })

    @Test
    fun largerThanDefaultPreviewResolution_returnsPrivResolution() {
        val smallSize = Size(101, 100)
        assertThat(mMaxPreviewSize.getMaxPreviewResolution(smallSize)).isEqualTo(
            SELECT_RESOLUTION_PRIV
        )
    }

    @Test
    fun smallerThanDefaultResolution_returnsDefaultResolution() {
        val largeDefaultSize = Size(10001, 10000)
        assertThat(mMaxPreviewSize.getMaxPreviewResolution(largeDefaultSize)).isEqualTo(
            largeDefaultSize
        )
    }

    @Test
    fun noQuirk_returnsOriginalMaxPreviewResolution() {
        noQuirk_returnsOriginalMaxPreviewResolution(null)
    }

    @Test
    fun noResolutionQuirk_returnsOriginalMaxPreviewResolution() {
        noQuirk_returnsOriginalMaxPreviewResolution(
            Mockito.mock(
                ExtraCroppingQuirk::class.java
            )
        )
    }

    private fun noQuirk_returnsOriginalMaxPreviewResolution(
        quirk: ExtraCroppingQuirk?
    ) {
        val maxPreviewSize = MaxPreviewSize(quirk)
        val result =
            maxPreviewSize.getMaxPreviewResolution(SELECT_RESOLUTION_JPEG)
        assertThat(result).isEqualTo(SELECT_RESOLUTION_JPEG)
    }
}