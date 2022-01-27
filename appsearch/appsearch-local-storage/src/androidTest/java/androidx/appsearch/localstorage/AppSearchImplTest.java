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

import static androidx.appsearch.localstorage.util.PrefixUtil.addPrefixToDocument;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.os.Process;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.FlakyTest;

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

public class AppSearchImplTest {
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mAppSearchDir;

    private final Context mContext = ApplicationProvider.getApplicationContext();
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a document and then remove it to generate garbage.
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/ null);
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
        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a valid doc
        GenericDocument validDoc =
                new GenericDocument.Builder<>("namespace1", "id1", "Type1").build();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                validDoc,
                /*logger=*/null);

        // Query it via global query. We use the same code again later so this is to make sure we
        // have our global query configured right.
        SearchResultPage results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mContext.getPackageName(),
                Process.INVALID_UID,
                /*callerHasSystemAccess=*/ false,
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
                /*callerPackageName=*/mContext.getPackageName(),
                /*callerUid=*/Process.myUid(),
                /*callerHasSystemAccess=*/false)
                .getSchemas())
                .isEmpty();
        results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mContext.getPackageName(),
                Process.INVALID_UID,
                /*callerHasSystemAccess=*/false,
                /*logger=*/ null);
        assertThat(results.getResults()).isEmpty();

        // Make sure the index can now be used successfully
        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                Collections.singletonList(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a valid doc
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                validDoc,
                /*logger=*/null);

        // Query it via global query.
        results = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                new SearchSpec.Builder().addFilterSchemas("Type1").build(),
                mContext.getPackageName(),
                Process.INVALID_UID,
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert package2 schema
        List<AppSearchSchema> schema2 =
                ImmutableList.of(new AppSearchSchema.Builder("schema2").build());
        mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schema2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert package1 document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "schema1")
                .build();
        mAppSearchImpl.putDocument("package1", "database1", document, /*logger=*/ null);

        // No query filters specified, package2 shouldn't be able to query for package1's documents.
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package2", "database2", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();

        // Insert package2 document
        document = new GenericDocument.Builder<>("namespace", "id", "schema2").build();
        mAppSearchImpl.putDocument("package2", "database2", document, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert package2 schema
        List<AppSearchSchema> schema2 =
                ImmutableList.of(new AppSearchSchema.Builder("schema2").build());
        mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schema2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert package1 document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document, /*logger=*/ null);

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
        mAppSearchImpl.putDocument("package2", "database2", document, /*logger=*/ null);

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
    public void testGlobalQueryEmptyDatabase() throws Exception {
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                "",
                searchSpec,
                /*callerPackageName=*/ "",
                Process.INVALID_UID,
                /*callerHasSystemAccess=*/ false,
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();
    }

    @Test
    public void testGetNextPageToken_query() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                "package1",
                Process.myUid(),
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                "package1",
                Process.myUid(),
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert one package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                "package1",
                Process.myUid(),
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument("package1", "database1", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package1", "database1", document2, /*logger=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                "package1",
                Process.myUid(),
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                oldSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Create incompatible schema
        List<AppSearchSchema> newSchemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // set email incompatible and delete text
        SetSchemaResponse setSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                newSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        SetSchemaResponse setSchemaResponse =
                mAppSearchImpl.setSchema(
                        "package",
                        "database1",
                        finalSchemas,
                        /*visibilityDocuments=*/ Collections.emptyList(),
                        /*forceOverride=*/ false,
                        /*version=*/ 0,
                        /* setSchemaStatsBuilder= */ null);
        // Check the Document type has been deleted.
        assertThat(setSchemaResponse.getDeletedTypes()).containsExactly("Document");

        // ForceOverride to delete.
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                finalSchemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schema,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert package document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id",
                "schema").build();
        mAppSearchImpl.putDocument("package", "database", document,
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

        // Insert schema for package A and B.
        List<AppSearchSchema> schema =
                ImmutableList.of(new AppSearchSchema.Builder("schema").build());
        mAppSearchImpl.setSchema(
                "packageA",
                "database",
                schema,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "packageB",
                "database",
                schema,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Verify these two packages is stored in AppSearch
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

        // Prune packages
        mAppSearchImpl.prunePackageData(existingPackages);

        // Verify the schema is same as beginning.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(existingSchemas);
        assertThat(mAppSearchImpl.getPackageToDatabases())
                .containsExactlyEntriesIn(existingDatabases);
    }

    @Test
    public void testGetPackageToDatabases() throws Exception {
        Map<String, Set<String>> existingMapping = mAppSearchImpl.getPackageToDatabases();
        Map<String, Set<String>> expectedMapping = new ArrayMap<>();
        expectedMapping.putAll(existingMapping);

        // Has database1
        expectedMapping.put("package1", ImmutableSet.of("database1"));
        mAppSearchImpl.setSchema(
                "package1", "database1",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);

        // Has both databases
        expectedMapping.put("package1", ImmutableSet.of("database1", "database2"));
        mAppSearchImpl.setSchema(
                "package1", "database2",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);

        // Has both packages
        expectedMapping.put("package2", ImmutableSet.of("database1"));
        mAppSearchImpl.setSchema(
                "package2", "database1",
                Collections.singletonList(new AppSearchSchema.Builder("schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas1,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas2,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas3,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(mAppSearchImpl.getAllPrefixedSchemaTypes()).containsExactly(
                "package1$database1/type1",
                "package1$database2/type2",
                "package2$database1/type3",
                "VS#Pkg$VS#Db/VisibilityType"); // plus the stored Visibility schema
    }

    @FlakyTest(bugId = 204186664)
    @Test
    public void testReportUsage() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two docs
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id1", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument("package", "database", document1, /*logger=*/ null);
        mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert document for "package1"
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id1", "type").build();
        mAppSearchImpl.putDocument("package1", "database", document, /*logger=*/ null);

        // Insert schema for "package2"
        mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert two documents for "package2"
        document = new GenericDocument.Builder<>("namespace", "id1", "type").build();
        mAppSearchImpl.putDocument("package2", "database", document, /*logger=*/ null);
        document = new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument("package2", "database", document, /*logger=*/ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document for "package1", "database1"
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package1", "database1", document, /*logger=*/ null);

        // Add two documents for "package1", "database2"
        document = new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package1", "database2", document, /*logger=*/ null);
        document = new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument("package1", "database2", document, /*logger=*/ null);


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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

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
                /*callerPackageName=*/mContext.getPackageName(),
                /*callerUid=*/Process.myUid(),
                /*callerHasSystemAccess=*/false));

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id", "type").build(),
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
                "package",
                Process.INVALID_UID,
                /*callerHasSystemAccess=*/ false,
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/null);
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add two documents and persist them.
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document1, /*logger=*/null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/null);
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add two documents and persist them.
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document1, /*logger=*/null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace2", "id2", "type").build();
        mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/null);
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add two documents
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document1, /*logger=*/null);
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace1", "id2", "type").build();
        mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/null);

        StorageInfoProto storageInfo = mAppSearchImpl.getRawStorageInfoProto();

        // Simple checks to verify if we can get correct StorageInfoProto from IcingSearchEngine
        // No need to cover all the fields
        assertThat(storageInfo.getTotalStorageSize()).isGreaterThan(0);
        assertThat(
                storageInfo.getDocumentStorageInfo().getNumAliveDocuments())
                .isEqualTo(2);
        assertThat(
                storageInfo.getSchemaStoreStorageInfo().getNumSchemaTypes())
                .isEqualTo(2); // +1 for VisibilitySchema
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Insert a document which is too large
        GenericDocument document = new GenericDocument.Builder<>(
                "this_namespace_is_long_to_make_the_doc_big", "id", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document, /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Document \"id\" for package \"package\" serialized to 99 bytes, which exceeds"
                        + " limit of 80 bytes");

        // Make sure this failure didn't increase our document count. We should still be able to
        // index 1 document.
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document3, /*logger=*/ null));
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/ null));
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Make sure the limit is maintained
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document2, /*logger=*/ null));
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index 3 documents
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id3", "type").build(),
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Remove a document that doesn't exist
        assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.remove(
                        "package", "database", "namespace", "id4", /*removeStatsBuilder=*/null));

        // Should still fail
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 3 documents");

        // Remove a document that does exist
        mAppSearchImpl.remove(
                "package", "database", "namespace", "id2", /*removeStatsBuilder=*/null);

        // Now doc4 should work
        mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null);

        // The next one should fail again
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id5", "type").build(),
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index documents in package1/database1
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database2",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*logger=*/ null);

        // Indexing a third doc into package1 should fail (here we use database3)
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package1",
                        "database3",
                        new GenericDocument.Builder<>("namespace", "id3", "type").build(),
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" exceeded limit of 2 documents");

        // Indexing a doc into package2 should succeed
        mAppSearchImpl.putDocument(
                "package2",
                "database1",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // package1 should still be out of space
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package1",
                        "database4",
                        new GenericDocument.Builder<>("namespace", "id4", "type").build(),
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package1\" exceeded limit of 2 documents");

        // package2 has room for one more
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*logger=*/ null);

        // now package2 really is out of space
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package2",
                        "database3",
                        new GenericDocument.Builder<>("namespace", "id3", "type").build(),
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
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index 3 documents
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "tablet")
                        .build(),
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type")
                        .setPropertyString("body", "tabby")
                        .build(),
                /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id3", "type")
                        .setPropertyString("body", "grabby")
                        .build(),
                /*logger=*/ null);

        // Now we should get a failure
        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null));
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
                mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null));
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
        mAppSearchImpl.putDocument("package", "database", document4, /*logger=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id5", "type").build(),
                /*logger=*/ null);

        // We only deleted 2 docs so the next one should fail again
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id6", "type").build(),
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("body").build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.orig")
                        .build(),
                /*logger=*/ null);
        // Replace it with another doc
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.new")
                        .build(),
                /*logger=*/ null);

        // Index id2. This should pass but only because we check for replacements.
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*logger=*/ null);

        // Now we should get a failure on id3
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document3, /*logger=*/ null));
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("body").build())
                        .build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Index a document
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.orig")
                        .build(),
                /*logger=*/ null);
        // Replace it with another doc
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("body", "id1.new")
                        .build(),
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
                },
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        // Index id2. This should pass but only because we check for replacements.
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id2", "type").build(),
                /*logger=*/ null);

        // Now we should get a failure on id3
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument("package", "database", document3, /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 2 documents");
    }

    /**
     * Ensure that it is okay to register the same observer for multiple packages and that removing
     * the observer for one package doesn't remove it for the other.
     */
    @Test
    public void testRemoveObserver_onlyAffectsOnePackage() throws Exception {
        final String fakePackage = "com.android.appsearch.fake.package";

        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                /*schemas=*/ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);

        // Register an observer twice, on different packages.
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                /*listeningPackageName=*/mContext.getPackageName(),
                /*listeningUid=*/Process.myUid(),
                /*listeningPackageHasSystemAccess=*/false,
                /*targetPackageName=*/mContext.getPackageName(),
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);
        mAppSearchImpl.addObserver(
                /*listeningPackageName=*/mContext.getPackageName(),
                /*listeningUid=*/Process.myUid(),
                /*listeningPackageHasSystemAccess=*/false,
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
                /*logger=*/null);

        // Dispatch notifications and empty the observers
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        observer.clear();

        // Remove the observer from the fake package
        mAppSearchImpl.removeObserver(fakePackage, observer);

        // Index a second document
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace1", "id2", "Type1").build();
        mAppSearchImpl.putDocument(
                mContext.getPackageName(),
                "database1",
                doc2,
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> false;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerPackageName=*/mContext.getPackageName(),
                        /*callerUid=*/Process.myUid(),
                        /*callerHasSystemAccess=*/false));
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        GenericDocument getResult = mAppSearchImpl.globalGetDocument(
                "package",
                "database",
                "namespace1",
                "id1",
                /*typePropertyPaths=*/Collections.emptyMap(),
                /*callerPackageName=*/mContext.getPackageName(),
                /*callerUid=*/Process.myUid(),
                /*callerHasSystemAccess=*/false);
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id2",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerPackageName=*/mContext.getPackageName(),
                        /*callerUid=*/Process.myUid(),
                        /*callerHasSystemAccess=*/false));
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore) ->
                        callerUid == 1;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // Add a document and persist it.
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument("package", "database", document, /*logger=*/null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        AppSearchException unauthorizedException = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerPackageName=*/mContext.getPackageName(),
                        /*callerUid=*/Process.myUid(),
                        /*callerHasSystemAccess=*/false));

        mAppSearchImpl.remove("package", "database", "namespace1", "id1",
                /*removeStatsBuilder=*/null);

        AppSearchException noDocException = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.globalGetDocument(
                        "package",
                        "database",
                        "namespace1",
                        "id1",
                        /*typePropertyPaths=*/Collections.emptyMap(),
                        /*callerPackageName=*/"package",
                        /*callerUid=*/Process.myUid(),
                        /*callerHasSystemAccess=*/true)
        );

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
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        String prefix = PrefixUtil.createPrefix("package", "database1");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
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
        mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas1,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument1),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        String prefix1 = PrefixUtil.createPrefix("package1", "database");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument1 = new VisibilityDocument.Builder(prefix1 + "Email1")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .setCreationTimestampMillis(12345L)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);

        // Create Visibility Document for Email2
        VisibilityDocument visibilityDocument2 = new VisibilityDocument.Builder("Email2")
                .setNotDisplayedBySystem(false)
                .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .setCreationTimestampMillis(54321L)
                .build();
        List<AppSearchSchema> schemas2 =
                Collections.singletonList(new AppSearchSchema.Builder("Email2").build());

        // Set schema Email2 to package1 with a visibility document
        mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas2,
                /*visibilityDocuments=*/ ImmutableList.of(visibilityDocument2),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        // assert the visibility document is saved.
        VisibilityDocument expectedDocument2 = new VisibilityDocument.Builder(prefix2 + "Email2")
                .setNotDisplayedBySystem(false)
                .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .setCreationTimestampMillis(54321)
                .build();
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix2 + "Email2"))
                .isEqualTo(expectedDocument2);

        // Check the existing visibility document retains.
        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);
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

        // Set schema Email and its all-default visibility document to AppSearch database1
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
        mAppSearchImpl.setSchema(
                "packageName",
                "databaseName",
                schemas,
                ImmutableList.of(visibilityDocument),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

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

        // remove schema and visibility document
        mAppSearchImpl.setSchema(
                "packageName",
                "databaseName",
                ImmutableList.of(),
                ImmutableList.of(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // close and re-open AppSearchImpl, the visibility document removed
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        assertThat(mAppSearchImpl.mVisibilityStoreLocked.getVisibility(prefix + "Email")).isNull();
    }

    @Test
    public void testGetSchema_global() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Type").build());

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        VisibilityChecker mockVisibilityChecker =
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> true;
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ImmutableList.of(
                        new VisibilityDocument.Builder("Type")
                                .setNotDisplayedBySystem(true).build()),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);

        // Get this schema as another package
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                /*callerPackageName=*/"com.android.appsearch.fake.package",
                /*callerUid=*/1,
                /*callerHasSystemAccess=*/false);
        assertThat(getResponse.getSchemas()).containsExactlyElementsIn(schemas);
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).containsExactly("Type");
    }

    @Test
    public void testGetSchema_nonExistentApp() throws Exception {
        // Add a schema. The test loses meaning if the schema is completely empty.
        mAppSearchImpl.setSchema(
                "package",
                "database",
                Collections.singletonList(new AppSearchSchema.Builder("Type").build()),
                /*visibilityDocuments=*/ImmutableList.of(),
                /*forceOverride=*/false,
                /*version=*/0,
                /*setSchemaStatsBuilder=*/null);

        // Try to get the schema of a nonexistent package.
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "com.android.appsearch.fake.package",
                "database",
                /*callerPackageName=*/"package",
                /*callerUid=*/1,
                /*callerHasSystemAccess=*/false);
        assertThat(getResponse.getSchemas()).isEmpty();
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).isEmpty();
    }

    @Test
    public void testGetSchema_noAccess() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Type").build());
        mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityDocuments=*/ImmutableList.of(),
                /*forceOverride=*/false,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                /*callerPackageName=*/"com.android.fake.package",
                /*callerUid=*/1,
                /*callerHasSystemAccess=*/false);
        assertThat(getResponse.getSchemas()).isEmpty();
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).isEmpty();
        assertThat(getResponse.getVersion()).isEqualTo(0);

        // Make sure the test is hooked up right by calling getSchema with the same parameters but
        // from the same package
        getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                /*callerPackageName=*/"package",
                /*callerUid=*/1,
                /*callerHasSystemAccess=*/false);
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> prefixedSchema.endsWith("VisibleType");
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                mockVisibilityChecker);

        mAppSearchImpl.setSchema(
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

        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                /*callerPackageName=*/"com.android.appsearch.fake.package",
                /*callerUid=*/1,
                /*callerHasSystemAccess=*/false);
        assertThat(getResponse.getSchemas()).containsExactly(schemas.get(0));
        assertThat(getResponse.getSchemaTypesNotDisplayedBySystem()).containsExactly("VisibleType");
        assertThat(getResponse.getVersion()).isEqualTo(1);
    }

    @Test
    public void testDispatchObserver_samePackage_noVisStore_accept() throws Exception {
        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                /*listeningPackageName=*/mContext.getPackageName(),
                Process.myUid(),
                /*listeningPackageHasSystemAccess=*/false,
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
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> false;
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                rejectChecker);

        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                /*listeningPackageName=*/mContext.getPackageName(),
                Process.myUid(),
                /*listeningPackageHasSystemAccess=*/false,
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
        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // Register an observer from a simulated different package
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                /*listeningPackageName=*/"com.fake.Listening.package",
                Process.myUid(),
                /*listeningPackageHasSystemAccess=*/false,
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
        final int fakeListeningUid = 42;

        // Make a visibility checker that allows only fakeListeningPackage.
        final VisibilityChecker visibilityChecker =
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> callerUid == fakeListeningUid;
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                visibilityChecker);

        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                fakeListeningPackage,
                fakeListeningUid,
                /*listeningPackageHasSystemAccess=*/false,
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
        final int fakeListeningUid = 42;

        // Make a visibility checker that rejects everything.
        final VisibilityChecker rejectChecker =
                (packageName, prefixedSchema, callerUid, callerHasSystemAccess, visibilityStore)
                        -> false;
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/null,
                ALWAYS_OPTIMIZE,
                rejectChecker);

        mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);

        // Register an observer
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.addObserver(
                fakeListeningPackage,
                fakeListeningUid,
                /*listeningPackageHasSystemAccess=*/false,
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
                /*logger=*/null);
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();

        // Dispatch notifications
        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).isEmpty();
        assertThat(observer.getDocumentChanges()).isEmpty();
    }
}
