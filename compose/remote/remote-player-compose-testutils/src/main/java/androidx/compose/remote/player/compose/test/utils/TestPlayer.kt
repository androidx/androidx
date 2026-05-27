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

package androidx.compose.remote.player.compose.test.utils

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.LongConstant
import java.io.ByteArrayInputStream
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.HashMap
import org.mockito.Mockito.mock
import org.mockito.Mockito.withSettings
import org.mockito.stubbing.Answer

/**
 * A test player that renders a Remote Compose document and captures all draw commands and paint
 * changes as a list of human-readable strings.
 */
class TestPlayer(private val document: CoreDocument, width: Float, height: Float) {
    internal val context = DebugRemoteContext()

    /** The list of captured draw commands. */
    val commands: MutableList<String> = ArrayList<String>()

    private val paintChanges =
        Proxy.newProxyInstance(
            PaintChanges::class.java.classLoader,
            arrayOf(PaintChanges::class.java),
        ) { _, method, args ->
            val methodName = method.name
            val text =
                when (methodName) {
                    "setColor" -> {
                        val color = args[0] as Int
                        String.format("  paint.setColor(0x%08X)", color)
                    }
                    "setColorFilter" -> {
                        val color = args[0] as Int
                        val mode = args[1] as Int
                        String.format("  paint.setColorFilter(0x%08X, %d)", color, mode)
                    }
                    "setLinearGradient",
                    "setRadialGradient",
                    "setSweepGradient",
                    "setFontVariationAxes",
                    "setPathEffect" -> {
                        "  paint.$methodName(...)"
                    }
                    "setTextureShader" -> {
                        val bitmapId = args[0] as Int
                        "  paint.setTextureShader($bitmapId, ...)"
                    }
                    else -> {
                        val mappedArgs =
                            args?.map { arg ->
                                if (arg is Float) arg.formatToString() else arg.toString()
                            }
                        val formattedArgs = mappedArgs?.joinToString() ?: ""
                        "  paint.$methodName($formattedArgs)"
                    }
                }
            commands.add(text)
            null
        } as PaintChanges

    private var needsRepaint = false

    val paintContext: DebugPaintContextBase =
        mock(
                DebugPaintContextBase::class.java,
                withSettings()
                    .useConstructor(context)
                    .defaultAnswer(
                        Answer { invocation ->
                            val method = invocation.method
                            val methodName = method.name
                            val args = invocation.arguments

                            if (Modifier.isAbstract(method.modifiers)) {
                                val formattedArgs = formatArgs(methodName, args)
                                commands.add("$methodName($formattedArgs)")

                                val returnType = method.returnType
                                when (returnType) {
                                    Boolean::class.javaPrimitiveType -> false
                                    Int::class.javaPrimitiveType -> 0
                                    Float::class.javaPrimitiveType -> 0f
                                    else -> null
                                }
                            } else {
                                invocation.callRealMethod()
                            }
                        }
                    ),
            )
            .apply {
                this.testPlayer = this@TestPlayer
                this.paintChanges = this@TestPlayer.paintChanges
            }

    init {
        context.setPaintContext(paintContext)
        document.initializeContext(context)

        val state = document.remoteComposeState
        state.updateFloat(RemoteContext.ID_WINDOW_WIDTH, width)
        state.updateFloat(RemoteContext.ID_WINDOW_HEIGHT, height)
        state.updateFloat(RemoteContext.ID_COMPONENT_WIDTH, width)
        state.updateFloat(RemoteContext.ID_COMPONENT_HEIGHT, height)

        document.applyDataOperations(context)
        context.mWidth = width
        context.mHeight = height
    }

    /** Execute the paint pass and return the captured draw commands. */
    fun paint(theme: Int = Theme.UNSPECIFIED): List<String> {
        document.paint(context, theme)
        commands.clear()
        // Necessary because there can be a 1 frame latency on settled layout.
        forceMarkDirty(document)
        document.paint(context, theme)
        return commands
    }

    private fun forceMarkDirty(document: CoreDocument) {
        for (op in document.operations) {
            op.markDirty()
            if (op is LayoutComponent) {
                markComponentDirtyRecursive(op)
            }
        }
    }

