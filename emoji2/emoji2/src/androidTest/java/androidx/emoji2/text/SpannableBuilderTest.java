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
package androidx.emoji2.text;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.QuoteSpan;
import android.text.style.TypefaceSpan;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SpannableBuilderTest {

    private TextWatcher mWatcher;
    private Class<?> mClass;

    @Before
    public void setup() {
        mWatcher = mock(TextWatcher.class, withSettings().extraInterfaces(SpanWatcher.class));
        mClass = mWatcher.getClass();
    }

    @Test
    public void testConstructor() {
        new SpannableBuilder(mClass);

        new SpannableBuilder(mClass, "abc");

        new SpannableBuilder(mClass, "abc", 0, 3);

        // test spannable copying? do I need it?
    }

    @Test
    public void testSubSequence() {
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "abc");
        final QuoteSpan span1 = mock(QuoteSpan.class);
        final QuoteSpan span2 = mock(QuoteSpan.class);
        spannable.setSpan(span1, 0, 1, SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(span2, 2, 3, SPAN_EXCLUSIVE_EXCLUSIVE);

        final CharSequence subsequence = spannable.subSequence(0, 1);
        assertNotNull(subsequence);
        assertThat(subsequence, instanceOf(SpannableBuilder.class));

        final QuoteSpan[] spans = spannable.getSpans(0, 1, QuoteSpan.class);
        assertThat(spans, arrayWithSize(1));
        assertSame(spans[0], span1);
    }

    @Test
    public void testSetAndGetSpan() {
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "abcde");
        spannable.setSpan(mWatcher, 1, 2, SPAN_INCLUSIVE_INCLUSIVE);

        // getSpans should return the span
        Object[] spans = spannable.getSpans(0, spannable.length(), mClass);
        assertNotNull(spans);
        assertThat(spans, arrayWithSize(1));
        assertSame(mWatcher, spans[0]);

        // span attributes should be correct
        assertEquals(1, spannable.getSpanStart(mWatcher));
        assertEquals(2, spannable.getSpanEnd(mWatcher));
        assertEquals(SPAN_INCLUSIVE_INCLUSIVE, spannable.getSpanFlags(mWatcher));

        // should remove the span
        spannable.removeSpan(mWatcher);
        spans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);
        assertNotNull(spans);
        assertThat(spans, arrayWithSize(0));
    }

    @Test
    public void testNextSpanTransition() {
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "abcde");
        spannable.setSpan(mWatcher, 1, 2, SPAN_INCLUSIVE_INCLUSIVE);
        final int start = spannable.nextSpanTransition(0, spannable.length(), mClass);
        assertEquals(1, start);
    }

    @Test
    public void testBlocksSpanCallbacks_forEmojiSpans() {
        final EmojiSpan span = mock(EmojiSpan.class);
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "123456");
        spannable.setSpan(mWatcher, 0, spannable.length(), SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(span, 1, 2, SPAN_EXCLUSIVE_EXCLUSIVE);
        reset(mWatcher);

        spannable.delete(0, 3);

        // verify that characters are deleted
        assertEquals("456", spannable.toString());
        // verify EmojiSpan is deleted
        EmojiSpan[] spans = spannable.getSpans(0, spannable.length(), EmojiSpan.class);
        assertThat(spans, arrayWithSize(0));

        // verify the call to span callbacks are blocked
        verify((SpanWatcher) mWatcher, never()).onSpanRemoved(any(Spannable.class),
                same(span), anyInt(), anyInt());
        verify((SpanWatcher) mWatcher, never()).onSpanAdded(any(Spannable.class),
                same(span), anyInt(), anyInt());
        verify((SpanWatcher) mWatcher, never()).onSpanChanged(any(Spannable.class),
                same(span), anyInt(), anyInt(), anyInt(), anyInt());

        // verify the call to TextWatcher callbacks are called
        verify(mWatcher, times(1)).beforeTextChanged(any(CharSequence.class), anyInt(),
                anyInt(), anyInt());
        verify(mWatcher, times(1)).onTextChanged(any(CharSequence.class), anyInt(), anyInt(),
                anyInt());
        verify(mWatcher, times(1)).afterTextChanged(any(Editable.class));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDoesNotBlockSpanCallbacks_forNonEmojiSpans() {
        final QuoteSpan span = mock(QuoteSpan.class);
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "123456");
        spannable.setSpan(mWatcher, 0, spannable.length(), SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(span, 1, 2, SPAN_EXCLUSIVE_EXCLUSIVE);
        reset(mWatcher);

        spannable.delete(0, 3);

        // verify that characters are deleted
        assertEquals("456", spannable.toString());
        // verify QuoteSpan is deleted
        QuoteSpan[] spans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);
        assertThat(spans, arrayWithSize(0));

        // verify the call to span callbacks are not blocked
        verify((SpanWatcher) mWatcher, times(1)).onSpanRemoved(any(Spannable.class),
                anyObject(), anyInt(), anyInt());

        // verify the call to TextWatcher callbacks are called
        verify(mWatcher, times(1)).beforeTextChanged(any(CharSequence.class), anyInt(), anyInt(),
                anyInt());
        verify(mWatcher, times(1)).onTextChanged(any(CharSequence.class), anyInt(), anyInt(),
                anyInt());
        verify(mWatcher, times(1)).afterTextChanged(any(Editable.class));
    }

    @Test
    public void testDoesNotBlockSpanCallbacksForOtherWatchers() {
        final TextWatcher textWatcher = mock(TextWatcher.class);
        final SpanWatcher spanWatcher = mock(SpanWatcher.class);

        final EmojiSpan span = mock(EmojiSpan.class);
        final SpannableBuilder spannable = new SpannableBuilder(mClass, "123456");
        spannable.setSpan(textWatcher, 0, spannable.length(), SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(spanWatcher, 0, spannable.length(), SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(span, 1, 2, SPAN_EXCLUSIVE_EXCLUSIVE);
        reset(textWatcher);

        spannable.delete(0, 3);

        // verify that characters are deleted
        assertEquals("456", spannable.toString());
        // verify EmojiSpan is deleted
        EmojiSpan[] spans = spannable.getSpans(0, spannable.length(), EmojiSpan.class);
        assertThat(spans, arrayWithSize(0));

        // verify the call to span callbacks are blocked
        verify(spanWatcher, times(1)).onSpanRemoved(any(Spannable.class), same(span),
                anyInt(), anyInt());

        // verify the call to TextWatcher callbacks are called
        verify(textWatcher, times(1)).beforeTextChanged(any(CharSequence.class), anyInt(),
                anyInt(), anyInt());
        verify(textWatcher, times(1)).onTextChanged(any(CharSequence.class), anyInt(), anyInt(),
                anyInt());
        verify(textWatcher, times(1)).afterTextChanged(any(Editable.class));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27) /* this is for testing before P crash */
    @SuppressWarnings("deprecation")
    public void testSpanRangeOldValueCalculation() throws Exception {
        // this test is ported from CTS
        // DynamicLayoutTest.testReflow_afterSpanChangedShouldNotThrowException
        String className = "android.text.DynamicLayout$ChangeWatcher";
        Class<?> watcherClass = Class.forName(className, false,
                getClass().getClassLoader());
        SpannableBuilder builder = new SpannableBuilder(watcherClass, "crash crash crash!!");

        TypefaceSpan span = mock(TypefaceSpan.class);
        builder.setSpan(span, 1, 4, SPAN_EXCLUSIVE_EXCLUSIVE);

        // this DynamicLayout causes the replace/insert before position 1 below to crash prior to P
        new DynamicLayout(builder, new TextPaint(), Integer.MAX_VALUE,
                Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, true);
        try {
            builder.replace(1, 1, "Hello there\n\n");
        } catch (Throwable e) {
            throw new RuntimeException("Inserting text into DynamicLayout should not crash", e);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testSpanRangeOldValueCalculation_androidPandLater() throws Exception {
        // this test is ported from CTS
        // DynamicLayoutTest.testReflow_afterSpanChangedShouldNotThrowException
        String className = "android.text.DynamicLayout$ChangeWatcher";
        Class<?> watcherClass = Class.forName(className, false,
                getClass().getClassLoader());
        SpannableBuilder builder = new SpannableBuilder(watcherClass, "crash crash crash!!");

        TypefaceSpan span = mock(TypefaceSpan.class);
        builder.setSpan(span, 1, 4, SPAN_EXCLUSIVE_EXCLUSIVE);

        // this DynamicLayout causes the replace/insert before position 1 below to crash prior to P
        DynamicLayout.Builder.obtain(builder, new TextPaint(), Integer.MAX_VALUE).build();
        try {
            builder.replace(1, 1, "Hello there\n\n");
        } catch (Throwable e) {
            throw new RuntimeException("Inserting text into DynamicLayout should not crash", e);
        }
    }
}
