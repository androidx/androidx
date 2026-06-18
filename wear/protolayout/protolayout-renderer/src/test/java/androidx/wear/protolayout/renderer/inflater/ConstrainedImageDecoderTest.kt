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

package androidx.wear.protolayout.renderer.inflater

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.wear.protolayout.renderer.test.R
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class ConstrainedImageDecoderTest {
    private val resources = getApplicationContext<Context>().resources

    @Test
    fun decodeDrawable_imageResource_loadsSuccessfully() {
        val drawable = ConstrainedImageDecoder.decodeDrawable(resources, R.drawable.filled_image)

        assertThat(drawable).isInstanceOf(BitmapDrawable::class.java)
        val bitmap = (drawable as BitmapDrawable).bitmap
        assertThat(bitmap.width).isGreaterThan(0)
        assertThat(bitmap.height).isGreaterThan(0)
    }

    @Test
    fun decodeDrawable_largeImageResource_throws() {
        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(resources, R.drawable.test2049x2049)
        }
    }

    @Test
    fun decodeDrawable_contentResolverImage_loadsSuccessfully() {
        val contentResolver = getApplicationContext<Context>().contentResolver
        val uri = Uri.parse("content://test/image")
        val inputStream = resources.openRawResource(R.drawable.filled_image)
        shadowOf(contentResolver).registerInputStream(uri, inputStream)

        val drawable = ConstrainedImageDecoder.decodeDrawable(contentResolver, uri)

        assertThat(drawable).isInstanceOf(BitmapDrawable::class.java)
        val bitmap = (drawable as BitmapDrawable).bitmap
        assertThat(bitmap.width).isGreaterThan(0)
        assertThat(bitmap.height).isGreaterThan(0)
    }

    @Test
    fun decodeDrawable_largeContentResolverImage_throws() {
        val contentResolver = getApplicationContext<Context>().contentResolver
        val uri = Uri.parse("content://test/image")
        val inputStream = resources.openRawResource(R.drawable.test2049x2049)
        shadowOf(contentResolver).registerInputStream(uri, inputStream)

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(contentResolver, uri)
        }
    }

    @Test
    fun decodeBitmap_imageResource_loadsSuccessfully() {
        val bitmap =
            ConstrainedImageDecoder.decodeBitmap(
                getBytes(R.drawable.filled_image),
                targetWidthPx = REASONABLE_SIZE_PX,
                targetHeightPx = REASONABLE_SIZE_PX,
            )

        assertThat(bitmap.width).isEqualTo(100)
        assertThat(bitmap.height).isEqualTo(100)
    }

    @Test
    fun decodeBitmap_imageResource_largeTargetSize_throws() {
        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeBitmap(
                getBytes(R.drawable.filled_image),
                targetWidthPx = LARGE_SIZE_PX,
                targetHeightPx = LARGE_SIZE_PX,
            )
        }
    }

    @Test
    fun decodeBitmap_largeImageResource_throws() {
        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeBitmap(
                getBytes(R.drawable.test2049x2049),
                targetWidthPx = REASONABLE_SIZE_PX,
                targetHeightPx = REASONABLE_SIZE_PX,
            )
        }
    }

    @Test
    fun decodeBitmap_largeImageResource_largeTargetSize_throws() {
        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeBitmap(
                getBytes(R.drawable.test2049x2049),
                targetWidthPx = LARGE_SIZE_PX,
                targetHeightPx = LARGE_SIZE_PX,
            )
        }
    }

    private fun getBytes(@DrawableRes res: Int) =
        resources.openRawResource(res).use { it.readBytes() }

    private companion object {
        const val REASONABLE_SIZE_PX = 100
        const val LARGE_SIZE_PX = 10000
    }
}
