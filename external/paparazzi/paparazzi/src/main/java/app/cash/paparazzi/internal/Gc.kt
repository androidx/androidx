/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.cash.paparazzi.internal

import java.lang.ref.WeakReference

internal object Gc {
  fun gc() {
    // See RuntimeUtil#gc in jlibs (http://jlibs.in/)
    var obj: Any? = Any()
    val ref = WeakReference<Any>(obj)

    @Suppress("UNUSED_VAlUE")
    obj = null
    while (ref.get() != null) {
      System.gc()
      System.runFinalization()
    }

    System.gc()
    System.runFinalization()
  }
}
