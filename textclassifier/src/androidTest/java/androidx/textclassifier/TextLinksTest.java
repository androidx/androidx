/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.textclassifier.TextClassifier.EntityConfig;
import static androidx.textclassifier.TextClassifier.TYPE_ADDRESS;
import static androidx.textclassifier.TextClassifier.TYPE_OTHER;
import static androidx.textclassifier.TextClassifier.TYPE_PHONE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.os.LocaleListCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Instrumentation unit tests for {@link TextLinks}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextLinksTest {

    private static final String FULL_TEXT = "this is just a test";
    private static final String LANGUAGE_TAGS = "en-US,de-DE";
    private static final LocaleListCompat LOCALE_LIST =
            LocaleListCompat.forLanguageTags(LANGUAGE_TAGS);

    private static class NoOpSpan extends ClickableSpan {
        @Override
        public void onClick(View v) {
            // Do nothing.
        }
    }

    private static class CustomTextLinkSpan extends TextLinks.TextLinkSpan {
        CustomTextLinkSpan(@Nullable TextLinks.TextLink textLink) {
            super(textLink);
        }
    }

    private static class CustomSpanFactory implements TextLinks.SpanFactory {
        @Override
        public TextLinks.TextLinkSpan createSpan(TextLinks.TextLink textLink) {
            return new CustomTextLinkSpan(textLink);
        }
    }

    private Map<String, Float> mDummyEntityScores;

    @Before
    public void setup() {
        mDummyEntityScores = new ArrayMap<>();
        mDummyEntityScores.put(TYPE_ADDRESS, 0.2f);
        mDummyEntityScores.put(TYPE_PHONE, 0.7f);
        mDummyEntityScores.put(TYPE_OTHER, 0.3f);
    }

    private Map<String, Float> getEntityScores(float address, float phone, float other) {
        final Map<String, Float> result = new ArrayMap<>();
        if (address > 0.f) {
            result.put(TYPE_ADDRESS, address);
        }
        if (phone > 0.f) {
            result.put(TYPE_PHONE, phone);
        }
        if (other > 0.f) {
            result.put(TYPE_OTHER, other);
        }
        return result;
    }

    @Test
    public void testBundle() {
        final TextLinks reference = new TextLinks.Builder(FULL_TEXT)
                .addLink(0, 4, getEntityScores(0.f, 0.f, 1.f))
                .addLink(5, 12, getEntityScores(.8f, .1f, .5f))
                .build();

        // Serialize/deserialize.
        final TextLinks result = TextLinks.createFromBundle(reference.toBundle());

        assertTextLinks(result);
    }

    @Test
    public void testBundleRequest() {
        TextLinks.Request reference = createTextLinksRequest();

        // Serialize/deserialize.
        TextLinks.Request result = TextLinks.Request.createFromBundle(reference.toBundle());

        assertEquals(FULL_TEXT, result.getText());
        assertEquals(LANGUAGE_TAGS, result.getDefaultLocales().toLanguageTags());
        assertThat(result.getEntityConfig().getHints()).containsExactly("hints");
        assertThat(result.getEntityConfig().resolveEntityTypes(
                Arrays.asList("default", "excluded")))
                .containsExactly("included", "default");
    }

    @Test
    public void testApplyDifferentText() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder("bar").build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null),
                TextLinks.STATUS_DIFFERENT_TEXT);
    }

    @Test
    public void testApplyNoLinks() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null),
                TextLinks.STATUS_NO_LINKS_FOUND);
    }

    @Test
    public void testApplyNoApplied() {
        SpannableString text = new SpannableString("foo");
        text.setSpan(new NoOpSpan(), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_IGNORE, null),
                TextLinks.STATUS_NO_LINKS_APPLIED);
    }

    @Test
    public void testApplyAppliedDefaultSpanFactory() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_IGNORE, null),
                TextLinks.STATUS_LINKS_APPLIED);
        TextLinks.TextLinkSpan[] spans = text.getSpans(0, 3, TextLinks.TextLinkSpan.class);
        assertEquals(spans.length, 1);
        assertTrue(links.getLinks().contains(spans[0].getTextLink()));
    }

    @Test
    public void testApplyAppliedDefaultSpanFactoryReplace() {
        SpannableString text = new SpannableString("foo");
        text.setSpan(new NoOpSpan(), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_REPLACE, null),
                TextLinks.STATUS_LINKS_APPLIED);
        TextLinks.TextLinkSpan[] spans = text.getSpans(0, 3, TextLinks.TextLinkSpan.class);
        assertEquals(spans.length, 1);
        assertTrue(links.getLinks().contains(spans[0].getTextLink()));
    }

    @Test
    public void testApplyAppliedCustomSpanFactory() {
        SpannableString text = new SpannableString("foo");
        TextLinks links = new TextLinks.Builder(text.toString()).addLink(
                0, 3, mDummyEntityScores).build();
        assertEquals(links.apply(text, TextLinks.APPLY_STRATEGY_IGNORE, new CustomSpanFactory()),
                TextLinks.STATUS_LINKS_APPLIED);
        CustomTextLinkSpan[] spans = text.getSpans(0, 3, CustomTextLinkSpan.class);
        assertEquals(spans.length, 1);
        assertTrue(links.getLinks().contains(spans[0].getTextLink()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertToPlatformRequest() {
        TextLinks.Request request = createTextLinksRequest();

        android.view.textclassifier.TextLinks.Request platformRequest =
                TextLinks.Request.Convert.toPlatform(request);
        assertEquals(FULL_TEXT, platformRequest.getText());
        assertEquals(LANGUAGE_TAGS, platformRequest.getDefaultLocales().toLanguageTags());
        assertThat(platformRequest.getEntityConfig().getHints()).containsExactly("hints");
        assertThat(platformRequest.getEntityConfig().resolveEntityListModifications(
                Arrays.asList("default", "excluded")))
                .containsExactly("included", "default");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertFromPlatformTextLinks() {
        final android.view.textclassifier.TextLinks platformTextLinks =
                new android.view.textclassifier.TextLinks.Builder(FULL_TEXT)
                        .addLink(0, 4, getEntityScores(0.f, 0.f, 1.f))
                        .addLink(5, 12, getEntityScores(.8f, .1f, .5f))
                        .build();

        TextLinks textLinks = TextLinks.Convert.fromPlatform(platformTextLinks, FULL_TEXT);
        assertTextLinks(textLinks);
    }

    private TextLinks.Request createTextLinksRequest() {
        EntityConfig entityConfig = new EntityConfig.Builder()
                .setIncludedEntityTypes(Arrays.asList("included"))
                .setExcludedEntityTypes(Arrays.asList("excluded"))
                .setHints(Arrays.asList("hints"))
                .build();

        return new TextLinks.Request.Builder(FULL_TEXT)
                .setDefaultLocales(LOCALE_LIST)
                .setEntityConfig(entityConfig)
                .build();
    }

    private void assertTextLinks(TextLinks textLinks) {
        assertEquals(FULL_TEXT, textLinks.getText());
        final List<TextLinks.TextLink> resultList = new ArrayList<>(textLinks.getLinks());
        final float epsilon = 1e-7f;
        assertEquals(2, resultList.size());
        assertEquals(0, resultList.get(0).getStart());
        assertEquals(4, resultList.get(0).getEnd());
        assertEquals(1, resultList.get(0).getEntityCount());
        assertEquals(TYPE_OTHER, resultList.get(0).getEntity(0));
        assertEquals(1.f, resultList.get(0).getConfidenceScore(TYPE_OTHER), epsilon);
        assertEquals(5, resultList.get(1).getStart());
        assertEquals(12, resultList.get(1).getEnd());
        assertEquals(3, resultList.get(1).getEntityCount());
        assertEquals(TYPE_ADDRESS, resultList.get(1).getEntity(0));
        assertEquals(TYPE_OTHER, resultList.get(1).getEntity(1));
        assertEquals(TYPE_PHONE, resultList.get(1).getEntity(2));
        assertEquals(.8f, resultList.get(1).getConfidenceScore(TYPE_ADDRESS), epsilon);
        assertEquals(.5f, resultList.get(1).getConfidenceScore(TYPE_OTHER), epsilon);
        assertEquals(.1f, resultList.get(1).getConfidenceScore(TYPE_PHONE), epsilon);
    }

}
