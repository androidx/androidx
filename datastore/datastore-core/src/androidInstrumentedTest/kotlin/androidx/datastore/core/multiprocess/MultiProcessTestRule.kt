/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.core.multiprocess

import androidx.datastore.core.twoWayIpc.TwoWayIpcConnection
import androidx.datastore.core.twoWayIpc.TwoWayIpcService
import androidx.datastore.core.twoWayIpc.TwoWayIpcService2
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Used for testing multi-process cases while also maintaining resources so that services
 * are properly closed after test.
 */
class MultiProcessTestRule : TestWatcher() {
    private val context = InstrumentationRegistry.getInstrumentation().context

    // use a real scope, it is too hard to use a TestScope when we cannot control the IPC
    val datastoreScope = CoroutineScope(
        Dispatchers.IO + Job()
    )
    private val connectionsMutex = Mutex()
    private val connections = mutableListOf<TwoWayIpcConnection>()
    private val availableServiceClasses = mutableListOf<Class<out TwoWayIpcService>>(
        TwoWayIpcService::class.java,
        TwoWayIpcService2::class.java
    )

    fun runTest(block: suspend CoroutineScope.() -> Unit) {
        // don't use datastore scope here as it will not finish by itself.
        runBlocking {
            withTimeout(TEST_TIMEOUT) {
                block()
            }
        }
    }

    suspend fun createConnection(): TwoWayIpcConnection {
        val connection = connectionsMutex.withLock {
            val klass = availableServiceClasses.removeFirstOrNull() ?: error(
                "Cannot create more services," +
                    "you can declare more in the manifest if needed"
            )
            TwoWayIpcConnection(context, klass).also {
                connections.add(it)
            }
        }
        connection.connect()
        return connection
    }

    override fun finished(description: Description) {
        super.finished(description)
        connections.forEach {
            it.disconnect()
        }
        datastoreScope.cancel()
    }

    companion object {
        val TEST_TIMEOUT = 10.seconds
    }
}
