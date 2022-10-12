package com.sdkwithvalues

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface SdkInterface {
    suspend fun exampleMethod(request: SdkRequest): SdkResponse
}

@PrivacySandboxValue
data class InnerSdkValue(
    val id: Int,
    val bigLong: Long,
    val shouldBeAwesome: Boolean,
    val separator: Char,
    val message: String,
    val floatingPoint: Float,
    val hugeNumber: Double,
)

@PrivacySandboxValue
data class SdkRequest(val id: Long, val innerValue: InnerSdkValue)

@PrivacySandboxValue
data class SdkResponse(val success: Boolean, val originalRequest: SdkRequest)
