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

package androidx.glance.adaptive.samples

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.remotecompose.RemoteComposeMatrixRenderer
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.quickactions.QuickActionsMatrixSelector
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.quickactions.QuickActionsTemplate
import androidx.glance.adaptive.core.templates.track.TrackTemplate
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_TAG = "WidgetLivePreview"
private val PreviewStageShape = RoundedCornerShape(24.dp)

/**
 * Representative Home Screen and Lock Screen size breakpoints available in the in-app live preview.
 */
internal enum class WidgetPreviewPreset(
    @StringRes val chipLabelRes: Int,
    @StringRes val surfaceLabelRes: Int,
    val surface: AppWidgetSurface,
    val dimensions: Dimensions,
) {
    Home1x1(
        chipLabelRes = R.string.widget_preview_preset_1x1,
        surfaceLabelRes = R.string.widget_preview_surface_home,
        surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        dimensions = Dimensions(widthDp = 88, heightDp = 88),
    ),
    Home2x2(
        chipLabelRes = R.string.widget_preview_preset_2x2,
        surfaceLabelRes = R.string.widget_preview_surface_home,
        surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        dimensions = Dimensions(widthDp = 176, heightDp = 176),
    ),
    Home4x2(
        chipLabelRes = R.string.widget_preview_preset_4x2,
        surfaceLabelRes = R.string.widget_preview_surface_home,
        surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        dimensions = Dimensions(widthDp = 348, heightDp = 132),
    ),
    Home4x3(
        chipLabelRes = R.string.widget_preview_preset_4x3,
        surfaceLabelRes = R.string.widget_preview_surface_home,
        surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        dimensions = Dimensions(widthDp = 348, heightDp = 264),
    ),
    LockLinear(
        chipLabelRes = R.string.widget_preview_preset_lock_linear,
        surfaceLabelRes = R.string.widget_preview_surface_lock,
        surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
        dimensions = Dimensions(widthDp = 176, heightDp = 24),
    ),
    LockMini(
        chipLabelRes = R.string.widget_preview_preset_lock_mini,
        surfaceLabelRes = R.string.widget_preview_surface_lock,
        surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
        dimensions = Dimensions(widthDp = 56, heightDp = 56),
    ),
}

private sealed interface PreviewRenderState {
    data object Loading : PreviewRenderState

    data class Ready(val remoteViews: RemoteViews, val dimensions: Dimensions) : PreviewRenderState

    data class Error(val message: String?) : PreviewRenderState
}

/**
 * Live on-device preview of a Glance Adaptive widget rendered through the real AppWidget matrix
 * pipeline ([RemoteViewsMatrixRenderer] on all API levels, or [RemoteComposeMatrixRenderer] ->
 * `RemoteViews(DrawInstructions)` on API 37+) and inflated in-place via [RemoteViews.apply].
 *
 * [TrackMatrixSelector], [QuickActionsMatrixSelector], [RemoteViewsMatrixRenderer],
 * [RemoteComposeMatrixRenderer], [AppWidgetSurface.withDimensions], [Dimensions], [MatrixContent],
 * and [androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput.toRemoteViews] are
 * restricted to the library group, so this is not a pattern external apps can copy: the sample uses
 * them only to preview templates and compare renderers side by side inside the demo app.
 */
@Composable
internal fun WidgetLivePreviewSection(
    useCaseKey: String,
    backend: WidgetBackend,
    presets: List<WidgetPreviewPreset>,
    selectedPreset: WidgetPreviewPreset,
    onSelectPreset: (WidgetPreviewPreset) -> Unit,
    buildPayload: () -> GlanceAdaptiveTemplate,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    val renderState by
        produceState<PreviewRenderState>(
            initialValue = PreviewRenderState.Loading,
            key1 =
                PreviewRenderKey(
                    useCaseKey = useCaseKey,
                    backend = backend,
                    preset = selectedPreset,
                    uiMode = configuration.uiMode,
                    fontScale = configuration.fontScale,
                    layoutDirection = configuration.layoutDirection,
                    density = density,
                ),
        ) {
            val appContext = context.applicationContext ?: context
            value =
                try {
                    val remoteViews =
                        withContext(Dispatchers.IO) {
                            renderPreviewRemoteViews(
                                context = appContext,
                                template = buildPayload(),
                                surface = selectedPreset.surface,
                                dimensions = selectedPreset.dimensions,
                                backend = backend,
                            )
                        }
                    PreviewRenderState.Ready(
                        remoteViews = remoteViews,
                        dimensions = selectedPreset.dimensions,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(PREVIEW_TAG, "Failed to render live preview for $useCaseKey", e)
                    PreviewRenderState.Error(e.message)
                }
        }

    var inflationError by remember(renderState) { mutableStateOf<PreviewRenderState.Error?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.widget_preview_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    stringResource(
                        R.string.widget_preview_dimensions_summary,
                        selectedPreset.dimensions.widthDp,
                        selectedPreset.dimensions.heightDp,
                        stringResource(selectedPreset.surfaceLabelRes),
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }

        PreviewStage(
            dimensions = selectedPreset.dimensions,
            state = inflationError ?: renderState,
            onApplyError = { error -> inflationError = PreviewRenderState.Error(error.message) },
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (preset in presets) {
                FilterChip(
                    selected = preset == selectedPreset,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(stringResource(preset.chipLabelRes)) },
                )
            }
        }
    }
}

private data class PreviewRenderKey(
    val useCaseKey: String,
    val backend: WidgetBackend,
    val preset: WidgetPreviewPreset,
    val uiMode: Int,
    val fontScale: Float,
    val layoutDirection: Int,
    val density: Float,
)

