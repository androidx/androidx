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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.List;
import java.util.Map;

public class SearchResultCtsTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildSearchResult() {
        SearchResult.MatchRange exactMatchRange = new SearchResult.MatchRange(3, 8);
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        SearchResult.MatchRange snippetMatchRange = new SearchResult.MatchRange(1, 10);
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("body")
                        .setExactMatchRange(exactMatchRange)
                        .setSubmatchRange(submatchRange)
                        .setSnippetRange(snippetMatchRange).build();

        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .setRankingSignal(2.9)
                .build();

        assertThat(searchResult.getPackageName()).isEqualTo("packageName");
        assertThat(searchResult.getDatabaseName()).isEqualTo("databaseName");
        assertThat(searchResult.getRankingSignal()).isEqualTo(2.9);
        assertThat(searchResult.getGenericDocument()).isEqualTo(email);
        assertThat(searchResult.getMatchInfos()).hasSize(1);
        SearchResult.MatchInfo actualMatchInfo = searchResult.getMatchInfos().get(0);
        assertThat(actualMatchInfo.getPropertyPath()).isEqualTo("body");
        assertThat(actualMatchInfo.getPropertyPathObject()).isEqualTo(new PropertyPath("body"));
        assertThat(actualMatchInfo.getExactMatchRange()).isEqualTo(exactMatchRange);
        assertThat(actualMatchInfo.getSubmatchRange()).isEqualTo(submatchRange);
        assertThat(actualMatchInfo.getSnippetRange()).isEqualTo(snippetMatchRange);
        assertThat(actualMatchInfo.getExactMatch().toString()).isEqualTo("lo Wo");
        assertThat(actualMatchInfo.getSubmatch().toString()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSnippet().toString()).isEqualTo("ello Worl");
        assertThat(actualMatchInfo.getFullText()).isEqualTo("Hello World.");
    }

    @Test
    public void testBuildSearchResult_matchInfoGetters() {
        SearchResult.MatchRange exactMatchRange = new SearchResult.MatchRange(3, 8);
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        SearchResult.MatchRange snippetMatchRange = new SearchResult.MatchRange(1, 10);
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("body")
                        .setExactMatchRange(exactMatchRange)
                        .setSubmatchRange(submatchRange)
                        .setSnippetRange(snippetMatchRange).build();

        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .setRankingSignal(2.9)
                .build();

        assertThat(searchResult.getPackageName()).isEqualTo("packageName");
        assertThat(searchResult.getDatabaseName()).isEqualTo("databaseName");
        assertThat(searchResult.getRankingSignal()).isEqualTo(2.9);
        assertThat(searchResult.getGenericDocument()).isEqualTo(email);
        assertThat(searchResult.getMatchInfos()).hasSize(1);
        SearchResult.MatchInfo actualMatchInfo = searchResult.getMatchInfos().get(0);
        assertThat(actualMatchInfo.getPropertyPath()).isEqualTo("body");
        assertThat(actualMatchInfo.getPropertyPathObject()).isEqualTo(new PropertyPath("body"));
        assertThat(actualMatchInfo.getExactMatchRange()).isEqualTo(exactMatchRange);
        assertThat(actualMatchInfo.getSubmatchRange()).isEqualTo(submatchRange);
        assertThat(actualMatchInfo.getSnippetRange()).isEqualTo(snippetMatchRange);
        assertThat(actualMatchInfo.getExactMatch().toString()).isEqualTo("lo Wo");
        assertThat(actualMatchInfo.getSubmatch().toString()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSnippet().toString()).isEqualTo("ello Worl");
        assertThat(actualMatchInfo.getFullText()).isEqualTo("Hello World.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
    public void testBuildSearchResult_textMatchInfoGetters() {
        SearchResult.MatchRange exactMatchRange = new SearchResult.MatchRange(3, 8);
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        SearchResult.MatchRange snippetMatchRange = new SearchResult.MatchRange(1, 10);
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("body")
                        .setExactMatchRange(exactMatchRange)
                        .setSubmatchRange(submatchRange)
                        .setSnippetRange(snippetMatchRange).build();

        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .setRankingSignal(2.9)
                .build();

        assertThat(searchResult.getPackageName()).isEqualTo("packageName");
        assertThat(searchResult.getDatabaseName()).isEqualTo("databaseName");
        assertThat(searchResult.getRankingSignal()).isEqualTo(2.9);
        assertThat(searchResult.getGenericDocument()).isEqualTo(email);
        assertThat(searchResult.getMatchInfos()).hasSize(1);
        SearchResult.MatchInfo actualMatchInfo = searchResult.getMatchInfos().get(0);
        assertThat(actualMatchInfo.getPropertyPath()).isEqualTo("body");
        assertThat(actualMatchInfo.getPropertyPathObject()).isEqualTo(new PropertyPath("body"));
        assertThat(actualMatchInfo.getTextMatch()).isNotNull();
        assertThat(actualMatchInfo.getEmbeddingMatch()).isNull();
        assertThat(actualMatchInfo.getTextMatch().getExactMatchRange()).isEqualTo(exactMatchRange);
        assertThat(actualMatchInfo.getTextMatch().getSubmatchRange()).isEqualTo(submatchRange);
        assertThat(actualMatchInfo.getTextMatch().getSnippetRange()).isEqualTo(snippetMatchRange);
        assertThat(actualMatchInfo.getTextMatch().getExactMatch()).isEqualTo("lo Wo");
        assertThat(actualMatchInfo.getTextMatch().getSubmatch()).isEqualTo("lo");
        assertThat(actualMatchInfo.getTextMatch().getSnippet()).isEqualTo("ello Worl");
        assertThat(actualMatchInfo.getTextMatch().getFullText()).isEqualTo("Hello World.");
    }

    @Test
    public void testMatchRange() {
        SearchResult.MatchRange matchRange = new SearchResult.MatchRange(13, 47);
        assertThat(matchRange.getStart()).isEqualTo(13);
        assertThat(matchRange.getEnd()).isEqualTo(47);
    }

    @Test
    public void testSubmatchRangeNotSet() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("body").build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .build();

        // When submatch isn't set, calling getSubmatch and getSubmatchRange should throw.
        final SearchResult.MatchInfo actualMatchInfoNoSubmatch =
                searchResult.getMatchInfos().get(0);
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getSubmatch());
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getSubmatchRange());

        // When submatch is set, calling getSubmatch and getSubmatchRange should return the
        // submatch without any problems.
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        matchInfo = new SearchResult.MatchInfo.Builder("body").setSubmatchRange(
                submatchRange).build();
        searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .build();
        final SearchResult.MatchInfo actualMatchInfo = searchResult.getMatchInfos().get(0);
        assertThat(actualMatchInfo.getSubmatch().toString()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSubmatchRange()).isEqualTo(submatchRange);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
    public void testSubmatchRangeNotSet_textMatchInfoGetters() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult.MatchInfo matchInfo =
                new SearchResult.MatchInfo.Builder("body").build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .build();

        // When submatch isn't set, calling getSubmatch and getSubmatchRange should throw.
        final SearchResult.MatchInfo actualMatchInfoNoSubmatch =
                searchResult.getMatchInfos().get(0);
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getSubmatch());
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getSubmatchRange());
        // TextMatchInfo getters
        assertThat(actualMatchInfoNoSubmatch.getTextMatch()).isNotNull();
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getTextMatch().getSubmatch());
        assertThrows(UnsupportedOperationException.class,
                () -> actualMatchInfoNoSubmatch.getTextMatch().getSubmatchRange());

        // When submatch is set, calling getSubmatch and getSubmatchRange should return the
        // submatch without any problems.
        SearchResult.MatchRange submatchRange = new SearchResult.MatchRange(3, 5);
        matchInfo = new SearchResult.MatchInfo.Builder("body").setSubmatchRange(
                submatchRange).build();
        searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .addMatchInfo(matchInfo)
                .build();
        final SearchResult.MatchInfo actualMatchInfo = searchResult.getMatchInfos().get(0);
        assertThat(actualMatchInfo.getSubmatch()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSubmatchRange()).isEqualTo(submatchRange);
        // TextMatchInfo getters
        assertThat(actualMatchInfo.getTextMatch()).isNotNull();
        assertThat(actualMatchInfo.getTextMatch().getSubmatch()).isEqualTo("lo");
        assertThat(actualMatchInfo.getTextMatch().getSubmatchRange()).isEqualTo(submatchRange);
    }

    @Test
    public void testJoinedDocument() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        AppSearchEmail joinDoc = new AppSearchEmail.Builder("namespace1", "id2")
                .setBody("Joined document.")
                .build();
        SearchResult joinSearchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(joinDoc)
                .build();

        SearchResult withoutJoin = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .build();
        SearchResult withJoin = new SearchResult.Builder("packageName", "databaseName")
                .addJoinedResult(joinSearchResult)
                .setGenericDocument(email)
                .build();

        assertThat(withoutJoin.getJoinedResults()).hasSize(0);

        assertThat(withJoin.getJoinedResults()).hasSize(1);
        SearchResult actualJoined = withJoin.getJoinedResults().get(0);
        assertThat(actualJoined.getGenericDocument()).isEqualTo(joinDoc);
    }

    @Test
    public void testRebuild() {
        AppSearchEmail doc1 = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Parent document.")
                .build();

        AppSearchEmail joinDoc1 = new AppSearchEmail.Builder("namespace1", "id2")
                .setBody("Joined document.")
                .build();
        AppSearchEmail joinDoc2 = new AppSearchEmail.Builder("namespace1", "id3")
                .setBody("Joined document.")
                .build();

        SearchResult joinSearchResult1 = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(joinDoc1)
                .build();
        SearchResult joinSearchResult2 = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(joinDoc2)
                .build();

        SearchResult.Builder searchResultBuilder =
                new SearchResult.Builder("packageName", "databaseName")
                        .setGenericDocument(doc1)
                        .addJoinedResult(joinSearchResult1);

        SearchResult original = searchResultBuilder.build();
        SearchResult rebuild = searchResultBuilder.addJoinedResult(joinSearchResult2).build();

        // Rebuild won't effect the original object
        assertThat(original.getJoinedResults()).hasSize(1);
        SearchResult originalJoinedResult = original.getJoinedResults().get(0);
        assertThat(originalJoinedResult.getGenericDocument().getId()).isEqualTo("id2");

        assertThat(rebuild.getJoinedResults()).hasSize(2);
        SearchResult rebuildJoinedResult1 = rebuild.getJoinedResults().get(0);
        assertThat(rebuildJoinedResult1.getGenericDocument().getId()).isEqualTo("id2");
        SearchResult rebuildJoinedResult2 = rebuild.getJoinedResults().get(1);
        assertThat(rebuildJoinedResult2.getGenericDocument().getId()).isEqualTo("id3");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public void testBuildSearchResult_informationalRankingSignals() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .setRankingSignal(2.9)
                .addInformationalRankingSignal(3.0)
                .addInformationalRankingSignal(4.0)
                .build();

        assertThat(searchResult.getRankingSignal()).isEqualTo(2.9);
        assertThat(searchResult.getInformationalRankingSignals())
                .containsExactly(3.0, 4.0).inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public void testRebuild_informationalRankingSignals() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();

        SearchResult.Builder searchResultBuilder =
                new SearchResult.Builder("packageName", "databaseName")
                        .setGenericDocument(email)
                        .setRankingSignal(2.9)
                        .addInformationalRankingSignal(3.0)
                        .addInformationalRankingSignal(4.0);

        SearchResult original = searchResultBuilder.build();
        SearchResult rebuild = searchResultBuilder.addInformationalRankingSignal(5).build();

        // Rebuild won't effect the original object
        assertThat(original.getRankingSignal()).isEqualTo(2.9);
        assertThat(original.getInformationalRankingSignals()).containsExactly(3.0, 4.0).inOrder();

        assertThat(rebuild.getRankingSignal()).isEqualTo(2.9);
        assertThat(rebuild.getInformationalRankingSignals())
                .containsExactly(3.0, 4.0, 5.0).inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
    public void testBuildSearchResult_parentTypeMap() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();
        SearchResult searchResult = new SearchResult.Builder("packageName", "databaseName")
                .setGenericDocument(email)
                .setParentTypeMap(Map.of(
                        "schema1", List.of("parent1", "parent2"),
                        "schema2", List.of("parent3", "parent4")
                ))
                .build();

        assertThat(searchResult.getParentTypeMap())
                .containsExactly(
                        "schema1", List.of("parent1", "parent2"),
                        "schema2", List.of("parent3", "parent4")
                ).inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
    public void testRebuild_parentTypeMap() {
        AppSearchEmail email = new AppSearchEmail.Builder("namespace1", "id1")
                .setBody("Hello World.")
                .build();

        SearchResult.Builder searchResultBuilder =
                new SearchResult.Builder("packageName", "databaseName")
                        .setGenericDocument(email)
                        .setParentTypeMap(Map.of(
                                "schema1", List.of("parent1", "parent2"),
                                "schema2", List.of("parent3", "parent4")
                        ));

        SearchResult original = searchResultBuilder.build();
        SearchResult rebuild = searchResultBuilder
                .setParentTypeMap(Map.of("schema3", List.of("parent5", "parent6"))).build();

        // Rebuild won't effect the original object
        assertThat(original.getParentTypeMap())
                .containsExactly(
                        "schema1", List.of("parent1", "parent2"),
                        "schema2", List.of("parent3", "parent4")
                ).inOrder();

        assertThat(rebuild.getParentTypeMap())
                .containsExactly("schema3", List.of("parent5", "parent6")).inOrder();
    }
}
