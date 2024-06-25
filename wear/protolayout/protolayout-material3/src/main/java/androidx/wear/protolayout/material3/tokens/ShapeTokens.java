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

import static androidx.wear.protolayout.DimensionBuilders.dp;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ModifiersBuilders.Corner;

/** The Material 3 shape system on Wear OS. */
@RestrictTo(Scope.LIBRARY)
public final class ShapeTokens {

  @NonNull public static final Corner CORNER_EXTRA_LARGE = roundedCornerShape(36.0f);

  @NonNull public static final Corner CORNER_EXTRA_SMALL = roundedCornerShape(4.0f);

  // Full corner can be achieved by setting a big radius value, which will be clamped by
  // min(halfWidth, halfHeight).
  @NonNull public static final Corner CORNER_FULL = roundedCornerShape(99999.0f);

  @NonNull public static final Corner CORNER_LARGE = roundedCornerShape(26.0f);

  @NonNull public static final Corner CORNER_MEDIUM = roundedCornerShape(18.0f);

  @NonNull public static final Corner CORNER_NONE = roundedCornerShape(0.0f);

  @NonNull public static final Corner CORNER_SMALL = roundedCornerShape(8.0f);

  @NonNull
  private static Corner roundedCornerShape(float sizeDp) {
    return new Corner.Builder().setRadius(dp(sizeDp)).build();
  }

  private ShapeTokens() {}
}
