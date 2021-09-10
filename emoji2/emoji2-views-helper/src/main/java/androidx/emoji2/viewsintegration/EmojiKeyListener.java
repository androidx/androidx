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

import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.emoji2.text.EmojiCompat;

/**
 * KeyListener class to handle delete operations correctly.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
final class EmojiKeyListener implements android.text.method.KeyListener {
    private final android.text.method.KeyListener mKeyListener;
    private final EmojiCompatHandleKeyDownHelper mEmojiCompatHandleKeyDownHelper;

    EmojiKeyListener(android.text.method.KeyListener keyListener) {
        this(keyListener, new EmojiCompatHandleKeyDownHelper());
    }

    EmojiKeyListener(KeyListener keyListener,
            EmojiCompatHandleKeyDownHelper emojiCompatKeydownHelper) {
        mKeyListener = keyListener;
        mEmojiCompatHandleKeyDownHelper = emojiCompatKeydownHelper;
    }

    @Override
    public int getInputType() {
        return mKeyListener.getInputType();
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        final boolean result = mEmojiCompatHandleKeyDownHelper
                .handleKeyDown(content, keyCode, event);
        return result || mKeyListener.onKeyDown(view, content, keyCode, event);
    }

    @Override
    public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
        return mKeyListener.onKeyUp(view, text, keyCode, event);
    }

    @Override
    public boolean onKeyOther(View view, Editable text, KeyEvent event) {
        return mKeyListener.onKeyOther(view, text, event);
    }

    @Override
    public void clearMetaKeyState(View view, Editable content, int states) {
        mKeyListener.clearMetaKeyState(view, content, states);
    }

    public static class EmojiCompatHandleKeyDownHelper {
        public boolean handleKeyDown(@NonNull final Editable editable, final int keyCode,
                @NonNull final KeyEvent event) {
            return EmojiCompat.handleOnKeyDown(editable, keyCode, event);
        }
    }

}
