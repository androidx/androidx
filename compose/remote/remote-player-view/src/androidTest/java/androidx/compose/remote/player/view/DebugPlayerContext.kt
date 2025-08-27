/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.Platform
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.PathData.CLOSE
import androidx.compose.remote.core.operations.PathData.CONIC
import androidx.compose.remote.core.operations.PathData.CUBIC
import androidx.compose.remote.core.operations.PathData.DONE
import androidx.compose.remote.core.operations.PathData.LINE
import androidx.compose.remote.core.operations.PathData.MOVE
import androidx.compose.remote.core.operations.PathData.QUADRATIC
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.Utils.idFromNan
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.operations.utilities.IntFloatMap
import androidx.compose.remote.core.operations.utilities.IntIntMap
import androidx.compose.remote.core.operations.utilities.IntMap
import androidx.compose.remote.core.types.LongConstant

class DebugPlayerContext : RemoteContext() {
    private var hideString: Boolean = true
    var stringBuilder = StringBuilder()
    var floatCache = IntFloatMap()
    var integerCache = IntIntMap()
    var colorCache = IntArray(200) { 0 }
    var stringCache = HashMap<Int, String>(200)
    var dataMapCache = IntMap<DataMap>()
    val mObjectMap = IntMap<Any>()
    val mPathDataMap = IntMap<FloatArray>()
    val mPathWindingMap = IntIntMap()
    var varNamesMap = HashMap<String, Int>(200)

    fun clearResults() {
        stringBuilder.clear()
    }

    fun getTestResults(): String {
        return stringBuilder.toString()
    }

    override fun getAnimationTime(): Float {
        return 1f
    }

