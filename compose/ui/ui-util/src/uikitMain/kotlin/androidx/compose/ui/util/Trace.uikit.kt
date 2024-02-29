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

package androidx.compose.ui.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.uikit.utils.CMPOSLogger

/**
 * Enables iOS OS logging for the `androidx.compose.ui` APIs.
 *
 * Tracing allows logging detailed information about the Compose UI framework, which can be further
 * analyzed using the XCode Instruments tool.
 *
 * This method is an [ExperimentalComposeUiApi], which means it is subject to change without notice in major, minor, or patch releases.
 *
 * @see ExperimentalComposeUiApi
 */
@OptIn(InternalComposeUiApi::class)
@ExperimentalComposeUiApi
fun enableTraceOSLog() {
    if (traceImpl == null) {
        traceImpl = CMPOSLogger(categoryName = "androidx.compose.ui")
    }
}

/**
 * Instance of the [CMPOSLogger] used for tracing. Public due to `inline` requirement of [trace].
 * Intended for internal use only.
 */
@InternalComposeUiApi
var traceImpl: CMPOSLogger? = null

@OptIn(InternalComposeUiApi::class)
actual inline fun <T> trace(sectionName: String, block: () -> T): T {
    val interval = traceImpl?.beginIntervalNamed(sectionName)
    try {
        return block()
    } finally {
        interval?.let {
            traceImpl?.endInterval(it)
        }
    }
}