/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.viewfinder

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import android.view.TextureView
import android.widget.FrameLayout
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.surface.populateFromCharacteristics
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class TextureViewImplementationTest {
    private var parent: FrameLayout? = null
    private var implementation: TextureViewImplementation? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var _surfaceRequest: ViewfinderSurfaceRequest? = null
    private val surfaceRequest: ViewfinderSurfaceRequest
        get() {
            if (_surfaceRequest == null) {
                val cameraManager =
                    ApplicationProvider.getApplicationContext<Context>()
                        .getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraIds = cameraManager.cameraIdList
                Assume.assumeTrue("No cameras found on device.", cameraIds.isNotEmpty())
                val cameraId = cameraIds[0]
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                _surfaceRequest =
                    ViewfinderSurfaceRequest.Builder(ANY_SIZE)
                        .populateFromCharacteristics(characteristics)
                        .build()
            }
            return _surfaceRequest!!
        }

    @Before
    fun setUp() {
        val mContext = InstrumentationRegistry.getInstrumentation().targetContext
        surfaceTexture = SurfaceTexture(0)
        parent = FrameLayout(mContext)
        implementation = TextureViewImplementation(parent!!, ViewfinderTransformation())
    }

    @After
    fun tearDown() {
        if (_surfaceRequest != null) {
            _surfaceRequest!!.willNotProvideSurface()
            // Ensure all successful requests have their returned future finish.
            _surfaceRequest = null
        }
    }

    @Ignore // b/324125795
    @LargeTest
    @Test(expected = TimeoutException::class)
    @Throws(Exception::class)
    fun doNotProvideSurface_ifSurfaceTextureNotAvailableYet() {
        val request = surfaceRequest
        implementation!!.onSurfaceRequested(request)
        request.getSurfaceAsync()[2, TimeUnit.SECONDS]
    }

    @Ignore // b/324125795
    @Test
    @Throws(Exception::class)
    fun provideSurface_ifSurfaceTextureAvailable() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        implementation!!
            .mTextureView
            ?.surfaceTextureListener!!
            .onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        val surface = surfaceListenableFuture.get()
        Truth.assertThat(surface).isNotNull()
    }

    @Ignore // b/324125795
    @Test
    @Throws(Exception::class)
    fun doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNotNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isFalse()
    }

    @Ignore // b/324125795
    @Test
    @LargeTest
    @Throws(Exception::class)
    fun destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceRequest.markSurfaceSafeToRelease()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
    }

    @Ignore // b/324125795
    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        surfaceRequest.markSurfaceSafeToRelease()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
    }

    @Ignore // b/324125795
    @Test
    @LargeTest
    @Throws(Exception::class)
    fun nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        // Cancel the request from the camera side
        surfaceRequest.getSurfaceAsync().cancel(true)

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceRequest).isNull()
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
    }

    @Ignore // b/324125795
    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseSurface_whenSurfaceTextureDestroyed_andCameraSurfaceRequestIsCancelled() {
        implementation!!.onSurfaceRequested(surfaceRequest)
        // Cancel the request from the client side
        surfaceRequest.willNotProvideSurface()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        // Wait enough time for surfaceReleaseFuture's listener to be called.
        Thread.sleep(1000)
        Truth.assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!))
            .isTrue()
        Truth.assertThat(implementation!!.mSurfaceTexture).isNull()
    }

    @Ignore // b/324125795
    @Test
    fun doNotCreateTextureView_beforeSensorOutputSizeKnown() {
        Truth.assertThat(parent!!.childCount).isEqualTo(0)
    }

    @Ignore // b/324125795
    @Test
    @Throws(Exception::class)
    fun resetSurfaceTextureOnDetachAndAttachWindow() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        implementation!!.onDetachedFromWindow()
        implementation!!.onAttachedToWindow()
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
        Truth.assertThat(implementation!!.mTextureView?.surfaceTexture).isEqualTo(surfaceTexture)
    }

    @Ignore // b/324125795
    @Test
    @LargeTest
    @Throws(Exception::class)
    fun releaseDetachedSurfaceTexture_whenDeferrableSurfaceClose() {
        val surfaceRequest = surfaceRequest
        implementation!!.onSurfaceRequested(surfaceRequest)
        val surfaceListenableFuture = surfaceRequest.getSurfaceAsync()
        val surfaceTextureListener = implementation!!.mTextureView?.surfaceTextureListener
        surfaceTextureListener!!.onSurfaceTextureAvailable(surfaceTexture!!, ANY_WIDTH, ANY_HEIGHT)
        surfaceListenableFuture.get()
        surfaceTextureListener.onSurfaceTextureDestroyed(surfaceTexture!!)
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNotNull()
        surfaceRequest.markSurfaceSafeToRelease()

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1000)
        Truth.assertThat(implementation!!.mSurfaceReleaseFuture).isNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Truth.assertThat(surfaceTexture!!.isReleased).isTrue()
        }
        Truth.assertThat(implementation!!.mDetachedSurfaceTexture).isNull()
    }

    @Ignore // b/324125795
    @Test
    fun keepOnlyLatestTextureView_whenGetSurfaceProviderCalledMultipleTimes() {
        implementation!!.onSurfaceRequested(surfaceRequest)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView1 = parent!!.getChildAt(0) as TextureView
        implementation!!.onSurfaceRequested(surfaceRequest)
        Truth.assertThat(parent!!.getChildAt(0)).isInstanceOf(TextureView::class.java)
        val textureView2 = parent!!.getChildAt(0) as TextureView
        Truth.assertThat(textureView1).isNotSameInstanceAs(textureView2)
        Truth.assertThat(parent!!.childCount).isEqualTo(1)
    }

    companion object {
        private const val ANY_WIDTH = 1600
        private const val ANY_HEIGHT = 1200
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }
    }
}
