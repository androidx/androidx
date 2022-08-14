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
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
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
 * @property dataSource The [ComponentName] of the
 * [androidx.wear.watchface.complications.datasource.ComplicationDataSourceService] that provided
 * the ComplicationData. This may be `null` when run on old systems.
 */
public sealed class ComplicationData constructor(
    public val type: ComplicationType,
    public val tapAction: PendingIntent?,
    internal var cachedWireComplicationData: WireComplicationData?,
    public val validTimeRange: TimeRange = TimeRange.ALWAYS,
    public val dataSource: ComponentName?
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
        } ?: WireComplicationDataBuilder(type.toWireComplicationType()).apply {
            setDataSource(dataSource)
        }

    internal open fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
    }

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
     * @param afterInstant The reference [Instant], after which changes will be reported.
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
 */
public class NoDataComplicationData internal constructor(
    public val placeholder: ComplicationData?,
    cachedWireComplicationData: WireComplicationData?
) : ComplicationData(
    TYPE,
    placeholder?.tapAction,
    cachedWireComplicationData,
    dataSource = null
) {

    /** Constructs a NoDataComplicationData without a [placeholder]. */
    constructor() : this(null, null)

    /**
     * Constructs a NoDataComplicationData with a [placeholder] [ComplicationData] which is allowed
     * to contain placeholder fields (see [hasPlaceholderFields]) which must be drawn to look like
     * placeholders. E.g. with grey boxes / arcs.
     */
    constructor(placeholder: ComplicationData) : this(
        placeholder,
        null
    )

    @OptIn(ComplicationExperimental::class)
    val contentDescription: ComplicationText? =
        when (placeholder) {
            is ShortTextComplicationData -> placeholder.contentDescription
            is LongTextComplicationData -> placeholder.contentDescription
            is RangedValueComplicationData -> placeholder.contentDescription
            is MonochromaticImageComplicationData -> placeholder.contentDescription
            is SmallImageComplicationData -> placeholder.contentDescription
            is PhotoImageComplicationData -> placeholder.contentDescription
            is GoalProgressComplicationData -> placeholder.contentDescription
            is DiscreteRangedValueComplicationData -> placeholder.contentDescription
            else -> null
        }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder().apply {
            if (placeholder == null) {
                setPlaceholder(null)
            } else {
                val builder = placeholder.createWireComplicationDataBuilder()
                placeholder.fillWireComplicationDataBuilder(builder)
                setPlaceholder(builder.build())
            }
        }.build().also { cachedWireComplicationData = it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoDataComplicationData

        if (placeholder != other.placeholder) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholder.hashCode()
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "NoDataComplicationData(" +
            "placeholder=$placeholder, " +
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
public class EmptyComplicationData : ComplicationData(
    TYPE,
    tapAction = null,
    cachedWireComplicationData = null,
    dataSource = null
) {
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
public class NotConfiguredComplicationData : ComplicationData(
    TYPE,
    tapAction = null,
    cachedWireComplicationData = null,
    dataSource = null
) {
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
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
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
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
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

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
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "ShortTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
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
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
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
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

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
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "LongTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
 * Describes an optional simple linear color ramp. For [RangedValueComplicationData]
 * [RangedValueComplicationData.min] is rendered with [minColor] and
 * [RangedValueComplicationData.max] with [maxColor]. For [GoalProgressComplicationData] 0 is
 * rendered with [minColor] and [GoalProgressComplicationData.targetValue] is renderd with [maxColor].Y
 *
 * This is a rendering hint that would override the normal watch face colors when there's a
 * particular semantic meaning. E.g. red to blue for a ranged value representing temperature.
 */
@ComplicationExperimental
public class ColorRamp(@ColorInt val minColor: Int, @ColorInt val maxColor: Int) {
    override fun toString(): String {
        return "ColorRamp(minColor=$minColor, maxColor=$maxColor)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorRamp

        if (minColor != other.minColor) return false
        if (maxColor != other.maxColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minColor
        result = 31 * result + maxColor
        return result
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
public class RangedValueComplicationData @OptIn(ComplicationExperimental::class)
internal constructor(
    public val value: Float,
    public val min: Float,
    public val max: Float,
    public val monochromaticImage: MonochromaticImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    colorRamp: ColorRamp?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
) {

    /**
     * Optional metadata for [value] which explains renderers may use to influence styling of the
     * ranged value complication.
     *
     * @hide
     */
    @IntDef(
        value = [
            ValueType.NONE,
            ValueType.DISCRETE,
            ValueType.PROGRESS,
            ValueType.SCORE
        ]
    )
    @ComplicationExperimental
    public annotation class ValueType {
        public companion object {
            /** The default [value] has no special meaning. */
            public const val NONE: Int = 0

            /**
             * The default [value] contains integral values, the renderer may chose to style the
             * complication accordingly. E.g. it may draw them with a segmented line/arc.
             */
            public const val DISCRETE: Int = 1

            /**
             * The default [value] represents progress towards a goal. E.g. 1200 / 2000 calories
             * burned, or 7500 / 10000 steps.
             */
            public const val PROGRESS: Int = 2

            /**
             * The default [value] represents score such as 75/100 oxygen saturation or 25/100 of a
             * task complete. The renderer may choose to style the complication accordingly, perhaps
             * rendering a marker on top of the line/arc.
             */
            public const val SCORE: Int = 3
        }
    }

    /** Optional hint to render the value with the specified [ColorRamp]. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ComplicationExperimental
    @ComplicationExperimental
    val colorRamp: ColorRamp? = colorRamp

    /**
     * Builder for [RangedValueComplicationData].
     *
     * You must at a minimum set the [value], [min], [max] and [contentDescription] fields and at
     * least one of [monochromaticImage], [text] or [title].
     *
     * @param value The value of the ranged complication which should be in the range
     * [[min]] .. [[max]]
     * @param min The minimum value
     * @param max The maximum value. This must be less than [Float.MAX_VALUE].
     * @param contentDescription Localized description for use by screen readers
     */
    @OptIn(ComplicationExperimental::class)
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
        private var dataSource: ComponentName? = null
        @OptIn(ComplicationExperimental::class)
        private var colorRamp: ColorRamp? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        /**
         * Sets an optional hint which suggests the renderer draws the complication using a
         * [ColorRamp].
         */
        @ComplicationExperimental
        public fun setColorRamp(colorRamp: ColorRamp?): Builder = apply {
            this.colorRamp = colorRamp
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [RangedValueComplicationData]. */
        @OptIn(ComplicationExperimental::class)
        public fun build(): RangedValueComplicationData {
            require(monochromaticImage != null || text != null || title != null) {
                "At least one of monochromaticImage, text or title must be set"
            }
            return RangedValueComplicationData(
                value,
                min,
                max,
                monochromaticImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                colorRamp
            )
        }
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

    @OptIn(ComplicationExperimental::class)
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
        colorRamp?.let {
            builder.setRangedMinColor(it.minColor)
            builder.setRangedMaxColor(it.maxColor)
        }
    }

    @OptIn(ComplicationExperimental::class)
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
        if (dataSource != other.dataSource) return false
        if (colorRamp != other.colorRamp) return false

        return true
    }

    @OptIn(ComplicationExperimental::class)
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
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + colorRamp.hashCode()
        return result
    }

    @OptIn(ComplicationExperimental::class)
    override fun toString(): String {
        val valueString = if (WireComplicationData.shouldRedact()) {
            "REDACTED"
        } else {
            value.toString()
        }
        return "RangedValueComplicationData(value=$valueString, min=$min, max=$max, " +
            "monochromaticImage=$monochromaticImage, title=$title, text=$text, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "colorRamp=$colorRamp)"
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
 * Type used for complications which show progress towards a goal, E.g. you've done 2400 out of your
 * daily target of 10000 steps. Unlike [RangedValueComplicationData] [value] is allowed to be larger
 * than [targetValue] (e.g. you've done 12000 steps) and renderers may chose to acknowledge this in
 * a special way. The value may be accompanied by an icon and/or short text and title.
 *
 * The [value], and [targetValue] fields are required for this type and the progress is expected to
 * always be displayed.
 *
 * The icon, title, and text fields are optional and the watch face may choose which of these
 * fields to display, if any.
 *
 * @property value The [Float] value of this complication which is >= 0f, this value may be larger
 * than [targetValue]. If it's equal to [PLACEHOLDER] the renderer must treat it as a placeholder
 * rather than rendering normally, its suggested to be drawn as a grey arc with a percentage value
 * selected by the renderer.
 * @property targetValue The target [Float] value for this complication.
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
@ComplicationExperimental
public class GoalProgressComplicationData
internal constructor(
    public val value: Float,
    public val targetValue: Float,
    public val monochromaticImage: MonochromaticImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    colorRamp: ColorRamp?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
) {
    /** Optional hint to render the value with the specified [ColorRamp]. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ComplicationExperimental
    @ComplicationExperimental
    val colorRamp: ColorRamp? = colorRamp

    /**
     * Builder for [GoalProgressComplicationData].
     *
     * You must at a minimum set the [value], [targetValue] and [contentDescription] fields and at
     * least one of [monochromaticImage], [text] or [title].
     *
     * @param value The value of the ranged complication which should be >= 0.
     * @param targetValue The target value. This must be less than [Float.MAX_VALUE].
     * @param contentDescription Localized description for use by screen readers
     */
    @OptIn(ComplicationExperimental::class)
    public class Builder(
        private val value: Float,
        private val targetValue: Float,
        private var contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var cachedWireComplicationData: WireComplicationData? = null
        private var dataSource: ComponentName? = null
        @OptIn(ComplicationExperimental::class)
        private var colorRamp: ColorRamp? = null

        init {
            require(targetValue != Float.MAX_VALUE) {
                "Float.MAX_VALUE is reserved and can't be used for target"
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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        /**
         * Sets an optional hint which suggests the renderer draws the complication using a
         * [ColorRamp].
         */
        @ComplicationExperimental
        public fun setColorRamp(colorRamp: ColorRamp?): Builder = apply {
            this.colorRamp = colorRamp
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [GoalProgressComplicationData]. */
        @OptIn(ComplicationExperimental::class)
        public fun build(): GoalProgressComplicationData {
            require(monochromaticImage != null || text != null || title != null) {
                "At least one of monochromaticImage, text or title must be set"
            }
            return GoalProgressComplicationData(
                value,
                targetValue,
                monochromaticImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                colorRamp
            )
        }
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

    @OptIn(ComplicationExperimental::class)
    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setRangedValue(value)
        builder.setTargetValue(targetValue)
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
        colorRamp?.let {
            builder.setRangedMinColor(it.minColor)
            builder.setRangedMaxColor(it.maxColor)
        }
    }

    @OptIn(ComplicationExperimental::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GoalProgressComplicationData

        if (value != other.value) return false
        if (targetValue != other.targetValue) return false
        if (monochromaticImage != other.monochromaticImage) return false
        if (title != other.title) return false
        if (text != other.text) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false
        if (dataSource != other.dataSource) return false
        if (colorRamp != other.colorRamp) return false

        return true
    }

    @OptIn(ComplicationExperimental::class)
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + targetValue.hashCode()
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + colorRamp.hashCode()
        return result
    }

    @OptIn(ComplicationExperimental::class)
    override fun toString(): String {
        val valueString = if (WireComplicationData.shouldRedact()) {
            "REDACTED"
        } else {
            value.toString()
        }
        return "GoalProgressComplicationData(value=$valueString, targetValue=$targetValue, " +
            "monochromaticImage=$monochromaticImage, title=$title, text=$text, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "colorRamp=$colorRamp)"
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
        @OptIn(ComplicationExperimental::class)
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.GOAL_PROGRESS

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
 * Type used for complications including a discrete integer value within a range. E.g. 3 out of 6
 * daily cups of water drunk. The value may be accompanied by an icon and/or short text and title.
 *
 * The [value], [min], and [max] fields are required for this type and the value within the
 * range is expected to always be displayed.
 *
 * The icon, title, and text fields are optional and the watch face may choose which of these
 * fields to display, if any.
 *
 * Unlike [RangedValueComplicationData], DiscreteRangedValueComplicationData doesn't specify a color
 * ramp, this is because the ranged value is expected to be rendered using solid colored segments
 * with watch face selected colors.
 *
 * @property value The [Int] value of this complication which is >= [min] and <= [max] or equal to
 * [PLACEHOLDER]. If it's equal to [PLACEHOLDER] the renderer must treat it as a placeholder rather
 * than rendering normally, its suggested to be drawn as a grey arc with a percentage value selected
 * by the renderer.
 * @property min The minimum [Int] value for this complication.
 * @property max The maximum [Int] value for this complication.
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
@ComplicationExperimental
public class DiscreteRangedValueComplicationData
internal constructor(
    public val value: Int,
    public val min: Int,
    public val max: Int,
    public val monochromaticImage: MonochromaticImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
) {
    /**
     * Builder for [DiscreteRangedValueComplicationData].
     *
     * You must at a minimum set the [value], [min], [max] and [contentDescription] fields and at
     * least one of [monochromaticImage], [text] or [title].
     *
     * @param value The value of the ranged complication which should be in the range
     * [[min]] .. [[max]]
     * @param min The minimum value
     * @param max The maximum value. This must be less than [Float.MAX_VALUE].
     * @param contentDescription Localized description for use by screen readers
     */
    @OptIn(ComplicationExperimental::class)
    public class Builder(
        private val value: Int,
        private val min: Int,
        private val max: Int,
        private var contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var cachedWireComplicationData: WireComplicationData? = null
        private var dataSource: ComponentName? = null

        init {
            require(max != Int.MAX_VALUE) {
                "Int.MAX_VALUE is reserved and can't be used for max"
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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply {
            this.cachedWireComplicationData = cachedWireComplicationData
        }

        /** Builds the [DiscreteRangedValueComplicationData]. */
        @OptIn(ComplicationExperimental::class)
        public fun build(): DiscreteRangedValueComplicationData {
            require(monochromaticImage != null || text != null || title != null) {
                "At least one of monochromaticImage, text or title must be set"
            }
            return DiscreteRangedValueComplicationData(
                value,
                min,
                max,
                monochromaticImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource
            )
        }
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
        builder.setDiscreteRangedValue(value)
        builder.setDiscreteRangedMinValue(min)
        builder.setDiscreteRangedMaxValue(max)
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

    @OptIn(ComplicationExperimental::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscreteRangedValueComplicationData

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
        if (dataSource != other.dataSource) return false

        return true
    }

    @OptIn(ComplicationExperimental::class)
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
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        val valueString = if (WireComplicationData.shouldRedact()) {
            "REDACTED"
        } else {
            value.toString()
        }
        return "DiscreteRangedValueComplicationData(value=$valueString, min=$min, max=$max, " +
            "monochromaticImage=$monochromaticImage, title=$title, text=$text, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
        @OptIn(ComplicationExperimental::class)
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.DISCRETE_RANGED_VALUE

        /**
         * Used to signal the range should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField
        public val PLACEHOLDER = Int.MAX_VALUE
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
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
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): MonochromaticImageComplicationData =
            MonochromaticImageComplicationData(
                monochromaticImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = monochromaticImage.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun hasPlaceholderFields() = monochromaticImage.isPlaceholder()

    override fun toString(): String {
        return "MonochromaticImageComplicationData(monochromaticImage=$monochromaticImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
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
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public fun build(): SmallImageComplicationData =
            SmallImageComplicationData(
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = smallImage.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "SmallImageComplicationData(smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = tapAction,
    cachedWireComplicationData = cachedWireComplicationData,
    validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
    dataSource = dataSource
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
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
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
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var result = IconHelperP.hashCode(photoImage)
            result = 31 * result + (contentDescription?.hashCode() ?: 0)
            result = 31 * result + tapActionLostDueToSerialization.hashCode()
            result = 31 * result + (tapAction?.hashCode() ?: 0)
            result = 31 * result + dataSource.hashCode()
            result
        } else {
            var result = IconHelperBeforeP.hashCode(photoImage)
            result = 31 * result + (contentDescription?.hashCode() ?: 0)
            result = 31 * result + tapActionLostDueToSerialization.hashCode()
            result = 31 * result + (tapAction?.hashCode() ?: 0)
            result = 31 * result + validTimeRange.hashCode()
            result = 31 * result + dataSource.hashCode()
            result
        }
    }

    override fun toString(): String {
        return "PhotoImageComplicationData(photoImage=$photoImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
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
 * A complication that contains a serialized protoLayout.
 *
 * @property contentDescription The content description field for accessibility and is used to
 * describe what data the image represents. If the image is purely stylistic, and does not convey
 * any information to the user, then provide an empty content description. If no content description
 * is provided, a generic content description will be used instead.
 */
@ComplicationExperimental
public class ProtoLayoutComplicationData
internal
/**
 * @param ambientLayoutWireFormat The [LayoutElementBuilders.Layout] serialized into a
 * [ByteArray] to be displayed when the device is ambient.
 * @param interactiveLayoutWireFormat The [LayoutElementBuilders.Layout] serialized into a
 * [ByteArray] to be displayed when the device is interactive.
 * @param layoutResourcesWireFormat The [ResourceBuilders.Resources] serialized into a [ByteArray]
 * for [interactiveLayoutWireFormat] and [ambientLayoutWireFormat].
 */
constructor(
    private val ambientLayoutWireFormat: ByteArray,
    private val interactiveLayoutWireFormat: ByteArray,
    private val layoutResourcesWireFormat: ByteArray,
    val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) :
    ComplicationData(
        TYPE,
        tapAction,
        cachedWireComplicationData,
        validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource
    ) {

    /** The [LayoutElementBuilders.Layout] to be displayed when the device is ambient. */
    public val ambientLayout: LayoutElementBuilders.Layout by lazy {
        LayoutElementBuilders.Layout.fromByteArray(ambientLayoutWireFormat)!!
    }

    /** The [LayoutElementBuilders.Layout] to be displayed when the device is interactive. */
    public val interactiveLayout: LayoutElementBuilders.Layout by lazy {
        LayoutElementBuilders.Layout.fromByteArray(interactiveLayoutWireFormat)!!
    }

    /** The [ResourceBuilders.Resources] for [ambientLayout] and [interactiveLayout]. */
    public val layoutResources: ResourceBuilders.Resources by lazy {
        ResourceBuilders.Resources.fromByteArray(layoutResourcesWireFormat)!!
    }

    /**
     * Builder for [ProtoLayoutComplicationData].
     *
     * You must at a minimum set the [ambientLayout], [interactiveLayout], [layoutResources] and
     * [contentDescription] fields.
     *
     * @param ambientLayout The [LayoutElementBuilders.Layout] serialized into a [ByteArray] to be
     * displayed when the device is ambient
     * @param interactiveLayout The [LayoutElementBuilders.Layout] serialized into a [ByteArray] to
     * be displayed when the device is interactive
     * @param layoutResources The [ResourceBuilders.Resources] serialized into a [ByteArray] for
     * [interactiveLayout] and [ambientLayout]
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val ambientLayout: ByteArray,
        private val interactiveLayout: ByteArray,
        private val layoutResources: ByteArray,
        private val contentDescription: ComplicationText
    ) {
        /**
         * @param ambientLayout The [LayoutElementBuilders.Layout] to be displayed when the device
         * is ambient
         * @param interactiveLayout The [LayoutElementBuilders.Layout] to be displayed when the
         * device is interactive
         * @param resources The [ResourceBuilders.Resources] for [interactiveLayout] and
         * [ambientLayout]
         * @param contentDescription Localized description for use by screen readers
         */
        constructor(
            ambientLayout: LayoutElementBuilders.Layout,
            interactiveLayout: LayoutElementBuilders.Layout,
            resources: ResourceBuilders.Resources,
            contentDescription: ComplicationText
        ) : this(
            ambientLayout.toByteArray(),
            interactiveLayout.toByteArray(),
            resources.toByteArray(),
            contentDescription
        )

        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var cachedWireComplicationData: WireComplicationData? = null
        private var dataSource: ComponentName? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply { this.cachedWireComplicationData = cachedWireComplicationData }

        /** Builds the [ProtoLayoutComplicationData]. */
        public fun build(): ProtoLayoutComplicationData =
            ProtoLayoutComplicationData(
                ambientLayout,
                interactiveLayout,
                layoutResources,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder()
            .apply { fillWireComplicationDataBuilder(this) }
            .build()
            .also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setInteractiveLayout(interactiveLayoutWireFormat)
        builder.setAmbientLayout(ambientLayoutWireFormat)
        builder.setLayoutResources(layoutResourcesWireFormat)
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

        other as ProtoLayoutComplicationData

        if (!interactiveLayoutWireFormat.contentEquals(other.interactiveLayoutWireFormat))
            return false
        if (!ambientLayoutWireFormat.contentEquals(other.ambientLayoutWireFormat)) return false
        if (!layoutResourcesWireFormat.contentEquals(other.layoutResourcesWireFormat)) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interactiveLayoutWireFormat.hashCode()
        result = 31 * result + ambientLayoutWireFormat.hashCode()
        result = 31 * result + layoutResourcesWireFormat.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "ProtoLayoutComplicationData(protoLayoutWireFormat=$interactiveLayoutWireFormat, " +
            "ambientProtoLayoutWireFormat=$ambientLayoutWireFormat, " +
            "resourcesWireFormat=$layoutResourcesWireFormat, " +
            "contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource)"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.PROTO_LAYOUT
    }
}

/**
 * A complication that's a list of other complications, typically rendered as a table. E.g. the
 * weather forecast for the next three days could consist of three [ShortTextComplicationData]s
 * displayed in a row of columns.
 *
 * @property complicationList The list of sub [ComplicationData]s to display. This has a maximum
 * size of [ListComplicationData.MAX_ITEMS]. Note complicationList may not include a
 * ListComplicationData.
 * @property contentDescription The content description field for accessibility and is used to
 * describe what data the image represents. If the image is purely stylistic, and does not convey
 * any information to the user, then provide an empty content description. If no content description
 * is provided, a generic content description will be used instead.
 * @property styleHint The [StyleHint] which influences layout.
 */
@ComplicationExperimental
public class ListComplicationData
internal constructor(
    public val complicationList: List<ComplicationData>,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    public val styleHint: StyleHint
) : ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource
    ) {

    init {
        require(complicationList.size <= MAX_ITEMS) {
            "complicationList has a maximum of $MAX_ITEMS entries, but found " +
                complicationList.size
        }

        for (entry in complicationList) {
            require(entry !is ListComplicationData) {
                "You may not include a ListComplicationData inside a ListComplicationData"
            }
        }
    }

    /** A hint for generating a layout for [ListComplicationData]. */
    @ComplicationExperimental
    enum class StyleHint(private val wireType: Int) {
        /** Hints the list should be displayed as a single column where the entries are rows. */
        ColumnOfRows(0),

        /** Hints the list should be displayed as a single row where the entries are columns. */
        RowOfColumns(1);

        override fun toString(): String {
            return "ListComplicationLayoutStyleHint(wireType=$wireType)"
        }

        internal companion object {
            fun fromWireFormat(wireType: Int): StyleHint =
                when (wireType) {
                    ColumnOfRows.ordinal -> ColumnOfRows
                    RowOfColumns.ordinal -> RowOfColumns
                    else ->
                        throw java.lang.IllegalArgumentException(
                            "Unrecognized ListComplicationLayoutStyleHint wireType $wireType"
                        )
                }
        }
    }

    /**
     * Builder for [ListComplicationData].
     *
     * You must at a minimum set the [complicationList], [styleHint] and [contentDescription]
     * fields.
     *
     * @param complicationList The list [ComplicationData] to be displayed, typically as a table.
     * Note complicationList may not include a ListComplicationData.
     * @param styleHint The [StyleHint] which influences layout.
     * @param contentDescription Localized description for use by screen readers
     */
    public class Builder(
        private val complicationList: List<ComplicationData>,
        private val styleHint: StyleHint,
        private val contentDescription: ComplicationText
    ) {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var cachedWireComplicationData: WireComplicationData? = null
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
        }

        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): Builder = apply { this.cachedWireComplicationData = cachedWireComplicationData }

        /** Builds the [ListComplicationData]. */
        public fun build(): ListComplicationData =
            ListComplicationData(
                complicationList,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                styleHint
            )
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder()
            .apply { fillWireComplicationDataBuilder(this) }
            .build()
            .also { cachedWireComplicationData = it }
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setListEntryCollection(complicationList.map {
            it.asWireComplicationData()
        })
        builder.setListStyleHint(styleHint.ordinal)
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

    override fun hasPlaceholderFields() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListComplicationData

        if (complicationList != other.complicationList) return false
        if (contentDescription != other.contentDescription) return false
        if (tapActionLostDueToSerialization != other.tapActionLostDueToSerialization) return false
        if (tapAction != other.tapAction) return false
        if (validTimeRange != other.validTimeRange) return false
        if (dataSource != other.dataSource) return false
        if (styleHint != other.styleHint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = complicationList.hashCode()
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + styleHint.hashCode()
        return result
    }

    override fun toString(): String {
        return "ListComplicationData(complicationList=$complicationList, " +
            "contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "styleHint=$styleHint)"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField
        public val TYPE: ComplicationType = ComplicationType.LIST

        /** The maximum number of items in [ListComplicationData.complicationList]. */
        public const val MAX_ITEMS = 5
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
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?
) : ComplicationData(
    TYPE,
    tapAction = null,
    cachedWireComplicationData = cachedWireComplicationData,
    dataSource = dataSource
) {
    /**
     * Builder for [NoPermissionComplicationData].
     */
    public class Builder {
        private var text: ComplicationText? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var cachedWireComplicationData: WireComplicationData? = null
        private var dataSource: ComponentName? = null

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

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        public fun setDataSource(dataSource: ComponentName?): Builder = apply {
            this.dataSource = dataSource
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
                cachedWireComplicationData,
                dataSource
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
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (monochromaticImage?.hashCode() ?: 0)
        result = 31 * result + tapActionLostDueToSerialization.hashCode()
        result = 31 * result + (tapAction?.hashCode() ?: 0)
        result = 31 * result + validTimeRange.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "NoPermissionComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, tapActionLostDueToSerialization=" +
            "$tapActionLostDueToSerialization, tapAction=$tapAction, " +
            "validTimeRange=$validTimeRange, dataSource=$dataSource)"
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

@OptIn(ComplicationExperimental::class)
internal fun WireComplicationData.toPlaceholderComplicationData(): ComplicationData? = when (type) {
    NoDataComplicationData.TYPE.toWireComplicationType() -> null

    ShortTextComplicationData.TYPE.toWireComplicationType() -> {
        ShortTextComplicationData.Builder(
            shortText!!.toApiComplicationTextPlaceholderAware(),
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        ).apply {
            setTapAction(tapAction)
            setValidTimeRange(parseTimeRange())
            setMonochromaticImage(parseIconPlaceholderAware())
            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
            setDataSource(dataSource)
        }.build()
    }

    LongTextComplicationData.TYPE.toWireComplicationType() -> {
        LongTextComplicationData.Builder(
            longText!!.toApiComplicationTextPlaceholderAware(),
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        ).apply {
            setTapAction(tapAction)
            setValidTimeRange(parseTimeRange())
            setMonochromaticImage(parseIconPlaceholderAware())
            setSmallImage(parseSmallImagePlaceholderAware())
            setTitle(longTitle?.toApiComplicationTextPlaceholderAware())
            setDataSource(dataSource)
        }.build()
    }

    RangedValueComplicationData.TYPE.toWireComplicationType() ->
        RangedValueComplicationData.Builder(
            value = rangedValue,
            min = rangedMinValue,
            max = rangedMaxValue,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        ).apply {
            setTapAction(tapAction)
            setValidTimeRange(parseTimeRange())
            setMonochromaticImage(parseIconPlaceholderAware())
            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
            setText(shortText?.toApiComplicationTextPlaceholderAware())
            setDataSource(dataSource)
            rangedMinColor?.let {
                setColorRamp(ColorRamp(it, rangedMaxColor!!))
            }
        }.build()

    MonochromaticImageComplicationData.TYPE.toWireComplicationType() ->
        MonochromaticImageComplicationData(
            parseIconPlaceholderAware()!!,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY,
            tapAction,
            parseTimeRange(),
            this,
            dataSource
        )

    SmallImageComplicationData.TYPE.toWireComplicationType() ->
        SmallImageComplicationData(
            parseSmallImagePlaceholderAware()!!,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY,
            tapAction,
            parseTimeRange(),
            this,
            dataSource
        )

    PhotoImageComplicationData.TYPE.toWireComplicationType() ->
        PhotoImageComplicationData(
            parseLargeImagePlaceholderAware()!!,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY,
            tapAction,
            parseTimeRange(),
            this,
            dataSource
        )

    // TODO(b/230102159): We need to build support for placeholder ProtoLayoutComplicationData.
    ProtoLayoutComplicationData.TYPE.toWireComplicationType() ->
        ProtoLayoutComplicationData.Builder(
            ambientLayout!!,
            interactiveLayout!!,
            layoutResources!!,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        )
            .apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setDataSource(dataSource)
            }
            .build()

    ListComplicationData.TYPE.toWireComplicationType() ->
        ListComplicationData.Builder(
            listEntries!!.map { it.toApiComplicationData() },
            ListComplicationData.StyleHint.fromWireFormat(listStyleHint),
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        )
            .apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setDataSource(dataSource)
            }
            .build()

    GoalProgressComplicationData.TYPE.toWireComplicationType() ->
        GoalProgressComplicationData.Builder(
            value = rangedValue,
            targetValue = targetValue,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        ).apply {
            setTapAction(tapAction)
            setValidTimeRange(parseTimeRange())
            setMonochromaticImage(parseIconPlaceholderAware())
            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
            setText(shortText?.toApiComplicationTextPlaceholderAware())
            setDataSource(dataSource)
            rangedMinColor?.let {
                setColorRamp(ColorRamp(it, rangedMaxColor!!))
            }
        }.build()

    DiscreteRangedValueComplicationData.TYPE.toWireComplicationType() ->
        DiscreteRangedValueComplicationData.Builder(
            value = discreteRangedValue,
            min = discreteRangedMinValue,
            max = discreteRangedMaxValue,
            contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
        ).apply {
            setTapAction(tapAction)
            setValidTimeRange(parseTimeRange())
            setMonochromaticImage(parseIconPlaceholderAware())
            setTitle(shortTitle?.toApiComplicationTextPlaceholderAware())
            setText(shortText?.toApiComplicationTextPlaceholderAware())
            setDataSource(dataSource)
        }.build()

    else -> null
}

/**
 * @hide
 */
@OptIn(ComplicationExperimental::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationData.toApiComplicationData(): ComplicationData {
    val wireComplicationData = this
    return when (type) {
        NoDataComplicationData.TYPE.toWireComplicationType() -> {
            placeholder?.toPlaceholderComplicationData() ?.let {
                NoDataComplicationData(it)
            } ?: NoDataComplicationData()
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
                setDataSource(dataSource)
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
                setDataSource(dataSource)
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
                setDataSource(dataSource)
                rangedMinColor?.let {
                    setColorRamp(ColorRamp(it, rangedMaxColor!!))
                }
            }.build()

        MonochromaticImageComplicationData.TYPE.toWireComplicationType() ->
            MonochromaticImageComplicationData.Builder(
                parseIcon()!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
            }.build()

        SmallImageComplicationData.TYPE.toWireComplicationType() ->
            SmallImageComplicationData.Builder(
                parseSmallImage()!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
            }.build()

        PhotoImageComplicationData.TYPE.toWireComplicationType() ->
            PhotoImageComplicationData.Builder(
                largeImage!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
            }.build()

        ProtoLayoutComplicationData.TYPE.toWireComplicationType() ->
            ProtoLayoutComplicationData.Builder(
                ambientLayout!!,
                interactiveLayout!!,
                layoutResources!!,
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            )
                .apply {
                    setTapAction(tapAction)
                    setValidTimeRange(parseTimeRange())
                    setCachedWireComplicationData(wireComplicationData)
                    setDataSource(dataSource)
                }
                .build()

        ListComplicationData.TYPE.toWireComplicationType() ->
            ListComplicationData.Builder(
                listEntries!!.map { it.toApiComplicationData() },
                ListComplicationData.StyleHint.fromWireFormat(listStyleHint),
                contentDescription?.toApiComplicationText() ?: ComplicationText.EMPTY
            )
                .apply {
                    setTapAction(tapAction)
                    setValidTimeRange(parseTimeRange())
                    setCachedWireComplicationData(wireComplicationData)
                    setDataSource(dataSource)
                }
                .build()

        NoPermissionComplicationData.TYPE.toWireComplicationType() ->
            NoPermissionComplicationData.Builder().apply {
                setMonochromaticImage(parseIcon())
                setTitle(shortTitle?.toApiComplicationText())
                setText(shortText?.toApiComplicationText())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
            }.build()

        GoalProgressComplicationData.TYPE.toWireComplicationType() ->
            GoalProgressComplicationData.Builder(
                value = rangedValue,
                targetValue = targetValue,
                contentDescription = contentDescription?.toApiComplicationText()
                    ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setMonochromaticImage(parseIcon())
                setTitle(shortTitle?.toApiComplicationText())
                setText(shortText?.toApiComplicationText())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
                rangedMinColor?.let {
                    setColorRamp(ColorRamp(it, rangedMaxColor!!))
                }
            }.build()

        DiscreteRangedValueComplicationData.TYPE.toWireComplicationType() ->
            DiscreteRangedValueComplicationData.Builder(
                value = discreteRangedValue,
                min = discreteRangedMinValue,
                max = discreteRangedMaxValue,
                contentDescription = contentDescription?.toApiComplicationText()
                    ?: ComplicationText.EMPTY
            ).apply {
                setTapAction(tapAction)
                setValidTimeRange(parseTimeRange())
                setMonochromaticImage(parseIcon())
                setTitle(shortTitle?.toApiComplicationText())
                setText(shortText?.toApiComplicationText())
                setCachedWireComplicationData(wireComplicationData)
                setDataSource(dataSource)
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
