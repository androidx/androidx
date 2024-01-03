package com.sdk

import androidx.privacysandbox.ui.core.SdkActivityLauncher

public interface MyInterface {
    public suspend fun add(x: Int, y: Int): Int

    public fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface)

    public fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?)

    public fun doSomethingWithSdkActivityLauncher(launcher: SdkActivityLauncher)
}
