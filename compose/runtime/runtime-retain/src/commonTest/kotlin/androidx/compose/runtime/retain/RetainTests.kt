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

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                CompositionLocalProvider(
                    value = LocalRetainedValuesStore provides ForgetfulRetainedValuesStore
                ) {
                    retain<RememberObserver> { ThrowingRememberObserver }
                }
            }
        }
    }

    @Suppress("RetainRememberObserver")
    @Test
    fun retain_throwsForRememberObserver_controlledScope() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        assertThrows<IllegalArgumentException> {
            compose {
                CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                    retain<RememberObserver> { ThrowingRememberObserver }
                }
            }
        }
    }

    @Test
    fun retain_notRetaining_remember() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var showContent = true

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                if (showContent) {
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
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var factoryResult: CountingRetainObject? = null
        var lastSeen: CountingRetainObject? = null
        var showContent = true

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                if (showContent) {
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
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
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
    fun retain_forgetsValuesWhenKeysChange_whenRetaining() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope.startRetainingExitedValues()
        key = "456"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 2)
        assertSame(factoryResults.last(), lastSeen)
        val secondResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        key = "123"
        lastSeen = null
        recomposeScope.invalidate()
        expectChanges()
        assertEquals(factoryResults.size, 2)
        assertSame(firstResult, lastSeen)
        firstResult.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        scope.stopRetainingExitedValues()
        firstResult.assertCounts(retained = 1, entered = 2, exited = 1, retired = 0)
        secondResult.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Test
    fun retain_remembersValuesWithSameKeys_whenRetaining() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        var key = "123"

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                lastSeen = retain(key) { CountingRetainObject().also { factoryResults += it } }
            }
        }

        assertEquals(factoryResults.size, 1)
        assertSame(factoryResults.last(), lastSeen)
        val firstResult = factoryResults.last()
        firstResult.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope.startRetainingExitedValues()
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
        val scope = ControlledRetainedValuesStore()
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
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                if (includeContent) {
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
        scope.startRetainingExitedValues()
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
        scope.stopRetainingExitedValues()
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
        scope.startRetainingExitedValues()
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
        scope.stopRetainingExitedValues()
        assertEquals(
            listOf("Retire(Buzz)", "Retire(Baz)", "Retire(Bar)", "Retire(Foo)"),
            callbackLog,
        )
    }

    @Test
    fun retain_callbackOrdering_relativeToRememberObserver() = compositionTest {
        val scope = ControlledRetainedValuesStore()
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
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                if (includeContent) {
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
        scope.startRetainingExitedValues()
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
        scope.stopRetainingExitedValues()
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
        scope.startRetainingExitedValues()
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
        scope.stopRetainingExitedValues()
        assertEquals(
            listOf("Retire(RetainedBaz)", "Retire(RetainedBar)", "Retire(RetainedFoo)"),
            callbackLog,
        )
    }

    @Test
    fun changingRetainedValuesStore_adoptsObjectsToNewScope() = compositionTest {
        var scope: RetainedValuesStore =
            ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var includeContent = true

        compose {
            recomposeScope = currentRecomposeScope
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                if (includeContent) {
                    @Suppress("UnusedVariable")
                    val retained = retain { CountingRetainObject().also { factoryResults += it } }
                }
            }
        }

        scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope = ForgetfulRetainedValuesStore
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope = ControlledRetainedValuesStore()
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope.startRetainingExitedValues()
        includeContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 0)

        scope.stopRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Test
    fun retain_inMovableContent_experiencesOriginRetentionPolicy() = compositionTest {
        val scope = ControlledRetainedValuesStore()
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
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                Linear { if (showContent) content() }
            }
        }

        assertEquals(1, factoryResults.size, "Only one object should be retained")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        scope.startRetainingExitedValues()
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

        scope.stopRetainingExitedValues()
        scope.startRetainingExitedValues()
        showContent = false
        recomposeScope.invalidate()
        advance()
        assertEquals(1, factoryResults.size, "Only one object should be retained")
        retained.assertCounts(retained = 1, entered = 2, exited = 2, retired = 0)
        scope.stopRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 2, exited = 2, retired = 1)
    }

    @Test
    fun retain_inMovableContent_adoptsToDestinationScope() = compositionTest {
        val scopeA = ControlledRetainedValuesStore()
        val scopeB = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
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
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scopeA) {
                Linear { if (!moveContent && showContent) content() }
            }
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scopeB) {
                Linear { if (moveContent && showContent) content() }
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

        scopeB.stopRetainingExitedValues()
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @Test
    fun retain_duplicateRetainKeys() = compositionTest {
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        val factoryResults = mutableListOf<CountingRetainObject>()
        var showContent = true

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                if (showContent) {
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
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        lateinit var recomposeScope: RecomposeScope
        var showContent = true
        var banFactoryObjectCreation = false

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                recomposeScope = currentRecomposeScope
                val compositeKeyHashCodes = mutableSetOf<CompositeKeyHashCode>()

                if (showContent) {
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
        val notRetainingRetainedValuesStore = ControlledRetainedValuesStore()
        val retainingRetainedValuesStore =
            ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        val events = mutableListOf<String>()

        try {
            compositionTest {
                compose {
                    CompositionLocalProvider(
                        LocalRetainedValuesStore provides notRetainingRetainedValuesStore
                    ) {
                        retain<LoggingRetainObject> { LoggingRetainObject("A", events) }
                        if (failComposition) {
                            retain<LoggingRetainObject> { LoggingRetainObject("B", events) }
                        }
                    }

                    CompositionLocalProvider(
                        LocalRetainedValuesStore provides retainingRetainedValuesStore
                    ) {
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
                advance()
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
            inAnyOrder("Unused(B)", "Retain(D)")
            eq("ExitComposition(C)")
            eq("ExitComposition(A)")
            eq("Retire(A)")
        }

        events += "stopRetainingExitedValues"
        retainingRetainedValuesStore.stopRetainingExitedValues()
        assertContent(events) {
            eq("Retain(A)")
            eq("EnterComposition(A)")
            eq("Retain(C)")
            eq("EnterComposition(C)")
            eq("recompose")
            eq("throw")
            inAnyOrder("Unused(B)", "Retain(D)")
            eq("ExitComposition(C)")
            eq("ExitComposition(A)")
            eq("Retire(A)")
            eq("stopRetainingExitedValues")
            inAnyOrder("Retire(D)", "Retire(C)")
        }
    }

    @Test
    fun retainedContentHostInitializationTest() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        var retainedContentHostScope: RetainedValuesStore? = null
        var showContent by mutableStateOf(true)
        var retainedCounter = 0

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                RetainedContentHost(active = showContent) {
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
    fun retainedContentHostNestedInitializationTest() = compositionTest {
        val scope = ControlledRetainedValuesStore().apply { startRetainingExitedValues() }
        var retainedContentHostScope: RetainedValuesStore? = null
        var showContent by mutableStateOf(true)
        var retainedCounter = 0

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                RetainedContentHost(active = showContent) {
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

        validate {
            Text("0")
            Text("1")
            Text("2")
        }
        expectNoChanges()
        assertNotNull(retainedContentHostScope, "retainedContentHostScope not initialized")
        assertTrue(retainedContentHostScope.isRetainingExitedValues)

        scope.stopRetainingExitedValues()
        expectNoChanges()
        assertFalse(retainedContentHostScope.isRetainingExitedValues)
    }

    @Test
    fun retainedContentHostTest() = compositionTest {
        val scope = ControlledRetainedValuesStore()
        var retainedContentHostScope: RetainedValuesStore? = null
        var showContent by mutableStateOf(true)
        var retainedCounter = 0

        compose {
            CompositionLocalProvider(value = LocalRetainedValuesStore provides scope) {
                RetainedContentHost(active = showContent) {
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

        assertNotNull(retainedContentHostScope, "retainedContentHostScope was not initialized")
        assertNotSame(
            scope,
            retainedContentHostScope,
            "RetainedContentHost should issue a new RetainedValuesStore for its children.",
        )
        assertFalse(
            retainedContentHostScope.isRetainingExitedValues,
            "retainedContentHostScope should be initialized as not retaining",
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
            retainedContentHostScope.isRetainingExitedValues,
            "retainedContentHostScope should be retaining values when inactive",
        )

        showContent = true
        expectChanges()
        validate {
            Text("0")
            Text("1")
            Text("2")
        }

        assertFalse(
            retainedContentHostScope.isRetainingExitedValues,
            "retainedContentHostScope should stop retaining after being restored",
        )
        scope.startRetainingExitedValues()
        assertTrue(
            retainedContentHostScope.isRetainingExitedValues,
            "retainedContentHostScope should match parent state",
        )
        scope.stopRetainingExitedValues()
        assertFalse(
            retainedContentHostScope.isRetainingExitedValues,
            "retainedContentHostScope should match parent state",
        )
    }

    @Test
    fun retainedValuesStoreRegistry_createsUniqueScopesPerKey() {
        val provider = RetainedValuesStoreRegistry()
        val keys = listOf("A", "B", "C", "D", "E", "F")
        val scopes = keys.associateWith { provider.getOrCreateRetainedValuesStoreForChild(it) }

        assertEquals(
            keys.size,
            scopes.values.distinct().size,
            "Found incorrect number of unique RetainedValuesStores for the given keys",
        )

        scopes.forEach { (key, scope) ->
            assertFalse(
                scope.isRetainingExitedValues,
                "RetainedValuesStore for key \"$key\" should not be initialized as retaining",
            )

            assertSame(
                scope,
                provider.getOrCreateRetainedValuesStoreForChild(key),
                "Provider returned a different scope for key \"$key\"",
            )
        }
    }

    @Test
    fun retainedValuesStoreRegistry_countsRequests() {
        val parentScope = ControlledRetainedValuesStore()
        val provider = RetainedValuesStoreRegistry()
        provider.setParentRetainStateProvider(parentScope)
        provider.getOrCreateRetainedValuesStoreForChild("A")
        provider.getOrCreateRetainedValuesStoreForChild("B")

        assertEquals(0, provider.retainExitedValuesRequestsFor("A"))
        assertEquals(0, provider.retainExitedValuesRequestsFor("B"))

        provider.startRetainingExitedValues("A")
        provider.startRetainingExitedValues("A")
        provider.startRetainingExitedValues("B")

        assertEquals(2, provider.retainExitedValuesRequestsFor("A"))
        assertEquals(1, provider.retainExitedValuesRequestsFor("B"))

        parentScope.startRetainingExitedValues()
        assertEquals(2, provider.retainExitedValuesRequestsFor("A"))
        assertEquals(1, provider.retainExitedValuesRequestsFor("B"))

        provider.stopRetainingExitedValues("A")
        provider.stopRetainingExitedValues("A")

        assertEquals(0, provider.retainExitedValuesRequestsFor("A"))
        assertTrue(provider.getOrCreateRetainedValuesStoreForChild("A").isRetainingExitedValues)
        assertEquals(1, provider.retainExitedValuesRequestsFor("B"))

        try {
            provider.stopRetainingExitedValues("A")
            fail("Expected an IllegalStateException to be thrown")
        } catch (_: IllegalStateException) {}
        assertEquals(0, provider.retainExitedValuesRequestsFor("A"))

        parentScope.stopRetainingExitedValues()
        assertFalse(provider.getOrCreateRetainedValuesStoreForChild("A").isRetainingExitedValues)
        assertTrue(provider.getOrCreateRetainedValuesStoreForChild("B").isRetainingExitedValues)
        assertEquals(0, provider.retainExitedValuesRequestsFor("A"))
        assertEquals(1, provider.retainExitedValuesRequestsFor("B"))
    }

    @Test
    fun retainedValuesStoreRegistry_retainsScopes_manually() = compositionTest {
        val knownKeys = listOf("A", "B", "C", "D", "E", "F")
        var visibleKeys by mutableStateOf(knownKeys.take(3))
        val retainedValues = knownKeys.associateWith { mutableListOf<CountingRetainObject>() }
        lateinit var retainedValuesStoreRegistry: RetainedValuesStoreRegistry

        compose {
            retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
            visibleKeys.forEach { visibleKey ->
                key(visibleKey) {
                    val retainedValuesStore =
                        retainedValuesStoreRegistry.getOrCreateRetainedValuesStoreForChild(
                            visibleKey
                        )
                    CompositionLocalProvider(
                        LocalRetainedValuesStore provides retainedValuesStore
                    ) {
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
        retainedValuesStoreRegistry.startRetainingExitedValues("A")
        retainedValuesStoreRegistry.startRetainingExitedValues("B")
        retainedValuesStoreRegistry.startRetainingExitedValues("C")
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
        retainedValuesStoreRegistry.stopRetainingExitedValues("A")
        retainedValuesStoreRegistry.stopRetainingExitedValues("B")
        retainedValuesStoreRegistry.stopRetainingExitedValues("C")
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
    fun retainedValuesStoreRegistry_retainsScopes_withPresenceIndicator() = compositionTest {
        val knownKeys = listOf("A", "B", "C", "D", "E", "F")
        var visibleKeys by mutableStateOf(knownKeys.take(3))
        val retainedValues = knownKeys.associateWith { mutableListOf<CountingRetainObject>() }

        compose {
            with(retainRetainedValuesStoreRegistry()) {
                visibleKeys.forEach { visibleKey ->
                    key(visibleKey) {
                        ProvideChildRetainedValuesStore(visibleKey) {
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
        lateinit var scopeA: RetainedValuesStore
        lateinit var scopeB: RetainedValuesStore
        compose {
            if (includeA) {
                provider.ProvideChildRetainedValuesStore("A") {
                    scopeA = LocalRetainedValuesStore.current
                    Text("A:" + retain { globalCounter++ })
                }
            }
            if (includeB) {
                provider.ProvideChildRetainedValuesStore("B") {
                    scopeB = LocalRetainedValuesStore.current
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
        val originalScopeA = scopeA
        assertTrue(scopeA.isRetainingExitedValues, "Removed scope should be retaining")
        assertFalse(scopeB.isRetainingExitedValues, "Unremoved scope should be retaining")
        provider.clearChild("A")
        assertFalse(scopeA.isRetainingExitedValues, "Cleared scope should stop retaining")

        expectedAText = "A:2"
        includeA = true
        expectChanges()
        revalidate()

        assertNotSame(originalScopeA, scopeA, "Cleared scope should be recreated when requested")
    }

    @Test
    fun retainRetainedValuesStoreRegistry_retireDispose() = compositionTest {
        val parentScope = ControlledRetainedValuesStore()
        lateinit var retainedValuesStoreRegistry: RetainedValuesStoreRegistry
        val events = mutableListOf<String>()
        var includeRetainedValuesStoreRegistry by mutableStateOf(true)

        compose {
            CompositionLocalProvider(LocalRetainedValuesStore provides parentScope) {
                retain<LoggingRetainObject> { LoggingRetainObject("A", events) }
                if (includeRetainedValuesStoreRegistry) {
                    retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
                    retainedValuesStoreRegistry.ProvideChildRetainedValuesStore("B") {
                        retain<LoggingRetainObject> { LoggingRetainObject("B", events) }
                    }
                }
            }
        }

        assertEquals(
            listOf("Retain(A)", "EnterComposition(A)", "Retain(B)", "EnterComposition(B)"),
            events,
        )

        val childScope = retainedValuesStoreRegistry.getOrCreateRetainedValuesStoreForChild("B")
        assertFalse(childScope.isRetainingExitedValues)

        parentScope.startRetainingExitedValues()
        assertTrue(childScope.isRetainingExitedValues)
        includeRetainedValuesStoreRegistry = false
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

        assertTrue(childScope.isRetainingExitedValues)
        parentScope.stopRetainingExitedValues()
        assertFalse(childScope.isRetainingExitedValues)
        assertEquals(
            listOf(
                "Retain(A)",
                "EnterComposition(A)",
                "Retain(B)",
                "EnterComposition(B)",
                "ExitComposition(B)",
                "Retire(B)",
            ),
            events,
        )

        assertThrows<IllegalStateException> {
            retainedValuesStoreRegistry.getOrCreateRetainedValuesStoreForChild("B")
        }
    }

    @Test
    fun retainedValuesStoreRegistry_manualDispose() {
        val retainedValuesStoreRegistry = RetainedValuesStoreRegistry()
        val childScope = retainedValuesStoreRegistry.getOrCreateRetainedValuesStoreForChild("B")
        repeat(4) { retainedValuesStoreRegistry.startRetainingExitedValues("B") }

        assertTrue(childScope.isRetainingExitedValues)

        retainedValuesStoreRegistry.dispose()
        assertFalse(childScope.isRetainingExitedValues)

        // Should no-op.
        retainedValuesStoreRegistry.dispose()

        assertThrows<IllegalStateException> {
            retainedValuesStoreRegistry.getOrCreateRetainedValuesStoreForChild("B")
        }
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        var didSucceed = false
        try {
            block()
            didSucceed = true
        } catch (t: Throwable) {
            assertEquals(T::class, t::class, "Block threw unexpected exception type")
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
            val remainingValues = values.toMutableSet()
            val targetIndex = index + values.size
            while (remainingValues.isNotEmpty() && index < targetIndex) {
                anyOf(remainingValues)?.let { remainingValues -= it }
            }
        }

        fun anyOf(values: Set<T>): T? {
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
