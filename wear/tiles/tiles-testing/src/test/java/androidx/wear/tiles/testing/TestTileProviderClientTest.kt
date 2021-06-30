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
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileProviderService
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
public class TestTileProviderClientTest {
    private companion object {
        private val RESOURCES_VERSION = "10"
    }
    private val fakeTileProvider = FakeTileProviderService()
    private lateinit var clientUnderTest: TestTileProviderClient<FakeTileProviderService>

    @Before
    public fun setUp() {
        val executor = InlineExecutorService()
        clientUnderTest = TestTileProviderClient(fakeTileProvider, executor)
    }

    @Test
    public fun canCallGetApiVersion() {
        val future = clientUnderTest.apiVersion

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(future.isDone).isTrue()
        assertThat(future.get()).isEqualTo(TileProvider.API_VERSION)
    }

    @Test
    public fun canCallOnTileRequest() {
        val future = clientUnderTest.tileRequest(RequestBuilders.TileRequest.builder().build())

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(future.isDone).isTrue()
        assertThat(future.get().resourcesVersion).isEqualTo(RESOURCES_VERSION)
    }

    @Test
    public fun canCallOnResourcesRequest() {
        val future = clientUnderTest.resourcesRequest(
            RequestBuilders.ResourcesRequest.builder().build()
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(future.isDone).isTrue()
        assertThat(future.get().version).isEqualTo(RESOURCES_VERSION)
    }

    @Test
    public fun canCallOnTileAdd() {
        val f = clientUnderTest.onTileAdded()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileProvider.onTileAddFired).isTrue()
        assertThat(fakeTileProvider.onTileRemoveFired).isFalse()
        assertThat(fakeTileProvider.onTileEnterFired).isFalse()
        assertThat(fakeTileProvider.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileRemove() {
        val f = clientUnderTest.onTileRemoved()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileProvider.onTileAddFired).isFalse()
        assertThat(fakeTileProvider.onTileRemoveFired).isTrue()
        assertThat(fakeTileProvider.onTileEnterFired).isFalse()
        assertThat(fakeTileProvider.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileEnter() {
        val f = clientUnderTest.onTileEnter()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileProvider.onTileAddFired).isFalse()
        assertThat(fakeTileProvider.onTileRemoveFired).isFalse()
        assertThat(fakeTileProvider.onTileEnterFired).isTrue()
        assertThat(fakeTileProvider.onTileLeaveFired).isFalse()
    }

    @Test
    public fun canCallOnTileLeave() {
        val f = clientUnderTest.onTileLeave()

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(f.isDone).isTrue()
        assertThat(fakeTileProvider.onTileAddFired).isFalse()
        assertThat(fakeTileProvider.onTileRemoveFired).isFalse()
        assertThat(fakeTileProvider.onTileEnterFired).isFalse()
        assertThat(fakeTileProvider.onTileLeaveFired).isTrue()
    }

    public inner class FakeTileProviderService : TileProviderService() {
        internal var onTileAddFired = false
        internal var onTileRemoveFired = false
        internal var onTileEnterFired = false
        internal var onTileLeaveFired = false

        override fun onTileRequest(
            requestParams: RequestBuilders.TileRequest
        ): ListenableFuture<TileBuilders.Tile> {
            val f = ResolvableFuture.create<TileBuilders.Tile>()

            f.set(TileBuilders.Tile.builder().setResourcesVersion(RESOURCES_VERSION).build())

            return f
        }

        override fun onResourcesRequest(
            requestParams: RequestBuilders.ResourcesRequest
        ): ListenableFuture<ResourceBuilders.Resources> {
            val f = ResolvableFuture.create<ResourceBuilders.Resources>()

            f.set(ResourceBuilders.Resources.builder().setVersion(RESOURCES_VERSION).build())

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
