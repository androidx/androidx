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

package androidx.core.telecom.extensions

import android.os.Build.VERSION_CODES
import android.telecom.Call
import androidx.annotation.RequiresApi
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Provides the capability for a remote surface (automotive, watch, etc...) to connect to extensions
 * provided by calling applications.
 *
 * Extensions allow a calling application to support additional optional features beyond the Android
 * platform provided features defined in [Call]. When a new [Call] has been created, this interface
 * allows the remote surface to also define which extensions that it supports in its UI. If the
 * calling application providing the [Call] also supports the extension, the extension will be
 * marked as supported. At that point, the remote surface can receive state updates and send action
 * requests to the calling application to change state.
 */
public interface CallExtensions {
    /**
     * Connects extensions to the provided [call], allowing the [call] to support additional
     * optional behaviors beyond the traditional call state management provided by [Call].
     *
     * @param call The [Call] to connect extensions on.
     * @param init The scope used to initialize and manage extensions in the scope of the [Call].
     * @see CallExtensionScope
     */
    @RequiresApi(VERSION_CODES.O)
    @ExperimentalAppActions
    public suspend fun connectExtensions(call: Call, init: CallExtensionScope.() -> Unit)
}
