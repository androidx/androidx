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

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import org.junit.Assert.assertNotNull
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Tests for [ExtensionCompat] implementation of [ExtensionInterfaceCompat] that are
 * executed with Extension implementation provided on the device (and only if one is available).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class ExtensionCompatDeviceTest : WindowTestBase(), CompatDeviceTestInterface {

    private lateinit var extensionCompat: ExtensionCompat

    @Before
    public fun setUp() {
        assumeExtensionV1_0()
        extensionCompat = ExtensionCompat(ApplicationProvider.getApplicationContext() as Context)
    }

    @Test
    override fun testWindowLayoutCallback() {
        val activity = activityTestRule.launchActivity(Intent())
        val windowToken = getActivityWindowToken(activity)
        assertNotNull(windowToken)
        val callbackInterface = mock(
            ExtensionCallbackInterface::class.java
        )
        extensionCompat.setExtensionCallback(callbackInterface)
        extensionCompat.onWindowLayoutChangeListenerAdded(activity)
        verify(callbackInterface)
            .onWindowLayoutChanged(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    private fun assumeExtensionV1_0() {
        val extensionVersion = ExtensionCompat.getExtensionVersion()
        Assume.assumeTrue(
            extensionVersion != null && Version.VERSION_1_0 <= extensionVersion
        )
    }
}
