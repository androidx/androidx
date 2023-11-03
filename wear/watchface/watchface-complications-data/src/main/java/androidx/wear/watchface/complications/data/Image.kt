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

package androidx.wear.watchface.complications.data

import android.graphics.drawable.Icon
import android.os.Build
import android.support.wearable.complications.ComplicationData as WireComplicationData
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.watchface.utility.iconEquals
import androidx.wear.watchface.utility.iconHashCode
import java.util.Objects

internal const val PLACEHOLDER_IMAGE_RESOURCE_ID = -1

internal fun createPlaceholderIcon(): Icon =
    Icon.createWithResource("", PLACEHOLDER_IMAGE_RESOURCE_ID)

/**
 * A simple, monochromatic image that can be tinted by the watch face.
 *
 * A monochromatic image doesn't have to be black and white, it can have a single color associated
 * with the provider / brand with the expectation that the watch face may recolor it (typically
 * using a SRC_IN filter).
 *
 * An ambient alternative is provided that may be shown instead of the regular image while the watch
 * is not active.
 *
 * @property [image] The image itself
 * @property [ambientImage] The image to be shown when the device is in ambient mode to save power
 *   or avoid burn in
 */
public class MonochromaticImage
internal constructor(public val image: Icon, public val ambientImage: Icon?) {
    /**
     * Builder for [MonochromaticImage].
     *
     * @param [image] the [Icon] representing the image
     */
    public class Builder(private var image: Icon) {
        private var ambientImage: Icon? = null

        /**
         * Sets a different image for when the device is ambient mode to save power and prevent burn
         * in. If no ambient variant is provided, the watch face may not show anything while in
         * ambient mode.
         */
        public fun setAmbientImage(ambientImage: Icon?): Builder = apply {
            this.ambientImage = ambientImage
        }

        /** Constructs the [MonochromaticImage]. */
        public fun build(): MonochromaticImage = MonochromaticImage(image, ambientImage)
    }

    /** Adds a [MonochromaticImage] to a builder for [WireComplicationData]. */
    internal fun addToWireComplicationData(builder: WireComplicationData.Builder) =
        builder.apply {
            setIcon(image)
            setBurnInProtectionIcon(ambientImage)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MonochromaticImage

        if (!(image iconEquals other.image)) return false
        if (!(ambientImage iconEquals other.ambientImage)) return false

        return true
    }

    override fun hashCode(): Int = image.iconHashCode()

    override fun toString(): String {
        return "MonochromaticImage(image=$image, ambientImage=$ambientImage)"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) fun isPlaceholder() = image.isPlaceholder()

    companion object {
        /**
         * For use when the real data isn't available yet, this [MonochromaticImage] should be
         * rendered as a placeholder. It is suggested that it should be rendered with a light grey
         * box.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER: MonochromaticImage =
            MonochromaticImage(createPlaceholderIcon(), null)
    }
}

/**
 * The type of image being provided.
 *
 * This is used to guide rendering on the watch face.
 */
public enum class SmallImageType {
    /**
     * Type for images that have a transparent background and are expected to be drawn entirely
     * within the space available, such as a launcher image. Watch faces may add padding when
     * drawing these images, but should never crop these images. Icons must not be recolored.
     */
    ICON,

    /**
     * Type for images which are photos that are expected to fill the space available. Images of
     * this style may be cropped to fit the shape of the complication - in particular, the image may
     * be cropped to a circle. Photos must not be recolored.
     */
    PHOTO
}

/**
 * An image that is expected to cover a small fraction of a watch face occupied by a single
 * complication. A SmallImage must not be tinted.
 *
 * An ambient alternative is provided that may be shown instead of the regular image while the watch
 * is not active.
 *
 * @property [image] The image itself
 * @property [type] The style of the image provided, to guide how it should be displayed
 * @property [ambientImage] The image to be shown when the device is in ambient mode to save power
 *   or avoid burn in
 */
public class SmallImage
internal constructor(
    public val image: Icon,
    public val type: SmallImageType,
    public val ambientImage: Icon?
) {
    /**
     * Builder for [SmallImage].
     *
     * @param [image] The [Icon] representing the image
     * @param [type] The style of the image provided, to guide how it should be displayed
     */
    public class Builder(private val image: Icon, private val type: SmallImageType) {
        private var ambientImage: Icon? = null

        /**
         * Sets a different image for when the device is ambient mode to save power and prevent burn
         * in. If no ambient variant is provided, the watch face may not show anything while in
         * ambient mode.
         */
        public fun setAmbientImage(ambientImage: Icon?): Builder = apply {
            this.ambientImage = ambientImage
        }

        /** Builds a [SmallImage]. */
        public fun build(): SmallImage = SmallImage(image, type, ambientImage)
    }

    /** Adds a [SmallImage] to a builder for [WireComplicationData]. */
    internal fun addToWireComplicationData(builder: WireComplicationData.Builder) =
        builder.apply {
            setSmallImage(image)
            setSmallImageStyle(
                when (this@SmallImage.type) {
                    SmallImageType.ICON -> WireComplicationData.IMAGE_STYLE_ICON
                    SmallImageType.PHOTO -> WireComplicationData.IMAGE_STYLE_PHOTO
                }
            )
            setBurnInProtectionSmallImage(ambientImage)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SmallImage

        if (type != other.type) return false

        if (!(image iconEquals other.image)) return false
        if (!(ambientImage iconEquals other.ambientImage)) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(image.iconHashCode(), ambientImage?.iconHashCode())

    override fun toString(): String {
        return "SmallImage(image=$image, type=$type, ambientImage=$ambientImage)"
    }

    companion object {
        /**
         * For use when the real data isn't available yet, this [SmallImage] should be rendered as a
         * placeholder. It is suggested that it should be rendered with a light grey box.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER: SmallImage =
            SmallImage(createPlaceholderIcon(), SmallImageType.ICON, null)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) fun isPlaceholder() = image.isPlaceholder()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Icon.isPlaceholder() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        IconP.isPlaceholder(this)
    } else {
        false
    }

@RequiresApi(Build.VERSION_CODES.P)
private object IconP {
    fun isPlaceholder(icon: Icon): Boolean {
        return icon.type == Icon.TYPE_RESOURCE && icon.resId == PLACEHOLDER_IMAGE_RESOURCE_ID
    }
}
