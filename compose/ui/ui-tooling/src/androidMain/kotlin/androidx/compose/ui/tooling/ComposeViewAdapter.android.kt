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

package androidx.compose.ui.tooling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.PreviewAnimationClock
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.NodeGroup
import androidx.compose.ui.tooling.data.SourceLocation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntRect
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.reflect.Method

private const val TOOLS_NS_URI = "http://schemas.android.com/tools"
private const val DESIGN_INFO_METHOD = "getDesignInfo"

private const val REMEMBER = "remember"

private val emptyContent: @Composable () -> Unit = @Composable {}

/**
 * Class containing the minimum information needed by the Preview to map components to the
 * source code and render boundaries.
 */
@OptIn(UiToolingDataApi::class)
internal data class ViewInfo(
    val fileName: String,
    val lineNumber: Int,
    val bounds: IntRect,
    val location: SourceLocation?,
    val children: List<ViewInfo>,
    val layoutInfo: Any?
) {
    fun hasBounds(): Boolean = bounds.bottom != 0 && bounds.right != 0

    fun allChildren(): List<ViewInfo> =
        children + children.flatMap { it.allChildren() }

    override fun toString(): String =
        """($fileName:$lineNumber,
            |bounds=(top=${bounds.top}, left=${bounds.left},
            |location=${location?.let { "(${it.offset}L${it.length}" } ?: "<none>"}
            |bottom=${bounds.bottom}, right=${bounds.right}),
            |childrenCount=${children.size})""".trimMargin()
}

/**
 * View adapter that renders a `@Composable`. The `@Composable` is found by
 * reading the `tools:composableName` attribute that contains the FQN. Additional attributes can
 * be used to customize the behaviour of this view:
 *  - `tools:parameterProviderClass`: FQN of the [PreviewParameterProvider] to be instantiated by
 *  the [ComposeViewAdapter] that will be used as source for the `@Composable` parameters.
 *  - `tools:parameterProviderIndex`: The index within the [PreviewParameterProvider] of the
 *  value to be used in this particular instance.
 *  - `tools:paintBounds`: If true, the component boundaries will be painted. This is only meant
 *  for debugging purposes.
 *  - `tools:printViewInfos`: If true, the [ComposeViewAdapter] will log the tree of [ViewInfo]
 *  to logcat for debugging.
 *  - `tools:animationClockStartTime`: When set, a [PreviewAnimationClock] will control the
 *  animations in the [ComposeViewAdapter] context.
 *
 */
@Suppress("unused")
@OptIn(UiToolingDataApi::class)
internal class ComposeViewAdapter : FrameLayout {
    private val TAG = "ComposeViewAdapter"

    /**
     * [ComposeView] that will contain the [Composable] to preview.
     */
    private val composeView = ComposeView(context)

    /**
     * When enabled, generate and cache [ViewInfo] tree that can be inspected by the Preview
     * to map components to source code.
     */
    private var debugViewInfos = false

    /**
     * When enabled, paint the boundaries generated by layout nodes.
     */
    private var debugPaintBounds = false
    internal var viewInfos: List<ViewInfo> = emptyList()
    internal var designInfoList: List<String> = emptyList()
    private val slotTableRecord = CompositionDataRecord.create()

    /**
     * Simple function name of the Composable being previewed.
     */
    private var composableName = ""

    /**
     * Whether the current Composable has animations.
     */
    private var hasAnimations = false

    /**
     * Saved exception from the last composition. Since we can not handle the exception during the
     * composition, we save it and throw it during onLayout, this allows Studio to catch it and
     * display it to the user.
     */
    private val delayedException = ThreadSafeException()

    /**
     * The [Composable] to be rendered in the preview. It is initialized when this adapter
     * is initialized.
     */
    private var previewComposition: @Composable () -> Unit = {}

