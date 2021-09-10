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

import com.google.android.play.core.splitinstall.SplitInstallManager
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
public class DynamicInstallMonitorTest {
    @Test
    public fun testCancelInstall_sessionIdZero() {
        val monitor = DynamicInstallMonitor()
        val manager = mock(SplitInstallManager::class.java)

        monitor.splitInstallManager = manager
        monitor.cancelInstall()

        verify(manager, times(0)).cancelInstall(anyInt())
    }

    @Test
    public fun testCancelInstall_sessionIdNotZero() {
        val monitor = DynamicInstallMonitor()
        val manager = mock(SplitInstallManager::class.java)

        monitor.splitInstallManager = manager
        monitor.sessionId = 1
        monitor.cancelInstall()

        verify(manager).cancelInstall(anyInt())
    }
}
