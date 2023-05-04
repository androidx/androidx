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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.EditText;
import android.widget.TextView;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.util.EmojiMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiInputFilterTest {

    private EmojiInputFilter mInputFilter;
    private EmojiCompat mEmojiCompat;
    private TextView mTextView;

    @Before
    public void setup() {
        mTextView = mock(TextView.class);
        mEmojiCompat = mock(EmojiCompat.class);
        EmojiCompat.reset(mEmojiCompat);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        mInputFilter = new EmojiInputFilter(mTextView);
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
        verify(mEmojiCompat, times(1)).process(
                EmojiMatcher.sameCharSequence("a"), eq(0), eq(1));
    }

    @Test
    public void testFilter_withSpannable() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.process(any(Spannable.class), anyInt(), anyInt())).thenReturn(testString);

        final CharSequence result = mInputFilter.filter(testString, 0, 1, null, 0, 1);

        assertNotNull(result);
        assertSame(result, testString);
        verify(mEmojiCompat, times(1)).process(
                EmojiMatcher.sameCharSequence(testString.subSequence(0, 1)),
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
    public void emojiInputFilterAdded_thenRemoved_beforeEmojiCompatInit_doesntProcess() {
        mEmojiCompat = mock(EmojiCompat.class);
        mTextView = mock(TextView.class);
        when(mTextView.isAttachedToWindow()).thenReturn(true);
        EmojiCompat.reset(mEmojiCompat);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
        mInputFilter = new EmojiInputFilter(mTextView);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = mInputFilter;
        when(mTextView.getFilters()).thenReturn(filters);

        // first ensure the input filter registers a callback
        SpannableString testString = new SpannableString("abc");
        mInputFilter.filter(testString, 0, 1,
                null, 0, 1);

        ArgumentCaptor<EmojiCompat.InitCallback> captor = ArgumentCaptor
                .forClass(EmojiCompat.InitCallback.class);
        verify(mEmojiCompat).registerInitCallback(captor.capture());
        reset(mEmojiCompat);

        // then "disable" the input filter
        when(mTextView.getFilters()).thenReturn(new InputFilter[0]);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        // trigger initialized now that we've removed the input filter
        captor.getValue().onInitialized();

        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void emojiInputFilterAdded_beforeEmojiCompatInit_callsProcess() {
        mEmojiCompat = mock(EmojiCompat.class);
        mTextView = mock(TextView.class);
        when(mTextView.isAttachedToWindow()).thenReturn(true);
        EmojiCompat.reset(mEmojiCompat);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
        mInputFilter = new EmojiInputFilter(mTextView);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = mInputFilter;
        when(mTextView.getFilters()).thenReturn(filters);

        // first ensure the input filter registers a callback
        SpannableString testString = new SpannableString("abc");
        when(mTextView.getText()).thenReturn(testString);
        mInputFilter.filter(testString, 0, 1,
                null, 0, 1);

        ArgumentCaptor<EmojiCompat.InitCallback> captor = ArgumentCaptor
                .forClass(EmojiCompat.InitCallback.class);
        verify(mEmojiCompat).registerInitCallback(captor.capture());

        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        // trigger initialized
        ((Runnable) captor.getValue()).run();

        verify(mEmojiCompat).process(eq(testString));
    }

    @Test
    public void emojiInputFilterAdded_beforeEmojiCompatInit_doesntCallSetText_ifSameString() {
        mEmojiCompat = mock(EmojiCompat.class);
        mTextView = mock(TextView.class);
        EmojiCompat.reset(mEmojiCompat);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
        mInputFilter = new EmojiInputFilter(mTextView);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = mInputFilter;

        // first ensure the input filter registers a callback
        SpannableString testString = new SpannableString("abc");
        mInputFilter.filter(testString, 0, 1, null, 0, 1);

        ArgumentCaptor<EmojiCompat.InitCallback> captor = ArgumentCaptor
                .forClass(EmojiCompat.InitCallback.class);
        verify(mEmojiCompat).registerInitCallback(captor.capture());

        reset(mTextView);
        when(mTextView.isAttachedToWindow()).thenReturn(true);
        when(mTextView.getText()).thenReturn(testString);
        when(mTextView.getFilters()).thenReturn(filters);

        // don't wrap the string
        when(mEmojiCompat.process(eq(testString))).thenReturn(testString);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        // trigger initialized
        ((Runnable) captor.getValue()).run();

        // validate interactions don't do anything except check for update
        verify(mTextView).getFilters();
        verify(mTextView).isAttachedToWindow();
        verify(mTextView).getText();
        // any other interactions fail this test because they _may_ be destructive on a TextView
        // if you add a safe interaction please update test
        verifyNoMoreInteractions(mTextView);
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