    private fun markComponentDirtyRecursive(component: LayoutComponent) {
        val modifiers = component.componentModifiers.modifiersList
        for (modifier in modifiers) {
            if (modifier is Operation) {
                modifier.markDirty()
            }
        }

        for (child in component.childrenComponents) {
            child.markDirty()
            if (child is LayoutComponent) {
                markComponentDirtyRecursive(child)
            }
        }
    }

    companion object {
        /** Create a [TestPlayer] from a serialized Remote Compose document byte array. */
        @JvmStatic
        fun fromBytes(
            bytes: ByteArray,
            width: Float,
            height: Float,
            clock: RemoteClock = RemoteClock.SYSTEM,
        ): TestPlayer {
            val doc =
                CoreDocument(clock).apply {
                    ByteArrayInputStream(bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
            return TestPlayer(doc, width, height)
        }
    }

    /** A debug implementation of RemoteContext that uses DebugPaintContext. */
    class DebugRemoteContext : RemoteContext() {

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
            mRemoteComposeState.putPathData(instanceId, floatPath)
            mRemoteComposeState.putPathWinding(instanceId, winding)
        }

        override fun getPathData(instanceId: Int): FloatArray? {
            return mRemoteComposeState.getPathData(instanceId)
        }

        private class VarName(val name: String, val id: Int, val type: Int)

        private val varNameMap = HashMap<String, ArrayList<VarName>>()

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {
            var list = varNameMap[varName]
            if (list == null) {
                list = ArrayList()
                varNameMap[varName] = list
            }
            for (v in list) {
                if (v.id == varId) return
            }
            list.add(VarName(varName, varId, varType))
        }

        override fun loadColor(id: Int, color: Int) {
            mRemoteComposeState.updateColor(id, color)
        }

        override fun setNamedColorOverride(colorName: String, color: Int) {
            val list = varNameMap[colorName]
            list?.forEach { mRemoteComposeState.overrideColor(it.id, color) }
        }

        override fun setNamedStringOverride(stringName: String, value: String) {
            val list = varNameMap[stringName]
            list?.forEach { mRemoteComposeState.overrideData(it.id, value) }
        }

        override fun clearNamedStringOverride(stringName: String) {
            val list = varNameMap[stringName]
            list?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
        }

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
            setNamedIntegerOverride(booleanName, if (value) 1 else 0)
        }

        override fun clearNamedBooleanOverride(stringName: String) {
            clearNamedIntegerOverride(stringName)
        }

        override fun setNamedIntegerOverride(integerName: String, value: Int) {
            val list = varNameMap[integerName]
            list?.forEach { mRemoteComposeState.overrideInteger(it.id, value) }
        }

        override fun clearNamedIntegerOverride(integerName: String) {
            val list = varNameMap[integerName]
            list?.forEach { mRemoteComposeState.clearIntegerOverride(it.id) }
        }

        override fun setNamedFloatOverride(floatName: String, value: Float) {
            val list = varNameMap[floatName]
            list?.forEach { mRemoteComposeState.overrideFloat(it.id, value) }
        }

        override fun clearNamedFloatOverride(floatName: String) {
            val list = varNameMap[floatName]
            list?.forEach { mRemoteComposeState.clearFloatOverride(it.id) }
        }

        override fun setNamedLong(name: String, value: Long) {
            val list = varNameMap[name]
            list?.forEach {
                val longConstant = mRemoteComposeState.getObject(it.id) as? LongConstant
                longConstant?.value = value
            }
        }

        override fun setNamedDataOverride(dataName: String, value: Any) {
            val list = varNameMap[dataName]
            list?.forEach { mRemoteComposeState.overrideData(it.id, value) }
        }

        override fun clearNamedDataOverride(dataName: String) {
            val list = varNameMap[dataName]
            list?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
        }

        override fun addCollection(id: Int, collection: ArrayAccess) {
            mRemoteComposeState.addCollection(id, collection)
        }

        override fun putDataMap(id: Int, map: DataMap) {
            mRemoteComposeState.putDataMap(id, map)
        }

        override fun getDataMap(id: Int): DataMap? {
            return mRemoteComposeState.getDataMap(id)
        }

        override fun runAction(id: Int, metadata: String) {
            mDocument.performClick(this, id, metadata)
        }

        override fun runNamedAction(id: Int, value: Any?) {
            val text = getText(id)
            if (text != null) {
                mDocument.runNamedAction(text, value)
            }
        }

        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateObject(id, value)
        }

