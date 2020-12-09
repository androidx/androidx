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
public sealed class ComplicationData constructor(
    public val type: ComplicationType,
    public val tapAction: PendingIntent?,
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
    public abstract fun asWireComplicationData(): WireComplicationData

    /**
     * Returns true if the complication is active and should be displayed at the given time. If this
     * returns false, the complication should not be displayed.
     *
     * This must be checked for any time for which the complication will be displayed.
     */
    public fun isActiveAt(dateTimeMillis: Long): Boolean =
        validTimeRange?.contains(dateTimeMillis) ?: true
}

/** A pair of id and [ComplicationData]. */
public class IdAndComplicationData(
    public val complicationId: Int,
    public val complicationData: ComplicationData
) {
    /** Convenience constructor which accepts a [WireComplicationData]. */
    public constructor(
        complicationId: Int,
        complicationData: WireComplicationData
    ) : this(
        complicationId,
        complicationData.asApiComplicationData()
    )
}

/**
 * Type that can be sent by any provider, regardless of the configured type, when the provider
 * has no data to be displayed. Watch faces may choose whether to render this in some way or
 * leave the slot empty.
 */
public class NoDataComplicationData : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData = asPlainWireComplicationData(type)

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.NO_DATA
    }
}

/**
 * Type sent when the user has specified that an active complication should have no provider,
 * i.e. when the user has chosen "Empty" in the provider chooser. Providers cannot send data of
 * this type.
 */
public class EmptyComplicationData : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData = asPlainWireComplicationData(type)

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.EMPTY
    }
}

/**
 * Type sent when a complication does not have a provider configured. The system will send data
 * of this type to watch faces when the user has not chosen a provider for an active
 * complication, and the watch face has not set a default provider. Providers cannot send data
 * of this type.
 */
