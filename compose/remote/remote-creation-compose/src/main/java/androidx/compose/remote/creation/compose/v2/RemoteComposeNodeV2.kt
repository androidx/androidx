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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteContentDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSpaced
import androidx.compose.remote.creation.compose.layout.encode
import androidx.compose.remote.creation.compose.layout.find
import androidx.compose.remote.creation.compose.layout.toImageScalingInt
import androidx.compose.remote.creation.compose.modifier.DrawWithContentModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach

internal abstract class RemoteComposeNodeV2 {
    val children = mutableListOf<RemoteComposeNodeV2>()
    var modifier: RemoteModifier = RemoteModifier

    abstract fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas)

    fun renderChildren(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val drawWithContent = modifier.find<DrawWithContentModifier>()

        if (drawWithContent != null) {
            val drawWithContentScope = RemoteContentDrawScope(remoteCanvas)

            creationState.document.startCanvasOperations()
            drawWithContent.onDraw(drawWithContentScope)
            creationState.document.endCanvasOperations()
        }

        children.fastForEach { it.render(creationState, remoteCanvas) }
    }
}

internal class RemoteCanvasNodeV2 : RemoteComposeNodeV2() {
    var onDraw: (RemoteDrawScope.() -> Unit) = {}

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {

        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startCanvas(recordingModifier)

        val drawWithContent = modifier.find<DrawWithContentModifier>()

        if (drawWithContent != null) {
            val drawWithContentScope = RemoteContentDrawScope(remoteCanvas, onDraw)

            // Draw any drawWithContentModifier, around canvas onDraw
            drawWithContent.onDraw(drawWithContentScope)
        } else {
            val drawScope = RemoteDrawScope(remoteCanvas)
            drawScope.onDraw()
        }

        creationState.document.endCanvas()
    }
}

internal class RemoteRootNodeV2 : RemoteComposeNodeV2() {
    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        creationState.document.root { renderChildren(creationState, remoteCanvas) }
    }
}

internal class RemoteBoxNodeV2 : RemoteComposeNodeV2() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startBox(
            recordingModifier,
            horizontalAlignment.toRemote(creationState.layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endBox()
    }
}

internal class RemoteRowNodeV2 : RemoteComposeNodeV2() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startRow(
            recordingModifier,
            horizontalArrangement.toRemote(creationState.layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endRow()
    }
}

internal class RemoteFlowRowNodeV2 : RemoteComposeNodeV2() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startFlow(
            recordingModifier,
            horizontalArrangement.toRemote(creationState.layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endFlow()
    }
}

internal class RemoteColumnNodeV2 : RemoteComposeNodeV2() {
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (verticalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startColumn(
            recordingModifier,
            horizontalAlignment.toRemote(creationState.layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endColumn()
    }
}

internal class RemoteStateLayoutNodeV2 : RemoteComposeNodeV2() {
    lateinit var currentState: RemoteInt

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startStateLayout(
            recordingModifier,
            currentState.getIdForCreationState(creationState),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endStateLayout()
    }
}

internal class RemoteFitBoxNodeV2 : RemoteComposeNodeV2() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        creationState.document.startFitBox(
            recordingModifier,
            horizontalAlignment.toRemote(creationState.layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endFitBox()
    }
}

internal class RemoteCollapsibleColumnNodeV2 : RemoteComposeNodeV2() {
    var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start
    var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (verticalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startCollapsibleColumn(
            recordingModifier,
            horizontalAlignment.toRemote(creationState.layoutDirection),
            verticalArrangement.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endCollapsibleColumn()
    }
}

internal class RemoteCollapsibleRowNodeV2 : RemoteComposeNodeV2() {
    var horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start
    var verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)
        (horizontalArrangement as? RemoteSpaced)?.let {
            recordingModifier.spacedBy(it.space.getFloatIdForCreationState(creationState))
        }
        creationState.document.startCollapsibleRow(
            recordingModifier,
            horizontalArrangement.toRemote(creationState.layoutDirection),
            verticalAlignment.toRemote(),
        )
        renderChildren(creationState, remoteCanvas)
        creationState.document.endCollapsibleRow()
    }
}

internal class RemoteTextNodeV2 : RemoteComposeNodeV2() {
    lateinit var text: RemoteString
    lateinit var color: RemoteColor
    var fontSize: RemoteFloat = 14f.rf
    var fontWeight: RemoteFloat = 400f.rf
    var fontStyle: FontStyle = FontStyle.Normal
    var fontFamily: String? = null
    var textAlign: TextAlign = TextAlign.Start
    var overflow: TextOverflow = TextOverflow.Clip
    var maxLines: Int = Int.MAX_VALUE
    var minFontSize: Float? = null
    var maxFontSize: Float? = null
    var letterSpacing: RemoteFloat = 0f.rf
    var lineHeightAdd: Float? = null
    var lineHeightMultiply: RemoteFloat = 1f.rf
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

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val useCoreTextComponent =
            creationState.profile.supportedOperations.contains(Operations.CORE_TEXT)

        if (useCoreTextComponent) {
            val textIdValue = text.getIdForCreationState(creationState)

            val colorInt = color.constantValueOrNull?.toArgb() ?: Color.Black.toArgb()
            val colorId =
                if (!color.hasConstantValue) {
                    color.getIdForCreationState(creationState)
                } else {
                    -1
                }

            val (fontAxisNames, fontAxisValues) =
                extractFontSettings(fontVariationSettings?.settings)

            val fontSizePx = fontSize.getFloatIdForCreationState(creationState)
            val letterSpacingId = letterSpacing.getFloatIdForCreationState(creationState)
            val lineHeightMultiplyId = lineHeightMultiply.getFloatIdForCreationState(creationState)

            creationState.document.startTextComponent(
                with(modifier) { creationState.toRecordingModifier() },
                textIdValue,
                -1,
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
                letterSpacingId,
                lineHeightAdd ?: 0f,
                lineHeightMultiplyId,
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

            val colorValue =
                color.constantValueOrNull?.toArgb() ?: color.getIdForCreationState(creationState)

            val flags =
                if (color.hasConstantValue) {
                    0.toShort()
                } else {
                    TextLayout.FLAG_IS_DYNAMIC_COLOR.toShort()
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

internal class RemoteImageNodeV2 : RemoteComposeNodeV2() {
    var image: Any? = null
    var remoteBitmap: RemoteBitmap? = null
    var contentScale: ContentScale = ContentScale.Fit
    var alpha: RemoteFloat = RemoteFloat(1f)
    var contentDescription: RemoteString? = null

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val bitmapId =
            remoteBitmap?.getIdForCreationState(creationState)
                ?: image?.let { creationState.document.addBitmap(it) }
                ?: 0
        creationState.document.image(
            creationState.toRecordingModifier(modifier),
            bitmapId,
            contentScale.toImageScalingInt(),
            alpha.getFloatIdForCreationState(creationState),
        )
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
