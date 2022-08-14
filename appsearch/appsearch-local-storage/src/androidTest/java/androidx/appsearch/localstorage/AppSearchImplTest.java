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

package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.localstorage.util.PrefixUtil.addPrefixToDocument;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.FlakyTest;

import com.google.android.icing.proto.DebugInfoProto;
import com.google.android.icing.proto.DebugInfoVerbosity;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.StatusProto;
import com.google.android.icing.proto.StorageInfoProto;
import com.google.android.icing.proto.StringIndexingConfig;
import com.google.android.icing.proto.TermMatchType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("GuardedBy")
public class AppSearchImplTest {
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mAppSearchDir;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    // The caller access for this package
    private final CallerAccess mSelfCallerAccess = new CallerAccess(mContext.getPackageName());

    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        mAppSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    /**
     * Ensure that we can rewrite an incoming schema type by adding the database as a prefix. While
     * also keeping any other existing schema types that may already be part of Icing's persisted
     * schema.
     */
    @Test
    public void testRewriteSchema_addType() throws Exception {
        SchemaProto.Builder existingSchemaBuilder = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$existingDatabase/Foo").build());

        // Create a copy so we can modify it.
        List<SchemaTypeConfigProto> existingTypes =
                new ArrayList<>(existingSchemaBuilder.getTypesList());
        SchemaTypeConfigProto schemaTypeConfigProto1 = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("Foo").build();
        SchemaTypeConfigProto schemaTypeConfigProto2 = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("TestType")
                .addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("subject")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setStringIndexingConfig(StringIndexingConfig.newBuilder()
                                .setTokenizerType(
                                        StringIndexingConfig.TokenizerType.Code.PLAIN)
                                .setTermMatchType(TermMatchType.Code.PREFIX)
                                .build()
                        ).build()
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("link")
                        .setDataType(PropertyConfigProto.DataType.Code.DOCUMENT)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setSchemaType("RefType")
                        .build()
                ).build();
        SchemaTypeConfigProto schemaTypeConfigProto3 = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("RefType").build();
        SchemaProto newSchema = SchemaProto.newBuilder()
                .addTypes(schemaTypeConfigProto1)
                .addTypes(schemaTypeConfigProto2)
                .addTypes(schemaTypeConfigProto3)
                .build();

        AppSearchImpl.RewrittenSchemaResults rewrittenSchemaResults = AppSearchImpl.rewriteSchema(
                createPrefix("package", "newDatabase"), existingSchemaBuilder,
                newSchema);

