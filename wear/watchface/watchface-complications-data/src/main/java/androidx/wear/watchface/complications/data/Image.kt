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

/**
 * A simple, monochromatic image that can be tinted by the watch face.
 *
 * An ambient alternative is provided that may be shown instead of the regular image while the
 * watch is not active.
 *
 * @param[image] the image itself
 * @param[ambientImage] the image to be shown when the device is in ambient mode to save power or
 * avoid burn in
 */
public class MonochromaticImage internal constructor(
    public val image: Icon,
    public val ambientImage: Icon?
) {
    /**
     * Builder for [MonochromaticImage].
     *
     * @param[image] the [Icon] representing the image
     */
    public class Builder(private var image: Icon) {
        private var ambientImage: Icon? = null

        /**
         * Sets a different image for when the device is ambient mode to save power and prevent
         * burn in. If no ambient variant is provided, the watch face may not show anything while
         * in ambient mode.
         */
        public fun setAmbientImage(ambientImage: Icon?): Builder = apply {
            this.ambientImage = ambientImage
        }

        /** Constructs the [MonochromaticImage]. */
        public fun build(): MonochromaticImage = MonochromaticImage(image, ambientImage)
    }

    /** Adds a [MonochromaticImage] to a builder for [WireComplicationData]. */
    internal fun addToWireComplicationData(builder: WireComplicationDataBuilder) = builder.apply {
        setIcon(image)
        setBurnInProtectionIcon(ambientImage)
    }
}

/**
 * The type of image being provided.
 *
 * This is used to guide rendering on the watch face.
 */
public enum class SmallImageType {
    /**
     * Type for images that have a transparent background and are expected to be drawn
     * entirely within the space available, such as a launcher image. Watch faces may add padding
     * when drawing these images, but should never crop these images. Icons may be tinted to fit
     * the complication style.
     */
    ICON,

    /**
     * Type for images which are photos that are expected to fill the space available. Images
     * of this style may be cropped to fit the shape of the complication - in particular, the image
     * may be cropped to a circle. Photos my not be recolored.
     */
    PHOTO
}

/**
 * An image that is expected to cover a small fraction of a watch face occupied by a single
 * complication.
 *
 * An ambient alternative is provided that may be shown instead of the regular image while the
 * watch is not active.
 *
 * @param[image] the image itself
 * @param[type] the style of the image provided, to guide how it should be displayed
 * @param[ambientImage] the image to be shown when the device is in ambient mode to save power or
 * avoid burn in
 */
public class SmallImage internal constructor(
    public val image: Icon,
    public val type: SmallImageType,
    public val ambientImage: Icon?
) {
    /**
     * Builder for [MonochromaticImage].
     *
     * @param[image] the [Icon] representing the image
     * @param[type] the style of the image provided, to guide how it should be displayed
     */
    public class Builder(private val image: Icon, private val type: SmallImageType) {
        private var ambientImage: Icon? = null

        /**
         * Sets a different image for when the device is ambient mode to save power and prevent
         * burn in. If no ambient variant is provided, the watch face may not show anything while
         * in ambient mode.
         */
        public fun setAmbientImage(ambientImage: Icon?): Builder = apply {
            this.ambientImage = ambientImage
        }

        /** Builds a [SmallImage]. */
        public fun build(): SmallImage = SmallImage(image, type, ambientImage)
    }

    /** Adds a [SmallImage] to a builder for [WireComplicationData]. */
    internal fun addToWireComplicationData(builder: WireComplicationDataBuilder) = builder.apply {
        setSmallImage(image)
        setSmallImageStyle(
            when (type) {
                SmallImageType.ICON -> WireComplicationData.IMAGE_STYLE_ICON
                SmallImageType.PHOTO -> WireComplicationData.IMAGE_STYLE_PHOTO
            }
        )
        setBurnInProtectionSmallImage(ambientImage)
    }
}
