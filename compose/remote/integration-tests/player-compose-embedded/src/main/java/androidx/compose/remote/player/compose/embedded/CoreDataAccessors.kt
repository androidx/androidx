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

@file:Suppress("BanUncheckedReflection", "RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation

/*
 * Centralized reflective accessors for package-private remote-core operation fields the embedded
 * player needs to read (remote-core left unchanged — no public getters yet).
 * Encapsulated via Kotlin data classes and extension methods.
 */

// --- Data Classes ---

// --- Extension Methods ---

internal fun ClipPath.readData(): ClipPathData {
    return ClipPathData(
        id = clipPathIdField.getInt(this),
        regionOp = clipPathRegionOpField.getInt(this),
    )
}

internal fun DrawTextOnPath.readData(): DrawTextOnPathData {
    return DrawTextOnPathData(
        pathId = dtopPathIdField.getInt(this),
        hOffset = dtopOutHOffsetField.getFloat(this),
        vOffset = dtopOutVOffsetField.getFloat(this),
    )
}

internal fun DrawTextAnchored.readData(): DrawTextAnchoredData {
    return DrawTextAnchoredData(
        textId = dtaTextIdField.getInt(this),
        x = dtaOutXField.getFloat(this),
        y = dtaOutYField.getFloat(this),
        panX = dtaOutPanXField.getFloat(this),
        panY = dtaOutPanYField.getFloat(this),
        flags = dtaFlagsField.getInt(this),
    )
}

internal fun DrawBitmapInt.readData(): DrawBitmapIntData {
    return DrawBitmapIntData(
        imageId = dbiImageIdField.getInt(this),
        srcLeft = dbiSrcLeftField.getInt(this),
        srcTop = dbiSrcTopField.getInt(this),
        srcRight = dbiSrcRightField.getInt(this),
        srcBottom = dbiSrcBottomField.getInt(this),
        dstLeft = dbiDstLeftField.getInt(this),
        dstTop = dbiDstTopField.getInt(this),
        dstRight = dbiDstRightField.getInt(this),
        dstBottom = dbiDstBottomField.getInt(this),
    )
}

internal fun DrawTextOnCircle.readData(): DrawTextOnCircleData {
    return DrawTextOnCircleData(
        centerX = dtocCenterXField.getFloat(this),
        centerY = dtocCenterYField.getFloat(this),
        radius = dtocRadiusField.getFloat(this),
        startAngle = dtocStartAngleField.getFloat(this),
        warpRadiusOffset = dtocWarpRadiusOffsetField.getFloat(this),
        alignment = dtocAlignmentField.get(this) as DrawTextOnCircle.Alignment,
        placement = dtocPlacementField.get(this) as DrawTextOnCircle.Placement,
    )
}

internal fun DrawBitmapFontText.readData(): DrawBitmapFontTextData {
    return DrawBitmapFontTextData(
        textId = dbftTextIdField.getInt(this),
        fontId = dbftFontIdField.getInt(this),
        start = dbftStartField.getInt(this),
        end = dbftEndField.getInt(this),
        x = dbftOutXField.getFloat(this),
        y = dbftOutYField.getFloat(this),
        glyphSpacing = dbftOutGlyphSpacingField.getFloat(this),
    )
}

@Suppress("UNCHECKED_CAST")
internal fun BitmapFontData.getKerningTable(): Map<String, Short>? {
    return bitmapFontKerningField.get(this) as? Map<String, Short>
}

internal fun DrawBitmapFontTextOnPath.readData(): DrawBitmapFontTextOnPathData {
    return DrawBitmapFontTextOnPathData(
        textId = dbfopTextIdField.getInt(this),
        fontId = dbfopFontIdField.getInt(this),
        pathId = dbfopPathIdField.getInt(this),
        start = dbfopStartField.getInt(this),
        end = dbfopEndField.getInt(this),
        yAdj = dbfopOutYAdjField.getFloat(this),
        glyphSpacing = dbfopOutGlyphSpacingField.getFloat(this),
    )
}

internal fun DrawBitmapTextAnchored.readData(): DrawBitmapTextAnchoredData {
    return DrawBitmapTextAnchoredData(
        textId = dbtaTextIdField.getInt(this),
        fontId = dbtaFontIdField.getInt(this),
        start = dbtaOutStartField.getFloat(this).toInt(),
        end = dbtaOutEndField.getFloat(this).toInt(),
        x = dbtaOutXField.getFloat(this),
        y = dbtaOutYField.getFloat(this),
        panX = dbtaOutPanXField.getFloat(this),
        panY = dbtaOutPanYField.getFloat(this),
        glyphSpacing = dbtaOutGlyphSpacingField.getFloat(this),
    )
}

internal fun DrawToBitmap.readData(): DrawToBitmapData {
    return DrawToBitmapData(
        bitmapId = dtbBitmapIdField.getInt(this),
        mode = dtbModeField.getInt(this),
        color = dtbColorField.getInt(this),
    )
}

internal fun DrawTweenPath.readData(): DrawTweenPathData {
    return DrawTweenPathData(
        path1Id = dtpPath1IdField.getInt(this),
        path2Id = dtpPath2IdField.getInt(this),
        tween = dtpOutTweenField.getFloat(this),
        start = dtpOutStartField.getFloat(this),
        stop = dtpOutStopField.getFloat(this),
    )
}

/**
 * The [ParticlesCreate] op a [ParticlesLoop] draws from. Particle *rendering* is bridged to the
 * core implementation (see RcPlayerParticles); the player only needs the source to run the core
 * seeding path once per document.
 */
internal val ParticlesLoop.particlesSourceReflection: ParticlesCreate?
    get() = plSourceField.get(this) as? ParticlesCreate

internal fun ConditionalOperations.readData(): ConditionalOperationsData {
    return ConditionalOperationsData(
        varAOut = condAOutField.getFloat(this),
        varBOut = condBOutField.getFloat(this),
        type = condTypeField.getByte(this),
    )
}

internal fun LoopOperation.readData(): LoopOperationData {
    return LoopOperationData(
        fromOut = loopFromOutField.getFloat(this),
        untilOut = loopUntilOutField.getFloat(this),
        stepOut = loopStepOutField.getFloat(this),
        indexVariableId = loopIndexVarField.getInt(this),
    )
}

internal fun FloatFunctionCall.readData(): FloatFunctionCallData {
    return FloatFunctionCallData(
        function = ffcFunctionField.get(this),
        outArgs = ffcOutArgsField.get(this) as? FloatArray,
    )
}

internal fun Custom.readData(): CustomData {
    return CustomData(
        config = customConfigField.get(this) as? String,
        configId = customConfigIdField.getInt(this),
        properties = customPropertiesField.get(this),
    )
}

// --- Private Reflective Fields ---

private val clipPathIdField =
    ClipPath::class.java.getDeclaredField("mId").apply { isAccessible = true }
private val clipPathRegionOpField =
    ClipPath::class.java.getDeclaredField("mRegionOp").apply { isAccessible = true }

private val dtopPathIdField =
    DrawTextOnPath::class.java.getDeclaredField("mPathId").apply { isAccessible = true }
private val dtopOutHOffsetField =
    DrawTextOnPath::class.java.getDeclaredField("mOutHOffset").apply { isAccessible = true }
private val dtopOutVOffsetField =
    DrawTextOnPath::class.java.getDeclaredField("mOutVOffset").apply { isAccessible = true }

private val dtaTextIdField =
    DrawTextAnchored::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
private val dtaOutXField =
    DrawTextAnchored::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
private val dtaOutYField =
    DrawTextAnchored::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
private val dtaOutPanXField =
    DrawTextAnchored::class.java.getDeclaredField("mOutPanX").apply { isAccessible = true }
private val dtaOutPanYField =
    DrawTextAnchored::class.java.getDeclaredField("mOutPanY").apply { isAccessible = true }
private val dtaFlagsField =
    DrawTextAnchored::class.java.getDeclaredField("mFlags").apply { isAccessible = true }

private val dbiImageIdField =
    DrawBitmapInt::class.java.getDeclaredField("mImageId").apply { isAccessible = true }
private val dbiSrcLeftField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcLeft").apply { isAccessible = true }
private val dbiSrcTopField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcTop").apply { isAccessible = true }
private val dbiSrcRightField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcRight").apply { isAccessible = true }
private val dbiSrcBottomField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcBottom").apply { isAccessible = true }
private val dbiDstLeftField =
    DrawBitmapInt::class.java.getDeclaredField("mDstLeft").apply { isAccessible = true }
private val dbiDstTopField =
    DrawBitmapInt::class.java.getDeclaredField("mDstTop").apply { isAccessible = true }
private val dbiDstRightField =
    DrawBitmapInt::class.java.getDeclaredField("mDstRight").apply { isAccessible = true }
private val dbiDstBottomField =
    DrawBitmapInt::class.java.getDeclaredField("mDstBottom").apply { isAccessible = true }

private val dtocCenterXField =
    DrawTextOnCircle::class.java.getDeclaredField("mCenterX").apply { isAccessible = true }
private val dtocCenterYField =
    DrawTextOnCircle::class.java.getDeclaredField("mCenterY").apply { isAccessible = true }
private val dtocRadiusField =
    DrawTextOnCircle::class.java.getDeclaredField("mRadius").apply { isAccessible = true }
private val dtocStartAngleField =
    DrawTextOnCircle::class.java.getDeclaredField("mStartAngle").apply { isAccessible = true }
private val dtocWarpRadiusOffsetField =
    DrawTextOnCircle::class.java.getDeclaredField("mWarpRadiusOffset").apply { isAccessible = true }
private val dtocAlignmentField =
    DrawTextOnCircle::class.java.getDeclaredField("mAlignment").apply { isAccessible = true }
private val dtocPlacementField =
    DrawTextOnCircle::class.java.getDeclaredField("mPlacement").apply { isAccessible = true }

private val dbftTextIdField =
    DrawBitmapFontText::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
private val dbftFontIdField =
    DrawBitmapFontText::class.java.getDeclaredField("mBitmapFontID").apply { isAccessible = true }
private val dbftStartField =
    DrawBitmapFontText::class.java.getDeclaredField("mStart").apply { isAccessible = true }
private val dbftEndField =
    DrawBitmapFontText::class.java.getDeclaredField("mEnd").apply { isAccessible = true }
private val dbftOutXField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
private val dbftOutYField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
private val dbftOutGlyphSpacingField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutGlyphSpacing").apply {
        isAccessible = true
    }