public class NotConfiguredComplicationData :
    ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData = asPlainWireComplicationData(type)

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.NOT_CONFIGURED
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
public class ShortTextComplicationData internal constructor(
    public val text: ComplicationText,
    public val title: ComplicationText?,
    public val image: MonochromaticImage?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [ShortTextComplicationData].
     *
     * You must at a minimum set the [text].
     */
    public class Builder(private val text: ComplicationText) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var image: MonochromaticImage? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        public fun setImage(image: MonochromaticImage?): Builder = apply {
            this.image = image
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [ShortTextComplicationData]. */
        public fun build(): ShortTextComplicationData =
            ShortTextComplicationData(
                text,
                title,
                image,
                contentDescription,
                tapAction,
                validTimeRange
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setShortText(text.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
            setContentDescription(contentDescription?.asWireComplicationText())
            image?.addToWireComplicationData(this)
            setTapAction(tapAction)
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.SHORT_TEXT
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
public class LongTextComplicationData internal constructor(
    public val text: ComplicationText,
    public val title: ComplicationText?,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [LongTextComplicationData].
     *
     * You must at a minimum set the [text].
     */
    public class Builder(private val text: ComplicationText) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional image associated with the complication data. */
        public fun setMonochromaticImage(icon: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = icon
        }

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [LongTextComplicationData]. */
        public fun build(): LongTextComplicationData =
            LongTextComplicationData(
                text,
                title,
                monochromaticImage,
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setLongText(text.asWireComplicationText())
            setLongTitle(title?.asWireComplicationText())
            monochromaticImage?.addToWireComplicationData(this)
            smallImage?.addToWireComplicationData(this)
            setTapAction(tapAction)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.LONG_TEXT
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
public class RangedValueComplicationData internal constructor(
    public val value: Float,
    public val min: Float,
    public val max: Float,
    public val image: MonochromaticImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [RangedValueComplicationData].
     *
     * You must at a minimum set the [value], [min], and [max] fields.
     */
    public class Builder(
        private val value: Float,
        private val min: Float,
        private val max: Float
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var image: MonochromaticImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional icon associated with the complication data. */
        public fun setImage(image: MonochromaticImage?): Builder = apply {
            this.image = image
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional title associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply {
            this.text = text
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [RangedValueComplicationData]. */
        public fun build(): RangedValueComplicationData =
            RangedValueComplicationData(
                value, min, max, image, title, text, contentDescription, tapAction, validTimeRange
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setRangedValue(value)
            setRangedMinValue(min)
            setRangedMaxValue(max)
            image?.addToWireComplicationData(this)
            setShortText(text?.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
            setTapAction(tapAction)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.RANGED_VALUE
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
public class MonochromaticImageComplicationData internal constructor(
    public val image: MonochromaticImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [MonochromaticImageComplicationData].
     *
     * You must at a minimum set the [image] field.
     */
    public class Builder(private val image: MonochromaticImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): MonochromaticImageComplicationData =
            MonochromaticImageComplicationData(image, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
            setTapAction(tapAction)
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.MONOCHROMATIC_IMAGE
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
public class SmallImageComplicationData internal constructor(
    public val image: SmallImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [SmallImageComplicationData].
     *
     * You must at a minimum set the [image] field.
     */
    public class Builder(private val image: SmallImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): SmallImageComplicationData =
            SmallImageComplicationData(image, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
            setTapAction(tapAction)
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.SMALL_IMAGE
    }
}

/**
 * Type used for complications which consist only of a [PhotoImage].
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
public class PhotoImageComplicationData internal constructor(
    public val image: PhotoImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?
) : ComplicationData(TYPE, tapAction, validTimeRange) {
    /**
     * Builder for [PhotoImageComplicationData].
     *
     * You must at a minimum set the [icon] field.
     */
    public class Builder(private val icon: PhotoImage) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var contentDescription: ComplicationText? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        @SuppressWarnings("MissingGetterMatchingBuilder") // See http://b/174052810
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @SuppressWarnings("MissingGetterMatchingBuilder") // See http://b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional content description associated with the complication data. */
        public fun setContentDescription(contentDescription: ComplicationText?): Builder = apply {
            this.contentDescription = contentDescription
        }

        /** Builds the [PhotoImageComplicationData]. */
        public fun build(): PhotoImageComplicationData =
            PhotoImageComplicationData(icon, contentDescription, tapAction, validTimeRange)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            image.addToWireComplicationData(this)
            setContentDescription(contentDescription?.asWireComplicationText())
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.PHOTO_IMAGE
    }
}

/**
 * Type sent by the system when the watch face does not have permission to receive complication
 * data.
 *
 * The text, title, and icon may be displayed by watch faces, but this is not required.
 *
 * It is recommended that, where possible, tapping on the complication when in this state
 * should trigger a permission request. A [ComplicationHelperActivity] may be used to make
 * this request and update all complications if the permission is granted.
 */
public class NoPermissionComplicationData internal constructor(
    public val text: ComplicationText?,
    public val title: ComplicationText?,
    public val image: MonochromaticImage?,
) : ComplicationData(TYPE, null, TimeRange.ALWAYS) {
    /**
     * Builder for [NoPermissionComplicationData].
     *
     * You must at a minimum set the [tapAction].
     */
    public class Builder {
        private var tapAction: PendingIntent? = null
        private var text: ComplicationText? = null
        private var title: ComplicationText? = null
        private var image: MonochromaticImage? = null

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply {
            this.text = text
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        public fun setImage(image: MonochromaticImage?): Builder = apply {
            this.image = image
        }

        /** Builds the [NoPermissionComplicationData]. */
        public fun build(): NoPermissionComplicationData =
            NoPermissionComplicationData(text, title, image)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun asWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(TYPE.asWireComplicationType()).apply {
            setShortText(text?.asWireComplicationText())
            setShortTitle(title?.asWireComplicationText())
            image?.addToWireComplicationData(this)
        }.build()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.NO_PERMISSION
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationData.asApiComplicationData(): ComplicationData =
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
                setContentDescription(contentDescription?.asApiComplicationText())
            }.build()

        LongTextComplicationData.TYPE.asWireComplicationType() ->
            LongTextComplicationData.Builder(longText!!.asApiComplicationText()).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setTitle(longTitle?.asApiComplicationText())
                setMonochromaticImage(parseIcon())
                setSmallImage(parseSmallImage())
                setContentDescription(contentDescription?.asApiComplicationText())
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
                setContentDescription(contentDescription?.asApiComplicationText())
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

        PhotoImageComplicationData.TYPE.asWireComplicationType() ->
            PhotoImageComplicationData.Builder(parseLargeImage()!!).apply {
                setValidTimeRange(parseTimeRange())
                setContentDescription(contentDescription?.asApiComplicationText())
            }.build()

        NoPermissionComplicationData.TYPE.asWireComplicationType() ->
            NoPermissionComplicationData.Builder().apply {
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
    largeImage?.let { PhotoImage.Builder(it).build() }

/** Some of the types, do not have any fields. This method provides a shorthard for that case. */
internal fun asPlainWireComplicationData(type: ComplicationType) =
    WireComplicationDataBuilder(type.asWireComplicationType()).build()
