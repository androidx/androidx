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

import android.content.res.Resources
import android.icu.util.TimeZone
import android.support.wearable.complications.TimeDependentText
import android.support.wearable.complications.TimeDifferenceText
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.annotation.RestrictTo
import java.time.Instant
import java.util.concurrent.TimeUnit

/** The wire format for [ComplicationText]. */
internal typealias WireComplicationText = android.support.wearable.complications.ComplicationText

private typealias WireComplicationTextTimeDifferenceBuilder =
    android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder

private typealias WireComplicationTextTimeFormatBuilder =
    android.support.wearable.complications.ComplicationText.TimeFormatBuilder

private typealias WireTimeDependentText = android.support.wearable.complications.TimeDependentText

/**
 * The text within a complication.
 *
 * This text may change over time and this interface provides both a way to determine the current
 * text to show with [getTextAt] but also a way to know whether the text needs to be
 * re-rendered, by means of [returnsSameText], [getNextChangeTime], and [isAlwaysEmpty].
 */
public interface ComplicationText {
    /**
     * Returns the text that should be displayed for the given timestamp.
     *
     * @param resources [Resources] from the current context
     * @param instant The [Instant] at which to sample the text
     */
    public fun getTextAt(
        resources: Resources,
        instant: Instant
    ): CharSequence

    /**
     * Returns true if the result of [getTextAt] will be the same for both [firstInstant] and
     * [secondInstant].
     */
    public fun returnsSameText(firstInstant: Instant, secondInstant: Instant): Boolean

    /** Returns the next time after [afterInstant] at which the text may change.  */
    public fun getNextChangeTime(afterInstant: Instant): Instant

    public fun isAlwaysEmpty(): Boolean

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public fun getTimeDependentText(): TimeDependentText

    /**
     * Converts this value to [WireComplicationText] object used for serialization.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toWireComplicationText(): WireComplicationText

    public companion object {
        @JvmField
        public val EMPTY: ComplicationText = PlainComplicationText.Builder("").build()
    }
}

/** A [ComplicationText] that contains plain text. */
public class PlainComplicationText internal constructor(
    delegate: WireComplicationText
) : ComplicationText by DelegatingComplicationText(delegate) {
    /**
     * A builder for [PlainComplicationText].
     *
     * @param[text] the text to be displayed.
     */
    public class Builder(private var text: CharSequence) {
        public fun build(): PlainComplicationText =
            PlainComplicationText(WireComplicationText.plainText(text))
    }
}

/** The styling used for showing a time different by [ComplicationText#TimeDifferenceBuilder]. */
public enum class TimeDifferenceStyle(internal var wireStyle: Int) {

    /**
     * Style for time differences shown in a numeric fashion like a timer.
     *
     * For time difference `t`:
     *
     * If `t < 1 hour`, the value will be shown as minutes and seconds, such as `02:35` for 2
     * minutes and 35 seconds.
     *
     * If `1 hour <= t < 1 day`, the value will be shown as hours and minutes, such as
     * `4:02` for 4 hours and 2 minutes, or as `12:02` for 12 hours and 2 minutes.
     *
     * If `1 day <= t < 10 days`, the value will be shown as days and hours, such as `3d 4h` for
     * 3 days 4 hours.
     *
     * If `10 days <= t`, the value will be shown as just days, such as `13d` for 13 days.
     *
     * The characters used will be localised to match the default locale.
     */
    STOPWATCH(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH),

    /**
     * Style for time differences shown in a short alpha-numeric style, with only the most
     * significant unit included.
     *
     * For time difference `t`:
     *
     * If `t < 1 hour`, the value will be shown as a number of minutes, such as `2m` for 2 minutes
     * . Seconds are not displayed.
     *
     * If `1 hour <= t < 1 day`, the value will be shown as a number of hours, such as `4h` for 4
     * hours.
     *
     * If `1 days <= t`, the value will be shown as a number of days, such as `13d` for 13 days.
     *
     * The characters used will be localised to match the default locale.
     */
    SHORT_SINGLE_UNIT(WireComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT),

