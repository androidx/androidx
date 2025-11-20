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

package androidx.xr.arcore.testing

import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import java.util.ServiceLoader
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FakeStateExtenderTest {

    lateinit var underTest: FakeStateExtender

    @Before
    fun setUp() {
        underTest = FakeStateExtender()
    }

    @Test
    fun class_isDiscoverableViaServiceLoader() {
        val stateExtenders = ServiceLoader.load(StateExtender::class.java)

        assertThat(stateExtenders.any { it is FakeStateExtender || it is AnotherFakeStateExtender })
            .isTrue()
    }

    @Test
    fun initialize_setsInitializedToTrue() {
        check(!underTest.isInitialized)
        val lifecycleManager = FakeLifecycleManager()

        underTest.initialize(
            listOf(FakePerceptionRuntime(FakeLifecycleManager(), FakePerceptionManager()))
        )

        assertThat(underTest.isInitialized).isTrue()
    }

    @Test
    fun extend_addsStateToExtended(): Unit = runBlocking {
        check(underTest.extended.isEmpty())
        val state = createStubCoreState(TestTimeSource().markNow())

        underTest.extend(state)

        assertThat(underTest.extended).containsExactly(state)
    }

    private fun createStubCoreState(timeMark: ComparableTimeMark): CoreState {
        return CoreState(timeMark)
    }
}
