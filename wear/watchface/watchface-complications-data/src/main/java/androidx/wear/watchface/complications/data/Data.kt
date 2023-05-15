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
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.support.wearable.complications.ComplicationData.Builder as WireComplicationDataBuilder
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.watchface.complications.data.GoalProgressComplicationData.Companion.PLACEHOLDER
import androidx.wear.watchface.complications.data.RangedValueComplicationData.Companion.PLACEHOLDER
import androidx.wear.watchface.complications.data.RangedValueComplicationData.Companion.TYPE_RATING
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData.Companion.PLACEHOLDER
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData.Companion.getMaxElements
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData.Element
import java.time.Instant

internal const val TAG = "Data.kt"

/** The policies that control complication persistence. */
public object ComplicationPersistencePolicies {
    /** The default policy is that persistence/caching is allowed. */
    public const val CACHING_ALLOWED: Int = 0

    /**
     * Instructs the system to not persist the complication past a reboot. This is useful when
     * freshness is important.
     */
    public const val DO_NOT_PERSIST: Int = 1
}

@IntDef(
    flag = true, // This is a flag to allow for future expansion.
    value =
        [
            ComplicationPersistencePolicies.CACHING_ALLOWED,
            ComplicationPersistencePolicies.DO_NOT_PERSIST
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class ComplicationPersistencePolicy

/** The policies that control when complications should be displayed. */
public object ComplicationDisplayPolicies {
    /** The default policy is that the complication should always be shown. */
    public const val ALWAYS_DISPLAY: Int = 0

    /** Instructs the system not to display the complication while the device is locked. */
    public const val DO_NOT_SHOW_WHEN_DEVICE_LOCKED: Int = 1
}

@IntDef(
    flag = true, // This is a flag to allow for future expansion.
    value =
        [
            ComplicationDisplayPolicies.ALWAYS_DISPLAY,
            ComplicationDisplayPolicies.DO_NOT_SHOW_WHEN_DEVICE_LOCKED
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class ComplicationDisplayPolicy

/**
 * Base type for all different types of [ComplicationData] types.
 *
 * Please note to aid unit testing of ComplicationDataSourceServices, [equals], [hashCode] and
 * [toString] have been overridden for all the types of ComplicationData, however due to the
 * embedded [Icon] class we have to fall back to reference equality and hashing below API 28 and
 * also for the [Icon]s that don't use either a resource or a uri (these should be rare but they can
 * exist).
 *
 * @property type The [ComplicationType] of this complication data.
 * @property tapAction The [PendingIntent] to send when the complication is tapped on.
 * @property validTimeRange The [TimeRange] within which the complication should be displayed.
 *   Whether the complication is active and should be displayed at the given time should be checked
 *   with [TimeRange.contains].
 * @property dataSource The [ComponentName] of the
 *   [androidx.wear.watchface.complications.datasource.ComplicationDataSourceService] that provided
 *   the ComplicationData. This may be `null` when run on old systems.
 * @property persistencePolicy The [ComplicationPersistencePolicy] for this complication. This
 *   requires the watchface to be built with a compatible library to work.
 * @property displayPolicy The [ComplicationDisplayPolicy] for this complication. This requires the
 *   watchface to be built with a compatible library to work.
 */
public sealed class ComplicationData
constructor(
    public val type: ComplicationType,
    public val tapAction: PendingIntent?,
    internal var cachedWireComplicationData: WireComplicationData?,
    public val validTimeRange: TimeRange = TimeRange.ALWAYS,
    public val dataSource: ComponentName?,
    @ComplicationPersistencePolicy public val persistencePolicy: Int,
    @ComplicationDisplayPolicy public val displayPolicy: Int,
    private val fallback: ComplicationData?,
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun asWireComplicationData(): WireComplicationData {
        cachedWireComplicationData?.let {
            return it
        }
        return createWireComplicationDataBuilder()
            .apply { fillWireComplicationDataBuilder(this) }
            .build()
            .also { cachedWireComplicationData = it }
    }

    internal fun createWireComplicationDataBuilder(): WireComplicationDataBuilder =
        cachedWireComplicationData?.let { WireComplicationDataBuilder(it) }
            ?: WireComplicationDataBuilder(type.toWireComplicationType())

    internal open fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        builder.setDataSource(dataSource)
        builder.setPersistencePolicy(persistencePolicy)
        builder.setDisplayPolicy(displayPolicy)
        if (fallback == null) {
            builder.setPlaceholder(null)
        } else {
            val placeholderBuilder = fallback.createWireComplicationDataBuilder()
            fallback.fillWireComplicationDataBuilder(placeholderBuilder)
            builder.setPlaceholder(placeholderBuilder.build())
        }
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

    override fun equals(other: Any?): Boolean =
        other is ComplicationData && asWireComplicationData() == other.asWireComplicationData()

    /**
     * Similar to [equals], but avoids comparing evaluated fields (if expressions exist).
     *
     * @hide
     */
    infix fun equalsUnevaluated(other: ComplicationData): Boolean =
        asWireComplicationData() equalsUnevaluated other.asWireComplicationData()

    override fun hashCode(): Int = asWireComplicationData().hashCode()

    /** Builder for properties in common for most Complication Types. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public sealed class BaseBuilder<BuilderT : BaseBuilder<BuilderT, BuiltT>, BuiltT> {
        internal var cachedWireComplicationData: WireComplicationData? = null
        internal var dataSource: ComponentName? = null
        internal var persistencePolicy = ComplicationPersistencePolicies.CACHING_ALLOWED
        internal var displayPolicy = ComplicationDisplayPolicies.ALWAYS_DISPLAY
        internal var fallback: BuiltT? = null

        /**
         * Sets the [ComponentName] of the ComplicationDataSourceService that provided this
         * ComplicationData, if any.
         *
         * Note a ComplicationDataSourceService does not need to call this because the system will
         * set this value on its behalf.
         */
        @Suppress("UNCHECKED_CAST", "SetterReturnsThis")
        public fun setDataSource(dataSource: ComponentName?): BuilderT {
            this.dataSource = dataSource
            return this as BuilderT
        }

        @Suppress("UNCHECKED_CAST", "SetterReturnsThis")
        internal fun setCachedWireComplicationData(
            cachedWireComplicationData: WireComplicationData?
        ): BuilderT {
            this.cachedWireComplicationData = cachedWireComplicationData
            return this as BuilderT
        }

        /** Sets the complication's [ComplicationPersistencePolicy]. */
        @Suppress("UNCHECKED_CAST", "SetterReturnsThis")
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public fun setPersistencePolicy(
            @ComplicationPersistencePolicy persistencePolicy: Int
        ): BuilderT {
            this.persistencePolicy = persistencePolicy
            return this as BuilderT
        }

        /** Sets the complication's [ComplicationDisplayPolicy]. */
        @Suppress("UNCHECKED_CAST", "SetterReturnsThis")
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public fun setDisplayPolicy(@ComplicationDisplayPolicy displayPolicy: Int): BuilderT {
            this.displayPolicy = displayPolicy
            return this as BuilderT
        }

        /** Sets the complication's fallback, use in case any expression has been invalidated. */
        // TODO(b/269414040): Unhide complication expression APIs.
        @Suppress("UNCHECKED_CAST", "SetterReturnsThis")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setFallback(fallback: BuiltT?): BuilderT {
            this.fallback = fallback
            return this as BuilderT
        }

        /** Builds the ComplicationData */
        abstract fun build(): BuiltT
    }
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
 * Some watchfaces may not support placeholders and in that case the NoDataComplicationData will be
 * treated as being empty.
 *
 * @property placeholder An optional [ComplicationData] which may contain placeholder fields (see
 *   [hasPlaceholderFields]). The type of the placeholder must match the type of the
 *   ComplicationData that would have otherwise been sent. The placeholder is expected to be
 *   rendered if the watch face has been built with a compatible library, older libraries which
 *   don't support placeholders will ignore this field.
 */
public class NoDataComplicationData
internal constructor(
    public val placeholder: ComplicationData?,
    cachedWireComplicationData: WireComplicationData?
) :
    ComplicationData(
        TYPE,
        placeholder?.tapAction,
        cachedWireComplicationData,
        dataSource = null,
        persistencePolicy = placeholder?.persistencePolicy
                ?: ComplicationPersistencePolicies.CACHING_ALLOWED,
        displayPolicy = placeholder?.displayPolicy ?: ComplicationDisplayPolicies.ALWAYS_DISPLAY,
        fallback = placeholder,
    ) {

    /** Constructs a NoDataComplicationData without a [placeholder]. */
    constructor() : this(null, null)

    /**
     * Constructs a NoDataComplicationData with a [placeholder] [ComplicationData] which is allowed
     * to contain placeholder fields (see [hasPlaceholderFields]) which must be drawn to look like
     * placeholders. E.g. with grey boxes / arcs.
     */
    constructor(placeholder: ComplicationData) : this(placeholder, null)

    val contentDescription: ComplicationText? =
        when (placeholder) {
            is ShortTextComplicationData -> placeholder.contentDescription
            is LongTextComplicationData -> placeholder.contentDescription
            is RangedValueComplicationData -> placeholder.contentDescription
            is MonochromaticImageComplicationData -> placeholder.contentDescription
            is SmallImageComplicationData -> placeholder.contentDescription
            is PhotoImageComplicationData -> placeholder.contentDescription
            is GoalProgressComplicationData -> placeholder.contentDescription
            is WeightedElementsComplicationData -> placeholder.contentDescription
            else -> null
        }

    override fun toString(): String {
        return "NoDataComplicationData(" +
            "placeholder=$placeholder, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy)"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.NO_DATA
    }
}

/**
 * Type sent when the user has specified that an active complication should have no complication
 * data source, i.e. when the user has chosen "Empty" in the complication data source chooser.
 * Complication data sources cannot send data of this type.
 */
public class EmptyComplicationData :
    ComplicationData(
        TYPE,
        tapAction = null,
        cachedWireComplicationData = null,
        dataSource = null,
        persistencePolicy = ComplicationPersistencePolicies.CACHING_ALLOWED,
        displayPolicy = ComplicationDisplayPolicies.ALWAYS_DISPLAY,
        fallback = null,
    ) {
    // Always empty.
    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {}

    override fun toString(): String {
        return "EmptyComplicationData()"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.EMPTY
    }
}

/**
 * Type sent when a complication does not have a complication data source configured. The system
 * will send data of this type to watch faces when the user has not chosen a complication data
 * source for an active complication, and the watch face has not set a default complication data
 * source. Complication data sources cannot send data of this type.
 */
public class NotConfiguredComplicationData :
    ComplicationData(
        TYPE,
        tapAction = null,
        cachedWireComplicationData = null,
        dataSource = null,
        persistencePolicy = ComplicationPersistencePolicies.CACHING_ALLOWED,
        displayPolicy = ComplicationDisplayPolicies.ALWAYS_DISPLAY,
        fallback = null,
    ) {
    // Always empty.
    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {}

    override fun toString(): String {
        return "NotConfiguredComplicationData()"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.NOT_CONFIGURED
    }
}

/**
 * Type used for complications where the primary piece of data is a short piece of text (expected to
 * be no more than seven characters in length). The text may be accompanied by an icon or a title or
 * both.
 *
 * If only one of icon and title is provided, it is expected that it will be displayed. If both are
 * provided, it is expected that at least one of these will be displayed.
 *
 * If a [monochromaticImage] and a [smallImage] are both specified then only one should be
 * displayed. If the complication is drawn with a single color it's recommended to choose
 * [monochromaticImage] and apply a tint. If the complication is rendered with multiple colors it's
 * recommended to choose the [smallImage]. It's best practice for a ComplicationDataSource to
 * specify both a [monochromaticImage] and a [smallImage]
 *
 * A data source that wants to serve a ShortTextComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="SHORT_TEXT"/>
 * ```
 *
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 *   any time-dependent values at any valid time, is expected to not exceed seven characters. When
 *   using this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as
 *   a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property title The optional title [ComplicationText]. The length of the text, including any
 *   time-dependent values at any valid time, is expected to not exceed seven characters. When using
 *   this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the title is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 *   as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 *   treat it as a placeholder rather than rendering normally, its suggested it should be rendered
 *   as a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
public class ShortTextComplicationData
internal constructor(
    public val text: ComplicationText,
    public val title: ComplicationText?,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val fallback: ShortTextComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {
    /**
     * Builder for [ShortTextComplicationData].
     *
     * You must at a minimum set the [text] and [contentDescription] fields.
     *
     * @param text The main localized [ComplicationText]. This must be less than 7 characters long
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    public class Builder(
        private val text: ComplicationText,
        private var contentDescription: ComplicationText
    ) : BaseBuilder<Builder, ShortTextComplicationData>() {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null

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
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional icon associated with the complication data. */
        public fun setMonochromaticImage(monochromaticImage: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = monochromaticImage
        }

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Builds the [ShortTextComplicationData]. */
        public override fun build(): ShortTextComplicationData =
            ShortTextComplicationData(
                text,
                title,
                monochromaticImage,
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
        builder.setShortText(text.toWireComplicationText())
        builder.setShortTitle(title?.toWireComplicationText())
        builder.setContentDescription(
            when (contentDescription) {
                ComplicationText.EMPTY -> null
                else -> contentDescription?.toWireComplicationText()
            }
        )
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
        builder.setTapAction(tapAction)
        setValidTimeRange(validTimeRange, builder)
        builder.setTapActionLostDueToSerialization(tapActionLostDueToSerialization)
    }

    override fun toString(): String {
        return "ShortTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "contentDescription=$contentDescription, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    override fun hasPlaceholderFields() =
        text.isPlaceholder() ||
            title?.isPlaceholder() == true ||
            monochromaticImage?.isPlaceholder() == true ||
            smallImage?.isPlaceholder() == true

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
        @JvmField public val TYPE: ComplicationType = ComplicationType.SHORT_TEXT

        /** The maximum length of [ShortTextComplicationData.text] in characters. */
        @JvmField public val MAX_TEXT_LENGTH = 7
    }
}

/**
 * Type used for complications where the primary piece of data is a piece of text. The text may be
 * accompanied by an icon and/or a title.
 *
 * The text is expected to always be displayed.
 *
 * The title, if provided, it is expected that this field will be displayed.
 *
 * If a [monochromaticImage] and a [smallImage] are both specified then only one should be
 * displayed. If the complication is drawn with a single color it's recommended to choose
 * [monochromaticImage] and apply a tint. If the complication is rendered with multiple colors it's
 * recommended to choose the [smallImage]. It's best practice for a ComplicationDataSource to
 * specify both a [monochromaticImage] and a [smallImage].
 *
 * A data source that wants to serve a LongTextComplicationData must include the following meta data
 * in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="LONG_TEXT"/>
 * ```
 *
 * @property text The body [ComplicationText] of the complication. If the text is equal to
 *   [ComplicationText.PLACEHOLDER] the renderer must treat it as a placeholder rather than
 *   rendering normally, its suggested it should be rendered as a light grey box.
 * @property title The optional title [ComplicationText]. If the title is equal to
 *   [ComplicationText.PLACEHOLDER] the renderer must treat it as a placeholder rather than
 *   rendering normally, its suggested it should be rendered as a light grey box.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 *   treat it as a placeholder rather than rendering normally, its suggested it should be rendered
 *   as a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
public class LongTextComplicationData
internal constructor(
    public val text: ComplicationText,
    public val title: ComplicationText?,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val fallback: LongTextComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {
    /**
     * Builder for [LongTextComplicationData].
     *
     * You must at a minimum set the [text] and [contentDescription] fields.
     *
     * @param text Localized main [ComplicationText] to display within the complication. There isn't
     *   an explicit character limit but text may be truncated if too long
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    public class Builder(
        private val text: ComplicationText,
        private var contentDescription: ComplicationText
    ) : BaseBuilder<Builder, LongTextComplicationData>() {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null

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
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional image associated with the complication data. */
        public fun setMonochromaticImage(icon: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = icon
        }

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Builds the [LongTextComplicationData]. */
        public override fun build(): LongTextComplicationData =
            LongTextComplicationData(
                text,
                title,
                monochromaticImage,
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
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

    override fun toString(): String {
        return "LongTextComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    override fun hasPlaceholderFields() =
        text.isPlaceholder() ||
            title?.isPlaceholder() == true ||
            monochromaticImage?.isPlaceholder() == true ||
            smallImage?.isPlaceholder() == true

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
        @JvmField public val TYPE: ComplicationType = ComplicationType.LONG_TEXT
    }
}

/**
 * Describes an optional color ramp for the progress bar associated with
 * [RangedValueComplicationData] or [GoalProgressComplicationData]. This is a rendering hint that
 * overrides the normal watch face colors when there's a particular semantic meaning. E.g. red to
 * blue for a ranged value representing temperature.
 *
 * Note this is a subset of the functionality of [android.graphics.LinearGradient] and the x & y
 * coordinates for the ramp are not known to the complication data source.
 *
 * @property colors The colors to render the progress bar with. For [RangedValueComplicationData]
 *   the first color corresponds to [RangedValueComplicationData.min] and the last color to
 *   [RangedValueComplicationData.max]. For [GoalProgressComplicationData] the first color
 *   corresponds to zero and the last color to [GoalProgressComplicationData.targetValue]. A maximum
 *   of 7 colors may be specified. When rendered the colors must be evenly spread along the progress
 *   bar. The colors must be meaningful to the user, e.g. blue = cold, red/yellow = warm.
 * @property interpolated If `true` then the colors should be smoothly interpolated when rendering
 *   the progress bar. If `false` the colors should be rendered as equal sized regions of solid
 *   color, resulting in a noticeable step between each color.
 */
public class ColorRamp(
    @ColorInt val colors: IntArray,
    @get:JvmName("isInterpolated") val interpolated: Boolean
) {
    init {
        require(colors.size <= 7) { "colors can have no more than seven entries" }
    }

    override fun toString(): String {
        return "ColorRamp(colors=[${colors.joinToString()}], interpolated=$interpolated)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorRamp

        if (!colors.contentEquals(other.colors)) return false
        if (interpolated != other.interpolated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.contentHashCode()
        result = 31 * result + interpolated.hashCode()
        return result
    }
}

/**
 * Type used for complications including a numerical value within a range, such as a percentage. The
 * value may be accompanied by an icon and/or short text and title.
 *
 * The [min] and [max] fields are required for this type, as well as one of [value] or
 * [valueExpression]. The value within the range is expected to always be displayed.
 *
 * The icon, title, and text fields are optional and the watch face may choose which of these fields
 * to display, if any.
 *
 * If a [monochromaticImage] and a [smallImage] are both specified then only one should be
 * displayed. If the complication is drawn with a single color it's recommended to choose
 * [monochromaticImage] and apply a tint. If the complication is rendered with multiple colors it's
 * recommended to choose the [smallImage]. It's best practice for a ComplicationDataSource to
 * specify both a [monochromaticImage] and a [smallImage].
 *
 * A data source that wants to serve a RangedValueComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="GOAL_PROGRESS"/>
 * ```
 *
 * @property value The [Float] value of this complication which is >= [min] and <= [max] or equal to
 *   [PLACEHOLDER]. If it's equal to [PLACEHOLDER] the renderer must treat it as a placeholder
 *   rather than rendering normally, its suggested to be drawn as a grey arc with a percentage value
 *   selected by the renderer. The semantic meaning of value is described by [valueType].
 * @property min The minimum [Float] value for this complication.
 * @property max The maximum [Float] value for this complication.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 *   treat it as a placeholder rather than rendering normally, its suggested it should be rendered
 *   as a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property title The optional title [ComplicationText]. The length of the title, including any
 *   time-dependent values at any valid time, is expected to not exceed seven characters. When using
 *   this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the title is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 *   as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 *   any time-dependent values at any valid time, is expected to not exceed seven characters. When
 *   using this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as
 *   a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property contentDescription The content description field for accessibility. Please do not
 *   include the word 'complication' in the description.
 * @property colorRamp Optional hint to render the value with the specified [ColorRamp]. When
 *   present the renderer may choose to use the ColorRamp when rendering the progress bar.
 * @property valueType The semantic meaning of [value]. The complication renderer may choose to
 *   visually differentiate between the different types, for example rendering a dot on a line/arc
 *   to indicate the value for a [TYPE_RATING].
 * @property fallback Used in case any expression has been invalidated.
 */
public class RangedValueComplicationData
internal constructor(
    public val value: Float,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET") valueExpression: DynamicFloat?,
    public val min: Float,
    public val max: Float,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    public val colorRamp: ColorRamp?,
    @RangedValueType public val valueType: Int,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: RangedValueComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {

    init {
        require(min <= max) { "min must be lower than or equal to max" }
        require(value == PLACEHOLDER || value in min..max) { "value must be between min and max" }
        require(max != Float.MAX_VALUE) { "Float.MAX_VALUE is reserved and can't be used for max" }
        require(monochromaticImage != null || smallImage != null || text != null || title != null) {
            "At least one of monochromaticImage, smallImage, text or title must be set"
        }
        if (valueType == TYPE_PERCENTAGE) {
            require(min == 0f)
            require(max == 100f)
        }
    }

    /**
     * The [DynamicFloat] optionally set by the data source. If present the system will dynamically
     * evaluate this and store the result in [value]. Watch faces can typically ignore this field.
     */
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val valueExpression: DynamicFloat? = valueExpression

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = [TYPE_UNDEFINED, TYPE_RATING, TYPE_PERCENTAGE])
    public annotation class RangedValueType

    /**
     * Builder for [RangedValueComplicationData].
     *
     * You must at a minimum set the [min], [max] and [contentDescription] fields, at least one of
     * [value] or [valueExpression], and at least one of [monochromaticImage], [smallImage], [text]
     * or [title].
     */
    public class Builder
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        private val value: Float,
        private val valueExpression: DynamicFloat?,
        private val min: Float,
        private val max: Float,
        private var contentDescription: ComplicationText
    ) : BaseBuilder<Builder, RangedValueComplicationData>() {
        /**
         * Creates a [Builder] for a [RangedValueComplicationData] with a [Float] value.
         *
         * @param value The value of the ranged complication which should be in the range [[min]] ..
         *   [[max]]. The semantic meaning of value can be specified via [setValueType].
         * @param min The minimum value. For [TYPE_PERCENTAGE] this must be 0f.
         * @param max The maximum value. This must be less than [Float.MAX_VALUE]. For
         *   [TYPE_PERCENTAGE] this must be 0f.
         * @param contentDescription Defines localized text that briefly describes content of the
         *   complication. This property is used primarily for accessibility. Since some
         *   complications do not have textual representation this attribute can be used for
         *   providing such. Please do not include the word 'complication' in the description.
         */
        public constructor(
            value: Float,
            min: Float,
            max: Float,
            contentDescription: ComplicationText
        ) : this(value, valueExpression = null, min, max, contentDescription)

        /**
         * Creates a [Builder] for a [RangedValueComplicationData] with a [DynamicFloat] value.
         *
         * @param valueExpression The [DynamicFloat] of the ranged complication which will be
         *   evaluated into a value dynamically, and should be in the range [[min]] .. [[max]]. The
         *   semantic meaning of value can be specified via [setValueType].
         * @param min The minimum value. For [TYPE_PERCENTAGE] this must be 0f.
         * @param max The maximum value. This must be less than [Float.MAX_VALUE]. For
         *   [TYPE_PERCENTAGE] this must be 0f.
         * @param contentDescription Defines localized text that briefly describes content of the
         *   complication. This property is used primarily for accessibility. Since some
         *   complications do not have textual representation this attribute can be used for
         *   providing such. Please do not include the word 'complication' in the description.
         */
        // TODO(b/269414040): Unhide complication expression APIs.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor(
            valueExpression: DynamicFloat,
            min: Float,
            max: Float,
            contentDescription: ComplicationText
        ) : this(value = min /* sensible default */, valueExpression, min, max, contentDescription)

        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var colorRamp: ColorRamp? = null

        @RangedValueType private var valueType: Int = TYPE_UNDEFINED

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

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply { this.text = text }

        /**
         * Sets an optional hint that the renderer should draw the progress bar using the
         * [ColorRamp].
         */
        public fun setColorRamp(colorRamp: ColorRamp?): Builder = apply {
            this.colorRamp = colorRamp
        }

        /**
         * Sets the semantic meaning of [value]. The complication renderer may choose to visually
         * differentiate between the different types, for example rendering a dot on a line/arc to
         * indicate the value for a [TYPE_RATING]. Defaults to [TYPE_UNDEFINED] if not set.
         */
        public fun setValueType(@RangedValueType valueType: Int): Builder = apply {
            this.valueType = valueType
        }

        /** Builds the [RangedValueComplicationData]. */
        public override fun build() =
            RangedValueComplicationData(
                value,
                valueExpression,
                min,
                max,
                monochromaticImage,
                smallImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                colorRamp,
                valueType,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
        builder.setRangedValue(value)
        builder.setRangedValueExpression(valueExpression)
        builder.setRangedMinValue(min)
        builder.setRangedMaxValue(max)
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
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
            builder.setColorRamp(it.colors)
            builder.setColorRampIsSmoothShaded(it.interpolated)
        }
        builder.setRangedValueType(valueType)
    }

    override fun toString(): String {
        val valueString =
            if (WireComplicationData.shouldRedact()) {
                "REDACTED"
            } else {
                value.toString()
            }
        val valueExpressionString =
            if (WireComplicationData.shouldRedact()) {
                "REDACTED"
            } else {
                valueExpression.toString()
            }
        return "RangedValueComplicationData(value=$valueString, " +
            "valueExpression=$valueExpressionString, valueType=$valueType, min=$min, " +
            "max=$max, monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "title=$title, text=$text, contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "colorRamp=$colorRamp, persistencePolicy=$persistencePolicy, " +
            "displayPolicy=$displayPolicy, fallback=$fallback)"
    }

    override fun hasPlaceholderFields() =
        value == PLACEHOLDER ||
            text?.isPlaceholder() == true ||
            title?.isPlaceholder() == true ||
            monochromaticImage?.isPlaceholder() == true ||
            smallImage?.isPlaceholder() == true

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
        @JvmField public val TYPE: ComplicationType = ComplicationType.RANGED_VALUE

        /**
         * Used to signal the range should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField public val PLACEHOLDER = Float.MAX_VALUE

        /**
         * The ranged value's semantic hasn't been explicitly defined, most commonly it's a
         * percentage however.
         */
        const val TYPE_UNDEFINED = 0

        /**
         * The ranged value represents a rating or score for something unrelated to the user, e.g.
         * the air quality index or the UV index.
         */
        const val TYPE_RATING = 1

        /** The ranged value represents a percentage in the range [0..100]. E.g. Battery charge. */
        const val TYPE_PERCENTAGE = 2
    }
}

/**
 * Type used for complications which shows the user's progress towards a goal, E.g. you've done 2400
 * out of your daily target of 10000 steps. Unlike [RangedValueComplicationData] [value] is allowed
 * to be larger than [targetValue] (e.g. you've done 12000 steps) and renderers may chose to
 * acknowledge this in a special way (e.g. by colorizing part of the progress bar in a different
 * color to indicate progress past the goal). The value may be accompanied by an icon and/or short
 * text and title.
 *
 * The [targetValue] field is required for this type, as well as one of [value] or
 * [valueExpression]. The progress is expected to always be displayed.
 *
 * The icon, title, and text fields are optional and the watch face may choose which of these fields
 * to display, if any.
 *
 * If a [monochromaticImage] and a [smallImage] are both specified then only one should be
 * displayed. If the complication is drawn with a single color it's recommended to choose
 * [monochromaticImage] and apply a tint. If the complication is rendered with multiple colors it's
 * recommended to choose the [smallImage]. It's best practice for a ComplicationDataSource to
 * specify both a [monochromaticImage] and a [smallImage].
 *
 * If you want to represent a score for something that's not based on the user (e.g. air quality
 * index) then you should instead use a [RangedValueComplicationData] and pass
 * [RangedValueComplicationData.TYPE_RATING] into
 * [RangedValueComplicationData.Builder.setValueType].
 *
 * A data source that wants to serve a SmallImageComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *        android:value="GOAL_PROGRESS"/>
 * ```
 *
 * @property value The [Float] value of this complication which is >= 0f, this value may be larger
 *   than [targetValue]. If it's equal to [PLACEHOLDER] the renderer must treat it as a placeholder
 *   rather than rendering normally, its suggested to be drawn as a grey arc with a percentage value
 *   selected by the renderer.
 * @property targetValue The target [Float] value for this complication.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 *   treat it as a placeholder rather than rendering normally, its suggested it should be rendered
 *   as a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property title The optional title [ComplicationText]. The length of the title, including any
 *   time-dependent values at any valid time, is expected to not exceed seven characters. When using
 *   this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the title is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 *   as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 *   any time-dependent values at any valid time, is expected to not exceed seven characters. When
 *   using this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as
 *   a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property contentDescription The content description field for accessibility. Please do not
 *   include the word 'complication' in the description.
 * @property colorRamp Optional hint to render the progress bar representing [value] with the
 *   specified [ColorRamp].
 * @property fallback Used in case any expression has been invalidated.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class GoalProgressComplicationData
internal constructor(
    public val value: Float,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET") valueExpression: DynamicFloat?,
    public val targetValue: Float,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    public val colorRamp: ColorRamp?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: GoalProgressComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {

    init {
        require(targetValue != Float.MAX_VALUE) {
            "Float.MAX_VALUE is reserved and can't be used for target"
        }
        require(monochromaticImage != null || smallImage != null || text != null || title != null) {
            "At least one of monochromaticImage, smallImage, text or title must be set"
        }
    }

    /**
     * The [DynamicFloat] optionally set by the data source. If present the system will dynamically
     * evaluate this and store the result in [value]. Watch faces can typically ignore this field.
     */
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val valueExpression: DynamicFloat? = valueExpression

    /**
     * Builder for [GoalProgressComplicationData].
     *
     * You must at a minimum set the [targetValue] and [contentDescription] fields, one of [value]
     * or [valueExpression], and at least one of [monochromaticImage], [smallImage], [text] or
     * [title].
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public class Builder
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        private val value: Float,
        private val valueExpression: DynamicFloat?,
        private val targetValue: Float,
        private var contentDescription: ComplicationText
    ) : BaseBuilder<Builder, GoalProgressComplicationData>() {
        /**
         * Creates a [Builder] for a [GoalProgressComplicationData] with a [Float] value.
         *
         * @param value The value of the goal complication which should be >= 0.
         * @param targetValue The target value. This must be less than [Float.MAX_VALUE].
         * @param contentDescription Defines localized text that briefly describes content of the
         *   complication. This property is used primarily for accessibility. Since some
         *   complications do not have textual representation this attribute can be used for
         *   providing such. Please do not include the word 'complication' in the description.
         */
        public constructor(
            value: Float,
            targetValue: Float,
            contentDescription: ComplicationText
        ) : this(value, valueExpression = null, targetValue, contentDescription)

        /**
         * Creates a [Builder] for a [GoalProgressComplicationData] with a [DynamicFloat] value.
         *
         * @param valueExpression The [DynamicFloat] of the goal complication which will be
         *   evaluated into a value dynamically, and should be >= 0.
         * @param targetValue The target value. This must be less than [Float.MAX_VALUE].
         * @param contentDescription Defines localized text that briefly describes content of the
         *   complication. This property is used primarily for accessibility. Since some
         *   complications do not have textual representation this attribute can be used for
         *   providing such. Please do not include the word 'complication' in the description.
         */
        // TODO(b/269414040): Unhide complication expression APIs.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor(
            valueExpression: DynamicFloat,
            targetValue: Float,
            contentDescription: ComplicationText
        ) : this(
            value = 0f /* sensible default */,
            valueExpression,
            targetValue,
            contentDescription
        )

        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null
        private var colorRamp: ColorRamp? = null

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

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply { this.text = text }

        /**
         * Sets an optional hint which suggests the renderer draws the complication using a
         * [ColorRamp].
         */
        public fun setColorRamp(colorRamp: ColorRamp?): Builder = apply {
            this.colorRamp = colorRamp
        }

        /** Builds the [GoalProgressComplicationData]. */
        public override fun build() =
            GoalProgressComplicationData(
                value,
                valueExpression,
                targetValue,
                monochromaticImage,
                smallImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                colorRamp,
                persistencePolicy,
                displayPolicy,
                fallback = fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
        builder.setRangedValue(value)
        builder.setRangedValueExpression(valueExpression)
        builder.setTargetValue(targetValue)
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
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
            builder.setColorRamp(it.colors)
            builder.setColorRampIsSmoothShaded(it.interpolated)
        }
    }

    override fun toString(): String {
        val valueString =
            if (WireComplicationData.shouldRedact()) {
                "REDACTED"
            } else {
                value.toString()
            }
        val valueExpressionString =
            if (WireComplicationData.shouldRedact()) {
                "REDACTED"
            } else {
                valueExpression.toString()
            }
        return "GoalProgressComplicationData(value=$valueString, " +
            "valueExpression=$valueExpressionString, targetValue=$targetValue, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, title=$title, " +
            "text=$text, contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "colorRamp=$colorRamp, persistencePolicy=$persistencePolicy, " +
            "displayPolicy=$displayPolicy, fallback=$fallback)"
    }

    override fun hasPlaceholderFields() =
        value == PLACEHOLDER ||
            text?.isPlaceholder() == true ||
            title?.isPlaceholder() == true ||
            monochromaticImage?.isPlaceholder() == true ||
            smallImage?.isPlaceholder() == true

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
        @JvmField public val TYPE: ComplicationType = ComplicationType.GOAL_PROGRESS

        /**
         * Used to signal the range should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField public val PLACEHOLDER = Float.MAX_VALUE
    }
}

/**
 * Type used for complications which want to display the breakdown of something (e.g. nutrition
 * data) perhaps as a pie chart, or as a stacked bar chart etc. The breakdown may be accompanied by
 * an icon and/or short text and title.
 *
 * The [elements] field is required for this type and is expected to always be displayed.
 *
 * The [monochromaticImage], [smallImage], [title], and [text] fields are optional but at least one
 * of them must be set. The watch face may choose which of these fields to display, if any.
 *
 * If a [monochromaticImage] and a [smallImage] are both specified then only one should be
 * displayed. If the complication is drawn with a single color it's recommended to choose
 * [monochromaticImage] and apply a tint. If the complication is rendered with multiple colors it's
 * recommended to choose the [smallImage]. It's best practice for a ComplicationDataSource to
 * specify both a [monochromaticImage] and a [smallImage].
 *
 * A data source that wants to serve a SmallImageComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="WEIGHTED_ELEMENTS"/>
 * ```
 *
 * @property elements The breakdown of the subject into various [Element]s (e.g. the proportion of
 *   calories consumed which were carbohydrates, fats, etc.). The colors need to be meaningful to
 *   the user (e.g. blue is cold, yellow/red is worm), and should be consistent with the experience
 *   launched by tapping on the complication. If this is equal to [PLACEHOLDER] then the renderer
 *   must display this in a visually distinct way to suggest to the user that it's placeholder data.
 *   E.g. each element is rendered in light grey. The maximum valid size of this list is provided by
 *   [getMaxElements] and it will be truncated if its larger.
 * @property elementBackgroundColor If elements are draw as segments then this is the background
 *   color to use in between them.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face. If the monochromaticImage is equal to [MonochromaticImage.PLACEHOLDER] the renderer must
 *   treat it as a placeholder rather than rendering normally, its suggested it should be rendered
 *   as a light grey box.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property title The optional title [ComplicationText]. The length of the title, including any
 *   time-dependent values at any valid time, is expected to not exceed seven characters. When using
 *   this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the title is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it
 *   as a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 *   any time-dependent values at any valid time, is expected to not exceed seven characters. When
 *   using this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated. If the text is equal to [ComplicationText.PLACEHOLDER] the renderer must treat it as
 *   a placeholder rather than rendering normally, its suggested it should be rendered as a light
 *   grey box.
 * @property contentDescription The content description field for accessibility. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class WeightedElementsComplicationData
internal constructor(
    public val elements: List<Element>,
    @ColorInt public val elementBackgroundColor: Int,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    public val title: ComplicationText?,
    public val text: ComplicationText?,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: WeightedElementsComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {

    init {
        require(monochromaticImage != null || smallImage != null || text != null || title != null) {
            "At least one of monochromaticImage, smallImage, text or title must be set"
        }
    }
    /**
     * Describes a single value within a [WeightedElementsComplicationData].
     *
     * @property weight The weight of the Element which must be > zero. The size of the element when
     *   rendered should be proportional to its weight. Weights are not required to sum to any
     *   particular value.
     * @property color The color of the Element, which must be used instead of the watch face's
     *   colors. This color needs to be meaningful to the user in conjunction with the other fields
     *   (e.g. blue is cold, red/yellow is warm). Tapping on the complication should launch an
     *   experience where the data is presented in more detail. Care must be taken to ensure the
     *   colors used are consistent with the launched experience.
     */
    class Element(
        @FloatRange(from = 0.0, fromInclusive = false) val weight: Float,
        @ColorInt val color: Int
    ) {
        init {
            require(weight > 0) { "The weight must be > 0" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Element

            if (color != other.color) return false
            if (weight != other.weight) return false

            return true
        }

        override fun hashCode(): Int {
            var result = color
            result = 31 * result + weight.hashCode()
            return result
        }

        override fun toString(): String {
            return "Element(color=$color, weight=$weight)"
        }
    }

    /**
     * Builder for [WeightedElementsComplicationData].
     *
     * You must at a minimum set the [elements] field and at least one of [monochromaticImage],
     * [smallImage], [text] or [title].
     *
     * @param elements The breakdown of the subject into various [Element]s. E.g. the proportion of
     *   calories consumed which were carbohydrates, fats etc... The [tapAction] must take the user
     *   to an experience where the color key becomes obvious. The maximum valid size of this list
     *   is provided by [getMaxElements].
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public class Builder(
        elements: List<Element>,
        private var contentDescription: ComplicationText
    ) : BaseBuilder<Builder, WeightedElementsComplicationData>() {
        @ColorInt private var elementBackgroundColor: Int = Color.TRANSPARENT
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null
        private var title: ComplicationText? = null
        private var text: ComplicationText? = null

        init {
            if (elements.size > getMaxElements()) {
                Log.w(
                    TAG,
                    "Found ${elements.size} elements but the maximum is ${getMaxElements()}," +
                        " truncating!"
                )
            }
        }

        private val elements: List<Element> =
            if (elements.size > getMaxElements()) {
                elements.subList(0, getMaxElements()) // NB the second parameter is exclusive!
            } else {
                elements
            }

        /**
         * Sets the background color to use between the [elements] if they are drawn segmented.
         * Defaults to [Color.TRANSPARENT] if not set.
         */
        public fun setElementBackgroundColor(@ColorInt elementBackgroundColor: Int): Builder =
            apply {
                this.elementBackgroundColor = elementBackgroundColor
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

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply { this.text = text }

        /** Builds the [GoalProgressComplicationData]. */
        public override fun build() =
            WeightedElementsComplicationData(
                elements,
                elementBackgroundColor,
                monochromaticImage,
                smallImage,
                title,
                text,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
        builder.setElementWeights(elements.map { it.weight }.toFloatArray())
        builder.setElementColors(elements.map { it.color }.toIntArray())
        builder.setElementBackgroundColor(elementBackgroundColor)
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
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

    override fun getNextChangeInstant(afterInstant: Instant): Instant {
        val titleChangeInstant = title?.getNextChangeTime(afterInstant) ?: Instant.MAX
        val textChangeInstant = text?.getNextChangeTime(afterInstant) ?: Instant.MAX
        return if (textChangeInstant.isBefore(titleChangeInstant)) {
            textChangeInstant
        } else {
            titleChangeInstant
        }
    }

    override fun toString(): String {
        val elementsString =
            if (WireComplicationData.shouldRedact()) {
                "REDACTED"
            } else {
                elements.joinToString()
            }
        return "WeightedElementsComplicationData(elements=$elementsString, " +
            "elementBackgroundColor=$elementBackgroundColor, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, title=$title, " +
            "text=$text, contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    override fun hasPlaceholderFields() =
        elements == PLACEHOLDER ||
            text?.isPlaceholder() == true ||
            title?.isPlaceholder() == true ||
            monochromaticImage?.isPlaceholder() == true ||
            smallImage?.isPlaceholder() == true

    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.WEIGHTED_ELEMENTS

        /**
         * Used to signal the range should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField public val PLACEHOLDER = emptyList<Element>()

        /**
         * Returns the maximum size for [elements]. Complications are small and if we have a very
         * large number of elements we likely won't be able to render them properly because the
         * individual elements will be too small on screen.
         */
        @JvmStatic public fun getMaxElements() = 7
    }
}

/**
 * Type used for complications which consist only of a [MonochromaticImage].
 *
 * The image is expected to always be displayed.
 *
 * A data source that wants to serve a MonochromaticImageComplicationData must include the following
 * meta data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="ICON"/>
 * ```
 *
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face (typically with SRC_IN). If the monochromaticImage is equal to
 *   [MonochromaticImage.PLACEHOLDER] the renderer must treat it as a placeholder rather than
 *   rendering normally, it's suggested it should be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 *   describe what data the icon represents. If the icon is purely stylistic, and does not convey
 *   any information to the user, then provide an empty content description. If no content
 *   description is provided, a generic content description will be used instead. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
public class MonochromaticImageComplicationData
internal constructor(
    public val monochromaticImage: MonochromaticImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: MonochromaticImageComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {
    /**
     * Builder for [MonochromaticImageComplicationData].
     *
     * You must at a minimum set the [monochromaticImage] and [contentDescription] fields.
     *
     * @param monochromaticImage The [MonochromaticImage] to be displayed
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    public class Builder(
        private val monochromaticImage: MonochromaticImage,
        private val contentDescription: ComplicationText
    ) : BaseBuilder<Builder, MonochromaticImageComplicationData>() {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public override fun build(): MonochromaticImageComplicationData =
            MonochromaticImageComplicationData(
                monochromaticImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
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

    override fun hasPlaceholderFields() = monochromaticImage.isPlaceholder()

    override fun toString(): String {
        return "MonochromaticImageComplicationData(monochromaticImage=$monochromaticImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.MONOCHROMATIC_IMAGE
    }
}

/**
 * Type used for complications which consist only of a [SmallImage].
 *
 * The image is expected to always be displayed.
 *
 * A data source that wants to serve a SmallImageComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="SMALL_IMAGE"/>
 * ```
 *
 * @property smallImage The [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication. If the smallImage is equal to [SmallImage.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 *   describe what data the image represents. If the image is purely stylistic, and does not convey
 *   any information to the user, then provide an empty content description. If no content
 *   description is provided, a generic content description will be used instead. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
public class SmallImageComplicationData
internal constructor(
    public val smallImage: SmallImage,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: SmallImageComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {
    /**
     * Builder for [SmallImageComplicationData].
     *
     * You must at a minimum set the [smallImage] and [contentDescription] fields.
     *
     * @param smallImage The [SmallImage] to be displayed
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    public class Builder(
        private val smallImage: SmallImage,
        private val contentDescription: ComplicationText
    ) : BaseBuilder<Builder, SmallImageComplicationData>() {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null

        /** Sets optional pending intent to be invoked when the complication is tapped. */
        public fun setTapAction(tapAction: PendingIntent?): Builder = apply {
            this.tapAction = tapAction
        }

        /** Sets optional time range during which the complication has to be shown. */
        @Suppress("MissingGetterMatchingBuilder") // b/174052810
        public fun setValidTimeRange(validTimeRange: TimeRange?): Builder = apply {
            this.validTimeRange = validTimeRange
        }

        /** Builds the [MonochromaticImageComplicationData]. */
        public override fun build(): SmallImageComplicationData =
            SmallImageComplicationData(
                smallImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
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

    override fun toString(): String {
        return "SmallImageComplicationData(smallImage=$smallImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    override fun hasPlaceholderFields() = smallImage.isPlaceholder()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.SMALL_IMAGE
    }
}

/**
 * Type used for complications which consist only of an image that is expected to fill a large part
 * of the watch face, large enough to be shown as either a background or as part of a high
 * resolution complication.
 *
 * The image is expected to always be displayed. The image may be shown as the background, any other
 * part of the watch face or within a complication. The image is large enough to be cover the entire
 * screen. The image may be cropped to fit the watch face or complication.
 *
 * A data source that wants to serve a PhotoImageComplicationData must include the following meta
 * data in its manifest (NB the value is a comma separated list):
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 *    android:value="LARGE_IMAGE"/>
 * ```
 *
 * @property photoImage The [Icon] that is expected to fill a large part of the watch face, large
 *   enough to be shown as either a background or as part of a high resolution complication. This
 *   must not be tinted. If the photoImage is equal to [PhotoImageComplicationData.PLACEHOLDER] the
 *   renderer must treat it as a placeholder rather than rendering normally, its suggested it should
 *   be rendered as a light grey box.
 * @property contentDescription The content description field for accessibility and is used to
 *   describe what data the image represents. If the image is purely stylistic, and does not convey
 *   any information to the user, then provide an empty content description. If no content
 *   description is provided, a generic content description will be used instead. Please do not
 *   include the word 'complication' in the description.
 * @property fallback Used in case any expression has been invalidated.
 */
public class PhotoImageComplicationData
internal constructor(
    public val photoImage: Icon,
    public val contentDescription: ComplicationText?,
    tapAction: PendingIntent?,
    validTimeRange: TimeRange?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
    // TODO(b/269414040): Unhide complication expression APIs.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val fallback: PhotoImageComplicationData?,
) :
    ComplicationData(
        TYPE,
        tapAction = tapAction,
        cachedWireComplicationData = cachedWireComplicationData,
        validTimeRange = validTimeRange ?: TimeRange.ALWAYS,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = fallback,
    ) {
    /**
     * Builder for [PhotoImageComplicationData].
     *
     * You must at a minimum set the [photoImage] and [contentDescription] fields.
     *
     * @param photoImage The [Icon] to be displayed
     * @param contentDescription Defines localized text that briefly describes content of the
     *   complication. This property is used primarily for accessibility. Since some complications
     *   do not have textual representation this attribute can be used for providing such. Please do
     *   not include the word 'complication' in the description.
     */
    public class Builder(
        private val photoImage: Icon,
        private val contentDescription: ComplicationText
    ) : BaseBuilder<Builder, PhotoImageComplicationData>() {
        private var tapAction: PendingIntent? = null
        private var validTimeRange: TimeRange? = null

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

        /** Builds the [PhotoImageComplicationData]. */
        public override fun build(): PhotoImageComplicationData =
            PhotoImageComplicationData(
                photoImage,
                contentDescription,
                tapAction,
                validTimeRange,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
                fallback,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
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

    override fun toString(): String {
        return "PhotoImageComplicationData(photoImage=$photoImage, " +
            "contentDescription=$contentDescription), " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy, " +
            "fallback=$fallback)"
    }

    override fun hasPlaceholderFields() = photoImage.isPlaceholder()

    /** @hide */
    public companion object {
        /** The [ComplicationType] corresponding to objects of this type. */
        @JvmField public val TYPE: ComplicationType = ComplicationType.PHOTO_IMAGE

        /**
         * Used to signal the photo image should be rendered as a placeholder. It's suggested that a
         * placeholder ranged value be drawn as a grey arc with a percentage value selected by the
         * renderer.
         *
         * Note a placeholder may only be used in the context of
         * [NoDataComplicationData.placeholder].
         */
        @JvmField public val PLACEHOLDER: Icon = createPlaceholderIcon()
    }
}

/**
 * Type sent by the system when the watch face does not have permission to receive complication
 * data.
 *
 * The text, title, and icon may be displayed by watch faces, but this is not required.
 *
 * It is recommended that, where possible, tapping on the complication when in this state should
 * trigger a permission request. Note this is done by
 * [androidx.wear.watchface.ComplicationSlotsManager] for androidx watch faces.
 *
 * @property text The body [ComplicationText] of the complication. The length of the text, including
 *   any time-dependent values at any valid time, is expected to not exceed seven characters. When
 *   using this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated.
 * @property title The optional title [ComplicationText]. The length of the text, including any
 *   time-dependent values at any valid time, is expected to not exceed seven characters. When using
 *   this text, the watch face should be able to display any string of up to seven characters
 *   (reducing the text size appropriately if the string is very wide). Although not expected, it is
 *   possible that strings of more than seven characters might be seen, in which case they may be
 *   truncated.
 * @property monochromaticImage A simple [MonochromaticImage] image that can be tinted by the watch
 *   face.
 * @property smallImage A [SmallImage] that is expected to cover a small fraction of a watch face
 *   occupied by a single complication
 */
public class NoPermissionComplicationData
internal constructor(
    public val text: ComplicationText?,
    public val title: ComplicationText?,
    public val monochromaticImage: MonochromaticImage?,
    public val smallImage: SmallImage?,
    cachedWireComplicationData: WireComplicationData?,
    dataSource: ComponentName?,
    @ComplicationPersistencePolicy persistencePolicy: Int,
    @ComplicationDisplayPolicy displayPolicy: Int,
) :
    ComplicationData(
        TYPE,
        tapAction = null,
        cachedWireComplicationData = cachedWireComplicationData,
        dataSource = dataSource,
        persistencePolicy = persistencePolicy,
        displayPolicy = displayPolicy,
        fallback = null,
    ) {
    /** Builder for [NoPermissionComplicationData]. */
    public class Builder : BaseBuilder<Builder, NoPermissionComplicationData>() {
        private var text: ComplicationText? = null
        private var title: ComplicationText? = null
        private var monochromaticImage: MonochromaticImage? = null
        private var smallImage: SmallImage? = null

        /** Sets optional text associated with the complication data. */
        public fun setText(text: ComplicationText?): Builder = apply { this.text = text }

        /** Sets optional title associated with the complication data. */
        public fun setTitle(title: ComplicationText?): Builder = apply { this.title = title }

        /** Sets optional icon associated with the complication data. */
        public fun setMonochromaticImage(monochromaticImage: MonochromaticImage?): Builder = apply {
            this.monochromaticImage = monochromaticImage
        }

        /** Sets optional image associated with the complication data. */
        public fun setSmallImage(smallImage: SmallImage?): Builder = apply {
            this.smallImage = smallImage
        }

        /** Builds the [NoPermissionComplicationData]. */
        public override fun build(): NoPermissionComplicationData =
            NoPermissionComplicationData(
                text,
                title,
                monochromaticImage,
                smallImage,
                cachedWireComplicationData,
                dataSource,
                persistencePolicy,
                displayPolicy,
            )
    }

    override fun fillWireComplicationDataBuilder(builder: WireComplicationDataBuilder) {
        super.fillWireComplicationDataBuilder(builder)
        builder.setShortText(text?.toWireComplicationText())
        builder.setShortTitle(title?.toWireComplicationText())
        monochromaticImage?.addToWireComplicationData(builder)
        smallImage?.addToWireComplicationData(builder)
    }

    override fun toString(): String {
        return "NoPermissionComplicationData(text=$text, title=$title, " +
            "monochromaticImage=$monochromaticImage, smallImage=$smallImage, " +
            "tapActionLostDueToSerialization=$tapActionLostDueToSerialization, " +
            "tapAction=$tapAction, validTimeRange=$validTimeRange, dataSource=$dataSource, " +
            "persistencePolicy=$persistencePolicy, displayPolicy=$displayPolicy)"
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
        @JvmField public val TYPE: ComplicationType = ComplicationType.NO_PERMISSION
    }
}

internal fun WireComplicationData.toPlaceholderComplicationData(): ComplicationData? =
    when (type) {
        NoDataComplicationData.TYPE.toWireComplicationType() -> null
        EmptyComplicationData.TYPE.toWireComplicationType() -> null
        NotConfiguredComplicationData.TYPE.toWireComplicationType() -> null
        else ->
            toApiComplicationData(placeholderAware = true).let {
                if (it is NoDataComplicationData) null else it
            }
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationData.toApiComplicationData(): ComplicationData =
    toApiComplicationData(placeholderAware = false)

@Suppress("NewApi")
private fun WireComplicationData.toApiComplicationData(
    placeholderAware: Boolean
): ComplicationData {
    try {
        return when (type) {
            NoDataComplicationData.TYPE.toWireComplicationType() ->
                NoDataComplicationData(placeholder?.toPlaceholderComplicationData(), this)
            EmptyComplicationData.TYPE.toWireComplicationType() -> EmptyComplicationData()
            NotConfiguredComplicationData.TYPE.toWireComplicationType() ->
                NotConfiguredComplicationData()
            ShortTextComplicationData.TYPE.toWireComplicationType() ->
                ShortTextComplicationData(
                    text = shortText!!.toApiComplicationText(placeholderAware),
                    title = shortTitle?.toApiComplicationText(placeholderAware),
                    monochromaticImage = parseIcon(placeholderAware),
                    smallImage = parseSmallImage(placeholderAware),
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            LongTextComplicationData.TYPE.toWireComplicationType() ->
                LongTextComplicationData(
                    text = longText!!.toApiComplicationText(placeholderAware),
                    title = longTitle?.toApiComplicationText(placeholderAware),
                    monochromaticImage = parseIcon(placeholderAware),
                    smallImage = parseSmallImage(placeholderAware),
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            RangedValueComplicationData.TYPE.toWireComplicationType() ->
                RangedValueComplicationData(
                    value = rangedValue,
                    valueExpression = rangedValueExpression,
                    min = rangedMinValue,
                    max = rangedMaxValue,
                    monochromaticImage = parseIcon(placeholderAware),
                    smallImage = parseSmallImage(placeholderAware),
                    title = shortTitle?.toApiComplicationText(placeholderAware),
                    text = shortText?.toApiComplicationText(placeholderAware),
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    colorRamp = colorRamp?.let { ColorRamp(it, isColorRampInterpolated!!) },
                    valueType = rangedValueType,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            MonochromaticImageComplicationData.TYPE.toWireComplicationType() ->
                MonochromaticImageComplicationData(
                    monochromaticImage = parseIcon(placeholderAware)!!,
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            SmallImageComplicationData.TYPE.toWireComplicationType() ->
                SmallImageComplicationData(
                    smallImage = parseSmallImage(placeholderAware)!!,
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            PhotoImageComplicationData.TYPE.toWireComplicationType() ->
                PhotoImageComplicationData(
                    photoImage = parseLargeImage(placeholderAware)!!,
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            NoPermissionComplicationData.TYPE.toWireComplicationType() ->
                NoPermissionComplicationData(
                    text = shortText?.toApiComplicationText(),
                    title = shortTitle?.toApiComplicationText(),
                    monochromaticImage = parseIcon(),
                    smallImage = parseSmallImage(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                )
            GoalProgressComplicationData.TYPE.toWireComplicationType() ->
                GoalProgressComplicationData(
                    value = rangedValue,
                    valueExpression = rangedValueExpression,
                    targetValue = targetValue,
                    monochromaticImage = parseIcon(placeholderAware),
                    smallImage = parseSmallImage(placeholderAware),
                    title = shortTitle?.toApiComplicationText(placeholderAware),
                    text = shortText?.toApiComplicationText(placeholderAware),
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    colorRamp = colorRamp?.let { ColorRamp(it, isColorRampInterpolated!!) },
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            WeightedElementsComplicationData.TYPE.toWireComplicationType() ->
                WeightedElementsComplicationData(
                    elements =
                        if (placeholderAware && elementWeights!!.isEmpty()) {
                            WeightedElementsComplicationData.PLACEHOLDER
                        } else {
                            val elementWeights = this.elementWeights!!
                            val elementColors = this.elementColors!!
                            require(elementWeights.size == elementColors.size) {
                                "elementWeights and elementColors must have the same size"
                            }
                            elementWeights
                                .mapIndexed { index, weight ->
                                    WeightedElementsComplicationData.Element(
                                        weight,
                                        elementColors[index]
                                    )
                                }
                                .toList()
                        },
                    elementBackgroundColor = elementBackgroundColor,
                    monochromaticImage = parseIcon(placeholderAware),
                    smallImage = parseSmallImage(placeholderAware),
                    title = shortTitle?.toApiComplicationText(placeholderAware),
                    text = shortText?.toApiComplicationText(placeholderAware),
                    contentDescription = contentDescription?.toApiComplicationText()
                            ?: ComplicationText.EMPTY,
                    tapAction = tapAction,
                    validTimeRange = parseTimeRange(),
                    cachedWireComplicationData = this,
                    dataSource = dataSource,
                    persistencePolicy = persistencePolicy,
                    displayPolicy = displayPolicy,
                    fallback = placeholder?.toTypedApiComplicationData(),
                )
            else -> NoDataComplicationData()
        }
    } catch (e: Exception) {
        Log.e(
            TAG,
            "WireComplicationData.toApiComplicationData failed for " + toStringNoRedaction(),
            e
        )
        throw e
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("UNCHECKED_CAST")
public fun <T : ComplicationData> WireComplicationData.toTypedApiComplicationData(): T =
    toApiComplicationData() as T

private fun WireComplicationData.parseTimeRange() =
    if ((startDateTimeMillis == 0L) and (endDateTimeMillis == Long.MAX_VALUE)) {
        null
    } else {
        TimeRange(
            Instant.ofEpochMilli(startDateTimeMillis),
            Instant.ofEpochMilli(endDateTimeMillis)
        )
    }

private fun WireComplicationData.parseIcon(placeholderAware: Boolean = false) =
    icon?.let {
        if (placeholderAware && it.isPlaceholder()) {
            MonochromaticImage.PLACEHOLDER
        } else {
            MonochromaticImage.Builder(it).apply { setAmbientImage(burnInProtectionIcon) }.build()
        }
    }

private fun WireComplicationData.parseSmallImage(placeholderAware: Boolean = false) =
    smallImage?.let {
        if (placeholderAware && it.isPlaceholder()) {
            SmallImage.PLACEHOLDER
        } else {
            val imageStyle =
                when (smallImageStyle) {
                    WireComplicationData.IMAGE_STYLE_ICON -> SmallImageType.ICON
                    WireComplicationData.IMAGE_STYLE_PHOTO -> SmallImageType.PHOTO
                    else -> SmallImageType.PHOTO
                }
            SmallImage.Builder(it, imageStyle)
                .apply { setAmbientImage(burnInProtectionSmallImage) }
                .build()
        }
    }

private fun WireComplicationData.parseLargeImage(placeholderAware: Boolean = false) =
    largeImage?.let {
        if (placeholderAware && it.isPlaceholder()) {
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
