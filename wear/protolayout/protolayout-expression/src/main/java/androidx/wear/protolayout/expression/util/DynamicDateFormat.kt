/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.expression.util

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Equivalent to [android.icu.text.SimpleDateFormat], but generates a [DynamicString] based on a
 * [DynamicInstant].
 *
 * See [android.icu.text.SimpleDateFormat] documentation for the pattern syntax.
 *
 * Literal patterns are fully supported, including quotes (`'`) or non-letters (e.g. `:`).
 *
 * Currently this implementation only supports hour (`HKhk`), minute (`m`), and AM/PM (`a`)
 * patterns. Every other letter will throw [IllegalArgumentException].
 *
 * NOTE: [DynamicDateFormat] uses `Locale.getDefault(Locale.Category.FORMAT)` at the time of calling
 * [format] for AM/PM markers. This can change on the remote side, which would cause a mismatch
 * between the locally-formatted AM/PM and the remotely-formatted numbers, unless the provider sends
 * a newly formatted [DynamicString] (using a new invocation of [format]).
 *
 * Example usage:
 * ```
 * // This statement:
 * DynamicDateFormat(pattern = "HH:mm", timeZone = zone).format(dynamicInstant)
 * // Generates an equivalent of:
 * dynamicInstant
 *   .getHour(zone)
 *   .format(DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build())
 *   .concat(DynamicString.constant(":"))
 *   .concat(
 *     dynamicInstant.getMinute(zone)
 *       .format(DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build())
 *   )
 * ```
 */
