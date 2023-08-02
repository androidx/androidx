package com.mysdk

public interface TestSandboxSdk {
    public suspend fun doSomethingAsync(
        first: Int,
        second: String,
        third: Long,
    ): Boolean

    public fun echoBoolean(input: Boolean)

    public fun echoChar(input: Char)

    public fun echoDouble(input: Double)

    public fun echoFloat(input: Float)

    public fun echoInt(input: Int)

    public fun echoLong(input: Long)

    public fun echoString(input: String)

    public suspend fun processBooleanList(x: List<Boolean>): List<Boolean>

    public suspend fun processCharList(x: List<Char>): List<Char>

    public suspend fun processDoubleList(x: List<Double>): List<Double>

    public suspend fun processFloatList(x: List<Float>): List<Float>

    public suspend fun processIntList(x: List<Int>): List<Int>

    public suspend fun processLongList(x: List<Long>): List<Long>

    public suspend fun processNullableInt(x: Int?): Int?

    public suspend fun processShortList(x: List<Short>): List<Short>

    public suspend fun processStringList(x: List<String>): List<String>

    public fun receiveAndReturnNothing()

    public suspend fun receiveAndReturnNothingAsync()

    public fun receiveMultipleArguments(
        first: Int,
        second: String,
        third: Long,
    )
}
