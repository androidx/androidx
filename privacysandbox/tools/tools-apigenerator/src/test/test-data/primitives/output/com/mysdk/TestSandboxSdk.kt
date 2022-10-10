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

    public fun receiveAndReturnNothing(): Unit

    public suspend fun receiveAndReturnNothingAsync(): Unit

    public fun receiveMultipleArguments(
        first: Int,
        second: String,
        third: Long,
    ): Unit
}
