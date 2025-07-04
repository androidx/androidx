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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.provider.FontsContractCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

@RestrictTo(LIBRARY)
@RequiresApi(31)
public class TypefaceCompatApi31Impl extends TypefaceCompatApi29Impl {
    private static final String TAG = "TypefaceCompatApi31Impl";

    private static Typeface getSystemFontFamily(@Nullable String familyName) {
        Typeface typeface = Typeface.create(familyName, Typeface.NORMAL);
        Typeface defaultTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        return typeface != null && !typeface.equals(defaultTypeface) ? typeface : null;
    }

    @RestrictTo(LIBRARY)
    @Override
    protected @Nullable Font getFontFromSystemFont(FontsContractCompat.@NonNull FontInfo font) {
        final String systemFont = font.getSystemFont();
        if (systemFont == null) {
            return null;
        }

        final Typeface typeface = getSystemFontFamily(systemFont);
        if (typeface == null) {
            return null;
        }

        final Font platformFont = TypefaceCompat.guessPrimaryFont(typeface);
        if (platformFont == null) {
            return null;
        }

        if (TextUtils.isEmpty(font.getVariationSettings())) {
            return platformFont;
        } else {
            try {
                return new Font.Builder(platformFont)
                        .setFontVariationSettings(font.getVariationSettings())
                        .build();
            } catch (IOException e) {
                // This unlikely happen since the font is already opened and mmaped.
                Log.e(TAG, "Failed to clone Font instance. Fall back to provider font.");
                return null;
            }
        }
    }
}
