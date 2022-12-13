package com.sdk

import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface MySdk {
    suspend fun getInterface(): MyInterface

    suspend fun maybeGetInterface(): MyInterface?
}

@PrivacySandboxInterface
interface MyInterface {
    suspend fun add(x: Int, y: Int): Int

    fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface)

    fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?)
}

@PrivacySandboxInterface
interface MySecondInterface {
   fun doStuff()
}
