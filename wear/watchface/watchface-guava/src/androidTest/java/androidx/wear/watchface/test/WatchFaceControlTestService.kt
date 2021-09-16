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

package androidx.wear.watchface.test

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.control.IWatchFaceInstanceServiceStub
import androidx.wear.watchface.control.WatchFaceControlService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.mockito.Mockito
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal const val TIMEOUT_MILLIS = 1000L

/**
 * Test shim to allow us to connect to WatchFaceControlService from a test and to optionally
 * override the reported API version.
 */
public class WatchFaceControlTestService : Service() {
    public companion object {
        /**
         * If non-null this overrides the API version reported by [IWatchFaceInstanceServiceStub].
         */
        public var apiVersionOverride: Int? = null
    }

    private val realService = object : WatchFaceControlService() {
        override fun createServiceStub(): IWatchFaceInstanceServiceStub =
            object : IWatchFaceInstanceServiceStub(this, Handler(Looper.getMainLooper())) {
                @RequiresApi(Build.VERSION_CODES.O_MR1)
                override fun getApiVersion(): Int = apiVersionOverride ?: super.getApiVersion()
            }

        init {
            setContext(ApplicationProvider.getApplicationContext<Context>())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onBind(intent: Intent?): IBinder? = realService.onBind(intent)
}

/** Base class for the various async tests. */
public open class WatchFaceControlClientServiceTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val handler = Handler(Looper.getMainLooper())
    val handlerCoroutineScope = CoroutineScope(handler.asCoroutineDispatcher().immediate)

    val surfaceHolder = Mockito.mock(SurfaceHolder::class.java)
    val surface = Mockito.mock(Surface::class.java)
    val renderLatch = CountDownLatch(1)

    val surfaceTexture = SurfaceTexture(false)
    val glSurface = Surface(surfaceTexture)
    val glSurfaceHolder = Mockito.mock(SurfaceHolder::class.java)

    val watchFaceControlClientService = runBlocking {
        WatchFaceControlClient.createWatchFaceControlClientImpl(
            context,
            Intent(context, WatchFaceControlTestService::class.java).apply {
                action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
            }
        )
    }

    @Before
    fun setUp() {
        Mockito.`when`(surfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 100, 100))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderLatch.countDown()
        }

        surfaceTexture.setDefaultBufferSize(10, 10)
        Mockito.`when`(glSurfaceHolder.surface).thenReturn(glSurface)
        Mockito.`when`(glSurfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 10, 10))
    }

    fun <X> awaitWithTimeout(
        thing: Deferred<X>,
        timeoutMillis: Long = TIMEOUT_MILLIS
    ): X {
        var value: X? = null
        val latch = CountDownLatch(1)
        handlerCoroutineScope.launch {
            value = thing.await()
            latch.countDown()
        }
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout waiting for thing!")
        }
        return value!!
    }
}