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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.text.emoji.EmojiCompat;
import android.text.Editable;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;

/**
 * InputConnectionWrapper for EditText delete operations. Keyboard does not have knowledge about
 * emojis and therefore might send commands to delete a part of the emoji sequence which creates
 * invalid codeunits/getCodepointAt in the text.
 * <p/>
 * This class tries to correctly delete an emoji checking if there is an emoji span.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
final class EmojiInputConnection extends InputConnectionWrapper {
    private final TextView mTextView;

    EmojiInputConnection(
            @NonNull final TextView textView,
            @NonNull final InputConnection inputConnection,
            @NonNull final EditorInfo outAttrs) {
        super(inputConnection, false);
        mTextView = textView;
        EmojiCompat.get().updateEditorInfoAttrs(outAttrs);
    }

    @Override
    public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
        final boolean result = EmojiCompat.handleDeleteSurroundingText(this, getEditable(),
                beforeLength, afterLength, false /*inCodePoints*/);
        return result || super.deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(final int beforeLength,
            final int afterLength) {
        final boolean result = EmojiCompat.handleDeleteSurroundingText(this, getEditable(),
                beforeLength, afterLength, true /*inCodePoints*/);
        return result || super.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
    }

    private Editable getEditable() {
        return mTextView.getEditableText();
    }
}
