package com.sdkwithvalues

public data class SdkRequest(
    public val id: Long,
    public val innerValue: InnerSdkValue,
    public val maybeInnerValue: InnerSdkValue?,
    public val moreValues: List<InnerSdkValue>,
)