private val bitmapFontKerningField =
    BitmapFontData::class.java.getDeclaredField("mKerningTable").apply { isAccessible = true }

private val dbfopTextIdField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
private val dbfopFontIdField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mBitmapFontID").apply {
        isAccessible = true
    }
private val dbfopPathIdField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mPathID").apply { isAccessible = true }
private val dbfopStartField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mStart").apply { isAccessible = true }
private val dbfopEndField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mEnd").apply { isAccessible = true }
private val dbfopOutYAdjField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mOutYAdj").apply { isAccessible = true }
private val dbfopOutGlyphSpacingField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mOutGlyphSpacing").apply {
        isAccessible = true
    }

private val dbtaTextIdField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
private val dbtaFontIdField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mBitmapFontID").apply {
        isAccessible = true
    }
private val dbtaOutStartField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutStart").apply { isAccessible = true }
private val dbtaOutEndField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutEnd").apply { isAccessible = true }
private val dbtaOutXField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
private val dbtaOutYField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
private val dbtaOutPanXField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutPanX").apply { isAccessible = true }
private val dbtaOutPanYField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutPanY").apply { isAccessible = true }
private val dbtaOutGlyphSpacingField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutGlyphSpacing").apply {
        isAccessible = true
    }

private val dtbBitmapIdField =
    DrawToBitmap::class.java.getDeclaredField("mBitmapId").apply { isAccessible = true }
private val dtbModeField =
    DrawToBitmap::class.java.getDeclaredField("mMode").apply { isAccessible = true }
private val dtbColorField =
    DrawToBitmap::class.java.getDeclaredField("mColor").apply { isAccessible = true }

private val dtpPath1IdField =
    DrawTweenPath::class.java.getDeclaredField("mPath1Id").apply { isAccessible = true }
private val dtpPath2IdField =
    DrawTweenPath::class.java.getDeclaredField("mPath2Id").apply { isAccessible = true }
private val dtpOutTweenField =
    DrawTweenPath::class.java.getDeclaredField("mOutTween").apply { isAccessible = true }
private val dtpOutStartField =
    DrawTweenPath::class.java.getDeclaredField("mOutStart").apply { isAccessible = true }
private val dtpOutStopField =
    DrawTweenPath::class.java.getDeclaredField("mOutStop").apply { isAccessible = true }

private val plSourceField =
    ParticlesLoop::class.java.getDeclaredField("mParticlesSource").apply { isAccessible = true }

private val condAOutField =
    ConditionalOperations::class.java.getDeclaredField("mVarAOut").apply { isAccessible = true }
private val condBOutField =
    ConditionalOperations::class.java.getDeclaredField("mVarBOut").apply { isAccessible = true }
