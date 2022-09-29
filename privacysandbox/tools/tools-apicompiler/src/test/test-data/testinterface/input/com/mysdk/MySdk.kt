package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    fun doMoreStuff()
}