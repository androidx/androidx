package com.sdkwithvalues

public interface SdkInterface {
    public suspend fun exampleMethod(request: SdkRequest): SdkResponse

    public suspend fun processNullableValues(request: SdkRequest?): SdkResponse?

    public suspend fun processValueList(x: List<SdkRequest>): List<SdkResponse>
}
