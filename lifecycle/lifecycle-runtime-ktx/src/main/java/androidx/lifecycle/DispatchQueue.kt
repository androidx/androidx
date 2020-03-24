/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import android.annotation.SuppressLint
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import java.util.ArrayDeque
import java.util.Queue
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Helper class for [PausingDispatcher] that tracks runnables which are enqueued to the dispatcher
 * and also calls back the [PausingDispatcher] when the runnable should run.
 */
internal class DispatchQueue {
    // handler thread
    private var paused: Boolean = true
    // handler thread
    private var finished: Boolean = false
    private var isDraining: Boolean = false

    private val queue: Queue<Runnable> = ArrayDeque<Runnable>()

    @MainThread
    fun pause() {
        paused = true
    }

    @MainThread
    fun resume() {
        if (!paused) {
            return
        }
        check(!finished) {
            "Cannot resume a finished dispatcher"
        }
        paused = false
        drainQueue()
    }

    @MainThread
    fun finish() {
        finished = true
        drainQueue()
    }

    @MainThread
    fun drainQueue() {
        if (isDraining) {
            // Block re-entrant calls to avoid deep stacks
            return
        }
        try {
            isDraining = true
            while (queue.isNotEmpty()) {
                if (!canRun()) {
                    break
                }
                queue.poll()?.run()
            }
        } finally {
            isDraining = false
        }
    }

    @MainThread
    private fun canRun() = finished || !paused

    @AnyThread
    @SuppressLint("WrongThread") // false negative, we are checking the thread
    fun runOrEnqueue(runnable: Runnable) {
        with(Dispatchers.Main.immediate) {
            if (isDispatchNeeded(EmptyCoroutineContext)) {
                dispatch(EmptyCoroutineContext, Runnable {
                    enqueue(runnable)
                })
            } else {
                enqueue(runnable)
            }
        }
    }

    @MainThread
    private fun enqueue(runnable: Runnable) {
        check(queue.offer(runnable)) {
            "cannot enqueue any more runnables"
        }
        drainQueue()
    }
}
