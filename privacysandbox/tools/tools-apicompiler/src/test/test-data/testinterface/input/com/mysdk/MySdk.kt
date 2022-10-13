package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    suspend fun handleRequest(request: Request): Response

    suspend fun logRequest(request: Request)

    fun doMoreStuff()
}

@PrivacySandboxValue
data class Request(val query: String)

@PrivacySandboxValue
data class Response(val response: String)