package com.mysdk

import android.content.Context

public class MyCallbackClientProxy(
    public val remote: IMyCallback,
    public val context: Context,
) : MyCallback {
    public override fun onComplete(response: Response): Unit {
        remote.onComplete(ResponseConverter(context).toParcelable(response))
    }

    public override fun onClick(x: Int, y: Int): Unit {
        remote.onClick(x, y)
    }

    public override fun onCompleteInterface(myInterface: MyInterface): Unit {
        remote.onCompleteInterface(MyInterfaceStubDelegate(myInterface, context))
    }
}
