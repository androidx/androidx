/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("RemoteFormattingAndroidKt")

package androidx.compose.remote.creation.compose.state

import android.icu.text.DecimalFormat as IcuDecimalFormat
import android.icu.text.DecimalFormatSymbols as IcuDecimalFormatSymbols
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY32
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY4
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_NONE
import androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES
import androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_ROUNDING
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_PERIOD_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_SPACE_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_UNDER_PERIOD
import java.math.BigDecimal
import java.text.DecimalFormat
import kotlin.math.pow

internal val DefaultDecimalFormat = IcuDecimalFormat()
internal val DefaultIntegerFormat = IcuDecimalFormat().apply { maximumFractionDigits = 0 }

/**
 * Formatting options for a `TextFromFloat` operation. [before] and [after] are shorts because that
 * is how the operation encodes them on the wire.
 */
internal data class TextFromFloatOptions(val before: Short, val after: Short, val flags: Int)

internal fun IcuDecimalFormat.toTextFromFloatOptions(): TextFromFloatOptions {
    val decimalSeparator = decimalFormatSymbols.decimalSeparator
    val groupingSeparator = decimalFormatSymbols.groupingSeparator

    val grouping =
        if (!isGroupingUsed) {
            GROUPING_NONE
        } else if (groupingSize == 3) {
            if (secondaryGroupingSize == 2) {
                GROUPING_BY32
            } else {
                GROUPING_BY3
            }
        } else if (groupingSize == 4) {
            GROUPING_BY4
        } else {
            GROUPING_NONE
        }

    val separator =
        if (groupingSeparator == ',' && decimalSeparator == '.') {
            SEPARATOR_COMMA_PERIOD
        } else if (groupingSeparator == '.' && decimalSeparator == ',') {
            SEPARATOR_PERIOD_COMMA
        } else if (groupingSeparator == ' ' && decimalSeparator == ',') {
            SEPARATOR_SPACE_COMMA
        } else if (groupingSeparator == '_' && decimalSeparator == '.') {
            SEPARATOR_UNDER_PERIOD
        } else {
            // default
            SEPARATOR_COMMA_PERIOD
        }

    val before = maximumIntegerDigits.coerceIn(0, 255).toShort()
    val after = maximumFractionDigits.coerceIn(0, 255).toShort()
    var options = 0
    if (negativePrefix == "(") {
        options = options or OPTIONS_NEGATIVE_PARENTHESES
    }

    // icu rounding mode
    @Suppress("DEPRECATION")
    if (roundingMode != BigDecimal.ROUND_UNNECESSARY) {
        options = options or OPTIONS_ROUNDING
    }

    var flags = separator or grouping or options

    if (minimumFractionDigits > 1) {
        flags = flags or PAD_AFTER_ZERO
    } else {
        flags = flags or PAD_AFTER_NONE
    }

    val padPre = minimumIntegerDigits > 1 || formatWidth > 0
    if (padPre) {
        if (formatWidth > 0 && padCharacter == ' ') {
            flags = flags or PAD_PRE_SPACE
        } else {
            flags = flags or PAD_PRE_ZERO
        }
    } else {
        flags = flags or PAD_PRE_NONE
    }

    return TextFromFloatOptions(before, after, flags)
}

internal fun formatRemoteFloat(value: RemoteFloat, format: IcuDecimalFormat): RemoteString {
    val (before, after, flags) = format.toTextFromFloatOptions()

    // Workaround for the player always keeping at least one trailing zero (e.g.
    // formatting 5.0 as "5.0" instead of "5") even when minimumFractionDigits is 0.
    // We only apply this if the format explicitly allows fractional digits (after > 0)
    // but doesn't require them (minimumFractionDigits == 0).
    if (format.minimumFractionDigits == 0 && after > 0 && format !== DefaultDecimalFormat) {
        value.constantValueOrNull?.let { constVal ->
            val resolvedAfter = if (constVal % 1f == 0f) 0.toShort() else after
            return value.toRemoteStringWithPadding(
                before,
                resolvedAfter,
                flags,
                format.minimumIntegerDigits,
                format.formatWidth,
                format.padCharacter,
            )
        }

        // Dynamic path: dynamically check if the value is close to an integer at playback time.
        // This is a workaround for float precision errors (e.g. 9.000001f should format as
        // "9").
        //
        // We calculate a tolerance (epsilon) based on the requested maximum fraction digits.
        // The epsilon is capped at a minimum of 1e-5 to ensure that 1 ULP float precision
        // errors are caught even when high precision (e.g. 6 decimal places) is requested.
        val epsilonVal = (0.5f * 10f.pow(-after)).coerceAtLeast(0.00001f)
        val epsilon = epsilonVal.rf
        val isInteger = abs(value - round(value)).isLessThan(epsilon)
        return isInteger.select(
            // If it is close to an integer, we round it first to the nearest integer
            // (to handle values close to the ceiling like 9.999999f correctly without
            // truncation) and then delegate to RemoteInt.toRemoteString() for clean formatting.
            round(value).toRemoteInt().toRemoteString(format),
            value.toRemoteStringWithPadding(
                before,
                after,
                flags,
                format.minimumIntegerDigits,
                format.formatWidth,
                format.padCharacter,
            ),
        )
    }

    // Fallback path (no workaround needed)
    return value.toRemoteStringWithPadding(
        before,
        after,
        flags,
        format.minimumIntegerDigits,
        format.formatWidth,
        format.padCharacter,
    )
}

