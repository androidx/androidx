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

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.text.emoji.EmojiCompat;
import android.support.v4.util.Preconditions;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.widget.TextView;

/**
 * Utility class to enhance custom TextView widgets with {@link EmojiCompat}.
 * <pre>
 * public class MyEmojiTextView extends TextView {
 *     public MyEmojiTextView(Context context) {
 *         super(context);
 *         init();
 *     }
 *     // ..
 *     private void init() {
 *         getEmojiTextViewHelper().updateTransformationMethod();
 *     }
 *
 *     {@literal @}Override
 *     public void setFilters(InputFilter[] filters) {
 *         super.setFilters(getEmojiTextViewHelper().getFilters(filters));
 *     }
 *
 *     {@literal @}Override
 *     public void setAllCaps(boolean allCaps) {
 *         super.setAllCaps(allCaps);
 *         getEmojiTextViewHelper().setAllCaps(allCaps);
 *     }
 *
 *     private EmojiTextViewHelper getEmojiTextViewHelper() {
 *         if (mEmojiTextViewHelper == null) {
 *             mEmojiTextViewHelper = new EmojiTextViewHelper(this);
 *         }
 *         return mEmojiTextViewHelper;
 *     }
 * }
 * </pre>
 */
public final class EmojiTextViewHelper {

    private final HelperInternal mHelper;

    /**
     * Default constructor.
     *
     * @param textView TextView instance
     */
    public EmojiTextViewHelper(@NonNull TextView textView) {
        Preconditions.checkNotNull(textView, "textView cannot be null");
        mHelper = Build.VERSION.SDK_INT >= 19 ? new HelperInternal19(textView)
                : new HelperInternal();
    }

    /**
     * Updates widget's TransformationMethod so that the transformed text can be processed.
     * Should be called in the widget constructor. When used on devices running API 18 or below,
     * this method does nothing.
     *
     * @see #wrapTransformationMethod(TransformationMethod)
     */
    public void updateTransformationMethod() {
        mHelper.updateTransformationMethod();
    }

    /**
     * Appends EmojiCompat InputFilters to the widget InputFilters. Should be called by {@link
     * TextView#setFilters(InputFilter[])} to update the InputFilters. When used on devices running
     * API 18 or below, this method returns {@code filters} that is given as a parameter.
     *
     * @param filters InputFilter array passed to {@link TextView#setFilters(InputFilter[])}
     *
     * @return same copy if the array already contains EmojiCompat InputFilter. A new array copy if
     * not.
     */
    @NonNull
    public InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
        return mHelper.getFilters(filters);
    }

    /**
     * Returns transformation method that can update the transformed text to display emojis. When
     * used on devices running API 18 or below, this method returns {@code transformationMethod}
     * that is given as a parameter.
     *
     * @param transformationMethod instance to be wrapped
     */
    @Nullable
    public TransformationMethod wrapTransformationMethod(
            @Nullable TransformationMethod transformationMethod) {
        return mHelper.wrapTransformationMethod(transformationMethod);
    }

    /**
     * Call when allCaps is set on TextView. When used on devices running API 18 or below, this
     * method does nothing.
     *
     * @param allCaps allCaps parameter passed to {@link TextView#setAllCaps(boolean)}
     */
    public void setAllCaps(boolean allCaps) {
        mHelper.setAllCaps(allCaps);
    }

    private static class HelperInternal {

        void updateTransformationMethod() {
            // do nothing
        }

        InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
            return filters;
        }

        TransformationMethod wrapTransformationMethod(TransformationMethod transformationMethod) {
            return transformationMethod;
        }

        void setAllCaps(boolean allCaps) {
            // do nothing
        }
    }

    @RequiresApi(19)
    private static class HelperInternal19 extends HelperInternal {
        private final TextView mTextView;
        private final EmojiInputFilter mEmojiInputFilter;

        HelperInternal19(TextView textView) {
            mTextView = textView;
            mEmojiInputFilter = new EmojiInputFilter(textView);
        }

        @Override
        void updateTransformationMethod() {
            final TransformationMethod tm = mTextView.getTransformationMethod();
            if (tm != null && !(tm instanceof PasswordTransformationMethod)) {
                mTextView.setTransformationMethod(wrapTransformationMethod(tm));
            }
        }

        @Override
        InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
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

        @Override
        TransformationMethod wrapTransformationMethod(TransformationMethod transformationMethod) {
            if (transformationMethod instanceof EmojiTransformationMethod) {
                return transformationMethod;
            }
            return new EmojiTransformationMethod(transformationMethod);
        }

        @Override
        void setAllCaps(boolean allCaps) {
            // When allCaps is set to false TextView sets the transformation method to be null. We
            // are only interested when allCaps is set to true in order to wrap the original method.
            if (allCaps) {
                updateTransformationMethod();
            }
        }

    }
}