private val condTypeField =
    ConditionalOperations::class.java.getDeclaredField("mType").apply { isAccessible = true }

private val loopFromOutField =
    LoopOperation::class.java.getDeclaredField("mFromOut").apply { isAccessible = true }
private val loopUntilOutField =
    LoopOperation::class.java.getDeclaredField("mUntilOut").apply { isAccessible = true }
private val loopStepOutField =
    LoopOperation::class.java.getDeclaredField("mStepOut").apply { isAccessible = true }
private val loopIndexVarField =
    LoopOperation::class.java.getDeclaredField("mIndexVariableId").apply { isAccessible = true }

private val ffcFunctionField =
    FloatFunctionCall::class.java.getDeclaredField("mFunction").apply { isAccessible = true }
private val ffcOutArgsField =
    FloatFunctionCall::class.java.getDeclaredField("mOutArgs").apply { isAccessible = true }

private val customConfigField =
    Custom::class.java.getDeclaredField("mConfig").apply { isAccessible = true }
private val customConfigIdField =
    Custom::class.java.getDeclaredField("mConfigId").apply { isAccessible = true }
private val customPropertiesField =
    Custom::class.java.getDeclaredField("mProperties").apply { isAccessible = true }

// --- Appended Reflection Helpers for Bypassing Core Changes ---

// 1. CoreDocument Helpers
@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.CoreDocument.getOperationsReflection():
    ArrayList<androidx.compose.remote.core.Operation> {
    return docOperationsField.get(this) as ArrayList<androidx.compose.remote.core.Operation>
}

@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.CoreDocument.getFloatExpressionsReflection():
    java.util.HashMap<Int, androidx.compose.remote.core.operations.FloatExpression> {
    return docFloatExpressionsField.get(this)
        as java.util.HashMap<Int, androidx.compose.remote.core.operations.FloatExpression>
}

internal fun androidx.compose.remote.core.CoreDocument.registerVariablesReflection(
    context: androidx.compose.remote.core.RemoteContext,
    operations: ArrayList<androidx.compose.remote.core.Operation>,
) {
    docRegisterVariablesMethod.invoke(this, context, operations)
}

internal fun androidx.compose.remote.core.CoreDocument.applyOperationsReflection(
    context: androidx.compose.remote.core.RemoteContext,
    operations: ArrayList<androidx.compose.remote.core.Operation>,
) {
    docApplyOperationsMethod.invoke(this, context, operations)
}

private val docOperationsField =
    androidx.compose.remote.core.CoreDocument::class.java.getDeclaredField("mOperations").apply {
        isAccessible = true
    }

private val docFloatExpressionsField =
    androidx.compose.remote.core.CoreDocument::class
        .java
        .getDeclaredField("mFloatExpressions")
        .apply { isAccessible = true }

private val docRegisterVariablesMethod =
    androidx.compose.remote.core.CoreDocument::class
        .java
        .getDeclaredMethod(
            "registerVariables",
            androidx.compose.remote.core.RemoteContext::class.java,
            java.util.ArrayList::class.java,
        )
        .apply { isAccessible = true }

private val docApplyOperationsMethod =
    androidx.compose.remote.core.CoreDocument::class
        .java
        .getDeclaredMethod(
            "applyOperations",
            androidx.compose.remote.core.RemoteContext::class.java,
            java.util.ArrayList::class.java,
        )
        .apply { isAccessible = true }

// 2. RemoteComposeState Helpers
internal fun androidx.compose.remote.core.RemoteComposeState.getRemoteContextReflection():
    androidx.compose.remote.core.RemoteContext? {
    return stateRemoteContextField.get(this) as? androidx.compose.remote.core.RemoteContext
}

private val stateRemoteContextField =
    androidx.compose.remote.core.RemoteComposeState::class
        .java
        .getDeclaredField("mRemoteContext")
        .apply { isAccessible = true }

// 3. Layout Component / Manager Helpers
@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.operations.layout.LayoutComponent
    .getDrawContentOperationsListReflection():
    java.util.ArrayList<androidx.compose.remote.core.Operation>? {
    val canvasOps = layoutDrawContentOpsField.get(this) ?: return null
    val listField = canvasOps.javaClass.getDeclaredField("mList").apply { isAccessible = true }
    return listField.get(canvasOps) as? java.util.ArrayList<androidx.compose.remote.core.Operation>
}

private val layoutDrawContentOpsField =
    androidx.compose.remote.core.operations.layout.LayoutComponent::class
        .java
        .getDeclaredField("mDrawContentOperations")
        .apply { isAccessible = true }

internal val androidx.compose.remote.core.operations.layout.managers.LayoutManager.horizontalPositioningReflection:
    Int
    get() {
        var clazz: Class<*>? = this.javaClass
        var field: java.lang.reflect.Field? = null
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField("mHorizontalPositioning")
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        if (field == null)
            throw NoSuchFieldException("mHorizontalPositioning not found in ${this.javaClass}")
        field.isAccessible = true
        return field.getInt(this)
    }

internal val androidx.compose.remote.core.operations.layout.managers.LayoutManager.verticalPositioningReflection:
    Int
    get() {
        var clazz: Class<*>? = this.javaClass
        var field: java.lang.reflect.Field? = null
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField("mVerticalPositioning")
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        if (field == null)
            throw NoSuchFieldException("mVerticalPositioning not found in ${this.javaClass}")
        field.isAccessible = true
        return field.getInt(this)
    }

// 4. Modifier Operations Helpers
internal fun androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
    .getVisibilityIdReflection(): Int {
    return visVisibilityIdField.getInt(this)
}

private val visVisibilityIdField =
    androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation::class
        .java
        .getDeclaredField("mVisibilityId")
        .apply { isAccessible = true }

internal val androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation.valueReflection:
    Float
    get() = zIndexValueField.getFloat(this)

private val zIndexValueField =
    androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation::class
        .java
        .getDeclaredField("mValue")
        .apply { isAccessible = true }

// Marquee

internal fun androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
    .readDataReflection(): MarqueeModifierOperationData {
    return MarqueeModifierOperationData(
        iterations = marqueeIterationsField.getInt(this),
        animationMode = marqueeAnimationModeField.getInt(this),
        repeatDelayMillis = marqueeRepeatDelayField.getFloat(this),
        initialDelayMillis = marqueeInitialDelayField.getFloat(this),
        spacing = marqueeSpacingField.getFloat(this),
        velocity = marqueeVelocityField.getFloat(this),
    )
}

private val marqueeIterationsField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mIterations")
        .apply { isAccessible = true }
