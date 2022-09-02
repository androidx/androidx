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

import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.processing.Packet
import androidx.camera.testing.ExifUtil.createExif
import androidx.camera.testing.TestImageUtil.createBitmap
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.getAverageDiff
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [JpegBytes2CroppedBitmap].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Bitmap2JpegBytesTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
    }

    private val processor = Bitmap2JpegBytes()

    @Test
    fun process_verifyOutput() {
        // Arrange.
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val inputPacket = Packet.of(
            bitmap,
            createExif(createJpegBytes(WIDTH, HEIGHT)),
            Rect(0, 0, WIDTH, HEIGHT),
            90,
            Matrix()
        )
        val input = Bitmap2JpegBytes.In.of(inputPacket, 100)

        // Act.
        val output = processor.process(input)

        // Assert
        val restoredBitmap = decodeByteArray(output.data, 0, output.data.size)
        assertThat(getAverageDiff(bitmap, restoredBitmap)).isEqualTo(0)
    }
}