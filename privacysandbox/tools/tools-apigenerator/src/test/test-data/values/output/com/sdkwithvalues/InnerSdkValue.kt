package com.sdkwithvalues

public data class InnerSdkValue(
    public val bigLong: Long,
    public val floatingPoint: Float,
    public val hugeNumber: Double,
    public val id: Int,
    public val message: String,
    public val myInterface: MyInterface,
    public val separator: Char,
    public val shouldBeAwesome: Boolean,
)
