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

package androidx.pdf.ink

import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.pdf.PdfSandboxHandle
import androidx.pdf.PdfWriteHandle
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.annotation.AnnotationsView
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.LocatedAnnotations
import androidx.pdf.annotation.OnAnnotationLocatedListener
import androidx.pdf.annotation.highlights.InProgressTextHighlightsListener
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.models.AnnotationsDisplayState
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.VisiblePdfAnnotations
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.ink.model.ApplyEditsState
import androidx.pdf.ink.model.ApplyInProgressException
import androidx.pdf.ink.state.AnnotationDrawingMode
import androidx.pdf.ink.state.PdfEditMode
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_ANNOTATIONS
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_FORM_FILLING
import androidx.pdf.ink.util.PageTransformCalculator
import androidx.pdf.ink.util.toHighlighterConfig
import androidx.pdf.ink.util.toInkBrush
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.ink.view.draganddrop.ToolbarCoordinator
import androidx.pdf.ink.view.tool.AnnotationToolInfo
import androidx.pdf.models.FormEditInfo
import androidx.pdf.view.PdfContentLayout
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfStylingOptions
import androidx.pdf.viewer.fragment.PdfViewerFragment
import java.util.Collections
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [androidx.fragment.app.Fragment] that extends [PdfViewerFragment] to provide PDF editing
 * capabilities, including annotation and form filling, leveraging the 'androidx.ink' library.
 *
 * <p>This fragment coordinates the underlying PDF content with editing layers, enabling users to
 * add ink strokes, create annotations, and modify form fields. It manages the interaction logic
 * between viewing the document and performing edits.
 *
 * <p><b>Editing Workflow:</b>
 * <ol>
 * <li><b>Viewing:</b> Behaves exactly like [PdfViewerFragment].
 * <li><b>Editing:</b> When [isEditModeEnabled] is set to `true`, user can leverage editing
 *   capabilities(such as annotating or filling forms).
 * <li><b>Saving:</b> Edits are accumulated as "drafts". To persist changes, the host must call
 *   [applyDraftEdits], which asynchronously applies unsaved edits and creates a [PdfWriteHandle]
 *   used to write the modified document to a file.
 * </ol>
 *
 * @see PdfViewerFragment
 * @see applyDraftEdits
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
public open class EditablePdfViewerFragment : PdfViewerFragment {

    public constructor() : super()

    protected constructor(pdfStylingOptions: PdfStylingOptions) : super(pdfStylingOptions)

    /**
     * If `true`, the fragment is in edit mode, allowing for annotating or editing. If `false`, the
     * fragment is in viewing mode.
     *
     * Note: The host is responsible for setting this to `false` after a write operation is
     * complete.
     */
    public var isEditModeEnabled: Boolean
        get() {
            return documentViewModel.pdfEditMode is PdfEditMode.Enabled
        }
        set(value) {
            documentViewModel.pdfEditMode =
                if (value) PdfEditMode.Enabled() else PdfEditMode.Disabled
        }

    /**
     * Returns `true` if an `applyDraftEdits` operation is currently in progress.
     *
     * @see applyDraftEdits
     */
    public val isApplyEditsInProgress: Boolean
        get() = documentViewModel.applyEditsStatus.value is ApplyEditsState.InProgress

    /**
     * Returns `true` if there are any draft edits that have not yet been applied to the document,
     * `false` otherwise.
     *
     * This can be used to prompt the user to save changes before navigating away, as draft edits
     * will be lost if the fragment is removed from the stack or comes out of edit mode.
     */
    @get:JvmName("hasUnsavedChanges")
    public val hasUnsavedChanges: Boolean
        get() = documentViewModel.hasUnsavedChanges()

    /**
     * Callback invoked when [EditablePdfViewerFragment] enters edit mode. This is triggered when
     * the user begins an edit for example modifying a form field or interaction via toolbox.
     *
     * <p> This callback can be used by the developers to make any UI changes required when the user
     * enters edit mode, e.g. showing the "Save" button to the user.
     */
    public open fun onEnterEditMode() {}

    /**
     * Callback invoked when [EditablePdfViewerFragment] exits edit mode. This is triggered when the
     * the edit mode is disabled and the fragment completes cleaning up it's edit state.
     *
     * <p> This callback can be used by the developers to make any UI changes required when the user
     * exits edit mode e.g. hiding the "Save" button.
     *
     * @see isEditModeEnabled
     */
    public open fun onExitEditMode() {}

