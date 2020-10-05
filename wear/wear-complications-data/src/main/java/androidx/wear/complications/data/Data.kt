/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications.data

import android.app.PendingIntent
import androidx.annotation.RestrictTo

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/** The builder for [WireComplicationData]. */
internal typealias WireComplicationDataBuilder =
    android.support.wearable.complications.ComplicationData.Builder

/** Base type for all different types of [ComplicationData] types. */
sealed class ComplicationData constructor(
    val type: ComplicationType,
    val tapAction: PendingIntent?,
    private val validTimeRange: TimeRange?
) {
    /**
     * Converts this value to [WireComplicationData] object used for serialization.
     *
     * This is only needed internally to convert to the underlying communication protocol.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    abstract fun asWireComplicationData(): WireComplicationData

    /**
     * Returns true if the complication is active and should be displayed at the given time. If this
     * returns false, the complication should not be displayed.
     *
     * This must be checked for any time for which the complication will be displayed.
     */
    fun isActiveAt(dateTimeMillis: Long) = validTimeRange?.contains(dateTimeMillis) ?: true
}

/**
 * Type that can be sent by any provider, regardless of the configured type, when the provider
 * has no data to be displayed. Watch faces may choose whether to render this in some way or
 * leave the slot empty.
 */
class NoDataComplicationData : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /* @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() = asPlainWireComplicationData(type)

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.NO_DATA
    }
}

/**
 * Type sent when the user has specified that an active complication should have no provider,
 * i.e. when the user has chosen "Empty" in the provider chooser. Providers cannot send data of
 * this type.
 */
class EmptyComplicationData : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /* @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() = asPlainWireComplicationData(type)

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.EMPTY
    }
}

/**
 * Type sent when a complication does not have a provider configured. The system will send data
 * of this type to watch faces when the user has not chosen a provider for an active
 * complication, and the watch face has not set a default provider. Providers cannot send data
 * of this type.
 */
class NotConfiguredComplicationData : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /* @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() = asPlainWireComplicationData(type)

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.NOT_CONFIGURED
    }
}

/**
 * Type used for complications where the primary piece of data is a short piece of text
 * (expected to be no more than seven characters in length). The text may be accompanied
 * by an icon or a title or both.
 *
 * If only one of icon and title is provided, it is expected that it will be displayed. If both
 * are provided, it is expected that at least one of these will be displayed.
 */
class ShortTextComplicationData internal constructor(
    val text: ComplicationText,
    val title: ComplicationText?,
    val image: MonochromaticImage?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [ShortTextComplicationData].
     *
     * You must at a minimum set the [text].
     */
    class Builder(private val text: ComplicationText) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var image: MonochromaticImage? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional title associated with the complication data. */
        fun setTitle(title: ComplicationText?) = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        fun setImage(image: MonochromaticImage?) = apply {
            this.image = image
        }

        /** Builds the [ShortTextComplicationData]. */
        fun build() = ShortTextComplicationData(text, title, image, tapAction, validTimeRange)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setShortText(text.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
            image?.addToWireComplicationData(this)
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.SHORT_TEXT
    }
}

/**
 * Type used for complications where the primary piece of data is a piece of text. The text may
 * be accompanied by an icon and/or a title.
 *
 * The text is expected to always be displayed.
 *
 * The title, if provided, it is expected that this field will be displayed.
 *
 * If at least one of the icon and image is provided, one of these should be displayed.
 */
class LongTextComplicationData internal constructor(
    val text: ComplicationText,
    val title: ComplicationText?,
    val monochromaticImage: MonochromaticImage?,
    val smallImage: SmallImage?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [LongTextComplicationData].
     *
     * You must at a minimum set the [text].
     */
    class Builder(private val text: ComplicationText) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional title associated with the complication data. */
        fun setTitle(title: ComplicationText?) = apply {
            this.title = title
        }

        /** Sets optional image associated with the complication data. */
        fun setMonochromaticImage(icon: MonochromaticImage?) = apply {
            this.monochromaticImage = icon
        }

        /** Sets optional image associated with the complication data. */
        fun setSmallImage(smallImage: SmallImage?) = apply {
            this.smallImage = smallImage
        }

        /** Builds the [LongTextComplicationData]. */
        fun build() =
            LongTextComplicationData(
                text,
                title,
                monochromaticImage,
                smallImage,
                tapAction,
                validTimeRange
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setLongText(text.asWireComplicationText())
            setLongTitle(title?.asWireComplicationText())
            monochromaticImage?.addToWireComplicationData(this)
            smallImage?.addToWireComplicationData(this)
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.LONG_TEXT
    }
}

