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
import java.util.concurrent.atomic.AtomicInteger

/** Put the JNI methods in a separate class to make them internal to the package. */
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
internal interface SharedCounter {
    fun getValue(): Int

    fun incrementAndGetValue(): Int

    private class RealSharedCounter(
        private val nativeSharedCounter: NativeSharedCounter,
        /** The memory address to be mapped. */
        private val mappedAddress: Long,
    ) : SharedCounter {
        override fun getValue(): Int {
            return nativeSharedCounter.nativeGetCounterValue(mappedAddress)
        }

        override fun incrementAndGetValue(): Int {
            return nativeSharedCounter.nativeIncrementAndGetCounterValue(mappedAddress)
        }
    }

    /** Shared counter implementation that is used when running Robolectric tests. */
    private class ShadowSharedCounter : SharedCounter {
        private val value = AtomicInteger(0)

        override fun getValue(): Int {
            return value.get()
        }

        override fun incrementAndGetValue(): Int {
            return value.incrementAndGet()
        }
    }

    companion object Factory {
        private val nativeSharedCounter: NativeSharedCounter? =
            try {
                System.loadLibrary("datastore_shared_counter")
                NativeSharedCounter()
            } catch (th: Throwable) {
                /**
                 * Currently this native library is only available for Android, it should not be
                 * loaded on host platforms, e.g. Robolectric.
                 */
                if (isDalvik()) {
                    // we should always be able to load it on dalvik
                    throw th
                } else {
                    // probably running on robolectric, ignore.
                    null
                }
            }

        private fun createCounterFromFd(pfd: ParcelFileDescriptor): SharedCounter {
            if (nativeSharedCounter == null) {
                // don't remove the following isDalvik check, it helps r8 cleanup the
                // ShadowSharedCounter code for android.
                if (!isDalvik()) {
                    // if it is null, we are not on Android so just use an in
                    // process shared counter as multi-process is not testable on
                    // Robolectric.
                    return ShadowSharedCounter()
                }
                // This actually will enver happen, because class creation would throw when
                // initializing nativeSharedCounter. But having it here helps future proofing as
                // well as the static analyzer.
                error(
                    """
                    DataStore failed to load the native library to create SharedCounter.
                """
                        .trimIndent()
                )
            }
            val nativeFd = pfd.getFd()
            if (nativeSharedCounter.nativeTruncateFile(nativeFd) != 0) {
                throw IOException("Failed to truncate counter file")
            }
            val address = nativeSharedCounter.nativeCreateSharedCounter(nativeFd)
            if (address < 0) {
                throw IOException("Failed to mmap counter file")
            }
            return RealSharedCounter(nativeSharedCounter, address)
        }

        internal fun create(produceFile: () -> File): SharedCounter {
            val file = produceFile()
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd =
                    ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE,
                    )
                return createCounterFromFd(pfd)
            } finally {
                pfd?.close()
            }
        }

        /** If you change this method, make sure to update the proguard rule. */
        private fun isDalvik(): Boolean {
            return "dalvik".equals(System.getProperty("java.vm.name"), ignoreCase = true)
        }
    }
}
