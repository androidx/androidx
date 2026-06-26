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

package androidx.compose.ui

import androidx.compose.ui.platform.DesktopPlatform
import kotlin.jvm.JvmField

internal object DesktopComposeUiFlags {
    @Suppress("MutableBareField")
    @JvmField
    var pollSystemTheme: Boolean = DesktopPlatform.Current != DesktopPlatform.Linux
}

/**
 * Whether the system theme should be polled to allow `isSystemInDarkTheme` to reflect the system
 * theme as it changes.
 *
 * This should be set before any Compose UI is created. Setting it afterward will have no effect
 * on existing UIs.
 *
 * Note that it's a temporary flag, it will be removed in the future.
 */
@ExperimentalComposeUiApi
var ComposeUiFlags.pollSystemTheme by DesktopComposeUiFlags::pollSystemTheme
