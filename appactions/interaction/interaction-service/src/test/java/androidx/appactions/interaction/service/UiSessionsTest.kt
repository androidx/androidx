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
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.ExecutionSessionFactory
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.SYNC
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.service.test.R
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils.buildRequestArgs
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils.buildArgs
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.CB_TIMEOUT
import androidx.appactions.interaction.service.testing.internal.FakeCapability
import androidx.appactions.interaction.service.testing.internal.FakeCapability.Arguments
import androidx.appactions.interaction.service.testing.internal.FakeCapability.Output
import androidx.appactions.interaction.service.testing.internal.FakeCapability.ExecutionSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSessionsTest {
    private val sessionFactory = object : ExecutionSessionFactory<ExecutionSession> {
        private val sessions = mutableListOf<ExecutionSession>()
        private var index = 0
        override fun createSession(
            hostProperties: HostProperties?,
        ): ExecutionSession {
            return sessions[index++]
        }

        fun addExecutionSessions(vararg session: ExecutionSession) {
            sessions.addAll(session)
        }

        fun reset() {
            sessions.clear()
            index = 0
        }
    }
    private val sessionId = "fakeSessionId"
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()
    private val multiTurnCapability = FakeCapability.CapabilityBuilder()
        .setId("multiTurnCapability")
        .setExecutionSessionFactory(sessionFactory).build()

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

    @After
    fun cleanup() {
        sessionFactory.reset()
        UiSessions.removeUiCache(sessionId)
    }

    fun createFakeSessionWithUiResponses(vararg uiResponses: UiResponse): ExecutionSession {
        return object : ExecutionSession {
            override suspend fun onExecute(
                arguments: Arguments,
            ): ExecutionResult<Output> {
                for (uiResponse in uiResponses) {
                    this.updateUi(uiResponse)
                }
                return ExecutionResult.Builder<Output>().build()
            }
        }
    }

    @Test
    fun sessionExtensionMethod_createCache_removeCache() {
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()

        sessionFactory.addExecutionSessions(
            createFakeSessionWithUiResponses(remoteViewsUiResponse),
        )
        val session = multiTurnCapability.createSession(sessionId, hostProperties)
        val callback = FakeCallbackInternal(CB_TIMEOUT)
        session.execute(
            buildRequestArgs(
                SYNC,
                "fieldOne",
                "hello",
            ),
            callback,
        )
        callback.receiveResponse()
        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)

        // Test removing.
        assertThat(UiSessions.removeUiCache(sessionId)).isTrue()
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()
    }

    @Test
    fun multipleUpdate_sharesCache() {
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()
        sessionFactory.addExecutionSessions(object : ExecutionSession {
            override suspend fun onExecute(
                arguments: Arguments,
            ): ExecutionResult<Output> {
                this.updateUi(remoteViewsUiResponse)
                this.updateUi(tileLayoutUiResponse)

                return ExecutionResult.Builder<Output>().build()
            }
        })
        val session = multiTurnCapability.createSession(sessionId, hostProperties)
        val callback = FakeCallbackInternal(CB_TIMEOUT)
        session.execute(
            buildRequestArgs(
                SYNC,
                "fieldOne",
                "hello",
            ),
            callback,
        )
        callback.receiveResponse()
        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)
        assertThat(uiCache?.cachedTileLayout).isNotNull()
    }

    @Test
    fun multipleSession_haveTheirOwnCache() {
        val sessionId1 = "fakeSessionId1"
        val sessionId2 = "fakeSessionId2"
        sessionFactory.addExecutionSessions(
            object : ExecutionSession {
                override suspend fun onExecute(
                    arguments: Arguments,
                ): ExecutionResult<Output> {
                    this.updateUi(remoteViewsUiResponse)
                    return ExecutionResult.Builder<Output>().build()
                }
            },
            object : ExecutionSession {
                override suspend fun onExecute(
                    arguments: Arguments,
                ): ExecutionResult<Output> {
                    this.updateUi(tileLayoutUiResponse)
                    return ExecutionResult.Builder<Output>().build()
                }
            },
        )
        val session1 = multiTurnCapability.createSession(sessionId1, hostProperties)
        val session2 = multiTurnCapability.createSession(sessionId2, hostProperties)

        val callback1 = FakeCallbackInternal(CB_TIMEOUT)
        val callback2 = FakeCallbackInternal(CB_TIMEOUT)

        session1.execute(
            buildRequestArgs(
                SYNC,
                "fieldOne",
                "hello",
            ),
            callback1,
        )
        session2.execute(
            buildRequestArgs(
                SYNC,
                "fieldOne",
                "hello",
            ),
            callback2,
        )
        callback1.receiveResponse()
        callback2.receiveResponse()

        val uiCache1 = UiSessions.getUiCacheOrNull(sessionId1)
        assertThat(uiCache1).isNotNull()
        assertThat(uiCache1?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache1?.cachedRemoteViews).isEqualTo(remoteViews)
        assertThat(uiCache1?.cachedTileLayout).isNull()

        val uiCache2 = UiSessions.getUiCacheOrNull(sessionId2)
        assertThat(uiCache2).isNotNull()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()
        assertThat(uiCache2?.cachedRemoteViews).isNull()

        // Assert that UiCache2 response still marked unread.
        uiCache1?.resetUnreadUiResponse()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()

        UiSessions.removeUiCache(sessionId1)
        UiSessions.removeUiCache(sessionId2)
    }

    @Test
    fun actionExecutor_hasUpdateUiExtension() {
        assertThat(UiSessions.getUiCacheOrNull(sessionId)).isNull()
        val oneShotCapability = FakeCapability.CapabilityBuilder().setId(
            "oneShotCapability",
        ).setExecutor(object : ActionExecutor<Arguments, Output> {
            override suspend fun onExecute(arguments: Arguments): ExecutionResult<Output> {
                this.updateUi(remoteViewsUiResponse)
                return ExecutionResult.Builder<Output>().build()
            }
        }).build()
        val session = oneShotCapability.createSession(
            sessionId,
            hostProperties,
        )
        val callback = FakeCallbackInternal(CB_TIMEOUT)
        session.execute(
            buildArgs(
                mapOf(
                    "fieldOne" to ParamValue.newBuilder().setIdentifier("hello").build(),
                ),
            ),
            callback,
        )
        callback.receiveResponse()
        val uiCache = UiSessions.getUiCacheOrNull(sessionId)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)
    }
}
