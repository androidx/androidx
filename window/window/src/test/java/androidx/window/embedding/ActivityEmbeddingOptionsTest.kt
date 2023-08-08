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

package androidx.window.embedding

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The unit tests for activity embedding extension functions to [ActivityOptions]
 *
 * @see [ActivityOptions.setLaunchingActivityStack]
 * @see [ActivityOptions.isSetLaunchingActivityStackSupported]
 */
@OptIn(androidx.window.core.ExperimentalWindowApi::class)
class ActivityEmbeddingOptionsTest {

    private lateinit var mockEmbeddingBackend: EmbeddingBackend
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var mockActivityStack: ActivityStack
    private lateinit var mockActivityOptions: ActivityOptions

    @Before
    fun setUp() {
        mockEmbeddingBackend = mock()
        mockContext = mock()
        mockActivity = mock()
        mockActivityOptions = mock()
        mockActivityStack = ActivityStack(listOf(), true, mock())

        whenever(mockActivity.applicationContext).doReturn(mockContext)
        whenever(mockEmbeddingBackend.getActivityStack(mockActivity)).doReturn(mockActivityStack)

        EmbeddingBackend.overrideDecorator(object : EmbeddingBackendDecorator {
            override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                mockEmbeddingBackend
        })

        // ActivityEmbeddingOptions is only supported since level 3
        ExtensionsUtil.setOverrideVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_3)
    }

    @After
    fun tearDown() {
        EmbeddingBackend.reset()
        ExtensionsUtil.resetOverrideVendorApiLevel()
    }

    @Test
    fun testSetLaunchingActivityStack() {
        mockActivityOptions.setLaunchingActivityStack(mockActivity, mockActivityStack)

        verify(mockEmbeddingBackend).setLaunchingActivityStack(
            mockActivityOptions, mockActivityStack.token)
    }

    @Test
    fun testSetLaunchingActivityStack_byActivity() {
        mockActivityOptions.setLaunchingActivityStack(mockActivity)

        verify(mockEmbeddingBackend).setLaunchingActivityStack(
            mockActivityOptions, mockActivityStack.token)
    }

    @Test
    fun testSetLaunchingActivityStack_unsupportedApiLevel() {
        ExtensionsUtil.setOverrideVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        assertThrows(UnsupportedOperationException::class.java) {
            mockActivityOptions.setLaunchingActivityStack(mockActivity, mockActivityStack)
        }
        verify(mockEmbeddingBackend, never()).setLaunchingActivityStack(any(), any())
    }

    @Test
    fun testIsSetLaunchingActivityStackSupported() {
        ExtensionsUtil.setOverrideVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        assertFalse(mockActivityOptions.isSetLaunchingActivityStackSupported())

        ExtensionsUtil.setOverrideVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_3)

        assertTrue(mockActivityOptions.isSetLaunchingActivityStackSupported())
    }
}
