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

package androidx.compose.runtime

import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

// Jvm-only tests moved from ../nonEmulatorCommonTest/kotlin/androidx/compose/runtime/RecomposerTests.kt
// because they use jvm-only APIs
class RecomposerJvmTests {

    @Ignore // b/329682091
    @OptIn(DelicateCoroutinesApi::class)
    @Test // b/329011032
    fun validatePotentialDeadlock() = compositionTest {
        var state by mutableIntStateOf(0)
        compose {
            repeat(1000) {
                androidx.compose.runtime.mock.Text("This is some text: $state")
            }
            LaunchedEffect(Unit) {
                newSingleThreadContext("other thread").use {
                    while (true) {
                        withContext(it) {
                            state++
                            Snapshot.registerGlobalWriteObserver { }.dispose()
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                while (true) {
                    withFrameNanos {
                        state++
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
        }

        repeat(10) {
            state++
            advance(ignorePendingWork = true)
        }
    }
}