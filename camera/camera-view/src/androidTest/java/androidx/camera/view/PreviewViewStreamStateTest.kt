/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.fakes.FakeActivity
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class PreviewViewStreamStateTest(private val implMode: PreviewView.ImplementationMode) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            PreviewView.ImplementationMode.TEXTURE_VIEW,
            PreviewView.ImplementationMode.SURFACE_VIEW
        )
    }

    private lateinit var mPreviewView: PreviewView
    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var mLifecycle: FakeLifecycleOwner
    private lateinit var mCameraProvider: ProcessCameraProvider
    @get:Rule
    var mCameraPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    var mActivityRule = ActivityTestRule(
        FakeActivity::class.java
    )

    @Throws(Throwable::class)
    private fun setContentView(view: View) {
        val activity: Activity = mActivityRule.activity
        mActivityRule.runOnUiThread { activity.setContentView(view) }
    }

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        CoreAppTestUtil.assumeCompatibleDevice()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Camera2Config.defaultConfig()
        CameraX.initialize(context, config)
        mLifecycle = FakeLifecycleOwner()
        mPreviewView = PreviewView(context)
        setContentView(mPreviewView)
        mPreviewView.preferredImplementationMode = implMode

        mCameraProvider = ProcessCameraProvider.getInstance(context).get()
    }

    @After
    fun tearDown() {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync { CameraX.unbindAll() }
        }
        CameraX.shutdown().get()
    }

    private fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraSelector: CameraSelector
    ): Preview {
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        mInstrumentation.runOnMainSync {
            preview.setSurfaceProvider(previewView.createSurfaceProvider())
            mCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        }

        return preview
    }

    @Test
    fun streamState_IDLE_TO_STREAMING_startPreview() {
        assertStreamState(PreviewView.StreamState.IDLE)

        startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_BACK_CAMERA)
        mLifecycle.startAndResume()

        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_TO_STREAMING_lifecycleStopAndStart() {
        startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_BACK_CAMERA)
        mLifecycle.startAndResume()
        assertStreamState(PreviewView.StreamState.STREAMING)

        mLifecycle.pauseAndStop()
        assertStreamState(PreviewView.StreamState.IDLE)

        mLifecycle.startAndResume()
        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_unbindAll() {
        startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_BACK_CAMERA)
        mLifecycle.startAndResume()
        assertStreamState(PreviewView.StreamState.STREAMING)

        mInstrumentation.runOnMainSync { mCameraProvider.unbindAll() }
        assertStreamState(PreviewView.StreamState.IDLE)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_unbindPreviewOnly() {
        val preview = startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_BACK_CAMERA)

        mLifecycle.startAndResume()
        assertStreamState(PreviewView.StreamState.STREAMING)

        mInstrumentation.runOnMainSync { mCameraProvider.unbind(preview) }
        assertStreamState(PreviewView.StreamState.IDLE)
    }

    @Test
    fun streamState_STREAMING_TO_IDLE_TO_STREAMING_switchCamera() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_BACK_CAMERA)
        mLifecycle.startAndResume()
        assertStreamState(PreviewView.StreamState.STREAMING)

        mInstrumentation.runOnMainSync { mCameraProvider.unbindAll() }
        startPreview(mLifecycle, mPreviewView, CameraSelector.DEFAULT_FRONT_CAMERA)

        assertStreamState(PreviewView.StreamState.IDLE)
        assertStreamState(PreviewView.StreamState.STREAMING)
    }

    private fun assertStreamState(expectStreamState: PreviewView.StreamState) {
        val latchForState = CountDownLatch(1)

        val observer = Observer<PreviewView.StreamState> { streamState ->
            if (streamState == expectStreamState) {
                latchForState.countDown()
            }
        }
        mInstrumentation.runOnMainSync {
            mPreviewView.previewStreamState.observeForever(observer)
        }

        try {
            assertThat(latchForState.await(5000, TimeUnit.MILLISECONDS)).isTrue()
        } finally {
            mInstrumentation.runOnMainSync {
                mPreviewView.previewStreamState.removeObserver(observer)
            }
        }
    }
}