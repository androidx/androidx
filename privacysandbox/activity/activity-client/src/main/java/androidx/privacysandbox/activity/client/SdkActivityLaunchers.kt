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

package androidx.privacysandbox.activity.client

import android.app.Activity
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.activity.core.ISdkActivityLauncher
import androidx.privacysandbox.activity.core.ISdkActivityLauncherCallback
import androidx.privacysandbox.activity.core.ProtocolConstants.SDK_ACTIVITY_LAUNCHER_BINDER_KEY
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Returns a lifecycle-aware SdkActivityLauncher that launches activities on behalf of an SDK by
 * using this activity as a starting context. The created launcher will be automatically disposed
 * when the current activity is destroyed, with no additional work required.
 *
 * @param T the current [LifecycleOwner] activity from which new SDK activities will be launched. If
 *   this activity is destroyed, any further SDK activity launches will simply be ignored, and
 *   [LocalManagedSdkActivityLauncher.launchSdkActivity] will return 'false'.
 * @param allowLaunch predicate called each time an activity is about to be launched by the SDK, the
 *   activity will only be launched if it returns true.
 */
public fun <T> T.createManagedSdkActivityLauncher(
    allowLaunch: () -> Boolean
): LocalManagedSdkActivityLauncher<T> where T : Activity, T : LifecycleOwner {
    val cancellationJob = Job(parent = lifecycleScope.coroutineContext[Job])
    val launcher =
        LocalManagedSdkActivityLauncher(
            activity = this,
            allowLaunch = allowLaunch,
            onDispose = { cancellationJob.cancel() },
        )
    cancellationJob.invokeOnCompletion { launcher.dispose() }
    return launcher
}

/**
 * Returns a lifecycle-unaware SdkActivityLauncher that launches activities on behalf of an SDK by
 * using this activity as a starting context. The created launcher will need to be manually disposed
 * explicitly by the caller.
 *
 * It is recommended to use a lifecycle-aware launcher created using
 * [createManagedSdkActivityLauncher] instead. It is automatically disposed when the current
 * activity is destroyed, making it less prone to memory leaks.
 *
 * @param T the current activity from which new SDK activities will be launched. If this activity is
 *   destroyed any further SDK activity launches will simply be ignored, and
 *   [LocalUnmanagedSdkActivityLauncher.launchSdkActivity] will return 'false'.
 * @param allowLaunch predicate called each time an activity is about to be launched by the SDK, the
 *   activity will only be launched if it returns true.
 * @see [createManagedSdkActivityLauncher]
 */
public fun <T> T.createUnmanagedSdkActivityLauncher(
    allowLaunch: () -> Boolean
): LocalUnmanagedSdkActivityLauncher<T> where T : Activity {
    return LocalUnmanagedSdkActivityLauncher(activity = this, allowLaunch = allowLaunch)
}

/**
 * Returns a [Bundle] with the information necessary to recreate this launcher. Possibly in a
 * different process.
 */
public fun SdkActivityLauncher.toLauncherInfo(): Bundle {
    val binderDelegate = SdkActivityLauncherBinderDelegate(this)
    return Bundle().also { bundle ->
        bundle.putBinder(SDK_ACTIVITY_LAUNCHER_BINDER_KEY, binderDelegate)
    }
}

/**
 * Local version of [SdkActivityLauncher] that allows callers in the app process to dispose the
 * launcher resources.
 *
 * @see LocalManagedSdkActivityLauncher
 * @see LocalUnmanagedSdkActivityLauncher
 */
public interface LocalSdkActivityLauncher : SdkActivityLauncher {
    /**
     * Clears references used to launch activities.
     *
     * After this method is called, all further attempts to launch activities wil be rejected, and
     * [LocalSdkActivityLauncher.launchSdkActivity] will return 'false'.
     *
     * Doesn't do anything if the launcher was already disposed of.
     */
    public fun dispose()
}

