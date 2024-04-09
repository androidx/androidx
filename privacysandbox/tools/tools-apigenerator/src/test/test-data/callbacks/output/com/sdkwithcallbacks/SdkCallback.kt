package com.sdkwithcallbacks

import androidx.privacysandbox.activity.core.SdkActivityLauncher

public interface SdkCallback {
    public fun onCompleteInterface(myInterface: MyInterface)

    public fun onEmptyEvent()

    public fun onPrimitivesReceived(x: Int, y: Int)

    public fun onSdkActivityLauncherReceived(myLauncher: SdkActivityLauncher)

    public fun onValueReceived(response: Response)

    public suspend fun testing(): Response
}
