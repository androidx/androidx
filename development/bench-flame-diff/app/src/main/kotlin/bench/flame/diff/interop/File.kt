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
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path

internal fun String.isValidFileName() = isNotBlank() && isValidPath() && isFileNameOnly()

private fun String.isValidPath(): Boolean = try {
    Path.of(this)
    true
} catch (_: InvalidPathException) {
    false
}

private fun String.isFileNameOnly(): Boolean = isNotBlank() && isValidPath() &&
        Path.of(this).parent == null

internal fun CliktCommand.openFileInOs(target: File) = execWithChecks(
    if (Os.isMac) "open" else "xdg-open", target.absolutePath
)

internal typealias FileWithId = IndexedValue<File>
internal val FileWithId.id get() = index + 1
internal val FileWithId.file get() = value
internal fun Sequence<File>.withId(): Sequence<FileWithId> = withIndex()