    /**
     * Style for time differences shown in a short alpha-numeric style, with up to two significant
     * units included.
     *
     * For time difference `t`:
     *
     * If `t < 1 hour`, the value will be shown as a number of minutes, such as `2m`
     * for 2 minutes. Seconds are not displayed.
     *
     * If `1 hour <= t < 1 day`, the value will be shown as hours and minutes, such as
     * `4h 2m` for 4 hours and 2 minutes.
     *
     * If `1 day <= t < 10 days`, the value will be shown as days and hours, such as `3d 4h` for
     * 3 days 4 hours.
     *
     * If `10 days <= t`, the value will be shown as a number of days, such as `13d`
     * for 13 days.
     *
     * The characters used will be localised to match the default locale. If the representation
     * of the time difference with two units would be too long in the default locale, just a single
     * unit may be shown instead.
     */
    SHORT_DUAL_UNIT(WireComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT),

    /**
     * Style for time differences shown using (possibly abbreviated) words, with only the most
     * significant unit included.
     *
     * For time difference `t`:
     *
     * If `t < 1 hour`, the value will be shown as a number of minutes, such as `1 min` for 1
     * minute or `2 mins` for 2 minutes. Seconds are not displayed.
     *
     * If `1 hour <= t < 1 day`, the value will be shown as a number of hours, such as
     * `1 hour` for 1 hour or `4 hours` for 4 hours.
     *
     * If `1 days <= t`, the value will be shown as a number of days, such as `1 day`
     * for 1 day or `13 days` for 13 days.
     *
     * The words used will be localised to match the default locale.
     */
    WORDS_SINGLE_UNIT(WireComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT),

