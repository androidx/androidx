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

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_SCHEMA;
import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;
import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;
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
import androidx.appsearch.app.AppSearchSchema.DocumentPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.cts.app.customer.EmailDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.SearchAction;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.collection.ArrayMap;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class AppSearchSessionCtsTestBase {
    static final String DB_NAME_1 = "";
    static final String DB_NAME_2 = "testDb2";

    // Since we cannot call non-public API in the cts test, make a copy of these 2 action types, so
    // we can create taken actions in GenericDocument form.
    private static final int ACTION_TYPE_SEARCH = 1;
    private static final int ACTION_TYPE_CLICK = 2;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor) throws Exception;

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        mDb2 = createSearchSessionAsync(DB_NAME_2).get();

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
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testSetSchema() throws Exception {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
    }

    @Test
    public void testSetSchema_Failure() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .build();

        SetSchemaRequest setSchemaRequest1 =
                new SetSchemaRequest.Builder().addSchemas(emailSchema1).build();
        ExecutionException executionException =
                assertThrows(ExecutionException.class,
                        () -> mDb1.setSchemaAsync(setSchemaRequest1).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Incompatible types: {builtin:Email}");

        SetSchemaRequest setSchemaRequest2 = new SetSchemaRequest.Builder().build();
        executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest2).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Deleted types: {builtin:Email}");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testSetSchema_schemaDescription_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_SET_DESCRIPTION));
        AppSearchSchema schema = new AppSearchSchema.Builder("Email1")
                .setDescription("Unsupported description")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(schema)
                .build();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(request).get());
        assertThat(exception).hasMessageThat().contains(Features.SCHEMA_SET_DESCRIPTION
                + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testSetSchema_propertyDescription_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_SET_DESCRIPTION));
        AppSearchSchema schema = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setDescription("Unsupported description")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(schema)
                .build();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(request).get());
        assertThat(exception).hasMessageThat().contains(Features.SCHEMA_SET_DESCRIPTION
                + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testSetSchema_updateSchemaDescription() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_SET_DESCRIPTION));

        AppSearchSchema schema1 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message.")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("A summary of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("body")
                                        .setDescription("All of the content of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema1).build())
                .get();

        Set<AppSearchSchema> actualSchemaTypes = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actualSchemaTypes).containsExactly(schema1);

        // Change the type description.
        AppSearchSchema schema2 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("Like mail but with an 'a'.")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("A summary of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("body")
                                        .setDescription("All of the content of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema2).build())
                .get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APP_FUNCTIONS)  // setDescription
    public void testSetSchema_updatePropertyDescription() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_SET_DESCRIPTION));

        AppSearchSchema schema1 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message.")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("A summary of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("body")
                                        .setDescription("All of the content of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema1).build())
                .get();

        Set<AppSearchSchema> actualSchemaTypes = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actualSchemaTypes).containsExactly(schema1);

        // Change the type description.
        AppSearchSchema schema2 =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message.")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setDescription("The most important part of the email.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("body")
                                        .setDescription("All the other stuff.")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema2).build())
                .get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema2);
    }

    @Test
    public void testSetSchema_updateVersion() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(1).build()).get();

        Set<AppSearchSchema> actualSchemaTypes = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actualSchemaTypes).containsExactly(schema);

        // increase version number
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(2).build()).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(2);
    }

    @Test
    public void testSetSchema_checkVersion() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // set different version number to different database.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(135).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(246).build()).get();


        // check the version has been set correctly.
        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(135);

        getSchemaResponse = mDb2.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(246);
    }

    @Test
    public void testSetSchema_addIndexedNestedDocumentProperty() throws Exception {
        // Create schema with a nested document type
        // SectionId assignment for 'Person':
        // - "name": string type, indexed. Section id = 0.
        // - "worksFor.name": string type, (nested) indexed. Section id = 1.
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        AppSearchSchema organizationSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(personSchema, organizationSchema)
                        .build()).get();

        // Index documents and verify using getDocuments
        GenericDocument person = new GenericDocument.Builder<>("namespace", "person1", "Person")
                .setPropertyString("name", "John")
                .setPropertyDocument("worksFor",
                        new GenericDocument.Builder<>("namespace", "org1", "Organization")
                                .setPropertyString("name", "Google")
                                .build())
                .build();

        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(person).build()));
        assertThat(putResult.getSuccesses()).containsExactly("person1", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("person1")
                        .build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person);

        // Verify search using property filter
        SearchResults searchResults = mDb1.search("worksFor.name:Google", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person);

        // Change the schema to add another nested document property to 'Person'
        // The added property has 'optional' cardinality, so this change is compatible and indexed
        // documents should still be searchable.
        //
        // New section id assignment for 'Person':
        // - "almaMater.name", string type, (nested) indexed. Section id = 0
        // - "name": string type, indexed. Section id = 1
        // - "worksFor.name": string type, (nested) indexed. Section id = 2
        AppSearchSchema newPersonSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("almaMater", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(newPersonSchema, organizationSchema)
                        .build()).get();
        Set<AppSearchSchema> outSchemaTypes = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(outSchemaTypes).containsExactly(newPersonSchema, organizationSchema);

        getByDocumentIdRequest = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("person1")
                .build();
        outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person);

        // Verify that index rebuild was triggered correctly. The same query "worksFor.name:Google"
        // should still match the same result.
        searchResults = mDb1.search("worksFor.name:Google", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person);

        // In new_schema the 'name' property is now indexed at section id 1. If searching for
        // "name:Google" matched the document, this means that index rebuild was not triggered
        // correctly and Icing is still searching the old index, where 'worksFor.name' was
        // indexed at section id 1.
        searchResults = mDb1.search("name:Google", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).isEmpty();
    }

    @Test
    public void testSetSchemaWithValidCycle_allowCircularReferences() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SET_SCHEMA_CIRCULAR_REFERENCES));

        // Create schema with valid cycle: Person -> Organization -> Person...
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        AppSearchSchema organizationSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(false)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(personSchema, organizationSchema)
                        .build()).get();

        // Test that documents following the circular schema are indexable, and that its sections
        // are searchable
        GenericDocument person = new GenericDocument.Builder<>("namespace", "person1", "Person")
                .setPropertyString("name", "John")
                .setPropertyDocument("worksFor")
                .build();
        GenericDocument org = new GenericDocument.Builder<>("namespace", "org1", "Organization")
                .setPropertyString("name", "Org")
                .setPropertyDocument("funder", person)
                .build();
        GenericDocument person2 = new GenericDocument.Builder<>("namespace", "person2", "Person")
                .setPropertyString("name", "Jane")
                .setPropertyDocument("worksFor", org)
                .build();

        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(person, org,
                                person2).build()));
        assertThat(putResult.getSuccesses()).containsExactly("person1", null, "org1", null,
                "person2", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("person1", "person2", "org1")
                        .build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(3);
        assertThat(outDocuments).containsExactly(person, person2, org);

        // Both org1 and person2 should be returned for query "Org"
        // For org1 this matches the 'name' property and for person2 this matches the
        // 'worksFor.name' property.
        SearchResults searchResults = mDb1.search("Org", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person2, org);
    }

    @Test
    public void testSetSchemaWithInvalidCycle_circularReferencesSupported() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SET_SCHEMA_CIRCULAR_REFERENCES));

        // Create schema with invalid cycle: Person -> Organization -> Person... where all
        // DocumentPropertyConfigs have setShouldIndexNestedProperties(true).
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        AppSearchSchema organizationSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder().addSchemas(personSchema, organizationSchema).build();
        ExecutionException executionException =
                assertThrows(ExecutionException.class,
                        () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().containsMatch("Invalid cycle|Infinite loop");
    }

    @Test
    public void testSetSchemaWithValidCycle_circularReferencesNotSupported() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SET_SCHEMA_CIRCULAR_REFERENCES));

        // Create schema with valid cycle: Person -> Organization -> Person...
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("worksFor", "Organization")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        AppSearchSchema organizationSchema = new AppSearchSchema.Builder("Organization")
                .addProperty(new StringPropertyConfig.Builder("name")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(false)
                        .build())
                .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder().addSchemas(personSchema, organizationSchema).build();
        ExecutionException executionException =
                assertThrows(ExecutionException.class,
                        () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().containsMatch("Invalid cycle|Infinite loop");
    }

// @exportToFramework:startStrip()

    @Test
    public void testSetSchema_addDocumentClasses() throws Exception {
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();
    }
// @exportToFramework:endStrip()

    /** Test indexing maximum properties into a schema. */
    @Test
    public void testSetSchema_maxProperties() throws Exception {
        int maxProperties = mDb1.getFeatures().getMaxIndexedProperties();
        AppSearchSchema.Builder schemaBuilder = new AppSearchSchema.Builder("testSchema");
        for (int i = 0; i < maxProperties; i++) {
            schemaBuilder.addProperty(new StringPropertyConfig.Builder("string" + i)
                    .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build());
        }
        AppSearchSchema maxSchema = schemaBuilder.build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(maxSchema).build()).get();
        Set<AppSearchSchema> actual1 = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual1).containsExactly(maxSchema);

        schemaBuilder.addProperty(new StringPropertyConfig.Builder("toomuch")
                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                .build());
        ExecutionException exception = assertThrows(ExecutionException.class, () ->
                mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                        .addSchemas(schemaBuilder.build()).setForceOverride(true).build()).get());
        Throwable cause = exception.getCause();
        assertThat(cause).isInstanceOf(AppSearchException.class);
        assertThat(cause.getMessage()).isEqualTo("Too many properties to be indexed, max "
                + "number of properties allowed: " + maxProperties);
    }

    @Test
    public void testSetSchema_maxProperties_nestedSchemas() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SET_SCHEMA_CIRCULAR_REFERENCES));

        int maxProperties = mDb1.getFeatures().getMaxIndexedProperties();
        AppSearchSchema.Builder personSchemaBuilder = new AppSearchSchema.Builder("Person");
        for (int i = 0; i < maxProperties / 3 + 1; i++) {
            personSchemaBuilder.addProperty(new StringPropertyConfig.Builder("string" + i)
                    .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build());
        }
        personSchemaBuilder.addProperty(new DocumentPropertyConfig.Builder("worksFor",
                "Organization")
                .setShouldIndexNestedProperties(false)
                .build());
        personSchemaBuilder.addProperty(new DocumentPropertyConfig.Builder("address", "Address")
                .setShouldIndexNestedProperties(true)
                .build());

        AppSearchSchema.Builder orgSchemaBuilder = new AppSearchSchema.Builder("Organization");
        for (int i = 0; i < maxProperties / 3; i++) {
            orgSchemaBuilder.addProperty(new StringPropertyConfig.Builder("string" + i)
                    .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build());
        }
        orgSchemaBuilder.addProperty(new DocumentPropertyConfig.Builder("funder", "Person")
                .setShouldIndexNestedProperties(true)
                .build());

        AppSearchSchema.Builder addressSchemaBuilder = new AppSearchSchema.Builder("Address");
        for (int i = 0; i < maxProperties / 3; i++) {
            addressSchemaBuilder.addProperty(new StringPropertyConfig.Builder("string" + i)
                    .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build());
        }

        AppSearchSchema personSchema = personSchemaBuilder.build();
        AppSearchSchema orgSchema = orgSchemaBuilder.build();
        AppSearchSchema addressSchema = addressSchemaBuilder.build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(personSchema, orgSchema, addressSchema)
                        .build()).get();
        Set<AppSearchSchema> schemas = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(schemas).containsExactly(personSchema, orgSchema, addressSchema);

        // Add one more property to bring the number of sections over the max limit
        personSchemaBuilder.addProperty(new StringPropertyConfig.Builder("toomuch")
                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                .build());
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchemaBuilder.build(), orgSchema, addressSchema)
                                .setForceOverride(true)
                                .build()
                ).get());
        Throwable cause = exception.getCause();
        assertThat(cause).isInstanceOf(AppSearchException.class);
        assertThat(cause.getMessage()).contains("Too many properties to be indexed");
    }

// @exportToFramework:startStrip()

    @Test
    public void testGetSchema() throws Exception {
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema emailSchema2 = new AppSearchSchema.Builder("Email2")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Diff
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Diff
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SetSchemaRequest request1 = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema1).addDocumentClasses(EmailDocument.class).build();
        SetSchemaRequest request2 = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema2).addDocumentClasses(EmailDocument.class).build();

        mDb1.setSchemaAsync(request1).get();
        mDb2.setSchemaAsync(request2).get();

        Set<AppSearchSchema> actual1 = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual1).hasSize(2);
        assertThat(actual1).isEqualTo(request1.getSchemas());
        Set<AppSearchSchema> actual2 = mDb2.getSchemaAsync().get().getSchemas();
        assertThat(actual2).hasSize(2);
        assertThat(actual2).isEqualTo(request2.getSchemas());
    }