/**
 * Local implementation of a lifecycle-aware SDK Activity launcher. Its resources will be
 * automatically disposed when its parent activity is destroyed. In this case, no additional work
 * will be required from the caller.
 *
 * It allows callers in the app process to dispose resources used to launch SDK activities.
 */
public class LocalManagedSdkActivityLauncher<T>
internal constructor(activity: T, allowLaunch: () -> Boolean, onDispose: () -> Unit) :
    LocalSdkActivityLauncher where T : Activity, T : LifecycleOwner {
    private val launcherDelegate =
        LocalSdkActivityLauncherDelegate(activity, allowLaunch, onDispose)

    /** @see [SdkActivityLauncher.launchSdkActivity] */
    override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean =
        launcherDelegate.launchSdkActivity(sdkActivityHandlerToken)

    /**
     * Clears references used to launch activities. This method __doesn't need__ to be called as
     * system will automatically dispose the launcher when the parent activity is destroyed.
     * However, the user __can__ optionally call it the launcher is not needed anymore.
     *
     * After this method is called all further attempts to launch activities wil be rejected, and
     * [LocalManagedSdkActivityLauncher.launchSdkActivity] will return 'false'.
     *
     * Doesn't do anything if the launcher was already disposed of.
     */
    override fun dispose(): Unit = launcherDelegate.dispose()
}

/**
 * Local implementation of an SDK Activity launcher. This launcher is not lifecycle-aware, meaning
 * its resources need to be released manually by the caller to avoid memory leaks.
 *
 * @see [LocalManagedSdkActivityLauncher]
 */
public class LocalUnmanagedSdkActivityLauncher<T>
internal constructor(activity: T, allowLaunch: () -> Boolean) : LocalSdkActivityLauncher where
T : Activity {

    private val launcherDelegate = LocalSdkActivityLauncherDelegate(activity, allowLaunch)

    /** @see [SdkActivityLauncher.launchSdkActivity] */
    override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean =
        launcherDelegate.launchSdkActivity(sdkActivityHandlerToken)

    /**
     * Clears references used to launch activities. This method __must__ be called once the launcher
     * is not needed anymore to avoid memory leaks.
     *
     * After this method is called all further attempts to launch activities wil be rejected, and
     * [LocalUnmanagedSdkActivityLauncher.launchSdkActivity] will return 'false'.
     *
     * Doesn't do anything if the launcher was already disposed of.
     */
    override fun dispose(): Unit = launcherDelegate.dispose()
}

private class LocalSdkActivityLauncherDelegate<T>(
    activity: T,
    allowLaunch: () -> Boolean,
    onDispose: (() -> Unit)? = null,
) : LocalSdkActivityLauncher where T : Activity {
    /**
     * Internal state for [LocalManagedSdkActivityLauncher], cleared when the launcher is disposed.
     */
    private class LocalLauncherState<T>(
        val activity: T,
        val allowLaunch: () -> Boolean,
        val sdkSandboxManager: SdkSandboxManagerCompat,
        val onDispose: (() -> Unit)?,
    ) where T : Activity

    private val stateReference: AtomicReference<LocalLauncherState<T>?> =
        AtomicReference<LocalLauncherState<T>?>(
            LocalLauncherState(
                activity,
                allowLaunch,
                SdkSandboxManagerCompat.from(activity),
                onDispose,
            )
        )

    override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean {
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
        stateReference.getAndSet(null)?.run { onDispose?.invoke() }
    }
}

private class SdkActivityLauncherBinderDelegate(private val launcher: SdkActivityLauncher) :
    ISdkActivityLauncher.Stub() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun launchSdkActivity(
        sdkActivityHandlerToken: IBinder?,
        callback: ISdkActivityLauncherCallback?,
    ) {
        requireNotNull(sdkActivityHandlerToken)
        requireNotNull(callback)

        coroutineScope.launch {
            val accepted =
                try {
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
