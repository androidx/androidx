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

package androidx.wear.tiles.connection

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourcesCallback
import androidx.wear.tiles.ResourcesData
import androidx.wear.tiles.ResourcesRequestData
import androidx.wear.tiles.TileAddEventData
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileCallback
import androidx.wear.tiles.TileData
import androidx.wear.tiles.TileEnterEventData
import androidx.wear.tiles.TileLeaveEventData
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileRemoveEventData
import androidx.wear.tiles.TileRequestData
import androidx.wear.tiles.TilesTestRunner
import androidx.wear.tiles.proto.TileProto
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.IllegalArgumentException

@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
@RunWith(TilesTestRunner::class)
public class DefaultTileClientTest {
    public companion object {
        private val TILE_PROVIDER = ComponentName("HelloWorld", "FooBarBaz")
    }

    private lateinit var appContext: Context
    private lateinit var fakeTileService: FakeTileService
    private lateinit var fakeCoroutineDispatcher: TestDispatcher
    private lateinit var fakeCoroutineScope: TestScope
    private lateinit var clientUnderTest: DefaultTileClient

    @Before
    public fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        fakeTileService = FakeTileService()
        fakeCoroutineDispatcher = UnconfinedTestDispatcher()
        fakeCoroutineScope = TestScope(fakeCoroutineDispatcher)

        Shadows.shadowOf(appContext as Application)
            .setComponentNameAndServiceForBindService(TILE_PROVIDER, fakeTileService.asBinder())

