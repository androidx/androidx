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

@file:RequiresApi(21)

package androidx.camera.camera2.internal.compat.workaround

import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk
import androidx.camera.core.impl.SurfaceConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val RESOLUTION_1 = Size(101, 100)
private val RESOLUTION_2 = Size(102, 100)

private val SELECT_RESOLUTION_PRIV = Size(1001, 1000)
private val SELECT_RESOLUTION_YUV = Size(1002, 1000)
private val SELECT_RESOLUTION_JPEG = Size(1003, 1000)

private val SUPPORTED_RESOLUTIONS = listOf(RESOLUTION_1, RESOLUTION_2)

/**
 * Unit test for [ResolutionCorrector].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ResolutionCorrectorTest {

    private val mResolutionCorrector = ResolutionCorrector(object : ExtraCroppingQuirk() {
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
    fun hasPrivResolution_prioritized() {
        hasResolution_prioritized(SurfaceConfig.ConfigType.PRIV, SELECT_RESOLUTION_PRIV)
    }

    @Test
    fun hasYuvResolution_prioritized() {
        hasResolution_prioritized(SurfaceConfig.ConfigType.YUV, SELECT_RESOLUTION_YUV)
    }

    @Test
    fun hasJpegResolution_prioritized() {
        hasResolution_prioritized(SurfaceConfig.ConfigType.JPEG, SELECT_RESOLUTION_JPEG)
    }

    private fun hasResolution_prioritized(
        configType: SurfaceConfig.ConfigType,
        resolution: Size
    ) {
        val resolutions: MutableList<Size> = ArrayList<Size>(SUPPORTED_RESOLUTIONS)
        resolutions.add(resolution)
        Truth.assertThat(mResolutionCorrector.insertOrPrioritize(configType, resolutions))
            .containsExactly(resolution, RESOLUTION_1, RESOLUTION_2).inOrder()
    }

    @Test
    fun noPrivResolution_inserted() {
        noResolution_inserted(SurfaceConfig.ConfigType.PRIV, SELECT_RESOLUTION_PRIV)
    }

    @Test
    fun noYuvResolution_inserted() {
        noResolution_inserted(SurfaceConfig.ConfigType.YUV, SELECT_RESOLUTION_YUV)
    }

    @Test
    fun noJpegResolution_inserted() {
        noResolution_inserted(SurfaceConfig.ConfigType.JPEG, SELECT_RESOLUTION_JPEG)
    }

    private fun noResolution_inserted(
        configType: SurfaceConfig.ConfigType,
        resolution: Size
    ) {
        Truth.assertThat(mResolutionCorrector.insertOrPrioritize(configType, SUPPORTED_RESOLUTIONS))
            .containsExactly(resolution, RESOLUTION_1, RESOLUTION_2).inOrder()
    }

    @Test
    fun noQuirk_returnsOriginalSupportedResolutions() {
        noQuirk_returnsOriginalSupportedResolutions(null)
    }

    @Test
    fun noResolution_returnsOriginalSupportedResolutions() {
        noQuirk_returnsOriginalSupportedResolutions(getEmptyQuirk())
    }

    private fun noQuirk_returnsOriginalSupportedResolutions(
        quirk: ExtraCroppingQuirk?
    ) {
        val resolutionCorrector = ResolutionCorrector(quirk)
        val result = resolutionCorrector.insertOrPrioritize(
            SurfaceConfig.ConfigType.PRIV,
            SUPPORTED_RESOLUTIONS
        )
        Truth.assertThat(result).containsExactlyElementsIn(SUPPORTED_RESOLUTIONS)
    }

    private fun getEmptyQuirk(): ExtraCroppingQuirk? {
        return Mockito.mock(ExtraCroppingQuirk::class.java)
    }
}