// @exportToFramework:endStrip()

    @Test
    public void testGetSchema_allPropertyTypes() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("string")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        // Add it to AppSearch and then obtain it again
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(inSchema, AppSearchEmail.SCHEMA).build()).get();
        GetSchemaResponse response = mDb1.getSchemaAsync().get();
        List<AppSearchSchema> schemas = new ArrayList<>(response.getSchemas());
        assertThat(schemas).containsExactly(inSchema, AppSearchEmail.SCHEMA);
        AppSearchSchema outSchema;
        if (schemas.get(0).getSchemaType().equals("Test")) {
            outSchema = schemas.get(0);
        } else {
            outSchema = schemas.get(1);
        }
        assertThat(outSchema.getSchemaType()).isEqualTo("Test");
        assertThat(outSchema).isNotSameInstanceAs(inSchema);

        List<PropertyConfig> properties = outSchema.getProperties();
        assertThat(properties).hasSize(6);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((StringPropertyConfig) properties.get(0)).getTokenizerType())
                .isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_PLAIN);

        assertThat(properties.get(1).getName()).isEqualTo("long");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(1)).isInstanceOf(AppSearchSchema.LongPropertyConfig.class);

        assertThat(properties.get(2).getName()).isEqualTo("double");
        assertThat(properties.get(2).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(properties.get(2)).isInstanceOf(AppSearchSchema.DoublePropertyConfig.class);

        assertThat(properties.get(3).getName()).isEqualTo("boolean");
        assertThat(properties.get(3).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(properties.get(3)).isInstanceOf(AppSearchSchema.BooleanPropertyConfig.class);

        assertThat(properties.get(4).getName()).isEqualTo("bytes");
        assertThat(properties.get(4).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(4)).isInstanceOf(AppSearchSchema.BytesPropertyConfig.class);

        assertThat(properties.get(5).getName()).isEqualTo("document");
        assertThat(properties.get(5).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5))
                .shouldIndexNestedProperties()).isEqualTo(true);
    }

    @Test
    public void testGetSchema_visibilitySetting() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        byte[] shar256Cert1 = new byte[32];
        Arrays.fill(shar256Cert1, (byte) 1);
        byte[] shar256Cert2 = new byte[32];
        Arrays.fill(shar256Cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("pkgFoo", shar256Cert1);
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("pkgBar", shar256Cert2);
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier1)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier2)
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR))
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                .build();

        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        Set<AppSearchSchema> actual = getSchemaResponse.getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).isEqualTo(request.getSchemas());
        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1");
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages())
                .containsExactly("Email1", ImmutableSet.of(
                        packageIdentifier1, packageIdentifier2));
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility())
                .containsExactly("Email1", ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA)));
    }

    @Test
    public void testGetSchema_visibilitySetting_oneSharedSchema() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));

        AppSearchSchema noteSchema = new AppSearchSchema.Builder("Note")
                .addProperty(new StringPropertyConfig.Builder("subject").build()).build();
        SetSchemaRequest.Builder requestBuilder = new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA, noteSchema)
                .setSchemaTypeDisplayedBySystem(noteSchema.getSchemaType(), false)
                .setSchemaTypeVisibilityForPackage(
                        noteSchema.getSchemaType(),
                        true,
                        new PackageIdentifier("com.some.package1", new byte[32]))
                .addRequiredPermissionsForSchemaTypeVisibility(
                        noteSchema.getSchemaType(),
                        Collections.singleton(SetSchemaRequest.READ_SMS));
        if (mDb1.getFeatures().isFeatureSupported(
                Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE)) {
            requestBuilder.setPubliclyVisibleSchema(
                    noteSchema.getSchemaType(),
                    new PackageIdentifier("com.some.package2", new byte[32]));
        }
        SetSchemaRequest request = requestBuilder.build();
        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        Set<AppSearchSchema> actual = getSchemaResponse.getSchemas();
        assertThat(actual).hasSize(2);
        assertThat(actual).isEqualTo(request.getSchemas());

        // Check visibility settings. Schemas without settings shouldn't appear in the result at
        // all, even with empty maps as values.
        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem())
                .containsExactly(noteSchema.getSchemaType());
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages())
                .containsExactly(
                        noteSchema.getSchemaType(),
                        ImmutableSet.of(new PackageIdentifier("com.some.package1", new byte[32])));
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility())
                .containsExactly(
                        noteSchema.getSchemaType(),
                        ImmutableSet.of(ImmutableSet.of(SetSchemaRequest.READ_SMS)));
        if (mDb1.getFeatures().isFeatureSupported(
                Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE)) {
            assertThat(getSchemaResponse.getPubliclyVisibleSchemas())
                    .containsExactly(
                            noteSchema.getSchemaType(),
                            new PackageIdentifier("com.some.package2", new byte[32]));
        }
    }

    @Test
    public void testGetSchema_visibilitySetting_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        byte[] shar256Cert1 = new byte[32];
        Arrays.fill(shar256Cert1, (byte) 1);
        byte[] shar256Cert2 = new byte[32];
        Arrays.fill(shar256Cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("pkgFoo", shar256Cert1);
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("pkgBar", shar256Cert2);
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier1)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier2)
                .build();

        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        Set<AppSearchSchema> actual = getSchemaResponse.getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).isEqualTo(request.getSchemas());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility());
    }

    @Test
    public void testSetSchema_visibilitySettingPermission_notSupported() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1").build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_SMS))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
    }

    @Test
    public void testSetSchema_publiclyVisible() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE));

        PackageIdentifier pkg = new PackageIdentifier(mContext.getPackageName(), new byte[32]);
        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA)
                .setPubliclyVisibleSchema("builtin:Email", pkg).build();

        mDb1.setSchemaAsync(request).get();
        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();

        assertThat(getSchemaResponse.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);
        assertThat(getSchemaResponse.getPubliclyVisibleSchemas())
                .isEqualTo(ImmutableMap.of("builtin:Email", pkg));

        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setSubject("testPut example").build();

        // mDb1 and mDb2 are in the same package, so we can't REALLY test out public acl. But we
        // can make sure they their own documents under the Public ACL.
        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(putResult.getSuccesses()).containsExactly("id1", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("id1")
                        .build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(email);
    }

    @Test
    public void testSetSchema_publiclyVisible_unsupported() {
        assumeFalse(mDb1.getFeatures()
                .isFeatureSupported(Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE));

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("Email").build())
                .setPubliclyVisibleSchema("Email",
                        new PackageIdentifier(mContext.getPackageName(), new byte[32])).build();
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo("Publicly visible schema are not supported on this "
                + "AppSearch implementation.");
    }

    @Test
    public void testSetSchema_visibleToConfig() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG));
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        PackageIdentifier pkg1 = new PackageIdentifier("package1", cert1);
        PackageIdentifier pkg2 = new PackageIdentifier("package2", cert2);
        SchemaVisibilityConfig config1 = new SchemaVisibilityConfig.Builder()
                .setPubliclyVisibleTargetPackage(pkg1)
                .addRequiredPermissions(ImmutableSet.of(1, 2)).build();
        SchemaVisibilityConfig config2 = new SchemaVisibilityConfig.Builder()
                .setPubliclyVisibleTargetPackage(pkg2)
                .addRequiredPermissions(ImmutableSet.of(3, 4)).build();
        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA)
                .addSchemaTypeVisibleToConfig("builtin:Email", config1)
                .addSchemaTypeVisibleToConfig("builtin:Email", config2)
                .build();
        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(AppSearchEmail.SCHEMA);
        assertThat(getSchemaResponse.getSchemaTypesVisibleToConfigs())
                .isEqualTo(ImmutableMap.of("builtin:Email", ImmutableSet.of(config1, config2)));
    }

    @Test
    public void testSetSchema_visibleToConfig_unsupported() {
        assumeFalse(mDb1.getFeatures()
                .isFeatureSupported(Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG));

        SchemaVisibilityConfig config = new SchemaVisibilityConfig.Builder()
                .addRequiredPermissions(ImmutableSet.of(1, 2)).build();
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("Email").build())
                .addSchemaTypeVisibleToConfig("Email", config).build();
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo("Schema visible to config are not supported on"
                + " this AppSearch implementation.");
    }

    @Test
    public void testGetSchema_longPropertyIndexingType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("indexableLong")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_longPropertyIndexingTypeNone_succeeds() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_longPropertyIndexingTypeRange_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("indexableLong")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo("LongProperty.INDEXING_TYPE_RANGE is not "
                + "supported on this AppSearch implementation.");
    }

    @Test
    public void testGetSchema_joinableValueType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("normalStr")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("optionalQualifiedIdStr")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredQualifiedIdStr")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_joinableValueTypeNone_succeeds() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("optionalString")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredString")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("repeatedString")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_joinableValueTypeQualifiedId_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo(
                "StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID is not supported on this "
                        + "AppSearch implementation.");
    }

    @Test
    public void testGetNamespaces() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespacesAsync().get()).isEmpty();

        // Index a document
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1");

        // Index additional data
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(
                        new AppSearchEmail.Builder("namespace2", "id1").build(),
                        new AppSearchEmail.Builder("namespace2", "id2").build(),
                        new AppSearchEmail.Builder("namespace3", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id2 -- namespace2 should still exist because of namespace2/id1
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id2").build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id1 -- namespace2 should now be gone
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id1").build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1", "namespace3");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1", "namespace3");
    }

    @Test
    public void testGetNamespaces_dbIsolation() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespacesAsync().get()).isEmpty();
        assertThat(mDb2.getNamespacesAsync().get()).isEmpty();

        // Index documents
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace2_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb2.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace_db2", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get())
                .containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespacesAsync().get()).containsExactly("namespace_db2");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        assertThat(mDb1.getNamespacesAsync().get())
                .containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespacesAsync().get()).containsExactly("namespace_db2");
    }

    @Test
    public void testGetSchema_emptyDB() throws Exception {
        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getVersion()).isEqualTo(0);
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_emptyProperties() throws Exception {
        // Schema registration. Due to b/204677124 is fixed in Android T. We have different
        // behaviour when set empty array to bytes and documents between local and platform storage.
        // This test only test String, long, boolean and double, for byte array and Document will be
        // test in backend's specific test.
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new StringPropertyConfig.Builder("string")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schema, AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "testSchema")
                .setPropertyBoolean("boolean")
                .setPropertyString("string")
                .setPropertyDouble("double")
                .setPropertyLong("long")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1")
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);
        assertThat(outDocuments).hasSize(1);
        GenericDocument outDocument = outDocuments.get(0);
        assertThat(outDocument.getPropertyBooleanArray("boolean")).isEmpty();
        assertThat(outDocument.getPropertyStringArray("string")).isEmpty();
        assertThat(outDocument.getPropertyDoubleArray("double")).isEmpty();
        assertThat(outDocument.getPropertyLongArray("long")).isEmpty();
    }

    @Test
    public void testPutLargeDocumentBatch() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Creates a large batch of Documents, since we have max document size in Framework which is
        // 512KiB, we will create 1KiB * 4000 docs = 4MiB total size > 1MiB binder transaction limit
        char[] chars = new char[1024];  // 1KiB
        Arrays.fill(chars, ' ');
        String body = String.valueOf(chars) + "the end.";
        List<GenericDocument> inDocuments = new ArrayList<>();
        GetByDocumentIdRequest.Builder getByDocumentIdRequestBuilder =
                new GetByDocumentIdRequest.Builder("namespace");
        for (int i = 0; i < 4000; i++) {
            GenericDocument inDocument = new GenericDocument.Builder<>(
                    "namespace", "id" + i, "Type")
                    .setPropertyString("body", body)
                    .build();
            inDocuments.add(inDocument);
            getByDocumentIdRequestBuilder.addIds("id" + i);
        }

        // Index documents.
        AppSearchBatchResult<String, Void> result =
                mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(inDocuments)
                        .build()).get();
        assertThat(result.isSuccess()).isTrue();

        // Query those documents and verify they are same with the input. This also verify
        // AppSearchResult could handle large batch.
        SearchResults searchResults = mDb1.search("end", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(4000)
                .build());
        List<GenericDocument> outDocuments = convertSearchResultsToDocuments(searchResults);

        // Create a map to assert the output is same to the input in O(n).
        // containsExactlyElementsIn will create two iterators and the complexity is O(n^2).
        Map<String, GenericDocument> outMap = new ArrayMap<>(outDocuments.size());
        for (int i = 0; i < outDocuments.size(); i++) {
            outMap.put(outDocuments.get(i).getId(), outDocuments.get(i));
        }
        for (int i = 0; i < inDocuments.size(); i++) {
            GenericDocument inDocument = inDocuments.get(i);
            assertThat(inDocument).isEqualTo(outMap.get(inDocument.getId()));
            outMap.remove(inDocument.getId());
        }
        assertThat(outMap).isEmpty();

        // Get by document ID and verify they are same with the input. This also verify
        // AppSearchBatchResult could handle large batch.
        AppSearchBatchResult<String, GenericDocument> batchResult = mDb1.getByDocumentIdAsync(
                getByDocumentIdRequestBuilder.build()).get();
        assertThat(batchResult.isSuccess()).isTrue();
        for (int i = 0; i < inDocuments.size(); i++) {
            GenericDocument inDocument = inDocuments.get(i);
            assertThat(batchResult.getSuccesses().get(inDocument.getId())).isEqualTo(inDocument);
        }
    }

// @exportToFramework:startStrip()

    @Test
    public void testPut_addDocumentClasses() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument email = new EmailDocument();
        email.namespace = "namespace";
        email.id = "id1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_takenActions() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // Schema registration
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addDocumentClasses(SearchAction.class, ClickAction.class)
                                .build())
                .get();

        // Put a SearchAction and ClickAction document
        SearchAction searchAction =
                new SearchAction.Builder("namespace", "search", /* actionTimestampMillis= */1000)
                        .setDocumentTtlMillis(0)
                        .setQuery("query")
                        .setFetchedResultCount(10)
                        .build();
        ClickAction clickAction =
                new ClickAction.Builder("namespace", "click", /* actionTimestampMillis= */2000)
                        .setDocumentTtlMillis(0)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .setTimeStayOnResultMillis(1024)
                        .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addTakenActions(searchAction, clickAction)
                        .build()));
        assertThat(result.getSuccesses()).containsEntry("search", null);
        assertThat(result.getSuccesses()).containsEntry("click", null);
        assertThat(result.getFailures()).isEmpty();
    }
