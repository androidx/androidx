package com.mysdk

public interface TestSandboxSdk {
    public suspend fun doSomethingAsync(
        first: Int,
        second: String,
        third: Long,
    ): Boolean

    public fun echoBoolean(input: Boolean): Unit

    public fun echoChar(input: Char): Unit

    public fun echoDouble(input: Double): Unit

    public fun echoFloat(input: Float): Unit

    public fun echoInt(input: Int): Unit

    public fun echoLong(input: Long): Unit

    public fun echoString(input: String): Unit

    public suspend fun processBooleanList(x: List<Boolean>): List<Boolean>

    public suspend fun processCharList(x: List<Char>): List<Char>

    public suspend fun processDoubleList(x: List<Double>): List<Double>

    public suspend fun processFloatList(x: List<Float>): List<Float>

    public suspend fun processIntList(x: List<Int>): List<Int>

    public suspend fun processLongList(x: List<Long>): List<Long>

    public suspend fun processNullableInt(x: Int?): Int?

    public suspend fun processShortList(x: List<Short>): List<Short>

    public suspend fun processStringList(x: List<String>): List<String>

    public fun receiveAndReturnNothing(): Unit

    public suspend fun receiveAndReturnNothingAsync(): Unit

    public fun receiveMultipleArguments(
        first: Int,
        second: String,
        third: Long,
    ): Unit
}
