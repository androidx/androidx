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

package androidx.inspection.testing.echo

import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * An inspector for test purposes, it echoes on commands and sends events once
 * [TickleManager] is tickled.
 */
class EchoInspector(connection: Connection) : Inspector(connection) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickleCounter: Byte = 1

    init {
        scope.launch {
            TickleManager.tickles.collect {
                connection.sendEvent("counter: ${tickleCounter++}".toByteArray())
            }
        }
    }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command = String(data)
        if (command == "<cancellation-test>") {
            callback.addCancellationListener(
                DirectExecutor,
                Runnable {
                    connection.sendEvent("cancellation: successfully cancelled".toByteArray())
                }
            )
            connection.sendEvent("cancellation: listener added".toByteArray())
        } else {
            callback.reply("echoed: $command".toByteArray())
        }
    }

    override fun onDispose() {
        scope.cancel()
    }
}

const val ECHO_INSPECTION_ID = "androidx.inspection.testing.echo"

object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

class EchoInspectorFactory : InspectorFactory<EchoInspector>(ECHO_INSPECTION_ID) {
    override fun createInspector(connection: Connection, unusedEnvironment: InspectorEnvironment) =
        EchoInspector(connection)
}
