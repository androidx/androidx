package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface MySdk {
    fun doSomething(magicNumber: Int, awesomeString: String): Boolean

    fun returnMagicNumber(): Int
}