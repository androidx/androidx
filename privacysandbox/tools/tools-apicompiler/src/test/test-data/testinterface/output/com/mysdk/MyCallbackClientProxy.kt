package com.mysdk

import com.mysdk.ResponseConverter.toParcelable
import kotlin.Int
import kotlin.Unit

internal class MyCallbackClientProxy(
  private val remote: IMyCallback,
) : MyCallback {
  public override fun onComplete(response: Response): Unit {
    remote.onComplete(toParcelable(response))
  }

  public override fun onClick(x: Int, y: Int): Unit {
    remote.onClick(x, y)
  }
}
