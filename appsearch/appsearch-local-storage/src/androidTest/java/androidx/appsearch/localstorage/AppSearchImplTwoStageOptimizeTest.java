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

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.testutil.flags.RequiresFlagsDisabled;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.GetResultSpecProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.OptimizePrepareResultProto;
import com.google.android.icing.proto.OptimizeResultProto;
import com.google.android.icing.proto.PersistToDiskResultProto;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.StatusProto;
import com.google.common.collect.ImmutableMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@link AppSearchImplOptimizeTestBase} with the 2-stage optimize, plus tests verifying the
 * Icing calls made by it and its behavior between the two stages. The optimized result itself is
 * covered by the base tests.
 */
@SuppressWarnings({"GuardedBy", "deprecation"})
public class AppSearchImplTwoStageOptimizeTest extends AppSearchImplOptimizeTestBase {
    private static final String CALL_PERSIST_TO_DISK_FULL = "persistToDisk(FULL)";
    private static final String CALL_OPTIMIZE = "optimize";
    private static final String CALL_PREPARE = "optimizePrepareDestDirectory";
    private static final String CALL_SWAP = "optimizeSwapToDestDirectory";

    private static final String PACKAGE_NAME = "package";
    private static final String DATABASE_NAME = "database";
    private static final String NAMESPACE = "namespace";
    private static final String SCHEMA_TYPE = "type";
    private static final String PREFIX = PrefixUtil.createPrefix(PACKAGE_NAME, DATABASE_NAME);

    @Override
    protected boolean enableUpgradableLockAndTwoStageOptimize() {
        return true;
    }

    /** An {@link IcingSearchEngine} that records the optimize related calls. */
    private static final class TestIcingSearchEngine extends IcingSearchEngine {
        final List<String> mCalls = new ArrayList<>();

        /** If set, runs at the end of Stage 1, while Stage 1 still holds the read lock. */
        @Nullable Runnable mAfterPrepareHook;

        /**
         * If set, runs right before Stage 2 swaps, to simulate a write landing between the two
         * stages.
         */
        @Nullable Runnable mBeforeSwapHook;

        /** If set, returned by Stage 1 instead of the real result. */
        @Nullable OptimizePrepareResultProto mPrepareResultOverride;

        TestIcingSearchEngine(@NonNull IcingSearchEngineOptions options) {
            super(options);
        }

        @Override
        public @NonNull PersistToDiskResultProto persistToDisk(
                PersistType.@NonNull Code persistTypeCode) {
            if (persistTypeCode == PersistType.Code.FULL) {
                mCalls.add(CALL_PERSIST_TO_DISK_FULL);
            }
            return super.persistToDisk(persistTypeCode);
        }

        @Override
        public @NonNull OptimizeResultProto optimize() {
            mCalls.add(CALL_OPTIMIZE);
            return super.optimize();
        }

        @Override
        public @NonNull OptimizePrepareResultProto optimizePrepareDestDirectory() {
            mCalls.add(CALL_PREPARE);
            if (mPrepareResultOverride != null) {
                return mPrepareResultOverride;
            }
            OptimizePrepareResultProto result = super.optimizePrepareDestDirectory();
            if (mAfterPrepareHook != null) {
                mAfterPrepareHook.run();
            }
            return result;
        }

        @Override
        public @NonNull OptimizeResultProto optimizeSwapToDestDirectory(
                @NonNull OptimizePrepareResultProto optimizePrepareResult) {
            mCalls.add(CALL_SWAP);
            if (mBeforeSwapHook != null) {
                mBeforeSwapHook.run();
            }
            return super.optimizeSwapToDestDirectory(optimizePrepareResult);
        }
    }

