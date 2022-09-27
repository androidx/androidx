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

package androidx.camera.video.internal.workaround

import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.video.internal.encoder.FakeVideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoEncoderInfoWrapperTest(
    private val brand: String,
    private val model: String,
    private val sizeToCheck: Size,
    private val expectedSupportedWidths: Range<Int>,
    private val expectedSupportedHeights: Range<Int>,
) {

    companion object {
        private const val WIDTH_ALIGNMENT = 2
        private const val HEIGHT_ALIGNMENT = 2
        private val SUPPORTED_WIDTHS = Range.create(WIDTH_ALIGNMENT, 640)
        private val SUPPORTED_HEIGHTS = Range.create(HEIGHT_ALIGNMENT, 480)
        private val VALID_SIZE = Size(320, 240)
        private val INVALID_SIZE = Size(1920, 1080)

        private const val WIDTH_4KDCI = 4096
        private const val HEIGHT_4KDCI = 2160
        private val OVERRIDE_SUPPORTED_WIDTHS = Range.create(WIDTH_ALIGNMENT, WIDTH_4KDCI)
        private val OVERRIDE_SUPPORTED_HEIGHTS = Range.create(HEIGHT_ALIGNMENT, HEIGHT_4KDCI)
        private const val NONE_QUIRK_BRAND = "NoneQuirkBrand"
        private const val NONE_QUIRK_MODEL = "NoneQuirkModel"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "brand={0}, model={1}, sizeToCheck={2}" +
                ", expectedSupportedWidths={3}, expectedSupportedHeights={4}"
        )
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(
                arrayOf(
                    NONE_QUIRK_BRAND,
                    NONE_QUIRK_MODEL,
                    VALID_SIZE,
                    SUPPORTED_WIDTHS,
                    SUPPORTED_HEIGHTS,
                )
            )
            add(
                arrayOf(
                    NONE_QUIRK_BRAND,
                    NONE_QUIRK_MODEL,
                    INVALID_SIZE,
                    OVERRIDE_SUPPORTED_WIDTHS,
                    OVERRIDE_SUPPORTED_HEIGHTS,
                )
            )
            add(
                arrayOf(
                    "Nokia",
                    "Nokia 1",
                    VALID_SIZE,
                    OVERRIDE_SUPPORTED_WIDTHS,
                    OVERRIDE_SUPPORTED_HEIGHTS,
                )
            )
            add(
                arrayOf(
                    "motorola",
                    "moto c",
                    VALID_SIZE,
                    OVERRIDE_SUPPORTED_WIDTHS,
                    OVERRIDE_SUPPORTED_HEIGHTS,
                )
            )
            // No necessary to test all models.
        }
    }

    private val baseVideoEncoderInfo = FakeVideoEncoderInfo(
        _supportedWidths = SUPPORTED_WIDTHS,
        _supportedHeights = SUPPORTED_HEIGHTS,
        _widthAlignment = WIDTH_ALIGNMENT,
        _heightAlignment = HEIGHT_ALIGNMENT,
    )

    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun from() {
        val videoEncoderInfo = VideoEncoderInfoWrapper.from(baseVideoEncoderInfo, sizeToCheck)

        assertThat(videoEncoderInfo.supportedWidths).isEqualTo(expectedSupportedWidths)
        assertThat(videoEncoderInfo.supportedHeights).isEqualTo(expectedSupportedHeights)
    }
}
