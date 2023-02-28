package com.sdkwithcallbacks

import com.sdkwithcallbacks.ResponseConverter.fromParcelable
import kotlin.Int
import kotlin.Unit

public class SdkCallbackStubDelegate internal constructor(
  public val `delegate`: SdkCallback,
) : ISdkCallback.Stub() {
  public override fun onCompleteInterface(myInterface: IMyInterface): Unit {
    delegate.onCompleteInterface(MyInterfaceClientProxy(myInterface))
  }

  public override fun onEmptyEvent(): Unit {
    delegate.onEmptyEvent()
  }

  public override fun onPrimitivesReceived(x: Int, y: Int): Unit {
    delegate.onPrimitivesReceived(x, y)
  }

  public override fun onValueReceived(response: ParcelableResponse): Unit {
    delegate.onValueReceived(fromParcelable(response))
  }
}
