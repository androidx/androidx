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

// VERSION: v0_64
// GENERATED CODE - DO NOT MODIFY BY HAND

package androidx.wear.protolayout.material3.tokens;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** A range of contrasting text styles in Material3 for supporting product needs. */
@RestrictTo(Scope.LIBRARY)
public final class TypographyTokens {

  /**
   * ArcMedium is for arc headers and titles. Arc is for text along a curved path on the screen,
   * reserved for short header text strings at the very top or bottom of the screen like page
   * titles.
   */
  public static final TextStyle ARC_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.ARC_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.ARC_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.ARC_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.ARC_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.ARC_MEDIUM_VARIATION_SETTINGS);

  /**
   * ArcSmall is for limited arc strings of text. Arc is for text along a curved path on the screen,
   * reserved for short curved text strings at the bottom of the screen.
   */
  public static final TextStyle ARC_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.ARC_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.ARC_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.ARC_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.ARC_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.ARC_SMALL_VARIATION_SETTINGS);

  /**
   * BodyExtraSmall is the smallest body. Body texts are typically used for long-form writing as it
   * works well for small text sizes. For longer sections of text, a serif or sans serif typeface is
   * recommended.
   */
  public static final TextStyle BODY_EXTRA_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.BODY_EXTRA_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.BODY_EXTRA_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.BODY_EXTRA_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.BODY_EXTRA_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.BODY_EXTRA_SMALL_VARIATION_SETTINGS);

  /**
   * BodyLarge is the largest body. Body texts are typically used for long-form writing as it works
   * well for small text sizes. For longer sections of text, a serif or sans serif typeface is
   * recommended.
   */
  public static final TextStyle BODY_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.BODY_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.BODY_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.BODY_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.BODY_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.BODY_LARGE_VARIATION_SETTINGS);

  /**
   * BodyMedium is second largest body. Body texts are typically used for long-form writing as it
   * works well for small text sizes. For longer sections of text, a serif or sans serif typeface is
   * recommended.
   */
  public static final TextStyle BODY_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.BODY_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.BODY_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.BODY_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.BODY_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.BODY_MEDIUM_VARIATION_SETTINGS);

  /**
   * BodySmall is third largest body. Body texts are typically used for long-form writing as it
   * works well for small text sizes. For longer sections of text, a serif or sans serif typeface is
   * recommended.
   */
  public static final TextStyle BODY_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.BODY_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.BODY_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.BODY_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.BODY_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.BODY_SMALL_VARIATION_SETTINGS);

  /**
   * DisplayLarge is the largest headline. Displays are the largest text on the screen, reserved for
   * short, important text.
   */
  public static final TextStyle DISPLAY_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.DISPLAY_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.DISPLAY_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.DISPLAY_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.DISPLAY_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.DISPLAY_LARGE_VARIATION_SETTINGS);

  /**
   * DisplayMedium is the second largest headline. Displays are the largest text on the screen,
   * reserved for short, important text.
   */
  public static final TextStyle DISPLAY_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.DISPLAY_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.DISPLAY_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.DISPLAY_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.DISPLAY_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.DISPLAY_MEDIUM_VARIATION_SETTINGS);

  /**
   * DisplaySmall is the smallest headline. Displays are the largest text on the screen, reserved
   * for short, important text.
   */
  public static final TextStyle DISPLAY_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.DISPLAY_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.DISPLAY_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.DISPLAY_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.DISPLAY_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.DISPLAY_SMALL_VARIATION_SETTINGS);

  /**
   * LabelLarge is the largest label. They are used for displaying prominent texts like label on
   * title buttons.
   */
  public static final TextStyle LABEL_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.LABEL_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.LABEL_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.LABEL_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.LABEL_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.LABEL_LARGE_VARIATION_SETTINGS);

  /**
   * LabelMedium is the medium label. They are used for displaying texts like primary label on
   * buttons.
   */
  public static final TextStyle LABEL_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.LABEL_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.LABEL_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.LABEL_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.LABEL_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.LABEL_MEDIUM_VARIATION_SETTINGS);

  /**
   * LabelSmall is the small label. They are used for displaying texts like secondary label on
   * buttons, labels on compact buttons.
   */
  public static final TextStyle LABEL_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.LABEL_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.LABEL_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.LABEL_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.LABEL_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.LABEL_SMALL_VARIATION_SETTINGS);

  /**
   * NumeralsExtraLarge is the largest role for digits. Numerals use tabular spacing by default.
   * They highlight and express glanceable numbers that are limited to a two or three characters
   * only, where no localization is required like the charging screen.
   */
  public static final TextStyle NUMERAL_EXTRA_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.NUMERAL_EXTRA_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.NUMERAL_EXTRA_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.NUMERAL_EXTRA_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.NUMERAL_EXTRA_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.NUMERAL_EXTRA_LARGE_VARIATION_SETTINGS);

  /**
   * NumeralsExtraSmall is the smallest role for digits. Numerals use tabular spacing by default.
   * They are for numbers that need to accommodate longer strings of digits, where no localization
   * is required like in-workout metrics.
   */
  public static final TextStyle NUMERAL_EXTRA_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.NUMERAL_EXTRA_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.NUMERAL_EXTRA_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.NUMERAL_EXTRA_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.NUMERAL_EXTRA_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.NUMERAL_EXTRA_SMALL_VARIATION_SETTINGS);

  /**
   * NumeralsLarge is the second largest role for digits. Numerals use tabular spacing by default.
   * They are large sized number strings that are limited to big displays of time, where no
   * localization is required like a timer countdown.
   */
  public static final TextStyle NUMERAL_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.NUMERAL_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.NUMERAL_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.NUMERAL_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.NUMERAL_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.NUMERAL_LARGE_VARIATION_SETTINGS);

  /**
   * NumeralsMedium is the third largest role for digits. Numerals use tabular spacing by default.
   * They are medium sized numbers that are limited to short strings of digits, where no
   * localization is required like a steps count.
   */
  public static final TextStyle NUMERAL_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.NUMERAL_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.NUMERAL_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.NUMERAL_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.NUMERAL_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.NUMERAL_MEDIUM_VARIATION_SETTINGS);

  /**
   * NumeralsSmall is the fourth largest role for digits. Numerals use tabular spacing by default.
   * They are for numbers that need emphasis at a smaller scale, where no localization is required
   * like date and time pickers.
   */
  public static final TextStyle NUMERAL_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.NUMERAL_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.NUMERAL_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.NUMERAL_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.NUMERAL_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.NUMERAL_SMALL_VARIATION_SETTINGS);

  /**
   * TitleLarge is the largest title. Titles are smaller than Displays. They are typically reserved
   * for medium-emphasis text that is shorter in length.
   */
  public static final TextStyle TITLE_LARGE =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.TITLE_LARGE_FONT_FAMILY,
          /* size= */ TypeScaleTokens.TITLE_LARGE_SIZE,
          /* lineHeight= */ TypeScaleTokens.TITLE_LARGE_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.TITLE_LARGE_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.TITLE_LARGE_VARIATION_SETTINGS);

  /**
   * TitleMedium is the medium title. Titles are smaller than Displays. They are typically reserved
   * for medium-emphasis text that is shorter in length.
   */
  public static final TextStyle TITLE_MEDIUM =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.TITLE_MEDIUM_FONT_FAMILY,
          /* size= */ TypeScaleTokens.TITLE_MEDIUM_SIZE,
          /* lineHeight= */ TypeScaleTokens.TITLE_MEDIUM_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.TITLE_MEDIUM_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.TITLE_MEDIUM_VARIATION_SETTINGS);

  /**
   * TitleSmall is the smallest title. Titles are smaller than Displays. They are typically reserved
   * for medium-emphasis text that is shorter in length.
   */
  public static final TextStyle TITLE_SMALL =
      new TextStyle(
          /* fontFamily= */ TypeScaleTokens.TITLE_SMALL_FONT_FAMILY,
          /* size= */ TypeScaleTokens.TITLE_SMALL_SIZE,
          /* lineHeight= */ TypeScaleTokens.TITLE_SMALL_LINE_HEIGHT,
          /* letterSpacing= */ TypeScaleTokens.TITLE_SMALL_TRACKING,
          /* fontSettings= */ VariableFontSettingsTokens.TITLE_SMALL_VARIATION_SETTINGS);

  private TypographyTokens() {}
}