    /**
     * Applies all draft edits to the document.
     *
     * This operation executes asynchronously. The operation will be terminated if
     * [EditablePdfViewerFragment] is removed from the fragment manager while an [applyDraftEdits]
     * is in progress. [EditablePdfViewerFragment] internally disallows editing capabilities during
     * complete operation. Upon completion, either [onApplyEditsSuccess] or [onApplyEditsFailed]
     * will be invoked with the result.
     *
     * @throws ApplyInProgressException if another apply operation is already in progress.
     */
    public fun applyDraftEdits() {
        if (isApplyEditsInProgress) {
            throw ApplyInProgressException()
        }
        documentViewModel.applyDraftEdits()
    }

    /**
     * Callback invoked when draft edits have been successfully applied to the document.
     *
     * The host should override this method to perform the write operation. The provided
     * [PdfWriteHandle] allows writing the document changes to a [android.os.ParcelFileDescriptor].
     * The handle **must** be closed after writing to ensure proper resource cleanup.
     *
     * After the write operation is complete, the host is responsible for exiting the edit mode by
     * setting [isEditModeEnabled] to `false`.
     *
     * @param handle A [PdfWriteHandle] to be used for writing the changes to a file.
     * @see applyDraftEdits
     */
    public open fun onApplyEditsSuccess(handle: PdfWriteHandle) {}

    /**
     * Callback invoked when applying draft edits has failed.
     *
     * @param error The [Throwable] that caused the failure.
     * @see applyDraftEdits
     */
    public open fun onApplyEditsFailed(error: Throwable) {}

    private lateinit var wetStrokesView: InProgressStrokesView
    private lateinit var annotationView: AnnotationsView
    private lateinit var onViewportChangedListener: PdfView.OnViewportChangedListener
    private lateinit var gestureStateChangedListener: PdfView.OnGestureStateChangedListener
    private lateinit var wetStrokesOnFinishedListener: WetStrokesOnFinishedListener
    private lateinit var onFormWidgetInfoUpdatedListener: PdfView.OnFormWidgetInfoUpdatedListener
    private lateinit var annotationsTouchEventDispatcher: AnnotationsTouchEventDispatcher

    private lateinit var wetStrokesViewTouchHandler: WetStrokesViewTouchHandler
    private lateinit var pdfContentLayoutTouchListener: PdfContentLayoutTouchListener

    @VisibleForTesting internal lateinit var annotationToolbar: AnnotationToolbar

    private lateinit var toolbarCoordinator: ToolbarCoordinator
    private lateinit var pdfLoaderHandle: PdfSandboxHandle

