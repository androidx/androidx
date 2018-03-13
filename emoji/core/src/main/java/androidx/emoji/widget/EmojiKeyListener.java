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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.emoji.text.EmojiCompat;

/**
 * KeyListener class to handle delete operations correctly.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
final class EmojiKeyListener implements android.text.method.KeyListener {
    private final android.text.method.KeyListener mKeyListener;

    EmojiKeyListener(android.text.method.KeyListener keyListener) {
        mKeyListener = keyListener;
    }

    @Override
    public int getInputType() {
        return mKeyListener.getInputType();
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        final boolean result = EmojiCompat.handleOnKeyDown(content, keyCode, event);
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
}