        override fun getObject(id: Int): Any? {
            return mRemoteComposeState.getObject(id)
        }

        override fun hapticEffect(type: Int) {
            mDocument.haptic(type)
        }

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {
            mRemoteComposeState.cacheData(imageId, "Bitmap{id=$imageId, w=$width, h=$height}")
        }

        override fun loadText(id: Int, text: String) {
            mRemoteComposeState.cacheData(id, text)
        }

        override fun getText(id: Int): String? {
            return mRemoteComposeState.getFromId(id) as? String
        }

        override fun loadFloat(id: Int, value: Float) {
            mRemoteComposeState.updateFloat(id, value)
        }

        override fun overrideFloat(id: Int, value: Float) {
            mRemoteComposeState.overrideFloat(id, value)
        }

        override fun loadInteger(id: Int, value: Int) {
            mRemoteComposeState.updateInteger(id, value)
        }

        override fun overrideInteger(id: Int, value: Int) {
            mRemoteComposeState.overrideInteger(id, value)
        }

        override fun overrideText(id: Int, valueId: Int) {
            val text = getText(valueId)
            if (text != null) {
                mRemoteComposeState.overrideData(id, text)
            }
        }

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
            mRemoteComposeState.cacheData(id, animatedFloat)
        }

        override fun loadShader(id: Int, value: ShaderData) {
            mRemoteComposeState.cacheData(id, value)
        }

        override fun getFloat(id: Int): Float {
            return mRemoteComposeState.getFloat(id)
        }

        override fun getInteger(id: Int): Int {
            return mRemoteComposeState.getInteger(id)
        }

        override fun getLong(id: Int): Long {
            val obj = mRemoteComposeState.getObject(id)
            if (obj is LongConstant) {
                return obj.value
            }
            return 0L
        }

        override fun getColor(id: Int): Int {
            return mRemoteComposeState.getColor(id)
        }

        override fun listensTo(id: Int, variableSupport: VariableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport)
        }

        override fun updateOps(): Int {
            return mRemoteComposeState.getOpsToUpdate(this, currentTime)
        }

        override fun getShader(id: Int): ShaderData? {
            return mRemoteComposeState.getFromId(id) as? ShaderData
        }

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {
            val contentDescription = getText(contentDescriptionId)
            val metadata = getText(metadataId)
            mDocument.addClickArea(id, contentDescription, left, top, right, bottom, metadata)
        }
    }

    /** A fake implementation of ComputedTextLayout for testing on JVM. */
    class FakeComputedTextLayout(
        private val mWidth: Float,
        private val mHeight: Float,
        private val mLineCount: Int = 1,
    ) : RcPlatformServices.ComputedTextLayout {
        override fun getWidth(): Float = mWidth

        override fun getHeight(): Float = mHeight

        override fun getVisibleLineCount(): Int = mLineCount

        override fun isHyphenatedText(): Boolean = false
    }
}

abstract class DebugPaintContextBase(context: RemoteContext) : PaintContext(context) {
    lateinit var testPlayer: TestPlayer
    lateinit var paintChanges: PaintChanges

    override fun getContext(): RemoteContext {
        return testPlayer.context
    }

    override fun getClock(): RemoteClock {
        return testPlayer.context.clock
    }

    override fun applyPaint(paintData: PaintBundle) {
        testPlayer.commands.add("applyPaint:")
        paintData.applyPaintChange(this, paintChanges)
    }

    override fun replacePaint(paintBundle: PaintBundle) {
        testPlayer.commands.add("replacePaint:")
        paintBundle.applyPaintChange(this, paintChanges)
    }

    override fun getText(id: Int): String? {
        return testPlayer.context.getText(id)
    }

    override fun getTextBounds(textId: Int, start: Int, end: Int, flags: Int, bounds: FloatArray) {
        val str = getText(textId)
        if (str == null) {
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = 0f
            bounds[3] = 0f
            return
        }
        val actualEnd = if (end == -1) str.length else end
        val len = actualEnd - start
        bounds[0] = 0f
        bounds[1] = -12f // ascent
        bounds[2] = len * 10f // width
        bounds[3] = 3f // descent
    }

