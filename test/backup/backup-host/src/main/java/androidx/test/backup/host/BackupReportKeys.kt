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

import java.io.File
import java.util.Properties

/**
 * Defines JUnit Platform report entry keys and engine input keys for backup and restore tests.
 *
 * [BackupRestoreExtension], [BackupRestoreControllerImpl], and [TestExecutionCapture] publish
 * `BackupRestore.*` telemetry entries alongside `androidx.report.*` detail entries.
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

    /** Prefix shared by all structured test report entries. */
    const val REPORT_PREFIX = "androidx.report."

    /** Schema version of the structured `androidx.report.*` entries. */
    const val SCHEMA_VERSION = "${REPORT_PREFIX}schemaVersion"

    /** Identifier of the test suite type that produced the entries. */
    const val SUITE_TYPE = "${REPORT_PREFIX}suiteType"

    /** Source file and optional 1-based line number (`File.kt:47`) of the test. */
    const val SOURCE = "${REPORT_PREFIX}source"

    /** Prefix for per-test summary attribute entries (`androidx.report.attr.<key>`). */
    const val ATTR_PREFIX = "${REPORT_PREFIX}attr."

    /** Prefix for device or participant entries (`androidx.report.actor.<id>.<field>`). */
    const val ACTOR_PREFIX = "${REPORT_PREFIX}actor."

    /** Prefix for ordered execution step entries (`androidx.report.step.<id>.<field>`). */
    const val STEP_PREFIX = "${REPORT_PREFIX}step."

    /** Prefix for failure callout entries (`androidx.report.callout.<field>`). */
    const val CALLOUT_PREFIX = "${REPORT_PREFIX}callout."

    /** Prefix for non-file artifact summary entries (`androidx.report.artifact.<n>.<field>`). */
    const val ARTIFACT_PREFIX = "${REPORT_PREFIX}artifact."

    /** Prefix for metadata on attached files (`androidx.report.file.<fileName>.<field>`). */
    const val FILE_PREFIX = "${REPORT_PREFIX}file."

    /** Engine input property holding the tested app's `targetSdkVersion`. */
    const val INPUT_TARGET_SDK_VERSION = "com.android.agp.test.TARGET_SDK_VERSION"

    /** Engine input property holding the tested app's `android:allowBackup` flag. */
    const val INPUT_ALLOW_BACKUP = "com.android.agp.test.ALLOW_BACKUP"

    /** Engine input property holding the `android:fullBackupContent` manifest attribute value. */
    const val INPUT_FULL_BACKUP_CONTENT = "com.android.agp.test.FULL_BACKUP_CONTENT"

    /** Engine input property holding the resolved `fullBackupContent` XML file name. */
    const val INPUT_FULL_BACKUP_CONTENT_FILE = "com.android.agp.test.FULL_BACKUP_CONTENT_FILE"

    /** Engine input property holding the resolved `fullBackupContent` XML content. */
    const val INPUT_FULL_BACKUP_CONTENT_XML = "com.android.agp.test.FULL_BACKUP_CONTENT_XML"

    /** Engine input property holding the `android:dataExtractionRules` manifest attribute value. */
    const val INPUT_DATA_EXTRACTION_RULES = "com.android.agp.test.DATA_EXTRACTION_RULES"

    /** Engine input property holding the resolved `dataExtractionRules` XML file name. */
    const val INPUT_DATA_EXTRACTION_RULES_FILE = "com.android.agp.test.DATA_EXTRACTION_RULES_FILE"

    /** Engine input property holding the resolved `dataExtractionRules` XML content. */
    const val INPUT_DATA_EXTRACTION_RULES_XML = "com.android.agp.test.DATA_EXTRACTION_RULES_XML"

    /** Environment variable holding the path of AGP's test-suite input properties file. */
    const val SUITE_PROPERTIES_ENV = "com.android.junit.engine.input.parameters"

    /** Property that turns off saving the device logs of failed tests when set to `false`. */
    const val PROP_DEVICE_LOGS_ENABLED = "androidx.test.backup.device.logs.enabled"

    /** Property that enables saving device logcat even on passing tests when set to `true`. */
    const val PROP_CAPTURE_LOGCAT_ON_PASS = "androidx.test.backup.report.captureLogcatOnPass"

    /** Property that enables copying raw `.backup` archives into test attachments. */
    const val PROP_INCLUDE_RAW_ARCHIVES = "androidx.test.backup.report.includeRawArchives"

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

    /**
     * Returns the non-empty value of [key] from [suiteProperties], falling back to JVM system
     * properties.
     */
    fun readProperty(suiteProperties: Properties?, key: String): String? =
        suiteProperties?.getProperty(key)?.takeIf { it.isNotEmpty() }
            ?: System.getProperty(key)?.takeIf { it.isNotEmpty() }

    /**
     * Loads AGP's test-suite input [Properties] from [path], returning empty [Properties] if absent
     * or unreadable.
     */
    fun loadSuiteProperties(
        path: String? = System.getenv(SUITE_PROPERTIES_ENV),
        onWarning: ((Exception) -> Unit)? = null,
    ): Properties {
        val file = path?.takeIf { it.isNotEmpty() }?.let(::File)
        if (file == null || !file.exists()) return Properties()
        return try {
            Properties().apply { file.reader(Charsets.UTF_8).use(::load) }
        } catch (e: Exception) {
            onWarning?.invoke(e)
            Properties()
        }
    }
}
