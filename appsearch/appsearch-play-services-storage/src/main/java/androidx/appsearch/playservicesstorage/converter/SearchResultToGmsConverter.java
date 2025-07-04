/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage.converter;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between Gms and Jetpack versions of {@link SearchResult}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchResultToGmsConverter {
    private SearchResultToGmsConverter() {
    }

    /** Translates from Gms to Jetpack versions of list of {@link SearchResult}. */
    public static @NonNull List<SearchResult> toJetpackSearchResultList(
            @NonNull List<com.google.android.gms.appsearch.SearchResult> gmsResultList) {
        Preconditions.checkNotNull(gmsResultList);
        List<androidx.appsearch.app.SearchResult> jetpackResults =
                new ArrayList<>(gmsResultList.size());
        for (int i = 0; i < gmsResultList.size(); i++) {
            androidx.appsearch.app.SearchResult jetpackResult =
                    toJetpackSearchResult(gmsResultList.get(i));
            jetpackResults.add(jetpackResult);
        }
        return jetpackResults;
    }

    /** Translates from Gms to Jetpack versions of {@link SearchResult}. */
    public static @NonNull SearchResult toJetpackSearchResult(
            com.google.android.gms.appsearch.@NonNull SearchResult gmsResult) {
        Preconditions.checkNotNull(gmsResult);
        GenericDocument document =
                GenericDocumentToGmsConverter.toJetpackGenericDocument(
                        gmsResult.getGenericDocument());
        SearchResult.Builder builder = new SearchResult.Builder(
                gmsResult.getPackageName(),
                gmsResult.getDatabaseName())
                .setGenericDocument(document)
                .setRankingSignal(gmsResult.getRankingSignal());
        List<com.google.android.gms.appsearch.SearchResult.MatchInfo> gmsMatches =
                gmsResult.getMatchInfos();
        for (int i = 0; i < gmsMatches.size(); i++) {
            SearchResult.MatchInfo jetpackMatchInfo = toJetpackMatchInfo(
                    gmsMatches.get(i));
            builder.addMatchInfo(jetpackMatchInfo);
        }
        for (com.google.android.gms.appsearch.SearchResult joinedResult :
                gmsResult.getJoinedResults()) {
            builder.addJoinedResult(toJetpackSearchResult(joinedResult));
        }
        return builder.build();
    }

    private static SearchResult.@NonNull MatchInfo toJetpackMatchInfo(
            com.google.android.gms.appsearch.SearchResult.@NonNull MatchInfo
                    gmsMatchInfo) {
        // TODO(b/395128139): Use the new version of MatchInfo with EmbeddingMatchInfo once it's
        //  available in GmsAppSearch.
        Preconditions.checkNotNull(gmsMatchInfo);
        SearchResult.MatchInfo.Builder builder = new SearchResult.MatchInfo.Builder(
                gmsMatchInfo.getPropertyPath())
                .setExactMatchRange(
                        new SearchResult.MatchRange(
                                gmsMatchInfo.getExactMatchRange().getStart(),
                                gmsMatchInfo.getExactMatchRange().getEnd()))
                .setSnippetRange(
                        new SearchResult.MatchRange(
                                gmsMatchInfo.getSnippetRange().getStart(),
                                gmsMatchInfo.getSnippetRange().getEnd()));
        builder.setSubmatchRange(
                new SearchResult.MatchRange(
                        gmsMatchInfo.getSubmatchRange().getStart(),
                        gmsMatchInfo.getSubmatchRange().getEnd()));
        return builder.build();
    }
}
