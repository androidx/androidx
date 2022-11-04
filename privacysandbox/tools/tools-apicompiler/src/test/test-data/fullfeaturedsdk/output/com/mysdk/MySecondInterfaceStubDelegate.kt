package com.mysdk

import kotlin.Int
import kotlin.Unit

public class MySecondInterfaceStubDelegate internal constructor(
  public val `delegate`: MySecondInterface,
) : IMySecondInterface.Stub() {
  public override fun doMoreStuff(x: Int): Unit {
    delegate.doMoreStuff(x)
  }
}
