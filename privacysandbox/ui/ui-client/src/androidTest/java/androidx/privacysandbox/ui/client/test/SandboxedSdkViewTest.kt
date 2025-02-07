/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.privacysandbox.ui.client.test

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.IBinder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionConstants
import androidx.privacysandbox.ui.integration.testingutils.TestEventListener
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/374919355): Create a common test for View and Compose
@RunWith(AndroidJUnit4::class)
@LargeTest
class SandboxedSdkViewTest {
    private lateinit var uiDevice: UiDevice
    private lateinit var context: Context
    private lateinit var view: SandboxedSdkView
    private lateinit var layoutParams: LayoutParams
    private lateinit var testSandboxedUiAdapter: TestSandboxedUiAdapter
    private lateinit var eventListener: TestEventListener
    private lateinit var linearLayout: LinearLayout
    private var mainLayoutWidth = -1
    private var mainLayoutHeight = -1
    @get:Rule var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        activityScenarioRule.withActivity {
            view = SandboxedSdkView(this)
            eventListener = TestEventListener()
            view.setEventListener(eventListener)
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            view.layoutParams = layoutParams
            testSandboxedUiAdapter = TestSandboxedUiAdapter()
            view.setAdapter(testSandboxedUiAdapter)
        }
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun eventListenerErrorTest() {
        activityScenarioRule.withActivity { view.setAdapter(FailingTestSandboxedUiAdapter()) }
        addViewToLayout()
        assertThat(eventListener.errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(eventListener.error?.message).isEqualTo("Error in openSession()")
    }

    @Test
    fun addAndRemoveEventListenerTest() {
        // Initially no events are received when the session is not open.
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        // When session is open, the events are received
        addViewToLayout()
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        // Remove the view from layout to close the session.
        removeAllViewsFromLayout()
        assertThat(eventListener.sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        eventListener.uiDisplayedLatch = CountDownLatch(1)
        // Remove the listener from the view.
        view.setEventListener(null)
        // Add view to layout again to start the session. The latches will not count down this time.
        addViewToLayout()
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun newEventListenerOverridesOldListenerTest() {
        val eventListener1 = TestEventListener()
        val eventListener2 = TestEventListener()
        view.setEventListener(eventListener1)
        view.setEventListener(eventListener2)
        activityScenarioRule.withActivity { view.setAdapter(FailingTestSandboxedUiAdapter()) }
        addViewToLayout()
        assertThat(eventListener1.errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(eventListener2.errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        activityScenarioRule.withActivity { view.setAdapter(testSandboxedUiAdapter) }
        assertThat(eventListener1.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(eventListener2.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        removeAllViewsFromLayout()
        assertThat(eventListener1.sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
            .isFalse()
        assertThat(eventListener2.sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun sessionNotOpenedWhenWindowIsNotVisible() {
        // the window is not visible when the activity is in the CREATED state.
        activityScenarioRule.scenario.moveToState(Lifecycle.State.CREATED)
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionNotOpened()
        // the window becomes visible when the activity is in the STARTED state.
        activityScenarioRule.scenario.moveToState(Lifecycle.State.STARTED)
        testSandboxedUiAdapter.assertSessionOpened()
    }

    @Test
    fun onAttachedToWindowTest() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        assertTrue(view.childCount == 0)
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
        activityScenarioRule.withActivity {
            testSandboxedUiAdapter.internalClient!!.onSessionError(Exception())
            assertTrue(view.childCount == 0)
        }
    }

    @Test
    fun onZOrderChangedTest() {
        addViewToLayout()
        // When session is opened, the provider should not receive a Z-order notification.
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession!!
        val adapter = testSandboxedUiAdapter
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(adapter.isZOrderOnTop).isTrue()
        // When state changes to false, the provider should be notified.
        view.orderProviderUiAboveClientUi(false)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(adapter.isZOrderOnTop).isFalse()
        // When state changes back to true, the provider should be notified.
        session.zOrderChangedLatch = CountDownLatch(1)
        view.orderProviderUiAboveClientUi(true)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(adapter.isZOrderOnTop).isTrue()
    }

    @Test
    fun onZOrderUnchangedTest() {
        addViewToLayout()
        // When session is opened, the provider should not receive a Z-order notification.
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession!!
        val adapter = testSandboxedUiAdapter
        assertThat(adapter.isZOrderOnTop).isTrue()
        // When Z-order state is unchanged, the provider should not be notified.
        session.zOrderChangedLatch = CountDownLatch(1)
        view.orderProviderUiAboveClientUi(true)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(adapter.isZOrderOnTop).isTrue()
    }

    @Test
    fun setZOrderNotOnTopBeforeOpeningSession() {
        view.orderProviderUiAboveClientUi(false)
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession!!
        // The initial Z-order state is passed to the session, but notifyZOrderChanged is not called
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun setZOrderNotOnTopWhileSessionLoading() {
        testSandboxedUiAdapter.delayOpenSessionCallback = true
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        view.orderProviderUiAboveClientUi(false)
        val session = testSandboxedUiAdapter.testSession!!
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        activityScenarioRule.withActivity { testSandboxedUiAdapter.sendOnSessionOpened() }
        // After session has opened, the pending Z order changed made while loading is notified
        // th the session.
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun onConfigurationChangedTest() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        // newWindow() will be triggered by a window state change, even if the activity handles
        // orientation changes without recreating the activity.
        uiDevice.performActionAndWait(
            { uiDevice.setOrientationLeft() },
            Until.newWindow(),
            UI_INTENSIVE_TIMEOUT
        )
        testSandboxedUiAdapter.assertSessionNotClosed()
        uiDevice.performActionAndWait(
            { uiDevice.setOrientationNatural() },
            Until.newWindow(),
            UI_INTENSIVE_TIMEOUT
        )
        testSandboxedUiAdapter.assertSessionNotClosed()
    }

    @Test
    fun onConfigurationChangedTestSameConfiguration() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertThat(testSandboxedUiAdapter.wasOnConfigChangedCalled()).isFalse()
    }

    @Test
    fun overrideProviderViewLayoutParams() {
        val providerViewWidth = (0..1000).random()
        val providerViewHeight = (0..1000).random()
        class CustomSession : AbstractSandboxedUiAdapter.AbstractSession() {
            override val view = View(context)

            init {
                view.layoutParams = LinearLayout.LayoutParams(providerViewWidth, providerViewHeight)
            }
        }
        class CustomUiAdapter : AbstractSandboxedUiAdapter() {
            override fun openSession(
                context: Context,
                sessionConstants: SessionConstants,
                initialWidth: Int,
                initialHeight: Int,
                isZOrderOnTop: Boolean,
                clientExecutor: Executor,
                client: SandboxedUiAdapter.SessionClient
            ) {
                clientExecutor.execute { client.onSessionOpened(CustomSession()) }
            }
        }
        view.setAdapter(CustomUiAdapter())
        addViewToLayout(waitToBeActive = true)
        val contentView = view.getChildAt(0)
        assertThat(contentView.layoutParams.width).isNotEqualTo(providerViewWidth)
        assertThat(contentView.layoutParams.height).isNotEqualTo(providerViewHeight)
        assertThat(contentView.layoutParams.width).isEqualTo(LinearLayout.LayoutParams.WRAP_CONTENT)
        assertThat(contentView.layoutParams.height)
            .isEqualTo(LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    // Verifies that session view resizing does not affect SandboxedSdkView's size
    @Test
    fun sandboxedSdkViewSizeUnchangedWhenSessionViewSizeChanges() {
        val initialWidth = 100
        val initialHeight = 100
        view.layoutParams = LinearLayout.LayoutParams(initialWidth, initialHeight)
        class CustomSession : AbstractSandboxedUiAdapter.AbstractSession() {
            override val view = TextView(context)

            init {
                view.text = "Test View"
            }
        }
        val customSession = CustomSession()
        class CustomUiAdapter : AbstractSandboxedUiAdapter() {
            override fun openSession(
                context: Context,
                sessionConstants: SessionConstants,
                initialWidth: Int,
                initialHeight: Int,
                isZOrderOnTop: Boolean,
                clientExecutor: Executor,
                client: SandboxedUiAdapter.SessionClient
            ) {
                clientExecutor.execute { client.onSessionOpened(customSession) }
            }
        }
        view.setAdapter(CustomUiAdapter())
        addViewToLayout(waitToBeActive = true)
        customSession.view.layout(0, 0, initialWidth * 2, initialHeight * 2)
        assertThat(customSession.view.width).isEqualTo(initialWidth * 2)
        assertThat(customSession.view.height).isEqualTo(initialHeight * 2)
        assertThat(view.width).isEqualTo(initialWidth)
        assertThat(view.height).isEqualTo(initialHeight)
    }

    @Test
    fun onLayoutTestWithSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            view.layoutParams = LinearLayout.LayoutParams(100, 200)
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isTrue()
        assertTrue(view.width == 100 && view.height == 200)
    }

    @Test
    fun onPaddingSetTest() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity { view.setPadding(10, 10, 10, 10) }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isTrue()
    }

    @Test
    fun signalsSentWhenPaddingApplied() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        val paddingLeft = 10
        val paddingTop = 10
        val paddingRight = 20
        val paddingBottom = 20
        session.runAndRetrieveNextUiChange {
            activityScenarioRule.withActivity {
                view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }
        assertThat(session.shortestGapBetweenUiChangeEvents)
            .isAtLeast(SHORTEST_TIME_BETWEEN_SIGNALS_MS)
    }

    @Test
    fun onLayoutTestNoSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            view.layout(view.left, view.top, view.right, view.bottom)
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onLayoutTestViewShiftWithoutSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        val rightShift = 10
        val upperShift = 30
        activityScenarioRule.withActivity {
            view.layout(
                view.left + rightShift,
                view.top - upperShift,
                view.right + rightShift,
                view.bottom - upperShift
            )
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onSdkRequestsResizeTest() {
        val globalLayoutLatch = CountDownLatch(1)
        lateinit var layout: LinearLayout
        activityScenarioRule.withActivity {
            layout = findViewById<LinearLayout>(R.id.mainlayout)
            layout.addView(view)
        }
        testSandboxedUiAdapter.assertSessionOpened()
        testSandboxedUiAdapter.testSession?.requestResize(layout.width, layout.height)
        val observer = view.viewTreeObserver
        observer.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (view.width == layout.width && view.height == layout.height) {
                        globalLayoutLatch.countDown()
                        observer.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        )
        globalLayoutLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(view.width == layout.width && view.height == layout.height)
    }

    @Test
    fun sandboxedSdkViewIsTransitionGroup() {
        activityScenarioRule.withActivity {
            val view = SandboxedSdkView(context)
            assertTrue("SandboxedSdkView isTransitionGroup by default", view.isTransitionGroup)
        }
    }

    @Test
    fun sandboxedSdkViewInflatesTransitionGroup() {
        activityScenarioRule.withActivity {
            val view =
                layoutInflater.inflate(
                    R.layout.sandboxedsdkview_transition_group_false,
                    null,
                    false
                ) as ViewGroup
            assertFalse("XML overrides SandboxedSdkView.isTransitionGroup", view.isTransitionGroup)
        }
    }

    /**
     * Ensures that the input token passed when opening a session is non-null and is the same host
     * token as another [SurfaceView] in the same activity.
     */
    @SuppressLint("NewApi") // Test runs on U+ devices
    @Test
    fun windowInputTokenIsCorrect() {
        // Input token is only needed when provider can be located on a separate process. It is
        // also only needed on U devices, on V+ we will use InputTransferToken
        assumeTrue(Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        assumeTrue(BackwardCompatUtil.canProviderBeRemote())
        lateinit var layout: LinearLayout
        val surfaceView = SurfaceView(context)
        val surfaceViewLatch = CountDownLatch(1)
        var token: IBinder? = null
        surfaceView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View) {
                    @Suppress("DEPRECATION")
                    token = surfaceView.hostToken
                    surfaceViewLatch.countDown()
                }

                override fun onViewDetachedFromWindow(p0: View) {}
            }
        )
        // Attach SurfaceView
        activityScenarioRule.withActivity {
            layout = findViewById(R.id.mainlayout)
            layout.addView(surfaceView)
            layout.removeView(surfaceView)
        }
        // Verify SurfaceView has a non-null token when attached.
        assertThat(surfaceViewLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(token).isNotNull()
        // Verify that the UI adapter receives the same host token object when opening a session.
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        assertThat(testSandboxedUiAdapter.sessionConstants?.windowInputToken).isEqualTo(token)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun inputTransferTokenIsCorrect() {
        // InputTransferToken is only sent on V+
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        addViewToLayoutAndWaitToBeActive()
        val inputTransferToken = view.rootSurfaceControl?.inputTransferToken
        assertThat(testSandboxedUiAdapter.sessionConstants?.inputTransferToken).isNotNull()
        assertThat(testSandboxedUiAdapter.sessionConstants?.inputTransferToken)
            .isEqualTo(inputTransferToken)
    }

    @Ignore("b/307829956")
    @Test
    fun requestResizeWithMeasureSpecAtMost_withinParentBounds() {
        view.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addViewToLayoutAndWaitToBeActive()
        requestResizeAndVerifyLayout(
            /* requestedWidth=*/ mainLayoutWidth - 100,
            /* requestedHeight=*/ mainLayoutHeight - 100,
            /* expectedWidth=*/ mainLayoutWidth - 100,
            /* expectedHeight=*/ mainLayoutHeight - 100
        )
    }

    @Test
    fun requestResizeWithMeasureSpecAtMost_exceedsParentBounds() {
        view.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addViewToLayoutAndWaitToBeActive()
        // the resize is constrained by the parent's size
        requestResizeAndVerifyLayout(
            /* requestedWidth=*/ mainLayoutWidth + 100,
            /* requestedHeight=*/ mainLayoutHeight + 100,
            /* expectedWidth=*/ mainLayoutWidth,
            /* expectedHeight=*/ mainLayoutHeight
        )
    }

    @Test
    fun requestResizeWithMeasureSpecExactly() {
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addViewToLayoutAndWaitToBeActive()
        val currentWidth = view.width
        val currentHeight = view.height
        // the request is a no-op when the MeasureSpec is EXACTLY
        requestResizeAndVerifyLayout(
            /* requestedWidth=*/ currentWidth - 100,
            /* requestedHeight=*/ currentHeight - 100,
            /* expectedWidth=*/ currentWidth,
            /* expectedHeight=*/ currentHeight
        )
    }

    @Ignore // b/356742276
    @Test
    fun signalsOnlyCollectedWhenSignalOptionsNonEmpty() {
        addViewToLayoutAndWaitToBeActive()
        assertThat(view.signalMeasurer).isNotNull()
        val adapter = TestSandboxedUiAdapter(setOf())
        val view2 = SandboxedSdkView(context)
        activityScenarioRule.withActivity { view2.setAdapter(adapter) }
        addViewToLayoutAndWaitToBeActive(view2)
        assertThat(view2.signalMeasurer).isNull()
    }

    @Test
    fun signalsNotSentWhenViewUnchanged() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        session.runAndRetrieveNextUiChange {}
        session.assertNoSubsequentUiChanges()
    }

    @Test
    fun signalsSentWhenSizeChanges() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        val newWidth = 500
        val newHeight = 500
        session.runAndRetrieveNextUiChange {
            activityScenarioRule.withActivity {
                view.layoutParams = LinearLayout.LayoutParams(newWidth, newHeight)
            }
        }
        assertThat(session.shortestGapBetweenUiChangeEvents)
            .isAtLeast(SHORTEST_TIME_BETWEEN_SIGNALS_MS)
    }

    /**
     * Shifts the view partially off screen and verifies that the reported onScreenGeometry is
     * cropped accordingly.
     */
    @Test
    fun correctSignalsSentForOnScreenGeometryWhenViewOffScreen() {
        val clippedWidth = 400
        val clippedHeight = 500
        activityScenarioRule.withActivity {
            val layoutParams = findViewById<LinearLayout>(R.id.mainlayout).layoutParams
            layoutParams.width = clippedWidth
            layoutParams.height = clippedHeight
        }
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        val initialHeight = view.height
        val initialWidth = view.width
        val xShiftDistance = 200f
        val yShiftDistance = 300f
        val sandboxedSdkViewUiInfo =
            session.runAndRetrieveNextUiChange {
                activityScenarioRule.withActivity {
                    view.y -= yShiftDistance
                    view.x -= xShiftDistance
                }
            }
        assertThat(sandboxedSdkViewUiInfo.uiContainerWidth).isEqualTo(clippedWidth)
        assertThat(sandboxedSdkViewUiInfo.uiContainerHeight).isEqualTo(clippedHeight)
        assertThat(sandboxedSdkViewUiInfo.onScreenGeometry.height().toFloat())
            .isEqualTo(initialHeight - yShiftDistance)
        assertThat(sandboxedSdkViewUiInfo.onScreenGeometry.width().toFloat())
            .isEqualTo(initialWidth - xShiftDistance)
    }

    @Test
    fun signalsSentWhenPositionChanges() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        val newXPosition = 100f
        val sandboxedSdkViewUiInfo =
            session.runAndRetrieveNextUiChange {
                activityScenarioRule.withActivity { view.x = newXPosition }
            }
        val containerWidth = sandboxedSdkViewUiInfo.uiContainerWidth
        val onScreenWidth = sandboxedSdkViewUiInfo.onScreenGeometry.width().toFloat()
        assertThat(containerWidth - newXPosition).isEqualTo(onScreenWidth)
    }

    @Test
    fun signalsSentWhenAlphaChanges() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        // Catch initial UI change so that the subsequent alpha change will be reflected in the
        // next SandboxedSdkViewUiInfo
        session.runAndRetrieveNextUiChange {}
        val newAlpha = 0.5f
        val sandboxedSdkViewUiInfo =
            session.runAndRetrieveNextUiChange {
                activityScenarioRule.withActivity { view.alpha = newAlpha }
            }
        assertThat(sandboxedSdkViewUiInfo.uiContainerOpacityHint).isEqualTo(newAlpha)
    }

    /**
     * Changes the alpha of the view several times in quick succession, and verifies that the
     * signals sent match the alpha of the final change.
     */
    @Test
    fun signalsSentAreFresh() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        var currentAlpha = view.alpha
        val sandboxedSdkViewUiInfo =
            session.runAndRetrieveNextUiChange {
                activityScenarioRule.withActivity {
                    for (i in 1..5) {
                        currentAlpha += 0.2f
                        if (currentAlpha > 1.0f) {
                            currentAlpha = 0.1f
                        }
                        view.alpha = currentAlpha
                    }
                }
            }
        assertThat(sandboxedSdkViewUiInfo.uiContainerOpacityHint).isEqualTo(currentAlpha)
    }

    /**
     * Creates many UI changes and ensures that these changes are not sent more frequently than
     * expected.
     */
    @Test
    @SuppressLint("BanThreadSleep") // Deliberate delay for testing
    fun signalsNotSentMoreFrequentlyThanLimit() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        for (i in 1..10) {
            activityScenarioRule.withActivity {
                view.layoutParams = LinearLayout.LayoutParams(view.width + 10, view.height + 10)
            }
            Thread.sleep(100)
        }
        assertThat(session.shortestGapBetweenUiChangeEvents)
            .isAtLeast(SHORTEST_TIME_BETWEEN_SIGNALS_MS)
    }

    @Test
    fun signalsSentWhenHostActivityStateChanges() {
        addViewToLayoutAndWaitToBeActive()
        val session = testSandboxedUiAdapter.testSession!!
        session.runAndRetrieveNextUiChange {}
        // Replace the first activity with a new activity. The onScreenGeometry should now be empty.
        var sandboxedSdkViewUiInfo =
            session.runAndRetrieveNextUiChange {
                activityScenarioRule.scenario.onActivity {
                    val intent = Intent(it, SecondActivity::class.java)
                    it.startActivity(intent)
                }
            }
        assertThat(sandboxedSdkViewUiInfo.onScreenGeometry.isEmpty).isTrue()
        // Return to the first activity. The onScreenGeometry should now be non-empty.
        sandboxedSdkViewUiInfo = session.runAndRetrieveNextUiChange { uiDevice.pressBack() }
        assertThat(sandboxedSdkViewUiInfo.onScreenGeometry.isEmpty).isFalse()
    }

    @Test
    fun addChildViewToSandboxedSdkView_throwsException() {
        addViewToLayout()
        val exception =
            assertThrows(UnsupportedOperationException::class.java) { view.addView(View(context)) }
        assertThat(exception.message).isEqualTo("Cannot add a view to SandboxedSdkView")
    }

    @Test
    fun removeViewsFromSandboxedSdkView_throwsException() {
        addViewToLayout()
        val removeChildRunnableArray =
            arrayOf(
                Runnable { view.removeView(View(context)) },
                Runnable { view.removeAllViews() },
                Runnable { view.removeViewAt(0) },
                Runnable { view.removeViews(0, 0) },
                Runnable { view.removeViewInLayout(View(context)) },
                Runnable { view.removeAllViewsInLayout() },
                Runnable { view.removeViewsInLayout(0, 0) }
            )
        removeChildRunnableArray.forEach { removeChildRunnable ->
            val exception =
                assertThrows(UnsupportedOperationException::class.java) {
                    removeChildRunnable.run()
                }
            assertThat(exception.message).isEqualTo("Cannot remove a view from SandboxedSdkView")
        }
    }

    private fun addViewToLayout(waitToBeActive: Boolean = false, viewToAdd: View = view) {
        activityScenarioRule.withActivity {
            linearLayout = findViewById(R.id.mainlayout)
            mainLayoutWidth = linearLayout.width
            mainLayoutHeight = linearLayout.height
            linearLayout.addView(viewToAdd)
        }
        if (waitToBeActive) {
            val eventListener = TestEventListener()
            view.setEventListener(eventListener)
            assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    private fun removeAllViewsFromLayout() {
        activityScenarioRule.withActivity {
            val mainLayout: LinearLayout = findViewById(R.id.mainlayout)
            mainLayout.removeAllViews()
        }
    }

    private fun addViewToLayoutAndWaitToBeActive(viewToAdd: View = view) {
        addViewToLayout(true, viewToAdd)
    }

    private fun requestResizeAndVerifyLayout(
        requestedWidth: Int,
        requestedHeight: Int,
        expectedWidth: Int,
        expectedHeight: Int
    ) {
        val layoutLatch = CountDownLatch(1)
        var width = -1
        var height = -1
        view.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            width = right - left
            height = bottom - top
            layoutLatch.countDown()
        }
        activityScenarioRule.withActivity { view.requestResize(requestedWidth, requestedHeight) }
        assertThat(layoutLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(width).isEqualTo(expectedWidth)
        assertThat(height).isEqualTo(expectedHeight)
    }

    companion object {
        const val TIMEOUT = 1000.toLong()
        // Longer timeout used for expensive operations like device rotation.
        const val UI_INTENSIVE_TIMEOUT = 2000.toLong()
        const val SHORTEST_TIME_BETWEEN_SIGNALS_MS = 200
    }
}
