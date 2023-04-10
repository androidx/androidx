/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.perfetto

import android.os.Build

/**
 * Enum representing set of all atrace tags used by macrobenchmark, and whether they're expected to
 * be supported on the current device.
 *
 * Note that this API assumes API >= 21, as that's the library's min API
 *
 * While supported tags could be collected from the local device (e.g. in `AtraceTagTest`), the
 * intent of this class is to track this information statically.
 */
@Suppress("unused") // enums always accessed via values()
internal enum class AtraceTag(
    val tag: String
) {
    ActivityManager("am"),
    Audio("audio") {
        override fun supported(api: Int, rooted: Boolean): Boolean {
            return api >= 23
        }
    },
    BinderDriver("binder_driver") {
        override fun supported(api: Int, rooted: Boolean): Boolean {
            return api >= 24
        }
    },
    Camera("camera"),
    Dalvik("dalvik"),
    Frequency("freq"),
    Graphics("gfx"),
    HardwareModules("hal"),
    Idle("idle"),
    Input("input"),
    MemReclaim("memreclaim") {
        override fun supported(api: Int, rooted: Boolean): Boolean {
            return rooted || api >= 24
        }
    },
    Resources("res"),
    Scheduling("sched"),
    Synchronization("sync") {
        override fun supported(api: Int, rooted: Boolean): Boolean {
            return rooted || api >= 28
        }
    },
    View("view"),
    WebView("webview"),
    WindowManager("wm");

    /**
     * Return true if the tag is available on the specified api level, with specified shell
     * session root status.
     */
    open fun supported(api: Int, rooted: Boolean): Boolean {
        return true
    }

    companion object {
        fun supported(
            api: Int = Build.VERSION.SDK_INT,
            rooted: Boolean
        ): Set<AtraceTag> {
            return values()
                .filter { it.supported(api = api, rooted = rooted) }
                .toSet()
        }

        fun unsupported(
            api: Int = Build.VERSION.SDK_INT,
            rooted: Boolean
        ): Set<AtraceTag> {
            return values().toSet() - supported(api, rooted)
        }
    }
}