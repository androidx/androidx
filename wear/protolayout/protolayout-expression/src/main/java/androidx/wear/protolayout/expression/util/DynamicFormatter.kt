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

import android.graphics.Color
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat.FloatFormatter
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import java.time.Duration
import java.time.Instant
import java.util.Formatter
import java.util.Locale
import java.util.MissingFormatArgumentException

/**
 * Equivalent to [Formatter], but supports [DynamicType]s and generates a [DynamicString].
 *
 * See [Formatter] documentation for the format string syntax.
 *
 * Example usage:
 * ```
 * DynamicFormatter().format(
 *   "%s has walked %d steps. %1$s has also walked %.2f meters.",
 *   "John", PlatformHealthSources.dailySteps(), PlatformHealthSources.dailyDistanceMeters()
 * )
 * // Generates an equivalent of:
 * DynamicString.constant("John has walked ")
 *   .concat(PlatformHealthSources.dailySteps().format())
 *   .concat(DynamicString.constant(" steps. John has also walked "))
 *   .concat(
 *     PlatformHealthSources.dailyMeters()
 *       .format(FloatFormatter.Builder().setMaxFractionDigits(2).build())
 *   )
 *   .concat(DynamicString.constant(" meters."))
 * ```
 *
 * Argument index options (`%s`, `%2$s`, and `%<s`) are fully supported.
 *
 * These are the supported conversions and options:
 *
 * |   |Non-[DynamicType]|[DynamicBool]  |[DynamicFloat]          |[DynamicInt32]          |[DynamicString]|Other [DynamicType]|
 * |---|-----------------|---------------|------------------------|------------------------|---------------|-------------------|
 * |%% |See [Formatter]  |Yes            |Yes                     |Yes                     |Yes            |Yes                |
 * |%n |See [Formatter]  |Yes            |Yes                     |Yes                     |Yes            |Yes                |
 * |%s |See [Formatter]  |Yes, no options|Yes, no options         |Yes, no options         |Yes, no options|No                 |
 * |%S |See [Formatter]  |Yes, no options|Yes, no options         |Yes, no options         |No             |No                 |
 * |%b |See [Formatter]  |Yes, no options|Yes, no options         |Yes, no options         |Yes, no options|Yes, no options    |
 * |%B |See [Formatter]  |Yes, no options|Yes, no options         |Yes, no options         |Yes, no options|Yes, no options    |
 * |%d |See [Formatter]  |No             |Yes, width              |Yes, width              |No             |No                 |
 * |%f |See [Formatter]  |No             |Yes, width and precision|Yes, width and precision|No             |No                 |
 * |...|See [Formatter]  |No             |No                      |No                      |No             |No                 |
 *
 * NOTE: `%f` has a default precision of 6..6 in [Formatter], which is different from
 * [DynamicFloat.format] which defaults to 0..3. [DynamicFormatter] behaves like [Formatter] and
 * defaults to 6..6.
 */
public class DynamicFormatter {
    // TODO: b/297323092 - Allow providing locale for remote evaluation in the constructor.
    private val locale
        get() = Locale.getDefault(Locale.Category.FORMAT)

