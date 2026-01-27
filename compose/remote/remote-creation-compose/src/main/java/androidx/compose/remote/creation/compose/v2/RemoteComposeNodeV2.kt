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

package androidx.compose.remote.creation.compose.v2

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.createBitmap

internal abstract class RemoteComposeNodeV2 {
    val children = mutableListOf<RemoteComposeNodeV2>()
    var modifier: RemoteModifier = RemoteModifier

    abstract fun render(creationState: RemoteComposeCreationState)

    fun renderChildren(creationState: RemoteComposeCreationState) {
        children.fastForEach { it.render(creationState) }
    }
}

internal class RemoteCanvasNodeV2 : RemoteComposeNodeV2() {
    var onDraw: (RemoteDrawScope.() -> Unit)? = null

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingCanvas =
            RecordingCanvas(createBitmap(1, 1)).apply {
                setRemoteComposeCreationState(creationState)
            }

        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startCanvas(recordingModifier)
        onDraw?.let { drawLambda ->
            val remoteCanvas = RemoteCanvas(recordingCanvas)
            val remoteDrawScope =
                RemoteDrawScope(
                    remoteCanvas = remoteCanvas,
                    // TODO use real value
                    fontScale = 1f.rf,
                    // TODO use real value
                    layoutDirection = LayoutDirection.Ltr,
                )
            remoteDrawScope.drawLambda()
        }
        creationState.document.endCanvas()
    }
}

internal class RemoteRootNodeV2 : RemoteComposeNodeV2() {
    override fun render(creationState: RemoteComposeCreationState) {
        creationState.document.root { renderChildren(creationState) }
    }
}

internal class RemoteBoxNodeV2 : RemoteComposeNodeV2() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startBox(
            recordingModifier,
            horizontalAlignment.toRemote(),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState)
        creationState.document.endBox()
    }
}

internal class RemoteRowNodeV2 : RemoteComposeNodeV2() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startRow(
            recordingModifier,
            horizontalArrangement.toRemote(),
            verticalAlignment.toRemote(),
        )
        renderChildren(creationState)
        creationState.document.endRow()
    }
}

internal class RemoteColumnNodeV2 : RemoteComposeNodeV2() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startColumn(
            recordingModifier,
            horizontalAlignment.toRemote(),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState)
        creationState.document.endColumn()
    }
}

internal class RemoteTextNodeV2 : RemoteComposeNodeV2() {
    lateinit var text: RemoteString
    var color: RemoteColor? = null
    var fontSize: RemoteFloat = 14f.rf
    var fontWeight: RemoteFloat = 400f.rf
    var fontStyle: FontStyle? = null
    var fontFamily: String? = null
    var textAlign: TextAlign? = null
    var overflow: TextOverflow = TextOverflow.Clip
    var maxLines: Int = Int.MAX_VALUE
    var minFontSize: Float? = null
    var maxFontSize: Float? = null
    var letterSpacing: Float? = null
    var lineHeightAdd: Float? = null
    var lineHeightMultiply: Float? = null
    var textDecoration: TextDecoration = TextDecoration.None
    var fontVariationSettings: FontVariation.Settings? = null

    private fun extractFontSettings(
        settings: List<FontVariation.Setting>?
    ): Pair<Array<String>?, FloatArray?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues = FloatArray(size) { settings[it].toVariationValue(null) }