    // Note: the constant emptyContent below instead of a literal {} works around
    // https://youtrack.jetbrains.com/issue/KT-17467, which causes the compiler to emit classes
    // named `content` and `Content` (from the Content method's composable update scope)
    // which causes compilation problems on case-insensitive filesystems.
    @Suppress("RemoveExplicitTypeArguments")
    private val content = mutableStateOf<@Composable () -> Unit>(emptyContent)

    /**
     * When true, the composition will be immediately invalidated after being drawn. This will
     * force it to be recomposed on the next render. This is useful for live literals so the
     * whole composition happens again on the next render.
     */
    private var forceCompositionInvalidation = false

    /**
     * When true, the adapter will try to look objects that support the call
     * [DESIGN_INFO_METHOD] within the slot table and populate [designInfoList]. Used to
     * support rendering in Studio.
     */
    private var lookForDesignInfoProviders = false

    /**
     * An additional [String] argument that will be passed to objects that support the
     * [DESIGN_INFO_METHOD] call. Meant to be used by studio to as a way to request additional
     * information from the Preview.
     */
    private var designInfoProvidersArgument: String = ""

    /**
     * Callback invoked when onDraw has been called.
     */
    private var onDraw = {}

    internal var stitchTrees = true

    private val debugBoundsPaint = Paint().apply {
        pathEffect = DashPathEffect(floatArrayOf(5f, 10f, 15f, 20f), 0f)
        style = Paint.Style.STROKE
        color = Color.Red.toArgb()
    }

    private var composition: Composition? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private val Group.fileName: String
        get() = location?.sourceFile ?: ""

    private val Group.lineNumber: Int
        get() = location?.lineNumber ?: -1

    /**
     * Returns true if this [Group] has no source position information
     */
    private fun Group.hasNullSourcePosition(): Boolean =
        fileName.isEmpty() && lineNumber == -1

    /**
     * Returns true if this [Group] has no source position information and no children
     */
    private fun Group.isNullGroup(): Boolean =
        hasNullSourcePosition() &&
            children.isEmpty() &&
            ((this as? NodeGroup)?.node as? LayoutInfo) == null

    private fun Group.toViewInfo(): ViewInfo {
        val layoutInfo = ((this as? NodeGroup)?.node as? LayoutInfo)

        if (children.size == 1 &&
            hasNullSourcePosition() &&
            layoutInfo == null) {
            // There is no useful information in this intermediate node, remove.
            return children.single().toViewInfo()
        }

        val childrenViewInfo = children
            .filter { !it.isNullGroup() }
            .map { it.toViewInfo() }

        // TODO: Use group names instead of indexing once it's supported
        return ViewInfo(
            location?.sourceFile ?: "",
            location?.lineNumber ?: -1,
            box,
            location,
            childrenViewInfo,
            layoutInfo
        )
    }

