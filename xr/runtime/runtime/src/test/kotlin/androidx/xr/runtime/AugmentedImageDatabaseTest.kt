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
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AugmentedImageDatabaseTest {

    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun constructor_augmentedImageDatabase_returnsEmpty() {
        val imageDatabase = AugmentedImageDatabase()

        assertThat(imageDatabase.entries).isNotNull()
        assertThat(imageDatabase.entries).isEmpty()
    }

    @Test
    fun addAugmentedImageDatabaseEntry_returns0AndElementAdded() {
        val imageDatabase = AugmentedImageDatabase()
        val result =
            imageDatabase.addAugmentedImageDatabaseEntry(
                AugmentedImageDatabaseEntryMode.DYNAMIC,
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )

        assertThat(imageDatabase.entries.size).isEqualTo(1)
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun addAugmentedImageDatabaseEntry_returns0AndNotEqualToDifferentEntry() {
        val imageDatabase = AugmentedImageDatabase()
        val result =
            imageDatabase.addAugmentedImageDatabaseEntry(
                AugmentedImageDatabaseEntryMode.DYNAMIC,
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )
        val entry = AugmentedImageDatabaseEntry(bitmap)

        assertThat(imageDatabase.entries.size).isEqualTo(1)
        assertThat(imageDatabase.entries[0] == entry).isEqualTo(false)
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun addAugmentedImageDatabaseEntry_unsupportedBitmapConfig_throwsIllegalArgumentException() {
        val imageDatabase = AugmentedImageDatabase()
        assertFailsWith<IllegalArgumentException> {
            imageDatabase.addAugmentedImageDatabaseEntry(
                AugmentedImageDatabaseEntryMode.DYNAMIC,
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444),
            )
        }
    }

    @Test
    fun createTwoDifferentAugmentedImageDatabasesHashCode_returnsFalse() {
        val imageDatabase1 = AugmentedImageDatabase()
        val imageDatabase2 = AugmentedImageDatabase()
        imageDatabase1.addAugmentedImageDatabaseEntry(
            AugmentedImageDatabaseEntryMode.DYNAMIC,
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        )
        imageDatabase2.addAugmentedImageDatabaseEntry(
            AugmentedImageDatabaseEntryMode.STATIC,
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        )

        assertThat(imageDatabase1).isEqualTo(imageDatabase1)
        assertThat(imageDatabase1).isNotEqualTo(imageDatabase2)

        val hash1 = imageDatabase1.hashCode()
        val hash2 = imageDatabase2.hashCode()
        assertThat(hash1).isNotEqualTo(hash2)
    }
}
