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

import androidx.compose.foundation.DesktopPlatform

internal actual val platformDefaultKeyMapping: KeyMapping
    get() = overriddenDefaultKeyMapping ?: _platformDefaultKeyMapping

/**
 * Used for testing purposes only
 */
internal var overriddenDefaultKeyMapping: KeyMapping? = null
private val _platformDefaultKeyMapping: KeyMapping =
    createPlatformDefaultKeyMapping(DesktopPlatform.Current)

internal fun createPlatformDefaultKeyMapping(platform: DesktopPlatform): KeyMapping {
    return when (platform) {
        DesktopPlatform.MacOS -> createMacosDefaultKeyMapping()
        else -> defaultSkikoKeyMapping
    }
}
