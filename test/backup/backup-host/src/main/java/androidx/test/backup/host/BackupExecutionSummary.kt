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
import java.util.EnumMap
import java.util.Locale

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
    VERIFICATION;

    /** Classifies [e], thrown during this stage, by the root cause its message points to. */
    fun errorCodeFor(e: Exception): BackupErrorCode {
        val msg = e.message?.lowercase(Locale.ROOT) ?: ""
        val isGmsCore = msg.contains("gmscore") || msg.contains("play store")
        val isBmgr = msg.contains("bmgr") || msg.contains("transport")
        return when (this) {
            PRECONDITION ->
                if (msg.contains("keyguard")) {
                    BackupErrorCode.KEYGUARD_UNLOCK_FAILED
                } else {
                    BackupErrorCode.UNKNOWN_ERROR
                }
            SEEDING -> BackupErrorCode.SEEDING_FAILED
            BACKUP ->
                when {
                    isGmsCore -> BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING
                    isBmgr -> BackupErrorCode.BMGR_INIT_FAILED
                    else -> BackupErrorCode.BACKUP_FAILED
                }
            CLEAR_DATA -> BackupErrorCode.CLEAR_DATA_FAILED
            RESTORE ->
                when {
                    msg.contains("timeout") ||
                        msg.contains("timed out") ||
                        msg.contains("polling") -> BackupErrorCode.RESTORE_POLL_TIMEOUT
                    isGmsCore -> BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING
                    isBmgr -> BackupErrorCode.BMGR_INIT_FAILED
                    else -> BackupErrorCode.RESTORE_FAILED
                }
            VERIFICATION -> BackupErrorCode.VERIFICATION_FAILED
        }
    }
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
) {

    /** Reports this summary, keyed by [BackupReportKeys], to [publish]. */
    fun publishTo(publish: (key: String, value: String) -> Unit) {
        publish(BackupReportKeys.LIBRARY_VERSION, LIBRARY_VERSION)
        publish(BackupReportKeys.TRANSPORT_MODE, transportMode.toString())
        publish(BackupReportKeys.STORAGE_DOMAIN_COUNT, storageDomainCount.toString())
        publish(BackupReportKeys.SEEDING_DURATION, seedingDuration.toMillis().toString())
        publish(BackupReportKeys.BACKUP_DURATION, backupDuration.toMillis().toString())
        publish(BackupReportKeys.CLEAR_DATA_DURATION, clearDataDuration.toMillis().toString())
        publish(BackupReportKeys.RESTORE_DURATION, restoreDuration.toMillis().toString())
        publish(BackupReportKeys.VERIFICATION_DURATION, verificationDuration.toMillis().toString())
        publish(BackupReportKeys.TOTAL_DURATION, totalDuration.toMillis().toString())
        publish(
            BackupReportKeys.STATUS,
            if (isSuccess) BackupReportKeys.REPORT_STATUS_SUCCESS
            else BackupReportKeys.REPORT_STATUS_FAILURE,
        )
        if (!isSuccess) {
            publish(BackupReportKeys.ERROR_CODE, errorCode.name)
            failureStage?.let { publish(BackupReportKeys.FAILURE_STAGE, it.name) }
            errorMessage?.let { publish(BackupReportKeys.ERROR_MESSAGE, it) }
        }
    }
}

/**
 * Times the stages of one backup and restore flow and produces its [BackupExecutionSummary].
 *
 * @param nanoTime monotonic clock, in nanoseconds
 */
internal class BackupExecutionTracker(
    private val transportMode: BackupTransportMode,
    private val storageDomainCount: Int,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val startNanos = nanoTime()
    private val durations =
        EnumMap<BackupExecutionStage, Duration>(BackupExecutionStage::class.java)
    private var currentStage = BackupExecutionStage.PRECONDITION

    /** Runs [block] as [stage], recording how long it took whether or not it completes. */
    suspend fun <T> stage(stage: BackupExecutionStage, block: suspend () -> T): T {
        currentStage = stage
        val stageStartNanos = nanoTime()
        try {
            return block()
        } finally {
            durations[stage] = elapsedSince(stageStartNanos)
        }
    }

    /** Returns the summary of a flow that completed every stage. */
    fun succeeded(): BackupExecutionSummary = summary(isSuccess = true)

    /** Returns the summary of a flow that failed with [e] during the latest stage. */
    fun failed(e: Exception): BackupExecutionSummary =
        summary(
            isSuccess = false,
            errorCode = currentStage.errorCodeFor(e),
            failureStage = currentStage,
            errorMessage = e.message,
        )

    private fun summary(
        isSuccess: Boolean,
        errorCode: BackupErrorCode = BackupErrorCode.NONE,
        failureStage: BackupExecutionStage? = null,
        errorMessage: String? = null,
    ) =
        BackupExecutionSummary(
            transportMode = transportMode,
            storageDomainCount = storageDomainCount,
            seedingDuration = durationOf(BackupExecutionStage.SEEDING),
            backupDuration = durationOf(BackupExecutionStage.BACKUP),
            clearDataDuration = durationOf(BackupExecutionStage.CLEAR_DATA),
            restoreDuration = durationOf(BackupExecutionStage.RESTORE),
            verificationDuration = durationOf(BackupExecutionStage.VERIFICATION),
            totalDuration = elapsedSince(startNanos),
            isSuccess = isSuccess,
            errorCode = errorCode,
            failureStage = failureStage,
            errorMessage = errorMessage,
        )

    private fun durationOf(stage: BackupExecutionStage): Duration =
        durations[stage] ?: Duration.ZERO

    private fun elapsedSince(startNanos: Long): Duration = Duration.ofNanos(nanoTime() - startNanos)
}

/**
 * Version of this library, read from the version resource packaged into the artifact.
 *
 * Falls back to [BackupReportKeys.UNKNOWN_LIBRARY_VERSION] when the resource is absent, for example
 * when running against locally built classes rather than a published artifact. Reporting a
 * placeholder here keeps the telemetry honest instead of attributing runs to a release that may not
 * be the one under test.
 */
private val LIBRARY_VERSION: String by lazy {
    try {
        readVersionResource("/META-INF/androidx.test.backup_backup-host.version")
            ?: readVersionResource("/META-INF/androidx.test.backup_backup.version")
            ?: BackupReportKeys.UNKNOWN_LIBRARY_VERSION
    } catch (_: Throwable) {
        BackupReportKeys.UNKNOWN_LIBRARY_VERSION
    }
}

private fun readVersionResource(resourcePath: String): String? =
    BackupExecutionSummary::class
        .java
        .getResourceAsStream(resourcePath)
        ?.bufferedReader()
        ?.use { it.readLine()?.trim() }
        ?.takeIf { it.isNotEmpty() }
