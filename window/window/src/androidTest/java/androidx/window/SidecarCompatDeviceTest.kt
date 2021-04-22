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

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.sidecar.SidecarWindowLayoutInfo
import org.junit.Assert.assertNotNull
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Tests for [SidecarCompat] implementation of [ExtensionInterfaceCompat] that are
 * executed with Sidecar implementation provided on the device (and only if one is available).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SidecarCompatDeviceTest : WindowTestBase(), CompatDeviceTestInterface {

    private lateinit var sidecarCompat: SidecarCompat
    private lateinit var testActivity: TestActivity

    @Before
    public fun setUp() {
        assumeExtensionV01()
        sidecarCompat = SidecarCompat(ApplicationProvider.getApplicationContext() as Context)
        testActivity = activityTestRule.launchActivity(Intent())
    }

    @Test
    override fun testWindowLayoutCallback() {
        val windowToken = getActivityWindowToken(testActivity)
        assertNotNull(windowToken)
        val callbackInterface = mock(ExtensionCallbackInterface::class.java)
        sidecarCompat.setExtensionCallback(callbackInterface)
        sidecarCompat.onWindowLayoutChangeListenerAdded(testActivity)
        val sidecarWindowLayoutInfo = sidecarCompat.mSidecar.getWindowLayoutInfo(windowToken)
        verify(callbackInterface, atLeastOnce()).onWindowLayoutChanged(
            any(),
            argThat(SidecarMatcher(sidecarWindowLayoutInfo))
        )
    }

    private fun assumeExtensionV01() {
        val sidecarVersion = SidecarCompat.getSidecarVersion()
        Assume.assumeTrue(Version.VERSION_0_1 == sidecarVersion)
    }

    private class SidecarMatcher(
        private val sidecarWindowLayoutInfo: SidecarWindowLayoutInfo
    ) : ArgumentMatcher<WindowLayoutInfo> {
        override fun matches(windowLayoutInfo: WindowLayoutInfo): Boolean {
            val sidecarDisplayFeatures =
                SidecarAdapter.getSidecarDisplayFeatures(sidecarWindowLayoutInfo)
            if (windowLayoutInfo.displayFeatures.size != sidecarDisplayFeatures!!.size) {
                return false
            }
            for (i in windowLayoutInfo.displayFeatures.indices) {
                // Sidecar only has folding features
                val feature = windowLayoutInfo.displayFeatures[i] as FoldingFeature
                val sidecarDisplayFeature = sidecarDisplayFeatures[i]
                if (feature.type != sidecarDisplayFeature.type) {
                    return false
                }
                if (feature.bounds != sidecarDisplayFeature.rect) {
                    return false
                }
            }
            return true
        }
    }
}
