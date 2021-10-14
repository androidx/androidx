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

import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.annotation.RestrictTo
import java.time.Instant

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/** The builder for [WireComplicationData]. */
internal typealias WireComplicationDataBuilder =
    android.support.wearable.complications.ComplicationData.Builder

/** Base type for all different types of [ComplicationData] types. */
public sealed class ComplicationData constructor(
    public val type: ComplicationType,
    public val tapAction: PendingIntent?,
    internal var cachedWireComplicationData: WireComplicationData?,
    /**
     * Describes when the complication should be displayed.
     *
     * Whether the complication is active and should be displayed at the given time should be
     * checked with [TimeRange.contains].
     */
    public val validTimeRange: TimeRange = TimeRange.ALWAYS
) {
    /**
     * Converts this value to [WireComplicationData] object used for serialization.
     *
     * This is only needed internally to convert to the underlying communication protocol.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun asWireComplicationData(): WireComplicationData

    internal fun createWireComplicationDataBuilder(): WireComplicationDataBuilder =
        cachedWireComplicationData?.let {
            WireComplicationDataBuilder(it)
        } ?: WireComplicationDataBuilder(type.toWireComplicationType())
}

/**
 * Type that can be sent by any complication data source, regardless of the configured type, when
 * the complication data source has no data to be displayed. Watch faces may choose whether to
 * render this in some way or  leave the slot empty.
 */
public class NoDataComplicationData : ComplicationData(TYPE, null, null) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData = asPlainWireComplicationData(type)

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.NO_DATA
    }
}

/**
 * Type sent when the user has specified that an active complication should have no complication
 * data source, i.e. when the user has chosen "Empty" in the complication data source chooser.
 * Complication data sources cannot send data of this type.
 */
public class EmptyComplicationData : ComplicationData(TYPE, null, null) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData = asPlainWireComplicationData(type)

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.EMPTY
    }
}

/**
 * Type sent when a complication does not have a complication data source configured. The system
 * will send data of this type to watch faces when the user has not chosen a complication data
 * source for an active complication, and the watch face has not set a default complication data
 * source. Complication data sources cannot send data of this type.
 */
