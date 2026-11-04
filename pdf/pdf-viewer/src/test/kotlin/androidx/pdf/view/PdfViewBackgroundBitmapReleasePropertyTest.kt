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

package androidx.pdf.view

import android.app.Activity
import androidx.pdf.ExperimentalPdfApi
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPdfApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfViewBackgroundBitmapReleasePropertyTest {

    private val pdfView = PdfView(Robolectric.buildActivity(Activity::class.java).get())

    @Test
    fun backgroundBitmapRelease_defaultsToNone() {
        assertThat(pdfView.backgroundBitmapRelease)
            .isEqualTo(PdfView.BACKGROUND_BITMAP_RELEASE_NONE)
    }

    @Test
    fun backgroundBitmapRelease_acceptsAll() {
        pdfView.backgroundBitmapRelease = PdfView.BACKGROUND_BITMAP_RELEASE_ALL

        assertThat(pdfView.backgroundBitmapRelease).isEqualTo(PdfView.BACKGROUND_BITMAP_RELEASE_ALL)
    }

    @Test
    fun backgroundBitmapRelease_rejectsUnknownValue() {
        assertThrows(IllegalArgumentException::class.java) { pdfView.backgroundBitmapRelease = 2 }

        assertThat(pdfView.backgroundBitmapRelease)
            .isEqualTo(PdfView.BACKGROUND_BITMAP_RELEASE_NONE)
    }
}