    /**
     * Generates a [DynamicString] with [args] embedded into the [format], based on the syntax rules
     * of [Formatter].
     *
     * Non-[DynamicType] args use [Formatter], whereas [DynamicType]s are implemented here. Not all
     * conversions are supported, either because of limitations of [DynamicType] or because of
     * unimplemented features. Any unsupported feature will throw [UnsupportedOperationException].
     *
     * @throws java.util.IllegalFormatException if the format string contains an illegal syntax
     * @throws MissingFormatArgumentException if the argument index is does not correspond to an
     *   available argument
     * @throws UnsupportedOperationException if the format string is allowed by [Formatter], but not
     *   implemented by [DynamicFormatter]
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public fun format(format: String, vararg args: Any?): DynamicString =
        extractFormatParts(format, args)
            .mergeConstants()
            .map { it.toDynamicString() }
            .ifEmpty { sequenceOf(DynamicString.constant("")) }
            // Concatenate all parts.
            .reduce { acc, formattedSection -> acc.concat(formattedSection) }

    /**
     * Generates a [Sequence] of constants (non-[DynamicType] sections) and dynamic values.
     *
     * Example usage:
     * ```
     * DynamicFormatter().format(
     *   "%s has walked %d steps.",
     *   "John", PlatformHealthSources.dailySteps()
     * )
     * // Generates:
     * sequenceOf(
     *   Constant("John"), Constant(" has walked "),
     *   Dynamic(PlatformHealthSources.dailySteps().format(),
     *   Constant(".")
     * )
     * ```
     *
     * Note this method does not ensure constants are merged, and [mergeConstants] should be called
     * on the result.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    private fun extractFormatParts(
        format: String,
        args: Array<out Any?>
    ): Sequence<ConstantOrDynamicPart> = sequence {
        var lastPosition = 0
        var lastVariableIndex = -1
        var lastPositionalVariableIndex = -1
        for (match in PATTERN.findAll(format)) {
            if (match.range.first > lastPosition) {
                // Non-variable (non-match) from the end of the last dynamic part, i.e.:
                // "...<dynamic><*constant*><dynamic>...".
                yield(ConstantPart(format.substring(lastPosition until match.range.first)))
            }
            // Variable match - parsing the specifier, maintaining last indices, and formatting.
            val formatAttributes =
                match.toFormatStringVariable(
                    lastIndex = lastVariableIndex,
                    lastPositionalIndex = lastPositionalVariableIndex
                )
            lastVariableIndex = formatAttributes.index
            if (formatAttributes.isPositionalIndex) {
                lastPositionalVariableIndex = formatAttributes.index
            }
            yield(formatAttributes.format(args))
            // Remembering position in order to extract non-variable parts between variable matches.
            lastPosition = match.range.last + 1
        }
        if (lastPosition < format.length) {
            // Non-variable (non-match) from the end of the last dynamic part at the end of the
            // format, i.e.: "...<dynamic><*constant*>".
            yield(ConstantPart(format.substring(lastPosition)))
        }
    }

    /**
     * Converts a [PATTERN] match to an [FormatStringVariable].
     *
     * Example usages:
     * ```
     * // Index examples:
     * PATTERN.matchEntire("%s").parse(lastIndex = 4, lastPositionIndex = 2)
     * -> FormatStringVariable(
     *   specifier = "%s", index = 3, isPositionalIndex = true, ..., conversion = 's'
     * )
     * PATTERN.matchEntire("%<s").parse(lastIndex = 4, lastPositionIndex = 2)
     * -> FormatStringVariable(
     *   value = "%<s", index = 4, isPositionalIndex = false, ..., conversion = 's'
     * )
     * PATTERN.matchEntire("%3$s").parse(lastIndex = 4, lastPositionIndex = 2)
     * -> FormatStringVariable(
     *   value = "%3$s", index = 2, isPositionalIndex = false, ..., conversion = 's'
     * )
     *
     * // Other attributes:
     * PATTERN.matchEntire("%#+2.3tT").parse(...)
     * -> FormatStringVariable(
     *   value = "%2.3tT", ..., flags = charArrayOf('#', '+'), width = 2, precision = 3,
     *   dateTimePrefix = 't', conversion = 'T'
     * )
     * ```
     */
    private fun MatchResult.toFormatStringVariable(
        lastIndex: Int,
        lastPositionalIndex: Int
    ): FormatStringVariable =
        destructured.let { (indexType, index, flags, width, precision, dateTimePrefix, conversion)
            ->
            FormatStringVariable(
                specifier = value,
                index =
                    when (indexType.lastOrNull()) {
                        null -> lastPositionalIndex + 1
                        '<' ->
                            lastIndex.takeIf { it >= 0 }
                                ?: throw MissingFormatArgumentException("Format specifier '$value'")
                        '$' -> index.toInt() - 1 // 1-based index
                        else -> throw AssertionError("Should not be matched by Regex: $indexType")
                    },
                isPositionalIndex = indexType == "",
                flags = flags.toCharArray(),
                width = width.takeIf { it.isNotEmpty() }?.toInt(),
                precision = precision.takeIf { it.isNotEmpty() }?.toInt(),
                dateTimePrefix = dateTimePrefix.takeIf { it.isNotEmpty() }?.single(),
                conversion = conversion.single(),
            )
        }

