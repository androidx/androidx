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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.emoji2.text.EmojiCompat;

/**
 * InputConnectionWrapper for EditText delete operations. Keyboard does not have knowledge about
 * emojis and therefore might send commands to delete a part of the emoji sequence which creates
 * invalid codeunits/getCodepointAt in the text.
 * <p/>
 * This class tries to correctly delete an emoji checking if there is an emoji span.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
final class EmojiInputConnection extends InputConnectionWrapper {
    private final TextView mTextView;
    private final EmojiCompatDeleteHelper mEmojiCompatDeleteHelper;

    EmojiInputConnection(
            @NonNull final TextView textView,
            @NonNull final InputConnection inputConnection,
            @NonNull final EditorInfo outAttrs) {
        this(textView, inputConnection, outAttrs, new EmojiCompatDeleteHelper());
    }

    EmojiInputConnection(
            @NonNull final TextView textView,
            @NonNull final InputConnection inputConnection,
            @NonNull final EditorInfo outAttrs,
            @NonNull final EmojiCompatDeleteHelper emojiCompatDeleteHelper
    ) {
        super(inputConnection, false);
        mTextView = textView;
        mEmojiCompatDeleteHelper = emojiCompatDeleteHelper;
        mEmojiCompatDeleteHelper.updateEditorInfoAttrs(outAttrs);
    }

    @Override
    public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
        final boolean result = mEmojiCompatDeleteHelper.handleDeleteSurroundingText(
                this, getEditable(), beforeLength, afterLength, false /*inCodePoints*/);
        return result || super.deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(final int beforeLength,
            final int afterLength) {
        final boolean result = mEmojiCompatDeleteHelper.handleDeleteSurroundingText(
                this, getEditable(), beforeLength, afterLength, true /*inCodePoints*/);
        return result || super.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
    }

    private Editable getEditable() {
        return mTextView.getEditableText();
    }

    public static class EmojiCompatDeleteHelper {
        public boolean handleDeleteSurroundingText(
                @NonNull final InputConnection inputConnection,
                @NonNull final Editable editable,
                @IntRange(from = 0) final int beforeLength,
                @IntRange(from = 0) final int afterLength,
                final boolean inCodePoints) {
            return EmojiCompat.handleDeleteSurroundingText(inputConnection, editable,
                    beforeLength, afterLength, inCodePoints);
        }

        public void updateEditorInfoAttrs(@NonNull final EditorInfo outAttrs) {
            if (EmojiCompat.isConfigured()) {
                EmojiCompat.get().updateEditorInfoAttrs(outAttrs);
            }
        }
    }
}
