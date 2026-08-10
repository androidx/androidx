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

package androidx.compose.ui.platform

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.S)
class CrossWindowBlurTest {

    private val mockContext = mock<Context>()
    private val mockWindowManager = mock<WindowManager>()
    private val mockExecutor = mock<Executor>()

    @Before
    fun setUp() {
        whenever(mockContext.getSystemService(WindowManager::class.java))
            .thenReturn(mockWindowManager)
        whenever(mockContext.mainExecutor).thenReturn(mockExecutor)
    }

    @Test
    fun systemListenerRegisteredLazily_whenObservedAfterAttach() {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(true)
        val blurObserver = CrossWindowBlur(mockContext)

        // Verify that the listener is NOT registered on creation (lazy behavior)
        verify(mockWindowManager, never()).addCrossWindowBlurEnabledListener(any(), any())

        // Call onAttached, should still not register since it's not observed
        blurObserver.onAttached()
        verify(mockWindowManager, never()).addCrossWindowBlurEnabledListener(any(), any())

        // Accessing the property triggers lazy observation since it's attached
        val isEnabled = blurObserver.isCrossWindowBlurEnabled

        assertThat(isEnabled).isTrue()
        verify(mockWindowManager).addCrossWindowBlurEnabledListener(eq(mockExecutor), any())
    }

    @Test
    fun systemListenerRegisteredLazily_whenObservedBeforeAttach() {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(true)
        val blurObserver = CrossWindowBlur(mockContext)

        // Accessing the property before attach does not register the listener,
        // but it should fetch the initial value.
        val isEnabled = blurObserver.isCrossWindowBlurEnabled
        assertThat(isEnabled).isTrue()
        verify(mockWindowManager, never()).addCrossWindowBlurEnabledListener(any(), any())

        // Call onAttached, should trigger registration since it was observed
        blurObserver.onAttached()
        verify(mockWindowManager).addCrossWindowBlurEnabledListener(eq(mockExecutor), any())
    }

    @Test
    fun blurStateUpdatesDynamically_whenSystemSettingChanges() = runTest {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(false)
        val blurObserver = CrossWindowBlur(mockContext)
        blurObserver.onAttached()

        // Access property to register the listener
        assertThat(blurObserver.isCrossWindowBlurEnabled).isFalse()

        val listenerCaptor = argumentCaptor<Consumer<Boolean>>()
        verify(mockWindowManager).addCrossWindowBlurEnabledListener(any(), listenerCaptor.capture())

        val systemListener = listenerCaptor.firstValue

        // Track changes using snapshotFlow
        var lastObservedValue: Boolean? = null
        backgroundScope.launch {
            snapshotFlow { blurObserver.isCrossWindowBlurEnabled }
                .collect { lastObservedValue = it }
        }
        testScheduler.runCurrent()
        assertThat(lastObservedValue).isFalse()

        // Simulate the system enabling cross-window blurs
        systemListener.accept(true)
        Snapshot.sendApplyNotifications()
        testScheduler.runCurrent()
        assertThat(lastObservedValue).isTrue()

        // Simulate the system disabling cross-window blurs
        systemListener.accept(false)
        Snapshot.sendApplyNotifications()
        testScheduler.runCurrent()
        assertThat(lastObservedValue).isFalse()
    }

    @Test
    fun systemListenerUnregisters_onDetach() {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(true)
        val blurObserver = CrossWindowBlur(mockContext)
        blurObserver.onAttached()

        // Access property to trigger registration
        val isEnabled = blurObserver.isCrossWindowBlurEnabled
        assertThat(isEnabled).isTrue()

        val listenerCaptor = argumentCaptor<Consumer<Boolean>>()
        verify(mockWindowManager).addCrossWindowBlurEnabledListener(any(), listenerCaptor.capture())
        val registeredListener = listenerCaptor.firstValue

        // Detaching should unregister the listener
        blurObserver.onDetached()
        verify(mockWindowManager).removeCrossWindowBlurEnabledListener(eq(registeredListener))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun blurStateAlwaysDisabled_belowApi31() {
        val blurObserver = CrossWindowBlur(mockContext)
        blurObserver.onAttached()

        val isEnabled = blurObserver.isCrossWindowBlurEnabled

        assertThat(isEnabled).isFalse()
        verify(mockContext, never()).getSystemService(eq(WindowManager::class.java))
    }

    @Test
    fun systemListenerReRegisters_onReAttach() {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(true)
        val blurObserver = CrossWindowBlur(mockContext)
        blurObserver.onAttached()

        // Access property to trigger registration
        assertThat(blurObserver.isCrossWindowBlurEnabled).isTrue()

        val listenerCaptor = argumentCaptor<Consumer<Boolean>>()
        verify(mockWindowManager).addCrossWindowBlurEnabledListener(any(), listenerCaptor.capture())
        val firstListener = listenerCaptor.firstValue

        // Detach unregisters
        blurObserver.onDetached()
        verify(mockWindowManager).removeCrossWindowBlurEnabledListener(eq(firstListener))

        // Re-attach should register again because it was already observed
        blurObserver.onAttached()
        // Verify addListener was called again (total 2 times)
        verify(mockWindowManager, times(2))
            .addCrossWindowBlurEnabledListener(eq(mockExecutor), any())
    }

    @Test
    fun systemListenerOnlyRegisteredOnce_onMultipleReads() {
        whenever(mockWindowManager.isCrossWindowBlurEnabled).thenReturn(true)
        val blurObserver = CrossWindowBlur(mockContext)
        blurObserver.onAttached()

        // Read multiple times
        assertThat(blurObserver.isCrossWindowBlurEnabled).isTrue()
        assertThat(blurObserver.isCrossWindowBlurEnabled).isTrue()
        assertThat(blurObserver.isCrossWindowBlurEnabled).isTrue()

        // Verify listener registered only once
        verify(mockWindowManager, times(1))
            .addCrossWindowBlurEnabledListener(eq(mockExecutor), any())
    }
}
