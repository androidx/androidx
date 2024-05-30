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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.lifecycle

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Android Unit Tests target doesn't provide a Main dispatcher. Lifecycle internals rely on Main &
 * Main.immediate dispatchers heavily, so we need to re-create their behavior in tests.
 */
@OptIn(DelicateCoroutinesApi::class)
private class SurrogateMainCoroutineDispatcher : MainCoroutineDispatcher() {
    private val isMainThread: ThreadLocal<Boolean> = ThreadLocal()

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    init {
        mainThreadSurrogate.dispatch(EmptyCoroutineContext, Runnable { isMainThread.set(true) })
    }

    override val immediate: MainCoroutineDispatcher =
        ImmediateMainCoroutineDispatcher(isMainThread, mainThreadSurrogate)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        mainThreadSurrogate.dispatch(context, block)
    }

    fun close() {
        mainThreadSurrogate.close()
    }
}

private class ImmediateMainCoroutineDispatcher(
    private val isMainThread: ThreadLocal<Boolean>,
    private val mainThreadSurrogate: CloseableCoroutineDispatcher,
) : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        mainThreadSurrogate.dispatch(context, block)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return isMainThread.get() != true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
actual fun runLifecycleTest(block: suspend CoroutineScope.() -> Unit) {
    val mainThreadSurrogate = SurrogateMainCoroutineDispatcher()
    Dispatchers.setMain(mainThreadSurrogate)

    try {
        runBlocking(mainThreadSurrogate, block = block)
    } finally {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }
}
