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
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
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

        val recordingModifier = modifier.toRemoteCompose()
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
        renderChildren(creationState)
    }
}

internal class RemoteBoxNodeV2 : RemoteComposeNodeV2() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = modifier.toRemoteCompose()
        creationState.document.startBox(
            recordingModifier,
            horizontalAlignment.toRemoteCompose(),
            verticalArrangement.toRemoteCompose(),
        )
        renderChildren(creationState)
        creationState.document.endBox()
    }
}

internal class RemoteRowNodeV2 : RemoteComposeNodeV2() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = modifier.toRemoteCompose()
        creationState.document.startRow(
            recordingModifier,
            horizontalArrangement.toRemoteCompose(),
            verticalAlignment.toRemoteCompose(),
        )
        renderChildren(creationState)
        creationState.document.endRow()
    }
}

internal class RemoteColumnNodeV2 : RemoteComposeNodeV2() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start

    override fun render(creationState: RemoteComposeCreationState) {
        val recordingModifier = modifier.toRemoteCompose()
        creationState.document.startColumn(
            recordingModifier,
            horizontalAlignment.toRemoteCompose(),
            verticalArrangement.toRemoteCompose(),
        )
        renderChildren(creationState)
        creationState.document.endColumn()
    }
}

internal class RemoteTextNodeV2 : RemoteComposeNodeV2() {
    var text: String? = null
    var remoteText: RemoteString? = null
    var color: RemoteColor? = null
    var fontSize: TextUnit = TextUnit.Unspecified
    var fontWeight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var fontFamily: FontFamily? = null
    var textAlign: TextAlign? = null
    var overflow: TextOverflow = TextOverflow.Clip
    var maxLines: Int = Int.MAX_VALUE
    var textDecoration: TextDecoration = TextDecoration.None
    var fontVariationSettings: FontVariation.Settings? = null

    override fun render(creationState: RemoteComposeCreationState) {
        val textId =
            remoteText?.getIdForCreationState(creationState)
                ?: text?.let { creationState.document.addText(it) }
                ?: 0

        val colorInt = color?.constantValue?.toArgb() ?: android.graphics.Color.BLACK
        val colorId =
            if (color?.hasConstantValue == false) {
                color!!.getIdForCreationState(creationState)
            } else {
                -1
            }

        // density is available in creationState if needed, but for now we continue with simplified
        // logic
        val fontSizePx =
            if (fontSize == TextUnit.Unspecified) {
                14f * creationState.creationDisplayInfo.density
            } else {
                fontSize.value * creationState.creationDisplayInfo.density
            }

        creationState.document.textComponent(
            modifier.toRemoteCompose(),
            textId,
            colorInt,
            colorId,
            fontSizePx,
            -1f, // minFontSize
            -1f, // maxFontSize
            fontStyle.toRemoteCompose(),
            fontWeight?.weight?.toFloat() ?: 400f,
            fontFamily.toRemoteCompose(),
            textAlign.toRemoteCompose(),
            overflow.toRemoteCompose(),
            maxLines,
            0f, // letterSpacing
            0f, // lineHeightAdd
            1f, // lineHeightMultiply
            0, // lineBreakStrategy
            0, // hyphenationFrequency
            0, // justificationMode
            textDecoration.contains(TextDecoration.Underline),
            textDecoration.contains(TextDecoration.LineThrough),
            null, // fontAxis
            null, // fontAxisValues
            false, // autosize
            0, // flags
            { /* content runs children if any, but RemoteTextV2 is a leaf */ },
        )
    }
}

private fun FontStyle?.toRemoteCompose(): Int =
    when (this) {
        FontStyle.Normal -> 0
        FontStyle.Italic -> 1
        else -> 0
    }

private fun FontFamily?.toRemoteCompose(): String? =
    when (this) {
        FontFamily.Default -> "default"
        FontFamily.SansSerif -> "sans-serif"
        FontFamily.Serif -> "serif"
        FontFamily.Monospace -> "monospace"
        FontFamily.Cursive -> "cursive"
        else -> null
    }

private fun TextAlign?.toRemoteCompose(): Int =
    when (this) {
        TextAlign.Left -> 1
        TextAlign.Right -> 2
        TextAlign.Center -> 3
        TextAlign.Justify -> 4
        TextAlign.Start -> 5
        TextAlign.End -> 6
        else -> 5
    }

private fun TextOverflow.toRemoteCompose(): Int =
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
            modifier.toRemoteCompose(),
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
