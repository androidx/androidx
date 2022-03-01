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
import android.os.Build
import androidx.annotation.RestrictTo
import java.time.Instant

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/** The builder for [WireComplicationData]. */
internal typealias WireComplicationDataBuilder =
    android.support.wearable.complications.ComplicationData.Builder

/**
 * Base type for all different types of [ComplicationData] types.
 *
 * Please note to aid unit testing of ComplicationDataSourceServices, [equals], [hashCode] and
 * [toString] have been overridden for all the types of ComplicationData, however due to the
 * embedded [Icon] class we have to fall back to reference equality and hashing below API 28 and
 * also for the [Icon]s that don't use either a resource or a uri (these should be rare but they
 * can exist).
 *
 * @property type The [ComplicationType] of this complication data.
 * @property tapAction The [PendingIntent] to send when the complication is tapped on.
 * @property validTimeRange The [TimeRange] within which the complication should be displayed.
 * Whether the complication is active and should be displayed at the given time should be
 * checked with [TimeRange.contains].
 */
public sealed class ComplicationData constructor(
    public val type: ComplicationType,
    public val tapAction: PendingIntent?,
    internal var cachedWireComplicationData: WireComplicationData?,
    public val validTimeRange: TimeRange = TimeRange.ALWAYS
) {
    /**
     * [tapAction] which is a [PendingIntent] unfortunately can't be serialized. This property is
     * 'true' if tapAction has been lost due to serialization (typically because it has been cached
     * locally). When 'true' the watch face should render the complication differently (e.g. as
     * semi-transparent or grayed out) to signal to the user it can't be tapped. The system will
     * subsequently deliver an updated complication, with a tapAction where applicable.
     */
    @get:JvmName("isTapActionLostDueToSerialization")
    public var tapActionLostDueToSerialization: Boolean =
        cachedWireComplicationData?.tapActionLostDueToSerialization ?: false

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

    internal open fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
    }

    internal fun asPlaceholderWireComplicationData(): WireComplicationData =
        WireComplicationDataBuilder(NoDataComplicationData.TYPE.toWireComplicationType()).apply {
            fillWireComplicationDataBuilder(this)
        }.build()

    /**
     * Returns `true` if any of the fields of this ComplicationData are placeholders. I.e. if any
     * fields are equal to: [ComplicationText.PLACEHOLDER], [SmallImage.PLACEHOLDER],
     * [MonochromaticImage.PLACEHOLDER], [PhotoImageComplicationData.PLACEHOLDER], or
     * [RangedValueComplicationData.PLACEHOLDER].
     */
    open fun hasPlaceholderFields(): Boolean = false

    /**
     * Returns the next [Instant] after [afterInstant] at which any field of the complication may
     * change. If there's no scheduled changes then [Instant.MAX] will be returned.
     *
     * See [ComplicationText.getNextChangeTime]
     *
     * @param afterInstant The earliest [Instant] for which we're interested in changes
     */
    public open fun getNextChangeInstant(afterInstant: Instant): Instant = Instant.MAX
}

/**
 * Type that can be sent by any complication data source, regardless of the configured type, when
 * the complication data source has no data to be displayed. If no [placeholder] is included then
 * watch faces may choose whether to render this in some way or leave the slot empty.
 *
 * If a [placeholder] is included than its expected that it will be rendered. Its suggested the
 * watch face renders the placeholder elements (text, title, smallImage, etc...) using solid grey
 * blocks. Any non-placeholder elements included in [placeholder] must be rendered normally.
 *
 * Some watchfaces may not support placeholders and in that case the NoDataComplicationData will
 * be treated as being empty.
 *
 * @property placeholder An optional [ComplicationData] which may contain placeholder fields (see
 * [hasPlaceholderFields]). The type of the placeholder must match the type of the ComplicationData
 * that would have otherwise been sent. The placeholder is expected to be rendered if the watch
 * face has been built with a compatible library, older libraries which don't support placeholders
 * will ignore this field.
 * @property contentDescription An optional localized [ComplicationText] label describing the
 * [placeholder] for use by screen readers.
 */
