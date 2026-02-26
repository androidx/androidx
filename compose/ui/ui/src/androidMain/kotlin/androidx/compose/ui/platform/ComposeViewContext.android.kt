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

package androidx.compose.ui.platform

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LocalHostDefaultProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.R
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.ResourceIdCache
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner

/**
 * [ComposeViewContext] can be used to compose a [ComposeView] while it isn't attached to the view
 * hierarchy. This is useful when prefetching items for a RecyclerView, for example. To use it, call
 * [AbstractComposeView.createComposition] with the [ComposeViewContext] after the content has been
 * set. If the [ComposeView] is never attached to the hierarchy,
 * [AbstractComposeView.disposeComposition] must be called to release resources and stop
 * composition. If the [ComposeView] is attached to the hierarchy, it will stop composition once it
 * has been removed from the hierarchy and calling [AbstractComposeView.disposeComposition] is
 * unnecessary. It will start again if the [ComposeView.setContent] is called again, the View is
 * reattached to the hierarchy, or [AbstractComposeView.createComposition] is called again.
 *
 * The [view] should be attached as long as [ComposeViewContext] is expected to be around.
 *
 * @param view A [View] attached to the same hierarchy as the [ComposeView]s constructed with this
 *   [ComposeViewContext]. This [View] must be attached before calling this constructor.
 * @param compositionContext The [CompositionContext] used by [ComposeView]s constructed with this
 *   [ComposeViewContext]. The default value is obtained from [View.findViewTreeCompositionContext],
 *   or, if not found from the window [androidx.compose.runtime.Recomposer].
 * @param lifecycleOwner Used to govern the lifecycle-important aspects of [ComposeView]s
 *   constructed with this [ComposeViewContext]. The default value is obtained from
 *   [View.findViewTreeLifecycleOwner]. If not found, [IllegalStateException] will be thrown.
 * @param savedStateRegistryOwner The [SavedStateRegistryOwner] used by [ComposeView]s constructed
 *   with this [ComposeViewContext]. The default value is obtained from
 *   [View.findViewTreeSavedStateRegistryOwner]. If not found, an [IllegalStateException] will be
 *   thrown.
 * @param viewModelStoreOwner [ViewModelStoreOwner] to be used by [ComposeView]s to create
 *   [RetainedValuesStore]s. The default value is obtained from
 *   [View.findViewTreeViewModelStoreOwner].
 */
