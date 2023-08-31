/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localstorage.converter;

import static androidx.appsearch.localstorage.util.PrefixUtil.getDatabaseName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchConfig;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SnippetMatchProto;
import com.google.android.icing.proto.SnippetProto;

import java.util.ArrayList;
import java.util.Map;

/**
 * Translates a {@link SearchResultProto} into {@link SearchResult}s.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchResultToProtoConverter {
    private SearchResultToProtoConverter() {
    }

    /**
     * Translate a {@link SearchResultProto} into {@link SearchResultPage}.
     *
     * @param proto         The {@link SearchResultProto} containing results.
     * @param schemaMap     The cached Map of <Prefix, Map<PrefixedSchemaType, schemaProto>>
     *                      stores all existing prefixed schema type.
     * @return {@link SearchResultPage} of results.
     */
    @NonNull
    public static SearchResultPage toSearchResultPage(@NonNull SearchResultProto proto,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull AppSearchConfig config)
            throws AppSearchException {
        Bundle bundle = new Bundle();
        bundle.putLong(SearchResultPage.NEXT_PAGE_TOKEN_FIELD, proto.getNextPageToken());
        ArrayList<Bundle> resultBundles = new ArrayList<>(proto.getResultsCount());
        for (int i = 0; i < proto.getResultsCount(); i++) {
            SearchResult result = toUnprefixedSearchResult(proto.getResults(i), schemaMap, config);
            resultBundles.add(result.getBundle());
        }
        bundle.putParcelableArrayList(SearchResultPage.RESULTS_FIELD, resultBundles);
        return new SearchResultPage(bundle);
    }

    /**
     * Translate a {@link SearchResultProto.ResultProto} into {@link SearchResult}. The package and
     * database prefix will be removed from {@link GenericDocument}.
     *
     * @param proto          The proto to be converted.
     * @param schemaMap      The cached Map of <Prefix, Map<PrefixedSchemaType, schemaProto>>
     *                       stores all existing prefixed schema type.
     * @return A {@link SearchResult}.
     */
    @NonNull
    private static SearchResult toUnprefixedSearchResult(
            @NonNull SearchResultProto.ResultProto proto,
            @NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap,
            @NonNull AppSearchConfig config) throws AppSearchException {

        DocumentProto.Builder documentBuilder = proto.getDocument().toBuilder();
        String prefix = removePrefixesFromDocument(documentBuilder);
        Map<String, SchemaTypeConfigProto> schemaTypeMap =
                Preconditions.checkNotNull(schemaMap.get(prefix));
        GenericDocument document =
                GenericDocumentToProtoConverter.toGenericDocument(documentBuilder, prefix,
                        schemaTypeMap, config);
        SearchResult.Builder builder =
                new SearchResult.Builder(getPackageName(prefix), getDatabaseName(prefix))
                        .setGenericDocument(document).setRankingSignal(proto.getScore());
        if (proto.hasSnippet()) {
            for (int i = 0; i < proto.getSnippet().getEntriesCount(); i++) {
                SnippetProto.EntryProto entry = proto.getSnippet().getEntries(i);
                for (int j = 0; j < entry.getSnippetMatchesCount(); j++) {
                    SearchResult.MatchInfo matchInfo = toMatchInfo(
                            entry.getSnippetMatches(j), entry.getPropertyName());
                    builder.addMatchInfo(matchInfo);
                }
            }
        }
        for (int i = 0; i < proto.getJoinedResultsCount(); i++) {
            SearchResultProto.ResultProto joinedResultProto = proto.getJoinedResults(i);

            if (joinedResultProto.getJoinedResultsCount() != 0) {
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Nesting joined results within joined results not allowed.");
            }

            builder.addJoinedResult(toUnprefixedSearchResult(joinedResultProto, schemaMap, config));
        }
        return builder.build();
    }

    private static SearchResult.MatchInfo toMatchInfo(
            @NonNull SnippetMatchProto snippetMatchProto, @NonNull String propertyPath) {
        int exactMatchPosition = snippetMatchProto.getExactMatchUtf16Position();
        return new SearchResult.MatchInfo.Builder(propertyPath)
                .setExactMatchRange(
                        new SearchResult.MatchRange(
                                exactMatchPosition,
                                exactMatchPosition + snippetMatchProto.getExactMatchUtf16Length()))
                .setSubmatchRange(
                        new SearchResult.MatchRange(
                                exactMatchPosition,
                                exactMatchPosition + snippetMatchProto.getSubmatchUtf16Length()))
                .setSnippetRange(
                        new SearchResult.MatchRange(
                                snippetMatchProto.getWindowUtf16Position(),
                                snippetMatchProto.getWindowUtf16Position()
                                        + snippetMatchProto.getWindowUtf16Length()))
                .build();
    }
}
