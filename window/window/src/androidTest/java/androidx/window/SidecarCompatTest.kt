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
import android.content.Context
import android.graphics.Rect
import android.os.IBinder
import android.view.View
import android.view.Window
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.TestFoldingFeatureUtil.invalidFoldBounds
import androidx.window.TestFoldingFeatureUtil.validFoldBound
import androidx.window.sidecar.SidecarDeviceState
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarInterface
import androidx.window.sidecar.SidecarInterface.SidecarCallback
import androidx.window.sidecar.SidecarWindowLayoutInfo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [SidecarCompat] implementation of [ExtensionInterfaceCompat]. This class
 * uses a mocked Sidecar to verify the behavior of the implementation on any hardware.
 *
 * Because this class extends [SidecarCompatDeviceTest], it will also run the mocked
 * versions of methods defined in [CompatDeviceTestInterface].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SidecarCompatTest : WindowTestBase(), CompatTestInterface {

    private lateinit var activity: Activity
    private lateinit var sidecarCompat: SidecarCompat

    @Before
    public fun setUp() {
        activity = mock()
        sidecarCompat = SidecarCompat(mock(), SidecarAdapter())
        whenever(activity.getResources())
            .thenReturn(ApplicationProvider.getApplicationContext<Context>().resources)
        val window: Window = spy(TestWindow(activity))
        window.attributes.token = mock()
        whenever(activity.getWindow()).thenReturn(window)
        val mWindowBoundsHelper = TestWindowBoundsHelper()
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS)
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper)

        // Setup mocked sidecar responses
        val defaultDeviceState = SidecarDeviceState()
        SidecarAdapter.setSidecarDevicePosture(
            defaultDeviceState,
            SidecarDeviceState.POSTURE_HALF_OPENED
        )
        whenever(sidecarCompat.sidecar!!.deviceState).thenReturn(defaultDeviceState)
        val sidecarDisplayFeature = newDisplayFeature(
            Rect(0, 1, WINDOW_BOUNDS.width(), 1), SidecarDisplayFeature.TYPE_HINGE
        )
        val sidecarWindowLayoutInfo = SidecarWindowLayoutInfo()
        val displayFeatures = listOf(sidecarDisplayFeature)
        SidecarAdapter.setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures)
        whenever(sidecarCompat.sidecar!!.getWindowLayoutInfo(any()))
            .thenReturn(sidecarWindowLayoutInfo)
    }

    @After
    public fun tearDown() {
        WindowBoundsHelper.setForTesting(null)
    }

    @Test
    override fun testGetWindowLayout() {
        val fakeSidecarImp = FakeExtensionImp(
            newDeviceState(SidecarDeviceState.POSTURE_OPENED),
            newWindowLayoutInfo(emptyList())
        )
        val compat = SidecarCompat(fakeSidecarImp, SidecarAdapter())
        val mockCallback = mock<ExtensionCallbackInterface>()
        compat.setExtensionCallback(mockCallback)
        compat.onWindowLayoutChangeListenerAdded(activity)
        fakeSidecarImp.triggerGoodSignal()
        verify(mockCallback, atLeastOnce()).onWindowLayoutChanged(eq(activity), any())
    }

    @Test
    public fun testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        val originalWindowLayoutInfo = sidecarCompat.sidecar!!.getWindowLayoutInfo(
            getActivityWindowToken(
                activity
            )
        )
        val sidecarDisplayFeatures =
            SidecarAdapter.getSidecarDisplayFeatures(originalWindowLayoutInfo)
        val newFeature = SidecarDisplayFeature()
        newFeature.rect = Rect()
        SidecarAdapter.setSidecarDisplayFeatures(
            originalWindowLayoutInfo,
            sidecarDisplayFeatures + newFeature
        )

        // Verify that this feature is skipped.
        val windowLayoutInfo = sidecarCompat.getWindowLayoutInfo(activity)
        assertEquals(
            (sidecarDisplayFeatures.size).toLong(),
            windowLayoutInfo.displayFeatures.size.toLong()
        )
    }

    @Test
    public fun testGetWindowLayout_foldWithNonZeroArea() {
        val originalWindowLayoutInfo = sidecarCompat.sidecar!!.getWindowLayoutInfo(mock())
        val sidecarDisplayFeatures =
            SidecarAdapter.getSidecarDisplayFeatures(originalWindowLayoutInfo)
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
        val originalWindowLayoutInfo = sidecarCompat.sidecar!!.getWindowLayoutInfo(mock())
        val sidecarDisplayFeatures =
            SidecarAdapter.getSidecarDisplayFeatures(originalWindowLayoutInfo)
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
        val originalWindowLayoutInfo = sidecarCompat.sidecar!!.getWindowLayoutInfo(mock())
        val sidecarDisplayFeatures =
            SidecarAdapter.getSidecarDisplayFeatures(originalWindowLayoutInfo)

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

    override fun testExtensionCallback_filterRemovesInvalidValues() {
        val fakeSidecarImp = FakeExtensionImp(
            newDeviceState(SidecarDeviceState.POSTURE_OPENED),
            newWindowLayoutInfo(emptyList())
        )
        val compat = SidecarCompat(fakeSidecarImp, SidecarAdapter())
        val mockCallback = mock<ExtensionCallbackInterface>()
        compat.setExtensionCallback(mockCallback)
        compat.onWindowLayoutChangeListenerAdded(activity)
        fakeSidecarImp.triggerMalformedSignal()
        verify(mockCallback).onWindowLayoutChanged(
            any(),
            argThat { windowLayoutInfo -> windowLayoutInfo.displayFeatures.isEmpty() }
        )
    }

    @Test
    override fun testSetExtensionCallback() {
        val sidecarCallbackCaptor = argumentCaptor<SidecarCallback>()

        // Verify that the sidecar got the callback set
        val callback = mock<ExtensionCallbackInterface>()
        sidecarCompat.setExtensionCallback(callback)
        verify(sidecarCompat.sidecar!!)
            .setSidecarCallback(sidecarCallbackCaptor.capture())

        // Verify that the callback set for sidecar propagates the device state callback
        val sidecarDeviceState = SidecarDeviceState()
        SidecarAdapter.setSidecarDevicePosture(
            sidecarDeviceState,
            SidecarDeviceState.POSTURE_HALF_OPENED
        )
        sidecarCallbackCaptor.firstValue.onDeviceStateChanged(sidecarDeviceState)

        // Verify that the callback set for sidecar propagates the window layout callback when a
        // window layout changed listener has been added.
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        val bounds = Rect(0, 1, WINDOW_BOUNDS.width(), 1)
        val sidecarDisplayFeature = newDisplayFeature(
            bounds,
            SidecarDisplayFeature.TYPE_HINGE
        )
        val sidecarWindowLayoutInfo = SidecarWindowLayoutInfo()
        val displayFeatures = listOf(sidecarDisplayFeature)
        SidecarAdapter.setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures)
        sidecarCallbackCaptor.firstValue.onWindowLayoutChanged(
            getActivityWindowToken(
                activity
            ),
            sidecarWindowLayoutInfo
        )
        val windowLayoutInfoCaptor = argumentCaptor<WindowLayoutInfo>()
        verify(callback).onWindowLayoutChanged(eq(activity), windowLayoutInfoCaptor.capture())
        val capturedLayout = windowLayoutInfoCaptor.firstValue
        assertEquals(1, capturedLayout.displayFeatures.size.toLong())
        val capturedDisplayFeature = capturedLayout.displayFeatures[0]
        val foldingFeature = capturedDisplayFeature as FoldingFeature
        assertNotNull(foldingFeature)
        assertEquals(bounds, capturedDisplayFeature.bounds)
    }

    @Test
    public fun testMissingCallToOnWindowLayoutChangedListenerAdded() {
        val sidecarCallbackCaptor = argumentCaptor<SidecarCallback>()
        // Verify that the sidecar got the callback set
        val callback = mock<ExtensionCallbackInterface>()
        sidecarCompat.setExtensionCallback(callback)
        verify(sidecarCompat.sidecar!!)
            .setSidecarCallback(sidecarCallbackCaptor.capture())

        // Verify that the callback set for sidecar propagates the window layout callback when a
        // window layout changed listener has been added.
        val sidecarDisplayFeature = SidecarDisplayFeature()
        sidecarDisplayFeature.type = SidecarDisplayFeature.TYPE_HINGE
        val bounds = Rect(1, 2, 3, 4)
        sidecarDisplayFeature.rect = bounds
        val sidecarWindowLayoutInfo = SidecarWindowLayoutInfo()
        val displayFeatures = listOf(sidecarDisplayFeature)
        SidecarAdapter.setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures)
        val windowToken = mock<IBinder>()
        sidecarCallbackCaptor.firstValue.onWindowLayoutChanged(
            windowToken,
            sidecarWindowLayoutInfo
        )
        verifyZeroInteractions(callback)
    }

    @Test
    override fun testOnWindowLayoutChangeListenerAdded() {
        val windowToken = getActivityWindowToken(
            activity
        )
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(sidecarCompat.sidecar!!).onWindowLayoutChangeListenerAdded(windowToken)
        verify(sidecarCompat.sidecar!!).onDeviceStateListenersChanged(false)
    }

    @Test
    public fun testOnWindowLayoutChangeListenerAdded_emitInitialValue() {
        val layoutInfo = SidecarWindowLayoutInfo()
        val expectedLayoutInfo = WindowLayoutInfo(listOf())
        val listener = mock<ExtensionCallbackInterface>()
        sidecarCompat.setExtensionCallback(listener)
        whenever(sidecarCompat.sidecar!!.getWindowLayoutInfo(any()))
            .thenReturn(layoutInfo)
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(listener).onWindowLayoutChanged(
            activity, expectedLayoutInfo
        )
    }

    @Test
    public fun testOnWindowLayoutChangeListenerAdded_emitInitialValueDelayed() {
        val layoutInfo = SidecarWindowLayoutInfo()
        val expectedLayoutInfo = WindowLayoutInfo(listOf())
        val listener = mock<ExtensionCallbackInterface>()
        sidecarCompat.setExtensionCallback(listener)
        whenever(sidecarCompat.sidecar!!.getWindowLayoutInfo(any()))
            .thenReturn(layoutInfo)
        val fakeView = mock<View>()
        val fakeWindow: Window = TestWindow(activity, fakeView)
        doAnswer { invocation ->
            val stateChangeListener = invocation.getArgument<View.OnAttachStateChangeListener>(0)
            fakeWindow.attributes.token = mock()
            stateChangeListener.onViewAttachedToWindow(fakeView)
            null
        }.whenever(fakeView).addOnAttachStateChangeListener(any())
        whenever(activity.window).thenReturn(fakeWindow)
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(listener).onWindowLayoutChanged(
            activity, expectedLayoutInfo
        )
    }

    @Test
    override fun testOnWindowLayoutChangeListenerRemoved() {
        val windowToken = getActivityWindowToken(
            activity
        )
        sidecarCompat.onWindowLayoutChangeListenerRemoved(activity)
        verify(sidecarCompat.sidecar!!).onWindowLayoutChangeListenerRemoved(
            eq(windowToken)
        )
    }

    @Test
    public fun testOnDeviceStateListenersChanged() {
        sidecarCompat.onWindowLayoutChangeListenerAdded(activity)
        sidecarCompat.onWindowLayoutChangeListenerRemoved(activity)
        verify(sidecarCompat.sidecar!!).onDeviceStateListenersChanged(true)
    }

    @Test
    public fun testOnDeviceStateChangedUpdatesWindowLayout() {
        val fakeSidecarImp = FakeExtensionImp(
            newDeviceState(SidecarDeviceState.POSTURE_CLOSED),
            validWindowLayoutInfo()
        )
        val compat = SidecarCompat(fakeSidecarImp, SidecarAdapter())
        val mockCallback = mock<ExtensionCallbackInterface>()
        compat.setExtensionCallback(mockCallback)
        compat.onWindowLayoutChangeListenerAdded(activity)
        val windowLayoutCaptor = argumentCaptor<WindowLayoutInfo>()
        reset(mockCallback)
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_OPENED))
        verify(mockCallback).onWindowLayoutChanged(eq(activity), windowLayoutCaptor.capture())
        var capturedFoldingFeature = windowLayoutCaptor.firstValue
            .displayFeatures[0] as FoldingFeature
        assertEquals(FLAT, capturedFoldingFeature.state)
        reset(mockCallback)
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_HALF_OPENED))
        verify(mockCallback)
            .onWindowLayoutChanged(eq(activity), windowLayoutCaptor.capture())
        capturedFoldingFeature = windowLayoutCaptor.secondValue.displayFeatures[0] as FoldingFeature
        assertEquals(HALF_OPENED, capturedFoldingFeature.state)

        // No display features must be reported in closed state or flipped state.
        reset(mockCallback)
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_CLOSED))
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_FLIPPED))
        verify(mockCallback)
            .onWindowLayoutChanged(eq(activity), windowLayoutCaptor.capture())
        assertTrue(windowLayoutCaptor.thirdValue.displayFeatures.isEmpty())
    }

    private class FakeExtensionImp(
        private var deviceState: SidecarDeviceState,
        private var info: SidecarWindowLayoutInfo
    ) : SidecarInterface {
        private var callback: SidecarCallback
        private val tokens = mutableListOf<IBinder>()
        override fun setSidecarCallback(callback: SidecarCallback) {
            this.callback = callback
        }

        override fun getWindowLayoutInfo(windowToken: IBinder): SidecarWindowLayoutInfo {
            return info
        }

        override fun onWindowLayoutChangeListenerAdded(windowToken: IBinder) {
            tokens.add(windowToken)
        }

        override fun onWindowLayoutChangeListenerRemoved(windowToken: IBinder) {
            tokens.remove(windowToken)
        }

        override fun getDeviceState(): SidecarDeviceState {
            return deviceState
        }

        override fun onDeviceStateListenersChanged(isEmpty: Boolean) {}
        fun triggerMalformedSignal() {
            triggerSignal(malformedWindowLayoutInfo())
        }

        fun triggerGoodSignal() {
            triggerSignal(validWindowLayoutInfo())
        }

        fun triggerSignal(info: SidecarWindowLayoutInfo) {
            this.info = info
            for (token in tokens) {
                callback.onWindowLayoutChanged(token, info)
            }
        }

        fun triggerDeviceState(state: SidecarDeviceState) {
            deviceState = state
            callback.onDeviceStateChanged(state)
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

        init {
            callback = object : SidecarCallback {
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
            SidecarAdapter.setSidecarDisplayFeatures(info, features)
            return info
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

        private fun newDeviceState(posture: Int): SidecarDeviceState {
            val state = SidecarDeviceState()
            SidecarAdapter.setSidecarDevicePosture(state, posture)
            return state
        }
    }
}
