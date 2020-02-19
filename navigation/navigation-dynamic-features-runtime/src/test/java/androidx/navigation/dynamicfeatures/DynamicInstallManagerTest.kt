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

import android.content.Context
import androidx.test.filters.SmallTest
import com.google.android.play.core.splitinstall.SplitInstallManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy

@SmallTest
@RunWith(JUnit4::class)
class DynamicInstallManagerTest {

    private val splitInstallManager = mock(SplitInstallManager::class.java)
    private var manager = DynamicInstallManager(
        spy(Context::class.java),
        splitInstallManager
    )

    @Test
    fun testNeedsInstall_InstallNeeded() {
        mockWhen(splitInstallManager.installedModules).thenReturn(setOf("not-module"))
        assertTrue(manager.needsInstall("module"))
    }

    @Test
    fun testNeedsInstall_NoInstallNeeded() {
            mockWhen(splitInstallManager.installedModules).thenReturn(setOf("module"))
        assertFalse(manager.needsInstall("module"))
    }
}
