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
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.R;
import androidx.emoji2.viewsintegration.EmojiEditTextHelper;

/**
 * Helper for using EmojiCompat from TextView in appcompat.
 */
class AppCompatEmojiEditTextHelper {

    @NonNull
    private final EditText mView;
    @NonNull
    private final EmojiEditTextHelper mEmojiEditTextHelper;

    /**
     * Helper for integrating EmojiCompat into an EditText subclass.
     *
     * You should use this instead of {@link AppCompatEmojiTextHelper} for any classes that
     * subclass {@link EditText}.
     */
    AppCompatEmojiEditTextHelper(@NonNull EditText view) {
        mView = view;
        mEmojiEditTextHelper = new EmojiEditTextHelper(view,
                /* expectInitializedEmojiCompat */ false);
    }

    /**
     * Load enabled behavior based on {@link R.styleable#AppCompatTextView_emojiCompatEnabled}.
     *
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
     * Call from constructor to initialize key listener correctly.
     */
    void initKeyListener() {
        // setKeyListener will cause a reset both focusable and the inputType to the most basic
        // style for the key listener. Since we're calling this from the View constructor, this
        // will cause both focusable and inputType to reset from the XML attributes.
        // See: b/191061070 and b/188049943 for details
        //
        // We will only reset this during initKeyListener, and default to the platform behavior
        // for later calls to setKeyListener, to emulate the exact behavior that a regular
        // EditText would provide.
        boolean wasFocusable = mView.isFocusable();
        int inputType = mView.getInputType();
        mView.setKeyListener(mView.getKeyListener());
        // reset the input type and focusable attributes after calling setKeyListener
        mView.setInputType(inputType);
        mView.setFocusable(wasFocusable);
    }

    /**
     * When set to false, this helper will do no further emoji processing.
     *
     * Disabling emoji on an EditText does not trigger any further processing, and previously
     * added spans will remain.
     */
    void setEnabled(boolean enabled) {
        mEmojiEditTextHelper.setEnabled(enabled);
    }

    /**
     * @return current enabled state
     */
    boolean isEnabled() {
        return mEmojiEditTextHelper.isEnabled();
    }

    /**
     * Attaches EmojiCompat KeyListener to the widget. Should be called from {@link
     * TextView#setKeyListener(KeyListener)}. Existing keyListener is wrapped into EmojiCompat
     * KeyListener. When used on devices running API 18 or below, this method returns
     * {@code keyListener} that is given as a parameter.
     *
     * This should always be installed even when emoji processing is disabled, as it enables
     * correct behavior for editing existing emoji spans.
     *
     * @param keyListener KeyListener passed into {@link TextView#setKeyListener(KeyListener)}
     *
     * @return a new KeyListener instance that wraps {@code keyListener}.
     */
    @NonNull
    KeyListener getKeyListener(@NonNull KeyListener keyListener) {
        return mEmojiEditTextHelper.getKeyListener(keyListener);
    }

    /**
     * Updates the InputConnection with emoji support. Should be called from {@link
     * TextView#onCreateInputConnection(EditorInfo)}. When used on devices running API 18 or below,
     * this method returns {@code inputConnection} that is given as a parameter. If
     * {@code inputConnection} is {@code null}, returns {@code null}.
     *
     * This should always be installed even when emoji processing is disabled, as it enables
     * correct behavior for editing existing emoji spans.
     *
     * @param inputConnection InputConnection instance created by TextView
     * @param outAttrs        EditorInfo passed into
     *                        {@link TextView#onCreateInputConnection(EditorInfo)}
     *
     * @return a new InputConnection instance that wraps {@code inputConnection}
     */
    @Nullable
    InputConnection onCreateInputConnection(@Nullable InputConnection inputConnection,
            @NonNull EditorInfo outAttrs) {
        return mEmojiEditTextHelper.onCreateInputConnection(inputConnection, outAttrs);
    }
}
