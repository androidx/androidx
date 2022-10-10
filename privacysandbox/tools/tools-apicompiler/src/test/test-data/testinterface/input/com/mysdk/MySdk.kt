package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    suspend fun handleRequest(request: Request): Response

    fun doMoreStuff()
}

@PrivacySandboxValue
data class Request(val query: String)

@PrivacySandboxValue
data class Response(val response: String)