        clientUnderTest = DefaultTileClient(
            appContext, TILE_PROVIDER,
            fakeCoroutineScope, fakeCoroutineDispatcher
        )
    }

    @After
    public fun tearDown() {
        fakeCoroutineDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    public fun getTileContents_canGetTileContents(): Unit = fakeCoroutineScope.runTest {
        val expectedTile = TileBuilders.Tile.Builder().setResourcesVersion("5").build()
        fakeTileService.returnTile = expectedTile.toProto().toByteArray()

        val result = async {
            clientUnderTest.requestTile(RequestBuilders.TileRequest.Builder().build()).await()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // We don't override #equals; check the proto forms for equality instead.
        assertThat(result.await().toProto()).isEqualTo(expectedTile.toProto())
    }

    @Test
    public fun getTileContents_failsIfUnparsableResult(): Unit =
        fakeCoroutineScope.runTest {
            // Put some random payload in and see if it breaks.
            fakeTileService.returnTile = byteArrayOf(127)

            val result = async(Job()) {
                clientUnderTest.requestTile(RequestBuilders.TileRequest.Builder().build()).await()
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertThat(result.isCompleted).isTrue()
            assertThat(result.getCompletionExceptionOrNull()).isInstanceOf(
                InvalidProtocolBufferException::class.java
            )
        }

    @Test
    public fun getTileContents_failsIfVersionMismatch(): Unit = fakeCoroutineScope.runTest {
        // Put some random payload in and see if it breaks.
        val expectedTile = TileProto.Tile.newBuilder().setResourcesVersion("5").build()
        fakeTileService.returnTile = expectedTile.toByteArray()
        fakeTileService.returnTileVersion = -1

        val result = async(Job()) {
            clientUnderTest.requestTile(RequestBuilders.TileRequest.Builder().build()).await()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(result.isCompleted).isTrue()
        assertThat(result.getCompletionExceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    public fun getTileContents_failsOnTimeout(): Unit = runTest {
        val expectedTile = TileProto.Tile.newBuilder().setResourcesVersion("5").build()
        fakeTileService.returnTile = expectedTile.toByteArray()
        fakeTileService.shouldReturnTile = false

        val stdDispatcher = StandardTestDispatcher(testScheduler)
        clientUnderTest = DefaultTileClient(
            appContext, TILE_PROVIDER,
            this, stdDispatcher
        )

        // This has to be dispatched on the correct dispatcher, so we can fully control its timing.
        val result = async(stdDispatcher + Job()) {
            clientUnderTest.requestTile(RequestBuilders.TileRequest.Builder().build()).await()
        }
        stdDispatcher.scheduler.runCurrent()
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...

        assertThat(result.isCompleted).isFalse()

        stdDispatcher.scheduler.advanceTimeBy(DefaultTileClient.TIMEOUT_MILLIS / 2)
        stdDispatcher.scheduler.runCurrent()
        assertThat(result.isCompleted).isFalse()

        stdDispatcher.scheduler.advanceTimeBy(DefaultTileClient.TIMEOUT_MILLIS / 2)
        stdDispatcher.scheduler.runCurrent()
        assertThat(result.isCompleted).isTrue()

        assertThat(result.getCompletionExceptionOrNull())
            .isInstanceOf(TimeoutCancellationException::class.java)
    }

    @Test
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    public fun getResources_canGetResources(): Unit = fakeCoroutineScope.runTest {
        val expectedResources = androidx.wear.tiles.ResourceBuilders.Resources.Builder()
            .setVersion("5")
            .build()
        fakeTileService.returnResources = expectedResources.toProto().toByteArray()

        val result = async {
            clientUnderTest.requestResources(
                RequestBuilders.ResourcesRequest.Builder().build()
            ).await()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(result.await().toProto()).isEqualTo(expectedResources.toProto())
    }

    @Test
    public fun getResources_failsIfUnparsableResult(): Unit = fakeCoroutineScope.runTest {
        fakeTileService.returnResources = byteArrayOf(127)

        val result = async(Job()) {
            clientUnderTest.requestResources(
                RequestBuilders.ResourcesRequest.Builder().build()
            ).await()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(result.isCompleted).isTrue()
        assertThat(result.getCompletionExceptionOrNull()).isInstanceOf(
            InvalidProtocolBufferException::class.java
        )
    }

    @Test
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    public fun getResources_failsIfVersionMismatch(): Unit = fakeCoroutineScope.runTest {
        val expectedResources = androidx.wear.tiles.ResourceBuilders.Resources.Builder()
            .setVersion("5")
            .build()
        fakeTileService.returnResources = expectedResources.toProto().toByteArray()
        fakeTileService.returnResourcesVersion = -2

        val result = async(Job()) {
            clientUnderTest.requestResources(
                RequestBuilders.ResourcesRequest.Builder().build()
            ).await()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(result.isCompleted).isTrue()
        assertThat(result.getCompletionExceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    public fun getResources_failsOnTimeout(): Unit = runTest {
        val expectedResources = androidx.wear.tiles.ResourceBuilders.Resources.Builder()
            .setVersion("5")
            .build()
        fakeTileService.returnResources = expectedResources.toProto().toByteArray()
        fakeTileService.shouldReturnResources = false

        val stdDispatcher = StandardTestDispatcher(testScheduler)
        clientUnderTest = DefaultTileClient(
            appContext, TILE_PROVIDER,
            this, stdDispatcher
        )

        // This has to be dispatched on the correct dispatcher, so we can fully control its timing.
        val result = async(stdDispatcher + Job()) {
            clientUnderTest.requestResources(
                RequestBuilders.ResourcesRequest.Builder().build()
            ).await()
        }
        stdDispatcher.scheduler.runCurrent()
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...

        assertThat(result.isCompleted).isFalse()

        advanceTimeBy(DefaultTileClient.TIMEOUT_MILLIS / 2)
        stdDispatcher.scheduler.runCurrent()
        assertThat(result.isCompleted).isFalse()

        advanceTimeBy(DefaultTileClient.TIMEOUT_MILLIS / 2)
        stdDispatcher.scheduler.runCurrent()
        assertThat(result.isCompleted).isTrue()

        assertThat(result.getCompletionExceptionOrNull())
            .isInstanceOf(TimeoutCancellationException::class.java)
    }

    @Test
    public fun onTileAdd_callsThrough(): Unit = fakeCoroutineScope.runTest {
        val job = launch {
            clientUnderTest.sendOnTileAddedEvent().await()
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...
        job.join()

        assertThat(fakeTileService.onTileAddCalled).isTrue()
    }

    @Test
    public fun onTileRemove_callsThrough(): Unit = fakeCoroutineScope.runTest {
        val job = launch {
            clientUnderTest.sendOnTileRemovedEvent().await()
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...
        job.join()

        assertThat(fakeTileService.onTileRemoveCalled).isTrue()
    }

    @Test
    public fun onTileEnter_callsThrough(): Unit = fakeCoroutineScope.runTest {
        val job = launch {
            clientUnderTest.sendOnTileEnterEvent().await()
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...
        job.join()

        assertThat(fakeTileService.onTileEnterCalled).isTrue()
    }

    @Test
    public fun onTileLeave_callsThrough(): Unit = fakeCoroutineScope.runTest {
        val job = launch {
            clientUnderTest.sendOnTileLeaveEvent().await()
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle() // Ensure it actually binds...
        job.join()

        assertThat(fakeTileService.onTileLeaveCalled).isTrue()
    }

    private class FakeTileService : TileProvider.Stub() {
        var shouldReturnTile = true
        var returnTile = ByteArray(0)
        var returnTileVersion = TileData.VERSION_PROTOBUF

        var shouldReturnResources = true
        var returnResources = ByteArray(0)
        var returnResourcesVersion = ResourcesData.VERSION_PROTOBUF

        var onTileAddCalled = false
        var onTileEnterCalled = false
        var onTileLeaveCalled = false
        var onTileRemoveCalled = false

        override fun getApiVersion(): Int {
            return 5
        }

        override fun onTileRequest(
            id: Int,
            requestData: TileRequestData?,
            callback: TileCallback?
        ) {
            if (shouldReturnTile) {
                callback!!.updateTileData(TileData(returnTile, returnTileVersion))
            }
        }

        override fun onResourcesRequest(
            id: Int,
            requestData: ResourcesRequestData?,
            callback: ResourcesCallback?
        ) {
            if (shouldReturnResources) {
                callback!!.updateResources(ResourcesData(returnResources, returnResourcesVersion))
            }
        }

        override fun onTileAddEvent(requestData: TileAddEventData?) {
            onTileAddCalled = true
        }

        override fun onTileRemoveEvent(requestData: TileRemoveEventData?) {
            onTileRemoveCalled = true
        }

        override fun onTileEnterEvent(requestData: TileEnterEventData?) {
            onTileEnterCalled = true
        }

        override fun onTileLeaveEvent(requestData: TileLeaveEventData?) {
            onTileLeaveCalled = true
        }
    }
}