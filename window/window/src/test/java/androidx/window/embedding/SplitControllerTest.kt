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
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalWindowApi::class)
internal class SplitControllerTest {

    private val mockBackend = mock<EmbeddingBackend>()
    private val splitController: SplitController = SplitController(mockBackend)
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun test_splitInfoListComesFromBackend() = testScope.runTest {
        val expected = listOf(SplitInfo(
            ActivityStack(emptyList(), true, mock()),
            ActivityStack(emptyList(), true, mock()),
            SplitAttributes(),
            mock()
        ))
        doAnswer { invocationOnMock ->
            @Suppress("UNCHECKED_CAST")
            val listener = invocationOnMock.arguments.last() as Consumer<List<SplitInfo>>
            listener.accept(expected)
        }.whenever(mockBackend).addSplitListenerForActivity(any(), any(), any())

        val mockActivity = mock<Activity>()
        val actual = splitController.splitInfoList(mockActivity).take(1).toList().first()

        assertEquals(expected, actual)
        verify(mockBackend).addSplitListenerForActivity(eq(mockActivity), any(), any())
        verify(mockBackend).removeSplitListenerForActivity(any())
    }

    @Test
    fun test_splitSupportStatus_delegates() {
        val expected = SplitController.SplitSupportStatus.SPLIT_AVAILABLE
        whenever(mockBackend.splitSupportStatus).thenReturn(expected)

        val actual = splitController.splitSupportStatus

        assertEquals(expected, actual)
    }

    @Test
    fun test_splitAttributesCalculator_delegates() {
        val mockCalculator = mock<(SplitAttributesCalculatorParams) -> SplitAttributes>()

        splitController.setSplitAttributesCalculator(mockCalculator)
        verify(mockBackend).setSplitAttributesCalculator(mockCalculator)

        splitController.clearSplitAttributesCalculator()
        verify(mockBackend).clearSplitAttributesCalculator()
    }

    @Test
    fun test_updateSplitAttribute_delegates() {
        val mockSplitAttributes = SplitAttributes()
        val mockSplitInfo = SplitInfo(
            ActivityStack(emptyList(), true, mock()),
            ActivityStack(emptyList(), true, mock()),
            mockSplitAttributes,
            mock()
        )
        splitController.updateSplitAttributes(mockSplitInfo, mockSplitAttributes)
        verify(mockBackend).updateSplitAttributes(eq(mockSplitInfo), eq(mockSplitAttributes))
    }

    @Test
    fun test_invalidateTopVisibleSplitAttributes_delegates() {
        splitController.invalidateTopVisibleSplitAttributes()
        verify(mockBackend).invalidateTopVisibleSplitAttributes()
    }
}
