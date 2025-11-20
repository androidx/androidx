package com.mysdk

import android.content.Context
import kotlin.Int
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MySharedUiInterfaceStubDelegate internal constructor(
  public val `delegate`: MySharedUiInterface,
  public val context: Context,
) : IMySharedUiInterface.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doSomethingForUi(x: Int, y: Int) {
    coroutineScope.launch {
      delegate.doSomethingForUi(x, y)
    }
  }
}