internal class ComposeViewContext(
    internal val view: View,
    internal val compositionContext: CompositionContext =
        view.findViewTreeCompositionContext() ?: view.windowRecomposer,
    internal val lifecycleOwner: LifecycleOwner =
        view.findViewTreeLifecycleOwner()
            ?: throw IllegalStateException(
                "Composed into a View which doesn't propagate ViewTreeLifecycleOwner!"
            ),
    internal val savedStateRegistryOwner: SavedStateRegistryOwner =
        view.findViewTreeSavedStateRegistryOwner()
            ?: throw IllegalStateException(
                "Composed into a View which doesn't propagate ViewTreeSavedStateRegistryOwner!"
            ),
    internal val viewModelStoreOwner: ViewModelStoreOwner? = view.findViewTreeViewModelStoreOwner(),
) {
    /** [ImageVectorCache] provided by [LocalImageVectorCache] */
    internal val imageVectorCache = ImageVectorCache()

    /** [ResourceIdCache] provided by [LocalResourceIdCache] */
    internal val resourceIdCache = ResourceIdCache()

    /**
     * [Configuration] that was last received. Used to determine if there has been an update to the
     * configuration or if we don't have to update the [configuration] instance.
     */
    private val currentConfiguration: Configuration =
        Configuration(view.context.resources.configuration)

    /** [Configuration] provided by [LocalConfiguration] */
    internal val configuration = mutableStateOf(Configuration(currentConfiguration))

    /** [AccessibilityManager] provided by [LocalAccessibilityManager] */
    internal val accessibilityManager = AndroidAccessibilityManager(view.context)

    /** [UriHandler] provided by [LocalUriHandler] */
    internal val uriHandler = AndroidUriHandler(view.context)

    /** [ClipboardManager] provided by [LocalClipboardManager] */
    internal val clipboardManager: AndroidClipboardManager = AndroidClipboardManager(view.context)

    /** [Clipboard] provided by [LocalClipboard] */
    internal val clipboard: AndroidClipboard = AndroidClipboard(clipboardManager)

    /** [ViewConfiguration] provided by [LocalViewConfiguration] */
    internal val viewConfiguration =
        AndroidViewConfiguration(android.view.ViewConfiguration.get(view.context))

    /** [LayoutNodeDrawScope] shared across all [ComposeView]s using this [ComposeViewContext] */
    internal val sharedDrawScope = LayoutNodeDrawScope()

    /** [WindowInfo] provide by [LocalWindowInfo]. */
    internal val windowInfo: LazyWindowInfo = LazyWindowInfo()

    /**
     * A [CanvasHolder] that can be used for all AndroidComposeViews using this
     * [ComposeViewContext].
     */
    internal val canvasHolder = CanvasHolder()

    /**
     * The number of Views that are currently attached to the view hierarchy that are using this
     * ComposeViewContext.
     */
    @get:VisibleForTesting
    internal var viewCount = 0
        private set

    /**
     * Used only for testing to set the window size. The window size is calculated based on platform
     * values that can't be directly modified, so a test value must be set to allow testing window
     * size configuration changes.
     */
    @get:VisibleForTesting internal var testWindowSize: IntSize = IntSize.Zero

    /** Used for recalculating the window size whenever there is a change to the Window. */
    private val calculateWindowSizeLambda = {
        if (testWindowSize == IntSize.Zero) {
            calculateWindowSize(view)
        } else {
            DerivedSize.fromPxSize(testWindowSize, Density(view.context))
        }
    }

    /**
     * A single callback that handles observing configuration changes, memory calls, window focus
     * changes, and [view] attach state changes.
     */
    private val callback =
        object : ComponentCallbacks2, ViewTreeObserver.OnWindowFocusChangeListener {
            override fun onConfigurationChanged(configuration: Configuration) {
                this@ComposeViewContext.onConfigurationChanged(configuration)
            }

            @Deprecated("This callback is superseded by onTrimMemory")
            @Suppress("OVERRIDE_DEPRECATION") // b/446706247
            override fun onLowMemory() {
                imageVectorCache.clear()
                resourceIdCache.clear()
            }

            override fun onTrimMemory(level: Int) {
                imageVectorCache.clear()
                resourceIdCache.clear()
            }

            override fun onWindowFocusChanged(hasFocus: Boolean) {
                windowInfo.isWindowFocused = hasFocus
            }
        }

    /**
     * Called when an AndroidComposeView is attached to the window. This will start observation if
     * it is the first view using this ComposeViewContext and it is created by the ComposeView.
     *
     * @see viewCount
     */
    internal fun incrementViewCount() {
        viewCount++
        if (viewCount == 1) {
            startObserving()
        }
    }

    /**
     * Called when the AndroidComposeView is detached from the window. If this ComposeViewContext
     * was created by ComposeView and it is called by the last AndroidComposeView, observation will
     * be stopped.
     *
     * @see viewCount
     */
    internal fun decrementViewCount() {
        viewCount--
        if (viewCount < 0) {
            Log.e("ComposeViewContext", "View count has dropped below 0")
            viewCount = 0
        }
        if (viewCount == 0) {
            stopObserving()
        }
    }

    /** Start observing configuration changes and window changes. */
    private fun startObserving() {
        view.context.registerComponentCallbacks(callback)
        onConfigurationChanged(view.resources.configuration)
        windowInfo.isWindowFocused = view.hasWindowFocus()
        windowInfo.setOnInitializeContainerSize(calculateWindowSizeLambda)
        windowInfo.updateContainerSizeIfObserved(calculateWindowSizeLambda)
        view.viewTreeObserver.addOnWindowFocusChangeListener(callback)
    }

    /** Stop observing configuration changes and window changes. */
    private fun stopObserving() {
        view.context.unregisterComponentCallbacks(callback)
        windowInfo.setOnInitializeContainerSize(null)
        view.viewTreeObserver.removeOnWindowFocusChangeListener(callback)
    }

    /**
     * Called by [AndroidComposeView]s when it detects configuration changes. Not all configuration
     * changes come through the Activity, so it is important to update through attached
     * AndroidComposeViews also.
     */
    internal fun onConfigurationChanged(configuration: Configuration) {
        val changedFlags = currentConfiguration.updateFrom(configuration)
        if (changedFlags != 0) {
            imageVectorCache.prune(changedFlags)
            this@ComposeViewContext.configuration.value = Configuration(configuration)
            resourceIdCache.clear()
            if (changedFlags and MaskForNonWindowMetricsChanges.inv() != 0) {
                windowInfo.updateContainerSizeIfObserved(calculateWindowSizeLambda)
            }
        }
    }

    /** Provide common CompositionLocals. */
    @OptIn(ExperimentalComposeUiApi::class)
    @Suppress("DEPRECATION")
    @Composable
    internal fun ProvideCompositionLocals(
        owner: AndroidComposeView,
        content: @Composable () -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        val inspectionTable =
            owner.getTag(R.id.inspection_slot_table_set) as? MutableSet<CompositionData>
                ?: (owner.parent as? View)?.getTag(R.id.inspection_slot_table_set)
                    as? MutableSet<CompositionData>
        if (inspectionTable != null) {
            inspectionTable.add(currentComposer.compositionData)
            currentComposer.collectParameterInformation()
        }
        val saveableStateRegistry = remember {
            DisposableSaveableStateRegistry(owner, savedStateRegistryOwner)
        }
        DisposableEffect(Unit) { onDispose { saveableStateRegistry.dispose() } }

        val scrollCaptureInProgress =
            LocalScrollCaptureInProgress.current or owner.scrollCaptureInProgress
        val hostDefaultProvider = remember(owner.view) { ViewTreeHostDefaultProvider(owner.view) }
        @Suppress("UNCHECKED_CAST")
        CompositionLocalProvider(
            LocalLifecycleOwner provides lifecycleOwner,
            LocalSavedStateRegistryOwner provides savedStateRegistryOwner,
            LocalImageVectorCache provides imageVectorCache,
            LocalResourceIdCache provides resourceIdCache,
            LocalContext provides owner.context,
            LocalInspectionTables provides inspectionTable,
            LocalConfiguration provides owner.configuration,
            LocalSaveableStateRegistry provides saveableStateRegistry,
            LocalView provides owner.view,
            LocalProvidableScrollCaptureInProgress provides scrollCaptureInProgress,
            LocalViewConfiguration provides owner.viewConfiguration,
            LocalHostDefaultProvider provides hostDefaultProvider,
        ) {
            ProvideCommonCompositionLocals(owner, uriHandler, content)
        }
    }
}

/**
 * A combined mask of all known configuration changes that do not affect the window metrics.
 *
 * For optimization purposes, any new configuration changes that don't result in the window metrics
 * changes should be added to this list.
 *
 * Order is copied from `ActivityInfo.Config` with the config change types that can affect the
 * window metrics excluded
 */
private const val MaskForNonWindowMetricsChanges =
    ActivityInfo.CONFIG_MCC or
        ActivityInfo.CONFIG_MNC or
        ActivityInfo.CONFIG_LOCALE or
        ActivityInfo.CONFIG_TOUCHSCREEN or
        ActivityInfo.CONFIG_KEYBOARD or
        ActivityInfo.CONFIG_KEYBOARD_HIDDEN or
        ActivityInfo.CONFIG_NAVIGATION or
        ActivityInfo.CONFIG_UI_MODE or
        ActivityInfo.CONFIG_LAYOUT_DIRECTION or
        ActivityInfo.CONFIG_FONT_SCALE or
        ActivityInfo.CONFIG_COLOR_MODE or
        ActivityInfo.CONFIG_GRAMMATICAL_GENDER or
        ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT

// TODO(b/450557132): Add when compileSdk is bumped to 36
//   ActivityInfo.CONFIG_ASSETS_PATHS
