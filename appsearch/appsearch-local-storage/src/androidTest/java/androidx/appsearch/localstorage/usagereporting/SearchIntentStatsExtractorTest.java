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
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.SearchAction;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SearchIntentStatsExtractorTest {
    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final String TEST_DATABASE = "database";

    @Test
    public void testExtract() {
        // Create search action and click action generic documents.
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("tes")
                .setFetchedResultCount(20)
                .build();
        GenericDocument clickAction1 = new ClickActionGenericDocument.Builder(
                "namespace", "click1", "builtin:ClickAction")
                .setCreationTimestampMillis(2000)
                .setQuery("tes")
                .setResultRankInBlock(1)
                .setResultRankGlobal(2)
                .setTimeStayOnResultMillis(512)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                .build();
        GenericDocument clickAction2 = new ClickActionGenericDocument.Builder(
                "namespace", "click2", "builtin:ClickAction")
                .setCreationTimestampMillis(3000)
                .setQuery("tes")
                .setResultRankInBlock(3)
                .setResultRankGlobal(6)
                .setTimeStayOnResultMillis(1024)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc2")
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(5000)
                .setQuery("test")
                .setFetchedResultCount(10)
                .build();
        GenericDocument clickAction3 = new ClickActionGenericDocument.Builder(
                "namespace", "click3", "builtin:ClickAction")
                .setCreationTimestampMillis(6000)
                .setQuery("test")
                .setResultRankInBlock(2)
                .setResultRankGlobal(4)
                .setTimeStayOnResultMillis(512)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc3")
                .build();
        GenericDocument clickAction4 = new ClickActionGenericDocument.Builder(
                "namespace", "click4", "builtin:ClickAction")
                .setCreationTimestampMillis(7000)
                .setQuery("test")
                .setResultRankInBlock(4)
                .setResultRankGlobal(8)
                .setTimeStayOnResultMillis(256)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc4")
                .build();
        GenericDocument clickAction5 = new ClickActionGenericDocument.Builder(
                "namespace", "click5", "builtin:ClickAction")
                .setCreationTimestampMillis(8000)
                .setQuery("test")
                .setResultRankInBlock(6)
                .setResultRankGlobal(12)
                .setTimeStayOnResultMillis(1024)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc5")
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, clickAction1, clickAction2,
                searchAction2, clickAction3, clickAction4, clickAction5);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        assertThat(result).hasSize(2);
        // Search intent 0
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("tes");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(20);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).hasSize(2);
        assertThat(result.get(0).getClicksStats().get(0).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankInBlock()).isEqualTo(1);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankGlobal()).isEqualTo(2);
        assertThat(result.get(0).getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(result.get(0).getClicksStats().get(1).getTimestampMillis()).isEqualTo(3000);
        assertThat(result.get(0).getClicksStats().get(1).getResultRankInBlock()).isEqualTo(3);
        assertThat(result.get(0).getClicksStats().get(1).getResultRankGlobal()).isEqualTo(6);
        assertThat(result.get(0).getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(1024);

        // Search intent 1
        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(5000);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("tes");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(10);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).hasSize(3);
        assertThat(result.get(1).getClicksStats().get(0).getTimestampMillis()).isEqualTo(6000);
        assertThat(result.get(1).getClicksStats().get(0).getResultRankInBlock()).isEqualTo(2);
        assertThat(result.get(1).getClicksStats().get(0).getResultRankGlobal()).isEqualTo(4);
        assertThat(result.get(1).getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(result.get(1).getClicksStats().get(1).getTimestampMillis()).isEqualTo(7000);
        assertThat(result.get(1).getClicksStats().get(1).getResultRankInBlock()).isEqualTo(4);
        assertThat(result.get(1).getClicksStats().get(1).getResultRankGlobal()).isEqualTo(8);
        assertThat(result.get(1).getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(256);
        assertThat(result.get(1).getClicksStats().get(2).getTimestampMillis()).isEqualTo(8000);
        assertThat(result.get(1).getClicksStats().get(2).getResultRankInBlock()).isEqualTo(6);
        assertThat(result.get(1).getClicksStats().get(2).getResultRankGlobal()).isEqualTo(12);
        assertThat(result.get(1).getClicksStats().get(2).getTimeStayOnResultMillis())
                .isEqualTo(1024);
    }

    @Test
    public void testExtract_shouldSkipUnknownActionTypeDocuments() {
        // Create search action and click action generic documents.
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("tes")
                .setFetchedResultCount(20)
                .build();
        GenericDocument clickAction1 = new GenericDocument.Builder<>(
                "namespace", "click1", "builtin:ClickAction")
                .setCreationTimestampMillis(2000)
                .setPropertyString("query", "tes")
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                .setPropertyLong("resultRankInBlock", 1)
                .setPropertyLong("resultRankGlobal", 2)
                .setPropertyLong("timeStayOnResultMillis", 512)
                .build();
        GenericDocument clickAction2 = new ClickActionGenericDocument.Builder(
                "namespace", "click2", "builtin:ClickAction")
                .setCreationTimestampMillis(3000)
                .setQuery("tes")
                .setResultRankInBlock(3)
                .setResultRankGlobal(6)
                .setTimeStayOnResultMillis(1024)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc2")
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, clickAction1, clickAction2);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // Since clickAction1 doesn't have property "actionType", it should be skipped without
        // throwing any exception.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("tes");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(20);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).hasSize(1);
        assertThat(result.get(0).getClicksStats().get(0).getTimestampMillis()).isEqualTo(3000);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankInBlock()).isEqualTo(3);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankGlobal()).isEqualTo(6);
        assertThat(result.get(0).getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(1024);
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
                        .setTimeStayOnResultMillis(1024)
                        .build();

        // Use PutDocumentsRequest taken action API to convert document class to GenericDocument.
        PutDocumentsRequest putDocumentsRequest = new PutDocumentsRequest.Builder()
                .addTakenActions(searchAction1, clickAction1, clickAction2,
                        searchAction2, clickAction3, clickAction4, clickAction5)
                .build();

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(
                        putDocumentsRequest.getTakenActionGenericDocuments());

        assertThat(result).hasSize(2);
        // Search intent 0
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("tes");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(20);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).hasSize(2);
        assertThat(result.get(0).getClicksStats().get(0).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankInBlock()).isEqualTo(1);
        assertThat(result.get(0).getClicksStats().get(0).getResultRankGlobal()).isEqualTo(2);
        assertThat(result.get(0).getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(result.get(0).getClicksStats().get(1).getTimestampMillis()).isEqualTo(3000);
        assertThat(result.get(0).getClicksStats().get(1).getResultRankInBlock()).isEqualTo(3);
        assertThat(result.get(0).getClicksStats().get(1).getResultRankGlobal()).isEqualTo(6);
        assertThat(result.get(0).getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(1024);

        // Search intent 1
        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(5000);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("tes");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(10);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).hasSize(3);
        assertThat(result.get(1).getClicksStats().get(0).getTimestampMillis()).isEqualTo(6000);
        assertThat(result.get(1).getClicksStats().get(0).getResultRankInBlock()).isEqualTo(2);
        assertThat(result.get(1).getClicksStats().get(0).getResultRankGlobal()).isEqualTo(4);
        assertThat(result.get(1).getClicksStats().get(0).getTimeStayOnResultMillis())
                .isEqualTo(512);
        assertThat(result.get(1).getClicksStats().get(1).getTimestampMillis()).isEqualTo(7000);
        assertThat(result.get(1).getClicksStats().get(1).getResultRankInBlock()).isEqualTo(4);
        assertThat(result.get(1).getClicksStats().get(1).getResultRankGlobal()).isEqualTo(8);
        assertThat(result.get(1).getClicksStats().get(1).getTimeStayOnResultMillis())
                .isEqualTo(256);
        assertThat(result.get(1).getClicksStats().get(2).getTimestampMillis()).isEqualTo(8000);
        assertThat(result.get(1).getClicksStats().get(2).getResultRankInBlock()).isEqualTo(6);
        assertThat(result.get(1).getClicksStats().get(2).getResultRankGlobal()).isEqualTo(12);
        assertThat(result.get(1).getClicksStats().get(2).getTimeStayOnResultMillis())
                .isEqualTo(1024);
    }
