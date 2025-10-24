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

package androidx.tracing.driver

import android.os.Build
import android.os.Process

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun TraceContext.currentProcessTrack(): ProcessTrack {
    val id = Process.myPid()
    return processes.getOrElse(id) {
        val name =
            if (Build.VERSION.SDK_INT >= 33) {
                Process.myProcessName()
            } else {
                // Only used in the context of tests where the default process track is not already
                // bootstrapped.
                "Process pid($id)"
            }
        getOrCreateProcessTrack(id = id, name = name)
    }
}
