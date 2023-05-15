package com.sdkwithcallbacks

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

@PrivacySandboxService
interface SdkService {
    fun registerCallback(callback: SdkCallback)
}

@PrivacySandboxCallback
interface SdkCallback {
    fun onValueReceived(response: Response)

    fun onPrimitivesReceived(x: Int, y: Int)

    fun onEmptyEvent()

    fun onCompleteInterface(myInterface: MyInterface)
}

@PrivacySandboxValue
data class Response(val response: String, val uiInterface: MyUiInterface)

@PrivacySandboxInterface
interface MyInterface {
    fun doStuff()
}

@PrivacySandboxInterface
interface MyUiInterface : SandboxedUiAdapter {
    fun doUiStuff()
}
