/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.remotecompose

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.PathData.CLOSE
import androidx.compose.remote.core.operations.PathData.CONIC
import androidx.compose.remote.core.operations.PathData.CUBIC
import androidx.compose.remote.core.operations.PathData.DONE
import androidx.compose.remote.core.operations.PathData.LINE
import androidx.compose.remote.core.operations.PathData.MOVE
import androidx.compose.remote.core.operations.PathData.QUADRATIC
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils.idFromNan
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.operations.utilities.IntFloatMap
import androidx.compose.remote.core.operations.utilities.IntIntMap
import androidx.compose.remote.core.operations.utilities.IntMap
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.Backend
import androidx.glance.appwidget.GlanceComponents
import androidx.glance.appwidget.RemoteViewsRoot
import androidx.glance.appwidget.normalizeCompositionTree
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.runTestingComposition

private object TestData {
    val fakeComponentName = ComponentName("test component", "test component")
    val fakeAppwidgetId = 1
    val fakeGlanceComponents =
        GlanceComponents(fakeComponentName, fakeComponentName, fakeComponentName, fakeComponentName)
}

object RemoteComposeTestUtils {

    /** Assuming a layout that consists of [root node]->[child 1] -> [child n], returns [child n] */
    fun getSimpleLeaf(doc: CoreDocument): Component {
        val root: RootLayoutComponent =
            doc.operations.find { it is RootLayoutComponent } as RootLayoutComponent
        val operations = root.mList
        val layoutComponent: LayoutComponent = operations.first() as LayoutComponent

        fun findLeafNode(component: Component): Component {

            if (component !is LayoutComponent) return component

            val children = component.childrenComponents
            return if (children.isEmpty()) {
                component
            } else if (children.size > 1) {
                throw IllegalStateException(
                    "not a simple linear tree, cannot traverse. $component has ${children.size} children"
                )
            } else {
                findLeafNode(children[0])
            }
        }

        return findLeafNode(layoutComponent)
    }

    fun debugPrintDoc(doc: CoreDocument) {
        if (DebugRemoteCompose) {
            Log.d(TAG, "~~~~~~~~~~~~~~~~~~~")
            Log.d(TAG, doc.toNestedString())
            doc.operations.forEach { println(it.toString() + "\n--- -- -- -- --") }
        }
    }

    /** @return A pair to make other code more succinct */
    internal suspend fun Context.runAndTranslateSingleRoot(
        content: @Composable () -> Unit
    ): Pair<RemoteComposeContext, WireBuffer> {

        val originalRoot = runTestingComposition(content)

        val normalizedRoot: RemoteViewsRoot =
            (originalRoot.copy() as RemoteViewsRoot).also {
                normalizeCompositionTree(backendOverrideRequest = Backend.RemoteCompose, root = it)
            }
        // ... or here?
        val rcContext: RemoteComposeContext =
            (GlanceRemoteComposeTranslator.translateEmittableTreeToRemoteCompose(
                    context = this,
                    remoteViewRoot = normalizedRoot,
                    appWidgetId = TestData.fakeAppwidgetId,
                    glanceComponents = TestData.fakeGlanceComponents,
                    actionBroadcastReceiver = TestData.fakeComponentName,
                ) as GlanceToRemoteComposeTranslation.Single)
                .remoteComposeContext
        val wireBuffer: WireBuffer = rcContext.buffer.buffer
        return Pair(rcContext, wireBuffer)
    }
}

/** For retrieving properties from [LayoutComponent] subclasses using the map serialization */
internal object RemoteComposeLayoutInspection {
    inline fun <reified T> getProperty(rcComponent: LayoutComponent, property: String): Any? {
        val mapSerializer = TestMapSerializer()
        rcComponent.serialize(mapSerializer)
        val result = mapSerializer.map.get(property)
        return result as T
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getSubProperty(
        rcComponent: LayoutComponent,
        property: String,
        subProperty: String,
    ): Any? {
        val mapSerializer = TestMapSerializer()
        rcComponent.serialize(mapSerializer)
        val innerMap = mapSerializer.map.get(property) as Map<String, Any?>
        return innerMap.get(subProperty) as T
    }
}

internal suspend fun Context.runAndTranslateMultiRoot(
    content: @Composable () -> Unit
): List<Pair<DpSize, GlanceToRemoteComposeTranslation.Single>> {
    val originalRoot = runTestingComposition(content)
    // TODO: could we put the size box stuff here?
    val normalizedRoot: RemoteViewsRoot =
        (originalRoot.copy() as RemoteViewsRoot).also {
            normalizeCompositionTree(backendOverrideRequest = Backend.RemoteCompose, root = it)
        }
    // ... or here?
    val result: GlanceToRemoteComposeTranslation.SizeMap =
        GlanceRemoteComposeTranslator.translateEmittableTreeToRemoteCompose(
            context = this,
            remoteViewRoot = normalizedRoot,
            appWidgetId = TestData.fakeAppwidgetId,
            glanceComponents = TestData.fakeGlanceComponents,
            actionBroadcastReceiver = TestData.fakeComponentName,
        ) as GlanceToRemoteComposeTranslation.SizeMap

    return result.results
}

class GlanceDebugCreationContext : RemoteContext() {
    private var hideString: Boolean = true
    var stringBuilder = StringBuilder()
    var textMap = HashMap<Int, String>()
    var dataMapCache = IntMap<DataMap>()

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        TODO("Not yet implemented")
    }

