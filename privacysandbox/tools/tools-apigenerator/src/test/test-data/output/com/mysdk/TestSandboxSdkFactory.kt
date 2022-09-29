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
  public override suspend fun doSomethingAsync(
    first: Int,
    second: String,
    third: Long,
  ): Boolean = suspendCancellableCoroutine {
    var mCancellationSignal: ICancellationSignal? = null
    val transactionCallback = object: IBooleanTransactionCallback.Stub() {
      override fun onCancellable(cancellationSignal: ICancellationSignal) {
        if (it.isCancelled) {
          cancellationSignal.cancel()
        }
        mCancellationSignal = cancellationSignal
      }
      override fun onSuccess(result: Boolean) {
        it.resumeWith(Result.success(result))
      }
      override fun onFailure(errorCode: Int, errorMessage: String) {
        it.resumeWithException(RuntimeException(errorMessage))
      }
    }
    remote.doSomethingAsync(first, second, third, transactionCallback)
    it.invokeOnCancellation {
      mCancellationSignal?.cancel()
    }
  }

  public override fun echoBoolean(input: Boolean) = remote.echoBoolean(input)

  public override fun echoChar(input: Char) = remote.echoChar(input)

  public override fun echoDouble(input: Double) = remote.echoDouble(input)

  public override fun echoFloat(input: Float) = remote.echoFloat(input)

  public override fun echoInt(input: Int) = remote.echoInt(input)

  public override fun echoLong(input: Long) = remote.echoLong(input)

  public override fun echoString(input: String) = remote.echoString(input)

  public override fun receiveAndReturnNothing(): Unit {
    remote.receiveAndReturnNothing()
  }

  public override suspend fun receiveAndReturnNothingAsync(): Unit = suspendCancellableCoroutine {
    var mCancellationSignal: ICancellationSignal? = null
    val transactionCallback = object: IUnitTransactionCallback.Stub() {
      override fun onCancellable(cancellationSignal: ICancellationSignal) {
        if (it.isCancelled) {
          cancellationSignal.cancel()
        }
        mCancellationSignal = cancellationSignal
      }
      override fun onSuccess() {
        it.resumeWith(Result.success(Unit))
      }
      override fun onFailure(errorCode: Int, errorMessage: String) {
        it.resumeWithException(RuntimeException(errorMessage))
      }
    }
    remote.receiveAndReturnNothingAsync(transactionCallback)
    it.invokeOnCancellation {
      mCancellationSignal?.cancel()
    }
  }

  public override fun receiveMultipleArguments(
    first: Int,
    second: String,
    third: Long,
  ): Unit {
    remote.receiveMultipleArguments(first, second, third)
  }
}
