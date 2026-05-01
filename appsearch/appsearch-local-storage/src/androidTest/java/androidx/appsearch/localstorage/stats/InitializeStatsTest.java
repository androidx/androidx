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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;

import com.google.android.icing.proto.IcingApiCallType;
import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.PersistType;
import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.List;

public class InitializeStatsTest {
    static final int TEST_STATUS_CODE = AppSearchResult.RESULT_INTERNAL_ERROR;
    static final int TEST_TOTAL_LATENCY_MILLIS = 20;

    @Test
    public void testBuilder() {
        // InitializeStats Java fields.
        int prepareSchemaAndNamespacesLatencyMillis = 1;
        int prepareVisibilityFileLatencyMillis = 2;
        // InitializeStats native fields.
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
        int nativeNumPreviousInitFailures = 10;
        int nativeIntegerIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int nativeQualifiedIdJoinIndexRestorationCause =
                InitializeStats.RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH;
        int nativeEmbeddingIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int nativeInitializeIcuDataStatusCode = 11;
        int nativeNumFailedReindexedDocuments = 12;
        InitializeStatsProto.FailureStage.Code nativeFailureStageCode =
                InitializeStatsProto.FailureStage.Code.BASE_DIRECTORY_CREATION;
        int nativeIcuSegmenterCreationStatusCode = AppSearchResult.RESULT_ABORTED;
        int nativeIcuNormalizerCreationStatusCode = AppSearchResult.RESULT_UNAVAILABLE;
        PersistType.Code nativeLastPersistType = PersistType.Code.RECOVERY_PROOF;
        List<IcingApiCallType.Code> nativeAfterLastPersistFullCallTypes =
                ImmutableList.of(IcingApiCallType.Code.DELETE, IcingApiCallType.Code.PUT);
        List<IcingApiCallType.Code> nativeAfterLastPersistRecoveryProofCallTypes =
                ImmutableList.of(IcingApiCallType.Code.BATCH_PUT);
        List<IcingApiCallType.Code> nativeAfterLastPersistLiteCallTypes =
                ImmutableList.of(
                        IcingApiCallType.Code.INITIALIZE,
                        IcingApiCallType.Code.OPTIMIZE,
                        IcingApiCallType.Code.REPORT_USAGE);
        long nativeSchemaProtoByteSize = 13;
        // BaseStats fields.
        int enabledFeatures = 0b0011;
        final int javaLockAcquisitionLatencyMillis = 1001;
        final int lastBlockingOperation = 1002;
        final int lastBlockingOperationLatencyMillis = 1003;
        int getVmLatencyMillis = 1004;

        final InitializeStats.Builder iStatsBuilder = new InitializeStats.Builder()
                // InitializeStats Java fields.
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setHasDeSync(/* hasDeSyncs= */ true)
                .setPrepareSchemaAndNamespacesLatencyMillis(prepareSchemaAndNamespacesLatencyMillis)
                .setPrepareVisibilityStoreLatencyMillis(prepareVisibilityFileLatencyMillis)
                .setHasReset(true)
                .setResetStatusCode(AppSearchResult.RESULT_INVALID_SCHEMA)
                // InitializeStats native fields.
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
                .setNativeNumPreviousInitFailures(nativeNumPreviousInitFailures)
                .setNativeIntegerIndexRestorationCause(nativeIntegerIndexRestorationCause)
                .setNativeQualifiedIdJoinIndexRestorationCause(
                        nativeQualifiedIdJoinIndexRestorationCause)
                .setNativeEmbeddingIndexRestorationCause(nativeEmbeddingIndexRestorationCause)
                .setNativeInitializeIcuDataStatusCode(nativeInitializeIcuDataStatusCode)
                .setNativeNumFailedReindexedDocuments(nativeNumFailedReindexedDocuments)
                .setNativeFailureStageCode(nativeFailureStageCode)
                .setNativeIcuSegmenterCreationStatusCode(nativeIcuSegmenterCreationStatusCode)
                .setNativeIcuNormalizerCreationStatusCode(nativeIcuNormalizerCreationStatusCode)
                .setNativeLastPersistType(nativeLastPersistType)
                .addNativeAfterLastPersistFullCallTypes(nativeAfterLastPersistFullCallTypes)
                .addNativeAfterLastPersistRecoveryProofCallTypes(
                        nativeAfterLastPersistRecoveryProofCallTypes)
                .addNativeAfterLastPersistLiteCallTypes(nativeAfterLastPersistLiteCallTypes)
                .setNativeSchemaProtoByteSize(nativeSchemaProtoByteSize)
                // BaseStats fields.
                .setLaunchVmEnabled(true)
                .setLaunchAiSealEnabled(true)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis);
        final InitializeStats iStats = iStatsBuilder.build();

