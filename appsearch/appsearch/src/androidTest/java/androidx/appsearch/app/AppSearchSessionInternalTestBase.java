/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.testutil.AppSearchEmail;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public abstract class AppSearchSessionInternalTestBase {

    static final String DB_NAME_1 = "";

    private AppSearchSession mDb1;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    // TODO(b/228240987) delete this test when we support property restrict for multiple terms
    @Test
    public void testSearchSuggestion_propertyFilter() throws Exception {
        // Schema registration
        AppSearchSchema schemaType1 =
                new AppSearchSchema.Builder("Type1")
                        .addProperty(
                                new StringPropertyConfig.Builder("propertyone")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("propertytwo")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema schemaType2 =
                new AppSearchSchema.Builder("Type2")
                        .addProperty(
                                new StringPropertyConfig.Builder("propertythree")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("propertyfour")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder().addSchemas(schemaType1, schemaType2).build())
                .get();

        // Index documents
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Type1")
                        .setPropertyString("propertyone", "termone")
                        .setPropertyString("propertytwo", "termtwo")
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "Type2")
                        .setPropertyString("propertythree", "termthree")
                        .setPropertyString("propertyfour", "termfour")
                        .build();

        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2).build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        // Only search for type1/propertyone
        List<SearchSuggestionResult> suggestions =
                mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t",
                                new SearchSuggestionSpec.Builder(/* totalResultCount= */ 10)
                                        .addFilterSchemas("Type1")
                                        .addFilterProperties(
                                                "Type1", ImmutableList.of("propertyone"))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne);

        // Only search for type1/propertyone and type1/propertytwo
        suggestions =
                mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t",
                                new SearchSuggestionSpec.Builder(/* totalResultCount= */ 10)
                                        .addFilterSchemas("Type1")
                                        .addFilterProperties(
                                                "Type1",
                                                ImmutableList.of("propertyone", "propertytwo"))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo);

        // Only search for type1/propertyone and type2/propertythree
        suggestions =
                mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t",
                                new SearchSuggestionSpec.Builder(/* totalResultCount= */ 10)
                                        .addFilterSchemas("Type1", "Type2")
                                        .addFilterProperties(
                                                "Type1", ImmutableList.of("propertyone"))
                                        .addFilterProperties(
                                                "Type2", ImmutableList.of("propertythree"))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne, resultThree);

        // Only search for type1/propertyone and type2/propertyfour, in addFilterPropertyPaths
        suggestions =
                mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t",
                                new SearchSuggestionSpec.Builder(/* totalResultCount= */ 10)
                                        .addFilterSchemas("Type1", "Type2")
                                        .addFilterProperties(
                                                "Type1", ImmutableList.of("propertyone"))
                                        .addFilterPropertyPaths(
                                                "Type2",
                                                ImmutableList.of(new PropertyPath("propertyfour")))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne, resultFour);

        // Only search for type1/propertyone and everything in type2
        suggestions =
                mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t",
                                new SearchSuggestionSpec.Builder(/* totalResultCount= */ 10)
                                        .addFilterProperties(
                                                "Type1", ImmutableList.of("propertyone"))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne, resultThree, resultFour);
    }

    // TODO(b/268521214): Move test to cts once deletion propagation is available in framework.
    @Test
    public void testGetSchema_joinableValueType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_SET_DELETION_PROPAGATION));
        AppSearchSchema inSchema =
                new AppSearchSchema.Builder("Test")
                        .addProperty(
                                new StringPropertyConfig.Builder("normalStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("optionalQualifiedIdStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("requiredQualifiedIdStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        // TODO(b/274157614): Export this to framework when we
                                        //  can access hidden APIs.
                                        // @exportToFramework:startStrip()
                                        .setDeletionPropagation(true)
                                        // @exportToFramework:endStrip()
                                        .build())
                        .build();

        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    // TODO(b/268521214): Move test to cts once deletion propagation is available in framework.
    @Test
    public void testGetSchema_deletionPropagation_unsupported() {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeFalse(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_SET_DELETION_PROPAGATION));
        AppSearchSchema schema =
                new AppSearchSchema.Builder("Test")
                        .addProperty(
                                new StringPropertyConfig.Builder("qualifiedIdDeletionPropagation")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .setDeletionPropagation(true)
                                        .build())
                        .build();
        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(schema).build();
        Exception e =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage())
                .isEqualTo(
                        "Setting deletion propagation is not supported "
                                + "on this AppSearch implementation.");
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testQuery_ResultGroupingLimits_SchemaGroupingSupported() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA));
        // Schema registration
        AppSearchSchema genericSchema =
                new AppSearchSchema.Builder("Generic")
                        .addProperty(
                                new StringPropertyConfig.Builder("foo")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(AppSearchEmail.SCHEMA)
                                .addSchemas(genericSchema)
                                .build())
                .get();

        // Index four documents.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace1", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace2", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));
        AppSearchEmail inEmail5 =
                new AppSearchEmail.Builder("namespace2", "id5")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail5).build()));
        GenericDocument inDoc1 =
                new GenericDocument.Builder<>("namespace3", "id6", "Generic")
                        .setPropertyString("foo", "body")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inDoc1).build()));
        GenericDocument inDoc2 =
                new GenericDocument.Builder<>("namespace3", "id7", "Generic")
                        .setPropertyString("foo", "body")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inDoc2).build()));
        GenericDocument inDoc3 =
                new GenericDocument.Builder<>("namespace4", "id8", "Generic")
                        .setPropertyString("foo", "body")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inDoc3).build()));

        // Query with per package result grouping. Only the last document 'doc3' should be
        // returned.
        SearchResults searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_PACKAGE, /* resultLimit= */ 1)
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3);

        // Query with per namespace result grouping. Only the last document in each namespace should
        // be returned ('doc3', 'doc2', 'email5' and 'email2').
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE,
                                        /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail2);

        // Query with per namespace result grouping. Two of the last documents in each namespace
        // should be returned ('doc3', 'doc2', 'doc1', 'email5', 'email4', 'email2', 'email1')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE,
                                        /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents)
                .containsExactly(inDoc3, inDoc2, inDoc1, inEmail5, inEmail4, inEmail2, inEmail1);

        // Query with per schema result grouping. Only the last document of each schema type should
        // be returned ('doc3', 'email5')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inEmail5);

        // Query with per schema result grouping. Only the last two documents of each schema type
        // should be returned ('doc3', 'doc2', 'email5', 'email4')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail4);

        // Query with per package and per namespace result grouping. Only the last document in each
        // namespace should be returned ('doc3', 'doc2', 'email5' and 'email2').
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail2);

        // Query with per package and per namespace result grouping. Only the last two documents
        // in each namespace should be returned ('doc3', 'doc2', 'doc1', 'email5', 'email4',
        // 'email2', 'email1')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents)
                .containsExactly(inDoc3, inDoc2, inDoc1, inEmail5, inEmail4, inEmail2, inEmail1);

        // Query with per package and per schema type result grouping. Only the last document in
        // each schema type should be returned. ('doc3', 'email5')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_SCHEMA
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inEmail5);

        // Query with per package and per schema type result grouping. Only the last two document in
        // each schema type should be returned. ('doc3', 'doc2', 'email5', 'email4')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_SCHEMA
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail4);

        // Query with per namespace and per schema type result grouping. Only the last document in
        // each namespace should be returned. ('doc3', 'doc2', 'email5' and 'email2').
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_SCHEMA,
                                        /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail2);

        // Query with per namespace and per schema type result grouping. Only the last two documents
        // in each namespace should be returned. ('doc3', 'doc2', 'doc1', 'email5', 'email4',
        // 'email2', 'email1')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_SCHEMA,
                                        /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents)
                .containsExactly(inDoc3, inDoc2, inDoc1, inEmail5, inEmail4, inEmail2, inEmail1);

        // Query with per namespace, per package and per schema type result grouping. Only the last
        // document in each namespace should be returned. ('doc3', 'doc2', 'email5' and 'email2')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_SCHEMA
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 1)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inDoc3, inDoc2, inEmail5, inEmail2);

        // Query with per namespace, per package and per schema type result grouping. Only the last
        // two documents in each namespace should be returned.('doc3', 'doc2', 'doc1', 'email5',
        // 'email4', 'email2', 'email1')
        searchResults =
                mDb1.search(
                        "body",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .setResultGrouping(
                                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                                | SearchSpec.GROUPING_TYPE_PER_SCHEMA
                                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                        /* resultLimit= */ 2)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents)
                .containsExactly(inDoc3, inDoc2, inDoc1, inEmail5, inEmail4, inEmail2, inEmail1);
    }

    // TODO(b/291122592): move to CTS once the APIs it uses are public
    @Test
    public void testQuery_ResultGroupingLimits_SchemaGroupingNotSupported() throws Exception {
        assumeFalse(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA));
        // Schema registration
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build())
                .get();

        // Index four documents.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace1", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace2", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));

        // SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA is not supported.
        // UnsupportedOperationException will be thrown.
        SearchSpec searchSpec1 =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* resultLimit= */ 1)
                        .build();
        UnsupportedOperationException exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.search("body", searchSpec1));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                                + " is not available on this"
                                + " AppSearch implementation.");

        // SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA is not supported.
        // UnsupportedOperationException will be thrown.
        SearchSpec searchSpec2 =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_PACKAGE
                                        | SearchSpec.GROUPING_TYPE_PER_SCHEMA,
                                /* resultLimit= */ 1)
                        .build();
        exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.search("body", searchSpec2));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                                + " is not available on this"
                                + " AppSearch implementation.");

        // SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA is not supported.
        // UnsupportedOperationException will be thrown.
        SearchSpec searchSpec3 =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                        | SearchSpec.GROUPING_TYPE_PER_SCHEMA,
                                /* resultLimit= */ 1)
                        .build();
        exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.search("body", searchSpec3));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                                + " is not available on this"
                                + " AppSearch implementation.");

        // SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA is not supported.
        // UnsupportedOperationException will be thrown.
        SearchSpec searchSpec4 =
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultGrouping(
                                SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                        | SearchSpec.GROUPING_TYPE_PER_SCHEMA
                                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE,
                                /* resultLimit= */ 1)
                        .build();
        exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.search("body", searchSpec4));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                                + " is not available on this"
                                + " AppSearch implementation.");
    }

    @Test
    public void testQuery_typeFilterWithPolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .addSchemas(AppSearchEmail.SCHEMA)
                                .build())
                .get();

        // Index some documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setPropertyString("name", "Foo")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setPropertyString("name", "Foo")
                        .build();
        AppSearchEmail emailDoc =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("Foo")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc, emailDoc)
                                .build()));
        GenericDocument artistDocWithParent = artistDoc.toBuilder().setParentTypes(
                Collections.singletonList("Person")).build();

        // Query for the documents
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
        assertThat(documents).containsExactly(personDoc, artistDocWithParent, emailDoc);

        // Query with a filter for the "Person" type should also include the "Artist" type.
        searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .addFilterSchemas("Person")
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(personDoc, artistDocWithParent);

        // Query with a filters for the "Artist" type should not include the "Person" type.
        searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .addFilterSchemas("Artist")
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(artistDocWithParent);
    }

    @Test
    public void testQuery_projectionWithPolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("company")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .build())
                .get();

        // Index two documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .setPropertyString("emailAddress", "person@gmail.com")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .setPropertyString("company", "Company")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc)
                                .build()));

        // Query with type property paths {"Person", ["name"]}, {"Artist", ["emailAddress"]}
        // This will be expanded to paths {"Person", ["name"]}, {"Artist", ["name", "emailAddress"]}
        // via polymorphism.
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .addProjection("Person", ImmutableList.of("name"))
                                .addProjection("Artist", ImmutableList.of("emailAddress"))
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The person document should have been returned with only the "name" property. The artist
        // document should have been returned with all of its properties.
        GenericDocument expectedPerson =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .build();
        GenericDocument expectedArtist =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setParentTypes(Collections.singletonList("Person"))
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .build();
        assertThat(documents).containsExactly(expectedPerson, expectedArtist);
    }

    @Test
    public void testQuery_indexBasedOnParentTypePolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("company")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema messageSchema =
                new AppSearchSchema.Builder("Message")
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .addSchemas(messageSchema)
                                .build())
                .get();

        // Index some an artistDoc and a messageDoc
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Artist")
                        .setPropertyString("name", "Foo")
                        .setPropertyString("company", "Bar")
                        .build();
        GenericDocument messageDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Message")
                        // sender is defined as a Person, which accepts an Artist because Artist <:
                        // Person.
                        // However, indexing will be based on what's defined in Person, so the
                        // "company"
                        // property in artistDoc cannot be used to search this messageDoc.
                        .setPropertyDocument("sender", artistDoc)
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(artistDoc, messageDoc)
                                .build()));
        GenericDocument expectedArtistDoc = artistDoc.toBuilder().setParentTypes(
                Collections.singletonList("Person")).build();
        GenericDocument expectedMessageDoc = messageDoc.toBuilder().setPropertyDocument("sender",
                expectedArtistDoc).build();

        // Query for the documents
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(expectedArtistDoc, expectedMessageDoc);

        // The "company" property in artistDoc cannot be used to search messageDoc.
        searchResults =
                mDb1.search(
                        "Bar",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(expectedArtistDoc);
    }

    @Test
    public void testQuery_parentTypeListIsTopologicalOrder() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        // Create the following subtype relation graph, where
        // 1. A's direct parents are B and C.
        // 2. B's direct parent is D.
        // 3. C's direct parent is B and D.
        // DFS order from A: [A, B, D, C]. Not acceptable because B and D appear before C.
        // BFS order from A: [A, B, C, D]. Not acceptable because B appears before C.
        // Topological order (all subtypes appear before supertypes) from A: [A, C, B, D].
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("A")
                        .addParentType("B")
                        .addParentType("C")
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("B")
                        .addParentType("D")
                        .build();
        AppSearchSchema schemaC =
                new AppSearchSchema.Builder("C")
                        .addParentType("B")
                        .addParentType("D")
                        .build();
        AppSearchSchema schemaD =
                new AppSearchSchema.Builder("D")
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(schemaA)
                                .addSchemas(schemaB)
                                .addSchemas(schemaC)
                                .addSchemas(schemaD)
                                .build())
                .get();

        // Index some documents
        GenericDocument docA =
                new GenericDocument.Builder<>("namespace", "id1", "A")
                        .build();
        GenericDocument docB =
                new GenericDocument.Builder<>("namespace", "id2", "B")
                        .build();
        GenericDocument docC =
                new GenericDocument.Builder<>("namespace", "id3", "C")
                        .build();
        GenericDocument docD =
                new GenericDocument.Builder<>("namespace", "id4", "D")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(docA, docB, docC, docD)
                                .build()));

        GenericDocument expectedDocA =
                docA.toBuilder().setParentTypes(
                        new ArrayList<>(Arrays.asList("C", "B", "D"))).build();
        GenericDocument expectedDocB =
                docB.toBuilder().setParentTypes(
                        Collections.singletonList("D")).build();
        GenericDocument expectedDocC =
                docC.toBuilder().setParentTypes(
                        new ArrayList<>(Arrays.asList("B", "D"))).build();
        // Query for the documents
        SearchResults searchResults = mDb1.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(4);
        assertThat(documents).containsExactly(expectedDocA, expectedDocB, expectedDocC, docD);
    }
}
