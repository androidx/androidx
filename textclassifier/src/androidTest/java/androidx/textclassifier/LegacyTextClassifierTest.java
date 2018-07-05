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

import static com.google.common.truth.Truth.assertThat;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.style.URLSpan;

import androidx.collection.ArraySet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LegacyTextClassifierTest {
    private static final String URL = "www.example.com";
    private static final String EMAIL = "my@example.com";
    private static final String PHONE_NUMBER = "+447481123456";
    private static final String ADDRESS = "Six Pancras Square, Kings Cross, London N1C 4AG";
    private static final String TEMPLATE = "Check this out: %s";
    private static final String TEXT_WITH_ALL_ENTITIES =
            String.format("Visit %s, email to %s and call my number %s", URL, EMAIL, PHONE_NUMBER);
    private static final int START = 16;

    private final LegacyTextClassifier mLegacyTextClassifier = LegacyTextClassifier.INSTANCE;

    @Test
    public void generateLinks_url() throws Exception {
        final String text = String.format(TEMPLATE, URL);
        TextLinks.Request request =
                createTextLinksRequest(text, Collections.singletonList(TextClassifier.TYPE_URL));

        TextLinks textLinks = mLegacyTextClassifier.generateLinks(request);

        Collection<TextLinks.TextLink> links = textLinks.getLinks();
        assertThat(links).hasSize(1);
        TextLinks.TextLink textLink = links.iterator().next();
        assertThat(textLink.getStart()).isEqualTo(START);
        assertThat(textLink.getEnd()).isEqualTo(text.length());
        assertThat(textLink.getConfidenceScore(TextClassifier.TYPE_URL)).isEqualTo(1.0f);
        URLSpan urlSpan = textLink.getUrlSpan();
        assertThat(urlSpan.getURL()).contains(URL);
    }

    @Test
    public void generateLinks_email() throws Exception {
        final String text = String.format(TEMPLATE, EMAIL);
        TextLinks.Request request =
                createTextLinksRequest(text, Collections.singletonList(TextClassifier.TYPE_EMAIL));

        TextLinks textLinks = mLegacyTextClassifier.generateLinks(request);

        Collection<TextLinks.TextLink> links = textLinks.getLinks();
        assertThat(links).hasSize(1);
        TextLinks.TextLink textLink = links.iterator().next();
        assertThat(textLink.getStart()).isEqualTo(START);
        assertThat(textLink.getEnd()).isEqualTo(text.length());
        assertThat(textLink.getConfidenceScore(TextClassifier.TYPE_EMAIL)).isEqualTo(1.0f);
        URLSpan urlSpan = textLink.getUrlSpan();
        assertThat(urlSpan.getURL()).contains(EMAIL);
    }

    @Test
    public void generateLinks_phoneNumber() throws Exception {
        final String text = String.format(TEMPLATE, PHONE_NUMBER);
        TextLinks.Request request =
                createTextLinksRequest(text, Collections.singletonList(TextClassifier.TYPE_PHONE));

        TextLinks textLinks = mLegacyTextClassifier.generateLinks(request);

        Collection<TextLinks.TextLink> links = textLinks.getLinks();
        assertThat(links).hasSize(1);
        TextLinks.TextLink textLink = links.iterator().next();
        assertThat(textLink.getStart()).isEqualTo(START);
        assertThat(textLink.getEnd()).isEqualTo(text.length());
        assertThat(textLink.getConfidenceScore(TextClassifier.TYPE_PHONE)).isEqualTo(1.0f);
        URLSpan urlSpan = textLink.getUrlSpan();
        assertThat(urlSpan.getURL()).contains(PHONE_NUMBER);
    }

    @Test
    public void generateLinks_map() throws Exception {
        final String text = String.format(TEMPLATE, ADDRESS);

        TextLinks.Request request =
                createTextLinksRequest(text,
                        Collections.singletonList(TextClassifier.TYPE_ADDRESS));
        TextLinks textLinks = mLegacyTextClassifier.generateLinks(request);

        Collection<TextLinks.TextLink> links = textLinks.getLinks();
        assertThat(links).isEmpty();
    }

    @Test
    public void generateLinks_mix() throws Exception {
        final String[] entityTypes =
                new String[]{
                        TextClassifier.TYPE_URL, TextClassifier.TYPE_EMAIL,
                        TextClassifier.TYPE_PHONE};
        TextLinks.Request request =
                createTextLinksRequest(TEXT_WITH_ALL_ENTITIES, Arrays.asList(entityTypes));

        verifyGenerateLinksOnAllEntities(request);
    }

    @Test
    public void generateLinks_defaultEntities() {
        TextLinks.Request request = new TextLinks.Request.Builder(TEXT_WITH_ALL_ENTITIES).build();

        verifyGenerateLinksOnAllEntities(request);
    }

    private void verifyGenerateLinksOnAllEntities(TextLinks.Request request) {

        final String[] entityTypes =
                new String[]{
                        TextClassifier.TYPE_URL, TextClassifier.TYPE_EMAIL,
                        TextClassifier.TYPE_PHONE};

        TextLinks textLinks = mLegacyTextClassifier.generateLinks(request);

        Collection<TextLinks.TextLink> links = textLinks.getLinks();
        assertThat(links).hasSize(3);

        Set<String> expectedEntities = new ArraySet<>();
        expectedEntities.addAll(Arrays.asList(entityTypes));

        Iterator<TextLinks.TextLink> iterator = links.iterator();
        while (iterator.hasNext()) {
            TextLinks.TextLink textLink = iterator.next();
            String entityType = textLink.getEntity(0);
            assertThat(expectedEntities).contains(entityType);
            assertThat(textLink.getConfidenceScore(entityType)).isEqualTo(1.0f);
            assertThat(textLink.getUrlSpan().getURL()).contains(entityToSpanText(entityType));
            expectedEntities.remove(entityType);
        }
    }

    private String entityToSpanText(String entity) {
        switch (entity) {
            case TextClassifier.TYPE_URL:
                return URL;
            case TextClassifier.TYPE_EMAIL:
                return EMAIL;
            case TextClassifier.TYPE_PHONE:
                return PHONE_NUMBER;
        }
        throw new IllegalArgumentException("Do not expect " + entity);
    }

    private TextLinks.Request createTextLinksRequest(String text, List<String> entityTypes) {
        TextClassifier.EntityConfig entityConfig =
                new TextClassifier.EntityConfig.Builder()
                        .setIncludedEntityTypes(entityTypes)
                        .build();
        return new TextLinks.Request.Builder(text).setEntityConfig(entityConfig).build();
    }
}
