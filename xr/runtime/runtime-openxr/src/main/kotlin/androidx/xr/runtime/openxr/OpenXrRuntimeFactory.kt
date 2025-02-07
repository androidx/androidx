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

package androidx.xr.runtime.openxr

import android.app.Activity
import androidx.xr.runtime.internal.Runtime
import androidx.xr.runtime.internal.RuntimeFactory

/** Factory for creating instances of [OpenXrRuntime]. */
public class OpenXrRuntimeFactory() : RuntimeFactory {
    public companion object {
        init {
            try {
                System.loadLibrary("androidx.xr.runtime.openxr")
            } catch (e: UnsatisfiedLinkError) {
                // TODO: b/344962771 - Use Flogger instead of println.
                println("Failed to load library: $e")
            }
        }
    }

    override fun createRuntime(activity: Activity): Runtime {
        val timeSource = OpenXrTimeSource()
        val perceptionManager = OpenXrPerceptionManager(timeSource)
        return OpenXrRuntime(
            OpenXrManager(activity, perceptionManager, timeSource),
            perceptionManager
        )
    }
}
