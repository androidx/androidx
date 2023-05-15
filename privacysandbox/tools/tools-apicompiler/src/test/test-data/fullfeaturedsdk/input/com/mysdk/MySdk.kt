package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    suspend fun handleRequest(request: Request): Response

    suspend fun logRequest(request: Request)

    fun setListener(listener: MyCallback)

    fun doMoreStuff()

    suspend fun getMyInterface(input: MyInterface): MyInterface

    fun mutateMySecondInterface(input: MySecondInterface)

    suspend fun handleNullablePrimitives(x: Int?, y: Int?): String?

    suspend fun handleNullableValues(maybeRequest: Request?): Response?

    suspend fun handleNullableInterfaces(maybeCallback: MyCallback?): MyInterface?

    suspend fun returnUiInterface(): MyUiInterface

    fun acceptUiInterfaceParam(input: MyUiInterface)
}

@PrivacySandboxInterface
interface MyInterface {
    suspend fun doSomething(request: Request): Response

    suspend fun getMyInterface(input: MyInterface): MyInterface

    suspend fun getMySecondInterface(input: MySecondInterface): MySecondInterface

    fun doMoreStuff(x: Int)
}

@PrivacySandboxInterface
interface MyUiInterface : SandboxedUiAdapter {
    fun doSomethingForUi(x: Int, y: Int)
}

@PrivacySandboxInterface
interface MySecondInterface {
    suspend fun doIntStuff(x: List<Int>): List<Int>

    suspend fun doCharStuff(x: List<Char>): List<Char>

    suspend fun doFloatStuff(x: List<Float>): List<Float>

    suspend fun doLongStuff(x: List<Long>): List<Long>

    suspend fun doDoubleStuff(x: List<Double>): List<Double>

    suspend fun doBooleanStuff(x: List<Boolean>): List<Boolean>

    suspend fun doShortStuff(x: List<Short>): List<Short>

    suspend fun doStringStuff(x: List<String>): List<String>

    suspend fun doValueStuff(x: List<Request>): List<Response>
}

@PrivacySandboxValue
data class Request(
    val query: String,
    val extraValues: List<InnerValue>,
    val maybeValue: InnerValue?,
    val myInterface: MyInterface,
    val myUiInterface: MyUiInterface,
)

@PrivacySandboxValue
data class InnerValue(val numbers: List<Int>, val maybeNumber: Int?)

@PrivacySandboxValue
data class Response(
    val response: String,
    val mySecondInterface: MySecondInterface,
    val maybeOtherInterface: MySecondInterface,
    val myUiInterface: MyUiInterface,
)

@PrivacySandboxCallback
interface MyCallback {
    fun onComplete(response: Response)

    fun onClick(x: Int, y: Int)

    fun onCompleteInterface(myInterface: MyInterface)

    fun onCompleteUiInterface(myUiInterface: MyUiInterface)
}