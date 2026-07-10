/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.view.inputmethod;

import android.os.Build;
import android.os.PersistableBundle;
import android.view.inputmethod.TextAttribute;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing features in {@link android.view.inputmethod.TextAttribute} in a backwards
 * compatible fashion.
 */
public final class TextAttributeCompat {

    private interface TextAttributeCompatImpl {
        @NonNull List<String> getTextConversionSuggestions();
        boolean isTextSuggestionSelected();
        @NonNull PersistableBundle getExtras();
        @Nullable Object getTextAttribute();
    }

    private static final class TextAttributeCompatBaseImpl implements TextAttributeCompatImpl {
        private final @NonNull List<String> mTextConversionSuggestions;
        private final @NonNull PersistableBundle mExtras;
        private final boolean mTextSuggestionSelected;

        TextAttributeCompatBaseImpl(@NonNull List<String> textConversionSuggestions,
                @NonNull PersistableBundle extras, boolean textSuggestionSelected) {
            mTextConversionSuggestions = Collections.unmodifiableList(textConversionSuggestions);
            mExtras = extras;
            mTextSuggestionSelected = textSuggestionSelected;
        }

        @Override
        public @NonNull List<String> getTextConversionSuggestions() {
            return mTextConversionSuggestions;
        }

        @Override
        public boolean isTextSuggestionSelected() {
            return mTextSuggestionSelected;
        }

        @Override
        public @NonNull PersistableBundle getExtras() {
            return mExtras;
        }

        @Override
        public @Nullable Object getTextAttribute() {
            return null;
        }
    }

    @RequiresApi(33)
    private static final class TextAttributeCompatApi33Impl implements TextAttributeCompatImpl {
        final @NonNull TextAttribute mObject;
        private final boolean mTextSuggestionSelected;

        TextAttributeCompatApi33Impl(@NonNull Object textAttribute) {
            mObject = (TextAttribute) textAttribute;
            mTextSuggestionSelected = false;
        }

        TextAttributeCompatApi33Impl(@NonNull List<String> textConversionSuggestions,
                @NonNull PersistableBundle extras, boolean textSuggestionSelected) {
            mObject = new TextAttribute.Builder()
                    .setTextConversionSuggestions(textConversionSuggestions)
                    .setExtras(extras)
                    .build();
            mTextSuggestionSelected = textSuggestionSelected;
        }

        @Override
        public @NonNull List<String> getTextConversionSuggestions() {
            return mObject.getTextConversionSuggestions();
        }

        @Override
        public boolean isTextSuggestionSelected() {
            return mTextSuggestionSelected;
        }

        @Override
        public @NonNull PersistableBundle getExtras() {
            return mObject.getExtras();
        }

        @Override
        public @NonNull Object getTextAttribute() {
            return mObject;
        }
    }

    @RequiresApi(37)
    private static final class TextAttributeCompatApi37Impl implements TextAttributeCompatImpl {
        final @NonNull TextAttribute mObject;

        TextAttributeCompatApi37Impl(@NonNull Object textAttribute) {
            mObject = (TextAttribute) textAttribute;
        }

        TextAttributeCompatApi37Impl(@NonNull List<String> textConversionSuggestions,
                @NonNull PersistableBundle extras, boolean textSuggestionSelected) {
            mObject = new TextAttribute.Builder()
                    .setTextConversionSuggestions(textConversionSuggestions)
                    .setExtras(extras)
                    .setTextSuggestionSelected(textSuggestionSelected)
                    .build();
        }

        @Override
        public @NonNull List<String> getTextConversionSuggestions() {
            return mObject.getTextConversionSuggestions();
        }

        @Override
        public boolean isTextSuggestionSelected() {
            return mObject.isTextSuggestionSelected();
        }

        @Override
        public @NonNull PersistableBundle getExtras() {
            return mObject.getExtras();
        }

        @Override
        public @NonNull Object getTextAttribute() {
            return mObject;
        }
    }

    private final TextAttributeCompatImpl mImpl;

