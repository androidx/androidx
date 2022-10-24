package com.mysdk

import com.mysdk.ResponseConverter.toParcelable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class MyCallbackClientProxy(
    public val remote: IMyCallback,
) : MyCallback {
    public override fun onComplete(response: Response): Unit {
        remote.onComplete(toParcelable(response))
    }

    public override fun onClick(x: Int, y: Int): Unit {
        remote.onClick(x, y)
    }
}
