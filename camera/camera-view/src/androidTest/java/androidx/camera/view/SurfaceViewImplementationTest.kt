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

import android.content.Context
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.view.PreviewViewImplementation.OnSurfaceNotInUseListener
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class SurfaceViewImplementationTest {

    companion object {
        private const val ANY_WIDTH = 640
        private const val ANY_HEIGHT = 480
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }
    }

    private lateinit var mParent: FrameLayout
    private lateinit var mImplementation: SurfaceViewImplementation
    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var mSurfaceRequest: SurfaceRequest
    private lateinit var mContext: Context

    // Shows the view in activity so that SurfaceView can work normally
    private lateinit var mActivityScenario: ActivityScenario<FakeActivity>

    @Before
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(mInstrumentation)

        mActivityScenario = ActivityScenario.launch(FakeActivity::class.java)
        mContext = ApplicationProvider.getApplicationContext()
        mParent = FrameLayout(mContext)
        setContentView(mParent)

        mSurfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
        mImplementation = SurfaceViewImplementation(mParent, PreviewTransformation())
    }

    @After
    fun tearDown() {
        mSurfaceRequest.deferrableSurface.close()
    }

    @Test
    fun surfaceProvidedSuccessfully() {
        CoreAppTestUtil.checkKeyguard(mContext)

        mImplementation.testSurfaceRequest(mSurfaceRequest)
    }

    @Test
    fun reuseSurfaceView_whenResolutionNotChanged() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        mImplementation.testSurfaceRequest(mSurfaceRequest)
        val previousSurfaceView = mImplementation.mSurfaceView

        // Act.
        val sameResolutionSurfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera()) {}
        mImplementation.testSurfaceRequest(sameResolutionSurfaceRequest)
        val newSurfaceView = mImplementation.mSurfaceView

        // Assert.
        assertThat(newSurfaceView).isEqualTo(previousSurfaceView)
    }

    @Test
    fun notReuseSurfaceView_whenResolutionChanged() {
        // Arrange.
        CoreAppTestUtil.checkKeyguard(mContext)
        mImplementation.testSurfaceRequest(mSurfaceRequest)
        val previousSurfaceView = mImplementation.mSurfaceView

        // Act.
        val differentSize: Size by lazy { Size(720, 480) }
        val differentResolutionSurfaceRequest =
            SurfaceRequest(differentSize, FakeCamera()) {}
        mImplementation.testSurfaceRequest(differentResolutionSurfaceRequest)
        val newSurfaceView = mImplementation.mSurfaceView

        // Assert.
        assertThat(newSurfaceView).isNotEqualTo(previousSurfaceView)
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceIsNotUsedAnyMore() {
        CoreAppTestUtil.checkKeyguard(mContext)

        val listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = {
            listenerLatch.countDown()
        }

        mImplementation.testSurfaceRequest(mSurfaceRequest, onSurfaceNotInUseListener)

        assertThat(listenerLatch.await(300, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceRequestIsCancelled() {
        val listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = {
            listenerLatch.countDown()
        }

        // Not attach the mParent to the window so that the Surface cannot be created.
        setContentView(View(mContext))

        mInstrumentation.runOnMainSync {
            mImplementation.onSurfaceRequested(mSurfaceRequest, onSurfaceNotInUseListener)
        }

        // Since the surface is not created,  close the deferrableSurface will trigger only
        // SurfaceRequest RequestCancellationListener
        mSurfaceRequest.deferrableSurface.close()

        assertThat(listenerLatch.await(300, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun waitForNextFrame_futureCompletesImmediately() {
        val future = mImplementation.waitForNextFrame()
        future.get(20, TimeUnit.MILLISECONDS)
    }

    @Throws(Throwable::class)
    private fun setContentView(view: View) {
        mActivityScenario.onActivity { activity -> activity.setContentView(view) }
    }

    private fun SurfaceViewImplementation.testSurfaceRequest(
        surfaceRequest: SurfaceRequest,
        listener: OnSurfaceNotInUseListener? = null
    ) {
        mInstrumentation.runOnMainSync {
            onSurfaceRequested(surfaceRequest, listener)
        }

        surfaceRequest.deferrableSurface.surface.get(1000, TimeUnit.MILLISECONDS)
        surfaceRequest.deferrableSurface.close()
    }
}
