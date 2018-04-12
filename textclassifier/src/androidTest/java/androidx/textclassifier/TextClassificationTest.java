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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
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
    public IconCompat generateTestIcon(int width, int height, int colorValue) {
        final int numPixels = width * height;
        final int[] colors = new int[numPixels];
        for (int i = 0; i < numPixels; ++i) {
            colors[i] = colorValue;
        }
        final Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
        return IconCompat.createWithBitmap(bitmap);
    }

    public void testBundle() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String text = "text";

        final IconCompat primaryIcon = generateTestIcon(576, 288, Color.BLUE);
        final String primaryLabel = "primaryLabel";
        final String primaryDescription = "primaryDescription";
        final Intent primaryIntent = new Intent("primaryIntentAction");
        final PendingIntent primaryPendingIntent = PendingIntent.getActivity(context, 0,
                primaryIntent, 0);
        final RemoteActionCompat remoteAction0 = new RemoteActionCompat(primaryIcon, primaryLabel,
                primaryDescription, primaryPendingIntent);

        final IconCompat secondaryIcon = generateTestIcon(32, 288, Color.GREEN);
        final String secondaryLabel = "secondaryLabel";
        final String secondaryDescription = "secondaryDescription";
        final Intent secondaryIntent = new Intent("secondaryIntentAction");
        final PendingIntent secondaryPendingIntent = PendingIntent.getActivity(context, 0,
                secondaryIntent, 0);
        final RemoteActionCompat remoteAction1 = new RemoteActionCompat(secondaryIcon,
                secondaryLabel, secondaryDescription, secondaryPendingIntent);

        final String id = "id";
        final TextClassification reference = new TextClassification.Builder()
                .setText(text)
                .addAction(remoteAction0)
                .addAction(remoteAction1)
                .setEntityType(TextClassifier.TYPE_ADDRESS, 0.3f)
                .setEntityType(TextClassifier.TYPE_PHONE, 0.7f)
                .setId(id)
                .build();

        // Serialize/deserialize.
        final TextClassification result = TextClassification.createFromBundle(reference.toBundle());

        assertEquals(text, result.getText());
        assertEquals(id, result.getId());
        assertEquals(2, result.getActions().size());

        // Primary action.
        final RemoteActionCompat primaryAction = result.getActions().get(0);
        assertEquals(primaryLabel, primaryAction.getTitle());
        assertEquals(primaryDescription, primaryAction.getContentDescription());
        assertEquals(primaryPendingIntent, primaryAction.getActionIntent());

        // Secondary action.
        final RemoteActionCompat secondaryAction = result.getActions().get(1);
        assertEquals(secondaryLabel, secondaryAction.getTitle());
        assertEquals(secondaryDescription, secondaryAction.getContentDescription());
        assertEquals(secondaryPendingIntent, secondaryAction.getActionIntent());

        // Entities.
        assertEquals(2, result.getEntityCount());
        assertEquals(TextClassifier.TYPE_PHONE, result.getEntity(0));
        assertEquals(TextClassifier.TYPE_ADDRESS, result.getEntity(1));
        assertEquals(0.7f, result.getConfidenceScore(TextClassifier.TYPE_PHONE), 1e-7f);
        assertEquals(0.3f, result.getConfidenceScore(TextClassifier.TYPE_ADDRESS), 1e-7f);
    }

    @Test
    public void testBundleRequest() {
        final String text = "text";
        final int startIndex = 2;
        final int endIndex = 4;
        final String callingPackageName = "packageName";
        final Calendar referenceTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        referenceTime.setTimeInMillis(946684800000L);  // 2000-01-01 00:00:00
        TextClassification.Request reference =
                new TextClassification.Request.Builder(text, startIndex, endIndex)
                        .setDefaultLocales(LocaleListCompat.forLanguageTags("en-US,de-DE"))
                        .setReferenceTime(referenceTime)
                        .build();

        // Serialize/deserialize.
        TextClassification.Request result = TextClassification.Request.createFromBundle(
                reference.toBundle());

        assertEquals(text, result.getText());
        assertEquals(startIndex, result.getStartIndex());
        assertEquals(endIndex, result.getEndIndex());
        assertEquals("en-US,de-DE", result.getDefaultLocales().toLanguageTags());
        assertEquals(referenceTime, result.getReferenceTime());
    }
}
