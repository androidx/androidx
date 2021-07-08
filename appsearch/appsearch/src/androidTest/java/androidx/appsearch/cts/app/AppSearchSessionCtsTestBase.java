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

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_SCHEMA;
import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;
import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.app.util.AppSearchTestUtils.doGet;
import static androidx.appsearch.app.util.AppSearchTestUtils.retrieveAllSearchResults;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.app.util.AppSearchEmail;
import androidx.appsearch.cts.app.customer.EmailDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class AppSearchSessionCtsTestBase {
    private static final String DB_NAME_1 = "";
    private static final String DB_NAME_2 = "testDb2";

    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    protected abstract ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        mDb1 = createSearchSession(DB_NAME_1).get();
        mDb2 = createSearchSession(DB_NAME_2).get();

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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchema(
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
    }

    @Test
    public void testSetSchema_Failure() throws Exception {
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .build();

        Throwable throwable = assertThrows(ExecutionException.class,
                () -> mDb1.setSchema(new SetSchemaRequest.Builder()
                        .addSchemas(emailSchema1).build()).get()).getCause();
        assertThat(throwable).isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) throwable;
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Incompatible types: {builtin:Email}");

        throwable = assertThrows(ExecutionException.class,
                () -> mDb1.setSchema(new SetSchemaRequest.Builder().build()).get()).getCause();

        assertThat(throwable).isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) throwable;
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Deleted types: {builtin:Email}");
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

        mDb1.setSchema(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(1).build()).get();

        Set<AppSearchSchema> actualSchemaTypes = mDb1.getSchema().get().getSchemas();
        assertThat(actualSchemaTypes).containsExactly(schema);

        // increase version number
        mDb1.setSchema(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(2).build()).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchema().get();
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
        mDb1.setSchema(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(135).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(246).build()).get();


        // check the version has been set correctly.
        GetSchemaResponse getSchemaResponse = mDb1.getSchema().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(135);

        getSchemaResponse = mDb2.getSchema().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(246);
    }

// @exportToFramework:startStrip()

    @Test
    public void testSetSchema_addDocumentClasses() throws Exception {
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();
    }
// @exportToFramework:endStrip()

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

        mDb1.setSchema(request1).get();
        mDb2.setSchema(request2).get();

        Set<AppSearchSchema> actual1 = mDb1.getSchema().get().getSchemas();
        assertThat(actual1).hasSize(2);
        assertThat(actual1).isEqualTo(request1.getSchemas());
        Set<AppSearchSchema> actual2 = mDb2.getSchema().get().getSchemas();
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
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(inSchema, AppSearchEmail.SCHEMA).build()).get();
        GetSchemaResponse response = mDb1.getSchema().get();
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
    public void testGetNamespaces() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespaces().get()).isEmpty();

        // Index a document
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1", "id1").build())
                .build()));
        assertThat(mDb1.getNamespaces().get()).containsExactly("namespace1");

        // Index additional data
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(
                        new AppSearchEmail.Builder("namespace2", "id1").build(),
                        new AppSearchEmail.Builder("namespace2", "id2").build(),
                        new AppSearchEmail.Builder("namespace3", "id1").build())
                .build()));
        assertThat(mDb1.getNamespaces().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id2 -- namespace2 should still exist because of namespace2/id1
        checkIsBatchResultSuccess(
                mDb1.remove(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id2").build()));
        assertThat(mDb1.getNamespaces().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id1 -- namespace2 should now be gone
        checkIsBatchResultSuccess(
                mDb1.remove(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id1").build()));
        assertThat(mDb1.getNamespaces().get()).containsExactly("namespace1", "namespace3");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSession(DB_NAME_1).get();
        assertThat(mDb1.getNamespaces().get()).containsExactly("namespace1", "namespace3");
    }

    @Test
    public void testGetNamespaces_dbIsolation() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespaces().get()).isEmpty();
        assertThat(mDb2.getNamespaces().get()).isEmpty();

        // Index documents
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace2_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb2.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace_db2", "id1").build())
                .build()));
        assertThat(mDb1.getNamespaces().get()).containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespaces().get()).containsExactly("namespace_db2");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSession(DB_NAME_1).get();
        assertThat(mDb1.getNamespaces().get()).containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespaces().get()).containsExactly("namespace_db2");
    }

    @Test
    public void testGetSchema_emptyDB() throws Exception {
        GetSchemaResponse getSchemaResponse = mDb1.getSchema().get();
        assertThat(getSchemaResponse.getVersion()).isEqualTo(0);
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutLargeDocument() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        char[] chars = new char[16_000_000];
        Arrays.fill(chars, ' ');
        String body = new StringBuilder().append(chars).append("the end.").toString();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody(body)
                .build();
        AppSearchBatchResult<String, Void> result = mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();
        assertThat(result.isSuccess()).isTrue();

        SearchResults searchResults = mDb1.search("end", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> outDocuments = convertSearchResultsToDocuments(searchResults);
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);
    }

    @Test
    public void testPutLargeDocument_exceedLimit() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Create a String property that make the document exceed the total size limit.
        char[] chars = new char[17_000_000];
        String body = new StringBuilder().append(chars).toString();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody(body)
                .build();
        AppSearchBatchResult<String, Void> result = mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();
        assertThat(result.getFailures()).containsKey("id1");
        assertThat(result.getFailures().get("id1").getErrorMessage())
                .contains("was too large to write. Max is 16777215");
    }

