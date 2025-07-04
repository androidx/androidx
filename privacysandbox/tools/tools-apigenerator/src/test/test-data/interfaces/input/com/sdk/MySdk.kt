package com.sdk

import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.activity.core.SdkActivityLauncher

@PrivacySandboxService
interface MySdk {
    suspend fun getInterface(): MyInterface

    suspend fun maybeGetInterface(): MyInterface?

    suspend fun getUiInterface(): MySecondInterface

    suspend fun getSharedUiInterface(): MySharedUiInterface
}

@PrivacySandboxInterface
interface MyInterface {
    suspend fun add(x: Int, y: Int): Int

    fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface, sharedUiInterface: MySharedUiInterface)

    fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?, maybeSharedUiInterface: MySharedUiInterface?)

    public fun doSomethingWithSdkActivityLauncher(launcher: SdkActivityLauncher)

    companion object {
        const val MY_CONST_INT = 7 // Chosen by random dice roll.
        const val MY_CONST_BOOL = true
        const val MY_CONST_STR = "tea > coffee"
        const val MY_CONST_MULTILINE_STR = """
            {}'$$$$"*oss
            snoateusoeut
            tea > coffee
        """
        const val MY_CONST_SHORT = 42.toShort()
        const val MY_CONST_CHAR_1 = '\''
        const val MY_CONST_CHAR_2 = '\"'
        const val MY_CONST_CHAR_3 = '!'
    }
}

@PrivacySandboxInterface
interface MySecondInterface : SandboxedUiAdapter {
   fun doStuff()
}

@PrivacySandboxInterface
interface MySharedUiInterface : SharedUiAdapter {
    fun doStuff()
}
