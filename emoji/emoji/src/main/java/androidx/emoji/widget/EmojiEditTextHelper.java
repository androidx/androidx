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
package androidx.emoji.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.text.method.KeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.EmojiSpan;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private int mMaxEmojiCount = EditTextAttributeHelper.MAX_EMOJI_COUNT;
    @EmojiCompat.ReplaceStrategy
    private int mEmojiReplaceStrategy = EmojiCompat.REPLACE_STRATEGY_DEFAULT;
    private final EditText mEditText;
    private final EmojiTextWatcher mTextWatcher;

    /**
     * Default constructor.
     *
     * @param editText EditText instance
     */
    public EmojiEditTextHelper(final @NonNull EditText editText) {
        Preconditions.checkNotNull(editText, "editText cannot be null");
        mEditText = editText;
        mTextWatcher = new EmojiTextWatcher(mEditText);
        mEditText.addTextChangedListener(mTextWatcher);
        mEditText.setEditableFactory(EmojiEditableFactory.getInstance());
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
        mTextWatcher.setMaxEmojiCount(maxEmojiCount);
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
     * @return a new KeyListener instance that wraps {@code keyListener}.
     */
    public @NonNull KeyListener getKeyListener(final @NonNull KeyListener keyListener) {
        Preconditions.checkNotNull(keyListener, "keyListener cannot be null");
        if (keyListener instanceof EmojiKeyListener) {
            return keyListener;
        }
        return new EmojiKeyListener(keyListener);
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
    public @Nullable InputConnection onCreateInputConnection(
            final @Nullable InputConnection inputConnection, final @NonNull EditorInfo outAttrs) {
        if (inputConnection == null) return null;
        if (inputConnection instanceof EmojiInputConnection) {
            return inputConnection;
        }
        return new EmojiInputConnection(mEditText, inputConnection, outAttrs);
    }

    /**
     * Sets whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @param replaceStrategy should be one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
        mEmojiReplaceStrategy = replaceStrategy;
        mTextWatcher.setEmojiReplaceStrategy(replaceStrategy);
    }

    /**
     * Returns whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @return one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int getEmojiReplaceStrategy() {
        return mEmojiReplaceStrategy;
    }
}
