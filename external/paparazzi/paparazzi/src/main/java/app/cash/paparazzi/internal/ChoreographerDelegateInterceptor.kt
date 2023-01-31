package app.cash.paparazzi.internal

import android.view.Choreographer
import com.android.internal.lang.System_Delegate

object ChoreographerDelegateInterceptor {
  @Suppress("unused")
  @JvmStatic
  fun intercept(
    @Suppress("UNUSED_PARAMETER") choreographer: Choreographer
  ): Long = System_Delegate.nanoTime()
}
