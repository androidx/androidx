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

@file:OptIn(ExperimentalForeignApi::class)

package androidx.compose.ui.util

import androidx.compose.ui.uikit.utils.CMPOSAppTraceLogger
import androidx.compose.ui.uikit.utils.CMPOSInitializeAppTraceLogger
import kotlinx.cinterop.ExperimentalForeignApi

// TODO: Move it to darwinMain (requires re-organization of obj-c integration)

/**
 * Enables iOS OS logging for the `androidx.compose.ui` APIs.
 *
 * Tracing allows logging detailed information about the Compose UI framework, which can be further
 * analyzed using the XCode Instruments tool.
 */
fun enableTraceOSLog() {
    CMPOSInitializeAppTraceLogger(name = "androidx.compose.ui")
}

actual inline fun <T> trace(sectionName: String, block: () -> T): T {
    val interval = CMPOSAppTraceLogger()?.beginIntervalNamed(sectionName)
    try {
        return block()
    } finally {
        interval?.let {
            CMPOSAppTraceLogger()?.endInterval(it)
        }
    }
}

actual fun traceValue(tag: String, value: Long) {
}
