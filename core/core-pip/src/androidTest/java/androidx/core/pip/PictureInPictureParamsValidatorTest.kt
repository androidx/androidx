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

package androidx.core.pip

import android.graphics.Rect
import android.util.Rational
import androidx.core.app.PictureInPictureParamsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PictureInPictureParamsValidatorTest {
    @Test
    fun validate_aspectRatioTooSmall_isCoercedToMin() {
        val params = PictureInPictureParamsCompat.Builder().setAspectRatio(Rational(1, 10)).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.aspectRatio)
            .isEqualTo(PictureInPictureParamsValidator.MIN_ASPECT_RATIO)
    }

    @Test
    fun validate_aspectRatioTooLarge_isCoercedToMax() {
        val params = PictureInPictureParamsCompat.Builder().setAspectRatio(Rational(10, 1)).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.aspectRatio)
            .isEqualTo(PictureInPictureParamsValidator.MAX_ASPECT_RATIO)
    }

    @Test
    fun validate_aspectRatioInRange_remainsUnchanged() {
        val aspectRatio = Rational(4, 3)
        val params = PictureInPictureParamsCompat.Builder().setAspectRatio(aspectRatio).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.aspectRatio).isEqualTo(aspectRatio)
    }

    @Test
    fun validate_nullAspectRatio_isSetToDefault() {
        val params = PictureInPictureParamsCompat.Builder().build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.aspectRatio)
            .isEqualTo(PictureInPictureParamsValidator.DEFAULT_ASPECT_RATIO)
    }

    @Test
    fun validate_sourceRectHintWithValidAspectRatio_isCenterCropped() {
        val params =
            PictureInPictureParamsCompat.Builder()
                .setAspectRatio(Rational(1, 1))
                .setSourceRectHint(Rect(0, 0, 200, 100))
                .build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        // new rect should be 100x100 and centered in 200x100
        assertThat(validatedParams.sourceRectHint).isEqualTo(Rect(50, 0, 150, 100))
    }

    @Test
    fun validate_sourceRectHintWithCoercedAspectRatio_isCenterCroppedToCoercedRatio() {
        val params =
            PictureInPictureParamsCompat.Builder()
                .setAspectRatio(Rational(1, 10)) // gets coerced to 100/239
                .setSourceRectHint(Rect(0, 0, 200, 100))
                .build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        // new width for 100/239 ratio in a 200x100 rect is 42 (rounded).
        // centered in 200x100, new rect is 42x100 at offset 79.
        assertThat(validatedParams.sourceRectHint).isEqualTo(Rect(79, 0, 121, 100))
    }

    @Test
    fun validate_emptySourceRectHint_resultsInNullHint() {
        val params =
            PictureInPictureParamsCompat.Builder().setSourceRectHint(Rect(0, 0, 0, 0)).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.sourceRectHint).isNull()
    }

    @Test
    fun validate_nullSourceRectHint_resultsInNullHint() {
        val params = PictureInPictureParamsCompat.Builder().setSourceRectHint(null).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.sourceRectHint).isNull()
    }

    @Test
    fun validate_otherParams_areCopied() {
        val params = PictureInPictureParamsCompat.Builder().setEnabled(false).build()
        val validatedParams = PictureInPictureParamsValidator.validate(params)
        assertThat(validatedParams.isEnabled).isFalse()
    }
}
