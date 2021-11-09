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

import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Spannable;
import android.text.SpannableString;

import androidx.collection.ArrayMap;
import androidx.core.os.LocaleListCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Instrumentation unit tests for {@link TextLinks}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextLinksTest {

    private static final String FULL_TEXT = "this is just a test";
    private static final int START = 5;
    private static final int END = 6;

    private static final String LANGUAGE_TAGS = "en-US,de-DE";
    private static final LocaleListCompat LOCALE_LIST =
            LocaleListCompat.forLanguageTags(LANGUAGE_TAGS);
    private static final long REFERENCE_TIME = System.currentTimeMillis();

    private Map<String, Float> mDummyEntityScores;

    private static final String BUNDLE_KEY = "key";
    private static final String BUNDLE_VALUE = "value";
    private static final Bundle BUNDLE = new Bundle();
    static {
        BUNDLE.putString(BUNDLE_KEY, BUNDLE_VALUE);
    }

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
                .setExtras(BUNDLE)
                .build();

        // Serialize/deserialize.
        final TextLinks result = TextLinks.createFromBundle(reference.toBundle());

        assertTextLinks(result);
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));
    }

    @Test
    public void testBundleRequest() {
        TextLinks.Request reference = createTextLinksRequest().setExtras(BUNDLE).build();

        // Serialize/deserialize.
        TextLinks.Request result = TextLinks.Request.createFromBundle(reference.toBundle());

        assertEquals(FULL_TEXT, result.getText().toString());
        assertEquals(LANGUAGE_TAGS, result.getDefaultLocales().toLanguageTags());
        assertThat(result.getEntityConfig().getHints()).containsExactly("hints");
        assertThat(result.getEntityConfig().resolveTypes(
                Arrays.asList("default", "excluded")))
                .containsExactly("included", "default");
        assertThat(result.getReferenceTime()).isEqualTo(REFERENCE_TIME);
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));
    }

    @Test
    public void testBundleRequest_minimalRequest() {
        TextLinks.Request reference = new TextLinks.Request.Builder(FULL_TEXT).build();

        // Serialize/deserialize.
        TextLinks.Request result = TextLinks.Request.createFromBundle(reference.toBundle());

        assertEquals(FULL_TEXT, result.getText().toString());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertToPlatformRequest() {
        TextLinks.Request request = createTextLinksRequest().build();

        android.view.textclassifier.TextLinks.Request platformRequest = request.toPlatform();
        assertEquals(FULL_TEXT, platformRequest.getText().toString());
        assertEquals(LANGUAGE_TAGS, platformRequest.getDefaultLocales().toLanguageTags());
        assertThat(platformRequest.getEntityConfig().getHints()).containsExactly("hints");
        assertThat(platformRequest.getEntityConfig().resolveEntityListModifications(
                Arrays.asList("default", "excluded")))
                .containsExactly("included", "default");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testFromPlatformRequest() {
        android.view.textclassifier.TextClassifier.EntityConfig entityConfig =
                android.view.textclassifier.TextClassifier.EntityConfig.create(
                        Collections.singleton("hints"),
                        Collections.singleton("include"),
                        Collections.singleton("exclude"));
        android.view.textclassifier.TextLinks.Request platformRequest =
                new android.view.textclassifier.TextLinks.Request.Builder(FULL_TEXT)
                        .setDefaultLocales(LocaleList.forLanguageTags(LANGUAGE_TAGS))
                        .setEntityConfig(entityConfig)
                        .build();

        TextLinks.Request request = TextLinks.Request.fromPlatform(platformRequest);
        assertThat(request.getText().toString()).isEqualTo(FULL_TEXT);
        assertThat(request.getDefaultLocales().toLanguageTags()).isEqualTo(LANGUAGE_TAGS);
        assertThat(request.getEntityConfig().getHints()).containsExactly("hints");
        assertThat(request.getEntityConfig().resolveTypes(Arrays.asList("default", "exclude")))
                .containsExactly("include", "default");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertFromPlatformTextLinks() {
        final android.view.textclassifier.TextLinks platformTextLinks =
                new android.view.textclassifier.TextLinks.Builder(FULL_TEXT)
                        .addLink(0, 4, getEntityScores(0.f, 0.f, 1.f))
                        .addLink(5, 12, getEntityScores(.8f, .1f, .5f))
                        .build();

        TextLinks textLinks = TextLinks.fromPlatform(platformTextLinks, FULL_TEXT);
        assertTextLinks(textLinks);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertToPlatformTextLinks() {
        final TextLinks textLinks =
                new TextLinks.Builder(FULL_TEXT)
                        .addLink(0, 4, getEntityScores(0.f, 0.f, 1.f))
                        .addLink(5, 12, getEntityScores(.8f, .1f, .5f))
                        .build();

        android.view.textclassifier.TextLinks platformTextLinks = textLinks.toPlatform();
        TextLinks recovered = TextLinks.fromPlatform(platformTextLinks, FULL_TEXT);

        assertTextLinks(recovered);
    }

    @Test
    public void testApply_spannable_no_link() {
        SpannableString text = new SpannableString(FULL_TEXT);
        TextLinks textLinks = new TextLinks.Builder(text).build();

        Context context = ApplicationProvider.getApplicationContext();
        TextClassifier textClassifier = TextClassificationManager.of(context).getTextClassifier();
        int status = textLinks.apply(text, textClassifier, TextLinksParams.DEFAULT_PARAMS);
        assertThat(status).isEqualTo(TextLinks.STATUS_NO_LINKS_FOUND);

        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).isEmpty();
    }

    @Test
    public void testApply_spannable() {
        SpannableString text = new SpannableString(FULL_TEXT);
        TextLinks textLinks = new TextLinks.Builder(text)
                .addLink(START, END, Collections.singletonMap(TextClassifier.TYPE_PHONE, 1.0f))
                .build();

        Context context = ApplicationProvider.getApplicationContext();
        TextClassifier textClassifier = TextClassificationManager.of(context).getTextClassifier();
        int status = textLinks.apply(text, textClassifier, TextLinksParams.DEFAULT_PARAMS);
        assertThat(status).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);

        assertAppliedSpannable(text);
    }

    private void assertAppliedSpannable(Spannable spannable) {
        TextLinks.TextLinkSpan[] spans =
                spannable.getSpans(0, spannable.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).hasLength(1);
        TextLinks.TextLinkSpan span = spans[0];
        assertThat(spannable.getSpanStart(span)).isEqualTo(START);
        assertThat(spannable.getSpanEnd(span)).isEqualTo(END);
        assertThat(span.getTextLinkSpanData().getTextLink().getEntityType(0))
                .isEqualTo(TextClassifier.TYPE_PHONE);
    }

    private TextLinks.Request.Builder createTextLinksRequest() {
        EntityConfig entityConfig = new EntityConfig.Builder()
                .setIncludedTypes(Arrays.asList("included"))
                .setExcludedTypes(Arrays.asList("excluded"))
                .setHints(Arrays.asList("hints"))
                .build();

        return new TextLinks.Request.Builder(FULL_TEXT)
                .setDefaultLocales(LOCALE_LIST)
                .setEntityConfig(entityConfig)
                .setReferenceTime(REFERENCE_TIME);
    }

    private void assertTextLinks(TextLinks textLinks) {
        assertEquals(FULL_TEXT, textLinks.getText().toString());
        final List<TextLinks.TextLink> resultList = new ArrayList<>(textLinks.getLinks());
        final float epsilon = 1e-7f;
        assertEquals(2, resultList.size());
        assertEquals(0, resultList.get(0).getStart());
        assertEquals(4, resultList.get(0).getEnd());
        assertEquals(1, resultList.get(0).getEntityTypeCount());
        assertEquals(TYPE_OTHER, resultList.get(0).getEntityType(0));
        assertEquals(1.f, resultList.get(0).getConfidenceScore(TYPE_OTHER), epsilon);
        assertEquals(5, resultList.get(1).getStart());
        assertEquals(12, resultList.get(1).getEnd());
        assertEquals(3, resultList.get(1).getEntityTypeCount());
        assertEquals(TYPE_ADDRESS, resultList.get(1).getEntityType(0));
        assertEquals(TYPE_OTHER, resultList.get(1).getEntityType(1));
        assertEquals(TYPE_PHONE, resultList.get(1).getEntityType(2));
        assertEquals(.8f, resultList.get(1).getConfidenceScore(TYPE_ADDRESS), epsilon);
        assertEquals(.5f, resultList.get(1).getConfidenceScore(TYPE_OTHER), epsilon);
        assertEquals(.1f, resultList.get(1).getConfidenceScore(TYPE_PHONE), epsilon);
    }

}
