package com.mysdk

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat

public abstract class AbstractSandboxedSdkProviderCompat : SandboxedSdkProviderCompat() {
  public override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
    val ctx = context
    if (ctx == null) {
      throw IllegalStateException("Context must not be null. Do you need to call attachContext()?")
    }
    val sdk = createMySdk(ctx)
    return SandboxedSdkCompat(MySdkStubDelegate(sdk, ctx))
  }

  protected abstract fun createMySdk(context: Context): MySdk
}