    /**
     * Style for time differences shown using (possibly abbreviated) words, with only the most
     * significant unit included, that should fit within the character limit for a short text field.
     *
     * The output will be the same as for [WORDS_SINGLE_UNIT], except that if the text does not
     * fit into the seven character limit then a shorter form will be used instead, e.g. `1356d`
     * instead of `1356 days`.
     */
    SHORT_WORDS_SINGLE_UNIT(WireComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT);
}

/** A [ComplicationText] that represents a time difference. */
public class TimeDifferenceComplicationText internal constructor(
    delegate: WireComplicationText
) : ComplicationText by DelegatingComplicationText(delegate) {
    /**
     * Gets the smallest unit that may be shown in the time difference text. If specified, units
     * smaller than this minimum will not be included.
     */
    public fun getMinimumTimeUnit(): TimeUnit? =
        if (getTimeDependentText() is TimeDifferenceText)
            (getTimeDependentText() as TimeDifferenceText).minimumUnit
        else null

    /**
     * Builder for [ComplicationText] representing a time difference.
     *
     * Requires setting a [TimeDifferenceStyle].
     */
    public class Builder private constructor(
        private val style: TimeDifferenceStyle,
        private val startInstant: Instant?,
        private val endInstant: Instant?
    ) {
        private var text: CharSequence? = null
        private var displayAsNow: Boolean? = null
        private var minimumUnit: TimeUnit? = null

        /**
         * Constructs a [TimeDifferenceComplicationText.Builder] where the complication is counting
         * up until [countUpTimeReference].
         *
         * @param style The [TimeDifferenceStyle] to use when rendering the time difference.
         * @param countUpTimeReference The [CountUpTimeReference] to count up until.
         */
        public constructor(
            style: TimeDifferenceStyle,
            countUpTimeReference: CountUpTimeReference
        ) : this(style, null, countUpTimeReference.instant)

        /**
         * Constructs a [TimeDifferenceComplicationText.Builder] where the complication is counting
         * down until [countDownTimeReference].
         *
         * @param style The [TimeDifferenceStyle] to use when rendering the time difference.
         * @param countDownTimeReference The [CountDownTimeReference] to count down until.
         */
        public constructor(
            style: TimeDifferenceStyle,
            countDownTimeReference: CountDownTimeReference
        ) : this(style, countDownTimeReference.instant, null)

        /**
         * Sets the text within which the time difference will be displayed.
         *
         * Within the text, `^1` will be replaced with the time difference, so for example
         * to show a result like `"2 mins: meeting"` the text would be `"^1: meeting"`
         *
         * To use the `^` character within the text, escape it as `^^`.
         *
         * The text may contain spans, but the watch face is not required to respect them.
         *
         * The allowed spans are [ForegroundColorSpan], [LocaleSpan], [SubscriptSpan],
         * [SuperscriptSpan], [StyleSpan], [StrikethroughSpan], [TypefaceSpan] and [UnderlineSpan].
         */
        public fun setText(text: CharSequence?): Builder = apply { this.text = text }

        /**
         * Sets whether time difference should be displayed as "now" (appropriately localized)
         * when the given time is within the reference period. If false, then text representing zero
         * (e.g. `0 mins` or `00:00`) will be shown instead.
         *
         * The default is true for all styles except for [TimeDifferenceStyle.STOPWATCH].
         */
        public fun setDisplayAsNow(displayAsNow: Boolean): Builder =
            apply { this.displayAsNow = displayAsNow }

        /**
         * Sets the smallest unit that may be shown in the time difference text. If specified, units
         * smaller than this minimum will not be included.
         *
         * For example, if this is set to [TimeUnit.HOURS], and the style is
         * [TimeDifferenceStyle.SHORT_SINGLE_UNIT] then `12d` or `5h` would be shown as normal,
         * but `35m` would be shown as `1h`.
         *
         * If not specified the style will determine the smallest unit that will be shown.
         *
         * If the specified minimum is smaller than the smallest unit supported by the style,
         * then the minimum will be ignored. For example, if the style is
         * [TimeDifferenceStyle.SHORT_SINGLE_UNIT], then a minimum unit of [TimeUnit.SECONDS] will
         * have no effect.
         */
        public fun setMinimumTimeUnit(minimumUnit: TimeUnit?): Builder =
            apply { this.minimumUnit = minimumUnit }

        /** Builds a [TimeDifferenceComplicationText]. */
        public fun build(): TimeDifferenceComplicationText = TimeDifferenceComplicationText(
            WireComplicationTextTimeDifferenceBuilder().apply {
                setStyle(style.wireStyle)
                setSurroundingText(text)
                startInstant?.let {
                    setReferencePeriodStartMillis(it.toEpochMilli())
                }
                endInstant?.let {
                    setReferencePeriodEndMillis(it.toEpochMilli())
                }
                displayAsNow?.let { setShowNowText(it) }
                setMinimumUnit(minimumUnit)
            }.build()
        )
    }
}

/** The format in which the time should be displayed. */
public enum class TimeFormatStyle(internal var wireStyle: Int) {
    DEFAULT(WireComplicationText.FORMAT_STYLE_DEFAULT),
    UPPER_CASE(WireComplicationText.FORMAT_STYLE_UPPER_CASE),
    LOWER_CASE(WireComplicationText.FORMAT_STYLE_LOWER_CASE);
}

/** A [ComplicationText] that shows a formatted time. */
public class TimeFormatComplicationText internal constructor(
    delegate: WireComplicationText
) : ComplicationText by DelegatingComplicationText(delegate) {
    /**
     * A builder for [TimeFormatComplicationText].
     *
     * @param[format] the format in which the time should be displayed. This should be a pattern
     * as used by [java.text.SimpleDateFormat].
     */
    public class Builder(private var format: String) {
        private var style: TimeFormatStyle? = null
        private var text: CharSequence? = null
        private var timeZone: TimeZone? = null

        /**
         * Sets the style in which the time format part will be displayed.
         *
         * If not set, defaults to [TimeFormatStyle.DEFAULT], which leaves the formatted date
         * unchanged.
         */
        public fun setStyle(style: TimeFormatStyle): Builder = apply { this.style = style }

        /**
         * Sets the text within which the time difference will be displayed.
         *
         * Within the text, `^1` will be replaced with the time difference, so for example
         * to show a result like `"2 mins: meeting"` the text would be `"^1: meeting"`
         *
         * To use the `^` character within the text, escape it as `^^`.
         *
         * The text may contain spans, but ther watch face is not required to respect them.
         *
         * The allowed spans are [ForegroundColorSpan], [LocaleSpan], [SubscriptSpan],
         * [SuperscriptSpan], [StyleSpan], [StrikethroughSpan], [TypefaceSpan] and [UnderlineSpan].
         */
        public fun setText(text: CharSequence): Builder = apply { this.text = text }

        /**
         * Sets the time zone that will be used for the formatted time. If not set, the system's
         * default time zone will be used.
         */
        public fun setTimeZone(timeZone: TimeZone): Builder = apply { this.timeZone = timeZone }

        /** Builds a [TimeFormatComplicationText]. */
        public fun build(): TimeFormatComplicationText = TimeFormatComplicationText(
            WireComplicationTextTimeFormatBuilder().apply {
                setFormat(format)
                setStyle(style?.wireStyle ?: WireComplicationText.FORMAT_STYLE_DEFAULT)
                setSurroundingText(text)
                setTimeZone(timeZone?.asJavaTimeZone())
            }.build()
        )
    }
}

/** [ComplicationText] implementation that delegates to a [WireComplicationText] instance. */
private class DelegatingComplicationText(
    private val delegate: WireComplicationText
) : ComplicationText {
    override fun getTextAt(resources: Resources, instant: Instant) =
        delegate.getTextAt(resources, instant.toEpochMilli())

    override fun returnsSameText(firstInstant: Instant, secondInstant: Instant) =
        delegate.returnsSameText(firstInstant.toEpochMilli(), secondInstant.toEpochMilli())

    override fun getNextChangeTime(afterInstant: Instant) =
        Instant.ofEpochMilli(delegate.getNextChangeTime(afterInstant.toEpochMilli()))

    override fun isAlwaysEmpty() = delegate.isAlwaysEmpty
    override fun getTimeDependentText(): TimeDependentText = delegate.timeDependentText

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toWireComplicationText() = delegate
}

/** Converts a [WireComplicationText] into an equivalent [ComplicationText] instead. */
internal fun WireComplicationText.toApiComplicationText(): ComplicationText =
    DelegatingComplicationText(this)

/** Converts a [TimeZone] into an equivalent [java.util.TimeZone]. */
internal fun TimeZone.asJavaTimeZone(): java.util.TimeZone =
    java.util.TimeZone.getTimeZone(this.id)

/** [ComplicationText] implementation that delegates to a [WireTimeDependentText] instance. */
private class DelegatingTimeDependentText(
    private val delegate: WireTimeDependentText
) : ComplicationText {
    override fun getTextAt(resources: Resources, instant: Instant) =
        delegate.getTextAt(resources, instant.toEpochMilli())

    override fun returnsSameText(firstInstant: Instant, secondInstant: Instant) =
        delegate.returnsSameText(firstInstant.toEpochMilli(), secondInstant.toEpochMilli())

    override fun getNextChangeTime(afterInstant: Instant) =
        Instant.ofEpochMilli(delegate.getNextChangeTime(afterInstant.toEpochMilli()))

    override fun isAlwaysEmpty() = false

    override fun getTimeDependentText(): TimeDependentText = delegate

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toWireComplicationText(): WireComplicationText {
        throw UnsupportedOperationException(
            "DelegatingTimeDependentText doesn't support asWireComplicationText"
        )
    }
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireTimeDependentText.toApiComplicationText(): ComplicationText =
    DelegatingTimeDependentText(this)
