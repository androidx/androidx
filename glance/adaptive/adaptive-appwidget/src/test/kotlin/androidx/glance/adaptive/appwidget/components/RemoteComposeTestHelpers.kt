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

package androidx.glance.adaptive.appwidget.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.MeasureSpec
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.RemoteCapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.glance.adaptive.appwidget.renderer.GlanceAppWidgetProfiles
import kotlin.math.roundToInt

/** A public framework `<vector>` drawable standing in for an app-provided vector glyph in tests. */
internal const val TEST_VECTOR_GLYPH: Int = android.R.drawable.ic_lock_power_off

/**
 * Captures [content] with the public `remote-creation-compose` API and plays the serialized wire
 * bytes back at `[widthDp] x [heightDp]` in Robolectric.
 *
 * Production code in `src/main/` relies exclusively on public `remote-creation-compose` APIs (zero
 * restricted `compose.remote` imports). On-device playback happens inside the Android 17+ launcher
 * framework (`RemoteViews(DrawInstructions)`), which does not exist in JVM Robolectric tests, and
 * `compose.remote`'s JVM player/test modules (`remote-player-*`, `remote-core`, `remote-testing`)
 * are all `@RestrictTo(LIBRARY_GROUP)` — so those player/inspection types are confined strictly to
 * `src/test/` here.
 */
@Suppress("RestrictedApiAndroidX")
@OptIn(ExperimentalRemoteCreationComposeApi::class)
internal suspend fun captureAndRender(
    context: Context,
    widthDp: Int,
    heightDp: Int,
    content: @RemoteComposable @Composable () -> Unit,
): RenderedDocument {
    val widthPx = widthDp.dpToPxInt(context)
    val heightPx = heightDp.dpToPxInt(context)
    val captured =
        captureSingleRemoteDocument(
            context = context,
            creationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(widthPx.toFloat(), heightPx.toFloat()),
                ),
            profile = GlanceAppWidgetProfiles.widgetsV7,
            content = content,
        )

    val player =
        RemoteComposePlayer(context).apply {
            setDocument(RemoteDocument(captured.bytes))
            setUseChoreographer(false)
        }
    player.measure(
        MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY),
    )
    player.layout(0, 0, widthPx, heightPx)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    player.draw(Canvas(bitmap))

    val coreDocument = player.document.document
    val textContext =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    coreDocument.initializeContext(textContext)

    return RenderedDocument(
        captured = captured,
        document = coreDocument,
        root = checkNotNull(coreDocument.rootLayoutComponent),
        textContext = textContext,
        bitmap = bitmap,
    )
}

internal fun Int.dpToPx(context: Context): Float = this * context.resources.displayMetrics.density

internal fun Int.dpToPxInt(context: Context): Int = dpToPx(context).roundToInt()

internal fun Context.withFontScale(fontScale: Float): Context =
    createConfigurationContext(
        Configuration(resources.configuration).apply { this.fontScale = fontScale }
    )

@Suppress("RestrictedApiAndroidX")
internal class RenderedDocument(
    val captured: RemoteCapturedDocument,
    val document: CoreDocument,
    val root: RootLayoutComponent,
    val textContext: AndroidRemoteContext,
    val bitmap: Bitmap,
) {
    fun firstLayoutChild(): LayoutComponent = root.layoutChildren().first()

    fun texts(): List<String> {
        val result = mutableListOf<String>()
        fun collect(ops: List<Operation>) {
            for (op in ops) {
                if (op is CoreText && !op.isGone) {
                    op.textId?.let { id -> textContext.getText(id)?.let(result::add) }
                }
                if (op is Component && !op.isGone) {
                    collect(op.list)
                }
            }
        }
        collect(root.list)
        return result
    }

    fun allOperations(): List<Operation> {
        val result = mutableListOf<Operation>()
        fun collect(ops: List<Operation>) {
            for (op in ops) {
                if (op is Component && op.isGone) continue
                result.add(op)
                if (op is LayoutComponent) {
                    collect(op.componentModifiers.list)
                }
                if (op is Container) {
                    collect(op.list)
                } else if (op is Component) {
                    collect(op.list)
                } else if (op is CanvasOperations) {
                    collect(op.list)
                }
            }
        }
        collect(root.list)
        return result
    }

    inline fun <reified T : Operation> operationsOfType(): List<T> =
        allOperations().filterIsInstance<T>()
}

@Suppress("RestrictedApiAndroidX")
internal fun Component.layoutChildren(): List<LayoutComponent> =
    list.filterIsInstance<LayoutComponent>().filter { !it.isGone }
