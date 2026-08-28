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

import android.app.backup.BackupManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.backup.ActionPhase
import androidx.test.backup.BackupDeviceAction
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.BackupDeviceActionResult
import java.io.File

/**
 * A generic on-device action that populates test data before a backup is performed.
 *
 * It parses the incoming `storage_type` argument from [BackupDeviceActionArgs] and seeds the
 * specified data into SharedPreferences, an SQLite database, or raw file storage. It automatically
 * notifies the `BackupManager` of state changes upon completion.
 */
public class PopulateStorageAction : BackupDeviceAction {
    override val phase: ActionPhase = ActionPhase.POPULATE

    override fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult {
        val payload = args.payload
        val isDeviceProtected = payload[KEY_IS_DEVICE_PROTECTED]?.toBoolean() ?: false
        val targetContext =
            if (isDeviceProtected) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
        val storageTypeStr = payload[KEY_STORAGE_TYPE] ?: STORAGE_TYPE_PREFS

        return try {
            when (storageTypeStr) {
                STORAGE_TYPE_PREFS -> {
                    val prefName = payload[KEY_PREF_NAME] ?: "default_prefs"
                    val key =
                        payload[KEY_PREF_KEY]
                            ?: return errorResult("Missing 'pref_key' argument for PREFS populate.")
                    val value =
                        payload[KEY_VALUE]
                            ?: return errorResult("Missing 'value' argument for PREFS populate.")
                    val valueType = payload[KEY_VALUE_TYPE] ?: "STRING"

                    val editor =
                        targetContext.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
                    when (valueType.uppercase()) {
                        "INT" -> editor.putInt(key, value.toInt())
                        "LONG" -> editor.putLong(key, value.toLong())
                        "FLOAT" -> editor.putFloat(key, value.toFloat())
                        "BOOLEAN" -> editor.putBoolean(key, value.toBoolean())
                        else -> editor.putString(key, value)
                    }
                    editor.commit()
                }

                STORAGE_TYPE_DATABASE -> {
                    val dbName =
                        payload[KEY_DB_NAME]
                            ?: return errorResult(
                                "Missing 'db_name' argument for DATABASE populate."
                            )
                    val table =
                        payload[KEY_TABLE]
                            ?: return errorResult("Missing 'table' argument for DATABASE populate.")
                    val valuesStr =
                        payload[KEY_VALUES]
                            ?: return errorResult(
                                "Missing 'values' argument for DATABASE populate."
                            )

                    targetContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db
                        ->
                        // Ensure the table exists
                        val cv = ContentValues()
                        val colDefs = mutableListOf<String>()
                        valuesStr.split("&").forEach { pair ->
                            val kv = pair.split("=")
                            if (kv.size == 2) {
                                val columnName =
                                    java.net.URLDecoder.decode(
                                        kv[0],
                                        java.nio.charset.StandardCharsets.UTF_8.name(),
                                    )
                                val columnValue =
                                    java.net.URLDecoder.decode(
                                        kv[1],
                                        java.nio.charset.StandardCharsets.UTF_8.name(),
                                    )
                                colDefs.add("`$columnName` TEXT")
                                cv.put(columnName, columnValue)
                            }
                        }
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `$table` (${colDefs.joinToString(", ")})"
                        )
                        db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                STORAGE_TYPE_FILES -> {
                    val path =
                        payload[KEY_PATH]
                            ?: return errorResult("Missing 'path' argument for FILES populate.")
                    val value =
                        payload[KEY_VALUE]
                            ?: return errorResult("Missing 'value' argument for FILES populate.")

                    val file =
                        File(path).let {
                            if (it.isAbsolute) {
                                it
                            } else {
                                File(targetContext.filesDir, path)
                            }
                        }
                    file.parentFile?.mkdirs()

                    val isBinary = payload[KEY_IS_BINARY]?.toBoolean() ?: false
                    if (isBinary) {
                        val bytes = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
                        file.writeBytes(bytes)
                    } else {
                        file.writeText(value)
                    }
                }

                else -> return errorResult("Unsupported storage type: $storageTypeStr")
            }

            // Automatically notify BackupManager that data has changed
            BackupManager(targetContext).dataChanged()

            BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_SUCCESS))
        } catch (e: Exception) {
            errorResult("PopulateStorageAction exception: ${e.message}")
        }
    }

    private fun errorResult(message: String): BackupDeviceActionResult {
        return BackupDeviceActionResult(mapOf(KEY_STATUS to STATUS_FAILURE, "error" to message))
    }

    /** Constants and action payload keys used to configure populate and verification behavior. */
    public companion object {
        /**
         * The payload key specifying whether to use device-protected (DE) storage instead of
         * default CE storage.
         *
         * The corresponding value must be a string representation of a boolean: `"true"` or
         * `"false"`.
         */
        public const val KEY_IS_DEVICE_PROTECTED: String = "is_device_protected"

        /**
         * Payload key containing the action status result.
         *
         * The value returned will be one of the status constants: [STATUS_SUCCESS] or
         * [STATUS_FAILURE]. Since this key tracks non-binary, extensible status categories (e.g.,
         * which could include future warnings or partial success info), it is represented as a
         * string rather than a simple boolean.
         */
        public const val KEY_STATUS: String = "status"

        /**
         * The payload key specifying whether the file value is base64-encoded binary data.
         *
         * The corresponding value must be a string representation of a boolean: `"true"` or
         * `"false"`.
         */
        public const val KEY_IS_BINARY: String = "is_binary"

        /**
         * The payload key for specifying the target storage medium.
         *
         * Must be set to one of the following string storage type constants:
         * - [STORAGE_TYPE_PREFS]
         * - [STORAGE_TYPE_DATABASE]
         * - [STORAGE_TYPE_FILES]
         */
        public const val KEY_STORAGE_TYPE: String = "storage_type"

        /** The payload key specifying the name of the SharedPreferences file. */
        public const val KEY_PREF_NAME: String = "pref_name"

        /** The payload key specifying the SharedPreferences key. */
        public const val KEY_PREF_KEY: String = "pref_key"

        /** The payload key specifying the string value to write to storage. */
        public const val KEY_VALUE: String = "value"

        /** The payload key specifying the SQLite database filename. */
        public const val KEY_DB_NAME: String = "db_name"

        /** The payload key specifying the table name inside the SQLite database. */
        public const val KEY_TABLE: String = "table"

        /**
         * The payload key specifying query/insert key-value pairs formatted as an
         * ampersand-separated string.
         */
        public const val KEY_VALUES: String = "values"

        /** The payload key specifying the relative or absolute file path. */
        public const val KEY_PATH: String = "path"

        /**
         * The payload key specifying the primitive type of the preference value (e.g. INT, LONG).
         */
        public const val KEY_VALUE_TYPE: String = "value_type"

        /** Storage type value indicating SharedPreferences. */
        public const val STORAGE_TYPE_PREFS: String = "PREFS"

        /** Storage type value indicating SQLite database. */
        public const val STORAGE_TYPE_DATABASE: String = "DATABASE"

        /** Storage type value indicating raw file storage. */
        public const val STORAGE_TYPE_FILES: String = "FILES"

        /** Status response indicating the action succeeded. */
        public const val STATUS_SUCCESS: String = "success"

        /** Status response indicating the action failed. */
        public const val STATUS_FAILURE: String = "failure"
    }
}
