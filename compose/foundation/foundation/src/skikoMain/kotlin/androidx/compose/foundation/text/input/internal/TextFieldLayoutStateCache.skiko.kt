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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.intl.PlatformLocale
import androidx.compose.ui.text.style.TextDirection

internal actual fun resolveTextDirectionForKeyboardTypePhone(
    locale: PlatformLocale
): TextDirection {
    // TODO implement resolveTextDirectionForKeyboardTypePhone
    //  Added in a0f82d5c7de2155a1f144bd606d9eea32659a9d7
    return TextDirection.Ltr
}
