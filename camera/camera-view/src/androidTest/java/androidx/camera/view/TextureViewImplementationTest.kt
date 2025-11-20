/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.camera.view

import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.TextureView
import android.widget.FrameLayout
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.view.PreviewViewImplementation.OnSurfaceNotInUseListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextureViewImplementationTest {
    private var parent: FrameLayout? = null
    private var implementation: TextureViewImplementation? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var _surfaceRequest: SurfaceRequest? = null
    private val surfaceRequest: SurfaceRequest
        get() {
            if (_surfaceRequest == null) {
                _surfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
            }
            return _surfaceRequest!!
        }

    @Before
    fun setUp() {
        val mContext = InstrumentationRegistry.getInstrumentation().targetContext
        surfaceTexture = SurfaceTexture(0)
        parent = FrameLayout(mContext)
        implementation = TextureViewImplementation(parent!!, PreviewTransformation())
    }

    @After
    fun tearDown() {
        if (_surfaceRequest != null) {
            _surfaceRequest!!.willNotProvideSurface()
            // Ensure all successful requests have their returned future finish.
            _surfaceRequest!!.deferrableSurface.close()
            _surfaceRequest = null
        }
    }

    @LargeTest
    @Test(expected = TimeoutException::class)
    @Throws(Exception::class)
    fun doNotProvideSurface_ifSurfaceTextureNotAvailableYet() {
        val request = surfaceRequest
        implementation!!.onSurfaceRequested(request, null)
        request.deferrableSurface.surface[2, TimeUnit.SECONDS]
    }

    @Test
    @Throws(Exception::class)
    fun provideSurface_ifSurfaceTextureAvailable() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceListenableFuture = surfaceRequest.deferrableSurface.surface
        implementation!!
            .mTextureView
            .surfaceTextureListener!!
            .onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        val surface = surfaceListenableFuture.get()
        Truth.assertThat(surface).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceListenableFuture = surfaceRequest.deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNotNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isFalse()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun onSurfaceNotInUseListener_IsCalledWhenCameraNotUsingSurface() {
        val surfaceRequest = surfaceRequest
        val latchForSurfaceNotInUse = CountDownLatch(1)
        val onSurfaceNotInUseListener = OnSurfaceNotInUseListener {
            latchForSurfaceNotInUse.countDown()
        }
        implementation!!.onSurfaceRequested(surfaceRequest, onSurfaceNotInUseListener)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        deferrableSurface.close()
        Truth.assertThat(latchForSurfaceNotInUse.await(500, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    @MediumTest
    @Throws(Exception::class)
    fun onSurfaceNotInUseListener_IsCalledWhenSurfaceRequestIsCancelled() {
        val surfaceRequest = surfaceRequest
        val latchForSurfaceNotInUse = CountDownLatch(1)
        val onSurfaceNotInUseListener = OnSurfaceNotInUseListener {
            latchForSurfaceNotInUse.countDown()
        }
        implementation!!.onSurfaceRequested(surfaceRequest, onSurfaceNotInUseListener)
        val deferrableSurface = surfaceRequest.deferrableSurface
        deferrableSurface.surface

        // onSurfaceTextureAvailable is not called, so the surface will not be provided.
        // closing the surface will only trigger the SurfaceRequest RequestCancellationListener.
        deferrableSurface.close()
        Truth.assertThat(latchForSurfaceNotInUse.await(500, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        // Cancel the request from the camera side
        surfaceRequest.deferrableSurface.surface.cancel(true)

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceRequest).isNull()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurface_whenSurfaceTextureDestroyed_andCameraSurfaceRequestIsCancelled() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        // Cancel the request from the client side
        surfaceRequest.willNotProvideSurface()
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        // Wait enough time for surfaceReleaseFuture's listener to be called.
        Thread.sleep(1000)
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
        Truth.assertThat(implementation!!.mSurfaceTexture).isNull()
    }

    @Test
    fun doNotCreateTextureView_beforeSensorOutputSizeKnown() {
        Truth.assertThat(parent!!.childCount).isEqualTo(0)
    }

    @Test
    @Throws(Exception::class)
    fun resetSurfaceTextureOnDetachAndAttachWindow() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        implementation!!.onDetachedFromWindow()
        implementation!!.onAttachedToWindow()
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
        Truth.assertThat(implementation!!.mTextureView.surfaceTexture).isEqualTo(surfaceTexture)
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseDetachedSurfaceTexture_whenDeferrableSurfaceClose() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val deferrableSurface = surfaceRequest.deferrableSurface
        val surfaceListenableFuture = deferrableSurface.surface
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        deferrableSurface.close()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
    }

    @Test
    fun keepOnlyLatestTextureView_whenGetSurfaceProviderCalledMultipleTimes() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView1 = parent!!.getChildAt(0) as TextureView
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView2 = parent!!.getChildAt(0) as TextureView
        Truth.assertThat(textureView1).isNotSameInstanceAs(textureView2)
        Truth.assertThat(parent!!.childCount).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun waitForNextFrame_futureCompletesWhenFrameArrives() {
        implementation!!.onSurfaceRequested(surfaceRequest, null)
        val surfaceTextureListener = implementation!!.mTextureView.surfaceTextureListener
        val futureNextFrame = implementation!!.waitForNextFrame()
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceTextureListener.onSurfaceTextureUpdated(surfaceTexture!!)
        futureNextFrame[300, TimeUnit.MILLISECONDS]
    }

    companion object {
        private const val ANY_WIDTH = 1600
        private const val ANY_HEIGHT = 1200
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }
    }
}
