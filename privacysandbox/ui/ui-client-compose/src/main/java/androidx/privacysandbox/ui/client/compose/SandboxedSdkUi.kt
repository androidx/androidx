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

@file:Suppress(
    "FacadeClassJvmName",
    "DEPRECATION",
    "DEPRECATED_JAVA_ANNOTATION",
) // TODO(b/444198856): add a jvmname
@file:JvmDeprecated

package androidx.privacysandbox.ui.client.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.lang.Deprecated as JvmDeprecated

/**
 * Composable that can be used to remotely render UI from a SandboxedSdk to host app window.
 *
 * @param sandboxedUiAdapter an adapter that provides content from a SandboxedSdk to be displayed as
 *   part of a host app's window.
 * @param modifier the [Modifier] to be applied to this SandboxedSdkUi.
 * @param providerUiOnTop sets the Z-order of the SandboxedSdkUi surface, relative to its host
 *   window. True means on top, while false means below (it is false if it is not specified).
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
 * Composable that can be used to remotely render UI from a SandboxedSdk to host app window.
 *
 * It always sets the Z-order of the remote content surface below its host window.
 *
 * @param sandboxedUiAdapter an adapter that provides content from a SandboxedSdk to be displayed as
 *   part of a host app's window.
 * @param modifier the [Modifier] to be applied to this SandboxedSdkUiBelowHostWindow.
 * @param sandboxedSdkViewEventListener an event listener to the UI presentation.
 * @deprecated This library is no longer supported.
 */
@Deprecated("This library is no longer supported.")
@Composable
// No need for @JvmOverloads as this is Kotlin only API
@Suppress("MissingJvmstatic")
// The listener relates to UI event and is expected to be triggered on the main thread.
@SuppressLint("ExecutorRegistration")
public fun SandboxedSdkUiBelowHostWindow(
    sandboxedUiAdapter: SandboxedUiAdapter,
    modifier: Modifier = Modifier,
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
            }
        },
        onReset = { view -> view.setEventListener(null) },
        onRelease = { view -> view.setAdapter(null) },
    )
}
