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

package androidx.wear.tiles.testing

import android.os.Looper
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileService
import androidx.wear.protolayout.ResourceBuilders
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(TilesTestingTestRunner::class)
@DoNotInstrument
public class TestTileClientTest {
    private companion object {
        private val RESOURCES_VERSION = "10"
    }
    private val fakeTileService = FakeTileService()
    private lateinit var clientUnderTest: TestTileClient<FakeTileService>

    @Before
    public fun setUp() {
        val executor = InlineExecutorService()
        clientUnderTest = TestTileClient(fakeTileService, executor)
    }

    @Test
    public fun canCallGetApiVersion() {
        val future = clientUnderTest.requestApiVersion()

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(future.isDone).isTrue()
        assertThat(future.get()).isEqualTo(TileProvider.API_VERSION)
    }

    @Test
    public fun canCallOnTileRequest() {
        val future = clientUnderTest.requestTile(RequestBuilders.TileRequest.Builder().build())

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(future.isDone).isTrue()
        assertThat(future.get().resourcesVersion).isEqualTo(RESOURCES_VERSION)
    }

    @Test
    public fun canCallOnResourcesRequest() {
        val future = clientUnderTest.requestResources(
            RequestBuilders.ResourcesRequest.Builder().build()
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(future.isDone).isTrue()
        assertThat(future.get().version).isEqualTo(RESOURCES_VERSION)
    }

    @Test
    public fun canCallOnTileAdd() {
        val f = clientUnderTest.sendOnTileAddedEvent()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileService.onTileAddFired).isTrue()
        assertThat(fakeTileService.onTileRemoveFired).isFalse()
        assertThat(fakeTileService.onTileEnterFired).isFalse()
        assertThat(fakeTileService.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileRemove() {
        val f = clientUnderTest.sendOnTileRemovedEvent()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileService.onTileAddFired).isFalse()
        assertThat(fakeTileService.onTileRemoveFired).isTrue()
        assertThat(fakeTileService.onTileEnterFired).isFalse()
        assertThat(fakeTileService.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileEnter() {
        val f = clientUnderTest.sendOnTileEnterEvent()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileService.onTileAddFired).isFalse()
        assertThat(fakeTileService.onTileRemoveFired).isFalse()
        assertThat(fakeTileService.onTileEnterFired).isTrue()
        assertThat(fakeTileService.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileLeave() {
        val f = clientUnderTest.sendOnTileLeaveEvent()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileService.onTileAddFired).isFalse()
        assertThat(fakeTileService.onTileRemoveFired).isFalse()
        assertThat(fakeTileService.onTileEnterFired).isFalse()
        assertThat(fakeTileService.onTileLeaveFired).isTrue()
    }

    public inner class FakeTileService : TileService() {
        internal var onTileAddFired = false
        internal var onTileRemoveFired = false
        internal var onTileEnterFired = false
        internal var onTileLeaveFired = false

        override fun onTileRequest(
            requestParams: RequestBuilders.TileRequest
        ): ListenableFuture<TileBuilders.Tile> {
            val f = ResolvableFuture.create<TileBuilders.Tile>()

            f.set(TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION).build())

            return f
        }

        override fun onTileResourcesRequest(
            requestParams: RequestBuilders.ResourcesRequest
        ): ListenableFuture<ResourceBuilders.Resources> {
            val f = ResolvableFuture.create<ResourceBuilders.Resources>()

            f.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())

            return f
        }

        override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
            onTileAddFired = true
        }

        override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
            onTileRemoveFired = true
        }

        override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
            onTileEnterFired = true
        }

        override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) {
            onTileLeaveFired = true
        }
    }
}
