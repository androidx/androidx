/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresExtension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.idlingresource.PdfIdlingResource
import androidx.pdf.ink.EditablePdfViewerFragment
import androidx.pdf.models.FormEditInfo
import androidx.pdf.testapp.R
import androidx.pdf.view.PdfContentLayout
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.R as PdfR
import java.util.UUID

/**
 * A subclass fragment from [EditablePdfViewerFragment] to include
 * [androidx.test.espresso.IdlingResource] while loading pdf document.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class TestEditablePdfViewerFragment : EditablePdfViewerFragment {

    constructor() : super()

    val pdfLoadingIdlingResource = PdfIdlingResource(PDF_LOAD_RESOURCE_NAME)
    val pdfScrollIdlingResource = PdfIdlingResource(PDF_SCROLL_RESOURCE_NAME)
    val pdfApplyEditsIdlingResource = PdfIdlingResource(APPLY_EDITS_RESOURCE_NAME)
    val pdfFormFillingIdlingResource = PdfIdlingResource(FORM_FILLING_RESOURCE_NAME)

    var pdfDocument: PdfDocument? = null
    var documentLoaded = false
    var documentError: Throwable? = null
    private var hostView: ConstraintLayout? = null

    var onApplyEditsSuccessCalled = false
    var onApplyEditsFailedCalled = false

    var onEnterEditModeCalled = false
    var onEnterEditModeCalledCount = 0
    var onExitEditModeCalled = false
    var onFormWidgetInfoUpdatedCalled = false

    private var gestureStateChangedListener: PdfView.OnGestureStateChangedListener? = null
    private var writeHandle: PdfWriteHandle? = null

    fun getPdfViewInstance(): PdfView = pdfView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ConstraintLayout

        // Inflate the custom layout for this fragment
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as ConstraintLayout
        hostView?.let { hostView -> handleInsets(hostView) }

        // Add the default PDF viewer to the custom layout
        hostView?.addView(view)
        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            findViewById<PdfContentLayout>(PdfR.id.pdfContentLayout).pdfView.apply {
                isFormFillingEnabled = true
            }
        }
        gestureStateChangedListener =
            object : PdfView.OnGestureStateChangedListener {
                    override fun onGestureStateChanged(newState: Int) {
                        if (newState == PdfView.GESTURE_STATE_IDLE) {
                            pdfScrollIdlingResource.decrement()
                        }
                    }
                }
                .also { pdfView.addOnGestureStateChangedListener(it) }
    }

    @OptIn(ExperimentalPdfApi::class)
    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        pdfView.addOnFormWidgetInfoUpdatedListener(
            object : PdfView.OnFormWidgetInfoUpdatedListener {
                override fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo) {
                    onFormWidgetInfoUpdatedCalled = true
                    pdfFormFillingIdlingResource.decrement()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gestureStateChangedListener?.let { pdfView.removeOnGestureStateChangedListener(it) }
        gestureStateChangedListener = null
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        pdfDocument = document
        documentLoaded = true
        pdfLoadingIdlingResource.decrement()
    }

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        documentError = error
        pdfLoadingIdlingResource.decrement()
    }

    override fun onApplyEditsSuccess(handle: PdfWriteHandle) {
        super.onApplyEditsSuccess(handle)
        writeHandle = handle
        onApplyEditsSuccessCalled = true
        pdfApplyEditsIdlingResource.decrement()
    }

    override fun onApplyEditsFailed(error: Throwable) {
        super.onApplyEditsFailed(error)
        onApplyEditsFailedCalled = true
        pdfApplyEditsIdlingResource.decrement()
    }

    override fun onEnterEditMode() {
        super.onEnterEditMode()
        onEnterEditModeCalled = true
        onEnterEditModeCalledCount += 1
    }

    override fun onExitEditMode() {
        super.onExitEditMode()
        onExitEditModeCalled = true
    }

    fun setIsAnnotationIntentResolvable(value: Boolean) {
        setAnnotationIntentResolvability(value)
    }

    suspend fun fetchAnnotations(pageNum: Int): List<KeyedPdfAnnotation> {
        return pdfDocument?.getAnnotationsForPage(pageNum) ?: emptyList()
    }

    suspend fun writeTo(destination: ParcelFileDescriptor) {
        writeHandle?.writeTo(destination)
    }

    companion object {
        // Resource name must be unique to avoid conflicts while running multiple test scenarios
        private val PDF_LOAD_RESOURCE_NAME = "PdfLoad-${UUID.randomUUID()}"
        private val PDF_SCROLL_RESOURCE_NAME = "PdfScroll-${UUID.randomUUID()}"
        private val APPLY_EDITS_RESOURCE_NAME = "ApplyEdits-${UUID.randomUUID()}"
        private val FORM_FILLING_RESOURCE_NAME = "FormFilling-${UUID.randomUUID()}"

        fun handleInsets(hostView: View) {
            ViewCompat.setOnApplyWindowInsetsListener(hostView) { view, insets ->
                // Get the insets for the system bars (status bar, navigation bar)
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Adjust the padding of the container view to accommodate system windows
                view.setPadding(
                    view.paddingLeft,
                    systemBarsInsets.top,
                    view.paddingRight,
                    systemBarsInsets.bottom,
                )
                insets
            }
        }
    }
}