        // InitializeStats Java fields.
        assertThat(iStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(iStats.hasDeSync()).isTrue();
        assertThat(iStats.getPrepareSchemaAndNamespacesLatencyMillis()).isEqualTo(
                prepareSchemaAndNamespacesLatencyMillis);
        assertThat(iStats.getPrepareVisibilityStoreLatencyMillis()).isEqualTo(
                prepareVisibilityFileLatencyMillis);
        assertThat(iStats.hasReset()).isTrue();
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_INVALID_SCHEMA);
        // InitializeStats native fields.
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
        assertThat(iStats.getNativeNumPreviousInitFailures())
                .isEqualTo(nativeNumPreviousInitFailures);
        assertThat(iStats.getNativeIntegerIndexRestorationCause())
                .isEqualTo(nativeIntegerIndexRestorationCause);
        assertThat(iStats.getNativeQualifiedIdJoinIndexRestorationCause())
                .isEqualTo(nativeQualifiedIdJoinIndexRestorationCause);
        assertThat(iStats.getNativeEmbeddingIndexRestorationCause())
                .isEqualTo(nativeEmbeddingIndexRestorationCause);
        assertThat(iStats.getNativeInitializeIcuDataStatusCode())
                .isEqualTo(nativeInitializeIcuDataStatusCode);
        assertThat(iStats.getNativeNumFailedReindexedDocuments())
                .isEqualTo(nativeNumFailedReindexedDocuments);
        assertThat(iStats.getNativeFailureStageCode()).isEqualTo(nativeFailureStageCode);
        assertThat(iStats.getNativeIcuSegmenterCreationStatusCode())
                .isEqualTo(nativeIcuSegmenterCreationStatusCode);
        assertThat(iStats.getNativeIcuNormalizerCreationStatusCode())
                .isEqualTo(nativeIcuNormalizerCreationStatusCode);
        assertThat(iStats.getNativeLastPersistType()).isEqualTo(nativeLastPersistType);
        assertThat(iStats.getNativeAfterLastPersistFullCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistFullCallTypes);
        assertThat(iStats.getNativeAfterLastPersistRecoveryProofCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistRecoveryProofCallTypes);
        assertThat(iStats.getNativeAfterLastPersistLiteCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistLiteCallTypes);
        assertThat(iStats.getNativeSchemaProtoByteSize()).isEqualTo(nativeSchemaProtoByteSize);
        // BaseStats fields.
        assertThat(iStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
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
                + "  nativeFailureStage=BASE_DIRECTORY_CREATION,\n"
                + "  nativeIcuSegmenterCreationStatusCode=13,\n"
                + "  nativeIcuNormalizerCreationStatusCode=14,\n"
                + "  nativeLastPersistType=RECOVERY_PROOF,\n"
                + "  nativeSchemaProtoByteSize=13,\n"
                + "  hasReset=true,\n"
                + "  resetStatusCode=7,\n"
                + "  nativeAfterLastPersistFullCallTypes=[\n"
                + "    DELETE,\n"
                + "    PUT,\n"
                + "  ],\n"
                + "  nativeAfterLastPersistRecoveryProofCallTypes=[\n"
                + "    BATCH_PUT,\n"
                + "  ],\n"
                + "  nativeAfterLastPersistLiteCallTypes=[\n"
                + "    INITIALIZE,\n"
                + "    OPTIMIZE,\n"
                + "    REPORT_USAGE,\n"
                + "  ],\n"
                + "  enabledFeatures=11,\n"  // bytes
                + "  javaLockAcquisitionLatencyMillis=1001,\n"
                + "  lastBlockingOperation=1002,\n"
                + "  lastBlockingOperationLatencyMillis=1003,\n"
                + "  getVmLatencyMillis=1004,\n"
                + "  unblockedAppSearchLatencyMillis=0,\n"
                + "  numIcingCalls=1\n"
                + "}";
        assertThat(iStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testBuilder_defaultValues() {
        final InitializeStats iStats = new InitializeStats.Builder().build();

        // InitializeStats Java fields.
        assertThat(iStats.getStatusCode()).isEqualTo(0);
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(0);
        assertThat(iStats.hasDeSync()).isFalse();
        assertThat(iStats.getPrepareSchemaAndNamespacesLatencyMillis()).isEqualTo(0);
        assertThat(iStats.getPrepareVisibilityStoreLatencyMillis()).isEqualTo(0);
        assertThat(iStats.hasReset()).isFalse();
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // InitializeStats native fields.
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(0);
        assertThat(iStats.getNativeDocumentStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeSchemaStoreRecoveryCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeDocumentStoreRecoveryLatencyMillis()).isEqualTo(0);
        assertThat(iStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(0);
        assertThat(iStats.getNativeSchemaStoreRecoveryLatencyMillis()).isEqualTo(0);
        assertThat(iStats.getNativeDocumentStoreDataStatus())
                .isEqualTo(InitializeStats.DOCUMENT_STORE_DATA_STATUS_NO_DATA_LOSS);
        assertThat(iStats.getNativeDocumentCount()).isEqualTo(0);
        assertThat(iStats.getNativeSchemaTypeCount()).isEqualTo(0);
        assertThat(iStats.getNativeNumPreviousInitFailures()).isEqualTo(0);
        assertThat(iStats.getNativeIntegerIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeQualifiedIdJoinIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeEmbeddingIndexRestorationCause())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeInitializeIcuDataStatusCode())
                .isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(iStats.getNativeNumFailedReindexedDocuments())
                .isEqualTo(InitializeStats.RECOVERY_CAUSE_NONE);
        assertThat(iStats.getNativeFailureStageCode())
                .isEqualTo(InitializeStatsProto.FailureStage.Code.NONE);
        assertThat(iStats.getNativeIcuSegmenterCreationStatusCode())
                .isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(iStats.getNativeIcuNormalizerCreationStatusCode())
                .isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(iStats.getNativeLastPersistType()).isEqualTo(PersistType.Code.UNKNOWN);
        assertThat(iStats.getNativeAfterLastPersistFullCallTypes()).isEmpty();
        assertThat(iStats.getNativeAfterLastPersistRecoveryProofCallTypes()).isEmpty();
        assertThat(iStats.getNativeAfterLastPersistLiteCallTypes()).isEmpty();
        assertThat(iStats.getNativeSchemaProtoByteSize()).isEqualTo(0);
        // BaseStats fields.
        assertThat(iStats.getEnabledFeatures()).isEqualTo(0);
        assertThat(iStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(-1);
        assertThat(iStats.getLastBlockingOperation()).isEqualTo(BaseStats.CALL_TYPE_UNKNOWN);
        assertThat(iStats.getLastBlockingOperationLatencyMillis()).isEqualTo(-1);
        assertThat(iStats.getGetVmLatencyMillis()).isEqualTo(0);
    }

    @Test
    public void testBuilder_builderReuse() {
        List<IcingApiCallType.Code> nativeAfterLastPersistFullCallTypes =
                ImmutableList.of(IcingApiCallType.Code.DELETE, IcingApiCallType.Code.PUT);
        List<IcingApiCallType.Code> nativeAfterLastPersistRecoveryProofCallTypes =
                ImmutableList.of(IcingApiCallType.Code.BATCH_PUT);
        List<IcingApiCallType.Code> nativeAfterLastPersistLiteCallTypes =
                ImmutableList.of(
                        IcingApiCallType.Code.INITIALIZE,
                        IcingApiCallType.Code.OPTIMIZE,
                        IcingApiCallType.Code.REPORT_USAGE);

        final InitializeStats.Builder iStatsBuilder = new InitializeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .addNativeAfterLastPersistFullCallTypes(nativeAfterLastPersistFullCallTypes)
                .addNativeAfterLastPersistRecoveryProofCallTypes(
                        nativeAfterLastPersistRecoveryProofCallTypes)
                .addNativeAfterLastPersistLiteCallTypes(nativeAfterLastPersistLiteCallTypes);
        final InitializeStats iStats0 = iStatsBuilder.build();

        iStatsBuilder
                .setStatusCode(AppSearchResult.RESULT_INVALID_ARGUMENT)
                .addNativeAfterLastPersistFullCallTypes(
                        ImmutableList.of(IcingApiCallType.Code.OPTIMIZE))
                .addNativeAfterLastPersistRecoveryProofCallTypes(
                        ImmutableList.of(IcingApiCallType.Code.REPORT_USAGE))
                .addNativeAfterLastPersistLiteCallTypes(
                        ImmutableList.of(IcingApiCallType.Code.DELETE_BY_SCHEMA_TYPE));
        final InitializeStats iStats1 = iStatsBuilder.build();

        // Check that iStats0 wasn't altered.
        assertThat(iStats0.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(iStats0.getNativeAfterLastPersistFullCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistFullCallTypes);
        assertThat(iStats0.getNativeAfterLastPersistRecoveryProofCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistRecoveryProofCallTypes);
        assertThat(iStats0.getNativeAfterLastPersistLiteCallTypes())
                .containsExactlyElementsIn(nativeAfterLastPersistLiteCallTypes);

        // Check that iStats1 has the new values.
        assertThat(iStats1.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INVALID_ARGUMENT);
        assertThat(iStats1.getNativeAfterLastPersistFullCallTypes())
                .containsExactly(
                        IcingApiCallType.Code.DELETE,
                        IcingApiCallType.Code.PUT,
                        IcingApiCallType.Code.OPTIMIZE);
        assertThat(iStats1.getNativeAfterLastPersistRecoveryProofCallTypes())
                .containsExactly(
                        IcingApiCallType.Code.BATCH_PUT,
                        IcingApiCallType.Code.REPORT_USAGE);
        assertThat(iStats1.getNativeAfterLastPersistLiteCallTypes())
                .containsExactly(
                        IcingApiCallType.Code.INITIALIZE,
                        IcingApiCallType.Code.OPTIMIZE,
                        IcingApiCallType.Code.REPORT_USAGE,
                        IcingApiCallType.Code.DELETE_BY_SCHEMA_TYPE);
    }
}
