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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What `androidx.test.backup.BackupRestoreTestRunner` reported for one device action.
 *
 * The action's payload arrives either inline as [inlinePayload], or, when it is too large to print,
 * in a device file at [payloadPath] that the caller reads with [parseOverflowFile].
 *
 * @property runnerFailure the failure the runner reported, or that decoding its output produced;
 *   null if the runner reported success
 * @property inlinePayload the action's payload as a JSON object string, if it was printed
 * @property payloadPath the device file holding the action's payload, if it was too large to print
 */
internal class BackupRunnerReport(
    val runnerFailure: BackupActionResult.Failure?,
    val inlinePayload: String?,
    val payloadPath: String?,
) {

    /** Returns the result of [actionClassName], given its payload as a JSON object string. */
    fun resultFor(payloadJson: String, actionClassName: String): BackupActionResult =
        reconcile(runnerFailure, toActionResult(parseStringMap(payloadJson), actionClassName))

    /**
     * Combines the runner's verdict with the one derived from the payload.
     *
     * The payload decides the message, but a failure reported by the runner is never downgraded to
     * a success, so a runner that is stricter than the payload rule still wins.
     */
    private fun reconcile(
        runnerFailure: BackupActionResult.Failure?,
        fromPayload: BackupActionResult,
    ): BackupActionResult =
        if (runnerFailure != null && fromPayload is BackupActionResult.Success) {
            runnerFailure
        } else {
            fromPayload
        }

    /**
     * Converts a device action's result payload into a [BackupActionResult].
     *
     * An action reports a problem in-band through [BackupActionOutputKeys.STATUS] rather than by
     * throwing. Reading it here keeps the host correct even against a runner that reports only
     * whether the action threw, and supplies the specific error message.
     *
     * This mirrors `androidx.test.backup.BackupDeviceActionResult.isSuccess` exactly, so the device
     * and the host never disagree about the same payload:
     * - A reported status decides on its own. Only the recognized failure value
     *   [BackupActionValues.STATUS_FAILURE] marks the action as failed; `status` is a generic key
     *   that custom actions legitimately publish their own vocabulary through, so an unrecognized
     *   value is reported as a success rather than being guessed at.
     * - With no status at all, a non-empty [BackupActionOutputKeys.ERROR] marks the action as
     *   failed, so an action that reports only an error is not read as passing.
     *
     * Actions that want their failures honored should use
     * `androidx.test.backup.BackupDeviceActionResult.failure`, which emits the recognized value.
     */
    private fun toActionResult(
        dataMap: Map<String, String>,
        actionClassName: String,
    ): BackupActionResult {
        val status = dataMap[BackupActionOutputKeys.STATUS]
        val error = dataMap[BackupActionOutputKeys.ERROR]
        if (status != null) {
            if (status.equals(BackupActionValues.STATUS_FAILURE, ignoreCase = true)) {
                return BackupActionResult.Failure(
                    error
                        ?: "$actionClassName reported ${BackupActionOutputKeys.STATUS}='$status' without an " +
                            "${BackupActionOutputKeys.ERROR} message."
                )
            }
        } else if (!error.isNullOrEmpty()) {
            return BackupActionResult.Failure(error)
        }
        return BackupActionResult.Success(dataMap)
    }

    companion object {
        private const val RESULT_MARKER = "BACKUP_RESTORE_RESULT: "
        private const val FALLBACK_RESULT_MARKER = "resultJson="

        private const val KEY_IS_SUCCESS = "isSuccess"
        private const val KEY_ERROR_MESSAGE = "errorMessage"
        private const val KEY_STACK_TRACE = "stackTrace"
        private const val KEY_PAYLOAD_PATH = "payload_path"
        private const val KEY_PAYLOAD_JSON = "payloadJson"

        /**
         * Decodes the output of an `am instrument` run of the runner.
         *
         * Output that carries no result, or a result that is not valid JSON, is reported as a
         * [runnerFailure] with no payload.
         */
        fun parse(stdout: String): BackupRunnerReport {
            val json =
                resultLine(stdout, RESULT_MARKER) ?: resultLine(stdout, FALLBACK_RESULT_MARKER)
            if (json.isNullOrEmpty()) {
                return failed("No execution result was received from device. Raw stdout:\n$stdout")
            }
            val result =
                try {
                    Json.parseToJsonElement(json).jsonObject
                } catch (e: Exception) {
                    return failed(
                        "Failed to parse runner output JSON: ${e.message}. Raw JSON:\n$json"
                    )
                }
            val runnerFailure =
                if (result.boolean(KEY_IS_SUCCESS) == true) {
                    null
                } else {
                    BackupActionResult.Failure(
                        errorMessage =
                            result.string(KEY_ERROR_MESSAGE) ?: "Unknown device failure.",
                        stackTrace = result.string(KEY_STACK_TRACE),
                    )
                }
            return BackupRunnerReport(
                runnerFailure = runnerFailure,
                inlinePayload = result.string(KEY_PAYLOAD_JSON),
                payloadPath = result.string(KEY_PAYLOAD_PATH),
            )
        }

        /**
         * Returns the payload held by an overflow file written by the runner, as a JSON object
         * string.
         *
         * @throws IllegalArgumentException if [content] is not a JSON object
         */
        fun parseOverflowFile(content: String): String =
            Json.parseToJsonElement(content).jsonObject.string(KEY_PAYLOAD_JSON) ?: ""

        private fun failed(message: String) =
            BackupRunnerReport(
                runnerFailure = BackupActionResult.Failure(message),
                inlinePayload = null,
                payloadPath = null,
            )

        /** Returns the first line after [marker] in [stdout], or null if [marker] is absent. */
        private fun resultLine(stdout: String, marker: String): String? {
            val index = stdout.indexOf(marker)
            if (index == -1) return null
            return stdout.substring(index + marker.length).trim().lineSequence().firstOrNull() ?: ""
        }

        private fun JsonObject.string(key: String): String? =
            this[key]?.jsonPrimitive?.contentOrNull

        private fun JsonObject.boolean(key: String): Boolean? =
            this[key]?.jsonPrimitive?.booleanOrNull

        /** Returns the primitive members of the JSON object [json] as strings; empty if invalid. */
        private fun parseStringMap(json: String): Map<String, String> {
            if (json.isEmpty()) return emptyMap()
            return try {
                buildMap {
                    for ((key, element) in Json.parseToJsonElement(json).jsonObject) {
                        (element as? JsonPrimitive)?.contentOrNull?.let { put(key, it) }
                    }
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