    private val toolbarLayoutChangeListener =
        View.OnLayoutChangeListener {
            _,
            left,
            top,
            right,
            bottom,
            oldLeft,
            oldTop,
            oldRight,
            oldBottom ->
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                wetStrokesView.maskPath = createToolbarMaskPath()
            }
        }

    private lateinit var pageInfoProvider: PageInfoProviderImpl

    private val annotationsViewDispatcher = AnnotationsViewTouchEventDispatcher()
    private val inkViewDispatcher = InkViewTouchEventDispatcher()

    private var pageTransformCalculator: PageTransformCalculator = PageTransformCalculator()
    private val strokeIdToPageNumMap: MutableMap<InProgressStrokeId, Int> =
        Collections.synchronizedMap(mutableMapOf<InProgressStrokeId, Int>())

    private val inProgressTextHighlightsListener =
        object : InProgressTextHighlightsListener {
            override fun onTextHighlightStarted(
                viewPoint: PointF,
                inProgressHighlightId: InProgressHighlightId,
            ) {
                annotationsTouchEventDispatcher.switchActiveDispatcher(
                    annotationsViewDispatcher,
                    viewPoint,
                )
            }

            override fun onTextHighlightRejected(viewPoint: PointF) {
                annotationsTouchEventDispatcher.switchActiveDispatcher(inkViewDispatcher, viewPoint)
            }

            override fun onTextHighlightFinished(
                annotations: Map<InProgressHighlightId, PdfAnnotation>
            ) {
                annotations.forEach { (_, annotation) ->
                    documentViewModel.addDraftAnnotation(annotation)
                }
            }

            override fun onTextHighlightError(exception: RequestFailedException) {
                // TODO(b/409464802): Propagate it through event callback
            }
        }

    private val onAnnotationLocatedListener =
        object : OnAnnotationLocatedListener {
            override fun onAnnotationsLocated(locatedAnnotations: LocatedAnnotations) {
                if (documentViewModel.drawingMode.value == AnnotationDrawingMode.EraserMode) {
                    val topAnnotation = locatedAnnotations.annotations.first()
                    documentViewModel.removeAnnotation(topAnnotation.key)
                }
            }
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val documentViewModel: EditableDocumentViewModel by viewModels {
        EditableDocumentViewModel.Factory
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        /**
         * By starting initialization early, subsequent calls to load a document—whether through a
         * new [documentUri] or a forced reload via[EditableDocumentViewModel.forceLoadDocument] can
         * reuse the existing service connection, significantly reducing latency by avoiding
         * repeated connect/disconnect cycles.
         */
        pdfLoaderHandle = SandboxedPdfLoader.startInitialization(context)
    }

    override fun onDetach() {
        super.onDetach()
        pdfLoaderHandle.close()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView =
            super.onCreateView(inflater, container, savedInstanceState) as ConstraintLayout

        wetStrokesView =
            InProgressStrokesView(requireContext()).apply {
                id = R.id.pdf_wet_strokes_view
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                visibility = VISIBLE
            }

        annotationView =
            AnnotationsView(requireContext()).apply {
                id = R.id.pdf_annotation_view
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        annotationToolbar =
            inflater.inflate(R.layout.annotation_toolbar_layout, null, false) as AnnotationToolbar
        toolbarCoordinator =
            ToolbarCoordinator(requireContext()).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }

        val pdfContentLayout =
            rootView.findViewById<PdfContentLayout>(
                androidx.pdf.viewer.fragment.R.id.pdfContentLayout
            )
        pdfContentLayout.addView(annotationView)
        pdfContentLayout.addView(wetStrokesView)

        rootView.addView(toolbarCoordinator)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageInfoProvider = PageInfoProviderImpl()
        viewLifecycleOwner.lifecycleScope.launch {
            documentViewModel.applyEditsStatus.collect { status ->
                when (status) {
                    is ApplyEditsState.Success -> {
                        onApplyEditsSuccess(status.handle)
                        documentViewModel.resetApplyEditsStatus()
                    }
                    is ApplyEditsState.Failure -> {
                        onApplyEditsFailed(status.error)
                        documentViewModel.resetApplyEditsStatus()
                    }
                    else -> {
                        /* No-Op */
                    }
                }
            }
        }

        setupUiStateCollectors()
        setupTouchListeners()
        setupPdfViewListeners()
        setupAnnotationViewListeners()
        setupAnnotationToolbar()
        setupToolbarCoordinator(annotationToolbar)
    }

    private fun setupAnnotationViewListeners() {
        annotationView.pageInfoProvider = pageInfoProvider
        annotationView.addOnAnnotationLocatedListener(onAnnotationLocatedListener)
        annotationView.addInProgressTextHighlightsListener(inProgressTextHighlightsListener)
    }

    private fun setupUiStateCollectors() {
        collectFlowOnLifecycleScope {
            documentViewModel.shouldShowAnnotationToolbar.collect {
                updateAnnotationToolbarVisibility(it)
            }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.pdfEditModeFlow.collect { editMode ->
                if (editMode is PdfEditMode.Enabled) onEnterEditMode() else onExitEditMode()
                updateUiForEditMode(editMode)
            }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.annotationsDisplayStateFlow.collect { displayState ->
                updateAnnotationsView(displayState)
            }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.areAnnotationsVisibleFlow.collect { areVisible ->
                annotationView.visibility = if (areVisible) VISIBLE else GONE
            }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.isAnnotationInteractionEnabled.collect { isEnabled ->
                pdfContentLayoutTouchListener.isAnnotationInteractionEnabled = isEnabled
            }
        }
    }

    private fun setupToolbarCoordinator(toolbar: AnnotationToolbar) {
        toolbarCoordinator.apply { attachToolbar(toolbar) }
    }

    override fun onDestroyView() {
        // Clean up the listener to avoid potential memory leaks
        super.onDestroyView()
        pdfView.removeOnViewportChangedListener(onViewportChangedListener)
        pdfView.removeOnGestureStateChangedListener(gestureStateChangedListener)
        pdfView.setOnBitmapUpdatedListener(null)
        annotationView.removeInProgressTextHighlightsListener(inProgressTextHighlightsListener)
        pdfView.removeOnFormWidgetInfoUpdatedListener(onFormWidgetInfoUpdatedListener)
        wetStrokesView.removeFinishedStrokesListener(wetStrokesOnFinishedListener)
        annotationToolbar.setAnnotationToolbarListener(null)
        pdfContainer.setOnTouchListener(null)
        if (::annotationToolbar.isInitialized) {
            annotationToolbar.removeOnLayoutChangeListener(toolbarLayoutChangeListener)
        }
    }

    private fun updateUiForEditMode(editMode: PdfEditMode) {
        toolboxView.visibility = if (editMode is PdfEditMode.Enabled) GONE else VISIBLE

        when (editMode) {
            is PdfEditMode.Enabled -> {
                if (editMode.journey == EDITING_JOURNEY_ANNOTATIONS) {
                    documentViewModel.initialFormFillingEnabledState = pdfView.isFormFillingEnabled
                    // Disable form filling when in annotations mode
                    pdfView.isFormFillingEnabled = false
                    updateUiForAnnotationsEditMode(true)
                }
            }
            is PdfEditMode.Disabled -> {
                // Restore form filling state when exiting edit mode
                documentViewModel.initialFormFillingEnabledState?.let {
                    pdfView.isFormFillingEnabled = it
                    documentViewModel.initialFormFillingEnabledState = null
                }
                updateUiForAnnotationsEditMode(false)
            }
        }
    }

    private fun updateAnnotationToolbarVisibility(isAnnotationToolbarVisible: Boolean) {
        toolbarCoordinator.isVisible = isAnnotationToolbarVisible
        annotationToolbar.isVisible = isAnnotationToolbarVisible
    }

    private fun updateUiForAnnotationsEditMode(isEnabled: Boolean) {
        PdfFeatureFlags.isMultiTouchScrollEnabled = isEnabled

        if (isEnabled) {
            pdfView.clearCurrentSelection()

            // Wait for the toolbar to be laid out, as we need to utilize its width and height
            annotationToolbar.post { wetStrokesView.maskPath = createToolbarMaskPath() }
        } else {
            annotationToolbar.apply {
                reset()
                wetStrokesView.maskPath = null
            }
            toolbarCoordinator.updateLayout()
        }
    }

    private fun setupTouchListeners() {
        toolboxView.setOnEditClickListener { isEditModeEnabled = true }

        wetStrokesOnFinishedListener =
            WetStrokesOnFinishedListener(
                wetStrokesView = wetStrokesView,
                strokeIdToPageNumMap = strokeIdToPageNumMap,
                annotationsViewModel = documentViewModel,
            )
        wetStrokesView.apply {
            addFinishedStrokesListener(wetStrokesOnFinishedListener)
            wetStrokesViewTouchHandler =
                WetStrokesViewTouchHandler(pageInfoProvider::getPageInfoFromViewCoordinates) {
                    strokeId,
                    pageNum ->
                    strokeIdToPageNumMap[strokeId] = pageNum
                }
            setOnTouchListener(wetStrokesViewTouchHandler)
        }

        annotationsTouchEventDispatcher =
            AnnotationsTouchEventDispatcher(annotationsViewDispatcher, inkViewDispatcher)

        val popupDismissalTouchListener = PopupDismissalTouchListener(annotationToolbar)
        pdfContentLayoutTouchListener =
            PdfContentLayoutTouchListener(
                requireContext(),
                annotationsTouchEventDispatcher,
                PdfViewTouchEventDispatcher(),
            )
        // The order of touch listeners is important, as touch events will be delegated
        // sequentially.
        val pdfCompositeTouchListener =
            PdfCompositeTouchListener(popupDismissalTouchListener, pdfContentLayoutTouchListener)
        pdfContainer.setOnTouchListener(pdfCompositeTouchListener)
        pdfContainer.isAnnotationInteractionEnabled = true
    }

    private fun updateAnnotationsView(displayState: AnnotationsDisplayState) {
        val pageRenderDataArray = SparseArray<PageAnnotationsData>()
        val firstVisiblePage = pdfView.firstVisiblePage
        val lastVisiblePage = firstVisiblePage + pdfView.visiblePagesCount - 1

        val visiblePageAnnotations = displayState.visiblePageAnnotations
        val transformationMatrices = displayState.transformationMatrices

        (firstVisiblePage..lastVisiblePage).forEach { pageNum ->
            val pageAnnotationData =
                createPageAnnotationsData(pageNum, visiblePageAnnotations, transformationMatrices)
            pageRenderDataArray.put(pageNum, pageAnnotationData)
        }
        annotationView.annotations = pageRenderDataArray
    }

    private fun createPageAnnotationsData(
        pageNum: Int,
        visiblePageAnnotations: VisiblePdfAnnotations,
        transformationMatrices: Map<Int, Matrix>,
    ): PageAnnotationsData {
        val annotationsForPage: List<KeyedPdfAnnotation> =
            visiblePageAnnotations.getKeyedAnnotationsForPage(pageNum)
        val transformMatrix = transformationMatrices[pageNum]

        if (transformMatrix == null) {
            return PageAnnotationsData(emptyList(), Matrix())
        }

        return PageAnnotationsData(annotationsForPage, transformMatrix)
    }

    private fun setupPdfViewListeners() {
        gestureStateChangedListener =
            object : PdfView.OnGestureStateChangedListener {
                override fun onGestureStateChanged(newState: Int) {
                    if (newState == PdfView.GESTURE_STATE_IDLE) {
                        documentViewModel.isPdfViewGestureActive = false
                    } else {
                        documentViewModel.isPdfViewGestureActive = true
                    }
                }
            }

        onViewportChangedListener =
            object : PdfView.OnViewportChangedListener {
                override fun onViewportChanged(
                    firstVisiblePage: Int,
                    visiblePagesCount: Int,
                    pageLocations: SparseArray<RectF>,
                    zoomLevel: Float,
                ) {
                    updateAnnotationDisplayState(
                        firstVisiblePage,
                        visiblePagesCount,
                        pageLocations,
                        zoomLevel,
                    )
                    pageInfoProvider.zoom = zoomLevel
                    pageInfoProvider.pageLocations = pageLocations
                }
            }

        onFormWidgetInfoUpdatedListener =
            object : PdfView.OnFormWidgetInfoUpdatedListener {
                override fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo) {
                    if (!isEditModeEnabled) {
                        documentViewModel.pdfEditMode =
                            PdfEditMode.Enabled(EDITING_JOURNEY_FORM_FILLING)
                    }
                }
            }

        pdfView.addOnGestureStateChangedListener(gestureStateChangedListener)
        pdfView.addOnViewportChangedListener(onViewportChangedListener)
        pdfView.addOnFormWidgetInfoUpdatedListener(onFormWidgetInfoUpdatedListener)

        pdfView.setOnBitmapUpdatedListener(
            object : PdfView.OnBitmapUpdatedListener {
                override fun onBitmapFetched(pageNum: Int) {
                    documentViewModel.onBitmapFetched(pageNum)
                }

                override fun onBitmapCleared(pageNum: Int) {
                    documentViewModel.onBitmapCleared(pageNum)
                }
            }
        )
    }

    private fun updateAnnotationDisplayState(
        firstVisiblePage: Int,
        visiblePagesCount: Int,
        pageLocations: SparseArray<RectF>,
        zoomLevel: Float,
    ) {
        val lastVisiblePage = firstVisiblePage + visiblePagesCount - 1

        updateTransformationMatrices(firstVisiblePage, visiblePagesCount, pageLocations, zoomLevel)

        documentViewModel.fetchAnnotationsForPageRange(
            startPage = firstVisiblePage,
            endPage = lastVisiblePage,
        )
    }

    private fun generatePageRangeTransformationMatrices(
        firstVisiblePage: Int,
        visiblePagesCount: Int,
        pageLocations: SparseArray<RectF>,
        zoomLevel: Float,
    ): Map<Int, Matrix> {
        val lastVisiblePage = firstVisiblePage + visiblePagesCount - 1
        documentViewModel.visiblePageRange = firstVisiblePage..lastVisiblePage

        return pageTransformCalculator.calculate(
            firstVisiblePage,
            visiblePagesCount,
            pageLocations,
            zoomLevel,
        )
    }

    private fun updateTransformationMatrices(
        firstVisiblePage: Int,
        visiblePagesCount: Int,
        pageLocations: SparseArray<RectF>,
        zoomLevel: Float,
    ) {
        val transformationMatrices =
            generatePageRangeTransformationMatrices(
                firstVisiblePage,
                visiblePagesCount,
                pageLocations,
                zoomLevel,
            )
        documentViewModel.updateTransformationMatrices(transformationMatrices)
    }

    private fun setupAnnotationToolbar() {
        annotationToolbar.addOnLayoutChangeListener(toolbarLayoutChangeListener)
        annotationToolbar.setAnnotationToolbarListener(
            object : AnnotationToolbar.AnnotationToolbarListener {
                override fun onToolChanged(toolInfo: AnnotationToolInfo) {
                    documentViewModel.setCurrentToolInfo(toolInfo)
                }

                override fun onUndo() {
                    documentViewModel.undo()
                }

                override fun onRedo() {
                    documentViewModel.redo()
                }

                override fun onAnnotationVisibilityChanged(isVisible: Boolean) {
                    documentViewModel.areAnnotationsVisible = isVisible
                }
            }
        )

        collectFlowOnLifecycleScope {
            documentViewModel.canUndo.collect { annotationToolbar.canUndo = it }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.canRedo.collect { annotationToolbar.canRedo = it }
        }

        collectFlowOnLifecycleScope {
            documentViewModel.drawingMode.collect { updateDrawingMode(it) }
        }
    }

    private fun updateDrawingMode(drawingMode: AnnotationDrawingMode) {
        annotationsTouchEventDispatcher.drawingMode = drawingMode
        when (drawingMode) {
            // TODO(b/448242937): Revisit touch interception logic;
            // Based on drawingMode, enable/disable touch interception
            is AnnotationDrawingMode.PenMode -> {
                wetStrokesView.setOnTouchListener(wetStrokesViewTouchHandler)
                wetStrokesViewTouchHandler.brushForInking = drawingMode.toInkBrush()
                annotationView.interactionMode = null
            }
            is AnnotationDrawingMode.HighlighterMode -> {
                wetStrokesView.setOnTouchListener(wetStrokesViewTouchHandler)
                wetStrokesViewTouchHandler.brushForInking = drawingMode.toInkBrush()
                annotationView.interactionMode =
                    AnnotationsView.AnnotationMode.Highlight(drawingMode.toHighlighterConfig())
            }
            is AnnotationDrawingMode.EraserMode -> {
                annotationView.interactionMode = AnnotationsView.AnnotationMode.Select()
            }
        }
    }

    private fun collectFlowOnLifecycleScope(block: suspend () -> Unit): Job {
        return viewLifecycleOwner.lifecycleScope.launch {
            /**
             * [repeatOnLifecycle] launches the block in a new coroutine every time the lifecycle is
             * in the STARTED state (or above) and cancels it when it's STOPPED.
             */
            repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
        }
    }

    /**
     * Creates a [android.graphics.Path] that encapsulate [AnnotationToolbar] and set it as a
     * [InProgressStrokesView.maskPath] where no ink should be visible.
     *
     * @return [Path] surrounding [AnnotationToolbar].
     */
    private fun createToolbarMaskPath(): Path {
        val toolbarLocation = IntArray(2)
        annotationToolbar.getLocationOnScreen(toolbarLocation)

        val wetStrokesLocation = IntArray(2)
        wetStrokesView.getLocationOnScreen(wetStrokesLocation)

        val left = (toolbarLocation[0] - wetStrokesLocation[0]).toFloat()
        val top = (toolbarLocation[1] - wetStrokesLocation[1]).toFloat()
        val right = left + annotationToolbar.width
        val bottom = top + annotationToolbar.height

        val cornerRadiusPx = resources.getDimension(R.dimen.annotation_toolbar_corner_radius)

        return Path().apply {
            addRoundRect(
                left,
                top,
                right,
                bottom,
                cornerRadiusPx,
                cornerRadiusPx,
                Path.Direction.CW,
            )
        }
    }

    internal inner class AnnotationsViewTouchEventDispatcher : TouchEventDispatcher {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            return annotationView.dispatchTouchEvent(event)
        }
    }

    internal inner class InkViewTouchEventDispatcher : TouchEventDispatcher {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            return wetStrokesView.dispatchTouchEvent(event)
        }
    }

    internal inner class PdfViewTouchEventDispatcher : TouchEventDispatcher {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            return pdfView.dispatchTouchEvent(event)
        }
    }

    private companion object {
        private const val TOUCH_TOLERANCE_IN_DP = 2f
    }
}
