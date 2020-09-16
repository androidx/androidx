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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SnippetMatchProto;
import com.google.android.icing.proto.SnippetProto;

import java.util.ArrayList;
import java.util.List;


/**
 * Translates a {@link SearchResultProto} into {@link SearchResults}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchResultToProtoConverter {
    private SearchResultToProtoConverter() {}

    /** Translate a {@link SearchResultProto.ResultProto} into {@link SearchResults.Result}. */
    @NonNull
    public static SearchResults.Result convert(@NonNull SearchResultProto.ResultProto proto) {
        GenericDocument document = GenericDocumentToProtoConverter.convert(proto.getDocument());
        List<MatchInfo> matchList = null;
        if (proto.hasSnippet()) {
            matchList = new ArrayList<>();
            for (int i = 0; i < proto.getSnippet().getEntriesCount(); i++) {
                SnippetProto.EntryProto entry = proto.getSnippet().getEntries(i);
                for (int j = 0; j < entry.getSnippetMatchesCount(); j++) {
                    matchList.add(convertToMatchInfo(
                            entry.getSnippetMatches(j), entry.getPropertyName(), document));
                }
            }
        }
        return new SearchResults.Result(document, matchList);
    }

    private static MatchInfo convertToMatchInfo(SnippetMatchProto snippetMatchProto,
            String propertyName, GenericDocument document) {
        return new MatchInfo.Builder(document)
                .setPropertyPath(propertyName)
                .setValuesIndex(snippetMatchProto.getValuesIndex())
                .setExactMatchPositionRange(snippetMatchProto.getExactMatchPosition(),
                        snippetMatchProto.getExactMatchPosition()
                                + snippetMatchProto.getExactMatchBytes())
                .setWindowPositionRange(snippetMatchProto.getWindowPosition(),
                        snippetMatchProto.getWindowPosition() + snippetMatchProto.getWindowBytes())
                .build();
    }

    /** Translates a {@link SearchResultProto} into a list of {@link SearchResults.Result}. */
    @NonNull
    public static List<SearchResults.Result> toResults(
            @NonNull SearchResultProto searchResultProto) {
        List<SearchResults.Result> results =
                new ArrayList<>(searchResultProto.getResultsCount());
        for (int i = 0; i < searchResultProto.getResultsCount(); i++) {
            results.add(convert(searchResultProto.getResults(i)));
        }
        return results;
    }
}
