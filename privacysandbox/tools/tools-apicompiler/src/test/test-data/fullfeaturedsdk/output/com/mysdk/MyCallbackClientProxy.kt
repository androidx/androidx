package com.mysdk

import com.mysdk.ResponseConverter.toParcelable

public class MyCallbackClientProxy(
    public val remote: IMyCallback,
) : MyCallback {
    public override fun onComplete(response: Response): Unit {
        remote.onComplete(toParcelable(response))
    }

    public override fun onClick(x: Int, y: Int): Unit {
        remote.onClick(x, y)
    }

    public override fun onCompleteInterface(myInterface: MyInterface): Unit {
        remote.onCompleteInterface(MyInterfaceStubDelegate(myInterface))
    }
}
