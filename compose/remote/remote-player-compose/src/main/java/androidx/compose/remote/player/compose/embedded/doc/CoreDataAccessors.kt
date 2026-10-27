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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.doc

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ComponentData
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.FontData
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.SoundData
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import java.util.ArrayList

@Suppress("UNCHECKED_CAST")
internal fun CoreDocument.getOperationsList(): ArrayList<Operation> {
    return operations as ArrayList<Operation>
}

internal inline fun <R> RemoteContext.withOpCountReset(block: () -> R): R {
    val state = mRemoteComposeState as? SnapshotRemoteComposeState
    val isOutermost = state == null || state.opCountDepth == 0
    if (isOutermost) {
        clearLastOpCount()
    }
    if (state != null) state.opCountDepth++
    var primaryException: Throwable? = null
    return try {
        block()
    } catch (t: Throwable) {
        primaryException = t
        throw t
    } finally {
        if (state != null) state.opCountDepth--
        if (isOutermost) {
            try {
                clearLastOpCount()
            } catch (t: Throwable) {
                primaryException?.addSuppressed(t) ?: throw t
            }
        }
    }
}

internal fun applyOperationsWithoutBitmaps(context: RemoteContext, list: List<Operation>) {
    for (i in list.indices) {
        val op = list[i]
        if (op is FloatFunctionDefine || op is LoopOperation) {
            // FloatFunctionDefine and LoopOperation implement Container, but their child operations
            // are function/loop bodies meant to run only when invoked or iterated during draw.
            // Recursing into them or calling LoopOperation.updateVariables/apply during setup would
            // prematurely increment mOpCount and evaluate body operations with uninitialized
            // parameter or loop-index variables.
            op.markNotDirty()
            context.incrementOpCount()
            if (op is FloatFunctionDefine) {
                op.registerListening(context)
            }
            registerNestedResources(context, (op as Container).list)
            continue
        }
        if (op is ImpulseOperation) {
            // An impulse body only runs inside its trigger window, on the draw pass. Applying it
            // here would fire its haptics and sounds when the document loads, so only register the
            // resources it holds.
            op.markNotDirty()
            context.incrementOpCount()
            registerImpulseResources(context, op)
            continue
        }
        if (op is VariableSupport) {
            op.updateVariables(context)
        }
        if (op is Component) {
            op.updateVariables(context)
        }
        op.markNotDirty()
        context.incrementOpCount()
        if (op is BitmapData) {
            // Register metadata object without decoding pixels
            context.putObject(op.id, op)
        } else if (op is Container) {
            if (op is ComponentData) {
                op.apply(context)
            }
            applyOperationsWithoutBitmaps(context, (op as Container).list)
            if (op is LayoutComponent) {
                // A component's draw stream runs in order on every draw pass, after layout, and
                // computed values it defines are resolved on demand through the GraphContext.
                // Evaluating it here, before measurement, would run expressions that read the
                // component's own width/height while they are still zero, fire draw-stream side
                // effects and inflate the op count. Only register the layout-independent
                // resources the draw pass reads from the store but never defines itself.
                op.drawContentOperations?.list?.let { canvasOps ->
                    registerCanvasResources(context, canvasOps)
                }
            }
        } else {
            op.apply(context)
        }
    }
}

/**
 * Registers the static resources nested in a function or loop body without running the body: bitmap
 * metadata (decoded lazily) and text, font, bitmap font and sound data. The draw pass reads these
 * from the store but never applies them itself, so a resource defined in a body would otherwise
 * never be found. Each is applied once per document, however many times the loop would iterate;
 * computed operations (expressions, text formatted from the loop index, sound expressions) are left
 * to the draw pass.
 */
