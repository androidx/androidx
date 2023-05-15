package com.sdkwithcallbacks

import androidx.privacysandbox.ui.core.SdkActivityLauncher

public interface SdkCallback {
    public fun onCompleteInterface(myInterface: MyInterface): Unit

    public fun onEmptyEvent(): Unit

    public fun onPrimitivesReceived(x: Int, y: Int): Unit

    public fun onSdkActivityLauncherReceived(myLauncher: SdkActivityLauncher): Unit

    public fun onValueReceived(response: Response): Unit
}
