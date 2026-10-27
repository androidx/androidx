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

package androidx.compose.remote.player.compose.embedded.inspectable

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableProvider
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBase2
import androidx.compose.remote.core.operations.DrawBase3
import androidx.compose.remote.core.operations.DrawBase4
import androidx.compose.remote.core.operations.DrawBase6
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.util.fastMap

/**
 * Base [InspectableValue] representing a non-component, non-modifier RemoteCompose [Operation]
 * (such as a canvas draw command, [LoopOperation], [ParticlesLoop], [ConditionalOperations],
 * [FloatFunctionDefine], [FloatFunctionCall], or variable definition).
 *
 * Each subclass is a lightweight, disposable wrapper holding only [operation] and [remoteContext]
 * references; all core reads and state lookups are performed lazily when property getters or
 * [inspectableElements] are iterated.
 */
internal sealed class RcOperationInspectable : InspectableValue {
    abstract val operation: Operation
    internal abstract val remoteContext: RemoteContext

    val kind: String
        get() = operation::class.java.simpleName.trimStart('_')

    override val nameFallback: String
        get() = kind

    override val valueOverride: Any
        get() = operation

    companion object {
        fun of(operation: Operation, remoteContext: RemoteContext): RcOperationInspectable =
            when (operation) {
                is LoopOperation -> RcLoopInspectable(operation, remoteContext)
                is ParticlesLoop -> RcParticlesLoopInspectable(operation, remoteContext)
                is ConditionalOperations -> RcConditionalInspectable(operation, remoteContext)
                is FloatFunctionDefine -> RcFunctionDefineInspectable(operation, remoteContext)
                is FloatFunctionCall -> RcFunctionCallInspectable(operation, remoteContext)
                is NamedVariable,
                is FloatConstant,
                is ColorConstant,
                is ColorTheme,
                is FloatExpression,
                is TextFromFloat -> RcVariableInspectable(operation, remoteContext)
                is ImpulseOperation -> RcImpulseInspectable(operation, remoteContext)
                is ShaderData -> RcShaderDataInspectable(operation, remoteContext)
                is PathData,
                is PathCreate,
                is PathTween -> RcPathOperationInspectable(operation, remoteContext)
                is DrawRect,
                is DrawCircle,
                is DrawOval,
                is DrawLine,
                is DrawRoundRect,
                is DrawArc,
                is DrawSector,
                is DrawPath,
                is DrawTweenPath,
                is DrawBitmap,
                is DrawBitmapInt,
                is DrawBitmapScaled,
                is DrawTextAnchored,
                is DrawTextOnPath,
                is DrawTextOnCircle,
                is DrawBitmapFontText,
                is DrawBitmapFontTextOnPath,
                is DrawBitmapTextAnchored,
                is DrawToBitmap,
                is PaintData,
                is ClipRect,
                is ClipPath,
                is MatrixSave,
                is MatrixRestore,
                is MatrixTranslate,
                is MatrixRotate,
                is MatrixScale,
                is MatrixSkew -> RcDrawOperationInspectable(operation, remoteContext)
                else -> RcOtherOperationInspectable(operation, remoteContext)
            }
    }
}

