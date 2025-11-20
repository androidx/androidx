/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ExifUtilDeviceTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private val EXIF_GAINMAP_PATTERNS =
            listOf(
                "xmlns:hdrgm=\"http://ns.adobe.com/hdr-gain-map/",
                "hdrgm:Version=",
                "Item:Semantic=\"GainMap\"",
            )
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun createJpegrWithExif_verifyExif() {
        // Arrange.
        val jpegBytes = TestImageUtil.createJpegrBytes(WIDTH, HEIGHT)

        // Act.
        val exif = ExifUtil.createExif(jpegBytes)

        // Assert.
        val exifMetadata = exif.metadata
        assertThat(exifMetadata).isNotNull()
        for (pattern in EXIF_GAINMAP_PATTERNS) {
            assertThat(exifMetadata).contains(pattern)
        }
    }
}
