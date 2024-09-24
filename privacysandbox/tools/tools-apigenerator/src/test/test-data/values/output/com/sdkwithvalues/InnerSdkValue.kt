package com.sdkwithvalues

import android.os.Bundle

public data class InnerSdkValue(
    public val id: Int,
    public val bigLong: Long,
    public val shouldBeAwesome: Boolean,
    public val separator: Char,
    public val message: String,
    public val floatingPoint: Float,
    public val hugeNumber: Double,
    public val myInterface: MyInterface,
    public val myUiInterface: MyUiInterface,
    public val numbers: List<Int>,
    public val bundle: Bundle,
    public val maybeNumber: Int?,
    public val maybeInterface: MyInterface?,
    public val maybeBundle: Bundle?,
) {
    public companion object {
        public const val DEFAULT_USER_ID: Int = 42

        public const val DEFAULT_SEPARATOR: Char = '\"'
    }
}
