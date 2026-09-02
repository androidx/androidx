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

package androidx.photopicker.activity.params

import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresExtension
import androidx.photopicker.activity.PickVisualMedia
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HighlightParamsTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testHighlightSearchResultsParams_builderValidation() {
        // Missing highlightType
        assertThrows(IllegalArgumentException::class.java) {
            HighlightSearchResultsParams.Builder().setHighlightSearchTextQuery("test").build()
        }

        // Missing query
        assertThrows(IllegalArgumentException::class.java) {
            HighlightSearchResultsParams.Builder().setHighlightType(HighlightType.EXPANDED).build()
        }

        // Blank query
        assertThrows(IllegalArgumentException::class.java) {
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("   ")
                .build()
        }
    }

    @Test
    fun testHighlightAlbumParams_builderValidation() {
        // Missing highlightType
        assertThrows(IllegalArgumentException::class.java) {
            HighlightAlbumParams.Builder()
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_FAVORITES)
                .build()
        }

        // Missing highlightAlbum
        assertThrows(IllegalArgumentException::class.java) {
            HighlightAlbumParams.Builder().setHighlightType(HighlightType.COLLAPSED).build()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 19)
    fun testCreateHighlightSearchResultsBundle_platform() {
        val params =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("cats")
                .build()

        val bundle = params.createHighlightSearchResultsBundle()

        assertThat(bundle.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
            .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED)
        assertThat(bundle.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY))
            .isEqualTo("cats")
    }

    @Test
    fun testCreateHighlightSearchResultsBundle_isSystemFallback() {
        val params =
            HighlightSearchResultsParams.Builder()
                .setHighlightType(HighlightType.EXPANDED)
                .setHighlightSearchTextQuery("cats")
                .build()

        val fallbackBundle = params.createHighlightSearchResultsFallbackBundle()

        assertThat(
                fallbackBundle.getInt(
                    PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE
                )
            )
            .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED)
        assertThat(
                fallbackBundle.getString(
                    PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY
                )
            )
            .isEqualTo("cats")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 19)
    fun testCreateHighlightAlbumBundle_platform() {
        val params =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_FAVORITES)
                .build()

        val bundle = params.createHighlightAlbumBundle()

        assertThat(bundle.getInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE))
            .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED)
        assertThat(bundle.getString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_ALBUM_ID))
            .isEqualTo(MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES)
    }

    @Test
    fun testCreateHighlightAlbumBundle_isSystemFallback() {
        val params =
            HighlightAlbumParams.Builder()
                .setHighlightType(HighlightType.COLLAPSED)
                .setHighlightAlbum(HighlightAlbumParams.ALBUM_CAMERA)
                .build()

        val fallbackBundle = params.createHighlightAlbumFallbackBundle()

        assertThat(
                fallbackBundle.getInt(
                    PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE
                )
            )
            .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED)
        assertThat(
                fallbackBundle.getString(
                    PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID
                )
            )
            .isEqualTo(PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA)
    }
}
