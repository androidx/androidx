/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Defines JUnit Platform report entry keys for backup and restore test executions.
 *
 * [BackupRestoreExtension] and [BackupRestoreControllerImpl] publish these entries:
 * `BackupRestore.*` keys carry execution telemetry, while [DETAIL_PATH] links each test to its
 * structured [TestDetailRecord] JSON file.
 */
internal object BackupReportKeys {
    /** Backup transport the execution ran against, as a [BackupTransportMode] name. */
    const val TRANSPORT_MODE = "BackupRestore.transportMode"

    /** Number of storage domains covered by the execution. */
    const val STORAGE_DOMAIN_COUNT = "BackupRestore.storageDomainCount"

    /** Time spent seeding storage domains with test data, in milliseconds. */
    const val SEEDING_DURATION = "BackupRestore.seedingDurationMillis"

    /** Time spent performing the backup, in milliseconds. */
    const val BACKUP_DURATION = "BackupRestore.backupDurationMillis"

    /** Time spent clearing app data between backup and restore, in milliseconds. */
    const val CLEAR_DATA_DURATION = "BackupRestore.clearDataDurationMillis"

    /** Time spent performing the restore, in milliseconds. */
    const val RESTORE_DURATION = "BackupRestore.restoreDurationMillis"

    /** Time spent verifying restored data, in milliseconds. */
    const val VERIFICATION_DURATION = "BackupRestore.verificationDurationMillis"

    /** Total wall-clock time of the execution, in milliseconds. */
    const val TOTAL_DURATION = "BackupRestore.totalDurationMillis"

    /** Overall execution status, either `SUCCESS` or `FAILURE`. */
    const val STATUS = "BackupRestore.status"

    /** Root cause of a failure, as a [BackupErrorCode] name. */
    const val ERROR_CODE = "BackupRestore.errorCode"

    /** Stage at which the execution failed, as a [BackupExecutionStage] name. */
    const val FAILURE_STAGE = "BackupRestore.failureStage"

    /** Human-readable description of the failure. */
    const val ERROR_MESSAGE = "BackupRestore.errorMessage"

    /**
     * Relative path from the suite results directory to the test's [TestDetailRecord] JSON file.
     */
    const val DETAIL_PATH = "androidx.report.detailPath"

    /**
     * Value reported for [STATUS] when the execution completed without failures.
     *
     * This is the status of a whole backup and restore execution and is distinct from the
     * per-action [BackupActionValues.STATUS_SUCCESS], which the device reports in lower case.
     */
    const val REPORT_STATUS_SUCCESS = "SUCCESS"

    /**
     * Value reported for [STATUS] when the execution failed.
     *
     * @see REPORT_STATUS_SUCCESS
     */
    const val REPORT_STATUS_FAILURE = "FAILURE"
}
