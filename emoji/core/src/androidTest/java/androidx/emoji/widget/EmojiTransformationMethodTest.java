/*
 * Copyright 2018 The Android Open Source Project
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

import static junit.framework.TestCase.assertSame;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static androidx.emoji.util.EmojiMatcher.sameCharSequence;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import androidx.emoji.text.EmojiCompat;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiTransformationMethodTest {

    private EmojiTransformationMethod mTransformationMethod;
    private TransformationMethod mWrappedTransformationMethod;
    private View mView;
    private EmojiCompat mEmojiCompat;
    private final String mTestString = "abc";

    @Before
    public void setup() {
        mEmojiCompat = mock(EmojiCompat.class);
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        when(mEmojiCompat.process(any(CharSequence.class))).thenAnswer(new Answer<CharSequence>() {
            @Override
            public CharSequence answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return new SpannableString((String) args[0]);
            }
        });
        EmojiCompat.reset(mEmojiCompat);

        mView = mock(View.class);
        when(mView.isInEditMode()).thenReturn(false);

        mWrappedTransformationMethod = mock(TransformationMethod.class);
        when(mWrappedTransformationMethod.getTransformation(any(CharSequence.class),
                any(View.class))).thenAnswer(new Answer<CharSequence>() {
            @Override
            public CharSequence answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });

        mTransformationMethod = new EmojiTransformationMethod(mWrappedTransformationMethod);
    }

    @Test
    public void testFilter_withNullSource() {
        assertNull(mTransformationMethod.getTransformation(null, mView));
        verify(mEmojiCompat, never()).process(any(CharSequence.class));
    }

    @Test(expected = NullPointerException.class)
    public void testFilter_withNullView() {
        mTransformationMethod.getTransformation("", null);
    }

    @Test
    public void testFilter_withNullTransformationMethod() {
        mTransformationMethod = new EmojiTransformationMethod(null);

        final CharSequence result = mTransformationMethod.getTransformation(mTestString, mView);

        assertTrue(TextUtils.equals(new SpannableString(mTestString), result));
        verify(mEmojiCompat, times(1)).process(sameCharSequence(mTestString));
    }

    @Test
    public void testFilter() {
        final CharSequence result = mTransformationMethod.getTransformation(mTestString, mView);

        assertTrue(TextUtils.equals(new SpannableString(mTestString), result));
        assertTrue(result instanceof Spannable);
        verify(mWrappedTransformationMethod, times(1)).getTransformation(
                sameCharSequence(mTestString), same(mView));
        verify(mEmojiCompat, times(1)).process(sameCharSequence(mTestString));
        verify(mEmojiCompat, never()).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testFilter_whenEmojiCompatLoading() {
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);

        final CharSequence result = mTransformationMethod.getTransformation(mTestString, mView);

        assertSame(mTestString, result);
        verify(mWrappedTransformationMethod, times(1)).getTransformation(
                sameCharSequence(mTestString), same(mView));
        verify(mEmojiCompat, never()).process(sameCharSequence(mTestString));
        verify(mEmojiCompat, never()).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testFilter_whenEmojiCompatLoadFailed() {
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_FAILED);

        final CharSequence result = mTransformationMethod.getTransformation(mTestString, mView);

        assertSame(mTestString, result);
        verify(mWrappedTransformationMethod, times(1)).getTransformation(
                sameCharSequence(mTestString), same(mView));
        verify(mEmojiCompat, never()).process(sameCharSequence(mTestString));
        verify(mEmojiCompat, never()).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    public void testFilter_withManualLoadStrategy() {
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_DEFAULT);

        final CharSequence result = mTransformationMethod.getTransformation(mTestString, mView);

        assertSame(mTestString, result);
        verify(mWrappedTransformationMethod, times(1)).getTransformation(
                sameCharSequence(mTestString), same(mView));
        verify(mEmojiCompat, never()).process(sameCharSequence(mTestString));
        verify(mEmojiCompat, never()).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }
}
