package com.mysdk

import kotlin.Int
import kotlin.Unit

public class MyUiInterfaceStubDelegate internal constructor(
  public val `delegate`: MyUiInterface,
) : IMyUiInterface.Stub() {
  public override fun doSomethingForUi(x: Int, y: Int): Unit {
    delegate.doSomethingForUi(x, y)
  }
}
