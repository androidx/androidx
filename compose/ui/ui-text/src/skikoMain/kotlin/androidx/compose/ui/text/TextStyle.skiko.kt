/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("DesktopTextStyle_skikoKt")

package androidx.compose.ui.text

import kotlin.jvm.JvmName

/**
 * Provides configuration options for behavior compatibility for TextStyle.
 */
actual class PlatformTextStyle {
    actual val spanStyle: PlatformSpanStyle?
    actual val paragraphStyle: PlatformParagraphStyle?

    constructor(
        spanStyle: PlatformSpanStyle?,
        paragraphStyle: PlatformParagraphStyle?
    ) {
        this.spanStyle = spanStyle
        this.paragraphStyle = paragraphStyle
    }

    /**
     * Allows specifying the style of the decoration line for the text.
     *
     * This parameter is relevant only if `textDecoration` is specified, for example in
     * `TextStyle(textDecoration = )` or in `SpanStyle(textDecoration = )`
     */
    @ExperimentalTextApi
    constructor(
        textDecorationLineStyle: TextDecorationLineStyle?
    ) : this (
        spanStyle = PlatformSpanStyle(textDecorationLineStyle),
        paragraphStyle = null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformTextStyle) return false
        if (paragraphStyle != other.paragraphStyle) return false
        if (spanStyle != other.spanStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

internal actual fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle {
    return PlatformTextStyle(spanStyle, paragraphStyle)
}

/**
 * Provides configuration options for behavior compatibility for SpanStyle.
 */
actual class PlatformParagraphStyle {
    actual companion object {
        actual val Default: PlatformParagraphStyle = PlatformParagraphStyle()
    }

    actual fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformParagraphStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * Provides configuration options for behavior compatibility for SpanStyle.
 *
 * @param textDecorationLineStyle The style of the text decoration line. Note that this parameter is
 * relevant only if `textDecoration` is specified, for example in `TextStyle(textDecoration = )` or
 * in `SpanStyle(textDecoration = )`.
 */
actual class PlatformSpanStyle @ExperimentalTextApi constructor(
    val textDecorationLineStyle: TextDecorationLineStyle?
) {

    constructor() : this(textDecorationLineStyle = null)


    actual companion object {
        actual val Default: PlatformSpanStyle = PlatformSpanStyle()
    }

    actual fun merge(other: PlatformSpanStyle?): PlatformSpanStyle {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformSpanStyle) return false
        if (textDecorationLineStyle != other.textDecorationLineStyle) return false
        return true
    }

    override fun hashCode(): Int {
        return textDecorationLineStyle.hashCode()
    }
}

/**
 * Interpolate between two PlatformParagraphStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
actual fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle {
    return start
}

/**
 * Interpolate between two PlatformSpanStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
actual fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle {
    if (start.textDecorationLineStyle == stop.textDecorationLineStyle) return start

    return PlatformSpanStyle(
        textDecorationLineStyle = lerpDiscrete(
            start.textDecorationLineStyle,
            stop.textDecorationLineStyle,
            fraction
        )
    )
}