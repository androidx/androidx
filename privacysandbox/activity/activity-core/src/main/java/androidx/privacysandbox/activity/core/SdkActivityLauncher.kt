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

package androidx.privacysandbox.activity.core

import android.os.IBinder

/**
 * Interface that allows SDKs running in the Privacy Sandbox to launch activities.
 *
 * Apps can create launchers by calling
 * [createActivityLauncher][androidx.privacysandbox.activity.client.createSdkActivityLauncher]
 * from one of their activities.
 *
 * To send an [SdkActivityLauncher] to another process, they can call
 * [toLauncherInfo][androidx.privacysandbox.activity.client.toLauncherInfo]
 * and send the resulting bundle.
 *
 * SDKs can create launchers from an app-provided bundle by calling
 * [fromLauncherInfo][androidx.privacysandbox.activity.provider.SdkActivityLauncherFactory.fromLauncherInfo].
 */
interface SdkActivityLauncher {

    /**
     * Tries to launch a new SDK activity using the given [sdkActivityHandlerToken].
     *
     * Returns true if the SDK activity intent was sent, false if the launch was rejected for any
     * reason.
     *
     * A valid [sdkActivityHandlerToken] can be obtained by registering an SDK activity with
     * [registerSdkSandboxActivityHandler][androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat.registerSdkSandboxActivityHandler].
     */
    suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean
}
