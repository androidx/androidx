/*
 * Copyright 2025 The Android Open Source Project
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
// @exportToFramework:skipFile()

package androidx.appsearch.cts.app;

import static androidx.appsearch.app.SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchEnvironmentFactory;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.usagereporting.ActionAccumulator;
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.DismissAction;
import androidx.appsearch.usagereporting.SearchAction;
import androidx.appsearch.usagereporting.TakenAction;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AccumulatorCtsTestBase {

    private AppSearchSession mSession;
    private Context mContext;
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    private static final String DB_NAME = "forTest";

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();

        mSession = createSearchSessionAsync(DB_NAME).get();
        clean();

        if (mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID)) {
            mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                    .addDocumentClasses(SearchAction.class, ClickAction.class)
                    .setForceOverride(true).build()).get();
        }
    }

    @After
    public void tearDown() throws Exception {
        clean();
    }

    private void clean() throws Exception {
        // Clear the cache file dir
        File cacheDir = AppSearchEnvironmentFactory.getEnvironmentInstance().getCacheDir(mContext);
        File[] cacheFileNames = cacheDir.listFiles();
        for (int i = 0; i < cacheFileNames.length; i++) {
            File cacheFile = cacheFileNames[i];
            if (cacheFile.isFile()) {
                cacheFile.delete();
            }
        }

        // Clear AppSearch
        mSession.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testReportSearch_updatesFetchedResultCount_forSameQuery() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long timestampA = System.currentTimeMillis() - 2000;
        long timestampB = timestampA + 1000;

        SearchAction search1 = new SearchAction.Builder("ns", "searchid1", timestampA)
                .setQuery("query1")
                .setFetchedResultCount(10)
                .build();
        SearchAction search2 = new SearchAction.Builder("ns", "searchid2", timestampB)
                .setQuery("query1")
                .setFetchedResultCount(20)
                .build();

        accumulator.reportActionAsync(search1).get();
        accumulator.reportActionAsync(search2).get();
        accumulator.saveDocumentsToAppSearchAsync().get();

        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // Because the two SearchActions have the same query, have no other SearchAction in between,
        // and were reported with no saveDocumentsToAppSearch call in between, we treat them as the
        // same search journey. This represents the scenario where we load additional documents for
        // the same query. So there should only be one document for the two queries.
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getPropertyLong("fetchedResultCount")).isEqualTo(20);
        // The original timestamp should be kept
        assertThat(documents.get(0).getCreationTimestampMillis()).isEqualTo(timestampA);
    }

    @Test
    public void testReportSearch_differentQueries() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long timestampA = System.currentTimeMillis() - 2000;
        long timestampB = timestampA + 1000;

        SearchAction search1 = new SearchAction.Builder("ns", "searchid1", timestampA)
                .setQuery("query1")
                .setFetchedResultCount(10)
                .build();
        SearchAction search2 = new SearchAction.Builder("ns", "searchid2", timestampB)
                .setQuery("query2")
                .setFetchedResultCount(20)
                .build();
        SearchAction search3 = new SearchAction.Builder("ns", "searchid3", timestampB)
                .setQuery("query1")
                .setFetchedResultCount(20)
                .build();

        accumulator.reportActionAsync(search1).get();
        accumulator.reportActionAsync(search2).get();
        accumulator.reportActionAsync(search3).get();
        accumulator.saveDocumentsToAppSearchAsync().get();

        // Rank by timestamp to ensure consistent ordering
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder()
                .setRankingStrategy(RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING).build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);

        // Assertions for the first document (search1)
        SearchAction doc1 = documents.get(0).toDocumentClass(SearchAction.class);
        assertThat(doc1.getActionTimestampMillis()).isEqualTo(timestampA);
        assertThat(doc1.getQuery()).isEqualTo("query1");
        assertThat(doc1.getFetchedResultCount()).isEqualTo(10L);

        // Assertions for the second document (search2)
        SearchAction doc2 = documents.get(1).toDocumentClass(SearchAction.class);
        assertThat(doc2.getActionTimestampMillis()).isEqualTo(timestampB);
        assertThat(doc2.getQuery()).isEqualTo("query2");
        assertThat(doc2.getFetchedResultCount()).isEqualTo(20L);

        // Assertions for the third document (search3). Even though this has the same query as the
        // first search action, it shouldn't be treated as the same search action with a higher
        // fetched result count. This is because we just reported a different query in the second
        // action.
        SearchAction doc3 = documents.get(2).toDocumentClass(SearchAction.class);
        assertThat(doc3.getActionTimestampMillis()).isEqualTo(timestampB);
        assertThat(doc3.getQuery()).isEqualTo("query1");
        assertThat(doc3.getFetchedResultCount()).isEqualTo(20L);
    }

    @Test
    public void testReportSearch_exceedsCacheLimit() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long timestampA = System.currentTimeMillis() - 2000;

        // Report 100 searches before the timer times out
        for (int i = 0; i < 100; i++) {
            SearchAction search = new SearchAction.Builder("ns", "searchid" + i, timestampA)
                    .setQuery("query" + i)
                    .setFetchedResultCount(10)
                    .build();
            accumulator.reportActionAsync(search).get();
        }

        // All SearchAction should be indexed immediately, without needing to call save to AppSearch
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder()
                // Set to slightly above expected count
                .setResultCountPerPage(101).build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(100);

        // Report 150 additional documents.
        for (int i = 100; i < 250; i++) {
            SearchAction search = new SearchAction.Builder("ns", "searchid" + i, timestampA)
                    .setQuery("query" + i)
                    .setFetchedResultCount(10)
                    .build();
            accumulator.reportActionAsync(search).get();
        }

        // The first 100 will be batched immediately, but not the remaining 50
        searchResults = mSession.search("", new SearchSpec.Builder()
                // Set to slightly above expected count
                .setResultCountPerPage(201).build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(200);
    }

    @Test
    public void testReportSearch_loadsPreviousCache() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long timestampA = System.currentTimeMillis() - 2000;

        // Report a search before the timer times out
        SearchAction search = new SearchAction.Builder("ns", "searchid", timestampA)
                .setQuery("query")
                .setFetchedResultCount(10)
                .build();
        accumulator.reportActionAsync(search).get();

        // By cancelling the timer, we simulate the client app shutting down
        accumulator.cancelTimer();

        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Then recreate the accumulator.
        ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        // The cached result should be immediately available
        searchResults = mSession.search("", new SearchSpec.Builder().build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);

        SearchAction doc1 = documents.get(0).toDocumentClass(SearchAction.class);
        assertThat(doc1.getId()).isEqualTo("searchid");
    }

    @Test
    public void testAccumulator_multipleAccumulators() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        // Both accumulators should write to the same database
        ActionAccumulator accumulator1 =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();
        ActionAccumulator accumulator2 =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long timestampA = System.currentTimeMillis() - 2000;
        long timestampB = timestampA + 1000;

        SearchAction search1 = new SearchAction.Builder("ns", "searchid1", timestampA)
                .setQuery("query1")
                .setFetchedResultCount(10)
                .build();
        SearchAction search2 = new SearchAction.Builder("ns", "searchid2", timestampB)
                .setQuery("query2")
                .setFetchedResultCount(10)
                .build();

        accumulator1.reportActionAsync(search1).get();
        accumulator2.reportActionAsync(search2).get();

        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        accumulator1.saveDocumentsToAppSearchAsync().get();

        searchResults = mSession.search("", new SearchSpec.Builder().build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        SearchAction doc1 = documents.get(0).toDocumentClass(SearchAction.class);
        assertThat(doc1.getId()).isEqualTo("searchid1");

        accumulator2.saveDocumentsToAppSearchAsync().get();

        searchResults = mSession.search("", new SearchSpec.Builder()
                .setRankingStrategy(RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING).build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        doc1 = documents.get(0).toDocumentClass(SearchAction.class);
        assertThat(doc1.getId()).isEqualTo("searchid1");
        SearchAction doc2 = documents.get(1).toDocumentClass(SearchAction.class);
        assertThat(doc2.getId()).isEqualTo("searchid2");
    }

    @Test
    public void testAccumulator_cacheJunkData() throws Exception {
        // Instead of valid action document parcels, there is random data in the cache.
        // Initialization should handle this gracefully.
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // Add a cache file with junk data.
        ActionAccumulator firstAccumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();
        File cacheFile = firstAccumulator.getCacheFile();
        // Shutdown firstAccumulator
        firstAccumulator.cancelTimer();

        try (FileOutputStream fos = new FileOutputStream(cacheFile, /*append=*/false)) {
            fos.write("This is not a valid document parcel! This is random data.".getBytes(
                    StandardCharsets.UTF_8));
        }

        // Create the ActionAccumulator. This should not throw an exception even with the junk data.
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        // Report a valid search to ensure the accumulator still works after handling the junk data.
        long timestampA = System.currentTimeMillis() - 2000;
        SearchAction search = new SearchAction.Builder("ns", "searchid", timestampA)
                .setQuery("query")
                .setFetchedResultCount(10)
                .build();
        accumulator.reportActionAsync(search).get();
        accumulator.saveDocumentsToAppSearchAsync().get();

        // Verify that the valid search was saved.
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        SearchAction doc1 = documents.get(0).toDocumentClass(SearchAction.class);
        assertThat(doc1.getId()).isEqualTo("searchid");
    }

    @Test
    public void testReportClick_updatesPreviousClickWithTimeStayOnResult() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long ts1 = System.currentTimeMillis() - 2000;
        ClickAction click1 = new ClickAction.Builder("ns", "clickid1", ts1)
                .setQuery("query1")
                .setResultRankGlobal(1)
                .build();

        accumulator.reportActionAsync(click1).get();

        long ts2 = ts1 + 1234;
        ClickAction click2 = new ClickAction.Builder("ns", "clickid2", ts2)
                .setQuery("query1")
                .setResultRankGlobal(2)
                .build();

        accumulator.reportActionAsync(click2).get();
        accumulator.saveDocumentsToAppSearchAsync().get();

        AppSearchBatchResult<String, GenericDocument> getResult =
                mSession.getByDocumentIdAsync(new GetByDocumentIdRequest.Builder("ns")
                        .addIds("clickid1").build()).get();

        // Find the first click and verify timeStayOnResultMillis is set
        GenericDocument firstClickDoc = getResult.getSuccesses().get("clickid1");
        assertThat(firstClickDoc).isNotNull();
        assertThat(firstClickDoc.getPropertyLong("timeStayOnResultMillis")).isEqualTo(ts2 - ts1);
    }

    @Test
    public void testReportSearch_updatesPreviousClickWithTimeStayOnResult() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long ts1 = System.currentTimeMillis() - 2000;
        ClickAction click1 = new ClickAction.Builder("ns", "clickid1", ts1)
                .setQuery("query1")
                .setResultRankGlobal(1)
                .build();

        accumulator.reportActionAsync(click1).get();

        long ts2 = ts1 + 1234;
        SearchAction search1 = new SearchAction.Builder("ns", "searchid1", ts2)
                .setQuery("query1")
                .setFetchedResultCount(10)
                .build();

        accumulator.reportActionAsync(search1).get();
        accumulator.saveDocumentsToAppSearchAsync().get();

        AppSearchBatchResult<String, GenericDocument> getResult =
                mSession.getByDocumentIdAsync(new GetByDocumentIdRequest.Builder("ns")
                        .addIds("clickid1").build()).get();

        // Find the first click and verify timeStayOnResultMillis is set
        GenericDocument firstClickDoc = getResult.getSuccesses().get("clickid1");

        assertThat(firstClickDoc).isNotNull();
        assertThat(firstClickDoc.getPropertyLong("timeStayOnResultMillis")).isEqualTo(ts2 - ts1);
    }

    @Test
    public void testReportAction_invalidType() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        TakenAction someAction = new DismissAction.Builder("ns", "id",
                System.currentTimeMillis()).build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> accumulator.reportActionAsync(someAction).get());
        assertThat(exception.getCause().getMessage())
                .isEqualTo("Reported actions must be ClickActions or SearchActions");
    }

    @Test
    public void testReportActions_interleavedThreads() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        ActionAccumulator accumulator =
                ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        long ts1 = System.currentTimeMillis() - 2000;
        ClickAction click1 = new ClickAction.Builder("ns", "clickid1", ts1)
                .setQuery("query1")
                .setResultRankGlobal(1)
                .build();

        long ts2 = ts1 + 1234;
        ClickAction click2 = new ClickAction.Builder("ns", "clickid2", ts2)
                .setQuery("query1")
                .setResultRankGlobal(2)
                .build();

        accumulator.reportActionAsync(click1).get();
        ListenableFuture<AppSearchBatchResult<String, Void>> future1 =
                accumulator.reportActionAsync(click1);
        ListenableFuture<AppSearchBatchResult<String, Void>> future2 =
                accumulator.reportActionAsync(click2);

        future1.get();
        future2.get();

        // By cancelling the timer, we simulate the client app shutting down
        accumulator.cancelTimer();

        SearchResults searchResults = mSession.search("query1", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Then recreate the accumulator.
        ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get();

        // Search for click1. Make sure it exists and has the correct timeStayOnResult.
        AppSearchBatchResult<String, GenericDocument> getResult =
                mSession.getByDocumentIdAsync(new GetByDocumentIdRequest.Builder("ns")
                        .addIds("clickid1").build()).get();

        // Find the first click and verify timeStayOnResultMillis is set
        GenericDocument firstClickDoc = getResult.getSuccesses().get("clickid1");

        assertThat(firstClickDoc).isNotNull();
        assertThat(firstClickDoc.getPropertyLong("timeStayOnResultMillis")).isEqualTo(1234);
    }

    @Test
    public void testInitialize_noSchemas() throws Exception {
        mSession.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get());
        assertThat(exception.getCause().getMessage()).isEqualTo(
                "ActionAccumulator must be used with an AppSearch database where "
                        + "builtin:SearchAction and builtin:ClickAction are set.");
    }

    @Test
    public void testInitialize_noSearchSchema() throws Exception {
        assumeTrue(mSession.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(ClickAction.class).setForceOverride(true).build()).get();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get());
        assertThat(exception.getCause().getMessage()).isEqualTo(
                "ActionAccumulator must be used with an AppSearch database where "
                        + "builtin:SearchAction and builtin:ClickAction are set.");
    }

    @Test
    public void testInitialize_noClickSchema() throws Exception {
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(SearchAction.class).setForceOverride(true).build()).get();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> ActionAccumulator.createAsync(mContext, mSession, EXECUTOR).get());
        assertThat(exception.getCause().getMessage()).isEqualTo(
                "ActionAccumulator must be used with an AppSearch database where "
                        + "builtin:SearchAction and builtin:ClickAction are set.");
    }
}
