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
                instrumentOutput(
                    """{"isSuccess":false,"errorMessage":"Something broke","stackTrace":"at A.kt:15"}"""
                )
            )

        assertEquals(
            BackupActionResult.Failure("Something broke", "at A.kt:15"),
            report.runnerFailure,
        )
        assertNull(report.inlinePayload)
    }

    @Test
    fun parseReportsAFailureWithoutAMessageAsUnknown() {
        val report = BackupRunnerReport.parse(instrumentOutput("""{"isSuccess":false}"""))

        assertEquals("Unknown device failure.", report.runnerFailure?.errorMessage)
    }

    @Test
    fun parseTreatsAMissingVerdictAsAFailure() {
        assertIs<BackupActionResult.Failure>(
            BackupRunnerReport.parse(instrumentOutput("{}")).runnerFailure
        )
    }

    @Test
    fun parseReadsTheOverflowPath() {
        val report = BackupRunnerReport.parse(overflowStdout("/data/local/tmp/p.json"))

        assertEquals("/data/local/tmp/p.json", report.payloadPath)
        assertNull(report.inlinePayload)
    }

    @Test
    fun parseFindsTheResultLineAmongOtherInstrumentationOutput() {
        val report =
            BackupRunnerReport.parse(
                "INSTRUMENTATION_STATUS: x\n" + instrumentOutput("""{"isSuccess":true}""")
            )

        assertNull(report.runnerFailure)
    }

    /**
     * What the app prints to its own stdout goes to logcat, not to the output of `am instrument`.
     */
    @Test
    fun parseReadsOnlyTheResultThatAmInstrumentPrints() {
        val report =
            BackupRunnerReport.parse(
                """BACKUP_RESTORE_RESULT: {"isSuccess":true}""" + "\nINSTRUMENTATION_CODE: -1\n"
            )

        assertTrue(report.runnerFailure!!.errorMessage.startsWith("No execution result"))
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

    /** The output of a run that sent a large result in parts can be megabytes long. */
    @Test
    fun parseShowsOnlyTheEndOfLongOutputWithoutAResult() {
        val stdout = "x".repeat(20_000) + "\nINSTRUMENTATION_RESULT: shortMsg=Process crashed.\n"

        val message = BackupRunnerReport.parse(stdout).runnerFailure!!.errorMessage

        assertTrue(message.startsWith("No execution result was received from device. Last 8192 of"))
        assertTrue(message.endsWith("shortMsg=Process crashed.\n"), message)
        assertTrue(message.length < 8_500, "${message.length}")
    }

    @Test
    fun parseDoesNotStartTheEndOfLongOutputWithHalfOfASurrogatePair() {
        val end = "y".repeat(8191)
        val stdout = "x".repeat(10_000) + "\uD83D\uDE00" + end

        val message = BackupRunnerReport.parse(stdout).runnerFailure!!.errorMessage

        assertEquals(
            "No execution result was received from device. " +
                "Last 8191 of 18193 characters of raw stdout:\n$end",
            message,
        )
    }

    /** The runner sends a result too large for one Binder transaction in parts. */
    @Test
    fun parseJoinsTheResultFromItsParts() {
        val resultJson = buildJsonObject {
            put("isSuccess", true)
            put("payloadJson", """{"data":"${"x".repeat(300)} with spaces "}""")
        }
            .toString()

        val report = BackupRunnerReport.parse(partedInstrumentOutput(resultJson, partLength = 7))

        assertNull(report.runnerFailure)
        assertEquals("""{"data":"${"x".repeat(300)} with spaces "}""", report.inlinePayload)
    }

    /** A part can start or end with a space within a JSON string, so parts are kept as they are. */
    @Test
    fun parseKeepsTheSpacesAtTheEndsOfParts() {
        val resultJson = """{"isSuccess":true,"payloadJson":"{\"a\":\"  b  \"}"}"""
        val partLength = resultJson.indexOf("  b") + 1

        val report = BackupRunnerReport.parse(partedInstrumentOutput(resultJson, partLength))

        assertEquals("""{"a":"  b  "}""", report.inlinePayload)
    }

    /** `Instrumentation.sendStatus` drops every status after one that fails to reach the host. */
    @Test
    fun parseReportsMissingPartsAsAFailure() {
        val resultJson = """{"isSuccess":true,"payloadJson":"{}"}"""
        val lastPart =
            "INSTRUMENTATION_STATUS: resultPart=${resultJson.chunked(10).last()}\n" +
                "INSTRUMENTATION_STATUS_CODE: 1\n"
        val stdout = partedInstrumentOutput(resultJson, partLength = 10).replace(lastPart, "")

        val report = BackupRunnerReport.parse(stdout)

        assertEquals(
            "Received 3 of the 4 parts of the result from device, with 30 of 37 characters.",
            report.runnerFailure?.errorMessage,
        )
        assertNull(report.inlinePayload)
    }

    @Test
    fun parseReportsAResultOfTheWrongLengthAsAFailure() {
        val resultJson = """{"isSuccess":true}"""
        val stdout =
            partedInstrumentOutput(resultJson, partLength = 5)
                .replace("resultLength=18", "resultLength=19")

        val message = BackupRunnerReport.parse(stdout).runnerFailure!!.errorMessage

        assertEquals(
            "Received 4 of the 4 parts of the result from device, with 18 of 19 characters.",
            message,
        )
    }

    @Test
    fun parseReportsAnEmptyResultAsAFailure() {
        val report = BackupRunnerReport.parse(instrumentOutput(""))

        assertTrue(report.runnerFailure!!.errorMessage.startsWith("No execution result"))
    }

    @Test
    fun parseReportsInvalidJsonAsAFailure() {
        val report = BackupRunnerReport.parse(instrumentOutput("{not json"))

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

    /**
     * A failure reported by the runner is never downgraded by a payload that looks successful, and
     * keeps the payload's data without its status.
     */
    @Test
    fun resultForKeepsTheRunnerFailureOverASuccessfulPayload() {
        val runnerFailure = BackupActionResult.Failure("Runner said no", "at A.kt:1")

        assertEquals(
            BackupActionResult.Failure("Runner said no", "at A.kt:1", mapOf("rows" to "3")),
            resultFor("""{"status":"success","rows":"3"}""", runnerFailure),
        )
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
    }
}
