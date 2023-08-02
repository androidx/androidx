package app.cash.paparazzi.internal

import android.os.IBinder

/**
 * The ImeTracing class attempts to initialize its [mService field in its constructor](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/util/imetracing/ImeTracing.java;l=60).
 *
 * Unfortunately, [layoutlib's version of ServiceManager](https://cs.android.com/android/platform/superproject/+/master:frameworks/layoutlib/bridge/src/android/os/ServiceManager.java;l=37)
 * throws an exception immediately.
 *
 * This interceptor overrides ServiceManager.getServiceOrThrow to simply return null instead.
 */
object ServiceManagerInterceptor {
  @Suppress("unused")
  @JvmStatic
  fun interceptGetServiceOrThrow(@Suppress("UNUSED_PARAMETER") name: String): IBinder? = null
}
