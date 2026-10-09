/*
 * Copyright 2026 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalPutDocumentResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.TestObserverCallback;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.android.icing.proto.PersistType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_PCC_DATA_ENCAPSULATION)
public class AppSearchImplPccTest {
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    private static final int NON_PCC_UID = 1000;
    private static final int PCC_UID = 30000;

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private final AppSearchConfig mUnlimitedConfig =
            new AppSearchConfigImpl(
                    new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig());

    private final VisibilityChecker mVisibilityChecker =
            new VisibilityChecker() {
                @Override
                public boolean isSchemaSearchableByCaller(
                        @NonNull CallerAccess callerAccess,
                        @NonNull String packageName,
                        @NonNull String prefixedSchema,
                        @NonNull VisibilityStore visibilityStore) {
                    return true;
                }

                @Override
                public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                    return false;
                }

                @Override
                public boolean isPrivateComputeCoreUid(int uid) {
                    return uid == PCC_UID;
                }
            };

    private final AppSearchUserPlugins mPlugins =
            new AppSearchUserPlugins.Builder()
                    .setVisibilityChecker(mVisibilityChecker)
                    .build();

    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        File appSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl =
                AppSearchImpl.create(
                        appSearchDir, mUnlimitedConfig, mPlugins, ALWAYS_OPTIMIZE);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testSetSchema_pccCaller_setsWriterUidInVisibilityStore() throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 1,
                        /* callingUid= */ PCC_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();

        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configA =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(PCC_UID);

        InternalVisibilityConfig configB =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(PCC_UID);
    }

    @Test
    public void testSetSchema_nonPccCaller_setsWriterUidInVisibilityStore() throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 1,
                        /* callingUid= */ NON_PCC_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();

        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configA =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(NON_PCC_UID);

        InternalVisibilityConfig configB =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(NON_PCC_UID);
    }

    @Test
    public void testSetSchema_invalidUid_doesNotMutateWriterUid() throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        // First set schema with PCC_UID
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 1,
                        /* callingUid= */ PCC_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();

        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configA =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(PCC_UID);

        InternalVisibilityConfig configB =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(PCC_UID);

        // Next set schema with Process.INVALID_UID
        InternalVisibilityConfig inputConfigA =
                new InternalVisibilityConfig.Builder(configA).setSchemaType("SchemaA").build();
        InternalVisibilityConfig inputConfigB =
                new InternalVisibilityConfig.Builder(configB).setSchemaType("SchemaB").build();
        InternalSetSchemaResponse response2 =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ ImmutableList.of(inputConfigA, inputConfigB),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 1,
                        /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response2.isSuccess()).isTrue();

        // Verify writerUid is still PCC_UID
        configA = mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(PCC_UID);

        configB = mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(PCC_UID);
    }

    @Test
    public void testSetSchema_upgradeNonPccToPcc_preservesDocumentsAndUpdatesWriterUid()
            throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        // Non-PCC caller sets schemas
        mAppSearchImpl.setSchema(
                "pkg",
                "db",
                ImmutableList.of(schemaA, schemaB),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ Collections.emptyMap(),
                /* forceOverride= */ false,
                /* version= */ 1,
                /* callingUid= */ NON_PCC_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);

        // Put documents under both schemas
        GenericDocument docA =
                new GenericDocument.Builder<>("namespace", "idA", "SchemaA")
                        .setPropertyString("prop", "valA")
                        .build();
        GenericDocument docB =
                new GenericDocument.Builder<>("namespace", "idB", "SchemaB")
                        .setPropertyString("prop", "valB")
                        .build();
        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "pkg",
                "db",
                ImmutableList.of(docA, docB),
                resultBuilder,
                /* sendChangeNotifications= */ false,
                /* logger= */ null,
                PersistType.Code.LITE,
                /* callStatsBuilder= */ null);
        assertThat(resultBuilder.build().isSuccess()).isTrue();

        // PCC caller upgrades schemas
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 2,
                        /* callingUid= */ PCC_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();

        // Verify documents are preserved
        GenericDocument retrievedA =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idA",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedA).isNotNull();

        GenericDocument retrievedB =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idB",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedB).isNotNull();

        // Verify writerUid is updated to PCC_UID for both
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configA =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(PCC_UID);

        InternalVisibilityConfig configB =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(PCC_UID);
    }

    @Test
    public void testSetSchema_downgradePccToNonPcc_withoutForceOverride_returnsFailedResponse()
            throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        mAppSearchImpl.setSchema(
                "pkg",
                "db",
                ImmutableList.of(schemaA, schemaB),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ Collections.emptyMap(),
                /* forceOverride= */ false,
                /* version= */ 1,
                /* callingUid= */ PCC_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);

        SetSchemaStats.Builder statsBuilder = new SetSchemaStats.Builder("pkg", "db");
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ false,
                        /* version= */ 2,
                        /* callingUid= */ NON_PCC_UID,
                        statsBuilder,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Cannot modify PCC-written schema");
        assertThat(response.getSetSchemaResponse().getIncompatibleTypes())
                .containsExactly("SchemaA", "SchemaB");

        SetSchemaStats stats = statsBuilder.build();
        assertThat(stats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);
        assertThat(stats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(2);
        assertThat(stats.getPccToNonPccTypesCount()).isEqualTo(2);
    }

    @Test
    public void testSetSchema_downgradePccToNonPcc_withForceOverride_wipesDocumentsAndSucceeds()
            throws Exception {
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        mAppSearchImpl.setSchema(
                "pkg",
                "db",
                ImmutableList.of(schemaA, schemaB),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ Collections.emptyMap(),
                /* forceOverride= */ false,
                /* version= */ 1,
                /* callingUid= */ PCC_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);

        GenericDocument docA =
                new GenericDocument.Builder<>("namespace", "idA", "SchemaA")
                        .setPropertyString("prop", "valA")
                        .build();
        GenericDocument docB =
                new GenericDocument.Builder<>("namespace", "idB", "SchemaB")
                        .setPropertyString("prop", "valB")
                        .build();
        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "pkg",
                "db",
                ImmutableList.of(docA, docB),
                resultBuilder,
                /* sendChangeNotifications= */ false,
                /* logger= */ null,
                PersistType.Code.LITE,
                /* callStatsBuilder= */ null);
        assertThat(resultBuilder.build().isSuccess()).isTrue();

        // Verify docs exist before downgrade
        GenericDocument retrievedA =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idA",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedA).isNotNull();
        GenericDocument retrievedB =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idB",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedB).isNotNull();

        // Non-PCC caller sets schemas with forceOverride=true
        SetSchemaStats.Builder statsBuilder = new SetSchemaStats.Builder("pkg", "db");
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ true,
                        /* version= */ 2,
                        /* callingUid= */ NON_PCC_UID,
                        statsBuilder,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSetSchemaResponse().getIncompatibleTypes())
                .containsExactly("SchemaA", "SchemaB");
        assertThat(response.getSetSchemaResponse().getDeletedTypes()).isEmpty();

        SetSchemaStats stats = statsBuilder.build();
        assertThat(stats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(2);
        assertThat(stats.getPccToNonPccTypesCount()).isEqualTo(2);
        assertThat(stats.getNewTypeCount()).isEqualTo(0);
        assertThat(stats.getDeletedTypeCount()).isEqualTo(0);
        assertThat(stats.getCompatibleTypeChangeCount()).isEqualTo(0);
        assertThat(stats.getDeletedDocumentCount()).isEqualTo(2);

        // Verify both docs are wiped
        AppSearchException eA =
                assertThrows(
                        AppSearchException.class,
                        () ->
                                mAppSearchImpl.getDocument(
                                        "pkg",
                                        "db",
                                        "namespace",
                                        "idA",
                                        /* typePropertyPaths= */ Collections.emptyMap(),
                                        /* callStatsBuilder= */ null));
        assertThat(eA.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        AppSearchException eB =
                assertThrows(
                        AppSearchException.class,
                        () ->
                                mAppSearchImpl.getDocument(
                                        "pkg",
                                        "db",
                                        "namespace",
                                        "idB",
                                        /* typePropertyPaths= */ Collections.emptyMap(),
                                        /* callStatsBuilder= */ null));
        assertThat(eB.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Verify writerUid is updated to NON_PCC_UID for both
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configA =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaA");
        assertThat(configA).isNotNull();
        assertThat(configA.getWriterUid()).isEqualTo(NON_PCC_UID);

        InternalVisibilityConfig configB =
                mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + "SchemaB");
        assertThat(configB).isNotNull();
        assertThat(configB.getWriterUid()).isEqualTo(NON_PCC_UID);
    }

    @Test
    public void testSetSchema_mixedSchemas_downgradeWipesOnlyPccSchemas() throws Exception {
        AppSearchSchema schemaPcc1 =
                new AppSearchSchema.Builder("SchemaPcc1")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaNonPcc1 =
                new AppSearchSchema.Builder("SchemaNonPcc1")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaNonPcc2 =
                new AppSearchSchema.Builder("SchemaNonPcc2")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();

        // Create all 3 schemas
        mAppSearchImpl.setSchema(
                "pkg",
                "db",
                ImmutableList.of(schemaPcc1, schemaNonPcc1, schemaNonPcc2),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ Collections.emptyMap(),
                /* forceOverride= */ false,
                /* version= */ 1,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);

        // Explicitly set PCC vs Non-PCC in VisibilityStore using package-private field
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig configPcc1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaPcc1")
                        .setWriterUid(PCC_UID)
                        .build();
        InternalVisibilityConfig configNonPcc1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc1")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig configNonPcc2 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc2")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        mAppSearchImpl.mDocumentVisibilityStoreLocked.setVisibility(
                ImmutableList.of(configPcc1, configNonPcc1, configNonPcc2),
                /* callStatsBuilder= */ null);

        // Put docs for each schema
        GenericDocument docPcc1 =
                new GenericDocument.Builder<>("namespace", "idPcc1", "SchemaPcc1")
                        .setPropertyString("prop", "valPcc1")
                        .build();
        GenericDocument docNonPcc1 =
                new GenericDocument.Builder<>("namespace", "idNonPcc1", "SchemaNonPcc1")
                        .setPropertyString("prop", "valNonPcc1")
                        .build();
        GenericDocument docNonPcc2 =
                new GenericDocument.Builder<>("namespace", "idNonPcc2", "SchemaNonPcc2")
                        .setPropertyString("prop", "valNonPcc2")
                        .build();

        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "pkg",
                "db",
                ImmutableList.of(docPcc1, docNonPcc1, docNonPcc2),
                resultBuilder,
                /* sendChangeNotifications= */ false,
                /* logger= */ null,
                PersistType.Code.LITE,
                /* callStatsBuilder= */ null);
        assertThat(resultBuilder.build().isSuccess()).isTrue();

        // Non-PCC caller calls setSchema on all 3 with forceOverride=true
        SetSchemaStats.Builder statsBuilder = new SetSchemaStats.Builder("pkg", "db");
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaPcc1, schemaNonPcc1, schemaNonPcc2),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ true,
                        /* version= */ 2,
                        /* callingUid= */ NON_PCC_UID,
                        statsBuilder,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSetSchemaResponse().getIncompatibleTypes())
                .containsExactly("SchemaPcc1");
        assertThat(response.getSetSchemaResponse().getDeletedTypes()).isEmpty();

        SetSchemaStats stats = statsBuilder.build();
        assertThat(stats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(1);
        assertThat(stats.getPccToNonPccTypesCount()).isEqualTo(1);
        assertThat(stats.getNewTypeCount()).isEqualTo(0);
        assertThat(stats.getDeletedTypeCount()).isEqualTo(0);
        assertThat(stats.getCompatibleTypeChangeCount()).isEqualTo(0);
        assertThat(stats.getDeletedDocumentCount()).isEqualTo(1);

        // PCC docs should be wiped
        AppSearchException exception =
                assertThrows(
                        AppSearchException.class,
                        () ->
                                mAppSearchImpl.getDocument(
                                        "pkg",
                                        "db",
                                        "namespace",
                                        "idPcc1",
                                        /* typePropertyPaths= */ Collections.emptyMap(),
                                        /* callStatsBuilder= */ null));
        assertThat(exception.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Non-PCC docs should still exist
        GenericDocument retrievedNonPcc1 =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idNonPcc1",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedNonPcc1).isNotNull();

        GenericDocument retrievedNonPcc2 =
                mAppSearchImpl.getDocument(
                        "pkg",
                        "db",
                        "namespace",
                        "idNonPcc2",
                        /* typePropertyPaths= */ Collections.emptyMap(),
                        /* callStatsBuilder= */ null);
        assertThat(retrievedNonPcc2).isNotNull();

        // All schemas should now have writerUid = NON_PCC_UID
        for (String type : ImmutableList.of("SchemaPcc1", "SchemaNonPcc1", "SchemaNonPcc2")) {
            InternalVisibilityConfig updatedConfig =
                    mAppSearchImpl.mDocumentVisibilityStoreLocked.getVisibility(prefix + type);
            assertThat(updatedConfig).isNotNull();
            assertThat(updatedConfig.getWriterUid()).isEqualTo(NON_PCC_UID);
        }
    }

    @Test
    public void testSetSchema_downgradePccToNonPcc_observerNotified() throws Exception {
        TestObserverCallback observer = new TestObserverCallback();
        mAppSearchImpl.registerObserverCallback(
                new CallerAccess("pkg"),
                "pkg",
                new ObserverSpec.Builder().build(),
                MoreExecutors.directExecutor(),
                observer);

        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("SchemaA")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("SchemaB")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("prop")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();

        // PCC caller creates SchemaA and SchemaB
        mAppSearchImpl.setSchema(
                "pkg",
                "db",
                ImmutableList.of(schemaA, schemaB),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ Collections.emptyMap(),
                /* forceOverride= */ false,
                /* version= */ 1,
                /* callingUid= */ PCC_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);

        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).hasSize(1);
        assertThat(
                Iterables.getOnlyElement(observer.getSchemaChanges())
                        .getChangedSchemaNames())
                .containsExactly("SchemaA", "SchemaB");
        observer.clear();

        // Non-PCC caller updates SchemaA and SchemaB with forceOverride=true
        InternalSetSchemaResponse response =
                mAppSearchImpl.setSchema(
                        "pkg",
                        "db",
                        ImmutableList.of(schemaA, schemaB),
                        /* visibilityConfigs= */ Collections.emptyList(),
                        /* accountPropertyPaths= */ Collections.emptyMap(),
                        /* forceOverride= */ true,
                        /* version= */ 2,
                        /* callingUid= */ NON_PCC_UID,
                        /* setSchemaStatsBuilder= */ null,
                        /* callStatsBuilder= */ null);
        assertThat(response.isSuccess()).isTrue();

        mAppSearchImpl.dispatchAndClearChangeNotifications();
        assertThat(observer.getSchemaChanges()).hasSize(1);
        assertThat(
                Iterables.getOnlyElement(observer.getSchemaChanges())
                        .getChangedSchemaNames())
                .containsExactly("SchemaA", "SchemaB");
    }
}
