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

package androidx.core.app

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.util.Rational
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PictureInPictureParamsCompatTest {

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun toPictureInPictureParams_api26() {
        val pipParamsCompat = PictureInPictureParamsCompat(aspectRatio = Rational(21, 9))

        assertThat(pipParamsCompat.toPictureInPictureParams()).isNotNull()
        assertThat(pipParamsCompat.aspectRatio).isEqualTo(Rational(21, 9))
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun toPictureInPictureParams_api33() {
        val pipParamsCompat =
            PictureInPictureParamsCompat(
                aspectRatio = Rational(21, 9),
                sourceRectHint = Rect(0, 0, 100, 100),
                isEnabled = true,
                isSeamlessResizeEnabled = true,
            )

        assertThat(pipParamsCompat.toPictureInPictureParams()).isNotNull()
        assertThat(pipParamsCompat.aspectRatio).isEqualTo(Rational(21, 9))
        assertThat(pipParamsCompat.sourceRectHint).isEqualTo(Rect(0, 0, 100, 100))
        assertThat(pipParamsCompat.isEnabled).isTrue()
        assertThat(pipParamsCompat.isSeamlessResizeEnabled).isTrue()
        assertThat(pipParamsCompat.actions).isEmpty()

        val pipParams: PictureInPictureParams = pipParamsCompat.toPictureInPictureParams()
        assertThat(pipParams.aspectRatio).isEqualTo(Rational(21, 9))
        assertThat(pipParams.sourceRectHint).isEqualTo(Rect(0, 0, 100, 100))
        assertThat(pipParams.isAutoEnterEnabled).isTrue()
        assertThat(pipParams.isSeamlessResizeEnabled).isTrue()
        assertThat(pipParams.actions).isEmpty()
    }
}
