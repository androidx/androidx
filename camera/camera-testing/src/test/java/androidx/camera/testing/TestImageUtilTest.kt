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

package androidx.camera.testing

import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.Color.BLUE
import android.graphics.Color.GREEN
import android.graphics.Color.RED
import android.graphics.Color.YELLOW
import android.graphics.Rect
import android.os.Build
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.getAverageDiff
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [TestImageUtil]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TestImageUtilTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
    }

    @Test
    fun createTestJpeg_verifyColor() {
        // Act.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val bitmap = decodeByteArray(jpegBytes, 0, jpegBytes.size)
        // Assert.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), RED)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, 640, 240), GREEN)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 241, 640, 480), YELLOW)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(0, 241, 320, 480), BLUE)).isEqualTo(0)
    }
}