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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources

@Immutable
@JvmInline
internal actual value class ContextMenuStrings actual constructor(actual val value: Int) {
    actual companion object {
        actual val Cut: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.cut)

        actual val Copy: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.copy)

        actual val Paste: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.paste)

        actual val SelectAll: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.selectAll)

        actual val Autofill: ContextMenuStrings
            get() =
                ContextMenuStrings(
                    if (Build.VERSION.SDK_INT <= 26) {
                        R.string.autofill
                    } else {
                        android.R.string.autofill
                    }
                )
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: ContextMenuStrings): String {
    val resources = LocalResources.current
    return resources.getString(string.value)
}