        // We rewrote all the new types that were added. And nothing was removed.
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet()).containsExactly(
                "package$newDatabase/Foo", "package$newDatabase/TestType",
                "package$newDatabase/RefType");
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.get(
                "package$newDatabase/Foo").getSchemaType()).isEqualTo(
                "package$newDatabase/Foo");
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.get(
                "package$newDatabase/TestType").getSchemaType()).isEqualTo(
                "package$newDatabase/TestType");
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.get(
                "package$newDatabase/RefType").getSchemaType()).isEqualTo(
                "package$newDatabase/RefType");
        assertThat(rewrittenSchemaResults.mDeletedPrefixedTypes).isEmpty();

        SchemaProto expectedSchema = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$newDatabase/Foo").build())
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$newDatabase/TestType")
                        .addProperties(PropertyConfigProto.newBuilder()
                                .setPropertyName("subject")
                                .setDataType(PropertyConfigProto.DataType.Code.STRING)
                                .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                .setStringIndexingConfig(StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                                        .build()
                                ).build()
                        ).addProperties(PropertyConfigProto.newBuilder()
                                .setPropertyName("link")
                                .setDataType(PropertyConfigProto.DataType.Code.DOCUMENT)
                                .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                .setSchemaType("package$newDatabase/RefType")
                                .build()
                        ).build())
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$newDatabase/RefType").build())
                .build();

        existingTypes.addAll(expectedSchema.getTypesList());
        assertThat(existingSchemaBuilder.getTypesList()).containsExactlyElementsIn(existingTypes);
    }

    /**
     * Ensure that we track all types that were rewritten in the input schema. Even if they were
     * not technically "added" to the existing schema.
     */
    @Test
    public void testRewriteSchema_rewriteType() throws Exception {
        SchemaProto.Builder existingSchemaBuilder = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$existingDatabase/Foo").build());

        SchemaProto newSchema = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("Foo").build())
                .build();

        AppSearchImpl.RewrittenSchemaResults rewrittenSchemaResults = AppSearchImpl.rewriteSchema(
                createPrefix("package", "existingDatabase"), existingSchemaBuilder,
                newSchema);

        // Nothing was removed, but the method did rewrite the type name.
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet()).containsExactly(
                "package$existingDatabase/Foo");
        assertThat(rewrittenSchemaResults.mDeletedPrefixedTypes).isEmpty();

        // Same schema since nothing was added.
        SchemaProto expectedSchema = existingSchemaBuilder.build();
        assertThat(existingSchemaBuilder.getTypesList())
                .containsExactlyElementsIn(expectedSchema.getTypesList());
    }

    /**
     * Ensure that we track which types from the existing schema are deleted when a new schema is
     * set.
     */
    @Test
    public void testRewriteSchema_deleteType() throws Exception {
        SchemaProto.Builder existingSchemaBuilder = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$existingDatabase/Foo").build());

        SchemaProto newSchema = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("Bar").build())
                .build();

        AppSearchImpl.RewrittenSchemaResults rewrittenSchemaResults = AppSearchImpl.rewriteSchema(
                createPrefix("package", "existingDatabase"), existingSchemaBuilder,
                newSchema);

        // Bar type was rewritten, but Foo ended up being deleted since it wasn't included in the
        // new schema.
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes)
                .containsKey("package$existingDatabase/Bar");
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet().size()).isEqualTo(1);
        assertThat(rewrittenSchemaResults.mDeletedPrefixedTypes)
                .containsExactly("package$existingDatabase/Foo");

        // Same schema since nothing was added.
        SchemaProto expectedSchema = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$existingDatabase/Bar").build())
                .build();

        assertThat(existingSchemaBuilder.getTypesList())
                .containsExactlyElementsIn(expectedSchema.getTypesList());
    }

    @Test
    public void testAddDocumentTypePrefix() {
        DocumentProto insideDocument = DocumentProto.newBuilder()
                .setUri("inside-id")
                .setSchema("type")
                .setNamespace("namespace")
                .build();
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("type")
                .setNamespace("namespace")
                .addProperties(PropertyProto.newBuilder().addDocumentValues(insideDocument))
                .build();

        DocumentProto expectedInsideDocument = DocumentProto.newBuilder()
                .setUri("inside-id")
                .setSchema("package$databaseName/type")
                .setNamespace("package$databaseName/namespace")
                .build();
        DocumentProto expectedDocumentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("package$databaseName/type")
                .setNamespace("package$databaseName/namespace")
                .addProperties(PropertyProto.newBuilder().addDocumentValues(expectedInsideDocument))
                .build();

        DocumentProto.Builder actualDocument = documentProto.toBuilder();
        addPrefixToDocument(actualDocument, createPrefix("package",
                "databaseName"));
        assertThat(actualDocument.build()).isEqualTo(expectedDocumentProto);
    }

    @Test
    public void testRemoveDocumentTypePrefixes() throws Exception {
        DocumentProto insideDocument = DocumentProto.newBuilder()
                .setUri("inside-id")
                .setSchema("package$databaseName/type")
                .setNamespace("package$databaseName/namespace")
                .build();
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("package$databaseName/type")
                .setNamespace("package$databaseName/namespace")
                .addProperties(PropertyProto.newBuilder().addDocumentValues(insideDocument))
                .build();

        DocumentProto expectedInsideDocument = DocumentProto.newBuilder()
                .setUri("inside-id")
                .setSchema("type")
                .setNamespace("namespace")
                .build();

        DocumentProto expectedDocumentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("type")
                .setNamespace("namespace")
                .addProperties(PropertyProto.newBuilder().addDocumentValues(expectedInsideDocument))
                .build();

        DocumentProto.Builder actualDocument = documentProto.toBuilder();
        assertThat(removePrefixesFromDocument(actualDocument)).isEqualTo(
                "package$databaseName/");
        assertThat(actualDocument.build()).isEqualTo(expectedDocumentProto);
    }

    @Test
    public void testRemoveDatabasesFromDocumentThrowsException() {
        // Set two different database names in the document, which should never happen
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("prefix1/type")
                .setNamespace("prefix2/namespace")
                .build();

        DocumentProto.Builder actualDocument = documentProto.toBuilder();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                removePrefixesFromDocument(actualDocument));
        assertThat(e).hasMessageThat().contains("Found unexpected multiple prefix names");
    }

    @Test
    public void testNestedRemoveDatabasesFromDocumentThrowsException() {
        // Set two different database names in the outer and inner document, which should never
        // happen.
        DocumentProto insideDocument = DocumentProto.newBuilder()
                .setUri("inside-id")
                .setSchema("prefix1/type")
                .setNamespace("prefix1/namespace")
                .build();
        DocumentProto documentProto = DocumentProto.newBuilder()
                .setUri("id")
                .setSchema("prefix2/type")
                .setNamespace("prefix2/namespace")
                .addProperties(PropertyProto.newBuilder().addDocumentValues(insideDocument))
                .build();

        DocumentProto.Builder actualDocument = documentProto.toBuilder();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                removePrefixesFromDocument(actualDocument));
        assertThat(e).hasMessageThat().contains("Found unexpected multiple prefix names");
    }

    @Test
    public void testTriggerCheckOptimizeByMutationSize() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a document and then remove it to generate garbage.
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.remove("package", "database", "namespace", "id",
                /*removeStatsBuilder=*/ null);

        // Verify there is garbage documents.
        GetOptimizeInfoResultProto optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked();
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(1);

        // Increase mutation counter and stop before reach the threshold
        mAppSearchImpl.checkForOptimize(AppSearchImpl.CHECK_OPTIMIZE_INTERVAL - 1,
                /*builder=*/null);

        // Verify the optimize() isn't triggered.
        optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked();
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(1);

        // Increase the counter and reach the threshold, optimize() should be triggered.
        OptimizeStats.Builder builder = new OptimizeStats.Builder();
        mAppSearchImpl.checkForOptimize(/*mutateBatchSize=*/ 1, builder);

        // Verify optimize() is triggered.
        optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked();
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(0);
        assertThat(optimizeInfo.getEstimatedOptimizableBytes()).isEqualTo(0);

        // Verify the stats have been set.
        OptimizeStats oStats = builder.build();
        assertThat(oStats.getOriginalDocumentCount()).isEqualTo(1);
        assertThat(oStats.getDeletedDocumentCount()).isEqualTo(1);
    }

    @Test
    public void testReset() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a valid doc
        GenericDocument validDoc =
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                validDoc,
                /*sendChangeNotifications=*/ false,
                /*logger=*/null);

        // Query it via global query. We use the same code again later so this is to make sure we
        // have our global query configured right.
        SearchResultPage results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mSelfCallerAccess,
                /*logger=*/ null);
        assertThat(results.getResults()).hasSize(1);
        assertThat(results.getResults().get(0).getGenericDocument()).isEqualTo(validDoc);

        // Create a doc with a malformed namespace
        DocumentProto invalidDoc = DocumentProto.newBuilder()
                .setNamespace("invalidNamespace")
                .setUri("id2")
                .setSchema(mContext.getPackageName() + "$database1/Type1")
                .build();
        AppSearchException e = assertThrows(
                AppSearchException.class,
                () -> PrefixUtil.getPrefix(invalidDoc.getNamespace()));
        assertThat(e).hasMessageThat().isEqualTo(
                "The prefixed value \"invalidNamespace\" doesn't contain a valid database name");

        // Insert the invalid doc with an invalid namespace right into icing
        PutResultProto putResultProto = mAppSearchImpl.mIcingSearchEngineLocked.put(invalidDoc);
        assertThat(putResultProto.getStatus().getCode()).isEqualTo(StatusProto.Code.OK);

        // Initialize AppSearchImpl. This should cause a reset.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir, new UnlimitedLimitConfig(), initStatsBuilder, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasDeSync()).isFalse();
        assertThat(initStats.getDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getSchemaStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getDocumentStoreDataStatus())
                .isEqualTo(InitializeStats.DOCUMENT_STORE_DATA_STATUS_NO_DATA_LOSS);
        assertThat(initStats.hasReset()).isTrue();
        assertThat(initStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);

        // Make sure all our data is gone
        assertThat(mAppSearchImpl.getSchema(
                /*packageName=*/mContext.getPackageName(),
                /*databaseName=*/"database1",
                /*callerAccess=*/mSelfCallerAccess)
                .getSchemas())
                .isEmpty();
        results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mSelfCallerAccess,
                /*logger=*/ null);
        assertThat(results.getResults()).isEmpty();

        // Make sure the index can now be used successfully
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                Collections.singletonList(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a valid doc
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                validDoc,
                /*sendChangeNotifications=*/ false,
                /*logger=*/null);

        // Query it via global query.
        results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mSelfCallerAccess,
                /*logger=*/ null);
        assertThat(results.getResults()).hasSize(1);
        assertThat(results.getResults().get(0).getGenericDocument()).isEqualTo(validDoc);
    }

    @Test
    public void testQueryEmptyDatabase() throws Exception {
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package", "EmptyDatabase", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();
    }

    /**
     * TODO(b/169883602): This should be an integration test at the cts-level. This is a
     * short-term test until we have official support for multiple-apps indexing at once.
     */
    @Test
    public void testQueryWithMultiplePackages_noPackageFilters() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package2 schema
        List<AppSearchSchema> schema2 =
                ImmutableList.of(new AppSearchSchema.Builder("schema2").build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schema2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package1 document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "schema1")
                .build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // No query filters specified, package2 shouldn't be able to query for package1's documents.
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package2", "database2", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();

        // Insert package2 document
        document = new GenericDocument.Builder<>("namespace", "id", "schema2").build();
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // No query filters specified. package2 should only get its own documents back.
        searchResultPage = mAppSearchImpl.query("package2", "database2", "", searchSpec, /*logger=
         */ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document);
    }

    /**
     * TODO(b/169883602): This should be an integration test at the cts-level. This is a
     * short-term test until we have official support for multiple-apps indexing at once.
     */
    @Test
    public void testQueryWithMultiplePackages_withPackageFilters() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package2 schema
        List<AppSearchSchema> schema2 =
                ImmutableList.of(new AppSearchSchema.Builder("schema2").build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schema2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package1 document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // "package1" filter specified, but package2 shouldn't be able to query for package1's
        // documents.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package2", "database2", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();

        // Insert package2 document
        document = new GenericDocument.Builder<>("namespace", "id", "schema2").build();
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // "package2" filter specified, package2 should only get its own documents back.
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package2")
                .build();
        searchResultPage = mAppSearchImpl.query("package2", "database2", "", searchSpec, /*logger=
         */ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document);
    }

    @Test
    public void testGlobalQuery_emptyPackage() throws Exception {
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/"",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/""),
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();
    }

    @Test
    public void testSearchSuggestion() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert three documents.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "type")
                .setPropertyString("body", "termOne")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "type")
                .setPropertyString("body", "termOne termTwo termThree")
                .build();
        mAppSearchImpl.putDocument("package", "database", doc1,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);
        mAppSearchImpl.putDocument("package", "database", doc2,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);
        mAppSearchImpl.putDocument("package", "database", doc3,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        List<SearchSuggestionResult> suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.get(0).getSuggestedResult()).isEqualTo("termone");
        assertThat(suggestions.get(1).getSuggestedResult()).isEqualTo("termtwo");
        assertThat(suggestions.get(2).getSuggestedResult()).isEqualTo("termthree");

        // Set total result count to be 2.
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/2).build());
        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getSuggestedResult()).isEqualTo("termone");
        assertThat(suggestions.get(1).getSuggestedResult()).isEqualTo("termtwo");
    }

    @Test
    public void testSearchSuggestion_removeDocument() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a document.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "type")
                .setPropertyString("body", "termOne")
                .build();
        mAppSearchImpl.putDocument("package", "database", doc1,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        List<SearchSuggestionResult> suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getSuggestedResult()).isEqualTo("termone");

        // Remove the document.
        mAppSearchImpl.remove("package", "database", "namespace", "id1",
                /*removeStatsBuilder=*/null);

        // Now we cannot find any suggestion
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_replaceDocument() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a document.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "type")
                .setPropertyString("body", "tart two three")
                .build();
        mAppSearchImpl.putDocument("package", "database", doc1,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);
        SearchSuggestionResult tartResult =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();
        SearchSuggestionResult twoResult =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult threeResult =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult twistResult =
                new SearchSuggestionResult.Builder().setSuggestedResult("twist").build();
        List<SearchSuggestionResult> suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).containsExactly(tartResult, twoResult, threeResult);

        // replace the document with two terms.
        GenericDocument replaceDocument = new GenericDocument.Builder<>("namespace", "id1", "type")
                .setPropertyString("body", "twist three")
                .build();
        mAppSearchImpl.putDocument("package", "database", replaceDocument,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        // Now we cannot find any suggestion
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).containsExactly(twistResult, threeResult);
    }

    @Test
    public void testSearchSuggestion_namespaceFilter() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert three documents.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "type")
                .setPropertyString("body", "term1")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace2", "id2", "type")
                .setPropertyString("body", "term1 term2")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace3", "id3", "type")
                .setPropertyString("body", "term1 term2 term3")
                .build();

        mAppSearchImpl.putDocument("package", "database", doc1,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);
        mAppSearchImpl.putDocument("package", "database", doc2,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);
        mAppSearchImpl.putDocument("package", "database", doc3,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        List<SearchSuggestionResult> suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1").build());
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getSuggestedResult()).isEqualTo("term1");

        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .build());
        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getSuggestedResult()).isEqualTo("term1");
        assertThat(suggestions.get(1).getSuggestedResult()).isEqualTo("term2");
    }

    @Test
    public void testSearchSuggestion_invalidPrefix() throws Exception {
        // Insert schema just put something in the AppSearch to make it searchable.
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        GenericDocument doc = new GenericDocument.Builder<>("namespace1", "id1", "type")
                .setPropertyString("body", "term1")
                .build();
        mAppSearchImpl.putDocument("package", "database", doc,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        List<SearchSuggestionResult> suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t:",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t-",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"t  ",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"{t}",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
        suggestions = mAppSearchImpl.searchSuggestion(
                "package",
                "database",
                /*suggestionQueryExpression=*/"(t)",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build());
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_emptyPrefix() throws Exception {
        // Insert schema just put something in the AppSearch to make it searchable.
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        GenericDocument doc = new GenericDocument.Builder<>("namespace1", "id1", "type")
                .setPropertyString("body", "term1")
                .build();
        mAppSearchImpl.putDocument("package", "database", doc,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.searchSuggestion(
                        "package",
                        "database",
                        /*suggestionQueryExpression=*/"",
                        new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                                .addFilterNamespaces("namespace1")
                                .build()));
        assertThat(e.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(e).hasMessageThat().contains("suggestionQueryExpression cannot be empty.");
    }

    @Test
    public void testGetNextPageToken_query() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testGetNextPageWithDifferentPackage_query() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Try getting next page with the wrong package, package2
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getNextPage("package2",
                        nextPageToken, /*statsBuilder=*/ null));
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);

        // Can continue getting next page for package1
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testGetNextPageToken_globalQuery() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testGetNextPageWithDifferentPackage_globalQuery() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Try getting next page with the wrong package, package2
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getNextPage("package2", nextPageToken,
                        /*statsBuilder=*/ null));
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);

        // Can continue getting next page for package1
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testInvalidateNextPageToken_query() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Invalidate the token
        mAppSearchImpl.invalidateNextPageToken("package1", nextPageToken);

        // Can't get next page because we invalidated the token.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getNextPage("package1", nextPageToken,
                        /*statsBuilder=*/ null));
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);
    }

    @Test
    public void testInvalidateNextPageToken_zeroNextPageToken() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert one package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for 2 results per page, so all the results can fit in one page.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(2) // make sure all the results can be returned in one page.
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // We only have one document indexed
        assertThat(searchResultPage.getResults()).hasSize(1);

        // nextPageToken should be 0 since there is no more results
        long nextPageToken = searchResultPage.getNextPageToken();
        assertThat(nextPageToken).isEqualTo(0);

        // Invalidate the token, no exception should be thrown
        mAppSearchImpl.invalidateNextPageToken("package1", nextPageToken);
    }

    @Test
    public void testInvalidateNextPageTokenWithDifferentPackage_query() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Try getting next page with the wrong package, package2
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.invalidateNextPageToken("package2",
                        nextPageToken));
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);

        // Can continue getting next page for package1
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testInvalidateNextPageToken_globalQuery() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Invalidate the token
        mAppSearchImpl.invalidateNextPageToken("package1", nextPageToken);

        // Can't get next page because we invalidated the token.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getNextPage("package1", nextPageToken,
                        /*statsBuilder=*/ null));
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);
    }

    @Test
    public void testInvalidateNextPageTokenWithDifferentPackage_globalQuery() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();

        // Try getting next page with the wrong package, package2
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.invalidateNextPageToken("package2",
                        nextPageToken));
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" cannot use nextPageToken: " + nextPageToken);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);

        // Can continue getting next page for package1
        searchResultPage = mAppSearchImpl.getNextPage("package1", nextPageToken,
                /*statsBuilder=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testRemoveEmptyDatabase_noExceptionThrown() throws Exception {
        SearchSpec searchSpec =
                new SearchSpec.Builder().addFilterSchemas("FakeType").setTermMatch(
                        TermMatchType.Code.PREFIX_VALUE).build();
        mAppSearchImpl.removeByQuery("package", "EmptyDatabase",
                "", searchSpec, /*statsBuilder=*/ null);

        searchSpec =
                new SearchSpec.Builder().addFilterNamespaces("FakeNamespace").setTermMatch(
                        TermMatchType.Code.PREFIX_VALUE).build();
        mAppSearchImpl.removeByQuery("package", "EmptyDatabase",
                "", searchSpec, /*statsBuilder=*/ null);

        searchSpec = new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        mAppSearchImpl.removeByQuery("package", "EmptyDatabase", "", searchSpec,
                /*statsBuilder=*/ null);
    }

    @Test
    public void testSetSchema() throws Exception {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());
        // Set schema Email to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email").setVersion(0))
                .build();

        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);
    }

    @Test
    public void testSetSchema_incompatible() throws Exception {
        List<AppSearchSchema> oldSchemas = new ArrayList<>();
        oldSchemas.add(new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("foo")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build());
        oldSchemas.add(new AppSearchSchema.Builder("Text").build());
        // Set schema Email to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                oldSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create incompatible schema
        List<AppSearchSchema> newSchemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // set email incompatible and delete text
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                newSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        SetSchemaResponse setSchemaResponse = internalSetSchemaResponse.getSetSchemaResponse();

        assertThat(setSchemaResponse.getDeletedTypes()).containsExactly("Text");
        assertThat(setSchemaResponse.getIncompatibleTypes()).containsExactly("Email");
    }

    @Test
    public void testRemoveSchema() throws Exception {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();

        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Email").build(),
                new AppSearchSchema.Builder("Document").build());
        // Set schema Email and Document to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email").setVersion(0))
                .addTypes(SchemaTypeConfigProto.newBuilder().setSchemaType(
                        "package$database1/Document").setVersion(0))
                .build();

        // Check both schema Email and Document saved correctly.
        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);

        final List<AppSearchSchema> finalSchemas = Collections.singletonList(
                new AppSearchSchema.Builder("Email").build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                        "package",
                        "database1",
                        finalSchemas,
                        /*visibilityDocuments=*/ Collections.emptyList(),
                        /*forceOverride=*/ false,
                        /*version=*/ 0,
                        /* setSchemaStatsBuilder= */ null);
        // We are fail to set this call since forceOverride is false.
        assertThat(internalSetSchemaResponse.isSuccess()).isFalse();
        SetSchemaResponse setSchemaResponse = internalSetSchemaResponse.getSetSchemaResponse();
        // Check the incompatible reason is we are trying to delete Document type.
        assertThat(setSchemaResponse.getDeletedTypes()).containsExactly("Document");

        // ForceOverride to delete.
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                finalSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Check Document schema is removed.
        expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email").setVersion(0))
                .build();

        expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);
    }

    @Test
    public void testRemoveSchema_differentDataBase() throws Exception {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();

        // Create schemas
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Email").build(),
                new AppSearchSchema.Builder("Document").build());

        // Set schema Email and Document to AppSearch database1 and 2
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email").setVersion(0))
                .addTypes(SchemaTypeConfigProto.newBuilder().setSchemaType(
                        "package$database1/Document").setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Email").setVersion(0))
                .addTypes(SchemaTypeConfigProto.newBuilder().setSchemaType(
                        "package$database2/Document").setVersion(0))
                .build();

        // Check Email and Document is saved in database 1 and 2 correctly.
        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);

        // Save only Email to database1 this time.
        schemas = Collections.singletonList(new AppSearchSchema.Builder("Email").build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType list, database 1 should only contain Email but database 2
        // remains in same.
        expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email").setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Email").setVersion(0))
                .addTypes(SchemaTypeConfigProto.newBuilder().setSchemaType(
                        "package$database2/Document").setVersion(0))
                .build();

        // Check nothing changed in database2.
        expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);
    }

    @Test
    public void testClearPackageData() throws AppSearchException {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();
        Map<String, Set<String>> existingDatabases = mAppSearchImpl.getPackageToDatabases();

        // Insert package schema
        List<AppSearchSchema> schema =
                ImmutableList.of(new AppSearchSchema.Builder("schema").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schema,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id",
                "schema").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Verify the document is indexed.
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package",
                "database",  /*queryExpression=*/ "", searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document);

        // Remove the package
        mAppSearchImpl.clearPackageData("package");

        // Verify the document is cleared.
        searchResultPage = mAppSearchImpl.query("package2", "database2",
                /*queryExpression=*/ "", searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();

        // Verify the schema is cleared.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(existingSchemas);
        assertThat(mAppSearchImpl.getPackageToDatabases())
                .containsExactlyEntriesIn(existingDatabases);
    }

    @Test
    public void testPrunePackageData() throws AppSearchException {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();
        Map<String, Set<String>> existingDatabases = mAppSearchImpl.getPackageToDatabases();

        Set<String> existingPackages = new ArraySet<>(existingSchemas.size());
        for (int i = 0; i < existingSchemas.size(); i++) {
            existingPackages.add(PrefixUtil.getPackageName(existingSchemas.get(i).getSchemaType()));
        }

        // Create VisibilityDocument
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("schema")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();

        // Insert schema for package A and B.
        List<AppSearchSchema> schema =
                ImmutableList.of(new AppSearchSchema.Builder("schema").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageA",
                "database",
                schema,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageB",
                "database",
                schema,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Verify these two packages are stored in AppSearch.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("packageA$database/schema").setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("packageB$database/schema").setVersion(0))
                .build();
        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);

        // Verify these two visibility documents are stored in AppSearch.
        VisibilityDocument expectedVisibilityDocumentA =
                new VisibilityDocument.Builder("packageA$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .setCreationTimestampMillis(12345L)
                        .build();
        VisibilityDocument expectedVisibilityDocumentB =
                new VisibilityDocument.Builder("packageB$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .setCreationTimestampMillis(12345L)
                        .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked
                .getVisibility("packageA$database/schema"))
                .isEqualTo(expectedVisibilityDocumentA);
        assertThat(mAppSearchImpl.mVisibilityStoreLocked
                .getVisibility("packageB$database/schema"))
                .isEqualTo(expectedVisibilityDocumentB);

        // Prune packages
        mAppSearchImpl.prunePackageData(existingPackages);

        // Verify the schema is same as beginning.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(existingSchemas);
        assertThat(mAppSearchImpl.getPackageToDatabases())
                .containsExactlyEntriesIn(existingDatabases);

        // Verify the VisibilitySetting is removed.
        assertThat(mAppSearchImpl.mVisibilityStoreLocked
                .getVisibility("packageA$database/schema")).isNull();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked
                .getVisibility("packageB$database/schema")).isNull();
    }

    @Test
    public void testGetPackageToDatabases() throws Exception {
        Map<String, Set<String>> existingMapping = mAppSearchImpl.getPackageToDatabases();
        Map<String, Set<String>> expectedMapping = new ArrayMap<>();
        expectedMapping.putAll(existingMapping);

        // Has database1
        expectedMapping.put("package1", ImmutableSet.of("database1"));
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1", "database1",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);

        // Has both databases
        expectedMapping.put("package1", ImmutableSet.of("database1", "database2"));
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1", "database2",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);

        // Has both packages
        expectedMapping.put("package2", ImmutableSet.of("database1"));
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2", "database1",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);
    }

    @Test
    public void testGetAllPrefixedSchemaTypes() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas1 =
                Collections.singletonList(new AppSearchSchema.Builder("type1").build());
        List<AppSearchSchema> schemas2 =
                Collections.singletonList(new AppSearchSchema.Builder("type2").build());
        List<AppSearchSchema> schemas3 =
                Collections.singletonList(new AppSearchSchema.Builder("type3").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas3,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getAllPrefixedSchemaTypes()).containsExactly(
                "package1$database1/type1",
                "package1$database2/type2",
                "package2$database1/type3",
                "VS#Pkg$VS#Db/VisibilityType",  // plus the stored Visibility schema
                "VS#Pkg$VS#Db/VisibilityPermissionType");
    }

    @FlakyTest(bugId = 204186664)
    @Test
    public void testReportUsage() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two docs
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id1", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Report some usages. id1 has 2 app and 1 system usage, id2 has 1 app and 2 system usage.
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id1", /*usageTimestampMillis=*/ 10, /*systemUsage=*/ false);
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id1", /*usageTimestampMillis=*/ 20, /*systemUsage=*/ false);
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id1", /*usageTimestampMillis=*/ 1000, /*systemUsage=*/ true);

        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id2", /*usageTimestampMillis=*/ 100, /*systemUsage=*/ false);
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id2", /*usageTimestampMillis=*/ 200, /*systemUsage=*/ true);
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id2", /*usageTimestampMillis=*/ 150, /*systemUsage=*/ true);

        // Sort by app usage count: id1 should win
        List<SearchResult> page = mAppSearchImpl.query("package", "database", "",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_COUNT)
                        .build(), /*logger=*/ null).getResults();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id2");

        // Sort by app usage timestamp: id2 should win
        page = mAppSearchImpl.query("package", "database", "",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP)
                        .build(), /*logger=*/ null).getResults();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id1");

        // Sort by system usage count: id2 should win
        page = mAppSearchImpl.query("package", "database", "",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_SYSTEM_USAGE_COUNT)
                        .build(), /*logger=*/ null).getResults();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id1");

        // Sort by system usage timestamp: id1 should win
        page = mAppSearchImpl.query("package", "database", "",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setRankingStrategy(
                                SearchSpec.RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP)
                        .build(), /*logger=*/ null).getResults();
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(page.get(1).getGenericDocument().getId()).isEqualTo("id2");
    }

    @Test
    public void testGetStorageInfoForPackage_nonexistentPackage() throws Exception {
        // "package2" doesn't exist yet, so it shouldn't have any storage size
        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForPackage("nonexistent.package");
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForPackage_withoutDocument() throws Exception {
        // Insert schema for "package1"
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Since "package1" doesn't have a document, it get any space attributed to it.
        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForPackage("package1");
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForPackage_proportionalToDocuments() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());

        // Insert schema for "package1"
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert document for "package1"
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Insert schema for "package2"
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two documents for "package2"
        document = new GenericDocument.Builder<>("namespace", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package2",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        document = new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package2",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForPackage("package1");
        long size1 = storageInfo.getSizeBytes();
        assertThat(size1).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(1);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(1);

        storageInfo = mAppSearchImpl.getStorageInfoForPackage("package2");
        long size2 = storageInfo.getSizeBytes();
        assertThat(size2).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(2);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(1);

        // Size is proportional to number of documents. Since "package2" has twice as many
        // documents as "package1", its size is twice as much too.
        assertThat(size2).isAtLeast(2 * size1);
    }

    @Test
    public void testGetStorageInfoForDatabase_nonexistentPackage() throws Exception {
        // "package2" doesn't exist yet, so it shouldn't have any storage size
        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForDatabase("nonexistent.package",
                "nonexistentDatabase");
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForDatabase_nonexistentDatabase() throws Exception {
        // Insert schema for "package1"
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // "package2" doesn't exist yet, so it shouldn't have any storage size
        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForDatabase("package1",
                "nonexistentDatabase");
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForDatabase_withoutDocument() throws Exception {
        // Insert schema for "package1"
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Since "package1", "database1" doesn't have a document, it get any space attributed to it.
        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForDatabase("package1", "database1");
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForDatabase_proportionalToDocuments() throws Exception {
        // Insert schema for "package1", "database1" and "database2"
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document for "package1", "database1"
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Add two documents for "package1", "database2"
        document = new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database2",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        document = new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database2",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        StorageInfo storageInfo = mAppSearchImpl.getStorageInfoForDatabase("package1", "database1");
        long size1 = storageInfo.getSizeBytes();
        assertThat(size1).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(1);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(1);

        storageInfo = mAppSearchImpl.getStorageInfoForDatabase("package1", "database2");
        long size2 = storageInfo.getSizeBytes();
        assertThat(size2).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(2);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(1);

        // Size is proportional to number of documents. Since "database2" has twice as many
        // documents as "database1", its size is twice as much too.
        assertThat(size2).isAtLeast(2 * size1);
    }

    @Test
    public void testThrowsExceptionIfClosed() throws Exception {
        // Initial check that we could do something at first.
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        mAppSearchImpl.close();

        // Check all our public APIs
        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getSchema(
                /*packageName=*/"package",
                /*databaseName=*/"database",
                /*callerAccess=*/mSelfCallerAccess));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getDocument(
                "package", "database", "namespace", "id", Collections.emptyMap()));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.query(
                "package",
                "database",
                "query",
                new SearchSpec.Builder().build(),
                /*logger=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.globalQuery(
                "query",
                new SearchSpec.Builder().build(),
                mSelfCallerAccess,
                /*logger=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getNextPage("package",
                /*nextPageToken=*/ 1L, /*statsBuilder=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.invalidateNextPageToken(
                "package",
                /*nextPageToken=*/ 1L));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.reportUsage(
                "package", "database", "namespace", "id",
                /*usageTimestampMillis=*/ 1000L, /*systemUsage=*/ false));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.remove(
                "package", "database", "namespace", "id", /*removeStatsBuilder=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.removeByQuery(
                "package",
                "database",
                "query",
                new SearchSpec.Builder().build(),
                /*removeStatsBuilder=*/ null));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getStorageInfoForPackage(
                "package"));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getStorageInfoForDatabase(
                "package", "database"));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.persistToDisk(
                PersistType.Code.FULL));
    }

    @Test
    public void testPutPersistsWithLiteFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        GenericDocument getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document);

        // That document should be visible even from another instance.
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        getResult = appSearchImpl2.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document);
        appSearchImpl2.close();
    }

    @Test
    public void testDeletePersistsWithLiteFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add two documents and persist them.
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        GenericDocument getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document1);
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Delete the first document
        mAppSearchImpl.remove("package", "database", "namespace1", "id1", /*statsBuilder=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);
        assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Only the second document should be retrievable from another instance.
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        assertThrows(AppSearchException.class, () -> appSearchImpl2.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = appSearchImpl2.getDocument("package", "database", "namespace1",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);
        appSearchImpl2.close();
    }

    @Test
    public void testDeleteByQueryPersistsWithLiteFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add two documents and persist them.
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace2", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        GenericDocument getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document1);
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace2",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Delete the first document
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().addFilterNamespaces("namespace1").setTermMatch(
                        SearchSpec.TERM_MATCH_EXACT_ONLY).build(), /*statsBuilder=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);
        assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace2",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Only the second document should be retrievable from another instance.
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        assertThrows(AppSearchException.class, () -> appSearchImpl2.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = appSearchImpl2.getDocument("package", "database", "namespace2",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);
        appSearchImpl2.close();
    }

    @Test
    public void testGetIcingSearchEngineStorageInfo() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add two documents
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        StorageInfoProto storageInfo = mAppSearchImpl.getRawStorageInfoProto();

        // Simple checks to verify if we can get correct StorageInfoProto from IcingSearchEngine
        // No need to cover all the fields
        assertThat(storageInfo.getTotalStorageSize()).isGreaterThan(0);
        assertThat(
                storageInfo.getDocumentStorageInfo().getNumAliveDocuments())
                .isEqualTo(2);
        assertThat(
                storageInfo.getSchemaStoreStorageInfo().getNumSchemaTypes())
                .isEqualTo(3); // +2 for VisibilitySchema
    }

    @Test
    public void testGetIcingSearchEngineDebugInfo() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add two documents
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        DebugInfoProto debugInfo =
                mAppSearchImpl.getRawDebugInfoProto(DebugInfoVerbosity.Code.DETAILED);

        // Simple checks to verify if we can get correct DebugInfoProto from IcingSearchEngine
        // No need to cover all the fields
        assertThat(debugInfo.getDocumentInfo().getCorpusInfoList()).hasSize(1);
        assertThat(
                debugInfo.getDocumentInfo().getDocumentStorageInfo().getNumAliveDocuments())
                .isEqualTo(2);
        assertThat(debugInfo.getSchemaInfo().getSchema().getTypesList())
                .hasSize(3); // +2 for VisibilitySchema
    }

    @Test
    public void testLimitConfig_DocumentSize() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 1;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a document which is too large
        GenericDocument document = new GenericDocument.Builder<>(
                "this_namespace_is_long_to_make_the_doc_big", "id", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package",
                        "database",
                        document,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Document \"id\" for package \"package\" serialized to 99 bytes, which exceeds"
                        + " limit of 80 bytes");

        // Make sure this failure didn't increase our document count. We should still be able to
        // index 1 document.
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document3,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");
    }

    @Test
    public void testLimitConfig_Init() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 1;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");

        // Close and reinitialize AppSearchImpl
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 1;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Make sure the limit is maintained
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");
    }

    @Test
    public void testLimitConfig_Remove() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index 3 documents
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id3", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Remove a document that doesn't exist
        assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.remove(
                        "package", "database", "namespace", "id4", /*removeStatsBuilder=*/null));

        // Should still fail
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Remove a document that does exist
        mAppSearchImpl.remove(
                "package", "database", "namespace", "id2", /*removeStatsBuilder=*/null);

        // Now doc4 should work
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // The next one should fail again
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id5", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");
    }

    @Test
    public void testLimitConfig_DifferentPackages() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index documents in package1/database1
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database2",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Indexing a third doc into package1 should fail (here we use database3)
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package1",
                        "database3",
                        new GenericDocument.Builder<>("namespace", "id3", "type").build(),
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" exceeded limit of 2 documents");

        // Indexing a doc into package2 should succeed
        mAppSearchImpl.putDocument(
                "package2",
                "database1",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Reinitialize to make sure packages are parsed correctly on init
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // package1 should still be out of space
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package1",
                        "database4",
                        new GenericDocument.Builder<>("namespace", "id4", "type").build(),
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" exceeded limit of 2 documents");

        // package2 has room for one more
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // now package2 really is out of space
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package2",
                        "database3",
                        new GenericDocument.Builder<>("namespace", "id3", "type").build(),
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" exceeded limit of 2 documents");
    }

    @Test
    public void testLimitConfig_RemoveByQuery() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .build())
                        .build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index 3 documents
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "tablet")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type")
                        .setPropertyString("body", "tabby")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id3", "type")
                        .setPropertyString("body", "grabby")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Run removebyquery, deleting nothing
        mAppSearchImpl.removeByQuery(
                "package",
                "database",
                "nothing",
                new SearchSpec.Builder().build(),
                /*removeStatsBuilder=*/null);

        // Should still fail
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Remove "tab*"
        mAppSearchImpl.removeByQuery(
                "package",
                "database",
                "tab",
                new SearchSpec.Builder().build(),
                /*removeStatsBuilder=*/null);

        // Now doc4 and doc5 should work
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document4,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id5", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // We only deleted 2 docs so the next one should fail again
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id6", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");
    }

    @Test
    public void testLimitConfig_Replace() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("body").build())
                        .build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.orig")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        // Replace it with another doc
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.new")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Index id2. This should pass but only because we check for replacements.
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure on id3
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document3,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 2 documents");
    }

    @Test
    public void testLimitConfig_ReplaceReinit() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("body").build())
                        .build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.orig")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        // Replace it with another doc
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.new")
                        .build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Reinitialize to make sure replacements are correctly accounted for by init
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Index id2. This should pass but only because we check for replacements.
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now we should get a failure on id3
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                "package",
                "database",
                document3,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 2 documents");
    }
    @Test
    public void testLimitConfig_suggestion() throws Exception {
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxDocumentCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return 2;
                    }
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.searchSuggestion(
                        "package",
                        "database",
                        /*suggestionQueryExpression=*/"t",
                        new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()));
        assertThat(e.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(e).hasMessageThat().contains(
                "Trying to get 10 suggestion results, which exceeds limit of 2");
    }

    /**
     * Ensure that it is okay to register the same observer for multiple packages and that removing
     * the observer for one package doesn't remove it for the other.
     */
    @Test
    public void testRemoveObserver_onlyAffectsOnePackage() throws Exception {
        final String fakePackage = "com.android.appsearch.fake.package";

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                /*schemas=*/ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer twice, on different packages.
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/fakePackage,
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a valid doc
        GenericDocument validDoc =
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                validDoc,
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);

        // Dispatch notifications and empty the observers
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        observer.clear();

        // Remove the observer from the fake package
        mAppSearchImpl.unregisterObserverCallback(fakePackage, observer);

        // Index a second document
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace1", "id2", "Type1").build();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                doc2,
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);

        // Observer should still have received this data from its registration on
        // context.getPackageName(), as we only removed the copy from fakePackage.
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        "namespace1",
                        "Type1",
                        /*changedDocumentIds=*/ImmutableSet.of("id2")));
    }

    @Test
    public void testGetGlobalDocumentThrowsExceptionWhenNotVisible() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> false;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/false,
                /*logger=*/null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerAccess=*/mSelfCallerAccess));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo("Document (namespace1, id1) not found.");
    }

    @Test
    public void testGetGlobalDocument() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        GenericDocument getResult = mAppSearchImpl.globalGetDocument(
                "package",
                "database",
                "namespace1",
                "id1",
                /*typePropertyPaths=*/Collections.emptyMap(),
                /*callerAccess=*/mSelfCallerAccess);
        assertThat(getResult).isEqualTo(document);
    }

    @Test
    public void getGlobalDocumentTest_notFound() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id2",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerAccess=*/mSelfCallerAccess));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo("Document (namespace1, id2) not found.");
    }

    @Test
    public void getGlobalDocumentNoAccessNoFileHasSameException() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) ->
                        callerAccess.getCallingPackageName().equals("visiblePackage");
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException unauthorizedException = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        new CallerAccess(/*callingPackageName=*/"invisiblePackage")));

        mAppSearchImpl.remove("package", "database", "namespace1", "id1",
                /*removeStatsBuilder=*/null);

        AppSearchException noDocException = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        new CallerAccess(/*callingPackageName=*/"visiblePackage")));

        assertThat(noDocException.getResultCode()).isEqualTo(unauthorizedException.getResultCode());
        assertThat(noDocException.getMessage()).isEqualTo(unauthorizedException.getMessage());
    }

    @Test
    public void testSetVisibility() throws Exception {
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email to AppSearch database1 with a visibility document
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix = PrefixUtil.createPrefix("package", "database1");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix + "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(expectedDocument);
    }

    @Test
    public void testSetVisibility_existingVisibilitySettingRetains() throws Exception {
        // Create Visibility Document for Email1
        VisibilityDocument visibilityDocument1 = new VisibilityDocument.Builder("Email1")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        List<AppSearchSchema> schemas1 =
                Collections.singletonList(new AppSearchSchema.Builder("Email1").build());

        // Set schema Email1 to package1 with a visibility document
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas1,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument1),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix1 = PrefixUtil.createPrefix("package1", "database");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument1 = new VisibilityDocument.Builder(prefix1 + "Email1")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument1 =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix1 + "Email1",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument1).isEqualTo(expectedDocument1);

        // Create Visibility Document for Email2
        VisibilityDocument visibilityDocument2 = new VisibilityDocument.Builder("Email2")
                .setNotDisplayedBySystem(false)
                .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .setCreationTimestampMillis(54321L)
                .build();
        List<AppSearchSchema> schemas2 =
                Collections.singletonList(new AppSearchSchema.Builder("Email2").build());

        // Set schema Email2 to package1 with a visibility document
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas2,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument2),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument2 = new VisibilityDocument.Builder(prefix2 + "Email2")
                .setNotDisplayedBySystem(false)
                .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .setCreationTimestampMillis(54321)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix2 + "Email2"))
                .isEqualTo(expectedDocument2);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument2 =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix2 + "Email2",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument2).isEqualTo(expectedDocument2);

        // Check the existing visibility document retains.
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        actualDocument1 =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix1 + "Email1",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument1).isEqualTo(expectedDocument1);
    }

    @Test
    public void testSetVisibility_removeVisibilitySettings() throws Exception {
        // Create a non-all-default visibility document
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email and its visibility document to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix = PrefixUtil.createPrefix("package", "database1");
        VisibilityDocument expectedDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix + "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(expectedDocument);

        // Set schema Email and its all-default visibility document to AppSearch database1
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        // All-default visibility document won't be saved in AppSearch.
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isNull();
        // Verify the VisibilityDocument is removed from AppSearchImpl.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityDocument.NAMESPACE,
                        /*id=*/ prefix + "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, package$database1/Email) not found.");
    }

    @Test
    public void testRemoveVisibility_noRemainingSettings() throws Exception {
        // Create a non-all-default visibility document
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email and its visibility document to AppSearch database1
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        String prefix = PrefixUtil.createPrefix("package", "database1");
        VisibilityDocument expectedDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix + "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(expectedDocument);

        // remove the schema and visibility setting from AppSearch
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                /*schemas=*/ new ArrayList<>(),
                /*visibilityDocuments=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // add the schema back with an all default visibility setting.
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        // All-default visibility document won't be saved in AppSearch.
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isNull();
        // Verify there is no visibility setting for the schema.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityDocument.NAMESPACE,
                        /*id=*/ prefix + "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, package$database1/Email) not found.");
    }

    @Test
    public void testCloseAndReopen_visibilityInfoRetains() throws Exception {
        // set Schema and visibility to AppSearch
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageName",
                "databaseName",
                schemas,
                ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // close and re-open AppSearchImpl, the visibility document retains
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        String prefix = PrefixUtil.createPrefix("packageName", "databaseName");
        VisibilityDocument expectedDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();

        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix + "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(expectedDocument);

        // remove schema and visibility document
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageName",
                "databaseName",
                ImmutableList.of(),
                ImmutableList.of(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // close and re-open AppSearchImpl, the visibility document removed
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email")).isNull();
        // Verify the VisibilityDocument is removed from AppSearchImpl.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityDocument.NAMESPACE,
                        /*id=*/ prefix + "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, packageName$databaseName/Email) not found.");
    }

    @Test
    public void testGetSchema_global() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Type").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        // Add a schema type that is not displayed by the system
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ImmutableList.of(
                        new VisibilityDocument.Builder("Type")
                                .setNotDisplayedBySystem(true).build()),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Get this schema as another package
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(/*callingPackageName=*/"com.android.appsearch.fake.package"));
        assertThat(getResponse.getSchemas()).containsExactlyElementsIn(schemas);
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).containsExactly("Type");
    }

    @Test
    public void testGetSchema_nonExistentApp() throws Exception {
        // Add a schema. The test loses meaning if the schema is completely empty.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                Collections.singletonList(new AppSearchSchema.Builder("Type").build()),
                /*visibilityDocuments=*/ImmutableList.of(),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Try to get the schema of a nonexistent package.
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "com.android.appsearch.fake.package",
                "database",
                new CallerAccess(/*callingPackageName=*/"package"));
        assertThat(getResponse.getSchemas()).isEmpty();
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).isEmpty();
    }

    @Test
    public void testGetSchema_noAccess() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Type").build());
        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ImmutableList.of(),
                /*forceOverride=*/false,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(/*callingPackageName=*/
                        "com.android.appsearch.fake.package"));
        assertThat(getResponse.getSchemas()).isEmpty();
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).isEmpty();
        assertThat(getResponse.getVersion()).isEqualTo(0);

        // Make sure the test is hooked up right by calling getSchema with the same parameters but
        // from the same package
        getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(/*callingPackageName=*/"package"));
        assertThat(getResponse.getSchemas()).containsExactlyElementsIn(schemas);
    }

    @Test
    public void testGetSchema_global_partialAccess() throws Exception {
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("VisibleType").build(),
                new AppSearchSchema.Builder("PrivateType").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> prefixedSchema.endsWith("VisibleType");
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        // Add two schema types that are not displayed by the system.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ImmutableList.of(
                        new VisibilityDocument.Builder("VisibleType")
                                .setNotDisplayedBySystem(true)
                                .build(),
                        new VisibilityDocument.Builder("PrivateType")
                                .setNotDisplayedBySystem(true)
                                .build()),
                /*forceOverride=*/false,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(/*callingPackageName=*/
                        "com.android.appsearch.fake.package"));
        assertThat(getResponse.getSchemas()).containsExactly(schemas.get(0));
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).containsExactly("VisibleType");
        assertThat(getResponse.getVersion()).isEqualTo(1);
    }

    @Test
    public void testDispatchObserver_samePackage_noVisStore_accept() throws Exception {
        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a valid doc
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build(),
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        "namespace1",
                        "Type1",
                        ImmutableSet.of("id1")));
    }

    @Test
    public void testDispatchObserver_samePackage_withVisStore_accept() throws Exception {
        // Make a visibility checker that rejects everything
        final VisibilityChecker rejectChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> false;
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                rejectChecker);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a valid doc
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build(),
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        "namespace1",
                        "Type1",
                        ImmutableSet.of("id1")));
    }

    @Test
    public void testDispatchObserver_differentPackage_noVisStore_reject() throws Exception {
        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer from a simulated different package
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/
                    "com.fake.Listening.package"),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a valid doc
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build(),
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testDispatchObserver_differentPackage_withVisStore_accept() throws Exception {
        final String fakeListeningPackage = "com.fake.listening.package";

        // Make a visibility checker that allows only fakeListeningPackage.
        final VisibilityChecker visibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> callerAccess.getCallingPackageName().equals(fakeListeningPackage);
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakeListeningPackage),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a valid doc
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build(),
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).containsExactly(
                new DocumentChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        "namespace1",
                        "Type1",
                        ImmutableSet.of("id1")));
    }

    @Test
    public void testDispatchObserver_differentPackage_withVisStore_reject() throws Exception {
        final String fakeListeningPackage = "com.fake.Listening.package";

        // Make a visibility checker that rejects everything.
        final VisibilityChecker rejectChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore) -> false;
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                rejectChecker);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakeListeningPackage),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Insert a doc
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build(),
                /*sendChangeNotifications=*/ true,
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_added() throws Exception {
        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Add a schema type
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        ImmutableSet.of("Type1")));
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Add two more schema types without touching the existing one
        observer.clear();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2").build(),
                        new AppSearchSchema.Builder("Type3").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2", "Type3")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_removed() throws Exception {
        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Remove Type2
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(),
                        "database1",
                        ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_contents() throws Exception {
        // Add a schema
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Update the schema, but don't make any actual changes
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 1,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Now update the schema again, but this time actually make a change (cardinality of the
        // property)
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 2,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_contents_skipBySpec() throws Exception {
        // Add a schema
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer that only listens for Type2
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Type2").build(),
                MoreExecutors.directExecutor(),
                observer);

        // Update both types of the schema (changed cardinalities)
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_visibilityOnly() throws Exception {
        final String fakeListeningPackage = "com.fake.listening.package";

        // Make a fake visibility checker that actually looks at visibility store
        final VisibilityChecker visibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> {
                    if (!callerAccess.getCallingPackageName().equals(fakeListeningPackage)) {
                        return false;
                    }
                    Set<String> allowedPackages = new ArraySet<>(
                            visibilityStore.getVisibility(prefixedSchema).getPackageNames());
                    return allowedPackages.contains(fakeListeningPackage);
                };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakeListeningPackage),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Add a schema where both types are visible to the fake package.
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(
                        new VisibilityDocument.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new VisibilityDocument.Builder("Type2")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build()
                ),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Notifications of addition should now be dispatched
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type1", "Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
        observer.clear();

        // Update schema, keeping the types identical but denying visibility to type2
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(
                        new VisibilityDocument.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new VisibilityDocument.Builder("Type2").build()
                ),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications. This should look like a deletion of Type2.
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
        observer.clear();

        // Now update Type2 and make sure no further notification is received.
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ ImmutableList.of(
                        new VisibilityDocument.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new VisibilityDocument.Builder("Type2").build()
                ),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Grant visibility to Type2 again and make sure it appears
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ImmutableList.of(
                        new VisibilityDocument.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new VisibilityDocument.Builder("Type2")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build()
                ),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications. This should look like a creation of Type2.
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_visibilityAndContents() throws Exception {
        final String fakeListeningPackage = "com.fake.listening.package";

        // Make a visibility checker that allows fakeListeningPackage access only to Type2.
        final VisibilityChecker visibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> callerAccess.getCallingPackageName().equals(fakeListeningPackage)
                        && prefixedSchema.endsWith("Type2");
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        // Add a schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakeListeningPackage),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Update both types of the schema (changed cardinalities)
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                                        "booleanProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                                .build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_partialVisibility_removed() throws Exception {
        final String fakeListeningPackage = "com.fake.listening.package";

        // Make a visibility checker that allows fakeListeningPackage access only to Type2.
        final VisibilityChecker visibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> callerAccess.getCallingPackageName().equals(fakeListeningPackage)
                        && prefixedSchema.endsWith("Type2");
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        // Add a schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakeListeningPackage),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Remove Type1
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type2").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications. Nothing should appear since Type1 is not visible to us.
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Now remove Type2. This should cause a notification.
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_multipleObservers() throws Exception {
        // Create two fake packages. One can access Type1, one can access Type2, they both can
        // access Type3, and no one can access Type4.
        final String fakePackage1 = "com.fake.listening.package1";

        final String fakePackage2 = "com.fake.listening.package2";

        final VisibilityChecker visibilityChecker =
                (callerAccess, packageName, prefixedSchema, visibilityStore)
                        -> {
                    if (prefixedSchema.endsWith("Type1")) {
                        return callerAccess.getCallingPackageName().equals(fakePackage1);
                    } else if (prefixedSchema.endsWith("Type2")) {
                        return callerAccess.getCallingPackageName().equals(fakePackage2);
                    } else if (prefixedSchema.endsWith("Type3")) {
                        return false;
                    } else if (prefixedSchema.endsWith("Type4")) {
                        return true;
                    } else {
                        throw new IllegalArgumentException(prefixedSchema);
                    }
                };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        // Add a schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2").build(),
                        new AppSearchSchema.Builder("Type3").build(),
                        new AppSearchSchema.Builder("Type4").build()
                ),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register three observers: one in each package, and another in package1 with a filter.
        TestObserverCallback observerPkg1NoFilter = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakePackage1),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observerPkg1NoFilter);

        TestObserverCallback observerPkg2NoFilter = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakePackage2),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observerPkg2NoFilter);

        TestObserverCallback observerPkg1FilterType4 = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/fakePackage1),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().addFilterSchemas("Type4").build(),
                MoreExecutors.directExecutor(),
                observerPkg1FilterType4);

        // Remove everything
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Dispatch notifications.
        mAppSearchImpl.dispatchAndClearChangeNotifications();

        // observerPkg1NoFilter should see Type1 and Type4 vanish.
        // observerPkg2NoFilter should see Type2 and Type4 vanish.
        // observerPkg2WithFilter should see Type4 vanish.
        assertThat(observerPkg1NoFilter.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type1", "Type4"))
        );
        assertThat(observerPkg1NoFilter.getDocumentChanges()).isEmpty();

        assertThat(observerPkg2NoFilter.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type2", "Type4"))
        );
        assertThat(observerPkg2NoFilter.getDocumentChanges()).isEmpty();

        assertThat(observerPkg1FilterType4.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type4"))
        );
        assertThat(observerPkg1FilterType4.getDocumentChanges()).isEmpty();
    }

    @Test
    public void testAddObserver_schemaChange_noChangeIfIncompatible() throws Exception {
        // Add a schema with two types.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1")
                                .addProperty(
                                        new AppSearchSchema.StringPropertyConfig.Builder("strProp")
                                                .setCardinality(
                                                        AppSearchSchema.PropertyConfig
                                                                .CARDINALITY_OPTIONAL)
                                                .build()
                                ).build(),
                        new AppSearchSchema.Builder("Type2")
                                .addProperty(
                                        new AppSearchSchema.StringPropertyConfig.Builder("strProp")
                                                .setCardinality(
                                                        AppSearchSchema.PropertyConfig
                                                                .CARDINALITY_OPTIONAL)
                                                .build()
                                ).build()
                ),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 1,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess(/*callingPackageName=*/mContext.getPackageName()),
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        // Update schema to try to make an incompatible change to Type1, and a compatible change to
        // Type2.
        List<AppSearchSchema> updatedSchemaTypes = ImmutableList.of(
                new AppSearchSchema.Builder("Type1")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("strProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                                        .build()
                        ).build(),
                new AppSearchSchema.Builder("Type2")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("strProp")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .build()
                        ).build()
        );
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                updatedSchemaTypes,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 2,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isFalse();
        SetSchemaResponse setSchemaResponse = internalSetSchemaResponse.getSetSchemaResponse();
        assertThat(setSchemaResponse.getDeletedTypes()).isEmpty();
        assertThat(setSchemaResponse.getIncompatibleTypes()).containsExactly("Type1");

        // Dispatch notifications. Nothing should appear since the schema was incompatible and has
        // not changed.
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Now force apply the schemas Type2. This should cause a notification.
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                updatedSchemaTypes,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 3,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).containsExactly(
                new SchemaChangeInfo(
                        mContext.getPackageName(), "database1", ImmutableSet.of("Type1", "Type2")));
        assertThat(observer.getDocumentChanges()).isEmpty();
    }
}
