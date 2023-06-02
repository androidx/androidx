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

package androidx.benchmark

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.File
import java.nio.file.Files

/**
 * Uses Java NIO APIs to move files.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
object FileMover {
    fun File.moveTo(destination: File) {
        // Ideally we would have used File.renameTo(...)
        // On Android we cannot rename across mount points so we are using this API instead.
        Files.move(this.toPath(), destination.toPath())
    }
}
