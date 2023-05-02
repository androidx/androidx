package com.sdkwithvalues

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
    public val maybeNumber: Int?,
    public val maybeInterface: MyInterface?,
)
