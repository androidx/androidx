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
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.service.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiCacheTest {
    private val capabilitySession = object : BaseExecutionSession<String, String> {}
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteViewsFactoryId = 123
    private val changeViewId = 111
    private val remoteViews = RemoteViews(context.packageName, R.layout.remote_view)
    private val remoteViewsUiResponse =
        UiResponse.RemoteViewsUiBuilder().setRemoteViews(remoteViews, SizeF(10f, 15f)).build()
    private val remoteViewsUiResponseWithFactory =
        UiResponse.RemoteViewsUiBuilder()
            .setRemoteViews(remoteViews, SizeF(10f, 15f))
            .addRemoteViewsFactory(remoteViewsFactoryId, FakeRemoteViewsFactory())
            .build()

    private fun assertEmptyCache(uiCache: UiCache) {
        assertThat(uiCache.cachedRemoteViewsInternal).isNull()
        assertThat(uiCache.cachedTileLayoutInternal).isNull()
    }

    @Test
    fun unreadUiResponseFlag_lifecycle() {
        val uiCache = UiCache()
        assertThat(uiCache.hasUnreadUiResponse).isFalse()

        // Test set unread flag.
        uiCache.updateUiInternal(remoteViewsUiResponse)
        assertThat(uiCache.hasUnreadUiResponse).isTrue()

        // Test reset.
        uiCache.resetUnreadUiResponse()
        assertThat(uiCache.hasUnreadUiResponse).isFalse()
    }

    @Test
    fun remoteViewsUiResponse_noFactory() {
        val uiCache = UiCache()
        assertEmptyCache(uiCache)

        uiCache.updateUiInternal(remoteViewsUiResponse)

        assertThat(uiCache.cachedRemoteViewsInternal?.remoteViews).isEqualTo(remoteViews)
        assertThat(uiCache.cachedRemoteViewsInternal?.size).isEqualTo(SizeF(10f, 15f))
        assertThat(
            uiCache.cachedRemoteViewsInternal?.collectionViewFactories?.get(remoteViewsFactoryId)
        ).isNull()
    }

    @Test
    fun remoteViewsUiResponse_withFactory() {
        val uiCache = UiCache()
        assertEmptyCache(uiCache)

        uiCache.updateUiInternal(remoteViewsUiResponseWithFactory)

        assertThat(uiCache.cachedRemoteViewsInternal?.remoteViews).isEqualTo(remoteViews)
        assertThat(uiCache.cachedRemoteViewsInternal?.size).isEqualTo(SizeF(10f, 15f))
        assertThat(
            uiCache.cachedRemoteViewsInternal?.collectionViewFactories?.get(remoteViewsFactoryId)
        ).isNotNull()
    }
}
