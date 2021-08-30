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
import android.text.method.KeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.EmojiDefaults;
import androidx.emoji2.text.EmojiSpan;

/**
 * Utility class to enhance custom EditText widgets with {@link EmojiCompat}.
 * <p/>
 * <pre>
 * public class MyEmojiEditText extends EditText {
 *      public MyEmojiEditText(Context context) {
 *          super(context);
 *          init();
 *      }
 *      // ...
 *      private void init() {
 *          super.setKeyListener(getEmojiEditTextHelper().getKeyListener(getKeyListener()));
 *      }
 *
 *      {@literal @}Override
 *      public void setKeyListener(android.text.method.KeyListener keyListener) {
 *          super.setKeyListener(getEmojiEditTextHelper().getKeyListener(keyListener));
 *      }
 *
 *      {@literal @}Override
 *      public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
 *          InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
 *          return getEmojiEditTextHelper().onCreateInputConnection(inputConnection, outAttrs);
 *      }
 *
 *      private EmojiEditTextHelper getEmojiEditTextHelper() {
 *          if (mEmojiEditTextHelper == null) {
 *              mEmojiEditTextHelper = new EmojiEditTextHelper(this);
 *          }
 *          return mEmojiEditTextHelper;
 *      }
 * }
 * </pre>
 *
 */
public final class EmojiEditTextHelper {
    private final HelperInternal mHelper;
    private int mMaxEmojiCount = EmojiDefaults.MAX_EMOJI_COUNT;
    @EmojiCompat.ReplaceStrategy
    private int mEmojiReplaceStrategy = EmojiCompat.REPLACE_STRATEGY_DEFAULT;

