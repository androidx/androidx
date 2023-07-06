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
package androidx.privacysandbox.sdkruntime.core

import android.content.Context
import android.os.Bundle
import android.view.View

/**
 * Compat version of [android.app.sdksandbox.SandboxedSdkProvider].
 *
 * Encapsulates API which SDK sandbox can use to interact with SDKs loaded into it.
 *
 * SDK has to implement this abstract class to generate an entry point for SDK sandbox to be able
 *  to call it through.
 *
 * @see [android.app.sdksandbox.SandboxedSdkProvider]
 */
abstract class SandboxedSdkProviderCompat {
    /**
     * Context previously set through [SandboxedSdkProviderCompat.attachContext].
     * This will return null if no context has been previously set.
     */
    var context: Context? = null
        private set

    /**
     * Sets the SDK [Context] which can then be received using [SandboxedSdkProviderCompat.context]
     *
     * This is called before [SandboxedSdkProviderCompat.onLoadSdk] is invoked.
     * No operations requiring a [Context] should be performed before then, as
     * [SandboxedSdkProviderCompat.context] will return null until this method has been called.
     *
     * @throws IllegalStateException if a base context has already been set.
     *
     * @param context The new base context.
     *
     * @see [android.app.sdksandbox.SandboxedSdkProvider.attachContext]
     */
    fun attachContext(context: Context) {
        check(this.context == null) { "Context already set" }
        this.context = context
    }

    /**
     * Does the work needed for the SDK to start handling requests.
     *
     * This function is called by the SDK sandbox after it loads the SDK.
     *
     * SDK should do any work to be ready to handle upcoming requests. It should not do any
     * long-running tasks here, like I/O and network calls. Doing so can prevent the SDK from
     * receiving requests from the client. Additionally, it should not do initialization that
     * depends on other SDKs being loaded into the SDK sandbox.
     *
     * The SDK should not do any operations requiring a [Context] object before this method
     * has been called.
     *
     * @param params list of params passed from the client when it loads the SDK. This can be empty.
     * @return Returns a [SandboxedSdkCompat], passed back to the client. The IBinder used to create
     * the [SandboxedSdkCompat] object will be used by the client to call into the SDK.
     *
     * @throws LoadSdkCompatException if initialization failed.
     *
     * @see [android.app.sdksandbox.SandboxedSdkProvider.onLoadSdk]
     */
    @Throws(LoadSdkCompatException::class)
    abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat

    /**
     * Does the work needed for the SDK to free its resources before being unloaded.
     *
     * This function is called by the SDK sandbox manager before it unloads the SDK. The SDK
     * should fail any invocations on the Binder previously returned to the client through
     * [SandboxedSdkCompat.getInterface]
     *
     * The SDK should not do any long-running tasks here, like I/O and network calls.
     *
     * @see [android.app.sdksandbox.SandboxedSdkProvider.beforeUnloadSdk]
     */
    open fun beforeUnloadSdk() {}

    /**
     * Requests a view to be remotely rendered to the client app process.
     *
     * @see [android.app.sdksandbox.SandboxedSdkProvider.getView]
     */
    abstract fun getView(
        windowContext: Context,
        params: Bundle,
        width: Int,
        height: Int
    ): View
}
