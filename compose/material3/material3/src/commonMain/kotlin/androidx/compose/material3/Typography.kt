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

package androidx.compose.material3

import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.material3.tokens.TypographyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/**
 * The Material Design type scale includes a range of contrasting styles that support the needs of
 * your product and its content.
 *
 * Use typography to make writing legible and beautiful. Material's default type scale includes
 * contrasting and flexible styles to support a wide range of use cases.
 *
 * The type scale is a combination of thirteen styles that are supported by the type system. It
 * contains reusable categories of text, each with an intended application and meaning.
 *
 * The emphasized versions of the baseline styles add dynamism and personality to the baseline
 * styles. It can be used to further stylize select pieces of text. The emphasized states have
 * pragmatic uses, such as creating clearer division of content and drawing users' eyes to relevant
 * material.
 *
 * To learn more about typography, see
 * [Material Design typography](https://m3.material.io/styles/typography/overview).
 *
 * @property displayLarge displayLarge is the largest display text.
 * @property displayMedium displayMedium is the second largest display text.
 * @property displaySmall displaySmall is the smallest display text.
 * @property headlineLarge headlineLarge is the largest headline, reserved for short, important text
 *   or numerals. For headlines, you can choose an expressive font, such as a display, handwritten,
 *   or script style. These unconventional font designs have details and intricacy that help attract
 *   the eye.
 * @property headlineMedium headlineMedium is the second largest headline, reserved for short,
 *   important text or numerals. For headlines, you can choose an expressive font, such as a
 *   display, handwritten, or script style. These unconventional font designs have details and
 *   intricacy that help attract the eye.
 * @property headlineSmall headlineSmall is the smallest headline, reserved for short, important
 *   text or numerals. For headlines, you can choose an expressive font, such as a display,
 *   handwritten, or script style. These unconventional font designs have details and intricacy that
 *   help attract the eye.
 * @property titleLarge titleLarge is the largest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property titleMedium titleMedium is the second largest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property titleSmall titleSmall is the smallest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property bodyLarge bodyLarge is the largest body, and is typically used for long-form writing as
 *   it works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 *   is recommended.
 * @property bodyMedium bodyMedium is the second largest body, and is typically used for long-form
 *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
 *   serif typeface is recommended.
 * @property bodySmall bodySmall is the smallest body, and is typically used for long-form writing
 *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
 *   typeface is recommended.
 * @property labelLarge labelLarge text is a call to action used in different types of buttons (such
 *   as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is
 *   typically sans serif, using all caps text.
 * @property labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
 *   annotate imagery or to introduce a headline.
 * @property labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
 *   annotate imagery or to introduce a headline.
 * @property displayLargeEmphasized an emphasized version of [displayLarge].
 * @property displayMediumEmphasized an emphasized version of [displayMedium].
 * @property displaySmallEmphasized an emphasized version of [displaySmall].
 * @property headlineLargeEmphasized an emphasized version of [headlineLarge].
 * @property headlineMediumEmphasized an emphasized version of [headlineMedium].
 * @property headlineSmallEmphasized an emphasized version of [headlineSmall].
 * @property titleLargeEmphasized an emphasized version of [titleLarge].
 * @property titleMediumEmphasized an emphasized version of [titleMedium].
 * @property titleSmallEmphasized an emphasized version of [titleSmall].
 * @property bodyLargeEmphasized an emphasized version of [bodyLarge].
 * @property bodyMediumEmphasized an emphasized version of [bodyMedium].
 * @property bodySmallEmphasized an emphasized version of [bodySmall].
 * @property labelLargeEmphasized an emphasized version of [labelLarge].
 * @property labelMediumEmphasized an emphasized version of [labelMedium].
 * @property labelSmallEmphasized an emphasized version of [labelSmall].
 */
