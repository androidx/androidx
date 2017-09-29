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
import android.support.text.emoji.EmojiCompat.InitCallback;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.widget.TextView;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * InputFilter to add EmojiSpans to the CharSequence set in a TextView. Unlike EditText where a
 * TextWatcher is used to enhance the CharSequence, InputFilter is used on TextView. The reason is
 * that if you add a TextWatcher to a TextView, its internal layout mechanism change, and therefore
 * depending on the CharSequence provided, adding a TextWatcher might have performance side
 * effects.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
final class EmojiInputFilter implements android.text.InputFilter {
    private final TextView mTextView;
    private InitCallback mInitCallback;

    EmojiInputFilter(@NonNull final TextView textView) {
        mTextView = textView;
    }

    @Override
    public CharSequence filter(final CharSequence source, final int sourceStart,
            final int sourceEnd, final Spanned dest, final int destStart, final int destEnd) {
        if (mTextView.isInEditMode()) {
            return source;
        }

        switch (EmojiCompat.get().getLoadState()){
            case EmojiCompat.LOAD_STATE_SUCCEEDED:
                boolean process = true;
                if (destEnd == 0 && destStart == 0 && dest.length() == 0) {
                    final CharSequence oldText = mTextView.getText();
                    if (source == oldText) {
                        process = false;
                    }
                }

                if (process && source != null) {
                    final CharSequence text;
                    if (sourceStart == 0 && sourceEnd == source.length()) {
                        text = source;
                    } else {
                        text = source.subSequence(sourceStart, sourceEnd);
                    }
                    return EmojiCompat.get().process(text, 0, text.length());
                }

                return source;
            case EmojiCompat.LOAD_STATE_LOADING:
                EmojiCompat.get().registerInitCallback(getInitCallback());
                return source;

            case EmojiCompat.LOAD_STATE_FAILED:
            default:
                return source;
        }
    }

    private InitCallback getInitCallback() {
        if (mInitCallback == null) {
            mInitCallback = new InitCallbackImpl(mTextView);
        }
        return mInitCallback;
    }

    private static class InitCallbackImpl extends InitCallback {
        private final Reference<TextView> mViewRef;

        InitCallbackImpl(TextView textView) {
            mViewRef = new WeakReference<>(textView);
        }

        @Override
        public void onInitialized() {
            super.onInitialized();
            final TextView textView = mViewRef.get();
            if (textView != null && textView.isAttachedToWindow()) {
                final CharSequence result = EmojiCompat.get().process(textView.getText());

                final int selectionStart = Selection.getSelectionStart(result);
                final int selectionEnd = Selection.getSelectionEnd(result);

                textView.setText(result);

                if (result instanceof Spannable) {
                    updateSelection((Spannable) result, selectionStart, selectionEnd);
                }
            }
        }
    }

    static void updateSelection(Spannable spannable, final int start, final int end) {
        if (start >= 0 && end >= 0) {
            Selection.setSelection(spannable, start, end);
        } else if (start >= 0) {
            Selection.setSelection(spannable, start);
        } else if (end >= 0) {
            Selection.setSelection(spannable, end);
        }
    }
}