private val marqueeAnimationModeField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mAnimationMode")
        .apply { isAccessible = true }
private val marqueeRepeatDelayField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mRepeatDelayMillis")
        .apply { isAccessible = true }
private val marqueeInitialDelayField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mInitialDelayMillis")
        .apply { isAccessible = true }
private val marqueeSpacingField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mSpacing")
        .apply { isAccessible = true }
private val marqueeVelocityField =
    androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation::class
        .java
        .getDeclaredField("mVelocity")
        .apply { isAccessible = true }

// GraphicsLayer AttributeValue

@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
    .getValuesReflection(): List<GraphicsLayerAttributeValueData> {
    val array = graphicsLayerValuesField.get(this) as? Array<*> ?: return emptyList()
    return array.map { item ->
        val clazz = item!!.javaClass
        val nameField = clazz.getDeclaredField("mName").apply { isAccessible = true }
        val idField = clazz.getDeclaredField("mId").apply { isAccessible = true }
        val getValueMethod = clazz.getDeclaredMethod("getValue").apply { isAccessible = true }
        GraphicsLayerAttributeValueData(
            name = nameField.get(item) as String,
            id = idField.getInt(item),
            value = getValueMethod.invoke(item) as Float,
        )
    }
}

private val graphicsLayerValuesField =
    androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation::class
        .java
        .getDeclaredField("mValues")
        .apply { isAccessible = true }

// HostNamedActionOperation

internal fun HostNamedActionOperation.readData(): HostNamedActionOperationData {
    return HostNamedActionOperationData(
        textId = hnaTextIdField.getInt(this),
        type = hnaTypeField.getInt(this),
        valueId = hnaValueIdField.getInt(this),
    )
}

private val hnaTextIdField =
    HostNamedActionOperation::class.java.getDeclaredField("mTextId").apply { isAccessible = true }
private val hnaTypeField =
    HostNamedActionOperation::class.java.getDeclaredField("mType").apply { isAccessible = true }
private val hnaValueIdField =
    HostNamedActionOperation::class.java.getDeclaredField("mValueId").apply { isAccessible = true }

// 5. Draw Operations Helpers

// DrawBase2

internal fun androidx.compose.remote.core.operations.DrawBase2.readDataReflection(): DrawBase2Data {
    return DrawBase2Data(
        v1 = db2V1Field.getFloat(this),
        v2 = db2V2Field.getFloat(this),
        value1 = db2Value1Field.getFloat(this),
        value2 = db2Value2Field.getFloat(this),
    )
}

private val db2V1Field =
    androidx.compose.remote.core.operations.DrawBase2::class.java.getDeclaredField("mV1").apply {
        isAccessible = true
    }
private val db2V2Field =
    androidx.compose.remote.core.operations.DrawBase2::class.java.getDeclaredField("mV2").apply {
        isAccessible = true
    }
private val db2Value1Field =
    androidx.compose.remote.core.operations.DrawBase2::class
        .java
        .getDeclaredField("mValue1")
        .apply { isAccessible = true }
private val db2Value2Field =
    androidx.compose.remote.core.operations.DrawBase2::class
        .java
        .getDeclaredField("mValue2")
        .apply { isAccessible = true }

// DrawBase3

internal fun androidx.compose.remote.core.operations.DrawBase3.readDataReflection(): DrawBase3Data {
    return DrawBase3Data(
        v1 = db3V1Field.getFloat(this),
        v2 = db3V2Field.getFloat(this),
        v3 = db3V3Field.getFloat(this),
        value1 = db3Value1Field.getFloat(this),
        value2 = db3Value2Field.getFloat(this),
        value3 = db3Value3Field.getFloat(this),
    )
}

private val db3V1Field =
    androidx.compose.remote.core.operations.DrawBase3::class.java.getDeclaredField("mV1").apply {
        isAccessible = true
    }
private val db3V2Field =
    androidx.compose.remote.core.operations.DrawBase3::class.java.getDeclaredField("mV2").apply {
        isAccessible = true
    }
private val db3V3Field =
    androidx.compose.remote.core.operations.DrawBase3::class.java.getDeclaredField("mV3").apply {
        isAccessible = true
    }
private val db3Value1Field =
    androidx.compose.remote.core.operations.DrawBase3::class
        .java
        .getDeclaredField("mValue1")
        .apply { isAccessible = true }
private val db3Value2Field =
    androidx.compose.remote.core.operations.DrawBase3::class
        .java
        .getDeclaredField("mValue2")
        .apply { isAccessible = true }
private val db3Value3Field =
    androidx.compose.remote.core.operations.DrawBase3::class
        .java
        .getDeclaredField("mValue3")
        .apply { isAccessible = true }

// DrawBase4

internal fun androidx.compose.remote.core.operations.DrawBase4.readDataReflection(): DrawBase4Data {
    return DrawBase4Data(
        x1 = db4X1Field.getFloat(this),
        y1 = db4Y1Field.getFloat(this),
        x2 = db4X2Field.getFloat(this),
        y2 = db4Y2Field.getFloat(this),
        x1Value = db4X1ValueField.getFloat(this),
        y1Value = db4Y1ValueField.getFloat(this),
        x2Value = db4X2ValueField.getFloat(this),
        y2Value = db4Y2ValueField.getFloat(this),
    )
}

private val db4X1Field =
    androidx.compose.remote.core.operations.DrawBase4::class.java.getDeclaredField("mX1").apply {
        isAccessible = true
    }
private val db4Y1Field =
    androidx.compose.remote.core.operations.DrawBase4::class.java.getDeclaredField("mY1").apply {
        isAccessible = true
    }
private val db4X2Field =
    androidx.compose.remote.core.operations.DrawBase4::class.java.getDeclaredField("mX2").apply {
        isAccessible = true
    }
private val db4Y2Field =
    androidx.compose.remote.core.operations.DrawBase4::class.java.getDeclaredField("mY2").apply {
        isAccessible = true
    }
private val db4X1ValueField =
    androidx.compose.remote.core.operations.DrawBase4::class
        .java
        .getDeclaredField("mX1Value")
        .apply { isAccessible = true }
private val db4Y1ValueField =
    androidx.compose.remote.core.operations.DrawBase4::class
        .java
        .getDeclaredField("mY1Value")
        .apply { isAccessible = true }
private val db4X2ValueField =
    androidx.compose.remote.core.operations.DrawBase4::class
        .java
        .getDeclaredField("mX2Value")
        .apply { isAccessible = true }
private val db4Y2ValueField =
    androidx.compose.remote.core.operations.DrawBase4::class
        .java
        .getDeclaredField("mY2Value")
        .apply { isAccessible = true }

