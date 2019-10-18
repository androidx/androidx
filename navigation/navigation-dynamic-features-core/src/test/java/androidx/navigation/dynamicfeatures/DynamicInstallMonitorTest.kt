/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import androidx.test.filters.SmallTest
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
@SmallTest
class DynamicInstallMonitorTest {

    @Test
    fun testIsEndState_true() {
        val endStates = arrayListOf(
            SplitInstallSessionStatus.UNKNOWN,
            SplitInstallSessionStatus.INSTALLED,
            SplitInstallSessionStatus.FAILED,
            SplitInstallSessionStatus.CANCELED
        )
        endStates.forEach {
            assertTrue(DynamicInstallMonitor.isEndState(it))
        }
    }

    @Test
    fun testIsEndState_false() {
        val nonTerminalStates = arrayListOf(
            SplitInstallSessionStatus.PENDING,
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION,
            SplitInstallSessionStatus.DOWNLOADING,
            SplitInstallSessionStatus.DOWNLOADED,
            SplitInstallSessionStatus.INSTALLING
        )
        nonTerminalStates.forEach {
            assertFalse(DynamicInstallMonitor.isEndState(it))
        }
    }

    @Test
    fun testCancelInstall_sessionIdZero() {
        val monitor = DynamicInstallMonitor()
        val manager = mock(SplitInstallManager::class.java)

        monitor.setSplitInstallManager(manager)
        monitor.cancelInstall()

        verify(manager, times(0)).cancelInstall(anyInt())
    }

    @Test
    fun testCancelInstall_sessionIdNotZero() {
        val monitor = DynamicInstallMonitor()
        val manager = mock(SplitInstallManager::class.java)

        monitor.setSplitInstallManager(manager)
        monitor.sessionId = 1
        monitor.cancelInstall()

        verify(manager).cancelInstall(anyInt())
    }

    @Test
    fun testHasException_true() {
        val monitor = DynamicInstallMonitor()
        monitor.exception = Exception()
        assertTrue(monitor.hasException())
    }

    @Test
    fun testHasException_false() {
        val monitor = DynamicInstallMonitor()
        assertFalse(monitor.hasException())
    }
}
