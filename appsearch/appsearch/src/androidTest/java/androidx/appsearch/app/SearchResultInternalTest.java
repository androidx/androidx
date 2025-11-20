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

import java.util.List;
import java.util.Map;

public class SearchResultInternalTest {
    @Test
    public void testSearchResultBuilderCopyConstructor() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType").build();
        SearchResult searchResult = new SearchResult.Builder("package", "database")
                .setGenericDocument(document)
                .setRankingSignal(1.23)
                .addJoinedResult(new SearchResult.Builder("pkg1", "db1").setGenericDocument(
                        document).build())
                .addJoinedResult(new SearchResult.Builder("pkg2", "db2").setGenericDocument(
                        document).build())
                .addMatchInfo(new SearchResult.MatchInfo.Builder("propertyPath1").build())
                .addMatchInfo(new SearchResult.MatchInfo.Builder("propertyPath2").build())
                .addMatchInfo(new SearchResult.MatchInfo.Builder("propertyPath3").build())
                .build();
        SearchResult searchResultCopy = new SearchResult.Builder(searchResult).build();
        assertThat(searchResultCopy.getGenericDocument()).isEqualTo(
                searchResult.getGenericDocument());
        assertThat(searchResultCopy.getRankingSignal()).isEqualTo(searchResult.getRankingSignal());
        // Specifically test JoinedResults and MatchInfos with different sizes since briefly had
        // a bug where we looped through joinedResults using matchInfos.size()
        assertThat(searchResultCopy.getJoinedResults().size()).isEqualTo(
                searchResult.getJoinedResults().size());
        assertThat(searchResultCopy.getJoinedResults().get(0).getPackageName()).isEqualTo("pkg1");
        assertThat(searchResultCopy.getJoinedResults().get(0).getDatabaseName()).isEqualTo("db1");
        assertThat(searchResultCopy.getJoinedResults().get(1).getPackageName()).isEqualTo("pkg2");
        assertThat(searchResultCopy.getJoinedResults().get(1).getDatabaseName()).isEqualTo("db2");
        assertThat(searchResultCopy.getMatchInfos().size()).isEqualTo(
                searchResult.getMatchInfos().size());
        assertThat(searchResultCopy.getMatchInfos().get(0).getPropertyPath()).isEqualTo(
                "propertyPath1");
        assertThat(searchResultCopy.getMatchInfos().get(1).getPropertyPath()).isEqualTo(
                "propertyPath2");
        assertThat(searchResultCopy.getMatchInfos().get(2).getPropertyPath()).isEqualTo(
                "propertyPath3");
    }

    @Test
    public void testSearchResultBuilderCopyConstructor_informationalRankingSignal() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType").build();
        SearchResult searchResult = new SearchResult.Builder("package", "database")
                .setGenericDocument(document)
                .setRankingSignal(1.23)
                .addInformationalRankingSignal(2)
                .addInformationalRankingSignal(3)
                .build();
        SearchResult searchResultCopy = new SearchResult.Builder(searchResult).build();
        assertThat(searchResultCopy.getRankingSignal()).isEqualTo(searchResult.getRankingSignal());
        assertThat(searchResultCopy.getInformationalRankingSignals()).isEqualTo(
                searchResult.getInformationalRankingSignals());
    }

    @Test
    public void testSearchResultBuilderCopyConstructor_parentType() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType1").build();
        SearchResult searchResult = new SearchResult.Builder("package", "database")
                .setGenericDocument(document)
                .setParentTypeMap(Map.of(
                        "schemaType1", List.of("parent1", "parent2"),
                        "schemaType2", List.of("parent3", "parent4")
                ))
                .build();
        SearchResult searchResultCopy = new SearchResult.Builder(searchResult).build();
        assertThat(searchResultCopy.getParentTypeMap()).containsExactlyEntriesIn(
                searchResult.getParentTypeMap());
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

    @Test
    public void testMatchInfoBuilderCopyConstructor() {
        SearchResult.MatchRange exactMatchRange = new SearchResult.MatchRange(3, 8);
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        SearchResult.MatchRange snippetMatchRange = new SearchResult.MatchRange(1, 10);
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("propertyPath1")
                        .setExactMatchRange(exactMatchRange)
                        .setSubmatchRange(submatchRange)
                        .setSnippetRange(snippetMatchRange).build();
        SearchResult.MatchInfo matchInfoCopy =
                new SearchResult.MatchInfo.Builder(matchInfo).build();
        assertThat(matchInfoCopy.getPropertyPath()).isEqualTo("propertyPath1");
        assertThat(matchInfoCopy.getExactMatchRange()).isEqualTo(exactMatchRange);
        assertThat(matchInfoCopy.getSubmatchRange()).isEqualTo(submatchRange);
        assertThat(matchInfoCopy.getSnippetRange()).isEqualTo(snippetMatchRange);
    }

    @Test
    public void testMatchInfoBuilderCopyConstructor_embeddingMatchInfoEnabled() {
        SearchResult.MatchRange exactMatchRange = new SearchResult.MatchRange(3, 8);
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        SearchResult.MatchRange snippetMatchRange = new SearchResult.MatchRange(1, 10);

        double semanticScore = 1.1d;
        int queryEmbeddingVectorIndex = 1;
        int embeddingSearchMetricType = SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE;
        SearchResult.EmbeddingMatchInfo embeddingMatchInfo =
                new SearchResult.EmbeddingMatchInfo(semanticScore, queryEmbeddingVectorIndex,
                        embeddingSearchMetricType);

        SearchResult.MatchInfo matchInfo1 =
                new SearchResult.MatchInfo.Builder("propertyPath1")
                        .setExactMatchRange(exactMatchRange)
                        .setSubmatchRange(submatchRange)
                        .setSnippetRange(snippetMatchRange)
                        .build();

        SearchResult.MatchInfo matchInfo2 =
                new SearchResult.MatchInfo.Builder("propertyPath2")
                        .setEmbeddingMatch(embeddingMatchInfo)
                        .build();

        SearchResult.MatchInfo matchInfoCopy1 =
                new SearchResult.MatchInfo.Builder(matchInfo1).build();
        assertThat(matchInfoCopy1.getPropertyPath()).isEqualTo("propertyPath1");
        assertThat(matchInfoCopy1.getTextMatch()).isNotNull();
        assertThat(matchInfoCopy1.getTextMatch().getExactMatchRange()).isEqualTo(exactMatchRange);
        assertThat(matchInfoCopy1.getTextMatch().getSubmatchRange()).isEqualTo(submatchRange);
        assertThat(matchInfoCopy1.getTextMatch().getSnippetRange()).isEqualTo(snippetMatchRange);
        assertThat(matchInfoCopy1.getEmbeddingMatch()).isNull();

        SearchResult.MatchInfo matchInfoCopy2 =
                new SearchResult.MatchInfo.Builder(matchInfo2).build();
        assertThat(matchInfoCopy2.getPropertyPath()).isEqualTo("propertyPath2");
        assertThat(matchInfoCopy2.getEmbeddingMatch()).isNotNull();
        assertThat(matchInfoCopy2.getEmbeddingMatch().getSemanticScore()).isEqualTo(semanticScore);
        assertThat(matchInfoCopy2.getEmbeddingMatch().getQueryEmbeddingVectorIndex()).isEqualTo(
                queryEmbeddingVectorIndex);
        assertThat(matchInfoCopy2.getEmbeddingMatch().getEmbeddingSearchMetricType()).isEqualTo(
                embeddingSearchMetricType);
        assertThat(matchInfoCopy2.getTextMatch()).isNull();
    }
}
