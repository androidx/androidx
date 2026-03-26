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
import androidx.compose.ui.text.font.FontFamily

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
class Typography(
    val displayLarge: TextStyle = typographyTokens.DisplayLarge,
    val displayMedium: TextStyle = typographyTokens.DisplayMedium,
    val displaySmall: TextStyle = typographyTokens.DisplaySmall,
    val headlineLarge: TextStyle = typographyTokens.HeadlineLarge,
    val headlineMedium: TextStyle = typographyTokens.HeadlineMedium,
    val headlineSmall: TextStyle = typographyTokens.HeadlineSmall,
    val titleLarge: TextStyle = typographyTokens.TitleLarge,
    val titleMedium: TextStyle = typographyTokens.TitleMedium,
    val titleSmall: TextStyle = typographyTokens.TitleSmall,
    val bodyLarge: TextStyle = typographyTokens.BodyLarge,
    val bodyMedium: TextStyle = typographyTokens.BodyMedium,
    val bodySmall: TextStyle = typographyTokens.BodySmall,
    val labelLarge: TextStyle = typographyTokens.LabelLarge,
    val labelMedium: TextStyle = typographyTokens.LabelMedium,
    val labelSmall: TextStyle = typographyTokens.LabelSmall,
    displayLargeEmphasized: TextStyle = typographyTokens.DisplayLargeEmphasized,
    displayMediumEmphasized: TextStyle = typographyTokens.DisplayMediumEmphasized,
    displaySmallEmphasized: TextStyle = typographyTokens.DisplaySmallEmphasized,
    headlineLargeEmphasized: TextStyle = typographyTokens.HeadlineLargeEmphasized,
    headlineMediumEmphasized: TextStyle = typographyTokens.HeadlineMediumEmphasized,
    headlineSmallEmphasized: TextStyle = typographyTokens.HeadlineSmallEmphasized,
    titleLargeEmphasized: TextStyle = typographyTokens.TitleLargeEmphasized,
    titleMediumEmphasized: TextStyle = typographyTokens.TitleMediumEmphasized,
    titleSmallEmphasized: TextStyle = typographyTokens.TitleSmallEmphasized,
    bodyLargeEmphasized: TextStyle = typographyTokens.BodyLargeEmphasized,
    bodyMediumEmphasized: TextStyle = typographyTokens.BodyMediumEmphasized,
    bodySmallEmphasized: TextStyle = typographyTokens.BodySmallEmphasized,
    labelLargeEmphasized: TextStyle = typographyTokens.LabelLargeEmphasized,
    labelMediumEmphasized: TextStyle = typographyTokens.LabelMediumEmphasized,
    labelSmallEmphasized: TextStyle = typographyTokens.LabelSmallEmphasized,
) {
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
     * @param fontFamily the [FontFamily] to be used for the typography.
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
     * @param displayLargeEmphasized an emphasized version of [displayLarge].
     * @param displayMediumEmphasized an emphasized version of [displayMedium].
     * @param displaySmallEmphasized an emphasized version of [displaySmall].
     * @param headlineLargeEmphasized an emphasized version of [headlineLarge].
     * @param headlineMediumEmphasized an emphasized version of [headlineMedium].
     * @param headlineSmallEmphasized an emphasized version of [headlineSmall].
     * @param titleLargeEmphasized an emphasized version of [titleLarge].
     * @param titleMediumEmphasized an emphasized version of [titleMedium].
     * @param titleSmallEmphasized an emphasized version of [titleSmall].
     * @param bodyLargeEmphasized an emphasized version of [bodyLarge].
     * @param bodyMediumEmphasized an emphasized version of [bodyMedium].
     * @param bodySmallEmphasized an emphasized version of [bodySmall].
     * @param labelLargeEmphasized an emphasized version of [labelLarge].
     * @param labelMediumEmphasized an emphasized version of [labelMedium].
     * @param labelSmallEmphasized an emphasized version of [labelSmall].
     */
    constructor(
        fontFamily: FontFamily,
        displayLarge: TextStyle? = null,
        displayMedium: TextStyle? = null,
        displaySmall: TextStyle? = null,
        headlineLarge: TextStyle? = null,
        headlineMedium: TextStyle? = null,
        headlineSmall: TextStyle? = null,
        titleLarge: TextStyle? = null,
        titleMedium: TextStyle? = null,
        titleSmall: TextStyle? = null,
        bodyLarge: TextStyle? = null,
        bodyMedium: TextStyle? = null,
        bodySmall: TextStyle? = null,
        labelLarge: TextStyle? = null,
        labelMedium: TextStyle? = null,
        labelSmall: TextStyle? = null,
        displayLargeEmphasized: TextStyle? = null,
        displayMediumEmphasized: TextStyle? = null,
        displaySmallEmphasized: TextStyle? = null,
        headlineLargeEmphasized: TextStyle? = null,
        headlineMediumEmphasized: TextStyle? = null,
        headlineSmallEmphasized: TextStyle? = null,
        titleLargeEmphasized: TextStyle? = null,
        titleMediumEmphasized: TextStyle? = null,
        titleSmallEmphasized: TextStyle? = null,
        bodyLargeEmphasized: TextStyle? = null,
        bodyMediumEmphasized: TextStyle? = null,
        bodySmallEmphasized: TextStyle? = null,
        labelLargeEmphasized: TextStyle? = null,
        labelMediumEmphasized: TextStyle? = null,
        labelSmallEmphasized: TextStyle? = null,
    ) : this(
        tokens = TypographyTokens(fontFamily = fontFamily),
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

    private constructor(
        tokens: TypographyTokens,
        displayLarge: TextStyle?,
        displayMedium: TextStyle?,
        displaySmall: TextStyle?,
        headlineLarge: TextStyle?,
        headlineMedium: TextStyle?,
        headlineSmall: TextStyle?,
        titleLarge: TextStyle?,
        titleMedium: TextStyle?,
        titleSmall: TextStyle?,
        bodyLarge: TextStyle?,
        bodyMedium: TextStyle?,
        bodySmall: TextStyle?,
        labelLarge: TextStyle?,
        labelMedium: TextStyle?,
        labelSmall: TextStyle?,
        displayLargeEmphasized: TextStyle?,
        displayMediumEmphasized: TextStyle?,
        displaySmallEmphasized: TextStyle?,
        headlineLargeEmphasized: TextStyle?,
        headlineMediumEmphasized: TextStyle?,
        headlineSmallEmphasized: TextStyle?,
        titleLargeEmphasized: TextStyle?,
        titleMediumEmphasized: TextStyle?,
        titleSmallEmphasized: TextStyle?,
        bodyLargeEmphasized: TextStyle?,
        bodyMediumEmphasized: TextStyle?,
        bodySmallEmphasized: TextStyle?,
        labelLargeEmphasized: TextStyle?,
        labelMediumEmphasized: TextStyle?,
        labelSmallEmphasized: TextStyle?,
    ) : this(
        displayLarge = displayLarge ?: tokens.DisplayLarge,
        displayMedium = displayMedium ?: tokens.DisplayMedium,
        displaySmall = displaySmall ?: tokens.DisplaySmall,
        headlineLarge = headlineLarge ?: tokens.HeadlineLarge,
        headlineMedium = headlineMedium ?: tokens.HeadlineMedium,
        headlineSmall = headlineSmall ?: tokens.HeadlineSmall,
        titleLarge = titleLarge ?: tokens.TitleLarge,
        titleMedium = titleMedium ?: tokens.TitleMedium,
        titleSmall = titleSmall ?: tokens.TitleSmall,
        bodyLarge = bodyLarge ?: tokens.BodyLarge,
        bodyMedium = bodyMedium ?: tokens.BodyMedium,
        bodySmall = bodySmall ?: tokens.BodySmall,
        labelLarge = labelLarge ?: tokens.LabelLarge,
        labelMedium = labelMedium ?: tokens.LabelMedium,
        labelSmall = labelSmall ?: tokens.LabelSmall,
        displayLargeEmphasized = displayLargeEmphasized ?: tokens.DisplayLargeEmphasized,
        displayMediumEmphasized = displayMediumEmphasized ?: tokens.DisplayMediumEmphasized,
        displaySmallEmphasized = displaySmallEmphasized ?: tokens.DisplaySmallEmphasized,
        headlineLargeEmphasized = headlineLargeEmphasized ?: tokens.HeadlineLargeEmphasized,
        headlineMediumEmphasized = headlineMediumEmphasized ?: tokens.HeadlineMediumEmphasized,
        headlineSmallEmphasized = headlineSmallEmphasized ?: tokens.HeadlineSmallEmphasized,
        titleLargeEmphasized = titleLargeEmphasized ?: tokens.TitleLargeEmphasized,
        titleMediumEmphasized = titleMediumEmphasized ?: tokens.TitleMediumEmphasized,
        titleSmallEmphasized = titleSmallEmphasized ?: tokens.TitleSmallEmphasized,
        bodyLargeEmphasized = bodyLargeEmphasized ?: tokens.BodyLargeEmphasized,
        bodyMediumEmphasized = bodyMediumEmphasized ?: tokens.BodyMediumEmphasized,
        bodySmallEmphasized = bodySmallEmphasized ?: tokens.BodySmallEmphasized,
        labelLargeEmphasized = labelLargeEmphasized ?: tokens.LabelLargeEmphasized,
        labelMediumEmphasized = labelMediumEmphasized ?: tokens.LabelMediumEmphasized,
        labelSmallEmphasized = labelSmallEmphasized ?: tokens.LabelSmallEmphasized,
    )

    /** an emphasized version of [displayLarge]. */
    val displayLargeEmphasized = displayLargeEmphasized

    /** an emphasized version of [displayMedium]. */
    val displayMediumEmphasized = displayMediumEmphasized

    /** an emphasized version of [displaySmall]. */
    val displaySmallEmphasized = displaySmallEmphasized

    /** an emphasized version of [headlineLarge]. */
    val headlineLargeEmphasized = headlineLargeEmphasized

    /** an emphasized version of [headlineMedium]. */
    val headlineMediumEmphasized = headlineMediumEmphasized

    /** an emphasized version of [headlineSmall]. */
    val headlineSmallEmphasized = headlineSmallEmphasized

    /** an emphasized version of [titleLarge]. */
    val titleLargeEmphasized = titleLargeEmphasized

    /** an emphasized version of [titleMedium]. */
    val titleMediumEmphasized = titleMediumEmphasized

    /** an emphasized version of [titleSmall]. */
    val titleSmallEmphasized = titleSmallEmphasized

    /** an emphasized version of [bodyLarge]. */
    val bodyLargeEmphasized = bodyLargeEmphasized

    /** an emphasized version of [bodyMedium]. */
    val bodyMediumEmphasized = bodyMediumEmphasized

    /** an emphasized version of [bodySmall]. */
    val bodySmallEmphasized = bodySmallEmphasized

    /** an emphasized version of [labelLarge]. */
    val labelLargeEmphasized = labelLargeEmphasized

    /** an emphasized version of [labelMedium]. */
    val labelMediumEmphasized = labelMediumEmphasized

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
    constructor(
        displayLarge: TextStyle = typographyTokens.DisplayLarge,
        displayMedium: TextStyle = typographyTokens.DisplayMedium,
        displaySmall: TextStyle = typographyTokens.DisplaySmall,
        headlineLarge: TextStyle = typographyTokens.HeadlineLarge,
        headlineMedium: TextStyle = typographyTokens.HeadlineMedium,
        headlineSmall: TextStyle = typographyTokens.HeadlineSmall,
        titleLarge: TextStyle = typographyTokens.TitleLarge,
        titleMedium: TextStyle = typographyTokens.TitleMedium,
        titleSmall: TextStyle = typographyTokens.TitleSmall,
        bodyLarge: TextStyle = typographyTokens.BodyLarge,
        bodyMedium: TextStyle = typographyTokens.BodyMedium,
        bodySmall: TextStyle = typographyTokens.BodySmall,
        labelLarge: TextStyle = typographyTokens.LabelLarge,
        labelMedium: TextStyle = typographyTokens.LabelMedium,
        labelSmall: TextStyle = typographyTokens.LabelSmall,
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
internal fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
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

private val typographyTokens: TypographyTokens = TypographyTokens()
