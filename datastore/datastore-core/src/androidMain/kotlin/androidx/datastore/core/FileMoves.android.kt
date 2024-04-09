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
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual fun File.atomicMoveTo(toFile: File): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Api26Impl.move(this, toFile)
    } else {
        renameTo(toFile)
    }
}

@RequiresApi(26)
private object Api26Impl {
    fun move(srcFile: File, dstFile: File): Boolean {
        try {
            Files.move(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (exception: IOException) {
            return false
        }
    }
}
