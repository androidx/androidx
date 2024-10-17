/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import kotlinx.coroutines.flow.MutableSharedFlow

internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    updateSelectionState: (() -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?
): Nothing = implementedInJetBrainsFork()
