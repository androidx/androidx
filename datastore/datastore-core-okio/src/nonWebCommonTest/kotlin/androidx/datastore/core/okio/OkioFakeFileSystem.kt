/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.datastore.core.okio

import kotlin.jvm.JvmName
import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source

/**
 * Used to test the race condition that may occur during [OkioReadScope.readData] when a file does
 * not exist during a call to read but is actually being created in different process slightly
 * later.
 *
 * This class wraps a [FileSystem] and overrides the [source] function to throw a
 * [FileNotFoundException] at the first read attempt, but to return the read value as expected in
 * the second read attempt.
 */
class OkioFakeFileSystem(@get:JvmName("delegate") val delegate: FileSystem) : FileSystem() {
    private var fileReadAttempt = 0

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        return delegate.appendingSink(file, mustExist)
    }

    override fun atomicMove(source: Path, target: Path) {
        delegate.atomicMove(source, target)
    }

    override fun canonicalize(path: Path): Path {
        return delegate.canonicalize(path)
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        delegate.createDirectory(dir, mustCreate)
    }

    override fun createSymlink(source: Path, target: Path) {
        delegate.createSymlink(source, target)
    }

    override fun delete(path: Path, mustExist: Boolean) {
        delegate.delete(path, mustExist)
    }

    override fun list(dir: Path): List<Path> {
        return delegate.list(dir)
    }

    override fun listOrNull(dir: Path): List<Path>? {
        return delegate.listOrNull(dir)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        return delegate.metadataOrNull(path)
    }

    override fun openReadOnly(file: Path): FileHandle {
        return delegate.openReadOnly(file)
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        return delegate.openReadWrite(file, mustCreate, mustExist)
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        return delegate.sink(file, mustCreate)
    }

    override fun source(file: Path): Source {
        // This function is invoked during fileSystem.read(). We change the behaviour to throw
        // an exception in the first read attempt, and return the correct value in the second to
        // mimic the race condition.
        fileReadAttempt++
        return if (fileReadAttempt <= 1) {
            // First attempt should throw.
            throw FileNotFoundException("Intentional failure to mimic race condition.")
        } else {
            // Second attempt onwards returns read value as expected, if the file exists. Throws an
            // [IOException] otherwise.
            delegate.source(file)
        }
    }
}
