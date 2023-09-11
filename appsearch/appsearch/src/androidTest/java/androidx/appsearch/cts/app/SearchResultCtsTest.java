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
import androidx.appsearch.testutil.AppSearchEmail;

import org.junit.Test;

public class SearchResultCtsTest {

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
        assertThat(actualMatchInfo.getExactMatch()).isEqualTo("lo Wo");
        assertThat(actualMatchInfo.getSubmatch()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSnippet()).isEqualTo("ello Worl");
        assertThat(actualMatchInfo.getFullText()).isEqualTo("Hello World.");
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
        assertThat(actualMatchInfo.getSubmatch()).isEqualTo("lo");
        assertThat(actualMatchInfo.getSubmatchRange()).isEqualTo(submatchRange);
    }

    @Test
    /*@exportToFramework:SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)*/
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
}
