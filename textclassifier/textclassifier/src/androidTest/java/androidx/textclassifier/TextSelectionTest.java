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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.core.os.LocaleListCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link TextSelection}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextSelectionTest {
    private static final float EPSILON = 1e-7f;

    private static final String TEXT = "This is an apple";
    private static final int START_INDEX = 2;
    private static final int END_INDEX = 5;
    private static final String ID = "id";
    private static final float ADDRESS_SCORE = 0.7f;
    private static final float PHONE_SCORE = 0.3f;
    private static final float URL_SCORE = 0.1f;

    private static final LocaleListCompat LOCALE_LIST =
            LocaleListCompat.forLanguageTags("en-US,de-DE");

    private static final String BUNDLE_KEY = "key";
    private static final String BUNDLE_VALUE = "value";
    private static final Bundle BUNDLE = new Bundle();
    static {
        BUNDLE.putString(BUNDLE_KEY, BUNDLE_VALUE);
    }

    @Test
    public void testParcel() {
        TextSelection reference = createTextSelection().setExtras(BUNDLE).build();

        // Serialize/deserialize.
        final TextSelection result = TextSelection.createFromBundle(reference.toBundle());

        assertEquals(START_INDEX, result.getSelectionStartIndex());
        assertEquals(END_INDEX, result.getSelectionEndIndex());
        assertEquals(ID, result.getId());
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));

        assertThat(result.getEntityTypeCount()).isEqualTo(3);
        assertThat(result.getEntityType(0)).isEqualTo(TextClassifier.TYPE_ADDRESS);
        assertThat(result.getEntityType(1)).isEqualTo(TextClassifier.TYPE_PHONE);
        assertThat(result.getEntityType(2)).isEqualTo(TextClassifier.TYPE_URL);
        assertThat(result.getConfidenceScore(TextClassifier.TYPE_ADDRESS))
                .isWithin(EPSILON).of(ADDRESS_SCORE);
        assertThat(result.getConfidenceScore(TextClassifier.TYPE_PHONE))
                .isWithin(EPSILON).of(PHONE_SCORE);
        assertThat(result.getConfidenceScore(TextClassifier.TYPE_URL))
                .isWithin(EPSILON).of(URL_SCORE);
    }

    @Test
    public void testParcelRequest() {
        final CharSequence text = "text";
        final int startIndex = 2;
        final int endIndex = 4;
        TextSelection.Request reference =
                new TextSelection.Request.Builder(text, startIndex, endIndex)
                        .setDefaultLocales(LocaleListCompat.forLanguageTags("en-US,de-DE"))
                        .setExtras(BUNDLE)
                        .build();

        // Serialize/deserialize.
        TextSelection.Request result = TextSelection.Request.createFromBundle(reference.toBundle());

        assertEquals(text, result.getText());
        assertEquals(startIndex, result.getStartIndex());
        assertEquals(endIndex, result.getEndIndex());
        assertEquals("en-US,de-DE", result.getDefaultLocales().toLanguageTags());
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testRequestToPlatform() {
        TextSelection.Request request = new TextSelection.Request.Builder(TEXT, START_INDEX,
                END_INDEX)
                .setDefaultLocales(LOCALE_LIST)
                .build();

        android.view.textclassifier.TextSelection.Request platformRequest =
                (android.view.textclassifier.TextSelection.Request) request.toPlatform();

        assertThat(platformRequest.getStartIndex()).isEqualTo(START_INDEX);
        assertThat(platformRequest.getEndIndex()).isEqualTo(END_INDEX);
        assertThat(platformRequest.getText().toString()).isEqualTo(TEXT);
        assertThat(platformRequest.getDefaultLocales().toLanguageTags())
                .isEqualTo(LOCALE_LIST.toLanguageTags());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testRequestFromPlatform() {
        android.view.textclassifier.TextSelection.Request platformRequest =
                new android.view.textclassifier.TextSelection.Request.Builder(
                        TEXT, START_INDEX, END_INDEX)
                        .setDefaultLocales((LocaleList) LOCALE_LIST.unwrap())
                        .build();

        TextSelection.Request request = TextSelection.Request.fromPlatfrom(platformRequest);
        assertThat(request.getStartIndex()).isEqualTo(START_INDEX);
        assertThat(request.getEndIndex()).isEqualTo(END_INDEX);
        assertThat(request.getText().toString()).isEqualTo(TEXT);
        assertThat(request.getDefaultLocales().toLanguageTags())
                .isEqualTo(LOCALE_LIST.toLanguageTags());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testFromPlatform() {
        android.view.textclassifier.TextSelection platformTextSelection =
                new android.view.textclassifier.TextSelection.Builder(START_INDEX, END_INDEX)
                        .setId(ID)
                        .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                        .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                        .setEntityType(TextClassifier.TYPE_URL, URL_SCORE)
                        .build();

        TextSelection textSelection = TextSelection.fromPlatform(platformTextSelection);

        assertTextSelection(textSelection);
        assertThat(textSelection.getId()).isEqualTo(ID);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testFromPlatform_O() {
        android.view.textclassifier.TextSelection platformTextSelection =
                new android.view.textclassifier.TextSelection.Builder(START_INDEX, END_INDEX)
                        .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                        .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                        .setEntityType(TextClassifier.TYPE_URL, URL_SCORE)
                        .build();

        TextSelection textSelection = TextSelection.fromPlatform(platformTextSelection);

        assertTextSelection(textSelection);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testToPlatform_O() {
        TextSelection reference = createTextSelection().build();

        android.view.textclassifier.TextSelection platformTextSelection =
                (android.view.textclassifier.TextSelection) reference.toPlatform();
        TextSelection textSelection = TextSelection.fromPlatform(platformTextSelection);

        assertTextSelection(textSelection);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testToPlatform_P() {
        TextSelection reference = createTextSelection().build();

        android.view.textclassifier.TextSelection platformTextSelection =
                (android.view.textclassifier.TextSelection) reference.toPlatform();
        TextSelection textSelection = TextSelection.fromPlatform(platformTextSelection);

        assertTextSelection(textSelection);
        assertThat(textSelection.getId()).isEqualTo(ID);
    }

    private void assertTextSelection(TextSelection textSelection) {
        assertThat(textSelection.getEntityTypeCount()).isEqualTo(3);
        assertThat(textSelection.getEntityType(0)).isEqualTo(TextClassifier.TYPE_ADDRESS);
        assertThat(textSelection.getEntityType(1)).isEqualTo(TextClassifier.TYPE_PHONE);
        assertThat(textSelection.getEntityType(2)).isEqualTo(TextClassifier.TYPE_URL);
        assertThat(textSelection.getConfidenceScore(TextClassifier.TYPE_ADDRESS))
                .isWithin(EPSILON).of(ADDRESS_SCORE);
        assertThat(textSelection.getConfidenceScore(TextClassifier.TYPE_PHONE))
                .isWithin(EPSILON).of(PHONE_SCORE);
        assertThat(textSelection.getConfidenceScore(TextClassifier.TYPE_URL))
                .isWithin(EPSILON).of(URL_SCORE);
    }

    private TextSelection.Builder createTextSelection() {
        return new TextSelection.Builder(START_INDEX, END_INDEX)
                .setId(ID)
                .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                .setEntityType(TextClassifier.TYPE_URL, URL_SCORE);
    }
}
