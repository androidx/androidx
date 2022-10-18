package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    suspend fun handleRequest(request: Request): Response

    suspend fun logRequest(request: Request)

    fun setListener(listener: MyCallback)

    fun doMoreStuff()

    suspend fun getMyInterface(): MyInterface
}

@PrivacySandboxInterface
interface MyInterface {
    suspend fun doSomething(request: Request): Response

    suspend fun getMyInterface(): MyInterface

    fun doMoreStuff(x: Int)
}

@PrivacySandboxValue
data class Request(val query: String)

@PrivacySandboxValue
data class Response(val response: String)

@PrivacySandboxCallback
interface MyCallback {
    fun onComplete(response: Response)

    fun onClick(x: Int, y: Int)
}