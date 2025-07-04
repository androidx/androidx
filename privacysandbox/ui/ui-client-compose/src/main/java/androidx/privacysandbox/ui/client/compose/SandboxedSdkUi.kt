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

package androidx.privacysandbox.ui.client.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Composable that can be used to remotely render UI from a SandboxedSdk to host app window.
 *
 * @param sandboxedUiAdapter an adapter that provides content from a SandboxedSdk to be displayed as
 *   part of a host app's window.
 * @param modifier the [Modifier] to be applied to this SandboxedSdkUi.
 * @param providerUiOnTop sets the Z-order of the SandboxedSdkUi surface, relative to its window.
 * @param sandboxedSdkViewEventListener an event listener to the UI presentation.
 */
@Composable
// No need for @JvmOverloads as this is Kotlin only API
@Suppress("MissingJvmstatic")
// The listener relates to UI event and is expected to be triggered on the main thread.
@SuppressLint("ExecutorRegistration")
@ExperimentalFeatures.ChangingContentUiZOrderApi
public fun SandboxedSdkUi(
    sandboxedUiAdapter: SandboxedUiAdapter,
    modifier: Modifier = Modifier,
    providerUiOnTop: Boolean = false,
    sandboxedSdkViewEventListener: SandboxedSdkViewEventListener? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SandboxedSdkView(context).apply { preserveSessionOnWindowDetachment() }
        },
        update = { view ->
            view.apply {
                setEventListener(sandboxedSdkViewEventListener)
                setAdapter(sandboxedUiAdapter)
                orderProviderUiAboveClientUi(providerUiOnTop)
            }
        },
        onReset = { view -> view.setEventListener(null) },
        onRelease = { view -> view.setAdapter(null) },
    )
}

/**
 * Similar to SandboxedSdkUi(SandboxedUiAdapter, Modifier, SandboxedSdkViewEventListener?, Boolean),
 * but always set the Z-order of the provider surface below the client window.
 *
 * @see SandboxedSdkUi(SandboxedUiAdapter, Modifier, Boolean, SandboxedSdkViewEventListener?)
 */
@Composable
// No need for @JvmOverloads as this is Kotlin only API
@Suppress("MissingJvmstatic")
// The listener relates to UI event and is expected to be triggered on the main thread.
@SuppressLint("ExecutorRegistration")
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
public fun SandboxedSdkUi(
    sandboxedUiAdapter: SandboxedUiAdapter,
    modifier: Modifier = Modifier,
    sandboxedSdkViewEventListener: SandboxedSdkViewEventListener? = null,
) {
    SandboxedSdkUi(sandboxedUiAdapter, modifier, false, sandboxedSdkViewEventListener)
}
