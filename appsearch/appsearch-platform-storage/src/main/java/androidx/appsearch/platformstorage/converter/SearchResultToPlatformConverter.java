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

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Translates between Platform and Jetpack versions of {@link SearchResult}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public class SearchResultToPlatformConverter {
    private static final String TAG = "AppSearchSearchResPlatC";

    private SearchResultToPlatformConverter() {}

    /** Translates from Platform to Jetpack versions of {@link SearchResult}. */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull SearchResult toJetpackSearchResult(
            android.app.appsearch.@NonNull SearchResult platformResult) {
        Preconditions.checkNotNull(platformResult);
        GenericDocument document = GenericDocumentToPlatformConverter.toJetpackGenericDocument(
                platformResult.getGenericDocument());
        SearchResult.Builder builder = new SearchResult.Builder(platformResult.getPackageName(),
                platformResult.getDatabaseName())
                .setGenericDocument(document)
                .setRankingSignal(platformResult.getRankingSignal());
        List<android.app.appsearch.SearchResult.MatchInfo> platformMatches =
                platformResult.getMatchInfos();
        for (int i = 0; i < platformMatches.size(); i++) {
            SearchResult.MatchInfo jetpackMatchInfo = toJetpackMatchInfo(platformMatches.get(i));
            builder.addMatchInfo(jetpackMatchInfo);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            for (android.app.appsearch.SearchResult joinedResult :
                    ApiHelperForU.getJoinedResults(platformResult)) {
                builder.addJoinedResult(toJetpackSearchResult(joinedResult));
            }
        }
        if (AppSearchVersionUtil.isAtLeastB()) {
            List<Double> informationalRankingSignals =
                    ApiHelperForB.getInformationalRankingSignals(platformResult);
            for (int i = 0; i < informationalRankingSignals.size(); i++) {
                builder.addInformationalRankingSignal(informationalRankingSignals.get(i));
            }

            try {
                // TODO(b/371610934): Ensure the parent type map is set for older devices receiving
                //  mainline updates. AppSearch will relocate parent type information from
                //  GenericDocument to SearchResult in new versions. Omitting this step will result
                //  in missing parent data and incorrect polymorphic deserialization behavior for
                //  GenericDocument.
                builder.setParentTypeMap(ApiHelperForB.getParentTypeMap(platformResult));
            } catch (NoSuchMethodError e) {
                // Catch NoSuchMethodError thrown by older pre-release Android B devices that may
                // not have the getParentTypeMap method. This is a temporary workaround until all
                // B devices have the method available.
                Log.e(TAG, "Failed to set parent type map.", e);
            }
        }
        return builder.build();
    }

    private static SearchResult.@NonNull MatchInfo toJetpackMatchInfo(
            android.app.appsearch.SearchResult.@NonNull MatchInfo platformMatchInfo) {
        // TODO(b/395128139): Use the new version of MatchInfo with EmbeddingMatchInfo once it's
        //  available in platform.
        Preconditions.checkNotNull(platformMatchInfo);
        SearchResult.MatchInfo.Builder builder = new SearchResult.MatchInfo.Builder(
                platformMatchInfo.getPropertyPath())
                .setExactMatchRange(
                        new SearchResult.MatchRange(
                                platformMatchInfo.getExactMatchRange().getStart(),
                                platformMatchInfo.getExactMatchRange().getEnd()))
                .setSnippetRange(
                        new SearchResult.MatchRange(
                                platformMatchInfo.getSnippetRange().getStart(),
                                platformMatchInfo.getSnippetRange().getEnd()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setSubmatchRange(
                    new SearchResult.MatchRange(
                            ApiHelperForT.getSubmatchRangeStart(platformMatchInfo),
                            ApiHelperForT.getSubmatchRangeEnd(platformMatchInfo)));
        }
        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static class ApiHelperForT {
        private ApiHelperForT() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getSubmatchRangeStart(
                android.app.appsearch.SearchResult.@NonNull MatchInfo platformMatchInfo) {
            return platformMatchInfo.getSubmatchRange().getStart();
        }

        @DoNotInline
        static int getSubmatchRangeEnd(
                android.app.appsearch.SearchResult.@NonNull MatchInfo platformMatchInfo) {
            return platformMatchInfo.getSubmatchRange().getEnd();
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static class ApiHelperForU {
        private ApiHelperForU() {
            // This class is not instantiable.
        }

        @DoNotInline
        static List<android.app.appsearch.SearchResult> getJoinedResults(
                android.app.appsearch.@NonNull SearchResult result) {
            return result.getJoinedResults();
        }
    }

    @RequiresApi(36)
    private static class ApiHelperForB {
        private ApiHelperForB() {
        }

        @DoNotInline
        @SuppressLint("NewApi") // getInformationalRankingSignals() incorrectly flagged as 34-ext16
        static List<Double> getInformationalRankingSignals(
                android.app.appsearch.@NonNull SearchResult result) {
            return result.getInformationalRankingSignals();
        }

        @DoNotInline
        static Map<String, List<String>> getParentTypeMap(
                android.app.appsearch.@NonNull SearchResult result) {
            return result.getParentTypeMap();
        }
    }
}
