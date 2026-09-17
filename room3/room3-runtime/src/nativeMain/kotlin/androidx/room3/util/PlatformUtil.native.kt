/*
 * Copyright 2025 The Android Open Source Project
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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.posix.mkdir
import platform.posix.remove

internal actual val platform = PlatformType.NATIVE

internal actual val defaultQueryDispatcher: CoroutineDispatcher = Dispatchers.IO

// 0777 octal (rwxrwxrwx); standard default directory permissions masked by process umask
private const val DEFAULT_DIR_MODE: Int = 0b111_111_111

// UnsafeNumber is needed because POSIX mode_t has different bit widths across platforms
// (16-bit on Darwin vs 32-bit on Linux). Converting DEFAULT_DIR_MODE (511 / 0777 octal)
// via convert() is safe since it requires only 9 bits and fits within all platform widths.
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun ensureParentDirectoryExists(fileName: String) {
    val separatorIndex = fileName.lastIndexOf('/')
    if (separatorIndex <= 0) return
    val parentDir = fileName.substring(0, separatorIndex)

    // Standard POSIX library does not provide creating nested directories, so we have to do it
    // manually.
    var index = 0
    while (index < parentDir.length) {
        index = parentDir.indexOf('/', startIndex = index + 1)
        val path = if (index == -1) parentDir else parentDir.substring(0, index)
        if (path.isNotEmpty() && path != "/") {
            mkdir(path, DEFAULT_DIR_MODE.convert())
        }
        if (index == -1) break
    }
}

internal actual fun deleteDatabaseFiles(fileName: String): Boolean {
    var deleted = false
    for (postfix in arrayOf("", "-wal", "-shm", "-journal")) {
        if (remove(fileName + postfix) == 0) {
            deleted = true
        }
    }
    return deleted
}