// @exportToFramework:startStrip()

    @Test
    public void testPut_addDocumentClasses() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument email = new EmailDocument();
        email.namespace = "namespace";
        email.id = "id1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }
// @exportToFramework:endStrip()

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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(oldEmailSchema).build()).get();

        // Try to index a gift. This should fail as it's not in the schema.
        GenericDocument gift =
                new GenericDocument.Builder<>("namespace", "gift1", "Gift").setPropertyLong("price",
                        5).build();
        AppSearchBatchResult<String, Void> result =
                mDb1.put(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()).get();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailures().get("gift1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Update the schema to include the gift and update email with a new field
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(newEmailSchema, giftSchema).build()).get();

        // Try to index the document again, which should now work
        checkIsBatchResultSuccess(
                mDb1.put(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()));

        // Indexing an email with a body should also work
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();
        checkIsBatchResultSuccess(
                mDb1.put(
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
        mDb1.setSchema(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present.
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.put(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, "namespace", "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);

        // Try to remove the email schema. This should fail as it's an incompatible change.
        Throwable failResult1 = assertThrows(
                ExecutionException.class,
                () -> mDb1.setSchema(new SetSchemaRequest.Builder().build()).get()).getCause();
        assertThat(failResult1).isInstanceOf(AppSearchException.class);
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
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
        AppSearchBatchResult<String, Void> failResult2 = mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email2").getErrorMessage())
                .isEqualTo("Schema type config 'androidx.appsearch.test$" + DB_NAME_1
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
        mDb1.setSchema(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present in database1.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.put(
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
                mDb2.put(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Try to remove the email schema in database1. This should fail as it's an incompatible
        // change.
        Throwable failResult1 = assertThrows(
                ExecutionException.class,
                () -> mDb1.setSchema(new SetSchemaRequest.Builder().build()).get()).getCause();
        assertThat(failResult1).isInstanceOf(AppSearchException.class);
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone in database 1.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("email1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace", "email3")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email3).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email3").getErrorMessage())
                .isEqualTo("Schema type config 'androidx.appsearch.test$" + DB_NAME_1
                        + "/builtin:Email' not found");

        // Make sure email in database 2 still present.
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Make sure email could still be indexed in database 2.
        checkIsBatchResultSuccess(
                mDb2.put(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
    }

    @Test
    public void testGetDocuments() throws Exception {
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

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, "namespace", "id1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult = mDb2.getByDocumentId(
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
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument inEmail = new EmailDocument();
        inEmail.namespace = "namespace";
        inEmail.id = "id1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(putDocumentsRequestBuilder.build()));

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
            results = searchResults.getNextPage().get();
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
    public void testQuery_relevanceScoring() throws Exception {
        // Schema registration
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
    public void testQuery_typeFilter() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
    }

    @Test
    public void testQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
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
        List<SearchResult> results = searchResults.getNextPage().get();
        assertThat(results).isEmpty();
    }

    @Test
    public void testQuery_namespaceFilter() throws Exception {
        // Schema registration
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
    }

    @Test
    public void testQuery_getPackageName() throws Exception {
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
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPage().get();
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
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPage().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getDatabaseName()).isEqualTo(DB_NAME_1);
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);

        // Schema registration for another database
        mDb2.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        checkIsBatchResultSuccess(mDb2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        searchResults = mDb2.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPage().get();
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(
                        SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, Collections.emptyList())
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType", []}, {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(
                        SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
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
    public void testQuery_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

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
    public void testSnippet() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "Generic")
                        .setPropertyString("subject", "A commonly used fake word is foo. "
                                + "Another nonsense word that’s used a lot is bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("foo",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setMaxSnippetSize(10)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPage().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo("A commonly used fake word is foo. "
                + "Another nonsense word that’s used a lot is bar");
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29,  /*upper=*/32));
        assertThat(matchInfo.getExactMatch()).isEqualTo("foo");
        assertThat(matchInfo.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/26,  /*upper=*/33));
        assertThat(matchInfo.getSnippet()).isEqualTo("is foo.");
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index documents
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder().addGenericDocuments(
                new GenericDocument.Builder<>("namespace", "id1", "Generic")
                        .setPropertyString(
                                "subject",
                                "I like cats", "I like dogs", "I like birds", "I like fish")
                        .setScore(10)
                        .build(),
                new GenericDocument.Builder<>("namespace", "id2", "Generic")
                        .setPropertyString(
                                "subject",
                                "I like red", "I like green", "I like blue", "I like yellow")
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
        List<SearchResult> results = searchResults.getNextPage().get();
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("は",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPage().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo(japanese);
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/44,  /*upper=*/45));
        assertThat(matchInfo.getExactMatch()).isEqualTo("は");
    }

    @Test
    public void testRemove() throws Exception {
        // Schema registration
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1",
                        "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Test if we delete a nonexistent id.
        AppSearchBatchResult<String, Void> deleteResult = mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()).get();

        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_multipleIds() throws Exception {
        // Schema registration
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the email 1 by query "foo"
        mDb1.remove("foo",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Delete the email 2 by query "bar"
        mDb1.remove("bar",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Try to delete email with query "foo", but restricted to a different package name.
        // Won't work and email will still exist.
        mDb1.remove("foo",
                new SearchSpec.Builder().setTermMatch(
                        SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                        "some.other.package").build()).get();
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the email by query "foo", restricted to the correct package this time.
        mDb1.remove("foo", new SearchSpec.Builder().setTermMatch(
                SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                ApplicationProvider.getApplicationContext().getPackageName()).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult = mDb2.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent id.
        deleteResult = mDb1.remove(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByTypes() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic").build();
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1", "id2", "id3")).hasSize(3);

        // Delete the email type
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
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
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the email type in instance 1
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentId(
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
        mDb1.setSchema(
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1", "id2")).hasSize(2);
        assertThat(doGet(mDb1, /*namespace=*/"document", "id3")).hasSize(1);

        // Delete the email namespace
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1", "id2").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("document")
                        .addIds("id3").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByNamespaces_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1")).hasSize(1);
        assertThat(doGet(mDb2, /*namespace=*/"email", "id2")).hasSize(1);

        // Delete the email namespace in instance 1
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentId(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the all document in instance 1
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_termMatchType() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));
        checkIsBatchResultSuccess(mDb2.put(
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
        mDb1.remove("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();
        searchResults = mDb1.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Delete the all document in instance 2 with TERM_MATCH_EXACT_ONLY
        mDb2.remove("",
                new SearchSpec.Builder()
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
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Remove the document
        checkIsBatchResultSuccess(
                mDb1.remove(new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Delete the all documents
        mDb1.remove(
                "", new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build())
                .get();

        // Make sure it's still gone
        getResult = mDb1.getByDocumentId(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testCloseAndReopen() throws Exception {
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

        // close and re-open the appSearchSession
        mDb1.close();
        mDb1 = createSearchSession(DB_NAME_1).get();

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
        AppSearchSession sameThreadDb = createSearchSession(
                "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();

        try {
            // Schema registration -- just mutate something
            sameThreadDb.setSchema(
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
            AppSearchSession reopen = createSearchSession(
                    "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();
            reopen.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        }
    }

    @Test
    public void testReportUsage() throws Exception {
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Email 1 has more usages, but email 2 has more recent usages.
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(1000).build()).get();
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(2000).build()).get();
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(3000).build()).get();
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id2")
                .setUsageTimestampMillis(10000).build()).get();
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id2")
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
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Use the correct namespace; it works
        mDb1.reportUsage(new ReportUsageRequest.Builder("namespace", "id1").build()).get();

        // Use an incorrect namespace; it fails
        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> mDb1.reportUsage(
                        new ReportUsageRequest.Builder("namespace2", "id1").build()).get());
        assertThat(e).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException cause = (AppSearchException) e.getCause();
        assertThat(cause.getResultCode()).isEqualTo(RESULT_NOT_FOUND);
    }

    @Test
    public void testGetStorageInfo() throws Exception {
        StorageInfo storageInfo = mDb1.getStorageInfo().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Still no storage space attributed with just a schema
        storageInfo = mDb1.getStorageInfo().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        // Index two documents.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace1", "id1").build();
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace1", "id2").build();
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace2", "id1").build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2,
                        email3).build()));

        // Non-zero size now
        storageInfo = mDb1.getStorageInfo().get();
        assertThat(storageInfo.getSizeBytes()).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(3);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(2);
    }

    @Test
    public void testFlush() throws Exception {
        // Schema registration
        mDb1.setSchema(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        // The future returned from requestFlush will be set as a void or an Exception on error.
        mDb1.requestFlush().get();
    }

    @Test
    public void testQuery_ResultGroupingLimits() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index four documents.
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
                new AppSearchEmail.Builder("namespace1", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace2", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));

        // Query with per package result grouping. Only the last document 'email4' should be
        // returned.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4);

        // Query with per namespace result grouping. Only the last document in each namespace should
        // be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(
                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE, /*resultLimit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);

        // Query with per package and per namespace result grouping. Only the last document in each
        // namespace should be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(
                        SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                                | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);
    }

    @Test
    public void testIndexNestedDocuments() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
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
        checkIsBatchResultSuccess(mDb1.put(new PutDocumentsRequest.Builder()
                .addGenericDocuments(yesNestedIndex, noNestedIndex).build()));

        // Query.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setSnippetCount(10)
                .setSnippetCountPerProperty(10)
                .build());
        List<SearchResult> page = searchResults.getNextPage().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument()).isEqualTo(yesNestedIndex);
        List<SearchResult.MatchInfo> matches = page.get(0).getMatchInfos();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getPropertyPath()).isEqualTo("prop.subject");
        assertThat(matches.get(0).getFullText()).isEqualTo("This is the body");
        assertThat(matches.get(0).getExactMatch()).isEqualTo("body");
    }

    @Test
    public void testCJKTQuery() throws Exception {
        // Schema registration
        mDb1.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "uri1")
                        .setBody("他是個男孩 is a boy")
                        .build();
        checkIsBatchResultSuccess(mDb1.put(
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
}
