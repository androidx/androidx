package com.sdkwithcallbacks

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.PrivacySandboxInterface

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
data class Response(val response: String)

@PrivacySandboxInterface
interface MyInterface {
    fun doStuff()
}