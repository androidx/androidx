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

package androidx.compose.runtime.retain

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.PausableComposition
import androidx.compose.runtime.PausedComposition
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.test.IgnoreWebTarget

class RetainTests {

    @Suppress("RetainRememberObserver")
    @Test
    fun retain_throwsForRememberObserver_noScope() = compositionTest {
        assertThrows<IllegalArgumentException> {
            compose { retain<RememberObserver> { ThrowingRememberObserver } }
        }
    }

    @Suppress("RetainRememberObserver")
    @Test
    fun retain_throwsForRememberObserver_forgetfulStore() = compositionTest {
        assertThrows<IllegalArgumentException> {
            compose {
                LocalRetainedValuesStoreProvider(ForgetfulRetainedValuesStore) {
                    retain<RememberObserver> { ThrowingRememberObserver }
                }
            }
        }
    }

    @Suppress("RetainRememberObserver")
    @Test
    fun retain_throwsForRememberObserver_controlledScope() = compositionTest {
        val store = ManagedRetainedValuesStore()
        assertThrows<IllegalArgumentException> {
            compose {
                LocalRetainedValuesStoreProvider(store) {
                    retain<RememberObserver> { ThrowingRememberObserver }
                }
            }
        }
    }

    @Test
    fun retain_notRetaining_remember() = compositionTest {
        val store = ManagedRetainedValuesStore()
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            LocalRetainedValuesStoreProvider(store) {
                lastSeen = retain {
                    assertNull(factoryResult, "Factory should only be called once")
                    CountingRetainObject().also { factoryResult = it }
                }
            }
        }

