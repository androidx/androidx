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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.text.emoji.EmojiCompat;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * EditText widget enhanced with emoji capability by using {@link EmojiEditTextHelper}. When used
 * on devices running API 18 or below, this widget acts as a regular {@link EditText}.
 *
 * @attr ref android.support.text.emoji.R.styleable#EmojiEditText_maxEmojiCount
 */
public class EmojiEditText extends EditText {
    private EmojiEditTextHelper mEmojiEditTextHelper;

    /**
     * Prevent calling {@link #init(AttributeSet, int, int)} multiple times in case super()
     * constructors call other constructors.
     */
    private boolean mInitialized;

    public EmojiEditText(Context context) {
        super(context);
        init(null /*attrs*/, 0 /*defStyleAttr*/, 0 /*defStyleRes*/);
    }

    public EmojiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, android.R.attr.editTextStyle, 0 /*defStyleRes*/);
    }

    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0 /*defStyleRes*/);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (!mInitialized) {
            mInitialized = true;
            final EditTextAttributeHelper attrHelper = new EditTextAttributeHelper(this, attrs,
                    defStyleAttr, defStyleRes);
            setMaxEmojiCount(attrHelper.getMaxEmojiCount());
            setKeyListener(super.getKeyListener());
        }
    }

    @Override
    public void setKeyListener(android.text.method.KeyListener keyListener) {
        super.setKeyListener(getEmojiEditTextHelper().getKeyListener(keyListener));
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        return getEmojiEditTextHelper().onCreateInputConnection(inputConnection, outAttrs);
    }

    /**
     * Set the maximum number of EmojiSpans to be added to a CharSequence. The number of spans in a
     * CharSequence affects the performance of the EditText insert/delete operations. Insert/delete
     * operations slow down as the number of spans increases.
     *
     * @param maxEmojiCount maximum number of EmojiSpans to be added to a single CharSequence,
     *                      should be equal or greater than 0
     *
     * @see EmojiCompat#process(CharSequence, int, int, int)
     *
     * @attr ref android.support.text.emoji.R.styleable#EmojiEditText_maxEmojiCount
     */
    public void setMaxEmojiCount(@IntRange(from = 0) int maxEmojiCount) {
        getEmojiEditTextHelper().setMaxEmojiCount(maxEmojiCount);
    }

    /**
     * Returns the maximum number of EmojiSpans to be added to a CharSequence.
     *
     * @see #setMaxEmojiCount(int)
     * @see EmojiCompat#process(CharSequence, int, int, int)
     *
     * @attr ref android.support.text.emoji.R.styleable#EmojiEditText_maxEmojiCount
     */
    public int getMaxEmojiCount() {
        return getEmojiEditTextHelper().getMaxEmojiCount();
    }

    private EmojiEditTextHelper getEmojiEditTextHelper() {
        if (mEmojiEditTextHelper == null) {
            mEmojiEditTextHelper = new EmojiEditTextHelper(this);
        }
        return mEmojiEditTextHelper;
    }
}
