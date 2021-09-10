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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.core.util.Preconditions;

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
        GenericDocument document = GenericDocumentToPlatformConverter.toJetpackGenericDocument(
                platformResult.getGenericDocument());
        SearchResult.Builder builder = new SearchResult.Builder(
                platformResult.getPackageName(), platformResult.getDatabaseName())
                .setGenericDocument(document)
                .setRankingSignal(platformResult.getRankingSignal());
        List<android.app.appsearch.SearchResult.MatchInfo> platformMatches =
                platformResult.getMatchInfos();
        for (int i = 0; i < platformMatches.size(); i++) {
            SearchResult.MatchInfo jetpackMatchInfo = toJetpackMatchInfo(platformMatches.get(i));
            builder.addMatchInfo(jetpackMatchInfo);
        }
        return builder.build();
    }

    @NonNull
    private static SearchResult.MatchInfo toJetpackMatchInfo(
            @NonNull android.app.appsearch.SearchResult.MatchInfo platformMatchInfo) {
        Preconditions.checkNotNull(platformMatchInfo);
        return new SearchResult.MatchInfo.Builder(platformMatchInfo.getPropertyPath())
                .setExactMatchRange(
                        new SearchResult.MatchRange(
                                platformMatchInfo.getExactMatchRange().getStart(),
                                platformMatchInfo.getExactMatchRange().getEnd()))
                .setSnippetRange(
                        new SearchResult.MatchRange(
                                platformMatchInfo.getSnippetRange().getStart(),
                                platformMatchInfo.getSnippetRange().getEnd()))
                .build();
    }
}
