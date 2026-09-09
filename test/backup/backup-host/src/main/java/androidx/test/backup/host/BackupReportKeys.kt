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
 * Keys used to publish structured backup and restore telemetry as JUnit Platform report entries.
 *
 * Report entries are emitted from two places: [BackupRestoreExtension], which reports precondition
 * failures encountered before a controller exists, and [BackupRestoreControllerImpl], which reports
 * the outcome of a completed execution. Centralizing the keys here keeps both producers consistent
 * so that downstream consumers can rely on a single, stable vocabulary.
 */
internal object BackupReportKeys {
    /** Version of the backup testing library that produced the report entries. */
    const val LIBRARY_VERSION = "BackupRestore.libraryVersion"

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

    /** Reported for [LIBRARY_VERSION] when the packaged version resource cannot be read. */
    const val UNKNOWN_LIBRARY_VERSION = "unknown"

    /** Value reported for [STATUS] when the execution completed without failures. */
    const val STATUS_SUCCESS = "SUCCESS"

    /** Value reported for [STATUS] when the execution failed. */
    const val STATUS_FAILURE = "FAILURE"
}