        val retained = factoryResult!!
        assertSame(retained, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_notRetaining_recompose() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            LocalRetainedValuesStoreProvider(store) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain {
                    assertNull(factoryResult, "Factory should only be called once")
                    CountingRetainObject().also { factoryResult = it }
                }
            }
        }

        val retained = factoryResult!!
        assertSame(factoryResult, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        recomposeScope.invalidate()
        expectNoChanges()
        assertSame(factoryResult, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_notRetaining_reconstruct() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var showContent = true

        store.disableRetainingExitedValues()

        compose {
            recomposeScope = currentRecomposeScope
            if (showContent) {
                LocalRetainedValuesStoreProvider(store) {
                    lastSeen = retain { CountingRetainObject().also { factoryResults += it } }
                }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        showContent = false
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertNull(lastSeen)
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)

        showContent = true
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 2)
        assertSame(factoryResults.last(), lastSeen)
        val secondResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_inForgetfulStore_synonymousToRemember() = compositionTest {
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null
        var includeContent = true

        compose {
            assertSame(
                ForgetfulRetainedValuesStore,
                LocalRetainedValuesStore.current,
                "Composition should use the ForgetfulRetainedValuesStore by default",
            )

            recomposeScope = currentRecomposeScope
            if (includeContent) {
                lastSeen = retain {
                    assertNull(factoryResult, "Factory should only be called once")
                    CountingRetainObject().also { factoryResult = it }
                }
            }
        }

        val retained1 = factoryResult!!
        assertSame(retained1, lastSeen)
        retained1.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        includeContent = false
        recomposeScope.invalidate()
        advance()
        retained1.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)

        includeContent = true
        factoryResult = null
        recomposeScope.invalidate()
        advance()
        val retained2 = factoryResult!!
        assertSame(retained2, lastSeen)
        assertNotSame(retained1, retained2)
        retained1.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        retained2.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_retaining_remember() = compositionTest {
        val store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            LocalRetainedValuesStoreProvider(store) {
                lastSeen = retain {
                    assertNull(factoryResult, "Factory should only be called once")
                    CountingRetainObject().also { factoryResult = it }
                }
            }
        }

        val retained = factoryResult!!
        assertSame(retained, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_retaining_recompose() = compositionTest {
        val store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            LocalRetainedValuesStoreProvider(store) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain {
                    assertNull(factoryResult, "Factory should only be called once")
                    CountingRetainObject().also { factoryResult = it }
                }
            }
        }

        val retained = factoryResult!!
        assertSame(retained, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        recomposeScope.invalidate()
        expectNoChanges()
        assertSame(retained, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_retaining_reconstruct() = compositionTest {
        val store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null
        var showContent = true

        compose {
            recomposeScope = currentRecomposeScope
            if (showContent) {
                LocalRetainedValuesStoreProvider(store) {
                    lastSeen = retain {
                        assertNull(factoryResult, "Factory should only be called once")
                        CountingRetainObject().also { factoryResult = it }
                    }
                }
            }
        }

        val retained = factoryResult!!
        assertSame(retained, lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        showContent = false
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertNull(lastSeen)
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        showContent = true
        recomposeScope.invalidate()
        expectChanges()
        assertSame(factoryResult, lastSeen)
        retained.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
    }

    @Test
    fun retain_recomputesForNewKeys_whenNotRetaining() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            LocalRetainedValuesStoreProvider(store) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        key = "456"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 2)
        assertSame(factoryResults.last(), lastSeen)
        val secondResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        key = "123"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 3)
        assertSame(factoryResults.last(), lastSeen)
        val thirdResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        thirdResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_reusesForPreviousKeys_whenNotRetaining() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            LocalRetainedValuesStoreProvider(store) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        key = "123"
        lastSeen = null
        recomposeScope.invalidate()
        expectNoChanges()
        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_forgetsValuesWhenKeysChange_whenNotRetaining() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            recomposeScope = currentRecomposeScope
            LocalRetainedValuesStoreProvider(store) {
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store.enableRetainingExitedValues()
        key = "456"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 2)
        assertSame(factoryResults.last(), lastSeen)
        val secondResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        key = "123"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 3)
        assertSame(factoryResults.last(), lastSeen)
        assertNotSame(firstResult, factoryResults.last())
        val thirdResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
        thirdResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retain_remembersValuesWithSameKeys_whenRetaining() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            LocalRetainedValuesStoreProvider(store) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store.enableRetainingExitedValues()
        key = "123"
        lastSeen = null
        recomposeScope.invalidate()
        expectNoChanges()
        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
    }

    @Test
    fun retainObserver_callbackOrdering() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        var includeContent = true
        var retainedValues = emptyList<LoggingRetainObject>()

        val callbackLog = mutableListOf<String>()
        val retainSequence = buildList {
            add(LoggingRetainObject("Foo", callbackLog))
            add(LoggingRetainObject("Bar", callbackLog))
            add(LoggingRetainObject("Baz", callbackLog))
            add(LoggingRetainObject("Buzz", callbackLog))
        }

        compose {
            recomposeScope = currentRecomposeScope
            if (includeContent) {
                LocalRetainedValuesStoreProvider(store) {
                    retainedValues = buildList {
                        add(retain { retainSequence[0] })
                        add(retain { retainSequence[1] })
                        add(retain { retainSequence[2] })
                        add(retain { retainSequence[3] })
                    }
                }
            }
        }

        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "Retain(Foo)",
                "EnterComposition(Foo)",
                "Retain(Bar)",
                "EnterComposition(Bar)",
                "Retain(Baz)",
                "EnterComposition(Baz)",
                "Retain(Buzz)",
                "EnterComposition(Buzz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.enableRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(Buzz)",
                "ExitComposition(Baz)",
                "ExitComposition(Bar)",
                "ExitComposition(Foo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = true
        recomposeScope.invalidate()
        advance()
        store.disableRetainingExitedValues()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "EnterComposition(Foo)",
                "EnterComposition(Bar)",
                "EnterComposition(Baz)",
                "EnterComposition(Buzz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(Buzz)",
                "Retire(Buzz)",
                "ExitComposition(Baz)",
                "Retire(Baz)",
                "ExitComposition(Bar)",
                "Retire(Bar)",
                "ExitComposition(Foo)",
                "Retire(Foo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = true
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "Retain(Foo)",
                "EnterComposition(Foo)",
                "Retain(Bar)",
                "EnterComposition(Bar)",
                "Retain(Baz)",
                "EnterComposition(Baz)",
                "Retain(Buzz)",
                "EnterComposition(Buzz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.enableRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(Buzz)",
                "ExitComposition(Baz)",
                "ExitComposition(Bar)",
                "ExitComposition(Foo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.disableRetainingExitedValues()
        assertEquals(
            listOf("Retire(Buzz)", "Retire(Baz)", "Retire(Bar)", "Retire(Foo)"),
            callbackLog,
        )
    }

    @Test
    fun retain_callbackOrdering_relativeToRememberObserver() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        var includeContent = true
        var retainedValues = emptyList<Any>()

        val callbackLog = mutableListOf<String>()
        val retainSequence = buildList {
            add(LoggingRetainObject("RetainedFoo", callbackLog))
            add(LoggingRememberObject("RememberedFoo", callbackLog))
            add(LoggingRetainObject("RetainedBar", callbackLog))
            add(LoggingRememberObject("RememberedBar", callbackLog))
            add(LoggingRetainObject("RetainedBaz", callbackLog))
        }

        compose {
            recomposeScope = currentRecomposeScope
            if (includeContent) {
                LocalRetainedValuesStoreProvider(store) {
                    retainedValues = buildList {
                        add(retain { retainSequence[0] })
                        add(remember { retainSequence[1] })
                        add(retain { retainSequence[2] })
                        add(remember { retainSequence[3] })
                        add(retain { retainSequence[4] })
                    }
                }
            }
        }

        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "Retain(RetainedFoo)",
                "EnterComposition(RetainedFoo)",
                "Remember(RememberedFoo)",
                "Retain(RetainedBar)",
                "EnterComposition(RetainedBar)",
                "Remember(RememberedBar)",
                "Retain(RetainedBaz)",
                "EnterComposition(RetainedBaz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.enableRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(RetainedBaz)",
                "Forget(RememberedBar)",
                "ExitComposition(RetainedBar)",
                "Forget(RememberedFoo)",
                "ExitComposition(RetainedFoo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = true
        recomposeScope.invalidate()
        advance()
        store.disableRetainingExitedValues()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "EnterComposition(RetainedFoo)",
                "Remember(RememberedFoo)",
                "EnterComposition(RetainedBar)",
                "Remember(RememberedBar)",
                "EnterComposition(RetainedBaz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(RetainedBaz)",
                "Retire(RetainedBaz)",
                "Forget(RememberedBar)",
                "ExitComposition(RetainedBar)",
                "Retire(RetainedBar)",
                "Forget(RememberedFoo)",
                "ExitComposition(RetainedFoo)",
                "Retire(RetainedFoo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        includeContent = true
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "Retain(RetainedFoo)",
                "EnterComposition(RetainedFoo)",
                "Remember(RememberedFoo)",
                "Retain(RetainedBar)",
                "EnterComposition(RetainedBar)",
                "Remember(RememberedBar)",
                "Retain(RetainedBaz)",
                "EnterComposition(RetainedBaz)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.enableRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(retainSequence, retainedValues, "Retained unexpected objects")
        assertEquals(
            listOf(
                "ExitComposition(RetainedBaz)",
                "Forget(RememberedBar)",
                "ExitComposition(RetainedBar)",
                "Forget(RememberedFoo)",
                "ExitComposition(RetainedFoo)",
            ),
            callbackLog,
        )

        callbackLog.clear()
        store.disableRetainingExitedValues()
        assertEquals(
            listOf("Retire(RetainedBaz)", "Retire(RetainedBar)", "Retire(RetainedFoo)"),
            callbackLog,
        )
    }

    @Test
    fun changingRetainedValuesStore_adoptsObjectsToNewScope() = compositionTest {
        var store: RetainedValuesStore =
            ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var includeContent = true

        compose {
            recomposeScope = currentRecomposeScope
            if (includeContent) {
                LocalRetainedValuesStoreProvider(store) {
                    @Suppress("UnusedVariable")
                    val retained = retain { CountingRetainObject().also { factoryResults += it } }
                }
            }
        }

        store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store = ForgetfulRetainedValuesStore
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store = ManagedRetainedValuesStore()
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store.enableRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        store.disableRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Test
    fun retain_inMovableContent_experiencesOriginRetentionPolicy() = compositionTest {
        val store = ManagedRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var showContent = true

        compose {
            recomposeScope = currentRecomposeScope
            val content = remember {
                movableContentOf {
                    @Suppress("UnusedVariable")
                    val retained = retain { CountingRetainObject().also { factoryResults += it } }
                }
            }
            if (showContent) {
                LocalRetainedValuesStoreProvider(store) { Linear { content() } }
            }
        }

        assertEquals(1, factoryResults.size, "Only one object should be retained")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        store.enableRetainingExitedValues()
        showContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        showContent = true
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)

        store.disableRetainingExitedValues()
        store.enableRetainingExitedValues()
        showContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 2, exited = 2, retired = 0)
        store.disableRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 2, exited = 2, retired = 1)
    }

    @Test
    fun retain_inMovableContent_adoptsToDestinationScope() = compositionTest {
        val storeA = ManagedRetainedValuesStore()
        val storeB = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var moveContent = false
        var showContent = true

        compose {
            recomposeScope = currentRecomposeScope
            val content = remember {
                movableContentOf {
                    @Suppress("UnusedVariable")
                    val retained = retain { CountingRetainObject().also { factoryResults += it } }
                }
            }
            if (showContent) {
                LocalRetainedValuesStoreProvider(storeA) { Linear { if (!moveContent) content() } }
                LocalRetainedValuesStoreProvider(storeB) { Linear { if (moveContent) content() } }
            }
        }

        assertEquals(1, factoryResults.size, "Only one object should be retained")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        moveContent = true
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        showContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        storeB.disableRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Test
    fun retain_duplicateRetainKeys() = compositionTest {
        val store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var showContent = true

        compose {
            recomposeScope = currentRecomposeScope
            if (showContent) {
                LocalRetainedValuesStoreProvider(store) {
                    factoryResults += retain { CountingRetainObject() }
                    factoryResults += retain { CountingRetainObject() }
                    factoryResults += retain { CountingRetainObject() }
                    factoryResults += retain { CountingRetainObject() }
                    factoryResults += retain { CountingRetainObject() }
                }
            }
        }

        val initialRetainedValues = factoryResults.toList()
        factoryResults.clear()
        assertEquals(5, initialRetainedValues.size)
        initialRetainedValues.forEach { retained ->
            retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
        }

        showContent = false
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(0, factoryResults.size)
        initialRetainedValues.forEach { retained ->
            retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)
        }

        showContent = true
        recomposeScope.invalidate()
        expectChanges()
        val updatedRetainedValues = factoryResults.toList()
        assertEquals(initialRetainedValues, updatedRetainedValues)
        initialRetainedValues.forEach { retained ->
            retained.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
        }
    }

    @Test
    fun retain_explicitKey_groupCollision() = compositionTest {
        val store = ManagedRetainedValuesStore().apply { enableRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var showContent = true
        var banFactoryObjectCreation = false

        compose {
            recomposeScope = currentRecomposeScope
            if (showContent) {
                LocalRetainedValuesStoreProvider(store) {
                    val compositeKeyHashCodes = mutableSetOf<CompositeKeyHashCode>()
                    key("A") {
                        assertTrue(
                            compositeKeyHashCodes.add(currentCompositeKeyHashCode),
                            "Expected this group to have a unique compositeKeyHashCode.",
                        )
                        key("X") {
                            assertTrue(
                                compositeKeyHashCodes.add(currentCompositeKeyHashCode),
                                "Expected this group to have a unique compositeKeyHashCode.",
                            )
                            val text = retain {
                                if (banFactoryObjectCreation) {
                                    throw IllegalStateException("Attempted to execute factory")
                                }
                                "X1"
                            }
                            Text(text)
                        }
                    }
                    key("A") {
                        assertFalse(
                            compositeKeyHashCodes.add(currentCompositeKeyHashCode),
                            "Expected this group to have a duplicate compositeKeyHashCode.",
                        )
                        key("X") {
                            assertFalse(
                                compositeKeyHashCodes.add(currentCompositeKeyHashCode),
                                "Expected this group to have a duplicate compositeKeyHashCode.",
                            )
                            val text = retain {
                                if (banFactoryObjectCreation) {
                                    throw IllegalStateException("Attempted to execute factory")
                                }
                                "X2"
                            }
                            Text(text)
                        }
                    }
                }
            }
        }

        validate {
            Text("X1")
            Text("X2")
        }

        showContent = false
        banFactoryObjectCreation = true
        recomposeScope.invalidate()
        expectChanges()
        validate {}

        showContent = true
        recomposeScope.invalidate()
        expectChanges()
        validate {
            Text("X1")
            Text("X2")
        }
    }

    // Ignore JS targets: b/444012850
    @IgnoreWebTarget
    @Test
    fun abandonCompositionTest() {
        var failComposition by mutableStateOf(false)
        val store1 = ManagedRetainedValuesStore()
        val store2 = ManagedRetainedValuesStore()
        val events = mutableListOf<String>()

        try {
            compositionTest {
                compose {
                    LocalRetainedValuesStoreProvider(store1) {
                        retain<LoggingRetainObject> { LoggingRetainObject("A", events) }
                        if (failComposition) {
                            retain<LoggingRetainObject> { LoggingRetainObject("B", events) }
                        }
                    }

                    LocalRetainedValuesStoreProvider(store2) {
                        retain<LoggingRetainObject> { LoggingRetainObject("C", events) }
                        if (failComposition) {
                            retain<LoggingRetainObject> { LoggingRetainObject("D", events) }
                        }
                    }

                    if (failComposition) {
                        events += "throw"
                        throw RuntimeException("Abandoning composition")
                    }
                }

                assertContentEquals(
                    listOf("Retain(A)", "EnterComposition(A)", "Retain(C)", "EnterComposition(C)"),
                    events,
                )
                failComposition = true
                events += "recompose"
                try {
                    advance()
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            if (!failComposition) throw t
        }

        assertContent(events) {
            eq("Retain(A)")
            eq("EnterComposition(A)")
            eq("Retain(C)")
            eq("EnterComposition(C)")
            eq("recompose")
            eq("throw")
            inAnyOrder("Unused(B)", "Unused(D)")
            eq("ExitComposition(C)")
            eq("ExitComposition(A)")
        }

        store2.disableRetainingExitedValues()
        store1.disableRetainingExitedValues()

        assertContent(events) {
            eq("Retain(A)")
            eq("EnterComposition(A)")
            eq("Retain(C)")
            eq("EnterComposition(C)")
            eq("recompose")
            eq("throw")
            inAnyOrder("Unused(B)", "Unused(D)")
            eq("ExitComposition(C)")
            eq("ExitComposition(A)")
            eq("Retire(C)")
            eq("Retire(A)")
        }
    }

    @Test
    fun retainedContentHostInitializationTest() = compositionTest {
        val parentStore = ManagedRetainedValuesStore()
        val childStore = ManagedRetainedValuesStore()
        var retainedContentHostScope: RetainedValuesStore? = null
        var showContent by mutableStateOf(true)
        var retainedCounter = 0

        compose {
            LocalRetainedValuesStoreProvider(parentStore) {
                if (showContent) {
                    LocalRetainedValuesStoreProvider(childStore) {
                        if (retainedContentHostScope != null) {
                            assertSame(retainedContentHostScope, LocalRetainedValuesStore.current)
                        } else {
                            retainedContentHostScope = LocalRetainedValuesStore.current
                        }

                        Text(retain { retainedCounter++.toString() })
                        Text(retain { retainedCounter++.toString() })
                        Text(retain { retainedCounter++.toString() })
                    }
                }
            }
        }

        validate {
            Text("0")
            Text("1")
            Text("2")
        }
        expectNoChanges()
        assertNotNull(retainedContentHostScope, "retainedContentHostScope not initialized")
        assertFalse(retainedContentHostScope.isRetainingExitedValues)
    }

    @Test
    fun retainLocalRetainedValuesStoreTest() = compositionTest {
        val parentStore = ManagedRetainedValuesStore()
        var childStore: RetainedValuesStore? = null
        var showContent by mutableStateOf(true)
        var showParent by mutableStateOf(true)
        var retainedCounter = 0

        compose {
            if (showParent) {
                LocalRetainedValuesStoreProvider(parentStore) {
                    val retainedStore = retainManagedRetainedValuesStore()
                    if (childStore != null) {
                        assertSame(childStore, retainedStore)
                    }
                    childStore = retainedStore
                    if (showContent) {
                        LocalRetainedValuesStoreProvider(childStore) {
                            Text(retain { retainedCounter++.toString() })
                            Text(retain { retainedCounter++.toString() })
                            Text(retain { retainedCounter++.toString() })
                        }
                    }
                }
            }
        }

        expectNoChanges()
        assertNotNull(childStore, "Child retainedValuesStore was not initialized")
        assertNotSame(
            parentStore,
            childStore,
            "ProvideLocalRetainedValuesStore did not update the LocalRetainedValuesStore.",
        )
        assertFalse(
            childStore.isRetainingExitedValues,
            "Child RetainedValuesStore should be initialized as not retaining",
        )

        validate {
            Text("0")
            Text("1")
            Text("2")
        }

        showContent = false
        expectChanges()
        validate {}
        assertTrue(
            childStore.isRetainingExitedValues,
            "Child RetainedValuesStore should be retaining values when inactive",
        )

        showContent = true
        expectChanges()
        validate {
            Text("0")
            Text("1")
            Text("2")
        }

        assertFalse(
            childStore.isRetainingExitedValues,
            "Child RetainedValuesStore should stop retaining after being restored",
        )
        showParent = false
        expectChanges()
        validate {}
        assertTrue(
            parentStore.isRetainingExitedValues,
            "Parent RetainedValuesStore should stop retaining after being restored",
        )
        assertTrue(
            childStore.isRetainingExitedValues,
            "Child RetainedValuesStore should match parent state",
        )

        parentStore.disableRetainingExitedValues()
        assertFalse(
            parentStore.isRetainingExitedValues,
            "Parent RetainedValuesStore should forget all retained values when disabled",
        )
        assertFalse(
            childStore.isRetainingExitedValues,
            "Child RetainedValuesStore be retired with parent",
        )
    }

    @Test
    fun retainedValuesStoreRegistry_retainsScopes() = compositionTest {
        val knownKeys = listOf("A", "B", "C", "D", "E", "F")
        var visibleKeys by mutableStateOf(knownKeys.take(3))
        val retainedValues = knownKeys.associateWith { mutableListOf<CountingRetainObject>() }

        compose {
            with(retainRetainedValuesStoreRegistry()) {
                visibleKeys.forEach { visibleKey ->
                    key(visibleKey) {
                        LocalRetainedValuesStoreProvider(visibleKey) {
                            repeat(5) { item ->
                                use(
                                    retain {
                                        CountingRetainObject().also {
                                            retainedValues[visibleKey]!!.add(it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        knownKeys.forEach { key ->
            val retainedValuesForKey = retainedValues[key]!!
            when (key) {
                "A",
                "B",
                "C" -> {
                    assertEquals(
                        5,
                        retainedValuesForKey.size,
                        "Retained an unexpected number of values for \"$key\"",
                    )
                    retainedValuesForKey.forEach {
                        it.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
                    }
                }
                "D",
                "E",
                "F" ->
                    assertEquals(
                        0,
                        retainedValuesForKey.size,
                        "No values should be retained for key \"$key\"",
                    )
            }
        }

        visibleKeys = knownKeys
        advance()
        knownKeys.forEach { key ->
            val retainedValuesForKey = retainedValues[key]!!
            assertEquals(
                5,
                retainedValuesForKey.size,
                "Retained an unexpected number of values for \"$key\"",
            )
            retainedValuesForKey.forEach {
                it.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
            }
        }

        visibleKeys = knownKeys.takeLast(3)
        advance()
        knownKeys.forEach { key ->
            val retainedValuesForKey = retainedValues[key]!!
            assertEquals(
                5,
                retainedValuesForKey.size,
                "Retained an unexpected number of values for \"$key\"",
            )
            when (key) {
                "A",
                "B",
                "C" -> {
                    retainedValuesForKey.forEach {
                        it.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)
                    }
                }
                "D",
                "E",
                "F" -> {
                    retainedValuesForKey.forEach {
                        it.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
                    }
                }
            }
        }

        visibleKeys = knownKeys
        advance()
        knownKeys.forEach { key ->
            val retainedValuesForKey = retainedValues[key]!!
            assertEquals(
                5,
                retainedValuesForKey.size,
                "Retained an unexpected number of values for \"$key\"",
            )
            when (key) {
                "A",
                "B",
                "C" -> {
                    retainedValuesForKey.forEach {
                        it.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
                    }
                }
                "D",
                "E",
                "F" -> {
                    retainedValuesForKey.forEach {
                        it.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)
                    }
                }
            }
        }
    }

    @Test
    fun retainedValuesStoreRegistry_clearChild_removesScope() = compositionTest {
        val provider = RetainedValuesStoreRegistry()
        var globalCounter = 0
        var includeA by mutableStateOf(true)
        var includeB by mutableStateOf(true)
        lateinit var storeA: RetainedValuesStore
        lateinit var storeB: RetainedValuesStore
        compose {
            if (includeA) {
                provider.LocalRetainedValuesStoreProvider("A") {
                    storeA = LocalRetainedValuesStore.current
                    Text("A:" + retain { globalCounter++ })
                }
            }
            if (includeB) {
                provider.LocalRetainedValuesStoreProvider("B") {
                    storeB = LocalRetainedValuesStore.current
                    Text("B:" + retain { globalCounter++ })
                }
            }
        }

        var expectedAText = "A:0"
        var expectedBText = "B:1"
        validate {
            if (includeA) Text(expectedAText)
            if (includeB) Text(expectedBText)
        }

        includeA = false
        expectChanges()
        revalidate()
        val originalScopeA = storeA
        assertTrue(storeA.isRetainingExitedValues, "Removed scope should be retaining")
        assertFalse(storeB.isRetainingExitedValues, "Unremoved scope should be retaining")
        provider.clearChild("A")
        assertFalse(storeA.isRetainingExitedValues, "Cleared scope should stop retaining")

        expectedAText = "A:2"
        includeA = true
        expectChanges()
        revalidate()

        assertNotSame(originalScopeA, storeA, "Cleared scope should be recreated when requested")
    }

    @Test
    fun retainRetainedValuesStoreRegistry_retireDispose() = compositionTest {
        val parentStore = ManagedRetainedValuesStore()
        lateinit var childStore: RetainedValuesStore
        lateinit var retainedValuesStoreRegistry: RetainedValuesStoreRegistry
        val events = mutableListOf<String>()
        var includeParentStore by mutableStateOf(true)
        var includeChildStore by mutableStateOf(true)

        compose {
            if (includeParentStore) {
                LocalRetainedValuesStoreProvider(parentStore) {
                    retain<LoggingRetainObject> { LoggingRetainObject("A", events) }
                    retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
                    if (includeChildStore) {
                        retainedValuesStoreRegistry.LocalRetainedValuesStoreProvider("B") {
                            childStore = LocalRetainedValuesStore.current
                            retain<LoggingRetainObject> { LoggingRetainObject("B", events) }
                        }
                    }
                }
            }
        }

        expectNoChanges()

        assertEquals(
            listOf("Retain(A)", "EnterComposition(A)", "Retain(B)", "EnterComposition(B)"),
            events,
        )

        assertFalse(childStore.isRetainingExitedValues)

        includeChildStore = false
        advance()

        assertEquals(
            listOf(
                "Retain(A)",
                "EnterComposition(A)",
                "Retain(B)",
                "EnterComposition(B)",
                "ExitComposition(B)",
            ),
            events,
        )

        assertTrue(childStore.isRetainingExitedValues)
        includeParentStore = false
        advance()
        assertEquals(
            listOf(
                "Retain(A)",
                "EnterComposition(A)",
                "Retain(B)",
                "EnterComposition(B)",
                "ExitComposition(B)",
                "ExitComposition(A)",
            ),
            events,
        )

        assertTrue(parentStore.isRetainingExitedValues)
        assertTrue(childStore.isRetainingExitedValues)
        parentStore.disableRetainingExitedValues()
        assertFalse(childStore.isRetainingExitedValues)
        assertContent(events) {
            eq("Retain(A)")
            eq("EnterComposition(A)")
            eq("Retain(B)")
            eq("EnterComposition(B)")
            eq("ExitComposition(B)")
            eq("ExitComposition(A)")
            inAnyOrder("Retire(A)", "Retire(B)")
        }
    }

    // Ignore JS targets: b/444012850
    @IgnoreWebTarget
    @Test
    fun retainedValuesStoreRegistry_manualDispose() {
        var shouldThrowException = false
        assertThrows<IllegalStateException>(
            throwableAssertion = {
                assertEquals(
                    "Cannot get a RetainedValuesStore after a RetainedValuesStoreRegistry " +
                        "has been disposed.",
                    it.message,
                )
            }
        ) {
            compositionTest {
                val parentStore = ManagedRetainedValuesStore()
                lateinit var childStore: RetainedValuesStore
                lateinit var retainedValuesStoreRegistry: RetainedValuesStoreRegistry
                val events = mutableListOf<String>()
                var includeChildStore by mutableStateOf(true)

                compose {
                    LocalRetainedValuesStoreProvider(parentStore) {
                        retain<LoggingRetainObject> { LoggingRetainObject("A", events) }
                        retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
                        if (includeChildStore) {
                            retainedValuesStoreRegistry.LocalRetainedValuesStoreProvider("B") {
                                childStore = LocalRetainedValuesStore.current
                                retain<LoggingRetainObject> { LoggingRetainObject("B", events) }
                            }
                        }
                    }
                }

                expectNoChanges()

                assertEquals(
                    listOf("Retain(A)", "EnterComposition(A)", "Retain(B)", "EnterComposition(B)"),
                    events,
                )

                assertFalse(childStore.isRetainingExitedValues)

                includeChildStore = false
                advance()

                assertEquals(
                    listOf(
                        "Retain(A)",
                        "EnterComposition(A)",
                        "Retain(B)",
                        "EnterComposition(B)",
                        "ExitComposition(B)",
                    ),
                    events,
                )

                assertTrue(childStore.isRetainingExitedValues)
                retainedValuesStoreRegistry.dispose()
                includeChildStore = true

                shouldThrowException = true
                advance()
            }
        }

        assertTrue(shouldThrowException, "Test threw expected exception too early")
    }

    @Test
    fun provideRetainedValuesStore_inPausableComposition() = compositionTest {
        val store = ManagedRetainedValuesStore()
        val events = mutableListOf<String>()
        var enableProvider by mutableStateOf(true)
        var retainKey by mutableStateOf("A")

        val awaiter = Awaiter()
        val workflow: suspend (PausedComposition) -> Unit = { pausedComposition ->
            advance()
            pausedComposition.resumeTillComplete()

            enableProvider = false
            advance()
            pausedComposition.resumeTillComplete()

            enableProvider = true
            advance()
            pausedComposition.resumeTillComplete()

            retainKey = "B"
            advance()
            pausedComposition.resumeTillComplete()

            enableProvider = false
            advance()
            pausedComposition.resumeTillComplete()

            enableProvider = true
            advance()
            pausedComposition.resumeTillComplete()

            pausedComposition.apply()
            awaiter.done()
        }

        compose {
            PausableContent(workflow) {
                if (enableProvider) {
                    LocalRetainedValuesStoreProvider(store) {
                        use(retain(retainKey) { LoggingRetainObject(retainKey, events) })
                    }
                }
            }
        }

        awaiter.await()
        assertContent(events) {
            eq("Retain(B)")
            eq("EnterComposition(B)")
            inAnyOrder("Unused(B)", "Unused(A)", "Unused(A)")
        }
    }

    @Test
    fun provideRetainedValuesStore_inMovableContent() = compositionTest {
        val events = mutableListOf<String>()
        val store = ManagedRetainedValuesStore()
        val movableContent = movableContentOf {
            LocalRetainedValuesStoreProvider(store) {
                use(retain { LoggingRetainObject("Movable", events) })
            }
        }

        var includeMovableContent by mutableStateOf(true)
        var moveContent by mutableStateOf(false)
        compose {
            if (!includeMovableContent) {
                LocalRetainedValuesStoreProvider(store) {
                    use(retain { LoggingRetainObject("NotMovable", events) })
                }
            } else {
                Linear { if (!moveContent) movableContent() }

                Linear { if (moveContent) movableContent() }
            }
        }

        advance()
        assertContentEquals(listOf("Retain(Movable)", "EnterComposition(Movable)"), events)
        assertFalse(store.isRetainingExitedValues)

        moveContent = true
        advance()

        assertContentEquals(listOf("Retain(Movable)", "EnterComposition(Movable)"), events)
        assertFalse(store.isRetainingExitedValues)

        includeMovableContent = false
        advance()

        assertContentEquals(
            listOf(
                "Retain(Movable)",
                "EnterComposition(Movable)",
                "Retain(NotMovable)",
                "EnterComposition(NotMovable)",
                "ExitComposition(Movable)",
                "Retire(Movable)",
            ),
            events,
        )
        assertFalse(store.isRetainingExitedValues)

        includeMovableContent = true
        advance()

        assertContentEquals(
            listOf(
                "Retain(Movable)",
                "EnterComposition(Movable)",
                "Retain(NotMovable)",
                "EnterComposition(NotMovable)",
                "ExitComposition(Movable)",
                "Retire(Movable)",
                "ExitComposition(NotMovable)",
                "Retain(Movable)",
                "EnterComposition(Movable)",
                "Retire(NotMovable)",
            ),
            events,
        )
        assertFalse(store.isRetainingExitedValues)
    }

    private inline fun <reified T : Throwable> assertThrows(
        throwableAssertion: (T) -> Unit = {},
        block: () -> Unit,
    ) {
        var didSucceed = false
        try {
            block()
            didSucceed = true
        } catch (t: Throwable) {
            assertEquals(T::class, t::class, "Block threw unexpected exception type")
            throwableAssertion(t as T)
        } finally {
            if (didSucceed) fail("Expected an exception of type ${T::class.simpleName}")
        }
    }

    private fun <T : Any> assertContent(
        actual: List<T>,
        comparison: ContentAssertionBlock<T>.() -> Unit,
    ) {
        ContentAssertionBlock(actual).apply {
            comparison()
            finish()
        }
    }

    private class ContentAssertionBlock<T : Any>(val actual: List<T>) {
        private var index = 0
        private val messages = mutableListOf<String>()

        fun eq(value: T) {
            if (index >= actual.size) {
                messages += "Missing item at index $index: <$value>"
            } else if (actual[index] != value) {
                messages += "Wrong item at index $index. Was <${actual[index]}>, expected <$value>."
            }
            index++
        }

        fun inAnyOrder(vararg values: T) {
            val remainingValues = values.toMutableList()
            val targetIndex = index + values.size
            while (remainingValues.isNotEmpty() && index < targetIndex) {
                anyOf(remainingValues)?.let { remainingValues -= it }
            }
        }

        fun anyOf(values: List<T>): T? {
            if (index >= actual.size) {
                messages +=
                    "Missing item at index $index: One of " + "[${values.joinToString { "<$it>" }}]"
            } else if (actual[index] !in values) {
                messages +=
                    "Wrong item at index $index. Was <${actual[index]}>, expected one of " +
                        "[${values.joinToString { "<$it>" }}]"
            }
            return actual.getOrNull(index++)
        }

        fun finish() {
            if (messages.isNotEmpty()) {
                fail("Element comparison failed:" + messages.joinToString { "\n\t$it" })
            }
        }
    }

    @Stable
    private class CountingRetainObject : RetainObserver {
        var retained = 0
            private set

        var entered = 0
            private set

        var exited = 0
            private set

        var retired = 0
            private set

        var unused = 0
            private set

        override fun onRetained() {
            retained++
        }

        override fun onEnteredComposition() {
            entered++
            assertValidCounts()
        }

        override fun onExitedComposition() {
            exited++
            assertValidCounts()
        }

        override fun onRetired() {
            retired++
            assertValidCounts()
        }

        override fun onUnused() {
            unused++
            assertValidCounts()
        }

        fun assertCounts(
            retained: Int = this.retained,
            entered: Int = this.entered,
            exited: Int = this.exited,
            retired: Int = this.retired,
            unused: Int = this.unused,
        ) {
            assertEquals(
                "[Retained: $retained, Entered: $entered, Exited: $exited, Retired: $retired, " +
                    "Unused: $unused]",
                "[Retained: ${this.retained}, Entered: ${this.entered}, Exited: ${this.exited}, " +
                    "Retired: ${this.retired}, Unused: ${this.unused}]",
                "Received an unexpected number of callback invocations",
            )
        }

        private fun assertValidCounts() {
            if (retained == 0 && entered + exited + retired > 0) {
                fail("RetainObject received events without being retained")
            }

            if (retained < retired) {
                fail("RetainObject was retired more times than it was retained")
            }

            if (exited > entered) {
                fail("RetainObject exited the composition more times than it entered")
            }

            if (entered > retained + exited) {
                fail("RetainObject re-entered the composition without first exiting")
            }
        }
    }

    @Stable
    private class LoggingRetainObject(val name: String, val output: MutableList<String>) :
        RetainObserver {

        override fun onRetained() {
            output += "Retain($name)"
        }

        override fun onEnteredComposition() {
            output += "EnterComposition($name)"
        }

        override fun onExitedComposition() {
            output += "ExitComposition($name)"
        }

        override fun onRetired() {
            output += "Retire($name)"
        }

        override fun onUnused() {
            output += "Unused($name)"
        }
    }

    @Stable
    private class LoggingRememberObject(val name: String, val output: MutableList<String>) :
        RememberObserver {

        override fun onRemembered() {
            output += "Remember($name)"
        }

        override fun onForgotten() {
            output += "Forget($name)"
        }

        override fun onAbandoned() {
            output += "Abandon($name)"
        }
    }

    private object ThrowingRememberObserver : RememberObserver {
        override fun onRemembered() {
            throw UnsupportedOperationException(
                "RememberObserver.onRemembered() should not be called"
            )
        }

        override fun onForgotten() {
            throw UnsupportedOperationException(
                "RememberObserver.onForgotten() should not be called"
            )
        }

        override fun onAbandoned() {
            throw UnsupportedOperationException(
                "RememberObserver.onAbandoned() should not be called"
            )
        }
    }

    private fun use(@Suppress("unused") value: Any) {}
}

@Composable
private fun PausableContent(
    workflow: suspend PausedComposition.() -> Unit,
    createApplier: (view: View) -> Applier<View> = { ViewApplier(it) },
    content: @Composable () -> Unit,
) {
    val host = remember { View().also { it.name = "PausableContentHost" } }
    ComposeNode<View, ViewApplier>(factory = { host }, update = {})
    val parent = rememberCompositionContext()
    val composition =
        remember(parent) {
            val pausableContent = View().also { it.name = "PausableContent" }
            PausableComposition(createApplier(pausableContent), parent)
        }
    LaunchedEffect(content as Any) { composition.setPausableContentWithReuse(content).workflow() }
    DisposableEffect(Unit) { onDispose { composition.dispose() } }
}

private fun PausedComposition.resumeTillComplete() {
    while (!isComplete) {
        resume { true }
    }
}

private class Awaiter {
    private var continuation: CancellableContinuation<Unit>? = null
    private var done = false

    suspend fun await() {
        if (!done) {
            suspendCancellableCoroutine { continuation = it }
        }
    }

    fun resume() {
        val current = continuation
        continuation = null
        current?.resume(Unit)
    }

    fun done() {
        done = true
        resume()
    }
}

private val RetainedValuesStore.isRetainingExitedValues
    get() =
        when (this) {
            is ForgetfulRetainedValuesStore -> false
            is ManagedRetainedValuesStore -> this.isRetainingExitedValues
            else -> throw UnsupportedOperationException("Cannot resolve retaining state for $this")
        }
