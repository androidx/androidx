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
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.fakes.FakeActivity
import androidx.camera.testing.fakes.FakeCamera
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceViewImplementationTest {

    private val ANY_WIDTH = 640
    private val ANY_HEIGHT = 480
    private val ANY_SIZE = Size(ANY_WIDTH, ANY_HEIGHT)

    private lateinit var mParent: FrameLayout
    private lateinit var mImplementation: SurfaceViewImplementation
    private val mInstrumentation =
        InstrumentationRegistry.getInstrumentation()
    private lateinit var mSurfaceRequest: SurfaceRequest
    private lateinit var mContext: Context

    // Shows the view in activity so that SurfaceView can work normally
    @Suppress("DEPRECATION")
    @get:Rule
    var mActivityRule = androidx.test.rule.ActivityTestRule(
        FakeActivity::class.java, false, false
    )

    @Throws(Throwable::class)
    private fun setContentView(view: View) {
        val activity: Activity = mActivityRule.activity
        mActivityRule.runOnUiThread { activity.setContentView(view) }
    }

    @Before
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(mInstrumentation)
        mActivityRule.launchActivity(null)
        mContext = ApplicationProvider.getApplicationContext<Context>()
        mParent = FrameLayout(mContext)
        setContentView(mParent)

        mSurfaceRequest = SurfaceRequest(ANY_SIZE, FakeCamera(), false)
        mImplementation = SurfaceViewImplementation(mParent, PreviewTransformation())
    }

    @After
    fun tearDown() {
        mSurfaceRequest.deferrableSurface.close()
    }

    @Test
    fun surfaceProvidedSuccessfully() {
        CoreAppTestUtil.checkKeyguard(mContext)

        mInstrumentation.runOnMainSync {
            mImplementation.onSurfaceRequested(mSurfaceRequest, null)
        }

        mSurfaceRequest.deferrableSurface.surface.get(1000, TimeUnit.MILLISECONDS)
        mSurfaceRequest.deferrableSurface.close()
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceIsNotUsedAnyMore() {
        CoreAppTestUtil.checkKeyguard(mContext)

        var listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = {
            listenerLatch.countDown()
        }

        mInstrumentation.runOnMainSync {
            mImplementation.onSurfaceRequested(mSurfaceRequest, onSurfaceNotInUseListener)
        }
        mSurfaceRequest.deferrableSurface.surface.get(1000, TimeUnit.MILLISECONDS)
        mSurfaceRequest.deferrableSurface.close()

        assertThat(listenerLatch.await(300, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun onSurfaceNotInUseListener_isCalledWhenSurfaceRequestIsCancelled() {
        var listenerLatch = CountDownLatch(1)
        val onSurfaceNotInUseListener = {
            listenerLatch.countDown()
        }

        // Not attach the mParent to the window so that the Surface cannot be created.
        setContentView(View(ApplicationProvider.getApplicationContext()))

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
}