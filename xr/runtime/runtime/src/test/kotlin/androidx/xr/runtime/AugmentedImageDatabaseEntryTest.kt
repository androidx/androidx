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

package androidx.xr.runtime

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AugmentedImageDatabaseEntryTest {

    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun constructor_augmentedImageDatabaseEntry_returnsDefaultValues() {
        val entry = AugmentedImageDatabaseEntry(bitmap)
        assertThat(entry).isNotNull()
        assertThat(entry.mode).isEqualTo(AugmentedImageDatabaseEntryMode.DYNAMIC)
        assertThat(entry.bitmap.width).isEqualTo(1)
        assertThat(entry.bitmap.height).isEqualTo(1)
        assertThat(entry.bitmap.config).isEqualTo(Bitmap.Config.ARGB_8888)
        assertThat(entry.widthInMeters).isEqualTo(0f)
    }

    @Test
    fun equals_twoAugmentedImageDatabasesEntriesHashCode_returnsFalse() {
        val entry = AugmentedImageDatabaseEntry(bitmap)
        val entry2 =
            AugmentedImageDatabaseEntry(
                bitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.ARGB_8888),
                mode = AugmentedImageDatabaseEntryMode.STATIC,
                widthInMeters = 0f,
            )

        assertThat(entry).isEqualTo(entry)
        assertThat(entry).isNotEqualTo(entry2)

        val hash1 = entry.hashCode()
        val hash2 = entry2.hashCode()
        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun equals_augmentedImageDatabaseEntryCopy_returnsTrue() {
        val entry = AugmentedImageDatabaseEntry(bitmap)
        val copiedEntry = entry.copy()

        assertThat(copiedEntry.mode).isEqualTo(entry.mode)
        assertThat(copiedEntry.bitmap).isEqualTo(entry.bitmap)
        assertThat(copiedEntry.widthInMeters).isEqualTo(entry.widthInMeters)
    }
}
