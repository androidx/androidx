/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling

internal const val DP_SUFFIX = "dp"
internal const val PX_SUFFIX = "px"

/**
 * Metadata about the `spec:` device string parameters handled by [ComposeViewAdapter].
 *
 * Read by Android Studio and the standalone preview renderer from the `.class` constant pool
 * without loading the class.
 */
@Suppress("unused")
private object PreviewDeviceSpec {
    /**
     * Comma-separated list of `spec:` parameter names supported by this version of `ui-tooling`.
     */
    // Must be `const` so its value is stored in the `.class` constant pool.
    const val SUPPORTED_PARAMETERS: String =
        "windowPosture,pointerPrecision,keyboardKind,hasCamera,hasMicrophone,viewingDistance,expand,maxHeight,maxWidth,scrollTo"
}