// @exportToFramework:endStrip()

    @Test
    public void testPutDocuments_takenActionGenericDocuments() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // Schema registration
        AppSearchSchema searchActionSchema = new AppSearchSchema.Builder("builtin:SearchAction")
                .addProperty(new LongPropertyConfig.Builder("actionType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("query")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema clickActionSchema = new AppSearchSchema.Builder("builtin:ClickAction")
                .addProperty(new LongPropertyConfig.Builder("actionType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("query")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("referencedQualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(searchActionSchema, clickActionSchema)
                        .build()).get();

        // Put search action and click action generic documents.
        GenericDocument searchAction =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setPropertyLong("actionType", ACTION_TYPE_SEARCH)
                        .setPropertyString("query", "body")
                        .build();
        GenericDocument clickAction =
                new GenericDocument.Builder<>("namespace", "click", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setPropertyLong("actionType", ACTION_TYPE_CLICK)
                        .setPropertyString("query", "body")
                        .setPropertyString("referencedQualifiedId", "pkg$db/ns#refId")
                        .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addTakenActionGenericDocuments(searchAction, clickAction)
                        .build()));
        assertThat(result.getSuccesses()).containsEntry("search", null);
        assertThat(result.getSuccesses()).containsEntry("click", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testUpdateSchema() throws Exception {
        // Schema registration
        AppSearchSchema oldEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema newEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(oldEmailSchema).build()).get();

        // Try to index a gift. This should fail as it's not in the schema.
        GenericDocument gift =
                new GenericDocument.Builder<>("namespace", "gift1", "Gift").setPropertyLong("price",
                        5).build();
        AppSearchBatchResult<String, Void> result =
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()).get();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailures().get("gift1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Update the schema to include the gift and update email with a new field
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(newEmailSchema, giftSchema).build()).get();

        // Try to index the document again, which should now work
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()));

        // Indexing an email with a body should also work
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
    }

    @Test
    public void testRemoveSchema() throws Exception {
        // Schema registration
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present.
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, "namespace", "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);

        // Try to remove the email schema. This should fail as it's an incompatible change.
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder().build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException failResult1 = (AppSearchException) executionException.getCause();
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("email1")
                        .build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace", "email2")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email2").getErrorMessage())
                .isEqualTo("Schema type config '" + mContext.getPackageName() + "$" + DB_NAME_1
                        + "/builtin:Email' not found");
    }

    @Test
    public void testRemoveSchema_twoDatabases() throws Exception {
        // Schema registration in mDb1 and mDb2
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present in database1.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, "namespace", "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email1);

        // Index an email and check it present in database2.
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace", "email2")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb2.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Try to remove the email schema in database1. This should fail as it's an incompatible
        // change.
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder().build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException failResult1 = (AppSearchException) executionException.getCause();
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone in database 1.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("email1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace", "email3")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email3).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email3").getErrorMessage())
                .isEqualTo("Schema type config '" + mContext.getPackageName() + "$" + DB_NAME_1
                        + "/builtin:Email' not found");

        // Make sure email in database 2 still present.
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Make sure email could still be indexed in database 2.
        checkIsBatchResultSuccess(
                mDb2.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
    }

    @Test
    public void testGetDocuments() throws Exception {
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

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, "namespace", "id1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()).get();
        assertThat(failResult.isSuccess()).isFalse();
        assertThat(failResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

// @exportToFramework:startStrip()

    @Test
    public void testGet_addDocumentClasses() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument inEmail = new EmailDocument();
        inEmail.namespace = "namespace";
        inEmail.id = "id1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, "namespace", "id1");
        assertThat(outDocuments).hasSize(1);
        EmailDocument outEmail = outDocuments.get(0).toDocumentClass(EmailDocument.class);
        assertThat(inEmail.id).isEqualTo(outEmail.id);
        assertThat(inEmail.subject).isEqualTo(outEmail.subject);
        assertThat(inEmail.body).isEqualTo(outEmail.body);
    }
// @exportToFramework:endStrip()


    @Test
    public void testGetDocuments_projection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection(
                        AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

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
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_projectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "id1",
                "id2").addProjection(AppSearchEmail.SCHEMA_TYPE, Collections.emptyList()).build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned without any properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_projectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

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
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection(
                        GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

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
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "id1",
                "id2").addProjection(GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                Collections.emptyList()).build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned without any properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(
                        GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

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
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testQuery() throws Exception {
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
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);

        // Multi-term query
        searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_getNextPage() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        Set<AppSearchEmail> emailSet = new HashSet<>();
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
            emailSet.add(inEmail);
            putDocumentsRequestBuilder.addGenericDocuments(inEmail);
        }
        checkIsBatchResultSuccess(mDb1.putAsync(putDocumentsRequestBuilder.build()));

        // Set number of results per page is 7.
        SearchResults searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultCountPerPage(7)
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
        assertThat(documents).containsExactlyElementsIn(emailSet);
        assertThat(pageNumber).isEqualTo(6); // 5 (upper(31/7)) + 1 (final empty page)
    }

    @Test
    public void testQueryIndexableLongProperty_numericSearchEnabledSucceeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));

        // Schema registration
        AppSearchSchema transactionSchema = new AppSearchSchema.Builder("transaction")
                .addProperty(new LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("cost")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(transactionSchema).build()).get();

        // Index some documents
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "transaction")
                        .setPropertyLong("price", 10)
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "transaction")
                        .setPropertyLong("price", 25)
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc3 =
                new GenericDocument.Builder<>("namespace", "id3", "transaction")
                        .setPropertyLong("cost", 2)
                        .setCreationTimestampMillis(1000)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("price < 20",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc1);

        searchResults = mDb1.search("price == 25",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc2);

        searchResults = mDb1.search("cost > 2",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        searchResults = mDb1.search("cost >= 2",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc3);

        searchResults = mDb1.search("price <= 25",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0)).isEqualTo(doc2);
        assertThat(documents.get(1)).isEqualTo(doc1);
    }

    @Test
    public void testQueryIndexableLongProperty_numericSearchNotEnabled() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));

        // Schema registration
        AppSearchSchema transactionSchema = new AppSearchSchema.Builder("transaction")
                .addProperty(new LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(transactionSchema).build()).get();

        // Index some documents
        GenericDocument doc =
                new GenericDocument.Builder<>("namespace", "id1", "transaction")
                        .setPropertyLong("price", 10)
                        .setCreationTimestampMillis(1000)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));

        // Query for the document
        // Use advanced query but disable NUMERIC_SEARCH in the SearchSpec.
        SearchResults searchResults = mDb1.search("price < 20",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(false)
                        .build());

        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.NUMERIC_SEARCH);
    }

    @Test
    public void testQuery_relevanceScoring() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("Mary had a little lamb")
                        .setBody("A little lamb, little lamb")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("I'm a little teapot")
                        .setBody("short and stout. Here is my handle, here is my spout.")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "little". It should match both emails.
        SearchResults searchResults = mDb1.search("little", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // The email1 should be ranked higher because 'little' appears three times in email1 and
        // only once in email2.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(1).getRankingSignal()).isGreaterThan(0);

        // Query for "little OR stout". It should match both emails.
        searchResults = mDb1.search("little OR stout", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build());
        results = retrieveAllSearchResults(searchResults);

        // The email2 should be ranked higher because 'little' appears once and "stout", which is a
        // rarer term, appears once. email1 only has the three 'little' appearances.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getRankingSignal()).isGreaterThan(0);
    }

    @Test
    public void testQuery_advancedRanking() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(3)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document, and set an advanced ranking expression that evaluates to 6.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                // "abs(pow(2, 2) - 6)" should be evaluated to 2.
                // "this.documentScore()" should be evaluated to 3.
                .setRankingStrategy("abs(pow(2, 2) - 6) * this.documentScore()")
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(inEmail);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(6);
    }

    @Test
    public void testQuery_advancedRankingWithPropertyWeights() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("test from")
                        .setTo("test to")
                        .setSubject("subject")
                        .setBody("test body")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document, and set an advanced ranking expression that evaluates to 0.7.
        SearchResults searchResults = mDb1.search("test", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE,
                        ImmutableMap.of("from", 0.1, "to", 0.2,
                                "subject", 2.0, "body", 0.4))
                // this.propertyWeights() returns normalized property weights, in which each
                // weight is divided by the maximum weight.
                // As a result, this expression will evaluates to the list {0.1 / 2.0, 0.2 / 2.0,
                // 0.4 / 2.0}, since the matched properties are "from", "to" and "body", and the
                // maximum weight provided is 2.0.
                // Thus, sum(this.propertyWeights()) will be evaluated to 0.05 + 0.1 + 0.2 = 0.35.
                .setRankingStrategy("sum(this.propertyWeights())")
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(inEmail);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(0.35);
    }

    @Test
    public void testQuery_advancedRankingWithJoin() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        String qualifiedId = DocumentIdUtil.createQualifiedId(mContext.getPackageName(), DB_NAME_1,
                "namespace", "id1");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id2", "ViewAction")
                .setScore(1)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Monday").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setScore(2)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Tuesday").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, viewAction1,
                        viewAction2).build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy("2 * this.documentScore()")
                        .setOrder(SearchSpec.ORDER_ASCENDING)
                        .build();

        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("", nestedSearchSpec)
                .build();

        SearchResults searchResults = mDb1.search("body email", new SearchSpec.Builder()
                // this.childrenRankingSignals() evaluates to the list {1 * 2, 2 * 2}.
                // Thus, sum(this.childrenRankingSignals()) evaluates to 6.
                .setRankingStrategy("sum(this.childrenRankingSignals())")
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        assertThat(sr).hasSize(1);
        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(0).getJoinedResults()).hasSize(2);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction1);
        assertThat(sr.get(0).getJoinedResults().get(1).getGenericDocument()).isEqualTo(viewAction2);
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(6.0);
    }

// @exportToFramework:startStrip()
    @Test
    public void testQueryRankByClickActions_useTakenAction() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // Schema registration
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(AppSearchEmail.SCHEMA)
                                .addDocumentClasses(SearchAction.class, ClickAction.class)
                                .build())
                .get();

        // Index several email documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "email1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "email2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        String qualifiedId1 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, inEmail1);
        String qualifiedId2 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, inEmail2);

        SearchAction searchAction =
                new SearchAction.Builder("namespace", "search1", /* actionTimestampMillis= */1000)
                        .setDocumentTtlMillis(0)
                        .setQuery("body")
                        .setFetchedResultCount(20)
                        .build();
        ClickAction clickAction1 =
                new ClickAction.Builder("namespace", "click1", /* actionTimestampMillis= */2000)
                        .setDocumentTtlMillis(0)
                        .setQuery("body")
                        .setReferencedQualifiedId(qualifiedId1)
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(1)
                        .setTimeStayOnResultMillis(512)
                        .build();
        ClickAction clickAction2 =
                new ClickAction.Builder("namespace", "click2", /* actionTimestampMillis= */3000)
                        .setDocumentTtlMillis(0)
                        .setQuery("body")
                        .setReferencedQualifiedId(qualifiedId2)
                        .setResultRankInBlock(2)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(128)
                        .build();
        ClickAction clickAction3 =
                new ClickAction.Builder("namespace", "click3", /* actionTimestampMillis= */4000)
                        .setDocumentTtlMillis(0)
                        .setQuery("body")
                        .setReferencedQualifiedId(qualifiedId1)
                        .setResultRankInBlock(2)
                        .setResultRankGlobal(2)
                        .setTimeStayOnResultMillis(256)
                        .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(inEmail1, inEmail2)
                        .addTakenActions(searchAction, clickAction1, clickAction2, clickAction3)
                        .build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_DESCENDING)
                        .addFilterDocumentClasses(ClickAction.class)
                        .build();

        // Note: SearchSpec.Builder#setMaxJoinedResultCount only limits the number of child
        // documents returned. It does not affect the number of child documents that are scored.
        JoinSpec js = new JoinSpec.Builder("referencedQualifiedId")
                .setNestedSearch("query:body", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(0)
                .build();

        // Search "body" for AppSearchEmail documents, ranking by ClickAction signals with
        // query = "body".
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        assertThat(sr).hasSize(2);
        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("email1");
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(2.0);
        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("email2");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(1.0);
    }
