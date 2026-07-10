/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.datastore.core

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal actual fun File.atomicMoveTo(toFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Api26Impl.move(this, toFile)
    } else if (!renameTo(toFile)) {
        throw IOException("File rename failed.")
    }
}

@RequiresApi(26)
private object Api26Impl {
    fun move(srcFile: File, dstFile: File) {
        val source = srcFile.toPath()
        val destination = dstFile.toPath()
        try {
            // First, we attempt to perform an atomic move, which either completes successfully, or
            // it fails completely, leaving the system in its original state.
            Files.move(source, destination, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (exception: AtomicMoveNotSupportedException) {
            retryMove(source, destination, exception)
        } catch (exception: FileAlreadyExistsException) {
            // Some implementations of ATOMIC_MOVE ignore the REPLACE_EXISTING option, so we should
            // retry without the ATOMIC_MOVE option.
            retryMove(source, destination, exception)
        }
    }

    private fun retryMove(source: Path, destination: Path, previousException: Throwable) {
        try {
            Files.move(source, destination, REPLACE_EXISTING)
        } catch (exception: IOException) {
            throw exception.apply { addSuppressed(previousException) }
        }
    }
}
