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

package androidx.sqliteMultiplatform.unbundled
import androidx.sqlite3.unbundled.SQLITE_OK
import androidx.sqlite3.unbundled.SQLITE_ROW
import androidx.sqlite3.unbundled.sqlite3_column_text
import androidx.sqlite3.unbundled.sqlite3_open
import androidx.sqlite3.unbundled.sqlite3_prepare_v2
import androidx.sqlite3.unbundled.sqlite3_step
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.utf8
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
actual class SimpleDatabaseSubject {
    actual fun openDatabaseAndReadVersion(): String {
        val dbPtr = nativeHeap.allocPointerTo<sqlite3>()
        val openResult = sqlite3_open(":memory:", dbPtr.ptr)
        check(openResult == SQLITE_OK) {
            "failed to open database"
        }
        val stmtPtr = nativeHeap.allocPointerTo<sqlite3_stmt>()
        val query = "select sqlite_version();"
        memScoped {
            sqlite3_prepare_v2(dbPtr.value, query.utf8, -1, stmtPtr.ptr, null)
            if (sqlite3_step(stmtPtr.value) == SQLITE_ROW) {
                val textPtr: CPointer<UByteVar> =
                    sqlite3_column_text(stmtPtr.value, 0) ?: error("no text")
                // TODO free C data
                return textPtr.reinterpret<ByteVar>().toKStringFromUtf8()
            }
        }
        error("cannot read database version")
    }
}