@Composable
private fun PreviewStage(
    dimensions: Dimensions,
    state: PreviewRenderState,
    onApplyError: (Exception) -> Unit,
) {
    val startColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val endColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val backdropBrush =
        remember(startColor, endColor) {
            Brush.linearGradient(colors = listOf(startColor, endColor))
        }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(PreviewStageShape)
                .background(backdropBrush)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PreviewStageShape)
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val activeDimensions = (state as? PreviewRenderState.Ready)?.dimensions ?: dimensions
        val availableWidthDp = maxWidth.value
        val scale =
            if (availableWidthDp > 0f) {
                minOf(1f, availableWidthDp / activeDimensions.widthDp.toFloat())
            } else {
                1f
            }
        val scaledWidth = (activeDimensions.widthDp * scale).dp
        val scaledHeight = (activeDimensions.heightDp * scale).dp

        when (state) {
            PreviewRenderState.Loading -> {
                Box(
                    modifier = Modifier.size(scaledWidth, scaledHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            }
            is PreviewRenderState.Error -> {
                Text(
                    text =
                        state.message
                            ?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.widget_preview_error, it) }
                            ?: stringResource(R.string.widget_preview_error_generic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                )
            }
            is PreviewRenderState.Ready -> {
                RemoteViewsPreviewHost(
                    remoteViews = state.remoteViews,
                    dimensions = state.dimensions,
                    scale = scale,
                    onApplyError = onApplyError,
                    modifier = Modifier.size(scaledWidth, scaledHeight),
                )
            }
        }
    }
}

@Composable
private fun RemoteViewsPreviewHost(
    remoteViews: RemoteViews,
    dimensions: Dimensions,
    scale: Float,
    onApplyError: (Exception) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val unscaledWidthPx = (dimensions.widthDp * density).roundToInt().coerceAtLeast(1)
    val unscaledHeightPx = (dimensions.heightDp * density).roundToInt().coerceAtLeast(1)

    AndroidView(
        factory = { ctx -> ScaledPreviewContainer(ctx) },
        update = { hostView ->
            hostView.updateScale(
                widthPx = unscaledWidthPx,
                heightPx = unscaledHeightPx,
                scale = scale,
            )
            if (hostView.appliedRemoteViews !== remoteViews) {
                try {
                    hostView.removeAllViews()
                    val widgetView = remoteViews.apply(hostView.context, hostView)
                    hostView.addView(widgetView)
                    hostView.applyScaleToChild()
                    hostView.appliedRemoteViews = remoteViews
                } catch (e: Exception) {
                    hostView.removeAllViews()
                    hostView.appliedRemoteViews = null
                    Log.w(PREVIEW_TAG, "Failed to inflate live preview", e)
                    onApplyError(e)
                }
            }
        },
        onRelease = { hostView ->
            hostView.removeAllViews()
            hostView.appliedRemoteViews = null
        },
        modifier = modifier,
    )
}

/**
 * Hosts a widget [View] measured at its unscaled target widget bounds ([childWidthPx] x
 * [childHeightPx]) and scaled down in-place via Android view transform properties on narrow
 * screens. Touch events are intercepted without being consumed so widget taps remain inert in the
 * preview (matching launcher picker previews) while vertical scrolling of the parent container
 * continues to work.
 */
private class ScaledPreviewContainer(context: Context) : FrameLayout(context) {
    var appliedRemoteViews: RemoteViews? = null
    private var childWidthPx: Int = 0
    private var childHeightPx: Int = 0
    private var contentScale: Float = 1f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    fun updateScale(widthPx: Int, heightPx: Int, scale: Float) {
        val sizeChanged = childWidthPx != widthPx || childHeightPx != heightPx
        childWidthPx = widthPx
        childHeightPx = heightPx
        contentScale = scale
        applyScaleToChild()
        if (sizeChanged) {
            requestLayout()
        }
    }

    fun applyScaleToChild() {
        if (childCount == 0) return
        getChildAt(0).apply {
            pivotX = 0f
            pivotY = 0f
            scaleX = contentScale
            scaleY = contentScale
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount > 0 && childWidthPx > 0 && childHeightPx > 0) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthPx, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightPx, MeasureSpec.EXACTLY)
            getChildAt(0).measure(childWidthSpec, childHeightSpec)
        }
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount == 0) return
        val child = getChildAt(0)
        child.layout(0, 0, childWidthPx, childHeightPx)
        applyScaleToChild()
    }
}

private suspend fun renderPreviewRemoteViews(
    context: Context,
    template: GlanceAdaptiveTemplate,
    surface: AppWidgetSurface,
    dimensions: Dimensions,
    backend: WidgetBackend,
): RemoteViews {
    val sizedSurface = surface.withDimensions(dimensions)
    val matrix = selectMatrixForPreview(template, sizedSurface)
    val output =
        when (backend) {
            WidgetBackend.RemoteCompose ->
                RemoteComposeMatrixRenderer.render(template, matrix, sizedSurface)
            WidgetBackend.RemoteViews ->
                RemoteViewsMatrixRenderer.render(template, matrix, sizedSurface)
        }
    return output.toRemoteViews(context, dimensions)
}

private fun selectMatrixForPreview(
    template: GlanceAdaptiveTemplate,
    surface: AppWidgetSurface,
): MatrixContent =
    when (template) {
        is TrackTemplate -> TrackMatrixSelector.select(template, surface)
        is QuickActionsTemplate -> QuickActionsMatrixSelector.select(template, surface)
        else ->
            throw IllegalArgumentException(
                "Unsupported template for preview: ${template::class.java.simpleName}"
            )
    }