internal fun formatRemoteFloat(value: RemoteFloat, format: DecimalFormat): RemoteString {
    val icuFormat = IcuDecimalFormat(format.toPattern())
    icuFormat.decimalFormatSymbols =
        IcuDecimalFormatSymbols.getInstance().apply {
            decimalSeparator = format.decimalFormatSymbols.decimalSeparator
            groupingSeparator = format.decimalFormatSymbols.groupingSeparator
        }
    icuFormat.minimumIntegerDigits = format.minimumIntegerDigits
    icuFormat.maximumIntegerDigits = format.maximumIntegerDigits
    icuFormat.minimumFractionDigits = format.minimumFractionDigits
    icuFormat.maximumFractionDigits = format.maximumFractionDigits
    icuFormat.groupingSize = format.groupingSize
    icuFormat.negativePrefix = format.negativePrefix

    return formatRemoteFloat(value, icuFormat)
}

internal fun formatRemoteInt(value: RemoteInt, format: IcuDecimalFormat): RemoteString {
    val (before, after, flags) = format.toTextFromFloatOptions()
    // If the format doesn't require fractional digits, we force it to 0.
    // This is typical for integer formatting.
    val resolvedAfter = if (format.minimumFractionDigits == 0) 0.toShort() else after

    // Optimization: We call toRemoteStringWithPadding directly on the float
    // representation rather than calling toRemoteFloat().toRemoteString(format).
    // This bypasses the dynamic isInteger check in RemoteFloat.toRemoteString, which is
    // redundant for RemoteInt and would cause infinite recursion (StackOverflowError)
    // since RemoteFloat's workaround delegates back to RemoteInt.toRemoteString.
    return value
        .toRemoteFloat()
        .toRemoteStringWithPadding(
            before,
            resolvedAfter,
            flags,
            format.minimumIntegerDigits,
            format.formatWidth,
            format.padCharacter,
        )
}

internal fun formatRemoteInt(value: RemoteInt, format: DecimalFormat): RemoteString {
    // We manually convert java.text.DecimalFormat to android.icu.text.DecimalFormat
    // and delegate to the ICU overload. This ensures we route through the optimized
    // direct-formatting path (bypassing RemoteFloat.toRemoteString) to avoid the
    // stack overflow hazard and redundant runtime checks.
    val icuFormat = IcuDecimalFormat(format.toPattern())
    icuFormat.decimalFormatSymbols =
        IcuDecimalFormatSymbols.getInstance().apply {
            decimalSeparator = format.decimalFormatSymbols.decimalSeparator
            groupingSeparator = format.decimalFormatSymbols.groupingSeparator
        }
    icuFormat.minimumIntegerDigits = format.minimumIntegerDigits
    icuFormat.maximumIntegerDigits = format.maximumIntegerDigits
    icuFormat.minimumFractionDigits = format.minimumFractionDigits
    icuFormat.maximumFractionDigits = format.maximumFractionDigits
    icuFormat.groupingSize = format.groupingSize
    icuFormat.negativePrefix = format.negativePrefix
    return formatRemoteInt(value, icuFormat)
}

/**
 * Returns a [RemoteString] that evaluates to the result of this [RemoteFloat] formatted according
 * to the provided [android.icu.text.DecimalFormat].
 *
 * This method maps the localized [android.icu.text.DecimalFormat] symbols (such as separators,
 * padding, rounding, and digit constraints) to a remote-compatible string representation. It
 * specifically handles complex padding logic and threshold-based selections to ensure the formatted
 * output remains consistent when evaluated on the remote target.
 *
 * @param format The ICU [android.icu.text.DecimalFormat] to use for determining formatting options
 *   like separators, grouping, and padding width.
 * @return A [RemoteString] representing the formatted float.
 */
public fun RemoteFloat.toRemoteString(format: IcuDecimalFormat): RemoteString =
    formatRemoteFloat(this, format)

/**
 * Returns a [RemoteString] that evaluates to the result of this [RemoteFloat] formatted according
 * to the provided [DecimalFormat].
 *
 * This method maps the localized [DecimalFormat] symbols (such as separators and grouping sizes)
 * and configuration (such as padding and rounding) to a remote-compatible string representation.
 *
 * @param format The [DecimalFormat] to use for determining separators, grouping, and padding.
 * @return A [RemoteString] representing the formatted float.
 */
public fun RemoteFloat.toRemoteString(@Suppress("UseIcu") format: DecimalFormat): RemoteString =
    formatRemoteFloat(this, format)

/**
 * Converts this [RemoteInt] to a [RemoteString] using the specified
 * [android.icu.text.DecimalFormat].
 *
 * This method maps the localized [android.icu.text.DecimalFormat] symbols (such as separators,
 * padding, and digit constraints) to a remote-compatible string representation.
 *
 * @param format The [android.icu.text.DecimalFormat] used to format the integer value.
 * @return A [RemoteString] representing the formatted integer value.
 */
public fun RemoteInt.toRemoteString(format: IcuDecimalFormat): RemoteString =
    formatRemoteInt(this, format)

/**
 * Converts this [RemoteInt] to a [RemoteString] using the specified [DecimalFormat].
 *
 * This method maps the localized [DecimalFormat] symbols (such as separators and grouping sizes)
 * and configuration (such as padding and rounding) to a remote-compatible string representation.
 *
 * @param format The [DecimalFormat] to use for determining separators, grouping, and padding.
 * @return A [RemoteString] representing the formatted integer.
 */
public fun RemoteInt.toRemoteString(@Suppress("UseIcu") format: DecimalFormat): RemoteString =
    formatRemoteInt(this, format)
