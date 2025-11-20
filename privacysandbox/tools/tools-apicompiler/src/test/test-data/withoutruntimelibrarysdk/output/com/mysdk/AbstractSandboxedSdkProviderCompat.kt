package com.mysdk

import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.content.Context
import android.os.Bundle
import android.view.View
import kotlin.Int

public abstract class AbstractSandboxedSdkProviderCompat : SandboxedSdkProvider() {
  public override fun onLoadSdk(params: Bundle): SandboxedSdk {
    val ctx = context
    if (ctx == null) {
      throw IllegalStateException("Context must not be null. Do you need to call attachContext()?")
    }
    val sdk = createWithoutRuntimeLibrarySdk(ctx)
    return SandboxedSdk(WithoutRuntimeLibrarySdkStubDelegate(sdk, ctx))
  }

  protected abstract fun createWithoutRuntimeLibrarySdk(context: Context): WithoutRuntimeLibrarySdk

  public override fun getView(
    windowContext: Context,
    params: Bundle,
    width: Int,
    height: Int,
  ): View = throw UnsupportedOperationException("This SDK doesn't support explicit SurfaceView requests.")
}