    /**
     * Constructs {@link TextAttributeCompat}
     *
     * @param textConversionSuggestions list of text conversion suggestions. If the list is empty,
     *                                  it means that the IME has not set this field or the IME
     *                                  didn't have suggestions for the application.
     * @param extras bundle of extras data, if empty then either the IME didn't set this field or
     *               no extras are specified.
     * @param textSuggestionSelected whether the text is undergoing a text candidate selection,
     *                               relevant for transliteration languages. This describes a state
     *                               when the user is currently in the process of selecting a text
     *                               suggestion candidate (via hover/highlight).
     */
    private TextAttributeCompat(@NonNull List<String> textConversionSuggestions,
            @NonNull PersistableBundle extras, boolean textSuggestionSelected) {
        if (Build.VERSION.SDK_INT >= 37) {
            mImpl = new TextAttributeCompatApi37Impl(textConversionSuggestions, extras,
                    textSuggestionSelected);
        } else if (Build.VERSION.SDK_INT >= 33) {
            mImpl = new TextAttributeCompatApi33Impl(textConversionSuggestions, extras,
                    textSuggestionSelected);
        } else {
            mImpl = new TextAttributeCompatBaseImpl(textConversionSuggestions, extras,
                    textSuggestionSelected);
        }
    }

    /**
     * Private constructor, use {@link TextAttributeCompat.Builder} to create a new instance.
     */
    private TextAttributeCompat(@NonNull TextAttributeCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Get the list of text conversion suggestions.
     */
    public @NonNull List<String> getTextConversionSuggestions() {
        return mImpl.getTextConversionSuggestions();
    }

    /**
     * Get whether the text is undergoing a text candidate selection.
     */
    public boolean isTextSuggestionSelected() {
        return mImpl.isTextSuggestionSelected();
    }

    /**
     * Get the extras data.
     */
    public @NonNull PersistableBundle getExtras() {
        return mImpl.getExtras();
    }

    /**
     * Creates an instance from a framework {@link android.view.inputmethod.TextAttribute} object.
     *
     * @param textAttribute an {@link android.view.inputmethod.TextAttribute} object, or
     *        {@code null}.
     * @return an equivalent {@link TextAttributeCompat} object, or {@code null} if not supported.
     */
    public static @Nullable TextAttributeCompat wrap(@Nullable Object textAttribute) {
        if (textAttribute == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 37) {
            return new TextAttributeCompat(new TextAttributeCompatApi37Impl(textAttribute));
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new TextAttributeCompat(new TextAttributeCompatApi33Impl(textAttribute));
        }
        return null;
    }

    /**
     * Gets the underlying framework {@link android.view.inputmethod.TextAttribute} object.
     *
     * @return an equivalent {@link android.view.inputmethod.TextAttribute} object, or {@code null}
     * if not supported.
     */
    public @Nullable Object unwrap() {
        return mImpl.getTextAttribute();
    }

    /**
     * Builder for creating a {@link TextAttributeCompat}.
     */
    public static final class Builder {
        private List<String> mTextConversionSuggestions = new ArrayList<>();
        private PersistableBundle mExtras = new PersistableBundle();
        private boolean mTextSuggestionSelected = false;

        /**
         * Sets text conversion suggestions.
         */
        public @NonNull Builder setTextConversionSuggestions(
                @NonNull List<String> textConversionSuggestions) {
            mTextConversionSuggestions = textConversionSuggestions;
            return this;
        }

        /**
         * Sets whether a text conversion suggestion is currently being selected.
         */
        public @NonNull Builder setTextSuggestionSelected(boolean textSuggestionSelected) {
            mTextSuggestionSelected = textSuggestionSelected;
            return this;
        }

        /**
         * Sets extras data.
         */
        public @NonNull Builder setExtras(@NonNull PersistableBundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * @return a new {@link TextAttributeCompat}.
         */
        public @NonNull TextAttributeCompat build() {
            return new TextAttributeCompat(mTextConversionSuggestions, mExtras,
                    mTextSuggestionSelected);
        }
    }
}
