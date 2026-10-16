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

package androidx.photopicker.activity.params

import androidx.photopicker.activity.PickVisualMedia
import androidx.photopicker.activity.params.UiCustomizationParams.Companion.ASPECT_RATIO_PORTRAIT_9_16
import androidx.photopicker.activity.params.UiCustomizationParams.Companion.ASPECT_RATIO_SQUARE_1_1
import androidx.photopicker.activity.params.UiCustomizationParams.Companion.ASPECT_RATIO_UNDEFINED
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class UiCustomizationParamsTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testUiCustomizationParams_defaultValues() {
        val params = UiCustomizationParams.Builder().build()
        assertThat(params.aspectRatio).isEqualTo(ASPECT_RATIO_UNDEFINED)
    }

    @Test
    fun testUiCustomizationParams_resetAspectRatio() {
        val builder = UiCustomizationParams.Builder()
        builder.setAspectRatio(ASPECT_RATIO_SQUARE_1_1)
        assertThat(builder.build().aspectRatio).isEqualTo(ASPECT_RATIO_SQUARE_1_1)

        builder.setAspectRatio(ASPECT_RATIO_UNDEFINED)
        assertThat(builder.build().aspectRatio).isEqualTo(ASPECT_RATIO_UNDEFINED)
    }

    @Test
    fun testUiCustomizationParams_squareAspectRatio() {
        val params = UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_SQUARE_1_1).build()
        assertThat(params.aspectRatio).isEqualTo(ASPECT_RATIO_SQUARE_1_1)
    }

    @Test
    fun testUiCustomizationParams_portraitAspectRatio() {
        val params =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_PORTRAIT_9_16).build()
        assertThat(params.aspectRatio).isEqualTo(ASPECT_RATIO_PORTRAIT_9_16)
    }

    @Test
    fun testCreateFallbackUiParamBundle() {
        val defaultParams = UiCustomizationParams.Builder().build()
        assertThat(
                defaultParams
                    .createFallbackUiParamBundle()
                    .containsKey(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO
                    )
            )
            .isFalse()

        val squareParams =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_SQUARE_1_1).build()
        assertThat(
                squareParams
                    .createFallbackUiParamBundle()
                    .getInt(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO
                    )
            )
            .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_SQUARE_1_1)

        val portraitParams =
            UiCustomizationParams.Builder().setAspectRatio(ASPECT_RATIO_PORTRAIT_9_16).build()
        assertThat(
                portraitParams
                    .createFallbackUiParamBundle()
                    .getInt(
                        PickVisualMedia
                            .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO
                    )
            )
            .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16)
    }
}
