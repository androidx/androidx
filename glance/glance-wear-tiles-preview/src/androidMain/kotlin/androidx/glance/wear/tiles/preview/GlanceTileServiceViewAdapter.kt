/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.glance.wear.tiles.preview

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.unit.DpSize
import androidx.core.content.ContextCompat
import androidx.glance.wear.tiles.ExperimentalGlanceWearTilesApi
import androidx.glance.wear.tiles.compose
import androidx.glance.wear.tiles.preview.ComposableInvoker.invokeComposable
import androidx.wear.tiles.TileBuilders
import kotlinx.coroutines.runBlocking
import androidx.wear.tiles.renderer.TileRenderer

private const val TOOLS_NS_URI = "http://schemas.android.com/tools"

/**
 * View adapter that renders a glance `@Composable`. The `@Composable` is found by reading the
 * `tools:composableName` attribute that contains the FQN of the function.
 */
internal class GlanceTileServiceViewAdapter : FrameLayout {

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

    @OptIn(ExperimentalGlanceWearTilesApi::class)
    @Suppress("deprecation") // For backwards compatibility
    internal fun init(
        className: String,
        methodName: String,
    ) {
        val content = @Composable {
            val composer = currentComposer
            invokeComposable(
                className,
                methodName,
                composer)
        }

        val wearTilesComposition = runBlocking {
            compose(
                context = context,
                size = DpSize.Unspecified,
                content = content)
        }
        // As far as GlanceWearTiles.compose accepts no timeleine argument, assume we only support
        // [TimelineMode.SingleEntry]
        val timelineBuilders = androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
        timelineBuilders.addTimelineEntry(
            androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    androidx.wear.tiles.LayoutElementBuilders.Layout.Builder()
                        .setRoot(wearTilesComposition.layout)
                        .build()
                ).build()
        )
        val tile = TileBuilders.Tile.Builder()
            .setTimeline(timelineBuilders.build())
            .build()
        val layout = tile.timeline?.timelineEntries?.get(0)?.layout

        @Suppress("DEPRECATION")
        if (layout != null) {
            val renderer = TileRenderer(
                context,
                layout,
                wearTilesComposition.resources,
                ContextCompat.getMainExecutor(context)
            ) { }
            renderer.inflate(this)?.apply {
                (layoutParams as LayoutParams).gravity = Gravity.CENTER
            }
        }
    }

    private fun init(attrs: AttributeSet) {
        val composableName = attrs.getAttributeValue(TOOLS_NS_URI, "composableName") ?: return
        val className = composableName.substringBeforeLast('.')
        val methodName = composableName.substringAfterLast('.')

        init(className, methodName)
    }
}