    override fun getPathData(instanceId: Int): FloatArray? {
        TODO("Not yet implemented")
    }

    override fun getDataMap(id: Int): DataMap? {
        return dataMapCache.get(id)
    }

    override fun runAction(id: Int, metadata: String) {
        TODO("Not yet implemented")
    }

    fun clearResults() {
        stringBuilder.clear()
    }

    fun getTestResults(): String {
        return stringBuilder.toString()
    }

    override fun getAnimationTime(): Float {
        return 1f
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        TODO("Not yet implemented")
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        TODO("Not yet implemented")
    }

    override fun clearNamedStringOverride(stringName: String) {
        TODO("Not yet implemented")
    }

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun clearNamedBooleanOverride(booleanName: String) {
        TODO("Not yet implemented")
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        TODO("Not yet implemented")
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        TODO("Not yet implemented")
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        TODO("Not yet implemented")
    }

    override fun clearNamedFloatOverride(floatName: String) {
        TODO("Not yet implemented")
    }

    override fun setNamedLong(name: String, value: Long) {
        TODO("Not yet implemented")
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        TODO("Not yet implemented")
    }

    override fun clearNamedDataOverride(dataName: String) {
        TODO("Not yet implemented")
    }

    override fun addCollection(id: Int, collection: ArrayAccess) {
        TODO("Not yet implemented")
    }

    override fun putDataMap(id: Int, map: DataMap) {
        TODO("Not yet implemented")
    }

    fun setHideString(h: Boolean) {
        hideString = h
    }

    override fun overrideInteger(id: Int, value: Int) {
        TODO("Not yet implemented")
    }

    override fun overrideText(id: Int, valueId: Int) {
        TODO("Not yet implemented")
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        TODO("Not yet implemented")
    }

    override fun loadColor(id: Int, color: Int) {
        TODO("Not yet implemented")
    }

