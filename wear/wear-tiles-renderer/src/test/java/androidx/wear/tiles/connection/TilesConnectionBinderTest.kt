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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.tiles.ResourcesCallback
import androidx.wear.tiles.ResourcesRequestData
import androidx.wear.tiles.TileAddEventData
import androidx.wear.tiles.TileCallback
import androidx.wear.tiles.TileEnterEventData
import androidx.wear.tiles.TileLeaveEventData
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileRemoveEventData
import androidx.wear.tiles.TileRequestData
import androidx.wear.tiles.TilesTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.IllegalStateException

@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(TilesTestRunner::class)
public class TilesConnectionBinderTest {
    public companion object {
        private val TILE_PROVIDER = ComponentName("HelloWorld", "FooBarBaz")
    }

    private lateinit var appContext: Context
    private lateinit var fakeTileProvider: FakeTileProviderService
    private lateinit var fakeCoroutineDispatcher: TestCoroutineDispatcher
    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private lateinit var connectionBinderUnderTest: TilesConnectionBinder

    @Before
    public fun setUp() {
        appContext = getApplicationContext()
        fakeTileProvider = FakeTileProviderService()
        fakeCoroutineDispatcher = TestCoroutineDispatcher()
        fakeCoroutineScope = TestCoroutineScope()

        shadowOf(appContext as Application)
            .setComponentNameAndServiceForBindService(TILE_PROVIDER, fakeTileProvider.asBinder())

        connectionBinderUnderTest = TilesConnectionBinder(
            appContext, TILE_PROVIDER,
            fakeCoroutineScope, fakeCoroutineDispatcher
        )
    }

    @After
    public fun tearDown() {
        fakeCoroutineDispatcher.advanceUntilIdle()

        fakeCoroutineDispatcher.cleanupTestCoroutines()
        fakeCoroutineScope.cleanupTestCoroutines()
    }

    @Test
    public fun canCallTileProvider(): Unit = fakeCoroutineScope.runBlockingTest {
        val result = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        // This is a little nasty, as we have to handle the interactions between the fake looper
        // and the fake coroutine dispatcher. TestCoroutineDispatcher should run everything
        // eagerly, so we now need to idle the main looper to get bind to be called.
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(result.await()).isEqualTo(5)
    }

    @Test
    public fun binderLeftOpen(): Unit = fakeCoroutineScope.runBlockingTest {
        val result = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result.await()

        // Ensure that the binder is still bound.
        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).isEmpty()
    }

    @Test
    public fun binderClosesAfterTimeout(): Unit = fakeCoroutineScope.runBlockingTest {
        val result = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result.await()

        // Wait for the timeout
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS)

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(shadowOf(appContext as Application).boundServiceConnections).isEmpty()
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).hasSize(1)
    }

    @Test
    public fun twoCallsShareSameBinder(): Unit = fakeCoroutineScope.runBlockingTest {
        val result1 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result1.await()

        val result2 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        result2.await()

        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).isEmpty()
    }

    @Test
    public fun longRunningCallsSuspendsBinderKill(): Unit = fakeCoroutineScope.runBlockingTest {
        val result = async(fakeCoroutineDispatcher) {
            connectionBinderUnderTest.runWithTilesConnection {
                delay(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS * 2)
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Binder should be shut down by this time, if there's nothing outstanding
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS)

        assertThat(result.isCompleted).isFalse()

        // Binder should still be alive...
        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).isEmpty()

        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS)

        assertThat(result.isCompleted).isTrue()

        // Still alive...
        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).isEmpty()

        // Shut down.
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(shadowOf(appContext as Application).boundServiceConnections).isEmpty()
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).hasSize(1)
    }

    @Test
    public fun anotherCallPostponesUnbind(): Unit = fakeCoroutineScope.runBlockingTest {
        val result1 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result1.await()

        // Wait a while...
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS / 2)

        val result2 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        result2.await()

        // Wait for the rest of the inactivity period.
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS / 2)

        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).isEmpty()

        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS / 2)

        assertThat(shadowOf(appContext as Application).boundServiceConnections).isEmpty()
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).hasSize(1)
    }

    @Test
    public fun canRebindAfterUnbind(): Unit = fakeCoroutineScope.runBlockingTest {
        val result1 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result1.await()

        // Wait a while...
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.INACTIVITY_TIMEOUT_MILLIS)

        val result2 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                it.apiVersion
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        result2.await()

        assertThat(shadowOf(appContext as Application).boundServiceConnections).hasSize(1)
        assertThat(shadowOf(appContext as Application).unboundServiceConnections).hasSize(1)
    }

    @Test
    public fun exceptionInCallPropagates(): Unit = fakeCoroutineScope.runBlockingTest {
        val result1 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                throw IllegalStateException("Hello")
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(result1.getCompletionExceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test(expected = TimeoutCancellationException::class)
    public fun bindCanTimeOut(): Unit = fakeCoroutineScope.runBlockingTest {
        val result1 = async {
            connectionBinderUnderTest.runWithTilesConnection {
                5
            }
        }

        // Never idle Robolectric's looper, so it can't call ServiceConnection#onServiceConnected.
        fakeCoroutineDispatcher.advanceTimeBy(TilesConnectionBinder.BIND_TIMEOUT_MILLIS * 2)

        // Await to throw the exception.
        result1.await()
    }

    private class FakeTileProviderService : TileProvider.Stub() {
        override fun getApiVersion(): Int {
            return 5
        }

        override fun onTileRequest(
            id: Int,
            requestData: TileRequestData?,
            callback: TileCallback?
        ) {
            TODO("Not yet implemented")
        }

        override fun onResourcesRequest(
            id: Int,
            requestData: ResourcesRequestData?,
            callback: ResourcesCallback?
        ) {
            TODO("Not yet implemented")
        }

        override fun onTileAddEvent(requestData: TileAddEventData?) {
            TODO("Not yet implemented")
        }

        override fun onTileRemoveEvent(requestData: TileRemoveEventData?) {
            TODO("Not yet implemented")
        }

        override fun onTileEnterEvent(requestData: TileEnterEventData?) {
            TODO("Not yet implemented")
        }

        override fun onTileLeaveEvent(requestData: TileLeaveEventData?) {
            TODO("Not yet implemented")
        }
    }
}