// @exportToFramework:endStrip()

    @Test
    public void testQueryRankByTakenActions_useTakenActionGenericDocument() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        AppSearchSchema searchActionSchema = new AppSearchSchema.Builder("builtin:SearchAction")
                .addProperty(new LongPropertyConfig.Builder("actionType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("query")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema clickActionSchema = new AppSearchSchema.Builder("builtin:ClickAction")
                .addProperty(new LongPropertyConfig.Builder("actionType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("query")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("referencedQualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA, searchActionSchema, clickActionSchema)
                        .build())
                .get();

        // Index several email documents
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "email1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "email2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        String qualifiedId1 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, inEmail1);
        String qualifiedId2 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, inEmail2);

        GenericDocument searchAction =
                new GenericDocument.Builder<>("namespace", "search1", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setPropertyLong("actionType", ACTION_TYPE_SEARCH)
                        .setPropertyString("query", "body")
                        .build();
        GenericDocument clickAction1 =
                new GenericDocument.Builder<>("namespace", "click1", "builtin:ClickAction")
                        .setCreationTimestampMillis(2000)
                        .setPropertyLong("actionType", ACTION_TYPE_CLICK)
                        .setPropertyString("query", "body")
                        .setPropertyString("referencedQualifiedId", qualifiedId1)
                        .build();
        GenericDocument clickAction2 =
                new GenericDocument.Builder<>("namespace", "click2", "builtin:ClickAction")
                        .setCreationTimestampMillis(3000)
                        .setPropertyLong("actionType", ACTION_TYPE_CLICK)
                        .setPropertyString("query", "body")
                        .setPropertyString("referencedQualifiedId", qualifiedId2)
                        .build();
        GenericDocument clickAction3 =
                new GenericDocument.Builder<>("namespace", "click3", "builtin:ClickAction")
                        .setCreationTimestampMillis(4000)
                        .setPropertyLong("actionType", ACTION_TYPE_CLICK)
                        .setPropertyString("query", "body")
                        .setPropertyString("referencedQualifiedId", qualifiedId1)
                        .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(inEmail1, inEmail2)
                        .addTakenActionGenericDocuments(
                                searchAction, clickAction1, clickAction2, clickAction3)
                        .build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_DESCENDING)
                        .addFilterSchemas("builtin:ClickAction")
                        .build();

        // Note: SearchSpec.Builder#setMaxJoinedResultCount only limits the number of child
        // documents returned. It does not affect the number of child documents that are scored.
        JoinSpec js = new JoinSpec.Builder("referencedQualifiedId")
                .setNestedSearch("query:body", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(0)
                .build();

        // Search "body" for AppSearchEmail documents, ranking by ClickAction signals with
        // query = "body".
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        assertThat(sr).hasSize(2);
        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("email1");
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(2.0);
        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("email2");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(1.0);
    }

    @Test
    public void testQuery_invalidAdvancedRanking() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

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

        // Query for the document, but set an invalid advanced ranking expression.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy("sqrt()")
                .build());
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains(
                "Math functions must have at least one argument.");
    }

    @Test
    public void testQuery_invalidAdvancedRankingWithChildrenRankingSignals() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

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

        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                // Using this.childrenRankingSignals() without the context of a join is invalid.
                .setRankingStrategy("sum(this.childrenRankingSignals())")
                .build());
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains(
                "childrenRankingSignals must only be used with join");
    }

    @Test
    public void testQuery_unsupportedAdvancedRanking() throws Exception {
        // Assume that advanced ranking has not been supported.
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

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

        // Query for the document, and set a valid advanced ranking expression.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy("sqrt(4)")
                .build();
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.search("body", searchSpec));
        assertThat(e).hasMessageThat().contains(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION + " is not available on this "
                        + "AppSearch implementation.");
    }

    @Test
    public void testQuery_typeFilter() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(genericSchema)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument inDoc = new GenericDocument.Builder<>("namespace", "id2", "Generic")
                .setPropertyString("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inDoc).build()));

        // Query for the documents
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(inEmail, inDoc);

        // Query only for Document
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .addFilterSchemas("Generic", "Generic") // duplicate type in filter won't matter.
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inDoc);

        // Query only for non-existent type
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .addFilterSchemas("nonExistType")
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Query for the document within our package
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterPackageNames(ApplicationProvider.getApplicationContext().getPackageName())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(email);

        // Query for the document in some other package, which won't exist
        searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterPackageNames("some.other.package")
                .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).isEmpty();
    }

    @Test
    public void testQuery_namespaceFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("expectedNamespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail unexpectedEmail =
                new AppSearchEmail.Builder("unexpectedNamespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(expectedEmail, unexpectedEmail).build()));

        // Query for all namespaces
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(expectedEmail, unexpectedEmail);

        // Query only for expectedNamespace
        searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .addFilterNamespaces("expectedNamespace")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(expectedEmail);

        // Query only for non-existent namespace
        searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .addFilterNamespaces("nonExistNamespace")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testQuery_getPackageName() throws Exception {
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
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getPackageName()).isEqualTo(
                        ApplicationProvider.getApplicationContext().getPackageName());
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);
    }

    @Test
    public void testQuery_getDatabaseName() throws Exception {
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
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getDatabaseName()).isEqualTo(DB_NAME_1);
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);

        // Schema registration for another database
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        searchResults = mDb2.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getDatabaseName()).isEqualTo(DB_NAME_2);
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);
    }

    @Test
    public void testQuery_projection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"Email", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_projectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"Email", []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(AppSearchEmail.SCHEMA_TYPE, Collections.emptyList())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned without any properties. The note document
        // should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_projectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType", []}, {"Email", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to" properties.
        // The note document should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(
                        SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with only the "body" property.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .build()).build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(SearchSpec.SCHEMA_TYPE_WILDCARD, Collections.emptyList())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email and note documents should have been returned without any properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000).build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType", []}, {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(
                        SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with only the "body" property.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testSearchSpec_setSourceTag_notSupported() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG));
        // UnsupportedOperationException will be thrown with these queries so no need to
        // define a schema and index document.
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec = builder.setSearchSourceLogTag("tag").build();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.search("\"Hello, world!\"", searchSpec));
        assertThat(exception).hasMessageThat().contains(
                Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG
                        + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testQuery_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

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

        // Query for instance 1.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail1);

        // Query for instance 2.
        searchResults = mDb2.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail2);
    }

    @Test
    public void testQuery_typePropertyFilters() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example subject with some body")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query with type property filters {"Email", ["subject", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // Only email2 should be returned because email1 doesn't have the term "body" in subject
        // or to fields
        assertThat(documents).containsExactly(email2);
    }

    @Test
    public void testQuery_typePropertyFiltersWithDifferentSchemaTypes() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"Email": ["subject", "to"], "Note": ["body"]}. Note
        // schema has body in its property filter but Email schema doesn't.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .addFilterProperties("Note", ImmutableList.of("body"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // Only the note document should be returned because the email property filter doesn't
        // allow searching in the body.
        assertThat(documents).containsExactly(note);
    }

    @Test
    public void testQuery_typePropertyFiltersWithWildcard() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example subject with some body")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*": ["subject", "title"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(SearchSpec.SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "title"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // The wildcard property filter will apply to both the Email and Note schema. The email
        // document should be returned since it has the term "body" in its subject property. The
        // note document should not be returned since it doesn't have the term "body" in the title
        // property (subject property is not applicable for Note schema)
        assertThat(documents).containsExactly(email);
    }

    @Test
    public void testQuery_typePropertyFiltersWithWildcardAndExplicitSchema() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example subject with some body")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*": ["subject", "title"], "Note": ["body"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(SearchSpec.SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "title"))
                .addFilterProperties("Note", ImmutableList.of("body"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // The wildcard property filter will only apply to the Email schema since Note schema has
        // its own explicit property filter specified. The email document should be returned since
        // it has the term "body" in its subject property. The note document should also be returned
        // since it has the term "body" in the body property.
        assertThat(documents).containsExactly(email, note);
    }

    @Test
    public void testQuery_typePropertyFiltersNonExistentType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example subject with some body")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType": ["to", "title"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties("NonExistentType", ImmutableList.of("to", "title"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // The supplied property filters don't apply to either schema types. Both the documents
        // should be returned since the term "body" is present in at least one of their properties.
        assertThat(documents).containsExactly(email, note);
    }

    @Test
    public void testQuery_typePropertyFiltersEmpty() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"email": []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(AppSearchEmail.SCHEMA_TYPE, Collections.emptyList())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        // The email document should not be returned since the property filter doesn't allow
        // searching any property.
        assertThat(documents).containsExactly(note);
    }

    @Test
    public void testSnippet() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "Generic")
                        .setPropertyString("subject", "A commonly used fake word is foo. "
                                + "Another nonsense word that’s used a lot is bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("fo",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setMaxSnippetSize(10)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo("A commonly used fake word is foo. "
                + "Another nonsense word that’s used a lot is bar");
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*start=*/29,  /*end=*/32));
        assertThat(matchInfo.getExactMatch()).isEqualTo("foo");
        assertThat(matchInfo.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*start=*/26,  /*end=*/33));
        assertThat(matchInfo.getSnippet()).isEqualTo("is foo.");

        if (!mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatchRange);
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatch);
        } else {
            assertThat(matchInfo.getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*start=*/29,  /*end=*/31));
            assertThat(matchInfo.getSubmatch()).isEqualTo("fo");
        }
    }

    @Test
    public void testSetSnippetCount() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index documents
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(
                        new GenericDocument.Builder<>("namespace", "id1", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like cats", "I like dogs", "I like birds", "I like fish")
                                .setScore(10)
                                .build(),
                        new GenericDocument.Builder<>("namespace", "id2", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like red",
                                        "I like green",
                                        "I like blue",
                                        "I like yellow")
                                .setScore(20)
                                .build(),
                        new GenericDocument.Builder<>("namespace", "id3", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like cupcakes",
                                        "I like donuts",
                                        "I like eclairs",
                                        "I like froyo")
                                .setScore(5)
                                .build())
                .build()));

        // Query for the document
        SearchResults searchResults = mDb1.search(
                "like",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(2)
                        .setSnippetCountPerProperty(3)
                        .setMaxSnippetSize(11)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());

        // Check result 1
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(3);

        assertThat(results.get(0).getGenericDocument().getId()).isEqualTo("id2");
        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).hasSize(3);
        assertThat(matchInfos.get(0).getSnippet()).isEqualTo("I like red");
        assertThat(matchInfos.get(1).getSnippet()).isEqualTo("I like");
        assertThat(matchInfos.get(2).getSnippet()).isEqualTo("I like blue");

        // Check result 2
        assertThat(results.get(1).getGenericDocument().getId()).isEqualTo("id1");
        matchInfos = results.get(1).getMatchInfos();
        assertThat(matchInfos).hasSize(3);
        assertThat(matchInfos.get(0).getSnippet()).isEqualTo("I like cats");
        assertThat(matchInfos.get(1).getSnippet()).isEqualTo("I like dogs");
        assertThat(matchInfos.get(2).getSnippet()).isEqualTo("I like");

        // Check result 2
        assertThat(results.get(2).getGenericDocument().getId()).isEqualTo("id3");
        matchInfos = results.get(2).getMatchInfos();
        assertThat(matchInfos).isEmpty();
    }

    @Test
    public void testCJKSnippet() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        String japanese =
                "差し出されたのが今日ランドセルでした普通の子であれば満面の笑みで俺を言うでしょうしかし私は赤いランド"
                        + "セルを見て笑うことができませんでしたどうしたのと心配そうな仕事ガラスながら渋い顔する私書いたこと言"
                        + "うんじゃないのカードとなる声を聞きたい私は目から涙をこぼしながらおじいちゃんの近くにかけおり頭をポ"
                        + "ンポンと叩きピンクが良かったんだもん";
        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "Generic")
                        .setPropertyString("subject", japanese)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("は",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo(japanese);
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*start=*/44,  /*end=*/45));
        assertThat(matchInfo.getExactMatch()).isEqualTo("は");

        if (!mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatchRange);
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatch);
        } else {
            assertThat(matchInfo.getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*start=*/44,  /*end=*/45));
            assertThat(matchInfo.getSubmatch()).isEqualTo("は");
        }
    }

    @Test
    public void testRemove() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1",
                                "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Test if we delete a nonexistent id.
        AppSearchBatchResult<String, Void> deleteResult = mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()).get();

        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_multipleIds() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1",
                                "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByQuery() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("bar")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the email 1 by query "foo"
        mDb1.removeAsync("foo",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace")
                                .addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Delete the email 2 by query "bar"
        mDb1.removeAsync("bar",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }


    @Test
    public void testRemoveByQuery_nonExistNamespace() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace2", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("bar")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace1", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace2", "id2")).hasSize(1);

        // Delete the email by nonExist namespace.
        mDb1.removeAsync("",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("nonExistNamespace").build()).get();
        // None of these emails will be deleted.
        assertThat(doGet(mDb1, "namespace1", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace2", "id2")).hasSize(1);
    }

    @Test
    public void testRemoveByQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Try to delete email with query "foo", but restricted to a different package name.
        // Won't work and email will still exist.
        mDb1.removeAsync("foo",
                new SearchSpec.Builder().setTermMatch(
                        SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                        "some.other.package").build()).get();
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the email by query "foo", restricted to the correct package this time.
        mDb1.removeAsync("foo", new SearchSpec.Builder().setTermMatch(
                SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                ApplicationProvider.getApplicationContext().getPackageName()).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
            new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult = mDb2.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent id.
        deleteResult = mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByTypes() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic").build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).addSchemas(
                        genericSchema).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id3", "Generic").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1", "id2", "id3")).hasSize(3);

        // Delete the email type
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2", "id3").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByTypes_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the email type in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveByNamespace() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).addSchemas(
                        genericSchema).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("email", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("email", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("document", "id3", "Generic")
                        .setPropertyString("foo", "bar").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1", "id2")).hasSize(2);
        assertThat(doGet(mDb1, /*namespace=*/"document", "id3")).hasSize(1);

        // Delete the email namespace
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1", "id2").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("document")
                        .addIds("id3").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByNamespaces_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("email", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("email", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1")).hasSize(1);
        assertThat(doGet(mDb2, /*namespace=*/"email", "id2")).hasSize(1);

        // Delete the email namespace in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the all document in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_termMatchType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email3 =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 3")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email4 =
                new AppSearchEmail.Builder("namespace", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 4")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email3, email4).build()));

        // Check the presence of the documents
        SearchResults searchResults = mDb1.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        searchResults = mDb2.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Delete the all document in instance 1 with TERM_MATCH_PREFIX
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();
        searchResults = mDb1.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Delete the all document in instance 2 with TERM_MATCH_EXACT_ONLY
        mDb2.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build())
                .get();
        searchResults = mDb2.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testRemoveAllAfterEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Remove the document
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Delete the all documents
        mDb1.removeAsync("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();

        // Make sure it's still gone
        getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveQueryWithJoinSpecThrowsException() {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> mDb2.removeAsync("", new SearchSpec.Builder()
                        .setJoinSpec(new JoinSpec.Builder("entityId").build())
                        .build()));
        assertThat(e.getMessage()).isEqualTo("JoinSpec not allowed in removeByQuery, "
                + "but JoinSpec was provided.");
    }

    @Test
    public void testCloseAndReopen() throws Exception {
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

        // close and re-open the appSearchSession
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();

        // Query for the document
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testCallAfterClose() throws Exception {

        // Create a same-thread database by inject an executor which could help us maintain the
        // execution order of those async tasks.
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession sameThreadDb = createSearchSessionAsync(
                "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();

        try {
            // Schema registration -- just mutate something
            sameThreadDb.setSchemaAsync(
                    new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

            // Close the database. No further call will be allowed.
            sameThreadDb.close();

            // Try to query the closed database
            // We are using the same-thread db here to make sure it has been closed.
            IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                    sameThreadDb.search("query", new SearchSpec.Builder()
                            .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                            .build()));
            assertThat(e).hasMessageThat().contains("SearchSession has already been closed");
        } finally {
            // To clean the data that has been added in the test, need to re-open the session and
            // set an empty schema.
            AppSearchSession reopen = createSearchSessionAsync(
                    "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();
            reopen.setSchemaAsync(new SetSchemaRequest.Builder()
                    .setForceOverride(true).build()).get();
        }
    }

    @Test
    public void testReportUsage() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Email 1 has more usages, but email 2 has more recent usages.
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(1000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(2000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(3000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id2")
                .setUsageTimestampMillis(10000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id2")
                .setUsageTimestampMillis(20000).build()).get();

        // Query by number of usages
        List<SearchResult> results = retrieveAllSearchResults(
                mDb1.search("", new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_COUNT)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        // Email 1 has three usages and email 2 has two usages.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(3);
        assertThat(results.get(1).getRankingSignal()).isEqualTo(2);

        // Query by most recent usage.
        results = retrieveAllSearchResults(
                mDb1.search("", new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(20000);
        assertThat(results.get(1).getRankingSignal()).isEqualTo(3000);
    }

    @Test
    public void testReportUsage_invalidNamespace() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Use the correct namespace; it works
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1").build()).get();

        // Use an incorrect namespace; it fails
        ReportUsageRequest reportUsageRequest =
                new ReportUsageRequest.Builder("namespace2", "id1").build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.reportUsageAsync(reportUsageRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException cause = (AppSearchException) executionException.getCause();
        assertThat(cause.getResultCode()).isEqualTo(RESULT_NOT_FOUND);
    }

    @Test
    public void testGetStorageInfo() throws Exception {
        StorageInfo storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Still no storage space attributed with just a schema
        storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        // Index two documents.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace1", "id1").build();
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace1", "id2").build();
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace2", "id1").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2,
                        email3).build()));

        // Non-zero size now
        storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(3);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(2);
    }

    @Test
    public void testFlush() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        // The future returned from requestFlush will be set as a void or an Exception on error.
        mDb1.requestFlushAsync().get();
    }

    @Test
    public void testQuery_ResultGroupingLimits() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index four documents.
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
                new AppSearchEmail.Builder("namespace1", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace2", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));

        // Query with per package result grouping. Only the last document 'email4' should be
        // returned.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*limit=*/ 1)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4);

        // Query with per namespace result grouping. Only the last document in each namespace should
        // be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE, /*limit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);

        // Query with per package and per namespace result grouping. Only the last document in each
        // namespace should be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*limit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);
    }

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
                        SearchSpec.GROUPING_TYPE_PER_PACKAGE, /* limit= */ 1)
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
                        /* limit= */ 1)
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
                        /* limit= */ 2)
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
                        SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* limit= */ 1)
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
                        SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* limit= */ 2)
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
                        /* limit= */ 1)
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
                        /* limit= */ 2)
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
                        /* limit= */ 1)
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
                        /* limit= */ 2)
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
                        /* limit= */ 1)
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
                        /* limit= */ 2)
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
                        /* limit= */ 1)
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
                        /* limit= */ 2)
                    .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents)
                .containsExactly(inDoc3, inDoc2, inDoc1, inEmail5, inEmail4, inEmail2, inEmail1);
    }

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
                    SearchSpec.GROUPING_TYPE_PER_SCHEMA, /* limit= */ 1)
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
                    /* limit= */ 1)
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
                    /* limit= */ 1)
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
                    /* limit= */ 1)
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
    public void testIndexNestedDocuments() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("YesNestedIndex")
                                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "prop", AppSearchEmail.SCHEMA_TYPE)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                                .build())
                        .addSchemas(new AppSearchSchema.Builder("NoNestedIndex")
                                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "prop", AppSearchEmail.SCHEMA_TYPE)
                                        .setShouldIndexNestedProperties(false)
                                        .build())
                                .build())
                        .build())
                .get();

        // Index the documents.
        AppSearchEmail email = new AppSearchEmail.Builder("", "")
                .setSubject("This is the body")
                .build();
        GenericDocument yesNestedIndex =
                new GenericDocument.Builder<>("namespace", "yesNestedIndex", "YesNestedIndex")
                        .setPropertyDocument("prop", email)
                        .build();
        GenericDocument noNestedIndex =
                new GenericDocument.Builder<>("namespace", "noNestedIndex", "NoNestedIndex")
                        .setPropertyDocument("prop", email)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(yesNestedIndex, noNestedIndex).build()));

        // Query.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setSnippetCount(10)
                .setSnippetCountPerProperty(10)
                .build());
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument()).isEqualTo(yesNestedIndex);
        List<SearchResult.MatchInfo> matches = page.get(0).getMatchInfos();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getPropertyPath()).isEqualTo("prop.subject");
        assertThat(matches.get(0).getPropertyPathObject())
                .isEqualTo(new PropertyPath("prop.subject"));
        assertThat(matches.get(0).getFullText()).isEqualTo("This is the body");
        assertThat(matches.get(0).getExactMatch()).isEqualTo("body");
    }

    @Test
    public void testCJKTQuery() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "uri1")
                        .setBody("他是個男孩 is a boy")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        // Query for "他" (He)
        SearchResults searchResults = mDb1.search("他", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);

        // Query for "男孩" (boy)
        searchResults = mDb1.search("男孩", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);

        // Query for "boy"
        searchResults = mDb1.search("boy", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);
    }

    @Test
    public void testSetSchemaWithIncompatibleNestedSchema() throws Exception {
        // 1. Set the original schema. This should succeed without any problems.
        AppSearchSchema originalNestedSchema =
                new AppSearchSchema.Builder("TypeA").addProperty(new StringPropertyConfig.Builder(
                        "prop1").setCardinality(
                        PropertyConfig.CARDINALITY_OPTIONAL).build()).build();
        SetSchemaRequest originalRequest =
                new SetSchemaRequest.Builder().addSchemas(originalNestedSchema).build();
        mDb1.setSchemaAsync(originalRequest).get();

        // 2. Set a new schema with a new type that refers to "TypeA" and an incompatible change to
        // "TypeA". This should fail.
        AppSearchSchema newNestedSchema =
                new AppSearchSchema.Builder("TypeA").addProperty(new StringPropertyConfig.Builder(
                        "prop1").setCardinality(
                        PropertyConfig.CARDINALITY_REQUIRED).build()).build();
        AppSearchSchema newSchema =
                new AppSearchSchema.Builder("TypeB").addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder("prop2",
                                "TypeA").build()).build();
        final SetSchemaRequest newRequest =
                new SetSchemaRequest.Builder().addSchemas(newNestedSchema,
                        newSchema).build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(newRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Incompatible types: {TypeA}");

        // 3. Now set that same set of schemas but with forceOverride=true. This should succeed.
        SetSchemaRequest newRequestForced =
                new SetSchemaRequest.Builder().addSchemas(newNestedSchema,
                        newSchema).setForceOverride(true).build();
        mDb1.setSchemaAsync(newRequestForced).get();
    }

    @Test
    public void testEmojiSnippet() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // String:     "Luca Brasi sleeps with the 🐟🐟🐟."
        //              ^    ^     ^      ^    ^   ^ ^  ^ ^
        // UTF8 idx:    0    5     11     18   23 27 3135 39
        // UTF16 idx:   0    5     11     18   23 27 2931 33
        // Breaks into segments: "Luca", "Brasi", "sleeps", "with", "the", "🐟", "🐟"
        // and "🐟".
        // Index a document to instance 1.
        String sicilianMessage = "Luca Brasi sleeps with the 🐟🐟🐟.";
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "uri1")
                        .setBody(sicilianMessage)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "uri2")
                        .setBody("Some other content.")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Query for "🐟"
        SearchResults searchResults = mDb1.search("🐟", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setSnippetCount(1)
                .setSnippetCountPerProperty(1)
                .build());
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument()).isEqualTo(inEmail1);
        List<SearchResult.MatchInfo> matches = page.get(0).getMatchInfos();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getPropertyPath()).isEqualTo("body");
        assertThat(matches.get(0).getFullText()).isEqualTo(sicilianMessage);
        assertThat(matches.get(0).getExactMatch()).isEqualTo("🐟");
        if (mDb1.getFeatures().isFeatureSupported(Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThat(matches.get(0).getSubmatch()).isEqualTo("🐟");
        }
    }

    @Test
    public void testRfc822() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.TOKENIZER_TYPE_RFC822));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_RFC822)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(emailSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>("NS", "alex1", "Email")
                .setPropertyString("address", "Alex Saveliev <alex.sav@google.com>")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchResults sr = mDb1.search("com", new SearchSpec.Builder().build());
        List<SearchResult> page = sr.getNextPageAsync().get();

        // RFC tokenization will produce the following tokens for
        // "Alex Saveliev <alex.sav@google.com>" : ["Alex Saveliev <alex.sav@google.com>", "Alex",
        // "Saveliev", "alex.sav", "alex.sav@google.com", "alex.sav", "google", "com"]. Therefore,
        // a query for "com" should match the document.
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("alex1");

        // Plain tokenizer will not match this
        AppSearchSchema plainEmailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Flipping the tokenizer type is a backwards compatible change. The index will be
        // rebuilt with the email doc being tokenized in the new way.
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(plainEmailSchema).build()).get();

        sr = mDb1.search("com", new SearchSpec.Builder().build());

        // Plain tokenization will produce the following tokens for
        // "Alex Saveliev <alex.sav@google.com>" : ["Alex", "Saveliev", "<", "alex.sav",
        // "google.com", ">"]. So "com" will not match any of the tokens produced.
        assertThat(sr.getNextPageAsync().get()).hasSize(0);
    }

    @Test
    public void testRfc822_unsupportedFeature_throwsException() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.TOKENIZER_TYPE_RFC822));

        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_RFC822)
                        .build()
                ).build();

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                        .setForceOverride(true).addSchemas(emailSchema).build()).get());
        assertThat(e.getMessage()).isEqualTo("tokenizerType is out of range of [0, 1] (too high)");
    }


    @Test
    public void testQuery_verbatimSearch() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        AppSearchSchema verbatimSchema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(verbatimSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchResults sr = mDb1.search("\"Hello, world!\"",
                new SearchSpec.Builder().setVerbatimSearchEnabled(true).build());
        List<SearchResult> page = sr.getNextPageAsync().get();

        // Verbatim tokenization would produce one token 'Hello, world!'.
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
    }

    @Test
    public void testQuery_verbatimSearchWithoutEnablingFeatureFails() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        AppSearchSchema verbatimSchema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(verbatimSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        // Disable VERBATIM_SEARCH in the SearchSpec.
        SearchResults searchResults = mDb1.search("\"Hello, world!\"",
                new SearchSpec.Builder()
                        .setVerbatimSearchEnabled(false)
                        .build());
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.VERBATIM_SEARCH);
    }

    @Test
    public void testQuery_listFilterQueryWithEnablingFeatureSucceeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema")
                .setPropertyString("prop", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        // Support for function calls `search`, `createList` was added in list filters
        SearchResults searchResults = mDb1.search("search(\"hello\", createList(\"prop\"))",
                searchSpec);
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");

        // Support for prefix operator * was added in list filters.
        searchResults = mDb1.search("wor*", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");

        // Combining negations with compound statements and property restricts was added in list
        // filters.
        searchResults = mDb1.search("NOT (foo OR otherProp:hello)", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
    }

    @Test
    public void testQuery_PropertyDefinedWithEnablingFeatureSucceeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Schema1")
                .addProperty(new StringPropertyConfig.Builder("prop1")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Schema2")
                .addProperty(new StringPropertyConfig.Builder("prop2")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema1, schema2).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema1")
                .setPropertyString("prop1", "Hello, world!")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>(
                "namespace1", "id2", "Schema2")
                .setPropertyString("prop2", "Hello, world!")
                .build();
        mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        // Support for function calls `search`, `createList` was added in list filters
        SearchResults searchResults = mDb1.search("propertyDefined(\"prop1\")",
                searchSpec);
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");

        // Support for prefix operator * was added in list filters.
        searchResults = mDb1.search("propertyDefined(\"prop2\")", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id2");

        // Combining negations with compound statements and property restricts was added in list
        // filters.
        searchResults = mDb1.search("NOT propertyDefined(\"prop1\")", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id2");
    }

    @Test
    public void testQuery_listFilterQueryWithoutEnablingFeatureFails() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema")
                .setPropertyString("prop", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        // Disable LIST_FILTER_QUERY_LANGUAGE in the SearchSpec.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(false)
                .build();
        SearchResults searchResults = mDb1.search("search(\"hello\", createList(\"prop\"))",
                searchSpec);
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        SearchResults searchResults2 = mDb1.search("wor*", searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults2.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        SearchResults searchResults3 = mDb1.search("NOT (foo OR otherProp:hello)", searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults3.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        SearchResults searchResults4 = mDb1.search("propertyDefined(\"prop\")", searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults4.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);
    }

    @Test
    public void testQuery_listFilterQueryFeatures_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));

        // UnsupportedOperationException will be thrown with these queries so no need to
        // define a schema and index document.
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec1 = builder.setNumericSearchEnabled(true).build();
        SearchSpec searchSpec2 = builder.setVerbatimSearchEnabled(true).build();
        SearchSpec searchSpec3 = builder.setListFilterQueryLanguageEnabled(true).build();

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec1));
        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec2));
        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec3));
    }

    @Test
    public void testQuery_listFilterQueryHasPropertyFunction_notSupported() throws Exception {
        assumeFalse(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_HAS_PROPERTY_FUNCTION));

        // UnsupportedOperationException will be thrown with these queries so no need to
        // define a schema and index document.
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec = builder.setListFilterHasPropertyFunctionEnabled(true).build();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.search("\"Hello, world!\"", searchSpec));
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_HAS_PROPERTY_FUNCTION
                + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testQuery_hasPropertyFunction() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_HAS_PROPERTY_FUNCTION));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop1")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("prop2")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>(
                "namespace", "id1", "Schema")
                .setPropertyString("prop1", "Hello, world!")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>(
                "namespace", "id2", "Schema")
                .setPropertyString("prop2", "Hello, world!")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>(
                "namespace", "id3", "Schema")
                .setPropertyString("prop1", "Hello, world!")
                .setPropertyString("prop2", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(doc1, doc2, doc3).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(true)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        SearchResults searchResults = mDb1.search("hasProperty(\"prop1\")",
                searchSpec);
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id3");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id1");

        searchResults = mDb1.search("hasProperty(\"prop2\")", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id3");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id2");

        searchResults = mDb1.search(
                "hasProperty(\"prop1\") AND hasProperty(\"prop2\")",
                searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id3");
    }

    @Test
    public void testQuery_hasPropertyFunctionWithoutEnablingFeatureFails() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_HAS_PROPERTY_FUNCTION));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema")
                .setPropertyString("prop", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()).get();

        // Enable LIST_FILTER_HAS_PROPERTY_FUNCTION but disable LIST_FILTER_QUERY_LANGUAGE in the
        // SearchSpec.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(false)
                .setListFilterHasPropertyFunctionEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search("hasProperty(\"prop\")",
                searchSpec);
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        // Disable LIST_FILTER_HAS_PROPERTY_FUNCTION in the SearchSpec.
        searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setListFilterHasPropertyFunctionEnabled(false)
                .build();
        SearchResults searchResults2 = mDb1.search("hasProperty(\"prop\")",
                searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults2.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains("HAS_PROPERTY_FUNCTION");
    }

    @Test
    public void testQuery_propertyWeightsNotSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject",
                        2.0, "body", 0.5))
                .build();
        UnsupportedOperationException exception =
                assertThrows(UnsupportedOperationException.class,
                        () -> mDb1.search("Hello", searchSpec));
        assertThat(exception).hasMessageThat().contains("Property weights are not supported");
    }

    @Test
    public void testQuery_propertyWeights() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "foo". It should match both emails.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject",
                        2.0, "body", 0.5))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // email1 should be ranked higher because "foo" appears in the "subject" property which
        // has higher weight than the "body" property.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);

        // Query for "foo" without property weights.
        SearchSpec searchSpecWithoutWeights = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .build();
        SearchResults searchResultsWithoutWeights = mDb1.search("foo", searchSpecWithoutWeights);
        List<SearchResult> resultsWithoutWeights =
                retrieveAllSearchResults(searchResultsWithoutWeights);

        // email1 should have the same ranking signal as email2 as each contains the term "foo"
        // once.
        assertThat(resultsWithoutWeights).hasSize(2);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isEqualTo(
                resultsWithoutWeights.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsNestedProperties() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Register a schema with a nested type
        AppSearchSchema schema =
                new AppSearchSchema.Builder("TypeA").addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder("nestedEmail",
                                AppSearchEmail.SCHEMA_TYPE).setShouldIndexNestedProperties(
                                true).build()).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA,
                schema).build()).get();

        // Index two documents
        AppSearchEmail nestedEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "TypeA").setPropertyDocument(
                        "nestedEmail", nestedEmail1).build();
        AppSearchEmail nestedEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "TypeA").setPropertyDocument(
                        "nestedEmail", nestedEmail2).build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(doc1, doc2).build()));

        // Query for "foo". It should match both emails.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights("TypeA", ImmutableMap.of(
                        "nestedEmail.subject",
                        2.0, "nestedEmail.body", 0.5))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // email1 should be ranked higher because "foo" appears in the "nestedEmail.subject"
        // property which has higher weight than the "nestedEmail.body" property.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc2);

        // Query for "foo" without property weights.
        SearchSpec searchSpecWithoutWeights = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .build();
        SearchResults searchResultsWithoutWeights = mDb1.search("foo", searchSpecWithoutWeights);
        List<SearchResult> resultsWithoutWeights =
                retrieveAllSearchResults(searchResultsWithoutWeights);

        // email1 should have the same ranking signal as email2 as each contains the term "foo"
        // once.
        assertThat(resultsWithoutWeights).hasSize(2);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isEqualTo(
                resultsWithoutWeights.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsDefaults() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "foo" without assigning property weights for any path.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of())
                .build());
        List<SearchResult> resultsWithoutPropertyWeights = retrieveAllSearchResults(
                searchResults);

        // Query for "foo" with assigning default property weights.
        searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 1.0,
                        "body", 1.0))
                .build());
        List<SearchResult> expectedResults = retrieveAllSearchResults(searchResults);

        assertThat(resultsWithoutPropertyWeights).hasSize(2);
        assertThat(expectedResults).hasSize(2);

        assertThat(resultsWithoutPropertyWeights.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(resultsWithoutPropertyWeights.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(expectedResults.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(expectedResults.get(1).getGenericDocument()).isEqualTo(email2);

        // The ranking signal for results with no property path and weights set should be equal
        // to the ranking signal for results with explicitly set default weights.
        assertThat(resultsWithoutPropertyWeights.get(0).getRankingSignal()).isEqualTo(
                expectedResults.get(0).getRankingSignal());
        assertThat(resultsWithoutPropertyWeights.get(1).getRankingSignal()).isEqualTo(
                expectedResults.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsIgnoresInvalidPropertyPaths() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index an email
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("baz")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        // Query for "baz" with property weight for "subject", a valid property in the schema type.
        SearchResults searchResults = mDb1.search("baz", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 2.0))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // Query for "baz" with property weights, one for valid property "subject" and one for a
        // non-existing property "invalid".
        searchResults = mDb1.search("baz", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 2.0,
                        "invalid", 3.0))
                .build());
        List<SearchResult> resultsWithInvalidPath = retrieveAllSearchResults(searchResults);

        assertThat(results).hasSize(1);
        assertThat(resultsWithInvalidPath).hasSize(1);

        // We expect the ranking signal to be unchanged in the presence of an invalid property
        // weight.
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithInvalidPath.get(0).getRankingSignal()).isEqualTo(
                results.get(0).getRankingSignal());

        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(resultsWithInvalidPath.get(0).getGenericDocument()).isEqualTo(email1);
    }

    @Test
    public void testQueryWithJoin_typePropertyFiltersOnNestedSpec() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used with property filters in join spec
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("viewType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index 2 email documents
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        // Index 2 viewAction documents, one for email1 and the other for email2
        String qualifiedId1 =
                DocumentIdUtil.createQualifiedId(
                        ApplicationProvider.getApplicationContext().getPackageName(), DB_NAME_1,
                        "namespace", "id1");
        String qualifiedId2 =
                DocumentIdUtil.createQualifiedId(
                        ApplicationProvider.getApplicationContext().getPackageName(), DB_NAME_1,
                        "namespace", "id2");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setPropertyString("entityId", qualifiedId1)
                .setPropertyString("note", "Viewed email on Monday")
                .setPropertyString("viewType", "Stared").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id4", "ViewAction")
                .setPropertyString("entityId", qualifiedId2)
                .setPropertyString("note", "Viewed email on Tuesday")
                .setPropertyString("viewType", "Viewed").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inEmail2,
                                viewAction1, viewAction2)
                        .build()));

        // The nested search spec only allows searching the viewType property for viewAction
        // schema type. It also specifies a property filter for Email schema.
        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .addFilterProperties("ViewAction", ImmutableList.of("viewType"))
                        .addFilterProperties(AppSearchEmail.SCHEMA_TYPE,
                                ImmutableList.of("subject"))
                        .build();

        // Search for the term "Viewed" in join spec
        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("Viewed", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .build();

        SearchResults searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        // Both email docs are returned, email2 comes first because it has higher number of
        // joined documents. The property filters for Email schema specified in the nested search
        // specs don't apply to the outer query (otherwise none of the email documents would have
        // been returned).
        assertThat(sr).hasSize(2);

        // Email2 has a viewAction document viewAction2 that satisfies the property filters in
        // the join spec, so it should be present in the joined results.
        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(1.0);
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction2);

        // Email1 has a viewAction document viewAction1 but it doesn't satisfy the property filters
        // in the join spec, so it should not be present in the joined results.
        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(0.0);
        assertThat(sr.get(1).getJoinedResults()).isEmpty();
    }

    @Test
    public void testQueryWithJoin_typePropertyFiltersOnOuterSpec() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used with property filters in join spec
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("viewType")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index 2 email documents
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();

        // Index 2 viewAction documents, one for email1 and the other for email2
        String qualifiedId1 =
                DocumentIdUtil.createQualifiedId(
                        ApplicationProvider.getApplicationContext().getPackageName(), DB_NAME_1,
                        "namespace", "id1");
        String qualifiedId2 =
                DocumentIdUtil.createQualifiedId(
                        ApplicationProvider.getApplicationContext().getPackageName(), DB_NAME_1,
                        "namespace", "id2");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setPropertyString("entityId", qualifiedId1)
                .setPropertyString("note", "Viewed email on Monday")
                .setPropertyString("viewType", "Stared").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id4", "ViewAction")
                .setPropertyString("entityId", qualifiedId2)
                .setPropertyString("note", "Viewed email on Tuesday")
                .setPropertyString("viewType", "Viewed").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inEmail2,
                                viewAction1, viewAction2)
                        .build()));

        // The nested search spec doesn't specify any property filters.
        SearchSpec nestedSearchSpec = new SearchSpec.Builder().build();

        // Search for the term "Viewed" in join spec
        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("Viewed", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .build();

        // Outer search spec adds property filters for both Email and ViewAction schema
        SearchResults searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("body"))
                .addFilterProperties("ViewAction", ImmutableList.of("viewType"))
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        // Both email docs are returned as they both satisfy the property filters for Email, email2
        // comes first because it has higher id lexicographically.
        assertThat(sr).hasSize(2);

        // Email2 has a viewAction document viewAction2 that satisfies the property filters in
        // the outer spec (although those property filters are irrelevant for joined documents),
        // it should be present in the joined results.
        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(1.0);
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction2);

        // Email1 has a viewAction document viewAction1 that doesn't satisfy the property filters
        // in the outer spec, but property filters in the outer spec should not apply on joined
        // documents, so viewAction1 should be present in the joined results.
        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(1.0);
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(1).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction1);
    }

    @Test
    public void testQuery_typePropertyFiltersNotSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Query with type property filters {"Email", ["subject", "to"]} and verify that unsupported
        // exception is thrown
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterProperties(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build();
        UnsupportedOperationException exception =
                assertThrows(UnsupportedOperationException.class,
                        () -> mDb1.search("body", searchSpec));
        assertThat(exception).hasMessageThat().contains(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testSimpleJoin() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index a document
        // While inEmail2 has a higher document score, we will rank based on the number of joined
        // documents. inEmail1 will have 1 joined document while inEmail2 will have 0 joined
        // documents.
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(10)
                        .build();

        String qualifiedId = DocumentIdUtil.createQualifiedId(mContext.getPackageName(), DB_NAME_1,
                "namespace", "id1");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setScore(1)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Monday").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id4", "ViewAction")
                .setScore(2)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Tuesday").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inEmail2,
                                viewAction1, viewAction2)
                        .build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_ASCENDING)
                        .build();

        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(1)
                .build();

        SearchResults searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        // Both email docs are returned, but id1 comes first due to the join
        assertThat(sr).hasSize(2);

        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction1);
        // SearchSpec.Builder#setMaxJoinedResultCount only limits the number of child documents
        // returned. It does not affect the number of child documents that are scored. So the score
        // (the COUNT of the number of children) is 2, even though only one child is returned.
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(2.0);

        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(0.0);
        assertThat(sr.get(1).getJoinedResults()).isEmpty();
    }

    @Test
    public void testJoin_unsupportedFeature_throwsException() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        SearchSpec nestedSearchSpec = new SearchSpec.Builder().build();
        JoinSpec js = new JoinSpec.Builder("entityId").setNestedSearch("", nestedSearchSpec)
                .build();
        Exception e = assertThrows(UnsupportedOperationException.class, () -> mDb1.search(
                /*queryExpression */ "",
                new SearchSpec.Builder()
                        .setJoinSpec(js)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        assertThat(e.getMessage()).isEqualTo("JoinSpec is not available on this AppSearch "
                + "implementation.");
    }

    @Test
    public void testSearchSuggestion_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.searchSuggestionAsync(
                        /*suggestionQueryExpression=*/"t",
                        new SearchSuggestionSpec.Builder(/*maximumResultCount=*/2).build()).get());
    }

    @Test
    public void testSearchSuggestion() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "termOne termTwo termThree termFour")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "termOne termTwo termThree")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "termOne")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo, resultThree, resultFour)
                .inOrder();

        // Query first 2 suggestions, and they will be ranked.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/2).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo).inOrder();
    }

    @Test
    public void testSearchSuggestion_namespaceFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "Type")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace2", "id2", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace3", "id3", "Type")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // namespace1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // namespace2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // namespace2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace2", "namespace3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist namespace has empty result
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("nonExistNamespace").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_documentIdFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "Type")
                .setPropertyString("body", "termone")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace1", "id2", "Type")
                .setPropertyString("body", "termtwo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace2", "id3", "Type")
                .setPropertyString("body", "termthree")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace2", "id4", "Type")
                .setPropertyString("body", "termfour")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(doc1, doc2, doc3, doc4).build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        // Only search for namespace1/doc1
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace1")
                        .addFilterDocumentIds("namespace1", "id1")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne);

        // Only search for namespace1/doc1 and namespace1/doc2
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace1")
                        .addFilterDocumentIds("namespace1", ImmutableList.of("id1", "id2"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo);

        // Only search for namespace1/doc1 and namespace2/doc3
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterDocumentIds("namespace1", "id1")
                        .addFilterDocumentIds("namespace2", ImmutableList.of("id3"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree);

        // Only search for namespace1/doc1 and everything in namespace2
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterDocumentIds("namespace1", "id1")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree, resultFour);
    }

    @Test
    public void testSearchSuggestion_schemaFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schemaType1 = new AppSearchSchema.Builder("Type1").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType2 = new AppSearchSchema.Builder("Type2").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType3 = new AppSearchSchema.Builder("Type3").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schemaType1, schemaType2, schemaType3).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type1")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type2")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type3")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // Type1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterSchemas("Type1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // Type2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterSchemas("Type2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // Type2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterSchemas("Type2", "Type3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist type has empty result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .addFilterSchemas("nonExistType").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_differentPrefix() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "bar")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "baz")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();
        SearchSuggestionResult resultBar =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar").build();
        SearchSuggestionResult resultBaz =
                new SearchSuggestionResult.Builder().setSuggestedResult("baz").build();

        // prefix f has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // prefix b has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"b",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultBar, resultBaz);
    }

    @Test
    public void testSearchSuggestion_differentRankingStrategy() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        // term1 appears 3 times in all 3 docs.
        // term2 appears 4 times in 2 docs.
        // term3 appears 5 times in 1 doc.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "term1 term3 term3 term3 term3 term3")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "term1 term2 term2 term2")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "term1 term2")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        SearchSuggestionResult result1 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term1").build();
        SearchSuggestionResult result2 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term2").build();
        SearchSuggestionResult result3 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term3").build();


        // rank by NONE, the order should be arbitrary but all terms appear.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_NONE)
                        .build()).get();
        assertThat(suggestions).containsExactly(result2, result1, result3);

        // rank by document count, the order should be term1:3 > term2:2 > term3:1
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT)
                        .build()).get();
        assertThat(suggestions).containsExactly(result1, result2, result3).inOrder();

        // rank by term frequency, the order should be term3:5 > term2:4 > term1:3
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY)
                        .build()).get();
        assertThat(suggestions).containsExactly(result3, result2, result1).inOrder();
    }

    @Test
    public void testSearchSuggestion_removeDocument() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument docTwo = new GenericDocument.Builder<>("namespace", "idTwo", "Type")
                .setPropertyString("body", "two")
                .build();
        GenericDocument docThree = new GenericDocument.Builder<>("namespace", "idThree", "Type")
                .setPropertyString("body", "three")
                .build();
        GenericDocument docTart = new GenericDocument.Builder<>("namespace", "idTart", "Type")
                .setPropertyString("body", "tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(docTwo, docThree, docTart)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "idTwo").build()));

        // now prefix t has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTart);
    }

    @Test
    public void testSearchSuggestion_replacementDocument() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "two three tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();
        SearchSuggestionResult resultTwist =
                new SearchSuggestionResult.Builder().setSuggestedResult("twist").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // replace the document
        GenericDocument replaceDoc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "twist three")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(replaceDoc)
                        .build()));

        // prefix t has 2 results for now.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTwist);
    }

    @Test
    public void testSearchSuggestion_twoInstances() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents to database 1.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "termOne")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2)
                        .build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();

        // database 1 could get suggestion results
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo).inOrder();

        // database 2 couldn't get suggestion results
        suggestions = mDb2.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_multipleTerms() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "bar fo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "cat foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "fool")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        // Search "bar AND f" only document 1 should match the search.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        SearchSuggestionResult barFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar fo").build();
        assertThat(suggestions).containsExactly(barFo);

        // Search for "(bar OR cat) AND f" both document1 "bar fo" and document2 "cat foo" could
        // match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar OR cat f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        SearchSuggestionResult barCatFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar OR cat fo").build();
        SearchSuggestionResult barCatFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar OR cat foo").build();
        assertThat(suggestions).containsExactly(barCatFo, barCatFoo);

        // Search for "(bar AND cat) OR f", all documents could match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"(bar cat) OR f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        SearchSuggestionResult barCatOrFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("(bar cat) OR fo").build();
        SearchSuggestionResult barCatOrFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("(bar cat) OR foo").build();
        SearchSuggestionResult barCatOrFool =
                new SearchSuggestionResult.Builder()
                        .setSuggestedResult("(bar cat) OR fool").build();
        assertThat(suggestions).containsExactly(barCatOrFo, barCatOrFoo, barCatOrFool);

        // Search for "-bar f", document2 "cat foo" could and document3 "fool" could match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"-bar f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        SearchSuggestionResult noBarFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("-bar foo").build();
        SearchSuggestionResult noBarFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("-bar fool").build();
        assertThat(suggestions).containsExactly(noBarFoo, noBarFool);
    }

    @Test
    public void testSearchSuggestion_propertyFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));
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
                                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
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
                                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
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
                                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
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
                                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
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
                                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
                                        .addFilterProperties(
                                                "Type1", ImmutableList.of("propertyone"))
                                        .build())
                        .get();
        assertThat(suggestions).containsExactly(resultOne, resultThree, resultFour);
    }

    @Test
    public void testSearchSuggestion_propertyFilter_notSupported() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES));

        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/* maximumResultCount= */ 10)
                    .addFilterSchemas("Type1")
                    .addFilterProperties("Type1", ImmutableList.of("property"))
                    .build();

        // Search suggest with type property filters {"Email", ["property"]} and verify that
        // unsupported exception is thrown
        UnsupportedOperationException exception =
                assertThrows(UnsupportedOperationException.class,
                        () -> mDb1.searchSuggestionAsync(
                                /* suggestionQueryExpression= */ "t", searchSuggestionSpec).get());
        assertThat(exception).hasMessageThat().contains(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testSearchSuggestion_PropertyRestriction() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("subject", "bar fo")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("subject", "bar cat foo")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "ide", "Type")
                .setPropertyString("subject", "fool")
                .setPropertyString("body", "fool")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        // Search for "bar AND subject:f"
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar subject:f",
                new SearchSuggestionSpec.Builder(/*maximumResultCount=*/10).build()).get();
        SearchSuggestionResult barSubjectFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar subject:fo").build();
        SearchSuggestionResult barSubjectFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar subject:foo").build();
        assertThat(suggestions).containsExactly(barSubjectFo, barSubjectFoo);
    }

    @Test
    public void testGetSchema_parentTypes() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email").build();
        AppSearchSchema messageSchema = new AppSearchSchema.Builder("Message").build();
        AppSearchSchema emailMessageSchema =
                new AppSearchSchema.Builder("EmailMessage")
                        .addProperty(
                                new StringPropertyConfig.Builder("sender")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("email")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("content")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addParentType("Email")
                        .addParentType("Message")
                        .build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(emailMessageSchema)
                        .addSchemas(emailSchema)
                        .addSchemas(messageSchema)
                        .build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(3);
        assertThat(actual).isEqualTo(request.getSchemas());

        // Check that calling getParentType() for the EmailMessage schema returns Email and Message
        for (AppSearchSchema schema : actual) {
            if (schema.getSchemaType().equals("EmailMessage")) {
                assertThat(schema.getParentTypes()).containsExactly("Email", "Message");
            }
        }
    }

    @Test
    public void testGetSchema_parentTypes_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email").build();
        AppSearchSchema messageSchema = new AppSearchSchema.Builder("Message").build();
        AppSearchSchema emailMessageSchema =
                new AppSearchSchema.Builder("EmailMessage")
                        .addParentType("Email")
                        .addParentType("Message")
                        .build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(emailMessageSchema)
                        .addSchemas(emailSchema)
                        .addSchemas(messageSchema)
                        .build();

        UnsupportedOperationException e =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.setSchemaAsync(request).get());
        assertThat(e)
                .hasMessageThat()
                .contains(
                        Features.SCHEMA_ADD_PARENT_TYPE
                                + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testGetSchema_indexableNestedPropsList() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(Collections.singleton("name"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder()
                        .addSchemas(personSchema, organizationSchema)
                        .build();
        mDb1.setSchemaAsync(setSchemaRequest).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(2);
        assertThat(actual).isEqualTo(setSchemaRequest.getSchemas());

        for (AppSearchSchema schema : actual) {
            if (schema.getSchemaType().equals("Person")) {
                for (PropertyConfig property : schema.getProperties()) {
                    if (property.getName().equals("worksFor")) {
                        assertThat(
                                ((DocumentPropertyConfig) property)
                                        .getIndexableNestedProperties()).containsExactly("name");
                    }
                }
            }
        }
    }

    @Test
    public void testSetSchema_dataTypeIncompatibleWithParentTypes() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema messageSchema =
                new AppSearchSchema.Builder("Message")
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("sender")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .build();
        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .addParentType("Message")
                        .addProperty(
                                new StringPropertyConfig.Builder("sender")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(messageSchema)
                        .addSchemas(emailSchema)
                        .build();

        ExecutionException executionException =
                assertThrows(ExecutionException.class, () -> mDb1.setSchemaAsync(request).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception)
                .hasMessageThat()
                .containsMatch(
                        "Property sender from child type .*\\$/Email is not compatible"
                                + " to the parent type .*\\$/Message.");
    }

    @Test
    public void testSetSchema_documentTypeIncompatibleWithParentTypes() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person").build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist").addParentType("Person").build();
        AppSearchSchema messageSchema =
                new AppSearchSchema.Builder("Message")
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Artist")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .build();
        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .addParentType("Message")
                        // "sender" is defined as an Artist in the parent type Message, which
                        // requires "sender"'s type here to be a subtype of Artist. Thus, this is
                        // incompatible because Person is not a subtype of Artist.
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(personSchema)
                        .addSchemas(artistSchema)
                        .addSchemas(messageSchema)
                        .addSchemas(emailSchema)
                        .build();

        ExecutionException executionException =
                assertThrows(ExecutionException.class, () -> mDb1.setSchemaAsync(request).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception)
                .hasMessageThat()
                .containsMatch(
                        "Property sender from child type .*\\$/Email is not compatible"
                                + " to the parent type .*\\$/Message.");
    }

    @Test
    public void testSetSchema_compatibleWithParentTypes() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person").build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist").addParentType("Person").build();
        AppSearchSchema messageSchema =
                new AppSearchSchema.Builder("Message")
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("note")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();
        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .addParentType("Message")
                        .addProperty(
                                // Artist is a subtype of Person, so compatible
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Artist")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("note")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        // A different indexing or tokenizer type is ok.
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                                        .build())
                        .build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(personSchema)
                        .addSchemas(artistSchema)
                        .addSchemas(messageSchema)
                        .addSchemas(emailSchema)
                        .build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(4);
        assertThat(actual).isEqualTo(request.getSchemas());
    }

    @Test
    public void testSetSchema_indexableNestedPropsList() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(Collections.singleton("name"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema, organizationSchema)
                                .build())
                .get();

        // Test that properties in Person's indexable_nested_properties_list are indexed and
        // searchable
        GenericDocument org1 =
                new GenericDocument.Builder<>("namespace", "org1", "Organization")
                        .setPropertyString("name", "Org1")
                        .setPropertyString("notes", "Some notes")
                        .build();
        GenericDocument person1 =
                new GenericDocument.Builder<>("namespace", "person1", "Person")
                        .setPropertyString("name", "Jane")
                        .setPropertyDocument("worksFor", org1)
                        .build();

        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(
                        mDb1.putAsync(
                                new PutDocumentsRequest.Builder()
                                        .addGenericDocuments(person1, org1)
                                        .build()));
        assertThat(putResult.getSuccesses()).containsExactly("person1", null, "org1", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace").addIds("person1", "org1").build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person1, org1);

        // Both org1 and person should be returned for query "Org1"
        // For org1 this matches the 'name' property and for person1 this matches the
        // 'worksFor.name' property.
        SearchResults searchResults =
                mDb1.search(
                        "Org1",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person1, org1);

        // Only org1 should be returned for query "notes", since 'worksFor.notes' is not indexed
        // for the Person-type.
        searchResults =
                mDb1.search(
                        "notes",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(org1);
    }

    @Test
    public void testSetSchema_indexableNestedPropsList_notSupported() throws Exception {
        assumeFalse(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(Collections.singleton("name"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder().addSchemas(personSchema, organizationSchema).build();
        UnsupportedOperationException e =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(e)
                .hasMessageThat()
                .contains(
                        "DocumentPropertyConfig.addIndexableNestedProperties is not supported on"
                                + " this AppSearch implementation.");
    }

    @Test
    public void testSetSchema_indexableNestedPropsList_nonIndexableProp() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(Collections.singleton("name"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_NONE)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_NONE)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema, organizationSchema)
                                .build())
                .get();

        // Test that Person's nested properties are indexed correctly.
        GenericDocument org1 =
                new GenericDocument.Builder<>("namespace", "org1", "Organization")
                        .setPropertyString("name", "Org1")
                        .setPropertyString("notes", "Some notes")
                        .build();
        GenericDocument person1 =
                new GenericDocument.Builder<>("namespace", "person1", "Person")
                        .setPropertyString("name", "Jane")
                        .setPropertyDocument("worksFor", org1)
                        .build();

        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(
                        mDb1.putAsync(
                                new PutDocumentsRequest.Builder()
                                        .addGenericDocuments(person1, org1)
                                        .build()));
        assertThat(putResult.getSuccesses()).containsExactly("person1", null, "org1", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace").addIds("person1", "org1").build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person1, org1);

        // Both org1 and person should be returned for query "Org1"
        // For org1 this matches the 'name' property and for person1 this matches the
        // 'worksFor.name' property.
        SearchResults searchResults =
                mDb1.search(
                        "Org1",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person1, org1);

        // No documents should match for "notes", since both 'Organization:notes'
        // and 'Person:worksFor.notes' are non-indexable.
        searchResults =
                mDb1.search(
                        "notes",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(0);
    }

    @Test
    public void testSetSchema_indexableNestedPropsList_multipleNestedLevels() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(
                                                Arrays.asList(
                                                        "name", "worksFor.name", "worksFor.notes"))
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "recipient", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("age")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(Arrays.asList("name", "id"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("id")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();

        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(emailSchema, personSchema, organizationSchema)
                                .build())
                .get();

        // Test that Email and Person's nested properties are indexed correctly.
        GenericDocument org1 =
                new GenericDocument.Builder<>("namespace", "org1", "Organization")
                        .setPropertyString("name", "Org1")
                        .setPropertyString("notes", "Some notes")
                        .setPropertyString("id", "1234")
                        .build();
        GenericDocument person1 =
                new GenericDocument.Builder<>("namespace", "person1", "Person")
                        .setPropertyString("name", "Jane")
                        .setPropertyString("age", "20")
                        .setPropertyDocument("worksFor", org1)
                        .build();
        GenericDocument person2 =
                new GenericDocument.Builder<>("namespace", "person2", "Person")
                        .setPropertyString("name", "John")
                        .setPropertyString("age", "30")
                        .setPropertyDocument("worksFor", org1)
                        .build();
        GenericDocument email1 =
                new GenericDocument.Builder<>("namespace", "email1", "Email")
                        .setPropertyString("subject", "Greetings!")
                        .setPropertyDocument("sender", person1)
                        .setPropertyDocument("recipient", person2)
                        .build();
        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(
                        mDb1.putAsync(
                                new PutDocumentsRequest.Builder()
                                        .addGenericDocuments(person1, org1, person2, email1)
                                        .build()));
        assertThat(putResult.getSuccesses())
                .containsExactly("person1", null, "org1", null, "person2", null, "email1", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("person1", "org1", "person2", "email1")
                        .build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(4);
        assertThat(outDocuments).containsExactly(person1, org1, person2, email1);

        // Indexed properties:
        // Email: 'subject', 'sender.name', 'sender.worksFor.name', 'sender.worksFor.notes',
        //        'recipient.name', 'recipient.age', 'recipient.worksFor.name',
        //        'recipient.worksFor.id'
        //        (Email:recipient sets index_nested_props=true, so it follows the same indexing
        //         configs as the next schema-type level (person))
        // Person: 'name', 'age', 'worksFor.name', 'worksFor.id'
        // Organization: 'name', 'notes', 'id'
        //
        // All documents should be returned for query 'Org1' because all schemaTypes index the
        // 'Organization:name' property.
        SearchResults searchResults =
                mDb1.search(
                        "Org1",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(4);
        assertThat(outDocuments).containsExactly(person1, org1, person2, email1);

        // org1 and email1 should be returned for query 'notes'
        searchResults =
                mDb1.search(
                        "notes",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(org1, email1);

        // all docs should be returned for query "1234"
        searchResults =
                mDb1.search(
                        "1234",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(4);
        assertThat(outDocuments).containsExactly(person1, org1, person2, email1);

        // email1 should be returned for query "30", but not for "20" since sender.age is not
        // indexed, but recipient.age is.
        // For query "30", person2 should also be returned
        // For query "20, person1 should be returned.
        searchResults =
                mDb1.search(
                        "30",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person2, email1);

        searchResults =
                mDb1.search(
                        "20",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person1);
    }

    @Test
    public void testSetSchema_indexableNestedPropsList_circularRefs() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SET_SCHEMA_CIRCULAR_REFERENCES));

        // Create schema with valid cycle: Person -> Organization -> Person...
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("address")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "worksFor", "Organization")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(
                                                Arrays.asList("name", "notes", "funder.name"))
                                        .build())
                        .build();
        AppSearchSchema organizationSchema =
                new AppSearchSchema.Builder("Organization")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("notes")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "funder", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(
                                                Arrays.asList(
                                                        "name",
                                                        "worksFor.name",
                                                        "worksFor.funder.address",
                                                        "worksFor.funder.worksFor.notes"))
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema, organizationSchema)
                                .build())
                .get();

        // Test that documents following the circular schema are indexed correctly, and that its
        // sections are searchable
        GenericDocument person1 =
                new GenericDocument.Builder<>("namespace", "person1", "Person")
                        .setPropertyString("name", "Person1")
                        .setPropertyString("address", "someAddress")
                        .build();
        GenericDocument org1 =
                new GenericDocument.Builder<>("namespace", "org1", "Organization")
                        .setPropertyString("name", "Org1")
                        .setPropertyString("notes", "someNote")
                        .setPropertyDocument("funder", person1)
                        .build();
        GenericDocument person2 =
                new GenericDocument.Builder<>("namespace", "person2", "Person")
                        .setPropertyString("name", "Person2")
                        .setPropertyString("address", "anotherAddress")
                        .setPropertyDocument("worksFor", org1)
                        .build();
        GenericDocument org2 =
                new GenericDocument.Builder<>("namespace", "org2", "Organization")
                        .setPropertyString("name", "Org2")
                        .setPropertyString("notes", "anotherNote")
                        .setPropertyDocument("funder", person2)
                        .build();

        AppSearchBatchResult<String, Void> putResult =
                checkIsBatchResultSuccess(
                        mDb1.putAsync(
                                new PutDocumentsRequest.Builder()
                                        .addGenericDocuments(person1, org1, person2, org2)
                                        .build()));
        assertThat(putResult.getSuccesses())
                .containsExactly("person1", null, "org1", null, "person2", null, "org2", null);
        assertThat(putResult.getFailures()).isEmpty();

        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("person1", "person2", "org1", "org2")
                        .build();
        List<GenericDocument> outDocuments = doGet(mDb1, getByDocumentIdRequest);
        assertThat(outDocuments).hasSize(4);
        assertThat(outDocuments).containsExactly(person1, person2, org1, org2);

        // Indexed properties:
        // Person: 'name', 'address', 'worksFor.name', 'worksFor.notes', 'worksFor.funder.name'
        // Organization: 'name', 'notes', 'funder.name', 'funder.worksFor.name',
        //               'funder.worksFor.funder.address', 'funder.worksFor.funder.worksFor.notes'
        //
        // "Person1" should match person1 (name), org1 (funder.name) and person2
        // (worksFor.funder.name)
        SearchResults searchResults =
                mDb1.search(
                        "Person1",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(3);
        assertThat(outDocuments).containsExactly(person1, org1, person2);

        // "someAddress" should match person1 (address) and org2 (funder.worksFor.funder.address)
        searchResults =
                mDb1.search(
                        "someAddress",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person1, org2);

        // "Org1" should match org1 (name), person2 (worksFor.name) and org2 (funder.worksFor.name)
        searchResults =
                mDb1.search(
                        "Org1",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(3);
        assertThat(outDocuments).containsExactly(org1, person2, org2);

        // "someNote" should match org1 (notes) and person2 (worksFor.notes)
        searchResults =
                mDb1.search(
                        "someNote",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(org1, person2);

        // "Person2" should match person2 (name), org2 (funder.name)
        searchResults =
                mDb1.search(
                        "Person2",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(2);
        assertThat(outDocuments).containsExactly(person2, org2);

        // "anotherAddress" should match only person2 (address)
        searchResults =
                mDb1.search(
                        "anotherAddress",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(person2);

        // "Org2" and "anotherNote" should both match only org2 (name, notes)
        searchResults =
                mDb1.search(
                        "Org2",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(org2);

        searchResults =
                mDb1.search(
                        "anotherNote",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        assertThat(outDocuments).containsExactly(org2);
    }

    @Test
    public void testSetSchema_toString_containsIndexableNestedPropsList() throws Exception {
        assumeTrue(
                mDb1.getFeatures()
                        .isFeatureSupported(Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .addProperty(
                                new StringPropertyConfig.Builder("subject")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(false)
                                        .addIndexableNestedProperties(
                                                Arrays.asList(
                                                        "name", "worksFor.name", "worksFor.notes"))
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "recipient", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();
        String expectedIndexableNestedPropertyMessage =
                "indexableNestedProperties: [name, worksFor.notes, worksFor.name]";

        assertThat(emailSchema.toString()).contains(expectedIndexableNestedPropertyMessage);

    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch_simple() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setPropertyString("body", "foo")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                        new float[]{-0.1f, -0.2f, -0.3f, 0.4f, 0.5f},
                                        "my_model_v1"),
                                new EmbeddingVector(
                                        new float[]{0.6f, 0.7f, 0.8f}, "my_model_v2"))
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setPropertyString("body", "bar")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{-0.1f, 0.2f, -0.3f, -0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                new float[]{0.6f, 0.7f, -0.8f}, "my_model_v2"))
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1).build()));

        // Add an embedding search with dot product semantic scores:
        // - document 0: -0.5 (embedding1), 0.3 (embedding2)
        // - document 1: -0.9 (embedding1)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "my_model_v1");

        // Match documents that have embeddings with a similarity closer to 0 that is
        // greater than -1.
        //
        // The matched embeddings for each doc are:
        // - document 0: -0.5 (embedding1), 0.3 (embedding2)
        // - document 1: -0.9 (embedding1)
        // The scoring expression for each doc will be evaluated as:
        // - document 0: sum({-0.5, 0.3}) + sum({}) = -0.2
        // - document 1: sum({-0.9}) + sum({}) = -0.9
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setRankingStrategy(
                        "sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search(
                "semanticSearch(getEmbeddingParameter(0), -1, 1)", searchSpec);
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc0);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(-0.2);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getRankingSignal()).isWithin(0.00001).of(-0.9);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch_propertyRestriction() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setPropertyString("body", "foo")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                        new float[]{-0.1f, -0.2f, -0.3f, 0.4f, 0.5f},
                                        "my_model_v1"),
                                new EmbeddingVector(
                                        new float[]{0.6f, 0.7f, 0.8f}, "my_model_v2"))
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setPropertyString("body", "bar")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{-0.1f, 0.2f, -0.3f, -0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                new float[]{0.6f, 0.7f, -0.8f}, "my_model_v2"))
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1).build()));

        // Add an embedding search with dot product semantic scores:
        // - document 0: -0.5 (embedding1), 0.3 (embedding2)
        // - document 1: -0.9 (embedding1)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "my_model_v1");

        // Create a query similar as above but with a property restriction, which still matches
        // document 0 and document 1 but the semantic score 0.3 should be removed from document 0.
        //
        // The matched embeddings for each doc are:
        // - document 0: -0.5 (embedding1)
        // - document 1: -0.9 (embedding1)
        // The scoring expression for each doc will be evaluated as:
        // - document 0: sum({-0.5}) = -0.5
        // - document 1: sum({-0.9}) = -0.9
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setRankingStrategy("sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search(
                "embedding1:semanticSearch(getEmbeddingParameter(0), -1, 1)", searchSpec);
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc0);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(-0.5);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getRankingSignal()).isWithin(0.00001).of(-0.9);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch_multipleSearchEmbeddings() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setPropertyString("body", "foo")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                        new float[]{-0.1f, -0.2f, -0.3f, 0.4f, 0.5f},
                                        "my_model_v1"),
                                new EmbeddingVector(
                                        new float[]{0.6f, 0.7f, 0.8f}, "my_model_v2"))
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setPropertyString("body", "bar")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{-0.1f, 0.2f, -0.3f, -0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                new float[]{0.6f, 0.7f, -0.8f}, "my_model_v2"))
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1).build()));

        // Add an embedding search with dot product semantic scores:
        // - document 0: -0.5 (embedding1), 0.3 (embedding2)
        // - document 1: -0.9 (embedding1)
        EmbeddingVector searchEmbedding1 = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "my_model_v1");
        // Add an embedding search with dot product semantic scores:
        // - document 0: -0.5 (embedding2)
        // - document 1: -2.1 (embedding2)
        EmbeddingVector searchEmbedding2 = new EmbeddingVector(
                new float[]{-1, -1, 1}, "my_model_v2");

        // Create a complex query that matches all hits from all documents.
        //
        // The matched embeddings for each doc are:
        // - document 0: -0.5 (embedding1), 0.3 (embedding2), -0.5 (embedding2)
        // - document 1: -0.9 (embedding1), -2.1 (embedding2)
        // The scoring expression for each doc will be evaluated as:
        // - document 0: sum({-0.5, 0.3}) + sum({-0.5}) = -0.7
        // - document 1: sum({-0.9}) + sum({-2.1}) = -3
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding1, searchEmbedding2)
                .setRankingStrategy("sum(this.matchedSemanticScores(getEmbeddingParameter(0))) + "
                        + "sum(this.matchedSemanticScores(getEmbeddingParameter(1)))")
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search(
                "semanticSearch(getEmbeddingParameter(0)) OR "
                        + "semanticSearch(getEmbeddingParameter(1))", searchSpec);
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc0);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(-0.7);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getRankingSignal()).isWithin(0.00001).of(-3);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch_hybrid() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setPropertyString("body", "foo")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                        new float[]{-0.1f, -0.2f, -0.3f, 0.4f, 0.5f},
                                        "my_model_v1"),
                                new EmbeddingVector(
                                        new float[]{0.6f, 0.7f, 0.8f}, "my_model_v2"))
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setPropertyString("body", "bar")
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding1", new EmbeddingVector(
                                new float[]{-0.1f, 0.2f, -0.3f, -0.4f, 0.5f}, "my_model_v1"))
                        .setPropertyEmbedding("embedding2", new EmbeddingVector(
                                new float[]{0.6f, 0.7f, -0.8f}, "my_model_v2"))
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1).build()));

        // Add an embedding search with dot product semantic scores:
        // - document 0: -0.5 (embedding2)
        // - document 1: -2.1 (embedding2)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{-1, -1, 1}, "my_model_v2");

        // Create a hybrid query that matches document 0 because of term-based search
        // and document 1 because of embedding-based search.
        //
        // The matched embeddings for each doc are:
        // - document 1: -2.1 (embedding2)
        // The scoring expression for each doc will be evaluated as:
        // - document 0: sum({}) = 0
        // - document 1: sum({-2.1}) = -2.1
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setRankingStrategy("sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search(
                "foo OR semanticSearch(getEmbeddingParameter(0), -10, -1)", searchSpec);
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc0);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(0);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getRankingSignal()).isWithin(0.00001).of(-2.1);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public void testEmbeddingSearch_notSupported() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        assumeFalse(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{-1, -1, 1}, "my_model_v2");
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .addEmbeddingParameters(searchEmbedding)
                .build();
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.search("semanticSearch(getEmbeddingParameter(0), -1, 1)", searchSpec));
        assertThat(exception).hasMessageThat().contains(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public void testSearchSpecStrings_simple() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(
                        Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setPropertyString("body", "foo bar")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setPropertyString("body", "bar")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "Email")
                        .setPropertyString("body", "foo")
                        .setCreationTimestampMillis(1000)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1, doc2).build()));

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("foo.")
                .build();
        SearchResults searchResults = mDb1.search("getSearchStringParameter(0)", searchSpec);
        List<GenericDocument> results = convertSearchResultsToDocuments(searchResults);
        assertThat(results).containsExactly(doc2, doc0);

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("bar, foo")
                .build();
        searchResults = mDb1.search("getSearchStringParameter(0)", searchSpec);
        results = convertSearchResultsToDocuments(searchResults);
        assertThat(results).containsExactly(doc0);

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("\\\"bar, \\\"foo\\\"")
                .build();
        searchResults = mDb1.search("getSearchStringParameter(0)", searchSpec);
        results = convertSearchResultsToDocuments(searchResults);
        assertThat(results).containsExactly(doc0);

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("bar ) foo")
                .build();
        searchResults = mDb1.search("getSearchStringParameter(0)", searchSpec);
        results = convertSearchResultsToDocuments(searchResults);
        assertThat(results).containsExactly(doc0);

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("bar foo(")
                .build();
        searchResults = mDb1.search("getSearchStringParameter(0)", searchSpec);
        results = convertSearchResultsToDocuments(searchResults);
        assertThat(results).containsExactly(doc0);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public void testSearchSpecString_notSupported() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        assumeFalse(
                mDb1.getFeatures().isFeatureSupported(
                        Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .addSearchStringParameters("bar foo(")
                .build();
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.search("getSearchStringParameter(0)", searchSpec));
        assertThat(exception).hasMessageThat().contains(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled({
            Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS,
            Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG})
    public void testInformationalRankingExpressions() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        final int doc0DocScore = 2;
        GenericDocument doc0 =
                new GenericDocument.Builder<>("namespace", "id0", "Email")
                        .setScore(doc0DocScore)
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding", new EmbeddingVector(
                                new float[]{-0.1f, -0.2f, -0.3f, -0.4f, -0.5f}, "my_model"))
                        .build();
        final int doc1DocScore = 3;
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Email")
                        .setScore(doc1DocScore)
                        .setCreationTimestampMillis(1000)
                        .setPropertyEmbedding("embedding", new EmbeddingVector(
                                        new float[]{-0.1f, 0.2f, -0.3f, -0.4f, 0.5f}, "my_model"),
                                new EmbeddingVector(
                                        new float[]{-0.1f, -0.2f, -0.3f, -0.4f, -0.5f}, "my_model"))
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc0, doc1).build()));

        // Add an embedding search with dot product semantic scores:
        // - document 0: 0.5
        // - document 1: -0.9, 0.5
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "my_model");

        // Make an embedding query that matches all documents.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setRankingStrategy(
                        "sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                .addInformationalRankingExpressions(
                        "len(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                .addInformationalRankingExpressions("this.documentScore()")
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SearchResults searchResults = mDb1.search(
                "semanticSearch(getEmbeddingParameter(0))", searchSpec);
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(2);
        // doc0:
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc0);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(0.5);
        // doc0 has 1 embedding vector and a document score of 2.
        assertThat(results.get(0).getInformationalRankingSignals())
                .containsExactly(1.0, (double) doc0DocScore).inOrder();

        // doc1:
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getRankingSignal()).isWithin(0.00001).of(-0.9 + 0.5);
        // doc1 has 2 embedding vectors and a document score of 3.
        assertThat(results.get(1).getInformationalRankingSignals())
                .containsExactly(2.0, (double) doc1DocScore).inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public void testInformationalRankingExpressions_notSupported() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS));

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRankingStrategy("this.documentScore() + 1")
                .addInformationalRankingExpressions("this.documentScore()")
                .build();
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mDb1.search("foo", searchSpec));
        assertThat(exception).hasMessageThat().contains(
                Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS
                + " are not available on this AppSearch implementation.");
    }

    @Test
    public void testPutDocuments_emptyBytesAndDocuments() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schema, AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "testSchema")
                .setPropertyBytes("bytes")
                .setPropertyDocument("document")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1")
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);
        assertThat(outDocuments).hasSize(1);
        GenericDocument outDocument = outDocuments.get(0);
        assertThat(outDocument.getPropertyBytesArray("bytes")).isEmpty();
        assertThat(outDocument.getPropertyDocumentArray("document")).isEmpty();
    }
}
