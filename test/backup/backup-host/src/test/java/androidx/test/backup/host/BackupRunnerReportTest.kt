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

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class BackupRunnerReportTest {

    @Test
    fun parseReadsASuccessfulResult() {
        val report = BackupRunnerReport.parse(runnerStdout("""{"rows":"3"}"""))

        assertNull(report.runnerFailure)
        assertEquals("""{"rows":"3"}""", report.inlinePayload)
        assertNull(report.payloadPath)
    }

    @Test
    fun parseReadsTheRunnerFailureAndStackTrace() {
        val report =
            BackupRunnerReport.parse(
                RESULT_MARKER +
                    """{"isSuccess":false,"errorMessage":"Something broke","stackTrace":"at A.kt:15"}"""
            )

        assertEquals(
            BackupActionResult.Failure("Something broke", "at A.kt:15"),
            report.runnerFailure,
        )
        assertNull(report.inlinePayload)
    }

    @Test
    fun parseReportsAFailureWithoutAMessageAsUnknown() {
        val report = BackupRunnerReport.parse(RESULT_MARKER + """{"isSuccess":false}""")

        assertEquals("Unknown device failure.", report.runnerFailure?.errorMessage)
    }

    @Test
    fun parseTreatsAMissingVerdictAsAFailure() {
        assertIs<BackupActionResult.Failure>(
            BackupRunnerReport.parse(RESULT_MARKER + "{}").runnerFailure
        )
    }

    @Test
    fun parseReadsTheOverflowPath() {
        val report =
            BackupRunnerReport.parse(
                RESULT_MARKER + """{"isSuccess":true,"payload_path":"/data/local/tmp/p.json"}"""
            )

        assertEquals("/data/local/tmp/p.json", report.payloadPath)
        assertNull(report.inlinePayload)
    }

    @Test
    fun parseReadsOnlyTheLineAfterTheMarker() {
        val report =
            BackupRunnerReport.parse(
                "INSTRUMENTATION_STATUS: x\n" +
                    RESULT_MARKER +
                    """{"isSuccess":true}""" +
                    "\nINSTRUMENTATION_CODE: -1\n"
            )

        assertNull(report.runnerFailure)
    }

    @Test
    fun parseFallsBackToTheInstrumentationResultKey() {
        val report =
            BackupRunnerReport.parse("""INSTRUMENTATION_RESULT: resultJson={"isSuccess":true}""")

        assertNull(report.runnerFailure)
    }

    @Test
    fun parseReportsMissingOutputAsAFailure() {
        val report = BackupRunnerReport.parse("Process crashed.")

        assertEquals(
            "No execution result was received from device. Raw stdout:\nProcess crashed.",
            report.runnerFailure?.errorMessage,
        )
        assertNull(report.inlinePayload)
        assertNull(report.payloadPath)
    }

    @Test
    fun parseReportsAnEmptyResultAsAFailure() {
        val report = BackupRunnerReport.parse(RESULT_MARKER)

        assertTrue(report.runnerFailure!!.errorMessage.startsWith("No execution result"))
    }

    @Test
    fun parseReportsInvalidJsonAsAFailure() {
        val report = BackupRunnerReport.parse(RESULT_MARKER + "{not json")

        val message = report.runnerFailure!!.errorMessage
        assertTrue(message.startsWith("Failed to parse runner output JSON: "), message)
        assertTrue(message.endsWith("Raw JSON:\n{not json"), message)
        assertNull(report.inlinePayload)
        assertNull(report.payloadPath)
    }

    @Test
    fun parseOverflowFileReturnsTheInnerPayload() {
        val file = buildJsonObject { put("payloadJson", """{"rows":"5"}""") }.toString()

        assertEquals("""{"rows":"5"}""", BackupRunnerReport.parseOverflowFile(file))
    }

    @Test
    fun parseOverflowFileReturnsAnEmptyPayloadWhenAbsent() {
        assertEquals("", BackupRunnerReport.parseOverflowFile("{}"))
    }

    @Test
    fun parseOverflowFileRejectsInvalidContent() {
        assertFailsWith<IllegalArgumentException> { BackupRunnerReport.parseOverflowFile("[1]") }
    }

    /**
     * A failed assertion inside AssertStorageAction shows up as `status=failure` in the payload.
     * The envelope here reports `isSuccess=true`, as an older runner does, so this pins that the
     * host reads the payload rather than trusting the envelope alone.
     */
    @Test
    fun resultForSurfacesAnInBandFailure() {
        assertEquals(
            BackupActionResult.Failure("Expected 'a' but found 'b'"),
            resultFor("""{"status":"failure","error":"Expected 'a' but found 'b'"}"""),
        )
    }

    /** A failure with no error message still fails, naming the action so the report is usable. */
    @Test
    fun resultForNamesTheActionOfAnInBandFailureWithoutAMessage() {
        val result = assertIs<BackupActionResult.Failure>(resultFor("""{"status":"failure"}"""))

        assertTrue(result.errorMessage.contains(ACTION), result.errorMessage)
    }

    /**
     * Only the recognized failure value fails an action.
     *
     * `status` predates this convention and custom actions publish their own vocabulary through it
     * — the AddressBook sample reports `status=verified` to mean "all checks passed". Treating
     * every non-success value as a failure broke that app, so unrecognized values must pass.
     */
    @Test
    fun resultForAcceptsAnUnrecognizedStatus() {
        assertEquals(
            BackupActionResult.Success(mapOf("status" to "verified", "authVerified" to "true")),
            resultFor("""{"status":"verified","authVerified":"true"}"""),
        )
    }

    @Test
    fun resultForAcceptsAnInBandSuccess() {
        assertEquals(
            BackupActionResult.Success(mapOf("status" to "success", "rows" to "3")),
            resultFor("""{"status":"success","rows":"3"}"""),
        )
    }

    /** Custom actions are not required to report a status; silence still means success. */
    @Test
    fun resultForTreatsAnAbsentStatusAsSuccess() {
        assertEquals(
            BackupActionResult.Success(mapOf("rows" to "3")),
            resultFor("""{"rows":"3"}"""),
        )
    }

    /**
     * An action that reports an error but forgets the status still fails.
     *
     * Mirrors `androidx.test.backup.BackupDeviceActionResult.isSuccess`; without this the host
     * would report a Success carrying the error text in its data map.
     */
    @Test
    fun resultForSurfacesAnErrorWithoutAStatus() {
        assertEquals(
            BackupActionResult.Failure("Disk full"),
            resultFor("""{"error":"Disk full"}"""),
        )
    }

    /**
     * An empty error without a status is not a failure.
     *
     * The AddressBook sample returns an empty `restore_credential_error` alongside a passing
     * verification, so the presence of the key alone cannot decide the outcome.
     */
    @Test
    fun resultForIgnoresAnEmptyErrorWithoutAStatus() {
        assertIs<BackupActionResult.Success>(resultFor("""{"error":""}"""))
    }

    /** The runner reports the action's verdict; the payload supplies the specific message. */
    @Test
    fun resultForTakesTheMessageFromThePayloadWhenTheRunnerReportsFailure() {
        assertEquals(
            BackupActionResult.Failure("Expected 'a' but found 'b'"),
            resultFor(
                """{"status":"failure","error":"Expected 'a' but found 'b'"}""",
                runnerFailure = BackupActionResult.Failure("Expected 'a' but found 'b'"),
            ),
        )
    }

    /**
     * With no error on the action, the runner has no message to report; the payload still lets the
     * host name the action instead of falling back to "Unknown device failure.".
     */
    @Test
    fun resultForNamesTheActionWhenTheRunnerReportsFailureWithoutAMessage() {
        val result =
            assertIs<BackupActionResult.Failure>(
                resultFor(
                    """{"status":"failure"}""",
                    runnerFailure = BackupActionResult.Failure("Unknown device failure."),
                )
            )

        assertTrue(result.errorMessage.contains(ACTION), result.errorMessage)
    }

    /** A failure reported by the runner is never downgraded by a payload that looks successful. */
    @Test
    fun resultForKeepsTheRunnerFailureOverASuccessfulPayload() {
        val runnerFailure = BackupActionResult.Failure("Runner said no")

        assertEquals(runnerFailure, resultFor("""{"rows":"3"}""", runnerFailure))
    }

    @Test
    fun resultForKeepsOnlyPrimitiveMembers() {
        assertEquals(
            BackupActionResult.Success(mapOf("n" to "1", "s" to "x")),
            resultFor("""{"n":1,"s":"x","o":{"k":"v"},"a":[1],"z":null}"""),
        )
    }

    @Test
    fun resultForTreatsAnInvalidPayloadAsEmpty() {
        assertEquals(BackupActionResult.Success(emptyMap()), resultFor("not json"))
    }

    private fun resultFor(
        payloadJson: String,
        runnerFailure: BackupActionResult.Failure? = null,
    ): BackupActionResult =
        BackupRunnerReport(runnerFailure, payloadJson, payloadPath = null)
            .resultFor(payloadJson, ACTION)

    private companion object {
        const val ACTION = "com.example.MyAction"
        const val RESULT_MARKER = "BACKUP_RESTORE_RESULT: "

        /** Wraps an action payload in the envelope the on-device runner prints to stdout. */
        fun runnerStdout(payloadJson: String): String {
            val envelope = buildJsonObject {
                put("isSuccess", true)
                put("payloadJson", payloadJson)
            }
            return "$RESULT_MARKER$envelope\n"
        }
    }
}
