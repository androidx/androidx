/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.core

import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Wrapper on FileOutputStream to prevent closing the underlying OutputStream.
 */
internal class UncloseableOutputStream(val fileOutputStream: FileOutputStream) : OutputStream() {

    override fun write(b: Int) {
        fileOutputStream.write(b)
    }

    override fun write(b: ByteArray) {
        fileOutputStream.write(b)
    }

    override fun write(bytes: ByteArray, off: Int, len: Int) {
        fileOutputStream.write(bytes, off, len)
    }

    override fun close() {
        // We will not close the underlying FileOutputStream until after we're done syncing
        // the fd. This is useful for things like b/173037611.
    }

    override fun flush() {
        fileOutputStream.flush()
    }
}
