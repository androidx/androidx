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

package androidx.emoji.text;

import static androidx.emoji.util.Emoji.EMOJI_SINGLE_CODEPOINT;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.emoji.util.TestString;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiSpanInstrumentationTest {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(
            TestActivity.class);
    private Instrumentation mInstrumentation;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testGetSize_withRelativeSizeSpan() {
        final TestActivity activity = mActivityRule.getActivity();
        final TextView textView = (TextView) activity.findViewById(
                androidx.emoji.test.R.id.text);

        // create a string with single codepoint emoji
        final TestString string = new TestString(EMOJI_SINGLE_CODEPOINT).withPrefix().withSuffix();
        final CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertNotNull(charSequence);
        assertThat(charSequence, hasEmojiCount(1));

        final Spannable spanned = (Spannable) charSequence;
        final EmojiSpan[] spans = spanned.getSpans(0, charSequence.length(), EmojiSpan.class);
        final EmojiSpan span = spans[0];

        // set text to the charSequence with the EmojiSpan
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(charSequence);
            }
        });
        mInstrumentation.waitForIdleSync();

        // record height of the default span
        final int defaultHeight = span.getHeight();

        // cover the charsequence with RelativeSizeSpan which will triple the size of the
        // characters.
        final float multiplier = 3.0f;
        final RelativeSizeSpan sizeSpan = new RelativeSizeSpan(multiplier);
        spanned.setSpan(sizeSpan, 0, charSequence.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // set the new text
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(charSequence);
            }
        });
        mInstrumentation.waitForIdleSync();

        // record the height measured after RelativeSizeSpan
        final int heightWithRelativeSpan = span.getHeight();

        // accept 1sp error rate.
        final float delta = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1,
                mInstrumentation.getTargetContext().getResources().getDisplayMetrics());
        assertEquals(defaultHeight * 3, heightWithRelativeSpan, delta);
    }
}
