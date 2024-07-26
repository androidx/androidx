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

package androidx.pdf.viewmodel

import android.content.Context
import android.net.Uri
import androidx.pdf.data.DisplayData
import androidx.pdf.data.Openable
import androidx.pdf.viewer.loader.PdfLoader
import androidx.pdf.viewer.loader.WeakPdfLoaderCallbacks
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.filters.SmallTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PdfViewerViewModelTest {

    private val context: Context = getApplicationContext()
    private val mockCallbacks = mock(WeakPdfLoaderCallbacks::class.java)
    private val displayData1 =
        DisplayData(
            Uri.parse("content://test.app.authority/fake-file1.pdf"),
            "FakeFile1",
            mock(Openable::class.java)
        )
    private val displayData2 =
        DisplayData(
            Uri.parse("content://test.app.authority/fake-file2.pdf"),
            "FakeFile2",
            mock(Openable::class.java)
        )
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun testUpdatePdfLoader_CreatesNewPdfLoader() {
        val viewModel = PdfLoaderViewModel()

        assertNull(viewModel.pdfLoaderStateFlow.value)
        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        assertNotNull(viewModel.pdfLoaderStateFlow.value)
    }

    @Test
    fun testUpdatePdfLoader_WithSameDisplayData_ReusesExistingPdfLoader() {
        val viewModel = PdfLoaderViewModel()

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val initialLoader = viewModel.pdfLoaderStateFlow.value
        assertNotNull(initialLoader)

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val currentLoader = viewModel.pdfLoaderStateFlow.value
        assertEquals(initialLoader, currentLoader)
    }

    @Test
    fun testUpdatePdfLoader_WithNewDisplayData_CreatesNewPdfLoader() {
        val viewModel = PdfLoaderViewModel()

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val initialLoader = viewModel.pdfLoaderStateFlow.value
        assertNotNull(initialLoader)

        viewModel.updatePdfLoader(context, displayData2, mockCallbacks)
        val currentLoader = viewModel.pdfLoaderStateFlow.value
        assertNotEquals(initialLoader, currentLoader)
    }

    @Test
    fun testUpdatePdfLoader_WithSameDisplayData_CallsReloadDocument() {
        val viewModel = PdfLoaderViewModel()

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val pdfLoader = mock(PdfLoader::class.java)
        viewModel.updatePdfLoader(pdfLoader)

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        verify(pdfLoader, times(1)).reloadDocument()
    }

    @Test
    fun testUpdatePdfLoader_WithSameDisplayData_CallsSetCallbacks() {
        val viewModel = PdfLoaderViewModel()

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val pdfLoader = mock(PdfLoader::class.java)
        viewModel.updatePdfLoader(pdfLoader)

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        verify(pdfLoader, times(1)).callbacks = mockCallbacks
    }

    @Test
    fun testUpdatePdfLoader_WithNewDisplayData_CleansUpOldLoader() {
        val viewModel = PdfLoaderViewModel()

        viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
        val pdfLoader = mock(PdfLoader::class.java)
        viewModel.updatePdfLoader(pdfLoader)

        viewModel.updatePdfLoader(context, displayData2, mockCallbacks)
        verify(pdfLoader, times(1)).disconnect()
        verify(pdfLoader, times(1)).cancelAll()
    }

    @Test
    fun testUpdatePdfLoaderWithNewDisplayData_triggersObserver() =
        testScope.runTest {
            val viewModel = PdfLoaderViewModel()
            var emissionCount = 0

            val job =
                testScope.launch {
                    viewModel.pdfLoaderStateFlow
                        // Filter out initial null value
                        .filterNotNull()
                        .collect { emissionCount++ }
                }

            viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
            viewModel.updatePdfLoader(context, displayData2, mockCallbacks)

            assertEquals(2, emissionCount)

            job.cancelAndJoin()
        }

    @Test
    fun testUpdatePdfLoaderWithSameDisplayData_doesNotTriggerObserver() =
        testScope.runTest {
            val viewModel = PdfLoaderViewModel()
            var emissionCount = 0

            val job =
                testScope.launch {
                    viewModel.pdfLoaderStateFlow
                        // Filter out initial null value
                        .filterNotNull()
                        .collect { emissionCount++ }
                }

            viewModel.updatePdfLoader(context, displayData1, mockCallbacks)
            viewModel.updatePdfLoader(context, displayData1, mockCallbacks)

            assertEquals(1, emissionCount)

            job.cancelAndJoin()
        }
}
