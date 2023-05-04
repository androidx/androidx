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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.util.EmojiMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19) // class is not instantiated prior to API19
public class EmojiTextWatcherTest {

    private EmojiTextWatcher mTextWatcher;
    private EmojiCompat mEmojiCompat;

    @Before
    public void setup() {
        EditText editText = mock(EditText.class);
        mEmojiCompat = mock(EmojiCompat.class);
        EmojiCompat.reset(mEmojiCompat);
        mTextWatcher = new EmojiTextWatcher(editText, /* expectInitializedEmojiCompat */ true);
    }

    @Test
    public void testOnTextChanged_callsProcess() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(1)).process(
                EmojiMatcher.sameCharSequence(testString),
                eq(0),
                eq(1),
                eq(Integer.MAX_VALUE),
                anyInt());
        verify(mEmojiCompat, times(0)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testOnTextChanged_whenEmojiCompatLoading() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt(), anyInt(),
                anyInt());
        verify(mEmojiCompat, times(1)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testOnTextChanged_whenEmojiCompatLoadFailed() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_FAILED);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt(), anyInt(),
                anyInt());
        verify(mEmojiCompat, times(0)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testSetEmojiReplaceStrategy() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        assertEquals(EmojiCompat.REPLACE_STRATEGY_DEFAULT, mTextWatcher.getEmojiReplaceStrategy());

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(1)).process(any(Spannable.class), anyInt(), anyInt(), anyInt(),
                eq(EmojiCompat.REPLACE_STRATEGY_DEFAULT));

        mTextWatcher.setEmojiReplaceStrategy(EmojiCompat.REPLACE_STRATEGY_ALL);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(1)).process(any(Spannable.class), anyInt(), anyInt(), anyInt(),
                eq(EmojiCompat.REPLACE_STRATEGY_ALL));
    }

    @Test
    public void testFilter_withManualLoadStrategy() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_DEFAULT);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt());
        verify(mEmojiCompat, times(1)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void afterDisable_noFurtherInteractions() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        mTextWatcher.setEnabled(/* isEnabled */ false);
        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void whenNotConfigured_andNotExpectInitialized_noCalls() {
        EditText editText = mock(EditText.class);
        EmojiCompat.reset((EmojiCompat) null);
        mTextWatcher = new EmojiTextWatcher(editText, /* expectInitializedEmojiCompat */ false);
        SpannableString expected = new SpannableString("abc");
        mTextWatcher.onTextChanged(expected, 0, 0, 1);
        assertTrue(TextUtils.equals(expected, "abc"));
    }

    @Test
    public void initCallback_doesntCrashWhenNotAttached() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        EditText editText = new EditText(context);
        EmojiTextWatcher subject = new EmojiTextWatcher(editText, false);
        subject.getInitCallback().onInitialized();
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void initCallback_sendsToNonMainHandler_beforeSetText() {
        // this is just testing that onInitialized dispatches to editText.getHandler before setText
        EditText mockEditText = mock(EditText.class);
        HandlerThread thread = new HandlerThread("random thread");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        thread.quitSafely();
        when(mockEditText.getHandler()).thenReturn(handler);
        EmojiTextWatcher subject = new EmojiTextWatcher(mockEditText, false);
        EmojiTextWatcher.InitCallbackImpl initCallback =
                (EmojiTextWatcher.InitCallbackImpl) subject.getInitCallback();
        initCallback.onInitialized();

        handler.hasCallbacks(initCallback);
    }
}
