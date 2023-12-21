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

package androidx.appsearch.cts.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.testutil.AppSearchTestUtils.retrieveAllSearchResults;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportSystemUsageRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class GlobalSearchSessionCtsTestBase {
    static final String DB_NAME_1 = "";
    static final String DB_NAME_2 = "testDb2";

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    protected AppSearchSession mDb1;
    protected AppSearchSession mDb2;

    protected GlobalSearchSession mGlobalSearchSession;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

    protected abstract ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync()
            throws Exception;

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        mDb2 = createSearchSessionAsync(DB_NAME_2).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();

        mGlobalSearchSession = createGlobalSearchSessionAsync().get();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    private List<GenericDocument> snapshotResults(String queryExpression, SearchSpec spec)
            throws Exception {
        SearchResults searchResults = mGlobalSearchSession.search(queryExpression, spec);
        return convertSearchResultsToDocuments(searchResults);
    }

    /**
     * Asserts that the union of {@code addedDocuments} and {@code beforeDocuments} is exactly
     * equivalent to {@code afterDocuments}. Order doesn't matter.
     *
     * @param beforeDocuments Documents that existed first.
     * @param afterDocuments  The total collection of documents that should exist now.
     * @param addedDocuments  The collection of documents that were expected to be added.
     */
    private void assertAddedBetweenSnapshots(List<? extends GenericDocument> beforeDocuments,
            List<? extends GenericDocument> afterDocuments,
            List<? extends GenericDocument> addedDocuments) {
        List<GenericDocument> expectedDocuments = new ArrayList<>(beforeDocuments);
        expectedDocuments.addAll(addedDocuments);
        assertThat(afterDocuments).containsExactlyElementsIn(expectedDocuments);
    }

    @Test
    public void testGlobalGetById() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_BY_ID));
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchBatchResult<String, GenericDocument> nonExistent =
                mGlobalSearchSession.getByDocumentIdAsync(mContext.getPackageName(), DB_NAME_1,
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1")
                                .build()).get();

        assertThat(nonExistent.isSuccess()).isFalse();
        assertThat(nonExistent.getSuccesses()).isEmpty();
        assertThat(nonExistent.getFailures()).containsKey("id1");
        assertThat(nonExistent.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        AppSearchBatchResult<String, GenericDocument> afterPutDocuments =
                mGlobalSearchSession.getByDocumentIdAsync(mContext.getPackageName(), DB_NAME_1,
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1")
                                .build()).get();
        assertThat(afterPutDocuments.getSuccesses()).containsExactly("id1", inEmail);
    }

    @Test
    public void testGlobalGetById_nonExistentPackage() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_BY_ID));
        AppSearchBatchResult<String, GenericDocument> fakePackage =
                mGlobalSearchSession.getByDocumentIdAsync("fake", DB_NAME_1,
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1")
                                .build()).get();
        assertThat(fakePackage.getFailures()).hasSize(1);
        assertThat(fakePackage.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testGlobalQuery_oneInstance() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);
        List<GenericDocument> beforeBodyEmailDocuments = snapshotResults("body email",
                exactSearchSpec);

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        List<GenericDocument> afterBodyDocuments = snapshotResults("body", exactSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyDocuments, afterBodyDocuments,
                Collections.singletonList(inEmail));

        // Multi-term query
        List<GenericDocument> afterBodyEmailDocuments = snapshotResults("body email",
                exactSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyEmailDocuments, afterBodyEmailDocuments,
                Collections.singletonList(inEmail));
    }

    @Test
    public void testGlobalQuery_twoInstances() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Query across all instances
        List<GenericDocument> afterBodyDocuments = snapshotResults("body", exactSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyDocuments, afterBodyDocuments,
                ImmutableList.of(inEmail1, inEmail2));
    }

    @Test
    public void testGlobalQuery_getNextPage() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        List<AppSearchEmail> emailList = new ArrayList<>();
        PutDocumentsRequest.Builder putDocumentsRequestBuilder = new PutDocumentsRequest.Builder();

        // Index 31 documents
        for (int i = 0; i < 31; i++) {
            AppSearchEmail inEmail =
                    new AppSearchEmail.Builder("namespace", "id" + i)
                            .setFrom("from@example.com")
                            .setTo("to1@example.com", "to2@example.com")
                            .setSubject("testPut example")
                            .setBody("This is the body of the testPut email")
                            .build();
            emailList.add(inEmail);
            putDocumentsRequestBuilder.addGenericDocuments(inEmail);
        }
        checkIsBatchResultSuccess(mDb1.putAsync(putDocumentsRequestBuilder.build()));

        // Set number of results per page is 7.
        int pageSize = 7;
        SearchResults searchResults = mGlobalSearchSession.search("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultCountPerPage(pageSize)
                        .build());
        List<GenericDocument> documents = new ArrayList<>();

        int pageNumber = 0;
        List<SearchResult> results;

        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            ++pageNumber;
            for (SearchResult result : results) {
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);

        // check all document presents
        assertAddedBetweenSnapshots(beforeBodyDocuments, documents, emailList);

        int totalDocuments = beforeBodyDocuments.size() + documents.size();

        // +1 for final empty page
        int expectedPages = (int) Math.ceil(totalDocuments * 1.0 / pageSize) + 1;
        assertThat(pageNumber).isEqualTo(expectedPages);
    }

    @Test
    public void testGlobalQuery_acrossTypes() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);

        SearchSpec exactEmailSearchSpec =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build();
        List<GenericDocument> beforeBodyEmailDocuments = snapshotResults("body",
                exactEmailSearchSpec);

        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();

        // db1 has both "Generic" and "builtin:Email"
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(genericSchema).addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // db2 only has "builtin:Email"
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a generic document into db1
        GenericDocument genericDocument = new GenericDocument.Builder<>("namespace", "id2",
                "Generic")
                .setPropertyString("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(genericDocument).build()));

        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        // Put the email in both databases
        checkIsBatchResultSuccess((mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build())));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Query for all documents across types
        List<GenericDocument> afterBodyDocuments = snapshotResults("body", exactSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyDocuments, afterBodyDocuments,
                ImmutableList.of(genericDocument, email, email));

        // Query only for email documents
        List<GenericDocument> afterBodyEmailDocuments = snapshotResults("body",
                exactEmailSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyEmailDocuments, afterBodyEmailDocuments,
                ImmutableList.of(email, email));
    }

    @Test
    public void testGlobalQuery_namespaceFilter() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);

        SearchSpec exactNamespace1SearchSpec =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterNamespaces("namespace1")
                        .build();
        List<GenericDocument> beforeBodyNamespace1Documents = snapshotResults("body",
                exactNamespace1SearchSpec);

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail document1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(document1).build()));

        AppSearchEmail document2 =
                new AppSearchEmail.Builder("namespace2", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document2).build()));

        // Query for all namespaces
        List<GenericDocument> afterBodyDocuments = snapshotResults("body", exactSearchSpec);
        assertAddedBetweenSnapshots(beforeBodyDocuments, afterBodyDocuments,
                ImmutableList.of(document1, document2));

        // Query only for "namespace1"
        List<GenericDocument> afterBodyNamespace1Documents = snapshotResults("body",
                exactNamespace1SearchSpec);
        assertAddedBetweenSnapshots(beforeBodyNamespace1Documents, afterBodyNamespace1Documents,
                ImmutableList.of(document1));
    }

    @Test
    public void testGlobalQuery_packageFilter() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec otherPackageSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterPackageNames("some.other.package")
                .build();
        List<GenericDocument> beforeOtherPackageDocuments = snapshotResults("body",
                otherPackageSearchSpec);

        SearchSpec testPackageSearchSpec =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterPackageNames(mContext.getPackageName())
                        .build();
        List<GenericDocument> beforeTestPackageDocuments = snapshotResults("body",
                testPackageSearchSpec);

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail document1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(document1).build()));

        AppSearchEmail document2 =
                new AppSearchEmail.Builder("namespace2", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document2).build()));

        // Query in some other package
        List<GenericDocument> afterOtherPackageDocuments = snapshotResults("body",
                otherPackageSearchSpec);
        assertAddedBetweenSnapshots(beforeOtherPackageDocuments, afterOtherPackageDocuments,
                Collections.emptyList());

        // Query within our package
        List<GenericDocument> afterTestPackageDocuments = snapshotResults("body",
                testPackageSearchSpec);
        assertAddedBetweenSnapshots(beforeTestPackageDocuments, afterTestPackageDocuments,
                ImmutableList.of(document1, document2));
    }

    // TODO(b/175039682) Add test cases for wildcard projection once go/oag/1534646 is submitted.
    @Test
    public void testGlobalQuery_projectionTwoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index one document in each database.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email2).build()));

        // Query with type property paths {"Email", ["subject", "to"]}
        List<GenericDocument> documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addProjection(
                                AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                        .build());

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(documents).containsExactly(expected1, expected2);
    }

    @Test
    public void testGlobalQuery_projectionEmptyTwoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index one document in each database.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email2).build()));

        // Query with type property paths {"Email", []}
        List<GenericDocument> documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addProjection(AppSearchEmail.SCHEMA_TYPE,
                                Collections.emptyList())
                        .build());

        // The two email documents should have been returned without any properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        assertThat(documents).containsExactly(expected1, expected2);
    }

    @Test
    public void testGlobalQuery_projectionNonExistentTypeTwoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index one document in each database.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email2).build()));

        // Query with type property paths {"NonExistentType", []}, {"Email", ["subject", "to"]}
        List<GenericDocument> documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addProjection("NonExistentType", Collections.emptyList())
                        .addProjection(
                                AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                        .build());

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(documents).containsExactly(expected1, expected2);
    }

    @Test
    public void testQuery_ResultGroupingLimits() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index one document in 'namespace1' and one document in 'namespace2' into db1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace2", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Index one document in 'namespace1' and one document in 'namespace2' into db2.
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace1", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));

        // Query with per package result grouping. Only the last document 'email4' should be
        // returned.
        List<GenericDocument> documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                        .build());
        assertThat(documents).containsExactly(inEmail4);

        // Query with per namespace result grouping. Only the last document in each namespace should
        // be returned ('email4' and 'email3').
        documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_NAMESPACE, /*resultLimit=*/ 1)
                        .build());
        assertThat(documents).containsExactly(inEmail4, inEmail3);

        // Query with per package and per namespace result grouping. Only the last document in each
        // namespace should be returned ('email4' and 'email3').
        documents =
                snapshotResults("body", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                        .build());
        assertThat(documents).containsExactly(inEmail4, inEmail3);
    }

    @Test
    public void testReportSystemUsage_ForbiddenFromNonSystem() throws Exception {
        // Index a document
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        // Query
        List<SearchResult> page;
        try (SearchResults results = mGlobalSearchSession.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                .build())) {
            page = results.getNextPageAsync().get();
        }
        assertThat(page).isNotEmpty();
        SearchResult firstResult = page.get(0);

        ExecutionException exception = assertThrows(
                ExecutionException.class, () -> mGlobalSearchSession.reportSystemUsageAsync(
                        new ReportSystemUsageRequest.Builder(
                                firstResult.getPackageName(),
                                firstResult.getDatabaseName(),
                                firstResult.getGenericDocument().getNamespace(),
                                firstResult.getGenericDocument().getId())
                                .build()).get());
        assertThat(exception).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException ase = (AppSearchException) exception.getCause();
        assertThat(ase.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);
        assertThat(ase).hasMessageThat().contains(
                mContext.getPackageName() + " does not have access to report system usage");
    }

    @Test
    public void testAddObserver_notSupported() {
        assumeFalse(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));
        assertThrows(
                UnsupportedOperationException.class,
                () -> mGlobalSearchSession.registerObserverCallback(
                        mContext.getPackageName(),
                        new ObserverSpec.Builder().build(),
                        EXECUTOR,
                        new TestObserverCallback()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> mGlobalSearchSession.unregisterObserverCallback(
                        mContext.getPackageName(), new TestObserverCallback()));
    }

    @Test
    public void testAddObserver() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback observer = new TestObserverCallback();

        // Register observer. Note: the type does NOT exist yet!
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                observer);

        // Index a document
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        // Make sure the notification was received.
        observer.waitForNotificationCount(2);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        /*changedSchemaNames=*/ImmutableSet.of(AppSearchEmail.SCHEMA_TYPE)));
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1"))
        );
    }

    @Test
    public void testRegisterObserver_MultiType() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email type in both databases, and the gift type in db1
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Register two observers. One has no filters, the other filters on email.
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                emailObserver);

        // Make sure everything is empty
        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).isEmpty();
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).isEmpty();

        // Index some documents
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id2", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(gift1).build()));

        // Make sure the notification was received.
        unfilteredObserver.waitForNotificationCount(5);
        emailObserver.waitForNotificationCount(3);

        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id2")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id2"))
        );

        // Check the filtered observer
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1"))
        );
    }

    @Test
    public void testRegisterObserver_removeById() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email and gift types in both databases
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register two observers. One, registered later, has no filters. The other, registered
        // now, filters on email.
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                emailObserver);

        // Make sure everything is empty
        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).isEmpty();
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).isEmpty();

        // Index some documents
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id2", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(gift1).build()));

        // Register the second observer
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);

        // Remove some of the documents.
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()));
        checkIsBatchResultSuccess(mDb2.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace2").addIds("id2").build()));

        // Make sure the notification was received. emailObserver should have seen:
        //   +db1:email, +db1:email, +db2:email, -db1:email.
        // unfilteredObserver (registered later) should have seen:
        //   -db1:email, -db2:gift
        emailObserver.waitForNotificationCount(4);
        unfilteredObserver.waitForNotificationCount(2);

        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1"))
        );

        // Check unfilteredObserver
        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id2"))
        );
    }

    @Test
    public void testRegisterObserver_removeByQuery() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email and gift types in both databases
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Index some documents
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").setBody("caterpillar").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id3", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(
                        email1, email2, gift1).build()));

        // Register observers
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                emailObserver);

        // Make sure everything is empty
        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).isEmpty();
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).isEmpty();

        // Remove "cat" emails in db1 and all types in db2
        mDb1.removeAsync("cat",
                        new SearchSpec.Builder()
                                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build())
                .get();
        mDb2.removeAsync("", new SearchSpec.Builder().build()).get();

        // Make sure the notification was received. UnfilteredObserver should have seen:
        //   -db1:id2, -db2:id1, -db2:id2, -db2:id3
        // emailObserver should have seen:
        //   -db1:id2, -db2:id1, -db2:id2
        unfilteredObserver.waitForNotificationCount(3);
        emailObserver.waitForNotificationCount(2);

        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id2")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1", "id2")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id3"))
        );

        // Check emailObserver
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id2")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1", "id2"))
        );
    }

    @Test
    public void testRegisterObserver_sameCallback_differentSpecs() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback observer = new TestObserverCallback();

        // Set up the email and gift types
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register the same observer twice: once for gift, once for email
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Gift").build(),
                EXECUTOR,
                observer);
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                observer);

        // Index one email and one gift
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id3", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));

        // Make sure the same observer received both values
        observer.waitForNotificationCount(2);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id3"))
        );
    }

    @Test
    public void testRemoveObserver() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        TestObserverCallback temporaryObserver = new TestObserverCallback();
        TestObserverCallback permanentObserver = new TestObserverCallback();

        // Set up the email and gift types
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register both observers. temporaryObserver is registered twice to ensure both instances
        // get removed.
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                temporaryObserver);
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Gift").build(),
                EXECUTOR,
                temporaryObserver);
        mGlobalSearchSession.registerObserverCallback(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                permanentObserver);

        // Make sure everything is empty
        assertThat(temporaryObserver.getSchemaChanges()).isEmpty();
        assertThat(temporaryObserver.getDocumentChanges()).isEmpty();
        assertThat(permanentObserver.getSchemaChanges()).isEmpty();
        assertThat(permanentObserver.getDocumentChanges()).isEmpty();

        // Index some documents
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").setBody("caterpillar").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id3", "Gift").build();
        GenericDocument gift2 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace3", "id4", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));

        // Make sure the notifications were received.
        temporaryObserver.waitForNotificationCount(2);
        permanentObserver.waitForNotificationCount(2);

        List<DocumentChangeInfo> expectedChangesOrig = ImmutableList.of(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id3")));
        assertThat(temporaryObserver.getSchemaChanges()).isEmpty();
        assertThat(temporaryObserver.getDocumentChanges())
                .containsExactlyElementsIn(expectedChangesOrig);
        assertThat(permanentObserver.getSchemaChanges()).isEmpty();
        assertThat(permanentObserver.getDocumentChanges())
                .containsExactlyElementsIn(expectedChangesOrig);

        // Unregister temporaryObserver
        mGlobalSearchSession.unregisterObserverCallback(
                mContext.getPackageName(), temporaryObserver);

        // Index some more documents
        checkIsBatchResultSuccess(
                mDb1.putAsync(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email2, gift2).build()));

        // Only the permanent observer should have received this
        permanentObserver.waitForNotificationCount(4);
        temporaryObserver.waitForNotificationCount(2);

        assertThat(permanentObserver.getSchemaChanges()).isEmpty();
        assertThat(permanentObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id1")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id3")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE,
                        /*changedDocumentIds=*/ImmutableSet.of("id2")),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace3",
                        "Gift",
                        /*changedDocumentIds=*/ImmutableSet.of("id4"))
        );
        assertThat(temporaryObserver.getSchemaChanges()).isEmpty();
        assertThat(temporaryObserver.getDocumentChanges())
                .containsExactlyElementsIn(expectedChangesOrig);
    }

    @Test
    public void testGlobalGetSchema() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA));

        // One schema should be set with global access and the other should be set with local
        // access.
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(
                        AppSearchEmail.SCHEMA).setSchemaTypeDisplayedBySystem(
                        AppSearchEmail.SCHEMA_TYPE, /*displayed=*/false).build()).get();

        GetSchemaResponse response = mGlobalSearchSession.getSchemaAsync(mContext.getPackageName(),
                DB_NAME_1).get();
        assertThat(response.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);

        response = mGlobalSearchSession.getSchemaAsync(mContext.getPackageName(), DB_NAME_2).get();
        assertThat(response.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);

        // A request for a db that doesn't exist should return a response with no schemas.
        response = mGlobalSearchSession.getSchemaAsync(
                mContext.getPackageName(), "NonexistentDb").get();
        assertThat(response.getSchemas()).isEmpty();
    }

    @Test
    public void testGlobalGetSchema_notSupported() throws Exception {
        assumeFalse(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA));

        // One schema should be set with global access and the other should be set with local
        // access.
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> mGlobalSearchSession.getSchemaAsync(mContext.getPackageName(), DB_NAME_1));
        assertThat(e).hasMessageThat().isEqualTo(Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA
                + " is not supported on this AppSearch implementation.");
    }

    @Test
    public void testGlobalGetByDocumentId_notSupported() throws Exception {
        assumeFalse(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_GET_BY_ID));

        Context context = ApplicationProvider.getApplicationContext();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> mGlobalSearchSession.getByDocumentIdAsync(context.getPackageName(), DB_NAME_1,
                        new GetByDocumentIdRequest.Builder("namespace")
                                .addIds("id").build()));

        assertThat(e).hasMessageThat().isEqualTo(Features.GLOBAL_SEARCH_SESSION_GET_BY_ID
                + " is not supported on this AppSearch implementation.");
    }

    @Test
    public void testAddObserver_schemaChange_added() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mGlobalSearchSession.registerObserverCallback(
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                observer);

        // Add a schema type
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(new AppSearchSchema.Builder("Type1").build())
                                .build())
                .get();

        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        ImmutableSet.of("Type1")));
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Add two more schema types without touching the existing one
        observer.clear();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(
                                        new AppSearchSchema.Builder("Type1").build(),
                                        new AppSearchSchema.Builder("Type2").build(),
                                        new AppSearchSchema.Builder("Type3").build())
                                .build())
                .get();

        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), DB_NAME_1, ImmutableSet.of("Type2", "Type3")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_removed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        // Add a schema type
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(
                                        new AppSearchSchema.Builder("Type1").build(),
                                        new AppSearchSchema.Builder("Type2").build())
                                .build())
                .get();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mGlobalSearchSession.registerObserverCallback(
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                observer);

        // Remove Type2
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(new AppSearchSchema.Builder("Type1").build())
                                .setForceOverride(true)
                                .build())
                .get();

        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), DB_NAME_1, ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_contents() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        // Add a schema
        mDb1.setSchemaAsync(
            new SetSchemaRequest.Builder()
                    .addSchemas(
                            new AppSearchSchema.Builder("Type1").build(),
                            new AppSearchSchema.Builder("Type2")
                                    .addProperty(
                                            new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                    "booleanProp")
                                                    .setCardinality(
                                                            PropertyConfig.CARDINALITY_REQUIRED)
                                                    .build())
                                    .build())
                    .build())
            .get();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mGlobalSearchSession.registerObserverCallback(
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                observer);

        // Update the schema, but don't make any actual changes
        mDb1.setSchemaAsync(
            new SetSchemaRequest.Builder()
                    .addSchemas(
                            new AppSearchSchema.Builder("Type1").build(),
                            new AppSearchSchema.Builder("Type2")
                                    .addProperty(
                                            new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                    "booleanProp")
                                                    .setCardinality(
                                                            PropertyConfig.CARDINALITY_REQUIRED)
                                                    .build())
                                    .build())
                    .build())
            .get();

        // Now update the schema again, but this time actually make a change (cardinality of the
        // property)
        mDb1.setSchemaAsync(
            new SetSchemaRequest.Builder()
                    .addSchemas(
                            new AppSearchSchema.Builder("Type1").build(),
                            new AppSearchSchema.Builder("Type2")
                                    .addProperty(
                                            new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                    "booleanProp")
                                                    .setCardinality(
                                                            PropertyConfig.CARDINALITY_OPTIONAL)
                                                    .build())
                                    .build())
                    .build())
            .get();

        // Dispatch notifications
        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), DB_NAME_1, ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_contents_skipBySpec() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        // Add a schema
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(
                                new AppSearchSchema.Builder("Type1")
                                        .addProperty(
                                                new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                        "booleanProp")
                                                        .setCardinality(
                                                                PropertyConfig.CARDINALITY_REQUIRED)
                                                        .build())
                                        .build(),
                                new AppSearchSchema.Builder("Type2")
                                        .addProperty(
                                                new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                        "booleanProp")
                                                        .setCardinality(
                                                                PropertyConfig.CARDINALITY_REQUIRED)
                                                        .build())
                                        .build())
                        .build())
                .get();

        // Register an observer that only listens for Type2
        TestObserverCallback observer = new TestObserverCallback();
        mGlobalSearchSession.registerObserverCallback(
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Type2").build(),
                EXECUTOR,
                observer);

        // Update both types of the schema (changed cardinalities)
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(
                                new AppSearchSchema.Builder("Type1")
                                        .addProperty(
                                                new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                        "booleanProp")
                                                        .setCardinality(
                                                                PropertyConfig.CARDINALITY_OPTIONAL)
                                                        .build())
                                        .build(),
                                new AppSearchSchema.Builder("Type2")
                                        .addProperty(
                                                new AppSearchSchema.BooleanPropertyConfig.Builder(
                                                        "booleanProp")
                                                        .setCardinality(
                                                                PropertyConfig.CARDINALITY_OPTIONAL)
                                                        .build())
                                        .build())
                        .build())
                .get();

        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), DB_NAME_1, ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testRegisterObserver_schemaMigration() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK));

        // Add a schema with two types
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setVersion(1)
                .addSchemas(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(
                                        new AppSearchSchema.StringPropertyConfig.Builder("strProp1")
                                                .build()
                                ).build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(
                                        new AppSearchSchema.LongPropertyConfig.Builder("longProp1")
                                                .build()
                                ).build()
                        ).build()
        ).get();

        // Index some documents
        GenericDocument type1doc1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace", "t1id1", "Type1")
                .setPropertyString("strProp1", "t1id1 prop value")
                .build();
        GenericDocument type1doc2 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace", "t1id2", "Type1")
                .setPropertyString("strProp1", "t1id2 prop value")
                .build();
        GenericDocument type2doc1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace", "t2id1", "Type2")
                .setPropertyLong("longProp1", 41)
                .build();
        GenericDocument type2doc2 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace", "t2id2", "Type2")
                .setPropertyLong("longProp1", 42)
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(type1doc1, type1doc2, type2doc1, type2doc2)
                .build())
                .get();

        // Register an observer that only listens for Type1
        TestObserverCallback observer = new TestObserverCallback();
        mGlobalSearchSession.registerObserverCallback(
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Type1").build(),
                EXECUTOR,
                observer);

        // Update both types of the schema with migration to a new property name
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setVersion(2)
                .addSchemas(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(
                                        new AppSearchSchema.StringPropertyConfig.Builder("strProp2")
                                                .build()
                                ).build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(
                                        new AppSearchSchema.LongPropertyConfig.Builder("longProp2")
                                                .build()
                                ).build()
                        )
                .setMigrator("Type1", new Migrator() {
                    @Override
                    public boolean shouldMigrate(int currentVersion, int finalVersion) {
                        assertThat(currentVersion).isEqualTo(1);
                        assertThat(finalVersion).isEqualTo(2);
                        return true;
                    }

                    @NonNull
                    @Override
                    public GenericDocument onUpgrade(
                            int currentVersion,
                            int finalVersion,
                            @NonNull GenericDocument document) {
                        assertThat(currentVersion).isEqualTo(1);
                        assertThat(finalVersion).isEqualTo(2);
                        assertThat(document.getSchemaType()).isEqualTo("Type1");
                        String[] prop = document.getPropertyStringArray("strProp1");
                        assertThat(prop).isNotNull();
                        return new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                document.getNamespace(),
                                document.getId(),
                                document.getSchemaType())
                                .setPropertyString("strProp2", prop)
                        .build();
                    }

                    @NonNull
                    @Override
                    public GenericDocument onDowngrade(
                            int currentVersion,
                            int finalVersion,
                            @NonNull GenericDocument document) {
                        // Doesn't happen in this test
                        throw new UnsupportedOperationException();
                    }
                }).setMigrator("Type2", new Migrator() {
                    @Override
                    public boolean shouldMigrate(int currentVersion, int finalVersion) {
                        assertThat(currentVersion).isEqualTo(1);
                        assertThat(finalVersion).isEqualTo(2);
                        return true;
                    }

                    @NonNull
                    @Override
                    public GenericDocument onUpgrade(
                            int currentVersion,
                            int finalVersion,
                            @NonNull GenericDocument document) {
                        assertThat(currentVersion).isEqualTo(1);
                        assertThat(finalVersion).isEqualTo(2);
                        assertThat(document.getSchemaType()).isEqualTo("Type2");
                        long[] prop = document.getPropertyLongArray("longProp1");
                        assertThat(prop).isNotNull();
                        return new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                document.getNamespace(),
                                document.getId(),
                                document.getSchemaType())
                                .setPropertyLong("longProp2", prop[0] + 1000)
                        .build();
                    }

                    @NonNull
                    @Override
                    public GenericDocument onDowngrade(
                            int currentVersion,
                            int finalVersion,
                            @NonNull GenericDocument document) {
                        // Doesn't happen in this test
                        throw new UnsupportedOperationException();
                    }
                })
                .build()
        ).get();

        // Make sure the test is valid by checking that migration actually occurred
        AppSearchBatchResult<String, GenericDocument> getResponse = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("t1id1", "t1id2", "t2id1", "t2id2")
                        .build())
                .get();
        assertThat(getResponse.isSuccess()).isTrue();
        assertThat(getResponse.getSuccesses().get("t1id1").getPropertyString("strProp2"))
                .isEqualTo("t1id1 prop value");
        assertThat(getResponse.getSuccesses().get("t1id2").getPropertyString("strProp2"))
                .isEqualTo("t1id2 prop value");
        assertThat(getResponse.getSuccesses().get("t2id1").getPropertyLong("longProp2"))
                .isEqualTo(1041);
        assertThat(getResponse.getSuccesses().get("t2id2").getPropertyLong("longProp2"))
                .isEqualTo(1042);

        // Per the observer documentation, for schema migrations, individual document changes are
        // not dispatched. Only SchemaChangeInfo is dispatched.
        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), DB_NAME_1, ImmutableSet.of("Type1")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testGlobalQuery_propertyWeights() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // RELEVANCE scoring depends on stats for the namespace+type of the scored document, namely
        // the average document length. This average document length calculation is only updated
        // when documents are added and when compaction runs. This means that old deleted
        // documents of the same namespace and type combination *can* affect RELEVANCE scores
        // through this channel.
        // To avoid this, we use a unique namespace that will not be shared by any other test
        // case or any other run of this test.
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        String namespace = "propertyWeightsNamespace" + System.currentTimeMillis();
        // Put two documents in separate databases.
        AppSearchEmail emailDb1 =
                new AppSearchEmail.Builder(namespace, "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(emailDb1).build()));
        AppSearchEmail emailDb2 =
                new AppSearchEmail.Builder(namespace, "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(emailDb2).build()));

        // Issue global query for "foo".
        SearchResults searchResults = mGlobalSearchSession.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE,
                        ImmutableMap.of("subject",
                                2.0, "body", 0.5))
                .addFilterNamespaces(namespace)
                .build());
        List<SearchResult> globalResults = retrieveAllSearchResults(searchResults);

        // We expect to two emails, one from each of the databases.
        assertThat(globalResults).hasSize(2);
        assertThat(globalResults.get(0).getGenericDocument()).isEqualTo(emailDb1);
        assertThat(globalResults.get(1).getGenericDocument()).isEqualTo(emailDb2);

        // We expect that the email added to db1 will have a higher score than the email added to
        // db2 as the query term "foo" is contained in the "subject" property which has a higher
        // weight than the "body" property.
        assertThat(globalResults.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(globalResults.get(0).getRankingSignal()).isGreaterThan(
                globalResults.get(1).getRankingSignal());

        // Query for "foo" without property weights.
        SearchResults searchResultsWithoutWeights = mGlobalSearchSession.search("foo",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                        .setOrder(SearchSpec.ORDER_DESCENDING)
                        .addFilterNamespaces(namespace)
                        .build());
        List<SearchResult> resultsWithoutWeights =
                retrieveAllSearchResults(searchResultsWithoutWeights);

        // email1 should have the same ranking signal as email2 as each contains the term "foo"
        // once.
        assertThat(resultsWithoutWeights).hasSize(2);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isEqualTo(
                resultsWithoutWeights.get(1).getRankingSignal());
    }
}