internal data class RcLoopInspectable(
    override val operation: LoopOperation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcLoop"

    val indexVariableId: Int
        get() = operation.indexVariableId

    val from: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.from, remoteContext)

    val until: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.until, remoteContext)

    val step: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.step, remoteContext)

    val estimatedIterations: Int
        get() {
            val f = from
            val u = until
            val s = if (step == 0f) 1f else step
            return if ((s > 0f && u > f) || (s < 0f && u < f)) {
                ((u - f) / s).toInt().coerceAtLeast(0)
            } else {
                0
            }
        }

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("indexVariableId", indexVariableId))
            yield(ValueElement("from", from))
            yield(ValueElement("until", until))
            yield(ValueElement("step", step))
            yield(ValueElement("estimatedIterations", estimatedIterations))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcParticlesLoopInspectable(
    override val operation: ParticlesLoop,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcParticlesLoop"

    val source: ParticlesCreate?
        get() = operation.particlesSource

    val particleCount: Int
        get() = source?.particles?.size ?: 0

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("particleCount", particleCount))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcConditionalInspectable(
    override val operation: ConditionalOperations,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcConditional"

    val typeCode: Int
        get() = operation.type.toInt()

    val type: String
        get() =
            when (typeCode) {
                0 -> "eq"
                1 -> "neq"
                2 -> "lt"
                3 -> "lte"
                4 -> "gt"
                5 -> "gte"
                6 -> "changed"
                else -> "eq"
            }

    val varA: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.varA, remoteContext)

    val varB: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.varB, remoteContext)

    val conditionMet: Boolean
        get() {
            val va = varA
            val vb = varB
            return when (typeCode) {
                0 -> va == vb
                1 -> va != vb
                2 -> va < vb
                3 -> va <= vb
                4 -> va > vb
                5 -> va >= vb
                else -> false
            }
        }

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("type", type))
            yield(ValueElement("varA", varA))
            yield(ValueElement("varB", varB))
            yield(ValueElement("conditionMet", conditionMet))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcFunctionDefineInspectable(
    override val operation: FloatFunctionDefine,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcFunctionDefine"

    val functionId: Int
        get() = operation.id

    val argVarIds: List<Int>
        get() = operation.args.toList()

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("functionId", functionId))
            yield(ValueElement("argVarIds", argVarIds))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcFunctionCallInspectable(
    override val operation: FloatFunctionCall,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcFunctionCall"

    val functionId: Int
        get() = operation.id

    val rawArgs: List<Float>
        get() = operation.args?.toList() ?: emptyList()

    val resolvedArgs: List<Float>
        get() =
            operation.args?.map { RcPlayerInspector.resolveFloatOrRaw(it, remoteContext) }
                ?: emptyList()

    val function: RcFunctionDefineInspectable?
        get() {
            val def =
                operation.function ?: (remoteContext.getObject(functionId) as? FloatFunctionDefine)
            return def?.let { RcFunctionDefineInspectable(it, remoteContext) }
        }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("functionId", functionId))
            yield(ValueElement("args", resolvedArgs))
            yield(ValueElement("function", function))
        }
}

internal data class RcVariableInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcVariable"

    val variableId: Int
        get() =
            when (val op = operation) {
                is NamedVariable -> op.mVarId
                is VariableProvider -> op.id
                else -> -1
            }

    val variableName: String?
        get() = (operation as? NamedVariable)?.mVarName

    val variableType: Int?
        get() = (operation as? NamedVariable)?.mVarType

    val resolvedValue: Any?
        get() {
            val id = variableId
            if (id <= 0) return null
            return when (val op = operation) {
                is NamedVariable ->
                    when (op.mVarType) {
                        NamedVariable.FLOAT_TYPE -> remoteContext.getFloat(id)
                        NamedVariable.INT_TYPE -> remoteContext.getInteger(id)
                        NamedVariable.STRING_TYPE -> remoteContext.getText(id)
                        NamedVariable.COLOR_TYPE -> remoteContext.getColor(id)
                        else -> remoteContext.getFloat(id)
                    }
                is ColorConstant,
                is ColorExpression,
                is ColorTheme -> remoteContext.getColor(id)
                is TextFromFloat -> remoteContext.getText(id)
                is IntegerExpression -> remoteContext.getInteger(id)
                is FloatConstant,
                is FloatExpression -> remoteContext.getFloat(id)
                else -> null
            }
        }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("variableId", variableId))
            variableName?.let { yield(ValueElement("variableName", it)) }
            variableType?.let { yield(ValueElement("variableType", it)) }
            yield(ValueElement("value", resolvedValue))
        }
}

