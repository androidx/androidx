/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

internal actual val platformDefaultKeyMapping: KeyMapping = createPlatformDefaultKeyMapping(hostOs)

internal fun createPlatformDefaultKeyMapping(platform: OS): KeyMapping {
    val keyMapping = when (platform) {
        OS.MacOS -> createMacosDefaultKeyMapping()
        else -> defaultKeyMapping
    }
    return object : KeyMapping {
        private val clipboardKeys = setOf(Key.C, Key.V, Key.X)

        override fun map(event: KeyEvent): KeyCommand? {
            val isCtrlOrCmd = if (hostOs.isMacOS) event.isMetaPressed else event.isCtrlPressed
            if (isCtrlOrCmd && event.key in clipboardKeys) {
                // we let a browser dispatch a clipboard event
                return null
            }
            return keyMapping.map(event)
        }
    }
}