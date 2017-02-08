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
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * EditText widget enhanced with emoji capability by using {@link EmojiEditTextHelper}.
 */
public class EmojiEditText extends EditText {
    private EmojiEditTextHelper mEmojiEditTextHelper;
    private boolean mInitialized;

    public EmojiEditText(Context context) {
        super(context);
        init();
    }

    public EmojiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (!mInitialized) {
            mInitialized = true;
            super.setKeyListener(getEmojiEditTextHelper().getKeyListener(getKeyListener()));
        }
    }

    @Override
    public void setKeyListener(android.text.method.KeyListener input) {
        super.setKeyListener(getEmojiEditTextHelper().getKeyListener(input));
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        return getEmojiEditTextHelper().onCreateInputConnection(inputConnection, outAttrs);
    }

    private EmojiEditTextHelper getEmojiEditTextHelper() {
        if (mEmojiEditTextHelper == null) {
            mEmojiEditTextHelper = new EmojiEditTextHelper(this);
        }
        return mEmojiEditTextHelper;
    }
}