private fun registerNestedResources(context: RemoteContext, list: List<Operation>) {
    for (i in list.indices) {
        val op = list[i]
        when (op) {
            is BitmapData -> context.putObject(op.mImageId, op)
            is TextData,
            is FontData,
            is BitmapFontData,
            is SoundData -> op.apply(context)
            is ImpulseOperation -> {
                registerNestedResources(context, op.list)
                val process = op.process
                if (process != null && !op.list.contains(process)) {
                    registerNestedResources(context, process.list)
                }
            }
            is Container -> {
                registerNestedResources(context, op.list)
                if (op is LayoutComponent) {
                    op.drawContentOperations?.list?.let { canvasOps ->
                        registerNestedResources(context, canvasOps)
                    }
                }
            }
        }
    }
}

/**
 * Registers the resources of a component's canvas draw stream during document setup without
 * evaluating the stream: bitmap metadata (decoded lazily), text/font data and sound data, which the
 * draw pass reads from the store but does not apply itself. Expressions, paint, touch, haptic and
 * particle operations are left for the draw pass, which runs after layout.
 */
private fun registerCanvasResources(context: RemoteContext, list: List<Operation>) {
    for (i in list.indices) {
        val op = list[i]
        when (op) {
            is BitmapData -> context.putObject(op.id, op)
            is TextData,
            is FontData,
            is BitmapFontData -> op.apply(context)
            // Sound resources load once into the document's sound engine (or its preload cache);
            // the draw pass only plays them, so a tone isn't re-synthesized on every frame.
            is SoundData,
            is SoundExpression -> op.apply(context)
            // Function and loop bodies only run when invoked or iterated; see
            // applyOperationsWithoutBitmaps.
            is FloatFunctionDefine,
            is LoopOperation -> registerNestedResources(context, (op as Container).list)
            is ImpulseOperation -> registerImpulseResources(context, op)
            is Container -> registerCanvasResources(context, op.list)
        }
    }
}

/**
 * Registers the resources of an impulse's body, including a trailing ImpulseProcess that
 * registerListening may already have moved out of the impulse's list.
 */
private fun registerImpulseResources(context: RemoteContext, op: ImpulseOperation) {
    registerCanvasResources(context, op.list)
    val process = op.process
    if (process != null && !op.list.contains(process)) {
        registerCanvasResources(context, process.list)
    }
}

/**
 * Fast-path DATA-mode document evaluation that registers variables, updates time, and registers
 * bitmap metadata without running pixel decoding passes.
 */
internal fun CoreDocument.applyDataOperationsWithoutBitmaps(context: RemoteContext) {
    context.withOpCountReset {
        context.mode = RemoteContext.ContextMode.DATA
        try {
            updateTime(context)
            registerVariables(context, getOperationsList())
            applyOperationsWithoutBitmaps(context, operations)
        } finally {
            context.mode = RemoteContext.ContextMode.UNSET
        }
    }
}

internal fun CoreDocument.recollectCollections() {
    val operations = getOperationsList()
    val state = remoteComposeState
    collectCollections(operations, state)
    recollectCanvasCollections(this, operations, state)
}

private fun recollectCanvasCollections(
    document: CoreDocument,
    operations: ArrayList<Operation>,
    state: RemoteComposeState,
) {
    for (i in operations.indices) {
        val op = operations[i]
        if (op is LayoutComponent) {
            op.drawContentOperations?.list?.let { canvasOps ->
                document.collectCollections(canvasOps, state)
                recollectCanvasCollections(document, canvasOps, state)
            }
        }
        if (op is Container) {
            recollectCanvasCollections(document, op.list, state)
        }
    }
}

internal fun RemoteContext.findVariableId(name: String): Int {
    val context = this as? AndroidRemoteContext ?: return -1
    return try {
        context.getVariableId(name)
    } catch (e: NoSuchElementException) {
        -1
    }
}

internal fun PathData.applyTrackingPathChange(context: RemoteContext) {
    val outputPath = getOutputPath()
    if (context.getPathData(getId()) !== outputPath) {
        markPathChanged()
    }
    apply(context)
}