/**
 * Type used for complications including a numerical value within a range, such as a percentage.
 * The value may be accompanied by an icon and/or short text and title.
 *
 * The [value], [min], and [max] fields are required for this type and the value within the
 * range is expected to always be displayed.
 *
 * The icon, title, and text fields are optional and the watch face may choose which of these
 * fields to display, if any.
 */
class RangedValueComplicationData internal constructor(
    val value: Float,
    val min: Float,
    val max: Float,
    val image: MonochromaticImage?,
    val title: ComplicationText?,
    val text: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [RangedValueComplicationData].
     *
     * You must at a minimum set the [value], [min], and [max] fields.
     */
    class Builder(
        private val value: Float,
        private val min: Float,
        private val max: Float
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var image: MonochromaticImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional icon associated with the complication data. */
        fun setImage(image: MonochromaticImage?) = apply {
            this.image = image
        }

        /** Sets optional title associated with the complication data. */
        fun setTitle(title: ComplicationText?) = apply {
            this.title = title
        }

        /** Sets optional title associated with the complication data. */
        fun setText(text: ComplicationText?) = apply {
            this.text = text
        }

        /** Builds the [RangedValueComplicationData]. */
        fun build() =
            RangedValueComplicationData(
                value, min, max, image, title, text, tapAction, validTimeRange
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setRangedValue(value)
            setRangedMinValue(min)
            setRangedMaxValue(max)
            image?.addToWireComplicationData(this)
            setShortText(text?.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.RANGED_VALUE
    }
}

/**
 * Type used for complications which consist only of a [MonochromaticImage].
 *
 * The image is expected to always be displayed.
 *
 * The contentDescription field and is used to describe what data the icon represents. If the
 * icon is purely stylistic, and does not convey any information to the user, then provide an
 * empty content description. If no content description is provided, a generic content
 * description will be used instead.
 */
class MonochromaticImageComplicationData internal constructor(
    val image: MonochromaticImage,
    val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [MonochromaticImageComplicationData].
     *
     * You must at a minimum set the [image] field.
     */
    class Builder(private val image: MonochromaticImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        fun setContentDescription(contentDescription: ComplicationText?) = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        fun build() =
            MonochromaticImageComplicationData(image, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.MONOCHROMATIC_IMAGE
    }
}

/**
 * Type used for complications which consist only of a [SmallImage].
 *
 * The image is expected to always be displayed.
 *
 * The [contentDescription] field and is used to describe what data the icon represents. If the
 * icon is purely stylistic, and does not convey any information to the user, then provide an
 * empty content description. If no content description is provided, a generic content
 * description will be used instead.
 */
class SmallImageComplicationData internal constructor(
    val image: SmallImage,
    val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [SmallImageComplicationData].
     *
     * You must at a minimum set the [image] field.
     */
    class Builder(private val image: SmallImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        fun setContentDescription(contentDescription: ComplicationText?) = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        fun build() =
            SmallImageComplicationData(image, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.SMALL_IMAGE
    }
}

/**
 * Type used for complications which consist only of a [BackgroundImage].
 *
 * The image is expected to always be displayed. The image may be shown as the background, any
 * other part of the watch face or within a complication. The image is large enough to be cover
 * the entire screen. The image may be cropped to fit the watch face or complication.
 *
 * The [contentDescription] field and is used to describe what data the icon represents. If the
 * icon is purely stylistic, and does not convey any information to the user, then provide an
 * empty content description. If no content description is provided, a generic content
 * description will be used instead.
 */
class BackgroundImageComplicationData internal constructor(
    val image: BackgroundImage,
    val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [BackgroundImageComplicationData].
     *
     * You must at a minimum set the [icon] field.
     */
    class Builder(private val icon: BackgroundImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        fun setValidTimeRange(validTimeRange: TimeRange?) = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        fun setContentDescription(contentDescription: ComplicationText?) = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [BackgroundImageComplicationData]. */
        fun build() =
            BackgroundImageComplicationData(icon, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.BACKGROUND_IMAGE
    }
}

/**
 * Type sent by the system when the watch face does not have permission to receive complication
 * data.
 *
 * The text, title, and icon may be displayed by watch faces, but this is not required.
 *
 * It is recommended that, where possible, tapping on the complication when in this state
 * should trigger a permission request. A {@link ComplicationHelperActivity} may be used to make
 * this request and update all complications if the permission is granted.
 */
class NoPermissionComplicationData internal constructor(
    val text: ComplicationText?,
    val title: ComplicationText?,
    val image: MonochromaticImage?,
    tapAction: PendingIntent?
) : ComplicationData(TYPE, tapAction, TimeRange.ALWAYS) {
    /**
     * Builder for [NoPermissionComplicationData].
     *
     * You must at a minimum set the [tapAction].
     */
    class Builder {
        private var tapAction: PendingIntent? = null
        private var text: ComplicationText? = null
        private var title: ComplicationText? = null
        private var image: MonochromaticImage? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        fun setTapAction(tapAction: PendingIntent?) = apply {
            this.tapAction = tapAction
        }

        /** Sets optional text associated with the complication data. */
        fun setText(text: ComplicationText?) = apply {
            this.text = text
        }

        /** Sets optional title associated with the complication data. */
        fun setTitle(title: ComplicationText?) = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        fun setImage(image: MonochromaticImage?) = apply {
            this.image = image
        }

        /** Builds the [NoPermissionComplicationData]. */
        fun build() = NoPermissionComplicationData(text, title, image, tapAction)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData() =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setShortText(text?.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
            image?.addToWireComplicationData(this)
        }.build()

    companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        val TYPE = ComplicationType.NO_PERMISSION
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun WireComplicationData.asApiComplicationData(): ComplicationData =
    when (type) {
        NoDataComplicationData.TYPE.asWireComplicationType() -> NoDataComplicationData()

        EmptyComplicationData.TYPE.asWireComplicationType() -> EmptyComplicationData()

        NotConfiguredComplicationData.TYPE.asWireComplicationType() ->
            NotConfiguredComplicationData()

        ShortTextComplicationData.TYPE.asWireComplicationType() ->
            ShortTextComplicationData.Builder(shortText!!.asApiComplicationText()).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setTitle(shortTitle?.asApiComplicationText())
                setImage(parseIcon())
            }.build()

        LongTextComplicationData.TYPE.asWireComplicationType() ->
            LongTextComplicationData.Builder(longText!!.asApiComplicationText()).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setTitle(longTitle?.asApiComplicationText())
                setMonochromaticImage(parseIcon())
                setSmallImage(parseSmallImage())
            }.build()

        RangedValueComplicationData.TYPE.asWireComplicationType() ->
            RangedValueComplicationData.Builder(
                value = rangedValue, min = rangedMinValue,
                max = rangedMaxValue
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setImage(parseIcon())
                setTitle(shortTitle?.asApiComplicationText())
                setText(shortText?.asApiComplicationText())
            }.build()

        MonochromaticImageComplicationData.TYPE.asWireComplicationType() ->
            MonochromaticImageComplicationData.Builder(parseIcon()!!).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setContentDescription(contentDescription?.asApiComplicationText())
            }.build()

        SmallImageComplicationData.TYPE.asWireComplicationType() ->
            SmallImageComplicationData.Builder(parseSmallImage()!!).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setContentDescription(contentDescription?.asApiComplicationText())
            }.build()

        BackgroundImageComplicationData.TYPE.asWireComplicationType() ->
            BackgroundImageComplicationData.Builder(parseLargeImage()!!).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setContentDescription(contentDescription?.asApiComplicationText())
            }.build()

        NoPermissionComplicationData.TYPE.asWireComplicationType() ->
            NoPermissionComplicationData.Builder().apply {
                setTapAction(tapAction)
                setImage(parseIcon())
                setTitle(shortTitle?.asApiComplicationText())
                setText(shortText?.asApiComplicationText())
            }.build()

        else -> NoDataComplicationData()
    }

private fun WireComplicationData.parseTimeRange() =
    if ((startDateTimeMillis == 0L) and (endDateTimeMillis == Long.MAX_VALUE)) {
        null
    } else {
        TimeRange(startDateTimeMillis, endDateTimeMillis)
    }

private fun WireComplicationData.parseIcon() =
    icon?.let {
        MonochromaticImage.Builder(it).apply {
            setAmbientImage(burnInProtectionIcon)
        }.build()
    }

private fun WireComplicationData.parseSmallImage() =
    smallImage?.let {
        val imageStyle = when (smallImageStyle) {
            WireComplicationData.IMAGE_STYLE_ICON -> SmallImageType.ICON
            WireComplicationData.IMAGE_STYLE_PHOTO -> SmallImageType.PHOTO
            else -> SmallImageType.PHOTO
        }
        SmallImage.Builder(it, imageStyle).apply {
            setAmbientImage(burnInProtectionSmallImage)
        }.build()
    }

private fun WireComplicationData.parseLargeImage() =
    largeImage?.let { BackgroundImage.Builder(it).build() }

/** Some of the types, do not have any fields. This method provides a shorthard for that case. */
internal fun asPlainWireComplicationData(type: ComplicationType) =
    WireComplicationDataBuilder(type.asWireComplicationType()).build()
