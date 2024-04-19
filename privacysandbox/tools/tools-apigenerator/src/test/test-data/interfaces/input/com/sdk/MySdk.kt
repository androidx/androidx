package com.sdk

import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.activity.core.SdkActivityLauncher

@PrivacySandboxService
interface MySdk {
    suspend fun getInterface(): MyInterface

    suspend fun maybeGetInterface(): MyInterface?

    suspend fun getUiInterface(): MySecondInterface
}

@PrivacySandboxInterface
interface MyInterface {
    suspend fun add(x: Int, y: Int): Int

    fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface)

    fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?)

    public fun doSomethingWithSdkActivityLauncher(launcher: SdkActivityLauncher)
}

@PrivacySandboxInterface
interface MySecondInterface : SandboxedUiAdapter {
   fun doStuff()
}
