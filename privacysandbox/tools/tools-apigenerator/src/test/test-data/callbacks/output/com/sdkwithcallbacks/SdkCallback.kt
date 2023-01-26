package com.sdkwithcallbacks

public interface SdkCallback {
    public fun onCompleteInterface(myInterface: MyInterface): Unit

    public fun onEmptyEvent(): Unit

    public fun onPrimitivesReceived(x: Int, y: Int): Unit

    public fun onValueReceived(response: Response): Unit
}
