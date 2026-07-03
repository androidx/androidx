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

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ComponentData
import androidx.compose.remote.core.operations.ConditionalOperations
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
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.TextSubtext
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.LayoutManager
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import java.util.ArrayList
import java.util.HashMap

/*
 * Centralized accessors for the remote-core operation state the embedded player needs to read,
 * encapsulated via Kotlin data classes and extension methods over the core getters
 * (the "*Reflection" names are historical: these used to read package-private fields
 * reflectively before remote-core exposed accessors).
 */

internal fun ClipPath.readData(): ClipPathData {
    return ClipPathData(id = getId(), regionOp = getRegionOp())
}

internal fun DrawTextOnPath.readData(): DrawTextOnPathData {
    return DrawTextOnPathData(
        pathId = getPathId(),
        hOffset = getOutHOffset(),
        vOffset = getOutVOffset(),
    )
}

internal fun DrawTextAnchored.readData(): DrawTextAnchoredData {
    return DrawTextAnchoredData(
        textId = getTextID(),
        x = getOutX(),
        y = getOutY(),
        panX = getOutPanX(),
        panY = getOutPanY(),
        flags = getFlags(),
    )
}

internal fun DrawBitmapInt.readData(): DrawBitmapIntData {
    return DrawBitmapIntData(
        imageId = getImageId(),
        srcLeft = getSrcLeft(),
        srcTop = getSrcTop(),
        srcRight = getSrcRight(),
        srcBottom = getSrcBottom(),
        dstLeft = getDstLeft(),
        dstTop = getDstTop(),
        dstRight = getDstRight(),
        dstBottom = getDstBottom(),
    )
}

internal fun DrawTextOnCircle.readData(): DrawTextOnCircleData {
    return DrawTextOnCircleData(
        centerX = getCenterX(),
        centerY = getCenterY(),
        radius = getRadius(),
        startAngle = getStartAngle(),
        warpRadiusOffset = getWarpRadiusOffset(),
        alignment = getAlignment() as DrawTextOnCircle.Alignment,
        placement = getPlacement() as DrawTextOnCircle.Placement,
    )
}

