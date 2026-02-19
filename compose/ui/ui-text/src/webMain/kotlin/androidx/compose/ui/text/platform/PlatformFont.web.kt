/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation

actual sealed class PlatformFont : Font {
    actual abstract val identity: String
    actual abstract val variationSettings: FontVariation.Settings
    internal actual val cacheKey: String
        // Unlike k/jvm and k/native, `this::class.qualifiedName` API is not available in k/js.
        // https://youtrack.jetbrains.com/issue/KT-34534
        // `class.qualifiedName` is supported in k/wasm since 2.3.0 - https://youtrack.jetbrains.com/issue/KT-69621
        // But for simplicity we keep using simpleName for now.
        // Reasoning:
        // Example: given LoadedFont(identity="abc", ...), it will return "LoadedFont|abc"
        // Such implementation is enough since PlatformFont is a sealed class, and
        // we control all of its variants (subclasses).
        get() = "${this::class.simpleName}|$identity|weight=${weight.weight}|style=$style|variationSettings=${variationSettings.settings}"
}
