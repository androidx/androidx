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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.textclassifier.TextClassificationContext;

import androidx.core.os.LocaleListCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** UnInstrumentation unit tests for {@link PlatformTextClassifierWrapper}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
public class PlatformTextClassifierWrapperTest {
    private PlatformTextClassifierWrapper mClassifier;

    private static final LocaleListCompat LOCALES = LocaleListCompat.forLanguageTags("en");
    private static final int START = 1;
    private static final int END = 3;
    // This text has lots of things that are probably entities in many cases.
    private static final String TEXT = "An email address is test@example.com. A phone number"
            + " might be +12122537077. Somebody lives at 123 Main Street, Mountain View, CA,"
            + " and there's good stuff at https://www.android.com :)";
    private static final TextSelection.Request TEXT_SELECTION_REQUEST =
            new TextSelection.Request.Builder(TEXT, START, END)
                    .setDefaultLocales(LOCALES)
                    .build();
    private static final TextClassification.Request TEXT_CLASSIFICATION_REQUEST =
            new TextClassification.Request.Builder(TEXT, START, END)
                    .setDefaultLocales(LOCALES)
                    .build();

    @Before
    public void setup() {
        mClassifier = new PlatformTextClassifierWrapper(getPlatformTextClassifier());
    }

    @Test
    public void testSuggestSelection() {
        assertValidResult(mClassifier.suggestSelection(TEXT_SELECTION_REQUEST));
    }

    @Test
    public void testClassifyText() {
        assertValidResult(mClassifier.classifyText(TEXT_CLASSIFICATION_REQUEST));
    }

    @Test
    public void testGenerateLinks() {
        assertValidResult(mClassifier.generateLinks(new TextLinks.Request.Builder(TEXT).build()));
    }

    @Test
    public void testDestroy() {
        mClassifier.destroy();

        assertTrue(mClassifier.isDestroyed());
    }

    private android.view.textclassifier.TextClassifier getPlatformTextClassifier() {
        android.view.textclassifier.TextClassificationManager
                textClassificationManager =
                InstrumentationRegistry.getContext().getSystemService(
                        android.view.textclassifier.TextClassificationManager.class);
        return textClassificationManager.createTextClassificationSession(
                new TextClassificationContext.Builder("pkg", "widget")
                        .build());
    }

    private static void assertValidResult(TextSelection selection) {
        assertNotNull(selection);
        assertTrue(selection.getSelectionStartIndex() >= 0);
        assertTrue(selection.getSelectionEndIndex() > selection.getSelectionStartIndex());
        assertTrue(selection.getEntityCount() >= 0);
        for (int i = 0; i < selection.getEntityCount(); i++) {
            final String entity = selection.getEntity(i);
            assertNotNull(entity);
            final float confidenceScore = selection.getConfidenceScore(entity);
            assertTrue(confidenceScore >= 0);
            assertTrue(confidenceScore <= 1);
        }
    }

    private static void assertValidResult(TextClassification classification) {
        assertNotNull(classification);
        assertTrue(classification.getEntityCount() >= 0);
        for (int i = 0; i < classification.getEntityCount(); i++) {
            final String entity = classification.getEntity(i);
            assertNotNull(entity);
            final float confidenceScore = classification.getConfidenceScore(entity);
            assertTrue(confidenceScore >= 0);
            assertTrue(confidenceScore <= 1);
        }
        assertNotNull(classification.getActions());
    }

    private static void assertValidResult(TextLinks links) {
        assertNotNull(links);
        for (TextLinks.TextLink link : links.getLinks()) {
            assertTrue(link.getEntityCount() > 0);
            assertTrue(link.getStart() >= 0);
            assertTrue(link.getStart() <= link.getEnd());
            for (int i = 0; i < link.getEntityCount(); i++) {
                String entityType = link.getEntity(i);
                assertNotNull(entityType);
                final float confidenceScore = link.getConfidenceScore(entityType);
                assertTrue(confidenceScore >= 0);
                assertTrue(confidenceScore <= 1);
            }
        }
    }
}
