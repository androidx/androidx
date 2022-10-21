package com.sdk

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class MySecondInterfaceClientProxy(
    public val remote: IMySecondInterface,
) : MySecondInterface {
    public override fun doStuff(): Unit {
        remote.doStuff()
    }
}