    override fun layoutComplexText(
        textId: Int,
        start: Int,
        end: Int,
        alignment: Int,
        overflow: Int,
        maxLines: Int,
        maxWidth: Float,
        maxHeight: Float,
        letterSpacing: Float,
        lineHeightAdd: Float,
        lineHeightMultiplier: Float,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        useUnderline: Boolean,
        strikethrough: Boolean,
        flags: Int,
    ): RcPlatformServices.ComputedTextLayout? {
        val str = getText(textId)
        val actualStr =
            if (str != null) {
                val s = if (start < 0) 0 else start
                val e = if (end == -1 || end > str.length) str.length else end
                if (s < e) str.substring(s, e) else ""
            } else ""

        testPlayer.commands.add(
            String.format(
                "layoutComplexText(id=%d, text=\"%s\", start=%d, end=%d, maxWidth=%f, maxLines=%d)",
                textId,
                actualStr,
                start,
                end,
                maxWidth,
                maxLines,
            )
        )

        val charWidth = 10f
        val defaultLineHeight = 15f
        val textWidth = actualStr.length * charWidth

        if (maxWidth <= 0f || textWidth <= maxWidth) {
            return TestPlayer.FakeComputedTextLayout(textWidth, defaultLineHeight, 1)
        }

        val words = actualStr.split(" ")
        var currentLineWidth = 0f
        var lineCount = 1
        var maxLineWidthMeasured = 0f

        for (word in words) {
            val wordWidth = word.length * charWidth
            val spaceWidth = if (currentLineWidth > 0) charWidth else 0f

            if (currentLineWidth + spaceWidth + wordWidth <= maxWidth) {
                currentLineWidth += spaceWidth + wordWidth
            } else {
                if (currentLineWidth > 0) {
                    maxLineWidthMeasured = maxOf(maxLineWidthMeasured, currentLineWidth)
                    lineCount++
                    currentLineWidth = wordWidth
                } else {
                    lineCount++
                    currentLineWidth = 0f
                    maxLineWidthMeasured = maxOf(maxLineWidthMeasured, wordWidth)
                }
            }
        }
        maxLineWidthMeasured = maxOf(maxLineWidthMeasured, currentLineWidth)

        val finalLineCount = if (maxLines in 1 until lineCount) maxLines else lineCount
        val computedHeight = finalLineCount * defaultLineHeight
        val computedWidth = minOf(maxLineWidthMeasured, maxWidth)

        return TestPlayer.FakeComputedTextLayout(computedWidth, computedHeight, finalLineCount)
    }

    override fun drawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        val text = getText(textId)
        testPlayer.commands.add(
            String.format(
                "drawTextRun(text=\"%s\", start=%d, end=%d, x=%f, y=%f)",
                text,
                start,
                end,
                x,
                y,
            )
        )
    }

    override fun drawComplexText(computedTextLayout: RcPlatformServices.ComputedTextLayout?) {
        if (computedTextLayout != null) {
            testPlayer.commands.add(
                String.format(
                    "drawComplexText(w=%f, h=%f)",
                    computedTextLayout.width,
                    computedTextLayout.height,
                )
            )
        } else {
            testPlayer.commands.add("drawComplexText(null)")
        }
    }

    override fun roundedClipRect(
        width: Float,
        height: Float,
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {
        testPlayer.commands.add(String.format("roundedClipRect(%f, %f, ...)", width, height))
    }

    override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {
        testPlayer.commands.add(
            String.format("drawToBitmap(id=%d, mode=%d, color=0x%08X)", bitmapId, mode, color)
        )
    }
}

private fun Float.formatToString(): String {
    return if (isNaN()) "NaN" else String.format("%f", this)
}

private fun formatArgs(methodName: String, args: Array<Any>?): String {
    if (args == null) return ""
    return args
        .map { arg ->
            when (arg) {
                is Float -> arg.formatToString()
                is Int -> {
                    if (
                        methodName.startsWith("draw") &&
                            methodName.contains("color", ignoreCase = true)
                    ) {
                        String.format("0x%08X", arg)
                    } else {
                        arg.toString()
                    }
                }
                else -> arg.toString()
            }
        }
        .joinToString()
}
