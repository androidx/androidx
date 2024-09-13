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

package androidx.camera.core.processing

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.impl.ExifUtil.createExif
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit test for [Packet]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class PacketTest {

    @Test
    fun createBitmapPacket_verifySizeAndFormat() {
        // Arrange.
        val width = 100
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Act.
        val bitmapPacket =
            Packet.of(
                bitmap,
                createExif(createJpegBytes(40, 30)),
                Rect(0, 0, 100, 200),
                0,
                Matrix(),
                FakeCameraCaptureResult()
            )

        // Assert.
        assertThat(bitmapPacket.size).isEqualTo(Size(100, 200))
        assertThat(bitmapPacket.format).isEqualTo(ImageFormat.FLEX_RGBA_8888)
    }
}
