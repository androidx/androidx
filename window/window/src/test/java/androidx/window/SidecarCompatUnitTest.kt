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
// Sidecar is deprecated but consuming code must be maintained for compatibility reasons
@file:Suppress("DEPRECATION")

package androidx.window

import android.app.Activity
import android.graphics.Rect
import android.os.IBinder
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.TestFoldingFeatureUtil.invalidFoldBounds
import androidx.window.TestFoldingFeatureUtil.validFoldBound
import androidx.window.sidecar.SidecarDeviceState
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarInterface
import androidx.window.sidecar.SidecarInterface.SidecarCallback
import androidx.window.sidecar.SidecarWindowLayoutInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock

/**
 * Unit tests for [SidecarCompat] that run on the JVM.
 */
public class SidecarCompatUnitTest {

    private lateinit var activity: Activity
    private lateinit var sidecarCompat: SidecarCompat

    @Before
    public fun setUp() {
        val windowBoundsHelper = TestWindowBoundsHelper()
        windowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS)
        WindowBoundsHelper.setForTesting(windowBoundsHelper)
        activity = mock(Activity::class.java)
        val window: Window = spy(TestWindow(activity))
        val params = WindowManager.LayoutParams()
        params.token = mock(IBinder::class.java)
        doReturn(params).`when`(window).attributes
        `when`(activity.getWindow()).thenReturn(window)
        val mockSidecarInterface = mock(
            SidecarInterface::class.java
        )
        `when`(mockSidecarInterface.deviceState).thenReturn(
            newDeviceState(SidecarDeviceState.POSTURE_FLIPPED)
        )
        `when`(mockSidecarInterface.getWindowLayoutInfo(any())).thenReturn(
            newWindowLayoutInfo(emptyList())
        )
        sidecarCompat = SidecarCompat(mockSidecarInterface, SidecarAdapter())
    }

    @After
    public fun tearDown() {
        WindowBoundsHelper.setForTesting(null)
    }

    @Test
    public fun testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        val originalWindowLayoutInfo = sidecarCompat.mSidecar.getWindowLayoutInfo(
            getActivityWindowToken(activity)!!
        )
        val sidecarDisplayFeatures = originalWindowLayoutInfo.displayFeatures ?: emptyList()
        val newFeature = SidecarDisplayFeature()
        newFeature.rect = Rect()
        val replacementFeatures = sidecarDisplayFeatures + newFeature
        SidecarAdapter.setSidecarDisplayFeatures(originalWindowLayoutInfo, replacementFeatures)

        // Verify that this feature is skipped.
        val windowLayoutInfo = sidecarCompat.getWindowLayoutInfo(activity)
        assertEquals(
            (sidecarDisplayFeatures.size).toLong(),
            windowLayoutInfo.displayFeatures.size.toLong()
        )
    }

    @Test
    public fun testGetWindowLayout_foldWithNonZeroArea() {
        val originalWindowLayoutInfo = sidecarCompat.mSidecar.getWindowLayoutInfo(
            mock(
                IBinder::class.java
            )
        )
        val sidecarDisplayFeatures = originalWindowLayoutInfo.displayFeatures ?: emptyList()
        val additionalFeatures = listOf(
            // Horizontal fold.
            newDisplayFeature(
                Rect(0, 1, WINDOW_BOUNDS.width(), 2),
                SidecarDisplayFeature.TYPE_FOLD
            ),
            // Vertical fold.
            newDisplayFeature(
                Rect(1, 0, 2, WINDOW_BOUNDS.height()),
                SidecarDisplayFeature.TYPE_FOLD
            )
        )
        val replacementFeatures = sidecarDisplayFeatures + additionalFeatures
        SidecarAdapter.setSidecarDisplayFeatures(originalWindowLayoutInfo, replacementFeatures)

        // Verify that these features are skipped.
        val windowLayoutInfo = sidecarCompat.getWindowLayoutInfo(activity)
        assertEquals(
            (sidecarDisplayFeatures.size).toLong(),
            windowLayoutInfo.displayFeatures.size.toLong()
        )
    }

    @Test
    public fun testGetWindowLayout_hingeNotSpanningEntireWindow() {
        val originalWindowLayoutInfo = sidecarCompat.mSidecar.getWindowLayoutInfo(
            mock(
                IBinder::class.java
            )
        )
        val sidecarDisplayFeatures = originalWindowLayoutInfo.displayFeatures ?: emptyList()
        val additionalFeatures = listOf(
            // Horizontal hinge.
            newDisplayFeature(
                Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                SidecarDisplayFeature.TYPE_FOLD
            ),
            // Vertical hinge.
            newDisplayFeature(
                Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                SidecarDisplayFeature.TYPE_FOLD
            )
        )
        val replacementFeatures = sidecarDisplayFeatures + additionalFeatures
        SidecarAdapter.setSidecarDisplayFeatures(originalWindowLayoutInfo, replacementFeatures)

        // Verify that these features are skipped.
        val windowLayoutInfo = sidecarCompat.getWindowLayoutInfo(activity)
        assertEquals(
            (sidecarDisplayFeatures.size).toLong(),
            windowLayoutInfo.displayFeatures.size.toLong()
        )
    }

    @Test
    public fun testGetWindowLayout_foldNotSpanningEntireWindow() {
        val originalWindowLayoutInfo = sidecarCompat.mSidecar.getWindowLayoutInfo(
            mock(
                IBinder::class.java
            )
        )
        val sidecarDisplayFeatures = originalWindowLayoutInfo.displayFeatures ?: emptyList()
        val additionalFeatures = listOf(
            // Horizontal fold.
            newDisplayFeature(
                Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                SidecarDisplayFeature.TYPE_FOLD
            ),
            // Vertical fold.
            newDisplayFeature(
                Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                SidecarDisplayFeature.TYPE_FOLD
            )
        )
        val replacementFeatures = sidecarDisplayFeatures + additionalFeatures
        SidecarAdapter.setSidecarDisplayFeatures(originalWindowLayoutInfo, replacementFeatures)

        // Verify that these features are skipped.
        val windowLayoutInfo = sidecarCompat.getWindowLayoutInfo(activity)
        assertEquals(
            (sidecarDisplayFeatures.size).toLong(),
            windowLayoutInfo.displayFeatures.size.toLong()
        )
    }

    @Test
    public fun testOnWindowLayoutChangeListenerAdded() {
        val expectedToken = mock(IBinder::class.java)
        activity.window.attributes.token = expectedToken
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(sidecarCompat.mSidecar).onWindowLayoutChangeListenerAdded(expectedToken)
        verify(sidecarCompat.mSidecar).onDeviceStateListenersChanged(false)
    }

    @Test
    public fun testOnWindowLayoutChangeListenerAdded_emitInitialValueDelayed() {
        val layoutInfo = SidecarWindowLayoutInfo()
        val expectedLayoutInfo = WindowLayoutInfo(emptyList())
        val listener = mock(
            ExtensionCallbackInterface::class.java
        )
        sidecarCompat.setExtensionCallback(listener)
        `when`(sidecarCompat.mSidecar.getWindowLayoutInfo(any()))
            .thenReturn(layoutInfo)
        val fakeView = mock(View::class.java)
        val mockWindow = mock(
            Window::class.java
        )
        `when`(mockWindow.attributes).thenReturn(WindowManager.LayoutParams())
        doAnswer { invocation: InvocationOnMock ->
            val stateChangeListener = invocation.getArgument<View.OnAttachStateChangeListener>(0)
            mockWindow.attributes.token = mock(IBinder::class.java)
            stateChangeListener.onViewAttachedToWindow(fakeView)
            null
        }.`when`(fakeView).addOnAttachStateChangeListener(ArgumentMatchers.any())
        `when`(mockWindow.decorView).thenReturn(fakeView)
        `when`(activity.window).thenReturn(mockWindow)
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(listener).onWindowLayoutChanged(
            activity, expectedLayoutInfo
        )
        verify(sidecarCompat.mSidecar).onWindowLayoutChangeListenerAdded(
            getActivityWindowToken(activity)!!
        )
        verify(fakeView).addOnAttachStateChangeListener(ArgumentMatchers.any())
    }

    @Test
    public fun testOnWindowLayoutChangeListenerRemoved() {
        val expectedToken = mock(IBinder::class.java)
        activity.window.attributes.token = expectedToken
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        sidecarCompat.onWindowLayoutChangeListenerRemoved(activity)
        verify(sidecarCompat.mSidecar).onWindowLayoutChangeListenerRemoved(expectedToken)
        verify(sidecarCompat.mSidecar).onDeviceStateListenersChanged(true)
    }

    @Test
    public fun testExtensionCallback_deduplicateValues() {
        val callback = mock(
            ExtensionCallbackInterface::class.java
        )
        val fakeExtensionImp = FakeExtensionImp()
        val compat = SidecarCompat(fakeExtensionImp, SidecarAdapter())
        compat.setExtensionCallback(callback)
        activity.window.attributes.token = mock(IBinder::class.java)
        compat.onWindowLayoutChangeListenerAdded(activity)
        fakeExtensionImp.triggerDeviceState(fakeExtensionImp.deviceState)
        fakeExtensionImp.triggerDeviceState(fakeExtensionImp.deviceState)
        verify(callback, times(1))
            .onWindowLayoutChanged(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    private class FakeExtensionImp() : SidecarInterface {
        private var mCallback: SidecarCallback
        private val mTokens: List<IBinder> = ArrayList()
        override fun setSidecarCallback(callback: SidecarCallback) {
            mCallback = callback
        }

        override fun getWindowLayoutInfo(windowToken: IBinder): SidecarWindowLayoutInfo {
            return SidecarWindowLayoutInfo()
        }

        override fun onWindowLayoutChangeListenerAdded(windowToken: IBinder) {}
        override fun onWindowLayoutChangeListenerRemoved(windowToken: IBinder) {}
        override fun getDeviceState(): SidecarDeviceState {
            return SidecarDeviceState()
        }

        override fun onDeviceStateListenersChanged(isEmpty: Boolean) {}
        fun triggerMalformedSignal() {
            triggerSignal(malformedWindowLayoutInfo())
        }

        fun triggerGoodSignal() {
            triggerSignal(validWindowLayoutInfo())
        }

        fun triggerSignal(info: SidecarWindowLayoutInfo?) {
            for (token in mTokens) {
                triggerSignal(token, info)
            }
        }

        fun triggerSignal(token: IBinder?, info: SidecarWindowLayoutInfo?) {
            mCallback.onWindowLayoutChanged(token!!, info!!)
        }

        fun triggerDeviceState(state: SidecarDeviceState?) {
            mCallback.onDeviceStateChanged(state!!)
        }

        private fun malformedWindowLayoutInfo(): SidecarWindowLayoutInfo {
            val malformedFeatures = mutableListOf<SidecarDisplayFeature>()
            for (malformedBound in invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(
                    newDisplayFeature(
                        malformedBound,
                        SidecarDisplayFeature.TYPE_FOLD
                    )
                )
            }
            for (malformedBound in invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(
                    newDisplayFeature(
                        malformedBound,
                        SidecarDisplayFeature.TYPE_HINGE
                    )
                )
            }
            return newWindowLayoutInfo(malformedFeatures)
        }

        private fun validWindowLayoutInfo(): SidecarWindowLayoutInfo {
            val goodFeatures = listOf(
                newDisplayFeature(
                    validFoldBound(WINDOW_BOUNDS),
                    SidecarDisplayFeature.TYPE_FOLD
                )
            )
            return newWindowLayoutInfo(goodFeatures)
        }

        init {
            mCallback = object : SidecarCallback {
                override fun onDeviceStateChanged(newDeviceState: SidecarDeviceState) {}
                override fun onWindowLayoutChanged(
                    windowToken: IBinder,
                    newLayout: SidecarWindowLayoutInfo
                ) {
                }
            }
        }
    }

    private companion object {
        private val WINDOW_BOUNDS = Rect(1, 1, 50, 100)
        private fun newDisplayFeature(rect: Rect, type: Int): SidecarDisplayFeature {
            val feature = SidecarDisplayFeature()
            feature.rect = rect
            feature.type = type
            return feature
        }

        private fun newWindowLayoutInfo(
            features: List<SidecarDisplayFeature>
        ): SidecarWindowLayoutInfo {
            val info = SidecarWindowLayoutInfo()
            info.displayFeatures = features
            return info
        }

        private fun newDeviceState(posture: Int): SidecarDeviceState {
            val state = SidecarDeviceState()
            state.posture = posture
            return state
        }
    }
}
