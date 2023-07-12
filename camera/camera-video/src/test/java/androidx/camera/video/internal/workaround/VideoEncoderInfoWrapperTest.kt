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
import androidx.camera.testing.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(Enclosed::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
object VideoEncoderInfoWrapperTest {

    private const val WIDTH_ALIGNMENT = 2
    private const val HEIGHT_ALIGNMENT = 2
    private val SUPPORTED_WIDTHS = Range.create(WIDTH_ALIGNMENT, 640)
    private val SUPPORTED_HEIGHTS = Range.create(HEIGHT_ALIGNMENT, 480)
    private val VALID_SIZE = Size(320, 240)
    private val SIZE_SHOULD_BE_VALID = Size(1920, 1080)

    private const val WIDTH_4KDCI = 4096
    private const val HEIGHT_4KDCI = 2160
    private val OVERRIDDEN_SUPPORTED_WIDTHS = Range.create(WIDTH_ALIGNMENT, WIDTH_4KDCI)
    private val OVERRIDDEN_SUPPORTED_HEIGHTS = Range.create(HEIGHT_ALIGNMENT, HEIGHT_4KDCI)

    private val baseVideoEncoderInfo by lazy {
        FakeVideoEncoderInfo(
            supportedWidths = SUPPORTED_WIDTHS,
            supportedHeights = SUPPORTED_HEIGHTS,
            widthAlignment = WIDTH_ALIGNMENT,
            heightAlignment = HEIGHT_ALIGNMENT,
        )
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @DoNotInstrument
    @Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
    class ModelWrappingTest(
        private val brand: String,
        private val model: String,
        private val sizeToCheck: Size,
        private val shouldWrapVideoEncoderInfo: Boolean,
    ) {
        companion object {
            private const val NONE_QUIRK_BRAND = "NoneQuirkBrand"
            private const val NONE_QUIRK_MODEL = "NoneQuirkModel"

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(
                name = "brand={0}, model={1}, sizeToCheck={2}, shouldWrapVideoEncoderInfo={3}"
            )
            fun data() = listOf(
                arrayOf(
                    NONE_QUIRK_BRAND,
                    NONE_QUIRK_MODEL,
                    VALID_SIZE,
                    false,
                ),
                arrayOf(
                    NONE_QUIRK_BRAND,
                    NONE_QUIRK_MODEL,
                    SIZE_SHOULD_BE_VALID,
                    true,
                ),
                arrayOf(
                    "Nokia",
                    "Nokia 1",
                    VALID_SIZE,
                    true,
                ),
                arrayOf(
                    "motorola",
                    "moto c",
                    VALID_SIZE,
                    true,
                ),
                // No necessary to test all models.
            )
        }

        @Before
        fun setup() {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
        }

        @Test
        fun from() {
            val videoEncoderInfo = VideoEncoderInfoWrapper.from(baseVideoEncoderInfo, sizeToCheck)

            assertThat(videoEncoderInfo is VideoEncoderInfoWrapper)
                .isEqualTo(shouldWrapVideoEncoderInfo)
        }
    }

    @RunWith(RobolectricTestRunner::class)
    @DoNotInstrument
    @Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
    class WrappingMethodTest {

        @Test
        fun willNotWrapAWrapper() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            val videoEncoderInfo2 = createFakeVideoEncoderInfoWrapper(videoEncoderInfo)
            assertThat(videoEncoderInfo2).isSameInstanceAs(videoEncoderInfo)
        }

        @Test
        fun getSupportedWidths() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThat(videoEncoderInfo.supportedWidths).isEqualTo(OVERRIDDEN_SUPPORTED_WIDTHS)
            assertThat(videoEncoderInfo.supportedHeights).isEqualTo(OVERRIDDEN_SUPPORTED_HEIGHTS)
        }

        @Test
        fun getSupportedHeight() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThat(videoEncoderInfo.supportedWidths).isEqualTo(OVERRIDDEN_SUPPORTED_WIDTHS)
            assertThat(videoEncoderInfo.supportedHeights).isEqualTo(OVERRIDDEN_SUPPORTED_HEIGHTS)
        }

        @Test
        fun getSupportedHeightsFor() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThat(videoEncoderInfo.getSupportedHeightsFor(640))
                .isEqualTo(OVERRIDDEN_SUPPORTED_HEIGHTS)
        }

        @Test
        fun getSupportedHeightFor_invalidWidth() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThrows(IllegalArgumentException::class.java) {
                // Too large
                assertThat(videoEncoderInfo.getSupportedHeightsFor(5000))
            }
            assertThrows(IllegalArgumentException::class.java) {
                // Non alignment
                assertThat(videoEncoderInfo.getSupportedHeightsFor(333))
            }
        }

        @Test
        fun getSupportedWidthsFor() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThat(videoEncoderInfo.getSupportedWidthsFor(480))
                .isEqualTo(OVERRIDDEN_SUPPORTED_WIDTHS)
        }

        @Test
        fun getSupportedWidthFor_invalidHeight() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThrows(IllegalArgumentException::class.java) {
                // Too large
                assertThat(videoEncoderInfo.getSupportedWidthsFor(5000))
            }
            assertThrows(IllegalArgumentException::class.java) {
                // Non alignment
                assertThat(videoEncoderInfo.getSupportedWidthsFor(333))
            }
        }

        @Test
        fun isSizeSupported() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            assertThat(videoEncoderInfo.isSizeSupported(640, 480)).isTrue()
        }

        @Test
        fun isSizeSupported_invalidSize() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper()
            // Too large
            assertThat(videoEncoderInfo.isSizeSupported(5000, 5000)).isFalse()
            // Non alignment
            assertThat(videoEncoderInfo.isSizeSupported(333, 333)).isFalse()
        }

        @Test
        fun isSizeSupported_validSizeIsSupported() {
            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper(
                createFakeVideoEncoderInfo(
                    supportedWidths = Range(16, 640),
                    supportedHeights = Range(16, 480),
                    widthAlignment = 16,
                    heightAlignment = 16,
                ), validSizeToCheck = Size(1920, 1080) // 1080 not align to 16
            )
            assertThat(videoEncoderInfo.isSizeSupported(1920, 1080)).isTrue()
        }

        @Test
        fun isSizeSupported_supportFhdForFhdProblematicDevices() {
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "sm-a032f")

            val videoEncoderInfo = createFakeVideoEncoderInfoWrapper(
                createFakeVideoEncoderInfo(
                    supportedWidths = Range(16, 640),
                    supportedHeights = Range(16, 480),
                    widthAlignment = 16,
                    heightAlignment = 16,
                ), null
            )
            assertThat(videoEncoderInfo.isSizeSupported(1920, 1080)).isTrue()
        }

        private fun createFakeVideoEncoderInfoWrapper(
            videoEncoderInfo: VideoEncoderInfo = createFakeVideoEncoderInfo(),
            validSizeToCheck: Size? = SIZE_SHOULD_BE_VALID,
        ) = VideoEncoderInfoWrapper.from(videoEncoderInfo, validSizeToCheck)

        private fun createFakeVideoEncoderInfo(
            supportedWidths: Range<Int> = SUPPORTED_WIDTHS,
            supportedHeights: Range<Int> = SUPPORTED_HEIGHTS,
            widthAlignment: Int = WIDTH_ALIGNMENT,
            heightAlignment: Int = HEIGHT_ALIGNMENT,
        ) = FakeVideoEncoderInfo(
            supportedWidths = supportedWidths,
            supportedHeights = supportedHeights,
            widthAlignment = widthAlignment,
            heightAlignment = heightAlignment,
        )
    }
}