    /**
     * Default constructor.
     *
     * @param editText EditText instance
     */
    public EmojiEditTextHelper(@NonNull final EditText editText) {
        this(editText, /* expectInitializedEmojiCompat */true);
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
     * {@link EmojiEditTextHelper#setEnabled(boolean)}.
     *
     * @param editText EditText instance
     * @param expectInitializedEmojiCompat if true, this helper will assume init has been called
     *                                     and throw if it has not. If false, the methods on this
     *                                     helper will have no effect until EmojiCompat.init is
     *                                     called.
     */
    public EmojiEditTextHelper(@NonNull EditText editText,
            boolean expectInitializedEmojiCompat) {
        Preconditions.checkNotNull(editText, "editText cannot be null");
        if (Build.VERSION.SDK_INT < 19) {
            mHelper = new HelperInternal();
        } else {
            mHelper = new HelperInternal19(editText, expectInitializedEmojiCompat);
        }
    }

    /**
     * Set the maximum number of EmojiSpans to be added to a CharSequence. The number of spans in a
     * CharSequence affects the performance of the EditText insert/delete operations. Insert/delete
     * operations slow down as the number of spans increases.
     * <p/>
     *
     * @param maxEmojiCount maximum number of EmojiSpans to be added to a single CharSequence,
     *                      should be equal or greater than 0
     *
     * @see EmojiCompat#process(CharSequence, int, int, int)
     */
    public void setMaxEmojiCount(@IntRange(from = 0) int maxEmojiCount) {
        Preconditions.checkArgumentNonnegative(maxEmojiCount,
                "maxEmojiCount should be greater than 0");
        mMaxEmojiCount = maxEmojiCount;
        mHelper.setMaxEmojiCount(maxEmojiCount);
    }

    /**
     * Returns the maximum number of EmojiSpans to be added to a CharSequence.
     *
     * @see #setMaxEmojiCount(int)
     * @see EmojiCompat#process(CharSequence, int, int, int)
     */
    public int getMaxEmojiCount() {
        return mMaxEmojiCount;
    }

    /**
     * Attaches EmojiCompat KeyListener to the widget. Should be called from {@link
     * TextView#setKeyListener(KeyListener)}. Existing keyListener is wrapped into EmojiCompat
     * KeyListener. When used on devices running API 18 or below, this method returns
     * {@code keyListener} that is given as a parameter.
     *
     * @param keyListener KeyListener passed into {@link TextView#setKeyListener(KeyListener)}
     *
     * @return a new KeyListener instance that wraps {@code keyListener}, or null if passed null.
     */
    @SuppressWarnings("ExecutorRegistration")
    @Nullable
    public KeyListener getKeyListener(@Nullable final KeyListener keyListener) {
        return mHelper.getKeyListener(keyListener);
    }

    /**
     * Updates the InputConnection with emoji support. Should be called from {@link
     * TextView#onCreateInputConnection(EditorInfo)}. When used on devices running API 18 or below,
     * this method returns {@code inputConnection} that is given as a parameter. If
     * {@code inputConnection} is {@code null}, returns {@code null}.
     *
     * @param inputConnection InputConnection instance created by TextView
     * @param outAttrs        EditorInfo passed into
     *                        {@link TextView#onCreateInputConnection(EditorInfo)}
     *
     * @return a new InputConnection instance that wraps {@code inputConnection}
     */
    @Nullable
    public InputConnection onCreateInputConnection(@Nullable final InputConnection inputConnection,
            @NonNull final EditorInfo outAttrs) {
        if (inputConnection == null) return null;
        return mHelper.onCreateInputConnection(inputConnection, outAttrs);
    }

    /**
     * Sets whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @param replaceStrategy should be one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
        mEmojiReplaceStrategy = replaceStrategy;
        mHelper.setEmojiReplaceStrategy(replaceStrategy);
    }

    /**
     * Returns whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @return one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getEmojiReplaceStrategy() {
        return mEmojiReplaceStrategy;
    }

    /**
     * If this helper should add new EmojiSpans to display emoji using the emoji font when text
     * changes.
     *
     * @return true if the helper will process emoji spans.
     */
    public boolean isEnabled() {
        return mHelper.isEnabled();
    }

    /**
     * When helper is enabled, it will process text changes to add appropriate EmojiSpans for
     * display.
     *
     * When helper is disabled, it will not process text changes, but existing spans will not be
     * removed by disabling.
     *
     * @param isEnabled if this helper should process spans
     */
    public void setEnabled(boolean isEnabled) {
        mHelper.setEnabled(isEnabled);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static class HelperInternal {

        @Nullable
        KeyListener getKeyListener(@Nullable KeyListener keyListener) {
            return keyListener;
        }

        InputConnection onCreateInputConnection(@NonNull InputConnection inputConnection,
                @NonNull EditorInfo outAttrs) {
            return inputConnection;
        }

        void setMaxEmojiCount(int maxEmojiCount) {
            // do nothing
        }

        void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
            // do nothing
        }

        void setEnabled(boolean isEnabled) {
            // do nothing
        }

        boolean isEnabled() {
            return false;
        }
    }

    @RequiresApi(19)
    private static class HelperInternal19 extends HelperInternal {
        private final EditText mEditText;
        private final EmojiTextWatcher mTextWatcher;

        HelperInternal19(@NonNull EditText editText, boolean expectInitializedEmojiCompat) {
            mEditText = editText;
            mTextWatcher = new EmojiTextWatcher(mEditText, expectInitializedEmojiCompat);
            mEditText.addTextChangedListener(mTextWatcher);
            mEditText.setEditableFactory(EmojiEditableFactory.getInstance());
        }

        @Override
        void setMaxEmojiCount(int maxEmojiCount) {
            mTextWatcher.setMaxEmojiCount(maxEmojiCount);
        }

        @Override
        void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
            mTextWatcher.setEmojiReplaceStrategy(replaceStrategy);
        }

        @Override
        KeyListener getKeyListener(@Nullable final KeyListener keyListener) {
            if (keyListener instanceof EmojiKeyListener) {
                return keyListener;
            }
            if (keyListener == null) {
                // don't wrap null key listener, as developer has explicitly request that editing
                // be disabled (this causes keyboard and soft keyboard interactions to not be
                // possible, and the EmojiKeyListener is not required)
                return null;
            }
            // make a KeyListener as it's always correct even if disabled
            return new EmojiKeyListener(keyListener);
        }

        @Override
        InputConnection onCreateInputConnection(@NonNull final InputConnection inputConnection,
                @NonNull final EditorInfo outAttrs) {
            if (inputConnection instanceof EmojiInputConnection) {
                return inputConnection;
            }
            // make an EmojiInputConnection even when disabled, as we may become enabled before
            // input connection is closed and it incurs little overhead
            return new EmojiInputConnection(mEditText, inputConnection, outAttrs);
        }

        @Override
        void setEnabled(boolean isEnabled) {
            mTextWatcher.setEnabled(isEnabled);
            // EmojiKeyListener and EmojiInputConnection are just for processing existing spans,
            // and should be left enabled

            // EmojiEditableFactory is just an optimization and should be left enabled
        }

        @Override
        boolean isEnabled() {
            return mTextWatcher.isEnabled();
        }
    }
}
