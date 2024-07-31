/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.os.Handler
import android.os.Looper
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Camera X normally handles lifecycle events itself. Optimizations in the API make it difficult to
 * perform a series of clean tests like Antelope does, so it requires its own custom lifecycle.
 */
class CustomLifecycle : LifecycleOwner {
    private var lifecycleRegistry = LifecycleRegistry(this)
    internal val mainHandler: Handler = Handler(Looper.getMainLooper())

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle = lifecycleRegistry

    fun start() {
        if (Looper.myLooper() != mainHandler.looper) {
            mainHandler.post { start() }
            return
        }

        if (lifecycleRegistry.currentState != Lifecycle.State.CREATED) {
            logd(
                "CustomLifecycle start error: Prior state should be CREATED. Instead it is: " +
                    lifecycleRegistry.currentState
            )
        } else {
            try {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            } catch (e: IllegalArgumentException) {
                logd("CustomLifecycle start error: unable to start " + e.message)
            }
        }
    }

    fun pauseAndStop() {
        if (Looper.myLooper() != mainHandler.looper) {
            mainHandler.post { pauseAndStop() }
            return
        }

        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            logd(
                "CustomLifecycle pause error: Prior state should be RESUMED. Instead it is: " +
                    lifecycleRegistry.currentState
            )
        } else {
            try {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
            } catch (e: IllegalArgumentException) {
                logd("CustomLifecycle pause error: unable to pause " + e.message)
            }
        }
    }

    fun finish() {
        if (Looper.myLooper() != mainHandler.looper) {
            mainHandler.post { finish() }
            return
        }

        if (lifecycleRegistry.currentState != Lifecycle.State.CREATED) {
            logd(
                "CustomLifecycle finish error: Prior state should be CREATED. Instead it is: " +
                    lifecycleRegistry.currentState
            )
        } else {
            try {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            } catch (e: IllegalArgumentException) {
                logd("CustomLifecycle finish error: unable to finish " + e.message)
            }
        }
    }

    fun isFinished(): Boolean {
        return (Lifecycle.State.DESTROYED == lifecycleRegistry.currentState)
    }
}
