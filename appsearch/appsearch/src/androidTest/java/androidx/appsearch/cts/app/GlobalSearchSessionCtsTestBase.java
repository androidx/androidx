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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
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
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
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

    protected abstract ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName);

    protected abstract ListenableFuture<GlobalSearchSession> createGlobalSearchSession();

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSession(DB_NAME_1).get();
        mDb2 = createSearchSession(DB_NAME_2).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();

        mGlobalSearchSession = createGlobalSearchSession().get();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchema(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchema(
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
    public void testGlobalQuery_oneInstance() throws Exception {
        // Snapshot what documents may already exist on the device.
        SearchSpec exactSearchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        List<GenericDocument> beforeBodyDocuments = snapshotResults("body", exactSearchSpec);
        List<GenericDocument> beforeBodyEmailDocuments = snapshotResults("body email",
                exactSearchSpec);

        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(putDocumentsRequestBuilder.build()));

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
            results = searchResults.getNextPage().get();
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchemas(genericSchema).addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // db2 only has "builtin:Email"
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a generic document into db1
        GenericDocument genericDocument = new GenericDocument.Builder<>("namespace", "id2",
                "Generic")
                .setPropertyString("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.put(
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
        checkIsBatchResultSuccess((mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build())));
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail document1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(document1).build()));

        AppSearchEmail document2 =
                new AppSearchEmail.Builder("namespace2", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail document1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(document1).build()));

        AppSearchEmail document2 =
                new AppSearchEmail.Builder("namespace2", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();
        mDb2.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index one document in 'namespace1' and one document in 'namespace2' into db1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace2", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Index one document in 'namespace1' and one document in 'namespace2' into db2.
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace1", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.setSchema(
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
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Query
        List<SearchResult> page;
        try (SearchResults results = mGlobalSearchSession.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                .build())) {
            page = results.getNextPage().get();
        }
        assertThat(page).isNotEmpty();
        SearchResult firstResult = page.get(0);

        ExecutionException exception = assertThrows(
                ExecutionException.class, () -> mGlobalSearchSession.reportSystemUsage(
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
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));
        assertThrows(
                UnsupportedOperationException.class,
                () -> mGlobalSearchSession.addObserver(
                        mContext.getPackageName(),
                        new ObserverSpec.Builder().build(),
                        EXECUTOR,
                        new TestObserverCallback()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> mGlobalSearchSession.removeObserver(
                        mContext.getPackageName(), new TestObserverCallback()));
    }

    @Test
    public void testAddObserver() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback observer = new TestObserverCallback();

        // Register observer. Note: the type does NOT exist yet!
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                observer);

        // Index a document
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Make sure the notification was received.
        observer.waitForNotificationCount(1);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE));
    }

    @Test
    public void testRegisterObserver_MultiType() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email type in both databases, and the gift type in db1
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Register two observers. One has no filters, the other filters on email.
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);
        mGlobalSearchSession.addObserver(
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
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.put(new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(gift1).build()));

        // Make sure the notification was received.
        unfilteredObserver.waitForNotificationCount(5);
        emailObserver.waitForNotificationCount(3);

        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift"),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift")
        );

        // Check the filtered observer
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE)
        );
    }

    @Test
    public void testRegisterObserver_removeById() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email and gift types in both databases
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register two observers. One, registered later, has no filters. The other, registered
        // now, filters on email.
        mGlobalSearchSession.addObserver(
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
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(gift1).build()));

        // Register the second observer
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);

        // Remove some of the documents.
        checkIsBatchResultSuccess(mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()));
        checkIsBatchResultSuccess(mDb2.remove(
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
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE)
        );

        // Check unfilteredObserver
        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace2",
                        "Gift")
        );
    }

    @Test
    public void testRegisterObserver_removeByQuery() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback unfilteredObserver = new TestObserverCallback();
        TestObserverCallback emailObserver = new TestObserverCallback();

        // Set up the email and gift types in both databases
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Index some documents
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").setBody("caterpillar").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id3", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2, gift1).build()));
        checkIsBatchResultSuccess(
                mDb2.put(new PutDocumentsRequest.Builder().addGenericDocuments(
                        email1, email2, gift1).build()));

        // Register observers
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                EXECUTOR,
                unfilteredObserver);
        mGlobalSearchSession.addObserver(
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
        mDb1.remove("cat",
                        new SearchSpec.Builder()
                                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build())
                .get();
        mDb2.remove("", new SearchSpec.Builder().build()).get();

        // Make sure the notification was received. UnfilteredObserver should have seen:
        //   -db1:id2, -db2:id1, -db2:id2, -db2:id3
        // emailObserver should have seen:
        //   -db1:id2, -db2:id1, -db2:id2
        // TODO(b/193494000): Notifications are currently grouped by
        //  (package, database, namespace, schema). This causes -db2:id1 and -db2:id2 to be combined
        //  into one notification. Once notifications have IDs, we need to check to make sure all
        //  the individual IDs are reported in the combined notification.
        unfilteredObserver.waitForNotificationCount(3);
        emailObserver.waitForNotificationCount(2);

        assertThat(unfilteredObserver.getSchemaChanges()).isEmpty();
        assertThat(unfilteredObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace2",
                        "Gift")
        );

        // Check emailObserver
        assertThat(emailObserver.getSchemaChanges()).isEmpty();
        assertThat(emailObserver.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_2,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE)
        );
    }

    @Test
    public void testRegisterObserver_sameCallback_differentSpecs() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback observer = new TestObserverCallback();

        // Set up the email and gift types
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register the same observer twice: once for gift, once for email
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Gift").build(),
                EXECUTOR,
                observer);
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                observer);

        // Index one email and one gift
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        GenericDocument gift1 = new GenericDocument.Builder<GenericDocument.Builder<?>>(
                "namespace2", "id3", "Gift").build();

        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));

        // Make sure the same observer received both values
        observer.waitForNotificationCount(2);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift")
        );
    }

    @Test
    public void testRemoveObserver() throws Exception {
        assumeTrue(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_ADD_REMOVE_OBSERVER));

        TestObserverCallback temporaryObserver = new TestObserverCallback();
        TestObserverCallback permanentObserver = new TestObserverCallback();

        // Set up the email and gift types
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("price").build())
                .build();
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, giftSchema).build()).get();

        // Register both observers. temporaryObserver is registered twice to ensure both instances
        // get removed.
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas(AppSearchEmail.SCHEMA_TYPE).build(),
                EXECUTOR,
                temporaryObserver);
        mGlobalSearchSession.addObserver(
                mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Gift").build(),
                EXECUTOR,
                temporaryObserver);
        mGlobalSearchSession.addObserver(
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
                mDb1.put(new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, gift1).build()));

        // Make sure the notifications were received.
        temporaryObserver.waitForNotificationCount(2);
        permanentObserver.waitForNotificationCount(2);

        List<DocumentChangeInfo> expectedChangesOrig = ImmutableList.of(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift"));
        assertThat(temporaryObserver.getSchemaChanges()).isEmpty();
        assertThat(temporaryObserver.getDocumentChanges())
                .containsExactlyElementsIn(expectedChangesOrig);
        assertThat(permanentObserver.getSchemaChanges()).isEmpty();
        assertThat(permanentObserver.getDocumentChanges())
                .containsExactlyElementsIn(expectedChangesOrig);

        // Unregister temporaryObserver
        mGlobalSearchSession.removeObserver(mContext.getPackageName(), temporaryObserver);

        // Index some more documents
        checkIsBatchResultSuccess(
                mDb1.put(new PutDocumentsRequest.Builder()
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
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace2",
                        "Gift"),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace",
                        AppSearchEmail.SCHEMA_TYPE),
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        DB_NAME_1,
                        "namespace3",
                        "Gift")
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(
                        AppSearchEmail.SCHEMA).setSchemaTypeDisplayedBySystem(
                        AppSearchEmail.SCHEMA_TYPE, /*displayed=*/false).build()).get();

        GetSchemaResponse response = mGlobalSearchSession.getSchema(mContext.getPackageName(),
                DB_NAME_1).get();
        assertThat(response.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);

        response = mGlobalSearchSession.getSchema(mContext.getPackageName(), DB_NAME_2).get();
        assertThat(response.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);

        // A request for a db that doesn't exist should return a response with no schemas.
        response = mGlobalSearchSession.getSchema(mContext.getPackageName(), "NonexistentDb").get();
        assertThat(response.getSchemas()).isEmpty();
    }

    @Test
    public void testGlobalGetSchema_notSupported() throws Exception {
        assumeFalse(mGlobalSearchSession.getFeatures()
                .isFeatureSupported(Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA));

        // One schema should be set with global access and the other should be set with local
        // access.
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> mGlobalSearchSession.getSchema(mContext.getPackageName(), DB_NAME_1));
        assertThat(e).hasMessageThat().isEqualTo(Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA
                + " is not supported on this AppSearch implementation.");
    }
}
