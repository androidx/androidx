package com.sdkwithvalues

import android.os.Bundle
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.activity.core.SdkActivityLauncher

@PrivacySandboxService
interface SdkInterface {
    suspend fun exampleMethod(request: SdkRequest): SdkResponse

    suspend fun processNullableValues(request: SdkRequest?): SdkResponse?

    suspend fun processValueList(x: List<SdkRequest>): List<SdkResponse>

    suspend fun processEnum(requestFlag: RequestFlag): RequestFlag
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
    val bundle: Bundle,
    val maybeNumber: Int?,
    val maybeInterface: MyInterface?,
    val maybeBundle: Bundle?,
) {
    companion object {
        const val DEFAULT_USER_ID = 42
        const val DEFAULT_SEPARATOR = '"'
    }
}

@PrivacySandboxValue
enum class RequestFlag {
    UP,
    DOWN;

    companion object {
        const val STEP_SIZE = 5
    }
}

@PrivacySandboxValue
data class SdkRequest(
    val id: Long,
    val innerValue: InnerSdkValue,
    val maybeInnerValue: InnerSdkValue?,
    val moreValues: List<InnerSdkValue>,
    val activityLauncher: SdkActivityLauncher,
    val requestFlag: RequestFlag,
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