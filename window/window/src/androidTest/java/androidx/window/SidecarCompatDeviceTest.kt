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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.FoldingFeature.Type.Companion.HINGE
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarWindowLayoutInfo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher

/**
 * Tests for [SidecarCompat] implementation of [ExtensionInterfaceCompat] that are
 * executed with Sidecar implementation provided on the device (and only if one is available).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SidecarCompatDeviceTest : WindowTestBase(), CompatDeviceTestInterface {

    private lateinit var sidecarCompat: SidecarCompat

    @Before
    public fun setUp() {
        assumeExtensionV01()
        sidecarCompat = SidecarCompat(ApplicationProvider.getApplicationContext() as Context)
    }

    @Test
    override fun testWindowLayoutCallback() {
        activityTestRule.scenario.onActivity { testActivity ->
            val windowToken = getActivityWindowToken(testActivity)
            assertNotNull(windowToken)
            val callbackInterface = mock<ExtensionCallbackInterface>()
            sidecarCompat.setExtensionCallback(callbackInterface)
            sidecarCompat.onWindowLayoutChangeListenerAdded(testActivity)
            val sidecarWindowLayoutInfo = sidecarCompat.sidecar!!.getWindowLayoutInfo(windowToken)
            verify(callbackInterface, atLeastOnce()).onWindowLayoutChanged(
                any(),
                argThat(SidecarMatcher(sidecarWindowLayoutInfo))
            )
        }
    }

    private fun assumeExtensionV01() {
        val sidecarVersion = SidecarCompat.sidecarVersion
        assumeTrue(Version.VERSION_0_1 == sidecarVersion)
    }

    private class SidecarMatcher(
        private val sidecarWindowLayoutInfo: SidecarWindowLayoutInfo
    ) : ArgumentMatcher<WindowLayoutInfo> {
        override fun matches(windowLayoutInfo: WindowLayoutInfo): Boolean {
            val sidecarDisplayFeatures =
                SidecarAdapter.getSidecarDisplayFeatures(sidecarWindowLayoutInfo)
            if (windowLayoutInfo.displayFeatures.size != sidecarDisplayFeatures.size) {
                return false
            }
            for (i in windowLayoutInfo.displayFeatures.indices) {
                // Sidecar only has folding features
                val feature = windowLayoutInfo.displayFeatures[i] as FoldingFeature
                val sidecarDisplayFeature = sidecarDisplayFeatures[i]
                if (!hasMatchingType(feature.type, sidecarDisplayFeature.type)) {
                    return false
                }
                if (feature.bounds != sidecarDisplayFeature.rect) {
                    return false
                }
            }
            return true
        }

        private fun hasMatchingType(featureType: FoldingFeature.Type, sidecarType: Int): Boolean {
            return when (featureType) {
                FOLD -> sidecarType == SidecarDisplayFeature.TYPE_FOLD
                HINGE -> sidecarType == SidecarDisplayFeature.TYPE_HINGE
                else -> false
            }
        }
    }
}