// DrawBase6

internal fun androidx.compose.remote.core.operations.DrawBase6.readDataReflection(): DrawBase6Data {
    return DrawBase6Data(
        v1 = db6V1Field.getFloat(this),
        v2 = db6V2Field.getFloat(this),
        v3 = db6V3Field.getFloat(this),
        v4 = db6V4Field.getFloat(this),
        v5 = db6V5Field.getFloat(this),
        v6 = db6V6Field.getFloat(this),
        value1 = db6Value1Field.getFloat(this),
        value2 = db6Value2Field.getFloat(this),
        value3 = db6Value3Field.getFloat(this),
        value4 = db6Value4Field.getFloat(this),
        value5 = db6Value5Field.getFloat(this),
        value6 = db6Value6Field.getFloat(this),
    )
}

private val db6V1Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV1").apply {
        isAccessible = true
    }
private val db6V2Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV2").apply {
        isAccessible = true
    }
private val db6V3Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV3").apply {
        isAccessible = true
    }
private val db6V4Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV4").apply {
        isAccessible = true
    }
private val db6V5Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV5").apply {
        isAccessible = true
    }
private val db6V6Field =
    androidx.compose.remote.core.operations.DrawBase6::class.java.getDeclaredField("mV6").apply {
        isAccessible = true
    }
private val db6Value1Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue1")
        .apply { isAccessible = true }
private val db6Value2Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue2")
        .apply { isAccessible = true }
private val db6Value3Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue3")
        .apply { isAccessible = true }
private val db6Value4Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue4")
        .apply { isAccessible = true }
private val db6Value5Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue5")
        .apply { isAccessible = true }
private val db6Value6Field =
    androidx.compose.remote.core.operations.DrawBase6::class
        .java
        .getDeclaredField("mValue6")
        .apply { isAccessible = true }

// DrawBitmap

internal fun androidx.compose.remote.core.operations.DrawBitmap.readDataReflection():
    DrawBitmapData {
    return DrawBitmapData(
        left = dbmpLeftField.getFloat(this),
        top = dbmpTopField.getFloat(this),
        right = dbmpRightField.getFloat(this),
        bottom = dbmpBottomField.getFloat(this),
        outputLeft = dbmpOutputLeftField.getFloat(this),
        outputTop = dbmpOutputTopField.getFloat(this),
        outputRight = dbmpOutputRightField.getFloat(this),
        outputBottom = dbmpOutputBottomField.getFloat(this),
        id = dbmpIdField.getInt(this),
        descriptionId = dbmpDescriptionIdField.getInt(this),
    )
}

private val dbmpLeftField =
    androidx.compose.remote.core.operations.DrawBitmap::class.java.getDeclaredField("mLeft").apply {
        isAccessible = true
    }
private val dbmpTopField =
    androidx.compose.remote.core.operations.DrawBitmap::class.java.getDeclaredField("mTop").apply {
        isAccessible = true
    }
private val dbmpRightField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mRight")
        .apply { isAccessible = true }
private val dbmpBottomField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mBottom")
        .apply { isAccessible = true }
private val dbmpOutputLeftField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mOutputLeft")
        .apply { isAccessible = true }
private val dbmpOutputTopField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mOutputTop")
        .apply { isAccessible = true }
private val dbmpOutputRightField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mOutputRight")
        .apply { isAccessible = true }
private val dbmpOutputBottomField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mOutputBottom")
        .apply { isAccessible = true }
private val dbmpIdField =
    androidx.compose.remote.core.operations.DrawBitmap::class.java.getDeclaredField("mId").apply {
        isAccessible = true
    }
private val dbmpDescriptionIdField =
    androidx.compose.remote.core.operations.DrawBitmap::class
        .java
        .getDeclaredField("mDescriptionId")
        .apply { isAccessible = true }

// DrawBitmapScaled

internal fun androidx.compose.remote.core.operations.DrawBitmapScaled.readDataReflection():
    DrawBitmapScaledData {
    return DrawBitmapScaledData(
        imageId = dbmpsImageIdField.getInt(this),
        srcLeft = dbmpsSrcLeftField.getFloat(this),
        outSrcLeft = dbmpsOutSrcLeftField.getFloat(this),
        srcTop = dbmpsSrcTopField.getFloat(this),
        outSrcTop = dbmpsOutSrcTopField.getFloat(this),
        srcRight = dbmpsSrcRightField.getFloat(this),
        outSrcRight = dbmpsOutSrcRightField.getFloat(this),
        srcBottom = dbmpsSrcBottomField.getFloat(this),
        outSrcBottom = dbmpsOutSrcBottomField.getFloat(this),
        dstLeft = dbmpsDstLeftField.getFloat(this),
        outDstLeft = dbmpsOutDstLeftField.getFloat(this),
        dstTop = dbmpsDstTopField.getFloat(this),
        outDstTop = dbmpsOutDstTopField.getFloat(this),
        dstRight = dbmpsDstRightField.getFloat(this),
        outDstRight = dbmpsOutDstRightField.getFloat(this),
        dstBottom = dbmpsDstBottomField.getFloat(this),
        outDstBottom = dbmpsOutDstBottomField.getFloat(this),
        contentDescId = dbmpsContentDescIdField.getInt(this),
        scaleFactor = dbmpsScaleFactorField.getFloat(this),
        outScaleFactor = dbmpsOutScaleFactorField.getFloat(this),
        scaleType = dbmpsScaleTypeField.getInt(this),
    )
}

private val dbmpsImageIdField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mImageId")
        .apply { isAccessible = true }
private val dbmpsSrcLeftField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mSrcLeft")
        .apply { isAccessible = true }
private val dbmpsOutSrcLeftField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutSrcLeft")
        .apply { isAccessible = true }
private val dbmpsSrcTopField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mSrcTop")
        .apply { isAccessible = true }
private val dbmpsOutSrcTopField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutSrcTop")
        .apply { isAccessible = true }
private val dbmpsSrcRightField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mSrcRight")
        .apply { isAccessible = true }
private val dbmpsOutSrcRightField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutSrcRight")
        .apply { isAccessible = true }
private val dbmpsSrcBottomField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mSrcBottom")
        .apply { isAccessible = true }
private val dbmpsOutSrcBottomField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutSrcBottom")
        .apply { isAccessible = true }
private val dbmpsDstLeftField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mDstLeft")
        .apply { isAccessible = true }
private val dbmpsOutDstLeftField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutDstLeft")
        .apply { isAccessible = true }
private val dbmpsDstTopField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mDstTop")
        .apply { isAccessible = true }
