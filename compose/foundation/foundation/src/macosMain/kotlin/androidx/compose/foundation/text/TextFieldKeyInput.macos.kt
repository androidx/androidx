/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

actual val KeyEvent.isTypedEvent: Boolean
    get() = type == KeyEventType.KeyDown &&
        !isISOControl(utf16CodePoint) &&
        !isAppKitReserved(utf16CodePoint)

private fun isISOControl(codePoint: Int): Boolean =
    codePoint in 0x00..0x1F ||
    codePoint in 0x7F..0x9F

// https://www.unicode.org/Public/MAPPINGS/VENDORS/APPLE/CORPCHAR.TXT
private fun isAppKitReserved(codePoint: Int): Boolean =
    codePoint in 0xF700..0xF8FF
