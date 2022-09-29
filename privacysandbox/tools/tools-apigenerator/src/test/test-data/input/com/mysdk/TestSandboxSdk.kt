package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface TestSandboxSdk {
    fun echoBoolean(input: Boolean): Boolean

    fun echoInt(input: Int): Int

    fun echoLong(input: Long): Long

    fun echoFloat(input: Float): Float

    fun echoDouble(input: Double): Double

    fun echoChar(input: Char): Char

    fun echoString(input: String): String

    fun receiveMultipleArguments(first: Int, second: String, third: Long)

    fun receiveAndReturnNothing()

    suspend fun doSomethingAsync(first: Int, second: String, third: Long): Boolean

    suspend fun receiveAndReturnNothingAsync()
}