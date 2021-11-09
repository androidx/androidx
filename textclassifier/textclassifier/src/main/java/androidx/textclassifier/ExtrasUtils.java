/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Utilities for inserting/retrieving data into/from textclassifier related results and intents.
 */
public final class ExtrasUtils {

    private static final String TAG = "ExtrasUtils";

    private static final String EXTRA_FROM_TEXT_CLASSIFIER =
            "android.view.textclassifier.extra.FROM_TEXT_CLASSIFIER";
    private static final String ENTITY_TYPE = "entity-type";
    private static final String SCORE = "score";
    private static final String TEXT_LANGUAGES = "text-languages";

    private ExtrasUtils() {}

    /**
     * Returns the highest scoring language found in the textclassifier extras in the intent.
     * This may return null if the data could not be found.
     *
     * @param intent the intent used to start the activity.
     * @see android.app.Activity#getIntent()
     */
    @Nullable
    public static Locale getTopLanguage(@Nullable Intent intent) {
        try {
            // NOTE: This is (and should be) a copy of the related platform code.
            // It is hard to test this code returns something on a given platform because we can't
            // guarantee the TextClassifier implementation that will be used to send the intent.
            // Depend on the platform tests instead and avoid this code running out of sync with
            // what is expected of each platform. Note that the code may differ from platform to
            // platform but that will be a bad idea as it will be hard to manage.
            // TODO: Include a "put" counterpart of this method so that other TextClassifier
            // implementations may use it to put language data into the generated intent in a way
            // that this method can retrieve it.
            if (intent == null) {
                return null;
            }
            final Bundle tcBundle = intent.getBundleExtra(EXTRA_FROM_TEXT_CLASSIFIER);
            if (tcBundle == null) {
                return null;
            }
            final Bundle textLanguagesExtra = tcBundle.getBundle(TEXT_LANGUAGES);
            if (textLanguagesExtra == null) {
                return null;
            }
            final String[] languages = textLanguagesExtra.getStringArray(ENTITY_TYPE);
            final float[] scores = textLanguagesExtra.getFloatArray(SCORE);
            if (languages == null || scores == null
                    || languages.length == 0 || languages.length != scores.length) {
                return null;
            }
            int highestScoringIndex = 0;
            for (int i = 1; i < languages.length; i++) {
                if (scores[highestScoringIndex] < scores[i]) {
                    highestScoringIndex = i;
                }
            }
            final LocaleListCompat localeList =
                    LocaleListCompat.forLanguageTags(languages[highestScoringIndex]);
            return localeList.isEmpty() ? null : localeList.get(0);
        } catch (Throwable t) {
            // Prevent this method from crashing the process.
            Log.e(TAG, "Error retrieving language information from textclassifier intent", t);
            return null;
        }
    }

    /**
     * Returns a fake TextClassifier generated intent for testing purposes.
     * @param languages ordered list of languages for the classified text
     */
    @VisibleForTesting
    static Intent buildFakeTextClassifierIntent(String... languages) {
        final float[] scores = new float[languages.length];
        float scoresLeft = 1f;
        for (int i = 0; i < scores.length; i++) {
            scores[i] = scoresLeft /= 2;
        }
        final Bundle textLanguagesExtra = new Bundle();
        textLanguagesExtra.putStringArray(ENTITY_TYPE, languages);
        textLanguagesExtra.putFloatArray(SCORE, scores);
        final Bundle tcBundle = new Bundle();
        tcBundle.putBundle(TEXT_LANGUAGES, textLanguagesExtra);
        return new Intent(Intent.ACTION_VIEW).putExtra(EXTRA_FROM_TEXT_CLASSIFIER, tcBundle);
    }
}