private val dbmpsOutDstTopField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutDstTop")
        .apply { isAccessible = true }
private val dbmpsDstRightField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mDstRight")
        .apply { isAccessible = true }
private val dbmpsOutDstRightField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutDstRight")
        .apply { isAccessible = true }
private val dbmpsDstBottomField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mDstBottom")
        .apply { isAccessible = true }
private val dbmpsOutDstBottomField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutDstBottom")
        .apply { isAccessible = true }
private val dbmpsContentDescIdField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mContentDescId")
        .apply { isAccessible = true }
private val dbmpsScaleFactorField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mScaleFactor")
        .apply { isAccessible = true }
private val dbmpsOutScaleFactorField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mOutScaleFactor")
        .apply { isAccessible = true }
private val dbmpsScaleTypeField =
    androidx.compose.remote.core.operations.DrawBitmapScaled::class
        .java
        .getDeclaredField("mScaleType")
        .apply { isAccessible = true }

// DrawPath

internal fun androidx.compose.remote.core.operations.DrawPath.readDataReflection(): DrawPathData {
    return DrawPathData(
        id = dpathIdField.getInt(this),
        start = dpathStartField.getFloat(this),
        end = dpathEndField.getFloat(this),
    )
}

private val dpathIdField =
    androidx.compose.remote.core.operations.DrawPath::class.java.getDeclaredField("mId").apply {
        isAccessible = true
    }
private val dpathStartField =
    androidx.compose.remote.core.operations.DrawPath::class.java.getDeclaredField("mStart").apply {
        isAccessible = true
    }
private val dpathEndField =
    androidx.compose.remote.core.operations.DrawPath::class.java.getDeclaredField("mEnd").apply {
        isAccessible = true
    }

// BitmapData

internal fun androidx.compose.remote.core.operations.BitmapData.readDataReflection():
    BitmapDataData {
    return BitmapDataData(
        imageId = bmpDataImageIdField.getInt(this),
        imageWidth = bmpDataWidthField.getInt(this),
        imageHeight = bmpDataHeightField.getInt(this),
        type = bmpDataTypeField.getShort(this),
        encoding = bmpDataEncodingField.getShort(this),
        bitmap = bmpDataBitmapField.get(this) as ByteArray,
    )
}

private val bmpDataImageIdField =
    androidx.compose.remote.core.operations.BitmapData::class
        .java
        .getDeclaredField("mImageId")
        .apply { isAccessible = true }
private val bmpDataWidthField =
    androidx.compose.remote.core.operations.BitmapData::class
        .java
        .getDeclaredField("mImageWidth")
        .apply { isAccessible = true }
private val bmpDataHeightField =
    androidx.compose.remote.core.operations.BitmapData::class
        .java
        .getDeclaredField("mImageHeight")
        .apply { isAccessible = true }
private val bmpDataTypeField =
    androidx.compose.remote.core.operations.BitmapData::class.java.getDeclaredField("mType").apply {
        isAccessible = true
    }
private val bmpDataEncodingField =
    androidx.compose.remote.core.operations.BitmapData::class
        .java
        .getDeclaredField("mEncoding")
        .apply { isAccessible = true }
private val bmpDataBitmapField =
    androidx.compose.remote.core.operations.BitmapData::class
        .java
        .getDeclaredField("mBitmap")
        .apply { isAccessible = true }

// PaintBundle
internal fun androidx.compose.remote.core.operations.paint.PaintBundle.getArrayReflection():
    IntArray {
    return paintBundleArrayField.get(this) as IntArray
}

internal fun androidx.compose.remote.core.operations.paint.PaintBundle.getPosReflection(): Int {
    return paintBundlePosField.getInt(this)
}

private val paintBundleArrayField =
    androidx.compose.remote.core.operations.paint.PaintBundle::class
        .java
        .getDeclaredField("mArray")
        .apply { isAccessible = true }
private val paintBundlePosField =
    androidx.compose.remote.core.operations.paint.PaintBundle::class
        .java
        .getDeclaredField("mPos")
        .apply { isAccessible = true }

// CoreSemantics nullable content description
internal fun androidx.compose.remote.core.semantics.CoreSemantics
    .getContentDescriptionIdReflection(): Int? {
    val id = semContentDescriptionIdField.getInt(this)
    return if (id != 0) id else null
}

private val semContentDescriptionIdField =
    androidx.compose.remote.core.semantics.CoreSemantics::class
        .java
        .getDeclaredField("mContentDescriptionId")
        .apply { isAccessible = true }

// Recollect collections
internal fun androidx.compose.remote.core.CoreDocument.recollectCollectionsReflection() {
    val operations = this.getOperationsReflection()
    val state = this.remoteComposeState
    docCollectCollectionsMethod.invoke(this, operations, state)
}

internal fun androidx.compose.remote.core.CoreDocument.updateTimeReflection(
    context: androidx.compose.remote.core.RemoteContext
) {
    timeVariablesField.get(this)?.let { timeVars -> updateTimeMethod.invoke(timeVars, context) }
}

private val timeVariablesField =
    androidx.compose.remote.core.CoreDocument::class.java.getDeclaredField("mTimeVariables").apply {
        isAccessible = true
    }

private val updateTimeMethod =
    androidx.compose.remote.core.TimeVariables::class
        .java
        .getDeclaredMethod("updateTime", androidx.compose.remote.core.RemoteContext::class.java)
        .apply { isAccessible = true }

private val docCollectCollectionsMethod =
    androidx.compose.remote.core.CoreDocument::class
        .java
        .getDeclaredMethod(
            "collectCollections",
            java.util.ArrayList::class.java,
            androidx.compose.remote.core.RemoteComposeState::class.java,
        )
        .apply { isAccessible = true }

// getVariableId via reflection
internal fun androidx.compose.remote.core.RemoteContext.getVariableIdReflection(name: String): Int {
    try {
        var clazz: Class<*>? = this.javaClass
        var field: java.lang.reflect.Field? = null
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField("mVarNameHashMap")
                break
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        if (field == null) return -1
        field.isAccessible = true
        val map = field.get(this) as? Map<*, *> ?: return -1
        val list = map[name] as? List<*> ?: return -1
        if (list.isEmpty()) return -1
        val varNameObj = list[0]!!
        val idField = varNameObj.javaClass.getDeclaredField("mId").apply { isAccessible = true }
        return idField.getInt(varNameObj)
    } catch (e: Exception) {
        return -1
    }
}

// --- Action Operations Helpers ---

// ValueIntegerChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation.targetValueIdReflection:
    Int
    get() = valIntChangeTargetField.getInt(this)
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation.valueReflection:
    Int
    get() = valIntChangeValueField.getInt(this)

