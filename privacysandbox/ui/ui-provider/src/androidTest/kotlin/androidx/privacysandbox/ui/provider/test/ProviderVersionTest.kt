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

package androidx.privacysandbox.ui.provider.test

import android.content.Context
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderVersionTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun sandboxedUiAdapter_toCoreLibInfo_containsProviderVersion() {
        val testSandboxedUiAdapter = StubSandboxedUiAdapter()
        val bundle = testSandboxedUiAdapter.toCoreLibInfo(context)
        val uiProviderVersion = bundle.getInt(ProtocolConstants.uiProviderVersionKey)
        val actualVersion = SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        assertThat(uiProviderVersion).isEqualTo(actualVersion)
    }

    @Test
    fun sharedUiAdapter_toCoreLibInfo_containsProviderVersion() {
        val testSharedUiAdapter = StubSharedUiAdapter()
        val bundle = testSharedUiAdapter.toCoreLibInfo()
        val uiProviderVersion = bundle.getInt(ProtocolConstants.uiProviderVersionKey)
        val actualVersion = SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        assertThat(uiProviderVersion).isEqualTo(actualVersion)
    }

    private class StubSandboxedUiAdapter() : SandboxedUiAdapter {
        override fun openSession(
            context: Context,
            sessionData: SessionData,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient,
        ) {}
    }

    private class StubSharedUiAdapter() : SharedUiAdapter {
        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {}
    }
}
