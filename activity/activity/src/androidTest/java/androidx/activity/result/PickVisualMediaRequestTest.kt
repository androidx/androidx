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

package androidx.activity.result

import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PickVisualMediaRequestTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun buildPickVisualMedia() {
        val request =
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                .build()
        assertThat(request.mediaType).isEqualTo(ActivityResultContracts.PickVisualMedia.VideoOnly)
    }

    @Test
    fun PickVisualMediaFun() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)

        assertThat(request.mediaType).isEqualTo(ActivityResultContracts.PickVisualMedia.VideoOnly)
    }

    @Test
    fun testPickVisualMediaRequest_maxItems() {
        val request =
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                maxItems = 5
            )

        assertThat(request.mediaType).isEqualTo(ActivityResultContracts.PickVisualMedia.ImageOnly)
        assertThat(request.maxItems).isEqualTo(5)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU, codeName = "Tiramisu")
    fun testPickMultipleVisualMediaRequest_maxItems() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val defaultMaxItems = ActivityResultContracts.PickMultipleVisualMedia.getMaxItems()

        // test boundary (maxItems should be greater than 1)
        assertThrows(IllegalArgumentException::class.java) {
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 1)
        }

        // test boundary (maxItems should be less than the defaultMaxItems)
        var request =
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 1 + defaultMaxItems)
        assertThrows(IllegalArgumentException::class.java) {
            request.createIntent(
                context,
                input = PickVisualMediaRequest(maxItems = 1 + defaultMaxItems)
            )
        }

        // test default
        request = ActivityResultContracts.PickMultipleVisualMedia()
        var intent = request.createIntent(context, input = PickVisualMediaRequest())

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ defaultMaxItems)

        // test given maxItems in PickMultipleVisualMedia
        request = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
        intent = request.createIntent(context, input = PickVisualMediaRequest())

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ 9)

        // test max selection limit updated to minimum of PickMultipleVisualMedia.maxItems and the
        // input PickVisualMediaRequest.maxItems
        intent = request.createIntent(context, input = PickVisualMediaRequest(maxItems = 7))

        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isTrue()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, /* defaultValue= */ 0))
            .isEqualTo(/* expected= */ 7)
    }

    @Test
    fun testPickVisualMediaRequest_accentColor() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.isCustomAccentColorApplied).isEqualTo(false)

        // test given accent color in PickVisualMediaRequest
        request = PickVisualMediaRequest(accentColor = 0xffff0000)
        assertThat(request.isCustomAccentColorApplied).isEqualTo(true)
        assertThat(request.accentColor).isEqualTo(0xffff0000)
    }

    @Test
    fun testPickVisualMediaRequest_defaultTab() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.defaultTab)
            .isEqualTo(ActivityResultContracts.PickVisualMedia.DefaultTab.PhotosTab)

        // test given default tab in PickVisualMediaRequest
        request =
            PickVisualMediaRequest(
                defaultTab = ActivityResultContracts.PickVisualMedia.DefaultTab.AlbumsTab
            )
        assertThat(request.defaultTab)
            .isEqualTo(ActivityResultContracts.PickVisualMedia.DefaultTab.AlbumsTab)
    }

    @Test
    fun testPickVisualMediaRequest_isOrderedSelection() {
        // test default
        var request = PickVisualMediaRequest()
        assertThat(request.isOrderedSelection).isEqualTo(false)

        // test given isOrderedSelection in PickVisualMediaRequest
        request = PickVisualMediaRequest(isOrderedSelection = true)
        assertThat(request.isOrderedSelection).isEqualTo(true)
    }
}
