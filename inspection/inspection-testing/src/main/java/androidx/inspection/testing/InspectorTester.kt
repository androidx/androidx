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

package androidx.inspection.testing

import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ServiceLoader
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine

/**
 * Instantiate an inspector with the given [inspectorId] and all operations such as instantition,
 * command dispatching are happening in the context of the given [dispatcher].
 *
 * if [InspectorFactory] for the given [inspectorId] can't be found in classpath
 * an [java.lang.AssertionError] is thrown.
 */
suspend fun InspectorTester(
    inspectorId: String,
    environment: InspectorEnvironment = FakeInspectorEnvironment(),
    dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
): InspectorTester {
    return withContext(dispatcher) {
        val loader = ServiceLoader.load(InspectorFactory::class.java)
        val factory = loader.iterator().asSequence().find { it.inspectorId == inspectorId }
            ?: throw AssertionError("Failed to find with inspector with $inspectorId")
        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        val scope = CoroutineScope(dispatcher)
        val inspector = factory.createInspector(ConnectionImpl(scope, channel), environment)
        InspectorTester(scope, inspector, channel)
    }
}

class InspectorTester internal constructor(
    private val scope: CoroutineScope,
    private val inspector: Inspector,
    val channel: ReceiveChannel<ByteArray>
) {

    suspend fun sendCommand(array: ByteArray): ByteArray {
        return withContext(scope.coroutineContext) {
            suspendCoroutine<ByteArray> { cont ->
                inspector.onReceiveCommand(array) { response ->
                    cont.resumeWith(Result.success(response))
                }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}

internal class FakeInspectorEnvironment : InspectorEnvironment {
    override fun <T : Any?> findInstances(clazz: Class<T>): List<T> {
        TODO("not implemented")
    }
    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        hookClass: Class<*>,
        hookMethodName: String
    ) {
        TODO("not implemented")
    }

    override fun registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        hookClass: Class<*>,
        hookMethod: String
    ) {
        TODO("not implemented")
    }
}

private class ConnectionImpl(
    val scope: CoroutineScope,
    val channel: SendChannel<ByteArray>
) : Connection() {
    override fun sendEvent(data: ByteArray) {
        scope.launch {
            channel.send(data)
        }
    }
}