    /**
     * Merges repeated constants to reduce the amount of concatenation nodes.
     *
     * Example usage:
     * ```
     * sequenceOf(
     *   Constant("John"), Constant(" has walked "),
     *   Dynamic(PlatformHealthSources.dailySteps().format(),
     *   Constant(".")
     * ).mergeConstants()
     * -> sequenceOf(
     *   Constant("John has walked "),
     *   Dynamic(PlatformHealthSources.dailySteps().format(),
     *   Constant(".")
     * )
     * ```
     */
    private fun Sequence<ConstantOrDynamicPart>.mergeConstants(): Sequence<ConstantOrDynamicPart> =
        sequence {
            val empty = ConstantPart("")
            var lastConstant = empty
            forEach { nextSection ->
                if (nextSection is ConstantPart) {
                    lastConstant += nextSection
                } else {
                    if (lastConstant.value.isNotEmpty()) yield(lastConstant)
                    lastConstant = empty
                    yield(nextSection)
                }
            }
            if (lastConstant.value.isNotEmpty()) yield(lastConstant)
        }

    /** Represents a [ConstantPart] or a [DynamicPart] value. */
    private sealed interface ConstantOrDynamicPart {
        @RequiresSchemaVersion(major = 1, minor = 200) fun toDynamicString(): DynamicString
    }

    private data class ConstantPart(val value: String) : ConstantOrDynamicPart {
        operator fun plus(other: ConstantPart) = ConstantPart(value + other.value)

        @RequiresSchemaVersion(major = 1, minor = 200)
        override fun toDynamicString() = DynamicString.constant(value)
    }

    private data class DynamicPart(val value: DynamicString) : ConstantOrDynamicPart {
        @RequiresSchemaVersion(major = 1, minor = 200) override fun toDynamicString() = value
    }