        return Pair(fontAxisNames, fontAxisValues)
    }

    override fun render(creationState: RemoteComposeCreationState) {
        val useCoreTextComponent =
            creationState.profile.supportedOperations.contains(Operations.CORE_TEXT)

        if (useCoreTextComponent) {
            val textIdValue = text.getIdForCreationState(creationState)

            val colorInt = color?.constantValueOrNull?.toArgb() ?: android.graphics.Color.BLACK
            val colorId =
                if (color?.hasConstantValue == false) {
                    color!!.getIdForCreationState(creationState)
                } else {
                    -1
                }

            val (fontAxisNames, fontAxisValues) =
                extractFontSettings(fontVariationSettings?.settings)

            val fontSizePx = fontSize.getFloatIdForCreationState(creationState)

            creationState.document.startTextComponent(
                with(modifier) { creationState.toRecordingModifier() },
                textIdValue,
                colorInt,
                colorId,
                fontSizePx,
                minFontSize ?: -1f,
                maxFontSize ?: -1f,
                fontStyle.encode(),
                fontWeight.getFloatIdForCreationState(creationState),
                fontFamily,
                textAlign.encode(),
                overflow.encode(),
                maxLines,
                letterSpacing ?: 0f,
                lineHeightAdd ?: 0f,
                lineHeightMultiply ?: 1f,
                0, // lineBreakStrategy
                0, // hyphenationFrequency
                0, // justificationMode
                textDecoration.contains(TextDecoration.Underline),
                textDecoration.contains(TextDecoration.LineThrough),
                fontAxisNames,
                fontAxisValues,
                false, // autosize
                0, // flags
            )
            creationState.document.endTextComponent()
        } else {
            val textId = text.getIdForCreationState(creationState)

            val colorInt = color?.constantValueOrNull?.toArgb() ?: android.graphics.Color.BLACK
            val colorId =
                if (color?.hasConstantValue == false) {
                    color!!.getIdForCreationState(creationState)
                } else {
                    -1
                }

            val colorValue =
                color?.constantValueOrNull?.toArgb()
                    ?: (if (color?.hasConstantValue == false) {
                        color!!.getIdForCreationState(creationState)
                    } else {
                        android.graphics.Color.BLACK
                    })
            val flags =
                if (color?.hasConstantValue == false) {
                    TextLayout.FLAG_IS_DYNAMIC_COLOR.toShort()
                } else {
                    0.toShort()
                }

            val fontSizePx = fontSize.getFloatIdForCreationState(creationState)
            creationState.document.startTextComponent(
                with(modifier) { creationState.toRecordingModifier() },
                textId,
                colorValue,
                fontSizePx,
                fontStyle.encode(),
                fontWeight.constantValueOrNull ?: 400f,
                fontFamily,
                flags,
                textAlign.encode().toShort(),
                overflow.encode(),
                maxLines,
            )
            creationState.document.endTextComponent()
        }
    }
}

private fun FontStyle?.encode(): Int =
    when (this) {
        FontStyle.Normal -> 0
        FontStyle.Italic -> 1
        else -> 0
    }

private fun FontFamily?.encode(): String? =
    when (this) {
        null -> null
        FontFamily.Default -> "default"
        FontFamily.SansSerif -> "sans-serif"
        FontFamily.Serif -> "serif"
        FontFamily.Monospace -> "monospace"
        FontFamily.Cursive -> "cursive"
        else -> null
    }

private fun TextAlign?.encode(): Int =
    when (this) {
        TextAlign.Left -> 1
        TextAlign.Right -> 2
        TextAlign.Center -> 3
        TextAlign.Justify -> 4
        TextAlign.Start -> 5
        TextAlign.End -> 6
        else -> 5
    }

private fun TextOverflow.encode(): Int =
    when (this) {
        TextOverflow.Clip -> 0
        TextOverflow.Ellipsis -> 1
        TextOverflow.Visible -> 2
        else -> 0
    }

internal class RemoteImageNodeV2 : RemoteComposeNodeV2() {
    var image: Any? = null
    var remoteBitmap: RemoteBitmap? = null
    var contentScale: ContentScale = ContentScale.Fit
    var alpha: RemoteFloat = RemoteFloat(1f)

    override fun render(creationState: RemoteComposeCreationState) {
        val bitmapId =
            remoteBitmap?.getIdForCreationState(creationState)
                ?: image?.let { creationState.document.addBitmap(it) }
                ?: 0
        creationState.document.image(
            creationState.toRecordingModifier(modifier),
            bitmapId,
            contentScaleToInt(contentScale),
            alpha.getFloatIdForCreationState(creationState),
        )
    }

    private fun contentScaleToInt(scale: ContentScale): Int =
        when (scale) {
            ContentScale.Fit -> 1
            ContentScale.Crop -> 2
            ContentScale.FillBounds -> 3
            ContentScale.FillWidth -> 4
            ContentScale.FillHeight -> 5
            ContentScale.Inside -> 6
            ContentScale.None -> 7
            else -> 1
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNodeV2> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
) {
    ComposeNode<T, RemoteComposeApplierV2>(factory, update)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNodeV2> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @RemoteComposable () -> Unit,
) {
    ComposeNode<T, RemoteComposeApplierV2>(factory, update, content)
}
