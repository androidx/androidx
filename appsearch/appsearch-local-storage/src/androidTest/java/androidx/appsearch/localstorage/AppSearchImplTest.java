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

import static androidx.appsearch.app.AppSearchResult.RESULT_ABORTED;
import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;
import static androidx.appsearch.app.AppSearchResult.RESULT_OUT_OF_SPACE;
import static androidx.appsearch.localstorage.util.PrefixUtil.addPrefixToDocument;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStore.BLOB_ANDROID_V_OVERLAY_DATABASE_NAME;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStore.BLOB_VISIBILITY_DATABASE_NAME;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStore.DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStore.VISIBILITY_PACKAGE_NAME;
import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.createMockVisibilityChecker;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.QueryStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityToDocumentConverter;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.appsearch.testutil.flags.RequiresFlagsDisabled;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.FlakyTest;

import com.google.android.appsearch.proto.AndroidVOverlayProto;
import com.google.android.appsearch.proto.PackageIdentifierProto;
import com.google.android.appsearch.proto.VisibilityConfigProto;
import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.IcingSearchEngineInterface;
import com.google.android.icing.proto.DebugInfoProto;
import com.google.android.icing.proto.DebugInfoVerbosity;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.GetSchemaResultProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.InitializeResultProto;
import com.google.android.icing.proto.PersistToDiskResultProto;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.ResetResultProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SetSchemaRequestProto;
import com.google.android.icing.proto.SetSchemaResultProto;
import com.google.android.icing.proto.StatusProto;
import com.google.android.icing.proto.StorageInfoProto;
import com.google.android.icing.proto.StorageInfoResultProto;
import com.google.android.icing.proto.StringIndexingConfig;
import com.google.android.icing.proto.TermMatchType;
import com.google.android.icing.protobuf.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"GuardedBy", "deprecation"})
public class AppSearchImplTest {
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    private static final StatusProto OK =
            StatusProto.newBuilder().setCode(StatusProto.Code.OK).build();
    private static final StatusProto ERROR =
            StatusProto.newBuilder().setCode(StatusProto.Code.INTERNAL).build();

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mAppSearchDir;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    // The caller access for this package
    private final CallerAccess mSelfCallerAccess = new CallerAccess(mContext.getPackageName());

    private AppSearchImpl mAppSearchImpl;
    private AppSearchConfig mUnlimitedConfig = new AppSearchConfigImpl(
            new UnlimitedLimitConfig(),
            new LocalStorageIcingOptionsConfig()
    );

