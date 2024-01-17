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
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when` as mockWhen

@RunWith(JUnit4::class)
public class DynamicInstallManagerTest {

    private val splitInstallManager = mock(SplitInstallManager::class.java)
    private var manager = DynamicInstallManager(
        spy(Context::class.java),
        splitInstallManager
    )

    @Test
    public fun testNeedsInstall_InstallNeeded() {
        mockWhen(splitInstallManager.installedModules).thenReturn(setOf("not-module"))
        assertThat(manager.needsInstall("module")).isTrue()
    }

    @Test
    public fun testNeedsInstall_NoInstallNeeded() {
        mockWhen(splitInstallManager.installedModules).thenReturn(setOf("module"))
        assertThat(manager.needsInstall("module")).isFalse()
    }
}
