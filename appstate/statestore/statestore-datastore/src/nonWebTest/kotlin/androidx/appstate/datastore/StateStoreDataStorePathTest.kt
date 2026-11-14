/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.datastore

import androidx.appstate.statestore.StateStore
import androidx.appstate.statestore.StateStoreKey
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot.Companion.sendApplyNotifications
import androidx.kruth.assertThat
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import okio.FileSystem

/** The file-backed `syncToDataStore(path, scope)` overload, on platforms with a file system. */
class StateStoreDataStorePathTest {

    private val testFile =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test_datastore_${Random.nextInt()}.json"

    @AfterTest
    fun tearDown() {
        FileSystem.SYSTEM.delete(testFile, mustExist = false)
    }

    @Serializable
    object StringKey : StateStoreKey<String>("default"), PersistToDataStore<String> {
        override val name: String = "string"
        override val serializer: KSerializer<String> = String.serializer()
    }

    @Test
    fun testSyncToPathWritesAndRestoresFromTheFile() = runTest {
        val storedValue = "stored value"

        // 1st StateStore: write a value through the file-backed sync
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val first = StateStore()
        first.getState(StringKey)
        firstScope.launch { first.syncToDataStore(testFile.toString(), firstScope) }
        first.setState(StringKey, storedValue)
        sendApplyNotifications()
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                while ((FileSystem.SYSTEM.metadataOrNull(testFile)?.size ?: 0L) == 0L) delay(10)
            }
        }
        // DataStore allows one active instance per file: release it before opening the 2nd one
        firstScope.coroutineContext.job.cancelAndJoin()

        // 2nd StateStore: the same path restores the value
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val second = StateStore()
        val state = second.getState(StringKey)
        secondScope.launch { second.syncToDataStore(testFile.toString(), secondScope) }
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { snapshotFlow { state.value }.first { it == storedValue } }
        }
        assertThat(state.value).isEqualTo(storedValue)
        secondScope.coroutineContext.job.cancelAndJoin()
    }
}
