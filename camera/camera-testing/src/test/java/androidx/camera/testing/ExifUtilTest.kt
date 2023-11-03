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

import android.os.Build
import androidx.camera.core.impl.utils.Exif.createFromInputStream
import androidx.camera.testing.impl.ExifUtil.updateExif
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ExifUtil]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ExifUtilTest {

    companion object {
        private const val DESCRIPTION = "description"
    }

    @Test
    fun createJpegWithExif_verifyExif() {
        // Arrange: create a JPEG file.
        val jpeg = createJpegBytes(640, 480)
        // Act: update the description tag.
        val jpegWithExif = updateExif(jpeg) {
            it.description = DESCRIPTION
        }
        // Assert: the description tag has been updated.
        val exif = createFromInputStream(ByteArrayInputStream(jpegWithExif))
        assertThat(exif.description).isEqualTo(DESCRIPTION)
    }
}
