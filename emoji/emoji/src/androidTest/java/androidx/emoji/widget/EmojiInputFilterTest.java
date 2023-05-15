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

import static androidx.emoji.util.EmojiMatcher.sameCharSequence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.EditText;
import android.widget.TextView;

import androidx.emoji.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EmojiInputFilterTest {

    private EmojiInputFilter mInputFilter;
    private EmojiCompat mEmojiCompat;

    @Before
    public void setup() {
        final TextView textView = mock(TextView.class);
        mEmojiCompat = mock(EmojiCompat.class);
        EmojiCompat.reset(mEmojiCompat);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        mInputFilter = new EmojiInputFilter(textView);
    }

    @Test
    public void testFilter_withNullSource() {
        assertNull(mInputFilter.filter(null, 0, 1, null, 0, 1));
        verify(mEmojiCompat, never()).process(any(CharSequence.class));
        verify(mEmojiCompat, never()).process(any(CharSequence.class), anyInt(), anyInt());
    }

    @Test
    public void testFilter_withString() {
        final String testString = "abc";
        when(mEmojiCompat.process(any(CharSequence.class), anyInt(), anyInt()))
                .thenReturn(new SpannableString(testString));
        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        assertTrue(result instanceof Spannable);
        verify(mEmojiCompat, times(1)).process(sameCharSequence("a"), eq(0), eq(1));
    }

    @Test
    public void testFilter_withSpannable() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.process(any(Spannable.class), anyInt(), anyInt())).thenReturn(testString);

        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        assertSame(result, testString);
        verify(mEmojiCompat, times(1)).process(sameCharSequence(testString.subSequence(0, 1)),
                eq(0), eq(1));
    }

    @Test
    public void testFilter_whenEmojiCompatLoading() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);

        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        assertSame(result, testString);
        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt());
        verify(mEmojiCompat, times(1)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testFilter_whenEmojiCompatLoadFailed() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_FAILED);

        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt());
        verify(mEmojiCompat, times(0)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testFilter_withManualLoadStrategy() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_DEFAULT);

        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        verify(mEmojiCompat, times(0)).process(any(Spannable.class), anyInt(), anyInt());
        verify(mEmojiCompat, times(1)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void initCallback_doesntCrashWhenNotAttached() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        EditText editText = new EditText(context);
        EmojiInputFilter subject = new EmojiInputFilter(editText);
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
        EmojiInputFilter subject = new EmojiInputFilter(mockEditText);
        EmojiInputFilter.InitCallbackImpl initCallback =
                (EmojiInputFilter.InitCallbackImpl) subject.getInitCallback();
        initCallback.onInitialized();

        handler.hasCallbacks(initCallback);
    }
}
