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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.widget.EditText;

import androidx.annotation.RestrictTo;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.EmojiCompat.InitCallback;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * TextWatcher used for an EditText.
 *
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
final class EmojiTextWatcher implements android.text.TextWatcher {
    private final EditText mEditText;
    private InitCallback mInitCallback;
    private int mMaxEmojiCount = EditTextAttributeHelper.MAX_EMOJI_COUNT;
    private int mStart = 0;
    private int mLength = 0;

    @EmojiCompat.ReplaceStrategy
    private int mEmojiReplaceStrategy = EmojiCompat.REPLACE_STRATEGY_DEFAULT;

    EmojiTextWatcher(EditText editText) {
        mEditText = editText;
    }

    void setMaxEmojiCount(int maxEmojiCount) {
        this.mMaxEmojiCount = maxEmojiCount;
    }

    int getMaxEmojiCount() {
        return mMaxEmojiCount;
    }

    @EmojiCompat.ReplaceStrategy int getEmojiReplaceStrategy() {
        return mEmojiReplaceStrategy;
    }

    void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
        mEmojiReplaceStrategy = replaceStrategy;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, final int start, final int before,
            final int after) {
        // do nothing
        mStart = start;
        if (before <= after) {
            mLength = after;
        } else {
            mLength = 0;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mEditText.isInEditMode()) {
            return;
        }

        int start = mStart;
        int length = mLength;

        //before > after --> a deletion occured
        if (length > 0) {
            switch (EmojiCompat.get().getLoadState()){
                case EmojiCompat.LOAD_STATE_SUCCEEDED:
                    EmojiCompat.get().process(s, start, start + length, mMaxEmojiCount,
                            mEmojiReplaceStrategy);
                    break;
                case EmojiCompat.LOAD_STATE_LOADING:
                case EmojiCompat.LOAD_STATE_DEFAULT:
                    EmojiCompat.get().registerInitCallback(getInitCallback());
                    break;
                case EmojiCompat.LOAD_STATE_FAILED:
                default:
                    break;
            }
        }
    }

    @RestrictTo(LIBRARY)
    InitCallback getInitCallback() {
        if (mInitCallback == null) {
            mInitCallback = new InitCallbackImpl(mEditText);
        }
        return mInitCallback;
    }

    @RestrictTo(LIBRARY)
    static class InitCallbackImpl extends InitCallback implements Runnable {
        private final Reference<EditText> mViewRef;

        InitCallbackImpl(EditText editText) {
            mViewRef = new WeakReference<>(editText);
        }

        @Override
        public void onInitialized() {
            super.onInitialized();
            final EditText editText = mViewRef.get();
            if (editText == null) {
                return;
            }
            Handler handler = editText.getHandler();
            if (handler == null) {
                return;
            }
            handler.post(this);
        }

        @Override
        public void run() {
            final EditText editText = mViewRef.get();
            if (editText != null && editText.isAttachedToWindow()) {
                final Editable text = editText.getEditableText();

                final int selectionStart = Selection.getSelectionStart(text);
                final int selectionEnd = Selection.getSelectionEnd(text);

                EmojiCompat.get().process(text);

                EmojiInputFilter.updateSelection(text, selectionStart, selectionEnd);
            }
        }
    }
}
