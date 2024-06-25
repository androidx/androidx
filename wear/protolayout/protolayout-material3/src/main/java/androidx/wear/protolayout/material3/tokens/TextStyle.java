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

package androidx.wear.protolayout.material3.tokens;

import static androidx.annotation.Dimension.SP;
import static androidx.wear.protolayout.DimensionBuilders.em;
import static androidx.wear.protolayout.DimensionBuilders.sp;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DimensionBuilders.EmProp;
import androidx.wear.protolayout.DimensionBuilders.SpProp;
import androidx.wear.protolayout.LayoutElementBuilders.FontSetting;
import java.util.ArrayList;
import java.util.List;

/** Text styling configuration. */
@RestrictTo(Scope.LIBRARY)
public class TextStyle {
  /** Font family, such as "Roboto". */
  @NonNull public final String fontFamily;

  /** The size of the font, in scaled pixels. */
  @NonNull public final SpProp size;

  /** The explicit height between lines of text. */
  @NonNull public final SpProp lineHeight;

  /**
   * The text letter spacing. Positive numbers increase the space between letters while negative
   * numbers tighten the space.
   */
  @NonNull public final EmProp letterSpacing;

  /** List of {@link FontSetting} option for font, such as weight, width. */
  @NonNull public final List<FontSetting> fontSettings;

  public TextStyle(
      @NonNull String fontFamily,
      @Dimension(unit = SP) float size,
      @Dimension(unit = SP) float lineHeight,
      @Dimension(unit = SP) float letterSpacing,
      @NonNull List<FontSetting> fontSettings) {
    this.fontFamily = fontFamily;
    this.size = sp(size);
    this.lineHeight = sp(lineHeight);
    this.letterSpacing = letterSpacingSpToEm(letterSpacing, size);
    this.fontSettings = new ArrayList<>(fontSettings);
  }

  private static EmProp letterSpacingSpToEm(
      @Dimension(unit = SP) float letterSpacing, @Dimension(unit = SP) float fontSize) {
    // When the font size and the font tracking are specified in same unit,
    // letter spacing in em = tracking value / font size
    return em(letterSpacing / fontSize);
  }
}
