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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.EmojiCompat.InitCallback;
import androidx.emoji2.text.EmojiDefaults;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * TextWatcher used for an EditText.
 *
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
final class EmojiTextWatcher implements android.text.TextWatcher {
    private final EditText mEditText;
    private final boolean mExpectInitializedEmojiCompat;
    private InitCallback mInitCallback;
    private int mMaxEmojiCount = EmojiDefaults.MAX_EMOJI_COUNT;
    @EmojiCompat.ReplaceStrategy
    private int mEmojiReplaceStrategy = EmojiCompat.REPLACE_STRATEGY_DEFAULT;
    private boolean mEnabled;

    EmojiTextWatcher(EditText editText, boolean expectInitializedEmojiCompat) {
        mEditText = editText;
        mExpectInitializedEmojiCompat = expectInitializedEmojiCompat;
        mEnabled = true;
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
        if (mEditText.isInEditMode() || shouldSkipForDisabledOrNotConfigured()) {
            return;
        }

        //before > after --> a deletion occurred
        if (before <= after && charSequence instanceof Spannable) {
            switch (EmojiCompat.get().getLoadState()){
                case EmojiCompat.LOAD_STATE_SUCCEEDED:
                    final Spannable s = (Spannable) charSequence;
                    EmojiCompat.get().process(s, start, start + after, mMaxEmojiCount,
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

    private boolean shouldSkipForDisabledOrNotConfigured() {
        return !mEnabled || (!mExpectInitializedEmojiCompat && !EmojiCompat.isConfigured());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        // do nothing
    }

    /**
     * @return
     */
    @RestrictTo(LIBRARY)
    InitCallback getInitCallback() {
        if (mInitCallback == null) {
            mInitCallback = new InitCallbackImpl(mEditText);
        }
        return mInitCallback;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        if (mEnabled != isEnabled) {
            if (mInitCallback != null) {
                EmojiCompat.get().unregisterInitCallback(mInitCallback);
            }
            mEnabled = isEnabled;
            if (mEnabled) {
                processTextOnEnablingEvent(mEditText, EmojiCompat.get().getLoadState());
            }
        }
    }

    @RestrictTo(LIBRARY)
    @RequiresApi(19)
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
            processTextOnEnablingEvent(editText, EmojiCompat.LOAD_STATE_SUCCEEDED);
        }
    }

    static void processTextOnEnablingEvent(@Nullable EditText editText, int currentLoadState) {
        if (currentLoadState == EmojiCompat.LOAD_STATE_SUCCEEDED && editText != null
                && editText.isAttachedToWindow()) {
            final Editable text = editText.getEditableText();

            final int selectionStart = Selection.getSelectionStart(text);
            final int selectionEnd = Selection.getSelectionEnd(text);

            EmojiCompat.get().process(text);

            EmojiInputFilter.updateSelection(text, selectionStart, selectionEnd);
        }
    }
}
