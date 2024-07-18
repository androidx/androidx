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

package androidx.datastore.core

import android.os.FileObserver
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.channelFlow

internal typealias FileMoveObserver = (String?) -> Unit

/**
 * A [FileObserver] wrapper that works around the Android bug that breaks observers when multiple
 * observers are registered on the same directory.
 *
 * see: b/37017033, b/279997241
 */
@Suppress("DEPRECATION")
internal class MulticastFileObserver private constructor(
    val path: String,
) : FileObserver(
    path,
    MOVED_TO
) {
    /**
     * The actual listeners.
     * We are using a CopyOnWriteArrayList because this field is modified by the companion object.
     */
    private val delegates = CopyOnWriteArrayList<FileMoveObserver>()
    override fun onEvent(event: Int, path: String?) {
        delegates.forEach {
            it(path)
        }
    }

    companion object {
        private val LOCK = Any()

        // visible for tests to be able to validate all observers are removed at the end
        @VisibleForTesting
        internal val fileObservers = mutableMapOf<String, MulticastFileObserver>()

        /**
         * Returns a `Flow` that emits a `Unit` every time the give [file] is changed.
         * It also emits a `Unit` when the file system observer is established.
         * Note that this class only observes move events as it is the only event needed for
         * DataStore.
         */
        @CheckResult
        fun observe(file: File) = channelFlow {
            val flowObserver = { fileName: String? ->
                if (fileName == file.name) {
                    // Note that, this block still be called after channel is closed as the disposal
                    // of the listener happens after the channel is closed.
                    // We don't need to check the result of `trySendBlocking` because we are not
                    // worried about missed events that happen after the channel is closed.
                    trySendBlocking(Unit)
                }
            }
            val disposeListener = observe(file.parentFile!!, flowObserver)
            // Send Unit after we create the observer on the filesystem, to denote "initialization".
            // This is not necessary for DataStore to function but it makes it easier to control
            // state in the MulticastFileObserverTest (e.g. test can know that the file system
            // observer is registered before continuing with assertions).
            send(Unit)
            awaitClose {
                disposeListener.dispose()
            }
        }

        /**
         * Creates a system level file observer (if needed) and starts observing the given [parent]
         * directory.
         *
         * Callers should dispose the returned handle when it is done.
         */
        @CheckResult
        private fun observe(
            parent: File,
            observer: FileMoveObserver
        ): DisposableHandle {
            val key = parent.canonicalFile.path
            synchronized(LOCK) {
                val filesystemObserver = fileObservers.getOrPut(key) {
                    MulticastFileObserver(key)
                }
                filesystemObserver.delegates.add(observer)
                if (filesystemObserver.delegates.size == 1) {
                    // start watching inside the lock so we can avoid the bug if multiple observers
                    // are registered/unregistered in parallel
                    filesystemObserver.startWatching()
                }
            }
            return DisposableHandle {
                synchronized(LOCK) {
                    fileObservers[key]?.let { filesystemObserver ->
                        filesystemObserver.delegates.remove(observer)
                        // return the instance if it needs to be stopped
                        if (filesystemObserver.delegates.isEmpty()) {
                            fileObservers.remove(key)
                            // stop watching inside the lock so we can avoid the bug if multiple
                            // observers are registered/unregistered in parallel
                            filesystemObserver.stopWatching()
                        }
                    }
                }
            }
        }

        /**
         * Used in tests to cleanup all observers.
         * There are tests that will potentially leak observers, which is usually OK but it is
         * harmful for the tests of [MulticastFileObserver], hence we provide this API to cleanup.
         */
        @VisibleForTesting
        internal fun removeAllObservers() {
            synchronized(LOCK) {
                fileObservers.values.forEach {
                    it.stopWatching()
                }
                fileObservers.clear()
            }
        }
    }
}
