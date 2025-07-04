/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical

import android.icu.lang.UCharacter
import android.icu.lang.UCharacter.VerticalOrientation
import android.icu.lang.UProperty
import android.text.Spanned
import androidx.annotation.IntDef

/**
 * Represents the orientation of text within a vertical writing mode.
 *
 * This controls how text characters are displayed when using a vertical writing mode. For more
 * information, refer to:
 * [CSS text-orientation](https://www.w3.org/TR/css-writing-modes-3/#text-orientation)
 */
public object TextOrientation {
    /**
     * Characters from horizontal scripts are rotated 90 degrees clockwise, while characters from
     * vertical scripts remain in their original orientation.
     *
     * This is useful for mixed-script content where you want horizontal text to fit vertically
     * while vertical text remains upright.
     *
     * Corresponds to CSS `text-orientation: mixed;`.
     */
    public const val MIXED: Int = 0

    /**
     * A value for the text orientation that represents the text will be laid out with the original
     * orientations.
     */
    /**
     * All characters are laid out in upright orientation, regardless of script.
     *
     * This is useful when you want all characters to remain upright even for horizontal scripts.
     *
     * Corresponds to CSS `text-orientation: upright;`.
     */
    public const val UPRIGHT: Int = 1

    /**
     * All characters are rotated 90 degrees clockwise, regardless of script.
     *
     * This is useful when you want all text to be sideways in a vertical layout.
     *
     * Corresponds to CSS `text-orientation: sideways;`.
     */
    public const val SIDEWAYS: Int = 2
}

@IntDef(value = [TextOrientation.MIXED, TextOrientation.UPRIGHT, TextOrientation.SIDEWAYS])
internal annotation class OrientationMode

/**
 * A sealed interface representing text orientation spans for use within a vertical text layout.
 *
 * These spans allow for overriding the default text orientation of portions of text, such as
 * setting it to upright, sideways, or combining text horizontally within a vertical flow
 * (tate-chu-yoko).
 *
 * These spans are intended for use with [VerticalTextLayout].
 */
public sealed interface TextOrientationSpan {
    /**
     * A span that forces the enclosed text to be displayed in an upright orientation
     * ([TextOrientation.UPRIGHT]) within a vertical text layout.
     *
     * This is useful for ensuring that text remains vertical, even if the surrounding text flow is
     * sideways.
     *
     * @see TextOrientation.UPRIGHT
     * @see VerticalTextLayout
     */
    public class Upright : TextOrientationSpan

    /**
     * A span that forces the enclosed text to be displayed in a sideways orientation
     * ([TextOrientation.SIDEWAYS]) within a vertical text layout.
     *
     * This is useful for orienting text horizontally when the surrounding text is vertical.
     *
     * @see TextOrientation.SIDEWAYS
     * @see VerticalTextLayout
     */
    public class Sideways : TextOrientationSpan

    /**
     * A span that combines a small sequence of characters (typically 2-4 digits) into a single
     * horizontal block within a vertical text flow.
     *
     * This is known as "tate-chu-yoko" in Japanese typography.
     *
     * @see VerticalTextLayout
     */
    public class TextCombineUpright() : TextOrientationSpan
}

/** Represents the resolved orientation of a run of text. */
internal enum class ResolvedOrientation {
    Upright,
    Rotate,
    TateChuYoko,
}

/**
 * `RunMerger` is a utility class designed to identify and merge consecutive runs of characters with
 * the same orientation.
 */
private class RunMerger(val end: Int, val consumer: (Int, Int, ResolvedOrientation) -> Unit) {
    private var prevStart = -1
    private var prevOrientation = ResolvedOrientation.Upright

