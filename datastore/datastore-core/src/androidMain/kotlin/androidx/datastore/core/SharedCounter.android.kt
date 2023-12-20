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

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException

/**
 * Put the JNI methods in a separate class to make them internal to the package.
 */
internal class NativeSharedCounter {
    external fun nativeTruncateFile(fd: Int): Int
    external fun nativeCreateSharedCounter(fd: Int): Long
    external fun nativeGetCounterValue(address: Long): Int
    external fun nativeIncrementAndGetCounterValue(address: Long): Int
}

/**
 * An atomic counter implemented by shared memory, which could be used by multi-process DataStore as
 * an atomic version counter. The underlying JNI library would be pre-compiled and shipped as part
 * of the `datastore-multiprocess` AAR artifact, users don't need extra steps other than adding it
 * as dependency.
 */
internal class SharedCounter private constructor(
    /**
     * The memory address to be mapped.
     */
    private val mappedAddress: Long
) {

    fun getValue(): Int {
        return nativeSharedCounter.nativeGetCounterValue(mappedAddress)
    }

    fun incrementAndGetValue(): Int {
        return nativeSharedCounter.nativeIncrementAndGetCounterValue(mappedAddress)
    }

    companion object Factory {
        internal val nativeSharedCounter = NativeSharedCounter()

        fun loadLib() = System.loadLibrary("datastore_shared_counter")

        private fun createCounterFromFd(pfd: ParcelFileDescriptor): SharedCounter {
            val nativeFd = pfd.getFd()
            if (nativeSharedCounter.nativeTruncateFile(nativeFd) != 0) {
                throw IOException("Failed to truncate counter file")
            }
            val address = nativeSharedCounter.nativeCreateSharedCounter(nativeFd)
            if (address < 0) {
                throw IOException("Failed to mmap counter file")
            }
            return SharedCounter(address)
        }

        internal fun create(produceFile: () -> File): SharedCounter {
            val file = produceFile()
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                )
                return createCounterFromFd(pfd)
            } finally {
                pfd?.close()
            }
        }
    }
}
