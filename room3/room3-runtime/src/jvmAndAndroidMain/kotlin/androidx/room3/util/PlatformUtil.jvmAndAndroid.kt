/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.util

import java.io.File

internal actual fun ensureParentDirectoryExists(fileName: String) {
    val file = File(fileName)
    file.parentFile?.mkdirs()
}

internal actual fun deleteDatabaseFiles(fileName: String): Boolean {
    var deleted = false
    for (postfix in arrayOf("", "-wal", "-shm", "-journal")) {
        val dbFile = File(fileName + postfix)
        if (dbFile.exists()) {
            deleted = dbFile.delete() || deleted
        }
    }
    return deleted
}