private val valIntChangeTargetField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueIntegerChangeActionOperation::class
        .java
        .getDeclaredField("mTargetValueId")
        .apply { isAccessible = true }
private val valIntChangeValueField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueIntegerChangeActionOperation::class
        .java
        .getDeclaredField("mValue")
        .apply { isAccessible = true }

// ValueFloatChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation.targetValueIdReflection:
    Int
    get() = valFloatChangeTargetField.getInt(this)
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation.valueReflection:
    Float
    get() = valFloatChangeValueField.getFloat(this)

private val valFloatChangeTargetField =
    androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation::class
        .java
        .getDeclaredField("mTargetValueId")
        .apply { isAccessible = true }
private val valFloatChangeValueField =
    androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation::class
        .java
        .getDeclaredField("mValue")
        .apply { isAccessible = true }

// ValueStringChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation.targetValueIdReflection:
    Int
    get() = valStringChangeTargetField.getInt(this)
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation.valueIdReflection:
    Int
    get() = valStringChangeValueIdField.getInt(this)

private val valStringChangeTargetField =
    androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation::class
        .java
        .getDeclaredField("mTargetValueId")
        .apply { isAccessible = true }
private val valStringChangeValueIdField =
    androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation::class
        .java
        .getDeclaredField("mValueId")
        .apply { isAccessible = true }

// ValueIntegerExpressionChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation.targetValueIdReflection:
    Long
    get() = valIntExprChangeTargetField.getLong(this)
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation.valueExpressionIdReflection:
    Long
    get() = valIntExprChangeValueExprField.getLong(this)

private val valIntExprChangeTargetField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueIntegerExpressionChangeActionOperation::class
        .java
        .getDeclaredField("mTargetValueId")
        .apply { isAccessible = true }
private val valIntExprChangeValueExprField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueIntegerExpressionChangeActionOperation::class
        .java
        .getDeclaredField("mValueExpressionId")
        .apply { isAccessible = true }

// ValueFloatExpressionChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation.targetValueIdReflection:
    Int
    get() = valFloatExprChangeTargetField.getInt(this)
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation.valueExpressionIdReflection:
    Int
    get() = valFloatExprChangeValueExprField.getInt(this)

private val valFloatExprChangeTargetField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueFloatExpressionChangeActionOperation::class
        .java
        .getDeclaredField("mTargetValueId")
        .apply { isAccessible = true }
private val valFloatExprChangeValueExprField =
    androidx.compose.remote.core.operations.layout.modifiers
            .ValueFloatExpressionChangeActionOperation::class
        .java
        .getDeclaredField("mValueExpressionId")
        .apply { isAccessible = true }

// --- BorderModifierOperation Helper ---

internal fun androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
    .readDataReflection(): BorderModifierOperationData {
    return BorderModifierOperationData(
        useColorId = borderUseColorIdField.getBoolean(this),
        colorId = borderColorIdField.getInt(this),
        r = borderRField.getFloat(this),
        g = borderGField.getFloat(this),
        b = borderBField.getFloat(this),
        a = borderAField.getFloat(this),
        borderWidth = borderWidthField.getFloat(this),
        roundedCorner = borderRoundedCornerField.getFloat(this),
        shapeType = borderShapeTypeField.getInt(this),
    )
}

private val borderUseColorIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mUseColorId")
        .apply { isAccessible = true }
private val borderColorIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mColorId")
        .apply { isAccessible = true }
private val borderRField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mR")
        .apply { isAccessible = true }
private val borderGField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mG")
        .apply { isAccessible = true }
private val borderBField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mB")
        .apply { isAccessible = true }
private val borderAField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mA")
        .apply { isAccessible = true }
private val borderWidthField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mBorderWidth")
        .apply { isAccessible = true }
private val borderRoundedCornerField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mRoundedCorner")
        .apply { isAccessible = true }
private val borderShapeTypeField =
    androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation::class
        .java
        .getDeclaredField("mShapeType")
        .apply { isAccessible = true }

// --- BackgroundModifierOperation Helper ---

internal fun androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
    .readDataReflection(): BackgroundModifierOperationData {
    return BackgroundModifierOperationData(
        useColorId = bgUseColorIdField.getBoolean(this),
        colorId = bgColorIdField.getInt(this),
        rId = bgRIdField.getFloat(this),
        gId = bgGIdField.getFloat(this),
        bId = bgBIdField.getFloat(this),
        aId = bgAIdField.getFloat(this),
        shapeType = bgShapeTypeField.getInt(this),
    )
}

private val bgUseColorIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mUseColorId")
        .apply { isAccessible = true }
private val bgColorIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mColorId")
        .apply { isAccessible = true }
private val bgRIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mRId")
        .apply { isAccessible = true }
private val bgGIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mGId")
        .apply { isAccessible = true }
private val bgBIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mBId")
        .apply { isAccessible = true }
private val bgAIdField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mAId")
        .apply { isAccessible = true }
private val bgShapeTypeField =
    androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation::class
        .java
        .getDeclaredField("mShapeType")
        .apply { isAccessible = true }

// --- StateLayout Helper ---
internal val androidx.compose.remote.core.operations.layout.managers.StateLayout.indexIdReflection:
    Int
    get() = stateLayoutIndexIdField.getInt(this)

private val stateLayoutIndexIdField =
    androidx.compose.remote.core.operations.layout.managers.StateLayout::class
        .java
        .getDeclaredField("mIndexId")
        .apply { isAccessible = true }

// --- CoreDocument updateVariables Helper ---
internal fun androidx.compose.remote.core.CoreDocument.updateVariablesReflection(
    context: androidx.compose.remote.core.RemoteContext,
    theme: Int,
    operations: List<androidx.compose.remote.core.Operation>,
) {
    docUpdateVariablesMethod.invoke(this, context, theme, operations)
}

private val docUpdateVariablesMethod =
    androidx.compose.remote.core.CoreDocument::class
        .java
        .getDeclaredMethod(
            "updateVariables",
            androidx.compose.remote.core.RemoteContext::class.java,
            Int::class.javaPrimitiveType,
            List::class.java,
        )
        .apply { isAccessible = true }

// AlignByModifierOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation.lineReflection:
    Float
    get() = alignByLineField.getFloat(this)

private val alignByLineField =
    androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation::class
        .java
        .getDeclaredField("mLine")
        .apply { isAccessible = true }

// --- CoreText Reflection Helper ---

