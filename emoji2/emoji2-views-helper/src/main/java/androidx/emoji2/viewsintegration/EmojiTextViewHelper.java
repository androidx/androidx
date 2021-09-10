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
package androidx.emoji2.viewsintegration;

import android.os.Build;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.SparseArray;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.emoji2.text.EmojiCompat;

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
        this(textView, true);
    }

    /**
     * Allows skipping of all processing until EmojiCompat.init is called.
     *
     * This is useful when integrating EmojiTextViewHelper into libraries that subclass TextView
     * that do not have control over EmojiCompat initialization by the app that uses the TextView
     * subclass.
     *
     * If this helper is initialized prior to EmojiCompat.init, the TextView it's configuring
     * will not display emoji using EmojiCompat after init is called until the transformation
     * method and filter are updated. The easiest way to do that is call
     * {@link EmojiTextViewHelper#setEnabled(boolean)}.
     *
     * @param textView TextView instance
     * @param expectInitializedEmojiCompat if true, this helper will assume init has been called
     *                                     and throw if it has not. If false, the methods on this
     *                                     helper will have no effect until EmojiCompat.init is
     *                                     called.
     */
    public EmojiTextViewHelper(@NonNull TextView textView, boolean expectInitializedEmojiCompat) {
        Preconditions.checkNotNull(textView, "textView cannot be null");
        if (Build.VERSION.SDK_INT < 19) {
            mHelper = new HelperInternal();
        } else if (!expectInitializedEmojiCompat) {
            mHelper = new SkippingHelper19(textView);
        } else {
            mHelper = new HelperInternal19(textView);
        }
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
    @SuppressWarnings("ArrayReturn")
    @NonNull
    public InputFilter[] getFilters(
            @SuppressWarnings("ArrayReturn") @NonNull final InputFilter[] filters) {
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
     * When enabled, methods will have their documented behavior.
     *
     * When disabled, all methods will have no effect and emoji will not be processed.
     *
     * Setting this to disable will also have the side effect of setting both the transformation
     * method and filter if enabled has changed since the last call. By default
     * EmojiTextViewHelper is enabled.
     *
     * You do not need to call {@link EmojiTextViewHelper#updateTransformationMethod()} again after
     * calling setEnabled.
     *
     * @param enabled if this helper should process emoji.
     */
    public void setEnabled(boolean enabled) {
        mHelper.setEnabled(enabled);
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

    /**
     * @return current enabled state for this helper
     */
    public boolean isEnabled() {
        return mHelper.isEnabled();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static class HelperInternal {

        void updateTransformationMethod() {
            // do nothing
        }

        @NonNull
        InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
            return filters;
        }

        @Nullable
        TransformationMethod wrapTransformationMethod(
                @Nullable TransformationMethod transformationMethod) {
            return transformationMethod;
        }

        void setAllCaps(boolean allCaps) {
            // do nothing
        }

        void setEnabled(boolean processEmoji) {
            // do nothing
        }

        public boolean isEnabled() {
            return false;
        }
    }

    /**
     * This helper allows EmojiTextViewHelper to skip all calls to EmojiCompat until
     * {@link EmojiCompat#isConfigured()} returns true on devices that are 19+.
     *
     * When isConfigured returns true, this delegates to {@link HelperInternal19} to provide
     * EmojiCompat behavior. This has the effect of making EmojiCompat calls a "no-op" when
     * EmojiCompat is not configured on a device.
     *
     * There is no mechanism to be informed when isConfigured becomes true as it will lead to
     * likely memory leaks in situations where isConfigured never becomes true, and it is the
     * responsibility of the caller to call
     * {@link EmojiTextViewHelper#updateTransformationMethod()} after configuring EmojiCompat if
     * TextView's using EmojiTextViewHelper are already displayed to the user.
     */
    @RequiresApi(19)
    private static class SkippingHelper19 extends HelperInternal {
        private final HelperInternal19 mHelperDelegate;

        SkippingHelper19(TextView textView) {
            mHelperDelegate = new HelperInternal19(textView);
        }

        private boolean skipBecauseEmojiCompatNotInitialized() {
            return !EmojiCompat.isConfigured();
        }

        /**
         * {@inheritDoc}
         *
         * This method will have no effect if !{@link EmojiCompat#isConfigured()}
         */
        @Override
        void updateTransformationMethod() {
            if (skipBecauseEmojiCompatNotInitialized()) {
                return;
            }
            mHelperDelegate.updateTransformationMethod();
        }

        /**
         * {@inheritDoc}
         *
         * This method will have no effect if !{@link EmojiCompat#isConfigured()}
         */
        @NonNull
        @Override
        InputFilter[] getFilters(@NonNull InputFilter[] filters) {
            if (skipBecauseEmojiCompatNotInitialized()) {
                return filters;
            }
            return mHelperDelegate.getFilters(filters);
        }

        /**
         * {@inheritDoc}
         *
         * This method will have no effect if !{@link EmojiCompat#isConfigured()}
         */
        @Nullable
        @Override
        TransformationMethod wrapTransformationMethod(
                @Nullable TransformationMethod transformationMethod) {
            if (skipBecauseEmojiCompatNotInitialized()) {
                return transformationMethod;
            }
            return mHelperDelegate.wrapTransformationMethod(transformationMethod);
        }

        /**
         * {@inheritDoc}
         *
         * This method will have no effect if !{@link EmojiCompat#isConfigured()}
         */
        @Override
        void setAllCaps(boolean allCaps) {
            if (skipBecauseEmojiCompatNotInitialized()) {
                return;
            }
            mHelperDelegate.setAllCaps(allCaps);
        }

        /**
         * {@inheritDoc}
         *
         * This method will track enabled, but have no other effect if
         * !{@link EmojiCompat#isConfigured()}
         */
        @Override
        void setEnabled(boolean processEmoji) {
            if (skipBecauseEmojiCompatNotInitialized()) {
                mHelperDelegate.setEnabledUnsafe(processEmoji);
            } else {
                mHelperDelegate.setEnabled(processEmoji);
            }
        }

        @Override
        public boolean isEnabled() {
            return mHelperDelegate.isEnabled();
        }
    }

    @RequiresApi(19)
    private static class HelperInternal19 extends HelperInternal {
        private final TextView mTextView;
        private final EmojiInputFilter mEmojiInputFilter;
        private boolean mEnabled;

        HelperInternal19(TextView textView) {
            mTextView = textView;
            mEnabled = true;
            mEmojiInputFilter = new EmojiInputFilter(textView);
        }


        @Override
        void updateTransformationMethod() {
            // since this is not a pure function, we need to have a side effect for both enabled
            // and disabled
            final TransformationMethod tm =
                    wrapTransformationMethod(mTextView.getTransformationMethod());
            mTextView.setTransformationMethod(tm);
        }

        /**
         * Call whenever mEnabled changes
         */
        private void updateFilters() {
            InputFilter[] oldFilters = mTextView.getFilters();
            mTextView.setFilters(getFilters(oldFilters));
        }

        @NonNull
        @Override
        InputFilter[] getFilters(@NonNull final InputFilter[] filters) {
            if (!mEnabled) {
                // remove any EmojiInputFilter when disabled
                return removeEmojiInputFilterIfPresent(filters);
            } else {
                return addEmojiInputFilterIfMissing(filters);
            }
        }

        /**
         * Make sure that EmojiInputFilter is present in filters, or add it.
         *
         * @param filters to check
         * @return filters with mEmojiInputFilter added, if not previously present
         */
        @NonNull
        private InputFilter[] addEmojiInputFilterIfMissing(@NonNull InputFilter[] filters) {
            final int count = filters.length;
            for (int i = 0; i < count; i++) {
                if (filters[i] == mEmojiInputFilter) {
                    return filters;
                }
            }
            final InputFilter[] newFilters = new InputFilter[filters.length + 1];
            System.arraycopy(filters, 0, newFilters, 0, count);
            newFilters[count] = mEmojiInputFilter;
            return newFilters;
        }

        /**
         * Remove all EmojiInputFilter from filters
         *
         * @return filters.filter { it !== mEmojiInputFilter }
         */
        @NonNull
        private InputFilter[] removeEmojiInputFilterIfPresent(@NonNull InputFilter[] filters) {
            // find out the new size after removing (all) EmojiInputFilter
            SparseArray<InputFilter> filterSet = getEmojiInputFilterPositionArray(filters);
            if (filterSet.size() == 0) {
                return filters;
            }


            final int inCount = filters.length;
            int outCount = filters.length - filterSet.size();
            InputFilter[] result = new InputFilter[outCount];
            int destPosition = 0;
            for (int srcPosition = 0; srcPosition < inCount; srcPosition++) {
                if (filterSet.indexOfKey(srcPosition) < 0) {
                    result[destPosition] = filters[srcPosition];
                    destPosition++;
                }
            }
            return result;
        }

        /**
         * Populate a sparse array with true for all indexes that contain an EmojiInputFilter.
         */
        private SparseArray<InputFilter> getEmojiInputFilterPositionArray(
                @NonNull InputFilter[] filters) {
            SparseArray<InputFilter> result = new SparseArray<>(1);
            for (int pos = 0; pos < filters.length; pos++) {
                if (filters[pos] instanceof EmojiInputFilter) {
                    result.put(pos, filters[pos]);
                }
            }
            return result;
        }

        @Nullable
        @Override
        TransformationMethod wrapTransformationMethod(
                @Nullable TransformationMethod transformationMethod) {
            if (mEnabled) {
                return wrapForEnabled(transformationMethod);
            } else {
                return unwrapForDisabled(transformationMethod);
            }
        }

        /**
         * Unwrap EmojiTransformationMethods safely.
         */
        @Nullable
        private TransformationMethod unwrapForDisabled(
                @Nullable TransformationMethod transformationMethod) {
            if (transformationMethod instanceof EmojiTransformationMethod) {
                EmojiTransformationMethod etm =
                        (EmojiTransformationMethod) transformationMethod;
                return etm.getOriginalTransformationMethod();
            } else {
                return transformationMethod;
            }
        }

        /**
         * Wrap in EmojiTransformationMethod, but don't double wrap.
         *
         * This will not wrap {@link PasswordTransformationMethod}.
         */
        @NonNull
        private TransformationMethod wrapForEnabled(
                @Nullable TransformationMethod transformationMethod) {
            if (transformationMethod instanceof EmojiTransformationMethod) {
                return transformationMethod;
            } else if (transformationMethod instanceof PasswordTransformationMethod) {
                return transformationMethod;
            } else {
                return new EmojiTransformationMethod(transformationMethod);
            }
        }

        @Override
        void setAllCaps(boolean allCaps) {
            // When allCaps is set to false TextView sets the transformation method to be null. We
            // are only interested when allCaps is set to true in order to wrap the original method.
            if (allCaps) {
                updateTransformationMethod();
            }
        }

        @Override
        void setEnabled(boolean enabled) {
            mEnabled = enabled;
            updateTransformationMethod();
            updateFilters();
        }

        @Override
        public boolean isEnabled() {
            return mEnabled;
        }

        /**
         * Call to set enabled without side effects. Should only be used when EmojiCompat is not
         * initialized.
         *
         * @param processEmoji when true, this helper will process emoji
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        void setEnabledUnsafe(boolean processEmoji) {
            mEnabled = processEmoji;
        }
    }
}
