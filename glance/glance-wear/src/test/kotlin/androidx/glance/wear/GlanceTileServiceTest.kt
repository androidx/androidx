/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.wear

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.glance.layout.Text
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.testing.TestTileClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceTileServiceTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private lateinit var tileService: TestGlanceTileService
    private lateinit var tileServiceClient: TestTileClient<GlanceTileService>

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
        tileService = TestGlanceTileService()
        tileServiceClient = TestTileClient(
            tileService,
            fakeCoroutineScope,
            fakeCoroutineScope.coroutineContext[CoroutineDispatcher]!!
        )
    }

    @Test
    fun tileProviderReturnsTile() = fakeCoroutineScope.runBlockingTest {
        // Add the composition to the service.
        tileService.actualContent = { Text("Hello World!") }

        // Request is currently un-used, provide an empty one.
        val tileRequest = RequestBuilders.TileRequest.Builder().build()

        // Requests need to be split; we need to allow Robolectric to schedule the service calls on
        // the main looper, so we can't just do requestTile().await().
        val tileFuture = tileServiceClient.requestTile(tileRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val tile = tileFuture.await()

        // Just uses a simple resource version for now.
        assertThat(tile.resourcesVersion).isEqualTo(GlanceTileService.ResourcesVersion)

        // No freshness interval (for now)
        assertThat(tile.freshnessIntervalMillis).isEqualTo(0)

        assertThat(tile.timeline!!.timelineEntries).hasSize(1)
        val entry = tile.timeline!!.timelineEntries[0]
        assertThat(entry.validity).isNull()

        // It always emits a box as the root-level layout.
        val box = assertIs<LayoutElementBuilders.Box>(entry.layout!!.root!!)
        assertThat(box.contents).hasSize(1)
        val text = assertIs<LayoutElementBuilders.Text>(box.contents[0])

        assertThat(text.text!!.value).isEqualTo("Hello World!")
    }

    @Test
    fun tileProviderReturnsResources() = fakeCoroutineScope.runBlockingTest {
        // The service doesn't use resources right now, but it must correctly respond to
        // onResourcesRequest. Just ensure that the version is set correctly.
        val resourcesRequest = RequestBuilders.ResourcesRequest.Builder().build()

        val resourcesFuture = tileServiceClient.requestResources(resourcesRequest)
        shadowOf(Looper.getMainLooper()).idle()
        val resources = resourcesFuture.await()

        assertThat(resources.version).isEqualTo(GlanceTileService.ResourcesVersion)
    }

    private inner class TestGlanceTileService : GlanceTileService() {
        var actualContent: @Composable () -> Unit = {}

        @Composable
        override fun Content() {
            actualContent()
        }
    }
}