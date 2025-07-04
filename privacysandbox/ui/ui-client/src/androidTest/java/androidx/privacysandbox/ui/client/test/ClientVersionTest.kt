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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.SharedUiAdapterFactory
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.LocalSharedUiAdapter
import androidx.privacysandbox.ui.core.LocalUiAdapter
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.core.test.TestProtocolConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ClientVersionTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun openLocalSession_clientVersionIsPassed() {
        val stubBinderAdapterDelegate = StubBinderDelegateAdapter()
        val bundle = getSandboxedUiAdapterBundle(stubBinderAdapterDelegate)
        SandboxedUiAdapterFactory.createFromCoreLibInfo(bundle)
            .openSession(context, SessionData(null, null), 0, 0, false, {}, StubSessionClient())
        stubBinderAdapterDelegate.assertClientVersionIsPresentInLocalSession(
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun openRemoteSession_clientVersionIsPassed() {
        assumeTrue(BackwardCompatUtil.canProviderBeRemote())

        val stubBinderAdapterDelegate = StubBinderDelegateAdapter()
        val bundle = getSandboxedUiAdapterBundle(stubBinderAdapterDelegate, true)
        SandboxedUiAdapterFactory.createFromCoreLibInfo(bundle)
            .openSession(context, SessionData(null, null), 0, 0, false, {}, StubSessionClient())

        stubBinderAdapterDelegate.assertClientVersionIsPresentInRemoteSession(
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        )
    }

    @Test
    fun nativeAd_openLocalSession_clientVersionIsPassed() {
        val stubBinderAdapterDelegate = StubSharedBinderDelegateAdapter()
        val bundle = getSharedUiAdapterBundle(stubBinderAdapterDelegate)
        SharedUiAdapterFactory.createFromCoreLibInfo(bundle)
            .openSession({}, StubSharedSessionClient())
        stubBinderAdapterDelegate.assertClientVersionIsPresentInLocalSession(
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun nativeAd_openRemoteSession_clientVersionIsPassed() {
        assumeTrue(BackwardCompatUtil.canProviderBeRemote())

        val stubBinderAdapterDelegate = StubSharedBinderDelegateAdapter()
        val bundle = getSharedUiAdapterBundle(stubBinderAdapterDelegate, true)
        SharedUiAdapterFactory.createFromCoreLibInfo(bundle)
            .openSession({}, StubSharedSessionClient())

        stubBinderAdapterDelegate.assertClientVersionIsPresentInRemoteSession(
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel
        )
    }

    private fun getSandboxedUiAdapterBundle(
        stubBinderAdapterDelegate: StubBinderDelegateAdapter,
        useRemoteAdapter: Boolean = false,
    ): Bundle {
        val bundle = Bundle()
        bundle.putInt(
            ProtocolConstants.uiProviderVersionKey,
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
        )
        bundle.putBinder(ProtocolConstants.uiAdapterBinderKey, stubBinderAdapterDelegate)
        bundle.putBoolean(TestProtocolConstants.testOnlyUseRemoteAdapterKey, useRemoteAdapter)
        return bundle
    }

    private class StubSessionClient() : SessionClient {
        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {}

        override fun onSessionError(throwable: Throwable) {}

        override fun onResizeRequested(width: Int, height: Int) {}
    }

    private class StubBinderDelegateAdapter() : LocalUiAdapter, ISandboxedUiAdapter.Stub() {
        private var clientVersion: Int = -1
        private val openLocalSessionLatch = CountDownLatch(1)
        private val openRemoteSessionLatch = CountDownLatch(1)

        override fun openLocalSession(
            clientVersion: Int,
            context: Context,
            sessionData: SessionData,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SessionClient,
        ) {
            this.clientVersion = clientVersion
            openLocalSessionLatch.countDown()
        }

        override fun openRemoteSession(
            clientVersion: Int,
            sessionData: Bundle?,
            displayId: Int,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            remoteSessionClient: IRemoteSessionClient?,
        ) {
            this.clientVersion = clientVersion
            openRemoteSessionLatch.countDown()
        }

        fun assertClientVersionIsPresentInLocalSession(actualVersion: Int) {
            assertThat(openLocalSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(clientVersion).isEqualTo(actualVersion)
        }

        fun assertClientVersionIsPresentInRemoteSession(actualVersion: Int) {
            assertThat(openRemoteSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(clientVersion).isEqualTo(actualVersion)
        }
    }

    private fun getSharedUiAdapterBundle(
        stubSharedBinderAdapterDelegate: StubSharedBinderDelegateAdapter,
        useRemoteAdapter: Boolean = false,
    ): Bundle {
        val bundle = Bundle()
        bundle.putInt(
            ProtocolConstants.uiProviderVersionKey,
            SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
        )
        bundle.putBinder(
            ProtocolConstants.sharedUiAdapterBinderKey,
            stubSharedBinderAdapterDelegate,
        )
        bundle.putBoolean(TestProtocolConstants.testOnlyUseRemoteAdapterKey, useRemoteAdapter)
        return bundle
    }

    private class StubSharedBinderDelegateAdapter() :
        LocalSharedUiAdapter, ISharedUiAdapter.Stub() {
        private var clientVersion: Int = -1
        private val openLocalSessionLatch = CountDownLatch(1)
        private val openRemoteSessionLatch = CountDownLatch(1)

        override fun openLocalSession(
            clientVersion: Int,
            clientExecutor: Executor,
            client: SharedUiAdapter.SessionClient,
        ) {
            this.clientVersion = clientVersion
            openLocalSessionLatch.countDown()
        }

        override fun openRemoteSession(
            clientVersion: Int,
            remoteSessionClient: IRemoteSharedUiSessionClient?,
        ) {
            this.clientVersion = clientVersion
            openRemoteSessionLatch.countDown()
        }

        fun assertClientVersionIsPresentInLocalSession(actualVersion: Int) {
            assertThat(openLocalSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(clientVersion).isEqualTo(actualVersion)
        }

        fun assertClientVersionIsPresentInRemoteSession(actualVersion: Int) {
            assertThat(openRemoteSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(clientVersion).isEqualTo(actualVersion)
        }
    }

    private class StubSharedSessionClient() : SharedUiAdapter.SessionClient {
        override fun onSessionOpened(session: SharedUiAdapter.Session) {}

        override fun onSessionError(throwable: Throwable) {}
    }

    private companion object {
        const val TIMEOUT = 1000.toLong()
    }
}
