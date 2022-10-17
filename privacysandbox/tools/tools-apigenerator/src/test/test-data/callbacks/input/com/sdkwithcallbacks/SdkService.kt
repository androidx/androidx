package com.sdkwithcallbacks

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface SdkService {
    suspend fun registerCallback(callback: SdkCallback)
}

@PrivacySandboxCallback
interface SdkCallback {
    fun onValueReceived(response: Response)

    fun onPrimitivesReceived(x: Int, y: Int)

    fun onEmptyEvent()
}

@PrivacySandboxValue
data class Response(val response: String)
