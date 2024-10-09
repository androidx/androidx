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

package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputFilter;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.R;
import androidx.emoji2.viewsintegration.EmojiTextViewHelper;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for using EmojiCompat from TextView in appcompat.
 */
class AppCompatEmojiTextHelper {

    private final @NonNull TextView mView;
    private final @NonNull EmojiTextViewHelper mEmojiTextViewHelper;

    AppCompatEmojiTextHelper(@NonNull TextView view) {
        mView = view;
        mEmojiTextViewHelper = new EmojiTextViewHelper(view, false);
    }

    /**
     * Load enabled behavior based on {@link R.styleable#AppCompatTextView_emojiCompatEnabled}.
     * @param attrs from view
     * @param defStyleAttr from view
     */
    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        Context context = mView.getContext();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppCompatTextView,
                defStyleAttr, 0);
        boolean enabled = true;
        try {
            if (a.hasValue(R.styleable.AppCompatTextView_emojiCompatEnabled)) {
                enabled = a.getBoolean(R.styleable.AppCompatTextView_emojiCompatEnabled, true);
            }
        } finally {
            a.recycle();
        }
        setEnabled(enabled);
    }

    /**
     * When set to false, this helper will do no emoji processing.
     *
     * After calling this method, the TextView will always be configured to display emoji (or
     * not) based on disabled state and you don't need to call other methods.
     *
     * You may call this method again to enable EmojiCompat on a TextView after EmojiCompat is
     * configured, if the configuration happened after the TextView loaded attributes. Doing so
     * will update the TextView to correctly display emoji.
     */
    void setEnabled(boolean enabled) {
        mEmojiTextViewHelper.setEnabled(enabled);
    }

    /**
     * @return current enabled state
     */
    public boolean isEnabled() {
        return mEmojiTextViewHelper.isEnabled();
    }

    /**
     * Get filters with appropriate emoji processing.
     *
     * TextViews using this helper should always call this is setFilters.
     *
     * @param filters current filters on the TextView
     * @return filters with optional emoji span filter depending on enabled state and system
     * availability
     */
    @SuppressWarnings("ArrayReturn")
    InputFilter @NonNull [] getFilters(
            @SuppressWarnings("ArrayReturn") InputFilter @NonNull [] filters) {
        return mEmojiTextViewHelper.getFilters(filters);
    }

    /**
     * Call this from TextView.setAllCaps to ensure the transformation method remains accurate
     * for emoji spans.
     *
     * @param allCaps if text should be all caps
     */
    void setAllCaps(boolean allCaps) {
        mEmojiTextViewHelper.setAllCaps(allCaps);
    }

    /**
     * Wrap an existing transformation method, or null, to display emoji using EmojiCompat.
     *
     * @param transformationMethod if not null, call this transformation method before emoji
     *                             transform
     * @return the correct transformation based on isEnabled, may be null
     */
    public @Nullable TransformationMethod wrapTransformationMethod(
            @Nullable TransformationMethod transformationMethod) {
        return mEmojiTextViewHelper.wrapTransformationMethod(transformationMethod);
    }
}