    /**
     * Represents a variable of a format string based on [Formatter] format string syntax.
     *
     * @see Formatter for what each option means.
     */
    private inner class FormatStringVariable(
        /** The entire specifier as-written by the caller to [format], e.g. `"%3$.2f"`. */
        val specifier: String,
        /** The index of the associated argument, e.g. `"%2$s" => 2`, `"%s %s" => [1, 2]`. */
        val index: Int,
        /**
         * Whether [index] is positional, e.g. `"%s"` and not `%2$s`. This affects parsing of
         * followup [FormatStringVariable] that have positional indices.
         */
        val isPositionalIndex: Boolean,
        /** The format flags option, e.g. `"%+s" => ['+']`. */
        val flags: CharArray,
        /** The format width option, e.g. `"%2.3f" => 2`. */
        val width: Int?,
        /** The format precision option, e.g. `"%2.3f" => 3`. */
        val precision: Int?,
        /** The format date-time prefix option, e.g. `"%tb" => 't'`. */
        val dateTimePrefix: Char?,
        /** The format conversion, e.g. `"%2.3d" => 'd'`. */
        val conversion: Char,
    ) {
        private val specifierWithoutIndex =
            "%" +
                flags.joinToString() +
                (width ?: "") +
                (precision?.let { ".$it" } ?: "") +
                (dateTimePrefix ?: "") +
                conversion

        /**
         * Generates a formatted part from this variable, given the args provided to
         * [DynamicFormatter.format].
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        fun format(args: Array<out Any?>): ConstantOrDynamicPart {
            if (index >= args.size) {
                throw MissingFormatArgumentException("Format specifier '$specifier'")
            }
            val arg = args[index]
            // Non-DynamicType arguments use Formatter.
            if (arg !is DynamicType) return ConstantPart(arg.defaultFormat())
            throwIfIfNotAllowed(arg)
            return when (conversion) {
                '%' -> ConstantPart(arg.defaultFormat()) // Argument is ignored by %%.
                'n' -> ConstantPart(arg.defaultFormat()) // Argument is ignored by %n.
                's' -> asStringPart(arg)
                'S' -> asStringUpperPart(arg)
                'b' -> asBooleanPart(arg)
                'B' -> asBooleanUpperPart(arg)
                'd' -> asDecimalPart(arg)
                'f' -> asFloatPart(arg)
                else ->
                    throw UnsupportedOperationException(
                        "Unsupported conversion for DynamicType: '$conversion'"
                    )
            }
        }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asStringPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicString -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(arg)
                }
                is DynamicInt32 -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(arg.format())
                }
                is DynamicFloat -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(
                        arg.format(FloatFormatter.Builder().setMinFractionDigits(1).build())
                    )
                }
                is DynamicBool -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(
                        DynamicString.onCondition(arg)
                            .use(true.defaultFormat())
                            .elseUse(false.defaultFormat())
                    )
                }
                else -> throwUnsupportedDynamicType(arg)
            }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asStringUpperPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicInt32 -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(arg.format())
                }
                is DynamicFloat -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(
                        arg.format(FloatFormatter.Builder().setMinFractionDigits(1).build())
                    )
                }
                is DynamicBool -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(
                        DynamicString.onCondition(arg)
                            .use(true.defaultFormat())
                            .elseUse(false.defaultFormat())
                    )
                }
                else -> throwUnsupportedDynamicType(arg)
            }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asBooleanPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicBool -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(
                        DynamicString.onCondition(arg)
                            .use(true.defaultFormat())
                            .elseUse(false.defaultFormat())
                    )
                }
                // All non-null is true, including DynamicType.
                else -> ConstantPart(arg.defaultFormat())
            }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asBooleanUpperPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicBool -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(DynamicString.onCondition(arg).use("TRUE").elseUse("FALSE"))
                }
                // All non-null is true, including DynamicType.
                else -> ConstantPart(arg.defaultFormat())
            }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asDecimalPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicInt32 -> {
                    throwUnsupportedForAnyOption()
                    DynamicPart(arg.format())
                }
                else -> throwUnsupportedDynamicType(arg)
            }

        @RequiresSchemaVersion(major = 1, minor = 200)
        private fun asFloatPart(arg: DynamicType): ConstantOrDynamicPart =
            when (arg) {
                is DynamicFloat -> {
                    throwUnsupportedSpecifierIf(
                        flags.isNotEmpty() || width != null || dateTimePrefix != null
                    )
                    DynamicPart(
                        arg.format(
                            FloatFormatter.Builder()
                                .apply {
                                    setMinFractionDigits(precision ?: DEFAULT_FLOAT_FRACTION_DIGITS)
                                    setMaxFractionDigits(precision ?: DEFAULT_FLOAT_FRACTION_DIGITS)
                                }
                                .build()
                        )
                    )
                }
                else -> throwUnsupportedDynamicType(arg)
            }

        /** Throws [UnsupportedOperationException] if the condition is true. */
        private fun throwUnsupportedSpecifierIf(condition: Boolean) {
            if (condition) {
                throw UnsupportedOperationException("Unsupported specifier: '$specifier'")
            }
        }

        /** Throws [UnsupportedOperationException] with the [DynamicType] in the message. */
        private fun throwUnsupportedDynamicType(dynamicType: DynamicType): Nothing {
            throw UnsupportedOperationException(
                "$dynamicType unsupported for specifier: '$specifier'"
            )
        }

        /** Throws [UnsupportedOperationException] if the argument has any options set. */
        private fun throwUnsupportedForAnyOption() {
            throwUnsupportedSpecifierIf(
                flags.isNotEmpty() || width != null || precision != null || dateTimePrefix != null
            )
        }

        /**
         * Verifies [Formatter] doesn't throw for Java types equivalent to the [DynamicType] for
         * this [FormatStringVariable].
         */
        private fun throwIfIfNotAllowed(value: DynamicType) {
            when (value) {
                is DynamicBool -> true.defaultFormat()
                is DynamicColor -> EMPTY_COLOR.defaultFormat()
                is DynamicDuration -> Duration.ZERO.defaultFormat()
                is DynamicFloat -> 0f.defaultFormat()
                is DynamicInstant -> Instant.MIN.defaultFormat()
                is DynamicInt32 -> 0.defaultFormat()
                is DynamicString -> "".defaultFormat()
                // New DynamicType not implemented by DynamicFormatter.
                else -> throwUnsupportedDynamicType(value)
            }
        }

        /** Uses [Formatter] to format this [FormatStringVariable]. */
        private fun Any?.defaultFormat(): String =
            Formatter(locale).format(specifierWithoutIndex, this).toString()
    }

    private companion object {
        /**
         * Extracts index, flags, width, precision, date-time prefix, format from a format string
         * variable (e.g. `%s`).
         *
         * See [Formatter] syntax.
         *
         * The pattern is this, such that all the parts (in `<...>`) are optional:
         * ```
         * %<indexWithType><index><flags><width>.<precision><dateTimePrefix><conversion>
         * ```
         *
         * @see toFormatStringVariable
         */
        private val PATTERN =
            Regex("""%((\d+)\$|<)?([\-#+ 0,(]+)?(\d+)?(?:\.(\d+))?([tT])?([A-Za-z%])""")

        /** Default fraction digits as seen by `"%f".format(1f) == "1.000000"`. */
        private const val DEFAULT_FLOAT_FRACTION_DIGITS = 6

        private val EMPTY_COLOR = Color()
    }
}