internal fun androidx.compose.remote.core.operations.layout.managers.CoreText.readDataReflection():
    CoreTextData {
    return CoreTextData(
        colorValue = coreTextColorField.getInt(this),
        fontSizeValue = coreTextFontSizeField.getFloat(this),
        type = coreTextTypeField.getInt(this),
        fontWeightValue = coreTextFontWeightField.getFloat(this),
        fontStyle = coreTextFontStyleField.getInt(this),
        textAlignValue = coreTextTextAlignField.getInt(this),
        overflow = coreTextOverflowField.getInt(this),
        maxLines = coreTextMaxLinesField.getInt(this),
        letterSpacing = coreTextLetterSpacingField.getFloat(this),
        lineHeightMultiplier = coreTextLineHeightMultiplierField.getFloat(this),
        lineHeightAdd = coreTextLineHeightAddField.getFloat(this),
        underline = coreTextUnderlineField.getBoolean(this),
        strikethrough = coreTextStrikethroughField.getBoolean(this),
        fontAxis = coreTextFontAxisField.get(this) as? IntArray,
        fontAxisValues = coreTextFontAxisValuesField.get(this) as? FloatArray,
    )
}

private val coreTextColorField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mColorValue")
        .apply { isAccessible = true }
private val coreTextFontSizeField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mFontSizeValue")
        .apply { isAccessible = true }
private val coreTextTypeField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mType")
        .apply { isAccessible = true }
private val coreTextFontWeightField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mFontWeightValue")
        .apply { isAccessible = true }
private val coreTextFontStyleField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mFontStyle")
        .apply { isAccessible = true }
private val coreTextTextAlignField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mTextAlignValue")
        .apply { isAccessible = true }
private val coreTextOverflowField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mOverflow")
        .apply { isAccessible = true }
private val coreTextMaxLinesField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mMaxLines")
        .apply { isAccessible = true }
private val coreTextLetterSpacingField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mLetterSpacing")
        .apply { isAccessible = true }
private val coreTextLineHeightMultiplierField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mLineHeightMultiplier")
        .apply { isAccessible = true }
private val coreTextLineHeightAddField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mLineHeightAdd")
        .apply { isAccessible = true }
private val coreTextUnderlineField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mUnderline")
        .apply { isAccessible = true }
private val coreTextStrikethroughField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mStrikethrough")
        .apply { isAccessible = true }
private val coreTextFontAxisField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mFontAxis")
        .apply { isAccessible = true }
private val coreTextFontAxisValuesField =
    androidx.compose.remote.core.operations.layout.managers.CoreText::class
        .java
        .getDeclaredField("mFontAxisValues")
        .apply { isAccessible = true }

// --- TextLayout Reflection Helper ---

internal fun androidx.compose.remote.core.operations.layout.managers.TextLayout
    .readDataReflection(): TextLayoutData {
    return TextLayoutData(
        colorValue = textLayoutColorField.getInt(this),
        fontSizeValue = textLayoutFontSizeField.getFloat(this),
        type = textLayoutTypeField.getInt(this),
        fontWeight = textLayoutFontWeightField.getFloat(this),
        textAlignValue = textLayoutTextAlignField.getInt(this),
        overflow = textLayoutOverflowField.getInt(this),
        maxLines = textLayoutMaxLinesField.getInt(this),
    )
}

private val textLayoutColorField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mColorValue")
        .apply { isAccessible = true }
private val textLayoutFontSizeField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mFontSizeValue")
        .apply { isAccessible = true }
private val textLayoutTypeField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mType")
        .apply { isAccessible = true }
private val textLayoutFontWeightField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mFontWeight")
        .apply { isAccessible = true }
private val textLayoutTextAlignField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mTextAlignValue")
        .apply { isAccessible = true }
private val textLayoutOverflowField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mOverflow")
        .apply { isAccessible = true }
private val textLayoutMaxLinesField =
    androidx.compose.remote.core.operations.layout.managers.TextLayout::class
        .java
        .getDeclaredField("mMaxLines")
        .apply { isAccessible = true }

// 6. Layout Spacing Helpers
private val rowSpacedByField =
    RowLayout::class.java.getDeclaredField("mSpacedBy").apply { isAccessible = true }
private val columnSpacedByField =
    ColumnLayout::class.java.getDeclaredField("mSpacedBy").apply { isAccessible = true }

internal fun rowSpacedBy(layout: RowLayout): Float = rowSpacedByField.getFloat(layout)

internal fun columnSpacedBy(layout: ColumnLayout): Float = columnSpacedByField.getFloat(layout)

// 7. ScrollModifier Reflection
private val scrollPositionField =
    ScrollModifierOperation::class.java.getDeclaredField("mPositionExpression").apply {
        isAccessible = true
    }

internal fun scrollPosition(op: ScrollModifierOperation): Float = scrollPositionField.getFloat(op)

private val touchStopModeField =
    TouchExpression::class.java.getDeclaredField("mStopMode").apply { isAccessible = true }
private val touchStopSpecField =
    TouchExpression::class.java.getDeclaredField("mStopSpec").apply { isAccessible = true }

internal fun touchStopMode(touch: TouchExpression): Int = touchStopModeField.getInt(touch)

internal fun touchStopSpec(touch: TouchExpression): FloatArray? =
    touchStopSpecField.get(touch) as? FloatArray

// 8. DimensionConstraints Reflection
private val dimensionConstraintsTypeField =
    DimensionConstraintsModifierOperation::class.java.getDeclaredField("mType").apply {
        isAccessible = true
    }

internal fun dimensionConstraintsType(op: DimensionConstraintsModifierOperation): Int =
    dimensionConstraintsTypeField.getInt(op)

// 9. DimensionModifier Reflection
private val dimensionMValueField =
    DimensionModifierOperation::class.java.getDeclaredField("mValue").apply { isAccessible = true }

internal fun dimensionRawValue(op: DimensionModifierOperation): Float =
    dimensionMValueField.getFloat(op)

// 10. CollapsiblePriority Reflection
private val sortWithPrioritiesMethod =
    Class.forName("androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority")
        .getDeclaredMethod(
            "sortWithPriorities",
            ArrayList::class.java,
            Int::class.javaPrimitiveType,
        )
        .apply { isAccessible = true }

internal fun sortWithPriorities(
    children: ArrayList<androidx.compose.remote.core.operations.layout.Component>,
    orientation: Int,
): List<androidx.compose.remote.core.operations.layout.Component> {
    @Suppress("UNCHECKED_CAST")
    return sortWithPrioritiesMethod.invoke(null, children, orientation)
        as ArrayList<androidx.compose.remote.core.operations.layout.Component>
}
