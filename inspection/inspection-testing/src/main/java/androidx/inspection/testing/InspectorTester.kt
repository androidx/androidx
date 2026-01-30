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

import androidx.inspection.ArtTooling
import androidx.inspection.Connection
import androidx.inspection.DefaultArtTooling
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorExecutors
import androidx.inspection.InspectorFactory
import java.util.ServiceLoader
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// TODO: should be non suspend function with CoroutineScope receiver, that would automatically
// dispose inspector;
/**
 * Instantiate an inspector with the given [inspectorId] and all operations such as creation,
 * command dispatching are happening in the context of executors passed within [environment]. It is
 * caller responsibility to shutdown those executors if the environment argument is passed. If
 * [environment] is unspecified or null, then [DefaultTestInspectorEnvironment] is used and
 * automatically disposed with [InspectorTester].
 *
 * You can pass [factoryOverride] to construct your inspector. This allows you to inject test
 * dependencies into the inspector, otherwise a factory for the given [inspectorId] will be looked
 * up in classpath and [java.lang.AssertionError] will be thrown if it couldn't be found.
 */
suspend fun InspectorTester(
    inspectorId: String,
    environment: InspectorEnvironment? = null,
    factoryOverride: InspectorFactory<*>? = null,
): InspectorTester {
    val inspectorTesterJob = Job()
    val resolved =
        environment
            ?: DefaultTestInspectorEnvironment(
                TestInspectorExecutors(inspectorTesterJob),
                DefaultArtTooling(inspectorId),
            )
    val dispatcher = resolved.executors().primary().asCoroutineDispatcher()
    return withContext(dispatcher) {
        val loader = ServiceLoader.load(InspectorFactory::class.java)
        val factory =
            factoryOverride
                ?: loader.iterator().asSequence().find { it.inspectorId == inspectorId }
                ?: throw AssertionError("Failed to find with inspector with $inspectorId")
        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        val scope = CoroutineScope(dispatcher + inspectorTesterJob)
        val inspector = factory.createInspector(ConnectionImpl(scope, channel), resolved)
        InspectorTester(scope, inspector, channel)
    }
}

class InspectorTester
internal constructor(
    private val scope: CoroutineScope,
    private val inspector: Inspector,
    val channel: ReceiveChannel<ByteArray>,
) {

    suspend fun sendCommand(array: ByteArray): ByteArray {
        val callerJob = coroutineContext[Job]!!
        // Tricky part: this actually can't be simplified to "withContext".
        // The following command should be cancelled in two scenarios:
        // 1. when Job of the caller is cancelled
        // 2. when inspector itself is disposed and its scope is cancelled
        // To achieve that we manually pass cancellation signal from caller's job to
        // coroutine.
        // The exactly same code won't work with withContext, because "invokeOnCompletion"
        // won't be called until this coroutine is completed, but the same time it won't complete
        // because cancellation signal isn't propagate. To workaround it you can call
        // invokeOnCompletion(onCancelling = true) {...}, but this method is marked
        // as InternalCoroutinesApi. Alternative workaround, is what we do: async allows separate
        // completion of caller of .await() and completion of async {} block, this way caller's Job
        // will be completed and once it is completed we can receive signal in async block and
        // cancel it.
        // More context at: https://github.com/Kotlin/kotlinx.coroutines/issues/1001
        return scope
            .async {
                callerJob.invokeOnCompletion {
                    if (it is CancellationException) {
                        this.cancel()
                    }
                }
                suspendCancellableCoroutine<ByteArray> { cont ->
                    inspector.onReceiveCommand(array, CommandCallbackImpl(cont))
                }
            }
            .await()
    }

    fun dispose() {
        runBlocking { scope.launch { inspector.onDispose() }.join() }
        scope.cancel()
    }
}

internal class CommandCallbackImpl(private val cont: CancellableContinuation<ByteArray>) :
    Inspector.CommandCallback {
    override fun reply(response: ByteArray) {
        cont.resume(response)
    }

    override fun addCancellationListener(executor: Executor, runnable: Runnable) {
        cont.invokeOnCancellation { executor.execute(runnable) }
    }
}

/**
 * Default test implementation of InspectorEnvironment.
 *
 * It will use either [testInspectorExecutors] that are passed in constructor, or create
 * [TestInspectorExecutors] scoped to given job, than those executors will be shutdown once job is
 * complete.
 */
class DefaultTestInspectorEnvironment(
    private val testInspectorExecutors: InspectorExecutors,
    private val artTooling: ArtTooling = FakeArtTooling(),
) : InspectorEnvironment {
    override fun artTooling() = artTooling

    override fun executors() = testInspectorExecutors
}

class FakeArtTooling : ArtTooling {
    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: ArtTooling.EntryHook,
    ) {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> findInstances(clazz: Class<T>): List<T> {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: ArtTooling.ExitHook<T>,
    ) {
        throw UnsupportedOperationException()
    }
}

private class ConnectionImpl(val scope: CoroutineScope, val channel: SendChannel<ByteArray>) :
    Connection() {
    override fun sendEvent(data: ByteArray) {
        scope.launch { channel.send(data) }
    }
}