    override fun runNamedAction(textId: Int, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun putObject(mId: Int, command: Any) {
        TODO("Not yet implemented")
    }

    override fun getObject(mId: Int): Any {
        TODO("Not yet implemented")
    }

    override fun hapticEffect(type: Int) {
        TODO("Not yet implemented")
    }

    fun pathString(path: FloatArray?): String {
        if (path == null) {
            return "null"
        }
        val str = StringBuilder()
        var last_return = 0
        for (i in path.indices) {
            if (i != 0) {
                str.append(" ")
            }
            str.append(
                if (path[i].isNaN()) {
                    if (str.length - last_return > 65) {
                        str.append("\n")
                        last_return = str.length
                    }
                    val id = idFromNan(path[i])
                    if (id <= DONE) {
                        when (id) {
                            MOVE -> "M"
                            LINE -> "L"
                            QUADRATIC -> "Q"
                            CONIC -> "R"
                            CUBIC -> "C"
                            CLOSE -> "Z"
                            DONE -> "."
                            else -> "X"
                        }
                    } else {
                        "($id)"
                    }
                } else {
                    "${path[i]}"
                }
            )
        }
        return str.toString()
    }

    override fun setTheme(theme: Int) {
        super.setTheme(theme)
        stringBuilder.append("setTheme($theme)\n")
    }

    init {
        mPaintContext =
            object : PaintContext(this) {

                override fun drawBitmap(
                    imageId: Int,
                    srcLeft: Int,
                    srcTop: Int,
                    srcRight: Int,
                    srcBottom: Int,
                    dstLeft: Int,
                    dstTop: Int,
                    dstRight: Int,
                    dstBottom: Int,
                    cdId: Int,
                ) {
                    stringBuilder.append("drawBitmap <$imageId>\n")
                }

                override fun drawBitmap(
                    id: Int,
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                ) {
                    stringBuilder.append("drawBitmap ($id, $left, $top, $right, $bottom)\n")
                }

                override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {
                    TODO("Not yet implemented")
                }

                override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {
                    TODO("Not yet implemented")
                }

                override fun getText(textID: Int): String? {
                    TODO("Not yet implemented")
                }

                override fun matrixFromPath(
                    pathId: Int,
                    fraction: Float,
                    vOffset: Float,
                    flags: Int,
                ) {
                    TODO("Not yet implemented")
                }

                override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {
                    TODO("Not yet implemented")
                }

                override fun scale(scaleX: Float, scaleY: Float) {
                    stringBuilder.append("scale ($scaleX, $scaleY)\n")
                }

                override fun translate(translateX: Float, translateY: Float) {
                    stringBuilder.append("translate ($translateX, $translateY)\n")
                }

                override fun drawArc(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                    startAngle: Float,
                    sweepAngle: Float,
                ) {
                    stringBuilder.append(
                        "drawArc($left, $top, $right, $bottom, $startAngle, $sweepAngle)\n"
                    )
                }

                override fun drawSector(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                    startAngle: Float,
                    sweepAngle: Float,
                ) {
                    stringBuilder.append(
                        "drawSector($left, $top, $right, $bottom, $startAngle, $sweepAngle)\n"
                    )
                }

                override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {
                    stringBuilder.append("drawCircle($centerX, $centerY, $radius)\n")
                }

                override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
                    stringBuilder.append("drawLine($x1, $y1, $x2, $y2)\n")
                }

                override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {
                    stringBuilder.append("drawOval($left, $top, $right, $bottom)\n")
                }

                override fun drawPath(id: Int, start: Float, end: Float) {
                    stringBuilder.append("drawPath($id, $start, $end)\n")
                }

                override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
                    stringBuilder.append("drawRect($left, $top, $right, $bottom)\n")
                }

                override fun savePaint() {
                    stringBuilder.append("savePaint\n")
                }

                override fun restorePaint() {
                    stringBuilder.append("restorePaint\n")
                }

                override fun replacePaint(paintBundle: PaintBundle) {
                    TODO("Not yet implemented")
                }

                override fun drawRoundRect(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                    radiusX: Float,
                    radiusY: Float,
                ) {
                    stringBuilder.append(
                        "drawRoundRect($left, $top, $right, $bottom, $radiusX, $radiusY)\n"
                    )
                }

                override fun drawTextOnPath(
                    textId: Int,
                    pathId: Int,
                    hOffset: Float,
                    vOffset: Float,
                ) {
                    stringBuilder.append("drawTextOnPath($textId, $pathId, $hOffset, $vOffset)\n")
                }

                override fun getTextBounds(
                    textId: Int,
                    start: Int,
                    end: Int,
                    flags: Int,
                    bounds: FloatArray,
                ) {
                    stringBuilder.append("getTextBounds([$textId], $start, $end)\n")
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
                    TODO("Not yet implemented")
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
                    stringBuilder.append(
                        "drawTextRun($textId, $start, $end, $contextStart, $contextEnd, $x, $y)\n"
                    )
                }

                override fun drawComplexText(
                    computedTextLayout: RcPlatformServices.ComputedTextLayout?
                ) {
                    TODO("Not yet implemented")
                }

                override fun drawTweenPath(
                    path1Id: Int,
                    path2Id: Int,
                    tween: Float,
                    start: Float,
                    stop: Float,
                ) {
                    stringBuilder.append(
                        "drawTweenPath($path1Id, $path2Id, $tween, $start, $stop)\n"
                    )
                }

                override fun applyPaint(paintData: PaintBundle) {
                    stringBuilder.append("paintData($paintData)\n")
                }

                override fun matrixScale(
                    scaleX: Float,
                    scaleY: Float,
                    centerX: Float,
                    centerY: Float,
                ) {
                    if (centerX.isNaN()) {
                        stringBuilder.append("scale($scaleX, $scaleY)\n")
                    } else {
                        stringBuilder.append("scale($scaleX, $scaleY, $centerX, $centerY)\n")
                    }
                }

                override fun matrixTranslate(translateX: Float, translateY: Float) {
                    stringBuilder.append("translate($translateX, $translateY)\n")
                }

                override fun matrixSkew(skewX: Float, skewY: Float) {
                    stringBuilder.append("skew($skewX, $skewY)\n")
                }

                override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {
                    if (pivotX.isNaN()) {
                        stringBuilder.append("rotate($rotate)\n")
                    } else {
                        stringBuilder.append("rotate($rotate, $pivotX, $pivotY)\n")
                    }
                }

                override fun matrixSave() {
                    stringBuilder.append("matrixSave()\n")
                }

                override fun matrixRestore() {
                    stringBuilder.append("matrixRestore()\n")
                }

                override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
                    stringBuilder.append("clipRect($left, $top, $right, $bottom)\n")
                }

