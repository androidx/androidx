package com.mysdk

import android.os.Bundle
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.activity.provider.SdkActivityLauncherFactory

public class SdkActivityLauncherAndBinderWrapper private constructor(
    private val `delegate`: SdkActivityLauncher,
    public val launcherInfo: Bundle,
) : SdkActivityLauncher by delegate {
    public constructor(launcherInfo: Bundle) :
            this(SdkActivityLauncherFactory.fromLauncherInfo(launcherInfo), launcherInfo)

    public companion object {
        public fun getLauncherInfo(launcher: SdkActivityLauncher): Bundle {
            if (launcher is SdkActivityLauncherAndBinderWrapper) {
                return launcher.launcherInfo
            }
            throw IllegalStateException("Invalid SdkActivityLauncher instance cannot be bundled. SdkActivityLaunchers may only be created by apps.")
        }
    }
}
