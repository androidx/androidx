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

package androidx.glance.wear.tiles

import android.content.Context
import android.util.Log
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.Applier
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.fillMaxSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Object containing the result from composition of [GlanceWearTiles]. */
@ExperimentalGlanceWearTilesApi
@Deprecated("glance-wear-tiles is deprecated and will be removed")
@Suppress("deprecation") // For backwards compatibility.
public class WearTilesCompositionResult(
    public val layout: androidx.wear.tiles.LayoutElementBuilders.LayoutElement,
    public val resources: androidx.wear.tiles.ResourceBuilders.Resources,
)

@ExperimentalGlanceWearTilesApi
/**
 * Triggers the composition of [content] and returns the result.
 *
 * @param context The [Context] to get the resources during glance ui building.
 * @param state Local view state that can be passed to composition through [LocalState].
 * @param size Size of the glance ui to be displayed at.
 * @param content Definition of the UI.
 * @return Composition result containing the glance ui.
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public suspend fun compose(
    context: Context,
    size: DpSize,
    state: Any? = null,
    content: @Composable () -> Unit,
): WearTilesCompositionResult = coroutineScope {
    withContext(BroadcastFrameClock()) {
        val WEAR_TILES_ID = object : GlanceId {}
        val compositionResult =
            composeTileHelper(
                size,
                { state },
                /*timeInterval=*/ null,
                /*glanceId=*/ WEAR_TILES_ID,
                context,
                errorUiLayout(),
                content,
            )
        WearTilesCompositionResult(compositionResult.layout, compositionResult.resources.build())
    }
}

/**
 * Triggers the composition of [content] and returns the result.
 *
 * @param screenSize Size of the glance ui to be displayed at.
 * @param state Local view state that can be passed to composition through [LocalState].
 * @param timeInterval defines the start and end of when this glance ui will be used.
 * @param glanceId an object used to describe the glance view.
 * @param context The [Context] to get the resources during glance ui building.
 * @param errorUiLayout The prebuilt layout to return if the content fails to compose.
 * @param content Definition of the UI.
 * @return Composition result containing the glance ui.
 */
@Suppress("deprecation") // For backwards compatibility.
internal suspend fun composeTileHelper(
    screenSize: DpSize,
    state: suspend () -> Any?,
    timeInterval: TimeInterval?,
    glanceId: GlanceId,
    context: Context,
    errorUiLayout: androidx.wear.tiles.LayoutElementBuilders.LayoutElement?,
    content: @Composable () -> Unit,
): CompositionResult = coroutineScope {
    val root = EmittableBox()
    root.modifier = GlanceModifier.fillMaxSize()
    root.contentAlignment = Alignment.Center
    val applier = Applier(root)
    val recomposer = Recomposer(currentCoroutineContext())
    val composition = Composition(applier, recomposer)
    val currentState = state()
    composition.setContent {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalSize provides screenSize,
            LocalState provides currentState,
            LocalTimeInterval provides timeInterval,
            LocalGlanceId provides glanceId,
            content = content,
        )
    }

    launch { recomposer.runRecomposeAndApplyChanges() }

    recomposer.close()
    recomposer.join()

    normalizeCompositionTree(context, root)

    try {
        translateTopLevelComposition(context, root)
    } catch (ex: CancellationException) {
        throw ex
    } catch (throwable: Throwable) {
        if (errorUiLayout == null) {
            throw throwable
        }
        Log.e(GlanceWearTileTag, throwable.toString())
        CompositionResult(errorUiLayout, androidx.wear.tiles.ResourceBuilders.Resources.Builder())
    }
}
