/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.PutDocumentStatsProto;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;
import java.util.List;

public class AppSearchLoggerTest {
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private AppSearchImpl mAppSearchImpl;
    private TestLogger mLogger;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Give ourselves global query permissions
        mAppSearchImpl = AppSearchImpl.create(mTemporaryFolder.newFolder(),
                context, VisibilityStore.NO_OP_USER_ID,
                /*globalQuerierPackage=*/ context.getPackageName(),
                /*logger=*/ null);
        mLogger = new TestLogger();
    }

    // Test only not thread safe.
    public class TestLogger implements AppSearchLogger {
        @Nullable
        CallStats mCallStats;
        @Nullable
        PutDocumentStats mPutDocumentStats;
        @Nullable
        InitializeStats mInitializeStats;

        @Override
        public void logStats(@NonNull CallStats stats) {
            mCallStats = stats;
        }

        @Override
        public void logStats(@NonNull PutDocumentStats stats) {
            mPutDocumentStats = stats;
        }

        @Override
        public void logStats(@NonNull InitializeStats stats) {
            mInitializeStats = stats;
        }
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_initialize() {
        int nativeLatencyMillis = 3;
        int nativeDocumentStoreRecoveryCause = InitializeStatsProto.RecoveryCause.DATA_LOSS_VALUE;
        int nativeIndexRestorationCause =
                InitializeStatsProto.RecoveryCause.INCONSISTENT_WITH_GROUND_TRUTH_VALUE;
        int nativeSchemaStoreRecoveryCause =
                InitializeStatsProto.RecoveryCause.SCHEMA_CHANGES_OUT_OF_SYNC_VALUE;
        int nativeDocumentStoreRecoveryLatencyMillis = 7;
        int nativeIndexRestorationLatencyMillis = 8;
        int nativeSchemaStoreRecoveryLatencyMillis = 9;
        int nativeDocumentStoreDataStatus =
                InitializeStatsProto.DocumentStoreDataStatus.NO_DATA_LOSS_VALUE;
        int nativeNumDocuments = 11;
        int nativeNumSchemaTypes = 12;
        InitializeStatsProto.Builder nativeInitBuilder = InitializeStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDocumentStoreRecoveryCause(InitializeStatsProto.RecoveryCause.forNumber(
                        nativeDocumentStoreRecoveryCause))
                .setIndexRestorationCause(
                        InitializeStatsProto.RecoveryCause.forNumber(nativeIndexRestorationCause))
                .setSchemaStoreRecoveryCause(
                        InitializeStatsProto.RecoveryCause.forNumber(
                                nativeSchemaStoreRecoveryCause))
                .setDocumentStoreRecoveryLatencyMs(nativeDocumentStoreRecoveryLatencyMillis)
                .setIndexRestorationLatencyMs(nativeIndexRestorationLatencyMillis)
                .setSchemaStoreRecoveryLatencyMs(nativeSchemaStoreRecoveryLatencyMillis)
                .setDocumentStoreDataStatus(InitializeStatsProto.DocumentStoreDataStatus.forNumber(
                        nativeDocumentStoreDataStatus))
                .setNumDocuments(nativeNumDocuments)
                .setNumSchemaTypes(nativeNumSchemaTypes);
        InitializeStats.Builder initBuilder = new InitializeStats.Builder();

        AppSearchLoggerHelper.copyNativeStats(nativeInitBuilder.build(), initBuilder);

        InitializeStats iStats = initBuilder.build();
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(iStats.getDocumentStoreRecoveryCause()).isEqualTo(
                nativeDocumentStoreRecoveryCause);
        assertThat(iStats.getIndexRestorationCause()).isEqualTo(nativeIndexRestorationCause);
        assertThat(iStats.getSchemaStoreRecoveryCause()).isEqualTo(
                nativeSchemaStoreRecoveryCause);
        assertThat(iStats.getDocumentStoreRecoveryLatencyMillis()).isEqualTo(
                nativeDocumentStoreRecoveryLatencyMillis);
        assertThat(iStats.getIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(iStats.getSchemaStoreRecoveryLatencyMillis()).isEqualTo(
                nativeSchemaStoreRecoveryLatencyMillis);
        assertThat(iStats.getDocumentStoreDataStatus()).isEqualTo(
                nativeDocumentStoreDataStatus);
        assertThat(iStats.getDocumentCount()).isEqualTo(nativeNumDocuments);
        assertThat(iStats.getSchemaTypeCount()).isEqualTo(nativeNumSchemaTypes);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_putDocument() {
        final int nativeLatencyMillis = 3;
        final int nativeDocumentStoreLatencyMillis = 4;
        final int nativeIndexLatencyMillis = 5;
        final int nativeIndexMergeLatencyMillis = 6;
        final int nativeDocumentSize = 7;
        final int nativeNumTokensIndexed = 8;
        final boolean nativeExceededMaxNumTokens = true;
        PutDocumentStatsProto nativePutDocumentStats = PutDocumentStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDocumentStoreLatencyMs(nativeDocumentStoreLatencyMillis)
                .setIndexLatencyMs(nativeIndexLatencyMillis)
                .setIndexMergeLatencyMs(nativeIndexMergeLatencyMillis)
                .setDocumentSize(nativeDocumentSize)
                .setTokenizationStats(PutDocumentStatsProto.TokenizationStats.newBuilder()
                        .setNumTokensIndexed(nativeNumTokensIndexed)
                        .setExceededMaxTokenNum(nativeExceededMaxNumTokens)
                        .build())
                .build();
        PutDocumentStats.Builder pBuilder = new PutDocumentStats.Builder(
                "packageName",
                "database");

        AppSearchLoggerHelper.copyNativeStats(nativePutDocumentStats, pBuilder);

        PutDocumentStats pStats = pBuilder.build();
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getNativeDocumentStoreLatencyMillis()).isEqualTo(
                nativeDocumentStoreLatencyMillis);
        assertThat(pStats.getNativeIndexLatencyMillis()).isEqualTo(nativeIndexLatencyMillis);
        assertThat(pStats.getNativeIndexMergeLatencyMillis()).isEqualTo(
                nativeIndexMergeLatencyMillis);
        assertThat(pStats.getNativeDocumentSizeBytes()).isEqualTo(nativeDocumentSize);
        assertThat(pStats.getNativeNumTokensIndexed()).isEqualTo(nativeNumTokensIndexed);
        assertThat(pStats.getNativeExceededMaxNumTokens()).isEqualTo(nativeExceededMaxNumTokens);
    }


    //
    // Testing actual logging
    //
    @Test
    public void testLoggingStats_initialize() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        AppSearchImpl appSearchImpl = AppSearchImpl.create(mTemporaryFolder.newFolder(),
                context,
                VisibilityStore.NO_OP_USER_ID,
                /*globalQuerierPackage=*/ context.getPackageName(),
                mLogger);

        InitializeStats iStats = mLogger.mInitializeStats;
        assertThat(iStats).isNotNull();
        assertThat(iStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(iStats.getTotalLatencyMillis()).isGreaterThan(0);
        assertThat(iStats.hasDeSync()).isFalse();
        assertThat(iStats.getNativeLatencyMillis()).isGreaterThan(0);
        assertThat(iStats.getDocumentStoreDataStatus()).isEqualTo(
                InitializeStatsProto.DocumentStoreDataStatus.NO_DATA_LOSS_VALUE);
        assertThat(iStats.getDocumentCount()).isEqualTo(0);
        assertThat(iStats.getSchemaTypeCount()).isEqualTo(0);
    }

    @Test
    public void testLoggingStats_putDocument() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        mAppSearchImpl.setSchema(testPackageName, testDatabase, schemas,
                /*schemasNotPlatformSurfaceable=*/
                Collections.emptyList(), /*schemasPackageAccessible=*/ Collections.emptyMap(),
                /*forceOverride=*/ false, /*version=*/ 0);
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "type").build();

        mAppSearchImpl.putDocument(testPackageName, testDatabase, document, mLogger);

        PutDocumentStats pStats = mLogger.mPutDocumentStats;
        assertThat(pStats).isNotNull();
        assertThat(pStats.getGeneralStats().getPackageName()).isEqualTo(testPackageName);
        assertThat(pStats.getGeneralStats().getDatabase()).isEqualTo(testDatabase);
        assertThat(pStats.getGeneralStats().getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // The rest of native stats have been tested in testCopyNativeStats
        assertThat(pStats.getNativeDocumentSizeBytes()).isGreaterThan(0);
    }
}
