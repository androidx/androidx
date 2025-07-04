/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.workaround

import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.JPEG_R
import android.graphics.ImageFormat.RAW_SENSOR
import android.graphics.ImageFormat.UNKNOWN
import android.graphics.ImageFormat.YUV_420_888
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.S)
class PostviewFormatValidatorTest(private val config: TestConfig) {

    @Test
    fun shouldDisableExtensionMode() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)

        val validator = PostviewFormatValidator()
        assertThat(
                validator
                    .getPostviewFormatSelector()
                    .select(config.stillImageFormat, config.supportedPostviewFormats)
            )
            .isEqualTo(
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    config.selectedPostviewFormatAndroidU
                else config.selectedPostviewFormatOthers
            )
    }

    class TestConfig(
        val brand: String,
        val stillImageFormat: Int,
        val supportedPostviewFormats: List<Int>,
        val selectedPostviewFormatAndroidU: Int,
        val selectedPostviewFormatOthers: Int,
    )

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            val supportedPostviewFormats = listOf(YUV_420_888, JPEG)
            return listOf(
                TestConfig(
                    "Samsung",
                    YUV_420_888,
                    supportedPostviewFormats,
                    YUV_420_888,
                    YUV_420_888,
                ),
                TestConfig("Samsung", JPEG, supportedPostviewFormats, JPEG, YUV_420_888),
                TestConfig("Samsung", JPEG_R, supportedPostviewFormats, UNKNOWN, YUV_420_888),
                TestConfig("", YUV_420_888, supportedPostviewFormats, YUV_420_888, YUV_420_888),
                TestConfig("", JPEG, supportedPostviewFormats, JPEG, YUV_420_888),
                TestConfig("", JPEG_R, supportedPostviewFormats, UNKNOWN, YUV_420_888),
                TestConfig("", YUV_420_888, listOf(RAW_SENSOR), UNKNOWN, UNKNOWN),
            )
        }
    }
}
