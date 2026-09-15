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

import java.time.Duration

/**
 * Categorized error codes capturing the root cause of an automated backup or restore test failure.
 */
internal enum class BackupErrorCode {
    /** No failure occurred; the execution completed successfully. */
    NONE,

    /** No online Android devices or emulators were detected via ADB. */
    NO_ONLINE_DEVICE,

    /** No online devices matched the specified criteria (e.g., requested serial or API level). */
    NO_MATCHING_DEVICE,

    /** The connected device API level is below the minimum required (API 31 / Android 12). */
    UNSUPPORTED_API_LEVEL,

    /** Google Play Services (GmsCore) is missing or below the required version. */
    GMSCORE_OUTDATED_OR_MISSING,

    /** Backup Manager (BMGR) failed to initialize, enable, or activate the requested transport. */
    BMGR_INIT_FAILED,

    /** The device keyguard/lockscreen could not be dismissed. */
    KEYGUARD_UNLOCK_FAILED,

    /** Device restore polling timed out before the restore operation completed. */
    RESTORE_POLL_TIMEOUT,

    /** On-device seeding via PopulateStorageAction failed. */
    SEEDING_FAILED,

    /** Device backup archiving or transfer failed. */
    BACKUP_FAILED,

    /** Clearing application sandbox data to simulate a clean restore environment failed. */
    CLEAR_DATA_FAILED,

    /** Device restore extraction or package re-initialization failed. */
    RESTORE_FAILED,

    /** On-device post-restore data verification via AssertStorageAction failed. */
    VERIFICATION_FAILED,

    /** An unclassified or unexpected exception occurred during test orchestration. */
    UNKNOWN_ERROR,
}

/** Execution stage during which a failure or milestone occurred. */
internal enum class BackupExecutionStage {
    /** Device discovery, API validation, and precondition checks. */
    PRECONDITION,

    /** Seeding initial test data into the application sandbox. */
    SEEDING,

    /** Force-stopping the app and generating the backup payload via BMGR. */
    BACKUP,

    /** Clearing application sandbox data to simulate a clean restore environment. */
    CLEAR_DATA,

    /** Restoring package data from the backup archive. */
    RESTORE,

    /** Asserting restored data integrity against expected values. */
    VERIFICATION,
}

/**
 * Encapsulates performance metrics, durations, and diagnostic telemetry for a completed or failed
 * backup and restore flow.
 */
internal data class BackupExecutionSummary(
    val transportMode: BackupTransportMode,
    val storageDomainCount: Int,
    val seedingDuration: Duration = Duration.ZERO,
    val backupDuration: Duration = Duration.ZERO,
    val clearDataDuration: Duration = Duration.ZERO,
    val restoreDuration: Duration = Duration.ZERO,
    val verificationDuration: Duration = Duration.ZERO,
    val totalDuration: Duration = Duration.ZERO,
    val isSuccess: Boolean,
    val errorCode: BackupErrorCode = BackupErrorCode.NONE,
    val failureStage: BackupExecutionStage? = null,
    val errorMessage: String? = null,
)
