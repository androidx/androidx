/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client

/**
 * A callback for tracking events SDK sandbox death.
 *
 * The callback can be added using [SdkSandboxManagerCompat.addSdkSandboxProcessDeathCallback]
 * and removed using [SdkSandboxManagerCompat.removeSdkSandboxProcessDeathCallback]
 *
 * @see [android.app.sdksandbox.SdkSandboxManager.SdkSandboxProcessDeathCallback]
 */
interface SdkSandboxProcessDeathCallbackCompat {
    /**
     * Notifies the client application that the SDK sandbox has died. The sandbox could die for
     * various reasons, for example, due to memory pressure on the system, or a crash in the
     * sandbox.
     *
     * The system will automatically restart the sandbox process if it died due to a crash.
     * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
     * would have to be loaded again, using [SdkSandboxManagerCompat.loadSdk] to continue using them.
     *
     * @see [android.app.sdksandbox.SdkSandboxManager.SdkSandboxProcessDeathCallback.onSdkSandboxDied]
     */
    fun onSdkSandboxDied()
}
