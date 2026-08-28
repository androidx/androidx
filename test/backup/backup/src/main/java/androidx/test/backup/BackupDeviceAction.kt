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

package androidx.test.backup

import android.content.Context

/**
 * Arguments passed from the host orchestrator to a [BackupDeviceAction].
 *
 * @property payload A map containing key-value arguments for the action. Keys represent target
 *   storage identifiers and values are the values to seed or assert. These are developer-defined
 *   for custom actions, or follow standardized schemas for prebuilt actions:
 *     - **SharedPreferences:** `"prefName"`, `"key"`, `"value"`
 *     - **Database:** `"dbName"`, `"table"`, `"primaryKeyCol"`, `"primaryKeyVal"`, and custom
 *       column keys
 *     - **Files:** `"path"`, `"content"`
 */
public class BackupDeviceActionArgs
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap())

/**
 * Results returned from a [BackupDeviceAction] back to the host orchestrator.
 *
 * @property payload A map containing the output data of the action. Keys represent result parameter
 *   names (such as `"status"` or custom output fields) and values represent their results. This
 *   payload is developer-defined and independent of the input arguments payload size or keys.
 *   Standard prebuilt actions return:
 *     - **Success:** `"status"` to `"success"`
 *     - **Failure:** `"status"` to `"error"` and `"error"` to `<description>`
 *
 * Non-fatal warnings and diagnostic info should be logged directly using standard Android logging
 * (such as [android.util.Log]) inside the action, rather than being passed via this result object.
 */
public class BackupDeviceActionResult
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap())

/** The phase during the backup and restore process when an action should be executed. */
public enum class ActionPhase {
    /** Seeds data inside the application sandbox before a backup. */
    POPULATE,
    /** Verifies restored data inside the application sandbox after a restore. */
    VERIFY,
}

/**
 * Runs a delegated action inside the application sandbox on the target device.
 *
 * Implementing classes must provide a public no-arg constructor to enable dynamic instantiation by
 * the device test runner.
 */
public interface BackupDeviceAction {
    /** The phase during the backup and restore test lifecycle when this action should run. */
    public val phase: ActionPhase

    /**
     * Executes the custom payload.
     *
     * ### Expected Arguments
     * The [args] parameter receives [BackupDeviceActionArgs] passed from the host orchestrator's
     * execution call. For standard prebuilt actions, the payload map keys represent target storage
     * parameters:
     * - **SharedPreferences:** `"prefName"`, `"key"`, `"value"`
     * - **Database:** `"dbName"`, `"table"`, `"primaryKeyCol"`, `"primaryKeyVal"`, and custom
     *   column keys
     * - **Files:** `"path"`, `"content"`
     *
     * For custom user-defined actions, developers can define custom key-value parameter schemas.
     *
     * ### Recommended Pattern
     * Declare expected parameter and result keys as public constants in a companion object:
     * ```
     * public class SeedUserSessionAction : BackupDeviceAction {
     *     override val phase = ActionPhase.POPULATE
     *
     *     public companion object {
     *         public const val KEY_USER_ID: String = "user_id"
     *         public const val KEY_SESSION_TOKEN: String = "session_token"
     *         public const val RESULT_STATUS: String = "status"
     *     }
     *
     *     override fun execute(args: BackupDeviceActionArgs): BackupDeviceActionResult {
     *         val userId = requireNotNull(args.payload[KEY_USER_ID]) { "Missing user_id" }
     *         val token = requireNotNull(args.payload[KEY_SESSION_TOKEN]) { "Missing session_token" }
     *         // ... execute sandbox database insertion ...
     *         return BackupDeviceActionResult(payload = mapOf(RESULT_STATUS to "success"))
     *     }
     * }
     * ```
     *
     * @param context the target device application context
     * @param args input arguments passed from the host orchestrator
     * @return [BackupDeviceActionResult] serialized back to the host JVM
     * @throws IllegalArgumentException if the provided arguments are missing or malformed
     */
    public fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult
}
