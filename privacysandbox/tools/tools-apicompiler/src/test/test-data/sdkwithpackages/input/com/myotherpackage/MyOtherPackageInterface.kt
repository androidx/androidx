package com.myotherpackage

import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxValue

@PrivacySandboxInterface
interface MyOtherPackageInterface {
    fun doStuff(x: Int)

    suspend fun useDataClass(x: MyOtherPackageDataClass)
}

@PrivacySandboxValue
data class MyOtherPackageDataClass(
    val query: String,
)