internal data class RcImpulseInspectable(
    override val operation: ImpulseOperation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcImpulse"

    val duration: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.duration, remoteContext)

    val startAt: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.startAt, remoteContext)

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("duration", duration))
            yield(ValueElement("startAt", startAt))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcShaderDataInspectable(
    override val operation: ShaderData,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcShaderData"

    val shaderId: Int
        get() = operation.id

    val uniformFloatNames: List<String>
        get() = operation.uniformFloatNames?.toList() ?: emptyList()

    val uniformIntegerNames: List<String>
        get() = operation.uniformIntegerNames?.toList() ?: emptyList()

    val uniformBitmapNames: List<String>
        get() = operation.uniformBitmapNames?.toList() ?: emptyList()

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("shaderId", shaderId))
            yield(ValueElement("uniformFloatNames", uniformFloatNames))
            yield(ValueElement("uniformIntegerNames", uniformIntegerNames))
            yield(ValueElement("uniformBitmapNames", uniformBitmapNames))
        }
}

internal data class RcPathOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() =
            when (operation) {
                is PathData -> "rcPathData"
                is PathCreate -> "rcPathCreate"
                is PathTween -> "rcPathTween"
                else -> "rcPath"
            }

    val instanceId: Int?
        get() =
            when (val op = operation) {
                is PathData -> op.id
                is PathCreate -> op.id
                else -> null
            }

    val outId: Int?
        get() = (operation as? PathTween)?.id

    val floatPath: FloatArray?
        get() = (operation as? PathData)?.floatPath

    val floatPathLength: Int?
        get() = floatPath?.size

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            instanceId?.let { yield(ValueElement("instanceId", it)) }
            outId?.let { yield(ValueElement("outId", it)) }
            floatPathLength?.let { yield(ValueElement("floatPathLength", it)) }
        }
}

