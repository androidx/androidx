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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.wear.protolayout.layout

import android.annotation.SuppressLint
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.wear.protolayout.DimensionBuilders.ImageDimension
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.ContentScaleMode
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.ProtoLayoutScope
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.AndroidLottieResourceByResId
import androidx.wear.protolayout.ResourceBuilders.IMAGE_FORMAT_ARGB_8888
import androidx.wear.protolayout.ResourceBuilders.IMAGE_FORMAT_RGB_565
import androidx.wear.protolayout.ResourceBuilders.IMAGE_FORMAT_UNDEFINED
import androidx.wear.protolayout.ResourceBuilders.ImageFormat
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.ResourceBuilders.InlineImageResource
import androidx.wear.protolayout.ResourceBuilders.LottieProperty
import androidx.wear.protolayout.TriggerBuilders.Trigger
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.LayoutColor

/**
 * Builds an image from the given resources.
 *
 * @param resource An Image resource, used in the layout in the place of this element
 * @param width The width of the image
 * @param height The height of the image
 * @param protoLayoutResourceId The optional String ID for the resource. This is optional as it has
 *   a default value assigned by the automatic resource registration, but can be used to override it
 *   if desired to mark the resource with readable ID.
 * @param modifier Modifiers to set to this element
 * @param contentScaleMode Defines how the content which does not match the dimensions of its bounds
 *   (e.g. an image resource being drawn inside an Image) will be resized to fit its bounds
 * @param tintColor The tint color to apply to the image
 */
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun ProtoLayoutScope.basicImage(
    resource: ImageResource,
    width: ImageDimension,
    height: ImageDimension,
    protoLayoutResourceId: String? = null,
    modifier: LayoutModifier? = null,
    @ContentScaleMode contentScaleMode: Int = CONTENT_SCALE_MODE_UNDEFINED,
    tintColor: LayoutColor? = null,
): Image =
    Image.Builder(this)
        .setWidth(width)
        .setHeight(height)
        .apply {
            protoLayoutResourceId?.let { setImageResource(resource, it) }
                ?: setImageResource(resource)
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            if (contentScaleMode != CONTENT_SCALE_MODE_UNDEFINED) {
                setContentScaleMode(contentScaleMode)
            }
            tintColor?.let { setColorFilter(ColorFilter.Builder().setTint(it.prop).build()) }
        }
        .build()