@Immutable
class Typography
@ExperimentalMaterial3ExpressiveApi
constructor(
    val displayLarge: TextStyle = TypographyTokens.DisplayLarge,
    val displayMedium: TextStyle = TypographyTokens.DisplayMedium,
    val displaySmall: TextStyle = TypographyTokens.DisplaySmall,
    val headlineLarge: TextStyle = TypographyTokens.HeadlineLarge,
    val headlineMedium: TextStyle = TypographyTokens.HeadlineMedium,
    val headlineSmall: TextStyle = TypographyTokens.HeadlineSmall,
    val titleLarge: TextStyle = TypographyTokens.TitleLarge,
    val titleMedium: TextStyle = TypographyTokens.TitleMedium,
    val titleSmall: TextStyle = TypographyTokens.TitleSmall,
    val bodyLarge: TextStyle = TypographyTokens.BodyLarge,
    val bodyMedium: TextStyle = TypographyTokens.BodyMedium,
    val bodySmall: TextStyle = TypographyTokens.BodySmall,
    val labelLarge: TextStyle = TypographyTokens.LabelLarge,
    val labelMedium: TextStyle = TypographyTokens.LabelMedium,
    val labelSmall: TextStyle = TypographyTokens.LabelSmall,
    displayLargeEmphasized: TextStyle = TypographyTokens.DisplayLargeEmphasized,
    displayMediumEmphasized: TextStyle = TypographyTokens.DisplayMediumEmphasized,
    displaySmallEmphasized: TextStyle = TypographyTokens.DisplaySmallEmphasized,
    headlineLargeEmphasized: TextStyle = TypographyTokens.HeadlineLargeEmphasized,
    headlineMediumEmphasized: TextStyle = TypographyTokens.HeadlineMediumEmphasized,
    headlineSmallEmphasized: TextStyle = TypographyTokens.HeadlineSmallEmphasized,
    titleLargeEmphasized: TextStyle = TypographyTokens.TitleLargeEmphasized,
    titleMediumEmphasized: TextStyle = TypographyTokens.TitleMediumEmphasized,
    titleSmallEmphasized: TextStyle = TypographyTokens.TitleSmallEmphasized,
    bodyLargeEmphasized: TextStyle = TypographyTokens.BodyLargeEmphasized,
    bodyMediumEmphasized: TextStyle = TypographyTokens.BodyMediumEmphasized,
    bodySmallEmphasized: TextStyle = TypographyTokens.BodySmallEmphasized,
    labelLargeEmphasized: TextStyle = TypographyTokens.LabelLargeEmphasized,
    labelMediumEmphasized: TextStyle = TypographyTokens.LabelMediumEmphasized,
    labelSmallEmphasized: TextStyle = TypographyTokens.LabelSmallEmphasized,
) {
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displayLarge]. */
    val displayLargeEmphasized = displayLargeEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displayMedium]. */
    val displayMediumEmphasized = displayMediumEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displaySmall]. */
    val displaySmallEmphasized = displaySmallEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineLarge]. */
    val headlineLargeEmphasized = headlineLargeEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineMedium]. */
    val headlineMediumEmphasized = headlineMediumEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineSmall]. */
    val headlineSmallEmphasized = headlineSmallEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleLarge]. */
    val titleLargeEmphasized = titleLargeEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleMedium]. */
    val titleMediumEmphasized = titleMediumEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleSmall]. */
    val titleSmallEmphasized = titleSmallEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodyLarge]. */
    val bodyLargeEmphasized = bodyLargeEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodyMedium]. */
    val bodyMediumEmphasized = bodyMediumEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodySmall]. */
    val bodySmallEmphasized = bodySmallEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelLarge]. */
    val labelLargeEmphasized = labelLargeEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelMedium]. */
    val labelMediumEmphasized = labelMediumEmphasized

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelSmall]. */
    val labelSmallEmphasized = labelSmallEmphasized

    /**
     * The Material Design type scale includes a range of contrasting styles that support the needs
     * of your product and its content.
     *
     * Use typography to make writing legible and beautiful. Material's default type scale includes
     * contrasting and flexible styles to support a wide range of use cases.
     *
     * The type scale is a combination of thirteen styles that are supported by the type system. It
     * contains reusable categories of text, each with an intended application and meaning.
     *
     * To learn more about typography, see
     * [Material Design typography](https://m3.material.io/styles/typography/overview).
     *
     * @param displayLarge displayLarge is the largest display text.
     * @param displayMedium displayMedium is the second largest display text.
     * @param displaySmall displaySmall is the smallest display text.
     * @param headlineLarge headlineLarge is the largest headline, reserved for short, important
     *   text or numerals. For headlines, you can choose an expressive font, such as a display,
     *   handwritten, or script style. These unconventional font designs have details and intricacy
     *   that help attract the eye.
     * @param headlineMedium headlineMedium is the second largest headline, reserved for short,
     *   important text or numerals. For headlines, you can choose an expressive font, such as a
     *   display, handwritten, or script style. These unconventional font designs have details and
     *   intricacy that help attract the eye.
     * @param headlineSmall headlineSmall is the smallest headline, reserved for short, important
     *   text or numerals. For headlines, you can choose an expressive font, such as a display,
     *   handwritten, or script style. These unconventional font designs have details and intricacy
     *   that help attract the eye.
     * @param titleLarge titleLarge is the largest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param titleMedium titleMedium is the second largest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param titleSmall titleSmall is the smallest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param bodyLarge bodyLarge is the largest body, and is typically used for long-form writing
     *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
     *   typeface is recommended.
     * @param bodyMedium bodyMedium is the second largest body, and is typically used for long-form
     *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
     *   serif typeface is recommended.
     * @param bodySmall bodySmall is the smallest body, and is typically used for long-form writing
     *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
     *   typeface is recommended.
     * @param labelLarge labelLarge text is a call to action used in different types of buttons
     *   (such as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text
     *   is typically sans serif, using all caps text.
     * @param labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
     *   annotate imagery or to introduce a headline.
     * @param labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
     *   annotate imagery or to introduce a headline.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    constructor(
        displayLarge: TextStyle = TypographyTokens.DisplayLarge,
        displayMedium: TextStyle = TypographyTokens.DisplayMedium,
        displaySmall: TextStyle = TypographyTokens.DisplaySmall,
        headlineLarge: TextStyle = TypographyTokens.HeadlineLarge,
        headlineMedium: TextStyle = TypographyTokens.HeadlineMedium,
        headlineSmall: TextStyle = TypographyTokens.HeadlineSmall,
        titleLarge: TextStyle = TypographyTokens.TitleLarge,
        titleMedium: TextStyle = TypographyTokens.TitleMedium,
        titleSmall: TextStyle = TypographyTokens.TitleSmall,
        bodyLarge: TextStyle = TypographyTokens.BodyLarge,
        bodyMedium: TextStyle = TypographyTokens.BodyMedium,
        bodySmall: TextStyle = TypographyTokens.BodySmall,
        labelLarge: TextStyle = TypographyTokens.LabelLarge,
        labelMedium: TextStyle = TypographyTokens.LabelMedium,
        labelSmall: TextStyle = TypographyTokens.LabelSmall,
    ) : this(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
        displayLargeEmphasized = displayLarge,
        displayMediumEmphasized = displayMedium,
        displaySmallEmphasized = displaySmall,
        headlineLargeEmphasized = headlineLarge,
        headlineMediumEmphasized = headlineMedium,
        headlineSmallEmphasized = headlineSmall,
        titleLargeEmphasized = titleLarge,
        titleMediumEmphasized = titleMedium,
        titleSmallEmphasized = titleSmall,
        bodyLargeEmphasized = bodyLarge,
        bodyMediumEmphasized = bodyMedium,
        bodySmallEmphasized = bodySmall,
        labelLargeEmphasized = labelLarge,
        labelMediumEmphasized = labelMedium,
        labelSmallEmphasized = labelSmall,
    )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        headlineLarge: TextStyle = this.headlineLarge,
        headlineMedium: TextStyle = this.headlineMedium,
        headlineSmall: TextStyle = this.headlineSmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
        displayLargeEmphasized: TextStyle = this.displayLargeEmphasized,
        displayMediumEmphasized: TextStyle = this.displayMediumEmphasized,
        displaySmallEmphasized: TextStyle = this.displaySmallEmphasized,
        headlineLargeEmphasized: TextStyle = this.headlineLargeEmphasized,
        headlineMediumEmphasized: TextStyle = this.headlineMediumEmphasized,
        headlineSmallEmphasized: TextStyle = this.headlineSmallEmphasized,
        titleLargeEmphasized: TextStyle = this.titleLargeEmphasized,
        titleMediumEmphasized: TextStyle = this.titleMediumEmphasized,
        titleSmallEmphasized: TextStyle = this.titleSmallEmphasized,
        bodyLargeEmphasized: TextStyle = this.bodyLargeEmphasized,
        bodyMediumEmphasized: TextStyle = this.bodyMediumEmphasized,
        bodySmallEmphasized: TextStyle = this.bodySmallEmphasized,
        labelLargeEmphasized: TextStyle = this.labelLargeEmphasized,
        labelMediumEmphasized: TextStyle = this.labelMediumEmphasized,
        labelSmallEmphasized: TextStyle = this.labelSmallEmphasized,
    ): Typography =
        Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            displayLargeEmphasized = displayLargeEmphasized,
            displayMediumEmphasized = displayMediumEmphasized,
            displaySmallEmphasized = displaySmallEmphasized,
            headlineLargeEmphasized = headlineLargeEmphasized,
            headlineMediumEmphasized = headlineMediumEmphasized,
            headlineSmallEmphasized = headlineSmallEmphasized,
            titleLargeEmphasized = titleLargeEmphasized,
            titleMediumEmphasized = titleMediumEmphasized,
            titleSmallEmphasized = titleSmallEmphasized,
            bodyLargeEmphasized = bodyLargeEmphasized,
            bodyMediumEmphasized = bodyMediumEmphasized,
            bodySmallEmphasized = bodySmallEmphasized,
            labelLargeEmphasized = labelLargeEmphasized,
            labelMediumEmphasized = labelMediumEmphasized,
            labelSmallEmphasized = labelSmallEmphasized,
        )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        headlineLarge: TextStyle = this.headlineLarge,
        headlineMedium: TextStyle = this.headlineMedium,
        headlineSmall: TextStyle = this.headlineSmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
    ): Typography =
        copy(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            displayLargeEmphasized = this.displayLargeEmphasized,
            displayMediumEmphasized = this.displayMediumEmphasized,
            displaySmallEmphasized = this.displaySmallEmphasized,
            headlineLargeEmphasized = this.headlineLargeEmphasized,
            headlineMediumEmphasized = this.headlineMediumEmphasized,
            headlineSmallEmphasized = this.headlineSmallEmphasized,
            titleLargeEmphasized = this.titleLargeEmphasized,
            titleMediumEmphasized = this.titleMediumEmphasized,
            titleSmallEmphasized = this.titleSmallEmphasized,
            bodyLargeEmphasized = this.bodyLargeEmphasized,
            bodyMediumEmphasized = this.bodyMediumEmphasized,
            bodySmallEmphasized = this.bodySmallEmphasized,
            labelLargeEmphasized = this.labelLargeEmphasized,
            labelMediumEmphasized = this.labelMediumEmphasized,
            labelSmallEmphasized = this.labelSmallEmphasized,
        )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (displayLarge != other.displayLarge) return false
        if (displayMedium != other.displayMedium) return false
        if (displaySmall != other.displaySmall) return false
        if (headlineLarge != other.headlineLarge) return false
        if (headlineMedium != other.headlineMedium) return false
        if (headlineSmall != other.headlineSmall) return false
        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false
        if (labelLarge != other.labelLarge) return false
        if (labelMedium != other.labelMedium) return false
        if (labelSmall != other.labelSmall) return false
        if (displayLargeEmphasized != other.displayLargeEmphasized) return false
        if (displayMediumEmphasized != other.displayMediumEmphasized) return false
        if (displaySmallEmphasized != other.displaySmallEmphasized) return false
        if (headlineLargeEmphasized != other.headlineLargeEmphasized) return false
        if (headlineMediumEmphasized != other.headlineMediumEmphasized) return false
        if (headlineSmallEmphasized != other.headlineSmallEmphasized) return false
        if (titleLargeEmphasized != other.titleLargeEmphasized) return false
        if (titleMediumEmphasized != other.titleMediumEmphasized) return false
        if (titleSmallEmphasized != other.titleSmallEmphasized) return false
        if (bodyLargeEmphasized != other.bodyLargeEmphasized) return false
        if (bodyMediumEmphasized != other.bodyMediumEmphasized) return false
        if (bodySmallEmphasized != other.bodySmallEmphasized) return false
        if (labelLargeEmphasized != other.labelLargeEmphasized) return false
        if (labelMediumEmphasized != other.labelMediumEmphasized) return false
        if (labelSmallEmphasized != other.labelSmallEmphasized) return false
        return true
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun hashCode(): Int {
        var result = displayLarge.hashCode()
        result = 31 * result + displayMedium.hashCode()
        result = 31 * result + displaySmall.hashCode()
        result = 31 * result + headlineLarge.hashCode()
        result = 31 * result + headlineMedium.hashCode()
        result = 31 * result + headlineSmall.hashCode()
        result = 31 * result + titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        result = 31 * result + labelLarge.hashCode()
        result = 31 * result + labelMedium.hashCode()
        result = 31 * result + labelSmall.hashCode()
        result = 31 * result + displayLargeEmphasized.hashCode()
        result = 31 * result + displayMediumEmphasized.hashCode()
        result = 31 * result + displaySmallEmphasized.hashCode()
        result = 31 * result + headlineLargeEmphasized.hashCode()
        result = 31 * result + headlineMediumEmphasized.hashCode()
        result = 31 * result + headlineSmallEmphasized.hashCode()
        result = 31 * result + titleLargeEmphasized.hashCode()
        result = 31 * result + titleMediumEmphasized.hashCode()
        result = 31 * result + titleSmallEmphasized.hashCode()
        result = 31 * result + bodyLargeEmphasized.hashCode()
        result = 31 * result + bodyMediumEmphasized.hashCode()
        result = 31 * result + bodySmallEmphasized.hashCode()
        result = 31 * result + labelLargeEmphasized.hashCode()
        result = 31 * result + labelMediumEmphasized.hashCode()
        result = 31 * result + labelSmallEmphasized.hashCode()
        return result
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun toString(): String {
        return "Typography(displayLarge=$displayLarge, displayMedium=$displayMedium," +
            "displaySmall=$displaySmall, " +
            "headlineLarge=$headlineLarge, headlineMedium=$headlineMedium," +
            " headlineSmall=$headlineSmall, " +
            "titleLarge=$titleLarge, titleMedium=$titleMedium, titleSmall=$titleSmall, " +
            "bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, bodySmall=$bodySmall, " +
            "labelLarge=$labelLarge, labelMedium=$labelMedium, labelSmall=$labelSmall, " +
            "displayLargeEmphasized=$displayLargeEmphasized, " +
            "displayMediumEmphasized=$displayMediumEmphasized, " +
            "displaySmallEmphasized=$displaySmallEmphasized, " +
            "headlineLargeEmphasized=$headlineLargeEmphasized, " +
            "headlineMediumEmphasized=$headlineMediumEmphasized, " +
            "headlineSmallEmphasized=$headlineSmallEmphasized, " +
            "titleLargeEmphasized=$titleLargeEmphasized, " +
            "titleMediumEmphasized=$titleMediumEmphasized, " +
            "titleSmallEmphasized=$titleSmallEmphasized, " +
            "bodyLargeEmphasized=$bodyLargeEmphasized, " +
            "bodyMediumEmphasized=$bodyMediumEmphasized, " +
            "bodySmallEmphasized=$bodySmallEmphasized, " +
            "labelLargeEmphasized=$labelLargeEmphasized, " +
            "labelMediumEmphasized=$labelMediumEmphasized, " +
            "labelSmallEmphasized=$labelSmallEmphasized)"
    }
}

/** Helper function for component typography tokens. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
    return when (value) {
        TypographyKeyTokens.DisplayLarge -> displayLarge
        TypographyKeyTokens.DisplayMedium -> displayMedium
        TypographyKeyTokens.DisplaySmall -> displaySmall
        TypographyKeyTokens.HeadlineLarge -> headlineLarge
        TypographyKeyTokens.HeadlineMedium -> headlineMedium
        TypographyKeyTokens.HeadlineSmall -> headlineSmall
        TypographyKeyTokens.TitleLarge -> titleLarge
        TypographyKeyTokens.TitleMedium -> titleMedium
        TypographyKeyTokens.TitleSmall -> titleSmall
        TypographyKeyTokens.BodyLarge -> bodyLarge
        TypographyKeyTokens.BodyMedium -> bodyMedium
        TypographyKeyTokens.BodySmall -> bodySmall
        TypographyKeyTokens.LabelLarge -> labelLarge
        TypographyKeyTokens.LabelMedium -> labelMedium
        TypographyKeyTokens.LabelSmall -> labelSmall
        TypographyKeyTokens.DisplayLargeEmphasized -> displayLargeEmphasized
        TypographyKeyTokens.DisplayMediumEmphasized -> displayMediumEmphasized
        TypographyKeyTokens.DisplaySmallEmphasized -> displaySmallEmphasized
        TypographyKeyTokens.HeadlineLargeEmphasized -> headlineLargeEmphasized
        TypographyKeyTokens.HeadlineMediumEmphasized -> headlineMediumEmphasized
        TypographyKeyTokens.HeadlineSmallEmphasized -> headlineSmallEmphasized
        TypographyKeyTokens.TitleLargeEmphasized -> titleLargeEmphasized
        TypographyKeyTokens.TitleMediumEmphasized -> titleMediumEmphasized
        TypographyKeyTokens.TitleSmallEmphasized -> titleSmallEmphasized
        TypographyKeyTokens.BodyLargeEmphasized -> bodyLargeEmphasized
        TypographyKeyTokens.BodyMediumEmphasized -> bodyMediumEmphasized
        TypographyKeyTokens.BodySmallEmphasized -> bodySmallEmphasized
        TypographyKeyTokens.LabelLargeEmphasized -> labelLargeEmphasized
        TypographyKeyTokens.LabelMediumEmphasized -> labelMediumEmphasized
        TypographyKeyTokens.LabelSmallEmphasized -> labelSmallEmphasized
    }
}

internal val TypographyKeyTokens.value: TextStyle
    @Composable @ReadOnlyComposable get() = MaterialTheme.typography.fromToken(this)

internal val LocalTypography = staticCompositionLocalOf { Typography() }
