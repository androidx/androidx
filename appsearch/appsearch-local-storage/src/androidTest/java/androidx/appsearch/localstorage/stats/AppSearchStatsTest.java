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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;
import androidx.appsearch.stats.SchemaMigrationStats;

import com.google.android.icing.proto.PersistType;

import org.junit.Test;

public class AppSearchStatsTest {
    static final String TEST_PACKAGE_NAME = "com.google.test";
    static final String TEST_DATA_BASE = "testDataBase";
    static final int TEST_STATUS_CODE = AppSearchResult.RESULT_INTERNAL_ERROR;
    static final int TEST_TOTAL_LATENCY_MILLIS = 20;

    @Test
    public void testAppSearchStats_CallStats() {
        final int estimatedBinderLatencyMillis = 1;
        final int numOperationsSucceeded = 2;
        final int numOperationsFailed = 3;
        final int javaLockAcquisitionLatencyMillis = 4;
        final @CallStats.CallType int lastBlockingOperation = BaseStats.CALL_TYPE_REMOVE_BLOB;
        final int lastBlockingOperationLatencyMillis = 6;
        final int getVmLatency1 = 7;
        final int getVmLatency2 = 8;
        final int unblockedAppSearchLatencyMillis = 9;
        final int callReceivedTimestampMillis = 10;
        final int lastCallTypeHoldExecutor = 11;
        final int executorAcquisitionLatencyMillis = 12;
        final int onExecutorLatencyMillis = 13;
        final int getUserInstanceLatencyMillis = 14;
        final int pvmBinderLatencyMillis = 15;
        final int requestPayloadSize = 16;
        final int responsePayloadSize = 17;
        final int enabled_features = 3; // 0b0011

        final @CallStats.CallType int callType =
                BaseStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setCallType(callType)
                .setEstimatedBinderLatencyMillis(estimatedBinderLatencyMillis)
                .setNumOperationsSucceeded(numOperationsSucceeded)
                .setNumOperationsFailed(numOperationsFailed)
                .setLaunchVMEnabled(true)
                .setLaunchVM2Enabled(true)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatency1)
                .addGetVmLatencyMillis(getVmLatency2)
                .setUnblockedAppSearchLatencyMillis(unblockedAppSearchLatencyMillis)
                .setCallReceivedTimestampMillis(callReceivedTimestampMillis)
                .setLastCallTypeHoldExecutor(lastCallTypeHoldExecutor)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setOnExecutorLatencyMillis(onExecutorLatencyMillis)
                .setGetUserInstanceLatency(getUserInstanceLatencyMillis)
                .setPvmBinderLatency(pvmBinderLatencyMillis)
                .setRequestPayloadSize(requestPayloadSize)
                .setResponsePayloadSize(responsePayloadSize)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEstimatedBinderLatencyMillis())
                .isEqualTo(estimatedBinderLatencyMillis);
        assertThat(cStats.getCallType()).isEqualTo(callType);
        assertThat(cStats.getNumOperationsSucceeded()).isEqualTo(numOperationsSucceeded);
        assertThat(cStats.getNumOperationsFailed()).isEqualTo(numOperationsFailed);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(enabled_features);
        assertThat(cStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(cStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(cStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(cStats.getGetVmLatencyMillis())
                .isEqualTo(getVmLatency1 + getVmLatency2);
        assertThat(cStats.getUnblockedAppSearchLatencyMillis())
                .isEqualTo(unblockedAppSearchLatencyMillis);
        assertThat(cStats.getNumIcingCalls())
                .isEqualTo(2);
        assertThat(cStats.getCallReceivedTimestampMillis())
                .isEqualTo(callReceivedTimestampMillis);
        assertThat(cStats.getLastCallTypeHoldExecutor())
                .isEqualTo(lastCallTypeHoldExecutor);
        assertThat(cStats.getExecutorAcquisitionLatencyMillis())
                .isEqualTo(executorAcquisitionLatencyMillis);
        assertThat(cStats.getOnExecutorLatencyMillis())
                .isEqualTo(onExecutorLatencyMillis);
        assertThat(cStats.getGetUserInstanceLatencyMillis())
                .isEqualTo(getUserInstanceLatencyMillis);
        assertThat(cStats.getPvmBinderLatencyMillis())
                .isEqualTo(pvmBinderLatencyMillis);
        assertThat(cStats.getRequestPayloadSize())
                .isEqualTo(requestPayloadSize);
        assertThat(cStats.getResponsePayloadSize())
                .isEqualTo(responsePayloadSize);
        String expectedString = "CallStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  callType=3,\n"
                + "  estimatedBinderLatencyMillis=1,\n"
                + "  numOperationsSucceeded=2,\n"
                + "  numOperationsFailed=3,\n"
                + "  callReceivedTimestampMillis=10,\n"
                + "  lastCallTypeHoldExecutor=11,\n"
                + "  executorAcquisitionLatencyMillis=12,\n"
                + "  onExecutorLatencyMillis=13,\n"
                + "  getUserInstanceLatencyMillis=14,\n"
                + "  pvmBinderLatencyMillis=15,\n"
                + "  requestPayloadSize=16,\n"
                + "  responsePayloadSize=17,\n"
                + "  enabledFeatures=11,\n"
                + "  javaLockAcquisitionLatencyMillis=4,\n"
                + "  lastBlockingOperation=36,\n"
                + "  lastBlockingOperationLatencyMillis=6,\n"
                + "  getVmLatencyMillis=15,\n"
                + "  unblockedAppSearchLatencyMillis=9,\n"
                + "  numIcingCalls=2\n"
                + "}";
        assertThat(cStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_noLaunchVMEnabled_false() {
        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(0);
    }

    @Test
    public void testAppSearchStats_setLaunchVMEnabled_false() {
        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setLaunchVMEnabled(false)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(0);
    }

    @Test
    public void testAppSearchStats_setLaunchVM2Enabled_false() {
        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setLaunchVM2Enabled(false)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(0);
    }

    @Test
    public void testAppSearchCallStats_nullValues() {
        final @CallStats.CallType int callType =
                BaseStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats.Builder cStatsBuilder = new CallStats.Builder()
                .setCallType(callType);

        final CallStats cStats = cStatsBuilder.build();

        assertThat(cStats.getPackageName()).isNull();
        assertThat(cStats.getDatabase()).isNull();
        assertThat(cStats.getCallType()).isEqualTo(callType);
    }

    @Test
    public void testAppSearchStats_PutDocumentStats() {
        final int generateDocumentProtoLatencyMillis = 1;
        final int rewriteDocumentTypesLatencyMillis = 2;
        final int nativeLatencyMillis = 3;
        final int nativeDocumentStoreLatencyMillis = 4;
        final int nativeIndexLatencyMillis = 5;
        final int nativeIndexMergeLatencyMillis = 6;
        final int nativeDocumentSize = 7;
        final int nativeNumTokensIndexed = 8;
        final boolean nativeExceededMaxNumTokens = true;
        final int nativeTermIndexLatencyMillis = 9;
        final int nativeIntegerIndexLatencyMillis = 10;
        final int nativeQualifiedIdJoinIndexLatencyMillis = 11;
        final int nativeLiteIndexSortLatencyMillis = 12;
        final int enabledFeatures = 3; //0b0011
        int metadataTermIndexLatencyMillis = 13;
        int embeddingIndexLatencyMillis = 14;
        final int javaLockAcquisitionLatencyMillis = 15;
        final int lastBlockingOperation = 16;
        final int lastBlockingOperationLatencyMillis = 17;
        final int getVmLatencyMillis = 18;
        final PutDocumentStats.Builder pStatsBuilder =
                new PutDocumentStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                        .setStatusCode(TEST_STATUS_CODE)
                        .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                        .setGenerateDocumentProtoLatencyMillis(generateDocumentProtoLatencyMillis)
                        .setRewriteDocumentTypesLatencyMillis(rewriteDocumentTypesLatencyMillis)
                        .setNativeLatencyMillis(nativeLatencyMillis)
                        .setNativeDocumentStoreLatencyMillis(nativeDocumentStoreLatencyMillis)
                        .setNativeIndexLatencyMillis(nativeIndexLatencyMillis)
                        .setNativeIndexMergeLatencyMillis(nativeIndexMergeLatencyMillis)
                        .setNativeDocumentSizeBytes(nativeDocumentSize)
                        .setNativeNumTokensIndexed(nativeNumTokensIndexed)
                        .setNativeTermIndexLatencyMillis(nativeTermIndexLatencyMillis)
                        .setNativeIntegerIndexLatencyMillis(nativeIntegerIndexLatencyMillis)
                        .setNativeQualifiedIdJoinIndexLatencyMillis(
                                nativeQualifiedIdJoinIndexLatencyMillis)
                        .setNativeLiteIndexSortLatencyMillis(nativeLiteIndexSortLatencyMillis)
                        .setLaunchVMEnabled(true)
                        .setLaunchVM2Enabled(true)
                        .setMetadataTermIndexLatencyMillis(metadataTermIndexLatencyMillis)
                        .setEmbeddingIndexLatencyMillis(embeddingIndexLatencyMillis)
                        .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                        .setLastBlockingOperation(lastBlockingOperation)
                        .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                        .addGetVmLatencyMillis(getVmLatencyMillis);

        final PutDocumentStats pStats = pStatsBuilder.build();

        assertThat(pStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(pStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(pStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(pStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(pStats.getGenerateDocumentProtoLatencyMillis()).isEqualTo(
                generateDocumentProtoLatencyMillis);
        assertThat(pStats.getRewriteDocumentTypesLatencyMillis()).isEqualTo(
                rewriteDocumentTypesLatencyMillis);
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getNativeDocumentStoreLatencyMillis()).isEqualTo(
                nativeDocumentStoreLatencyMillis);
        assertThat(pStats.getNativeIndexLatencyMillis()).isEqualTo(nativeIndexLatencyMillis);
        assertThat(pStats.getNativeIndexMergeLatencyMillis()).isEqualTo(
                nativeIndexMergeLatencyMillis);
        assertThat(pStats.getNativeDocumentSizeBytes()).isEqualTo(nativeDocumentSize);
        assertThat(pStats.getNativeNumTokensIndexed()).isEqualTo(nativeNumTokensIndexed);
        assertThat(pStats.getNativeTermIndexLatencyMillis()).isEqualTo(
                nativeTermIndexLatencyMillis);
        assertThat(pStats.getNativeIntegerIndexLatencyMillis()).isEqualTo(
                nativeIntegerIndexLatencyMillis);
        assertThat(pStats.getNativeQualifiedIdJoinIndexLatencyMillis()).isEqualTo(
                nativeQualifiedIdJoinIndexLatencyMillis);
        assertThat(pStats.getNativeLiteIndexSortLatencyMillis()).isEqualTo(
                nativeLiteIndexSortLatencyMillis);
        assertThat(pStats.getEnabledFeatures()).isEqualTo(
                enabledFeatures);
        assertThat(pStats.getMetadataTermIndexLatencyMillis()).isEqualTo(
                metadataTermIndexLatencyMillis);
        assertThat(pStats.getEmbeddingIndexLatencyMillis()).isEqualTo(
                embeddingIndexLatencyMillis);
        assertThat(pStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(pStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(pStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(pStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "PutDocumentStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  generateDocumentProtoLatencyMillis=1,\n"
                + "  rewriteDocumentTypesLatencyMillis=2,\n"
                + "  nativeLatencyMillis=3,\n"
                + "  nativeDocumentStoreLatencyMillis=4,\n"
                + "  nativeIndexLatencyMillis=5,\n"
                + "  nativeIndexMergeLatencyMillis=6,\n"
                + "  nativeDocumentSizeBytes=7,\n"
                + "  nativeNumTokensIndexed=8,\n"
                + "  nativeTermIndexLatencyMillis=9,\n"
                + "  nativeIntegerIndexLatencyMillis=10,\n"
                + "  nativeQualifiedIdJoinIndexLatencyMillis=11,\n"
                + "  nativeLiteIndexSortLatencyMillis=12,\n"
                + "  metadataTermIndexLatencyMillis=13,\n"
                + "  embeddingIndexLatencyMillis=14,\n"
                + "  enabledFeatures=11,\n"
                + "  javaLockAcquisitionLatencyMillis=15,\n"
                + "  lastBlockingOperation=16,\n"
                + "  lastBlockingOperationLatencyMillis=17,\n"
                + "  getVmLatencyMillis=18,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(pStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_InitializeStats() {
        int enabledFeatures = 3; //0b0011
        int prepareSchemaAndNamespacesLatencyMillis = 1;
        int prepareVisibilityFileLatencyMillis = 2;
        int nativeLatencyMillis = 3;
        int nativeDocumentStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_DEPENDENCIES_CHANGED;
        int nativeIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_FEATURE_FLAG_CHANGED;
        int nativeSchemaStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_IO_ERROR;
        int nativeDocumentStoreRecoveryLatencyMillis = 4;
        int nativeIndexRestorationLatencyMillis = 5;
        int nativeSchemaStoreRecoveryLatencyMillis = 6;
        int nativeDocumentStoreDataStatus = 7;
        int nativeNumDocuments = 8;
        int nativeNumSchemaTypes = 9;
        int numPreviousInitFailures = 10;
        int integerIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int qualifiedIdJoinIndexRestorationCause =
                InitializeStats.RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH;
        int embeddingIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int initializeIcuDataStatusCode = 11;
        int numFailedReindexedDocuments = 12;
        final int javaLockAcquisitionLatencyMillis = 13;
        final int lastBlockingOperation = 14;
        final int lastBlockingOperationLatencyMillis = 15;
        int getVmLatencyMillis = 16;

        final InitializeStats.Builder iStatsBuilder = new InitializeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setHasDeSync(/* hasDeSyncs= */ true)
                .setPrepareSchemaAndNamespacesLatencyMillis(prepareSchemaAndNamespacesLatencyMillis)
                .setPrepareVisibilityStoreLatencyMillis(prepareVisibilityFileLatencyMillis)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setNativeDocumentStoreRecoveryCause(nativeDocumentStoreRecoveryCause)
                .setNativeIndexRestorationCause(nativeIndexRestorationCause)
                .setNativeSchemaStoreRecoveryCause(nativeSchemaStoreRecoveryCause)
                .setNativeDocumentStoreRecoveryLatencyMillis(
                        nativeDocumentStoreRecoveryLatencyMillis)
                .setNativeIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setNativeSchemaStoreRecoveryLatencyMillis(nativeSchemaStoreRecoveryLatencyMillis)
                .setNativeDocumentStoreDataStatus(nativeDocumentStoreDataStatus)
                .setNativeDocumentCount(nativeNumDocuments)
                .setNativeSchemaTypeCount(nativeNumSchemaTypes)
                .setHasReset(true)
                .setResetStatusCode(AppSearchResult.RESULT_INVALID_SCHEMA)
                .setLaunchVMEnabled(true)
                .setLaunchVM2Enabled(true)
                .setNativeNumPreviousInitFailures(numPreviousInitFailures)
                .setNativeIntegerIndexRestorationCause(integerIndexRestorationCause)
                .setNativeQualifiedIdJoinIndexRestorationCause(qualifiedIdJoinIndexRestorationCause)
                .setNativeEmbeddingIndexRestorationCause(embeddingIndexRestorationCause)
                .setNativeInitializeIcuDataStatusCode(initializeIcuDataStatusCode)
                .setNativeNumFailedReindexedDocuments(numFailedReindexedDocuments)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis);
        final InitializeStats iStats = iStatsBuilder.build();

        assertThat(iStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(iStats.hasDeSync()).isTrue();
        assertThat(iStats.getPrepareSchemaAndNamespacesLatencyMillis()).isEqualTo(
                prepareSchemaAndNamespacesLatencyMillis);
        assertThat(iStats.getPrepareVisibilityStoreLatencyMillis()).isEqualTo(
                prepareVisibilityFileLatencyMillis);
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreRecoveryCause()).isEqualTo(
                nativeDocumentStoreRecoveryCause);
        assertThat(iStats.getNativeIndexRestorationCause()).isEqualTo(nativeIndexRestorationCause);
        assertThat(iStats.getNativeSchemaStoreRecoveryCause()).isEqualTo(
                nativeSchemaStoreRecoveryCause);
        assertThat(iStats.getNativeDocumentStoreRecoveryLatencyMillis()).isEqualTo(
                nativeDocumentStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(iStats.getNativeSchemaStoreRecoveryLatencyMillis()).isEqualTo(
                nativeSchemaStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreDataStatus()).isEqualTo(
                nativeDocumentStoreDataStatus);
        assertThat(iStats.getNativeDocumentCount()).isEqualTo(nativeNumDocuments);
        assertThat(iStats.getNativeSchemaTypeCount()).isEqualTo(nativeNumSchemaTypes);
        assertThat(iStats.hasReset()).isTrue();
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_INVALID_SCHEMA);
        assertThat(iStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(iStats.getNativeNumPreviousInitFailures()).isEqualTo(numPreviousInitFailures);
        assertThat(iStats.getNativeIntegerIndexRestorationCause())
                .isEqualTo(integerIndexRestorationCause);
        assertThat(iStats.getNativeQualifiedIdJoinIndexRestorationCause())
                .isEqualTo(qualifiedIdJoinIndexRestorationCause);
        assertThat(iStats.getNativeEmbeddingIndexRestorationCause())
                .isEqualTo(embeddingIndexRestorationCause);
        assertThat(iStats.getNativeInitializeIcuDataStatusCode())
                .isEqualTo(initializeIcuDataStatusCode);
        assertThat(iStats.getNativeNumFailedReindexedDocuments())
                .isEqualTo(numFailedReindexedDocuments);
        assertThat(iStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(iStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(iStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(iStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "InitializeStats {\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  hasDeSync=true,\n"
                + "  prepareSchemaAndNamespacesLatencyMillis=1,\n"
                + "  prepareVisibilityStoreLatencyMillis=2,\n"
                + "  nativeLatencyMillis=3,\n"
                + "  nativeDocumentStoreRecoveryCause=7,\n"
                + "  nativeIndexRestorationCause=8,\n"
                + "  nativeSchemaStoreRecoveryCause=4,\n"
                + "  nativeDocumentStoreRecoveryLatencyMillis=4,\n"
                + "  nativeIndexRestorationLatencyMillis=5,\n"
                + "  nativeSchemaStoreRecoveryLatencyMillis=6,\n"
                + "  nativeDocumentStoreDataStatus=7,\n"
                + "  nativeNumDocuments=8,\n"
                + "  nativeNumSchemaTypes=9,\n"
                + "  nativeNumPreviousInitFailures=10,\n"
                + "  nativeIntegerIndexRestorationCause=1,\n"
                + "  nativeQualifiedIdJoinIndexRestorationCause=2,\n"
                + "  nativeEmbeddingIndexRestorationCause=1,\n"
                + "  nativeInitializeIcuDataStatusCode=11,\n"
                + "  nativeNumFailedReindexedDocuments=12,\n"
                + "  hasReset=true,\n"
                + "  resetStatusCode=7,\n"
                + "  enabledFeatures=11,\n"
                + "  javaLockAcquisitionLatencyMillis=13,\n"
                + "  lastBlockingOperation=14,\n"
                + "  lastBlockingOperationLatencyMillis=15,\n"
                + "  getVmLatencyMillis=16,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(iStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_SearchStats() {
        int nativeQueryLength = 1;
        int nativeNumTerms = 2;
        int nativeNumNamespacesFiltered = 3;
        int nativeNumSchemaTypesFiltered = 4;
        int nativeRankingStrategy = 5;
        int nativeNumDocumentsScored = 6;
        int nativeParseQueryLatencyMillis = 7;
        int nativeScoringLatencyMillis = 8;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 9;
        int numFetchedHitsMainIndex = 10;
        int numFetchedHitsIntegerIndex = 11;
        int queryProcessorLexerExtractTokenLatencyMillis = 12;
        int queryProcessorParserConsumeQueryLatencyMillis = 13;
        int queryProcessorQueryVisitorLatencyMillis = 14;
        int numUnquantizedEmbeddingsScored = 15;
        int numQuantizedEmbeddingsScored = 16;
        int numEmbeddingShardsRead = 17;
        long numEmbeddingBytesRead = 18L;

        final SearchStats.Builder sStatsBuilder = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis)
                .setNativeNumUnquantizedEmbeddingsScored(numUnquantizedEmbeddingsScored)
                .setNativeNumQuantizedEmbeddingsScored(numQuantizedEmbeddingsScored)
                .setNativeNumEmbeddingShardsRead(numEmbeddingShardsRead)
                .setNativeNumEmbeddingBytesRead(numEmbeddingBytesRead);
        final SearchStats sStats = sStatsBuilder.build();

        assertThat(sStats.getNativeQueryLength()).isEqualTo(nativeQueryLength);
        assertThat(sStats.getNativeTermCount()).isEqualTo(nativeNumTerms);
        assertThat(sStats.getNativeFilteredNamespaceCount()).isEqualTo(nativeNumNamespacesFiltered);
        assertThat(sStats.getNativeFilteredSchemaTypeCount()).isEqualTo(
                nativeNumSchemaTypesFiltered);
        assertThat(sStats.getNativeRankingStrategy()).isEqualTo(nativeRankingStrategy);
        assertThat(sStats.getNativeParseQueryLatencyMillis()).isEqualTo(
                nativeParseQueryLatencyMillis);
        assertThat(sStats.getNativeScoredDocumentCount()).isEqualTo(nativeNumDocumentsScored);
        assertThat(sStats.getNativeScoringLatencyMillis()).isEqualTo(nativeScoringLatencyMillis);
        assertThat(sStats.isNativeNumericQuery()).isEqualTo(isNumericQuery);
        assertThat(sStats.getNativeNumFetchedHitsLiteIndex()).isEqualTo(numFetchedHitsLiteIndex);
        assertThat(sStats.getNativeNumFetchedHitsMainIndex()).isEqualTo(numFetchedHitsMainIndex);
        assertThat(sStats.getNativeNumFetchedHitsIntegerIndex())
                .isEqualTo(numFetchedHitsIntegerIndex);
        assertThat(sStats.getNativeQueryProcessorLexerExtractTokenLatencyMillis())
                .isEqualTo(queryProcessorLexerExtractTokenLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorParserConsumeQueryLatencyMillis())
                .isEqualTo(queryProcessorParserConsumeQueryLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorQueryVisitorLatencyMillis())
                .isEqualTo(queryProcessorQueryVisitorLatencyMillis);
        assertThat(sStats.getNativeNumUnquantizedEmbeddingsScored()).isEqualTo(
                numUnquantizedEmbeddingsScored);
        assertThat(sStats.getNativeNumQuantizedEmbeddingsScored()).isEqualTo(
                numQuantizedEmbeddingsScored);
        assertThat(sStats.getNativeNumEmbeddingShardsRead()).isEqualTo(numEmbeddingShardsRead);
        assertThat(sStats.getNativeNumEmbeddingBytesRead()).isEqualTo(numEmbeddingBytesRead);
        String expectedString = "SearchStats {\n"
                + "  nativeQueryLength=1,\n"
                + "  nativeNumTerms=2,\n"
                + "  nativeNumNamespacesFiltered=3,\n"
                + "  nativeNumSchemaTypesFiltered=4,\n"
                + "  nativeRankingStrategy=5,\n"
                + "  nativeNumDocumentsScored=6,\n"
                + "  nativeParseQueryLatencyMillis=7,\n"
                + "  nativeScoringLatencyMillis=8,\n"
                + "  nativeIsNumericQuery=true,\n"
                + "  nativeNumFetchedHitsLiteIndex=9,\n"
                + "  nativeNumFetchedHitsMainIndex=10,\n"
                + "  nativeNumFetchedHitsIntegerIndex=11,\n"
                + "  nativeQueryProcessorLexerExtractTokenLatencyMillis=12,\n"
                + "  nativeQueryProcessorParserConsumeQueryLatencyMillis=13,\n"
                + "  nativeQueryProcessorQueryVisitorLatencyMillis=14\n"
                + "  nativeNumUnquantizedEmbeddingsScored=15\n"
                + "  nativeNumQuantizedEmbeddingsScored=16\n"
                + "  nativeNumEmbeddingShardsRead=17\n"
                + "  nativeNumEmbeddingBytesRead=18\n"
                + "}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_QueryStats() {
        int nativeQueryLength = 101;
        int nativeNumTerms = 102;
        int nativeNumNamespacesFiltered = 103;
        int nativeNumSchemaTypesFiltered = 104;
        int nativeRankingStrategy = 105;
        int nativeNumDocumentsScored = 106;
        int nativeParseQueryLatencyMillis = 107;
        int nativeScoringLatencyMillis = 108;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 109;
        int numFetchedHitsMainIndex = 110;
        int numFetchedHitsIntegerIndex = 111;
        int queryProcessorLexerExtractTokenLatencyMillis = 112;
        int queryProcessorParserConsumeQueryLatencyMillis = 113;
        int queryProcessorQueryVisitorLatencyMillis = 114;
        int numUnquantizedEmbeddingsScored = 115;
        int numQuantizedEmbeddingsScored = 116;
        int numEmbeddingShardsRead = 117;
        long numEmbeddingBytesRead = 118L;

        SearchStats searchStats = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis)
                .setNativeNumUnquantizedEmbeddingsScored(numUnquantizedEmbeddingsScored)
                .setNativeNumQuantizedEmbeddingsScored(numQuantizedEmbeddingsScored)
                .setNativeNumEmbeddingShardsRead(numEmbeddingShardsRead)
                .setNativeNumEmbeddingBytesRead(numEmbeddingBytesRead)
                .build();

        int enabledFeatures = 3; //0b0011
        int rewriteSearchSpecLatencyMillis = 202;
        int rewriteSearchResultLatencyMillis = 203;
        int javaLockAcquisitionLatencyMillis = 204;
        int aclCheckLatencyMillis = 205;
        int visibilityScope = QueryStats.VISIBILITY_SCOPE_LOCAL;
        String searchSourceLogTag = "tag";
        boolean nativeIsFirstPage = true;
        int nativeRequestedPageSize = 206;
        int nativeNumResultsReturnedCurrentPage = 207;
        int nativeLatencyMillis = 208;
        int nativeRankingLatencyMillis = 209;
        int nativeDocumentRetrievingLatencyMillis = 210;
        int nativeNumResultsSnippeted = 211;
        int nativeLockAcquisitionLatencyMillis = 212;
        int javaToNativeJniLatencyMillis = 213;
        int nativeToJavaJniLatencyMillis = 214;
        long liteIndexHitBufferByteSize = 215;
        long liteIndexHitBufferUnsortedByteSize = 216;
        int pageTypeToken = QueryStats.PAGE_TOKEN_TYPE_EMPTY;
        int numResultStatesEvicted = 217;
        int additionalPageCount = 218;
        int numResultsReturnedAdditionalPages = 219;
        int additionalPagesRetrievalLatency = 220;
        int firstNativeCallLatencyMillis = 221;
        int lastBlockingOperation = 222;
        int lastBlockingOperationLatencyMillis = 223;
        int getVmLatencyMillis = 224;

        final QueryStats.Builder qStatsBuilder = new QueryStats.Builder(visibilityScope,
                TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setLaunchVMEnabled(true)
                .setLaunchVM2Enabled(true)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setRewriteSearchSpecLatencyMillis(rewriteSearchSpecLatencyMillis)
                .setRewriteSearchResultLatencyMillis(rewriteSearchResultLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setAclCheckLatencyMillis(aclCheckLatencyMillis)
                .setSearchSourceLogTag(searchSourceLogTag)
                .setIsFirstPage(nativeIsFirstPage)
                .setRequestedPageSize(nativeRequestedPageSize)
                .setCurrentPageReturnedResultCount(nativeNumResultsReturnedCurrentPage)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setRankingLatencyMillis(nativeRankingLatencyMillis)
                .setDocumentRetrievingLatencyMillis(nativeDocumentRetrievingLatencyMillis)
                .setResultWithSnippetsCount(nativeNumResultsSnippeted)
                .setNativeLockAcquisitionLatencyMillis(nativeLockAcquisitionLatencyMillis)
                .setJavaToNativeJniLatencyMillis(javaToNativeJniLatencyMillis)
                .setNativeToJavaJniLatencyMillis(nativeToJavaJniLatencyMillis)
                .setChildSearchStats(searchStats)
                .setParentSearchStats(searchStats)
                .setLiteIndexHitBufferByteSize(liteIndexHitBufferByteSize)
                .setLiteIndexHitBufferUnsortedByteSize(liteIndexHitBufferUnsortedByteSize)
                .setPageTokenType(pageTypeToken)
                .setNumResultStatsEvicted(numResultStatesEvicted)
                .setAdditionalPageCount(additionalPageCount)
                .setAdditionalPagesReturnedResultCount(numResultsReturnedAdditionalPages)
                .setAdditionalPageRetrievalLatencyMillis(additionalPagesRetrievalLatency)
                .setFirstNativeCallLatency(firstNativeCallLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis);
        final QueryStats qStats = qStatsBuilder.build();

        assertThat(qStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(qStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(qStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(qStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(qStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(qStats.getRewriteSearchSpecLatencyMillis()).isEqualTo(
                rewriteSearchSpecLatencyMillis);
        assertThat(qStats.getRewriteSearchResultLatencyMillis()).isEqualTo(
                rewriteSearchResultLatencyMillis);
        assertThat(qStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(qStats.getAclCheckLatencyMillis()).isEqualTo(
                aclCheckLatencyMillis);
        assertThat(qStats.getVisibilityScope()).isEqualTo(visibilityScope);
        assertThat(qStats.getSearchSourceLogTag()).isEqualTo(searchSourceLogTag);
        assertThat(qStats.isFirstPage()).isTrue();
        assertThat(qStats.getRequestedPageSize()).isEqualTo(nativeRequestedPageSize);
        assertThat(qStats.getCurrentPageReturnedResultCount()).isEqualTo(
                nativeNumResultsReturnedCurrentPage);
        assertThat(qStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(qStats.getFirstNativeCallLatencyMillis()).isEqualTo(
                firstNativeCallLatencyMillis);
        assertThat(qStats.getRankingLatencyMillis()).isEqualTo(nativeRankingLatencyMillis);
        assertThat(qStats.getResultWithSnippetsCount()).isEqualTo(nativeNumResultsSnippeted);
        assertThat(qStats.getDocumentRetrievingLatencyMillis()).isEqualTo(
                nativeDocumentRetrievingLatencyMillis);
        assertThat(qStats.getNativeLockAcquisitionLatencyMillis()).isEqualTo(
                nativeLockAcquisitionLatencyMillis);
        assertThat(qStats.getJavaToNativeJniLatencyMillis()).isEqualTo(
                javaToNativeJniLatencyMillis);
        assertThat(qStats.getNativeToJavaJniLatencyMillis()).isEqualTo(
                nativeToJavaJniLatencyMillis);
        assertThat(qStats.getParentSearchStats()).isEqualTo(searchStats);
        assertThat(qStats.getChildSearchStats()).isEqualTo(searchStats);
        assertThat(qStats.getLiteIndexHitBufferByteSize()).isEqualTo(liteIndexHitBufferByteSize);
        assertThat(qStats.getLiteIndexHitBufferUnsortedByteSize())
                .isEqualTo(liteIndexHitBufferUnsortedByteSize);
        assertThat(qStats.getPageTokenType()).isEqualTo(pageTypeToken);
        assertThat(qStats.getNumResultStatesEvicted()).isEqualTo(numResultStatesEvicted);
        assertThat(qStats.getAdditionalPageCount()).isEqualTo(additionalPageCount);
        assertThat(qStats.getAdditionalPagesReturnedResultCount()).isEqualTo(
                numResultsReturnedAdditionalPages);
        assertThat(qStats.getAdditionalPageRetrievalLatencyMillis()).isEqualTo(
                additionalPagesRetrievalLatency);
        assertThat(qStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(qStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(qStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        String expectedString = "QueryStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  rewriteSearchSpecLatencyMillis=202,\n"
                + "  rewriteSearchResultLatencyMillis=203,\n"
                + "  aclCheckLatencyMillis=205,\n"
                + "  visibilityScope=1,\n"
                + "  searchSourceLogTag=tag,\n"
                + "  nativeIsFirstPage=true,\n"
                + "  additionalPageCount=218,\n"
                + "  nativeRequestedPageSize=206,\n"
                + "  nativeNumResultsReturnedCurrentPage=207,\n"
                + "  numResultsReturnedAdditionalPages=219,\n"
                + "  nativeLatencyMillis=208,\n"
                + "  firstNativeCallLatencyMillis=221,\n"
                + "  additionalPageRetrievalLatencyMillis=220,\n"
                + "  nativeRankingLatencyMillis=209,\n"
                + "  nativeDocumentRetrievingLatencyMillis=210,\n"
                + "  nativeNumResultsWithSnippets=211,\n"
                + "  nativeLockAcquisitionLatencyMillis=212,\n"
                + "  javaToNativeJniLatencyMillis=213,\n"
                + "  nativeToJavaJniLatencyMillis=214,\n"
                + "  nativeJoinLatencyMillis=0,\n"
                + "  nativeNumJoinedResultsCurrentPage=0,\n"
                + "  joinType=0,\n"
                + "  parentSearchStats=SearchStats {\n"
                + "    nativeQueryLength=101,\n"
                + "    nativeNumTerms=102,\n"
                + "    nativeNumNamespacesFiltered=103,\n"
                + "    nativeNumSchemaTypesFiltered=104,\n"
                + "    nativeRankingStrategy=105,\n"
                + "    nativeNumDocumentsScored=106,\n"
                + "    nativeParseQueryLatencyMillis=107,\n"
                + "    nativeScoringLatencyMillis=108,\n"
                + "    nativeIsNumericQuery=true,\n"
                + "    nativeNumFetchedHitsLiteIndex=109,\n"
                + "    nativeNumFetchedHitsMainIndex=110,\n"
                + "    nativeNumFetchedHitsIntegerIndex=111,\n"
                + "    nativeQueryProcessorLexerExtractTokenLatencyMillis=112,\n"
                + "    nativeQueryProcessorParserConsumeQueryLatencyMillis=113,\n"
                + "    nativeQueryProcessorQueryVisitorLatencyMillis=114\n"
                + "    nativeNumUnquantizedEmbeddingsScored=115\n"
                + "    nativeNumQuantizedEmbeddingsScored=116\n"
                + "    nativeNumEmbeddingShardsRead=117\n"
                + "    nativeNumEmbeddingBytesRead=118\n"
                + "  },\n"
                + "  childSearchStats=SearchStats {\n"
                + "    nativeQueryLength=101,\n"
                + "    nativeNumTerms=102,\n"
                + "    nativeNumNamespacesFiltered=103,\n"
                + "    nativeNumSchemaTypesFiltered=104,\n"
                + "    nativeRankingStrategy=105,\n"
                + "    nativeNumDocumentsScored=106,\n"
                + "    nativeParseQueryLatencyMillis=107,\n"
                + "    nativeScoringLatencyMillis=108,\n"
                + "    nativeIsNumericQuery=true,\n"
                + "    nativeNumFetchedHitsLiteIndex=109,\n"
                + "    nativeNumFetchedHitsMainIndex=110,\n"
                + "    nativeNumFetchedHitsIntegerIndex=111,\n"
                + "    nativeQueryProcessorLexerExtractTokenLatencyMillis=112,\n"
                + "    nativeQueryProcessorParserConsumeQueryLatencyMillis=113,\n"
                + "    nativeQueryProcessorQueryVisitorLatencyMillis=114\n"
                + "    nativeNumUnquantizedEmbeddingsScored=115\n"
                + "    nativeNumQuantizedEmbeddingsScored=116\n"
                + "    nativeNumEmbeddingShardsRead=117\n"
                + "    nativeNumEmbeddingBytesRead=118\n"
                + "  },\n"
                + "  liteIndexHitBufferByteSize=215,\n"
                + "  liteIndexHitBufferUnsortedByteSize=216,\n"
                + "  pageTokenType=3,\n"
                + "  numResultStatesEvicted=217,\n"
                + "  enabledFeatures=11,\n"
                + "  javaLockAcquisitionLatencyMillis=204,\n"
                + "  lastBlockingOperation=222,\n"
                + "  lastBlockingOperationLatencyMillis=223,\n"
                + "  getVmLatencyMillis=224,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(qStats.toString()).isEqualTo(expectedString);
        assertThat(qStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
    }

    @Test
    public void testAppSearchStats_SetSchemaStats() {
        int newTypeCount = 1;
        int deletedTypeCount = 2;
        int compatibleTypeChangeCount = 3;
        int indexIncompatibleTypeChangeCount = 4;
        int backwardsIncompatibleTypeChangeCount = 5;
        int verifyIncomingCallLatencyMillis = 6;
        int executorAcquisitionLatencyMillis = 7;
        int rebuildFromBundleLatencyMillis = 8;
        int javaLockAcquisitionLatencyMillis = 9;
        int totalNativeLatencyMillis = 10;
        int rewriteSchemaLatencyMillis = 11;
        int visibilitySettingLatencyMillis = 12;
        int convertToResponseLatencyMillis = 13;
        int dispatchChangeNotificationsLatencyMillis = 14;
        int optimizeLatencyMillis = 15;
        boolean isPackageObserved = true;
        int getOldSchemaLatencyMillis = 16;
        int getObserverLatencyMillis = 17;
        int sendNotificationLatencyMillis = 18;
        int lastBlockingOperation = 19;
        int lastBlockingOperationLatencyMillis = 20;
        int getVmLatencyMillis = 21;
        int enabledFeatures = 3; //0b0011
        int joinIndexIncompatibleTypeChangeCount = 22;
        int scorablePropertyIncompatibleTypeChangeCount = 23;
        int deletedDocumentCount = 24;
        boolean isTermIndexRestored = true;
        boolean isIntegerIndexRestored = true;
        boolean isEmbeddingIndexRestored = true;
        boolean isQualifiedIdJoinIndexRestored = true;
        int nativeSchemaStoreSetSchemaLatencyMillis = 25;
        int nativeDocumentStoreUpdateSchemaLatencyMillis = 26;
        int nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis = 27;
        int nativeIndexRestorationLatencyMillis = 28;
        int nativeScorablePropertyCacheRegenerationLatencyMillis = 29;
        SetSchemaStats sStats = new SetSchemaStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNewTypeCount(newTypeCount)
                .setDeletedTypeCount(deletedTypeCount)
                .setCompatibleTypeChangeCount(compatibleTypeChangeCount)
                .setIndexIncompatibleTypeChangeCount(indexIncompatibleTypeChangeCount)
                .setJoinIndexIncompatibleTypeChangeCount(joinIndexIncompatibleTypeChangeCount)
                .setScorablePropertyIncompatibleTypeChangeCount(
                        scorablePropertyIncompatibleTypeChangeCount)
                .setBackwardsIncompatibleTypeChangeCount(backwardsIncompatibleTypeChangeCount)
                .setDeletedDocumentCount(deletedDocumentCount)
                .setIsTermIndexRestored(isTermIndexRestored)
                .setIsIntegerIndexRestored(isIntegerIndexRestored)
                .setIsEmbeddingIndexRestored(isEmbeddingIndexRestored)
                .setIsQualifiedIdJoinIndexRestored(isQualifiedIdJoinIndexRestored)
                .setVerifyIncomingCallLatencyMillis(verifyIncomingCallLatencyMillis)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setRebuildFromBundleLatencyMillis(rebuildFromBundleLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setRewriteSchemaLatencyMillis(rewriteSchemaLatencyMillis)
                .setTotalNativeLatencyMillis(totalNativeLatencyMillis)
                .setNativeSchemaStoreSetSchemaLatencyMillis(nativeSchemaStoreSetSchemaLatencyMillis)
                .setNativeDocumentStoreUpdateSchemaLatencyMillis(
                        nativeDocumentStoreUpdateSchemaLatencyMillis)
                .setNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis(
                        nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis)
                .setNativeIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setNativeScorablePropertyCacheRegenerationLatencyMillis(
                        nativeScorablePropertyCacheRegenerationLatencyMillis)
                .setVisibilitySettingLatencyMillis(visibilitySettingLatencyMillis)
                .setConvertToResponseLatencyMillis(convertToResponseLatencyMillis)
                .setDispatchChangeNotificationsLatencyMillis(
                        dispatchChangeNotificationsLatencyMillis)
                .setOptimizeLatencyMillis(optimizeLatencyMillis)
                .setIsPackageObserved(isPackageObserved)
                .setGetOldSchemaLatencyMillis(getOldSchemaLatencyMillis)
                .setGetObserverLatencyMillis(getObserverLatencyMillis)
                .setPreparingChangeNotificationLatencyMillis(sendNotificationLatencyMillis)
                .setSchemaMigrationCallType(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA)
                .setLaunchVMEnabled(true)
                .setLaunchVM2Enabled(true)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getNewTypeCount()).isEqualTo(newTypeCount);
        assertThat(sStats.getDeletedTypeCount()).isEqualTo(deletedTypeCount);
        assertThat(sStats.getCompatibleTypeChangeCount()).isEqualTo(compatibleTypeChangeCount);
        assertThat(sStats.getIndexIncompatibleTypeChangeCount()).isEqualTo(
                indexIncompatibleTypeChangeCount);
        assertThat(sStats.getJoinIndexIncompatibleTypeChangeCount()).isEqualTo(
                joinIndexIncompatibleTypeChangeCount);
        assertThat(sStats.getScorablePropertyIncompatibleTypeChangeCount()).isEqualTo(
                scorablePropertyIncompatibleTypeChangeCount);
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(
                backwardsIncompatibleTypeChangeCount);
        assertThat(sStats.getDeletedDocumentCount()).isEqualTo(deletedDocumentCount);
        assertThat(sStats.isTermIndexRestored()).isEqualTo(isTermIndexRestored);
        assertThat(sStats.isIntegerIndexRestored()).isEqualTo(isIntegerIndexRestored);
        assertThat(sStats.isEmbeddingIndexRestored()).isEqualTo(isEmbeddingIndexRestored);
        assertThat(sStats.isQualifiedIdJoinIndexRestored()).isEqualTo(
                isQualifiedIdJoinIndexRestored);
        assertThat(sStats.getVerifyIncomingCallLatencyMillis()).isEqualTo(
                verifyIncomingCallLatencyMillis);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis()).isEqualTo(
                executorAcquisitionLatencyMillis);
        assertThat(sStats.getRebuildFromBundleLatencyMillis()).isEqualTo(
                rebuildFromBundleLatencyMillis);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getRewriteSchemaLatencyMillis()).isEqualTo(rewriteSchemaLatencyMillis);
        assertThat(sStats.getTotalNativeLatencyMillis()).isEqualTo(totalNativeLatencyMillis);
        assertThat(sStats.getNativeSchemaStoreSetSchemaLatencyMillis()).isEqualTo(
                nativeSchemaStoreSetSchemaLatencyMillis);
        assertThat(sStats.getNativeDocumentStoreUpdateSchemaLatencyMillis()).isEqualTo(
                nativeDocumentStoreUpdateSchemaLatencyMillis);
        assertThat(sStats.getNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis()).isEqualTo(
                nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis);
        assertThat(sStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(sStats.getNativeScorablePropertyCacheRegenerationLatencyMillis()).isEqualTo(
                nativeScorablePropertyCacheRegenerationLatencyMillis);
        assertThat(sStats.getVisibilitySettingLatencyMillis()).isEqualTo(
                visibilitySettingLatencyMillis);
        assertThat(sStats.getConvertToResponseLatencyMillis()).isEqualTo(
                convertToResponseLatencyMillis);
        assertThat(sStats.getDispatchChangeNotificationsLatencyMillis()).isEqualTo(
                dispatchChangeNotificationsLatencyMillis);
        assertThat(sStats.getOptimizeLatencyMillis()).isEqualTo(optimizeLatencyMillis);
        assertThat(sStats.isPackageObserved()).isEqualTo(isPackageObserved);
        assertThat(sStats.getGetOldSchemaLatencyMillis()).isEqualTo(getOldSchemaLatencyMillis);
        assertThat(sStats.getGetObserverLatencyMillis()).isEqualTo(getObserverLatencyMillis);
        assertThat(sStats.getPreparingChangeNotificationLatencyMillis())
                .isEqualTo(sendNotificationLatencyMillis);
        assertThat(sStats.getSchemaMigrationCallType())
                .isEqualTo(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA);
        assertThat(sStats.getEnabledFeatures())
                .isEqualTo(enabledFeatures);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(sStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(sStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "SetSchemaStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  newTypeCount=1,\n"
                + "  deletedTypeCount=2,\n"
                + "  compatibleTypeChangeCount=3,\n"
                + "  indexIncompatibleTypeChangeCount=4,\n"
                + "  joinIndexIncompatibleTypeChangeCount=22,\n"
                + "  scorablePropertyIncompatibleTypeChangeCount=23,\n"
                + "  backwardsIncompatibleTypeChangeCount=5,\n"
                + "  deletedDocumentCount=24,\n"
                + "  isTermIndexRestored=true,\n"
                + "  isIntegerIndexRestored=true,\n"
                + "  isEmbeddingIndexRestored=true,\n"
                + "  isQualifiedIdJoinIndexRestored=true,\n"
                + "  verifyIncomingCallLatencyMillis=6,\n"
                + "  executorAcquisitionLatencyMillis=7,\n"
                + "  rebuildFromBundleLatencyMillis=8,\n"
                + "  rewriteSchemaLatencyMillis=11,\n"
                + "  totalNativeLatencyMillis=10,\n"
                + "  nativeSchemaStoreSetSchemaLatencyMillis=25,\n"
                + "  nativeDocumentStoreUpdateSchemaLatencyMillis=26,\n"
                + "  nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis=27,\n"
                + "  nativeIndexRestorationLatencyMillis=28,\n"
                + "  nativeScorablePropertyCacheRegenerationLatencyMillis=29,\n"
                + "  visibilitySettingLatencyMillis=12,\n"
                + "  convertToResponseLatencyMillis=13,\n"
                + "  dispatchChangeNotificationsLatencyMillis=14,\n"
                + "  optimizeLatencyMillis=15,\n"
                + "  isPackageObserved=true,\n"
                + "  getOldSchemaLatencyMillis=16,\n"
                + "  getObserverLatencyMillis=17,\n"
                + "  preparingChangeNotificationLatencyMillis=18,\n"
                + "  schemaMigrationCallType=2,\n"
                + "  skippedIcingInteraction=false,\n"
                + "  enabledFeatures=11,\n"
                + "  javaLockAcquisitionLatencyMillis=9,\n"
                + "  lastBlockingOperation=19,\n"
                + "  lastBlockingOperationLatencyMillis=20,\n"
                + "  getVmLatencyMillis=21,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_SchemaMigrationStats() {
        int executorAcquisitionLatencyMillis = 1;
        int getSchemaLatency = 2;
        int queryAndTransformLatency = 3;
        int firstSetSchemaLatency = 4;
        boolean isFirstSetSchemaSuccess = true;
        int secondSetSchemaLatency = 5;
        int saveDocumentLatency = 6;
        int migratedDocumentCount = 7;
        int savedDocumentCount = 8;
        int migrationFailureCount = 9;
        SchemaMigrationStats sStats = new SchemaMigrationStats.Builder(
                TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setGetSchemaLatencyMillis(getSchemaLatency)
                .setQueryAndTransformLatencyMillis(queryAndTransformLatency)
                .setFirstSetSchemaLatencyMillis(firstSetSchemaLatency)
                .setIsFirstSetSchemaSuccess(isFirstSetSchemaSuccess)
                .setSecondSetSchemaLatencyMillis(secondSetSchemaLatency)
                .setSaveDocumentLatencyMillis(saveDocumentLatency)
                .setTotalNeedMigratedDocumentCount(migratedDocumentCount)
                .setTotalSuccessMigratedDocumentCount(savedDocumentCount)
                .setMigrationFailureCount(migrationFailureCount)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis())
                .isEqualTo(executorAcquisitionLatencyMillis);
        assertThat(sStats.getGetSchemaLatencyMillis()).isEqualTo(getSchemaLatency);
        assertThat(sStats.getQueryAndTransformLatencyMillis()).isEqualTo(queryAndTransformLatency);
        assertThat(sStats.getFirstSetSchemaLatencyMillis()).isEqualTo(firstSetSchemaLatency);
        assertThat(sStats.isFirstSetSchemaSuccess()).isEqualTo(isFirstSetSchemaSuccess);
        assertThat(sStats.getSecondSetSchemaLatencyMillis()).isEqualTo(secondSetSchemaLatency);
        assertThat(sStats.getSaveDocumentLatencyMillis()).isEqualTo(saveDocumentLatency);
        assertThat(sStats.getTotalNeedMigratedDocumentCount()).isEqualTo(migratedDocumentCount);
        assertThat(sStats.getTotalSuccessMigratedDocumentCount()).isEqualTo(savedDocumentCount);
        assertThat(sStats.getMigrationFailureCount()).isEqualTo(migrationFailureCount);
        String expectedString = "SchemaMigrationStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  executorAcquisitionLatencyMillis=1,\n"
                + "  totalLatencyMillis=20,\n"
                + "  getSchemaLatencyMillis=2,\n"
                + "  queryAndTransformLatencyMillis=3,\n"
                + "  firstSetSchemaLatencyMillis=4,\n"
                + "  isFirstSetSchemaSuccess=true,\n"
                + "  secondSetSchemaLatencyMillis=5,\n"
                + "  saveDocumentLatencyMillis=6,\n"
                + "  totalNeedMigratedDocumentCount=7,\n"
                + "  migrationFailureCount=9,\n"
                + "  totalSuccessMigratedDocumentCount=8,\n"
                + "  enabledFeatures=0\n"
                + "}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_RemoveStats() {
        int nativeLatencyMillis = 1;
        @RemoveStats.DeleteType int deleteType = 2;
        int documentDeletedCount = 3;
        int enabledFeatures = 1;
        int queryLength = 4;
        int numTerms = 5;
        int numNamespacesFiltered = 6;
        int numSchemaTypesFiltered = 7;
        int parseQueryLatencyMillis = 8;
        int documentRemovalLatencyMillis = 9;
        int javaLockAcquisitionLatencyMillis = 10;
        int lastBlockingOperation = 11;
        int lastBlockingOperationLatencyMillis = 12;
        int getVmLatencyMillis = 13;

        final RemoveStats rStats = new RemoveStats.Builder(TEST_PACKAGE_NAME,
                TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDeleteType(deleteType)
                .setDeletedDocumentCount(documentDeletedCount)
                .setLaunchVMEnabled(true)
                .setQueryLength(queryLength)
                .setNumTerms(numTerms)
                .setNumNamespacesFiltered(numNamespacesFiltered)
                .setNumSchemaTypesFiltered(numSchemaTypesFiltered)
                .setParseQueryLatencyMillis(parseQueryLatencyMillis)
                .setDocumentRemovalLatencyMillis(documentRemovalLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .build();


        assertThat(rStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(rStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(rStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(rStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(rStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(rStats.getDeleteType()).isEqualTo(deleteType);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(documentDeletedCount);
        assertThat(rStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(rStats.getQueryLength()).isEqualTo(queryLength);
        assertThat(rStats.getNumTerms()).isEqualTo(numTerms);
        assertThat(rStats.getNumNamespacesFiltered()).isEqualTo(numNamespacesFiltered);
        assertThat(rStats.getNumSchemaTypesFiltered()).isEqualTo(numSchemaTypesFiltered);
        assertThat(rStats.getParseQueryLatencyMillis()).isEqualTo(parseQueryLatencyMillis);
        assertThat(rStats.getDocumentRemovalLatencyMillis())
                .isEqualTo(documentRemovalLatencyMillis);
        assertThat(rStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(rStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(rStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(rStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "RemoveStats {\n"
                + "  packageName=com.google.test,\n"
                + "  database=testDataBase,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  nativeLatencyMillis=1,\n"
                + "  nativeDeleteType=2,\n"
                + "  nativeNumDocumentsDeleted=3,\n"
                + "  queryLength=4,\n"
                + "  numTerms=5,\n"
                + "  numNamespacesFiltered=6,\n"
                + "  numSchemaTypesFiltered=7,\n"
                + "  parseQueryLatencyMillis=8,\n"
                + "  documentRemovalLatencyMillis=9,\n"
                + "  enabledFeatures=1,\n"
                + "  javaLockAcquisitionLatencyMillis=10,\n"
                + "  lastBlockingOperation=11,\n"
                + "  lastBlockingOperationLatencyMillis=12,\n"
                + "  getVmLatencyMillis=13,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(rStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_OptimizeStats() {
        int nativeLatencyMillis = 1;
        int nativeDocumentStoreOptimizeLatencyMillis = 2;
        int nativeIndexRestorationLatencyMillis = 3;
        int nativeNumOriginalDocuments = 4;
        int nativeNumDeletedDocuments = 5;
        int nativeNumExpiredDocuments = 6;
        int enabledFeatures = 1;
        long nativeStorageSizeBeforeBytes = Integer.MAX_VALUE + 1;
        long nativeStorageSizeAfterBytes = Integer.MAX_VALUE + 2;
        long nativeTimeSinceLastOptimizeMillis = Integer.MAX_VALUE + 3;
        int indexRestorationMode = 1;
        int numOriginalNamespaces = 7;
        int numDeletedNamespaces = 8;
        int javaLockAcquisitionLatencyMillis = 9;
        int lastBlockingOperation = 10;
        int lastBlockingOperationLatencyMillis = 11;
        int getVmLatencyMillis = 12;

        final OptimizeStats oStats = new OptimizeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDocumentStoreOptimizeLatencyMillis(nativeDocumentStoreOptimizeLatencyMillis)
                .setIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setOriginalDocumentCount(nativeNumOriginalDocuments)
                .setDeletedDocumentCount(nativeNumDeletedDocuments)
                .setExpiredDocumentCount(nativeNumExpiredDocuments)
                .setStorageSizeBeforeBytes(nativeStorageSizeBeforeBytes)
                .setStorageSizeAfterBytes(nativeStorageSizeAfterBytes)
                .setTimeSinceLastOptimizeMillis(nativeTimeSinceLastOptimizeMillis)
                .setLaunchVMEnabled(true)
                .setIndexRestorationMode(indexRestorationMode)
                .setNumOriginalNamespaces(numOriginalNamespaces)
                .setNumDeletedNamespaces(numDeletedNamespaces)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .build();

        assertThat(oStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(oStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(oStats.getDocumentStoreOptimizeLatencyMillis()).isEqualTo(
                nativeDocumentStoreOptimizeLatencyMillis);
        assertThat(oStats.getIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(oStats.getOriginalDocumentCount()).isEqualTo(nativeNumOriginalDocuments);
        assertThat(oStats.getDeletedDocumentCount()).isEqualTo(nativeNumDeletedDocuments);
        assertThat(oStats.getExpiredDocumentCount()).isEqualTo(nativeNumExpiredDocuments);
        assertThat(oStats.getStorageSizeBeforeBytes()).isEqualTo(nativeStorageSizeBeforeBytes);
        assertThat(oStats.getStorageSizeAfterBytes()).isEqualTo(nativeStorageSizeAfterBytes);
        assertThat(oStats.getTimeSinceLastOptimizeMillis()).isEqualTo(
                nativeTimeSinceLastOptimizeMillis);
        assertThat(oStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(oStats.getIndexRestorationMode()).isEqualTo(indexRestorationMode);
        assertThat(oStats.getNumOriginalNamespaces()).isEqualTo(numOriginalNamespaces);
        assertThat(oStats.getNumDeletedNamespaces()).isEqualTo(numDeletedNamespaces);
        assertThat(oStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(oStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(oStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(oStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "OptimizeStats {\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  nativeLatencyMillis=1,\n"
                + "  nativeDocumentStoreOptimizeLatencyMillis=2,\n"
                + "  nativeIndexRestorationLatencyMillis=3,\n"
                + "  nativeOriginalDocumentCount=4,\n"
                + "  nativeDeletedDocumentCount=5,\n"
                + "  nativeExpiredDocumentCount=6,\n"
                + "  nativeStorageSizeBeforeBytes=-2147483648,\n"
                + "  nativeStorageSizeAfterBytes=-2147483647,\n"
                + "  nativeTimeSinceLastOptimizeMillis=-2147483646,\n"
                + "  indexRestorationMode=1,\n"
                + "  numOriginalNamespaces=7,\n"
                + "  numDeletedNamespaces=8,\n"
                + "  callReceivedTimestampMillis=0,\n"
                + "  executorAcquisitionLatencyMillis=0,\n"
                + "  onExecutorLatencyMillis=0,\n"
                + "  enabledFeatures=1,\n"
                + "  javaLockAcquisitionLatencyMillis=9,\n"
                + "  lastBlockingOperation=10,\n"
                + "  lastBlockingOperationLatencyMillis=11,\n"
                + "  getVmLatencyMillis=12,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(oStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_JavaLockLatencyCanBeSetOnce() {
        final OptimizeStats oStats = new OptimizeStats.Builder()
                .setJavaLockAcquisitionLatencyMillis(-10)
                .setJavaLockAcquisitionLatencyMillis(10)
                .setJavaLockAcquisitionLatencyMillis(20)
                .build();
        // Can only be set once for non-negative latency.
        assertThat(oStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(10);
    }

    @Test
    public void testAppSearchStats_PersistToDiskStats() {
        int triggerCallType = 1;
        PersistType.Code persistType = PersistType.Code.FULL;
        int nativeLatencyMillis = 3;
        int blobStorePersistLatencyMillis = 4;
        int documentStoreTotalPersistLatencyMillis = 5;
        int documentStoreComponentsPersistLatencyMillis = 6;
        int documentStoreChecksumUpdateLatencyMillis = 7;
        int documentLogChecksumUpdateLatencyMillis = 8;
        int documentLogDataSyncLatencyMillis = 9;
        int schemaStorePersistLatencyMillis = 10;
        int indexPersistLatencyMillis = 11;
        int integerIndexPersistLatencyMillis = 12;
        int qualifiedIdJoinIndexPersistLatencyMillis = 13;
        int embeddingIndexPersistLatencyMillis = 14;
        int javaLockAcquisitionLatencyMillis = 101;
        int lastBlockingOperation = 102;
        int lastBlockingOperationLatencyMillis = 103;
        int getVmLatencyMillis = 104;
        int enabledFeatures = 1;

        final PersistToDiskStats pStats = new PersistToDiskStats.Builder(
                TEST_PACKAGE_NAME, triggerCallType)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setPersistType(persistType)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setNativeBlobStorePersistLatencyMillis(blobStorePersistLatencyMillis)
                .setNativeDocumentStoreTotalPersistLatencyMillis(
                        documentStoreTotalPersistLatencyMillis)
                .setNativeDocumentStoreComponentsPersistLatencyMillis(
                        documentStoreComponentsPersistLatencyMillis)
                .setNativeDocumentStoreChecksumUpdateLatencyMillis(
                        documentStoreChecksumUpdateLatencyMillis)
                .setNativeDocumentLogChecksumUpdateLatencyMillis(
                        documentLogChecksumUpdateLatencyMillis)
                .setNativeDocumentLogDataSyncLatencyMillis(documentLogDataSyncLatencyMillis)
                .setNativeSchemaStorePersistLatencyMillis(schemaStorePersistLatencyMillis)
                .setNativeIndexPersistLatencyMillis(indexPersistLatencyMillis)
                .setNativeIntegerIndexPersistLatencyMillis(integerIndexPersistLatencyMillis)
                .setNativeQualifiedIdJoinIndexPersistLatencyMillis(
                        qualifiedIdJoinIndexPersistLatencyMillis)
                .setNativeEmbeddingIndexPersistLatencyMillis(embeddingIndexPersistLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(pStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(pStats.getTriggerCallType()).isEqualTo(triggerCallType);
        assertThat(pStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(pStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getBlobStorePersistLatencyMillis())
                .isEqualTo(blobStorePersistLatencyMillis);
        assertThat(pStats.getDocumentStoreTotalPersistLatencyMillis())
                .isEqualTo(documentStoreTotalPersistLatencyMillis);
        assertThat(pStats.getDocumentStoreComponentsPersistLatencyMillis())
                .isEqualTo(documentStoreComponentsPersistLatencyMillis);
        assertThat(pStats.getDocumentStoreChecksumUpdateLatencyMillis())
                .isEqualTo(documentStoreChecksumUpdateLatencyMillis);
        assertThat(pStats.getDocumentLogChecksumUpdateLatencyMillis())
                .isEqualTo(documentLogChecksumUpdateLatencyMillis);
        assertThat(pStats.getDocumentLogDataSyncLatencyMillis())
                .isEqualTo(documentLogDataSyncLatencyMillis);
        assertThat(pStats.getSchemaStorePersistLatencyMillis())
                .isEqualTo(schemaStorePersistLatencyMillis);
        assertThat(pStats.getIndexPersistLatencyMillis()).isEqualTo(indexPersistLatencyMillis);
        assertThat(pStats.getIntegerIndexPersistLatencyMillis())
                .isEqualTo(integerIndexPersistLatencyMillis);
        assertThat(pStats.getQualifiedIdJoinIndexPersistLatencyMillis())
                .isEqualTo(qualifiedIdJoinIndexPersistLatencyMillis);
        assertThat(pStats.getEmbeddingIndexPersistLatencyMillis())
                .isEqualTo(embeddingIndexPersistLatencyMillis);

        assertThat(pStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(pStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(pStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(pStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
        String expectedString = "PersistToDiskStats {\n"
                + "  packageName=com.google.test,\n"
                + "  triggerCallType=1,\n"
                + "  statusCode=2,\n"
                + "  totalLatencyMillis=20,\n"
                + "  persistType=FULL,\n"
                + "  nativeLatencyMillis=3,\n"
                + "  blobStorePersistLatencyMillis=4,\n"
                + "  documentStoreTotalPersistLatencyMillis=5,\n"
                + "  documentStoreComponentsPersistLatencyMillis=6,\n"
                + "  documentStoreChecksumUpdateLatencyMillis=7,\n"
                + "  documentLogChecksumUpdateLatencyMillis=8,\n"
                + "  documentLogDataSyncLatencyMillis=9,\n"
                + "  schemaStorePersistLatencyMillis=10,\n"
                + "  indexPersistLatencyMillis=11,\n"
                + "  integerIndexPersistLatencyMillis=12,\n"
                + "  qualifiedIdJoinIndexPersistLatencyMillis=13,\n"
                + "  embeddingIndexPersistLatencyMillis=14,\n"
                + "  enabledFeatures=1,\n"
                + "  javaLockAcquisitionLatencyMillis=101,\n"
                + "  lastBlockingOperation=102,\n"
                + "  lastBlockingOperationLatencyMillis=103,\n"
                + "  getVmLatencyMillis=104,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(pStats.toString()).isEqualTo(expectedString);
    }
}
