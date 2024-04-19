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
package bench.flame.diff.interop

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.Widget
import kotlin.system.exitProcess

internal fun CliktCommand.exitProcessWithError(error: String): Nothing =
    exitProcessWithErrorImpl(error)

internal fun CliktCommand.exitProcessWithError(error: Widget): Nothing =
    exitProcessWithErrorImpl(error)

internal fun CliktCommand.log(message: String, isStdErr: Boolean = false): Unit =
    logImpl(message, isStdErr)

private fun CliktCommand.exitProcessWithErrorImpl(error: Any): Nothing {
    logImpl(error, true)
    exitProcess(1)
}

private fun CliktCommand.logImpl(error: Any, isStdErr: Boolean) {
    echo("bench-flame-diff: ", err = true, trailingNewline = false)
    echo(error, err = isStdErr)
}
