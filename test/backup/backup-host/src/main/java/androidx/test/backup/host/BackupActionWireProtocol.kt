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
