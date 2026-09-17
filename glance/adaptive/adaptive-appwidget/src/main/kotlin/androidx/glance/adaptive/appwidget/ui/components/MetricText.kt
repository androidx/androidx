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

package androidx.glance.adaptive.appwidget.ui.components

import androidx.compose.ui.unit.TextUnit

/**
 * Average glyph advance as a fraction of the font size.
 *
 * The headline metric is bold digits and separators, whose advances vary little, so a single ratio
 * estimates the run to within a few percent. Measured against Roboto Medium.
 */
private const val GLYPH_WIDTH_RATIO = 0.58f

/** Group separators used across locales, any of which may appear inside a formatted integer. */
private const val GROUP_SEPARATORS = ",. \u00A0\u202F\u2009'"

/** A plain integer, optionally split into groups of three. */
private val GroupedInteger = Regex("""\d+|\d{1,3}([$GROUP_SEPARATORS]\d{3})+""")

/**
 * Estimated width of [text] rendered at [fontSizeSp].
 *
 * Glance cannot measure text — the layout is `RemoteViews`, measured by the host long after the
 * composition is gone — so anything that has to reason about whether a string fits has to estimate
 * it. Returns DP, which equals SP at the default font scale; at larger scales the estimate is
 * conservative in the wrong direction, and the text falls back to ellipsizing as it would anyway.
 */
internal fun estimateTextWidthDp(text: String, fontSizeSp: Float): Float =
    text.length * fontSizeSp * GLYPH_WIDTH_RATIO

/**
 * Shortens a headline metric until it fits [maxWidthDp], the way the design's narrow breakpoints
 * do.
 *
 * The design draws `10K` at W1 and the full figure at W4: the same metric, abbreviated to the room
 * available. Since only the caller's payload knows the real value, that abbreviation happens here
 * rather than being asked of the app.
 *
 * Candidates are tried longest first — grouped, ungrouped, one decimal place, whole units — so the
 * value keeps as much precision as it can. Truncation rather than rounding, so the widget never
 * claims progress that has not happened. A value that is not a plain integer is returned untouched
 * and left to ellipsize.
 *
 * @param value The metric as the app formatted it, e.g. `11,056`.
 * @param maxWidthDp Room available for the metric.
 * @param fontSize Type size the metric is drawn at.
 * @return The longest candidate that fits, or the shortest one if none does.
 */
internal fun fitMetric(value: String, maxWidthDp: Float, fontSize: TextUnit): String {
    val fontSizeSp = fontSize.value
    if (estimateTextWidthDp(value, fontSizeSp) <= maxWidthDp) return value

    val candidates = abbreviations(value) ?: return value
    for (i in candidates.indices) {
        val candidate = candidates[i]
        if (estimateTextWidthDp(candidate, fontSizeSp) <= maxWidthDp) return candidate
    }
    return candidates[candidates.size - 1]
}

/**
 * Progressively shorter renderings of [value], or `null` if it is not a plain integer.
 *
 * Ordered longest first, and never empty when non-null.
 */
private fun abbreviations(value: String): List<String>? {
    val trimmed = value.trim()
    if (!GroupedInteger.matches(trimmed)) return null
    val number = trimmed.filter { it.isDigit() }.toLongOrNull() ?: return null

    return buildList {
        // Dropping the group separators is free precision: same digits, narrower run.
        add(number.toString())

        val (unit, divisor) =
            when {
                number >= 1_000_000_000L -> "B" to 1_000_000_000L
                number >= 1_000_000L -> "M" to 1_000_000L
                number >= 1_000L -> "K" to 1_000L
                else -> return@buildList
            }

        val whole = number / divisor
        val tenths = (number % divisor) * 10 / divisor
        if (tenths > 0L) add("$whole.$tenths$unit")
        add("$whole$unit")
    }
}
