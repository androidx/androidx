/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.core.util.Consumer as JetpackConsumer
import androidx.window.WindowSdkExtensionsRule
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.EmbeddedActivityWindowInfo as ExtensionsActivityWindowInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Verifies [ActivityWindowInfoCallbackController] */
@Suppress("GuardedBy")
class ActivityWindowInfoCallbackControllerTest {

    @get:Rule val testRule = WindowSdkExtensionsRule()

    @Mock private lateinit var embeddingExtension: ActivityEmbeddingComponent
    @Mock private lateinit var callback1: JetpackConsumer<EmbeddedActivityWindowInfo>
    @Mock private lateinit var callback2: JetpackConsumer<EmbeddedActivityWindowInfo>
    @Mock private lateinit var activity1: Activity
    @Mock private lateinit var activity2: Activity

    @Captor
    private lateinit var callbackCaptor: ArgumentCaptor<Consumer<ExtensionsActivityWindowInfo>>

    private lateinit var controller: ActivityWindowInfoCallbackController
    private lateinit var mockAnnotations: AutoCloseable

    @Before
    fun setUp() {
        testRule.overrideExtensionVersion(6)
        mockAnnotations = MockitoAnnotations.openMocks(this)

        controller = ActivityWindowInfoCallbackController(embeddingExtension)
        // ArrayMap is not available in JVM test
        controller.callbacks = HashMap()
        controller = spy(controller)
    }

    @After
    fun tearDown() {
        mockAnnotations.close()
    }

    @Test
    fun testAddCallback() {
        // Register the extensions callback
        controller.addCallback(activity1, callback1)

        verify(embeddingExtension).setEmbeddedActivityWindowInfoCallback(any(), any())

        // Do not register for the additional callback
        clearInvocations(embeddingExtension)
        controller.addCallback(activity1, callback2)

        verify(embeddingExtension, never()).setEmbeddedActivityWindowInfoCallback(any(), any())
    }

    @Test
    fun testRemoveCallback() {
        // Unregister after the last Jetpack callback is removed.
        controller.addCallback(activity1, callback1)
        controller.addCallback(activity1, callback2)

        controller.removeCallback(callback1)

        verify(embeddingExtension, never()).clearEmbeddedActivityWindowInfoCallback()

        controller.removeCallback(callback2)

        verify(embeddingExtension).clearEmbeddedActivityWindowInfoCallback()
    }

    @Test
    fun testActivityWindowInfoChanged() {
        controller.addCallback(activity1, callback1)
        controller.addCallback(activity1, callback2)

        verify(embeddingExtension)
            .setEmbeddedActivityWindowInfoCallback(any(), callbackCaptor.capture())
        val extensionsCallback = callbackCaptor.value

        val extensionsInfo: ExtensionsActivityWindowInfo = mock()
        val expectedInfo =
            EmbeddedActivityWindowInfo(
                isEmbedded = true,
                parentHostBounds = mock(),
                boundsInParentHost = mock()
            )
        doReturn(expectedInfo).whenever(controller).translate(extensionsInfo)

        // No callback because the activity doesn't match.
        doReturn(activity2).whenever(extensionsInfo).activity
        extensionsCallback.accept(extensionsInfo)

        verify(callback1, never()).accept(any())
        verify(callback2, never()).accept(any())

        // Should receive the correct translated info.
        doReturn(activity1).whenever(extensionsInfo).activity
        extensionsCallback.accept(extensionsInfo)

        verify(callback1).accept(expectedInfo)
        verify(callback2).accept(expectedInfo)

        // Do not send unchanged info.
        clearInvocations(callback1)
        clearInvocations(callback2)
        extensionsCallback.accept(extensionsInfo)

        verify(callback1, never()).accept(any())
        verify(callback2, never()).accept(any())

        // Only the remaining callback can receive the info.
        controller.removeCallback(callback1)
        val expectedInfo2 =
            EmbeddedActivityWindowInfo(
                isEmbedded = false,
                parentHostBounds = mock(),
                boundsInParentHost = mock()
            )
        doReturn(expectedInfo2).whenever(controller).translate(extensionsInfo)
        extensionsCallback.accept(extensionsInfo)

        verify(callback1, never()).accept(any())
        verify(callback2).accept(any())
    }
}
