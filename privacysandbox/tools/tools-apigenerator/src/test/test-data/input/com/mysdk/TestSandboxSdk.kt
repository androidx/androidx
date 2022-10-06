package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface TestSandboxSdk {
    fun echoBoolean(input: Boolean)

    fun echoInt(input: Int)

    fun echoLong(input: Long)

    fun echoFloat(input: Float)

    fun echoDouble(input: Double)

    fun echoChar(input: Char)

    fun echoString(input: String)

    fun receiveMultipleArguments(first: Int, second: String, third: Long)

    fun receiveAndReturnNothing()

    suspend fun doSomethingAsync(first: Int, second: String, third: Long): Boolean

    suspend fun receiveAndReturnNothingAsync()
}