public class NotConfiguredComplicationData : ComplicationData(TYPE, null, null) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    public val monochromaticImage: MonochromaticImage?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [ShortTextComplicationData].
     *
     * You must at a minimum set the [text] and [contentDescription] fields.
     *
     * @param text The main localized [ComplicationText]. This must be less than 7 characters long
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val text: ComplicationText,
        private var contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        public fun setMonochromaticImage(monochromaticImage: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = monochromaticImage
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [ShortTextComplicationData]. */
        public fun build(): ShortTextComplicationData =
            ShortTextComplicationData(
                text,
                title,
                monochromaticImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            setShortText(text.toWireComplicationText())
            setShortTitle(title?.toWireComplicationText())
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            monochromaticImage?.addToWireComplicationData(this)
            setTapAction(tapAction)
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.SHORT_TEXT

        /** The maximum length of [ShortTextComplicationData.text] in characters. */
        @JvmField
        public val MAX_TEXT_LENGTH = 7
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
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [LongTextComplicationData].
     *
     * You must at a minimum set the [text] and [contentDescription] fields.
     *
     * @param text Localized main [ComplicationText] to display within the complication. There
     * isn't an explicit character limit but text may be truncated if too long
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val text: ComplicationText,
        private var contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
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

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
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
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            setLongText(text.toWireComplicationText())
            setLongTitle(title?.toWireComplicationText())
            monochromaticImage?.addToWireComplicationData(this)
            smallImage?.addToWireComplicationData(this)
            setTapAction(tapAction)
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

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
    public val monochromaticImage: MonochromaticImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [RangedValueComplicationData].
     *
     * You must at a minimum set the [value], [min], [max] and [contentDescription] fields.
     *
     * @param value The value of the ranged complication which should be in the range
     * [[min]] .. [[max]]
     * @param min The minimum value
     * @param max The maximum value
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val value: Float,
        private val min: Float,
        private val max: Float,
        private var contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Sets optional icon associated with the complication data. */
        public fun setMonochromaticImage(monochromaticImage: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = monochromaticImage
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional title associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply {
            this.text = text
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [RangedValueComplicationData]. */
        public fun build(): RangedValueComplicationData =
            RangedValueComplicationData(
                value,
                min,
                max,
                monochromaticImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            setRangedValue(value)
            setRangedMinValue(min)
            setRangedMaxValue(max)
            monochromaticImage?.addToWireComplicationData(this)
            setShortText(text?.toWireComplicationText())
            setShortTitle(title?.toWireComplicationText())
            setTapAction(tapAction)
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

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
    public val monochromaticImage: MonochromaticImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [MonochromaticImageComplicationData].
     *
     * You must at a minimum set the [monochromaticImage] and [contentDescription] fields.
     *
     * @param monochromaticImage The [MonochromaticImage] to be displayed
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val monochromaticImage: MonochromaticImage,
        private val contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): MonochromaticImageComplicationData =
            MonochromaticImageComplicationData(
                monochromaticImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            monochromaticImage.addToWireComplicationData(this)
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            setTapAction(tapAction)
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

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
    public val smallImage: SmallImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [SmallImageComplicationData].
     *
     * You must at a minimum set the [smallImage] and [contentDescription] fields.
     *
     * @param smallImage The [SmallImage] to be displayed
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val smallImage: SmallImage,
        private val contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): SmallImageComplicationData =
            SmallImageComplicationData(
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            smallImage.addToWireComplicationData(this)
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            setTapAction(tapAction)
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.SMALL_IMAGE
    }
}

/**
 * Type used for complications which consist only of an image that is expected to fill a large part
 * of the watch face, large enough to be shown as either a background or as part of a high
 * resolution complication.
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
    public val photoImage: Icon,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE, tapAction, cachedWireComplicationData, validTimeRange ?: TimeRange.ALWAYS
) {
    /**
     * Builder for [PhotoImageComplicationData].
     *
     * You must at a minimum set the [photoImage] and [contentDescription] fields.
     *
     * @param photoImage The [Icon] to be displayed
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val photoImage: Icon,
        private val contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var cachedWireComplicationData: WireComplicationData? = null

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

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [PhotoImageComplicationData]. */
        public fun build(): PhotoImageComplicationData =
            PhotoImageComplicationData(
                photoImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            setLargeImage(photoImage)
            setContentDescription(
                when (contentDescription) {
                    ComplicationText.EMPTY -> null
                    else -> contentDescription?.toWireComplicationText()
                }
            )
            setValidTimeRange(validTimeRange, this)
        }.build().also { cachedWireComplicationData = it }

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
 * should trigger a permission request. Note this is done by
 * [androidx.wear.watchface.ComplicationSlotsManager] for androidx watch faces.
 */
public class NoPermissionComplicationData internal constructor(
    public val text: ComplicationText?,
    public val title: ComplicationText?,
    public val monochromaticImage: MonochromaticImage?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(TYPE, null, cachedWireComplicationData) {
    /**
     * Builder for [NoPermissionComplicationData].
     *
     * You must at a minimum set the [tapAction].
     */
    public class Builder {
        private var text: ComplicationText? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var cachedWireComplicationData: WireComplicationData? = null

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply {
            this.text = text
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply {
            this.title = title
        }

        /** Sets optional icon associated with the complication data. */
        public fun setMonochromaticImage(monochromaticImage: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = monochromaticImage
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [NoPermissionComplicationData]. */
        public fun build(): NoPermissionComplicationData =
            NoPermissionComplicationData(
                text,
                title,
                monochromaticImage,
                cachedWireComplicationData
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData =
        createWireComplicationDataBuilder().apply {
            setShortText(text?.toWireComplicationText())
            setShortTitle(title?.toWireComplicationText())
            monochromaticImage?.addToWireComplicationData(this)
        }.build().also { cachedWireComplicationData = it }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.NO_PERMISSION
    }
}

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationData.toApiComplicationData(): ComplicationData {
    val wireComplicationData = this
    return when (type) {
        NoDataComplicationData.TYPE.toWireComplicationType() -> NoDataComplicationData()

        EmptyComplicationData.TYPE.toWireComplicationType() -> EmptyComplicationData()

        NotConfiguredComplicationData.TYPE.toWireComplicationType() ->
            NotConfiguredComplicationData()

        ShortTextComplicationData.TYPE.toWireComplicationType() ->
            ShortTextComplicationData.Builder(
                shortText!!.toApiComplicationText(),
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setTitle(shortTitle?.toApiComplicationText())
                setMonochromaticImage(parseIcon())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        LongTextComplicationData.TYPE.toWireComplicationType() ->
            LongTextComplicationData.Builder(
                longText!!.toApiComplicationText(),
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setTitle(longTitle?.toApiComplicationText())
                setMonochromaticImage(parseIcon())
                setSmallImage(parseSmallImage())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        RangedValueComplicationData.TYPE.toWireComplicationType() ->
            RangedValueComplicationData.Builder(
                value = rangedValue, min = rangedMinValue,
                max = rangedMaxValue,
                contentDescription = contentDescription?.toApiComplicationText()
                    ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setMonochromaticImage(parseIcon())
                setTitle(shortTitle?.toApiComplicationText())
                setText(shortText?.toApiComplicationText())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        MonochromaticImageComplicationData.TYPE.toWireComplicationType() ->
            MonochromaticImageComplicationData.Builder(
                parseIcon()!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        SmallImageComplicationData.TYPE.toWireComplicationType() ->
            SmallImageComplicationData.Builder(
                parseSmallImage()!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        PhotoImageComplicationData.TYPE.toWireComplicationType() ->
            PhotoImageComplicationData.Builder(
                largeImage!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        NoPermissionComplicationData.TYPE.toWireComplicationType() ->
            NoPermissionComplicationData.Builder().apply {
                setMonochromaticImage(parseIcon())
                setTitle(shortTitle?.toApiComplicationText())
                setText(shortText?.toApiComplicationText())
                setCachedWireComplicationData(wireComplicationData)
            }.build()

        else -> NoDataComplicationData()
    }
}

private fun WireComplicationData.parseTimeRange() =
    if ((startDateTimeMillis == 0L) and (endDateTimeMillis == Long.MAX_VALUE)) {
        null
    } else {
        TimeRange(
            Instant.ofEpochMilli(startDateTimeMillis),
            Instant.ofEpochMilli(endDateTimeMillis)
        )
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

/** Some of the types, do not have any fields. This method provides a shorthard for that case. */
internal fun asPlainWireComplicationData(type: ComplicationType) =
    WireComplicationDataBuilder(type.toWireComplicationType()).build()

internal fun setValidTimeRange(validTimeRange: TimeRange?, data: WireComplicationDataBuilder) {
    validTimeRange?.let {
        if (it.startDateTimeMillis > Instant.MIN) {
            data.setStartDateTimeMillis(it.startDateTimeMillis.toEpochMilli())
        }
        if (it.endDateTimeMillis != Instant.MAX) {
            data.setEndDateTimeMillis(it.endDateTimeMillis.toEpochMilli())
        }
    }
}
