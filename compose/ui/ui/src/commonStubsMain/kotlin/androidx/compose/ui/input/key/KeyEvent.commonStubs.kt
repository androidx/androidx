/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.key

import androidx.compose.ui.implementedInJetBrainsFork

actual class NativeKeyEvent

actual val KeyEvent.key: Key
    get() = implementedInJetBrainsFork()

actual val KeyEvent.utf16CodePoint: Int
    get() = implementedInJetBrainsFork()

actual val KeyEvent.type: KeyEventType
    get() = implementedInJetBrainsFork()

actual val KeyEvent.isAltPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val KeyEvent.isCtrlPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val KeyEvent.isMetaPressed: Boolean
    get() = implementedInJetBrainsFork()

actual val KeyEvent.isShiftPressed: Boolean
    get() = implementedInJetBrainsFork()
