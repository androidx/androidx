/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between Platform and Jetpack versions of {@link SearchResult}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public class SearchResultToPlatformConverter {
    private SearchResultToPlatformConverter() {}

    /** Translates from Platform to Jetpack versions of {@link SearchResult}. */
    @NonNull
    public static SearchResult toJetpackSearchResult(
            @NonNull android.app.appsearch.SearchResult platformResult) {
        Preconditions.checkNotNull(platformResult);
        Bundle bundle = new Bundle();
        GenericDocument document = GenericDocumentToPlatformConverter.toJetpackGenericDocument(
                platformResult.getDocument());
        bundle.putBundle(SearchResult.DOCUMENT_FIELD, document.getBundle());
        bundle.putString(SearchResult.PACKAGE_NAME_FIELD, platformResult.getPackageName());
        bundle.putString(SearchResult.DATABASE_NAME_FIELD, platformResult.getDatabaseName());

        List<android.app.appsearch.SearchResult.MatchInfo> platformMatches =
                platformResult.getMatches();
        ArrayList<Bundle> jetpackMatches = new ArrayList<>(platformMatches.size());
        for (int i = 0; i < platformMatches.size(); i++) {
            Bundle jetpackMatchInfoBundle = convertToJetpackMatchInfoBundle(platformMatches.get(i));
            jetpackMatches.add(jetpackMatchInfoBundle);
        }
        bundle.putParcelableArrayList(SearchResult.MATCHES_FIELD, jetpackMatches);
        return new SearchResult(bundle);
    }

    @NonNull
    private static Bundle convertToJetpackMatchInfoBundle(
            @NonNull android.app.appsearch.SearchResult.MatchInfo platformMatchInfo) {
        Preconditions.checkNotNull(platformMatchInfo);
        Bundle bundle = new Bundle();
        bundle.putString(
                SearchResult.MatchInfo.PROPERTY_PATH_FIELD, platformMatchInfo.getPropertyPath());
        bundle.putInt(
                SearchResult.MatchInfo.EXACT_MATCH_POSITION_LOWER_FIELD,
                platformMatchInfo.getExactMatchPosition().getStart());
        bundle.putInt(
                SearchResult.MatchInfo.EXACT_MATCH_POSITION_UPPER_FIELD,
                platformMatchInfo.getExactMatchPosition().getEnd());
        bundle.putInt(
                SearchResult.MatchInfo.WINDOW_POSITION_LOWER_FIELD,
                platformMatchInfo.getSnippetPosition().getStart());
        bundle.putInt(
                SearchResult.MatchInfo.WINDOW_POSITION_UPPER_FIELD,
                platformMatchInfo.getSnippetPosition().getEnd());
        return bundle;
    }
}
