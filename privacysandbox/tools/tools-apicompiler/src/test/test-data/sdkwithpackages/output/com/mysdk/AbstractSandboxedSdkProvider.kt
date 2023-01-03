package com.mysdk

import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.content.Context
import android.os.Bundle
import android.view.View
import kotlin.Int

public abstract class AbstractSandboxedSdkProvider : SandboxedSdkProvider() {
  public override fun onLoadSdk(params: Bundle): SandboxedSdk {
    val sdk = createMySdk(context!!)
    return SandboxedSdk(MySdkStubDelegate(sdk))
  }

  public override fun getView(
    windowContext: Context,
    params: Bundle,
    width: Int,
    height: Int,
  ): View {
    TODO("Implement")
  }

  protected abstract fun createMySdk(context: Context): MySdk
}