internal fun DrawBitmapFontText.readData(): DrawBitmapFontTextData {
    return DrawBitmapFontTextData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        start = getStart(),
        end = getEnd(),
        x = getOutX(),
        y = getOutY(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawBitmapFontTextOnPath.readData(): DrawBitmapFontTextOnPathData {
    return DrawBitmapFontTextOnPathData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        pathId = getPathID(),
        start = getStart(),
        end = getEnd(),
        yAdj = getOutYAdj(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawBitmapTextAnchored.readData(): DrawBitmapTextAnchoredData {
    return DrawBitmapTextAnchoredData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        start = getOutStart().toInt(),
        end = getOutEnd().toInt(),
        x = getOutX(),
        y = getOutY(),
        panX = getOutPanX(),
        panY = getOutPanY(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawToBitmap.readData(): DrawToBitmapData {
    return DrawToBitmapData(
        bitmapId = getBitmapId(),
        mode = getMode(),
        color = getColor(),
    )
}

internal fun DrawTweenPath.readData(): DrawTweenPathData {
    return DrawTweenPathData(
        path1Id = getPath1Id(),
        path2Id = getPath2Id(),
        tween = getOutTween(),
        start = getOutStart(),
        stop = getOutStop(),
    )
}

/**
 * The [ParticlesCreate] op a [ParticlesLoop] draws from. Particle *rendering* is bridged to the
 * core implementation (see RcPlayerParticles); the player only needs the source to run the core
 * seeding path once per document.
 */
internal val ParticlesLoop.particlesSourceReflection: ParticlesCreate?
    get() = getParticlesSource()

internal fun ConditionalOperations.readData(
    context: RemoteContext? = null
): ConditionalOperationsData {
    val a =
        if (context != null) {
            val rawA = getVarA()
            if (rawA.isNaN()) context.getFloat(Utils.idFromNan(rawA)) else rawA
        } else {
            getVarAOut()
        }
    val b =
        if (context != null) {
            val rawB = getVarB()
            if (rawB.isNaN()) context.getFloat(Utils.idFromNan(rawB)) else rawB
        } else {
            getVarBOut()
        }
    return ConditionalOperationsData(
        varAOut = a,
        varBOut = b,
        type = getType(),
    )
}

internal fun LoopOperation.readData(context: RemoteContext? = null): LoopOperationData {
    val from =
        if (context != null) {
            val rawFrom = getFrom()
            if (rawFrom.isNaN()) context.getFloat(Utils.idFromNan(rawFrom)) else rawFrom
        } else {
            getFromOut()
        }
    val until =
        if (context != null) {
            val rawUntil = getUntil()
            if (rawUntil.isNaN()) context.getFloat(Utils.idFromNan(rawUntil)) else rawUntil
        } else {
            getUntilOut()
        }
    val step =
        if (context != null) {
            val rawStep = getStep()
            if (rawStep.isNaN()) context.getFloat(Utils.idFromNan(rawStep)) else rawStep
        } else {
            getStepOut()
        }
    return LoopOperationData(
        fromOut = from,
        untilOut = until,
        stepOut = step,
        indexVariableId = getIndexVariableId(),
    )
}

internal fun FloatFunctionCall.readData(): FloatFunctionCallData {
    return FloatFunctionCallData(
        function = getFunction(),
        outArgs = getOutArgs(),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Custom.readData(): CustomData {
    return CustomData(
        config = getConfig(),
        configId = getConfigId(),
        properties = getProperties(),
    )
}

internal val Custom.CustomProperty.typeNameReflection: String
    get() = getTypeName()

// 1. CoreDocument Helpers
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CoreDocument.getOperationsReflection(): ArrayList<Operation> {
    return getOperations() as ArrayList<Operation>
}

internal fun CoreDocument.getFloatExpressionsReflection(): HashMap<Int, FloatExpression> {
    return getFloatExpressions()
}

internal fun CoreDocument.registerVariablesReflection(
    context: RemoteContext,
    operations: ArrayList<Operation>,
) {
    registerVariables(context, operations)
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

internal fun CoreDocument.applyOperationsReflection(
    context: RemoteContext,
    operations: ArrayList<Operation>,
) {
    context.withOpCountReset { applyOperationsWithoutBitmaps(context, operations) }
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
            registerNestedBitmapMetadata(context, (op as Container).list)
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
                op.getDrawContentOperationsListReflection()?.let { canvasOps ->
                    applyOperationsWithoutBitmaps(context, canvasOps)
                }
            }
        } else {
            op.apply(context)
        }
    }
}

private fun registerNestedBitmapMetadata(context: RemoteContext, list: List<Operation>) {
    for (i in list.indices) {
        val op = list[i]
        if (op is BitmapData) {
            context.putObject(op.mImageId, op)
        } else if (op is Container) {
            registerNestedBitmapMetadata(context, op.list)
            if (op is LayoutComponent) {
                op.getDrawContentOperationsListReflection()?.let { canvasOps ->
                    registerNestedBitmapMetadata(context, canvasOps)
                }
            }
        }
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
            updateTimeReflection(context)
            registerVariablesReflection(context, getOperationsReflection())
            applyOperationsWithoutBitmaps(context, getOperationsReflection())
        } finally {
            context.mode = RemoteContext.ContextMode.UNSET
        }
    }
}

// 2. RemoteComposeState Helpers
internal fun RemoteComposeState.getRemoteContextReflection(): RemoteContext? {
    return getRemoteContext() as? RemoteContext
}

// 3. Layout Component / Manager Helpers
internal fun LayoutComponent.getDrawContentOperationsListReflection(): ArrayList<Operation>? {
    return getDrawContentOperations()?.list
}

internal val LayoutManager.horizontalPositioningReflection: Int
    get() =
        when (this) {
            is RowLayout -> getHorizontalPositioning()
            is ColumnLayout -> getHorizontalPositioning()
            is FitBoxLayout -> getHorizontalPositioning()
            is BoxLayout -> getHorizontalPositioning()
            else -> throw NoSuchFieldException("mHorizontalPositioning not found in $javaClass")
        }

internal val LayoutManager.verticalPositioningReflection: Int
    get() =
        when (this) {
            is RowLayout -> getVerticalPositioning()
            is ColumnLayout -> getVerticalPositioning()
            is FitBoxLayout -> getVerticalPositioning()
            is BoxLayout -> getVerticalPositioning()
            else -> throw NoSuchFieldException("mVerticalPositioning not found in $javaClass")
        }

// 4. Modifier Operations Helpers
internal fun ComponentVisibilityOperation.getVisibilityIdReflection(): Int {
    return getVisibilityId()
}

internal val ZIndexModifierOperation.valueReflection: Float
    get() = getRawValue()

// --- MarqueeModifierOperation Helper ---

internal fun MarqueeModifierOperation.readDataReflection(): MarqueeModifierOperationData {
    return MarqueeModifierOperationData(
        iterations = getIterations(),
        animationMode = getAnimationMode(),
        repeatDelayMillis = getRepeatDelayMillis(),
        initialDelayMillis = getInitialDelayMillis(),
        spacing = getSpacing(),
        velocity = getVelocity(),
    )
}

// --- GraphicsLayerModifierOperation Helper ---

internal fun GraphicsLayerModifierOperation.getValuesReflection():
    List<GraphicsLayerAttributeValueData> {
    return values.map { item ->
        GraphicsLayerAttributeValueData(
            name = item.name,
            id = item.id,
            source = item.source,
        )
    }
}

// --- HostNamedActionOperation Helper ---

internal fun HostNamedActionOperation.readData(): HostNamedActionOperationData {
    return HostNamedActionOperationData(
        textId = getTextId(),
        type = getType(),
        valueId = getValueId(),
    )
}

// TextSubtext, TextTransform

/** The id of the text this operation produces. */
internal val TextSubtext.textIdReflection: Int
    get() = getTextId()

/** The id of the text this operation produces. */
internal val TextTransform.textIdReflection: Int
    get() = getTextId()

/** The document's RootContentBehavior alignment (`mContentAlignment`). */
internal val CoreDocument.contentAlignmentReflection: Int
    get() = getContentAlignment()

// --- DrawBase Helpers ---

internal fun DrawBase2.readDataReflection(): DrawBase2Data {
    return DrawBase2Data(
        v1 = getV1(),
        v2 = getV2(),
        value1 = getValue1(),
        value2 = getValue2(),
    )
}

internal fun DrawBase3.readDataReflection(): DrawBase3Data {
    return DrawBase3Data(
        v1 = getV1(),
        v2 = getV2(),
        v3 = getV3(),
        value1 = getValue1(),
        value2 = getValue2(),
        value3 = getValue3(),
    )
}

internal fun DrawBase4.readDataReflection(): DrawBase4Data {
    return DrawBase4Data(
        x1 = getRawX1(),
        y1 = getRawY1(),
        x2 = getRawX2(),
        y2 = getRawY2(),
        x1Value = getX1Value(),
        y1Value = getY1Value(),
        x2Value = getX2Value(),
        y2Value = getY2Value(),
    )
}

internal fun DrawBase6.readDataReflection(): DrawBase6Data {
    return DrawBase6Data(
        v1 = getV1(),
        v2 = getV2(),
        v3 = getV3(),
        v4 = getV4(),
        v5 = getV5(),
        v6 = getV6(),
        value1 = getValue1(),
        value2 = getValue2(),
        value3 = getValue3(),
        value4 = getValue4(),
        value5 = getValue5(),
        value6 = getValue6(),
    )
}

// --- DrawBitmap Helper ---

internal fun DrawBitmap.readDataReflection(): DrawBitmapData {
    return DrawBitmapData(
        left = getLeft(),
        top = getTop(),
        right = getRight(),
        bottom = getBottom(),
        outputLeft = getOutputLeft(),
        outputTop = getOutputTop(),
        outputRight = getOutputRight(),
        outputBottom = getOutputBottom(),
        id = getImageId(),
        descriptionId = getDescriptionId(),
    )
}

// --- DrawBitmapScaled Helper ---

internal fun DrawBitmapScaled.readDataReflection(): DrawBitmapScaledData {
    return DrawBitmapScaledData(
        imageId = getImageId(),
        srcLeft = getSrcLeft(),
        outSrcLeft = getOutSrcLeft(),
        srcTop = getSrcTop(),
        outSrcTop = getOutSrcTop(),
        srcRight = getSrcRight(),
        outSrcRight = getOutSrcRight(),
        srcBottom = getSrcBottom(),
        outSrcBottom = getOutSrcBottom(),
        dstLeft = getDstLeft(),
        outDstLeft = getOutDstLeft(),
        dstTop = getDstTop(),
        outDstTop = getOutDstTop(),
        dstRight = getDstRight(),
        outDstRight = getOutDstRight(),
        dstBottom = getDstBottom(),
        outDstBottom = getOutDstBottom(),
        contentDescId = getContentDescId(),
        scaleFactor = getScaleFactor(),
        outScaleFactor = getOutScaleFactor(),
        scaleType = getScaleType(),
    )
}

// --- DrawPath Helper ---

internal fun DrawPath.readDataReflection(): DrawPathData {
    return DrawPathData(id = getPathId(), start = getStart(), end = getEnd())
}

// --- BitmapData Helper ---

internal fun BitmapData.readDataReflection(): BitmapDataData {
    return BitmapDataData(
        imageId = mImageId,
        imageWidth = getImageWidth(),
        imageHeight = getImageHeight(),
        type = getType().toShort(),
        encoding = getEncoding(),
        bitmap = getBitmap(),
    )
}

// 5. PaintBundle Reflection Helper
internal fun PaintBundle.getArrayReflection(): IntArray {
    return getArray()
}

internal fun PaintBundle.getPosReflection(): Int {
    return getPos()
}

// CoreSemantics
internal fun CoreSemantics.getContentDescriptionIdReflection(): Int? {
    val id = mContentDescriptionId
    return if (id != 0) id else null
}

// Recollect collections
internal fun CoreDocument.recollectCollectionsReflection() {
    val operations = this.getOperationsReflection()
    val state = this.remoteComposeState
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
            op.getDrawContentOperationsListReflection()?.let { canvasOps ->
                document.collectCollections(canvasOps, state)
                recollectCanvasCollections(document, canvasOps, state)
            }
        }
        if (op is Container) {
            recollectCanvasCollections(document, op.list, state)
        }
    }
}

internal fun CoreDocument.updateTimeReflection(context: RemoteContext) {
    updateTime(context)
}

internal fun RemoteContext.getVariableIdReflection(name: String): Int {
    val context = this as? AndroidRemoteContext ?: return -1
    return try {
        context.getVariableId(name)
    } catch (e: NoSuchElementException) {
        -1
    }
}

// ValueIntegerChangeActionOperation
internal val ValueIntegerChangeActionOperation.targetValueIdReflection: Int
    get() = getTargetValueId()

internal val ValueIntegerChangeActionOperation.valueReflection: Int
    get() = getValue()

// ValueFloatChangeActionOperation
internal val ValueFloatChangeActionOperation.targetValueIdReflection: Int
    get() = getTargetValueId()

internal val ValueFloatChangeActionOperation.valueReflection: Float
    get() = getValue()

// ValueStringChangeActionOperation
internal val ValueStringChangeActionOperation.targetValueIdReflection: Int
    get() = getTargetValueId()

internal val ValueStringChangeActionOperation.valueIdReflection: Int
    get() = getValueId()

// ValueIntegerExpressionChangeActionOperation
internal val ValueIntegerExpressionChangeActionOperation.targetValueIdReflection: Long
    get() = getTargetValueId()

internal val ValueIntegerExpressionChangeActionOperation.valueExpressionIdReflection: Long
    get() = getValueExpressionId()

// ValueFloatExpressionChangeActionOperation
internal val ValueFloatExpressionChangeActionOperation.targetValueIdReflection: Int
    get() = getTargetValueId()

internal val ValueFloatExpressionChangeActionOperation.valueExpressionIdReflection: Int
    get() = getValueExpressionId()

// --- BorderModifierOperation Helper ---

internal fun BorderModifierOperation.readDataReflection(): BorderModifierOperationData {
    return BorderModifierOperationData(
        useColorId = getUseColorId(),
        colorId = getColorId(),
        r = getR(),
        g = getG(),
        b = getB(),
        a = getA(),
        borderWidth = getBorderWidth(),
        roundedCorner = getRoundedCorner(),
        shapeType = getShapeType(),
    )
}

// --- BackgroundModifierOperation Helper ---

internal fun BackgroundModifierOperation.readDataReflection(): BackgroundModifierOperationData {
    return BackgroundModifierOperationData(
        useColorId = getUseColorId(),
        colorId = getColorId(),
        rId = getRId(),
        gId = getGId(),
        bId = getBId(),
        aId = getAId(),
        shapeType = getShapeType(),
    )
}

// --- StateLayout Helper ---
internal val StateLayout.indexIdReflection: Int
    get() = getIndexId()

// --- IntegerExpression Helper ---
/** The bit mask marking which [IntegerExpression.mSrcValue] entries are variable IDs. */
internal val IntegerExpression.maskReflection: Int
    get() = getMask()

// --- FitBoxLayout Helpers ---
internal val FitBoxLayout.horizontalPositioningReflection: Int
    get() = getHorizontalPositioning()

internal val FitBoxLayout.verticalPositioningReflection: Int
    get() = getVerticalPositioning()

// --- CoreDocument updateVariables Helper ---
internal fun CoreDocument.updateVariablesReflection(
    context: RemoteContext,
    theme: Int,
    operations: List<Operation>,
) {
    updateVariables(context, theme, operations)
}

// AlignByModifierOperation
internal val AlignByModifierOperation.lineReflection: Float
    get() = getLine()

// --- CoreText Reflection Helper ---

internal fun CoreText.readDataReflection(): CoreTextData {
    return CoreTextData(
        colorValue = getColorValue(),
        fontSizeValue = getFontSizeValue(),
        type = getType(),
        fontWeightValue = getFontWeightValue(),
        fontStyle = getFontStyle(),
        textAlignValue = getTextAlignValue(),
        overflow = getOverflow(),
        maxLines = getMaxLines(),
        letterSpacing = getLetterSpacing(),
        lineHeightMultiplier = getLineHeightMultiplier(),
        lineHeightAdd = getLineHeightAdd(),
        underline = getUnderline(),
        strikethrough = getStrikethrough(),
        fontAxis = getFontAxis(),
        fontAxisValues = getFontAxisValues(),
        autosize = getAutosize(),
        minFontSize = getMinFontSize(),
        maxFontSize = getMaxFontSize(),
        lineBreakStrategy = getLineBreakStrategy(),
        hyphenationFrequency = getHyphenationFrequency(),
        justificationMode = getJustificationMode(),
        isDynamicColorEnabled = isDynamicColorEnabled(),
        colorId = getColorId(),
        fontSize = getFontSize(),
    )
}

// --- TextLayout Reflection Helper ---

internal fun TextLayout.readDataReflection(): TextLayoutData {
    return TextLayoutData(
        colorValue = getColorValue(),
        fontSizeValue = getFontSizeValue(),
        type = getType(),
        fontWeight = getFontWeight(),
        textAlignValue = getTextAlignValue(),
        overflow = getOverflow(),
        maxLines = getMaxLines(),
        isDynamicColorEnabled = isDynamicColorEnabled(),
        colorId = getColor(),
        fontSize = getFontSize(),
    )
}

// 6. Layout Spacing Helpers
internal fun rowSpacedBy(layout: RowLayout): Float = layout.getSpacedBy()

internal fun columnSpacedBy(layout: ColumnLayout): Float = layout.getSpacedBy()

// 7. ScrollModifier Reflection
internal fun scrollPosition(op: ScrollModifierOperation): Float = op.getPositionExpression()

internal fun touchStopMode(touch: TouchExpression): Int = touch.getStopMode()

internal fun touchStopSpec(touch: TouchExpression): FloatArray? = touch.getStopSpec()

// 8. DimensionConstraints Reflection
internal fun dimensionConstraintsType(op: DimensionConstraintsModifierOperation): Int = op.getType()

// 9. DimensionModifier Reflection
internal fun dimensionRawValue(op: DimensionModifierOperation): Float = op.getRawValue()

// OffsetModifier
internal fun offsetRawValues(op: OffsetModifierOperation): FloatArray =
    floatArrayOf(op.rawX, op.rawY)

// DimensionInModifier
/** The raw min/max constraint sources, in dp or as NaN-encoded variable ids. */
internal fun dimensionInRawValues(op: DimensionInModifierOperation): FloatArray =
    floatArrayOf(op.rawMin, op.rawMax)

// 10. CollapsiblePriority Reflection
internal fun sortWithPriorities(children: ArrayList<Component>, orientation: Int): List<Component> =
    CollapsiblePriority.sortWithPriorities(children, orientation)

// 11. LayoutComponent Content Reflection
internal fun LayoutComponent.getContentReflection(): LayoutComponentContent? = getLayoutContent()

// 12. Component AnimationSpec Reflection
internal val Component.animationSpecReflection: AnimationSpec?
    get() = getAnimationSpec()

// 13. PaddingModifierOperation Raw Values
internal fun paddingRawValues(op: PaddingModifierOperation): FloatArray =
    floatArrayOf(op.rawLeft, op.rawTop, op.rawRight, op.rawBottom)

// 14. MultiClickModifier clickType
internal val MultiClickModifier.clickTypeReflection: Int
    get() = getClickType()

// 15. PathCombine and PathData
internal val PathCombine.operationReflection: Byte
    get() = getOperation()

internal fun PathData.applyReflection(context: RemoteContext) {
    val outputPath = getOutputPath()
    if (context.getPathData(getId()) !== outputPath) {
        markPathChanged()
    }
    apply(context)
}

internal val PathData.instanceIdReflection: Int
    get() = getId()

internal val PathData.floatPathReflection: FloatArray
    get() = getFloatPath()

// 16. ConditionalOperations, LoopOperation, Function, Text, and Impulse raw values

internal val ConditionalOperations.varARawReflection: Float
    get() = getVarA()

internal val ConditionalOperations.varBRawReflection: Float
    get() = getVarB()

internal val LoopOperation.fromRawReflection: Float
    get() = getFrom()

internal val LoopOperation.untilRawReflection: Float
    get() = getUntil()

internal val LoopOperation.stepRawReflection: Float
    get() = getStep()

internal val FloatFunctionDefine.idReflection: Int
    get() = getId()

internal val FloatFunctionDefine.floatVarIdsReflection: IntArray
    get() = getArgs()

internal val FloatFunctionCall.idReflection: Int
    get() = getId()

internal val FloatFunctionCall.argsReflection: FloatArray?
    get() = getArgs()

internal val DrawTextOnPath.hOffsetRawReflection: Float
    get() = getHOffset()

internal val DrawTextOnPath.vOffsetRawReflection: Float
    get() = getVOffset()

internal val DrawTextAnchored.xRawReflection: Float
    get() = getX()

internal val DrawTextAnchored.yRawReflection: Float
    get() = getY()

internal val DrawTextAnchored.panXRawReflection: Float
    get() = getPanX()

internal val DrawTextAnchored.panYRawReflection: Float
    get() = getPanY()

internal val ImpulseOperation.durationReflection: Float
    get() = getDuration()

internal val ImpulseOperation.startAtReflection: Float
    get() = getStartAt()