    /** Recreates {@link #mAppSearchImpl} with a {@link TestIcingSearchEngine}. */
    private @NonNull TestIcingSearchEngine recreateAppSearchImplWithTestIcing() throws Exception {
        mAppSearchImpl.close();
        IcingSearchEngineOptions options = mUnlimitedConfig.toIcingSearchEngineOptions(
                mAppSearchDir.getAbsolutePath(), /* isVmEnabled= */ false);
        TestIcingSearchEngine icingSearchEngine = new TestIcingSearchEngine(options);
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                new AppSearchUserPlugins.Builder().setIcingSearchEngine(icingSearchEngine).build(),
                ALWAYS_OPTIMIZE);
        return icingSearchEngine;
    }

    /**
     * Sets a schema, puts "id1" and "id2", and removes "id1", so that there is one optimizable
     * document.
     */
    private void setUpOptimizableDocuments() throws Exception {
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                PACKAGE_NAME,
                DATABASE_NAME,
                Collections.singletonList(new AppSearchSchema.Builder(SCHEMA_TYPE).build()),
                /* visibilityConfigs= */ Collections.emptyList(),
                /* accountPropertyPaths= */ ImmutableMap.of(),
                /* forceOverride= */ false,
                /* version= */ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /* callStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        putDocument("id1");
        putDocument("id2");
        mAppSearchImpl.remove(PACKAGE_NAME, DATABASE_NAME, NAMESPACE, "id1",
                /* removeStatsBuilder= */ null, /* callStatsBuilder= */ null);
        assertThat(getOptimizableDocs()).isEqualTo(1);
    }

    private void putDocument(@NonNull String id) throws Exception {
        mAppSearchImpl.putDocument(
                PACKAGE_NAME,
                DATABASE_NAME,
                new GenericDocument.Builder<>(NAMESPACE, id, SCHEMA_TYPE).build(),
                /* sendChangeNotifications= */ false,
                /* logger= */ null,
                /* callStatsBuilder= */ null);
    }

    private @NonNull GenericDocument getDocument(@NonNull String id) throws Exception {
        return mAppSearchImpl.getDocument(PACKAGE_NAME, DATABASE_NAME, NAMESPACE, id,
                /* typePropertyPaths= */ Collections.emptyMap(),
                /* callStatsBuilder= */ null);
    }

    private long getOptimizableDocs() throws Exception {
        GetOptimizeInfoResultProto optimizeInfo =
                mAppSearchImpl.getOptimizeInfoResultLocked(/* callStatsBuilder= */ null);
        return optimizeInfo.getOptimizableDocs();
    }

    /** Puts a document directly into Icing, bypassing AppSearchImpl and its lock. */
    private static void putDocumentToIcing(@NonNull IcingSearchEngine icing, @NonNull String id) {
        DocumentProto.Builder documentBuilder = DocumentProto.newBuilder()
                .setNamespace(NAMESPACE)
                .setUri(id)
                .setSchema(SCHEMA_TYPE)
                .setCreationTimestampMs(System.currentTimeMillis());
        PrefixUtil.addPrefixToDocument(documentBuilder, PREFIX);
        assertThat(icing.put(documentBuilder.build()).getStatus().getCode())
                .isEqualTo(StatusProto.Code.OK);
    }

    private static StatusProto.@NonNull Code getDocumentStatusFromIcing(
            @NonNull IcingSearchEngine icing, @NonNull String id) {
        return icing.get(PREFIX + NAMESPACE, id, GetResultSpecProto.getDefaultInstance())
                .getStatus().getCode();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_OPTIMIZE_IMPROVEMENTS)
    public void testTwoStageOptimize_optimizeImprovementsDisabled_persistsBeforePrepare()
            throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();

        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        // Same as the single-stage Icing optimize, data is flushed before optimizing. The
        // single-stage optimize itself is never called.
        assertThat(icing.mCalls)
                .containsExactly(CALL_PERSIST_TO_DISK_FULL, CALL_PREPARE, CALL_SWAP).inOrder();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_OPTIMIZE_IMPROVEMENTS)
    public void testTwoStageOptimize_optimizeImprovementsEnabled_noPersistBeforePrepare()
            throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();

        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        assertThat(icing.mCalls).containsExactly(CALL_PREPARE, CALL_SWAP).inOrder();
    }

    @Test
    public void testTwoStageOptimize_putBetweenStages_abortsAndKeepsStorage() throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        icing.mBeforeSwapHook = () -> putDocumentToIcing(icing, "id3");

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        // Icing aborts Stage 2 without touching the active storage.
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(getOptimizableDocs()).isEqualTo(1);
        assertThat(getDocumentStatusFromIcing(icing, "id2")).isEqualTo(StatusProto.Code.OK);
        // The write between the two stages is not lost.
        assertThat(getDocumentStatusFromIcing(icing, "id3")).isEqualTo(StatusProto.Code.OK);
    }

    @Test
    public void testTwoStageOptimize_deleteBetweenStages_abortsAndKeepsStorage()
            throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        icing.mBeforeSwapHook = () -> assertThat(
                icing.delete(PREFIX + NAMESPACE, "id2").getStatus().getCode())
                .isEqualTo(StatusProto.Code.OK);

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        // The delete between the two stages is not reverted by a stale swap.
        assertThat(getDocumentStatusFromIcing(icing, "id2"))
                .isEqualTo(StatusProto.Code.NOT_FOUND);
        assertThat(getOptimizableDocs()).isEqualTo(2);
    }

    @Test
    public void testTwoStageOptimize_setSchemaBetweenStages_abortsAndKeepsStorage()
            throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        String newSchemaType = PREFIX + "newType";
        icing.mBeforeSwapHook = () -> {
            SchemaProto newSchema = icing.getSchema().getSchema().toBuilder()
                    .addTypes(SchemaTypeConfigProto.newBuilder().setSchemaType(newSchemaType))
                    .build();
            assertThat(icing.setSchema(newSchema).getStatus().getCode())
                    .isEqualTo(StatusProto.Code.OK);
        };

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        // The schema change between the two stages is kept and the documents are untouched.
        List<String> schemaTypes = new ArrayList<>();
        for (SchemaTypeConfigProto type : icing.getSchema().getSchema().getTypesList()) {
            schemaTypes.add(type.getSchemaType());
        }
        assertThat(schemaTypes).contains(newSchemaType);
        assertThat(getDocumentStatusFromIcing(icing, "id2")).isEqualTo(StatusProto.Code.OK);
        assertThat(getOptimizableDocs()).isEqualTo(1);
    }

    @Test
    public void testTwoStageOptimize_retryAfterAbort_succeeds() throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        icing.mBeforeSwapHook = () -> putDocumentToIcing(icing, "id3");
        assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        // Retry without any write between the two stages.
        icing.mBeforeSwapHook = null;
        mAppSearchImpl.optimize(/* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null);

        assertThat(getOptimizableDocs()).isEqualTo(0);
        assertThat(getDocumentStatusFromIcing(icing, "id2")).isEqualTo(StatusProto.Code.OK);
        assertThat(getDocumentStatusFromIcing(icing, "id3")).isEqualTo(StatusProto.Code.OK);
        // AppSearchImpl is still writable after optimize.
        putDocument("id4");
        assertThat(getDocument("id4").getId()).isEqualTo("id4");
    }

    @Test
    public void testTwoStageOptimize_multipleTimes_succeeds() throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();

        mAppSearchImpl.optimize(/* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null);
        assertThat(getOptimizableDocs()).isEqualTo(0);

        putDocument("id3");
        mAppSearchImpl.remove(PACKAGE_NAME, DATABASE_NAME, NAMESPACE, "id2",
                /* removeStatsBuilder= */ null, /* callStatsBuilder= */ null);
        assertThat(getOptimizableDocs()).isEqualTo(1);
        mAppSearchImpl.optimize(/* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null);

        assertThat(getOptimizableDocs()).isEqualTo(0);
        assertThat(getDocument("id3").getId()).isEqualTo("id3");
        assertThat(icing.mCalls).containsAtLeast(CALL_PREPARE, CALL_SWAP, CALL_PREPARE, CALL_SWAP)
                .inOrder();
        assertThat(icing.mCalls).doesNotContain(CALL_OPTIMIZE);
    }

    @Test
    public void testTwoStageOptimize_prepareFails_throwsAndSkipsSwap() throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        icing.mPrepareResultOverride = OptimizePrepareResultProto.newBuilder()
                .setStatus(StatusProto.newBuilder().setCode(StatusProto.Code.INTERNAL))
                .build();

        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(icing.mCalls).contains(CALL_PREPARE);
        assertThat(icing.mCalls).doesNotContain(CALL_SWAP);
        // The active storage is untouched and AppSearchImpl is still usable.
        assertThat(getOptimizableDocs()).isEqualTo(1);
        assertThat(getDocument("id2").getId()).isEqualTo("id2");
    }

    @Test
    public void testTwoStageOptimize_closedBeforeOptimize_throwsAndSkipsPrepare()
            throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        mAppSearchImpl.close();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> mAppSearchImpl.optimize(
                        /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null));

        assertThat(e).hasMessageThat().contains("closed AppSearchImpl");
        assertThat(icing.mCalls).doesNotContain(CALL_PREPARE);
        assertThat(icing.mCalls).doesNotContain(CALL_SWAP);
    }

    @Test
    public void testTwoStageOptimize_readsAreNotBlockedDuringPrepare() throws Exception {
        TestIcingSearchEngine icing = recreateAppSearchImplWithTestIcing();
        setUpOptimizableDocuments();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<GenericDocument> readResults = new ArrayList<>();
        // Stage 1 only holds the read lock, so a read from another thread can complete while
        // Stage 1 is still running.
        icing.mAfterPrepareHook = () -> {
            Future<GenericDocument> future = executor.submit(() -> getDocument("id2"));
            try {
                readResults.add(future.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new AssertionError("Read was blocked during Stage 1", e);
            }
        };

        try {
            mAppSearchImpl.optimize(
                    /* optimizeStatsBuilder= */ null, /* callStatsBuilder= */ null);
        } finally {
            executor.shutdownNow();
        }

        assertThat(readResults).hasSize(1);
        assertThat(readResults.get(0).getId()).isEqualTo("id2");
        assertThat(getOptimizableDocs()).isEqualTo(0);
    }
}