    /**
     * Processes the recorded slot table and re-generates the [viewInfos] attribute.
     */
    private fun processViewInfos() {
        val newViewInfos = slotTableRecord
            .store
            .map { it.asTree().toViewInfo() }
            .toList()

        viewInfos = if (stitchTrees)
            stitchTrees(newViewInfos)
        else newViewInfos

        if (debugViewInfos) {
            val debugString = viewInfos.toDebugString()
            Log.d(TAG, debugString)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // If there was a pending exception then throw it here since Studio will catch it and show
        // it to the user.
        delayedException.throwIfPresent()

        processViewInfos()
        if (composableName.isNotEmpty()) {
            findAndTrackAnimations()
            if (lookForDesignInfoProviders) {
                findDesignInfoProviders()
            }
        }
    }

    override fun onAttachedToWindow() {
        composeView.rootView.setViewTreeLifecycleOwner(FakeSavedStateRegistryOwner)
        super.onAttachedToWindow()
    }

    /**
     * Finds all animations defined in the Compose tree where the root is the
     * `@Composable` being previewed.
     */
    private fun findAndTrackAnimations() {
        val slotTrees = slotTableRecord.store.map { it.asTree() }
        val isAnimationPreview = ::clock.isInitialized
        AnimationSearch(::clock, ::requestLayout).let {
            hasAnimations = it.searchAny(slotTrees)
            if (isAnimationPreview && hasAnimations) {
                it.attachAllAnimations(slotTrees)
            }
        }
    }

    /**
     * Find all data objects within the slotTree that can invoke '[DESIGN_INFO_METHOD]', and store
     * their result in [designInfoList].
     */
    private fun findDesignInfoProviders() {
        val slotTrees = slotTableRecord.store.map { it.asTree() }

        designInfoList = slotTrees.flatMap { rootGroup ->
            rootGroup.findAll { group ->
                (group.name != REMEMBER && group.hasDesignInfo()) || group.children.any { child ->
                    child.name == REMEMBER && child.hasDesignInfo()
                }
            }.mapNotNull { group ->
                // Get the DesignInfoProviders from the group or one of its children
                group.getDesignInfoOrNull(group.box)
                    ?: group.children.firstNotNullOfOrNull { it.getDesignInfoOrNull(group.box) }
            }
        }
    }

    private fun Group.hasDesignInfo(): Boolean =
        data.any { it?.getDesignInfoMethodOrNull() != null }

    private fun Group.getDesignInfoOrNull(box: IntRect): String? =
        data.firstNotNullOfOrNull { it?.invokeGetDesignInfo(box.left, box.right) }

    /**
     * Check if the object supports the method call for [DESIGN_INFO_METHOD], which is expected
     * to take two Integer arguments for coordinates and a String for additional encoded
     * arguments that may be provided from Studio.
     */
    private fun Any.getDesignInfoMethodOrNull(): Method? {
        return try {
            javaClass.getDeclaredMethod(
                DESIGN_INFO_METHOD,
                Integer.TYPE,
                Integer.TYPE,
                String::class.java
            )
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    @Suppress("BanUncheckedReflection")
    private fun Any.invokeGetDesignInfo(x: Int, y: Int): String? {
        return this.getDesignInfoMethodOrNull()?.let { designInfoMethod ->
            try {
                // Workaround for unchecked Method.invoke
                val result = designInfoMethod.invoke(
                    this,
                    x,
                    y,
                    designInfoProvidersArgument
                )
                (result as String).ifEmpty { null }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun invalidateComposition() {
        // Invalidate the full composition by setting it to empty and back to the actual value
        content.value = {}
        content.value = previewComposition
        // Invalidate the state of the view so it gets redrawn
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (forceCompositionInvalidation) invalidateComposition()

        onDraw()
        if (!debugPaintBounds) {
            return
        }

        viewInfos
            .flatMap { listOf(it) + it.allChildren() }
            .forEach {
                if (it.hasBounds()) {
                    canvas.apply {
                        val pxBounds = android.graphics.Rect(
                            it.bounds.left,
                            it.bounds.top,
                            it.bounds.right,
                            it.bounds.bottom
                        )
                        drawRect(pxBounds, debugBoundsPaint)
                    }
                }
            }
    }

    /**
     * Clock that controls the animations defined in the context of this [ComposeViewAdapter].
     */
    @VisibleForTesting
    internal lateinit var clock: PreviewAnimationClock

    /**
     * Wraps a given [Preview] method an does any necessary setup.
     */
    @Composable
    private fun WrapPreview(content: @Composable () -> Unit) {
        // We need to replace the FontResourceLoader to avoid using ResourcesCompat.
        // ResourcesCompat can not load fonts within Layoutlib and, since Layoutlib always runs
        // the latest version, we do not need it.
        @Suppress("DEPRECATION")
        CompositionLocalProvider(
            LocalFontLoader provides LayoutlibFontResourceLoader(context),
            LocalFontFamilyResolver provides createFontFamilyResolver(context),
            LocalOnBackPressedDispatcherOwner provides FakeOnBackPressedDispatcherOwner,
            LocalActivityResultRegistryOwner provides FakeActivityResultRegistryOwner,
        ) {
            Inspectable(slotTableRecord, content)
        }
    }

    /**
     * Initializes the adapter and populates it with the given [Preview] composable.
     * @param className name of the class containing the preview function
     * @param methodName `@Preview` method name
     * @param parameterProvider [Class] for the [PreviewParameterProvider] to be used as
     * parameter input for this call. If null, no parameters will be passed to the composable.
     * @param parameterProviderIndex when [parameterProvider] is not null, this index will
     * reference the element in the [Sequence] to be used as parameter.
     * @param debugPaintBounds if true, the view will paint the boundaries around the layout
     * elements.
     * @param debugViewInfos if true, it will generate the [ViewInfo] structures and will log it.
     * @param animationClockStartTime if positive, [clock] will be defined and will control the
     * animations defined in the context of the `@Composable` being previewed.
     * @param forceCompositionInvalidation if true, the composition will be invalidated on every
     * draw, forcing it to recompose on next render.
     * @param lookForDesignInfoProviders if true, it will try to populate [designInfoList].
     * @param designInfoProvidersArgument String to use as an argument when populating
     * [designInfoList].
     * @param onCommit callback invoked after every commit of the preview composable.
     * @param onDraw callback invoked after every draw of the adapter. Only for test use.
     */
    @Suppress("DEPRECATION")
    @OptIn(ExperimentalComposeUiApi::class)
    @VisibleForTesting
    internal fun init(
        className: String,
        methodName: String,
        parameterProvider: Class<out PreviewParameterProvider<*>>? = null,
        parameterProviderIndex: Int = 0,
        debugPaintBounds: Boolean = false,
        debugViewInfos: Boolean = false,
        animationClockStartTime: Long = -1,
        forceCompositionInvalidation: Boolean = false,
        lookForDesignInfoProviders: Boolean = false,
        designInfoProvidersArgument: String? = null,
        onCommit: () -> Unit = {},
        onDraw: () -> Unit = {}
    ) {
        this.debugPaintBounds = debugPaintBounds
        this.debugViewInfos = debugViewInfos
        this.composableName = methodName
        this.forceCompositionInvalidation = forceCompositionInvalidation
        this.lookForDesignInfoProviders = lookForDesignInfoProviders
        this.designInfoProvidersArgument = designInfoProvidersArgument ?: ""
        this.onDraw = onDraw

        previewComposition = @Composable {
            SideEffect(onCommit)

            WrapPreview {
                val composer = currentComposer
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                val composable = {
                    try {
                        ComposableInvoker.invokeComposable(
                            className,
                            methodName,
                            composer,
                            *getPreviewProviderParameters(parameterProvider, parameterProviderIndex)
                        )
                    } catch (t: Throwable) {
                        // If there is an exception, store it for later but do not catch it so
                        // compose can handle it and dispose correctly.
                        var exception: Throwable = t
                        // Find the root cause and use that for the delayedException.
                        while (exception is ReflectiveOperationException) {
                            exception = exception.cause ?: break
                        }
                        delayedException.set(exception)
                        throw t
                    }
                }
                if (animationClockStartTime >= 0) {
                    // When animation inspection is enabled, i.e. when a valid (non-negative)
                    // `animationClockStartTime` is passed, set the Preview Animation Clock. This
                    // clock will control the animations defined in this `ComposeViewAdapter`
                    // from Android Studio.
                    clock = PreviewAnimationClock {
                        // Invalidate the descendants of this ComposeViewAdapter's only grandchild
                        // (an AndroidOwner) when setting the clock time to make sure the Compose
                        // Preview will animate when the states are read inside the draw scope.
                        val composeView = getChildAt(0) as ComposeView
                        (composeView.getChildAt(0) as? ViewRootForTest)
                            ?.invalidateDescendants()
                        // Send pending apply notifications to ensure the animation duration will
                        // be read in the correct frame.
                        Snapshot.sendApplyNotifications()
                    }
                }
                composable()
            }
        }
        composeView.setContent(previewComposition)
        invalidate()
    }

    /**
     * Disposes the Compose elements allocated during [init]
     */
    internal fun dispose() {
        composeView.disposeComposition()
        if (::clock.isInitialized) {
            clock.dispose()
        }
        FakeSavedStateRegistryOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        FakeViewModelStoreOwner.viewModelStore.clear()
    }

    /**
     *  Returns whether this `@Composable` has animations. This allows Android Studio to decide if
     *  the Animation Inspector icon should be displayed for this preview. The reason for using a
     *  method instead of the property directly is we use Java reflection to call it from Android
     *  Studio, and to find the property we'd need to filter the method names using `contains`
     *  instead of `equals`.
     */
    fun hasAnimations() = hasAnimations

    private fun init(attrs: AttributeSet) {
        // ComposeView and lifecycle initialization
        setViewTreeLifecycleOwner(FakeSavedStateRegistryOwner)
        setViewTreeSavedStateRegistryOwner(FakeSavedStateRegistryOwner)
        setViewTreeViewModelStoreOwner(FakeViewModelStoreOwner)
        addView(composeView)

        val composableName = attrs.getAttributeValue(TOOLS_NS_URI, "composableName") ?: return
        val className = composableName.substringBeforeLast('.')
        val methodName = composableName.substringAfterLast('.')
        val parameterProviderIndex = attrs.getAttributeIntValue(
            TOOLS_NS_URI,
            "parameterProviderIndex", 0
        )
        val parameterProviderClass = attrs.getAttributeValue(TOOLS_NS_URI, "parameterProviderClass")
            ?.asPreviewProviderClass()

        val animationClockStartTime = try {
            attrs.getAttributeValue(TOOLS_NS_URI, "animationClockStartTime").toLong()
        } catch (e: Exception) {
            -1L
        }

        val forceCompositionInvalidation = attrs.getAttributeBooleanValue(
            TOOLS_NS_URI,
            "forceCompositionInvalidation", false
        )

        init(
            className = className,
            methodName = methodName,
            parameterProvider = parameterProviderClass,
            parameterProviderIndex = parameterProviderIndex,
            debugPaintBounds = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "paintBounds",
                debugPaintBounds
            ),
            debugViewInfos = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "printViewInfos",
                debugViewInfos
            ),
            animationClockStartTime = animationClockStartTime,
            forceCompositionInvalidation = forceCompositionInvalidation,
            lookForDesignInfoProviders = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "findDesignInfoProviders",
                lookForDesignInfoProviders
            ),
            designInfoProvidersArgument = attrs.getAttributeValue(
                TOOLS_NS_URI,
                "designInfoProvidersArgument"
            )
        )
    }

    @SuppressLint("VisibleForTests")
    private val FakeSavedStateRegistryOwner = object : SavedStateRegistryOwner {
        val lifecycleRegistry = LifecycleRegistry.createUnsafe(this)
        private val controller = SavedStateRegistryController.create(this).apply {
            performRestore(Bundle())
        }

        init {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry

        override val lifecycle: LifecycleRegistry
            get() = lifecycleRegistry
    }

    private val FakeViewModelStoreOwner = object : ViewModelStoreOwner {
        private val vmStore = ViewModelStore()

        override val viewModelStore = vmStore
    }

    private val FakeOnBackPressedDispatcherOwner = object : OnBackPressedDispatcherOwner {
        override val onBackPressedDispatcher = OnBackPressedDispatcher()

        override val lifecycle: LifecycleRegistry
            get() = FakeSavedStateRegistryOwner.lifecycleRegistry
    }

    private val FakeActivityResultRegistryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                throw IllegalStateException("Calling launch() is not supported in Preview")
            }
        }
    }
}
