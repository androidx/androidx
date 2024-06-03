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
import android.content.Context
import android.os.Bundle
import androidx.window.extensions.embedding.ActivityStack.Token as ActivityStackToken
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The unit tests for activity embedding extension functions to [Bundle]
 *
 * @see Bundle.setLaunchingActivityStack
 * @see Bundle.setOverlayCreateParams
 */
class ActivityEmbeddingOptionsTest {

    @Mock private lateinit var mockEmbeddingBackend: EmbeddingBackend
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockActivity: Activity
    @Mock private lateinit var mockOptions: Bundle

    private lateinit var annotationClosable: AutoCloseable

    private lateinit var mockActivityStack: ActivityStack

    @Before
    fun setUp() {
        annotationClosable = MockitoAnnotations.openMocks(this)

        mockActivityStack =
            ActivityStack(listOf(), true, ActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN)
        whenever(mockActivity.applicationContext).doReturn(mockContext)

        EmbeddingBackend.overrideDecorator(
            object : EmbeddingBackendDecorator {
                override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                    mockEmbeddingBackend
            }
        )
    }

    @After
    fun tearDown() {
        EmbeddingBackend.reset()
        annotationClosable.close()
    }

    @Test
    fun testSetLaunchingActivityStack() {
        mockOptions.setLaunchingActivityStack(mockActivity, mockActivityStack)

        verify(mockEmbeddingBackend).setLaunchingActivityStack(mockOptions, mockActivityStack)
    }

    @Test
    fun testSetOverlayCreateParams() {
        val overlayCreateParams = OverlayCreateParams(overlayAttributes = OverlayAttributes())
        mockOptions.setOverlayCreateParams(mockActivity, overlayCreateParams)

        verify(mockEmbeddingBackend).setOverlayCreateParams(mockOptions, overlayCreateParams)
    }
}
