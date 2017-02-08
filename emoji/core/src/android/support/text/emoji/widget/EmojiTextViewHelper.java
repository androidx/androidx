/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.text.emoji.widget;

import android.support.annotation.NonNull;
import android.support.v4.util.Preconditions;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.widget.TextView;

/**
 * Utility class to enhance a TextView with emoji capability.
 */
public final class EmojiTextViewHelper {
    private final TextView mTextView;
    private final EmojiInputFilter mEmojiInputFilter;

    /**
     * Default constructor.
     *
     * @param textView TextView instance
     */
    public EmojiTextViewHelper(@NonNull TextView textView) {
        Preconditions.checkNotNull(textView, "textView cannot be null");
        mTextView = textView;
        mEmojiInputFilter = new EmojiInputFilter(textView);
    }

    /**
     * Updates widget's TransformationMethod so that the transformed text can be processed.
     * Should be called in the widget constructor.
     *
     * @see #getTransformationMethod(TransformationMethod)
     */
    public void updateTransformationMethod() {
        final TransformationMethod transformationMethod = mTextView.getTransformationMethod();
        if (transformationMethod != null
                && !(transformationMethod instanceof PasswordTransformationMethod)) {
            mTextView.setTransformationMethod(getTransformationMethod(transformationMethod));
        }
    }

    /**
     * Appends EmojiCompat InputFilters to the widget InputFilters. Should be called by {@link
     * TextView#setFilters(InputFilter[])} to update the InputFilters.
     * <p/>
     * <pre><code> {@literal @}Override
     * public void setFilters(InputFilter[] filters) {
     *     super.setFilters(getEmojiTextViewHelper().getFilters(filters));
     * }</code></pre>
     *
     * @param filters InputFilter array passed to {@link TextView#setFilters(InputFilter[])}
     *
     * @return same copy if the array already contains EmojiCompat InputFilter. A new array copy if
     * not.
     */
    public InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
        final int count = filters.length;
        for (int i = 0; i < count; i++) {
            if (filters[i] instanceof EmojiInputFilter) {
                return filters;
            }
        }
        final InputFilter[] newFilters = new InputFilter[filters.length + 1];
        System.arraycopy(filters, 0, newFilters, 0, count);
        newFilters[count] = mEmojiInputFilter;
        return newFilters;
    }

    /**
     * Returns transformation method that can update the transformed text to display emojis.
     *
     * @param transformationMethod instance to be wrapped
     */
    public TransformationMethod getTransformationMethod(
            final TransformationMethod transformationMethod) {
        return new EmojiTransformationMethod(transformationMethod);
    }

    /**
     * Call when allCaps is set on TextView.
     * <p/>
     * <pre><code> {@literal @}Override
     * public void setAllCaps(boolean allCaps) {
     *     super.setAllCaps(allCaps);
     *     getEmojiTextViewHelper().setAllCaps(allCaps);
     * }</code></pre>
     *
     * @param allCaps allCaps parameter passed to {@link TextView#setAllCaps(boolean)}
     */
    public void setAllCaps(boolean allCaps) {
        // When allCaps is set to false TextView sets the transformation method to be null. We
        // are only interested when allCaps is set to true in order to wrap the original method.
        if (allCaps) {
            updateTransformationMethod();
        }
    }
}
