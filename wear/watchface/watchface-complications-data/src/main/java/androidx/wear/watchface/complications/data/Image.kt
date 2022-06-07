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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

internal const val PLACEHOLDER_IMAGE_RESOURCE_ID = -1

internal fun createPlaceholderIcon(): Icon =
    Icon.createWithResource("", PLACEHOLDER_IMAGE_RESOURCE_ID)

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MonochromaticImage

        if (!if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                IconHelperP.equals(image, other.image)
            } else {
                IconHelperBeforeP.equals(image, other.image)
            }
        ) return false

        if (!if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                IconHelperP.equals(ambientImage, other.ambientImage)
            } else {
                IconHelperBeforeP.equals(ambientImage, other.ambientImage)
            }
        ) return false

        return true
    }

    override fun hashCode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var result = IconHelperP.hashCode(image)
            result = 31 * result + IconHelperP.hashCode(ambientImage)
            result
        } else {
            var result = IconHelperBeforeP.hashCode(image)
            result = 31 * result + IconHelperBeforeP.hashCode(ambientImage)
            result
        }
    }

    override fun toString(): String {
        return "MonochromaticImage(image=$image, ambientImage=$ambientImage)"
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isPlaceholder() = image.isPlaceholder()

    /** @hide */
    public companion object {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SmallImage

        if (type != other.type) return false

        if (!if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                IconHelperP.equals(image, other.image)
            } else {
                IconHelperBeforeP.equals(image, other.image)
            }
        ) return false

        if (!if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                IconHelperP.equals(ambientImage, other.ambientImage)
            } else {
                IconHelperBeforeP.equals(ambientImage, other.ambientImage)
            }
        ) return false
        return true
    }

    override fun hashCode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var result = IconHelperP.hashCode(image)
            result = 31 * result + type.hashCode()
            result = 31 * result + IconHelperP.hashCode(ambientImage)
            result
        } else {
            var result = IconHelperBeforeP.hashCode(image)
            result = 31 * result + type.hashCode()
            result = 31 * result + IconHelperBeforeP.hashCode(ambientImage)
            result
        }
    }

    override fun toString(): String {
        return "SmallImage(image=$image, type=$type, ambientImage=$ambientImage)"
    }

    /** @hide */
    public companion object {
        /**
         * For use when the real data isn't available yet, this [SmallImage] should be rendered
         * as a placeholder. It is suggested that it should be rendered with a light grey box.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER: SmallImage =
            SmallImage(createPlaceholderIcon(), SmallImageType.ICON, null)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isPlaceholder() = image.isPlaceholder()
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Icon.isPlaceholder() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    IconHelperP.isPlaceholder(this)
} else {
    false
}

@RequiresApi(Build.VERSION_CODES.P)
internal class IconHelperP {
    companion object {
        fun isPlaceholder(icon: Icon): Boolean {
            return icon.type == Icon.TYPE_RESOURCE && icon.resId == PLACEHOLDER_IMAGE_RESOURCE_ID
        }

        fun equals(a: Icon?, b: Icon?): Boolean {
            if (a == null) {
                return b == null
            }
            if (b == null) {
                return false
            }
            if (a.type != b.type) return false
            when (a.type) {
                Icon.TYPE_RESOURCE -> {
                    if (a.resId != b.resId) return false
                    if (a.resPackage != b.resPackage) return false
                }
                Icon.TYPE_URI -> {
                    if (a.uri.toString() != b.uri.toString()) return false
                }
                else -> {
                    if (a != b) return false
                }
            }
            return true
        }

        fun hashCode(a: Icon?): Int {
            if (a == null) return 0
            when (a.type) {
                Icon.TYPE_RESOURCE -> {
                    var result = a.type.hashCode()
                    result = 31 * result + a.resId.hashCode()
                    result = 31 * result + a.resPackage.hashCode()
                    return result
                }

                Icon.TYPE_URI -> {
                    var result = a.type.hashCode()
                    result = 31 * result + a.uri.toString().hashCode()
                    return result
                }

                else -> return a.hashCode()
            }
        }
    }
}

internal class IconHelperBeforeP {
    companion object {
        fun equals(a: Icon?, b: Icon?): Boolean = (a == b)

        fun hashCode(a: Icon?): Int = a?.hashCode() ?: 0
    }
}
