/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

/** TextRenderer tests. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class TextRendererTest {

    private TextRenderer mTextRenderer;

    @Before
    public void setUp() {
        mTextRenderer = new TextRenderer();
    }

    @Test
    public void applySpanAllowlistRemovesSpans() {
        SpannableStringBuilder builder = new SpannableStringBuilder("THIS IS STYLED!");

        Object foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        builder.setSpan(foregroundColorSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        Object alignmentSpan = new AlignmentSpan.Standard(Alignment.ALIGN_CENTER);
        builder.setSpan(alignmentSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        Object absoluteSizeSpan = new AbsoluteSizeSpan(10);
        builder.setSpan(absoluteSizeSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        Object superscriptSpan = new SuperscriptSpan();
        builder.setSpan(superscriptSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        Object subscriptSpan = new SubscriptSpan();
        builder.setSpan(subscriptSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        Spanned result = (Spanned) mTextRenderer.applySpanAllowlist(builder);

        assertThat(result.toString()).isEqualTo(builder.toString());
        assertThat(result.getSpans(0, result.length(), Object.class))
                .asList()
                .containsExactly(superscriptSpan, subscriptSpan, foregroundColorSpan);
    }

    @Test
    public void applySpannAllowlistNotSpanned() {
        String text = "some test text";

        assertTrue(mTextRenderer.applySpanAllowlist(text).toString().contentEquals(text));
    }
}
