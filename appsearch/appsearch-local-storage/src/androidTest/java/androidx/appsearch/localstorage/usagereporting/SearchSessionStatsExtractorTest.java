/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.localstorage.usagereporting;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.localstorage.stats.SearchIntentStats;
import androidx.appsearch.localstorage.stats.SearchSessionStats;
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.SearchAction;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SearchSessionStatsExtractorTest {
    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final String TEST_DATABASE = "database";

    @Test
    public void testExtract() {
        // Create search action and click action generic documents.
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("tes")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("tes")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(512)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                        .build();
        GenericDocument clickAction2 =
                new ClickActionGenericDocument.Builder("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(3000)
                        .setQuery("tes")
                        .setResultRankInBlock(3)
                        .setResultRankGlobal(6)
                        .setTimeStayOnResultMillis(1024)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc2")
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(5000)
                        .setQuery("test")
                        .setFetchedResultCount(10)
                        .build();
        GenericDocument clickAction3 =
                new ClickActionGenericDocument.Builder("namespace", "click3", "builtin:ClickAction")
                        .setCreationTimestampMillis(6000)
                        .setQuery("test")
                        .setResultRankInBlock(2)
                        .setResultRankGlobal(4)
                        .setTimeStayOnResultMillis(512)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc3")
                        .build();
        GenericDocument clickAction4 =
                new ClickActionGenericDocument.Builder("namespace", "click4", "builtin:ClickAction")
                        .setCreationTimestampMillis(7000)
                        .setQuery("test")
                        .setResultRankInBlock(4)
                        .setResultRankGlobal(8)
                        .setTimeStayOnResultMillis(256)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc4")
                        .build();
        GenericDocument clickAction5 =
                new ClickActionGenericDocument.Builder("namespace", "click5", "builtin:ClickAction")
                        .setCreationTimestampMillis(8000)
                        .setQuery("test")
                        .setResultRankInBlock(6)
                        .setResultRankGlobal(12)
                        .setTimeStayOnResultMillis(2048)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc5")
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(
                        searchAction1,
                        clickAction1,
                        clickAction2,
                        searchAction2,
                        clickAction3,
                        clickAction4,
                        clickAction5);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(2);

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("tes");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(20);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).hasSize(2);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankInBlock()).isEqualTo(1);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankGlobal()).isEqualTo(2);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(searchIntentStats0.getClicksStats().get(0).isGoodClick()).isFalse();
        assertThat(searchIntentStats0.getClicksStats().get(1).getTimestampMillis()).isEqualTo(3000);
        assertThat(searchIntentStats0.getClicksStats().get(1).getResultRankInBlock()).isEqualTo(3);
        assertThat(searchIntentStats0.getClicksStats().get(1).getResultRankGlobal()).isEqualTo(6);
        assertThat(searchIntentStats0.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(1024);
        assertThat(searchIntentStats0.getClicksStats().get(1).isGoodClick()).isFalse();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(5000);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("tes");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(10);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).hasSize(3);
        assertThat(searchIntentStats1.getClicksStats().get(0).getTimestampMillis()).isEqualTo(6000);
        assertThat(searchIntentStats1.getClicksStats().get(0).getResultRankInBlock()).isEqualTo(2);
        assertThat(searchIntentStats1.getClicksStats().get(0).getResultRankGlobal()).isEqualTo(4);
        assertThat(searchIntentStats1.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(searchIntentStats1.getClicksStats().get(0).isGoodClick()).isFalse();
        assertThat(searchIntentStats1.getClicksStats().get(1).getTimestampMillis()).isEqualTo(7000);
        assertThat(searchIntentStats1.getClicksStats().get(1).getResultRankInBlock()).isEqualTo(4);
        assertThat(searchIntentStats1.getClicksStats().get(1).getResultRankGlobal()).isEqualTo(8);
        assertThat(searchIntentStats1.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(256);
        assertThat(searchIntentStats1.getClicksStats().get(1).isGoodClick()).isFalse();
        assertThat(searchIntentStats1.getClicksStats().get(2).getTimestampMillis()).isEqualTo(8000);
        assertThat(searchIntentStats1.getClicksStats().get(2).getResultRankInBlock()).isEqualTo(6);
        assertThat(searchIntentStats1.getClicksStats().get(2).getResultRankGlobal()).isEqualTo(12);
        assertThat(searchIntentStats1.getClicksStats().get(2).getTimeStayOnResultMillis())
                .isEqualTo(2048);
        assertThat(searchIntentStats1.getClicksStats().get(2).isGoodClick()).isTrue();
    }

    @Test
    public void testExtract_noSearchActionShouldReturnEmptyList() {
        // Create search action and click action generic documents.
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("tes")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(512)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                        .build();
        GenericDocument clickAction2 =
                new ClickActionGenericDocument.Builder("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(3000)
                        .setQuery("tes")
                        .setResultRankInBlock(3)
                        .setResultRankGlobal(6)
                        .setTimeStayOnResultMillis(1024)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc2")
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(clickAction1, clickAction2);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);
        assertThat(result).isEmpty();
    }

    @Test
    public void testExtract_shouldSkipUnknownActionTypeDocuments() {
        // Create search action and click action generic documents.
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("tes")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument clickAction1 =
                new GenericDocument.Builder<>("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setPropertyString("query", "tes")
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                        .setPropertyLong("resultRankInBlock", 1)
                        .setPropertyLong("resultRankGlobal", 2)
                        .setPropertyLong("timeStayOnResultMillis", 512)
                        .build();
        GenericDocument clickAction2 =
                new ClickActionGenericDocument.Builder("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(3000)
                        .setQuery("tes")
                        .setResultRankInBlock(3)
                        .setResultRankGlobal(6)
                        .setTimeStayOnResultMillis(1024)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc2")
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, clickAction1, clickAction2);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(1);

        // Since clickAction1 doesn't have property "actionType", it should be skipped without
        // throwing any exception.
        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("tes");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(20);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).hasSize(1);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimestampMillis()).isEqualTo(3000);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankInBlock()).isEqualTo(3);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankGlobal()).isEqualTo(6);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(1024);
        assertThat(searchIntentStats0.getClicksStats().get(0).isGoodClick()).isFalse();
    }

