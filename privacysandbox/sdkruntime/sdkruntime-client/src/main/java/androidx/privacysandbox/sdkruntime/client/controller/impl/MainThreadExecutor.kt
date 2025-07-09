/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Execute on main thread. If called from main thread then executes immediately, otherwise schedule
 * via post().
 */
internal object MainThreadExecutor : Executor {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        if (mainHandler.looper == Looper.myLooper()) {
            command.run()
        } else {
            mainHandler.post(command)
        }
    }
}
