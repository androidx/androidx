package com.sdkwithvalues

import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

@PrivacySandboxService
interface SdkInterface {
    suspend fun exampleMethod(request: SdkRequest): SdkResponse

    suspend fun processNullableValues(request: SdkRequest?): SdkResponse?

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
    val myUiInterface: MyUiInterface,
    val numbers: List<Int>,
    val maybeNumber: Int?,
    val maybeInterface: MyInterface?,
)

@PrivacySandboxValue
data class SdkRequest(
    val id: Long,
    val innerValue: InnerSdkValue,
    val maybeInnerValue: InnerSdkValue?,
    val moreValues: List<InnerSdkValue>
)

@PrivacySandboxValue
data class SdkResponse(val success: Boolean, val originalRequest: SdkRequest)

@PrivacySandboxInterface
interface MyInterface {
    fun doStuff()
}

@PrivacySandboxInterface
interface MyUiInterface : SandboxedUiAdapter {
    fun doUiStuff()
}