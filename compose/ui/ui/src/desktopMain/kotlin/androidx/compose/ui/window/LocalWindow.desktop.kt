/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import java.awt.Window

/**
 * Window-owner of the current composition (for example, [ComposeWindow] or [ComposeDialog]).
 * If the composition is not inside Window (for example, [ComposePanel]), then return null
 */
internal val LocalWindow = compositionLocalOf<Window?> { null }
