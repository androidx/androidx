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

package androidx.camera.core.imagecapture

import android.os.Build
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.processing.Packet
import androidx.camera.testing.ExifUtil.createExif
import androidx.camera.testing.TestImageUtil.createBitmap
import androidx.camera.testing.TestImageUtil.createJpegBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [RgbaImageProxy].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RgbaImageProxyTest {

    @Test
    fun closeImage_invokingMethodsThrowsException() {
        // Arrange.
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val image = RgbaImageProxy(
            Packet.of(
                bitmap,
                createExif(createJpegBytes(WIDTH, HEIGHT)),
                CROP_RECT,
                ROTATION_DEGREES,
                SENSOR_TO_BUFFER,
                CAMERA_CAPTURE_RESULT
            )
        )
        // Act.
        image.close()
        // Assert
        assertThat(hasException { image.close() }).isTrue()
        assertThat(hasException { image.width }).isTrue()
        assertThat(hasException { image.height }).isTrue()
        assertThat(hasException { image.planes }).isTrue()
        assertThat(hasException { image.cropRect }).isTrue()
        assertThat(hasException { image.imageInfo }).isTrue()
        assertThat(hasException { image.image }).isTrue()
        assertThat(hasException { image.setCropRect(CROP_RECT) }).isTrue()
        assertThat(hasException { image.format }).isTrue()
        assertThat(hasException { image.createBitmap() }).isTrue()
    }

    private fun hasException(runnable: Runnable): Boolean {
        try {
            runnable.run()
        } catch (exception: IllegalStateException) {
            return true
        }
        return false
    }
}