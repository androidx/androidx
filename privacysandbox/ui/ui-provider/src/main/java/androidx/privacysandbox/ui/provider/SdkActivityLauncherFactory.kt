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

package androidx.privacysandbox.ui.provider

import android.os.Bundle
import android.os.IBinder
import androidx.core.os.BundleCompat
import androidx.privacysandbox.ui.core.ISdkActivityLauncher
import androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback
import androidx.privacysandbox.ui.core.ProtocolConstants.sdkActivityLauncherBinderKey
import androidx.privacysandbox.ui.core.SdkActivityLauncher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object SdkActivityLauncherFactory {

    /**
     * Creates a [SdkActivityLauncher] using the given [launcherInfo] Bundle.
     *
     * You can create such a Bundle by calling [toLauncherInfo][androidx.privacysandbox.ui.client.toLauncherInfo].
     * A [launcherInfo] is expected to have a valid SdkActivityLauncher Binder with
     * `"sdkActivityLauncherBinderKey"` for a key, [IllegalArgumentException] is thrown otherwise.
     */
    @JvmStatic
    fun fromLauncherInfo(launcherInfo: Bundle): SdkActivityLauncher {
        val remote: ISdkActivityLauncher? = ISdkActivityLauncher.Stub.asInterface(
            BundleCompat.getBinder(launcherInfo, sdkActivityLauncherBinderKey)
        )
        requireNotNull(remote) { "Invalid SdkActivityLauncher info bundle." }
        return SdkActivityLauncherProxy(remote)
    }

    private class SdkActivityLauncherProxy(
        private val remote: ISdkActivityLauncher
    ) : SdkActivityLauncher {
        override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean =
            suspendCancellableCoroutine {
                remote.launchSdkActivity(
                    sdkActivityHandlerToken,
                    object : ISdkActivityLauncherCallback.Stub() {
                        override fun onLaunchAccepted(sdkActivityHandlerToken: IBinder?) {
                            it.resume(true)
                        }

                        override fun onLaunchRejected(sdkActivityHandlerToken: IBinder?) {
                            it.resume(false)
                        }

                        override fun onLaunchError(message: String?) {
                            it.resumeWithException(RuntimeException(message))
                        }
                    })
            }
    }
}