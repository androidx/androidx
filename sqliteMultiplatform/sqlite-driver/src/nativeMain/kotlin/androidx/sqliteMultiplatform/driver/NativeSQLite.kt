/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.sqliteMultiplatform.driver

import androidx.sqliteMultiplatform.SQLiteException
import cnames.structs.sqlite3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf16
import sqlite3.sqlite3_errmsg16

internal fun CPointer<sqlite3>.getErrorMsg(): String? {
    return sqlite3_errmsg16(this)?.reinterpret<UShortVar>()?.toKStringFromUtf16()
}

internal fun throwSQLiteException(errorCode: Int, errorMsg: String?): Nothing {
    val message = buildString {
        append("Error code: $errorCode")
        if (errorMsg != null) {
            append(", message: $errorMsg")
        }
    }
    throw SQLiteException(message)
}
