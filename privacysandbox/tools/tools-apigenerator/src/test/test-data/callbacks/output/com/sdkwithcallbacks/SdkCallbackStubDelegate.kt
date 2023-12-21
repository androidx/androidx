package com.sdkwithcallbacks

import android.os.Bundle
import com.sdkwithcallbacks.ResponseConverter.fromParcelable
import com.sdkwithcallbacks.SdkActivityLauncherConverter.getLocalOrProxyLauncher
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class SdkCallbackStubDelegate internal constructor(
  public val `delegate`: SdkCallback,
) : ISdkCallback.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun onCompleteInterface(myInterface: IMyInterface): Unit {
    coroutineScope.launch {
      delegate.onCompleteInterface(MyInterfaceClientProxy(myInterface))
    }
  }

  public override fun onEmptyEvent(): Unit {
    coroutineScope.launch {
      delegate.onEmptyEvent()
    }
  }

  public override fun onPrimitivesReceived(x: Int, y: Int): Unit {
    coroutineScope.launch {
      delegate.onPrimitivesReceived(x, y)
    }
  }

  public override fun onSdkActivityLauncherReceived(myLauncher: Bundle): Unit {
    coroutineScope.launch {
      delegate.onSdkActivityLauncherReceived(getLocalOrProxyLauncher(myLauncher))
    }
  }

  public override fun onValueReceived(response: ParcelableResponse): Unit {
    coroutineScope.launch {
      delegate.onValueReceived(fromParcelable(response))
    }
  }
}
