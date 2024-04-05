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

package androidx.compose.material

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

internal actual val KeyEvent.isDirectionUp: Boolean
    get() = key == Key.DirectionUp

internal actual val KeyEvent.isDirectionDown: Boolean
    get() = key == Key.DirectionDown

internal actual val KeyEvent.isDirectionRight: Boolean
    get() = key == Key.DirectionRight

internal actual val KeyEvent.isDirectionLeft: Boolean
    get() = key == Key.DirectionLeft

internal actual val KeyEvent.isHome: Boolean
    get() = key == Key.Home

internal actual val KeyEvent.isMoveEnd: Boolean
    get() = key == Key.MoveEnd

internal actual val KeyEvent.isPgUp: Boolean
    get() = key == Key.PageUp

internal actual val KeyEvent.isPgDn: Boolean
    get() = key == Key.PageDown
