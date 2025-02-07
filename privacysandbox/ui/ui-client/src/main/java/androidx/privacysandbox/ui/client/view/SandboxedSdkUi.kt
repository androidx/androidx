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
package androidx.privacysandbox.ui.client.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Composable that can be used to remotely render UI from a SandboxedSdk to host app window.
 *
 * @param sandboxedUiAdapter an adapter that provides content from a SandboxedSdk to be displayed as
 *   part of a host app's window.
 * @param modifier the [Modifier] to be applied to this SandboxedSdkUi.
 * @param providerUiOnTop sets the Z-ordering of the SandboxedSdkUi surface, relative to its window.
 * @param sandboxedSdkViewEventListener an event listener to the UI presentation.
 */
@Composable
@Suppress("MissingJvmstatic")
fun SandboxedSdkUi(
    sandboxedUiAdapter: SandboxedUiAdapter,
    modifier: Modifier = Modifier,
    providerUiOnTop: Boolean = true,
    sandboxedSdkViewEventListener: SandboxedSdkViewEventListener? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> SandboxedSdkView(context).apply { isInComposeNode = true } },
        update = { view ->
            view.apply {
                setEventListener(sandboxedSdkViewEventListener)
                setAdapter(sandboxedUiAdapter)
                orderProviderUiAboveClientUi(providerUiOnTop)
            }
        },
        onReset = { view -> view.setEventListener(null) },
        onRelease = { view -> view.closeClient() }
    )
}
