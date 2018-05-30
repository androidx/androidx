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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.LocaleListCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Instrumentation unit tests for {@link TextClassification}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextClassificationTest {
    private static final float EPSILON = 1e-7f;

    private static final long REFERENCE_TIME_IN_MS = 946684800000L; // 2000-01-01 00:00:00
    private static final String TEXT = "This is an apple";
    private static final int START_INDEX = 2;
    private static final int END_INDEX = 5;
    private static final String ID = "id";
    private static final float ADDRESS_SCORE = 0.7f;
    private static final float PHONE_SCORE = 0.3f;
    private static final LocaleListCompat LOCALE_LIST =
            LocaleListCompat.forLanguageTags("en-US,de-DE");


    private static final String PRIMARY_LABEL = "primaryLabel";
    private static final String PRIMARY_DESCRIPTION = "primaryDescription";
    private static final Intent PRIMARY_INTENT = new Intent("primaryIntentAction");
    private static final IconCompat PRIMARY_ICON = generateTestIcon(567, 288, Color.BLUE);

    private static final IconCompat SECONDARY_ICON = generateTestIcon(32, 288, Color.GREEN);
    private static final String SECONDARY_LABEL = "secondaryLabel";
    private static final String SECONDARY_DESCRIPTION = "secondaryDescription";
    private static final Intent SECONDARY_INTENT = new Intent("secondaryIntentAction");

    private static final Calendar REFERENCE_TIME;
    static {
        REFERENCE_TIME = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        REFERENCE_TIME.setTimeInMillis(REFERENCE_TIME_IN_MS);
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
        final TextClassification reference = createTextClassification();
        // Serialize/deserialize.
        final TextClassification result = TextClassification.createFromBundle(reference.toBundle());
        assertTextClassification(result);
    }

    @Test
    public void testBundleRequest() {
        TextClassification.Request reference = createTextClassificationRequest();

        // Serialize/deserialize.
        TextClassification.Request result = TextClassification.Request.createFromBundle(
                reference.toBundle());

        assertEquals(TEXT, result.getText());
        assertEquals(START_INDEX, result.getStartIndex());
        assertEquals(END_INDEX, result.getEndIndex());
        assertEquals(LOCALE_LIST.toLanguageTags(), result.getDefaultLocales().toLanguageTags());
        assertEquals(REFERENCE_TIME, result.getReferenceTime());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testToPlatformRequest() {
        TextClassification.Request request = createTextClassificationRequest();
        android.view.textclassifier.TextClassification.Request platformRequest =
                TextClassification.Request.Convert.toPlatform(request);

        assertThat(platformRequest.getStartIndex()).isEqualTo(START_INDEX);
        assertThat(platformRequest.getEndIndex()).isEqualTo(END_INDEX);
        assertThat(platformRequest.getText()).isEqualTo(TEXT);
        assertThat(platformRequest.getDefaultLocales().toLanguageTags())
                .isEqualTo(LOCALE_LIST.toLanguageTags());
        assertThat(platformRequest.getReferenceTime().toInstant().toEpochMilli())
                .isEqualTo(REFERENCE_TIME_IN_MS);
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

        TextClassification textClassification = TextClassification.Convert.fromPlatform(
                platformTextClassification);

        assertTextClassification(textClassification);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testConvertToPlatformTextClassification() {
        TextClassification reference = createTextClassification();

        android.view.textclassifier.TextClassification platformTextClassification =
                TextClassification.Convert.toPlatform(reference);

        TextClassification textClassification =
                TextClassification.Convert.fromPlatform(platformTextClassification);

        assertTextClassification(textClassification);
    }

    private static TextClassification.Request createTextClassificationRequest() {
        return new TextClassification.Request.Builder(TEXT, START_INDEX, END_INDEX)
                .setDefaultLocales(LOCALE_LIST)
                .setReferenceTime(REFERENCE_TIME)
                .build();
    }

    private static TextClassification createTextClassification() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final PendingIntent primaryPendingIntent = PendingIntent.getActivity(context, 0,
                PRIMARY_INTENT, 0);
        final RemoteActionCompat remoteAction0 = new RemoteActionCompat(PRIMARY_ICON, PRIMARY_LABEL,
                PRIMARY_DESCRIPTION, primaryPendingIntent);

        final PendingIntent secondaryPendingIntent = PendingIntent.getActivity(context, 0,
                SECONDARY_INTENT, 0);
        final RemoteActionCompat remoteAction1 = new RemoteActionCompat(SECONDARY_ICON,
                SECONDARY_LABEL, SECONDARY_DESCRIPTION, secondaryPendingIntent);

        return new TextClassification.Builder()
                .setText(TEXT)
                .addAction(remoteAction0)
                .addAction(remoteAction1)
                .setEntityType(TextClassifier.TYPE_ADDRESS, ADDRESS_SCORE)
                .setEntityType(TextClassifier.TYPE_PHONE, PHONE_SCORE)
                .setId(ID)
                .build();
    }

    private void assertTextClassification(TextClassification textClassification) {
        assertEquals(TEXT, textClassification.getText());
        assertEquals(ID, textClassification.getId());
        assertEquals(2, textClassification.getActions().size());

        // Primary action.
        final RemoteActionCompat primaryAction = textClassification.getActions().get(0);
        assertEquals(PRIMARY_LABEL, primaryAction.getTitle());
        assertEquals(PRIMARY_DESCRIPTION, primaryAction.getContentDescription());
        assertEquals(createPendingIntent(PRIMARY_INTENT), primaryAction.getActionIntent());

        // Secondary action.
        final RemoteActionCompat secondaryAction = textClassification.getActions().get(1);
        assertEquals(SECONDARY_LABEL, secondaryAction.getTitle());
        assertEquals(SECONDARY_DESCRIPTION, secondaryAction.getContentDescription());
        assertEquals(createPendingIntent(SECONDARY_INTENT), secondaryAction.getActionIntent());

        // Entities.
        assertEquals(2, textClassification.getEntityCount());
        assertEquals(TextClassifier.TYPE_ADDRESS, textClassification.getEntity(0));
        assertEquals(TextClassifier.TYPE_PHONE, textClassification.getEntity(1));
        assertEquals(
                PHONE_SCORE,
                textClassification.getConfidenceScore(TextClassifier.TYPE_PHONE),
                EPSILON);
        assertEquals(
                ADDRESS_SCORE,
                textClassification.getConfidenceScore(TextClassifier.TYPE_ADDRESS),
                EPSILON);
    }

    private static PendingIntent createPendingIntent(Intent intent) {
        return PendingIntent.getActivity(InstrumentationRegistry.getTargetContext(), 0,
                intent, 0);
    }
}
