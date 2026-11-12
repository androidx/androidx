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

package androidx.test.backup.actions

import android.content.Context
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import androidx.test.backup.BackupRestoreTestRunner
import androidx.test.backup.truncated
import org.json.JSONArray

/**
 * Runs several storage actions in order, in one run of the runner.
 *
 * [KEY_CALLS] holds the calls as a JSON array. Each call is a JSON object with the class name of
 * its action under [CALL_ACTION], which must be that of [PopulateStorageAction] or
 * [AssertStorageAction], and the arguments of the action under [CALL_ARGS], as a JSON object of
 * strings.
 *
 * The batch stops at the first call that fails. Its result then has the error message that the call
 * reports when it runs on its own, and the index of the call under [KEY_FAILED_CALL]. When
 * [KEY_CALLS] is missing or invalid, the batch runs no call and fails without an index.
 */
internal class StorageBatchAction : BackupDeviceAction {
    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val json =
            args[KEY_CALLS]
                ?: return BackupDeviceActionResult.failure("Missing '$KEY_CALLS' argument.")
        val calls =
            try {
                parseCalls(json)
            } catch (e: Exception) {
                return BackupDeviceActionResult.failure(
                    "Invalid '$KEY_CALLS' argument: ${e.message}"
                )
            }
        for ((index, call) in calls.withIndex()) {
            val (action, actionArgs) = call
            val error =
                try {
                    val result = action.execute(context, actionArgs)
                    if (result.isSuccess) continue
                    result.errorMessage.orEmpty()
                } catch (e: Throwable) {
                    truncated(
                        BackupRestoreTestRunner.thrownMessage(e),
                        BackupRestoreTestRunner.MAX_TEXT_LENGTH_CHARS,
                    )
                }
            return BackupDeviceActionResult.failure(
                error,
                mapOf(KEY_FAILED_CALL to index.toString()),
            )
        }
        return BackupDeviceActionResult.success()
    }

    /**
     * Returns the action and the arguments of each call in [json].
     *
     * @throws org.json.JSONException if [json] is not a JSON array of calls
     * @throws IllegalArgumentException if a call names another action than the two storage actions,
     *   or has an argument that is not a string
     */
    private fun parseCalls(json: String): List<Pair<BackupDeviceAction, BackupDeviceActionArgs>> {
        val calls = JSONArray(json)
        return List(calls.length()) { index ->
            val call = calls.getJSONObject(index)
            val action =
                when (val className = call.getString(CALL_ACTION)) {
                    PopulateStorageAction::class.java.name -> PopulateStorageAction()
                    AssertStorageAction::class.java.name -> AssertStorageAction()
                    else -> throw IllegalArgumentException("Unsupported action class $className")
                }
            action to
                BackupDeviceActionArgs(
                    BackupRestoreTestRunner.argsOf(call.getJSONObject(CALL_ARGS))
                )
        }
    }

    companion object {
        /** The argument that holds the calls, as a JSON array of objects. */
        const val KEY_CALLS = "calls"

        /** The member of a call that holds the class name of its action. */
        const val CALL_ACTION = "action"

        /** The member of a call that holds the arguments of its action. */
        const val CALL_ARGS = "args"

        /** The result key that holds the index in [KEY_CALLS] of the call that failed. */
        const val KEY_FAILED_CALL = "failed_call"
    }
}
