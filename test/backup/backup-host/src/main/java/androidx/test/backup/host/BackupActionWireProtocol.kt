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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Mirrors `androidx.test.backup.BackupActionInputKeys` for the host.
 *
 * This host library is a Kotlin/JVM artifact and the device library is an Android artifact, so the
 * host cannot link against the device-side class directly. The literals below must be changed in
 * lockstep with it; `BackupActionWireProtocolTest` and the device-side `BackupActionKeysTest` pin
 * both copies to the same strings so that a one-sided edit fails the build instead of silently
 * breaking a device run.
 */
internal object BackupActionInputKeys {
    const val STORAGE_TYPE = "storage_type"
    const val IS_DEVICE_PROTECTED = "is_device_protected"
    const val PREF_NAME = "pref_name"
    const val PREF_KEY = "pref_key"
    const val VALUE = "value"
    const val VALUE_TYPE = "value_type"
    const val DB_NAME = "db_name"
    const val TABLE = "table"
    const val VALUES = "values"
    const val KEY_COL = "key_col"
    const val KEY_VAL = "key_val"
    const val PATH = "path"
    const val IS_BINARY = "is_binary"
    const val EXPECTED = "expected"
    const val EXPECTED_COL = "expected_col"
    const val EXPECTED_VAL = "expected_val"
    const val EXPECT_NULL = "expect_null"
}

/**
 * Mirrors `androidx.test.backup.BackupActionOutputKeys` for the host.
 *
 * @see BackupActionInputKeys
 */
internal object BackupActionOutputKeys {
    const val STATUS = "status"
    const val ERROR = "error"
}

/**
 * Mirrors `androidx.test.backup.BackupActionValues` for the host.
 *
 * @see BackupActionInputKeys
 */
internal object BackupActionValues {
    const val STORAGE_TYPE_PREFS = "PREFS"
    const val STORAGE_TYPE_DATABASE = "DATABASE"
    const val STORAGE_TYPE_FILES = "FILES"
    const val VALUE_TYPE_INT = "INT"
    const val VALUE_TYPE_LONG = "LONG"
    const val VALUE_TYPE_FLOAT = "FLOAT"
    const val VALUE_TYPE_BOOLEAN = "BOOLEAN"
    const val VALUE_TYPE_STRING = "STRING"
    const val STATUS_SUCCESS = "success"
    const val STATUS_FAILURE = "failure"
}

/**
 * The arguments the host sends to the device-side runner and actions in `androidx.test.backup`, and
 * how they are encoded.
 */
internal object BackupActionWireProtocol {

    /** Class of the instrumentation that runs the actions in the app's process. */
    const val RUNNER_CLASS = "androidx.test.backup.BackupRestoreTestRunner"

    /** Returns the instrumentation component that AGP gives the test APK of [applicationId]. */
    fun runnerComponent(applicationId: String): String = "$applicationId.test/$RUNNER_CLASS"

    /**
     * Returns the instrumentation arguments that make the runner execute [actionClassName] with the
     * action [args]. When [waitForDebugger] is true, the runner waits for a debugger to attach
     * before it starts.
     *
     * @throws IllegalArgumentException if [args] has one of the keys that the runner reads itself
     */
    fun instrumentationArgs(
        actionClassName: String,
        args: Map<String, String>,
        waitForDebugger: Boolean,
    ): Map<String, String> {
        val runnerKeys = args.keys.filter { it in RUNNER_KEYS }
        require(runnerKeys.isEmpty()) {
            "Action arguments must not use the keys $runnerKeys, which the runner reads itself."
        }
        return buildMap {
            if (waitForDebugger) put(RUNNER_DEBUG, "true")
            put(RUNNER_ACTION_CLASS, actionClassName)
            put(RUNNER_REDIRECT_DIR, OVERFLOW_REDIRECT_DIR)
            putAll(args)
        }
    }

