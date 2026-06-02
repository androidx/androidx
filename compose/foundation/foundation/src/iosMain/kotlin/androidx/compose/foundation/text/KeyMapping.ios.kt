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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent
import org.jetbrains.skiko.OS

internal actual val platformDefaultKeyMapping: KeyMapping = createPlatformDefaultKeyMapping()

internal fun createPlatformDefaultKeyMapping(): KeyMapping {
    val keyMapping = createMacOsDefaultKeyMapping()
    return object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? {
            return when (val command = keyMapping.map(event)) {
                // UITextInput is used to handle clipboard events
                KeyCommand.COPY, KeyCommand.CUT, KeyCommand.PASTE, KeyCommand.SELECT_ALL -> null
                else -> command
            }
        }
    }
}