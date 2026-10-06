/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import com.android.adblib.AdbSession
import java.util.function.Function
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BackupRestoreExtensionTest {

    @Suppress("UNUSED_PARAMETER")
    private fun dummyMethod(device: BackupRestoreController, other: String) {}

    private val rootStore = FakeStore()
    private val context = contextWithRootStore(rootStore)

    @Test
    fun testSupportsParameter() {
        val extension = BackupRestoreExtension()
        val mockParameterContext = mock(ParameterContext::class.java)
        val mockExtensionContext = mock(ExtensionContext::class.java)

        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "dummyMethod",
                    BackupRestoreController::class.java,
                    String::class.java,
                )
        val parameters = method.parameters
        val deviceParameter = parameters[0]
        val otherParameter = parameters[1]

        // Test when parameter is BackupRestoreController
        `when`(mockParameterContext.parameter).thenReturn(deviceParameter)
        assertTrue(extension.supportsParameter(mockParameterContext, mockExtensionContext))

        // Test when parameter is something else
        `when`(mockParameterContext.parameter).thenReturn(otherParameter)
        assertFalse(extension.supportsParameter(mockParameterContext, mockExtensionContext))
    }

    /** Each test class gets its own extension instance, but they all share one session. */
    @Test
    fun standaloneExtensionsShareOneSessionUntilTheTestRunEnds() {
        val session = mock(AdbSession::class.java)
        var sessionsCreated = 0
        val createSession = {
            sessionsCreated++
            session
        }
        val first = BackupRestoreExtension(isStandalone = true, createSession)
        val second = BackupRestoreExtension(isStandalone = true, createSession)

        assertSame(session, first.sessionFor(context))
        assertSame(session, second.sessionFor(context))
        assertEquals(1, sessionsCreated)
        verify(session, never()).close()

        rootStore.closeAll()

        verify(session).close()
    }

    @Test
    fun aStandaloneSessionThatFailsToCloseDoesNotFailTheTestRun() {
        val session = mock(AdbSession::class.java)
        doThrow(IllegalStateException("Session already closed")).`when`(session).close()
        BackupRestoreExtension(isStandalone = true) { session }.sessionFor(context)

        rootStore.closeAll()

        verify(session).close()
    }

    @Test
    fun aSharedSessionIsLeftOpenForItsOwner() {
        val session = mock(AdbSession::class.java)

        assertSame(session, BackupRestoreExtension(session).sessionFor(context))
        rootStore.closeAll()

        verify(session, never()).close()
    }

    /**
     * An [ExtensionContext.Store] that closes its [CloseableResource]s on [closeAll], as JUnit does
     * when the context that owns the store ends.
     */
    private class FakeStore : ExtensionContext.Store {
        private val values = LinkedHashMap<Any, Any?>()

        /** Closes the stored resources in reverse order of insertion, like JUnit. */
        fun closeAll() {
            values.values.reversed().filterIsInstance<CloseableResource>().forEach { it.close() }
        }

        override fun get(key: Any): Any? = values[key]

        override fun <V> get(key: Any, requiredType: Class<V>): V = requiredType.cast(values[key])

        override fun <K, V> getOrComputeIfAbsent(key: K, defaultCreator: Function<K, V>): Any? =
            values.getOrPut(key as Any) { defaultCreator.apply(key) }

        override fun <K, V> getOrComputeIfAbsent(
            key: K,
            defaultCreator: Function<K, V>,
            requiredType: Class<V>,
        ): V = requiredType.cast(getOrComputeIfAbsent(key, defaultCreator))

        override fun put(key: Any, value: Any?) {
            values[key] = value
        }

        override fun remove(key: Any): Any? = values.remove(key)

        override fun <V> remove(key: Any, requiredType: Class<V>): V =
            requiredType.cast(values.remove(key))
    }

    private companion object {
        /** Returns a root [ExtensionContext] whose store, in any namespace, is [store]. */
        fun contextWithRootStore(store: ExtensionContext.Store): ExtensionContext {
            val context = mock(ExtensionContext::class.java)
            `when`(context.root).thenReturn(context)
            `when`(context.getStore(any())).thenReturn(store)
            return context
        }
    }
}
