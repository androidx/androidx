/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.translators

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.test.R
import androidx.glance.layout.ContentScale
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageTranslatorTest {

    private lateinit var fakeCoroutineScope: TestScope
    private lateinit var expectedBitmap: Bitmap
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
        expectedBitmap = context.getDrawable(R.drawable.oval)!!.toBitmap()
    }

    @Test
    fun canTranslateImage_bitmap() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(expectedBitmap),
                contentDescription = "2x1 bitmap"
            )
        }
        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        assertThat(imageView.getContentDescription()).isEqualTo("2x1 bitmap")
        val bitmapDrawable = assertIs<BitmapDrawable>(imageView.getDrawable())
        assertThat(bitmapDrawable.getBitmap().sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun canTranslateImage_drawableRes() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "oval"
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        assertThat(imageView.getContentDescription()).isEqualTo("oval")
        val gradientDrawable = assertIs<GradientDrawable>(imageView.getDrawable())
        assertThat(gradientDrawable.toBitmap().sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun canTranslateImage_uri() = fakeCoroutineScope.runTest {
        val uri = with(context.resources) {
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getResourcePackageName(R.drawable.oval))
                .appendPath(getResourceTypeName(R.drawable.oval))
                .appendPath(getResourceEntryName(R.drawable.oval))
                .build()
        }
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(uri),
                contentDescription = ""
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        val gradientDrawable = assertIs<GradientDrawable>(imageView.getDrawable())
        assertThat(gradientDrawable.toBitmap().sameAs(expectedBitmap)).isTrue()
    }

    @Test
    @Config(minSdk = 23)
    @SdkSuppress(minSdkVersion = 23)
    fun canTranslateImage_icon() = fakeCoroutineScope.runTest {
        val icon = Icon.createWithResource(context, R.drawable.oval)
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(icon),
                contentDescription = ""
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        val gradientDrawable = assertIs<GradientDrawable>(imageView.getDrawable())
        assertThat(gradientDrawable.toBitmap().sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun canTranslateImageContentScale_crop() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "oval",
                contentScale = ContentScale.Crop
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        assertThat(imageView.getContentDescription()).isEqualTo("oval")
        assertThat(imageView.getScaleType()).isEqualTo(ImageView.ScaleType.CENTER_CROP)
    }

    @Test
    fun canTranslateImageContentScale_fit() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "oval",
                contentScale = ContentScale.Fit
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        assertThat(imageView.getContentDescription()).isEqualTo("oval")
        assertThat(imageView.getScaleType()).isEqualTo(ImageView.ScaleType.FIT_CENTER)
    }

    @Test
    fun canTranslateImageContentScale_fillBounds() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "oval",
                contentScale = ContentScale.FillBounds
            )
        }

        val imageView = assertIs<ImageView>(context.applyRemoteViews(rv))
        assertThat(imageView.getContentDescription()).isEqualTo("oval")
        assertThat(imageView.getScaleType()).isEqualTo(ImageView.ScaleType.FIT_XY)
    }
}