    /**
     * Returns the `PopulateStorageAction` arguments that write [storage] on the device.
     *
     * @throws IllegalArgumentException if [storage] is of an unsupported type
     */
    fun populateArgs(storage: StorageDomain): Map<String, String> =
        when (storage) {
            is StorageDomain.Preference ->
                buildMap {
                    put(BackupActionInputKeys.STORAGE_TYPE, BackupActionValues.STORAGE_TYPE_PREFS)
                    put(BackupActionInputKeys.PREF_NAME, storage.prefName)
                    put(BackupActionInputKeys.PREF_KEY, storage.key)
                    val value = storage.value
                    if (value != null) {
                        put(BackupActionInputKeys.VALUE, value.toString())
                        put(BackupActionInputKeys.VALUE_TYPE, valueTypeOf(value))
                    } else {
                        put(BackupActionInputKeys.EXPECT_NULL, "true")
                    }
                }
            is StorageDomain.Database -> {
                val columns = storage.columnValues.map { (name, value) -> name to value.orEmpty() }
                // The primary key is written too, so that verification can find the row again.
                val hasKeyColumn =
                    storage.columnValues.keys.any {
                        it.equals(storage.primaryKeyCol, ignoreCase = true)
                    }
                val keyColumn = storage.primaryKeyCol to storage.primaryKeyVal.toString()
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_DATABASE,
                    BackupActionInputKeys.DB_NAME to storage.dbName,
                    BackupActionInputKeys.TABLE to storage.table,
                    BackupActionInputKeys.VALUES to
                        encodeColumnValues(if (hasKeyColumn) columns else columns + keyColumn),
                    BackupActionInputKeys.KEY_COL to storage.primaryKeyCol,
                    BackupActionInputKeys.KEY_VAL to storage.primaryKeyVal.toString(),
                )
            }
            is StorageDomain.TextFile ->
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to storage.path,
                    BackupActionInputKeys.VALUE to storage.content,
                )
            is StorageDomain.BinaryFile ->
                mapOf(
                    BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                    BackupActionInputKeys.PATH to storage.path,
                    BackupActionInputKeys.VALUE to base64(storage.content),
                    BackupActionInputKeys.IS_BINARY to "true",
                )
            else -> throw IllegalArgumentException("Unsupported storage domain type: $storage")
        }

    /**
     * Returns the `AssertStorageAction` arguments that check that [storage] holds its value on the
     * device.
     *
     * @throws IllegalArgumentException if [storage] is of an unsupported type
     */
    fun assertArgs(storage: StorageDomain): Map<String, String> =
        populateArgs(storage) +
            when (storage) {
                // A null value already carries EXPECT_NULL from populateArgs.
                is StorageDomain.Preference ->
                    storage.value?.let { mapOf(BackupActionInputKeys.EXPECTED to it.toString()) }
                        ?: emptyMap()
                is StorageDomain.Database -> {
                    // populateArgs already carries the primary key and every column in VALUES,
                    // which the action verifies. The first column is also sent on its own, which
                    // is all that device libraries predating VALUES verification check.
                    val (expectedCol, expectedVal) = storage.columnValues.entries.first()
                    mapOf(
                        BackupActionInputKeys.EXPECTED_COL to expectedCol,
                        BackupActionInputKeys.EXPECTED_VAL to expectedVal.orEmpty(),
                    )
                }
                is StorageDomain.TextFile ->
                    mapOf(BackupActionInputKeys.EXPECTED to storage.content)
                is StorageDomain.BinaryFile ->
                    mapOf(BackupActionInputKeys.EXPECTED to base64(storage.content))
                else -> emptyMap()
            }

    /**
     * Encodes column name/value pairs into the [BackupActionInputKeys.VALUES] wire format.
     *
     * Both halves of every pair are percent-encoded in UTF-8, which is what lets a column value
     * contain `&`, `=`, `%` or `+`. The device decodes this with `java.net.URLDecoder`, so the two
     * sides must stay symmetric: writing the pairs unencoded corrupts any value containing a
     * separator.
     */
    fun encodeColumnValues(pairs: List<Pair<String, String>>): String =
        pairs.joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun valueTypeOf(value: Any): String =
        when (value) {
            is Int -> BackupActionValues.VALUE_TYPE_INT
            is Long -> BackupActionValues.VALUE_TYPE_LONG
            is Float -> BackupActionValues.VALUE_TYPE_FLOAT
            is Boolean -> BackupActionValues.VALUE_TYPE_BOOLEAN
            else -> BackupActionValues.VALUE_TYPE_STRING
        }

    /** A database cell value as the device reads it; null is sent as an empty string. */
    private fun Any?.orEmpty(): String = this?.toString() ?: ""

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    // Instrumentation arguments read by `androidx.test.backup.BackupRestoreTestRunner`, which it
    // does not pass on to the action.
    private const val RUNNER_DEBUG = "debug"
    private const val RUNNER_ACTION_CLASS = "actionClass"
    private const val RUNNER_REDIRECT_DIR = "redirect_dir"
    private val RUNNER_KEYS = setOf(RUNNER_DEBUG, RUNNER_ACTION_CLASS, RUNNER_REDIRECT_DIR)

    /** Device directory the runner may write a payload to when it is too large to print. */
    private const val OVERFLOW_REDIRECT_DIR = "/data/local/tmp"
}
