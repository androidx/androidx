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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.BenchmarkState.Companion.TAG
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Uses Java NIO APIs to move files. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
object FileMover {
    fun File.moveTo(destination: File, overwrite: Boolean = false) {
        try {
            if (overwrite) {
                destination.delete()
            }
            // Ideally we would have used File.renameTo(...)
            // On Android we cannot rename across mount points so we are using this API instead.
            Files.move(this.toPath(), destination.toPath())
        } catch (exception: IOException) {
            // Moves can fail when trying to move across mount points. This is especially true
            // for environments like FTL. In such cases, we fallback to trying to copy the file
            // instead.
            Log.w(TAG, "Unable to move $this to $destination. Attempting copy instead.", exception)
            try {
                // Sometimes copyTo can throw an exception as well. This typically happens in
                // multi-user environments where the destination is not accessible to the
                // shell user. We only want to use Shell.mv(...) in places when this is true because
                // when Shell.mv(...) is used, it also changes ownership of the file across mount
                // points.
                copyTo(target = destination, overwrite = overwrite)
            } catch (exception: IOException) {
                Log.w(
                    TAG,
                    "Unable to copy $this to $destination. Using Shell.mv(...) instead.",
                    exception,
                )
                Shell.mv(from = this.absolutePath, to = destination.absolutePath)
            }
        }
    }
}