    @Mock
    private IcingSearchEngine mMockIcingSearchEngine;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mAppSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    /**
     * Ensure that we can rewrite an incoming schema type by adding the database as a prefix. While
     * also keeping any other existing schema types that may already be part of Icing's persisted
     * schema.
     *
     * <p> This test is disabled when database-scoped schema operations are enabled, since
     * rewriteSchema will not be called with existing types from multiple prefixes in that case.
     */
    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_DATABASE_SCOPED_SCHEMA_OPERATIONS)
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
                .setSchemaType("RefType")
                .addParentTypes("Foo")
                .build();
        SchemaProto newSchema = SchemaProto.newBuilder()
                .addTypes(schemaTypeConfigProto1)
                .addTypes(schemaTypeConfigProto2)
                .addTypes(schemaTypeConfigProto3)
                .build();

        AppSearchImpl.RewrittenSchemaResults rewrittenSchemaResults = AppSearchImpl.rewriteSchema(
                createPrefix("package", "newDatabase"), existingSchemaBuilder,
                newSchema, mAppSearchImpl.useDatabaseScopedSchemaOperations());

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
                        .setSchemaType("package$newDatabase/RefType")
                        .addParentTypes("package$newDatabase/Foo")
                        .build())
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
                newSchema, mAppSearchImpl.useDatabaseScopedSchemaOperations());

        // Nothing was removed, but the method did rewrite the type name.
        assertThat(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet()).containsExactly(
                "package$existingDatabase/Foo");
        assertThat(rewrittenSchemaResults.mDeletedPrefixedTypes).isEmpty();

        // Same schema since nothing was added, but the database field should be populated if
        // useDatabaseScopedSchemaOperations() is true
        SchemaProto expectedSchema = existingSchemaBuilder.build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedSchema = getSchemaProtoWithDatabase(expectedSchema);
        }
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
                newSchema, mAppSearchImpl.useDatabaseScopedSchemaOperations());

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
                        .setSchemaType("package$existingDatabase/Bar"))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedSchema = getSchemaProtoWithDatabase(expectedSchema);
        }

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                mAppSearchDir, new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasDeSync()).isFalse();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeSchemaStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeDocumentStoreDataStatus())
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    public void testResetNativeInitFails_retryWithoutReset() throws Exception {
        // Setup Icing mock to fail the first init call, but then succeed
        setUpSuccessfulMocksForCreation();
        InitializeResultProto failedInit =
                InitializeResultProto.newBuilder().setStatus(ERROR).build();
        InitializeResultProto okInit =
                InitializeResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.initialize()).thenReturn(failedInit, okInit);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(initStats.hasReset()).isFalse();
    }

    @Test
    public void testResetNativeGetSchemaFails_retryWithoutReset() throws Exception {
        // Setup Icing mock to fail the first getSchema call, but then succeed
        setUpSuccessfulMocksForCreation();
        GetSchemaResultProto failedGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(ERROR).build();
        GetSchemaResultProto successGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getSchema()).thenReturn(failedGetSchema, successGetSchema);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(initStats.hasReset()).isFalse();
    }


    @Test
    public void testResetNativeGetStorageInfoFails_retryWithoutReset() throws Exception {
        // Setup Icing mock to fail the first getStorageInfo call, but then succeed
        setUpSuccessfulMocksForCreation();
        StorageInfoResultProto failedGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(ERROR).build();
        StorageInfoResultProto successGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getStorageInfo()).thenReturn(
                failedGetStorageInfo, successGetStorageInfo);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(initStats.hasReset()).isFalse();
    }

    @Test
    public void testResetNativeInitExhaustsRetries_resets() throws Exception {
        // Setup Icing mock to fail the first three init calls, but then succeed (if ever called
        // after)
        setUpSuccessfulMocksForCreation();
        InitializeResultProto failedInit =
                InitializeResultProto.newBuilder().setStatus(ERROR).build();
        InitializeResultProto okInit =
                InitializeResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.initialize()).thenReturn(
                failedInit, failedInit, failedInit, okInit);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasReset()).isTrue();
    }

    @Test
    public void testResetNativeGetSchemaExhaustsRetries_resets() throws Exception {
        // Setup Icing mock to fail the three getSchema call, but then succeed (if ever called).
        setUpSuccessfulMocksForCreation();
        GetSchemaResultProto failedGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(ERROR).build();
        GetSchemaResultProto successGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getSchema()).thenReturn(
                failedGetSchema, failedGetSchema, failedGetSchema, successGetSchema);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasReset()).isTrue();
    }

    @Test
    public void testResetNativeGetStorageInfoExhaustsRetries_resets() throws Exception {
        // Setup Icing mock to fail the first three getStorageInfo calls, but then succeed (if ever
        // called again)
        setUpSuccessfulMocksForCreation();

        StorageInfoResultProto failedGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(ERROR).build();
        StorageInfoResultProto successGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getStorageInfo()).thenReturn(
                failedGetStorageInfo, failedGetStorageInfo, failedGetStorageInfo,
                successGetStorageInfo);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasReset()).isTrue();
    }

    @Test
    public void testResetNativeCallsExhaustsRetries_resets() throws Exception {
        // Setup Icing mock to fail the first call for all native apis. This will exceed the max
        // retry limit and trigger a reset.
        setUpSuccessfulMocksForCreation();
        InitializeResultProto failedInit =
                InitializeResultProto.newBuilder().setStatus(ERROR).build();
        InitializeResultProto okInit =
                InitializeResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.initialize()).thenReturn(failedInit, okInit);

        GetSchemaResultProto failedGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(ERROR).build();
        GetSchemaResultProto successGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getSchema()).thenReturn(
                failedGetSchema, successGetSchema);

        StorageInfoResultProto failedGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(ERROR).build();
        StorageInfoResultProto successGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getStorageInfo()).thenReturn(
                failedGetStorageInfo, successGetStorageInfo);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasReset()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testReset_withBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        File blobFilesDir = new File(mAppSearchDir, "blob_dir/blob_files");

        // Insert schema
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Put a blob
        byte[] blobData = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] blobDigest = calculateDigest(blobData);
        AppSearchBlobHandle blobHandle = AppSearchBlobHandle.createWithSha256(
                blobDigest, mContext.getPackageName(), "database1", "namespace1");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob(
                mContext.getPackageName(), "database1", blobHandle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(blobData);
            outputStream.flush();
        }
        // Commit and read the blob.
        mAppSearchImpl.commitBlob(mContext.getPackageName(), "database1", blobHandle);
        byte[] readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd = mAppSearchImpl.openReadBlob(
                mContext.getPackageName(), "database1", blobHandle);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(blobData);
        // Check that the blob file is created by AppSearch.
        if (Flags.enableAppSearchManageBlobFiles()) {
            assertThat(blobFilesDir.list()).asList().hasSize(1);
        }

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
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                initStatsBuilder,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasDeSync()).isFalse();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeSchemaStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeDocumentStoreDataStatus())
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

        // Make sure blob files are deleted.
        if (Flags.enableAppSearchManageBlobFiles()) {
            assertThat(blobFilesDir.list()).isEmpty();
        }

        // Make sure the index can now be used successfully
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                Collections.singletonList(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Put a blob
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob(
                mContext.getPackageName(), "database1", blobHandle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(blobData);
            outputStream.flush();
        }
        // Commit and read the blob.
        mAppSearchImpl.commitBlob(mContext.getPackageName(), "database1", blobHandle);
        readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd = mAppSearchImpl.openReadBlob(
                mContext.getPackageName(), "database1", blobHandle);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(blobData);
        // Check that the blob file is created by AppSearch.
        if (Flags.enableAppSearchManageBlobFiles()) {
            assertThat(blobFilesDir.list()).asList().hasSize(1);
        }
    }

    @Test
    public void testResetWithSchemaDatabaseMigration() throws Exception {
        IcingSearchEngineOptions.Builder optionsBuilder =
                IcingSearchEngineOptions.newBuilder(mUnlimitedConfig.toIcingSearchEngineOptions(
                        mAppSearchDir.getAbsolutePath(),  /* isVMEnabled= */ false));
        // Initialize Icing without schema database enabled.
        IcingSearchEngine icingSearchEngine = new IcingSearchEngine(
                optionsBuilder.setEnableSchemaDatabase(false).build());
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                // Initializing with a custom icing instance will cause AppSearch to assume
                // isVMEnabled. Therefore we cannot call AppSearch::setSchema below since it'll
                // still use database-scoped operations.
                icingSearchEngine,
                ALWAYS_OPTIMIZE);

        SchemaProto existingSchema = mAppSearchImpl.getSchemaProtoLocked();
        // Insert some schemas in 2 databases. We need to use the full SchemaProto and call Icing's
        // set schema API directly as AppSearch will use database-scoped schema operation since
        // we initialized with a custom icing instance (which AppSearch understands as having VM
        // enabled). This will fail as the Icing instance has not enabled schema database.
        SchemaProto expectedProto =
                SchemaProto.newBuilder()
                        .addTypes(
                                SchemaTypeConfigProto.newBuilder()
                                        .setSchemaType("package$database1/Type1")
                                        .setDescription("")
                                        .setVersion(0))
                        .addTypes(
                                SchemaTypeConfigProto.newBuilder()
                                        .setSchemaType("package$database1/Type2")
                                        .setDescription("")
                                        .setVersion(0))
                        .addTypes(
                                SchemaTypeConfigProto.newBuilder()
                                        .setSchemaType("package$database2/Type3")
                                        .setDescription("")
                                        .setVersion(0))
                        .build();
        SchemaProto fullSchema =
                SchemaProto.newBuilder(existingSchema)
                        .addAllTypes(expectedProto.getTypesList())
                        .build();
        SetSchemaRequestProto requestProto =
                SetSchemaRequestProto.newBuilder()
                        .setSchema(fullSchema)
                        .setIgnoreErrorsAndDeleteDocuments(false)
                        .build();
        assertThat(icingSearchEngine.setSchemaWithRequestProto(requestProto).getStatus().getCode())
                .isEqualTo(StatusProto.Code.OK);

        // We need to get the full schema here since Icing doesn't have schema database enabled,
        // which also disables the getSchemaForPrefix API. The schema should be exactly the same
        // as what we've just set, without the database fields being populated.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(fullSchema.getTypesList());

        // Reinitialize Icing and AppSearch, this time with schema database enabled.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        icingSearchEngine = new IcingSearchEngine(
                optionsBuilder.setEnableSchemaDatabase(true).build());
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                icingSearchEngine,
                ALWAYS_OPTIMIZE);

        // Initialization should NOT trigger a recovery
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);

        // GetSchema for db1 and db2. The old schema should have the database field populated
        // after the migration
        SchemaProto expectedDb1Proto = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database1/Type1")
                        .setDatabase("package$database1/")
                        .setDescription("")
                        .setVersion(0))
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database1/Type2")
                        .setDatabase("package$database1/")
                        .setDescription("")
                        .setVersion(0))
                .build();
        SchemaProto expectedDb2Proto = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database2/Type3")
                        .setDatabase("package$database2/")
                        .setDescription("")
                        .setVersion(0))
                .build();
        assertThat(
                mAppSearchImpl.getSchemaProtoForPrefixLocked("package$database1/").getTypesList())
                .containsExactlyElementsIn(expectedDb1Proto.getTypesList());
        assertThat(
                mAppSearchImpl.getSchemaProtoForPrefixLocked("package$database2/").getTypesList())
                .containsExactlyElementsIn(expectedDb2Proto.getTypesList());

        // SetSchema for database 1 again. We can use the AppSearch API this time since we've
        // enabled database-scoped schema operation for Icing too. Check that the old db1 schema
        // gets overridden and db2 is not affected
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("Type4").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // expectedDb1Proto has changed. The proto should contain the database field.
        expectedDb1Proto = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database1/Type4")
                        .setDatabase("package$database1/")
                        .setDescription("")
                        .setVersion(0))
                .build();
        assertThat(
                mAppSearchImpl.getSchemaProtoForPrefixLocked("package$database1/").getTypesList())
                .containsExactlyElementsIn(expectedDb1Proto.getTypesList());
        assertThat(
                mAppSearchImpl.getSchemaProtoForPrefixLocked("package$database2/").getTypesList())
                .containsExactlyElementsIn(expectedDb2Proto.getTypesList());
    }

    @Test
    @RequiresFlagsEnabled({
            Flags.FLAG_ENABLE_RESET_VISIBILITY_STORE,
            Flags.FLAG_ENABLE_DATABASE_SCOPED_SCHEMA_OPERATIONS})
    public void testResetBlobStore() throws Exception {
        // Setup Icing mock to success to all calls in initialize expect the setSchema call of
        // VisibilityStore.
        InitializeResultProto okInit =
                InitializeResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.initialize()).thenReturn(okInit);

        GetSchemaResultProto successGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getSchema()).thenReturn(successGetSchema);
        when(mMockIcingSearchEngine.getSchemaForDatabase(any())).thenReturn(successGetSchema);

        StorageInfoResultProto successGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getStorageInfo()).thenReturn(successGetStorageInfo);

        // Setup Icing mock to fail the first time setSchema call for visibility type. Success to
        // the second time setSchema call which is after the reset.
        SetSchemaResultProto failedSetSchemaResult =
                SetSchemaResultProto.newBuilder().setStatus(ERROR).build();
        SetSchemaResultProto okSetSchemaResult =
                SetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.setSchemaWithRequestProto(any()))
                .thenReturn(failedSetSchemaResult, okSetSchemaResult);

        ResetResultProto successReset =
                ResetResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.reset()).thenReturn(successReset);

        PersistToDiskResultProto successPersist =
                PersistToDiskResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.persistToDisk(any())).thenReturn(successPersist);

        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // Initializing with a custom icing instance will cause AppSearch to assume
        // isVMEnabled. This will enable both database-scoped operations and init retries.
        mAppSearchImpl =
                AppSearchImpl.create(
                        mAppSearchDir,
                        new AppSearchConfigImpl(
                                new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                        initStatsBuilder,
                        /* visibilityChecker= */ null,
                        /* revocableFileDescriptorStore= */ null,
                        mMockIcingSearchEngine,
                        ALWAYS_OPTIMIZE);

        // Check recovery state
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats).isNotNull();
        assertThat(initStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(initStats.hasReset()).isTrue();
    }

    @Test
    public void testQueryEmptyDatabase() throws Exception {
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package", "EmptyDatabase", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).isEmpty();
    }

    @Test
    public void testQueryWithPageSizeLimit() throws Exception {
        IcingSearchEngineOptions icingOptions =
                IcingSearchEngineOptions.newBuilder(mUnlimitedConfig.toIcingSearchEngineOptions(
                                mAppSearchDir.getAbsolutePath(),  /* isVMEnabled= */ false))
                        .setEnableStrictPageByteSizeLimit(true)
                        // We need to enable schema database as by passing in a custom Icing
                        // instance, AppSearch assumes that the VM is enabled and will use
                        // database-scoped schema operations. We need Icing's options to match
                        // this in order for setSchema to work properly.
                        .setEnableSchemaDatabase(true)
                        .build();
        IcingSearchEngine icingSearchEngine = new IcingSearchEngine(icingOptions);
        AppSearchConfig appSearchConfig = new AppSearchConfigImpl(
                new UnlimitedLimitConfig(),
                new LocalStorageIcingOptionsConfig()
        ) {
            @Override
            // Set a very small page byte size limit -- this means that each search result page
            // would normally be able to fit one result only.
            public int getMaxPageBytesLimit() {
                return 1;
            }
        };
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                appSearchConfig,
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                icingSearchEngine,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schema = ImmutableList.of(AppSearchEmail.SCHEMA);
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert 12 documents
        for (int i = 0; i < 12; i++) {
            AppSearchEmail email =
                    new AppSearchEmail.Builder("namespace", "id" + i)
                            .setFrom("from@example.com")
                            .setTo("to1@example.com", "to2@example.com")
                            .setSubject("testPut example")
                            .setBody("This is the body of the testPut email")
                            .build();
            mAppSearchImpl.putDocument(
                    "package1",
                    "database1",
                    email,
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/ null);
        }

        // Search for the documents with requested page size of 5, 5 documents should be returned
        // despite the page byte-size limit
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(
                        TermMatchType.Code.PREFIX_VALUE).setResultCountPerPage(5).build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(5);
        assertThat(searchResultPage.getNextPageToken()).isNotEqualTo(
                SearchResultPage.EMPTY_PAGE_TOKEN);

        // Do getNextPage. A full page of 5 results should be returned again.
        searchResultPage = mAppSearchImpl.getNextPage("package1",
                searchResultPage.getNextPageToken(), /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(5);
        assertThat(searchResultPage.getNextPageToken()).isNotEqualTo(
                SearchResultPage.EMPTY_PAGE_TOKEN);

        // Do getNextPage one last time. Only 2 results should remain, and getNextPageToken should
        // be invalid after this call.
        searchResultPage = mAppSearchImpl.getNextPage("package1",
                searchResultPage.getNextPageToken(), /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getNextPageToken()).isEqualTo(
                SearchResultPage.EMPTY_PAGE_TOKEN);
    }

    @Test
    public void testBatchPut_emptyList_noDocInserted() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert no documents
        List<GenericDocument> documents = new ArrayList<>();

        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package1",
                "database1",
                documents,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                PersistType.Code.LITE);

        assertThat(resultBuilder.build().getAll()).isEmpty();
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        assertThat(searchResultPage.getResults()).isEmpty();
    }

    @Test
    public void testBatchPut_docsInsertedCorrectly() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert three package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        GenericDocument document3 = new GenericDocument.Builder<>("namespace", "id3",
                "schema1").build();
        List<GenericDocument> documents = Arrays.asList(document1, document2, document3);

        AppSearchBatchResult.Builder<String, Void> batchResultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package1",
                "database1",
                documents,
                batchResultBuilder,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                PersistType.Code.LITE);
        AppSearchBatchResult<String, Void> batchResult = batchResultBuilder.build();

        // Check batchResult
        assertThat(batchResult.getSuccesses()).containsExactly("id1", null,
                "id2", null, "id3", null).inOrder();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        assertThat(searchResultPage.getResults()).hasSize(3);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document3);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(document2);
        assertThat(searchResultPage.getResults().get(2).getGenericDocument()).isEqualTo(document1);
    }

    @Test
    public void testBatchPut_docsInsertedCorrectly_withoutPersistToDisk() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert three package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        GenericDocument document3 = new GenericDocument.Builder<>("namespace", "id3",
                "schema1").build();
        List<GenericDocument> documents = Arrays.asList(document1, document2, document3);

        AppSearchBatchResult.Builder<String, Void> batchResultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package1",
                "database1",
                documents,
                batchResultBuilder,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                // Specify UNKNOWN PersistType to indicate not to call persistToDisk at the end.
                PersistType.Code.UNKNOWN);
        AppSearchBatchResult<String, Void> batchResult = batchResultBuilder.build();

        // Check batchResult
        assertThat(batchResult.getSuccesses()).containsExactly("id1", null,
                "id2", null, "id3", null).inOrder();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        assertThat(searchResultPage.getResults()).hasSize(3);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document3);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(document2);
        assertThat(searchResultPage.getResults().get(2).getGenericDocument()).isEqualTo(document1);
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    public void testGlobalQuery_withJoin_packageFilter() throws Exception {
        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        // We need to share across packages
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(true);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE
        );

        // Insert package1 schema
        List<AppSearchSchema> personSchema =
                ImmutableList.of(new AppSearchSchema.Builder("personSchema").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                personSchema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        AppSearchSchema.StringPropertyConfig personField =
                new AppSearchSchema.StringPropertyConfig.Builder("personId")
                        .setJoinableValueType(AppSearchSchema.StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID).build();
        // Insert package2 schema
        List<AppSearchSchema> callSchema =
                ImmutableList.of(new AppSearchSchema.Builder("callSchema")
                        .addProperty(personField).build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                callSchema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        List<AppSearchSchema> textSchema =
                ImmutableList.of(new AppSearchSchema.Builder("textSchema")
                        .addProperty(personField).build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package3",
                "database3",
                textSchema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package1 document
        GenericDocument person = new GenericDocument.Builder<>("namespace", "id",
                "personSchema")
                .build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                person,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Insert package2 document
        GenericDocument call =
                new GenericDocument.Builder<>("namespace", "id", "callSchema")
                        .setPropertyString("personId", "package1$database1/namespace#id").build();
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                call,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Insert package3 document
        GenericDocument text =
                new GenericDocument.Builder<>("namespace", "id", "textSchema")
                        .setPropertyString("personId", "package1$database1/namespace#id").build();
        mAppSearchImpl.putDocument(
                "package3",
                "database3",
                text,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Filter on parent spec only
        SearchSpec nested = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .build();
        JoinSpec join = new JoinSpec.Builder("personId").setNestedSearch("", nested).build();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setJoinSpec(join)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery("", searchSpec,
                new CallerAccess("package1"),
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        SearchResult result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(2);

        // Filter on neither
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.globalQuery("", searchSpec,
                new CallerAccess("package1"),
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(3);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call);
        assertThat(searchResultPage.getResults().get(2).getGenericDocument()).isEqualTo(text);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(2);

        // Filter on child spec only
        nested = new SearchSpec.Builder()
                .addFilterPackageNames("package2")
                .build();
        join = new JoinSpec.Builder("personId")
                .setNestedSearch("", nested)
                .build();

        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.globalQuery("", searchSpec,
                new CallerAccess("package1"),
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(3);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call);
        assertThat(searchResultPage.getResults().get(2).getGenericDocument()).isEqualTo(text);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(call);

        // Filter on both
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.globalQuery("", searchSpec,
                new CallerAccess("package1"),
                /*logger=*/ null);
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(call);
    }

    @Test
    public void testQueryInvalidPackages_withJoin() throws Exception {
        // Make sure that local queries with joinspecs including package filters don't access
        // other packages.

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        // We need to share across packages
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(true);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE
        );

        AppSearchSchema.StringPropertyConfig personField =
                new AppSearchSchema.StringPropertyConfig.Builder("personId")
                        .setJoinableValueType(
                                AppSearchSchema.StringPropertyConfig
                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build();

        // Insert package1 schema
        List<AppSearchSchema> personAndCallSchema =
                ImmutableList.of(new AppSearchSchema.Builder("personSchema").build(),
                        new AppSearchSchema.Builder("callSchema")
                                .addProperty(personField).build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                personAndCallSchema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package2 schema
        List<AppSearchSchema> callSchema =
                ImmutableList.of(new AppSearchSchema.Builder("callSchema")
                        .addProperty(personField).build());
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                callSchema,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert package1 document
        GenericDocument person = new GenericDocument.Builder<>("namespace", "person",
                "personSchema")
                .build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                person,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument call1 = new GenericDocument.Builder<>("namespace", "id1", "callSchema")
                .setPropertyString("personId", "package1$database1/namespace#person").build();
        GenericDocument call2 = new GenericDocument.Builder<>("namespace", "id2", "callSchema")
                .setPropertyString("personId", "package1$database1/namespace#person").build();

        // Insert package1 action document
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                call1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Insert package2 action document
        mAppSearchImpl.putDocument(
                "package2",
                "database2",
                call2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Invalid parent spec filter
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1", "package2")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);
        // Only package1 documents should be returned
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call1);

        // Valid parent spec filter with invalid child spec filter
        SearchSpec nested = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .addFilterPackageNames("package1", "package2")
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .build();
        JoinSpec join = new JoinSpec.Builder("personId").setNestedSearch("", nested).build();
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Only package1 documents should be returned, for both the outer and nested searches
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call1);
        SearchResult result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(call1);

        // Valid parent spec, but child spec package filters only contain other packages
        nested = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .addFilterPackageNames("package2", "package3")
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .build();
        join = new JoinSpec.Builder("personId").setNestedSearch("", nested).build();
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Package1 documents should be returned, but no packages should be joined
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call1);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).isEmpty();

        // Valid parent spec, empty child spec package filters
        nested = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .build();
        join = new JoinSpec.Builder("personId").setNestedSearch("", nested).build();
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);

        // Only package1 documents should be returned, for both the outer and nested searches
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call1);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(call1);

        // Valid parent spec filter with valid child spec filter
        nested = new SearchSpec.Builder().build();
        join = new JoinSpec.Builder("personId").setNestedSearch("", nested).build();
        searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .addFilterPackageNames("package1")
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setOrder(SearchSpec.ORDER_ASCENDING)
                .setJoinSpec(join)
                .build();
        searchResultPage = mAppSearchImpl.query("package1", "database1", "",
                searchSpec, /*logger=*/ null);
        // Should work as expected
        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(person);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(call1);
        result = searchResultPage.getResults().get(0);
        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(call1);
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

    @Ignore("b/273733335")
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    @RequiresFlagsEnabled({
            Flags.FLAG_ENABLE_RESULT_ABORTED,
            Flags.FLAG_ENABLE_THROW_EXCEPTION_FOR_NATIVE_NOT_FOUND_PAGE_TOKEN})
    public void testEvictedNextPageToken_flagEnabledShouldThrow() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        assertThat(nextPageToken).isNotEqualTo(0);

        // Call Optimize.
        mAppSearchImpl.optimize(/* builder= */ null);

        // All page tokens are evicted after optimize, so AppSearchException with code
        // RESULT_ABORTED will be thrown.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getNextPage("package1",
                        nextPageToken, /* statsBuilder= */ null));
        assertThat(e.getResultCode()).isEqualTo(RESULT_ABORTED);
        assertThat(e).hasMessageThat().contains(
                "Page token not found. It is usually caused by pagination cache eviction.");
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_THROW_EXCEPTION_FOR_NATIVE_NOT_FOUND_PAGE_TOKEN)
    public void testEvictedNextPageToken_flagDisabledShouldReturnEmptyResult() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        assertThat(nextPageToken).isNotEqualTo(0);

        // Call Optimize.
        mAppSearchImpl.optimize(/* builder= */ null);

        // All page tokens are evicted after optimize. getNextPage should return an empty page if
        // the flag is disabled.
        SearchResultPage searchResultPage2 =
                mAppSearchImpl.getNextPage("package1", nextPageToken, /* statsBuilder= */ null);
        assertThat(searchResultPage2.getResults()).isEmpty();
        assertThat(searchResultPage2.getNextPageToken()).isEqualTo(0);
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database1/Email")
                        .setDescription("")
                        .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Document")
                                .setDescription("")
                                .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

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
                        /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Check Document schema is removed.
        expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email")
                                .setDescription("")
                                .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database2",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Document")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Email")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Document")
                                .setDescription("")
                                .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType list, database 1 should only contain Email but database 2
        // remains in same.
        expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database1/Email")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Email")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("package$database2/Document")
                                .setDescription("")
                                .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

        // Check nothing changed in database2.
        expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testWriteAndReadBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }

        // commit the change and read the blob.
        mAppSearchImpl.commitBlob("package", "db1", handle);
        byte[] readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd =  mAppSearchImpl.openReadBlob("package", "db1", handle);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testWriteAfterCommit_notAllowed() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        // Open a pfd for write, write the blob data without close the pfd.
        ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
        try (FileOutputStream outputStream = new FileOutputStream(writePfd.getFileDescriptor())) {
            outputStream.write(data);
            outputStream.flush();
        }

        // Commit the blob.
        mAppSearchImpl.commitBlob("package", "db1", handle);

        // Keep writing to the pfd for write.
        assertThrows(IOException.class,
                () -> {
                try (FileOutputStream outputStream =
                         new FileOutputStream(writePfd.getFileDescriptor())) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemovePendingBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                    OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }

        mAppSearchImpl.commitBlob("package", "db1", handle);

        // Remove the committed blob
        mAppSearchImpl.removeBlob("package", "db1", handle);

        // Read will get NOT_FOUND
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openReadBlob("package", "db1", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemoveCommittedBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }

        // Remove the blob
        mAppSearchImpl.removeBlob("package", "db1", handle);

        // Commit will get NOT_FOUND
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.commitBlob("package", "db1", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemoveAndReWriteBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] wrongData = generateRandomBytes(10 * 1024); // 10 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                    OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            // write wrong data
            outputStream.write(wrongData);
            outputStream.flush();
        }

        // Remove the blob
        mAppSearchImpl.removeBlob("package", "db1", handle);

        // reopen and rewrite
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }

        // commit the change and read the blob.
        mAppSearchImpl.commitBlob("package", "db1", handle);
        byte[] readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd =  mAppSearchImpl.openReadBlob("package", "db1", handle);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenReadForWrite_notAllowed() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                    OutputStream outputStream = new ParcelFileDescriptor
                            .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }

        // commit the change and read the blob.
        mAppSearchImpl.commitBlob("package", "db1", handle);

        // Open output stream on read-only pfd.
        assertThrows(IOException.class, () -> {
            try (ParcelFileDescriptor readPfd =
                         mAppSearchImpl.openReadBlob("package", "db1", handle);
                    OutputStream outputStream = new ParcelFileDescriptor
                            .AutoCloseOutputStream(readPfd)) {
                outputStream.write(data);
            }
        });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenWriteForRead_allowed() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        // openWriteBlob returns read and write fd.
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                InputStream inputStream = new ParcelFileDescriptor
                        .AutoCloseInputStream(writePfd)) {
            inputStream.read(new byte[10]);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenMultipleBlobForWrite() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");

        // only allow open 1 fd for writing.
        try (ParcelFileDescriptor writePfd1 =
                     mAppSearchImpl.openWriteBlob("package", "db1", handle);
                ParcelFileDescriptor writePfd2 =
                        mAppSearchImpl.openWriteBlob("package", "db1", handle)) {
            assertThat(writePfd1).isEqualTo(writePfd2);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenMultipleBlobForRead() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");

        // write a blob first.
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        // commit the change and read the blob.
        mAppSearchImpl.commitBlob("package", "db1", handle);

        // allow open multiple fd for reading.
        try (ParcelFileDescriptor readPfd1 =
                     mAppSearchImpl.openReadBlob("package", "db1", handle);
                ParcelFileDescriptor readPfd2 =
                        mAppSearchImpl.openReadBlob("package", "db1", handle)) {
            assertThat(readPfd1).isNotEqualTo(readPfd2);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOptimizeBlob() throws Exception {
        // Create a new AppSearchImpl with lower orphan blob time to live.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public long getOrphanBlobTimeToLiveMs() {
                                // 0 will make it non-expire
                                return 1L;
                            }
                        }),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Write the blob and commit it.
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "namespace");
        ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
        try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        writePfd.close();
        mAppSearchImpl.commitBlob("package", "db1", handle);

        mAppSearchImpl.persistToDisk(PersistType.Code.FULL);

        // Optimize remove the expired orphan blob.
        mAppSearchImpl.optimize(/*builder=*/null);
        AppSearchException e = assertThrows(AppSearchException.class, () -> {
            mAppSearchImpl.openReadBlob("package", "db1", handle);
        });
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOptimizeBlobWithDocument() throws Exception {
        // Create a new AppSearchImpl with lower orphan blob time to live.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public long getOrphanBlobTimeToLiveMs() {
                                // 0 will make it non-expire
                                return 1L;
                            }
                        }),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Write the blob and commit it.
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "namespace");
        ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle);
        try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        writePfd.close();
        mAppSearchImpl.commitBlob("package", "db1", handle);

        // Put a document link that blob handle.
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "db1",
                ImmutableList.of(schema),
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyBlobHandle("blob", handle)
                .build();
        mAppSearchImpl.putDocument(
                "package",
                "db1",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        mAppSearchImpl.persistToDisk(PersistType.Code.FULL);

        // Optimize won't remove the blob since it has reference document.
        mAppSearchImpl.optimize(/*builder=*/null);
        byte[] readBytes = new byte[20];
        try (ParcelFileDescriptor readPfd =  mAppSearchImpl.openReadBlob("package", "db1", handle);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);

        mAppSearchImpl.remove("package", "db1",  "namespace", "id", /*statsBuilder=*/ null);

        // The blob is orphan now and optimize will remove it.
        mAppSearchImpl.optimize(/*builder=*/null);
        AppSearchException e = assertThrows(AppSearchException.class, () -> {
            mAppSearchImpl.openReadBlob("package", "db1", handle);
        });
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRevokeFileDescriptor() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        try (ParcelFileDescriptor writePfd =
                     mAppSearchImpl.openWriteBlob("package", "db1", handle)) {
            // Clear package data and all file descriptor to that package will be revoked.
            mAppSearchImpl.clearPackageData("package");

            assertThrows(IOException.class, () -> {
                try (OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
                    outputStream.write(data);
                }
            });
        }

        // reopen file descriptor could work.
        try (ParcelFileDescriptor writePfd2 =
                     mAppSearchImpl.openWriteBlob("package", "db1", handle)) {
            try (OutputStream outputStream = new ParcelFileDescriptor
                    .AutoCloseOutputStream(writePfd2)) {
                outputStream.write(data);
            }
            // close the AppSearchImpl will revoke all sent fds.
            mAppSearchImpl.close();
            assertThrows(IOException.class, () -> {
                try (OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd2)) {
                    outputStream.write(data);
                }
            });
        }
    }

    // Verify the blob handle won't sent request to Icing. So no need to enable
    // FLAG_ENABLE_BLOB_STORE.
    @Test
    public void testInvalidBlobHandle() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openWriteBlob("wrongPackageName", "db1", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(e.getMessage()).contains("Blob package doesn't match calling package, "
                + "calling package: wrongPackageName, blob package: package");

        e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openWriteBlob("package", "wrongDb", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(e.getMessage()).contains("Blob database doesn't match calling database, "
                + "calling database: wrongDb, blob database: db1");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testSetBlobVisibility() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        SchemaVisibilityConfig visibleToConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .build();
        InternalVisibilityConfig config = new InternalVisibilityConfig.Builder("namespace")
                .setNotDisplayedBySystem(false)
                .addVisibleToConfig(visibleToConfig)
                .build();

        String prefix = PrefixUtil.createPrefix("package", "db1");
        mAppSearchImpl.setBlobNamespaceVisibility("package", "db1", ImmutableList.of(config));

        // Expect the config will be added prefix.
        InternalVisibilityConfig expectedConfig =
                new InternalVisibilityConfig.Builder(prefix + "namespace")
                        .setNotDisplayedBySystem(false)
                        .addVisibleToConfig(visibleToConfig)
                        .build();
        assertThat(mAppSearchImpl.mBlobVisibilityStoreLocked
                .getVisibility(prefix + "namespace"))
                .isEqualTo(expectedConfig);

        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        GenericDocument visibilityDocument = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                /*id=*/ prefix + "namespace",
                /*typePropertyPaths=*/ Collections.emptyMap());
        GenericDocument overLayVisibilityDocument = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                /*id=*/ prefix + "namespace",
                /*typePropertyPaths=*/ Collections.emptyMap());

        InternalVisibilityConfig outputConfig = VisibilityToDocumentConverter
                .createInternalVisibilityConfig(visibilityDocument, overLayVisibilityDocument);

        assertThat(outputConfig).isEqualTo(expectedConfig);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testSetBlobVisibility_notSupported() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        SchemaVisibilityConfig visibleToConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .build();
        InternalVisibilityConfig config = new InternalVisibilityConfig.Builder("namespace")
                .setNotDisplayedBySystem(false)
                .addVisibleToConfig(visibleToConfig)
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mAppSearchImpl.setBlobNamespaceVisibility(
                        "package", "db1", ImmutableList.of(config)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemoveBlobVisibility() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        SchemaVisibilityConfig visibleToConfig1 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkgBar1", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkgFoo1", new byte[32]))
                .build();
        InternalVisibilityConfig config1 = new InternalVisibilityConfig.Builder("namespace1")
                .setNotDisplayedBySystem(false)
                .addVisibleToConfig(visibleToConfig1)
                .build();
        SchemaVisibilityConfig visibleToConfig2 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkgBar2", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(3, 4))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkgFoo2", new byte[32]))
                .build();
        InternalVisibilityConfig config2 = new InternalVisibilityConfig.Builder("namespace2")
                .setNotDisplayedBySystem(false)
                .addVisibleToConfig(visibleToConfig2)
                .build();

        String prefix = PrefixUtil.createPrefix("package", "db1");
        mAppSearchImpl.setBlobNamespaceVisibility("package", "db1",
                ImmutableList.of(config1, config2));

        // Expect the config will be added prefix.
        InternalVisibilityConfig expectedConfig1 =
                new InternalVisibilityConfig.Builder(prefix + "namespace1")
                        .setNotDisplayedBySystem(false)
                        .addVisibleToConfig(visibleToConfig1)
                        .build();
        assertThat(mAppSearchImpl.mBlobVisibilityStoreLocked
                .getVisibility(prefix + "namespace1"))
                .isEqualTo(expectedConfig1);

        InternalVisibilityConfig expectedConfig2 =
                new InternalVisibilityConfig.Builder(prefix + "namespace2")
                        .setNotDisplayedBySystem(false)
                        .addVisibleToConfig(visibleToConfig2)
                        .build();
        assertThat(mAppSearchImpl.mBlobVisibilityStoreLocked
                .getVisibility(prefix + "namespace2"))
                .isEqualTo(expectedConfig2);

        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        GenericDocument visibilityDocument1 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                /*id=*/ prefix + "namespace1",
                /*typePropertyPaths=*/ Collections.emptyMap());
        GenericDocument overLayVisibilityDocument1 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                /*id=*/ prefix + "namespace1",
                /*typePropertyPaths=*/ Collections.emptyMap());
        InternalVisibilityConfig outputConfig1 = VisibilityToDocumentConverter
                .createInternalVisibilityConfig(visibilityDocument1, overLayVisibilityDocument1);
        assertThat(outputConfig1).isEqualTo(expectedConfig1);

        GenericDocument visibilityDocument2 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                /*id=*/ prefix + "namespace2",
                /*typePropertyPaths=*/ Collections.emptyMap());
        GenericDocument overLayVisibilityDocument2 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                /*id=*/ prefix + "namespace2",
                /*typePropertyPaths=*/ Collections.emptyMap());
        InternalVisibilityConfig outputConfig2 = VisibilityToDocumentConverter
                .createInternalVisibilityConfig(visibilityDocument2, overLayVisibilityDocument2);
        assertThat(outputConfig2).isEqualTo(expectedConfig2);

        // remove config1 by only set config2 to db
        mAppSearchImpl.setBlobNamespaceVisibility("package", "db1",
                /*visibilityConfigs=*/ImmutableList.of(config2));

        // Check config 1 is removed from VisibilityStore
        assertThat(mAppSearchImpl.mBlobVisibilityStoreLocked
                .getVisibility(prefix + "namespace1")).isNull();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        BLOB_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "namespace1",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo(
                "Document (VS#Pkg$VSBlob#Db/, package$db1/namespace1) not found.");
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        BLOB_ANDROID_V_OVERLAY_DATABASE_NAME,
                        VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                        /*id=*/ prefix + "namespace1",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo(
                "Document (VS#Pkg$VSBlob#AndroidVDb/androidVOverlay, "
                        + "package$db1/namespace1) not found.");

        // Config2 remains.
        assertThat(mAppSearchImpl.mBlobVisibilityStoreLocked
                .getVisibility(prefix + "namespace2"))
                .isEqualTo(expectedConfig2);
        visibilityDocument2 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                /*id=*/ prefix + "namespace2",
                /*typePropertyPaths=*/ Collections.emptyMap());
        overLayVisibilityDocument2 = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                BLOB_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                /*id=*/ prefix + "namespace2",
                /*typePropertyPaths=*/ Collections.emptyMap());
        outputConfig2 = VisibilityToDocumentConverter
                .createInternalVisibilityConfig(visibilityDocument2, overLayVisibilityDocument2);

        assertThat(outputConfig2).isEqualTo(expectedConfig2);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGlobalReadBlob_notSupported() throws Exception {
        String visiblePrefix = PrefixUtil.createPrefix("package", "db1");
        VisibilityChecker mockVisibilityChecker =
                createMockVisibilityChecker(ImmutableSet.of(visiblePrefix + "visibleNamespace"));
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "ns");
        // nonVisibleHandle is not visible to the caller.
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(handle, mSelfCallerAccess));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGlobalReadBlob() throws Exception {
        String visiblePrefix = PrefixUtil.createPrefix("package", "db1");
        VisibilityChecker mockVisibilityChecker =
                createMockVisibilityChecker(ImmutableSet.of(visiblePrefix + "visibleNamespace"));
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Set mock visibility setting.
        InternalVisibilityConfig config =
                new InternalVisibilityConfig.Builder("visibleNamespace").build();
        mAppSearchImpl.setBlobNamespaceVisibility(
                "package", "db1", ImmutableList.of(config));

        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle visibleHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "visibleNamespace");
        try (ParcelFileDescriptor writePfd =
                     mAppSearchImpl.openWriteBlob("package", "db1", visibleHandle);
                OutputStream outputStream = new ParcelFileDescriptor
                         .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        mAppSearchImpl.commitBlob("package", "db1", visibleHandle);

        AppSearchBlobHandle nonVisibleHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "nonVisibleNamespace");
        try (ParcelFileDescriptor writePfd =
                     mAppSearchImpl.openWriteBlob("package", "db1", nonVisibleHandle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        mAppSearchImpl.commitBlob("package", "db1", nonVisibleHandle);

        // visibleHandle is visible to the caller.
        byte[] readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd =
                     mAppSearchImpl.globalOpenReadBlob(visibleHandle, mSelfCallerAccess);
                 InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);

        // nonVisibleHandle is not visible to the caller.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(nonVisibleHandle, mSelfCallerAccess));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGlobalReadBlob_sameErrorMessage() throws Exception {
        String visiblePrefix = PrefixUtil.createPrefix("package", "db1");
        VisibilityChecker mockVisibilityChecker =
                createMockVisibilityChecker(ImmutableSet.of(visiblePrefix + "visibleNamespace"));
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Set mock visibility setting.
        InternalVisibilityConfig config =
                new InternalVisibilityConfig.Builder("visibleNamespace").build();
        mAppSearchImpl.setBlobNamespaceVisibility(
                "package", "db1", ImmutableList.of(config));

        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle visibleHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "visibleNamespace");
        try (ParcelFileDescriptor writePfd =
                     mAppSearchImpl.openWriteBlob("package", "db1", visibleHandle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        mAppSearchImpl.commitBlob("package", "db1", visibleHandle);

        AppSearchBlobHandle nonVisibleHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "nonVisibleNamespace");
        try (ParcelFileDescriptor writePfd =
                     mAppSearchImpl.openWriteBlob("package", "db1", nonVisibleHandle);
                 OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        mAppSearchImpl.commitBlob("package", "db1", nonVisibleHandle);

        // visibleHandle is visible to the caller.
        byte[] readBytes = new byte[20 * 1024];
        try (ParcelFileDescriptor readPfd =
                     mAppSearchImpl.globalOpenReadBlob(visibleHandle, mSelfCallerAccess);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);

        // nonVisibleHandle is not visible to the caller.
        AppSearchException exception1 = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(nonVisibleHandle, mSelfCallerAccess));
        assertThat(exception1.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(exception1.getMessage()).contains("Cannot find the blob for handle:");
        assertThat(exception1.getCause()).isNull();

        // Remove visibleHandle and verify the error code and message should be same between not
        // found and inaccessible.
        mAppSearchImpl.removeBlob("package", "db1", visibleHandle);
        AppSearchException exception2 = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(visibleHandle, mSelfCallerAccess));
        assertThat(exception2.getCause()).isNull();
        assertThat(exception2.getResultCode()).isEqualTo(exception1.getResultCode());
        assertThat(exception2.getMessage()).isEqualTo(exception1.getMessage());
    }

    @Test
    public void testClearPackageData() throws Exception {
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Create VisibilityConfig
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("schema")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();

        // Insert schema for package A and B.
        List<AppSearchSchema> schema =
                ImmutableList.of(new AppSearchSchema.Builder("schema").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageA",
                "database",
                schema,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageB",
                "database",
                schema,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Verify these two packages are stored in AppSearch.
        SchemaProto expectedProto = SchemaProto.newBuilder()
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("packageA$database/schema")
                                .setDescription("")
                                .setVersion(0))
                .addTypes(
                        SchemaTypeConfigProto.newBuilder()
                                .setSchemaType("packageB$database/schema")
                                .setDescription("")
                                .setVersion(0))
                .build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);

        // Verify these two visibility documents are stored in AppSearch.
        InternalVisibilityConfig expectedVisibilityConfigA =
                new InternalVisibilityConfig.Builder("packageA$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        InternalVisibilityConfig expectedVisibilityConfigB =
                new InternalVisibilityConfig.Builder("packageB$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("packageA$database/schema"))
                .isEqualTo(expectedVisibilityConfigA);
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("packageB$database/schema"))
                .isEqualTo(expectedVisibilityConfigB);

        // Prune packages
        mAppSearchImpl.prunePackageData(existingPackages);

        // Verify the schema is same as beginning.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(existingSchemas);
        assertThat(mAppSearchImpl.getPackageToDatabases())
                .containsExactlyEntriesIn(existingDatabases);

        // Verify the VisibilitySetting is removed.
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("packageA$database/schema")).isNull();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("packageB$database/schema")).isNull();
    }

    @Test
    public void testPrunePackageData_overDatabaseScopedThreshold() throws AppSearchException {
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();
        Map<String, Set<String>> existingDatabases = mAppSearchImpl.getPackageToDatabases();

        Set<String> existingPackages = new ArraySet<>(existingSchemas.size());
        for (int i = 0; i < existingSchemas.size(); i++) {
            existingPackages.add(PrefixUtil.getPackageName(existingSchemas.get(i).getSchemaType()));
        }

        // Create VisibilityConfig
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("schema")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();

        // Insert schema for package A and B.
        List<AppSearchSchema> schema =
                ImmutableList.of(new AppSearchSchema.Builder("schema").build());
        SchemaProto.Builder expectedProtoBuilder = SchemaProto.newBuilder();
        for (int i = 0; i < AppSearchImpl.PRUNE_PACKAGE_USING_FULL_SET_SCHEMA_THRESHOLD;
                i++) {
            String packageName = "package" + i;
            String databaseName = "database";
            InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                    packageName,
                    databaseName,
                    schema,
                    /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                    /*forceOverride=*/ false,
                    /*version=*/ 0,
                    /* setSchemaStatsBuilder= */ null);
            assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

            String schemaType = PrefixUtil.createPrefix(packageName, databaseName) + "schema";
            expectedProtoBuilder.addTypes(
                    SchemaTypeConfigProto.newBuilder()
                            .setSchemaType(schemaType)
                            .setDescription("")
                            .setVersion(0));
        }

        // Verify these two packages are stored in AppSearch.
        SchemaProto expectedProto = expectedProtoBuilder.build();
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = getSchemaProtoWithDatabase(expectedProto);
        }

        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);

        // Verify some visibility documents
        InternalVisibilityConfig expectedVisibilityConfig1 =
                new InternalVisibilityConfig.Builder("package1$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        InternalVisibilityConfig expectedVisibilityConfig2 =
                new InternalVisibilityConfig.Builder("package2$database/schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("package1$database/schema"))
                .isEqualTo(expectedVisibilityConfig1);
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("package2$database/schema"))
                .isEqualTo(expectedVisibilityConfig2);

        // Prune packages
        mAppSearchImpl.prunePackageData(existingPackages);

        // Verify the schema is same as beginning.
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(existingSchemas);
        assertThat(mAppSearchImpl.getPackageToDatabases())
                .containsExactlyEntriesIn(existingDatabases);

        // Verify the VisibilitySetting is removed.
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("package1$database/schema")).isNull();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility("package2$database/schema")).isNull();
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getPackageToDatabases()).containsExactlyEntriesIn(
                expectedMapping);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_BLOB_STORE)
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas2,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas3,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getAllPrefixedSchemaTypes()).containsExactly(
                "package1$database1/type1",
                "package1$database2/type2",
                "package2$database1/type3",
                "VS#Pkg$VS#Db/VisibilityType",  // plus the stored Visibility schema
                "VS#Pkg$VS#Db/VisibilityPermissionType",
                "VS#Pkg$VS#AndroidVDb/AndroidVOverlayType");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetAllPrefixedSchemaTypes_enableBlobStore() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas2,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas3,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(mAppSearchImpl.getAllPrefixedSchemaTypes()).containsExactly(
                "package1$database1/type1",
                "package1$database2/type2",
                "package2$database1/type3",
                "VS#Pkg$VS#Db/VisibilityType",  // plus the stored Visibility schema
                "VS#Pkg$VS#Db/VisibilityPermissionType",
                "VS#Pkg$VS#AndroidVDb/AndroidVOverlayType",
                "VS#Pkg$VSBlob#Db/VisibilityType",
                "VS#Pkg$VSBlob#Db/VisibilityPermissionType",
                "VS#Pkg$VSBlob#AndroidVDb/AndroidVOverlayType");
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    public void testGetStorageInfoForPackages_nonexistentPackage() throws Exception {
        // "package2" doesn't exist yet, so it shouldn't have any storage size
        StorageInfo storageInfo =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("nonexistent.package")));
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForPackages_withoutDocument() throws Exception {
        // Insert schema for "package1"
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Since "package1" doesn't have a document, it get any space attributed to it.
        StorageInfo storageInfo =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("package1")));
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testGetStorageInfoForPackages_proportionalToDocuments() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());

        // Insert schema for "package1"
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        StorageInfo storageInfo =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("package1")));
        long size1 = storageInfo.getSizeBytes();
        assertThat(size1).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(1);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(1);

        storageInfo =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("package2")));
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetStorageInfoForPackages_withBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        byte[] data1 = generateRandomBytes(5 * 1024); // 5 KiB
        byte[] digest1 = calculateDigest(data1);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, "package1", "db1", "ns");
        ParcelFileDescriptor writePfd1 = mAppSearchImpl.openWriteBlob("package1", "db1", handle1);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd1)) {
            outputStream.write(data1);
            outputStream.flush();
        }

        byte[] data2 = generateRandomBytes(10 * 1024); // 10 KiB
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, "package1", "db1", "ns");
        ParcelFileDescriptor writePfd2 = mAppSearchImpl.openWriteBlob("package1", "db1", handle2);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd2)) {
            outputStream.write(data2);
            outputStream.flush();
        }

        byte[] data3 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest3 = calculateDigest(data3);
        AppSearchBlobHandle handle3 = AppSearchBlobHandle.createWithSha256(
                digest3, "package2", "db1", "ns");
        ParcelFileDescriptor writePfd3 = mAppSearchImpl.openWriteBlob("package2", "db1", handle3);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd3)) {
            outputStream.write(data3);
            outputStream.flush();
        }

        StorageInfo storageInfo1 =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("package1")));
        assertThat(storageInfo1.getBlobsSizeBytes()).isEqualTo(15 * 1024);
        assertThat(storageInfo1.getBlobsCount()).isEqualTo(2);
        StorageInfo storageInfo2 =
                mAppSearchImpl.getStorageInfoForPackages(
                        new ArraySet<>(Collections.singleton("package2")));
        assertThat(storageInfo2.getBlobsSizeBytes()).isEqualTo(20 * 1024);
        assertThat(storageInfo2.getBlobsCount()).isEqualTo(1);
    }


    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetStorageInfoForDatabase_withBlob() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        byte[] data1 = generateRandomBytes(5 * 1024); // 5 KiB
        byte[] digest1 = calculateDigest(data1);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, "package", "db1", "ns");
        ParcelFileDescriptor writePfd1 = mAppSearchImpl.openWriteBlob("package", "db1", handle1);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd1)) {
            outputStream.write(data1);
            outputStream.flush();
        }

        byte[] data2 = generateRandomBytes(10 * 1024); // 10 KiB
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, "package", "db1", "ns");
        ParcelFileDescriptor writePfd2 = mAppSearchImpl.openWriteBlob("package", "db1", handle2);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd2)) {
            outputStream.write(data2);
            outputStream.flush();
        }

        byte[] data3 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest3 = calculateDigest(data3);
        AppSearchBlobHandle handle3 = AppSearchBlobHandle.createWithSha256(
                digest3, "package", "db2", "ns");
        ParcelFileDescriptor writePfd3 = mAppSearchImpl.openWriteBlob("package", "db2", handle3);
        try (OutputStream outputStream = new ParcelFileDescriptor
                .AutoCloseOutputStream(writePfd3)) {
            outputStream.write(data3);
            outputStream.flush();
        }

        StorageInfo storageInfo1 = mAppSearchImpl.getStorageInfoForDatabase("package", "db1");
        assertThat(storageInfo1.getBlobsSizeBytes()).isEqualTo(15 * 1024);
        assertThat(storageInfo1.getBlobsCount()).isEqualTo(2);
        StorageInfo storageInfo2 = mAppSearchImpl.getStorageInfoForDatabase("package", "db2");
        assertThat(storageInfo2.getBlobsSizeBytes()).isEqualTo(20 * 1024);
        assertThat(storageInfo2.getBlobsCount()).isEqualTo(1);
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        assertThrows(IllegalStateException.class, () -> mAppSearchImpl.getStorageInfoForPackages(
                new ArraySet<>(Collections.singleton("package"))));

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should trigger a recovery
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);

        // That document should be visible even from another instance.
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should trigger a recovery
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);

        // Only the second document should be retrievable from another instance.
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should trigger a recovery
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_IO_ERROR);

        // Only the second document should be retrievable from another instance.
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
    public void testPutPersistsWithoutRecoveryWithRecoveryProofFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        mAppSearchImpl.persistToDisk(PersistType.Code.RECOVERY_PROOF);

        GenericDocument getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document);

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should NOT trigger a recovery
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);

        // That document should be visible even from another instance.
        getResult = appSearchImpl2.getDocument("package", "database", "namespace1",
                "id1",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document);
        appSearchImpl2.close();
    }

    @Test
    public void testDeletePersistsWithoutRecoveryWithRecoveryProofFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        mAppSearchImpl.persistToDisk(PersistType.Code.RECOVERY_PROOF);

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
        mAppSearchImpl.persistToDisk(PersistType.Code.RECOVERY_PROOF);
        assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace1",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should NOT trigger a recovery.
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);

        // Only the second document should be retrievable from another instance.
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
    public void testDeleteByQueryPersistsWithoutRecoveryWithRecoveryProofFlush() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        mAppSearchImpl.persistToDisk(PersistType.Code.RECOVERY_PROOF);

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
        mAppSearchImpl.persistToDisk(PersistType.Code.RECOVERY_PROOF);
        assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument("package",
                "database",
                "namespace1",
                "id1",
                Collections.emptyMap()));
        getResult = mAppSearchImpl.getDocument("package", "database", "namespace2",
                "id2",
                Collections.emptyMap());
        assertThat(getResult).isEqualTo(document2);

        // Initialize a new instance of AppSearch to test initialization.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl2 = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization should NOT trigger a recovery.
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(initStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);

        // Only the second document should be retrievable from another instance.
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
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetIcingSearchEngineStorageInfo() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                .isEqualTo(4); // +2 for VisibilitySchema, +1 for VisibilityOverlay
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetIcingSearchEngineStorageInfo_enableBlobStore() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        // +2 (document and blob db) * (2 for VisibilitySchema +1 for VisibilityOverlay)
        assertThat(
                storageInfo.getSchemaStoreStorageInfo().getNumSchemaTypes())
                .isEqualTo(7);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetIcingSearchEngineDebugInfo() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                .hasSize(4); // +2 for VisibilitySchema, +1 for VisibilityOverlay
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetIcingSearchEngineDebugInfo_enableBlobStore() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        // +2 (document and blob db) * (2 for VisibilitySchema +1 for VisibilityOverlay)
        assertThat(debugInfo.getSchemaInfo().getSchema().getTypesList())
                .hasSize(7);
    }

    @Test
    public void testStatsIsLaunchVM() throws Exception {
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        IcingSearchEngineOptions options = mUnlimitedConfig.toIcingSearchEngineOptions(
                mAppSearchDir.getAbsolutePath(), /* isVMEnabled= */ true);
        IcingSearchEngine icingSearchEngine = new IcingSearchEngine(options);
        // the bit mask for only enable launch VM feature.
        int onlyLaunchVMFeature = 1;
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                icingSearchEngine,
                ALWAYS_OPTIMIZE);

        // Initialization and check initStats
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);

        // Set a schema and check SetSchemaStats
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        SetSchemaStats.Builder setSchemaStatsBuilder = new SetSchemaStats.Builder(
                "package", "database");
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                setSchemaStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        SetSchemaStats setSchemaStats = setSchemaStatsBuilder.build();
        assertThat(setSchemaStats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);

        // Add documents and test putDocumentStats.
        AppSearchLogger fakeLogger = new AppSearchLogger() {
            @Override
            public void logStats(@NonNull PutDocumentStats stats) {
                assertThat(stats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);
            }

            @Override
            public void logStats(@NonNull QueryStats stats) {
                assertThat(stats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);
            }
        };
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                fakeLogger);

        List<GenericDocument> documents = new ArrayList<>();
        documents.add(document);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package1",
                "database1",
                documents,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                PersistType.Code.LITE);

        mAppSearchImpl.query(
                "package", "database", "",
                new SearchSpec.Builder().build(), fakeLogger);

        // Delete the document and check remove stats
        RemoveStats.Builder removeStatsBuilder = new RemoveStats.Builder(
                "package", "database");
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().addFilterNamespaces("namespace1").setTermMatch(
                        SearchSpec.TERM_MATCH_EXACT_ONLY).build(), removeStatsBuilder);
        RemoveStats removeStats = removeStatsBuilder.build();
        assertThat(removeStats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);

        // Trigger optimize and check optimize stats
        OptimizeStats.Builder optimizeStatsBuilder = new OptimizeStats.Builder();
        mAppSearchImpl.optimize(optimizeStatsBuilder);
        OptimizeStats optimizeStats = optimizeStatsBuilder.build();
        assertThat(optimizeStats.getEnabledFeatures()).isEqualTo(onlyLaunchVMFeature);
    }

    @Test
    public void testStatsIsNotLaunchVM() throws Exception {
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        // the bit mask for nothing enabled feature.
        int noLaunchFeature = 0;
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                initStatsBuilder,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Initialization and check initStats
        InitializeStats initStats = initStatsBuilder.build();
        assertThat(initStats.getEnabledFeatures()).isEqualTo(noLaunchFeature);

        // Set a schema and check SetSchemaStats
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        SetSchemaStats.Builder setSchemaStatsBuilder = new SetSchemaStats.Builder(
                "package", "database");
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                setSchemaStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        SetSchemaStats setSchemaStats = setSchemaStatsBuilder.build();
        assertThat(setSchemaStats.getEnabledFeatures()).isEqualTo(noLaunchFeature);

        // Add documents and test putDocumentStats.
        AppSearchLogger fakeLogger = new AppSearchLogger() {
            @Override
            public void logStats(@NonNull PutDocumentStats stats) {
                assertThat(stats.getEnabledFeatures()).isEqualTo(noLaunchFeature);
            }

            @Override
            public void logStats(@NonNull QueryStats stats) {
                assertThat(stats.getEnabledFeatures()).isEqualTo(noLaunchFeature);
            }
        };
        GenericDocument document =
                new GenericDocument.Builder<>("namespace1", "id1", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                fakeLogger);

        List<GenericDocument> documents = new ArrayList<>();
        documents.add(document);
        AppSearchBatchResult.Builder<String, Void> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package1",
                "database1",
                documents,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                PersistType.Code.LITE);

        mAppSearchImpl.query(
                "package", "database", "",
                new SearchSpec.Builder().build(), fakeLogger);

        // Delete the document and check remove stats
        RemoveStats.Builder removeStatsBuilder = new RemoveStats.Builder(
                "package", "database");
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().addFilterNamespaces("namespace1").setTermMatch(
                        SearchSpec.TERM_MATCH_EXACT_ONLY).build(), removeStatsBuilder);
        RemoveStats removeStats = removeStatsBuilder.build();
        assertThat(removeStats.getEnabledFeatures()).isEqualTo(noLaunchFeature);

        // Trigger optimize and check optimize stats
        OptimizeStats.Builder optimizeStatsBuilder = new OptimizeStats.Builder();
        mAppSearchImpl.optimize(optimizeStatsBuilder);
        OptimizeStats optimizeStats = optimizeStatsBuilder.build();
        assertThat(optimizeStats.getEnabledFeatures()).isEqualTo(noLaunchFeature);
    }

    @Test
    public void testLimitConfig_DocumentSize() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(), new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return 80;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                mTemporaryFolder.newFolder(), new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 3;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 2;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database2",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database1",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database2",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 2;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                mTemporaryFolder.newFolder(), new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 3;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
    public void testRemoveByQuery_withJoinSpec_throwsException() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> mAppSearchImpl.removeByQuery(
                        /*packageName=*/"",
                        /*databaseName=*/"",
                        /*queryExpression=*/"",
                        new SearchSpec.Builder()
                                .setJoinSpec(new JoinSpec.Builder("childProp").build())
                                .build(),
                        null));
        assertThat(e.getMessage()).isEqualTo(
                "JoinSpec not allowed in removeByQuery, but JoinSpec was provided");
    }

    @Test
    public void testLimitConfig_Replace() throws Exception {
        // Create a new mAppSearchImpl with a lower limit
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(), new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 2;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 2;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 2;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 0;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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

    @Test
    public void testLimitConfig_belowLimitStartThreshold_limitHasNoEffect() throws Exception {
        // Create a new mAppSearchImpl with a low limit, but a higher limit start threshold.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // We should still be able to index another document even though we are over the
        // getPerPackageDocumentCountLimit threshold.
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
    }

    @Test
    public void testLimitConfig_aboveLimitStartThreshold_limitTakesEffect() throws Exception {
        // Create a new mAppSearchImpl with a low limit, but a higher limit start threshold.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schemas for thress packages
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package3",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        // We should still be able to index another document even though we are over the
        // getPerPackageDocumentCountLimit threshold.
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Index a document in another package. We will now be at the limit start threshold.
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package2", "database", document3, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Both packages are at the maxPerPackageDocumentLimitCount and the limit is in force.
        // Neither should be able to add another document.
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
                "Package \"package\" exceeded limit of 1 documents");

        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package2",
                        "database",
                        document4,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package2\" exceeded limit of 1 documents");

        // A new package should still be able to add a document however.
        mAppSearchImpl.putDocument(
                "package3", "database", document4, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
    }

    @Test
    public void testLimitConfig_replacement_doesntTriggerLimitStartThreshold() throws Exception {
        // Create a new mAppSearchImpl with a low limit, but a higher limit start threshold.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index two documents
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // Now Index a replacement. This should not trigger the DocumentCountLimitStartThreshold
        // because the total number of living documents should still be two.
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // We should be able to index one more document before triggering the limit.
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document3, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
    }

    @Test
    public void testLimitConfig_remove_deactivatesDocumentCountLimit() throws Exception {
        // Create a new mAppSearchImpl with a low limit, but a higher limit start threshold.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index three documents in "package" and one in "package2". This will mean four total
        // documents in the system which will exceed the limit start threshold of three. The limit
        // will be in force and neither package will be able to documents.
        mAppSearchImpl.putDocument(
                "package",
                "database",
                new GenericDocument.Builder<>("namespace", "id1", "type").build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package", "database", document3, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type").build();
        mAppSearchImpl.putDocument(
                "package2", "database", document4, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // The limit is in force. We should be unable to index another document. Even after we
        // delete one document, the system is still over the limit start threshold.
        GenericDocument document5 =
                new GenericDocument.Builder<>("namespace", "id5", "type").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package",
                        "database",
                        document5,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");

        mAppSearchImpl.remove(
                "package", "database", "namespace", "id2", /*removeStatsBuilder=*/null);
        e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package",
                        "database",
                        document5,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");

        // Removing another document will bring the system below the limit start threshold. Now,
        // adding another document can succeed.
        mAppSearchImpl.remove(
                "package", "database", "namespace", "id3", /*removeStatsBuilder=*/null);
        mAppSearchImpl.putDocument(
                "package", "database", document5, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
    }

    @Test
    public void testLimitConfig_removeByQuery_deactivatesDocumentCountLimit() throws Exception {
        // Create a new mAppSearchImpl with a low limit, but a higher limit start threshold.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, new AppSearchConfigImpl(new LimitConfig() {
                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return 1;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return 3;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return getMaxDocumentSizeBytes();
                    }
                }, new LocalStorageIcingOptionsConfig()),
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Insert schema
        AppSearchSchema schema =
                new AppSearchSchema.Builder("type")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("number")
                                        .setIndexingType(
                                                AppSearchSchema.StringPropertyConfig.
                                                        INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(
                                                AppSearchSchema.StringPropertyConfig.
                                                        TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("evenOdd")
                                        .setIndexingType(
                                                AppSearchSchema.StringPropertyConfig.
                                                        INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(
                                                AppSearchSchema.StringPropertyConfig.
                                                        TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .build();
        List<AppSearchSchema> schemas = Collections.singletonList(schema);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Index three documents in "package" and one in "package2". This will mean four total
        // documents in the system which will exceed the limit start threshold of three. The limit
        // will be in force and neither package will be able to documents.
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("number","first")
                        .setPropertyString("evenOdd", "odd").build();
        mAppSearchImpl.putDocument("package", "database", document1,
                /*sendChangeNotifications=*/ false, /*logger=*/ null);

        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type")
                        .setPropertyString("number","second")
                        .setPropertyString("evenOdd", "even").build();
        mAppSearchImpl.putDocument(
                "package", "database", document2, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type")
                        .setPropertyString("number","third")
                        .setPropertyString("evenOdd", "odd").build();
        mAppSearchImpl.putDocument(
                "package", "database", document3, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        GenericDocument document4 =
                new GenericDocument.Builder<>("namespace", "id4", "type")
                        .setPropertyString("number","fourth")
                        .setPropertyString("evenOdd", "even").build();
        mAppSearchImpl.putDocument(
                "package2", "database", document4, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        // The limit is in force. We should be unable to index another document.
        GenericDocument document5 =
                new GenericDocument.Builder<>("namespace", "id5", "type")
                        .setPropertyString("number","five")
                        .setPropertyString("evenOdd", "odd").build();
        AppSearchException e = assertThrows(AppSearchException.class, () ->
                mAppSearchImpl.putDocument(
                        "package",
                        "database",
                        document5,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 1 documents");

        // Remove two documents by query. Now we should be under the limit and be able to add
        // another document.
        mAppSearchImpl.removeByQuery("package", "database", "evenOdd:odd",
                new SearchSpec.Builder().build(), /*removeStatsBuilder=*/null);
        mAppSearchImpl.putDocument(
                "package", "database", document5, /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testLimitConfig_activeWriteFds() throws Exception {
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        AppSearchConfig config = new AppSearchConfigImpl(new LimitConfig() {
            @Override
            public int getMaxDocumentSizeBytes() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getPerPackageDocumentCountLimit() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getDocumentCountLimitStartThreshold() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxSuggestionCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxOpenBlobCount() {
                return 2;
            }

            @Override
            public int getMaxByteLimitForBatchPut() {
                return getMaxDocumentSizeBytes();
            }
        }, new LocalStorageIcingOptionsConfig());
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                config,
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(config),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        // We could open only 2 fds per package.
        byte[] data1 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest1 = calculateDigest(data1);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, "package", "db1", "ns");
        ParcelFileDescriptor writer1 = mAppSearchImpl.openWriteBlob("package", "db1", handle1);

        byte[] data2 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, "package", "db1", "ns");
        ParcelFileDescriptor writer2 = mAppSearchImpl.openWriteBlob("package", "db1", handle2);

        // Open 3rd fd will fail.
        byte[] data3 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest3 = calculateDigest(data3);
        AppSearchBlobHandle handle3 = AppSearchBlobHandle.createWithSha256(
                digest3, "package", "db1", "ns");
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openWriteBlob("package", "db1", handle3));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"package\" exceeded limit of 2 opened file descriptors. "
                        + "Some file descriptors must be closed to open additional ones.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testLimitConfig_activeReadFds() throws Exception {
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        AppSearchConfig config = new AppSearchConfigImpl(new LimitConfig() {
            @Override
            public int getMaxDocumentSizeBytes() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getPerPackageDocumentCountLimit() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getDocumentCountLimitStartThreshold() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxSuggestionCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxOpenBlobCount() {
                return 2;
            }

            @Override
            public int getMaxByteLimitForBatchPut() {
                return getMaxDocumentSizeBytes();
            }
        }, new LocalStorageIcingOptionsConfig());
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                config,
                /*initStatsBuilder=*/ null, /*visibilityChecker=*/ null,
                new JetpackRevocableFileDescriptorStore(config),
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Write and commit one blob
        byte[] data = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mContext.getPackageName(), "db1", "ns");
        try (ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob(
                mContext.getPackageName(), "db1", handle);
                OutputStream outputStream = new ParcelFileDescriptor
                        .AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        mAppSearchImpl.commitBlob(mContext.getPackageName(), "db1", handle);

        ParcelFileDescriptor reader1 =
                mAppSearchImpl.openReadBlob(mContext.getPackageName(), "db1", handle);
        ParcelFileDescriptor reader2 =
                mAppSearchImpl.openReadBlob(mContext.getPackageName(), "db1", handle);
        // Open 3rd fd will fail.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openReadBlob(mContext.getPackageName(), "db1", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"" + mContext.getPackageName() + "\" exceeded limit of 2 opened file "
                        + "descriptors. Some file descriptors must be closed to open additional "
                        + "ones.");

        // Open new fd for write will also fail since read and write share the same limit.
        byte[] data2 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mContext.getPackageName(), "db1", "ns");
        e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openWriteBlob(mContext.getPackageName(), "db1", handle2));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"" + mContext.getPackageName() + "\" exceeded limit of 2 opened file "
                        + "descriptors. Some file descriptors must be closed to open additional "
                        + "ones.");

        // Close 1st fd and open 3rd fd will success
        reader1.close();
        ParcelFileDescriptor reader3 =
                mAppSearchImpl.openReadBlob(mContext.getPackageName(), "db1", handle);

        // GlobalOpenRead will share same limit.
        e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(handle, mSelfCallerAccess));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"" + mContext.getPackageName() + "\" exceeded limit of 2 opened file "
                        + "descriptors. Some file descriptors must be closed to open additional "
                        + "ones.");
        // Close 2st fd and global open fd will success
        reader2.close();
        ParcelFileDescriptor reader4 = mAppSearchImpl.globalOpenReadBlob(handle, mSelfCallerAccess);

        // Keep opening will fail
        e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.openReadBlob(mContext.getPackageName(), "db1", handle));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"" + mContext.getPackageName() + "\" exceeded limit of 2 opened file "
                        + "descriptors. Some file descriptors must be closed to open additional "
                        + "ones.");
        e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.globalOpenReadBlob(handle, mSelfCallerAccess));
        assertThat(e.getResultCode()).isEqualTo(RESULT_OUT_OF_SPACE);
        assertThat(e).hasMessageThat().contains(
                "Package \"" + mContext.getPackageName() + "\" exceeded limit of 2 opened file "
                        + "descriptors. Some file descriptors must be closed to open additional "
                        + "ones.");

        reader3.close();
        reader4.close();
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(false);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(true);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(true);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        VisibilityChecker mockVisibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return callerAccess.getCallingPackageName().equals("visiblePackage");
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };

        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email to AppSearch database1 with a visibility document
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix = PrefixUtil.createPrefix("package", "database1");

        // assert the visibility document is saved.
        InternalVisibilityConfig expectedDocument =
                new InternalVisibilityConfig.Builder(prefix + "Email")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        InternalVisibilityConfig actualDocument =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix + "Email",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);
        assertThat(actualDocument).isEqualTo(expectedDocument);
    }

    @Test
    public void testSetVisibility_existingVisibilitySettingRetains() throws Exception {
        // Create Visibility Document for Email1
        InternalVisibilityConfig visibilityConfig1 = new InternalVisibilityConfig.Builder("Email1")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        List<AppSearchSchema> schemas1 =
                Collections.singletonList(new AppSearchSchema.Builder("Email1").build());

        // Set schema Email1 to package1 with a visibility document
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database",
                schemas1,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig1),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix1 = PrefixUtil.createPrefix("package1", "database");

        // assert the visibility document is saved.
        InternalVisibilityConfig expectedDocument1 =
                new InternalVisibilityConfig.Builder(prefix1 + "Email1")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);
        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        InternalVisibilityConfig actualDocument1 =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix1 + "Email1",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);

        assertThat(actualDocument1).isEqualTo(expectedDocument1);

        // Create Visibility Document for Email2
        InternalVisibilityConfig visibilityConfig2 = new InternalVisibilityConfig.Builder("Email2")
                .setNotDisplayedBySystem(false)
                .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                .build();
        List<AppSearchSchema> schemas2 =
                Collections.singletonList(new AppSearchSchema.Builder("Email2").build());

        // Set schema Email2 to package1 with a visibility document
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package2",
                "database",
                schemas2,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig2),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix2 = PrefixUtil.createPrefix("package2", "database");

        // assert the visibility document is saved.
        InternalVisibilityConfig expectedDocument2 =
                new InternalVisibilityConfig.Builder(prefix2 + "Email2")
                        .setNotDisplayedBySystem(false)
                        .addVisibleToPackage(new PackageIdentifier("pkgFoo", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix2 + "Email2"))
                .isEqualTo(expectedDocument2);
        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        InternalVisibilityConfig actualDocument2 =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix2 + "Email2",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);
        assertThat(actualDocument2).isEqualTo(expectedDocument2);

        // Check the existing visibility document retains.
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix1 + "Email1"))
                .isEqualTo(expectedDocument1);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        actualDocument1 = VisibilityToDocumentConverter.createInternalVisibilityConfig(
                mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        DOCUMENT_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix1 + "Email1",
                        /*typePropertyPaths=*/ Collections.emptyMap()),
                /*androidVOverlayDocument=*/null);
        assertThat(actualDocument1).isEqualTo(expectedDocument1);
    }

    @Test
    public void testSetVisibility_removeVisibilitySettings() throws Exception {
        // Create a non-all-default visibility document
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email and its visibility document to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        String prefix = PrefixUtil.createPrefix("package", "database1");
        InternalVisibilityConfig expectedDocument =
                new InternalVisibilityConfig.Builder(prefix + "Email")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        InternalVisibilityConfig actualDocument =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix + "Email",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);
        assertThat(actualDocument).isEqualTo(expectedDocument);

        // Set schema Email and its all-default visibility document to AppSearch database1
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        // All-default visibility document won't be saved in AppSearch.
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isNull();
        // Verify the InternalVisibilityConfig is removed from AppSearchImpl.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        DOCUMENT_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, package$database1/Email) not found.");
    }

    @Test
    public void testRemoveVisibility_noRemainingSettings() throws Exception {
        // Create a non-all-default visibility document
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());

        // Set schema Email and its visibility document to AppSearch database1
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        String prefix = PrefixUtil.createPrefix("package", "database1");
        InternalVisibilityConfig expectedDocument =
                new InternalVisibilityConfig.Builder(prefix + "Email")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        InternalVisibilityConfig actualDocument =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix + "Email",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);
        assertThat(actualDocument).isEqualTo(expectedDocument);

        // remove the schema and visibility setting from AppSearch
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                /*schemas=*/ new ArrayList<>(),
                /*visibilityConfigs=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        // add the schema back with an all default visibility setting.
        mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ ImmutableList.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        // All-default visibility document won't be saved in AppSearch.
        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "Email"))
                .isNull();
        // Verify there is no visibility setting for the schema.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        DOCUMENT_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, package$database1/Email) not found.");
    }

    @Test
    public void testCloseAndReopen_visibilityInfoRetains() throws Exception {
        // set Schema and visibility to AppSearch
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "packageName",
                "databaseName",
                schemas,
                ImmutableList.of(visibilityConfig),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // close and re-open AppSearchImpl, the visibility document retains
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        String prefix = PrefixUtil.createPrefix("packageName", "databaseName");
        InternalVisibilityConfig expectedDocument =
                new InternalVisibilityConfig.Builder(prefix + "Email")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                        .build();

        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix + "Email"))
                .isEqualTo(expectedDocument);
        // Verify the InternalVisibilityConfig is saved to AppSearchImpl.
        InternalVisibilityConfig actualDocument =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                DOCUMENT_VISIBILITY_DATABASE_NAME,
                                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                                /*id=*/ prefix + "Email",
                                /*typePropertyPaths=*/ Collections.emptyMap()),
                        /*androidVOverlayDocument=*/null);
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
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        assertThat(mAppSearchImpl.mDocumentVisibilityStoreLocked
                .getVisibility(prefix + "Email")).isNull();
        // Verify the InternalVisibilityConfig is removed from AppSearchImpl.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VISIBILITY_PACKAGE_NAME,
                        DOCUMENT_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
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
        VisibilityChecker mockVisibilityChecker = createMockVisibilityChecker(true);
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add a schema type that is not displayed by the system
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ImmutableList.of(
                        new InternalVisibilityConfig.Builder("Type")
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
                /*visibilityConfigs=*/ImmutableList.of(),
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
                /*visibilityConfigs=*/ImmutableList.of(),
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
        VisibilityChecker mockVisibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return prefixedSchema.endsWith("VisibleType");
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };

        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                mockVisibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add two schema types that are not displayed by the system.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ImmutableList.of(
                        new InternalVisibilityConfig.Builder("VisibleType")
                                .setNotDisplayedBySystem(true)
                                .build(),
                        new InternalVisibilityConfig.Builder("PrivateType")
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
    public void testGetSchema_global_publicAcl() throws Exception {
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("PublicTypeA").build(),
                new AppSearchSchema.Builder("PublicTypeB").build(),
                new AppSearchSchema.Builder("PublicTypeC").build());

        PackageIdentifier pkgA = new PackageIdentifier("A", new byte[32]);
        PackageIdentifier pkgB = new PackageIdentifier("B", new byte[32]);
        PackageIdentifier pkgC = new PackageIdentifier("C", new byte[32]);

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();

        // Package A is visible to package B & C, package B is visible to package C (based on
        // canPackageQuery, which we are mocking).
        Map<String, Set<String>> packageCanSee = ImmutableMap.of(
                "A", ImmutableSet.of("A"),
                "B", ImmutableSet.of("A", "B"),
                "C", ImmutableSet.of("A", "B", "C"));
        final VisibilityChecker publicAclMockChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                InternalVisibilityConfig param = visibilityStore.getVisibility(prefixedSchema);
                return packageCanSee.get(callerAccess.getCallingPackageName())
                        .contains(param.getVisibilityConfig().getPubliclyVisibleTargetPackage()
                                .getPackageName());
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };

        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                publicAclMockChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        List<InternalVisibilityConfig> visibilityConfigs = ImmutableList.of(
                new InternalVisibilityConfig.Builder("PublicTypeA")
                        .setPubliclyVisibleTargetPackage(pkgA).build(),
                new InternalVisibilityConfig.Builder("PublicTypeB")
                        .setPubliclyVisibleTargetPackage(pkgB).build(),
                new InternalVisibilityConfig.Builder("PublicTypeC")
                        .setPubliclyVisibleTargetPackage(pkgC).build());

        // Add the three schema types, each with their own publicly visible target package.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                visibilityConfigs,
                /*forceOverride=*/true,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Verify access to schemas based on calling package
        GetSchemaResponse getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(pkgA.getPackageName()));
        assertThat(getResponse.getSchemas()).containsExactly(schemas.get(0));
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeA");

        getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(pkgB.getPackageName()));
        assertThat(getResponse.getSchemas()).containsExactly(schemas.get(0), schemas.get(1));
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeA");
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeB");

        getResponse = mAppSearchImpl.getSchema(
                "package",
                "database",
                new CallerAccess(pkgC.getPackageName()));
        assertThat(getResponse.getSchemas()).containsExactlyElementsIn(schemas);
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeA");
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeB");
        assertThat(getResponse.getPubliclyVisibleSchemas()).containsKey("PublicTypeC");
    }

    @Test
    public void testGetSchema_global_publicAcl_removal() throws Exception {
        // This test to ensure the proper documents are created through setSchema, then removed
        // when setSchema is called again
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("PublicTypeA").build(),
                new AppSearchSchema.Builder("PublicTypeB").build(),
                new AppSearchSchema.Builder("PublicTypeC").build());

        PackageIdentifier pkgA = new PackageIdentifier("A", new byte[32]);
        PackageIdentifier pkgB = new PackageIdentifier("B", new byte[32]);
        PackageIdentifier pkgC = new PackageIdentifier("C", new byte[32]);

        // Create a new mAppSearchImpl with a mock Visibility Checker
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();

        // Package A is visible to package B & C, package B is visible to package C (based on
        // canPackageQuery, which we are mocking).
        Map<String, Set<String>> packageCanSee = ImmutableMap.of(
                "A", ImmutableSet.of("A"),
                "B", ImmutableSet.of("A", "B"),
                "C", ImmutableSet.of("A", "B", "C"));
        final VisibilityChecker publicAclMockChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                InternalVisibilityConfig param = visibilityStore.getVisibility(prefixedSchema);
                return packageCanSee.get(callerAccess.getCallingPackageName())
                        .contains(param.getVisibilityConfig()
                                .getPubliclyVisibleTargetPackage().getPackageName());
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };

        mAppSearchImpl = AppSearchImpl.create(
                tempFolder,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/ null,
                publicAclMockChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        List<InternalVisibilityConfig> visibilityConfigs = ImmutableList.of(
                new InternalVisibilityConfig.Builder("PublicTypeA")
                        .setPubliclyVisibleTargetPackage(pkgA).build(),
                new InternalVisibilityConfig.Builder("PublicTypeB")
                        .setPubliclyVisibleTargetPackage(pkgB).build(),
                new InternalVisibilityConfig.Builder("PublicTypeC")
                        .setPubliclyVisibleTargetPackage(pkgC).build());

        // Add two schema types that are not displayed by the system.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                visibilityConfigs,
                /*forceOverride=*/true,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Now check for documents
        GenericDocument visibilityOverlayA = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeA",
                Collections.emptyMap());
        GenericDocument visibilityOverlayB = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeB",
                Collections.emptyMap());
        GenericDocument visibilityOverlayC = mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME,
                DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeC",
                Collections.emptyMap());

        AndroidVOverlayProto overlayProtoA = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(VisibilityConfigProto.newBuilder()
                        .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                                .setPackageName("A")
                                .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build())
                        .build())
                .build();
        AndroidVOverlayProto overlayProtoB = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(VisibilityConfigProto.newBuilder()
                        .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                                .setPackageName("B")
                                .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build())
                        .build())
                .build();
        AndroidVOverlayProto overlayProtoC = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(VisibilityConfigProto.newBuilder()
                        .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                                .setPackageName("C")
                                .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build())
                        .build())
                .build();

        assertThat(visibilityOverlayA.getPropertyBytes("visibilityProtoSerializeProperty"))
                .isEqualTo(overlayProtoA.toByteArray());
        assertThat(visibilityOverlayB.getPropertyBytes("visibilityProtoSerializeProperty"))
                .isEqualTo(overlayProtoB.toByteArray());
        assertThat(visibilityOverlayC.getPropertyBytes("visibilityProtoSerializeProperty"))
                .isEqualTo(overlayProtoC.toByteArray());

        // now undo the "public" setting
        visibilityConfigs = ImmutableList.of(
                new InternalVisibilityConfig.Builder("PublicTypeA").build(),
                new InternalVisibilityConfig.Builder("PublicTypeB").build(),
                new InternalVisibilityConfig.Builder("PublicTypeC").build());

        InternalSetSchemaResponse internalSetSchemaResponseRemoved = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                visibilityConfigs,
                /*forceOverride=*/true,
                /*version=*/1,
                /*setSchemaStatsBuilder=*/null);
        assertThat(internalSetSchemaResponseRemoved.isSuccess()).isTrue();

        // Now check for documents again
        Exception e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME, DOCUMENT_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeA", Collections.emptyMap()));
        assertThat(e.getMessage()).endsWith("not found.");
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME, DOCUMENT_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeB", Collections.emptyMap()));
        assertThat(e.getMessage()).endsWith("not found.");
        e = assertThrows(AppSearchException.class, () -> mAppSearchImpl.getDocument(
                VISIBILITY_PACKAGE_NAME, DOCUMENT_VISIBILITY_DATABASE_NAME,
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE,
                "package$database/PublicTypeC", Collections.emptyMap()));
        assertThat(e.getMessage()).endsWith("not found.");
    }

    @Test
    public void testDispatchObserver_samePackage_noVisStore_accept() throws Exception {
        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        final VisibilityChecker rejectChecker = createMockVisibilityChecker(false);
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                rejectChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        final VisibilityChecker visibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return callerAccess.getCallingPackageName().equals(fakeListeningPackage);
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                visibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        final VisibilityChecker rejectChecker = createMockVisibilityChecker(false);
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                rejectChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add a schema type
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(new AppSearchSchema.Builder("Type1").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        final VisibilityChecker visibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                if (!callerAccess.getCallingPackageName().equals(fakeListeningPackage)) {
                    return false;
                }

                for (PackageIdentifier packageIdentifier :
                        visibilityStore.getVisibility(prefixedSchema)
                                .getVisibilityConfig().getAllowedPackages()) {
                    if (packageIdentifier.getPackageName().equals(fakeListeningPackage)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                visibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ ImmutableList.of(
                        new InternalVisibilityConfig.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new InternalVisibilityConfig.Builder("Type2")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build()),
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
                /*visibilityConfigs=*/ ImmutableList.of(
                        new InternalVisibilityConfig.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new InternalVisibilityConfig.Builder("Type2").build()
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
                /*visibilityConfigs=*/ ImmutableList.of(
                        new InternalVisibilityConfig.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new InternalVisibilityConfig.Builder("Type2").build()),
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
                /*visibilityConfigs=*/ImmutableList.of(
                        new InternalVisibilityConfig.Builder("Type1")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build(),
                        new InternalVisibilityConfig.Builder("Type2")
                                .addVisibleToPackage(
                                        new PackageIdentifier(fakeListeningPackage, new byte[0]))
                                .build()),
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
        final VisibilityChecker visibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return callerAccess.getCallingPackageName().equals(fakeListeningPackage)
                        && prefixedSchema.endsWith("Type2");
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                visibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
        final VisibilityChecker visibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
                return callerAccess.getCallingPackageName().equals(fakeListeningPackage)
                        && prefixedSchema.endsWith("Type2");
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                visibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Add a schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mContext.getPackageName(),
                "database1",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Type1").build(),
                        new AppSearchSchema.Builder("Type2").build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

        final VisibilityChecker visibilityChecker = new VisibilityChecker() {
            @Override
            public boolean isSchemaSearchableByCaller(@NonNull CallerAccess callerAccess,
                    @NonNull String packageName, @NonNull String prefixedSchema,
                    @NonNull VisibilityStore visibilityStore) {
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
            }

            @Override
            public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                return false;
            }
        };
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()
                ),
                /*initStatsBuilder=*/null,
                visibilityChecker, /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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
                /*visibilityConfigs=*/ Collections.emptyList(),
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

    @Test
    public void testProvideIcingInstance_setSchema() throws Exception {
        SchemaTypeConfigProto additionalConfigProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("nopackage$nodatabase/notype")
                .setDescription("From modified icing instance")
                .setVersion(0).build();
        IcingSearchEngineInterface modifiedIcingInstance = new IcingSearchEngine(
                mUnlimitedConfig.toIcingSearchEngineOptions(
                        mAppSearchDir.getAbsolutePath(), /* isVMEnabled= */ true)) {
            @Override
            public GetSchemaResultProto getSchema() {
                GetSchemaResultProto.Builder resultBuilder = super.getSchema().toBuilder();
                resultBuilder.setSchema(
                        resultBuilder.getSchema().toBuilder().addTypes(additionalConfigProto));
                return resultBuilder.build();
            }
        };
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                modifiedIcingInstance,
                ALWAYS_OPTIMIZE);
        List<SchemaTypeConfigProto> existingSchemas =
                mAppSearchImpl.getSchemaProtoLocked().getTypesList();

        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("Email").build());
        // Set schema Email to AppSearch database1
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database1",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Create expected schemaType proto.
        SchemaProto expectedProto;
        if (mAppSearchImpl.useDatabaseScopedSchemaOperations()) {
            expectedProto = SchemaProto.newBuilder()
                    .addTypes(
                            SchemaTypeConfigProto.newBuilder()
                                    .setSchemaType("package$database1/Email")
                                    .setDatabase("package$database1/")
                                    .setDescription("")
                                    .setVersion(0))
                    .build();
        } else {
            expectedProto = SchemaProto.newBuilder()
                    .addTypes(
                            SchemaTypeConfigProto.newBuilder()
                                    .setSchemaType("package$database1/Email")
                                    .setDescription("")
                                    .setVersion(0))
                    // This type shows up twice in the expectedProto when it actually should only
                    // be there once. This is because we call getSchema in the setSchema
                    // operation, and then build on top of the retrieved schema. When the new
                    // database-scoped schema operation we'll only retrieve the types from the
                    // requested database, and so this shouldn't be built into the existing schema
                    // again.
                    .addTypes(additionalConfigProto)
                    .build();
        }

        List<SchemaTypeConfigProto> expectedTypes = new ArrayList<>();
        expectedTypes.addAll(existingSchemas);
        expectedTypes.addAll(expectedProto.getTypesList());
        assertThat(mAppSearchImpl.getSchemaProtoLocked().getTypesList())
                .containsExactlyElementsIn(expectedTypes);
    }

    @Test
    public void testBatchGetDocumentsWithEmptyIdList() throws Exception {
        AppSearchBatchResult<String, GenericDocument> batchGetResult =
                mAppSearchImpl.batchGetDocuments(
                        "packageName",
                        "dbName",
                        new GetByDocumentIdRequest.Builder("namespace").build(),
                        /*callerAccess=*/ null);

        assertThat(batchGetResult.getAll()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMPRESSION_THRESHOLD)
    public void testCompressionThreshold() throws Exception {
        mAppSearchImpl.close();
        // Initialize AppSearch with a small compression threshold, which should force
        // compression for large documents.
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public int getCompressionThresholdBytes() {
                                return 10;
                            }
                        }),
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Set schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("Type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("largeString")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                                .build())
                        .build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a large document
        GenericDocument largeDoc = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString(
                        "largeString",
                        // A string of 10000 'A' characters.
                        new String(new char[10000]).replace('\0', 'A')
                )
                .build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                largeDoc,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);
        mAppSearchImpl.optimize(/*builder=*/ null);
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);

        // Record storage size (the document should be compressed)
        StorageInfoProto storageInfo = mAppSearchImpl.getRawStorageInfoProto();
        long compressedSize = storageInfo.getDocumentStorageInfo().getDocumentLogSize();
        assertThat(compressedSize).isGreaterThan(0);

        // Close and re-open AppSearchImpl with a larger threshold, which should disable
        // compression for the document.
        mAppSearchImpl.close();
        AppSearchConfig configLargeThreshold = new AppSearchConfigImpl(
                new UnlimitedLimitConfig(),
                new LocalStorageIcingOptionsConfig() {
                    @Override
                    public int getCompressionThresholdBytes() {
                        return 100000;
                    }
                });
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                configLargeThreshold,
                /*initStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        // Run optimize, and test that the document is decompressed based on the new threshold.
        mAppSearchImpl.optimize(/*builder=*/ null);

        // Record storage size again (should be uncompressed)
        storageInfo = mAppSearchImpl.getRawStorageInfoProto();
        long uncompressedSize = storageInfo.getDocumentStorageInfo().getDocumentLogSize();
        assertThat(uncompressedSize).isGreaterThan(0);

        // Check that previous size (compressed) is smaller than the latter (uncompressed)
        assertThat(compressedSize).isLessThan(uncompressedSize);
    }

    private SchemaProto getSchemaProtoWithDatabase(SchemaProto schema) throws AppSearchException {
        SchemaProto.Builder schemaBuilder = SchemaProto.newBuilder();
        for (int i = 0; i < schema.getTypesList().size(); i++) {
            SchemaTypeConfigProto type = schema.getTypes(i);
            SchemaTypeConfigProto.Builder typeBuilder = SchemaTypeConfigProto.newBuilder(type)
                    .setDatabase(getPrefix(type.getSchemaType()));
            schemaBuilder.addTypes(typeBuilder);
        }
        return schemaBuilder.build();
    }

    // Mocks all methods that are called during AppSearchImpl#create to return successful statuses.
    private void setUpSuccessfulMocksForCreation() {
        // Setup Icing mock to fail the first init call, but then succeed
        InitializeResultProto okInit =
                InitializeResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.initialize()).thenReturn(okInit);

        GetSchemaResultProto successGetSchema =
                GetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getSchema()).thenReturn(successGetSchema);
        when(mMockIcingSearchEngine.getSchemaForDatabase(any())).thenReturn(successGetSchema);

        StorageInfoResultProto successGetStorageInfo =
                StorageInfoResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.getStorageInfo()).thenReturn(successGetStorageInfo);

        ResetResultProto successReset =
                ResetResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.reset()).thenReturn(successReset);

        SetSchemaResultProto successSetSchema =
                SetSchemaResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.setSchemaWithRequestProto(any())).thenReturn(successSetSchema);

        PersistToDiskResultProto successPersist =
                PersistToDiskResultProto.newBuilder().setStatus(OK).build();
        when(mMockIcingSearchEngine.persistToDisk(any())).thenReturn(successPersist);
    }
}