@RequiresSchemaVersion(major = 1, minor = 300)
public class DynamicDateFormat
@VisibleForTesting
internal constructor(
    private val pattern: String,
    // TODO: b/297323092 - Allow providing in the constructor for both local and remote evaluation.
    //       Currently only used for AM/PM.
    private val locale: Locale?,
    public var timeZone: ZoneId,
) {

    @JvmOverloads
    public constructor(
        pattern: String,
        timeZone: ZoneId = ZoneId.systemDefault()
    ) : this(pattern, locale = null, timeZone)

    init {
        require(pattern.count { it == '\'' } % 2 == 0) { "Unterminated quote" }
    }

    private val _locale: Locale
        get() = locale ?: Locale.getDefault(Locale.Category.FORMAT)

    private val patternParts: List<Part> = extractPatternParts().mergeConstants().toList()

    /**
     * Formats the [DynamicInstant] (defaults to [DynamicInstant.platformTimeWithSecondsPrecision])
     * into a date/time [DynamicString].
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    @JvmOverloads
    public fun format(
        instant: DynamicInstant = DynamicInstant.platformTimeWithSecondsPrecision(),
    ): DynamicString =
        patternParts
            .map { it.format(instant) }
            .ifEmpty { listOf(DynamicString.constant("")) }
            .reduce { acc, formattedSection -> acc.concat(formattedSection) }

    /** Builds a [Part] sequence from the [pattern]. */
    private fun extractPatternParts(): Sequence<Part> = sequence {
        val patternLeft: ArrayDeque<Token> = pattern.tokenize()
        // Taking tokens from the left and yielding Parts until there's no more tokens.
        while (patternLeft.isNotEmpty()) {
            if (patternLeft.first().isUnescapedQuote) {
                yield(ConstantPart(takeQuotedConstant(patternLeft)))
            } else if (patternLeft.first().isConstant) {
                yield(ConstantPart(takeNonLetterConstant(patternLeft)))
            } else {
                // Not constant (dynamic pattern).
                yield(DynamicPart(takeDynamic(patternLeft)))
            }
        }
    }

    /**
     * Returns everything until the closing quote, and removes it (including the closing quote) from
     * [patternLeft].
     *
     * Assumes the constructor checks that the amount of quotes are even, and all are closed.
     */
    private fun takeQuotedConstant(patternLeft: ArrayDeque<Token>): String {
        patternLeft.removeFirst()
        val result = patternLeft.takeWhile { !it.isUnescapedQuote }.asString()
        patternLeft.removeFirst(result.length + 1)
        return result
    }

    /** Returns all upcoming non-letter constants, and removes it from [patternLeft]. */
    private fun takeNonLetterConstant(patternLeft: ArrayDeque<Token>): String {
        val result = patternLeft.takeWhile { it.isConstant }.asString()
        patternLeft.removeFirst(result.length)
        return result
    }

    /**
     * Returns the next dynamic section in the pattern, which is basically the repetition of the
     * first character.
     */
    private fun takeDynamic(patternLeft: ArrayDeque<Token>): String =
        patternLeft
            // Taking repetitions to determine padding length.
            .takeWhile { it == patternLeft.first() }
            .asString()
            .also { patternLeft.removeFirst(it.length) }

    /** Merges repeated constants to reduce the amount of concatenation nodes. */
    private fun Sequence<Part>.mergeConstants(): Sequence<Part> = sequence {
        val empty = ConstantPart("") // Saving an allocation every time we reset lastConstant.
        var lastConstant = empty
        forEach { nextSection ->
            if (nextSection is ConstantPart) {
                lastConstant += nextSection
            } else {
                lastConstant.ifNotEmpty { yield(it) }
                lastConstant = empty
                yield(nextSection)
            }
        }
        lastConstant.ifNotEmpty { yield(it) }
    }

    /** Either a [ConstantPart] or [DynamicPart] part of the pattern. */
    private sealed interface Part {
        fun format(instant: DynamicInstant): DynamicString
    }

    /** A pattern section that is built with [DynamicString.constant]. */
    private data class ConstantPart(val value: String) : Part {
        @RequiresSchemaVersion(major = 1, minor = 200)
        override fun format(instant: DynamicInstant): DynamicString = DynamicString.constant(value)

        operator fun plus(other: ConstantPart) = ConstantPart(value + other.value)

        /** Invokes [block] with `this` if `value != ""`. */
        inline fun ifNotEmpty(block: (ConstantPart) -> Unit) {
            if (value.isNotEmpty()) block(this)
        }
    }

    /** A pattern section that is formatted into a [DynamicString] based on [~]. */
    private inner class DynamicPart(code: Char, val length: Int) : Part {
        private val dynamicBuilder: (DynamicInstant) -> DynamicString

        init {
            dynamicBuilder =
                when (code) {
                    'H' -> this::buildHourInDay0To23
                    'k' -> this::buildHourInDay1To24
                    'K' -> this::buildHourInAmPm0To11
                    'h' -> this::buildHourInAmPm1To12
                    'm' -> this::buildMinuteInHour
                    'a' -> this::buildAmPmMarker
                    else -> throw IllegalArgumentException("Illegal pattern character '$code'")
                }
        }

        constructor(value: String) : this(value[0], value.length)

        override fun format(instant: DynamicInstant): DynamicString = dynamicBuilder(instant)

        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildHourInDay0To23(instant: DynamicInstant): DynamicString =
            instant.getHour(timeZone).format(intFormatter)

        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildHourInDay1To24(instant: DynamicInstant): DynamicString {
            val hour = instant.getHour(timeZone)
            return DynamicInt32.onCondition(hour.eq(0)).use(24).elseUse(hour).format(intFormatter)
        }

        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildHourInAmPm0To11(instant: DynamicInstant): DynamicString =
            instant.getHour(timeZone).rem(12).format(intFormatter)

        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildHourInAmPm1To12(instant: DynamicInstant): DynamicString {
            val hourRem12: DynamicInt32 = instant.getHour(timeZone).rem(12)
            return DynamicInt32.onCondition(hourRem12.eq(0))
                .use(12)
                .elseUse(hourRem12)
                .format(intFormatter)
        }

        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildMinuteInHour(instant: DynamicInstant): DynamicString =
            instant.getMinute(timeZone).format(intFormatter)

        // NOTE: This dynamic part ignores length.
        @RequiresSchemaVersion(major = 1, minor = 300)
        private fun buildAmPmMarker(instant: DynamicInstant): DynamicString {
            // Using SimpleDateFormat to determine what AM/PM formats to in the given locale.
            val simpleDateFormat =
                SimpleDateFormat("a", _locale).also { it.timeZone = TimeZone.getTimeZone("UTC") }
            // Epoch is AM in UTC.
            val am = simpleDateFormat.format(Date.from(Instant.EPOCH))
            // Epoch + 12h is PM in UTC.
            val pm = simpleDateFormat.format(Date.from(Instant.EPOCH.plus(Duration.ofHours(12))))

            return DynamicString.onCondition(instant.getHour(timeZone).lt(12)).use(am).elseUse(pm)
        }

        /** Returns a formatter based on the desired [length]. */
        private val intFormatter
            @RequiresSchemaVersion(major = 1, minor = 200)
            get() = DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(length).build()
    }

    /**
     * Tokenizes the characters of the string, by replacing every double quotes (`''`) with
     * [LiteralQuoteToken] and everything else with [CharToken].
     */
    private fun String.tokenize(): ArrayDeque<Token> =
        split("''")
            .asSequence()
            .flatMap { it.map(::CharToken) + LiteralQuoteToken }
            .toCollection(ArrayDeque())
            .also { it.removeLast() }

    /** Either a "normal" character or an escaped quote (`'`). */
    private sealed interface Token {
        /** The value of the token, which should be used based on [isUnescapedQuote]. */
        val value: Char

        /** Whether token is an unescaped quote (a single `'`). */
        val isUnescapedQuote: Boolean

        /** Whether the token is a constant (vs dynamic). */
        val isConstant: Boolean
    }

    /** A non-literal token that can be a quote (`'`), a constant ([^A-Za-z]), or a pattern. */
    private data class CharToken(override val value: Char) : Token {
        override val isUnescapedQuote: Boolean = value == '\''
        override val isConstant: Boolean =
            !isUnescapedQuote && value !in 'a'..'z' && value !in 'A'..'Z'
    }

    /** An escaped quote (`''` that is formatted as a single `'`). */
    private object LiteralQuoteToken : Token {
        override val value = '\''
        override val isUnescapedQuote = false // It's an escaped quote.
        override val isConstant = true
    }

    private fun List<Token>.asString() = map { it.value }.joinToString("")
}

/** In-place equivalent of [ArrayDeque.drop], that accepts a count. */
private fun <T : Any> ArrayDeque<T>.removeFirst(n: Int) {
    repeat(n) { removeFirst() }
}
