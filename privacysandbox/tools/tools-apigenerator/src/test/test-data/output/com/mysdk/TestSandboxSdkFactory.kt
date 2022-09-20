package com.mysdk

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Bundle
import android.os.OutcomeReceiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public suspend fun createTestSandboxSdk(context: Context): TestSandboxSdk =
    suspendCancellableCoroutine {
  val sdkSandboxManager = context.getSystemService(SdkSandboxManager::class.java)
  sdkSandboxManager.loadSdk(
      "com.mysdk",
      Bundle.EMPTY,
      { obj: Runnable -> obj.run() },
      object : OutcomeReceiver<SandboxedSdk, LoadSdkException> {
          override fun onResult(result: SandboxedSdk) {
              it.resume(TestSandboxSdkClientProxy(
                  ITestSandboxSdk.Stub.asInterface(result.getInterface())))
          }

          override fun onError(error: LoadSdkException) {
              it.resumeWithException(error)
          }
      })}

private class TestSandboxSdkClientProxy(
  private val remote: ITestSandboxSdk,
) : TestSandboxSdk {
  public override fun echoBoolean(input: Boolean) = remote.echoBoolean(input)!!

  public override fun echoChar(input: Char) = remote.echoChar(input)!!

  public override fun echoDouble(input: Double) = remote.echoDouble(input)!!

  public override fun echoFloat(input: Float) = remote.echoFloat(input)!!

  public override fun echoInt(input: Int) = remote.echoInt(input)!!

  public override fun echoLong(input: Long) = remote.echoLong(input)!!

  public override fun echoString(input: String) = remote.echoString(input)!!

  public override fun receiveAndReturnNothing(): Unit {
    remote.receiveAndReturnNothing()
  }

  public override fun receiveMultipleArguments(
    first: Int,
    second: String,
    third: Long,
  ): Unit {
    remote.receiveMultipleArguments(first, second, third)
  }
}
