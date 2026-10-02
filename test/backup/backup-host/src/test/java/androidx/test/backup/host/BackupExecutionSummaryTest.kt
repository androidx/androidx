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

import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BackupExecutionSummaryTest {

    @Test
    fun errorCodeForClassifiesEachStage() {
        fun code(stage: BackupExecutionStage, message: String?) =
            stage.errorCodeFor(IOException(message))

        assertEquals(BackupErrorCode.UNKNOWN_ERROR, code(BackupExecutionStage.PRECONDITION, "x"))
        assertEquals(BackupErrorCode.SEEDING_FAILED, code(BackupExecutionStage.SEEDING, "timeout"))
        assertEquals(BackupErrorCode.BACKUP_FAILED, code(BackupExecutionStage.BACKUP, "x"))
        assertEquals(
            BackupErrorCode.CLEAR_DATA_FAILED,
            code(BackupExecutionStage.CLEAR_DATA, "bmgr"),
        )
        assertEquals(BackupErrorCode.RESTORE_FAILED, code(BackupExecutionStage.RESTORE, null))
        assertEquals(
            BackupErrorCode.VERIFICATION_FAILED,
            code(BackupExecutionStage.VERIFICATION, "timeout"),
        )
    }

    @Test
    fun errorCodeForRecognizesCausesCaseInsensitively() {
        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT,
            BackupExecutionStage.RESTORE.errorCodeFor(IOException("TIMEOUT DURING RESTORE")),
        )
        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT,
            BackupExecutionStage.RESTORE.errorCodeFor(IOException("POLLING FAILED")),
        )
        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT,
            BackupExecutionStage.RESTORE.errorCodeFor(IOException("Restore TIMED OUT after 5s")),
        )
        assertEquals(
            BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
            BackupExecutionStage.BACKUP.errorCodeFor(IOException("MISSING GMSCORE")),
        )
        assertEquals(
            BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
            BackupExecutionStage.RESTORE.errorCodeFor(IOException("Update the Play Store")),
        )
        assertEquals(
            BackupErrorCode.BMGR_INIT_FAILED,
            BackupExecutionStage.BACKUP.errorCodeFor(IOException("BMGR TRANSPORT ERROR")),
        )
        assertEquals(
            BackupErrorCode.BMGR_INIT_FAILED,
            BackupExecutionStage.RESTORE.errorCodeFor(IOException("bmgr: transport error")),
        )
    }

    @Test
    fun errorCodeForRecognizesAKeyguardFailureBeforeTheFlowStarts() {
        assertEquals(
            BackupErrorCode.KEYGUARD_UNLOCK_FAILED,
            BackupExecutionStage.PRECONDITION.errorCodeFor(
                IllegalStateException("Device keyguard dismiss failed")
            ),
        )
    }

    @Test
    fun trackerRecordsTheDurationOfEachStage() = runBlocking {
        val clock = FakeClock()
        val tracker = BackupExecutionTracker(BackupTransportMode.LOCAL, 2, clock::nanoTime)

        tracker.stage(BackupExecutionStage.SEEDING) { clock.advanceMillis(10) }
        tracker.stage(BackupExecutionStage.BACKUP) { clock.advanceMillis(20) }
        tracker.stage(BackupExecutionStage.CLEAR_DATA) { clock.advanceMillis(30) }
        tracker.stage(BackupExecutionStage.RESTORE) { clock.advanceMillis(40) }
        clock.advanceMillis(5)
        tracker.stage(BackupExecutionStage.VERIFICATION) { clock.advanceMillis(50) }

        assertEquals(
            BackupExecutionSummary(
                transportMode = BackupTransportMode.LOCAL,
                storageDomainCount = 2,
                seedingDuration = Duration.ofMillis(10),
                backupDuration = Duration.ofMillis(20),
                clearDataDuration = Duration.ofMillis(30),
                restoreDuration = Duration.ofMillis(40),
                verificationDuration = Duration.ofMillis(50),
                totalDuration = Duration.ofMillis(155),
                isSuccess = true,
            ),
            tracker.succeeded(),
        )
    }

    @Test
    fun trackerReturnsTheResultOfAStage() = runBlocking {
        val tracker = BackupExecutionTracker(BackupTransportMode.LOCAL, 1)

        assertEquals("archive", tracker.stage(BackupExecutionStage.BACKUP) { "archive" })
    }

    @Test
    fun trackerAttributesAFailureToTheStageThatThrew() {
        val clock = FakeClock()
        val tracker = BackupExecutionTracker(BackupTransportMode.LOCAL, 1, clock::nanoTime)
        val error = IOException("bmgr transport unavailable")

        val thrown =
            assertFailsWith<IOException> {
                runBlocking {
                    tracker.stage(BackupExecutionStage.SEEDING) { clock.advanceMillis(10) }
                    tracker.stage(BackupExecutionStage.BACKUP) {
                        clock.advanceMillis(7)
                        throw error
                    }
                }
            }

        assertEquals(
            BackupExecutionSummary(
                transportMode = BackupTransportMode.LOCAL,
                storageDomainCount = 1,
                seedingDuration = Duration.ofMillis(10),
                backupDuration = Duration.ofMillis(7),
                totalDuration = Duration.ofMillis(17),
                isSuccess = false,
                errorCode = BackupErrorCode.BMGR_INIT_FAILED,
                failureStage = BackupExecutionStage.BACKUP,
                errorMessage = "bmgr transport unavailable",
            ),
            tracker.failed(thrown),
        )
    }

    @Test
    fun trackerAttributesAFailureBeforeAnyStageToThePrecondition() {
        val tracker = BackupExecutionTracker(BackupTransportMode.CLOUD_ENCRYPTED, 0)

        val summary = tracker.failed(IllegalArgumentException("no domains"))

        assertEquals(BackupExecutionStage.PRECONDITION, summary.failureStage)
        assertEquals(BackupErrorCode.UNKNOWN_ERROR, summary.errorCode)
        assertEquals(Duration.ZERO, summary.seedingDuration)
    }

    @Test
    fun publishToReportsASuccess() {
        val published = linkedMapOf<String, String>()

        BackupExecutionSummary(
                transportMode = BackupTransportMode.LOCAL,
                storageDomainCount = 3,
                seedingDuration = Duration.ofMillis(1),
                backupDuration = Duration.ofMillis(2),
                clearDataDuration = Duration.ofMillis(3),
                restoreDuration = Duration.ofMillis(4),
                verificationDuration = Duration.ofMillis(5),
                totalDuration = Duration.ofMillis(15),
                isSuccess = true,
            )
            .publishTo { key, value -> published[key] = value }

        assertEquals(
            mapOf(
                BackupReportKeys.LIBRARY_VERSION to BackupReportKeys.UNKNOWN_LIBRARY_VERSION,
                BackupReportKeys.TRANSPORT_MODE to "LOCAL",
                BackupReportKeys.STORAGE_DOMAIN_COUNT to "3",
                BackupReportKeys.SEEDING_DURATION to "1",
                BackupReportKeys.BACKUP_DURATION to "2",
                BackupReportKeys.CLEAR_DATA_DURATION to "3",
                BackupReportKeys.RESTORE_DURATION to "4",
                BackupReportKeys.VERIFICATION_DURATION to "5",
                BackupReportKeys.TOTAL_DURATION to "15",
                BackupReportKeys.STATUS to BackupReportKeys.REPORT_STATUS_SUCCESS,
            ),
            published,
        )
    }

    @Test
    fun publishToReportsTheCauseOfAFailure() {
        val published = linkedMapOf<String, String>()

        BackupExecutionSummary(
                transportMode = BackupTransportMode.LOCAL,
                storageDomainCount = 1,
                isSuccess = false,
                errorCode = BackupErrorCode.CLEAR_DATA_FAILED,
                failureStage = BackupExecutionStage.CLEAR_DATA,
                errorMessage = "pm clear failed",
            )
            .publishTo { key, value -> published[key] = value }

        assertEquals(BackupReportKeys.REPORT_STATUS_FAILURE, published[BackupReportKeys.STATUS])
        assertEquals("CLEAR_DATA_FAILED", published[BackupReportKeys.ERROR_CODE])
        assertEquals("CLEAR_DATA", published[BackupReportKeys.FAILURE_STAGE])
        assertEquals("pm clear failed", published[BackupReportKeys.ERROR_MESSAGE])
    }

    /** A clock that only moves when told to. */
    private class FakeClock {
        private var nanos = 1_000_000_000L

        fun nanoTime(): Long = nanos

        fun advanceMillis(millis: Long) {
            nanos += Duration.ofMillis(millis).toNanos()
        }
    }
}
