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

package androidx.compose.ui.text

import kotlin.jvm.JvmField

/**
 * Flags that guard new code paths in this module. These flags are on by default. Apps can turn them
 * off if a new path causes a bug.
 *
 * These flags are short-term. Do not rely on them staying around. If turning a flag off fixes a
 * bug, please file an issue right away.
 *
 * **Usage:**
 *
 * In a debug build, set a flag to `false` as early as possible when the app starts. Changing it
 * after Compose text code loads can cause bugs.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              ComposeUiTextFlags.isPackedTextStylesEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In a release build, R8 rules can also lock the flag for the whole build. This lets R8 strip the
 * unused code path from the app.
 *
 *      -assumevalues class androidx.compose.ui.text.ComposeUiTextFlags {
 *          public static boolean isPackedTextStylesEnabled return false
 *      }
 */
@ExperimentalTextApi
public object ComposeUiTextFlags {

    /**
     * When `true`, [SpanStyle], [ParagraphStyle], [TextStyle], and [AnnotatedString] use a compact
     * memory format. When `false`, these classes use their old format.
     *
     * Do not mix objects built before and after changing this flag. Set this flag in
     * `Application.onCreate` before any [TextStyle], [SpanStyle], [ParagraphStyle], or
     * [AnnotatedString] is used.
     */
    // TODO(b/576496490): Remove isPackedTextStylesEnabled after soak
    @field:Suppress("MutableBareField")
    @JvmField
    public var isPackedTextStylesEnabled: Boolean = true
}

/**
 * Locks in [ComposeUiTextFlags.isPackedTextStylesEnabled] when Compose text code first checks the
 * flag. This stops the mode from changing while the app runs.
 */
@OptIn(ExperimentalTextApi::class)
internal object PackedTextStylesLatch {
    @JvmField internal val isEnabled: Boolean = ComposeUiTextFlags.isPackedTextStylesEnabled
}