    fun setHideString(h: Boolean) {
        hideString = h
    }

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        if (winding == 0) {
            stringBuilder.append("loadPathData($instanceId)=" + pathString(floatPath) + "\n")
        } else {
            stringBuilder.append(
                "loadPathData($instanceId)= [$winding]" + pathString(floatPath) + "\n"
            )
        }
        mPathDataMap.put(instanceId, floatPath)
        mPathWindingMap.put(instanceId, winding)
    }

    override fun getPathData(instanceId: Int): FloatArray {
        stringBuilder.append("getPathData($instanceId)= \n")
        return mPathDataMap.get(instanceId)!!
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        varNamesMap[varName] = varId
        stringBuilder.append("loadVariableName($varName)= [$varId] $varType\n")
    }

    override fun loadColor(id: Int, color: Int) {
        colorCache[id] = color
        stringBuilder.append("loadColor([$id])= " + Utils.colorInt(color) + "\n")
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        stringBuilder.append("setNamedColorOverride([$colorName])= " + Utils.colorInt(color) + "\n")
    }

    override fun setNamedLong(name: String, value: Long) {
        var lc = varNamesMap[name]?.let { mObjectMap.get(it) } as LongConstant
        lc.setValue(value)
        stringBuilder.append("setNamedLong([$name])= " + value + "\n")
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        stringBuilder.append("setNamedStringOverride([$stringName])= " + value + "\n")
    }

    override fun clearNamedStringOverride(stringName: String) {
        stringBuilder.append("clearNamedStringOverride([$stringName])\n")
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        stringBuilder.append("setNamedIntegerOverride([$integerName])= " + value + "\n")
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        stringBuilder.append("clearNamedIntegerOverride([$integerName])\n")
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        stringBuilder.append("setNamedFloatOverride([$floatName])= " + value + "\n")
    }

    override fun clearNamedFloatOverride(floatName: String) {
        stringBuilder.append("clearNamedIntegerOverride([$floatName])\n")
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        stringBuilder.append("setNamedDataOverride([$dataName])= " + value + "\n")
    }

    override fun clearNamedDataOverride(dataName: String) {
        stringBuilder.append("clearNamedDataOverride([$dataName])\n")
    }

    override fun addCollection(id: Int, collection: ArrayAccess) {
        Utils.log("add collection **** " + id.toString(16))
        mRemoteComposeState.addCollection(id, collection)
    }

    override fun putDataMap(id: Int, map: DataMap) {
        dataMapCache.put(id, map)
    }

    override fun getDataMap(id: Int): DataMap? {
        return dataMapCache.get(id)
    }

    var lastAction = -1

    override fun runAction(id: Int, metadata: String) {
        lastAction = id
    }

    override fun runNamedAction(textId: Int, value: Any?) {}

    override fun putObject(key: Int, command: Any) {
        mObjectMap.put(key, command)
    }

    override fun getObject(key: Int): Any? {
        return mObjectMap.get(key)
    }

    override fun hapticEffect(type: Int) {
        stringBuilder.append("hapticEffect $type\n")
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

                override fun replacePaint(paint: PaintBundle) {
                    stringBuilder.append("replacePaint\n")
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
                    bounds[0] = 0f
                    bounds[1] = 0f
                    bounds[2] = 100f
                    bounds[3] = 10f
                    stringBuilder.append("getTextBounds($textId, $start, $end)\n")
                }

                override fun layoutComplexText(
                    textId: Int,
                    start: Int,
                    end: Int,
                    alignment: Int,
                    overflow: Int,
                    maxLines: Int,
                    maxWidth: Float,
                    flags: Int,
                ): Platform.ComputedTextLayout? {
                    stringBuilder.append("layoutComplexText($textId, $start, $end)\n")
                    return null
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

                override fun drawComplexText(computedTextLayout: Platform.ComputedTextLayout?) {
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

                override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {
                    stringBuilder.append("tweenPath($out, $path1, $path2, $tween)\n")
                }

                override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {
                    stringBuilder.append("combinePath($out, $path1, $path2, $operation)\n")
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
                    stringBuilder.append("clipPath($pathId)\n")
                }

                override fun reset() {}

                override fun startGraphicsLayer(w: Int, h: Int) {
                    stringBuilder.append("startGraphicsLayer($w x $h)\n")
                }

                override fun setGraphicsLayer(attributes: java.util.HashMap<Int, Any>) {
                    stringBuilder.append("setGraphicsLayer()\n")
                }

                override fun endGraphicsLayer() {
                    stringBuilder.append("endGraphicsLayer()\n")
                }

                override fun getText(textID: Int): String? {
                    stringBuilder.append("getText($textID)\n")
                    return "<TEST$textID>"
                }

                override fun roundedClipRect(
                    width: Float,
                    height: Float,
                    topStart: Float,
                    topEnd: Float,
                    bottomStart: Float,
                    bottomEnd: Float,
                ) {
                    stringBuilder.append("rounded clipRect\n")
                }

                override fun log(content: String) {
                    stringBuilder.append("log: $content\n")
                }

                override fun matrixFromPath(
                    pathId: Int,
                    fraction: Float,
                    vOffset: Float,
                    flags: Int,
                ) {
                    stringBuilder.append("matrixFromPath($pathId, $fraction, $vOffset, $flags)")
                }

                override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {
                    stringBuilder.append("drawToBitmap($bitmapId)\n")
                }
            }
    }

    override fun header(
        majorVersion: Int,
        minorVersion: Int,
        patchVersion: Int,
        width: Int,
        height: Int,
        capabilities: Long,
        map: IntMap<Any>?,
    ) {
        loadInteger(ID_WINDOW_WIDTH, width)
        loadInteger(ID_WINDOW_HEIGHT, height)

        stringBuilder.append(
            "header($majorVersion, $minorVersion, $patchVersion)" +
                " $width x $height, $capabilities\n"
        )
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        bitmap: ByteArray,
    ) {
        stringBuilder.append("loadImage($imageId)\n")
    }

    override fun loadText(id: Int, text: String) {
        stringCache[id] = text
        val str =
            if (hideString) ""
            else if (text.length < 10) {
                "=\"" + text + "\""
            } else {
                "=\"" + text.substring(0, 7) + "...\""
            }

        stringBuilder.append("loadText($id)$str\n")
    }

    override fun getText(id: Int): String {
        stringBuilder.append("getText[$id]= " + stringCache[id] + "\n")
        return stringCache[id].toString()
    }

    override fun loadFloat(id: Int, value: Float) {
        floatCache.put(id, value)
        integerCache.put(id, value.toInt())
        if (!hideString) {
            stringBuilder.append("loadFloat[$id]=$value\n")
        }
        val list = mVariableSupport[id]
        if (list != null) {
            for (v in list) {
                v.markDirty()
            }
        }
    }

    override fun overrideFloat(id: Int, value: Float) {
        stringBuilder.append("overrideFloat($id)$value\n")
    }

    override fun loadInteger(id: Int, value: Int) {
        floatCache.put(id, value.toFloat())
        integerCache.put(id, value)
        if (!hideString) {
            stringBuilder.append("loadInteger[$id]=$value\n")
        }
    }

    override fun overrideInteger(id: Int, value: Int) {
        stringBuilder.append("overrideInteger($id)$value\n")
    }

    override fun overrideText(id: Int, valueId: Int) {
        stringBuilder.append("overrideText($id)$valueId\n")
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
        return colorCache[id]
    }

    var mVariableSupport = Array<ArrayList<VariableSupport>?>(400, { null })
    var listeners = IntArray(400)
    var listenerCount = 0

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        if (mVariableSupport[id] == null) {
            mVariableSupport[id] = ArrayList<VariableSupport>()
        }
        val list = mVariableSupport[id]
        list!!.add(variableSupport)
        listeners[listenerCount++] = id
    }

    override fun updateOps(): Int {
        for (c in (0 until listenerCount)) {
            val list = mVariableSupport[listeners[c]]
            if (list != null) {
                for (v in list) {
                    v.updateVariables(this)
                }
            }
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
}
