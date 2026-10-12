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
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.wear.protolayout.proto.ResourceProto.AndroidAnimatedImageResourceByResId
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId
import androidx.wear.protolayout.proto.ResourceProto.AnimatedImageFormat
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
    fun decodeDrawable_xmlVectorResource_loadsSuccessfully() {
        val drawable = ConstrainedImageDecoder.decodeDrawable(resources, R.drawable.android_24dp)

        assertThat(drawable).isInstanceOf(VectorDrawable::class.java)
    }

    @Test
    @Suppress("DEPRECATION")
    fun decodeDrawable_colorResource_loadsSuccessfully() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    outValue.type = TypedValue.TYPE_INT_COLOR_ARGB8
                    outValue.data = 0xFF000000.toInt()
                    outValue.string = null
                }

                override fun getDrawable(
                    id: Int,
                    theme: Theme?,
                ): android.graphics.drawable.Drawable =
                    android.graphics.drawable.ColorDrawable(0xFF000000.toInt())
            }

        val drawable = ConstrainedImageDecoder.decodeDrawable(testResources, 1)
        assertThat(drawable).isInstanceOf(android.graphics.drawable.ColorDrawable::class.java)
    }

    @Test
    fun decodeDrawable_xmlWrappedBitmapDrawableWithLargeRaster_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_animated_24dp,
                customXmlRootTag = "bitmap",
                childDrawableReplacementResId = R.drawable.test2049x2049,
            )

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    fun decodeDrawable_xmlWrappedNinePatchDrawableWithLargeRaster_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_animated_24dp,
                customXmlRootTag = "nine-patch",
                childDrawableReplacementResId = R.drawable.test2049x2049,
            )

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    fun decodeDrawable_xmlShapeDrawable_loadsSuccessfully() {
        val testResources =
            createTestResources(xmlResId = R.drawable.android_24dp, customXmlRootTag = "shape")

        val drawable =
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_24dp)
        assertThat(drawable).isNotNull()
    }

    @Test
    @Suppress("DEPRECATION")
    fun verifyDrawableXml_repeatedCalls_usesCachedPixelCostWithoutReopeningXml() {
        var xmlOpenCount = 0
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getXml(id: Int): XmlResourceParser {
                    xmlOpenCount++
                    return super.getXml(id)
                }
            }

        XmlDrawableValidator.verifyDrawableXml(testResources, R.drawable.android_24dp)
        assertThat(xmlOpenCount).isEqualTo(1)

        XmlDrawableValidator.verifyDrawableXml(testResources, R.drawable.android_24dp)
        assertThat(xmlOpenCount).isEqualTo(1)
    }

    @Test
    fun decodeDrawable_xmlLayerListWithSmallRasterChild_loadsSuccessfully() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_animated_24dp,
                customXmlRootTag = "layer-list",
                childDrawableReplacementResId = R.drawable.filled_image,
            )

        XmlDrawableValidator.verifyDrawableXml(testResources, R.drawable.android_animated_24dp)
    }

    @Test
    fun decodeDrawable_xmlLayerListWithRasterChild_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_animated_24dp,
                customXmlRootTag = "layer-list",
                childDrawableReplacementResId = R.drawable.test2049x2049,
            )

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    fun decodeDrawable_animatedVectorWithBitmapChild_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_head_24dp,
                customXmlRootTag = "bitmap",
            )

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    fun decodeDrawable_animatedVectorWithRasterChild_throws() {
        val testResources =
            createTestResources(childDrawableReplacementResId = R.drawable.test2049x2049)

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    fun animatedImageResolver_restrictImageSizeTrue_xmlWrappedBitmapWithLargeRaster_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_animated_24dp,
                customXmlRootTag = "bitmap",
                childDrawableReplacementResId = R.drawable.test2049x2049,
            )
        val resolver =
            DefaultAndroidAnimatedImageResourceByResIdResolver(
                testResources,
                /* restrictImageSize= */ true,
            )
        val resource =
            AndroidAnimatedImageResourceByResId.newBuilder()
                .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                .setResourceId(R.drawable.android_animated_24dp)
                .build()

        assertFailsWith<IllegalArgumentException> { resolver.getDrawableOrThrow(resource) }
    }

    @Test
    fun seekableAnimatedImageResolver_restrictImageSizeTrue_validAvd_loadsSuccessfully() {
        val resolver =
            DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
                resources,
                /* restrictImageSize= */ true,
            )
        val resource =
            AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                .setResourceId(R.drawable.android_animated_24dp)
                .build()

        val drawable = resolver.getDrawableOrThrow(resource)
        assertThat(drawable).isNotNull()
    }

    @Test
    fun seekableAnimatedImageResolver_restrictImageSizeTrue_xmlWrappedBitmap_throws() {
        val testResources =
            createTestResources(
                xmlResId = R.drawable.android_head_24dp,
                customXmlRootTag = "bitmap",
            )
        val resolver =
            DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
                testResources,
                /* restrictImageSize= */ true,
            )
        val resource =
            AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                .setResourceId(R.drawable.android_animated_24dp)
                .build()

        assertFailsWith<IllegalArgumentException> { resolver.getDrawableOrThrow(resource) }
    }

    @Test
    fun seekableAnimatedImageResolver_restrictImageSizeTrue_animatedVectorWithRasterChild_throws() {
        val testResources =
            createTestResources(childDrawableReplacementResId = R.drawable.test2049x2049)
        val resolver =
            DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
                testResources,
                /* restrictImageSize= */ true,
            )
        val resource =
            AndroidSeekableAnimatedImageResourceByResId.newBuilder()
                .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                .setResourceId(R.drawable.android_animated_24dp)
                .build()

        assertFailsWith<IllegalArgumentException> { resolver.getDrawableOrThrow(resource) }
    }

    @Suppress("DEPRECATION")
    private fun createTestResources(
        @DrawableRes xmlResId: Int = 0,
        customXmlRootTag: String? = null,
        @DrawableRes childDrawableReplacementResId: Int = 0,
    ): Resources =
        object : Resources(resources.assets, resources.displayMetrics, resources.configuration) {
            private fun resolveId(id: Int): Int =
                if (childDrawableReplacementResId != 0 && id == R.drawable.android_head_24dp) {
                    childDrawableReplacementResId
                } else {
                    id
                }

            override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                super.getValue(resolveId(id), outValue, resolveRefs)
            }

            override fun openRawResource(id: Int, value: TypedValue): java.io.InputStream =
                super.openRawResource(resolveId(id), value)

            override fun getXml(id: Int): XmlResourceParser {
                val resolvedId = resolveId(id)
                return if (customXmlRootTag != null && resolvedId == xmlResId) {
                    CustomTagXmlResourceParser(super.getXml(resolvedId), customXmlRootTag)
                } else {
                    super.getXml(resolvedId)
                }
            }
        }

    private class CustomTagXmlResourceParser(
        private val delegate: XmlResourceParser,
        private val customTag: String,
    ) : XmlResourceParser by delegate {
        override fun getName(): String = customTag
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
    fun decodeBitmap_imageResource_nonPositiveTargetSize_throws() {
        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeBitmap(
                getBytes(R.drawable.filled_image),
                targetWidthPx = 0,
                targetHeightPx = REASONABLE_SIZE_PX,
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
