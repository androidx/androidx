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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.LocaleListCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.textclassifier.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link TextClassification}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextClassificationTest {
    private static final Long REFERENCE_TIME_IN_MS = 946684800000L; // 2000-01-01 00:00:00
    private static final String TEXT = "This is an apple";
    private static final int START_INDEX = 2;
    private static final int END_INDEX = 5;
    private static final String ID = "id";
    private static final float ADDRESS_SCORE = 0.7f;
    private static final float PHONE_SCORE = 0.3f;
    private static final LocaleListCompat LOCALE_LIST =
            LocaleListCompat.forLanguageTags("en-US,de-DE");

    private static final String BUNDLE_KEY = "key";
    private static final String BUNDLE_VALUE = "value";
    private static final Bundle BUNDLE = new Bundle();
    static {
        BUNDLE.putString(BUNDLE_KEY, BUNDLE_VALUE);
    }

    private static final String PRIMARY_LABEL = "primaryLabel";
    private static final String PRIMARY_DESCRIPTION = "primaryDescription";
    private static final Intent PRIMARY_INTENT = new Intent("primaryIntentAction");
    private static final IconCompat PRIMARY_ICON = generateTestIcon(567, 288, Color.BLUE);

    private static final IconCompat SECONDARY_ICON = generateTestIcon(32, 288, Color.GREEN);
    private static final String SECONDARY_LABEL = "secondaryLabel";
    private static final String SECONDARY_DESCRIPTION = "secondaryDescription";
    private static final Intent SECONDARY_INTENT = new Intent("secondaryIntentAction");

    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    private static IconCompat generateTestIcon(int width, int height, int colorValue) {
        final int numPixels = width * height;
        final int[] colors = new int[numPixels];
        for (int i = 0; i < numPixels; ++i) {
            colors[i] = colorValue;
        }
        final Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
        return IconCompat.createWithBitmap(bitmap);
    }

    @Test
    public void testBundle() {
        final TextClassification reference = createExpectedBuilderWithRemoteActions()
                .setExtras(BUNDLE).build();
        // Serialize/deserialize.
        final TextClassification result = TextClassification.createFromBundle(reference.toBundle());
        assertTextClassificationEquals(result, reference);
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));
    }

    @Test
    public void testBundleRequest() {
        TextClassification.Request reference = createTextClassificationRequestBuilder()
                .setExtras(BUNDLE).build();

        // Serialize/deserialize.
        TextClassification.Request result = TextClassification.Request.createFromBundle(
                reference.toBundle());

        assertEquals(TEXT, result.getText().toString());
        assertEquals(START_INDEX, result.getStartIndex());
        assertEquals(END_INDEX, result.getEndIndex());
        assertEquals(LOCALE_LIST.toLanguageTags(), result.getDefaultLocales().toLanguageTags());
        assertEquals(REFERENCE_TIME_IN_MS, result.getReferenceTime());
        assertEquals(BUNDLE_VALUE, result.getExtras().getString(BUNDLE_KEY));
    }

    @Test
    public void testMinimalBundleRequest() {
        TextClassification.Request reference =
                new TextClassification.Request.Builder(TEXT, START_INDEX, END_INDEX).build();
        // Serialize/deserialize.
        TextClassification.Request result = TextClassification.Request.createFromBundle(
                reference.toBundle());

        assertEquals(TEXT, result.getText().toString());
        assertEquals(START_INDEX, result.getStartIndex());
        assertEquals(END_INDEX, result.getEndIndex());
        assertEquals(null, result.getReferenceTime());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testToPlatformRequest() {
        TextClassification.Request request = createTextClassificationRequestBuilder().build();
        android.view.textclassifier.TextClassification.Request platformRequest =
                (android.view.textclassifier.TextClassification.Request) request.toPlatform();

        assertThat(platformRequest.getStartIndex()).isEqualTo(START_INDEX);
        assertThat(platformRequest.getEndIndex()).isEqualTo(END_INDEX);
        assertThat(platformRequest.getText().toString()).isEqualTo(TEXT);
        assertThat(platformRequest.getDefaultLocales().toLanguageTags())
                .isEqualTo(LOCALE_LIST.toLanguageTags());
        assertThat(platformRequest.getReferenceTime().toInstant().toEpochMilli())
                .isEqualTo(REFERENCE_TIME_IN_MS);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testRequestFromPlatform() {
        android.view.textclassifier.TextClassification.Request platformRequest =
                new android.view.textclassifier.TextClassification.Request.Builder(
                        TEXT, START_INDEX, END_INDEX)
                        .setDefaultLocales((LocaleList) LOCALE_LIST.unwrap())
                        .setReferenceTime(
                                ConvertUtils.createZonedDateTimeFromUtc(REFERENCE_TIME_IN_MS))
                        .build();


        TextClassification.Request request =
                TextClassification.Request.fromPlatform(platformRequest);

        assertThat(request.getStartIndex()).isEqualTo(START_INDEX);
        assertThat(request.getEndIndex()).isEqualTo(END_INDEX);
        assertThat(request.getText().toString()).isEqualTo(TEXT);
        assertThat(request.getDefaultLocales().toLanguageTags())
                .isEqualTo(LOCALE_LIST.toLanguageTags());
        assertThat(request.getReferenceTime()).isEqualTo(REFERENCE_TIME_IN_MS);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertFromPlatformTextClassification() {
        final PendingIntent primaryPendingIntent = createPendingIntent(PRIMARY_INTENT);
        final RemoteActionCompat remoteAction0 = new RemoteActionCompat(PRIMARY_ICON, PRIMARY_LABEL,
                PRIMARY_DESCRIPTION, primaryPendingIntent);

        final PendingIntent secondaryPendingIntent = createPendingIntent(SECONDARY_INTENT);
        final RemoteActionCompat remoteAction1 = new RemoteActionCompat(SECONDARY_ICON,
                SECONDARY_LABEL, SECONDARY_DESCRIPTION, secondaryPendingIntent);

        android.view.textclassifier.TextClassification platformTextClassification =
                new android.view.textclassifier.TextClassification.Builder()
                        .setText(TEXT)
                        .addAction(remoteAction0.toRemoteAction())
                        .addAction(remoteAction1.toRemoteAction())
                        .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                        .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                        .setId(ID)
                        .build();

        TextClassification actual =
                TextClassification.fromPlatform(mContext, platformTextClassification);
        assertTextClassificationEquals(
                actual, createExpectedBuilderWithRemoteActions().build());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26, maxSdkVersion = 27)
    public void testConvertFromPlatformTextClassification_O() {
        android.view.textclassifier.TextClassification platformTextClassification =
                new android.view.textclassifier.TextClassification.Builder()
                        .setText(TEXT)
                        .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                        .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                        .setIcon(mContext.getDrawable(R.drawable.ic_star_black_16dp))
                        .setLabel(PRIMARY_LABEL)
                        .setIntent(PRIMARY_INTENT)
                        .build();

        TextClassification actual =
                TextClassification.fromPlatform(mContext, platformTextClassification);
        TextClassification expected =
                createExpectedBuilder()
                        .setId(null)
                        .addAction(
                                createRemoteActionCompat(
                                        PRIMARY_INTENT, PRIMARY_ICON, PRIMARY_LABEL, PRIMARY_LABEL))
                        .build();
        assertTextClassificationEquals(actual, expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertToPlatformTextClassification() {
        TextClassification reference = createExpectedBuilder().build();

        android.view.textclassifier.TextClassification platformTextClassification =
                (android.view.textclassifier.TextClassification) reference.toPlatform(mContext);

        TextClassification textClassification =
                TextClassification.fromPlatform(mContext, platformTextClassification);

        assertTextClassificationEquals(textClassification, reference);
    }

    @Test
    @SdkSuppress(minSdkVersion = 26, maxSdkVersion = 27)
    public void testConvertToPlatformTextClassification_O() {
        TextClassification reference = createExpectedBuilderWithRemoteActions().build();

        android.view.textclassifier.TextClassification platformTextClassification =
                (android.view.textclassifier.TextClassification) reference.toPlatform(mContext);

        assertThat(platformTextClassification.getText()).isEqualTo(TEXT);
        assertThat(platformTextClassification.getIcon()).isNotNull();
        assertThat(platformTextClassification.getOnClickListener()).isNotNull();
        assertThat(platformTextClassification.getEntityCount()).isEqualTo(2);
        assertThat(platformTextClassification.getEntity(0)).isEqualTo(TextClassifier.TYPE_ADDRESS);
        assertThat(platformTextClassification.getEntity(1)).isEqualTo(TextClassifier.TYPE_PHONE);
        assertThat(platformTextClassification.getIntent()).isNull();
    }

    // Ensures the package manager can recognize a url scheme that is not all lowercase.
    // b/123640937
    @Test
    public void testNormalizeUriSchemeInRequest() {
        // Change scheme to lower case if classifying a URI.
        String text = "Visit hTTp://www.android.com today";
        String expected = "Visit http://www.android.com today";
        int startIndex = text.indexOf("hTTp://www.android.com");
        int endIndex = startIndex + "hTTp://www.android.com".length();
        TextClassification.Request request =
                new TextClassification.Request.Builder(text, startIndex, endIndex).build();
        assertThat(request.getText().toString()).isEqualTo(expected);

        // No changes if classifying non-URIs.
        text = "Visit hTTp://www.android.com today";
        startIndex = text.indexOf("hTTp");
        endIndex = startIndex + "hTTp".length();
        request = new TextClassification.Request.Builder(text, startIndex, endIndex).build();
        assertThat(request.getText().toString()).isEqualTo(text);

        // No changes if classifying URLs that do not specify any scheme.
        text = "Visit www.android.com today";
        startIndex = text.indexOf("www.android.com");
        endIndex = startIndex + "www.android.com".length();
        request = new TextClassification.Request.Builder(text, startIndex, endIndex).build();
        assertThat(request.getText().toString()).isEqualTo(text);
    }

    private static TextClassification.Request.Builder createTextClassificationRequestBuilder() {
        return new TextClassification.Request.Builder(TEXT, START_INDEX, END_INDEX)
                .setDefaultLocales(LOCALE_LIST)
                .setReferenceTime(REFERENCE_TIME_IN_MS);
    }

    private TextClassification.Builder createExpectedBuilder() {
        TextClassification.Builder builder = new TextClassification.Builder()
                .setText(TEXT)
                .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                .setId(ID);
        return builder;
    }

    private TextClassification.Builder createExpectedBuilderWithRemoteActions() {
        return createExpectedBuilder()
                .addAction(createRemoteActionCompat(
                        PRIMARY_INTENT, PRIMARY_ICON, PRIMARY_LABEL, PRIMARY_DESCRIPTION))
                .addAction(
                        createRemoteActionCompat(
                                SECONDARY_INTENT, SECONDARY_ICON, SECONDARY_LABEL,
                                SECONDARY_DESCRIPTION));
    }

    private RemoteActionCompat createRemoteActionCompat(
            Intent intent, IconCompat icon, String label, String contentDescription) {
        final PendingIntent primaryPendingIntent =
                PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        return new RemoteActionCompat(icon, label, contentDescription, primaryPendingIntent);
    }

    private void assertTextClassificationEquals(
            TextClassification actual, TextClassification expected) {
        assertThat(actual.getText()).isEqualTo(expected.getText());
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getActions()).hasSize(expected.getActions().size());

        for (int i = 0; i < actual.getActions().size(); i++) {
            RemoteActionCompat actualAction = actual.getActions().get(i);
            RemoteActionCompat expectedAction = expected.getActions().get(i);

            assertThat(actualAction.getTitle()).isEqualTo(expectedAction.getTitle());
            assertThat(actualAction.getContentDescription())
                    .isEqualTo(expectedAction.getContentDescription());
            // Can't find a way to get the original intent from the PendingIntent, so just
            // check not null here.
            assertThat(actualAction.getActionIntent()).isNotNull();
        }

        assertThat(actual.getEntityTypeCount()).isEqualTo(expected.getEntityTypeCount());
        for (int i = 0; i < actual.getEntityTypeCount(); i++) {
            String actualEntity = actual.getEntityType(0);
            String expectedEntity = expected.getEntityType(0);

            assertThat(actualEntity).isEqualTo(expectedEntity);
            assertThat(actual.getConfidenceScore(actualEntity))
                    .isEqualTo(expected.getConfidenceScore(expectedEntity));
        }
    }

    private PendingIntent createPendingIntent(Intent intent) {
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
