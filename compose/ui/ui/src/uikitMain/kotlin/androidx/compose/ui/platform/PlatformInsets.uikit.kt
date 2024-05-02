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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.dp

/**
 * Composition local for SafeArea of ComposeUIViewController
 */
@InternalComposeUiApi
val LocalSafeArea = staticCompositionLocalOf { PlatformInsets.Zero }

/**
 * Composition local for layoutMargins of ComposeUIViewController
 */
@InternalComposeUiApi
val LocalLayoutMargins = staticCompositionLocalOf { PlatformInsets.Zero }

private object SafeAreaInsetsConfig : InsetsConfig {
    override val safeInsets: PlatformInsets
        @Composable get() = LocalSafeArea.current

    override val ime: PlatformInsets
        @Composable get() = PlatformInsets(bottom = LocalKeyboardOverlapHeight.current)

    @Composable
    override fun excludeInsets(
        safeInsets: Boolean,
        ime: Boolean,
        content: @Composable () -> Unit
    ) {
        val safeArea = LocalSafeArea.current
        val layoutMargins = LocalLayoutMargins.current
        val keyboardOverlapHeight = LocalKeyboardOverlapHeight.current
        CompositionLocalProvider(
            LocalSafeArea provides if (safeInsets) PlatformInsets() else safeArea,
            LocalLayoutMargins provides if (safeInsets) layoutMargins.exclude(safeArea) else layoutMargins,
            LocalKeyboardOverlapHeight provides if (ime) 0.dp else keyboardOverlapHeight,
            content = content
        )
    }
}

internal actual var PlatformInsetsConfig: InsetsConfig = SafeAreaInsetsConfig