public class NoDataComplicationData internal constructor(
    public val placeholder: ComplicationData?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(TYPE, tapAction, cachedWireComplicationData) {

    /** Constructs a NoDataComplicationData without a [placeholder]. */
    constructor() : this(null, null, null, null)

    /**
     * Constructs a NoDataComplicationData with a [placeholder] [ComplicationData] which is allowed
     * to contain placeholder fields (see [hasPlaceholderFields]) which must be drawn to look like
     * placeholders. E.g. with grey boxes / arcs.
     */
    constructor(placeholder: ComplicationData) : this(
        placeholder,
        when (placeholder) {
            is ShortTextComplicationData -> placeholder.contentDescription
            is LongTextComplicationData -> placeholder.contentDescription
            is RangedValueComplicationData -> placeholder.contentDescription
            is MonochromaticImageComplicationData -> placeholder.contentDescription
            is SmallImageComplicationData -> placeholder.contentDescription
            is PhotoImageComplicationData -> placeholder.contentDescription
            else -> null
        },
        placeholder.tapAction,
        null
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            if (placeholder == null) {
                setPlaceholderType(TYPE.toWireComplicationType())
            } else {
                setPlaceholderType(placeholder.type.toWireComplicationType())
                placeholder.fillWireComplicationDataBuilder(this)
            }
        }.build().also { cachedWireComplicationData = it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoDataComplicationData

        if (placeholder != other.placeholder) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholder.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "NoDataComplicationData(" +
            "placeholder=$placeholder, contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "EmptyComplicationData()"
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "NotConfiguredComplicationData()"
    }

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
 *
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 * as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 * grey box.
 * @property title The optional title [ComplicationText]. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated. If the title is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 * as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 * grey box.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 * face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 * treat it as a placeholder rather than rendering normally, its suggested it should be rendered as
 * a light grey box.
 * @property contentDescription The content description field for accessibility.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setShortText(text.toWireComplicationText())
        builder.setShortTitle(title?.toWireComplicationText())
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        monochromaticImage?.addToWireComplicationData(builder)
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortTextComplicationData

        if (text != other.text) return false
        if (title != other.title) return false
        if (monochromaticImage != other.monochromaticImage) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "ShortTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

    override fun hasPlaceholderFields() = text.isPlaceholder() || title?.isPlaceholder() == true ||
        monochromaticImage?.isPlaceholder() == true

    override fun getNextChangeInstant(afterInstant: Instant): Instant {
        if (title != null) {
            val titleChangeInstant = title.getNextChangeTime(afterInstant)
            val textChangeInstant = text.getNextChangeTime(afterInstant)
            return if (textChangeInstant.isBefore(titleChangeInstant)) {
                textChangeInstant
            } else {
                titleChangeInstant
            }
        } else {
            return text.getNextChangeTime(afterInstant)
        }
    }

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
 *
 * @property text The body [ComplicationText] of the complication. If the text is equal to
 * [ComplicationText.PLACEHOLDER] the renderer must treat it as a placeholder rather than rendering
 * normally, its suggested it should be rendered as a light grey box.
 * @property title The optional title [ComplicationText]. If the title is equal to
 * [ComplicationText.PLACEHOLDER] the renderer must treat it as a placeholder rather than rendering
 * normally, its suggested it should be rendered as a light grey box.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 * face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 * treat it as a placeholder rather than rendering normally, its suggested it should be rendered as
 * a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 * occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 * renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 * be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setLongText(text.toWireComplicationText())
        builder.setLongTitle(title?.toWireComplicationText())
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongTextComplicationData

        if (text != other.text) return false
        if (title != other.title) return false
        if (monochromaticImage != other.monochromaticImage) return false
        if (smallImage != other.smallImage) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + (smallImage?.hashCode() ?: 0)
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "LongTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

    override fun hasPlaceholderFields() = text.isPlaceholder() || title?.isPlaceholder() == true ||
        monochromaticImage?.isPlaceholder() == true || smallImage?.isPlaceholder() == true

    override fun getNextChangeInstant(afterInstant: Instant): Instant {
        if (title != null) {
            val titleChangeInstant = title.getNextChangeTime(afterInstant)
            val textChangeInstant = text.getNextChangeTime(afterInstant)
            return if (textChangeInstant.isBefore(titleChangeInstant)) {
                textChangeInstant
            } else {
                titleChangeInstant
            }
        } else {
            return text.getNextChangeTime(afterInstant)
        }
    }

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
 *
 * @property value The [Float] value of this complication which is >= [min] and <= [max] or equal to
 * [PLACEHOLDER]. If it's equal to [PLACEHOLDER] the renderer must treat it as a placeholder rather
 * than rendering normally, its suggested to be drawn as a grey arc with a percentage value selected
 * by the renderer.
 * @property min The minimum [Float] value for this complication.
 * @property max The maximum [Float] value for this complication.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 * face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 * treat it as a placeholder rather than rendering normally, its suggested it should be rendered as
 * a light grey box.
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as a
 * placeholder rather than rendering normally, its suggested it should be rendered as a light grey
 * box.
 * @property title The optional title [ComplicationText]. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as a
 * placeholder rather than rendering normally, its suggested it should be rendered as a light grey
 * box.
 * @property contentDescription The content description field for accessibility.
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
     * @param max The maximum value. This must be less than [Float.MAX_VALUE].
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

        init {
            require(max != Float.MAX_VALUE) {
                "Float.MAX_VALUE is reserved and can't be used for max"
            }
        }

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
    public override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setRangedValue(value)
        builder.setRangedMinValue(min)
        builder.setRangedMaxValue(max)
        monochromaticImage?.addToWireComplicationData(builder)
        builder.setShortText(text?.toWireComplicationText())
        builder.setShortTitle(title?.toWireComplicationText())
        builder.setTapAction(tapAction)
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RangedValueComplicationData

        if (value != other.value) return false
        if (min != other.min) return false
        if (max != other.max) return false
        if (monochromaticImage != other.monochromaticImage) return false
        if (title != other.title) return false
        if (text != other.text) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + min.hashCode()
        result = 31 * result + max.hashCode()
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "RangedValueComplicationData(value=$value, min=$min, max=$max, " +
            "monochromaticImage=$monochromaticImage, title=$title, text=$text, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

    override fun hasPlaceholderFields() = value == PLACEHOLDER || text?.isPlaceholder() == true ||
        title?.isPlaceholder() == true || monochromaticImage?.isPlaceholder() == true

    override fun getNextChangeInstant(afterInstant: Instant): Instant {
        val titleChangeInstant = title?.getNextChangeTime(afterInstant) ?: Instant.MAX
        val textChangeInstant = text?.getNextChangeTime(afterInstant) ?: Instant.MAX
        return if (textChangeInstant.isBefore(titleChangeInstant)) {
            textChangeInstant
        } else {
            titleChangeInstant
        }
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.RANGED_VALUE

        /**
         * Used to signal the range should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER = Float.MAX_VALUE
    }
}

/**
 * Type used for complications which consist only of a [MonochromaticImage].
 *
 * The image is expected to always be displayed.
 *
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 * face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 * treat it as a placeholder rather than rendering normally, its suggested it should be rendered as
 * a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 * describe what data the icon represents. If the icon is purely stylistic, and does not convey any
 * information to the user, then provide an empty content description. If no content description is
 * provided, a generic content description will be used instead.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        monochromaticImage.addToWireComplicationData(builder)
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MonochromaticImageComplicationData

        if (monochromaticImage != other.monochromaticImage) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = monochromaticImage.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun hasPlaceholderFields() = monochromaticImage.isPlaceholder()

    override fun toString(): String {
        return "MonochromaticImageComplicationData(monochromaticImage=$monochromaticImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

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
 * @property smallImage The [SmallImage] that is expected to cover a small fraction of a watch face
 * occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 * renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 * be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 * describe what data the image represents. If the image is purely stylistic, and does not convey
 * any information to the user, then provide an empty content description. If no content description
 * is provided, a generic content description will be used instead.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        smallImage.addToWireComplicationData(builder)
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SmallImageComplicationData

        if (smallImage != other.smallImage) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = smallImage.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "SmallImageComplicationData(smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

    override fun hasPlaceholderFields() = smallImage.isPlaceholder()

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
 * @property photoImage The [Icon] that is expected to fill a large part of the watch face, large
 * enough to be shown as either a background or as part of a high resolution complication. This must
 * not be tinted. If the photoImage is equal to [PhotoImageComplicationData.PLACEHOLDER] the
 * renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 * be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 * describe what data the image represents. If the image is purely stylistic, and does not convey
 * any information to the user, then provide an empty content description. If no content description
 * is provided, a generic content description will be used instead.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            fillWireComplicationDataBuilder(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setLargeImage(photoImage)
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhotoImageComplicationData

        if (!if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                IconHelperP.equals(photoImage, other.photoImage)
            } else {
                IconHelperBeforeP.equals(photoImage, other.photoImage)
            }
        ) return false

        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var result = IconHelperP.hashCode(photoImage)
            result = 31 * result + (contentDescription?.hashCode() ?: 0)
            result = 31 * result + tapActionLostDueToSerialization.hashCode()
            result = 31 * result + (tapAction?.hashCode() ?: 0)
            result = 31 * result + validTimeRange.hashCode()
            result
        } else {
            var result = IconHelperBeforeP.hashCode(photoImage)
            result = 31 * result + (contentDescription?.hashCode() ?: 0)
            result = 31 * result + tapActionLostDueToSerialization.hashCode()
            result = 31 * result + (tapAction?.hashCode() ?: 0)
            result = 31 * result + validTimeRange.hashCode()
            result
        }
    }

    override fun toString(): String {
        return "PhotoImageComplicationData(photoImage=$photoImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange)"
    }

    override fun hasPlaceholderFields() = photoImage.isPlaceholder()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.PHOTO_IMAGE

        /**
         * Used to signal the photo image should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER: Icon = createPlaceholderIcon()
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
 *
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated.
 * @property title The optional title [ComplicationText]. The length of the text, including
 * any time-dependent values at any valid time, is expected to not exceed seven characters. When
 * using this text, the watch face should be able to display any string of up to seven characters
 * (reducing the text size appropriately if the string is very wide). Although not expected, it is
 * possible that strings of more than seven characters might be seen, in which case they may be
 * truncated.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 * face.
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
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            setShortText(text?.toWireComplicationText())
            setShortTitle(title?.toWireComplicationText())
            monochromaticImage?.addToWireComplicationData(this)
        }.build().also { cachedWireComplicationData = it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoPermissionComplicationData

        if (text != other.text) return false
        if (title != other.title) return false
        if (monochromaticImage != other.monochromaticImage) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "NoPermissionComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, tapActionLostDueToSerialization=" +
            "$tapActionLostDueToSerialization, tapAction=$tapAction, " +
            "validTimeRange=$validTimeRange)"
    }

    override fun getNextChangeInstant(afterInstant: Instant): Instant {
        val titleChangeInstant = title?.getNextChangeTime(afterInstant) ?: Instant.MAX
        val textChangeInstant = text?.getNextChangeTime(afterInstant) ?: Instant.MAX
        return if (textChangeInstant.isBefore(titleChangeInstant)) {
            textChangeInstant
        } else {
            titleChangeInstant
        }
    }

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
        NoDataComplicationData.TYPE.toWireComplicationType() -> {
            if (hasPlaceholderType()) {
                val placeholder = when (placeholderType) {
                    NoDataComplicationData.TYPE.toWireComplicationType() -> null

                    ShortTextComplicationData.TYPE.toWireComplicationType() -> {
                        ShortTextComplicationData.Builder(
                            shortText!!.toApiComplicationTextPlaceholderAware(),
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY
                        ).apply {
                            setMonochromaticImage(parseIconPlaceholderAware())
                            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
                        }.build()
                    }

                    LongTextComplicationData.TYPE.toWireComplicationType() -> {
                        LongTextComplicationData.Builder(
                            longText!!.toApiComplicationTextPlaceholderAware(),
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY
                        ).apply {
                            setMonochromaticImage(parseIconPlaceholderAware())
                            setSmallImage(parseSmallImagePlaceholderAware())
                            setTitle(longTitle?.toApiComplicationTextPlaceholderAware())
                        }.build()
                    }

                    RangedValueComplicationData.TYPE.toWireComplicationType() ->
                        RangedValueComplicationData.Builder(
                            value = rangedValue,
                            min = rangedMinValue,
                            max = rangedMaxValue,
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY
                        ).apply {
                            setMonochromaticImage(parseIconPlaceholderAware())
                            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
                            setText(shortText?.toApiComplicationTextPlaceholderAware())
                        }.build()

                    MonochromaticImageComplicationData.TYPE.toWireComplicationType() ->
                        MonochromaticImageComplicationData(
                            parseIconPlaceholderAware()!!,
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY,
                            tapAction,
                            parseTimeRange(),
                            wireComplicationData
                        )

                    SmallImageComplicationData.TYPE.toWireComplicationType() ->
                        SmallImageComplicationData(
                            parseSmallImagePlaceholderAware()!!,
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY,
                            tapAction,
                            parseTimeRange(),
                            wireComplicationData
                        )

                    PhotoImageComplicationData.TYPE.toWireComplicationType() ->
                        PhotoImageComplicationData(
                            parseLargeImagePlaceholderAware()!!,
                            contentDescription?.toApiComplicationText()
                                ?: ComplicationText.EMPTY,
                            tapAction,
                            parseTimeRange(),
                            wireComplicationData
                        )

                    else -> throw IllegalStateException(
                        "Unrecognized placeholderType $placeholderType"
                    )
                }

                if (placeholder != null) {
                    NoDataComplicationData(placeholder)
                } else {
                    NoDataComplicationData()
                }
            } else {
                NoDataComplicationData()
            }
        }

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
                setTapAction(tapAction)
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

private fun WireComplicationData.parseIconPlaceholderAware() =
    icon?.let {
        if (it.isPlaceholder()) {
            MonochromaticImage.PLACEHOLDER
        } else {
            MonochromaticImage.Builder(it).apply {
                setAmbientImage(burnInProtectionIcon)
            }.build()
        }
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

private fun WireComplicationData.parseSmallImagePlaceholderAware() =
    smallImage?.let {
        if (it.isPlaceholder()) {
            SmallImage.PLACEHOLDER
        } else {
            val imageStyle = when (smallImageStyle) {
                WireComplicationData.IMAGE_STYLE_ICON -> SmallImageType.ICON
                WireComplicationData.IMAGE_STYLE_PHOTO -> SmallImageType.PHOTO
                else -> SmallImageType.PHOTO
            }
            SmallImage.Builder(it, imageStyle).apply {
                setAmbientImage(burnInProtectionSmallImage)
            }.build()
        }
    }

private fun WireComplicationData.parseLargeImagePlaceholderAware() =
    largeImage?.let {
        if (it.isPlaceholder()) {
            PhotoImageComplicationData.PLACEHOLDER
        } else {
            it
        }
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
