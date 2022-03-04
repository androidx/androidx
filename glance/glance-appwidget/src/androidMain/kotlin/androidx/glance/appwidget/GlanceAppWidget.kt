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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Applier
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.min

/**
 * Object handling the composition and the communication with [AppWidgetManager].
 *
 * The UI is defined by the [Content] composable function. Calling [update] will start
 * the composition and translate [Content] into a [RemoteViews] which is then sent to the
 * [AppWidgetManager].
 *
 * @param errorUiLayout If different from 0 and an error occurs within this GlanceAppWidget,
 * the App Widget is updated with an error UI using this layout resource ID.
 */
public abstract class GlanceAppWidget(
    @LayoutRes
    private val errorUiLayout: Int = R.layout.glance_error_layout
) {
    /**
     * Definition of the UI.
     */
    @Composable
    public abstract fun Content()

    /**
     * Defines the handling of sizes.
     */
    public open val sizeMode: SizeMode = SizeMode.Single

    /**
     * Data store for widget data specific to the view.
     */
    public open val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    /**
     * Method called by the framework when an App Widget has been removed from its host.
     *
     * When the method returns, the state associated with the [glanceId] will be deleted.
     */
    public open suspend fun onDelete(context: Context, glanceId: GlanceId) {}

    /**
     * Triggers the composition of [Content] and sends the result to the [AppWidgetManager].
     */
    public suspend fun update(context: Context, glanceId: GlanceId) {
        require(glanceId is AppWidgetId) {
            "The glanceId '$glanceId' is not a valid App Widget glance id"
        }
        update(context, AppWidgetManager.getInstance(context), glanceId.appWidgetId)
    }

    /**
     * Calls [onDelete], then clear local data associated with the [appWidgetId].
     *
     * This is meant to be called when the App Widget instance has been deleted from the host.
     */
    internal suspend fun deleted(context: Context, appWidgetId: Int) {
        val glanceId = AppWidgetId(appWidgetId)
        try {
            onDelete(context, glanceId)
        } catch (cancelled: CancellationException) {
            // Nothing to do here
        } catch (t: Throwable) {
            Log.e(GlanceAppWidgetTag, "Error in user-provided deletion callback", t)
        } finally {
            stateDefinition?.let {
                GlanceState.deleteStore(context, it, createUniqueRemoteUiName(appWidgetId))
            }
        }
    }

    /**
     * Internal version of [update], to be used by the broadcast receiver directly.
     */
    internal suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle? = null,
    ) {
        safeRun(context, appWidgetManager, appWidgetId) {
            val opts = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)!!
            val state = stateDefinition?.let {
                GlanceState.getValue(context, it, createUniqueRemoteUiName(appWidgetId))
            }
            appWidgetManager.updateAppWidget(
                appWidgetId,
                compose(context, appWidgetManager, appWidgetId, state, opts)
            )
        }
    }

    /**
     * Internal method called when a resize event is detected.
     */
    internal suspend fun resize(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle
    ) {
        // Note, on Android S, if the mode is `Responsive`, then all the sizes are specified from
        // the start and we don't need to update the AppWidget when the size changes.
        if (sizeMode is SizeMode.Exact ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && sizeMode is SizeMode.Responsive)
        ) {
            update(context, appWidgetManager, appWidgetId, options)
        }
    }

    // Retrieves the minimum size of an App Widget, as configured by the App Widget provider.
    @VisibleForTesting
    internal fun appWidgetMinSize(
        displayMetrics: DisplayMetrics,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): DpSize {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return DpSize.Zero
        val minWidth = min(
            info.minWidth,
            if (info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) {
                info.minResizeWidth
            } else {
                Int.MAX_VALUE
            }
        )
        val minHeight = min(
            info.minHeight,
            if (info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) {
                info.minResizeHeight
            } else {
                Int.MAX_VALUE
            }
        )
        return DpSize(minWidth.pixelsToDp(displayMetrics), minHeight.pixelsToDp(displayMetrics))
    }

    // Trigger the composition of the View to create the RemoteViews.
    @VisibleForTesting
    internal suspend fun compose(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle
    ): RemoteViews {
        val layoutConfig = LayoutConfiguration.load(context, appWidgetId)
        return try {
            compose(context, appWidgetManager, appWidgetId, state, options, layoutConfig)
        } finally {
            layoutConfig.save()
        }
    }

    @VisibleForTesting
    internal suspend fun compose(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        layoutConfig: LayoutConfiguration,
    ): RemoteViews =
        when (val localSizeMode = this.sizeMode) {
            is SizeMode.Single -> {
                composeForSize(
                    context,
                    appWidgetId,
                    state,
                    options,
                    appWidgetMinSize(
                        context.resources.displayMetrics,
                        appWidgetManager,
                        appWidgetId
                    ),
                    layoutConfig,
                )
            }
            is SizeMode.Exact -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Impl.composeAllSizes(
                        this,
                        context,
                        appWidgetId,
                        state,
                        options,
                        options.extractAllSizes {
                            appWidgetMinSize(
                                context.resources.displayMetrics,
                                appWidgetManager,
                                appWidgetId
                            )
                        },
                        layoutConfig,
                    )
                } else {
                    composeExactMode(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        state,
                        options,
                        layoutConfig,
                    )
                }
            }
            is SizeMode.Responsive -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Impl.composeAllSizes(
                        this,
                        context,
                        appWidgetId,
                        state,
                        options,
                        localSizeMode.sizes,
                        layoutConfig,
                    )
                } else {
                    composeResponsiveMode(
                        context,
                        appWidgetId,
                        state,
                        options,
                        localSizeMode.sizes,
                        layoutConfig,
                    )
                }
            }
        }

    private suspend fun composeExactMode(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        layoutConfig: LayoutConfiguration,
    ) = coroutineScope {
        val views =
            options.extractOrientationSizes()
                .map { size ->
                    async {
                        composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
        combineLandscapeAndPortrait(views) ?: composeForSize(
            context,
            appWidgetId,
            state,
            options,
            appWidgetMinSize(context.resources.displayMetrics, appWidgetManager, appWidgetId),
            layoutConfig,
        )
    }

    // Combine the views, which should be landscape and portrait, in that order, if they are
    // available.
    private fun combineLandscapeAndPortrait(views: List<RemoteViews>): RemoteViews? =
        when (views.size) {
            2 -> RemoteViews(views[0], views[1])
            1 -> views[0]
            0 -> null
            else -> throw IllegalArgumentException("There must be between 0 and 2 views.")
        }

    private suspend fun composeResponsiveMode(
        context: Context,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        sizes: Set<DpSize>,
        layoutConfig: LayoutConfiguration,
    ) = coroutineScope {
        // Find the best view, emulating what Android S+ would do.
        val orderedSizes = sizes.sortedBySize()
        val smallestSize = orderedSizes[0]
        val views =
            options.extractOrientationSizes()
                .map { size ->
                    findBestSize(size, sizes) ?: smallestSize
                }
                .map { size ->
                    async {
                        composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
        combineLandscapeAndPortrait(views) ?: composeForSize(
            context,
            appWidgetId,
            state,
            options,
            smallestSize,
            layoutConfig,
        )
    }

    @VisibleForTesting
    internal suspend fun composeForSize(
        context: Context,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        size: DpSize,
        layoutConfig: LayoutConfiguration,
    ): RemoteViews = withContext(BroadcastFrameClock()) {
        // The maximum depth must be reduced if the compositions are combined
        val root = RemoteViewsRoot(maxDepth = MaxComposeTreeDepth)
        val applier = Applier(root)
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(applier, recomposer)
        val glanceId = AppWidgetId(appWidgetId)
        composition.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalGlanceId provides glanceId,
                LocalAppWidgetOptions provides options,
                LocalState provides state,
                LocalSize provides size,
            ) { Content() }
        }
        launch { recomposer.runRecomposeAndApplyChanges() }
        recomposer.close()
        recomposer.join()

        normalizeCompositionTree(root)

        translateComposition(
            context,
            appWidgetId,
            root,
            layoutConfig,
            layoutConfig.addLayout(root),
            size
        )
    }

    private companion object {
        /**
         * Maximum depth for a composition. Although there is no hard limit, this should avoid deep
         * recursions, which would create [RemoteViews] too large to be sent.
         */
        private const val MaxComposeTreeDepth = 50
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        @DoNotInline
        suspend fun composeAllSizes(
            glance: GlanceAppWidget,
            context: Context,
            appWidgetId: Int,
            state: Any?,
            options: Bundle,
            allSizes: Collection<DpSize>,
            layoutConfig: LayoutConfiguration
        ): RemoteViews = coroutineScope {
            val allViews =
                allSizes.map { size ->
                    async {
                        size.toSizeF() to glance.composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
            allViews.singleOrNull()?.second ?: RemoteViews(allViews.toMap())
        }
    }

    private suspend fun safeRun(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (ex: CancellationException) {
            // Nothing to do
        } catch (throwable: Throwable) {
            if (errorUiLayout == 0) {
                throw throwable
            }
            logException(throwable)
            val rv = RemoteViews(context.packageName, errorUiLayout)
            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }
}

internal fun createUniqueRemoteUiName(appWidgetId: Int) = "appWidget-$appWidgetId"

internal data class AppWidgetId(val appWidgetId: Int) : GlanceId

// Extract the sizes from the bundle
@Suppress("DEPRECATION")
internal fun Bundle.extractAllSizes(minSize: () -> DpSize): List<DpSize> {
    val sizes = getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    return if (sizes.isNullOrEmpty()) {
        estimateSizes(minSize)
    } else {
        sizes.map { DpSize(it.width.dp, it.height.dp) }
    }
}

// If the list of sizes is not available, estimate it from the min/max width and height.
// We can assume that the min width and max height correspond to the portrait mode and the max
// width / min height to the landscape mode.
private fun Bundle.estimateSizes(minSize: () -> DpSize): List<DpSize> {
    val minHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val maxHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val minWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    val maxWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    // If the min / max widths and heights are not specified, fall back to the unique mode,
    // giving the minimum size the app widget may have.
    if (minHeight == 0 || maxHeight == 0 || minWidth == 0 || maxWidth == 0) {
        return listOf(minSize())
    }
    return listOf(DpSize(minWidth.dp, maxHeight.dp), DpSize(maxWidth.dp, minHeight.dp))
}

// Landscape is min height / max width
private fun Bundle.extractLandscapeSize(): DpSize? {
    val minHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val maxWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    return if (minHeight == 0 || maxWidth == 0) null else DpSize(maxWidth.dp, minHeight.dp)
}

// Portrait is max height / min width
private fun Bundle.extractPortraitSize(): DpSize? {
    val maxHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val minWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    return if (maxHeight == 0 || minWidth == 0) null else DpSize(minWidth.dp, maxHeight.dp)
}

private fun Bundle.extractOrientationSizes() =
    listOfNotNull(extractLandscapeSize(), extractPortraitSize())

// True if the object fits in the given size.
private infix fun DpSize.fitsIn(other: DpSize) =
    (ceil(other.width.value) + 1 > width.value) &&
        (ceil(other.height.value) + 1 > height.value)

@VisibleForTesting
internal fun DpSize.toSizeF(): SizeF = SizeF(width.value, height.value)

private fun squareDistance(widgetSize: DpSize, layoutSize: DpSize): Float {
    val dw = widgetSize.width.value - layoutSize.width.value
    val dh = widgetSize.height.value - layoutSize.height.value
    return dw * dw + dh * dh
}

// Find the best size that fits in the available [widgetSize] or null if no layout fits.
@VisibleForTesting
internal fun findBestSize(widgetSize: DpSize, layoutSizes: Collection<DpSize>): DpSize? =
    layoutSizes.mapNotNull { layoutSize ->
        if (layoutSize fitsIn widgetSize) {
            layoutSize to squareDistance(widgetSize, layoutSize)
        } else {
            null
        }
    }.minByOrNull { it.second }?.first

private fun Collection<DpSize>.sortedBySize() =
    sortedWith(compareBy({ it.width.value * it.height.value }, { it.width.value }))

internal fun logException(throwable: Throwable) {
    Log.e(GlanceAppWidgetTag, "Error in Glance App Widget", throwable)
}

/** Update all App Widgets managed by the [GlanceAppWidget] class. */
public suspend fun GlanceAppWidget.updateAll(@Suppress("ContextFirst") context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(javaClass).forEach { update(context, it) }
}

/**
 * Update all App Widgets managed by the [GlanceAppWidget] class, if they fulfill some condition.
 */
public suspend inline fun <reified State> GlanceAppWidget.updateIf(
    @Suppress("ContextFirst") context: Context,
    predicate: (State) -> Boolean
) {
    val stateDef = stateDefinition
    requireNotNull(stateDef) { "GlanceAppWidget.updateIf cannot be used if no state is defined." }
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(javaClass).forEach { glanceId ->
        val state = getAppWidgetState(context, stateDef, glanceId) as State
        if (predicate(state)) update(context, glanceId)
    }
}
