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

import android.content.res.Resources
import android.icu.util.TimeZone
import androidx.annotation.RestrictTo
import java.util.concurrent.TimeUnit

/** The wire format for [ComplicationText]. */
internal typealias WireComplicationText = android.support.wearable.complications.ComplicationText

private typealias WireComplicationTextTimeDifferenceBuilder =
    android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder

private typealias WireComplicationTextTimeFormatBuilder =
    android.support.wearable.complications.ComplicationText.TimeFormatBuilder

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
     * @param dateTimeMillis milliseconds since epoch, e.g. from [System.currentTimeMillis]
     */
    public fun getTextAt(
        resources: Resources,
        dateTimeMillis: Long
    ): CharSequence

    /**
     * Returns true if the result of [getTextAt] will be the same for both [firstDateTimeMillis]
     * and [secondDateTimeMillis].
     */
    public fun returnsSameText(
        firstDateTimeMillis: Long,
        secondDateTimeMillis: Long
    ): Boolean

    /** Returns the next time after [fromDateTimeMillis] at which the text may change.  */
    public fun getNextChangeTime(fromDateTimeMillis: Long): Long

    public fun isAlwaysEmpty(): Boolean

    /**
     * Converts this value to [WireComplicationText] object used for serialization.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun asWireComplicationText(): WireComplicationText

    public companion object {
        /** Returns a [ComplicationText] that represent a plain [CharSequence]. */
        @JvmStatic
        public fun plain(text: CharSequence): ComplicationText =
            WireComplicationText.plainText(text).asApiComplicationText()

        /**
         * Returns a builder for a [ComplicationText] representing a time difference, which can
         * be either a count up or a count down.
         *
         * @param[style] the style of the time difference to be displayed.
         */
        @JvmStatic
        public fun timeDifferenceBuilder(
            style: TimeDifferenceStyle,
            reference: TimeReference
        ): TimeDifferenceComplicationText.Builder =
            TimeDifferenceComplicationText.Builder(style, reference)

        /**
         * Returns a builder for a [ComplicationText] representing the current time.
         *
         * @param[format] the format in which the time should be displayed. This should be a pattern
         * as used by [java.text.SimpleDateFormat].
         */
        @JvmStatic
        public fun timeFormatBuilder(format: String): TimeFormatComplicationText.Builder =
            TimeFormatComplicationText.Builder(format)
    }
}

/** The styling used for showing a time different by [ComplicationText.timeDifferenceBuilder]. */
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
     * Builder for [ComplicationText] representing a time difference.
     *
     * Requires setting a [TimeDifferenceStyle].
     */
    public class Builder(
        private val style: TimeDifferenceStyle,
        private val reference: TimeReference
    ) {
        private var text: CharSequence? = null
        private var displayAsNow: Boolean? = null
        private var minimumUnit: TimeUnit? = null

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
        public fun setMinimumUnit(minimumUnit: TimeUnit?): Builder =
            apply { this.minimumUnit = minimumUnit }

        /** Builds a [TimeDifferenceComplicationText]. */
        public fun build(): TimeDifferenceComplicationText = TimeDifferenceComplicationText(
            WireComplicationTextTimeDifferenceBuilder().apply {
                setStyle(style.wireStyle)
                setSurroundingText(text)
                if (reference.hasStartDateTimeMillis()) {
                    setReferencePeriodStartMillis(reference.startDateTimeMillis)
                }
                if (reference.hasEndDateTimeMillis()) {
                    setReferencePeriodEndMillis(reference.endDateTimeMillis)
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
    override fun getTextAt(resources: Resources, dateTimeMillis: Long) =
        delegate.getTextAt(resources, dateTimeMillis)

    override fun returnsSameText(firstDateTimeMillis: Long, secondDateTimeMillis: Long) =
        delegate.returnsSameText(firstDateTimeMillis, secondDateTimeMillis)

    override fun getNextChangeTime(fromDateTimeMillis: Long) =
        delegate.getNextChangeTime(fromDateTimeMillis)

    override fun isAlwaysEmpty() = delegate.isAlwaysEmpty

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun asWireComplicationText() = delegate
}

/** Converts a [WireComplicationText] into an equivalent [ComplicationText] instead. */
internal fun WireComplicationText.asApiComplicationText(): ComplicationText =
    DelegatingComplicationText(this)

/** Converts a [TimeZone] into an equivalent [java.util.TimeZone]. */
internal fun TimeZone.asJavaTimeZone(): java.util.TimeZone =
    java.util.TimeZone.getTimeZone(this.id)
