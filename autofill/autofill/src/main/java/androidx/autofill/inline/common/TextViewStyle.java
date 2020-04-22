/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Specifies the style for a {@link TextView}.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public final class TextViewStyle extends ViewStyle {

    private static final String KEY_TEXT_VIEW_STYLE = "text_view_style";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_TEXT_FONT_FAMILY = "text_font_family";
    private static final String KEY_TEXT_FONT_STYLE = "text_font_style";

    /**
     * This is made public so it can be used by the renderer to converted the received bundle to
     * a style. It does not validate the provided bundle. {@link #isValid()} or
     * {@link #assertIsValid()} can be used for validation.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public TextViewStyle(@NonNull Bundle bundle) {
        super(bundle);
    }

    /**
     * Applies the specified style on the {@code textView}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void applyStyleOnTextViewIfValid(@NonNull TextView textView) {
        if (!isValid()) {
            return;
        }
        super.applyStyleOnViewIfValid(textView);
        if (mBundle.containsKey(KEY_TEXT_COLOR)) {
            textView.setTextColor(mBundle.getInt(KEY_TEXT_COLOR));
        }
        if (mBundle.containsKey(KEY_TEXT_SIZE)) {
            textView.setTextSize(mBundle.getFloat(KEY_TEXT_SIZE));
        }
        if (mBundle.containsKey(KEY_TEXT_FONT_FAMILY)) {
            final String fontFamily = mBundle.getString(KEY_TEXT_FONT_FAMILY);
            if (!TextUtils.isEmpty(fontFamily)) {
                textView.setTypeface(
                        Typeface.create(fontFamily, mBundle.getInt(KEY_TEXT_FONT_STYLE)));
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    @Override
    protected String getStyleKey() {
        return KEY_TEXT_VIEW_STYLE;
    }

    /**
     * Builder for the {@link TextViewStyle}.
     */
    public static final class Builder extends BaseBuilder<TextViewStyle, Builder> {
        public Builder() {
            super(KEY_TEXT_VIEW_STYLE);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @Override
        protected Builder getThis() {
            return this;
        }

        /**
         * Specifies the text size, in sp.
         *
         * @param textSize The scaled pixel size.
         * @see TextView#setTextSize(float)
         */
        @NonNull
        public Builder setTextSize(float textSize) {
            mBundle.putFloat(KEY_TEXT_SIZE, textSize);
            return this;
        }

        /**
         * Specifies the text color.
         *
         * @param textColor A color value in the form 0xAARRGGBB.
         * @see TextView#setTextColor(int)
         */
        @NonNull
        public Builder setTextColor(@ColorInt int textColor) {
            mBundle.putInt(KEY_TEXT_COLOR, textColor);
            return this;
        }

        /**
         * Specifies the text font family and style. If the font family can not be found/loaded from
         * the renderer process, it may fallback to the default system font.
         *
         * @param fontFamily the font family for the type face
         * @param fontStyle  the style for the type face.
         * @see Typeface#create(String, int)
         * @see TextView#setTypeface(Typeface, int)
         */
        @NonNull
        public Builder setTypeface(@NonNull String fontFamily, int fontStyle) {
            Preconditions.checkNotNull(fontFamily, "fontFamily should not be null");
            mBundle.putString(KEY_TEXT_FONT_FAMILY, fontFamily);
            mBundle.putInt(KEY_TEXT_FONT_STYLE, fontStyle);
            return this;
        }

        @NonNull
        @Override
        public TextViewStyle build() {
            return new TextViewStyle(mBundle);
        }
    }
}
