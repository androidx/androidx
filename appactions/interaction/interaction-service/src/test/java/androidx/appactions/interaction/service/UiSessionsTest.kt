/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service

import android.content.Context
import android.util.SizeF
import android.widget.RemoteViews
import androidx.appactions.interaction.capabilities.core.ActionExecutor
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.service.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSessionsTest {

    private val externalSession = object : BaseSession<String, String> {}
    private val capabilitySession = mock<CapabilitySession>() {
        on { this.uiHandle } doReturn(externalSession)
    }
    private val sessionId = "fakeSessionId"

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteViewsFactoryId = 123
    private val changeViewId = 111
    private val remoteViews = RemoteViews(context.packageName, R.layout.remote_view)
    private val remoteViewsUiResponse =
        UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, FakeRemoteViewsFactory())
            .addViewIdForCollectionUpdate(changeViewId)
            .build()
    private val layout =
        LayoutElementBuilders.Layout.Builder()
            .setRoot(
                LayoutElementBuilders.Box.Builder()
                    .addContent(
                        LayoutElementBuilders.Column.Builder()
                            .addContent(
                                LayoutElementBuilders.Text.Builder().setText("LA8JE92").build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()
    private val resources = ResourceBuilders.Resources.Builder().setVersion("1234").build()
    private val tileLayoutUiResponse: UiResponse =
        UiResponse.TileLayoutBuilder().setTileLayout(layout, resources).build()

    @Before
    fun setup() {
        SessionManager.putSession(sessionId, capabilitySession)
    }
    @After
    fun cleanup() {
        UiSessions.removeUiCache(sessionId)
        SessionManager.removeSession(sessionId)
    }

    @Test
    fun sessionExtensionMethod_createsCache() {
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()

        externalSession.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)
    }

    @Test
    fun removeUiCache_removesWhatWasPreviouslyCreated() {
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()

        // Invoke extension method.
        externalSession.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()

        // Test removing.
        assertThat(UiSessions.removeUiCache(sessionId)).isTrue()
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()
    }

    @Test
    fun getOrCreateUiCache_explicitTest() {
        val uiCache = UiSessions.getOrCreateUiCache(sessionId)
        assertThat(uiCache).isNotNull()

        // Calling a second time does not create a new cache instance.
        val uiCache2 = UiSessions.getOrCreateUiCache(sessionId)
        assertThat(uiCache).isEqualTo(uiCache2)
    }

    @Test
    fun multipleSession_haveTheirOwnCache() {
        val externalSession1 = object : BaseSession<String, String> {}
        val capabilitySession1 = mock<CapabilitySession> {
            on { this.uiHandle } doReturn(externalSession1)
        }
        val sessionId1 = "fakeSessionId1"
        val externalSession2 = object : BaseSession<String, String> {}
        val capabilitySession2 = mock<CapabilitySession> {
            on { this.uiHandle } doReturn(externalSession2)
        }
        val sessionId2 = "fakeSessionId2"
        SessionManager.putSession(sessionId1, capabilitySession1)
        SessionManager.putSession(sessionId2, capabilitySession2)

        externalSession1.updateUi(remoteViewsUiResponse)

        val uiCache1 = UiSessions.getUiCacheOrNull(sessionId1)
        assertThat(uiCache1).isNotNull()
        assertThat(uiCache1?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache1?.cachedRemoteViews).isEqualTo(remoteViews)
        assertThat(uiCache1?.cachedTileLayout).isNull()

        externalSession2.updateUi(tileLayoutUiResponse)

        val uiCache2 = UiSessions.getUiCacheOrNull(sessionId2)
        assertThat(uiCache2).isNotNull()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()
        assertThat(uiCache2?.cachedRemoteViews).isNull()

        // Assert that UiCache2 response still marked unread.
        uiCache1?.resetUnreadUiResponse()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()

        SessionManager.removeSession(sessionId1)
        SessionManager.removeSession(sessionId2)
    }

    @Test
    fun actionExecutor_hasUpdateUiExtension() {
        val actionExecutor = ActionExecutor<String, String> {
            ExecutionResult.getDefaultInstance()
        }
        val session = mock<CapabilitySession> {
            on { this.uiHandle } doReturn(actionExecutor)
        }
        val sessionId = "actionExecutorSessionId"
        SessionManager.putSession(sessionId, session)

        actionExecutor.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)

        SessionManager.removeSession(sessionId)
    }
}
