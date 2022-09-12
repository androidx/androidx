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
