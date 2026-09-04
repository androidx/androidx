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
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Arguments passed from the host orchestrator to a [BackupDeviceAction].
 *
 * @property payload key-value arguments for the action
 */
public class BackupDeviceActionArgs
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupDeviceActionArgs) return false
        return payload == other.payload
    }

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = "BackupDeviceActionArgs(payload=$payload)"
}

/**
 * Results returned from a [BackupDeviceAction] back to the host orchestrator.
 *
 * @property payload key-value results produced by the action
 */
public class BackupDeviceActionResult
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupDeviceActionResult) return false
        return payload == other.payload
    }

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = "BackupDeviceActionResult(payload=$payload)"
}

/** Phase during the backup and restore lifecycle when an action runs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Retention(AnnotationRetention.SOURCE)
@IntDef(BackupDeviceAction.PHASE_POPULATE, BackupDeviceAction.PHASE_VERIFY)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
)
public annotation class ActionPhase {
    public companion object {
        /** Seeds data inside the application sandbox before a backup. */
        public const val POPULATE: Int = BackupDeviceAction.PHASE_POPULATE

        /** Verifies restored data inside the application sandbox after a restore. */
        public const val VERIFY: Int = BackupDeviceAction.PHASE_VERIFY
    }
}

/**
 * Runs an action inside the application sandbox on the target device.
 *
 * Implementations must provide a public no-arg constructor to enable dynamic instantiation.
 */
public interface BackupDeviceAction {
    public companion object {
        /** Seeds data inside the application sandbox before a backup. */
        public const val PHASE_POPULATE: Int = 1

        /** Verifies restored data inside the application sandbox after a restore. */
        public const val PHASE_VERIFY: Int = 2
        /**
         * Key specifying the target storage medium.
         *
         * Must be set to one of the following storage type constants:
         * - [STORAGE_TYPE_PREFS]
         * - [STORAGE_TYPE_DATABASE]
         * - [STORAGE_TYPE_FILES]
         */
        public const val KEY_STORAGE_TYPE: String = "storage_type"

        /** Storage type value indicating SharedPreferences. */
        public const val STORAGE_TYPE_PREFS: String = "PREFS"

        /** Storage type value indicating SQLite database. */
        public const val STORAGE_TYPE_DATABASE: String = "DATABASE"

        /** Storage type value indicating raw file storage. */
        public const val STORAGE_TYPE_FILES: String = "FILES"

        /** Key specifying the SharedPreferences filename. */
        public const val KEY_PREF_NAME: String = "pref_name"

        /** Key specifying the SharedPreferences preference key. */
        public const val KEY_PREF_KEY: String = "pref_key"

        /** Key specifying the value to write or verify. */
        public const val KEY_VALUE: String = "value"

        /**
         * Key specifying the primitive value type for SharedPreferences entries.
         *
         * Must be one of `"INT"`, `"LONG"`, `"BOOLEAN"`, `"FLOAT"`, or `"STRING"`.
         */
        public const val KEY_VALUE_TYPE: String = "value_type"

        /** Key specifying the SQLite database filename. */
        public const val KEY_DB_NAME: String = "db_name"

        /** Key specifying the table name inside the SQLite database. */
        public const val KEY_TABLE: String = "table"

        /**
         * Key specifying query or insert key-value pairs formatted as an ampersand-separated
         * string.
         *
         * For example, `"column1=value1&column2=value2"`.
         */
        public const val KEY_VALUES: String = "values"

        /** Key specifying a relative or absolute file path. */
        public const val KEY_PATH: String = "path"

        /**
         * Key specifying whether a file value is base64-encoded binary data.
         *
         * Accepted values are `"true"` or `"false"`. When `"true"`, file contents are decoded using
         * [android.util.Base64.DEFAULT].
         */
        public const val KEY_IS_BINARY: String = "is_binary"

        /**
         * Key specifying whether device-protected storage context should be used.
         *
         * When set to `"true"`, operations target
         * [android.content.Context.createDeviceProtectedStorageContext] instead of default
         * credential-encrypted storage. Accepted values are `"true"` or `"false"`.
         */
        public const val KEY_IS_DEVICE_PROTECTED: String = "is_device_protected"

        /** Key specifying the primary key column name in SQL query verification. */
        public const val KEY_KEY_COL: String = "key_col"

        /** Key specifying the primary key column value to filter on in SQL verification. */
        public const val KEY_KEY_VAL: String = "key_val"

        /** Key specifying the column containing the expected value in SQL verification. */
        public const val KEY_EXPECTED_COL: String = "expected_col"

        /**
         * Key specifying the expected column value in SQL verification.
         *
         * Must be passed as a string representation of the expected column value (e.g., `"true"` or
         * `"false"` for SQLite booleans, or string values).
         */
        public const val KEY_EXPECTED_VAL: String = "expected_val"

        /** Key specifying the expected value in verification. */
        public const val KEY_EXPECTED: String = "expected"

        /**
         * Key specifying whether to assert that a preference or value is null.
         *
         * Accepted values are `"true"` or `"false"`. When `"true"`, verification asserts that the
         * preference key, column, or file does not exist.
         */
        public const val KEY_EXPECT_NULL: String = "expect_null"

        /**
         * Key indicating the execution status in [BackupDeviceActionResult].
         *
         * The returned value will be one of the status constants: [STATUS_SUCCESS] or
         * [STATUS_FAILURE]. Since this key tracks non-binary, extensible status categories (e.g.,
         * which could include future warnings or partial success info), it is represented as a
         * string rather than a simple boolean.
         */
        public const val KEY_STATUS: String = "status"

        /** Status value indicating successful execution. */
        public const val STATUS_SUCCESS: String = "success"

        /** Status value indicating failed execution. */
        public const val STATUS_FAILURE: String = "failure"

        /**
         * Key containing the error message in [BackupDeviceActionResult] upon failure.
         *
         * Present when [KEY_STATUS] is [STATUS_FAILURE].
         */
        public const val KEY_ERROR: String = "error"
    }

    /** Lifecycle phase when this action runs. */
    @get:ActionPhase public val phase: Int

    /**
     * Runs the action payload.
     *
     * @param context application context on the target device
     * @param args input arguments passed from the host orchestrator
     * @return result returned to the host
     */
    public fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult
}
