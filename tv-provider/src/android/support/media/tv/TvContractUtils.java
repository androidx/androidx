/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.media.tv;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.media.tv.TvContentRating;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Static helper methods for working with {@link android.media.tv.TvContract}.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TvContractUtils {

    static final TvContentRating[] EMPTY = new TvContentRating[0];

    private static final String TAG = "TvContractUtils";
    private static final boolean DEBUG = false;
    private static final String DELIMITER = ",";

    /**
     * Parses a string of comma-separated ratings into an array of {@link TvContentRating}.
     * <p>Invalid strings are droppped. Duplicates are not removed. The order is preserved.</p>
     *
     * @param commaSeparatedRatings String containing various ratings, separated by commas.
     * @return An array of TvContentRatings.
     */
    public static TvContentRating[] stringToContentRatings(String commaSeparatedRatings) {
        if (TextUtils.isEmpty(commaSeparatedRatings)) {
            return EMPTY;
        }
        String[] ratings = commaSeparatedRatings.split("\\s*,\\s*");
        List<TvContentRating> contentRatings = new ArrayList<>(ratings.length);
        for (String rating : ratings) {
            try {
                contentRatings.add(TvContentRating.unflattenFromString(rating));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Can't parse the content rating: '" + rating + "', skipping", e);
            }
        }
        return contentRatings.size() == 0 ? EMPTY
                : contentRatings.toArray(new TvContentRating[contentRatings.size()]);
    }

    /**
     * Flattens an array of {@link TvContentRating} into a String to be inserted into a database.
     *
     * @param contentRatings An array of TvContentRatings.
     * @return A comma-separated String of ratings.
     */
    public static String contentRatingsToString(TvContentRating[] contentRatings) {
        if (contentRatings == null || contentRatings.length == 0) {
            return null;
        }
        StringBuilder ratings = new StringBuilder(contentRatings[0].flattenToString());
        for (int i = 1; i < contentRatings.length; ++i) {
            ratings.append(DELIMITER);
            ratings.append(contentRatings[i].flattenToString());
        }
        return ratings.toString();
    }

    /**
     * Parses a string of comma-separated audio languages into an array of audio language strings.
     *
     * @param commaSeparatedString String containing audio languages, separated by commas.
     * @return An array of audio language.
     */
    public static String[] stringToAudioLanguages(String commaSeparatedString) {
        if (TextUtils.isEmpty(commaSeparatedString)) {
            return null;
        }
        return commaSeparatedString.split("\\s*,\\s*");
    }

    /**
     * Concatenate an array of audio languages into a String to be inserted into a database.
     *
     * @param audioLanguages An array of audio languages.
     * @return A comma-separated String of audio languages.
     */
    public static String audioLanguagesToString(String[] audioLanguages) {
        if (audioLanguages == null || audioLanguages.length == 0) {
            return null;
        }
        StringBuilder ratings = new StringBuilder(audioLanguages[0]);
        for (int i = 1; i < audioLanguages.length; ++i) {
            ratings.append(DELIMITER);
            ratings.append(audioLanguages[i]);
        }
        return ratings.toString();
    }

    private TvContractUtils() {
    }
}
