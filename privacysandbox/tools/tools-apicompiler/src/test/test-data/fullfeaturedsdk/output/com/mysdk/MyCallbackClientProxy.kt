package com.mysdk

import android.content.Context
import androidx.privacysandbox.ui.provider.toCoreLibInfo

public class MyCallbackClientProxy(
    public val remote: IMyCallback,
    public val context: Context,
) : MyCallback {
    public override fun onComplete(response: Response) {
        remote.onComplete(ResponseConverter(context).toParcelable(response))
    }

    public override fun onClick(x: Int, y: Int) {
        remote.onClick(x, y)
    }

    public override fun onCompleteInterface(myInterface: MyInterface) {
        remote.onCompleteInterface(MyInterfaceStubDelegate(myInterface, context))
    }

    public override fun onCompleteUiInterface(myUiInterface: MyUiInterface) {
        remote.onCompleteUiInterface(IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable(myUiInterface.toCoreLibInfo(context),
                MyUiInterfaceStubDelegate(myUiInterface, context)))
    }
}
