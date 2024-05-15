package com.sdkwithvalues

import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.activity.client.toLauncherInfo
import androidx.privacysandbox.activity.core.ISdkActivityLauncher
import androidx.privacysandbox.activity.core.ISdkActivityLauncherCallback
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class SdkActivityLauncherProxy(
    public val remote: ISdkActivityLauncher,
    public val launcherInfo: Bundle,
) : SdkActivityLauncher {
    public override suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean =
            suspendCancellableCoroutine {
        remote.launchSdkActivity(
            sdkActivityHandlerToken,
            object: ISdkActivityLauncherCallback.Stub() {
                override fun onLaunchAccepted(sdkActivityHandlerToken: IBinder?) {
                    it.resume(true)
                }
                override fun onLaunchRejected(sdkActivityHandlerToken: IBinder?) {
                    it.resume(true)
                }
                override fun onLaunchError(message: String?) {
                    it.resumeWithException(RuntimeException(message))
                }
            }
        )
    }
}

public object SdkActivityLauncherConverter {
    public fun getLocalOrProxyLauncher(launcherInfo: Bundle): SdkActivityLauncher {
        val remote = launcherInfo.getBinder("sdkActivityLauncherBinderKey")
        requireNotNull(remote) { "Invalid SdkActivityLauncher info bundle." }
        val binder = ISdkActivityLauncher.Stub.asInterface(remote)
        return SdkActivityLauncherProxy(binder, launcherInfo)
    }

    public fun toBinder(launcher: SdkActivityLauncher): Bundle {
        if (launcher is SdkActivityLauncherProxy) {
            return launcher.launcherInfo
        }
        return launcher.toLauncherInfo()
    }
}
