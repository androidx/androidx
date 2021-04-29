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
package androidx.window

import android.app.Activity
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.TestFoldingFeatureUtil.invalidFoldBounds
import androidx.window.TestFoldingFeatureUtil.validFoldBound
import androidx.window.extensions.ExtensionDisplayFeature
import androidx.window.extensions.ExtensionFoldingFeature
import androidx.window.extensions.ExtensionInterface
import androidx.window.extensions.ExtensionInterface.ExtensionCallback
import androidx.window.extensions.ExtensionWindowLayoutInfo
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Tests for [ExtensionCompat] implementation of [ExtensionInterfaceCompat]. This
 * class uses a mocked Extension to verify the behavior of the implementation on any hardware.
 *
 * Because this class extends [ExtensionCompatDeviceTest], it will also run the mocked
 * versions of methods defined in [CompatDeviceTestInterface].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class ExtensionCompatTest : WindowTestBase(), CompatTestInterface {

    private lateinit var extensionCompat: ExtensionCompat
    private lateinit var activity: Activity

    @Before
    public fun setUp() {
        extensionCompat = ExtensionCompat(
            mock(
                ExtensionInterface::class.java
            ),
            ExtensionAdapter()
        )
        activity = mock(Activity::class.java)
        val mWindowBoundsHelper = TestWindowBoundsHelper()
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS)
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper)
    }

    @After
    public fun tearDown() {
        WindowBoundsHelper.setForTesting(null)
    }

    @Test
    override fun testGetWindowLayout() {
        val fakeExtensionImp = FakeExtensionImp()
        val compat = ExtensionCompat(fakeExtensionImp, ExtensionAdapter())
        val mockCallback = mock(
            ExtensionCallbackInterface::class.java
        )
        compat.setExtensionCallback(mockCallback)
        compat.onWindowLayoutChangeListenerAdded(
            mock(
                Activity::class.java
            )
        )
        fakeExtensionImp.triggerValidSignal()
        verify(mockCallback).onWindowLayoutChanged(
            ArgumentMatchers.any(),
            ArgumentMatchers.argThat { windowLayoutInfo: WindowLayoutInfo ->
                windowLayoutInfo.displayFeatures.isNotEmpty()
            }
        )
    }

    @Test
    override fun testSetExtensionCallback() {
        val extensionCallbackCaptor = ArgumentCaptor.forClass(
            ExtensionCallback::class.java
        )

        // Verify that the extension got the callback set
        val callback = mock(
            ExtensionCallbackInterface::class.java
        )
        extensionCompat.setExtensionCallback(callback)
        verify(extensionCompat.mWindowExtension).setExtensionCallback(
            extensionCallbackCaptor.capture()
        )

        // Verify that the callback set for extension propagates the window layout callback when
        // a listener has been registered.
        extensionCompat.onWindowLayoutChangeListenerAdded(activity)
        val bounds = Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.width(), 1)
        val extensionDisplayFeature: ExtensionDisplayFeature = ExtensionFoldingFeature(
            bounds, ExtensionFoldingFeature.TYPE_HINGE,
            ExtensionFoldingFeature.STATE_HALF_OPENED
        )
        val displayFeatures = listOf(extensionDisplayFeature)
        val extensionWindowLayoutInfo = ExtensionWindowLayoutInfo(displayFeatures)
        extensionCallbackCaptor.value.onWindowLayoutChanged(
            activity,
            extensionWindowLayoutInfo
        )
        val windowLayoutInfoCaptor = ArgumentCaptor.forClass(
            WindowLayoutInfo::class.java
        )
        verify(callback)
            .onWindowLayoutChanged(ArgumentMatchers.eq(activity), windowLayoutInfoCaptor.capture())
        val capturedLayout = windowLayoutInfoCaptor.value
        Assert.assertEquals(1, capturedLayout.displayFeatures.size.toLong())
        val capturedDisplayFeature = capturedLayout.displayFeatures[0]
        val foldingFeature = capturedDisplayFeature as FoldingFeature
        Assert.assertNotNull(foldingFeature)
        Assert.assertEquals(bounds, capturedDisplayFeature.bounds)
    }

    override fun testExtensionCallback_filterRemovesInvalidValues() {
        val fakeExtensionImp = FakeExtensionImp()
        val compat = ExtensionCompat(fakeExtensionImp, ExtensionAdapter())
        val mockCallback = mock(
            ExtensionCallbackInterface::class.java
        )
        compat.setExtensionCallback(mockCallback)
        compat.onWindowLayoutChangeListenerAdded(
            mock(
                Activity::class.java
            )
        )
        fakeExtensionImp.triggerMalformedSignal()
        verify(mockCallback).onWindowLayoutChanged(
            ArgumentMatchers.any(),
            ArgumentMatchers.argThat { windowLayoutInfo: WindowLayoutInfo ->
                windowLayoutInfo.displayFeatures.isEmpty()
            }
        )
    }

    @Test
    override fun testOnWindowLayoutChangeListenerAdded() {
        extensionCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(extensionCompat.mWindowExtension).onWindowLayoutChangeListenerAdded(
            ArgumentMatchers.eq(activity)
        )
    }

    @Test
    override fun testOnWindowLayoutChangeListenerRemoved() {
        extensionCompat.onWindowLayoutChangeListenerRemoved(activity)
        verify(extensionCompat.mWindowExtension)
            .onWindowLayoutChangeListenerRemoved(ArgumentMatchers.eq(activity))
    }

    @Test
    public fun testValidateExtensionInterface() {
        Assert.assertTrue(extensionCompat.validateExtensionInterface())
    }

    private class FakeExtensionImp : ExtensionInterface {
        private var callback: ExtensionCallback
        private val activities = mutableListOf<Activity>()
        override fun setExtensionCallback(callback: ExtensionCallback) {
            this.callback = callback
        }

        override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
            activities.add(activity)
        }

        override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {
            activities.remove(activity)
        }

        fun triggerMalformedSignal() {
            triggerSignal(malformedWindowLayoutInfo())
        }

        fun triggerValidSignal() {
            triggerSignal(validWindowLayoutInfo())
        }

        fun triggerSignal(info: ExtensionWindowLayoutInfo?) {
            activities.forEach { activity ->
                callback.onWindowLayoutChanged(activity, info!!)
            }
        }

        private fun malformedWindowLayoutInfo(): ExtensionWindowLayoutInfo {
            val malformedFeatures = mutableListOf<ExtensionDisplayFeature>()
            for (malformedBound in invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(
                    ExtensionFoldingFeature(
                        malformedBound,
                        ExtensionFoldingFeature.TYPE_FOLD,
                        ExtensionFoldingFeature.STATE_FLAT
                    )
                )
            }
            for (malformedBound in invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(
                    ExtensionFoldingFeature(
                        malformedBound,
                        ExtensionFoldingFeature.TYPE_HINGE,
                        ExtensionFoldingFeature.STATE_FLAT
                    )
                )
            }
            return ExtensionWindowLayoutInfo(malformedFeatures)
        }

        private fun validWindowLayoutInfo(): ExtensionWindowLayoutInfo {
            val validFeatures = listOf(
                ExtensionFoldingFeature(
                    validFoldBound(WINDOW_BOUNDS),
                    ExtensionFoldingFeature.TYPE_FOLD, ExtensionFoldingFeature.STATE_FLAT
                )
            )
            return ExtensionWindowLayoutInfo(validFeatures)
        }

        init {
            callback = ExtensionCallback { _, _ -> }
        }
    }

    private companion object {
        private val WINDOW_BOUNDS = Rect(0, 0, 50, 100)
    }
}
