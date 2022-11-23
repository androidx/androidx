package com.sdkwithvalues

import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface SdkInterface {
    suspend fun exampleMethod(request: SdkRequest): SdkResponse

    suspend fun processValueList(x: List<SdkRequest>): List<SdkResponse>
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
    val myInterface: MyInterface,
    val numbers: List<Int>,
)

@PrivacySandboxValue
data class SdkRequest(
    val id: Long,
    val innerValue: InnerSdkValue,
    val moreValues: List<InnerSdkValue>
)

@PrivacySandboxValue
data class SdkResponse(val success: Boolean, val originalRequest: SdkRequest)

@PrivacySandboxInterface
interface MyInterface {
    fun doStuff()
}