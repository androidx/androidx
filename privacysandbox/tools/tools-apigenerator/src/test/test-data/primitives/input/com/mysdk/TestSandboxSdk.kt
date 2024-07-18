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

    suspend fun processIntList(x: List<Int>): List<Int>

    suspend fun processCharList(x: List<Char>): List<Char>

    suspend fun processFloatList(x: List<Float>): List<Float>

    suspend fun processLongList(x: List<Long>): List<Long>

    suspend fun processDoubleList(x: List<Double>): List<Double>

    suspend fun processBooleanList(x: List<Boolean>): List<Boolean>

    suspend fun processShortList(x: List<Short>): List<Short>

    suspend fun processStringList(x: List<String>): List<String>

    suspend fun processNullableInt(x: Int?): Int?
}