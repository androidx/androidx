/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import androidx.pdf.PdfDocument
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewAutofillTest {

    private lateinit var formEditInfos: MutableList<FormEditInfo>
    private val fakePdfDocument = createFakePdfDocument()

    @Before
    fun setUp() {
        PdfFeatureFlags.isAutofillEnabled = true
        formEditInfos = Collections.synchronizedList(mutableListOf())
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    private fun setupPdfView(
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        fakePdfDocument: FakePdfDocument?,
        enableFormFilling: Boolean,
    ) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                val pdfView =
                    PdfView(activity).apply {
                        isFormFillingEnabled = enableFormFilling
                        id = PDF_VIEW_ID
                        pdfDocument = fakePdfDocument
                        addOnFormWidgetInfoUpdatedListener(
                            object : PdfView.OnFormWidgetInfoUpdatedListener {
                                override fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo) {
                                    formEditInfos.add(formEditInfo)
                                }
                            }
                        )
                    }
                container.addView(pdfView, ViewGroup.LayoutParams(width, height))
            }
        }
    }

    @Test
    fun onProvideAutofillVirtualStructure_formFillingDisabled_doesNotPopulate() = runTest {
        val structure = mock(ViewStructure::class.java)

        withPdfView(enableFormFilling = false) { pdfView, scenario ->
            scenario.onActivity { pdfView.onProvideAutofillVirtualStructure(structure, 0) }
            verify(structure, never()).addChildCount(anyInt())
        }
    }

    @Test
    fun onProvideAutofillVirtualStructure_populatesStructure() = runTest {
        val structure = mock(ViewStructure::class.java)
        val childStructure = mock(ViewStructure::class.java)
        whenever(structure.newChild(anyInt())).thenReturn(childStructure)

        withPdfView(enableFormFilling = true) { pdfView, scenario ->
            scenario.onActivity {
                whenever(structure.autofillId).thenReturn(pdfView.autofillId)
                pdfView.onProvideAutofillVirtualStructure(structure, 0)
            }
            // Verify that 1 child was added for the single form widget
            verify(structure).addChildCount(1)
        }
    }

    @Test
    fun autofill_formFillingDisabled_doesNotApply() = runTest {
        val autofillValues =
            SparseArray<AutofillValue>().apply { put(0, AutofillValue.forText("Autofill Text")) }

        withPdfView(enableFormFilling = false) { pdfView, scenario ->
            scenario.onActivity { pdfView.autofill(autofillValues) }
            Espresso.onIdle()
            assertThat(formEditInfos).isEmpty()
        }
    }

    @Test
    fun autofill_appliesValues() = runTest {
        val autofillText = "Autofilled Text"
        val autofillValues =
            SparseArray<AutofillValue>().apply { put(0, AutofillValue.forText(autofillText)) }

        withPdfView(enableFormFilling = true) { pdfView, scenario ->
            scenario.onActivity { pdfView.dispatchWindowVisibilityChanged(View.VISIBLE) }
            Espresso.onIdle()
            scenario.onActivity { pdfView.autofill(autofillValues) }

            waitForAutofillValues(formEditInfos, expectedSize = 1)
            assertThat(formEditInfos).hasSize(1)
            assertThat(formEditInfos[0].text).isEqualTo(autofillText)
        }
    }

    private fun createFakePdfDocument(
        formWidgetRect: Rect = Rect(10, 10, 100, 100)
    ): FakePdfDocument {
        return FakePdfDocument(
            pages = List(1) { Point(DEFAULT_WIDTH, DEFAULT_HEIGHT) },
            formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
            pageFormWidgetInfos =
                mapOf(
                    0 to
                        listOf(
                            FormWidgetInfo.createTextField(
                                widgetIndex = 0,
                                widgetRect = formWidgetRect,
                                textValue = "TextField",
                                accessibilityLabel = "TextField",
                                isReadOnly = false,
                                isEditableText = true,
                                isMultiLineText = false,
                                maxLength = 100,
                                fontSize = 12f,
                            )
                        )
                ),
        )
    }

    private suspend fun withPdfView(
        enableFormFilling: Boolean,
        block: suspend (PdfView, ActivityScenario<PdfViewTestActivity>) -> Unit,
    ) {
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = enableFormFilling)
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            if (enableFormFilling) {
                fakePdfDocument.waitForFormDataFetch(untilPage = 0)
            }

            var pdfView: PdfView? = null
            scenario.onActivity { activity ->
                pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
            }
            block(pdfView!!, scenario)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun waitForAutofillValues(
        formEditInfos: MutableList<FormEditInfo>,
        expectedSize: Int,
        timeoutMillis: Long = 1000,
    ) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(timeoutMillis.milliseconds) {
                while (formEditInfos.size < expectedSize) {
                    delay(100.milliseconds)
                }
            }
        }
    }

    companion object {
        private const val PDF_VIEW_ID = 123456789
        private const val DEFAULT_WIDTH = 200
        private const val DEFAULT_HEIGHT = 400
    }
}
