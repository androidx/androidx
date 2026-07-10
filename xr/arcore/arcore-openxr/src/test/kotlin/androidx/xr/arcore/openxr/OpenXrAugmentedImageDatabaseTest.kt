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

package androidx.xr.arcore.openxr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntryMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenXrAugmentedImageDatabaseTest {

    @Test
    fun fromAugmentedImageDatabase_withValidValue_convertsOpenXrAugmentedImageDatabase() {
        val augmentedImageDatabase = AugmentedImageDatabase()
        augmentedImageDatabase.addAugmentedImageDatabaseEntry(
            mode = AugmentedImageDatabaseEntryMode.DYNAMIC,
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        )
        val openXrAugmentedImageDatabase =
            OpenXrAugmentedImageDatabase.fromAugmentedImageDatabase(augmentedImageDatabase)

        assertThat(openXrAugmentedImageDatabase)
            .isInstanceOf(OpenXrAugmentedImageDatabase::class.java)
        assertThat(openXrAugmentedImageDatabase?.entries).isNotEmpty()
        assertThat(openXrAugmentedImageDatabase?.entries?.size).isEqualTo(1)
    }

    @Test
    fun fromAugmentedImageDatabase_withEmptyEntries_returnsNull() {
        val augmentedImageDatabase = AugmentedImageDatabase()
        val openXrAugmentedImageDatabase =
            OpenXrAugmentedImageDatabase.fromAugmentedImageDatabase(augmentedImageDatabase)

        assertThat(augmentedImageDatabase.entries).isEmpty()
        assertThat(openXrAugmentedImageDatabase).isNull()
    }
}
