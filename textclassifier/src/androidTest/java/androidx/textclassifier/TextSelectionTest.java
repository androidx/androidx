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

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.os.LocaleListCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link TextSelection}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextSelectionTest {
    @Test
    public void testParcel() {
        final int startIndex = 13;
        final int endIndex = 37;
        final String id = "id";
        final TextSelection reference = new TextSelection.Builder(startIndex, endIndex)
                .setEntityType(TextClassifier.TYPE_ADDRESS, 0.3f)
                .setEntityType(TextClassifier.TYPE_PHONE, 0.7f)
                .setEntityType(TextClassifier.TYPE_URL, 0.1f)
                .setId(id)
                .build();

        // Serialize/deserialize.
        final TextSelection result = TextSelection.createFromBundle(reference.toBundle());

        assertEquals(startIndex, result.getSelectionStartIndex());
        assertEquals(endIndex, result.getSelectionEndIndex());
        assertEquals(id, result.getId());

        assertEquals(3, result.getEntityCount());
        assertEquals(TextClassifier.TYPE_PHONE, result.getEntity(0));
        assertEquals(TextClassifier.TYPE_ADDRESS, result.getEntity(1));
        assertEquals(TextClassifier.TYPE_URL, result.getEntity(2));
        assertEquals(0.7f, result.getConfidenceScore(TextClassifier.TYPE_PHONE), 1e-7f);
        assertEquals(0.3f, result.getConfidenceScore(TextClassifier.TYPE_ADDRESS), 1e-7f);
        assertEquals(0.1f, result.getConfidenceScore(TextClassifier.TYPE_URL), 1e-7f);
    }

    @Test
    public void testParcelRequest() {
        final String text = "text";
        final int startIndex = 2;
        final int endIndex = 4;
        final String callingPackageName = "packageName";
        TextSelection.Request reference =
                new TextSelection.Request.Builder(text, startIndex, endIndex)
                        .setDefaultLocales(LocaleListCompat.forLanguageTags("en-US,de-DE"))
                        .build();

        // Serialize/deserialize.
        TextSelection.Request result = TextSelection.Request.createFromBundle(reference.toBundle());

        assertEquals(text, result.getText());
        assertEquals(startIndex, result.getStartIndex());
        assertEquals(endIndex, result.getEndIndex());
        assertEquals("en-US,de-DE", result.getDefaultLocales().toLanguageTags());
    }
}
