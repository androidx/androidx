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
import androidx.appactions.interaction.service.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSessionsTest {

    private val capabilitySession = object : BaseSession<String, String> {}
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
                                LayoutElementBuilders.Text.Builder().setText("LA8JE92").build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    private val resources = ResourceBuilders.Resources.Builder().setVersion("1234").build()
    private val tileLayoutUiResponse: UiResponse =
        UiResponse.TileLayoutBuilder().setTileLayout(layout, resources).build()

    @Test
    fun sessionExtensionMethod_createsCache() {
        assertThat(UiSessions.getUiCacheOrNull(capabilitySession)).isNull()

        capabilitySession.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(capabilitySession)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)
    }

    @Test
    fun removeUiCache_removesWhatWasPreviouslyCreated() {
        assertThat(UiSessions.getUiCacheOrNull(capabilitySession)).isNull()

        // Invoke extension method.
        capabilitySession.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(capabilitySession)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()

        // Test removing.
        assertThat(UiSessions.removeUiCache(capabilitySession)).isTrue()
        assertThat(UiSessions.getUiCacheOrNull(capabilitySession)).isNull()
    }

    @Test
    fun getOrCreateUiCache_explicitTest() {
        val uiCache = UiSessions.getOrCreateUiCache(capabilitySession)
        assertThat(uiCache).isNotNull()

        // Calling a second time does not create a new cache instance.
        val uiCache2 = UiSessions.getOrCreateUiCache(capabilitySession)
        assertThat(uiCache).isEqualTo(uiCache2)
    }

    @Test
    fun multipleSession_haveTheirOwnCache() {
        val capabilitySession1 = object : BaseSession<String, String> {}
        val capabilitySession2 = object : BaseSession<String, String> {}

        capabilitySession1.updateUi(remoteViewsUiResponse)

        val uiCache1 = UiSessions.getUiCacheOrNull(capabilitySession1)
        assertThat(uiCache1).isNotNull()
        assertThat(uiCache1?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache1?.cachedRemoteViews).isEqualTo(remoteViews)
        assertThat(uiCache1?.cachedTileLayout).isNull()

        capabilitySession2.updateUi(tileLayoutUiResponse)

        val uiCache2 = UiSessions.getUiCacheOrNull(capabilitySession2)
        assertThat(uiCache2).isNotNull()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()
        assertThat(uiCache2?.cachedRemoteViews).isNull()

        // Assert that UiCache2 response still marked unread.
        uiCache1?.resetUnreadUiResponse()
        assertThat(uiCache2?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache2?.cachedTileLayout).isNotNull()
    }

    @Test
    fun actionExecutor_hasUpdateUiExtension() {
        val actionExecutor = ActionExecutor<String, String> {
            ExecutionResult.getDefaultInstance()
        }

        actionExecutor.updateUi(remoteViewsUiResponse)

        val uiCache = UiSessions.getUiCacheOrNull(actionExecutor)
        assertThat(uiCache).isNotNull()
        assertThat(uiCache?.hasUnreadUiResponse()).isTrue()
        assertThat(uiCache?.cachedChangedViewIds).containsExactly(changeViewId)
        assertThat(uiCache?.cachedRemoteViewsSize).isEqualTo(SizeF(10f, 15f))
        assertThat(uiCache?.cachedRemoteViews).isEqualTo(remoteViews)
    }
}
