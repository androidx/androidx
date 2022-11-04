package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxService
interface BackwardsCompatibleSdk {
    suspend fun doStuff(x: Int, y: Int): String
}
