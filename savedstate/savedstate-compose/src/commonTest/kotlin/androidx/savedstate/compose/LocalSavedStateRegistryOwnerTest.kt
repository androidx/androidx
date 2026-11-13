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

package androidx.savedstate.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.HostDefaultProvider
import androidx.compose.runtime.LocalHostDefaultProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@IgnoreAndroidHostTest
@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalSavedStateRegistryOwnerTest {

    @Test
    fun localSavedStateRegistryOwner_resolvesFromHostDefaultProvider() = runComposeTest {
        val expectedOwner = FakeSavedStateRegistryOwner()
        val provider =
            TestHostDefaultProvider(mapOf(SavedStateRegistryOwnerHostDefaultKey to expectedOwner))
        var resolvedOwner: SavedStateRegistryOwner? = null

        setContent {
            CompositionLocalProvider(LocalHostDefaultProvider provides provider) {
                resolvedOwner = LocalSavedStateRegistryOwner.current
            }
        }

        assertThat(resolvedOwner).isSameInstanceAs(expectedOwner)
    }

    @Test
    fun localSavedStateRegistryOwner_explicitValueOverridesHostDefault() = runComposeTest {
        val hostOwner = FakeSavedStateRegistryOwner()
        val explicitOwner = FakeSavedStateRegistryOwner()
        val provider =
            TestHostDefaultProvider(mapOf(SavedStateRegistryOwnerHostDefaultKey to hostOwner))
        var resolvedOwner: SavedStateRegistryOwner? = null

        setContent {
            CompositionLocalProvider(LocalHostDefaultProvider provides provider) {
                CompositionLocalProvider(LocalSavedStateRegistryOwner provides explicitOwner) {
                    resolvedOwner = LocalSavedStateRegistryOwner.current
                }
            }
        }

        assertThat(resolvedOwner).isSameInstanceAs(explicitOwner)
    }

    @Test
    fun localSavedStateRegistryOwner_throwsWhenHostDefaultProviderNotPresent() = runComposeTest {
        assertFailsWith<IllegalStateException> {
            setContent {
                @Suppress("UNUSED_VARIABLE") val unused = LocalSavedStateRegistryOwner.current
            }
        }
    }

    @Test
    fun localSavedStateRegistryOwner_updatesWhenHostDefaultProviderChanges() = runComposeTest {
        val ownerA = FakeSavedStateRegistryOwner()
        val ownerB = FakeSavedStateRegistryOwner()
        val providerA =
            TestHostDefaultProvider(mapOf(SavedStateRegistryOwnerHostDefaultKey to ownerA))
        val providerB =
            TestHostDefaultProvider(mapOf(SavedStateRegistryOwnerHostDefaultKey to ownerB))

        var useProviderA by mutableStateOf(true)
        var resolvedOwner: SavedStateRegistryOwner? = null

        setContent {
            val currentProvider = if (useProviderA) providerA else providerB
            CompositionLocalProvider(LocalHostDefaultProvider provides currentProvider) {
                resolvedOwner = LocalSavedStateRegistryOwner.current
            }
        }

        assertThat(resolvedOwner).isSameInstanceAs(ownerA)

        useProviderA = false
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        assertThat(resolvedOwner).isSameInstanceAs(ownerB)
    }

    private class ComposeTestContext(
        val scope: TestScope,
        private val composition: Composition,
    ) {
        val testScheduler
            get() = scope.testScheduler

        fun setContent(content: @Composable () -> Unit) {
            composition.setContent(content)
        }
    }

    private fun runComposeTest(block: ComposeTestContext.() -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            val clock =
                object : MonotonicFrameClock {
                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
                }
            val recomposerContext: CoroutineContext = coroutineContext + clock
            val recomposer = Recomposer(recomposerContext)
            val recomposerJob =
                backgroundScope.launch(recomposerContext) {
                    recomposer.runRecomposeAndApplyChanges()
                }
            val applier =
                object : AbstractApplier<Unit>(Unit) {
                    override fun insertTopDown(index: Int, instance: Unit) {}

                    override fun insertBottomUp(index: Int, instance: Unit) {}

                    override fun remove(index: Int, count: Int) {}

                    override fun move(from: Int, to: Int, count: Int) {}

                    override fun onClear() {}
                }
            val composition = Composition(applier, recomposer)
            try {
                ComposeTestContext(this, composition).block()
            } finally {
                composition.dispose()
                recomposer.close()
                recomposerJob.cancel()
            }
        }

    private class TestHostDefaultProvider(private val values: Map<HostDefaultKey<*>, Any?>) :
        HostDefaultProvider {
        @Suppress("UNCHECKED_CAST")
        override fun <T> getHostDefault(key: HostDefaultKey<T>): T = values[key] as T
    }

    private class FakeSavedStateRegistryOwner : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry.createUnsafe(owner = this)
        private val controller = SavedStateRegistryController.create(owner = this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry
    }
}