internal data class RcDrawOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    val commandName: String?
        get() =
            when (operation) {
                is PaintData -> "paint"
                is DrawRect -> "drawRect"
                is DrawCircle -> "drawCircle"
                is DrawRoundRect -> "drawRoundRect"
                is DrawLine -> "drawLine"
                is DrawOval -> "drawOval"
                is DrawArc -> "drawArc"
                is DrawSector -> "drawSector"
                is DrawPath -> "drawPath"
                is DrawTweenPath -> "drawTweenPath"
                is DrawBitmap -> "drawBitmap"
                is DrawBitmapInt -> "drawBitmapInt"
                is DrawBitmapScaled -> "drawBitmapScaled"
                is DrawTextAnchored -> "drawTextAnchored"
                is DrawTextOnPath -> "drawTextOnPath"
                is DrawTextOnCircle -> "drawTextOnCircle"
                is DrawBitmapFontText -> "drawBitmapFontText"
                is DrawBitmapFontTextOnPath -> "drawBitmapFontTextOnPath"
                is DrawBitmapTextAnchored -> "drawBitmapTextAnchored"
                is DrawToBitmap -> "drawToBitmap"
                is ClipRect -> "clipRect"
                is ClipPath -> "clipPath"
                is MatrixSave -> "save"
                is MatrixRestore -> "restore"
                is MatrixTranslate -> "translate"
                is MatrixRotate -> "rotate"
                is MatrixScale -> "scale"
                is MatrixSkew -> "skew"
                else -> null
            }

    override val nameFallback: String
        get() = commandName ?: kind

    val textId: Int?
        get() =
            when (val op = operation) {
                is DrawTextAnchored -> op.textID
                is DrawTextOnPath -> op.mTextId
                is DrawTextOnCircle -> op.mTextId
                is DrawBitmapFontText -> op.textID
                is DrawBitmapFontTextOnPath -> op.textID
                is DrawBitmapTextAnchored -> op.textID
                else -> null
            }

    val text: String?
        get() = textId?.let { remoteContext.getText(it) }

    val pathId: Int?
        get() =
            when (val op = operation) {
                is DrawPath -> op.pathId
                is DrawTextOnPath -> op.pathId
                is DrawBitmapFontTextOnPath -> op.pathID
                is ClipPath -> op.id
                else -> null
            }

    val x: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    MaybeVariableFloat(op.x).let {
                        if (it.isUnset) 0f else it.resolve(remoteContext)
                    }
                else -> null
            }

    val y: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    MaybeVariableFloat(op.y).let {
                        if (it.isUnset) 0f else it.resolve(remoteContext)
                    }
                else -> null
            }

    val panX: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    MaybeVariableFloat(op.panX).let {
                        if (it.isUnset) 0f else it.resolve(remoteContext)
                    }
                else -> null
            }

    val panY: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored -> MaybeVariableFloat(op.panY).resolve(remoteContext)
                else -> null
            }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("command", commandName))
            when (val op = operation) {
                is DrawBase2 -> {
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(op.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(op.value2, remoteContext),
                        )
                    )
                }
                is DrawBase3 -> {
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(op.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(op.value2, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v3",
                            RcPlayerInspector.resolveFloatOrRaw(op.value3, remoteContext),
                        )
                    )
                }
                is DrawBase4 -> {
                    yield(
                        ValueElement(
                            "x1",
                            RcPlayerInspector.resolveFloatOrRaw(op.x1Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "y1",
                            RcPlayerInspector.resolveFloatOrRaw(op.y1Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "x2",
                            RcPlayerInspector.resolveFloatOrRaw(op.x2Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "y2",
                            RcPlayerInspector.resolveFloatOrRaw(op.y2Value, remoteContext),
                        )
                    )
                }
                is DrawBase6 -> {
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(op.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(op.value2, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v3",
                            RcPlayerInspector.resolveFloatOrRaw(op.value3, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v4",
                            RcPlayerInspector.resolveFloatOrRaw(op.value4, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v5",
                            RcPlayerInspector.resolveFloatOrRaw(op.value5, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v6",
                            RcPlayerInspector.resolveFloatOrRaw(op.value6, remoteContext),
                        )
                    )
                }
                is DrawPath -> {
                    yield(ValueElement("pathId", op.pathId))
                    yield(ValueElement("start", op.start))
                    yield(ValueElement("end", op.end))
                }
                is DrawTweenPath -> {
                    yield(ValueElement("path1Id", op.path1Id))
                    yield(ValueElement("path2Id", op.path2Id))
                    yield(ValueElement("tween", op.outTween))
                }
                is DrawBitmap -> {
                    yield(ValueElement("imageId", op.imageId))
                    yield(
                        ValueElement(
                            "left",
                            RcPlayerInspector.resolveFloatOrRaw(op.left, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "top",
                            RcPlayerInspector.resolveFloatOrRaw(op.top, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "right",
                            RcPlayerInspector.resolveFloatOrRaw(op.right, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "bottom",
                            RcPlayerInspector.resolveFloatOrRaw(op.bottom, remoteContext),
                        )
                    )
                }
                is DrawBitmapScaled -> {
                    yield(ValueElement("imageId", op.imageId))
                    yield(ValueElement("scaleType", op.scaleType))
                }
                is DrawTextAnchored -> {
                    yield(ValueElement("textId", op.textID))
                    yield(ValueElement("text", text))
                    yield(ValueElement("x", x))
                    yield(ValueElement("y", y))
                    yield(ValueElement("panX", panX))
                    yield(ValueElement("panY", panY))
                    yield(ValueElement("flags", op.flags))
                }
                is DrawTextOnPath -> {
                    yield(ValueElement("textId", op.mTextId))
                    yield(ValueElement("text", text))
                    yield(ValueElement("pathId", op.pathId))
                    yield(
                        ValueElement(
                            "hOffset",
                            RcPlayerInspector.resolveFloatOrRaw(
                                op.hOffset,
                                remoteContext,
                            ),
                        )
                    )
                    yield(
                        ValueElement(
                            "vOffset",
                            RcPlayerInspector.resolveFloatOrRaw(
                                op.vOffset,
                                remoteContext,
                            ),
                        )
                    )
                }
                is ClipPath -> {
                    yield(ValueElement("pathId", op.id))
                    yield(ValueElement("regionOp", op.regionOp))
                }
            }
        }
}

internal data class RcOtherOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("operation", operation))
        }
}
