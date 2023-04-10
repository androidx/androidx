package com.mysdk

import android.content.Context
import kotlin.Int
import kotlin.Unit

public class MyUiInterfaceStubDelegate internal constructor(
  public val `delegate`: MyUiInterface,
  public val context: Context,
) : IMyUiInterface.Stub() {
  public override fun doSomethingForUi(x: Int, y: Int): Unit {
    delegate.doSomethingForUi(x, y)
  }
}