/**
 * Builds a resource object for the image, based on the given resources types. This can hold
 * multiple underlying resource types, which the underlying renderer will pick according to what it
 * is supported in its version and what it thinks is appropriate.
 *
 * It is recommended to use this inside the APIs for the automatic resource registration, such as
 * [basicImage].
 *
 * @param androidImage The image resource that maps to an Android drawable by resource ID
 * @param inlineImage The image resource that contains the image data inline
 * @param lottie The Lottie resource that is read from a raw Android resource ID
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun imageResource(
    androidImage: AndroidImageResourceByResId? = null,
    inlineImage: InlineImageResource? = null,
    lottie: AndroidLottieResourceByResId? = null,
): ImageResource =
    ImageResource.Builder()
        .apply {
            androidImage?.let { setAndroidResourceByResId(it) }
            inlineImage?.let { setInlineResource(it) }
            lottie?.let { setAndroidLottieResourceByResId(it) }
        }
        .build()

/**
 * Builds image resource that maps to an Android drawable by the given resource ID.
 *
 * @param resourceId The drawable resource ID to map this image
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun androidImageResource(@DrawableRes resourceId: Int): AndroidImageResourceByResId =
    AndroidImageResourceByResId.Builder().setResourceId(resourceId).build()

/**
 * Builds Lottie resource that is read from a raw Android resource ID and can be played via starting
 * trigger.
 *
 * @param rawResourceId The raw resource ID to map this Lottie
 * @param startTrigger The trigger to start the animation. If not set, animation will be played on
 *   layout load
 * @param properties The list of properties to customize Lottie further
 * @throws IllegalArgumentException if the number of properties is larger than
 *   [AndroidLottieResourceByResId.getMaxPropertiesCount()]
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun lottieResource(
    @RawRes rawResourceId: Int,
    startTrigger: Trigger? = null,
    properties: List<LottieProperty> = emptyList(),
): AndroidLottieResourceByResId =
    AndroidLottieResourceByResId.Builder(rawResourceId)
        .apply {
            startTrigger?.let { setStartTrigger(it) }
            if (properties.isNotEmpty()) {
                setProperties(*properties.toTypedArray())
            }
        }
        .build()

/**
 * Builds Lottie resource that is read from a raw Android resource ID and can be played via
 * progress.
 *
 * @param rawResourceId The raw resource ID to map this Lottie
 * @param progress The [DynamicFloat], normally transformed from certain states with the data
 *   binding pipeline to control the progress of the animation. Its value is required to fall in the
 *   range of [0.0, 1.0]. Any values outside this range would be clamped.
 * @param properties The list of properties to customize Lottie further
 * @throws IllegalArgumentException if the number of properties is larger than
 *   [AndroidLottieResourceByResId.getMaxPropertiesCount()]
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun lottieResource(
    @RawRes rawResourceId: Int,
    progress: DynamicFloat,
    properties: List<LottieProperty> = emptyList(),
): AndroidLottieResourceByResId =
    AndroidLottieResourceByResId.Builder(rawResourceId)
        .setProgress(progress)
        .apply {
            if (properties.isNotEmpty()) {
                setProperties(*properties.toTypedArray())
            }
        }
        .build()

/**
 * Builds image resource that contains the pixel buffer in the specified format of the given byte
 * array data.
 *
 * @param pixelBuffer The byte array representing the image
 * @param format The format of the byte array data representing the image. This should be specific
 *   format of [IMAGE_FORMAT_RGB_565] or [IMAGE_FORMAT_ARGB_8888]. If compressed image data is used,
 *   then [inlineImageResource] with `(ByteArray, Int, Int)` should be used, without specified
 *   format.
 * @param widthPx The native width of the image, in pixels.
 * @param heightPx The native height of the image, in pixels.
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun inlineImageResource(
    pixelBuffer: ByteArray,
    @ImageFormat format: Int,
    @Dimension(unit = Dimension.PX) widthPx: Int,
    @Dimension(unit = Dimension.PX) heightPx: Int,
): InlineImageResource {
    require(format != IMAGE_FORMAT_UNDEFINED) {
        "Format for Inline Image must be specified as one of the following: `IMAGE_FORMAT_RGB_565` or `IMAGE_FORMAT_ARGB_8888`"
    }
    return InlineImageResource.Builder()
        .setData(pixelBuffer)
        .setFormat(format)
        .apply {
            if (widthPx != 0) {
                setWidthPx(widthPx)
            }
            if (heightPx != 0) {
                setHeightPx(heightPx)
            }
        }
        .build()
}

/**
 * Builds image resource that contains the compressed image data extracted from the raw image data.
 *
 * This data shouldn't be in any format. If the image data bytes are formatted use
 * [inlineImageResource] with `(ByteArray, Int, Int, Int)`.
 *
 * @param compressedBytes The byte array representing the image
 * @param widthPx The native width of the image, in pixels.
 * @param heightPx The native height of the image, in pixels.
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun inlineImageResource(
    compressedBytes: ByteArray,
    @Dimension(unit = Dimension.PX) widthPx: Int,
    @Dimension(unit = Dimension.PX) heightPx: Int,
): InlineImageResource =
    InlineImageResource.Builder()
        .setData(compressedBytes)
        .apply {
            // Undefined format
            if (widthPx != 0) {
                setWidthPx(widthPx)
            }
            if (heightPx != 0) {
                setHeightPx(heightPx)
            }
        }
        .build()
