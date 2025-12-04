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

@file:JvmName("WindowInsets_notMobileKt")
@file:OptIn(InternalComposeUiApi::class)

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.safeContent
import androidx.compose.ui.platform.safeDrawing
import androidx.compose.ui.platform.safeGestures
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.jvm.JvmName

// TODO: https://youtrack.jetbrains.com/issue/CMP-9379

actual val WindowInsets.Companion.captionBar: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.captionBar.toWindowInsets()

actual val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.displayCutout.toWindowInsets()

actual val WindowInsets.Companion.ime: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.ime.toWindowInsets()

actual val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.mandatorySystemGestures.toWindowInsets()

actual val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.navigationBars.toWindowInsets()

actual val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.statusBars.toWindowInsets()

actual val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.systemBars.toWindowInsets()

actual val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.systemGestures.toWindowInsets()

actual val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.tappableElement.toWindowInsets()

actual val WindowInsets.Companion.cutoutPath: Path?
    @Composable
    get() = LocalPlatformWindowInsets.current.cutoutPath

actual val WindowInsets.Companion.waterfall: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.waterfall.toWindowInsets()

actual val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.safeDrawing.toWindowInsets()

actual val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.safeGestures.toWindowInsets()

actual val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    get() = LocalPlatformWindowInsets.current.safeContent.toWindowInsets()

internal fun PlatformInsets.toWindowInsets(): WindowInsets = object : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = left
    override fun getTop(density: Density): Int = top
    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = right
    override fun getBottom(density: Density): Int = bottom
}
