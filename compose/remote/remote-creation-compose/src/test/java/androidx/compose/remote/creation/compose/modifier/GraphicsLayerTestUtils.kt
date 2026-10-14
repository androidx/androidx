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

package androidx.compose.remote.creation.compose.modifier

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import java.util.HashMap

internal fun recordAndEvaluate(
    density: Float = 2f,
    densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Pixels,
    componentWidth: Float = 100f,
    componentHeight: Float = 80f,
    buildModifier: () -> RemoteModifier,
): EvaluatedGraphicsLayer {
    val capturingBuffer = CapturingRemoteComposeBuffer()
    val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, _ ->
            object :
                RemoteComposeWriterAndroid(
                    profile,
                    capturingBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                    RemoteComposeWriter.hTag(
                        Header.DOC_DENSITY_BEHAVIOR,
                        creationDisplayInfo.densityBehavior,
                    ),
                ) {}
        }

    val creationState =
        RemoteComposeCreationState(
                creationDisplayInfo =
                    RemoteCreationDisplayInfo(
                        width = 500,
                        height = 500,
                        densityDpi = (density * 160f).toInt(),
                        fontScale = 1f,
                        densityBehavior = densityBehavior,
                    ),
                contentDescription = null,
                profile = profile,
            )
            .apply { remoteDensity = RemoteDensity(density.rf, 1f.rf) }

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)))
            mWidth = 500f
            mHeight = 500f
            this.density = density
        }

    creationState.document.root {
        creationState.document.startBox(
            creationState.toRecordingModifier(
                RemoteModifier.width(componentWidth.rf).height(componentHeight.rf)
            ),
            1,
            1,
        )
        creationState.document.startBox(
            creationState.toRecordingModifier(
                RemoteModifier.width(componentWidth.rf)
                    .height(componentHeight.rf)
                    .then(buildModifier())
            ),
            1,
            1,
        )
        creationState.document.endBox()
        creationState.document.endBox()
    }

    val doc =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)
            paint(context, 0)
        }

    val op =
        findGraphicsLayerOp(doc.operations)
            ?: throw AssertionError("GraphicsLayerModifierOperation not found")
    updateVariablesRecursive(doc.operations, context)
    op.paint(context.paintContext!!)
    return EvaluatedGraphicsLayer(op, capturingBuffer.lastGraphicsLayerAttributes, context)
}

internal fun updateVariablesRecursive(
    operations: List<Operation>,
    context: AndroidRemoteContext,
) {
    for (docOp in operations) {
        if (docOp is VariableSupport) {
            docOp.updateVariables(context)
        }
        if (docOp !is Header) {
            docOp.apply(context)
        }
        if (docOp is Container) {
            updateVariablesRecursive(docOp.list, context)
        }
    }
}

internal fun findGraphicsLayerOp(operations: List<Operation>): GraphicsLayerModifierOperation? {
    for (op in operations) {
        if (op is GraphicsLayerModifierOperation) return op
        if (op is ComponentModifiers) {
            val found = op.list.filterIsInstance<GraphicsLayerModifierOperation>().firstOrNull()
            if (found != null) return found
        }
        if (op is Container) {
            val found = findGraphicsLayerOp(op.list)
            if (found != null) return found
        }
    }
    return null
}

internal class CapturingRemoteComposeBuffer : RemoteComposeBuffer() {
    var lastGraphicsLayerAttributes: Map<Int, Any> = emptyMap()

    override fun addModifierGraphicsLayer(attributes: HashMap<Int, Any>) {
        lastGraphicsLayerAttributes = HashMap(attributes)
        super.addModifierGraphicsLayer(attributes)
    }
}

internal data class EvaluatedGraphicsLayer(
    val op: GraphicsLayerModifierOperation,
    val writtenAttributes: Map<Int, Any>,
    val context: AndroidRemoteContext,
)
