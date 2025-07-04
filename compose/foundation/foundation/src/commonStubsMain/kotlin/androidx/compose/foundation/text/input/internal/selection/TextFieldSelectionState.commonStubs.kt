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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier = implementedInJetBrainsFork()

internal actual class ClipboardPasteState actual constructor(clipboard: Clipboard) {
    actual val hasText: Boolean = implementedInJetBrainsFork()
    actual val hasClip: Boolean = implementedInJetBrainsFork()

    actual suspend fun update() {
        implementedInJetBrainsFork()
    }
}