// @exportToFramework:startStrip()
    @Test
    public void testExtract_builtFromDocumentClass() throws Exception {
        SearchAction searchAction1 =
                new SearchAction.Builder("namespace", "search1", /* actionTimestampMillis= */1000)
                        .setQuery("tes")
                        .setFetchedResultCount(20)
                        .build();
        ClickAction clickAction1 =
                new ClickAction.Builder("namespace", "click1", /* actionTimestampMillis= */2000)
                        .setQuery("tes")
                        .setReferencedQualifiedId("pkg$db/ns#doc1")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(512)
                        .build();
        ClickAction clickAction2 =
                new ClickAction.Builder("namespace", "click2", /* actionTimestampMillis= */3000)
                        .setQuery("tes")
                        .setReferencedQualifiedId("pkg$db/ns#doc2")
                        .setResultRankInBlock(3)
                        .setResultRankGlobal(6)
                        .setTimeStayOnResultMillis(1024)
                        .build();
        SearchAction searchAction2 =
                new SearchAction.Builder("namespace", "search2", /* actionTimestampMillis= */5000)
                        .setQuery("test")
                        .setFetchedResultCount(10)
                        .build();
        ClickAction clickAction3 =
                new ClickAction.Builder("namespace", "click3", /* actionTimestampMillis= */6000)
                        .setQuery("test")
                        .setReferencedQualifiedId("pkg$db/ns#doc3")
                        .setResultRankInBlock(2)
                        .setResultRankGlobal(4)
                        .setTimeStayOnResultMillis(512)
                        .build();
        ClickAction clickAction4 =
                new ClickAction.Builder("namespace", "click4", /* actionTimestampMillis= */7000)
                        .setQuery("test")
                        .setReferencedQualifiedId("pkg$db/ns#doc4")
                        .setResultRankInBlock(4)
                        .setResultRankGlobal(8)
                        .setTimeStayOnResultMillis(256)
                        .build();
        ClickAction clickAction5 =
                new ClickAction.Builder("namespace", "click5", /* actionTimestampMillis= */8000)
                        .setQuery("test")
                        .setReferencedQualifiedId("pkg$db/ns#doc5")
                        .setResultRankInBlock(6)
                        .setResultRankGlobal(12)
                        .setTimeStayOnResultMillis(2048)
                        .build();

        // Use PutDocumentsRequest taken action API to convert document class to GenericDocument.
        PutDocumentsRequest putDocumentsRequest = new PutDocumentsRequest.Builder()
                .addTakenActions(searchAction1, clickAction1, clickAction2,
                        searchAction2, clickAction3, clickAction4, clickAction5)
                .build();

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE,
                                putDocumentsRequest.getTakenActionGenericDocuments());

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(2);

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("tes");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(20);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).hasSize(2);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankInBlock()).isEqualTo(1);
        assertThat(searchIntentStats0.getClicksStats().get(0).getResultRankGlobal()).isEqualTo(2);
        assertThat(searchIntentStats0.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(searchIntentStats0.getClicksStats().get(0).isGoodClick()).isFalse();
        assertThat(searchIntentStats0.getClicksStats().get(1).getTimestampMillis()).isEqualTo(3000);
        assertThat(searchIntentStats0.getClicksStats().get(1).getResultRankInBlock()).isEqualTo(3);
        assertThat(searchIntentStats0.getClicksStats().get(1).getResultRankGlobal()).isEqualTo(6);
        assertThat(searchIntentStats0.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(1024);
        assertThat(searchIntentStats0.getClicksStats().get(1).isGoodClick()).isFalse();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(5000);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("tes");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(10);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).hasSize(3);
        assertThat(searchIntentStats1.getClicksStats().get(0).getTimestampMillis()).isEqualTo(6000);
        assertThat(searchIntentStats1.getClicksStats().get(0).getResultRankInBlock()).isEqualTo(2);
        assertThat(searchIntentStats1.getClicksStats().get(0).getResultRankGlobal()).isEqualTo(4);
        assertThat(searchIntentStats1.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(searchIntentStats1.getClicksStats().get(0).isGoodClick()).isFalse();
        assertThat(searchIntentStats1.getClicksStats().get(1).getTimestampMillis()).isEqualTo(7000);
        assertThat(searchIntentStats1.getClicksStats().get(1).getResultRankInBlock()).isEqualTo(4);
        assertThat(searchIntentStats1.getClicksStats().get(1).getResultRankGlobal()).isEqualTo(8);
        assertThat(searchIntentStats1.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(256);
        assertThat(searchIntentStats1.getClicksStats().get(1).isGoodClick()).isFalse();
        assertThat(searchIntentStats1.getClicksStats().get(2).getTimestampMillis()).isEqualTo(8000);
        assertThat(searchIntentStats1.getClicksStats().get(2).getResultRankInBlock()).isEqualTo(6);
        assertThat(searchIntentStats1.getClicksStats().get(2).getResultRankGlobal()).isEqualTo(12);
        assertThat(searchIntentStats1.getClicksStats().get(2).getTimeStayOnResultMillis())
                .isEqualTo(2048);
        assertThat(searchIntentStats1.getClicksStats().get(2).isGoodClick()).isTrue();
    }
// @exportToFramework:endStrip()

    @Test
    public void testExtract_detectAndSkipSearchNoise_appendNewCharacters() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("te")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(3000)
                        .setQuery("tes")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction4 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search4", "builtin:SearchAction")
                        .setCreationTimestampMillis(3001)
                        .setQuery("test")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction5 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search5", "builtin:SearchAction")
                        .setCreationTimestampMillis(10000)
                        .setQuery("testing")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(
                        searchAction1, searchAction2, searchAction3, searchAction4, searchAction5);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(3);

        // searchAction2, searchAction3 should be considered as noise since they're intermediate
        // search actions with no clicks. The extractor should create search intents only for the
        // others.
        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(3001);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("t");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();

        // Search session 0, search intent 2
        SearchIntentStats searchIntentStats2 = searchSessionStats0.getSearchIntentsStats().get(2);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(10000);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("testing");
        assertThat(searchIntentStats2.getPrevQuery()).isEqualTo("test");
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_detectAndSkipSearchNoise_deleteCharacters() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("testing")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("test")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(3000)
                        .setQuery("tes")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction4 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search4", "builtin:SearchAction")
                        .setCreationTimestampMillis(3001)
                        .setQuery("te")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction5 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search5", "builtin:SearchAction")
                        .setCreationTimestampMillis(10000)
                        .setQuery("t")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(
                        searchAction1, searchAction2, searchAction3, searchAction4, searchAction5);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(3);

        // searchAction2, searchAction3 should be considered as noise since they're intermediate
        // search actions with no clicks. The extractor should create search intents only for the
        // others.
        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("testing");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(3001);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("testing");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();

        // Search session 0, search intent 2
        SearchIntentStats searchIntentStats2 = searchSessionStats0.getSearchIntentsStats().get(2);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(10000);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats2.getPrevQuery()).isEqualTo("te");
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_occursAfterThresholdShouldNotBeSearchNoise() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(3001)
                        .setQuery("te")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(10000)
                        .setQuery("test")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2, searchAction3);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(3);

        // searchAction2 should not be considered as noise since it occurs after the threshold from
        // searchAction1 (and therefore not intermediate search actions).
        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(3001);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("t");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();

        // Search session 0, search intent 2
        SearchIntentStats searchIntentStats2 = searchSessionStats0.getSearchIntentsStats().get(2);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(10000);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats2.getPrevQuery()).isEqualTo("te");
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_nonPrefixQueryStringShouldNotBeSearchNoise() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("apple")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(1500)
                        .setQuery("application")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("email")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction4 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search4", "builtin:SearchAction")
                        .setCreationTimestampMillis(10000)
                        .setQuery("google")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2, searchAction3, searchAction4);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(4);

        // searchAction2 and searchAction3 should not be considered as noise since neither query
        // string is a prefix of the previous one (and therefore not intermediate search actions).

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("apple");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(1500);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("application");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("apple");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();

        // Search session 0, search intent 2
        SearchIntentStats searchIntentStats2 = searchSessionStats0.getSearchIntentsStats().get(2);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("email");
        assertThat(searchIntentStats2.getPrevQuery()).isEqualTo("application");
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();

        // Search session 0, search intent 3
        SearchIntentStats searchIntentStats3 = searchSessionStats0.getSearchIntentsStats().get(3);
        assertThat(searchIntentStats3.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats3.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats3.getTimestampMillis()).isEqualTo(10000);
        assertThat(searchIntentStats3.getCurrQuery()).isEqualTo("google");
        assertThat(searchIntentStats3.getPrevQuery()).isEqualTo("email");
        assertThat(searchIntentStats3.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats3.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(searchIntentStats3.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_lastSearchActionShouldNotBeSearchNoise() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("te")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(2);

        // searchAction2 should not be considered as noise since it is the last search action (and
        // therefore not an intermediate search action).

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("t");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_lastSearchActionOfRelatedSearchSequenceShouldNotBeSearchNoise() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("te")
                        .setFetchedResultCount(0)
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(602001)
                        .setQuery("test")
                        .setFetchedResultCount(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2, searchAction3);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        // searchAction2 should not be considered as noise:
        // - searchAction3 is independent from searchAction2 and therefore forms an independent
        //   search session.
        // - So searchAction2 is the last search action of its search session (and therefore not an
        // intermediate search action).
        assertThat(result).hasSize(2);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(2);

        SearchSessionStats searchSessionStats1 = result.get(1);
        assertThat(searchSessionStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats1.getSearchIntentsStats()).hasSize(1);

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("t");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();

        // Search session 1, search intent 0
        SearchIntentStats searchIntentStats2 = searchSessionStats1.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(602001);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats2.getPrevQuery()).isNull();
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(0);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_withClickActionShouldNotBeSearchNoise() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(2000)
                        .setQuery("te")
                        .setFetchedResultCount(10)
                        .build();
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2050)
                        .setQuery("te")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(512)
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                        .build();
        GenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setCreationTimestampMillis(10000)
                        .setQuery("test")
                        .setFetchedResultCount(5)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2, clickAction1, searchAction3);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(3);

        // Even though searchAction2 is an intermediate search action, it should not be considered
        // as noise since there is at least 1 valid click action associated with it.

        // Search session 0, search intent 0
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(20);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 0, search intent 1
        SearchIntentStats searchIntentStats1 = searchSessionStats0.getSearchIntentsStats().get(1);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(2000);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isEqualTo("t");
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(10);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats1.getClicksStats()).hasSize(1);

        // Search session 0, search intent 2
        SearchIntentStats searchIntentStats2 = searchSessionStats0.getSearchIntentsStats().get(2);
        assertThat(searchIntentStats2.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats2.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats2.getTimestampMillis()).isEqualTo(10000);
        assertThat(searchIntentStats2.getCurrQuery()).isEqualTo("test");
        assertThat(searchIntentStats2.getPrevQuery()).isEqualTo("te");
        assertThat(searchIntentStats2.getNumResultsFetched()).isEqualTo(5);
        assertThat(searchIntentStats2.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(searchIntentStats2.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_independentSearchIntentShouldStartNewSearchSession() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setCreationTimestampMillis(601001)
                        .setQuery("te")
                        .setFetchedResultCount(10)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, searchAction2);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        // Since time difference between searchAction1 and searchAction2 exceeds the threshold,
        // searchAction2 should be considered as an independent search intent and therefore a new
        // search session stats is created.
        assertThat(result).hasSize(2);

        // Search session 0
        SearchSessionStats searchSessionStats0 = result.get(0);
        assertThat(searchSessionStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats0.getSearchIntentsStats()).hasSize(1);
        SearchIntentStats searchIntentStats0 = searchSessionStats0.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats0.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats0.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats0.getTimestampMillis()).isEqualTo(1000);
        assertThat(searchIntentStats0.getCurrQuery()).isEqualTo("t");
        assertThat(searchIntentStats0.getPrevQuery()).isNull();
        assertThat(searchIntentStats0.getNumResultsFetched()).isEqualTo(20);
        assertThat(searchIntentStats0.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats0.getClicksStats()).isEmpty();

        // Search session 1
        SearchSessionStats searchSessionStats1 = result.get(1);
        assertThat(searchSessionStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats1.getSearchIntentsStats()).hasSize(1);
        SearchIntentStats searchIntentStats1 = searchSessionStats1.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats1.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchIntentStats1.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchIntentStats1.getTimestampMillis()).isEqualTo(601001);
        assertThat(searchIntentStats1.getCurrQuery()).isEqualTo("te");
        assertThat(searchIntentStats1.getPrevQuery()).isNull();
        assertThat(searchIntentStats1.getNumResultsFetched()).isEqualTo(10);
        assertThat(searchIntentStats1.getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(searchIntentStats1.getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_shouldSetIsGoodClick() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setTimeStayOnResultMillis(2001)
                        .build();
        GenericDocument clickAction2 =
                new ClickActionGenericDocument.Builder("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(4500)
                        .setTimeStayOnResultMillis(1999)
                        .build();
        GenericDocument clickAction3 =
                new ClickActionGenericDocument.Builder("namespace", "click3", "builtin:ClickAction")
                        .setCreationTimestampMillis(7000)
                        .setTimeStayOnResultMillis(1)
                        .build();
        GenericDocument clickAction4 =
                new ClickActionGenericDocument.Builder("namespace", "click4", "builtin:ClickAction")
                        .setCreationTimestampMillis(7500)
                        .setTimeStayOnResultMillis(2000)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(
                        searchAction1, clickAction1, clickAction2, clickAction3, clickAction4);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats = result.get(0);
        assertThat(searchSessionStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats.getSearchIntentsStats()).hasSize(1);

        SearchIntentStats searchIntentStats = searchSessionStats.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats.getClicksStats()).hasSize(4);

        assertThat(searchIntentStats.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(2001);
        assertThat(searchIntentStats.getClicksStats().get(0).isGoodClick()).isTrue();

        assertThat(searchIntentStats.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(1999);
        assertThat(searchIntentStats.getClicksStats().get(1).isGoodClick()).isFalse();

        assertThat(searchIntentStats.getClicksStats().get(2).getTimeStayOnResultMillis())
                .isEqualTo(1);
        assertThat(searchIntentStats.getClicksStats().get(2).isGoodClick()).isFalse();

        assertThat(searchIntentStats.getClicksStats().get(3).getTimeStayOnResultMillis())
                .isEqualTo(2000);
        assertThat(searchIntentStats.getClicksStats().get(3).isGoodClick()).isTrue();
    }

    @Test
    public void testExtract_unsetTimeStayOnResultShouldBeGoodClick() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, clickAction1);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats = result.get(0);
        assertThat(searchSessionStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats.getSearchIntentsStats()).hasSize(1);

        SearchIntentStats searchIntentStats = searchSessionStats.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats.getClicksStats()).hasSize(1);

        assertThat(result).hasSize(1);
        assertThat(searchIntentStats.getClicksStats()).hasSize(1);

        assertThat(searchIntentStats.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(0);
        assertThat(searchIntentStats.getClicksStats().get(0).isGoodClick()).isTrue();
    }

    @Test
    public void testExtract_nonPositiveTimeStayOnResultShouldBeGoodClick() {
        GenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("t")
                        .setFetchedResultCount(20)
                        .build();
        GenericDocument clickAction1 =
                new ClickActionGenericDocument.Builder("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setTimeStayOnResultMillis(-1)
                        .build();
        GenericDocument clickAction2 =
                new ClickActionGenericDocument.Builder("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(3000)
                        .setTimeStayOnResultMillis(0)
                        .build();

        List<GenericDocument> takenActionGenericDocuments =
                Arrays.asList(searchAction1, clickAction1, clickAction2);

        List<SearchSessionStats> result =
                new SearchSessionStatsExtractor()
                        .extract(TEST_PACKAGE_NAME, TEST_DATABASE, takenActionGenericDocuments);

        assertThat(result).hasSize(1);

        SearchSessionStats searchSessionStats = result.get(0);
        assertThat(searchSessionStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(searchSessionStats.getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(searchSessionStats.getSearchIntentsStats()).hasSize(1);

        SearchIntentStats searchIntentStats = searchSessionStats.getSearchIntentsStats().get(0);
        assertThat(searchIntentStats.getClicksStats()).hasSize(2);

        assertThat(searchIntentStats.getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(-1);
        assertThat(searchIntentStats.getClicksStats().get(0).isGoodClick()).isTrue();

        assertThat(searchIntentStats.getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(0);
        assertThat(searchIntentStats.getClicksStats().get(1).isGoodClick()).isTrue();
    }

    @Test
    public void testGetQueryCorrectionType_unknown() {
        SearchActionGenericDocument searchAction =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setQuery("test")
                        .build();
        SearchActionGenericDocument searchActionWithNullQueryStr =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .build();

        // Query correction type should be unknown if the current search action's query string is
        // null.
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchActionWithNullQueryStr,
                        /* prevSearchAction= */ null))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchActionWithNullQueryStr,
                        /* prevSearchAction= */ searchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);

        // Query correction type should be unknown if the previous search action contains null query
        // string.
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction,
                        /* prevSearchAction= */ searchActionWithNullQueryStr))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchActionWithNullQueryStr,
                        /* prevSearchAction= */ searchActionWithNullQueryStr))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
    }

    @Test
    public void testGetQueryCorrectionType_firstQuery() {
        SearchActionGenericDocument currSearchAction =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setQuery("test")
                        .build();

        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        currSearchAction, /* prevSearchAction= */ null))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
    }

    @Test
    public void testGetQueryCorrectionType_refinement() {
        SearchActionGenericDocument prevSearchAction =
                new SearchActionGenericDocument.Builder(
                        "namespace", "baseSearch", "builtin:SearchAction")
                        .setQuery("test")
                        .build();

        // Append 1 new character should be query refinement.
        SearchActionGenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setQuery("teste")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction1, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Append 2 new characters should be query refinement.
        SearchActionGenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setQuery("tester")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction2, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Backspace 1 character should be query refinement.
        SearchActionGenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setQuery("tes")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction3, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Backspace 1 character and append new character(s) should be query refinement.
        SearchActionGenericDocument searchAction4 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search4", "builtin:SearchAction")
                        .setQuery("tesla")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction4, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
    }

    @Test
    public void testGetQueryCorrectionType_abandonment() {
        SearchActionGenericDocument prevSearchAction =
                new SearchActionGenericDocument.Builder(
                        "namespace", "baseSearch", "builtin:SearchAction")
                        .setQuery("test")
                        .build();

        // Completely different query should be query abandonment.
        SearchActionGenericDocument searchAction1 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search1", "builtin:SearchAction")
                        .setQuery("unit")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction1, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);

        // Backspace 2 characters should be query abandonment.
        SearchActionGenericDocument searchAction2 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .setQuery("te")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction2, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);

        // Backspace 2 characters and append new character(s) should be query abandonment.
        SearchActionGenericDocument searchAction3 =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search3", "builtin:SearchAction")
                        .setQuery("texas")
                        .build();
        assertThat(
                SearchSessionStatsExtractor.getQueryCorrectionType(
                        /* currSearchAction= */ searchAction3, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
    }
}