// @exportToFramework:endStrip()

    @Test
    public void testExtract_detectAndSkipSearchNoise_appendNewCharacters() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("te")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(3000)
                .setQuery("tes")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction4 = new SearchActionGenericDocument.Builder(
                "namespace", "search4", "builtin:SearchAction")
                .setCreationTimestampMillis(3001)
                .setQuery("test")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction5 = new SearchActionGenericDocument.Builder(
                "namespace", "search5", "builtin:SearchAction")
                .setCreationTimestampMillis(10000)
                .setQuery("testing")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, searchAction3, searchAction4, searchAction5);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2, searchAction3 should be considered as noise since they're intermediate
        // search actions with no clicks. The extractor should create search intents only for the
        // others.
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(3001);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("t");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(10000);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("testing");
        assertThat(result.get(2).getPrevQuery()).isEqualTo("test");
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(2).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_detectAndSkipSearchNoise_deleteCharacters() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("testing")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("test")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(3000)
                .setQuery("tes")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction4 = new SearchActionGenericDocument.Builder(
                "namespace", "search4", "builtin:SearchAction")
                .setCreationTimestampMillis(3001)
                .setQuery("te")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction5 = new SearchActionGenericDocument.Builder(
                "namespace", "search5", "builtin:SearchAction")
                .setCreationTimestampMillis(10000)
                .setQuery("t")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, searchAction3, searchAction4, searchAction5);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2, searchAction3 should be considered as noise since they're intermediate
        // search actions with no clicks. The extractor should create search intents only for the
        // others.
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("testing");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(3001);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("testing");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(10000);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(2).getPrevQuery()).isEqualTo("te");
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(2).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_occursAfterThresholdShouldNotBeSearchNoise() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(3001)
                .setQuery("te")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(10000)
                .setQuery("test")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, searchAction3);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2 should not be considered as noise since it occurs after the threshold from
        // searchAction1 (and therefore not intermediate search actions).
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(3001);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("t");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(10000);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(2).getPrevQuery()).isEqualTo("te");
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(2).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_nonPrefixQueryStringShouldNotBeSearchNoise() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("apple")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(1500)
                .setQuery("application")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("email")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction4 = new SearchActionGenericDocument.Builder(
                "namespace", "search4", "builtin:SearchAction")
                .setCreationTimestampMillis(10000)
                .setQuery("google")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, searchAction3, searchAction4);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2 and searchAction3 should not be considered as noise since neither query
        // string is a prefix of the previous one (and therefore not intermediate search actions).
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("apple");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(1500);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("application");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("apple");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("email");
        assertThat(result.get(2).getPrevQuery()).isEqualTo("application");
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(result.get(2).getClicksStats()).isEmpty();

        assertThat(result.get(3).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(3).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(3).getTimestampMillis()).isEqualTo(10000);
        assertThat(result.get(3).getCurrQuery()).isEqualTo("google");
        assertThat(result.get(3).getPrevQuery()).isEqualTo("email");
        assertThat(result.get(3).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(3).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
        assertThat(result.get(3).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_lastSearchActionShouldNotBeSearchNoise() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("te")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2 should not be considered as noise since it is the last search action (and
        // therefore not an intermediate search action).
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("t");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_lastSearchActionOfRelatedSearchSequenceShouldNotBeSearchNoise() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("te")
                .setFetchedResultCount(0)
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(602001)
                .setQuery("test")
                .setFetchedResultCount(0)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, searchAction3);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // searchAction2 should not be considered as noise:
        // - searchAction3 is independent from searchAction2.
        // - So searchAction2 is the last search action of the related search sequence (and
        //   therefore not an intermediate search action).
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("t");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).isEmpty();

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(602001);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(2).getPrevQuery()).isNull();
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(0);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(2).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_withClickActionShouldNotBeSearchNoise() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(20)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(2000)
                .setQuery("te")
                .setFetchedResultCount(10)
                .build();
        GenericDocument clickAction1 = new ClickActionGenericDocument.Builder(
                "namespace", "click1", "builtin:ClickAction")
                .setCreationTimestampMillis(2050)
                .setQuery("te")
                .setResultRankInBlock(1)
                .setResultRankGlobal(2)
                .setTimeStayOnResultMillis(512)
                .setPropertyString("referencedQualifiedId", "pkg$db/ns#doc1")
                .build();
        GenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setCreationTimestampMillis(10000)
                .setQuery("test")
                .setFetchedResultCount(5)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2, clickAction1, searchAction3);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // Even though searchAction2 is an intermediate search action, it should not be considered
        // as noise since there is at least 1 valid click action associated with it.
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(20);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(2000);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isEqualTo("t");
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(10);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(1).getClicksStats()).hasSize(1);

        assertThat(result.get(2).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(2).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(2).getTimestampMillis()).isEqualTo(10000);
        assertThat(result.get(2).getCurrQuery()).isEqualTo("test");
        assertThat(result.get(2).getPrevQuery()).isEqualTo("te");
        assertThat(result.get(2).getNumResultsFetched()).isEqualTo(5);
        assertThat(result.get(2).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
        assertThat(result.get(2).getClicksStats()).isEmpty();
    }

    @Test
    public void testExtract_detectIndependentSearchIntent() {
        GenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setCreationTimestampMillis(1000)
                .setQuery("t")
                .setFetchedResultCount(20)
                .build();
        GenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setCreationTimestampMillis(601001)
                .setQuery("te")
                .setFetchedResultCount(10)
                .build();

        List<GenericDocument> takenActionGenericDocuments = Arrays.asList(
                searchAction1, searchAction2);

        List<SearchIntentStats> result = new SearchIntentStatsExtractor(
                TEST_PACKAGE_NAME, TEST_DATABASE).extract(takenActionGenericDocuments);

        // Since time difference between searchAction1 and searchAction2 exceeds the threshold,
        // searchAction2 should be considered as an independent search intent.
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(0).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(0).getTimestampMillis()).isEqualTo(1000);
        assertThat(result.get(0).getCurrQuery()).isEqualTo("t");
        assertThat(result.get(0).getPrevQuery()).isNull();
        assertThat(result.get(0).getNumResultsFetched()).isEqualTo(20);
        assertThat(result.get(0).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(0).getClicksStats()).isEmpty();

        assertThat(result.get(1).getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(result.get(1).getDatabase()).isEqualTo(TEST_DATABASE);
        assertThat(result.get(1).getTimestampMillis()).isEqualTo(601001);
        assertThat(result.get(1).getCurrQuery()).isEqualTo("te");
        assertThat(result.get(1).getPrevQuery()).isNull();
        assertThat(result.get(1).getNumResultsFetched()).isEqualTo(10);
        assertThat(result.get(1).getQueryCorrectionType())
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
        assertThat(result.get(1).getClicksStats()).isEmpty();
    }

    @Test
    public void testGetQueryCorrectionType_unknown() {
        SearchActionGenericDocument searchAction = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setQuery("test")
                .build();
        SearchActionGenericDocument searchActionWithNullQueryStr =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search2", "builtin:SearchAction")
                        .build();

        // Query correction type should be unknown if the current search action's query string is
        // null.
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchActionWithNullQueryStr,
                    /* prevSearchAction= */null))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchActionWithNullQueryStr,
                    /* prevSearchAction= */searchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);

        // Query correction type should be unknown if the previous search action contains null query
        // string.
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction,
                    /* prevSearchAction= */searchActionWithNullQueryStr))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchActionWithNullQueryStr,
                    /* prevSearchAction= */searchActionWithNullQueryStr))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN);
    }

    @Test
    public void testGetQueryCorrectionType_firstQuery() {
        SearchActionGenericDocument currSearchAction = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setQuery("test")
                .build();

        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    currSearchAction, /* prevSearchAction= */null))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY);
    }

    @Test
    public void testGetQueryCorrectionType_refinement() {
        SearchActionGenericDocument prevSearchAction = new SearchActionGenericDocument.Builder(
                "namespace", "baseSearch", "builtin:SearchAction")
                .setQuery("test")
                .build();

        // Append 1 new character should be query refinement.
        SearchActionGenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setQuery("teste")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction1, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Append 2 new characters should be query refinement.
        SearchActionGenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setQuery("tester")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction2, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Backspace 1 character should be query refinement.
        SearchActionGenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setQuery("tes")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction3, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);

        // Backspace 1 character and append new character(s) should be query refinement.
        SearchActionGenericDocument searchAction4 = new SearchActionGenericDocument.Builder(
                "namespace", "search4", "builtin:SearchAction")
                .setQuery("tesla")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction4, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT);
    }

    @Test
    public void testGetQueryCorrectionType_abandonment() {
        SearchActionGenericDocument prevSearchAction = new SearchActionGenericDocument.Builder(
                "namespace", "baseSearch", "builtin:SearchAction")
                .setQuery("test")
                .build();

        // Completely different query should be query abandonment.
        SearchActionGenericDocument searchAction1 = new SearchActionGenericDocument.Builder(
                "namespace", "search1", "builtin:SearchAction")
                .setQuery("unit")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction1, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);

        // Backspace 2 characters should be query abandonment.
        SearchActionGenericDocument searchAction2 = new SearchActionGenericDocument.Builder(
                "namespace", "search2", "builtin:SearchAction")
                .setQuery("te")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction2, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);

        // Backspace 2 characters and append new character(s) should be query abandonment.
        SearchActionGenericDocument searchAction3 = new SearchActionGenericDocument.Builder(
                "namespace", "search3", "builtin:SearchAction")
                .setQuery("texas")
                .build();
        assertThat(SearchIntentStatsExtractor.getQueryCorrectionType(
                    /* currSearchAction= */searchAction3, prevSearchAction))
                .isEqualTo(SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT);
    }
}
