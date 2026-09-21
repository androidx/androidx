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
    fun decodeDrawable_xmlWrappedBitmapDrawable_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    outValue.string = "res/drawable/wrapped_bitmap.xml"
                }

                override fun getXml(id: Int): XmlResourceParser =
                    CustomTagXmlResourceParser(super.getXml(R.drawable.android_24dp), "bitmap")
            }

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, 1)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun decodeDrawable_xmlWrappedNinePatchDrawable_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    outValue.string = "res/drawable/wrapped_ninepatch.xml"
                }

                override fun getXml(id: Int): XmlResourceParser =
                    CustomTagXmlResourceParser(super.getXml(R.drawable.android_24dp), "nine-patch")
            }

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, 1)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun decodeDrawable_xmlLayerListDrawable_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    outValue.string = "res/drawable/malicious_layer_list.xml"
                }

                override fun getXml(id: Int): XmlResourceParser =
                    CustomTagXmlResourceParser(super.getXml(R.drawable.android_24dp), "layer-list")
            }

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, 1)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun decodeDrawable_animatedVectorWithNonVectorChild_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getXml(id: Int): XmlResourceParser =
                    if (id == R.drawable.android_head_24dp) {
                        CustomTagXmlResourceParser(super.getXml(id), "layer-list")
                    } else {
                        super.getXml(id)
                    }
            }

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun decodeDrawable_animatedVectorWithRasterChild_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    super.getValue(id, outValue, resolveRefs)
                    if (id == R.drawable.android_head_24dp) {
                        outValue.string = "res/drawable/large_image.png"
                    }
                }

                override fun getXml(id: Int): XmlResourceParser =
                    if (id == R.drawable.android_head_24dp) {
                        throw NotFoundException("Raster PNG cannot be opened as XML")
                    } else {
                        super.getXml(id)
                    }
            }

        assertFailsWith<IllegalArgumentException> {
            ConstrainedImageDecoder.decodeDrawable(testResources, R.drawable.android_animated_24dp)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun animatedImageResolver_restrictImageSizeTrue_xmlWrappedBitmap_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
                    outValue.string = "res/drawable/wrapped_bitmap.xml"
                }

                override fun getXml(id: Int): XmlResourceParser =
                    CustomTagXmlResourceParser(super.getXml(R.drawable.android_24dp), "bitmap")
            }
        val resolver =
            DefaultAndroidAnimatedImageResourceByResIdResolver(
                testResources,
                /* restrictImageSize= */ true,
            )
        val resource =
            AndroidAnimatedImageResourceByResId.newBuilder()
                .setAnimatedImageFormat(AnimatedImageFormat.ANIMATED_IMAGE_FORMAT_AVD)
                .setResourceId(1)
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
    @Suppress("DEPRECATION")
    fun seekableAnimatedImageResolver_restrictImageSizeTrue_xmlWrappedBitmap_throws() {
        val testResources =
            object :
                Resources(resources.assets, resources.displayMetrics, resources.configuration) {
                override fun getXml(id: Int): XmlResourceParser =
                    if (id == R.drawable.android_head_24dp) {
                        CustomTagXmlResourceParser(super.getXml(id), "bitmap")
                    } else {
                        super.getXml(id)
                    }
            }
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
