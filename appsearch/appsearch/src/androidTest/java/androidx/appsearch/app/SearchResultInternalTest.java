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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SearchResultInternalTest {
    @Test
    public void testSearchResultBuilderCopyConstructor() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType").build();
        SearchResult searchResult = new SearchResult.Builder("package", "database")
                .setGenericDocument(document)
                .setRankingSignal(1.23)
                .addJoinedResult(new SearchResult.Builder("pkg", "db").setGenericDocument(
                        document).build())
                .addMatchInfo(new SearchResult.MatchInfo.Builder("propertyPath").build())
                .build();
        SearchResult searchResultCopy = new SearchResult.Builder(searchResult).build();
        assertThat(searchResultCopy.getGenericDocument()).isEqualTo(
                searchResult.getGenericDocument());
        assertThat(searchResultCopy.getRankingSignal()).isEqualTo(searchResult.getRankingSignal());
        assertThat(searchResultCopy.getJoinedResults().size()).isEqualTo(
                searchResult.getJoinedResults().size());
        assertThat(searchResultCopy.getMatchInfos().size()).isEqualTo(
                searchResult.getMatchInfos().size());
    }

    @Test
    public void testSearchResultBuilder_clearJoinedResults() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType").build();
        SearchResult searchResult = new SearchResult.Builder("package", "database")
                .setGenericDocument(document)
                .addJoinedResult(new SearchResult.Builder("pkg", "db").setGenericDocument(
                        document).build())
                .clearJoinedResults()
                .build();
        assertThat(searchResult.getJoinedResults()).isEmpty();
    }
}
