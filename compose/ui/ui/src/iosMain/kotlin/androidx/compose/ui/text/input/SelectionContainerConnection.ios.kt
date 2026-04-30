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

package androidx.compose.ui.text.input

import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.scene.ComposeSceneFocusManager
import kotlinx.coroutines.CoroutineScope
import platform.UIKit.UIView


internal class SelectionContainerConnection(
    view: UIView,
    coroutineScope: CoroutineScope,
    viewConfiguration: ViewConfiguration,
    focusManager: () -> ComposeSceneFocusManager?
) : ComposeTextInputConnection(
    {},
    view,
    coroutineScope,
    viewConfiguration,
    null,
    {},
    focusManager
) {
    override fun stop() {
        textInputView.resignFirstResponder()
        super.stop()
    }
}