                override fun clipPath(pathId: Int, regionOp: Int) {
                    stringBuilder.append("clipPath($pathId, $regionOp)\n")
                }

                override fun roundedClipRect(
                    width: Float,
                    height: Float,
                    topStart: Float,
                    topEnd: Float,
                    bottomStart: Float,
                    bottomEnd: Float,
                ) {
                    stringBuilder.append("roundedClipRect($width, $height)\n")
                }

                override fun reset() {}

                override fun startGraphicsLayer(w: Int, h: Int) {
                    stringBuilder.append("startGraphicsLayer($w x $h)\n")
                }

                override fun setGraphicsLayer(attributes: java.util.HashMap<Int?, in Any>) {
                    TODO("Not yet implemented")
                }

                override fun endGraphicsLayer() {
                    stringBuilder.append("endGraphicsLayer()\n")
                }
            }
    }

    //
    //    fun loadBitmap(imageId: Int, width: Int, height: Int, bitmap: ByteArray) {
    //        stringBuilder.append("loadImage($imageId)\n")
    //    }

    override fun loadText(id: Int, text: String) {
        textMap[id] = text
        val str =
            if (hideString) ""
            else if (text.length < 10) {
                "=\"" + text + "\""
            } else {
                "=\"" + text.substring(0, 7) + "...\""
            }

        stringBuilder.append("loadText($id)$str\n")
    }

    //
    override fun getText(id: Int): String {
        return textMap[id].toString()
    }

    //
    var floatCache = IntFloatMap()
    var integerCache = IntIntMap()

    //
    override fun loadFloat(id: Int, value: Float) {
        floatCache.put(id, value)
        integerCache.put(id, value.toInt())
        stringBuilder.append("loadFloat[$id]=$value\n")
    }

    override fun overrideFloat(id: Int, value: Float) {
        stringBuilder.append("overrideFloat[$id]=$value\n")
    }

    override fun loadInteger(id: Int, value: Int) {
        floatCache.put(id, value.toFloat())
        integerCache.put(id, value)
        stringBuilder.append("loadInteger[$id]=$value\n")
    }

    var animatedFloatCache = Array<FloatExpression?>(200, { null })

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
        animatedFloatCache[id] = animatedFloat
        stringBuilder.append("animatedFloat($id)=$animatedFloat\n")
    }

    override fun loadShader(id: Int, value: ShaderData) {
        stringBuilder.append("loadShaderData($id)\n")
    }

    override fun getFloat(id: Int): Float {
        return floatCache[id]
    }

    override fun getInteger(id: Int): Int {
        return integerCache[id]
    }

    override fun getLong(id: Int): Long {
        TODO("Not yet implemented")
    }

    override fun getColor(id: Int): Int {
        TODO("Not yet implemented")
    }

    // =============== support listening tree ========
    var mVariableSupport = Array<VariableSupport?>(400, { null })
    var listeners = IntArray(400)
    var listenerCount = 0

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        mVariableSupport[id] = variableSupport
        listeners[listenerCount++] = id
    }

    override fun updateOps(): Int {
        for (c in (0 until listenerCount)) {
            mVariableSupport[listeners[c]]?.updateVariables(this)
        }
        return 0 // TODO map out when to update
    }

    override fun getShader(id: Int): ShaderData {
        TODO("Not yet implemented")
    }

    override fun addClickArea(
        id: Int,
        contentDescription: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {
        stringBuilder.append("clickArea($id, $left, $top, $right, $bottom, $metadataId)\n")
    }

    override fun setRootContentBehavior(scroll: Int, alignment: Int, sizing: Int, mode: Int) {
        stringBuilder.append("rootContentBehavior $scroll, $alignment, $sizing, $mode\n")
        super.setRootContentBehavior(scroll, alignment, sizing, mode)
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        bitmap: ByteArray,
    ) {
        TODO("Not yet implemented")
    }
}
