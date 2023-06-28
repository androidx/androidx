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

@file:JvmName("SdkActivityLaunchers")

package androidx.privacysandbox.ui.client

import android.app.Activity
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.ui.core.ISdkActivityLauncher
import androidx.privacysandbox.ui.core.ISdkActivityLauncherCallback
import androidx.privacysandbox.ui.core.ProtocolConstants.sdkActivityLauncherBinderKey
import androidx.privacysandbox.ui.core.SdkActivityLauncher
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Returns an SdkActivityLauncher that launches activities on behalf of an SDK by using this
 * activity as a starting context.
 *
 * @param T the current activity from which new SDK activities will be launched. If this activity is
 * destroyed any further SDK activity launches will simply be ignored.
 * @param allowLaunch predicate called each time an activity is about to be launched by the
 * SDK, the activity will only be launched if it returns true.
 */
fun <T> T.createSdkActivityLauncher(
    allowLaunch: () -> Boolean
): LocalSdkActivityLauncher<T>
    where T : Activity, T : LifecycleOwner {
    val cancellationJob = Job(parent = lifecycleScope.coroutineContext[Job])
    val launcher = LocalSdkActivityLauncherImpl(
        activity = this,
        allowLaunch = allowLaunch,
        onDispose = { cancellationJob.cancel() },
    )
    cancellationJob.invokeOnCompletion {
        launcher.dispose()
    }
    return launcher
}

/**
 * Returns a [Bundle] with the information necessary to recreate this launcher.
 * Possibly in a different process.
 */
fun SdkActivityLauncher.toLauncherInfo(): Bundle {
    val binderDelegate = SdkActivityLauncherBinderDelegate(this)
    return Bundle().also { bundle ->
        BundleCompat.putBinder(bundle, sdkActivityLauncherBinderKey, binderDelegate)
    }
}

/**
 * Local implementation of an SDK Activity launcher.
 *
 * It allows callers in the app process to dispose resources used to launch SDK activities.
 */
interface LocalSdkActivityLauncher<T> : SdkActivityLauncher where T : Activity, T : LifecycleOwner {
    /**
     * Clears references used to launch activities.
     *
     * After this method is called all further attempts to launch activities wil be rejected.
     * Doesn't do anything if the launcher was already disposed of.
     */
    fun dispose()
}

private class LocalSdkActivityLauncherImpl<T>(
    activity: T,
    allowLaunch: () -> Boolean,
    onDispose: () -> Unit
) : LocalSdkActivityLauncher<T> where T : Activity, T : LifecycleOwner {

    /** Internal state for [LocalSdkActivityLauncher], cleared when the launcher is disposed. */
    private class LocalLauncherState<T>(
        val activity: T,
        val allowLaunch: () -> Boolean,
        val sdkSandboxManager: SdkSandboxManagerCompat,
        val onDispose: () -> Unit
    ) where T : Activity, T : LifecycleOwner

    private val stateReference: AtomicReference<LocalLauncherState<T>?> =
        AtomicReference<LocalLauncherState<T>?>(
        LocalLauncherState(
            activity,
            allowLaunch,
            SdkSandboxManagerCompat.from(activity),
            onDispose
        )
    )

    override suspend fun launchSdkActivity(
        sdkActivityHandlerToken: IBinder
    ): Boolean {
        val state = stateReference.get() ?: return false
        return withContext(Dispatchers.Main.immediate) {
            state.run {
                allowLaunch().also { didAllowLaunch ->
                    if (didAllowLaunch) {
                        sdkSandboxManager.startSdkSandboxActivity(activity, sdkActivityHandlerToken)
                    }
                }
            }
        }
    }

    override fun dispose() {
        stateReference.getAndSet(null)?.run {
            onDispose()
        }
    }
}

private class SdkActivityLauncherBinderDelegate(private val launcher: SdkActivityLauncher) :
    ISdkActivityLauncher.Stub() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun launchSdkActivity(
        sdkActivityHandlerToken: IBinder?,
        callback: ISdkActivityLauncherCallback?
    ) {
        requireNotNull(sdkActivityHandlerToken)
        requireNotNull(callback)

        coroutineScope.launch {
            val accepted = try {
                launcher.launchSdkActivity(sdkActivityHandlerToken)
            } catch (t: Throwable) {
                callback.onLaunchError(t.message ?: "Unknown error launching SDK activity.")
                return@launch
            }

            if (accepted) {
                callback.onLaunchAccepted(sdkActivityHandlerToken)
            } else {
                callback.onLaunchRejected(sdkActivityHandlerToken)
            }
        }
    }
}