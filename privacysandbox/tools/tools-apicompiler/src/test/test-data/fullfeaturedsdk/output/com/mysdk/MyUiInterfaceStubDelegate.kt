package com.mysdk

import android.content.Context
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MyUiInterfaceStubDelegate internal constructor(
  public val `delegate`: MyUiInterface,
  public val context: Context,
) : IMyUiInterface.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doSomethingForUi(x: Int, y: Int): Unit {
    coroutineScope.launch {
      delegate.doSomethingForUi(x, y)
    }
  }
}
