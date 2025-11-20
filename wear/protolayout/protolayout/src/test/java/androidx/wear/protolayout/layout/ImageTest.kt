/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.layout

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.ProtoLayoutScope
import androidx.wear.protolayout.ResourceBuilders.IMAGE_FORMAT_RGB_565
import androidx.wear.protolayout.ResourceBuilders.IMAGE_FORMAT_UNDEFINED
import androidx.wear.protolayout.ResourceBuilders.LottieProperty.colorForSlot
import androidx.wear.protolayout.TriggerBuilders
import androidx.wear.protolayout.TriggerBuilders.createOnVisibleTrigger
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageTest {
    @Test
    fun androidResource_full_inflates() {
        val androidImage = androidImageResource(RESOURCE_ID)

        assertThat(androidImage.resourceId).isEqualTo(RESOURCE_ID)
    }

    @Test
    fun lottieResource_withTrigger_inflates() {
        val property = colorForSlot("s", COLOR.prop)
        val trigger = TriggerBuilders.createOnLoadTrigger()

        val lottie =
            lottieResource(
                rawResourceId = RESOURCE_ID,
                properties = listOf(property),
                startTrigger = trigger,
            )

        assertThat(lottie.rawResourceId).isEqualTo(RESOURCE_ID)
        assertThat(lottie.properties).hasSize(1)
        assertThat(lottie.properties[0].toLottiePropertyProto())
            .isEqualTo(property.toLottiePropertyProto())
        assertThat(lottie.startTrigger!!.toTriggerProto()).isEqualTo(trigger.toTriggerProto())
        assertThat(lottie.progress).isNull()
    }

    @Test
    fun lottieResource_withProgress_inflates() {
        val property = colorForSlot("s", COLOR.prop)
        val progress = DynamicFloat.constant(1f)

        val lottie =
            lottieResource(
                rawResourceId = RESOURCE_ID,
                properties = listOf(property),
                progress = progress,
            )

        assertThat(lottie.rawResourceId).isEqualTo(RESOURCE_ID)
        assertThat(lottie.properties).hasSize(1)
        assertThat(lottie.properties[0].toLottiePropertyProto())
            .isEqualTo(property.toLottiePropertyProto())
        assertThat(lottie.progress!!.toDynamicFloatProto())
            .isEqualTo(progress.toDynamicFloatProto())
        assertThat(lottie.startTrigger).isNull()
    }

    @Test
    fun inlineImageResource_withoutFormat_inflates() {
        val data = byteArrayOf(1, 2, 3, 4)
        val width = 1
        val height = 2

        val inlineImage =
            inlineImageResource(compressedBytes = data, widthPx = width, heightPx = height)

        assertThat(inlineImage.data).isEqualTo(data)
        assertThat(inlineImage.widthPx).isEqualTo(width)
        assertThat(inlineImage.heightPx).isEqualTo(height)
        assertThat(inlineImage.format).isEqualTo(IMAGE_FORMAT_UNDEFINED)
    }

    @Test
    fun inlineImageResource_withFormat_inflates() {
        val data = byteArrayOf(1, 2, 3, 4)
        val width = 1
        val height = 2
        val format = IMAGE_FORMAT_RGB_565

        val inlineImage =
            inlineImageResource(
                pixelBuffer = data,
                widthPx = width,
                heightPx = height,
                format = format,
            )

        assertThat(inlineImage.data).isEqualTo(data)
        assertThat(inlineImage.widthPx).isEqualTo(width)
        assertThat(inlineImage.heightPx).isEqualTo(height)
        assertThat(inlineImage.format).isEqualTo(format)
    }

    @Test
    fun inlineImageResource_withUndefinedFormat_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            inlineImageResource(
                pixelBuffer = byteArrayOf(1, 2, 3, 4),
                widthPx = 1,
                heightPx = 2,
                format = IMAGE_FORMAT_UNDEFINED,
            )
        }
    }

    @Test
    fun imageResource_empty_inflates() {
        val resource = imageResource()

        assertThat(resource.androidResourceByResId).isNull()
        assertThat(resource.inlineResource).isNull()
        assertThat(resource.androidLottieResourceByResId).isNull()
        assertThat(resource.androidAnimatedResourceByResId).isNull()
        assertThat(resource.androidSeekableAnimatedResourceByResId).isNull()
    }

    @Test
    fun imageResource_full_inflates() {
        val androidImage = androidImageResource(RESOURCE_ID)
        val lottie = lottieResource(rawResourceId = RESOURCE_ID, startTrigger = TRIGGER)
        val inlineImage =
            inlineImageResource(compressedBytes = byteArrayOf(), widthPx = 1, heightPx = 2)

        val resource =
            imageResource(androidImage = androidImage, lottie = lottie, inlineImage = inlineImage)

        assertThat(resource.androidResourceByResId).isEqualTo(androidImage)
        assertThat(resource.inlineResource).isEqualTo(inlineImage)
        assertThat(resource.androidLottieResourceByResId).isEqualTo(lottie)
        assertThat(resource.androidAnimatedResourceByResId).isNull()
        assertThat(resource.androidSeekableAnimatedResourceByResId).isNull()
    }

    @Test
    fun image_inflates() {
        val scope = ProtoLayoutScope()
        val resource = imageResource()

        val image =
            scope.basicImage(
                resource = resource,
                width = WIDTH,
                height = HEIGHT,
                protoLayoutResourceId = RESOURCE_ID_STRING,
                modifier = MODIFIER,
                contentScaleMode = CONTENT_SCALE_MODE_FIT,
                tintColor = COLOR,
            )

        assertThat(image.width!!.toImageDimensionProto().linearDimension.value)
            .isEqualTo(WIDTH.value)
        assertThat(image.height!!.toImageDimensionProto().linearDimension.value)
            .isEqualTo(HEIGHT.value)
        assertThat(image.resourceId!!.value).isEqualTo(RESOURCE_ID_STRING)
        assertThat(image.modifiers!!.toProto())
            .isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
        assertThat(image.contentScaleMode!!.value).isEqualTo(CONTENT_SCALE_MODE_FIT)
        assertThat(image.colorFilter!!.tint!!.argb).isEqualTo(COLOR.staticArgb)
        assertThat(scope.resources).hasSize(1)
        assertThat(scope.resources.get(RESOURCE_ID_STRING)!!.toProto())
            .isEqualTo(resource.toProto())
    }

    private companion object {
        const val RESOURCE_ID = 1
        const val RESOURCE_ID_STRING = "1"
        val WIDTH = 10f.dp
        val HEIGHT = 10f.dp
        val MODIFIER = LayoutModifier.contentDescription("description")
        val COLOR = Color.YELLOW.argb
        val TRIGGER = createOnVisibleTrigger()
    }
}
