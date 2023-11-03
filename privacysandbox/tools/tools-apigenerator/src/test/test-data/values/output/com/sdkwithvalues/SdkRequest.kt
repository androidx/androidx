package com.sdkwithvalues

import androidx.privacysandbox.ui.core.SdkActivityLauncher

public data class SdkRequest(
    public val id: Long,
    public val innerValue: InnerSdkValue,
    public val maybeInnerValue: InnerSdkValue?,
    public val moreValues: List<InnerSdkValue>,
    public val activityLauncher: SdkActivityLauncher,
)