    /**
     * Appends a new segment to the current run.
     *
     * @param start The inclusive starting position of the segment to be appended.
     * @param orientation The orientation of the current segment.
     */
    fun append(start: Int, orientation: ResolvedOrientation) {
        if (prevStart == -1) {
            prevStart = start
            prevOrientation = orientation
            return
        } else if (
            prevOrientation != orientation || orientation == ResolvedOrientation.TateChuYoko
        ) {
            // orientation transition point. callback and update the state
            consumer(prevStart, start, prevOrientation)
            prevStart = start
            prevOrientation = orientation
        } else {
            // do nothing. keep extending the current run.
        }
    }

    /** Finalize the merge and callback the last run. Do not call this method multiple times. */
    fun finish() {
        if (prevStart != -1) {
            consumer(prevStart, end, prevOrientation)
        }
    }
}

/**
 * Iterates over characters and resolves the each character's orientation property along with the
 * attached `OrientationSpan`.
 *
 * @param text The CharSequence
 * @param start The inclusive starting index
 * @param end The exclusive ending index
 * @param textOrientation The text orientation mode.
 * @param consumer A callback function that is called for each orientation transition. It receives
 *   three parameters:
 *     - The inclusive start index of the orientation run.
 *     - The exclusive end index of the orientation run.
 *     - The orientation of the range.
 */
internal fun forEachOrientation(
    text: CharSequence,
    start: Int,
    end: Int,
    @OrientationMode textOrientation: Int,
    consumer: (Int, Int, ResolvedOrientation) -> Unit,
) {
    if (start >= end) {
        return
    }

    if (text !is Spanned) {
        // Easy case, no spans are attached.
        forOrientationNoSpans(text, start, end, textOrientation, consumer)
        return
    }

    val merger = RunMerger(end, consumer)
    forEachSpan<TextOrientationSpan>(text, start, end) { runStart, runEnd, spans ->
        if (spans.isEmpty()) {
            forOrientationNoSpans(text, runStart, runEnd, textOrientation) { oStart, _, resolved ->
                merger.append(oStart, resolved)
            }
        } else {
            // If multiple TextOrientationSpans are attached, use the last one.
            val resolved =
                when (spans.last()) {
                    is TextOrientationSpan.TextCombineUpright -> ResolvedOrientation.TateChuYoko
                    is TextOrientationSpan.Upright -> ResolvedOrientation.Upright
                    is TextOrientationSpan.Sideways -> ResolvedOrientation.Rotate
                }
            merger.append(runStart, resolved)
        }
    }
    merger.finish()
}

/** Iterates over characters and resolves the each character's orientation property. */
private inline fun forOrientationNoSpans(
    text: CharSequence,
    start: Int,
    end: Int,
    @OrientationMode textOrientation: Int,
    crossinline consumer: (Int, Int, ResolvedOrientation) -> Unit,
) {
    var prevProp = ResolvedOrientation.Upright // unused initial value
    var prevStart = start
    fotEachCodePoints(text, start, end) { i, cp ->
        val prop = resolveOrientation(textOrientation, cp)
        if (i == start) {
            prevProp = prop
        } else if (prevProp != prop) {
            consumer(prevStart, i, prevProp)
            prevProp = prop
            prevStart = i
        }
    }
    consumer(prevStart, end, prevProp)
}

/**
 * Resolves the orientation of a character based on the provided text orientation and the
 * character's code point.
 *
 * @param textOrientation The text orientation mode.
 * @param cp The code point of the character.
 * @return The resolved orientation of the character.
 */
private fun resolveOrientation(@OrientationMode textOrientation: Int, cp: Int) =
    when (textOrientation) {
        TextOrientation.UPRIGHT -> ResolvedOrientation.Upright
        TextOrientation.SIDEWAYS -> ResolvedOrientation.Rotate
        TextOrientation.MIXED -> {
            when (UCharacter.getIntPropertyValue(cp, UProperty.VERTICAL_ORIENTATION)) {
                VerticalOrientation.ROTATED -> ResolvedOrientation.Rotate
                else -> ResolvedOrientation.Upright
            }
        }
        else -> throw RuntimeException("Unknown orientation: $textOrientation")
    }
