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

import android.os.Bundle
import androidx.core.util.Consumer
import androidx.window.extensions.embedding.ActivityStack.Token as ActivityStackToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class OverlayControllerTest {
    private val mockBackend = mock<EmbeddingBackend>()
    private val overlayController = OverlayController(mockBackend)
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun testSetOverlayCreateParams() {
        val options = Bundle()
        val params = OverlayCreateParams()
        overlayController.setOverlayCreateParams(options, params)

        verify(mockBackend).setOverlayCreateParams(options, params)
    }

    @Test
    fun test_overlayAttributesCalculator_delegates() {
        val calculator = { _: OverlayAttributesCalculatorParams -> OverlayAttributes() }

        overlayController.setOverlayAttributesCalculator(calculator)

        verify(mockBackend).setOverlayAttributesCalculator(calculator)

        overlayController.clearOverlayAttributesCalculator()

        verify(mockBackend).clearOverlayAttributesCalculator()
    }

    @Test
    fun test_updateOverlayAttributes_delegates() {
        val tag = "test"
        val overlayAttributes = OverlayAttributes()

        overlayController.updateOverlayAttributes(tag, overlayAttributes)

        verify(mockBackend).updateOverlayAttributes(tag, overlayAttributes)
    }

    @Test
    fun test_overlayInfoComesFromBackend() =
        testScope.runTest {
            val tag = "test"
            val expected =
                OverlayInfo(
                    overlayTag = tag,
                    currentOverlayAttributes = OverlayAttributes(),
                    activityStack =
                        ActivityStack(
                            emptyList(),
                            true,
                            ActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
                        )
                )
            doAnswer { invocationOnMock ->
                    @Suppress("UNCHECKED_CAST")
                    val listener = invocationOnMock.arguments.last() as Consumer<OverlayInfo>
                    listener.accept(expected)
                }
                .whenever(mockBackend)
                .addOverlayInfoCallback(any(), any(), any())

            val actual = overlayController.overlayInfo(tag).take(1).toList().first()

            Assert.assertEquals(expected, actual)
            verify(mockBackend).addOverlayInfoCallback(eq(tag), any(), any())
            verify(mockBackend).removeOverlayInfoCallback(any())
        }
}
