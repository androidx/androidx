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
import android.text.method.KeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Utility class to enhance an EditText with emoji capability.
 */
public final class EmojiEditTextHelper {
    private final EditText mEditText;
    private final EmojiTextWatcher mTextWatcher;

    /**
     * Default constructor.
     *
     * @param editText EditText instance
     */
    public EmojiEditTextHelper(@NonNull final EditText editText) {
        Preconditions.checkNotNull(editText, "editText cannot be null");
        mEditText = editText;
        mTextWatcher = new EmojiTextWatcher(mEditText);
        editText.addTextChangedListener(mTextWatcher);
        editText.setEditableFactory(EmojiEditableFactory.getInstance());
    }

    /**
     * Attaches EmojiCompat KeyListener to the widget. Should be called from {@link
     * TextView#setKeyListener(KeyListener)}. Existing keyListener is wrapped into EmojiCompat
     * KeyListener.
     * <p/>
     * <pre><code> {@literal @}Override
     * public void setKeyListener(android.text.method.KeyListener input) {
     *     super.setKeyListener(getEmojiEditTextHelper().getKeyListener(input));
     * }</code></pre>
     *
     * @param keyListener KeyListener passed into {@link TextView#setKeyListener(KeyListener)}
     *
     * @return a new KeyListener instance that wraps {@code keyListener}.
     */

    public KeyListener getKeyListener(@NonNull final KeyListener keyListener) {
        Preconditions.checkNotNull(keyListener, "keyListener cannot be null");
        return new EmojiKeyListener(keyListener);
    }

    /**
     * Updates the InputConnection with emoji support. Should be called from {@link
     * TextView#onCreateInputConnection(EditorInfo)}.
     * <p/>
     * <pre><code> {@literal @}Override
     * public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
     *     InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
     *     return getEmojiHelper().onCreateInputConnection(inputConnection, outAttrs);
     * }</code></pre>
     *
     * @param inputConnection InputConnection instance created by TextView
     * @param outAttrs        EditorInfo passed into
     *                        {@link TextView#onCreateInputConnection(EditorInfo)}
     *
     * @return a new InputConnection instance that wraps {@code inputConnection}
     */
    public InputConnection onCreateInputConnection(@NonNull final InputConnection inputConnection,
            @NonNull final EditorInfo outAttrs) {
        Preconditions.checkNotNull(inputConnection, "inputConnection cannot be null");
        return new EmojiInputConnection(mEditText, inputConnection, outAttrs);
    }
}
