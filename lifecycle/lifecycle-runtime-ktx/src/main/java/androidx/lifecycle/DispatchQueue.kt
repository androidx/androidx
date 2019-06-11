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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val queue: Queue<Runnable> = ArrayDeque<Runnable>()

    private val consumer = Runnable {
        // this one runs inside Dispatchers.Main
        // if it should run, grabs an item, runs it
        // if it has more, will re-enqueue
        // To avoid starving Dispatchers.Main, we don't consume more than 1
        if (!canRun()) {
            return@Runnable
        }
        val next = queue.poll() ?: return@Runnable
        try {
            next.run()
        } finally {
            maybeEnqueueConsumer()
        }
    }

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
        maybeEnqueueConsumer()
    }

    @MainThread
    fun finish() {
        finished = true
        maybeEnqueueConsumer()
    }

    @MainThread
    fun maybeEnqueueConsumer() {
        if (queue.isNotEmpty()) {
            Dispatchers.Main.dispatch(EmptyCoroutineContext, consumer)
        }
    }

    @MainThread
    private fun canRun() = finished || !paused

    @AnyThread
    @ExperimentalCoroutinesApi
    @SuppressLint("WrongThread") // false negative, we are checking the thread
    fun runOrEnqueue(runnable: Runnable) {
        Dispatchers.Main.immediate.dispatch(EmptyCoroutineContext, Runnable {
            enqueue(runnable)
        })
    }

    @MainThread
    private fun enqueue(runnable: Runnable) {
        check(queue.offer(runnable)) {
            "cannot enqueue any more runnables"
        }
        maybeEnqueueConsumer()
    }
}
