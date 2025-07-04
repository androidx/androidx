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
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/** This class holds all tests that won't be exported to the framework.  */
public abstract class AppSearchSessionInternalTestBase {

    static final String DB_NAME_1 = "";

    protected AppSearchSession mDb1;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

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

    // TODO(b/268521214): Move test to cts once deletion propagation is available in framework.
    @Test
    public void testGetSchema_joinableValueType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
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
                                        .build())
                        .build();

        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    // TODO(b/371610934): Remove this test once GenericDocument#setParentTypes is removed.
    @Test
    @SuppressWarnings("deprecation")
    public void testQuery_genericDocumentWrapsParentTypeForPolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        // When SearchResult does not wrap parent information, GenericDocument should do.
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_RESULT_PARENT_TYPES));

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
        AppSearchSchema musicianSchema =
                new AppSearchSchema.Builder("Musician")
                        .addParentType("Artist")
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
                                        "receivers", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .addSchemas(musicianSchema)
                                .addSchemas(messageSchema)
                                .build())
                .get();

        // Index documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "person")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "artist")
                        .setPropertyString("company", "foo")
                        .build();
        GenericDocument musicianDoc =
                new GenericDocument.Builder<>("namespace", "id3", "Musician")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "musician")
                        .setPropertyString("company", "foo")
                        .build();
        GenericDocument messageDoc =
                new GenericDocument.Builder<>("namespace", "id4", "Message")
                        .setCreationTimestampMillis(1000)
                        .setPropertyDocument("receivers", artistDoc, musicianDoc)
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc, musicianDoc, messageDoc)
                                .build()));
        GenericDocument artistDocWithParent =
                new GenericDocument.Builder<>(artistDoc).setParentTypes(
                        Collections.singletonList("Person")).build();
        GenericDocument musicianDocWithParent =
                new GenericDocument.Builder<>(musicianDoc).setParentTypes(
                        ImmutableList.of("Artist", "Person")).build();
        GenericDocument messageDocWithParent =
                new GenericDocument.Builder<>("namespace", "id4", "Message")
                        .setCreationTimestampMillis(1000)
                        .setPropertyDocument("receivers", artistDocWithParent,
                                musicianDocWithParent)
                        .build();

        // Query to get all the documents
        SearchResults searchResults =
                mDb1.search("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(personDoc, artistDocWithParent, musicianDocWithParent,
                messageDocWithParent);
    }

    // TODO(b/384947619): move delete propagation tests back to AppSearchSessionCtsTestBase once the
    //   API is ready.
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testGetSchema_deletePropagationTypePropagateFrom() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));

        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("normalStr")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setDeletePropagationType(
                                StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
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
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testGetSchema_deletePropagationTypeNoneWithNonJoinable_succeeds() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("optionalString")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .setDeletePropagationType(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredString")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .setDeletePropagationType(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("repeatedString")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .setDeletePropagationType(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
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
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testGetSchema_deletePropagationTypeNoneWithJoinable_succeeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("optionalString")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setDeletePropagationType(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredString")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setDeletePropagationType(StringPropertyConfig.DELETE_PROPAGATION_TYPE_NONE)
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
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testRemove_withDeletePropagationFromParentToChildren() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));

        // Person (parent) schema.
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(
                        new StringPropertyConfig.Builder("name")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .build();
        // Email (child) schema: "sender" has delete propagation type PROPAGATE_FROM, and "receiver"
        // doesn't have delete propagation.
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(
                        new StringPropertyConfig.Builder("subject")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("sender")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .setDeletePropagationType(
                                        StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("receiver")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .build())
                .build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(personSchema, emailSchema)
                        .build()).get();

        // Put 1 person and 2 email documents.
        GenericDocument person =
                new GenericDocument.Builder<>("namespace", "person", "Person")
                        .setPropertyString("name", "test person")
                        .build();
        String personQualifiedId = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, "namespace", "person");
        GenericDocument email1 =
                new GenericDocument.Builder<>("namespace", "email1", "Email")
                        .setPropertyString("subject", "test email subject")
                        .setPropertyString("sender", personQualifiedId)
                        .build();
        GenericDocument email2 =
                new GenericDocument.Builder<>("namespace", "email2", "Email")
                        .setPropertyString("subject", "test email subject")
                        .setPropertyString("receiver", personQualifiedId)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(person, email1, email2)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "person")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "email1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "email2")).hasSize(1);

        // Delete the person (parent) document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("person").build()));

        // Verify that:
        // - Person document is deleted.
        // - Email1 document is also deleted due to the delete propagation via "sender".
        // - Email2 document is still present since "receiver" does not have delete propagation.
        AppSearchBatchResult<String, GenericDocument> getResult1 =
                mDb1.getByDocumentIdAsync(
                                new GetByDocumentIdRequest.Builder("namespace").addIds(
                                        "person", "email1").build())
                        .get();
        assertThat(getResult1.isSuccess()).isFalse();
        assertThat(getResult1.getFailures()).hasSize(2);
        assertThat(getResult1.getFailures().get("person").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult1.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        AppSearchBatchResult<String, GenericDocument> getResult2 =
                mDb1.getByDocumentIdAsync(
                                new GetByDocumentIdRequest.Builder("namespace").addIds(
                                        "email2").build())
                        .get();
        assertThat(getResult2.isSuccess()).isTrue();
        assertThat(getResult2.getSuccesses()).hasSize(1);
        assertThat(getResult2.getSuccesses().get("email2")).isEqualTo(email2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testRemove_withDeletePropagationFromParentToGrandchildren() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));

        // Person (parent) schema.
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(
                        new StringPropertyConfig.Builder("name")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .build();
        // Email (child) schema: "sender" has delete propagation type PROPAGATE_FROM, and "receiver"
        // doesn't have delete propagation.
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(
                        new StringPropertyConfig.Builder("subject")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("sender")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .setDeletePropagationType(
                                        StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("receiver")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .build())
                .build();

        // Label (grandchild) schema: "object" has delete propagation type PROPAGATE_FROM, and
        // "softLink" doesn't have delete propagation.
        AppSearchSchema labelSchema = new AppSearchSchema.Builder("Label")
                .addProperty(
                        new StringPropertyConfig.Builder("text")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("object")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .setDeletePropagationType(
                                        StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("softLink")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .build())
                .build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(personSchema, emailSchema, labelSchema)
                        .build()).get();

        // Put 1 person, 2 email, and 4 label documents with the following relations:
        //
        //                           ("object") - label1
        //                         /
        //               email1 <-
        //             /           \
        //       ("sender")          ("softLink") - label2
        //           /
        // person <-
        //           \
        //       ("receiver")        ("object") - label3
        //             \           /
        //               email2 <-
        //                         \
        //                           ("softLink") - label4
        GenericDocument person =
                new GenericDocument.Builder<>("namespace", "person", "Person")
                        .setPropertyString("name", "test person")
                        .build();
        String personQualifiedId = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, "namespace", "person");

        GenericDocument email1 =
                new GenericDocument.Builder<>("namespace", "email1", "Email")
                        .setPropertyString("subject", "test email subject")
                        .setPropertyString("sender", personQualifiedId)
                        .build();
        GenericDocument email2 =
                new GenericDocument.Builder<>("namespace", "email2", "Email")
                        .setPropertyString("subject", "test email subject")
                        .setPropertyString("receiver", personQualifiedId)
                        .build();
        String emailQualifiedId1 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, "namespace", "email1");
        String emailQualifiedId2 = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, "namespace", "email2");

        GenericDocument label1 =
                new GenericDocument.Builder<>("namespace", "label1", "Label")
                        .setPropertyString("text", "label1")
                        .setPropertyString("object", emailQualifiedId1)
                        .build();
        GenericDocument label2 =
                new GenericDocument.Builder<>("namespace", "label2", "Label")
                        .setPropertyString("text", "label2")
                        .setPropertyString("softLink", emailQualifiedId1)
                        .build();
        GenericDocument label3 =
                new GenericDocument.Builder<>("namespace", "label3", "Label")
                        .setPropertyString("text", "label3")
                        .setPropertyString("object", emailQualifiedId2)
                        .build();
        GenericDocument label4 =
                new GenericDocument.Builder<>("namespace", "label4", "Label")
                        .setPropertyString("text", "label4")
                        .setPropertyString("softLink", emailQualifiedId2)
                        .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(
                                person, email1, email2, label1, label2, label3, label4)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "person")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "email1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "email2")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "label1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "label2")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "label3")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "label4")).hasSize(1);

        // Delete the person (parent) document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("person").build()));

        // Verify that:
        // - Person document is deleted.
        // - Email1 document is also deleted due to the delete propagation via "sender".
        // - Label1 document is also deleted due to the delete propagation via "object".
        // - Label2 document is still present since "softLink" does not have delete propagation.
        // - Email2 document is still present since "receiver" does not have delete propagation.
        // - Label3 document is still present since Email2 is not deleted.
        // - Label4 document is still present since Email2 is not deleted.
        AppSearchBatchResult<String, GenericDocument> getResult1 =
                mDb1.getByDocumentIdAsync(
                                new GetByDocumentIdRequest.Builder("namespace").addIds(
                                        "person", "email1", "label1").build())
                        .get();
        assertThat(getResult1.isSuccess()).isFalse();
        assertThat(getResult1.getFailures()).hasSize(3);
        assertThat(getResult1.getFailures().get("person").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult1.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult1.getFailures().get("label1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        AppSearchBatchResult<String, GenericDocument> getResult2 =
                mDb1.getByDocumentIdAsync(
                                new GetByDocumentIdRequest.Builder("namespace").addIds(
                                        "email2", "label2", "label3", "label4").build())
                        .get();
        assertThat(getResult2.isSuccess()).isTrue();
        assertThat(getResult2.getSuccesses()).hasSize(4);
        assertThat(getResult2.getSuccesses().get("email2")).isEqualTo(email2);
        assertThat(getResult2.getSuccesses().get("label2")).isEqualTo(label2);
        assertThat(getResult2.getSuccesses().get("label3")).isEqualTo(label3);
        assertThat(getResult2.getSuccesses().get("label4")).isEqualTo(label4);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testRemove_withDeletePropagationFromParentToChildren_fromMultipleProperties()
            throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));

        // Person (parent) schema.
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(
                        new StringPropertyConfig.Builder("name")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .build();
        // Email (child) schema: "sender" has delete propagation type PROPAGATE_FROM, and "receiver"
        // doesn't have delete propagation.
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(
                        new StringPropertyConfig.Builder("subject")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("sender")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .setDeletePropagationType(
                                        StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                                .build())
                .addProperty(
                        new StringPropertyConfig.Builder("receiver")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setJoinableValueType(
                                        StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                .build())
                .build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(personSchema, emailSchema)
                        .build()).get();

        // Put 1 person and 1 email document.
        // Email document has both "sender" and "receiver" referring to the person document.
        GenericDocument person =
                new GenericDocument.Builder<>("namespace", "person", "Person")
                        .setPropertyString("name", "test person")
                        .build();
        String personQualifiedId = DocumentIdUtil.createQualifiedId(
                mContext.getPackageName(), DB_NAME_1, "namespace", "person");
        GenericDocument email =
                new GenericDocument.Builder<>("namespace", "email", "Email")
                        .setPropertyString("subject", "test email subject")
                        .setPropertyString("sender", personQualifiedId)
                        .setPropertyString("receiver", personQualifiedId)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(person, email)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "person")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "email")).hasSize(1);

        // Delete the person (parent) document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("person").build()));

        // Verify that:
        // - Person document is deleted.
        // - Email document is also deleted since there is at least one property ("sender") with
        //   DELETE_PROPAGATION_TYPE_PROPAGATE_FROM.
        AppSearchBatchResult<String, GenericDocument> getResult1 =
                mDb1.getByDocumentIdAsync(
                                new GetByDocumentIdRequest.Builder("namespace").addIds(
                                        "person", "email").build())
                        .get();
        assertThat(getResult1.isSuccess()).isFalse();
        assertThat(getResult1.getFailures()).hasSize(2);
        assertThat(getResult1.getFailures().get("person").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult1.getFailures().get("email").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }
}
