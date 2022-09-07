package com.mysdk

private class TestSandboxSdkClientProxy private constructor(
  private val remote: ITestSandboxSdk,
) : TestSandboxSdk {
  public override fun echoBoolean(input: Boolean) = remote.echoBoolean(input, null)!!

  public override fun echoChar(input: Char) = remote.echoChar(input, null)!!

  public override fun echoDouble(input: Double) = remote.echoDouble(input, null)!!

  public override fun echoFloat(input: Float) = remote.echoFloat(input, null)!!

  public override fun echoInt(input: Int) = remote.echoInt(input, null)!!

  public override fun echoLong(input: Long) = remote.echoLong(input, null)!!

  public override fun echoString(input: String) = remote.echoString(input, null)!!

  public override fun receiveAndReturnNothing(): Unit {
    remote.receiveAndReturnNothing(null)
  }

  public override fun receiveMultipleArguments(
    first: Int,
    second: String,
    third: Long,
  ): Unit {
    remote.receiveMultipleArguments(first, second, third, null)
  }
}
