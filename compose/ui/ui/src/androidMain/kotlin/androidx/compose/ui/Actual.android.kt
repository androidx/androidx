/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui

import android.os.Handler
import android.os.Looper

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private val handler = Handler(Looper.getMainLooper())

internal actual fun postDelayed(delayMillis: Long, block: () -> Unit): Any {
    val runnable = Runnable { block() }
    handler.postDelayed(runnable, delayMillis)
    return runnable
}

internal actual fun removePost(token: Any?) {
    token as? Runnable ?: return
    handler.removeCallbacks(token)
}
