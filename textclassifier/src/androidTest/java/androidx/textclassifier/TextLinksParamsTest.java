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
package androidx.textclassifier;

import static androidx.textclassifier.TextClassifier.TYPE_ADDRESS;
import static androidx.textclassifier.TextClassifier.TYPE_OTHER;
import static androidx.textclassifier.TextClassifier.TYPE_PHONE;

import static com.google.common.truth.Truth.assertThat;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

@MediumTest
public class TextLinksParamsTest {

    private Map<String, Float> mDummyEntityScores;
    @Mock
    private TextClassifier mTextClassifier;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mDummyEntityScores = new ArrayMap<>();
        mDummyEntityScores.put(TYPE_ADDRESS, 0.2f);
        mDummyEntityScores.put(TYPE_PHONE, 0.7f);
        mDummyEntityScores.put(TYPE_OTHER, 0.3f);
    }

    private static class NoOpSpan extends ClickableSpan {
        @Override
        public void onClick(View v) {
            // Do nothing.
        }
    }

    private static class CustomTextLinkSpan extends TextLinks.DefaultTextLinkSpan {
        CustomTextLinkSpan(@Nullable TextLinks.TextLinkSpanData textLinkSpanData) {
            super(textLinkSpanData);
        }
    }

    private static class CustomSpanFactory implements TextLinks.SpanFactory {
        @Override
        public TextLinks.TextLinkSpan createSpan(TextLinks.TextLinkSpanData textLinkSpanData) {
            return new CustomTextLinkSpan(textLinkSpanData);
        }
    }

    @Test
    public void testApplyDifferentText() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder("bar").build();
        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_REPLACE)
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_DIFFERENT_TEXT);
    }

    @Test
    public void testApplyNoLinks() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).build();
        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_REPLACE)
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_NO_LINKS_FOUND);
    }

    @Test
    public void testApplyNoApplied() {
        SpannableString text = new SpannableString("foo");
        text.setSpan(new NoOpSpan(), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();
        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_IGNORE)
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_NO_LINKS_APPLIED);
    }

    @Test
    public void testApplyAppliedDefaultSpanFactory() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();

        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_IGNORE)
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_LINKS_APPLIED);


        TextLinks.TextLinkSpan[] spans = text.getSpans(0, 3, TextLinks.TextLinkSpan.class);
        assertThat(spans).hasLength(1);
        assertThat(links.getLinks()).contains(spans[0].getTextLinkSpanData().getTextLink());
    }

    @Test
    public void testApplyAppliedDefaultSpanFactoryReplace() {
        SpannableString text = new SpannableString("foo");
        text.setSpan(new NoOpSpan(), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();

        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_REPLACE)
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_LINKS_APPLIED);

        TextLinks.TextLinkSpan[] spans = text.getSpans(0, 3, TextLinks.TextLinkSpan.class);
        assertThat(spans).hasLength(1);
        assertThat(links.getLinks()).contains(spans[0].getTextLinkSpanData().getTextLink());
    }

    @Test
    public void testApplyAppliedCustomSpanFactory() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();

        TextLinksParams textLinksParams =
                new TextLinksParams.Builder()
                        .setApplyStrategy(TextLinks.APPLY_STRATEGY_IGNORE)
                        .setSpanFactory(new CustomSpanFactory())
                        .build();
        assertThat(textLinksParams.apply(text, links, mTextClassifier))
                .isEqualTo(TextLinks.STATUS_LINKS_APPLIED);

        TextLinks.TextLinkSpan[] spans = text.getSpans(0, 3, TextLinks.TextLinkSpan.class);
        assertThat(spans).hasLength(1);
        assertThat(links.getLinks()).contains(spans[0].getTextLinkSpanData().getTextLink());
    }
}
