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

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline


//TODO: remove this file
// when this will be in JB fork
// https://android.googlesource.com/platform/frameworks/support/+/d8bc9d81dffa35162626e45ee68d4a7e271c6ada

@Immutable
@JvmInline
internal value class ContextMenuIcons(val value: Int) {
    companion object {
        val ActionModeCutDrawable = ContextMenuIcons(0)
        val ActionModeCopyDrawable = ContextMenuIcons(1)
        val ActionModePasteDrawable = ContextMenuIcons(2)
        val ActionModeSelectAllDrawable = ContextMenuIcons(3)
        val ID_NULL = ContextMenuIcons(-1)
    }
}