package app.cash.paparazzi.internal

import android.os.IBinder
import com.android.internal.view.IInputMethodManager

/**
 * With [ServiceManagerInterceptor] returning null for the service, we must override the logic
 * in [com.android.internal.view.IInputMethodManager.Stub.asInterface] to return the default
 * implementation of [IInputMethodManager].
 */
object IInputMethodManagerInterceptor {
  @Suppress("unused")
  @JvmStatic
  fun interceptAsInterface(@Suppress("UNUSED_PARAMETER") obj: IBinder?): IInputMethodManager =
    IInputMethodManager.Default()
}
